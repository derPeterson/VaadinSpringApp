package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.service.AccountSecurityService.Rejected;
import de.derpeterson.app.validation.EmailConflict;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;

/** Einheitliche sichere Fehlermeldungen ohne Passwort-, Token- oder SQL-Inhalte in der UI. */
@Slf4j
final class AccountSecurityFeedback {
  enum RejectedSecurityInput {
    CURRENT_PASSWORD,
    NEW_PASSWORD,
    EMAIL,
    NONE
  }

  private AccountSecurityFeedback() {}

  static RejectedSecurityInput showSecurityActionFailure(
      MessageProperties messageProperties, RuntimeException securityActionFailure) {
    Supplier<String> feedbackTextSupplier = messageProperties::getAccountSecurityFailed;
    RejectedSecurityInput rejectedField = RejectedSecurityInput.NONE;
    if (securityActionFailure instanceof LoginThrottledException) {
      feedbackTextSupplier = messageProperties::getLoginThrottledMessage;
    } else if (securityActionFailure instanceof BadCredentialsException) {
      feedbackTextSupplier = messageProperties::getAccountSecurityWrongPassword;
      rejectedField = RejectedSecurityInput.CURRENT_PASSWORD;
    } else if (securityActionFailure instanceof Rejected rejectedAction) {
      switch (rejectedAction.problem()) {
        case EMAIL_UNAVAILABLE -> {
          feedbackTextSupplier = messageProperties::getBaseValidationEmailExistsMessage;
          rejectedField = RejectedSecurityInput.EMAIL;
        }
        case SAME_EMAIL -> {
          feedbackTextSupplier = messageProperties::getAccountSecuritySameEmail;
          rejectedField = RejectedSecurityInput.EMAIL;
        }
        case SAME_PASSWORD -> {
          feedbackTextSupplier = messageProperties::getAccountSecuritySamePassword;
          rejectedField = RejectedSecurityInput.NEW_PASSWORD;
        }
        case REQUEST_PENDING ->
            feedbackTextSupplier = messageProperties::getAccountSecurityRequestPending;
      }
    } else if (EmailConflict.isDuplicate(securityActionFailure)) {
      feedbackTextSupplier = messageProperties::getBaseValidationEmailExistsMessage;
      rejectedField = RejectedSecurityInput.EMAIL;
    } else {
      log.error("Account security operation failed", safeFailure(securityActionFailure));
    }
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            feedbackTextSupplier,
            -1,
            NotificationHelper.NotificationType.ERROR);
    return rejectedField;
  }
}
