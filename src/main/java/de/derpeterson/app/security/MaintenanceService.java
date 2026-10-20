package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Fresh admission decisions; no session invalidation and no scheduler-wide switch. */
public final class MaintenanceService {
  public enum State {
    OPEN,
    ACTIVE,
    UNAVAILABLE
  }

  private final ConfigService config;

  public MaintenanceService(ConfigService config) {
    this.config = config;
  }

  public State state() {
    try {
      return config.getBoolean(ConfigEntry.MAINTENANCE_MODE) ? State.ACTIVE : State.OPEN;
    } catch (RuntimeException _) {
      // An unreadable/invalid setting must never silently open the application.
      return State.UNAVAILABLE;
    }
  }

  public static boolean administrator(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(
                a ->
                    "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SUPERADMIN".equals(a.getAuthority()));
  }

  public static Authentication authentication(HttpServletRequest request) {
    // Push invokes admission before resolving its Vaadin UI/session. Bind the role decision
    // to this request's stored context, never a different identity left on a worker thread.
    HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
            instanceof SecurityContext context) {
      return context.getAuthentication();
    }
    // Spring's HTTP wrapper exposes the current request principal. Atmosphere exposes
    // its own handshake principal; an anonymous socket must not inherit a thread's admin.
    return request.getUserPrincipal() instanceof Authentication principal ? principal : null;
  }

  public boolean blocks(HttpServletRequest request) {
    State state = state();
    return state != State.OPEN
        && (state != State.ACTIVE || !administrator(authentication(request)));
  }

  public void requireLogin(Authentication authentication) {
    State state = state();
    if (state == State.UNAVAILABLE) {
      throw new AuthenticationServiceException("Maintenance configuration unavailable");
    }
    if (state == State.ACTIVE && !administrator(authentication)) {
      throw new MaintenanceAuthenticationException();
    }
  }

  public static void requirePublicAccess(ConfigService config) {
    State state = new MaintenanceService(config).state();
    if (state == State.UNAVAILABLE) {
      throw new MaintenanceAccessException("Service temporarily unavailable");
    }
    if (state == State.ACTIVE
        && !administrator(SecurityContextHolder.getContext().getAuthentication())) {
      throw new MaintenanceAccessException("Public account actions are paused for maintenance");
    }
  }
}
