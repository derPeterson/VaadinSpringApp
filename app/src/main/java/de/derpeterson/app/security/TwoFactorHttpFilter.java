package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinService;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorDocuments;
import de.derpeterson.app.ui.components.RecoveryCodeDownload;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

/** Schützt auch alte Ansichten und Remember-me-Sitzungen vor dem Servlet-/RPC-Aufruf. */
public final class TwoFactorHttpFilter implements Filter {
  // Flow registriert Downloads mit UI-Nummer, Ressourcen-UUID und genau diesem Namen.
  private static final Pattern RECOVERY_DOWNLOAD =
      Pattern.compile(
          "/VAADIN/dynamic/resource/[0-9]+/[0-9a-f-]{36}/"
              + Pattern.quote(RecoveryCodeDownload.RESOURCE_NAME));

  private final SecurityService securityService;
  private final TwoFactorAuthorization twoFactorAuthorization;

  public TwoFactorHttpFilter(
      SecurityService securityService, TwoFactorAuthorization twoFactorAuthorization) {
    this.securityService = securityService;
    this.twoFactorAuthorization = twoFactorAuthorization;
  }

  @Override
  public void doFilter(
      ServletRequest httpRequest, ServletResponse httpResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) httpRequest;
    HttpServletResponse reply = (HttpServletResponse) httpResponse;
    if (securityService.getAuthenticatedUser(http).isEmpty()) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    String path = http.getServletPath();
    String type = http.getParameter("v-r");
    boolean protocol = isProtocol(path, type);
    TwoFactorFlow flow = TwoFactorFlow.current(http);
    boolean confirmed = flow == null && confirmed(http, path, protocol);
    if (protocol) {
      if (serviceEndpoint(path) && (confirmed || TwoFactorTransport.allowedUi(http))) {
        filterChain.doFilter(httpRequest, httpResponse);
      } else {
        reply.setContentType("application/json;charset=UTF-8");
        reply.setHeader("Cache-Control", "no-store");
        reply
            .getWriter()
            .write(
                VaadinService.createCriticalNotificationJSON(
                    null, null, null, TwoFactorTransport.destination(http)));
      }
      return;
    }
    if (confirmed || permitted(http, path, type, flow)) {
      filterChain.doFilter(httpRequest, httpResponse);
      return;
    }
    reply.setHeader("Cache-Control", "no-store");
    if (type == null && "GET".equals(http.getMethod())) {
      reply.sendRedirect(TwoFactorTransport.destination(http));
    } else {
      reply.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  private boolean isProtocol(String path, String type) {
    return "uidl".equals(type)
        || "push".equals(type)
        || "/VAADIN/uidl".equals(path)
        || "/VAADIN/push".equals(path);
  }

  private boolean confirmed(HttpServletRequest httpRequest, String path, boolean protocol) {
    return twoFactorAuthorization.sessionConfirmed(httpRequest)
        && (!("/admin".equals(path) || protocol && TwoFactorTransport.administrativeUi(httpRequest))
            || twoFactorAuthorization.administrationConfirmed(httpRequest));
  }

  private boolean permitted(
      HttpServletRequest httpRequest, String path, String type, TwoFactorFlow flow) {
    if (serviceEndpoint(path) && ("init".equals(type) || "heartbeat".equals(type))) {
      return true;
    }
    if (flow != null) {
      return type == null
          && ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
          && (flow.path().equals(path)
              || !flow.help()
                  && (path.startsWith(TwoFactorDocuments.PATH + "/")
                      || RECOVERY_DOWNLOAD.matcher(path).matches())
              || resource(path));
    }
    return type == null
        && (AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE.equals(path)
            || "/recovery-help".equals(path)
            || "/logout".equals(path)
            || "/login".equals(path)
            || path.startsWith("/oauth2/authorization/")
            || path.startsWith("/login/oauth2/code/")
            || path.startsWith(TwoFactorDocuments.PATH + "/")
            || ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
                && resource(path));
  }

  private boolean serviceEndpoint(String path) {
    return path.isEmpty()
        || "/".equals(path)
        || "/VAADIN/uidl".equals(path)
        || "/VAADIN/push".equals(path);
  }

  private boolean resource(String path) {
    if (path.startsWith("/VAADIN/dynamic/")) {
      return false;
    }
    return Set.of(
                "/VAADIN/",
                "/themes/",
                "/custom-theme/",
                "/lumo/",
                "/frontend/",
                "/icons/",
                "/images/",
                "/webjars/")
            .stream()
            .anyMatch(path::startsWith)
        || Set.of(
                "/favicon.ico",
                "/manifest.webmanifest",
                "/sw.js",
                "/sw-runtime-resources-precache.js",
                "/offline.html",
                "/error")
            .contains(path);
  }
}
