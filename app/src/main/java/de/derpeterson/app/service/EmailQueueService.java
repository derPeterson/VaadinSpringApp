package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.config.SmtpRejectionException;
import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AdminNotificationStatus;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailQueueService {

  private static final Logger logger = LoggerFactory.getLogger(EmailQueueService.class);
  private static final List<EmailStatus> OPEN_STATUSES =
      List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS);

  private final ConfigService configService;

  private final EmailQueueRepository emailQueueRepository;
  private final EmailService emailService;
  private final UserRepository userRepository;
  private final PlatformTransactionManager transactionManager;
  private final Clock emailQueueClock;
  private final AdminNotificationAttemptRepository adminNotificationAttemptRepository;
  private final EmailQueueWorkerPool emailQueueWorkerPool;
  private final EmailQueueDispatchGuard emailQueueDispatchGuard;

  @Autowired
  public EmailQueueService(
      ConfigService configService,
      EmailQueueRepository emailQueueRepository,
      EmailService emailService,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager,
      AdminNotificationAttemptRepository adminNotificationAttemptRepository,
      EmailQueueWorkerPool emailQueueWorkerPool,
      EmailQueueDispatchGuard emailQueueDispatchGuard) {
    this(
        configService,
        emailQueueRepository,
        emailService,
        userRepository,
        transactionManager,
        adminNotificationAttemptRepository,
        Clock.systemDefaultZone(),
        emailQueueWorkerPool,
        emailQueueDispatchGuard);
  }

  public EmailQueueService(
      ConfigService configService,
      EmailQueueRepository emailQueueRepository,
      EmailService emailService,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager,
      AdminNotificationAttemptRepository adminNotificationAttemptRepository,
      Clock emailQueueClock,
      EmailQueueWorkerPool emailQueueWorkerPool) {
    this(
        configService,
        emailQueueRepository,
        emailService,
        userRepository,
        transactionManager,
        adminNotificationAttemptRepository,
        emailQueueClock,
        emailQueueWorkerPool,
        new EmailQueueDispatchGuard(null));
  }

  public EmailQueueService(
      ConfigService configService,
      EmailQueueRepository emailQueueRepository,
      EmailService emailService,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager,
      AdminNotificationAttemptRepository adminNotificationAttemptRepository,
      Clock emailQueueClock,
      EmailQueueWorkerPool emailQueueWorkerPool,
      EmailQueueDispatchGuard emailQueueDispatchGuard) {
    this.emailQueueDispatchGuard = Objects.requireNonNull(emailQueueDispatchGuard);
    this.emailQueueWorkerPool = Objects.requireNonNull(emailQueueWorkerPool, "workers");
    this.configService = configService;
    this.emailQueueRepository = emailQueueRepository;
    this.emailService = emailService;
    this.userRepository = userRepository;
    this.transactionManager = transactionManager;
    this.emailQueueClock = Objects.requireNonNull(emailQueueClock, "clock");
    this.adminNotificationAttemptRepository =
        Objects.requireNonNull(adminNotificationAttemptRepository, "adminAttempts");
  }

  private String deliveryRecipient(EmailQueueEntity email) {
    return email.getRecipient() == null ? email.getUserEntity().getEmail() : email.getRecipient();
  }

  private void requireUserAndType(UserEntity user, EmailType type) {
    Objects.requireNonNull(user, "userEntity");
    Objects.requireNonNull(user.getId(), "userEntity.id");
    Objects.requireNonNull(type, "emailType");
  }

  public boolean hasOpenEmailForUserAndType(UserEntity userEntity, EmailType emailType) {
    requireUserAndType(userEntity, emailType);
    return emailQueueRepository.existsOpenEmailForCurrentAddress(
        userEntity, emailType, OPEN_STATUSES);
  }

  @Transactional
  public void addEmailToQueue(
      UserEntity userEntity, String subject, String body, EmailType emailType) {
    requireUserAndType(userEntity, emailType);
    if (emailType == EmailType.ACCOUNT_DELETED) {
      throw new IllegalArgumentException("Deletion confirmations must not reference an account");
    }
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(body, "body");
    userEntity =
        userRepository
            .lockVerificationUser(userEntity.getId())
            .orElseThrow(() -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
    if (userEntity.isDevelopmentAccount()) {
      return;
    }
    if (emailType == EmailType.VERIFICATION && !userEntity.requiresEmailVerification()) {
      return;
    }
    if (hasOpenEmailForUserAndType(userEntity, emailType)) {
      logger.debug(
          "Duplicate queue request skipped: userId={}, type={}", userEntity.getId(), emailType);
      return;
    }

    EmailQueueEntity email = new EmailQueueEntity();
    email.setUserEntity(userEntity);
    if (emailType == EmailType.VERIFICATION || emailType == EmailType.PASSWORD_RESET) {
      email.setRecipient(userEntity.getEmail());
    }
    email.setSubject(subject);
    email.setBody(body);
    email.setEmailType(emailType);
    email.setStatus(EmailStatus.PENDING);
    email.setCreatedAt(LocalDateTime.now(emailQueueClock));
    emailQueueRepository.save(email);
    AfterCommitLog.debug(
        logger,
        "Email queued: queueId={}, userId={}, type={}",
        email.getId(),
        email.getUserEntity().getId(),
        email.getEmailType());
  }

  /**
   * Sicherheitsereignisse werden vollständig angelegt, nicht als beliebige NOTIFICATION
   * dedupliziert.
   */
  @Transactional
  public void addAddressedNotification(
      UserEntity user, String recipient, String subject, String body) {
    requireUserAndType(user, EmailType.NOTIFICATION);
    if (!UserInputRules.isEmailValid(recipient)) {
      throw new IllegalArgumentException("Invalid notification recipient");
    }
    UserEntity stored = userRepository.lockVerificationUser(user.getId()).orElseThrow();
    if (stored.isDevelopmentAccount()) {
      return;
    }
    EmailQueueEntity email = new EmailQueueEntity();
    email.setUserEntity(stored);
    email.setRecipient(EmailIdentity.canonicalize(recipient));
    email.setSubject(Objects.requireNonNull(subject));
    email.setBody(Objects.requireNonNull(body));
    email.setEmailType(EmailType.NOTIFICATION);
    email.setStatus(EmailStatus.PENDING);
    email.setCreatedAt(LocalDateTime.now(emailQueueClock));
    emailQueueRepository.save(email);
    AfterCommitLog.debug(
        logger,
        "Email queued: queueId={}, userId={}, type={}",
        email.getId(),
        email.getUserEntity().getId(),
        email.getEmailType());
  }

  @Transactional
  public void addAccountDeletionConfirmation(
      String recipientEmail, String confirmationSubject, String confirmationBody) {
    if (!UserInputRules.isEmailValid(recipientEmail)) {
      throw new IllegalArgumentException("Invalid confirmation recipient");
    }

    EmailQueueEntity confirmationQueueEntry = new EmailQueueEntity();
    confirmationQueueEntry.setRecipient(EmailIdentity.canonicalize(recipientEmail));
    confirmationQueueEntry.setSubject(Objects.requireNonNull(confirmationSubject));
    confirmationQueueEntry.setBody(Objects.requireNonNull(confirmationBody));
    confirmationQueueEntry.setEmailType(EmailType.ACCOUNT_DELETED);
    confirmationQueueEntry.setCreatedAt(LocalDateTime.now(emailQueueClock));
    emailQueueRepository.save(confirmationQueueEntry);
  }

  /**
   * Claim and outcome commit independently, with no database transaction during SMTP. IN_PROGRESS
   * is durable before sending and is never automatically reclaimed: a crash or an unknown
   * send/commit outcome requires operator investigation. SMTP and the outcome commit are not
   * atomic; this is not an exactly-once guarantee.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Scheduled(fixedDelay = 5000)
  public void processQueue() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int capacity = configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY);
    if (capacity <= 0) {
      throw new IllegalArgumentException("Email queue capacity must be positive.");
    }
    int maxRetry = configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
    if (maxRetry < 0) {
      throw new IllegalArgumentException("Email queue retry limit must not be negative.");
    }
    int retryDelaySeconds = configService.getInteger(ConfigEntry.EMAIL_QUEUE_RETRY_DELAY_SECONDS);
    if (retryDelaySeconds < 1) {
      throw new IllegalArgumentException("Email queue retry delay must be positive.");
    }
    Duration retryDelay = Duration.ofSeconds(retryDelaySeconds);
    LocalDateTime retryCutoff = LocalDateTime.now(emailQueueClock).minus(retryDelay);
    List<Long> pendingIds =
        transaction.execute(status ->
            emailQueueRepository
                .findPendingEmails(retryCutoff, PageRequest.of(0, capacity))
                .stream()
                .map(EmailQueueEntity::getId)
                .toList());
    emailQueueWorkerPool.dispatchEmailBatch(
        pendingIds,
        queueEntryId ->
            emailQueueDispatchGuard.runDispatch(
                () -> processCandidate(queueEntryId, maxRetry, retryDelay)));
  }

  private void processCandidate(long queueEntryId, int maxRetry, Duration retryDelay) {
    TransactionTemplate dispatchTransactionTemplate = new TransactionTemplate(transactionManager);
    dispatchTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    try {
      EmailQueueEntity claimedEmailEntity =
          dispatchTransactionTemplate.execute(status -> {
            EmailQueueEntity pendingEmailEntity =
                emailQueueRepository.lockById(queueEntryId).orElse(null);
            if (pendingEmailEntity == null
                || pendingEmailEntity.getStatus() != EmailStatus.PENDING) {
              return null;
            }

            if (pendingEmailEntity.getLastRetryAt() != null
                && pendingEmailEntity
                    .getLastRetryAt()
                    .isAfter(LocalDateTime.now(emailQueueClock).minus(retryDelay))) {
              return null;
            }
            // Empfänger im frischen Worker-Claim initialisieren, SMTP außerhalb der
            // Transaktion.
            if (pendingEmailEntity.getRecipient() == null) {
              pendingEmailEntity.getUserEntity().getEmail();
            }
            pendingEmailEntity.setStatus(EmailStatus.IN_PROGRESS);
            pendingEmailEntity.setProcessingStartedAt(LocalDateTime.now(emailQueueClock));
            emailQueueRepository.save(pendingEmailEntity);
            return pendingEmailEntity;
          });

      if (claimedEmailEntity != null) {
        dispatchClaimed(dispatchTransactionTemplate, claimedEmailEntity, maxRetry);
      }
    } catch (RuntimeException dispatchFailure) {
      logger.error(
          "Queue entry {} could not be completed; no automatic release of IN_PROGRESS.",
          queueEntryId,
          safeFailure(dispatchFailure));
    }
  }

  private void dispatchClaimed(
      TransactionTemplate dispatchTransactionTemplate,
      EmailQueueEntity claimedEmailEntity,
      int maxRetry) {
    long sendAttemptNumber = (long) claimedEmailEntity.getRetryCount() + 1;
    Exception sendFailure = null;
    try {
      if (claimedEmailEntity.getRecipient() == null) {
        emailService.sendEmail(
            claimedEmailEntity.getUserEntity(),
            claimedEmailEntity.getSubject(),
            claimedEmailEntity.getBody());
      } else {
        emailService.sendEmailTo(
            claimedEmailEntity.getRecipient(),
            claimedEmailEntity.getSubject(),
            claimedEmailEntity.getBody());
      }
    } catch (MailException | MessagingException e) {
      // Transport errors may follow SMTP acceptance (for example a lost reply).
      // Do not confuse an exception with proof of non-delivery.
      if (!(e instanceof MessagingException
          || e instanceof MailAuthenticationException
          || e instanceof MailPreparationException
          || e instanceof MailParseException
          || e instanceof SmtpRejectionException)) {
        logger.error(
            "❌ Transport outcome for queue entry {} is unknown; IN_PROGRESS retained.",
            claimedEmailEntity.getId(),
            safeFailure(e));
        return;
      }
      sendFailure = e;
    }
    final Exception failure = sendFailure;
    EmailQueueEntity completed =
        dispatchTransactionTemplate.execute(
            status -> completeClaimed(claimedEmailEntity, failure, maxRetry));
    if (failure == null) {
      logger.info(
          "Email accepted by transport: queueId={}, type={}, attempt={}",
          completed.getId(),
          completed.getEmailType(),
          sendAttemptNumber);
    } else if (completed.getStatus() == EmailStatus.PENDING) {
      logger.warn(
          "Email retry scheduled: queueId={}, attempt={} of {}, failureType={}",
          completed.getId(),
          sendAttemptNumber,
          (long) maxRetry + 1,
          failure.getClass().getName());
    } else {
      logger.error(
          "Email permanently failed: queueId={}, attempts={}",
          completed.getId(),
          sendAttemptNumber,
          safeFailure(failure));
      if (completed.getEmailType() != EmailType.ACCOUNT_DELETED) {
        sendAdminNotification(dispatchTransactionTemplate, completed, failure, sendAttemptNumber);
      }
    }
  }

  // Prüfung und Statuswechsel bleiben in derselben unabhängigen Abschlusstransaktion.
  private EmailQueueEntity completeClaimed(
      EmailQueueEntity claimed, Exception failure, int maxRetry) {
    EmailQueueEntity email = emailQueueRepository.lockById(claimed.getId()).orElseThrow();
    if (email.getStatus() != EmailStatus.IN_PROGRESS
        || email.getRetryCount() != claimed.getRetryCount()) {
      throw new IllegalStateException("Queue claim no longer matches the stored state.");
    }
    if (failure == null) {
      email.setStatus(EmailStatus.SENT);
    } else {
      email.setLastRetryAt(LocalDateTime.now(emailQueueClock));
      boolean permanentRejection =
          failure instanceof SmtpRejectionException rejection && !rejection.isTemporaryRejection();
      if (!permanentRejection && email.getRetryCount() < maxRetry) {
        email.setRetryCount(email.getRetryCount() + 1);
        email.setStatus(EmailStatus.PENDING);
      } else {
        email.setStatus(EmailStatus.FAILED);
      }
    }
    if (email.getEmailType() == EmailType.ACCOUNT_DELETED
        && email.getStatus() != EmailStatus.PENDING) {
      emailQueueRepository.delete(email);
    } else {
      emailQueueRepository.save(email);
      if (email.getRecipient() == null) {
        email.getUserEntity().getEmail();
      }
    }
    return email;
  }

  @Transactional
  @Scheduled(cron = "0 0 3 ? * SUN")
  public void deleteSentEmails() {
    Duration retention =
        Duration.parse(
            Objects.requireNonNull(
                configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION), "retention"));
    if (retention.isZero() || retention.isNegative()) {
      throw new IllegalArgumentException("Email queue retention must be positive.");
    }
    LocalDateTime liveDateTime = LocalDateTime.now(emailQueueClock).minus(retention);
    int deleted =
        emailQueueRepository.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, liveDateTime);
    String formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    if (deleted > 0) {
      AfterCommitLog.info(
          logger, "Sent emails deleted: count={}, cutoff={}", deleted, formattedLiveDateTime);
    }
  }

  @Transactional
  @Scheduled(cron = "0 0 3 ? * SUN")
  public void deleteResolvedUnknownEmails() {
    LocalDateTime retentionCutoff = LocalDateTime.now(emailQueueClock).minusDays(30);
    int deletedEmailCount =
        emailQueueRepository.deleteByStatusAndCreatedAtBefore(EmailStatus.UNKNOWN, retentionCutoff);
    if (deletedEmailCount > 0) {
      AfterCommitLog.info(
          logger,
          "Administratively resolved mail entries deleted: count={}, cutoff={}",
          deletedEmailCount,
          retentionCutoff.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
  }

  @Transactional
  @Scheduled(cron = "0 0 3 ? * SUN")
  public void deleteFailedEmails() {
    LocalDateTime cutoff = LocalDateTime.now(emailQueueClock).minusDays(30);
    int deleted =
        emailQueueRepository.deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, cutoff);
    if (deleted > 0) {
      AfterCommitLog.info(
          logger,
          "Failed emails deleted: count={}, cutoff={}",
          deleted,
          cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
  }

  private void sendAdminNotification(
      TransactionTemplate transaction, EmailQueueEntity failedEmail, Exception e, long attempt) {
    // A failed/ambiguous start commit must prevent external dispatch.
    Long auditId =
        transaction.execute(status -> {
          AdminNotificationAttemptEntity audit = new AdminNotificationAttemptEntity();
          audit.setQueueId(failedEmail.getId());
          audit.setStartedAt(LocalDateTime.now(emailQueueClock));
          return adminNotificationAttemptRepository.saveAndFlush(audit).getId();
        });
    AdminNotificationStatus outcome = AdminNotificationStatus.NOT_SENT;
    boolean dispatchStarted = false;
    try {
      String adminEmail = configService.getString(ConfigEntry.EMAIL_ADMIN);
      String subject = "Email dispatch failed";
      String body =
          "The email to %s with the subject '%s' could not be delivered after %d attempts.\n\nError message: %s\nEmail type: %s\nSending time: %s" // NOSONAR
              .formatted(
                  deliveryRecipient(failedEmail),
                  failedEmail.getSubject(),
                  attempt,
                  e.getMessage(),
                  failedEmail.getEmailType(),
                  failedEmail.getLastRetryAt());
      dispatchStarted = true;
      emailService.sendAdminEmail(adminEmail, subject, body);
      outcome = AdminNotificationStatus.SENT;
      logger.info(
          "Admin failure alert accepted by transport: queueId={}, attemptId={}",
          failedEmail.getId(),
          auditId);
    } catch (RuntimeException ex) {
      if (dispatchStarted
          && !(ex instanceof MailAuthenticationException
              || ex instanceof MailPreparationException
              || ex instanceof MailParseException)) {
        outcome = AdminNotificationStatus.UNKNOWN;
      }
      logger.error(
          "❌ Admin notification attempt {} for queue entry {} ended with {}.",
          auditId,
          failedEmail.getId(),
          outcome,
          safeFailure(ex));
    }
    final AdminNotificationStatus finalOutcome = outcome;
    // Independent of the already committed FAILED; failure here leaves the durable start.
    transaction.executeWithoutResult(status -> {
      AdminNotificationAttemptEntity audit =
          adminNotificationAttemptRepository.lockById(auditId).orElseThrow();
      if (audit.getStatus() != AdminNotificationStatus.IN_PROGRESS) {
        throw new IllegalStateException(
            "Admin notification attempt no longer matches the stored state.");
      }
      audit.setStatus(finalOutcome);
      audit.setCompletedAt(LocalDateTime.now(emailQueueClock));
      adminNotificationAttemptRepository.saveAndFlush(audit);
    });
  }
}
