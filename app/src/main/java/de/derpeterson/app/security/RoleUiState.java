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

  public static Set<String> extractRoleAuthorities(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .filter(a -> !(a instanceof FactorGrantedAuthority))
        .map(a -> a.getAuthority())
        .collect(Collectors.toUnmodifiableSet());
  }

  public static boolean hasUiRoleStateChanged(UI vaadinUi, Authentication authentication) {
    RoleUiState roleUiState = ComponentUtil.getData(vaadinUi, RoleUiState.class);
    if (roleUiState == null) {
      ComponentUtil.setData(
          vaadinUi, RoleUiState.class, new RoleUiState(extractRoleAuthorities(authentication)));
      return false;
    }
    return !roleUiState.authorities().equals(extractRoleAuthorities(authentication));
  }

  public static String resolveRoleRefreshLocation(
      UI vaadinUi, Authentication authentication, String contextPath) {
    Location activeViewLocation = vaadinUi.getInternals().getActiveViewLocation();
    if (activeViewLocation == null) {
      return contextPath + HOME_PATH;
    }
    String routePath = activeViewLocation.getPath();
    if (routePath.equals(AppRoute.ADMIN.getRoute())
        && !extractRoleAuthorities(authentication).contains("ROLE_ADMIN")) {
      return contextPath + HOME_PATH;
    }
    if (!Set.of("home", "account", "notifications", "admin").contains(routePath)) {
      return contextPath + HOME_PATH;
    }
    return contextPath + "/" + activeViewLocation.getPathWithQueryParameters();
  }

  public static boolean refreshUiAfterRoleChange(
      UI vaadinUi, Authentication authentication, String contextPath) {
    if (!hasUiRoleStateChanged(vaadinUi, authentication)) {
      return false;
    }
    vaadinUi
        .getPage()
        .setLocation(resolveRoleRefreshLocation(vaadinUi, authentication, contextPath));
    return true;
  }
}
