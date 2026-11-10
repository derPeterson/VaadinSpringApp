package de.derpeterson.app.security;

import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.RecoveryHelpView;
import de.derpeterson.app.views.TwoFactorView;
import jakarta.servlet.http.HttpServletRequest;

/** Nur die serverseitig ermittelte eingeschränkte Ansicht darf MFA-RPCs empfangen. */
public final class TwoFactorTransport {
  private TwoFactorTransport() {}

  public static String destination(HttpServletRequest httpRequest) {
    TwoFactorFlow activeTwoFactorFlow = TwoFactorFlow.current(httpRequest);
    if (activeTwoFactorFlow != null) {
      return httpRequest.getContextPath() + activeTwoFactorFlow.location();
    }

    boolean administrationRequested =
        "/admin".equals(httpRequest.getServletPath()) || administrativeUi(httpRequest);

    return httpRequest.getContextPath()
        + AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE
        + (administrationRequested ? "?administration" : "");
  }

  public static boolean allowedUi(HttpServletRequest httpRequest) {
    return MaintenanceTransport.withUi(
        httpRequest,
        ui ->
            ui.getInternals().getActiveRouterTargetsChain().isEmpty()
                || ui.getInternals().getActiveRouterTargetsChain().stream()
                    .anyMatch(activeRouteComponent -> {
                      TwoFactorFlow activeTwoFactorFlow = TwoFactorFlow.current(httpRequest);
                      if (activeTwoFactorFlow == null) {
                        return activeRouteComponent instanceof TwoFactorView
                            || activeRouteComponent instanceof RecoveryHelpView;
                      }

                      return activeRouteComponent instanceof TwoFactorView twoFactorView
                              && twoFactorView.matchesActiveTwoFactorFlow(activeTwoFactorFlow)
                          || activeRouteComponent instanceof RecoveryHelpView recoveryHelpView
                              && recoveryHelpView.matchesActiveTwoFactorFlow(activeTwoFactorFlow);
                    }),
        false);
  }

  public static void redirectUi(HttpServletRequest httpRequest) {
    MaintenanceTransport.withUi(
        httpRequest,
        ui -> {
          ui.access(() -> {
            ui.getPage().setLocation(destination(httpRequest));

            if (ui.getPushConfiguration().getPushMode().isEnabled()) {
              ui.push();
            }
          });

          return true;
        },
        false);
  }

  public static boolean administrativeUi(HttpServletRequest httpRequest) {
    return MaintenanceTransport.withUi(
        httpRequest,
        ui ->
            ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(AdminView.class::isInstance),
        false);
  }
}
