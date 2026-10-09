package de.derpeterson.app.service;

import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailQueueService {

    private static final Logger logger = LoggerFactory.getLogger(EmailQueueService.class);
    private static final List<EmailStatus> OPEN_STATUSES = List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS);

    private final ConfigService configService;

    private final EmailQueueRepository emailQueueRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public boolean hasOpenEmailForUserAndType(UserEntity userEntity, EmailType emailType) {
        return emailQueueRepository.existsByUserEntityAndEmailTypeAndStatusIn(userEntity, emailType, OPEN_STATUSES);
    }

    @Transactional
    public void addEmailToQueue(UserEntity userEntity, String subject, String body, EmailType emailType) {
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
        emailQueueRepository.save(email);
    }

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        List<EmailQueueEntity> pendingEmails = emailQueueRepository.findPendingEmails(PageRequest.of(0, configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)));  // Max. 50 E-Mails

        for (EmailQueueEntity email : pendingEmails) {
            if (email.getStatus() == EmailStatus.PENDING) {
                email.setStatus(EmailStatus.IN_PROGRESS);
                emailQueueRepository.save(email);

                long attempt = (long) email.getRetryCount() + 1;
                try {
                    emailService.sendEmail(email.getUserEntity(), email.getSubject(), email.getBody());
                    email.setStatus(EmailStatus.SENT);
                    emailQueueRepository.save(email);
                    logger.info("✅ Email successfully sent to {} on attempt {}.", email.getUserEntity().getEmail(), attempt);
                } catch (MailException | MessagingException e) {
                    int maxRetry = configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
                    if (email.getRetryCount() < maxRetry) {
                        email.setRetryCount(email.getRetryCount() + 1);
                        email.setLastRetryAt(LocalDateTime.now());
                        email.setStatus(EmailStatus.PENDING);
                        emailQueueRepository.save(email);
                        logger.warn("⚠️ Error sending the email to {}, attempt {} of {}: {}", email.getUserEntity().getEmail(),
                                attempt, (long) maxRetry + 1, e.getMessage());
                    } else {
                        email.setStatus(EmailStatus.FAILED);
                        email.setLastRetryAt(LocalDateTime.now());
                        emailQueueRepository.save(email);
                        logger.error("❌ Email to {} failed after {} attempts: {}", email.getUserEntity().getEmail(), attempt, e.getMessage());

                        sendAdminNotification(email, e, attempt);
                    }
                    logger.error("❌ Error sending the email: ", e);
                }
            }
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteSentEmails() {
        LocalDateTime liveDateTime = LocalDateTime.now().minus(Duration.parse(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)));
        int deleted = emailQueueRepository.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        logger.info("✅ {} emails with status SENT created before {} have been deleted.", deleted, formattedLiveDateTime);
    }

    private void sendAdminNotification(EmailQueueEntity failedEmail, Exception e, long attempt) {
        String adminEmail = configService.getString(ConfigEntry.EMAIL_ADMIN);
        String subject = "Email dispatch failed";

        String body = "The email to %s with the subject '%s' could not be delivered after %d attempts.\n\nError message: %s\nEmail type: %s\nSending time: %s" //NOSONAR
                .formatted(failedEmail.getUserEntity().getEmail(), failedEmail.getSubject(), attempt, e.getMessage(), failedEmail.getEmailType(), failedEmail.getLastRetryAt());
        try {
            emailService.sendAdminEmail(adminEmail, subject, body);
            logger.info("✅ Notification sent to administrator: {}", adminEmail);
        } catch (MailException ex) {
            logger.error("❌ Error sending the notification to the administrator: ", ex);
        }
    }
}
