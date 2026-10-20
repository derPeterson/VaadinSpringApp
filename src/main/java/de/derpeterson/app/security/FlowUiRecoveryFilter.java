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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Nach einem Neustart nur ein neues Dokument anfordern; niemals veraltete RPCs ausführen. */
public final class FlowUiRecoveryFilter implements Filter {
  private final String mapping;
  private final SessionAdmissionService admissions;
  private final MaintenanceService maintenance;

  public FlowUiRecoveryFilter(
      String mapping, SessionAdmissionService admissions, MaintenanceService maintenance) {
    this.mapping = mapping;
    this.admissions = admissions;
    this.maintenance = maintenance;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) request;
    if (!staleTransport(http) || !restoredAuthentication(http)) {
      chain.doFilter(request, response);
      return;
    }
    HttpServletResponse target = (HttpServletResponse) response;
    target.setContentType("application/json;charset=UTF-8");
    target.setHeader("Cache-Control", "no-store");
    AppRoute route = maintenance.blocks(http) ? AppRoute.MAINTENANCE : AppRoute.HOME;
    target
        .getWriter()
        .write(
            VaadinService.createCriticalNotificationJSON(
                null, null, null, http.getContextPath() + route.getSecurityRoute()));
  }

  private boolean staleTransport(HttpServletRequest request) {
    String type = request.getParameter("v-r");
    if (!("uidl".equals(type) || "push".equals(type))) {
      return false;
    }
    Optional<String> inside =
        HandlerHelper.getPathIfInsideServlet(
            mapping, HandlerHelper.getRequestPathInsideContext(request));
    if (inside.isEmpty()) {
      return false;
    }
    String path = inside.get();
    return path.isEmpty() || "/".equals(path) || "VAADIN/push".equals(path) && "push".equals(type);
  }

  private boolean restoredAuthentication(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null
        || session.getAttribute(DurableSessionRepository.ACTIVE) == null
        || admissions.isExpired(request)) {
      return false;
    }
    Object value =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    if (!(value instanceof SecurityContext context)
        || context.getAuthentication() == null
        || !context.getAuthentication().isAuthenticated()) {
      return false;
    }
    return Collections.list(session.getAttributeNames()).stream()
        .noneMatch(name -> session.getAttribute(name) instanceof VaadinSession);
  }
}
