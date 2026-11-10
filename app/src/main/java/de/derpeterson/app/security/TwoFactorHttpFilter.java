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
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    if (securityService.getAuthenticatedUser(httpRequest).isEmpty()) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    String servletPath = httpRequest.getServletPath();
    String vaadinRequestType = httpRequest.getParameter("v-r");
    boolean protocolRequest = isVaadinProtocolRequest(servletPath, vaadinRequestType);
    TwoFactorFlow twoFactorFlow = TwoFactorFlow.current(httpRequest);
    boolean accessConfirmed =
        twoFactorFlow == null && isAccessConfirmed(httpRequest, servletPath, protocolRequest);

    if (protocolRequest) {
      if (isVaadinServiceEndpoint(servletPath)
          && (accessConfirmed || TwoFactorTransport.allowedUi(httpRequest))) {
        filterChain.doFilter(servletRequest, servletResponse);
      } else {
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.setHeader("Cache-Control", "no-store");
        httpResponse
            .getWriter()
            .write(
                VaadinService.createCriticalNotificationJSON(
                    null, null, null, TwoFactorTransport.destination(httpRequest)));
      }
      return;
    }

    if (accessConfirmed
        || isPermittedRequest(httpRequest, servletPath, vaadinRequestType, twoFactorFlow)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    httpResponse.setHeader("Cache-Control", "no-store");
    if (vaadinRequestType == null && "GET".equals(httpRequest.getMethod())) {
      httpResponse.sendRedirect(TwoFactorTransport.destination(httpRequest));
    } else {
      httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  private boolean isVaadinProtocolRequest(String servletPath, String vaadinRequestType) {
    return "uidl".equals(vaadinRequestType)
        || "push".equals(vaadinRequestType)
        || "/VAADIN/uidl".equals(servletPath)
        || "/VAADIN/push".equals(servletPath);
  }

  private boolean isAccessConfirmed(
      HttpServletRequest httpRequest, String servletPath, boolean protocolRequest) {
    return twoFactorAuthorization.sessionConfirmed(httpRequest)
        && (!("/admin".equals(servletPath)
                || protocolRequest && TwoFactorTransport.administrativeUi(httpRequest))
            || twoFactorAuthorization.administrationConfirmed(httpRequest));
  }

  private boolean isPermittedRequest(
      HttpServletRequest httpRequest,
      String servletPath,
      String vaadinRequestType,
      TwoFactorFlow twoFactorFlow) {
    if (isVaadinServiceEndpoint(servletPath)
        && ("init".equals(vaadinRequestType) || "heartbeat".equals(vaadinRequestType))) {
      return true;
    }

    if (twoFactorFlow != null) {
      return vaadinRequestType == null
          && ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
          && (twoFactorFlow.path().equals(servletPath)
              || !twoFactorFlow.help()
                  && (servletPath.startsWith(TwoFactorDocuments.PATH + "/")
                      || RECOVERY_DOWNLOAD.matcher(servletPath).matches())
              || isPublicResourcePath(servletPath));
    }

    return vaadinRequestType == null
        && (AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE.equals(servletPath)
            || "/recovery-help".equals(servletPath)
            || "/logout".equals(servletPath)
            || "/login".equals(servletPath)
            || servletPath.startsWith("/oauth2/authorization/")
            || servletPath.startsWith("/login/oauth2/code/")
            || servletPath.startsWith(TwoFactorDocuments.PATH + "/")
            || ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
                && isPublicResourcePath(servletPath));
  }

  private boolean isVaadinServiceEndpoint(String servletPath) {
    return servletPath.isEmpty()
        || "/".equals(servletPath)
        || "/VAADIN/uidl".equals(servletPath)
        || "/VAADIN/push".equals(servletPath);
  }

  private boolean isPublicResourcePath(String servletPath) {
    if (servletPath.startsWith("/VAADIN/dynamic/")) {
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
            .anyMatch(servletPath::startsWith)
        || Set.of(
                "/favicon.ico",
                "/manifest.webmanifest",
                "/sw.js",
                "/sw-runtime-resources-precache.js",
                "/offline.html",
                "/error")
            .contains(servletPath);
  }
}
