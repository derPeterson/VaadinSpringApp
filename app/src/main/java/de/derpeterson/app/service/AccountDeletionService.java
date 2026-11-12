package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.DeviceTokenRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Gemeinsame transaktionale Bereinigung vor jeder administrativen oder eigenen Kontolöschung. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {
  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;
  private final DeviceTokenRepository deviceTokenRepository;
  private final SessionRegistry sessionRegistry;
  private final AvatarDraftStore avatarDraftStore;

  @Transactional(propagation = Propagation.MANDATORY)
  public void prepareAccountDeletion(Long accountUserId, String preservedSessionId) {
    UserEntity accountUser =
        userRepository
            .findByIdForUpdate(accountUserId)
            .orElseThrow(() -> new OptimisticLockingFailureException("Account already deleted"));
    String recipientEmail = accountUser.getEmail();
    deleteAccountContent(accountUserId, recipientEmail);
    deviceTokenRepository.removeUserTokens(recipientEmail);
    // Die eigene UIDL-Antwort benötigt ihre Sitzung noch bis zum anschließenden Logout.
    jdbcTemplate.update(
        "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ? AND SESSION_ID <> ?",
        recipientEmail,
        preservedSessionId);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clearCommittedAccountCaches(accountUserId, recipientEmail, preservedSessionId);
          }
        });
  }

  private void deleteAccountContent(Long accountUserId, String recipientEmail) {
    // Eigene Inhalte entfernen; offene Vorgänge anderer Konten wieder zur Übernahme freigeben.
    jdbcTemplate.update(
        "DELETE FROM admin_notification_attempt WHERE queue_id IN "
            + "(SELECT id FROM email_queue WHERE user_id = ?)",
        accountUserId);
    jdbcTemplate.update(
        "DELETE FROM user_notifications WHERE id IN "
            + "(SELECT notification_id FROM user_notification_recipients WHERE user_id = ?) "
            + "AND NOT EXISTS (SELECT 1 FROM user_notification_recipients r "
            + "WHERE r.notification_id = user_notifications.id AND r.user_id <> ?)",
        accountUserId,
        accountUserId);
    deleteAccountTicketNotifications(accountUserId);
    jdbcTemplate.update("DELETE FROM support_replies WHERE author_id = ?", accountUserId);
    jdbcTemplate.update(
        "UPDATE support_tickets SET assignee_id = NULL, status = 'OPEN', version = version + 1 "
            + "WHERE assignee_id = ? AND status = 'IN_PROGRESS'",
        accountUserId);
    jdbcTemplate.update("DELETE FROM user_notifications WHERE sender_id = ?", accountUserId);
    // Die Empfängerzahl der verbleibenden gemeinsamen Nachricht muss nach dem FK-Cascade stimmen.
    jdbcTemplate.update(
        "UPDATE user_notifications SET recipient_count = "
            + "(SELECT COUNT(*) FROM user_notification_recipients r "
            + "WHERE r.notification_id = user_notifications.id AND r.user_id <> ?) "
            + "WHERE id IN (SELECT notification_id FROM user_notification_recipients WHERE user_id = ?)",
        accountUserId,
        accountUserId);
    jdbcTemplate.update(
        "DELETE FROM login_attempts WHERE identity_hash = ?",
        LoginAttemptService.identityKey(recipientEmail));
  }

  private void deleteAccountTicketNotifications(Long accountUserId) {
    jdbcTemplate.update(
        "DELETE FROM user_notifications WHERE support_reply_id IN "
            + "(SELECT id FROM support_replies WHERE author_id = ?)",
        accountUserId);
    List<Long> ownedTicketIds =
        jdbcTemplate.queryForList(
            "SELECT id FROM support_tickets WHERE owner_id = ?", Long.class, accountUserId);
    for (Long ownedTicketId : ownedTicketIds) {
      jdbcTemplate.update(
          "DELETE FROM user_notifications WHERE support_ticket_number = ?",
          SupportTicketService.ticketNumber(ownedTicketId));
    }
  }

  private void clearCommittedAccountCaches(
      Long accountUserId, String accountEmail, String preservedSessionId) {
    try {
      avatarDraftStore.releaseForUser(accountUserId);
      sessionRegistry.getAllSessions(accountEmail, true).stream()
          .map(SessionInformation::getSessionId)
          .filter(sessionId -> !sessionId.equals(preservedSessionId))
          .forEach(sessionRegistry::removeSessionInformation);
    } catch (RuntimeException cleanupFailure) {
      // Die persistenten Nachweise sind bereits widerrufen; kein falscher Rollbackbericht.
      log.warn(
          "Account deletion committed; local cache cleanup interrupted",
          safeFailure(cleanupFailure));
    }
  }
}
