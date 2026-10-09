package de.derpeterson.app.service;

import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import de.derpeterson.app.model.enums.AdminNotificationStatus;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class EmailQueueService {

    private static final Logger logger = LoggerFactory.getLogger(EmailQueueService.class);
    private static final List<EmailStatus> OPEN_STATUSES = List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS);
    private static final Duration RETRY_INTERVAL = Duration.ofMinutes(1);

    private final ConfigService configService;

    private final EmailQueueRepository emailQueueRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final AdminNotificationAttemptRepository adminAttempts;

    @Autowired
    public EmailQueueService(ConfigService configService, EmailQueueRepository emailQueueRepository,
            EmailService emailService, UserRepository userRepository, PlatformTransactionManager transactionManager,
            AdminNotificationAttemptRepository adminAttempts) {
        this(configService, emailQueueRepository, emailService, userRepository, transactionManager, adminAttempts, Clock.systemDefaultZone());
    }

    public EmailQueueService(ConfigService configService, EmailQueueRepository emailQueueRepository,
            EmailService emailService, UserRepository userRepository, PlatformTransactionManager transactionManager,
            AdminNotificationAttemptRepository adminAttempts, Clock clock) {
        this.configService = configService;
        this.emailQueueRepository = emailQueueRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.transactionManager = transactionManager;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.adminAttempts = Objects.requireNonNull(adminAttempts, "adminAttempts");
    }

    private void requireUserAndType(UserEntity user, EmailType type) {
        Objects.requireNonNull(user, "userEntity");
        Objects.requireNonNull(user.getId(), "userEntity.id");
        Objects.requireNonNull(type, "emailType");
    }

    public boolean hasOpenEmailForUserAndType(UserEntity userEntity, EmailType emailType) {
        requireUserAndType(userEntity, emailType);
        return emailQueueRepository.existsByUserEntityAndEmailTypeAndStatusIn(userEntity, emailType, OPEN_STATUSES);
    }

    @Transactional
    public void addEmailToQueue(UserEntity userEntity, String subject, String body, EmailType emailType) {
        requireUserAndType(userEntity, emailType);
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
        userEntity = userRepository.lockVerificationUser(userEntity.getId()).orElseThrow(
                () -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
        if (emailType == EmailType.VERIFICATION && (userEntity.isEnabled() || !userEntity.isVerificationPending())) {
            return;
        }
        if (hasOpenEmailForUserAndType(userEntity, emailType)) {
            logger.warn("⚠️ Email for {} with type {} is already queued or in progress. Duplicate request skipped.",
                    userEntity.getEmail(), emailType);
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
     * Claim and outcome commit independently, with no database transaction during SMTP.
     * IN_PROGRESS is durable before sending and is never automatically reclaimed:
     * a crash or an unknown send/commit outcome requires operator investigation.
     * SMTP and the outcome commit are not atomic; this is not an exactly-once guarantee.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        int capacity = configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY);
        if (capacity <= 0) throw new IllegalArgumentException("Email queue capacity must be positive.");
        int maxRetry = configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
        if (maxRetry < 0) throw new IllegalArgumentException("Email queue retry limit must not be negative.");
        LocalDateTime retryCutoff = LocalDateTime.now(clock).minus(RETRY_INTERVAL);
        List<EmailQueueEntity> pendingEmails = transaction.execute(status ->
                emailQueueRepository.findPendingEmails(retryCutoff, PageRequest.of(0, capacity)));
        for (EmailQueueEntity candidate : pendingEmails) {
            try {
                EmailQueueEntity claimed = transaction.execute(status -> {
                    var email = emailQueueRepository.lockById(candidate.getId()).orElse(null);
                    if (email == null || email.getStatus() != EmailStatus.PENDING) return null;
                    if (email.getLastRetryAt() != null && email.getLastRetryAt().isAfter(LocalDateTime.now(clock).minus(RETRY_INTERVAL))) return null;
                    // Initialize the lazy recipient while the claim transaction is open.
                    email.getUserEntity().getEmail();
                    email.setStatus(EmailStatus.IN_PROGRESS);
                    emailQueueRepository.save(email);
                    return email;
                });
                if (claimed != null) dispatchClaimed(transaction, claimed, maxRetry);
            } catch (RuntimeException e) {
                logger.error("❌ Queue entry {} could not be completed; no automatic release of IN_PROGRESS.", candidate.getId(), e);
            }
        }
    }

    private void dispatchClaimed(TransactionTemplate transaction, EmailQueueEntity claimed, int maxRetry) {
        long attempt = (long) claimed.getRetryCount() + 1;
        Exception sendFailure = null;
        try {
            emailService.sendEmail(claimed.getUserEntity(), claimed.getSubject(), claimed.getBody());
        } catch (MailException | MessagingException e) {
            // Transport errors may follow SMTP acceptance (for example a lost reply).
            // Do not confuse an exception with proof of non-delivery.
            if (!(e instanceof MessagingException || e instanceof MailAuthenticationException
                    || e instanceof MailPreparationException || e instanceof MailParseException)) {
                logger.error("❌ Transport outcome for queue entry {} is unknown; IN_PROGRESS retained.", claimed.getId(), e);
                return;
            }
            sendFailure = e;
        }
        final Exception failure = sendFailure;
        EmailQueueEntity completed = transaction.execute(status -> {
            var email = emailQueueRepository.lockById(claimed.getId()).orElseThrow();
            if (email.getStatus() != EmailStatus.IN_PROGRESS || email.getRetryCount() != claimed.getRetryCount()) {
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
            logger.info("✅ Email successfully sent to {} on attempt {}.", completed.getUserEntity().getEmail(), attempt);
        } else if (completed.getStatus() == EmailStatus.PENDING) {
            logger.warn("⚠️ Error sending the email to {}, attempt {} of {}: {}", completed.getUserEntity().getEmail(),
                    attempt, (long) maxRetry + 1, failure.getMessage());
        } else {
            logger.error("❌ Email to {} failed after {} attempts: {}", completed.getUserEntity().getEmail(), attempt, failure.getMessage());
            sendAdminNotification(transaction, completed, failure, attempt);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteSentEmails() {
        Duration retention = Duration.parse(Objects.requireNonNull(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION), "retention"));
        if (retention.isZero() || retention.isNegative()) throw new IllegalArgumentException("Email queue retention must be positive.");
        LocalDateTime liveDateTime = LocalDateTime.now(clock).minus(retention);
        int deleted = emailQueueRepository.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        logger.info("✅ {} emails with status SENT created before {} have been deleted.", deleted, formattedLiveDateTime);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteFailedEmails() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(30);
        int deleted = emailQueueRepository.deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, cutoff);
        logger.info("✅ {} FAILED emails with last attempt before {} have been deleted.", deleted,
                cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private void sendAdminNotification(TransactionTemplate transaction, EmailQueueEntity failedEmail, Exception e, long attempt) {
        // A failed/ambiguous start commit must prevent external dispatch.
        Long auditId = transaction.execute(status -> {
            var audit = new AdminNotificationAttemptEntity();
            audit.setQueueId(failedEmail.getId());
            audit.setStartedAt(LocalDateTime.now(clock));
            return adminAttempts.saveAndFlush(audit).getId();
        });
        AdminNotificationStatus outcome = AdminNotificationStatus.NOT_SENT;
        boolean dispatchStarted = false;
        try {
            String adminEmail = configService.getString(ConfigEntry.EMAIL_ADMIN);
            String subject = "Email dispatch failed";
            String body = "The email to %s with the subject '%s' could not be delivered after %d attempts.\n\nError message: %s\nEmail type: %s\nSending time: %s" //NOSONAR
                    .formatted(failedEmail.getUserEntity().getEmail(), failedEmail.getSubject(), attempt, e.getMessage(), failedEmail.getEmailType(), failedEmail.getLastRetryAt());
            dispatchStarted = true;
            emailService.sendAdminEmail(adminEmail, subject, body);
            outcome = AdminNotificationStatus.SENT;
            logger.info("✅ Notification sent to administrator: {}", adminEmail);
        } catch (RuntimeException ex) {
            if (dispatchStarted && !(ex instanceof MailAuthenticationException
                    || ex instanceof MailPreparationException || ex instanceof MailParseException)) {
                outcome = AdminNotificationStatus.UNKNOWN;
            }
            logger.error("❌ Admin notification attempt {} for queue entry {} ended with {}.", auditId, failedEmail.getId(), outcome, ex);
        }
        final AdminNotificationStatus finalOutcome = outcome;
        // Independent of the already committed FAILED; failure here leaves the durable start.
        transaction.executeWithoutResult(status -> {
            var audit = adminAttempts.lockById(auditId).orElseThrow();
            if (audit.getStatus() != AdminNotificationStatus.IN_PROGRESS) {
                throw new IllegalStateException("Admin notification attempt no longer matches the stored state.");
            }
            audit.setStatus(finalOutcome);
            audit.setCompletedAt(LocalDateTime.now(clock));
            adminAttempts.saveAndFlush(audit);
        });
    }
}
