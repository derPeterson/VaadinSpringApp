package de.derpeterson.app.security.mfa;

import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.service.AccountSecurityMail;
import de.derpeterson.app.service.EmailQueueService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

  private final AccountSecurityMail templates;
  private final EmailQueueService queue;

  public void setupCode(UserEntity user, String code) {
    MessageProperties messageProperties = templates.messagePropertiesForRecipient(user);
    enqueue(
        user,
        messageProperties.getTwoFactorEmailSubject(),
        messageProperties.getTwoFactorEmailExplanation(),
        code,
        messageProperties.getTwoFactorEmailNotice());
  }

  public void securityChange(UserEntity user, Event event) {
    MessageProperties messageProperties = templates.messagePropertiesForRecipient(user);
    String title =
        switch (event) {
          case ENABLED -> messageProperties.getTwoFactorEnabledSubject();
          case DISABLED -> messageProperties.getTwoFactorDisabledSubject();
          case RECOVERY_CODES -> messageProperties.getTwoFactorRenewedSubject();
        };
    String message =
        switch (event) {
          case ENABLED -> messageProperties.getTwoFactorEnabledExplanation();
          case DISABLED -> messageProperties.getTwoFactorDisabledExplanation();
          case RECOVERY_CODES -> messageProperties.getTwoFactorRenewedExplanation();
        };
    enqueue(user, title, message, "", messageProperties.getTwoFactorSecurityNotice());
  }

  private void enqueue(UserEntity user, String title, String message, String code, String notice) {
    String body =
        templates.render(
            "two_factor",
            user,
            Map.of("TITLE", title, "MESSAGE", message, "CODE", code, "NOTICE", notice));
    queue.addAddressedNotification(user, user.getEmail(), title, body);
  }
}
