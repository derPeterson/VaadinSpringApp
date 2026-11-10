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
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    String servletPath = httpRequest.getServletPath();

    if (!maintenanceService.blocks(httpRequest)
        || AppRoute.LOGOUT.getSecurityRoute().equals(servletPath)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    String vaadinRequestType = httpRequest.getParameter("v-r");
    if (isFlowMessage(servletPath, vaadinRequestType)) {
      handleFlowMessage(httpRequest, httpResponse, filterChain, servletPath);
      return;
    }

    // Initialization cannot invoke component RPC. The UI route guard and account services
    // protect its route resolution; heartbeat only keeps an existing session alive.
    if ((servletPath.isEmpty() || "/".equals(servletPath))
        && ("init".equals(vaadinRequestType) || "heartbeat".equals(vaadinRequestType))) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    if (vaadinRequestType == null && permittedDocument(httpRequest, servletPath)) {
      if (AppRoute.MAINTENANCE.getSecurityRoute().equals(servletPath)) {
        httpResponse.setStatus(503);
      }
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    if (("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
        && vaadinRequestType == null) {
      // Render the existing Vaadin view in a forward while retaining HTTP 503, not a 302 loop.
      httpResponse.setStatus(503);
      httpResponse.setHeader("Cache-Control", "no-store");
      httpRequest.setAttribute(FORWARDED_DOCUMENT, Boolean.TRUE);
      httpRequest
          .getRequestDispatcher(AppRoute.MAINTENANCE.getSecurityRoute())
          .forward(servletRequest, servletResponse);
    } else {
      httpResponse.sendError(503);
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
