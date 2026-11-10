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
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

    TwoFactorFlow activeTwoFactorFlow = TwoFactorFlow.current(httpRequest);
    if (activeTwoFactorFlow != null
        && !sessionAdmissionService.isExpired(httpRequest)
        && (AppRoute.LOGOUT.getSecurityRoute().equals(httpRequest.getServletPath())
            || AppRoute.LOGIN.getSecurityRoute().equals(httpRequest.getServletPath())
                && "POST".equals(httpRequest.getMethod()))) {
      httpResponse.setHeader("Cache-Control", "no-store");
      httpResponse.sendRedirect(httpRequest.getContextPath() + activeTwoFactorFlow.location());
      return;
    }

    if (!staleTransport(httpRequest) || !restoredAuthentication(httpRequest)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    httpResponse.setContentType("application/json;charset=UTF-8");
    httpResponse.setHeader("Cache-Control", "no-store");

    String flowDestination =
        activeTwoFactorFlow == null
            ? AppRoute.HOME.getSecurityRoute()
            : activeTwoFactorFlow.location();
    String redirectRoute =
        maintenanceService.blocks(httpRequest)
            ? AppRoute.MAINTENANCE.getSecurityRoute()
            : flowDestination;

    httpResponse
        .getWriter()
        .write(
            VaadinService.createCriticalNotificationJSON(
                null, null, null, httpRequest.getContextPath() + redirectRoute));
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
