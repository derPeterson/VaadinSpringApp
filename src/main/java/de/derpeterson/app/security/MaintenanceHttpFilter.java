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
  private final MaintenanceService maintenance;

  public MaintenanceHttpFilter(MaintenanceService maintenance) {
    this.maintenance = maintenance;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    String path = req.getServletPath();
    if (!maintenance.blocks(req) || AppRoute.LOGOUT.getSecurityRoute().equals(path)) {
      chain.doFilter(request, response);
      return;
    }
    String type = req.getParameter("v-r");
    if (isFlowMessage(path, type)) {
      handleFlowMessage(req, res, chain, path);
      return;
    }
    // Initialization cannot invoke component RPC. The UI route guard and account services
    // protect its route resolution; heartbeat only keeps an existing session alive.
    if ((path.isEmpty() || "/".equals(path)) && ("init".equals(type) || "heartbeat".equals(type))) {
      chain.doFilter(request, response);
      return;
    }
    if (type == null && permittedDocument(req, path)) {
      if (AppRoute.MAINTENANCE.getSecurityRoute().equals(path)) {
        res.setStatus(503);
      }
      chain.doFilter(request, response);
      return;
    }
    if (("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) && type == null) {
      // Render the existing Vaadin view in a forward while retaining HTTP 503, not a 302 loop.
      res.setStatus(503);
      res.setHeader("Cache-Control", "no-store");
      req.setAttribute(FORWARDED_DOCUMENT, Boolean.TRUE);
      req.getRequestDispatcher(AppRoute.MAINTENANCE.getSecurityRoute()).forward(request, response);
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
      HttpServletRequest request, HttpServletResponse response, FilterChain chain, String path)
      throws IOException, ServletException {
    if (flowEndpoint(path) && MaintenanceTransport.allowedUi(request)) {
      chain.doFilter(request, response);
      return;
    }
    // A Vaadin protocol message, never an HTML page in an existing UIDL response.
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response
        .getWriter()
        .write(
            VaadinService.createCriticalNotificationJSON(
                null,
                null,
                null,
                request.getContextPath() + AppRoute.MAINTENANCE.getSecurityRoute()));
  }

  private boolean permittedDocument(HttpServletRequest request, String path) {
    return AppRoute.LOGIN.getSecurityRoute().equals(path)
        || AppRoute.MAINTENANCE.getSecurityRoute().equals(path)
        || resource(request, path);
  }

  private boolean flowEndpoint(String path) {
    // Vaadin uses the application-root service URL (plus its explicit UIDL/push endpoints).
    // A forged v-r parameter must never release another servlet such as the H2 console.
    return path.isEmpty()
        || "/".equals(path)
        || "/VAADIN/uidl".equals(path)
        || "/VAADIN/push".equals(path);
  }

  private boolean resource(HttpServletRequest request, String path) {
    if (!("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
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
