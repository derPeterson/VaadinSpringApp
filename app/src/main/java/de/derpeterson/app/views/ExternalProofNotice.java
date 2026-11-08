package de.derpeterson.app.views;

import com.vaadin.flow.router.AfterNavigationEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import java.util.List;
import java.util.function.Supplier;

final class ExternalProofNotice {
  private ExternalProofNotice() {}

  static void showDisconnected(
      AfterNavigationEvent event, MessageProperties texts, boolean signIn) {
    if (event
        .getLocation()
        .getQueryParameters()
        .getParameters()
        .containsKey(ExternalLoginFlow.DISCONNECTED_PARAMETER)) {
      NotificationHelper.getInstance()
          .showNotification(
              texts::getExternalDisconnected,
              signIn ? texts::getExternalDisconnectedSignIn : texts::getExternalDisconnectedHint,
              NotificationHelper.NotificationType.SUCCESS);
    }
  }

  static void show(AfterNavigationEvent event, MessageProperties messageProperties) {
    // Erst der angehängte Zielrahmen liefert die Palette für ausgelagerte Meldungen.
    List<String> values =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .get(ExternalLoginFlow.ERROR_PARAMETER);
    if (values == null) {
      return;
    }
    ExternalLoginFlow.ProofProblem problem = ExternalLoginFlow.ProofProblem.UNAVAILABLE;
    if (values.size() == 1) {
      try {
        problem = ExternalLoginFlow.ProofProblem.valueOf(values.getFirst());
      } catch (IllegalArgumentException _) {
        // Nur bekannte Fehlerarten auswählen, nie Queryinhalte als Meldung ausgeben.
      }
    }
    Supplier<String> message =
        switch (problem) {
          case FRESH_AUTHENTICATION_REQUIRED -> messageProperties::getExternalProofFreshRequired;
          case IDENTITY_MISMATCH -> messageProperties::getExternalProofIdentityMismatch;
          case UNAVAILABLE -> messageProperties::getExternalError;
        };
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            NotificationHelper.NotificationType.ERROR);
  }
}
