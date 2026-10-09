package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final ConfigService configService;

    private final JavaMailSender mailSender;

    /**
     * Sends a single UTF-8 HTML part without attachments or inline resources.
     * User, email, subject and content must be non-null; empty subject/content are
     * preserved. The helper parses exactly one recipient/from address (including
     * display names); local addresses are accepted, no stricter validation is enabled.
     * A null user raises NullPointerException; other null arguments raise
     * IllegalArgumentException, malformed/empty/multiple addresses MessagingException.
     * Config, MIME construction and sender failures propagate; no retry or fallback.
     */
    public void sendEmail(UserEntity userEntity, String subject, String htmlContent) throws MailException, MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setTo(userEntity.getEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

        mailSender.send(message);
    }

    /**
     * Passes a plain-text SimpleMailMessage to the configured sender, whose default
     * encoding is UTF-8. No service-level validation, trimming or HTML interpretation:
     * null/empty recipient, subject, body and configured from are passed unchanged.
     * The real sender may reject such values during conversion/dispatch with a
     * MailException. Config/sender runtime failures propagate unchanged; no retry.
     */
    public void sendAdminEmail(String adminEmail, String subject, String body) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

        mailSender.send(message);
    }
}
