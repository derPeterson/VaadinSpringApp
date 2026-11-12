package de.derpeterson.app.service;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_REACTIVATION_ROUTE;
import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
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
import de.derpeterson.app.security.PendingAccountReactivationFlow;
import de.derpeterson.app.security.PortalUserDetails;
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
import java.util.Map;
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
import org.springframework.transaction.support.TransactionTemplate;

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
  private final AccountSecurityMail accountSecurityMail;
  private final TwoFactorAuthorization twoFactorAuthorization;
  private final LoginAttemptService loginAttemptService;
  private final PasswordEncoder passwordEncoder;
  private final DeviceTokenRepository deviceTokenRepository;
  private final SessionRegistry sessionRegistry;
  private final JdbcTemplate jdbcTemplate;
  private final PlatformTransactionManager platformTransactionManager;
  private final AvatarDraftStore avatarDraftStore;
  private final AccountDeletionService accountDeletionService;

  public ProofMethod proofMethod(HttpServletRequest httpServletRequest) {
    Owner accountOwner = twoFactorAuthorization.owner(httpServletRequest);
    if (twoFactorAuthorization.state(accountOwner).enrolled()) {
      return ProofMethod.AUTHENTICATOR;
    }
    return accountOwner.hasPassword() ? ProofMethod.PORTAL_PASSWORD : ProofMethod.UNAVAILABLE;
  }

  public boolean changeOwnAccount(
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

    boolean mailQueued =
        Boolean.TRUE.equals(
            newAccountTransaction()
                .execute(transactionStatus -> {
                  userRepository.lockRole(RoleType.ROLE_SUPERADMIN);
                  UserEntity lockedAccountUser =
                      lockProvenAccount(accountOwner, provenFactorRevision);
                  List<Long> enabledSuperadminIds =
                      userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
                  if (enabledSuperadminIds.size() == 1
                      && enabledSuperadminIds.contains(accountOwner.id())) {
                    throw new LastSuperadminException();
                  }

                  if (accountAction == AccountAction.DEACTIVATE) {
                    deviceTokenRepository.removeUserTokens(lockedAccountUser.getEmail());
                    deactivateAccount(lockedAccountUser);
                  } else {
                    deleteAccount(lockedAccountUser, currentSessionId);
                  }
                  return !lockedAccountUser.isDevelopmentAccount();
                }));

    try {
      avatarDraftStore.releaseForUser(accountOwner.id());
      removeOtherSessions(accountOwner.email(), currentSessionId);
    } catch (RuntimeException sessionCleanupFailure) {
      // Der neue Kontozustand sperrt alte Nachweise auch bei gestörter Sitzungsbereinigung.
      log.warn(
          "Account state committed; session cleanup interrupted",
          safeFailure(sessionCleanupFailure));
    }
    return mailQueued;
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
            deletionAt, reminderAt, tokenDigest(reactivationToken), null, null, deactivationTime));
    userRepository.saveAndFlush(lockedAccountUser);

    MessageProperties accountMessageProperties = createAccountMessageProperties(lockedAccountUser);
    queueReactivationNotice(
        lockedAccountUser,
        "account_deactivated",
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
              queueReactivatedNotice(lockedAccountUser);
              AfterCommitLog.info(log, "Account reactivated: userId={}", lockedAccountUser.getId());
              return true;
            }));
  }

  public boolean requestReactivationLink(Long accountUserId, String credentialVersion) {
    MaintenanceService.requirePublicAccess(configService);
    return Boolean.TRUE.equals(
        newAccountTransaction()
            .execute(transactionStatus -> {
              UserEntity lockedAccountUser =
                  userRepository.findByIdForUpdate(accountUserId).orElse(null);
              Instant requestedAt = Instant.now();
              if (lockedAccountUser == null
                  || !PendingAccountReactivationFlow.canReactivate(lockedAccountUser)
                  || !Objects.equals(
                      credentialVersion,
                      PortalUserDetails.credentialVersion(
                          lockedAccountUser.getAccessVersion(), lockedAccountUser.getPassword()))) {
                throw new AccessDeniedException("Reactivation proof changed");
              }
              if (lockedAccountUser.isDevelopmentAccount()) {
                return false;
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
                return false;
              }

              String reactivationToken = UUID.randomUUID().toString();
              lockedAccountUser.setAccountDeactivation(
                  accountDeactivation.withRequestedLink(
                      tokenDigest(reactivationToken), requestedAt));
              MessageProperties accountMessageProperties =
                  createAccountMessageProperties(lockedAccountUser);
              queueReactivationNotice(
                  lockedAccountUser,
                  "account_deactivated",
                  accountMessageProperties.getAccountDeactivatedMailSubject(),
                  accountMessageProperties.getAccountDeactivatedMailBody(
                      formatDeletionDate(lockedAccountUser, accountDeactivation.deletionAt())),
                  reactivationToken);
              return true;
            }));
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
      deleteAccount(lockedAccountUser, "");
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
          "account_deletion_reminder",
          accountMessageProperties.getAccountDeletionReminderSubject(),
          accountMessageProperties.getAccountDeletionReminderBody(
              formatDeletionDate(lockedAccountUser, accountDeactivation.deletionAt())),
          reactivationToken);
    }
  }

  private void deleteAccount(UserEntity lockedAccountUser, String preservedSessionId) {
    Long accountUserId = lockedAccountUser.getId();
    String recipientEmail = lockedAccountUser.getEmail();
    MessageProperties accountMessageProperties = createAccountMessageProperties(lockedAccountUser);

    accountDeletionService.prepareAccountDeletion(accountUserId, preservedSessionId);
    userRepository.delete(lockedAccountUser);
    userRepository.flush();
    if (!lockedAccountUser.isDevelopmentAccount()) {
      emailQueueService.addAccountDeletionConfirmation(
          recipientEmail,
          accountMessageProperties.getAccountDeletedMailSubject(),
          accountSecurityMail.render(
              "account_deleted",
              lockedAccountUser,
              Map.of(
                  "TITLE",
                  accountMessageProperties.getAccountDeletedMailSubject(),
                  "MESSAGE",
                  accountMessageProperties.getAccountDeletedMailBody())));
    }
    AfterCommitLog.info(log, "Account permanently deleted: userId={}", accountUserId);
  }

  private void queueReactivationNotice(
      UserEntity lockedAccountUser,
      String templateName,
      String emailSubject,
      String emailBody,
      String reactivationToken) {
    if (lockedAccountUser.isDevelopmentAccount()) {
      return;
    }
    String reactivationUrl =
        URI.create(configService.getString(ConfigEntry.BASE_URL))
            .resolve("/" + ACCOUNT_REACTIVATION_ROUTE + "?token=" + reactivationToken)
            .toString();
    String emailHtml =
        accountSecurityMail.render(
            templateName,
            lockedAccountUser,
            Map.of(
                "TITLE", emailSubject, "MESSAGE", emailBody, "REACTIVATION_LINK", reactivationUrl));
    emailQueueService.addEmailToQueue(
        lockedAccountUser, emailSubject, emailHtml, EmailType.ACCOUNT_REACTIVATION);
  }

  private void queueReactivatedNotice(UserEntity accountUser) {
    if (accountUser.isDevelopmentAccount()) {
      return;
    }
    MessageProperties accountMessageProperties = createAccountMessageProperties(accountUser);
    String subject = accountMessageProperties.getAccountReactivatedMailSubject();
    String mailHtml =
        accountSecurityMail.render(
            "account_reactivated",
            accountUser,
            Map.of(
                "TITLE",
                subject,
                "MESSAGE",
                accountMessageProperties.getAccountReactivatedMailBody()));
    emailQueueService.addEmailToQueue(accountUser, subject, mailHtml, EmailType.NOTIFICATION);
  }

  private MessageProperties createAccountMessageProperties(UserEntity accountUser) {
    return accountSecurityMail.messagePropertiesForRecipient(accountUser);
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
