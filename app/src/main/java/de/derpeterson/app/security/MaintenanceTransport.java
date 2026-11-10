package de.derpeterson.app.security;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.MaintenanceView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Only server-resolved UI identities may use login/maintenance RPC during maintenance. */
public final class MaintenanceTransport {
  private MaintenanceTransport() {}

  public static boolean allowedUi(HttpServletRequest request) {
    return withUi(
        request,
        ui -> {
          List<HasElement> targets = ui.getInternals().getActiveRouterTargetsChain();
          // A freshly created server UI needs its first UIDL to run the router. It has
          // no active business view yet; MaintenanceUiGuard and service admission guard navigation.
          return targets.isEmpty()
              || targets.stream()
                  .anyMatch(
                      target -> target instanceof LoginView || target instanceof MaintenanceView);
        },
        false);
  }

  public static void redirectUi(HttpServletRequest request) {
    withUi(
        request,
        ui -> {
          ui.access(() -> {
            ui.getPage()
                .setLocation(request.getContextPath() + AppRoute.MAINTENANCE.getSecurityRoute());
            if (ui.getPushConfiguration().getPushMode().isEnabled()) {
              ui.push();
            }
          });
          return true;
        },
        false);
  }

  static <T> T withUi(HttpServletRequest httpRequest, Function<UI, T> uiAction, T fallbackResult) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession == null) {
      return fallbackResult;
    }

    int requestedUiId;
    try {
      requestedUiId = Integer.parseInt(httpRequest.getParameter("v-uiId"));
    } catch (NumberFormatException _) {
      return fallbackResult;
    }

    for (String sessionAttributeName : Collections.list(httpSession.getAttributeNames())) {
      if (httpSession.getAttribute(sessionAttributeName) instanceof VaadinSession vaadinSession) {
        // Nach einem Serverneustart kann die Sitzung bereits deserialisiert sein,
        // bevor Vaadin ihre transienten Felder wiederherstellt. Ohne den echten
        // Sitzungslock keine UI freigeben; die Framework-Initialisierung nicht vorwegnehmen.
        if (vaadinSession.getLockInstance() == null) {
          continue;
        }
        vaadinSession.lock();
        try {
          UI requestedUi = vaadinSession.getUIById(requestedUiId);
          if (requestedUi != null) {
            return uiAction.apply(requestedUi);
          }
        } finally {
          vaadinSession.unlock();
        }
      }
    }

    return fallbackResult;
  }
}
