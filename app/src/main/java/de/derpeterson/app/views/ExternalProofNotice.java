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
      AfterNavigationEvent navigationEvent, MessageProperties messageProperties, boolean signIn) {
    if (navigationEvent
        .getLocation()
        .getQueryParameters()
        .getParameters()
        .containsKey(ExternalLoginFlow.DISCONNECTED_PARAMETER)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getExternalDisconnected,
              signIn
                  ? messageProperties::getExternalDisconnectedSignIn
                  : messageProperties::getExternalDisconnectedHint,
              NotificationHelper.NotificationType.SUCCESS);
    }
  }

  static void show(AfterNavigationEvent navigationEvent, MessageProperties messageProperties) {
    // Erst der angehängte Zielrahmen liefert die Palette für ausgelagerte Meldungen.
    List<String> queryParameterValues =
        navigationEvent
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .get(ExternalLoginFlow.ERROR_PARAMETER);
    if (queryParameterValues == null) {
      return;
    }
    ExternalLoginFlow.ProofProblem proofProblem = ExternalLoginFlow.ProofProblem.UNAVAILABLE;
    if (queryParameterValues.size() == 1) {
      try {
        proofProblem = ExternalLoginFlow.ProofProblem.valueOf(queryParameterValues.getFirst());
      } catch (IllegalArgumentException _) {
        // Nur bekannte Fehlerarten auswählen, nie Queryinhalte als Meldung ausgeben.
      }
    }
    Supplier<String> proofFailureTextSupplier =
        switch (proofProblem) {
          case FRESH_AUTHENTICATION_REQUIRED -> messageProperties::getExternalProofFreshRequired;
          case IDENTITY_MISMATCH -> messageProperties::getExternalProofIdentityMismatch;
          case UNAVAILABLE -> messageProperties::getExternalError;
        };
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            proofFailureTextSupplier,
            NotificationHelper.NotificationType.ERROR);
  }
}
