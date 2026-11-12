package de.derpeterson.app.security.mfa;

import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.service.AccountSecurityMail;
import de.derpeterson.app.service.EmailQueueService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Nur Queueaufnahme. Die vorhandene Mailqueue verantwortet den tatsächlichen Transport. */
@Component
@RequiredArgsConstructor
public class TwoFactorMail {
  public enum Event {
    ENABLED,
    DISABLED,
    RECOVERY_CODES
  }

  private final AccountSecurityMail accountSecurityMail;
  private final EmailQueueService emailQueueService;

  public void setupCode(UserEntity recipientUserEntity, String setupCodeText) {
    if (recipientUserEntity.isDevelopmentAccount()) {
      throw new AccessDeniedException("Development accounts cannot receive confirmation codes");
    }
    MessageProperties messageProperties =
        accountSecurityMail.messagePropertiesForRecipient(recipientUserEntity);

    enqueueTwoFactorMail(
        recipientUserEntity,
        messageProperties.getTwoFactorEmailSubject(),
        messageProperties.getTwoFactorEmailExplanation(),
        setupCodeText,
        messageProperties.getTwoFactorEmailNotice());
  }

  public void securityChange(UserEntity recipientUserEntity, Event securityChangeEvent) {
    if (recipientUserEntity.isDevelopmentAccount()) {
      return;
    }
    MessageProperties messageProperties =
        accountSecurityMail.messagePropertiesForRecipient(recipientUserEntity);

    String mailSubject =
        switch (securityChangeEvent) {
          case ENABLED -> messageProperties.getTwoFactorEnabledSubject();
          case DISABLED -> messageProperties.getTwoFactorDisabledSubject();
          case RECOVERY_CODES -> messageProperties.getTwoFactorRenewedSubject();
        };

    String mailExplanation =
        switch (securityChangeEvent) {
          case ENABLED -> messageProperties.getTwoFactorEnabledExplanation();
          case DISABLED -> messageProperties.getTwoFactorDisabledExplanation();
          case RECOVERY_CODES -> messageProperties.getTwoFactorRenewedExplanation();
        };

    enqueueTwoFactorMail(
        recipientUserEntity,
        mailSubject,
        mailExplanation,
        "",
        messageProperties.getTwoFactorSecurityNotice());
  }

  private void enqueueTwoFactorMail(
      UserEntity recipientUserEntity,
      String mailSubject,
      String mailExplanation,
      String setupCodeText,
      String mailNotice) {
    String mailBodyHtml =
        accountSecurityMail.render(
            "two_factor",
            recipientUserEntity,
            Map.of(
                "TITLE",
                mailSubject,
                "MESSAGE",
                mailExplanation,
                "CODE",
                setupCodeText,
                "NOTICE",
                mailNotice));

    emailQueueService.addAddressedNotification(
        recipientUserEntity, recipientUserEntity.getEmail(), mailSubject, mailBodyHtml);
  }
}
