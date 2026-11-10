package de.derpeterson.app.security;

import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.RecoveryHelpView;
import de.derpeterson.app.views.TwoFactorView;
import jakarta.servlet.http.HttpServletRequest;

/** Nur die serverseitig ermittelte eingeschränkte Ansicht darf MFA-RPCs empfangen. */
public final class TwoFactorTransport {
  private TwoFactorTransport() {}

  public static String destination(HttpServletRequest request) {
    TwoFactorFlow flow = TwoFactorFlow.current(request);
    if (flow != null) {
      return request.getContextPath() + flow.location();
    }
    boolean administration = "/admin".equals(request.getServletPath()) || administrativeUi(request);
    return request.getContextPath()
        + AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE
        + (administration ? "?administration" : "");
  }

  public static boolean allowedUi(HttpServletRequest request) {
    return MaintenanceTransport.withUi(
        request,
        ui ->
            ui.getInternals().getActiveRouterTargetsChain().isEmpty()
                || ui.getInternals().getActiveRouterTargetsChain().stream()
                    .anyMatch(activeRouteComponent -> {
                      TwoFactorFlow activeTwoFactorFlow = TwoFactorFlow.current(request);
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

  public static void redirectUi(HttpServletRequest request) {
    MaintenanceTransport.withUi(
        request,
        ui -> {
          ui.access(() -> {
            ui.getPage().setLocation(destination(request));
            if (ui.getPushConfiguration().getPushMode().isEnabled()) {
              ui.push();
            }
          });
          return true;
        },
        false);
  }

  public static boolean administrativeUi(HttpServletRequest request) {
    return MaintenanceTransport.withUi(
        request,
        ui ->
            ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(AdminView.class::isInstance),
        false);
  }
}
