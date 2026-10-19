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
          ui.access(
              () -> {
                ui.getPage()
                    .setLocation(
                        request.getContextPath() + AppRoute.MAINTENANCE.getSecurityRoute());
                if (ui.getPushConfiguration().getPushMode().isEnabled()) {
                  ui.push();
                }
              });
          return true;
        },
        false);
  }

  static <T> T withUi(HttpServletRequest request, Function<UI, T> action, T fallback) {
    HttpSession http = request.getSession(false);
    if (http == null) {
      return fallback;
    }
    int id;
    try {
      id = Integer.parseInt(request.getParameter("v-uiId"));
    } catch (NumberFormatException missingOrInvalid) {
      return fallback;
    }
    for (String key : Collections.list(http.getAttributeNames())) {
      if (http.getAttribute(key) instanceof VaadinSession session) {
        // Nach einem Serverneustart kann die Sitzung bereits deserialisiert sein,
        // bevor Vaadin ihre transienten Felder wiederherstellt. Ohne den echten
        // Sitzungslock keine UI freigeben; die Framework-Initialisierung nicht vorwegnehmen.
        if (session.getLockInstance() == null) {
          continue;
        }
        session.lock();
        try {
          UI ui = session.getUIById(id);
          if (ui != null) {
            return action.apply(ui);
          }
        } finally {
          session.unlock();
        }
      }
    }
    return fallback;
  }
}
