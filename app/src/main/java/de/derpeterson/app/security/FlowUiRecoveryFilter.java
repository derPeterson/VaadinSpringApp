package de.derpeterson.app.security;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.session.DurableSessionRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Nach einem Neustart nur ein neues Dokument anfordern; niemals veraltete RPCs ausführen. */
public final class FlowUiRecoveryFilter implements Filter {
  private final String vaadinServletMapping;
  private final SessionAdmissionService sessionAdmissionService;
  private final MaintenanceService maintenanceService;

  public FlowUiRecoveryFilter(
      String vaadinServletMapping,
      SessionAdmissionService sessionAdmissionService,
      MaintenanceService maintenanceService) {
    this.vaadinServletMapping = vaadinServletMapping;
    this.sessionAdmissionService = sessionAdmissionService;
    this.maintenanceService = maintenanceService;
  }

  @Override
  public void doFilter(
      ServletRequest httpRequest, ServletResponse httpResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) httpRequest;
    HttpServletResponse target = (HttpServletResponse) httpResponse;
    TwoFactorFlow flow = TwoFactorFlow.current(http);
    if (flow != null
        && !sessionAdmissionService.isExpired(http)
        && (AppRoute.LOGOUT.getSecurityRoute().equals(http.getServletPath())
            || AppRoute.LOGIN.getSecurityRoute().equals(http.getServletPath())
                && "POST".equals(http.getMethod()))) {
      target.setHeader("Cache-Control", "no-store");
      target.sendRedirect(http.getContextPath() + flow.location());
      return;
    }
    if (!staleTransport(http) || !restoredAuthentication(http)) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    target.setContentType("application/json;charset=UTF-8");
    target.setHeader("Cache-Control", "no-store");
    String destination = flow == null ? AppRoute.HOME.getSecurityRoute() : flow.location();
    String route =
        maintenanceService.blocks(http) ? AppRoute.MAINTENANCE.getSecurityRoute() : destination;
    target
        .getWriter()
        .write(
            VaadinService.createCriticalNotificationJSON(
                null, null, null, http.getContextPath() + route));
  }

  private boolean staleTransport(HttpServletRequest httpRequest) {
    String type = httpRequest.getParameter("v-r");
    if (!("uidl".equals(type) || "push".equals(type))) {
      return false;
    }
    Optional<String> inside =
        HandlerHelper.getPathIfInsideServlet(
            vaadinServletMapping, HandlerHelper.getRequestPathInsideContext(httpRequest));
    if (inside.isEmpty()) {
      return false;
    }
    String path = inside.get();
    return path.isEmpty() || "/".equals(path) || "VAADIN/push".equals(path) && "push".equals(type);
  }

  private boolean restoredAuthentication(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    if (session == null
        || session.getAttribute(DurableSessionRepository.ACTIVE) == null
        || sessionAdmissionService.isExpired(httpRequest)) {
      return false;
    }
    Object value =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    if (!(value instanceof SecurityContext context)) {
      return false;
    }
    Authentication authentication = context.getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    return Collections.list(session.getAttributeNames()).stream()
        .noneMatch(name -> session.getAttribute(name) instanceof VaadinSession);
  }
}
