package de.derpeterson.app.service;

import de.derpeterson.app.config.ConnectionAwareJavaMailSender;
import de.derpeterson.app.config.SmtpRejectionException;
import de.derpeterson.app.helper.email.EmailLogo;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final ConfigService configService;

  private final JavaMailSender mailSender;

  /**
   * Sends UTF-8 HTML; account templates with the known logo CID use multipart/related with an
   * inline PNG. Other HTML keeps the single-part format. Logo read failures are
   * MailPreparationException before invoking the transport. User, email, subject and content must
   * be non-null; empty subject/content are preserved. The helper parses exactly one recipient/from
   * address (including display names); local addresses are accepted, no stricter validation is
   * enabled. A null user raises NullPointerException; other null arguments raise
   * IllegalArgumentException, malformed/empty/multiple addresses MessagingException. Config, MIME
   * construction and sender failures propagate; no retry or fallback. A connection-boundary failure
   * proven for this exact single message is exposed as MailPreparationException (with the original
   * Spring exception as cause). Send/close failures and unmarked MailSendException remain unknown
   * outcomes.
   */
  public void sendEmail(UserEntity userEntity, String subject, String htmlContent)
      throws MailException, MessagingException {
    // Erst im MIME-Aufbau auswerten: Eine gebundene Methodenreferenz würde bei null früher
    // scheitern.
    sendEmailResolved(() -> userEntity.getEmail(), subject, htmlContent);
  }

  /** Fester Empfänger für verifizierungsgebundene Sicherheitsmails; kein Entity-Umschreiben. */
  public void sendEmailTo(String recipient, String subject, String htmlContent)
      throws MailException, MessagingException {
    sendEmailResolved(() -> recipient, subject, htmlContent);
  }

  private void sendEmailResolved(
      Supplier<String> recipientAddressSupplier, String emailSubject, String htmlContent)
      throws MailException, MessagingException {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    boolean includeLogo =
        htmlContent != null && htmlContent.contains("cid:" + EmailLogo.CONTENT_ID);
    MimeMessageHelper mimeMessageHelper =
        new MimeMessageHelper(
            mimeMessage,
            includeLogo
                ? MimeMessageHelper.MULTIPART_MODE_RELATED
                : MimeMessageHelper.MULTIPART_MODE_NO,
            "UTF-8");

    mimeMessageHelper.setTo(recipientAddressSupplier.get());
    mimeMessageHelper.setSubject(emailSubject);
    Assert.notNull(htmlContent, "Text must not be null");
    mimeMessageHelper.setText(htmlContent, true);
    mimeMessageHelper.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

    if (includeLogo) {
      try {
        // Bytes vor dem Transport lesen; jeder MIME-Lesezugriff erhält einen neuen Stream.
        // Inline erst nach setText hinzufügen, damit Mailprogramme die CID auflösen können.
        mimeMessageHelper.addInline(
            EmailLogo.CONTENT_ID,
            "service-logo.png",
            new ByteArrayResource(EmailLogo.readBytes()),
            "image/png");
      } catch (IOException e) {
        throw new MailPreparationException("Cannot read inline service logo before sending", e);
      }
    }

    try {
      mailSender.send(mimeMessage);
    } catch (MailSendException e) {
      if (ConnectionAwareJavaMailSender.failedBeforeSending(e, mimeMessage)) {
        throw new MailPreparationException("Connection failed before this message was sent", e);
      }
      SmtpRejectionException smtpRejection = SmtpRejectionException.classify(e, mimeMessage);
      if (smtpRejection != null) {
        throw smtpRejection;
      }
      throw e;
    }
  }

  /**
   * Passes a plain-text SimpleMailMessage to the configured sender, whose default encoding is
   * UTF-8. No service-level validation, trimming or HTML interpretation: null/empty recipient,
   * subject, body and configured from are passed unchanged. The real sender may reject such values
   * during conversion/dispatch with a MailException. Config/sender runtime failures propagate
   * unchanged; no retry.
   */
  public void sendAdminEmail(String adminEmail, String emailSubject, String plainTextBody)
      throws MailException {
    SimpleMailMessage adminMailMessage = new SimpleMailMessage();
    adminMailMessage.setTo(adminEmail);
    adminMailMessage.setSubject(emailSubject);
    adminMailMessage.setText(plainTextBody);
    adminMailMessage.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

    mailSender.send(adminMailMessage);
  }
}
