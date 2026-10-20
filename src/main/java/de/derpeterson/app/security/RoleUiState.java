package de.derpeterson.app.security;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.model.enums.AppRoute;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;

/** Jede UI behält ihre ursprünglichen Rechte, bis ein neues Dokument aufgebaut wurde. */
public record RoleUiState(Set<String> authorities) implements Serializable {
  private static final String HOME_PATH = AppRouteConstants.HOME_SECURITY_ROUTE;

  public static Set<String> roles(Authentication auth) {
    return auth.getAuthorities().stream()
        .filter(a -> !(a instanceof FactorGrantedAuthority))
        .map(a -> a.getAuthority())
        .collect(Collectors.toUnmodifiableSet());
  }

  public static boolean changed(UI ui, Authentication authentication) {
    RoleUiState state = ComponentUtil.getData(ui, RoleUiState.class);
    if (state == null) {
      ComponentUtil.setData(ui, RoleUiState.class, new RoleUiState(roles(authentication)));
      return false;
    }
    return !state.authorities().equals(roles(authentication));
  }

  public static String target(UI ui, Authentication auth, String contextPath) {
    Location location = ui.getInternals().getActiveViewLocation();
    if (location == null) {
      return contextPath + HOME_PATH;
    }
    String path = location.getPath();
    if (path.equals(AppRoute.ADMIN.getRoute()) && !roles(auth).contains("ROLE_ADMIN")) {
      return contextPath + HOME_PATH;
    }
    if (!Set.of("home", "account", "notifications", "admin").contains(path)) {
      return contextPath + HOME_PATH;
    }
    return contextPath + "/" + location.getPathWithQueryParameters();
  }

  public static boolean refresh(UI ui, Authentication auth, String contextPath) {
    if (!changed(ui, auth)) {
      return false;
    }
    ui.getPage().setLocation(target(ui, auth, contextPath));
    return true;
  }
}
