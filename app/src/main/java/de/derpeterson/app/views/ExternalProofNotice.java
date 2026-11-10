package de.derpeterson.app.views;

import com.vaadin.flow.router.AfterNavigationEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import java.util.List;
import java.util.function.Supplier;

final class ExternalProofNotice {
  private ExternalProofNotice() {}

  static void showExternalDisconnectNotice(
      AfterNavigationEvent navigationEvent,
      MessageProperties messageProperties,
      boolean isSignInPage) {
    if (navigationEvent
        .getLocation()
        .getQueryParameters()
        .getParameters()
        .containsKey(ExternalLoginFlow.DISCONNECTED_PARAMETER)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getExternalDisconnected,
              isSignInPage
                  ? messageProperties::getExternalDisconnectedSignIn
                  : messageProperties::getExternalDisconnectedHint,
              NotificationHelper.NotificationType.SUCCESS);
    }
  }

  static void showExternalProofFailure(
      AfterNavigationEvent navigationEvent, MessageProperties messageProperties) {
    // Erst der angehängte Zielrahmen liefert die Palette für ausgelagerte Meldungen.
    List<String> proofErrorParameterValues =
        navigationEvent
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .get(ExternalLoginFlow.ERROR_PARAMETER);
    if (proofErrorParameterValues == null) {
      return;
    }

    ExternalLoginFlow.ProofProblem proofProblem = ExternalLoginFlow.ProofProblem.UNAVAILABLE;
    if (proofErrorParameterValues.size() == 1) {
      try {
        proofProblem = ExternalLoginFlow.ProofProblem.valueOf(proofErrorParameterValues.getFirst());
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
