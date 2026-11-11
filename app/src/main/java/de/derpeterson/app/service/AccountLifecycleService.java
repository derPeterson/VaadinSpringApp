package de.derpeterson.app.service;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_REACTIVATION_ROUTE;
import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.AccountDeactivation;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.Owner;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.State;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLifecycleService {
  public enum AccountAction {
    DEACTIVATE,
    DELETE
  }

  public enum ProofMethod {
    AUTHENTICATOR,
    PORTAL_PASSWORD,
    UNAVAILABLE
  }

  public static final class LastSuperadminException extends IllegalStateException {}

  private static final Duration REACTIVATION_REQUEST_INTERVAL = Duration.ofMinutes(5);

  private final UserRepository userRepository;
  private final EmailQueueRepository emailQueueRepository;
  private final EmailQueueService emailQueueService;
  private final ConfigService configService;
  private final CustomI18NProvider customI18NProvider;
  private final TwoFactorAuthorization twoFactorAuthorization;
  private final LoginAttemptService loginAttemptService;
  private final PasswordEncoder passwordEncoder;
  private final DeviceTokenRepository deviceTokenRepository;
  private final SessionRegistry sessionRegistry;
  private final JdbcTemplate jdbcTemplate;
  private final PlatformTransactionManager platformTransactionManager;
  private final AvatarDraftStore avatarDraftStore;

  public ProofMethod proofMethod(HttpServletRequest httpServletRequest) {
    Owner accountOwner = twoFactorAuthorization.owner(httpServletRequest);
    if (twoFactorAuthorization.state(accountOwner).enrolled()) {
      return ProofMethod.AUTHENTICATOR;
    }
    return accountOwner.hasPassword() ? ProofMethod.PORTAL_PASSWORD : ProofMethod.UNAVAILABLE;
  }

  public void changeOwnAccount(
      HttpServletRequest httpServletRequest, AccountAction accountAction, String enteredProof) {
    Objects.requireNonNull(accountAction, "Account action required");
    MaintenanceService.requirePublicAccess(configService);
    HttpSession accountSession = httpServletRequest.getSession(false);
    if (accountSession == null) {
      throw new AccessDeniedException("Session required");
    }
    Owner accountOwner = twoFactorAuthorization.owner(httpServletRequest);
    UUID provenFactorRevision = verifyAccountProof(accountOwner, enteredProof);
    String currentSessionId = accountSession.getId();

    newAccountTransaction()
        .executeWithoutResult(transactionStatus -> {
          userRepository.lockRole(RoleType.ROLE_SUPERADMIN);
          UserEntity lockedAccountUser = lockProvenAccount(accountOwner, provenFactorRevision);
          List<Long> enabledSuperadminIds =
              userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
          if (enabledSuperadminIds.size() == 1
              && enabledSuperadminIds.contains(accountOwner.id())) {
            throw new LastSuperadminException();
          }

          deviceTokenRepository.removeUserTokens(lockedAccountUser.getEmail());
          if (accountAction == AccountAction.DEACTIVATE) {
            deactivateAccount(lockedAccountUser);
          } else {
            deleteAccount(lockedAccountUser);
          }
        });

    try {
      avatarDraftStore.releaseForUser(accountOwner.id());
      removeOtherSessions(accountOwner.email(), currentSessionId);
    } catch (RuntimeException sessionCleanupFailure) {
      // Der neue Kontozustand sperrt alte Nachweise auch bei gestörter Sitzungsbereinigung.
      log.warn(
          "Account state committed; session cleanup interrupted",
          safeFailure(sessionCleanupFailure));
    }
  }

  private UserEntity lockProvenAccount(Owner accountOwner, UUID provenFactorRevision) {
    UserEntity lockedAccountUser =
        userRepository
            .findByIdForUpdate(accountOwner.id())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    State currentFactorState = twoFactorAuthorization.state(accountOwner);
    UUID currentFactorRevision =
        currentFactorState.enrolled() ? currentFactorState.revision() : null;
    if (!lockedAccountUser.isEnabled()
        || !lockedAccountUser.getAccessVersion().equals(accountOwner.accessVersion())
        || !Objects.equals(currentFactorRevision, provenFactorRevision)) {
      throw new AccessDeniedException("Account proof changed");
    }
    return lockedAccountUser;
  }

  private UUID verifyAccountProof(Owner accountOwner, String enteredProof) {
    if (twoFactorAuthorization.state(accountOwner).enrolled()) {
      return twoFactorAuthorization.verifyAccountLifecycleCode(accountOwner, enteredProof);
    }
    if (!accountOwner.hasPassword()) {
      throw new AccessDeniedException("Set up an authenticator or portal password first");
    }

    loginAttemptService.authenticate(
        accountOwner.email(),
        () -> {
          UserEntity accountUser =
              userRepository
                  .findById(accountOwner.id())
                  .orElseThrow(() -> new BadCredentialsException("Account unavailable"));
          if (enteredProof == null
              || accountUser.getPassword() == null
              || !passwordEncoder.matches(enteredProof, accountUser.getPassword())) {
            throw new BadCredentialsException("Current portal password invalid");
          }
          return UsernamePasswordAuthenticationToken.authenticated(
              accountOwner.email(), null, List.of());
        });
    return null;
  }

  private void deactivateAccount(UserEntity lockedAccountUser) {
    Instant deactivationTime = Instant.now();
    int retentionDays = configService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS);
    int reminderDays = configService.getInteger(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS);
    if (retentionDays < 1 || reminderDays < 0) {
      throw new IllegalStateException("Invalid account deactivation limits");
    }

    Instant deletionAt = deactivationTime.plus(retentionDays, ChronoUnit.DAYS);
    int effectiveReminderDays = Math.min(reminderDays, retentionDays - 1);
    Instant reminderAt =
        effectiveReminderDays == 0
            ? null
            : deletionAt.minus(effectiveReminderDays, ChronoUnit.DAYS);
    String reactivationToken = UUID.randomUUID().toString();
    lockedAccountUser.setEnabled(false);
    lockedAccountUser.rotateAccessVersion();
    lockedAccountUser.setAccountDeactivation(
        new AccountDeactivation(
            deletionAt, reminderAt, tokenDigest(reactivationToken), null, null, null));
    userRepository.saveAndFlush(lockedAccountUser);

    MessageProperties accountMessageProperties = createAccountMessageProperties(lockedAccountUser);
    queueReactivationNotice(
        lockedAccountUser,
        accountMessageProperties.getAccountDeactivatedMailSubject(),
        accountMessageProperties.getAccountDeactivatedMailBody(
            formatDeletionDate(lockedAccountUser, deletionAt)),
        reactivationToken);
    AfterCommitLog.info(log, "Account self-deactivated: userId={}", lockedAccountUser.getId());
  }

  public boolean reactivateAccount(String reactivationToken) {
    MaintenanceService.requirePublicAccess(configService);
    if (reactivationToken == null || !isCanonicalUuid(reactivationToken)) {
      return false;
    }

    return Boolean.TRUE.equals(
        newAccountTransaction()
            .execute(transactionStatus -> {
              UserEntity lockedAccountUser =
                  userRepository
                      .lockByAccountReactivationToken(tokenDigest(reactivationToken))
                      .orElse(null);
              if (lockedAccountUser == null
                  || !lockedAccountUser
                      .getAccountDeactivation()
                      .deletionAt()
                      .isAfter(Instant.now())) {
                return false;
              }

              lockedAccountUser.setEnabled(true);
              lockedAccountUser.rotateAccessVersion();
              emailQueueRepository.deleteByUserEntityAndEmailTypeAndStatus(
                  lockedAccountUser, EmailType.ACCOUNT_REACTIVATION, EmailStatus.PENDING);
              userRepository.saveAndFlush(lockedAccountUser);
              AfterCommitLog.info(log, "Account reactivated: userId={}", lockedAccountUser.getId());
              return true;
            }));
  }

  public void requestReactivationLink(String accountEmail) {
    MaintenanceService.requirePublicAccess(configService);
    newAccountTransaction()
        .executeWithoutResult(transactionStatus -> {
          UserEntity lockedAccountUser =
              userRepository.findByEmailForUpdate(accountEmail).orElse(null);
          Instant requestedAt = Instant.now();
          if (lockedAccountUser == null
              || lockedAccountUser.isEnabled()
              || lockedAccountUser.getAccountDeactivation() == null) {
            return;
          }

          AccountDeactivation accountDeactivation = lockedAccountUser.getAccountDeactivation();
          if (!accountDeactivation.deletionAt().isAfter(requestedAt)
              || accountDeactivation.reactivationRequestedAt() != null
                  && accountDeactivation
                      .reactivationRequestedAt()
                      .plus(REACTIVATION_REQUEST_INTERVAL)
                      .isAfter(requestedAt)
              || emailQueueService.hasOpenEmailForUserAndType(
                  lockedAccountUser, EmailType.ACCOUNT_REACTIVATION)) {
            return;
          }

          String reactivationToken = UUID.randomUUID().toString();
          lockedAccountUser.setAccountDeactivation(
              accountDeactivation.withRequestedLink(tokenDigest(reactivationToken), requestedAt));
          MessageProperties accountMessageProperties =
              createAccountMessageProperties(lockedAccountUser);
          queueReactivationNotice(
              lockedAccountUser,
              accountMessageProperties.getAccountDeactivatedMailSubject(),
              accountMessageProperties.getAccountDeactivatedMailBody(
                  formatDeletionDate(lockedAccountUser, accountDeactivation.deletionAt())),
              reactivationToken);
        });
  }

  @Scheduled(fixedDelay = 60_000)
  public void processDueDeactivations() {
    Instant processingTime = Instant.now();
    for (Long accountUserId :
        userRepository.findAccountLifecycleDueIds(processingTime, PageRequest.ofSize(50))) {
      try {
        newAccountTransaction()
            .executeWithoutResult(
                transactionStatus -> processLockedDeactivation(accountUserId, processingTime));
      } catch (RuntimeException processingFailure) {
        log.error(
            "Account deactivation processing failed: userId={}",
            accountUserId,
            safeFailure(processingFailure));
      }
    }
  }

  private void processLockedDeactivation(Long accountUserId, Instant processingTime) {
    UserEntity lockedAccountUser = userRepository.findByIdForUpdate(accountUserId).orElse(null);
    if (lockedAccountUser == null
        || lockedAccountUser.isEnabled()
        || lockedAccountUser.getAccountDeactivation() == null) {
      return;
    }

    AccountDeactivation accountDeactivation = lockedAccountUser.getAccountDeactivation();
    if (!accountDeactivation.deletionAt().isAfter(processingTime)) {
      deviceTokenRepository.removeUserTokens(lockedAccountUser.getEmail());
      removeOtherSessions(lockedAccountUser.getEmail(), "");
      deleteAccount(lockedAccountUser);
    } else if (accountDeactivation.reminderAt() != null
        && !accountDeactivation.reminderAt().isAfter(processingTime)
        && !emailQueueService.hasOpenEmailForUserAndType(
            lockedAccountUser, EmailType.ACCOUNT_REACTIVATION)) {
      String reactivationToken = UUID.randomUUID().toString();
      lockedAccountUser.setAccountDeactivation(
          accountDeactivation.withQueuedReminder(tokenDigest(reactivationToken)));
      MessageProperties accountMessageProperties =
          createAccountMessageProperties(lockedAccountUser);
      queueReactivationNotice(
          lockedAccountUser,
          accountMessageProperties.getAccountDeletionReminderSubject(),
          accountMessageProperties.getAccountDeletionReminderBody(
              formatDeletionDate(lockedAccountUser, accountDeactivation.deletionAt())),
          reactivationToken);
    }
  }

  private void deleteAccount(UserEntity lockedAccountUser) {
    Long accountUserId = lockedAccountUser.getId();
    String recipientEmail = lockedAccountUser.getEmail();
    MessageProperties accountMessageProperties = createAccountMessageProperties(lockedAccountUser);

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
    jdbcTemplate.update(
        "DELETE FROM login_attempts WHERE identity_hash = ?",
        LoginAttemptService.identityKey(recipientEmail));
    userRepository.delete(lockedAccountUser);
    userRepository.flush();
    emailQueueService.addAccountDeletionConfirmation(
        recipientEmail,
        accountMessageProperties.getAccountDeletedMailSubject(),
        "<p>"
            + HtmlUtils.htmlEscape(accountMessageProperties.getAccountDeletedMailBody())
            + "</p>");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            avatarDraftStore.releaseForUser(accountUserId);
          }
        });
    AfterCommitLog.info(log, "Account permanently deleted: userId={}", accountUserId);
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

  private void queueReactivationNotice(
      UserEntity lockedAccountUser,
      String emailSubject,
      String emailBody,
      String reactivationToken) {
    String reactivationUrl =
        URI.create(configService.getString(ConfigEntry.BASE_URL))
            .resolve("/" + ACCOUNT_REACTIVATION_ROUTE + "?token=" + reactivationToken)
            .toString();
    String linkLabel =
        createAccountMessageProperties(lockedAccountUser).getAccountReactivateAction();
    String emailHtml =
        "<p>"
            + HtmlUtils.htmlEscape(emailBody)
            + "</p><p><a target=\"_blank\" rel=\"noopener noreferrer\" href=\""
            + HtmlUtils.htmlEscape(reactivationUrl)
            + "\">"
            + HtmlUtils.htmlEscape(linkLabel)
            + "</a></p>";
    emailQueueService.addEmailToQueue(
        lockedAccountUser, emailSubject, emailHtml, EmailType.ACCOUNT_REACTIVATION);
  }

  private MessageProperties createAccountMessageProperties(UserEntity accountUser) {
    return MessageProperties.forLocale(customI18NProvider, accountUser.getPreferredLocale());
  }

  private String formatDeletionDate(UserEntity accountUser, Instant deletionAt) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(accountUser.getPreferredLocale())
            .withZone(ZoneOffset.UTC)
            .format(deletionAt)
        + " UTC";
  }

  private void removeOtherSessions(String accountEmail, String currentSessionId) {
    sessionRegistry.getAllSessions(accountEmail, true).stream()
        .map(SessionInformation::getSessionId)
        .filter(sessionId -> !sessionId.equals(currentSessionId))
        .forEach(sessionRegistry::removeSessionInformation);
    // Auch bereits abgelaufene Datenbanksitzungen gehören zum Konto. Die laufende
    // UIDL-Antwort darf ihre eigene Sitzung erst beim anschließenden Logout verlieren.
    jdbcTemplate.update(
        "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ? AND SESSION_ID <> ?",
        accountEmail,
        currentSessionId);
  }

  private TransactionTemplate newAccountTransaction() {
    TransactionTemplate accountTransactionTemplate =
        new TransactionTemplate(platformTransactionManager);
    accountTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return accountTransactionTemplate;
  }

  private static String tokenDigest(String reactivationToken) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(reactivationToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException unavailableAlgorithm) {
      throw new IllegalStateException("SHA-256 unavailable", unavailableAlgorithm);
    }
  }

  private static boolean isCanonicalUuid(String reactivationToken) {
    try {
      return UUID.fromString(reactivationToken).toString().equals(reactivationToken);
    } catch (IllegalArgumentException invalidToken) {
      return false;
    }
  }
}
