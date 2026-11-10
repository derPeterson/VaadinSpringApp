package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinService;
import de.derpeterson.app.model.enums.AppRoute;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/** Runs after privilege revalidation and before authorization/servlet dispatch. */
public final class MaintenanceHttpFilter implements Filter {
  public static final String FORWARDED_DOCUMENT =
      MaintenanceHttpFilter.class.getName() + ".forward";
  private final MaintenanceService maintenanceService;

  public MaintenanceHttpFilter(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }

  @Override
  public void doFilter(
      ServletRequest httpRequest, ServletResponse httpResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) httpRequest;
    HttpServletResponse res = (HttpServletResponse) httpResponse;
    String path = req.getServletPath();
    if (!maintenanceService.blocks(req) || AppRoute.LOGOUT.getSecurityRoute().equals(path)) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    String type = req.getParameter("v-r");
    if (isFlowMessage(path, type)) {
      handleFlowMessage(req, res, filterChain, path);
      return;
    }
    // Initialization cannot invoke component RPC. The UI route guard and account services
    // protect its route resolution; heartbeat only keeps an existing session alive.
    if ((path.isEmpty() || "/".equals(path)) && ("init".equals(type) || "heartbeat".equals(type))) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    if (type == null && permittedDocument(req, path)) {
      if (AppRoute.MAINTENANCE.getSecurityRoute().equals(path)) {
        res.setStatus(503);
      }
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    if (("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) && type == null) {
      // Render the existing Vaadin view in a forward while retaining HTTP 503, not a 302 loop.
      res.setStatus(503);
      res.setHeader("Cache-Control", "no-store");
      req.setAttribute(FORWARDED_DOCUMENT, Boolean.TRUE);
      req.getRequestDispatcher(AppRoute.MAINTENANCE.getSecurityRoute())
          .forward(httpRequest, httpResponse);
    } else {
      res.sendError(503);
    }
  }

  private boolean isFlowMessage(String path, String type) {
    return "uidl".equals(type)
        || "push".equals(type)
        || "/VAADIN/uidl".equals(path)
        || "/VAADIN/push".equals(path);
  }

  private void handleFlowMessage(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      FilterChain filterChain,
      String path)
      throws IOException, ServletException {
    if (flowEndpoint(path) && MaintenanceTransport.allowedUi(httpRequest)) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    // A Vaadin protocol message, never an HTML page in an existing UIDL response.
    httpResponse.setContentType("application/json;charset=UTF-8");
    httpResponse.setHeader("Cache-Control", "no-store");
    httpResponse
        .getWriter()
        .write(
            VaadinService.createCriticalNotificationJSON(
                null,
                null,
                null,
                httpRequest.getContextPath() + AppRoute.MAINTENANCE.getSecurityRoute()));
  }

  private boolean permittedDocument(HttpServletRequest httpRequest, String path) {
    return AppRoute.LOGIN.getSecurityRoute().equals(path)
        || AppRoute.MAINTENANCE.getSecurityRoute().equals(path)
        || resource(httpRequest, path);
  }

  private boolean flowEndpoint(String path) {
    // Vaadin uses the application-root service URL (plus its explicit UIDL/push endpoints).
    // A forged v-r parameter must never release another servlet such as the H2 console.
    return path.isEmpty()
        || "/".equals(path)
        || "/VAADIN/uidl".equals(path)
        || "/VAADIN/push".equals(path);
  }

  private boolean resource(HttpServletRequest httpRequest, String path) {
    if (!("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))) {
      return false;
    }
    if (Set.of(
            "/favicon.ico",
            "/manifest.webmanifest",
            "/sw.js",
            "/sw-runtime-resources-precache.js",
            "/offline.html",
            "/error")
        .contains(path)) {
      return true;
    }
    return Set.of(
            "/VAADIN/",
            "/themes/",
            "/custom-theme/",
            "/lumo/",
            "/frontend/",
            "/icons/",
            "/images/",
            "/line-awesome/",
            "/webjars/")
        .stream()
        .anyMatch(path::startsWith);
  }
}
