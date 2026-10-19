package de.derpeterson.app.ui.components;

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
  enum Field {
    CURRENT_PASSWORD,
    NEW_PASSWORD,
    EMAIL,
    NONE
  }

  private AccountSecurityFeedback() {}

  static Field failure(MessageProperties messages, RuntimeException failure) {
    Supplier<String> text = messages::getAccountSecurityFailed;
    Field field = Field.NONE;
    if (failure instanceof LoginThrottledException) {
      text = messages::getLoginThrottledMessage;
    } else if (failure instanceof BadCredentialsException) {
      text = messages::getAccountSecurityWrongPassword;
      field = Field.CURRENT_PASSWORD;
    } else if (failure instanceof Rejected rejected) {
      switch (rejected.problem()) {
        case EMAIL_UNAVAILABLE -> {
          text = messages::getBaseValidationEmailExistsMessage;
          field = Field.EMAIL;
        }
        case SAME_EMAIL -> {
          text = messages::getAccountSecuritySameEmail;
          field = Field.EMAIL;
        }
        case SAME_PASSWORD -> {
          text = messages::getAccountSecuritySamePassword;
          field = Field.NEW_PASSWORD;
        }
        case REQUEST_PENDING -> text = messages::getAccountSecurityRequestPending;
      }
    } else if (EmailConflict.isDuplicate(failure)) {
      text = messages::getBaseValidationEmailExistsMessage;
      field = Field.EMAIL;
    } else {
      log.error("Account security operation failed", failure);
    }
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseFailedTitle, text, -1, NotificationHelper.NotificationType.ERROR);
    return field;
  }
}
