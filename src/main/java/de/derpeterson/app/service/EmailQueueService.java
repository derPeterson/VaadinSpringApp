package de.derpeterson.app.service;

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
  private static final Duration RETRY_INTERVAL = Duration.ofMinutes(1);

  private final ConfigService configService;

  private final EmailQueueRepository emailQueueRepository;
  private final EmailService emailService;
  private final UserRepository userRepository;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;
  private final AdminNotificationAttemptRepository adminAttempts;
  private final EmailQueueWorkerPool workers;

  @Autowired
  public EmailQueueService(
      ConfigService configService,
      EmailQueueRepository emailQueueRepository,
      EmailService emailService,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager,
      AdminNotificationAttemptRepository adminAttempts,
      EmailQueueWorkerPool workers) {
    this(
        configService,
        emailQueueRepository,
        emailService,
        userRepository,
        transactionManager,
        adminAttempts,
        Clock.systemDefaultZone(),
        workers);
  }

  public EmailQueueService(
      ConfigService configService,
      EmailQueueRepository emailQueueRepository,
      EmailService emailService,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager,
      AdminNotificationAttemptRepository adminAttempts,
      Clock clock,
      EmailQueueWorkerPool workers) {
    this.workers = Objects.requireNonNull(workers, "workers");
    this.configService = configService;
    this.emailQueueRepository = emailQueueRepository;
    this.emailService = emailService;
    this.userRepository = userRepository;
    this.transactionManager = transactionManager;
    this.clock = Objects.requireNonNull(clock, "clock");
    this.adminAttempts = Objects.requireNonNull(adminAttempts, "adminAttempts");
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
    return emailQueueRepository.existsByUserEntityAndEmailTypeAndStatusIn(
        userEntity, emailType, OPEN_STATUSES);
  }

  @Transactional
  public void addEmailToQueue(
      UserEntity userEntity, String subject, String body, EmailType emailType) {
    requireUserAndType(userEntity, emailType);
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(body, "body");
    userEntity =
        userRepository
            .lockVerificationUser(userEntity.getId())
            .orElseThrow(() -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
    if (emailType == EmailType.VERIFICATION
        && (userEntity.isEnabled() || !userEntity.isVerificationPending())) {
      return;
    }
    if (hasOpenEmailForUserAndType(userEntity, emailType)) {
      logger.warn(
          "⚠️ Email for {} with type {} is already queued or in progress. Duplicate request skipped.",
          userEntity.getEmail(),
          emailType);
      return;
    }

    EmailQueueEntity email = new EmailQueueEntity();
    email.setUserEntity(userEntity);
    email.setSubject(subject);
    email.setBody(body);
    email.setEmailType(emailType);
    email.setStatus(EmailStatus.PENDING);
    email.setCreatedAt(LocalDateTime.now(clock));
    emailQueueRepository.save(email);
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
    EmailQueueEntity email = new EmailQueueEntity();
    email.setUserEntity(stored);
    email.setRecipient(EmailIdentity.canonicalize(recipient));
    email.setSubject(Objects.requireNonNull(subject));
    email.setBody(Objects.requireNonNull(body));
    email.setEmailType(EmailType.NOTIFICATION);
    email.setStatus(EmailStatus.PENDING);
    email.setCreatedAt(LocalDateTime.now(clock));
    emailQueueRepository.save(email);
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
    LocalDateTime retryCutoff = LocalDateTime.now(clock).minus(RETRY_INTERVAL);
    List<Long> pendingIds =
        transaction.execute(
            status ->
                emailQueueRepository
                    .findPendingEmails(retryCutoff, PageRequest.of(0, capacity))
                    .stream()
                    .map(EmailQueueEntity::getId)
                    .toList());
    workers.process(pendingIds, id -> processCandidate(id, maxRetry));
  }

  private void processCandidate(long id, int maxRetry) {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      EmailQueueEntity claimed =
          transaction.execute(
              status -> {
                EmailQueueEntity email = emailQueueRepository.lockById(id).orElse(null);
                if (email == null || email.getStatus() != EmailStatus.PENDING) {
                  return null;
                }
                if (email.getLastRetryAt() != null
                    && email
                        .getLastRetryAt()
                        .isAfter(LocalDateTime.now(clock).minus(RETRY_INTERVAL))) {
                  return null;
                }
                // Empfänger im frischen Worker-Claim initialisieren, SMTP außerhalb der
                // Transaktion.
                email.getUserEntity().getEmail();
                email.setStatus(EmailStatus.IN_PROGRESS);
                emailQueueRepository.save(email);
                return email;
              });
      if (claimed != null) {
        dispatchClaimed(transaction, claimed, maxRetry);
      }
    } catch (RuntimeException failure) {
      logger.error(
          "Queue entry {} could not be completed; no automatic release of IN_PROGRESS.",
          id,
          failure);
    }
  }

  private void dispatchClaimed(
      TransactionTemplate transaction, EmailQueueEntity claimed, int maxRetry) {
    long attempt = (long) claimed.getRetryCount() + 1;
    Exception sendFailure = null;
    try {
      if (claimed.getRecipient() == null) {
        emailService.sendEmail(claimed.getUserEntity(), claimed.getSubject(), claimed.getBody());
      } else {
        emailService.sendEmailTo(claimed.getRecipient(), claimed.getSubject(), claimed.getBody());
      }
    } catch (MailException | MessagingException e) {
      // Transport errors may follow SMTP acceptance (for example a lost reply).
      // Do not confuse an exception with proof of non-delivery.
      if (!(e instanceof MessagingException
          || e instanceof MailAuthenticationException
          || e instanceof MailPreparationException
          || e instanceof MailParseException)) {
        logger.error(
            "❌ Transport outcome for queue entry {} is unknown; IN_PROGRESS retained.",
            claimed.getId(),
            e);
        return;
      }
      sendFailure = e;
    }
    final Exception failure = sendFailure;
    EmailQueueEntity completed =
        transaction.execute(
            status -> {
              EmailQueueEntity email = emailQueueRepository.lockById(claimed.getId()).orElseThrow();
              if (email.getStatus() != EmailStatus.IN_PROGRESS
                  || email.getRetryCount() != claimed.getRetryCount()) {
                throw new IllegalStateException("Queue claim no longer matches the stored state.");
              }
              if (failure == null) {
                email.setStatus(EmailStatus.SENT);
              } else {
                email.setLastRetryAt(LocalDateTime.now(clock));
                if (email.getRetryCount() < maxRetry) {
                  email.setRetryCount(email.getRetryCount() + 1);
                  email.setStatus(EmailStatus.PENDING);
                } else {
                  email.setStatus(EmailStatus.FAILED);
                }
              }
              emailQueueRepository.save(email);
              email.getUserEntity().getEmail();
              return email;
            });
    if (failure == null) {
      logger.info(
          "✅ Email successfully sent to {} on attempt {}.", deliveryRecipient(completed), attempt);
    } else if (completed.getStatus() == EmailStatus.PENDING) {
      logger.warn(
          "⚠️ Error sending the email to {}, attempt {} of {}: {}",
          deliveryRecipient(completed),
          attempt,
          (long) maxRetry + 1,
          failure.getMessage());
    } else {
      logger.error(
          "❌ Email to {} failed after {} attempts: {}",
          deliveryRecipient(completed),
          attempt,
          failure.getMessage());
      sendAdminNotification(transaction, completed, failure, attempt);
    }
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
    LocalDateTime liveDateTime = LocalDateTime.now(clock).minus(retention);
    int deleted =
        emailQueueRepository.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, liveDateTime);
    String formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    logger.info(
        "✅ {} emails with status SENT created before {} have been deleted.",
        deleted,
        formattedLiveDateTime);
  }

  @Transactional
  @Scheduled(cron = "0 0 3 ? * SUN")
  public void deleteFailedEmails() {
    LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(30);
    int deleted =
        emailQueueRepository.deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, cutoff);
    logger.info(
        "✅ {} FAILED emails with last attempt before {} have been deleted.",
        deleted,
        cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  private void sendAdminNotification(
      TransactionTemplate transaction, EmailQueueEntity failedEmail, Exception e, long attempt) {
    // A failed/ambiguous start commit must prevent external dispatch.
    Long auditId =
        transaction.execute(
            status -> {
              AdminNotificationAttemptEntity audit = new AdminNotificationAttemptEntity();
              audit.setQueueId(failedEmail.getId());
              audit.setStartedAt(LocalDateTime.now(clock));
              return adminAttempts.saveAndFlush(audit).getId();
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
      logger.info("✅ Notification sent to administrator: {}", adminEmail);
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
          ex);
    }
    final AdminNotificationStatus finalOutcome = outcome;
    // Independent of the already committed FAILED; failure here leaves the durable start.
    transaction.executeWithoutResult(
        status -> {
          AdminNotificationAttemptEntity audit = adminAttempts.lockById(auditId).orElseThrow();
          if (audit.getStatus() != AdminNotificationStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Admin notification attempt no longer matches the stored state.");
          }
          audit.setStatus(finalOutcome);
          audit.setCompletedAt(LocalDateTime.now(clock));
          adminAttempts.saveAndFlush(audit);
        });
  }
}
