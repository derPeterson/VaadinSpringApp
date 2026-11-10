package de.derpeterson.app.security;

import static org.springframework.http.HttpHeaders.CACHE_CONTROL;

import de.derpeterson.app.model.enums.AppRoute;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

/** H2's native forms use origin protection, only on its explicitly exposed local servlet. */
final class H2ConsoleOriginFilter implements Filter {
  private static final String NO_STORE = "no-store";
  private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");
  private final boolean consoleExposed;
  private final Supplier<ServletRegistrationBean<?>> consoleRegistrationSupplier;

  H2ConsoleOriginFilter(
      boolean consoleExposed, Supplier<ServletRegistrationBean<?>> consoleRegistrationSupplier) {
    this.consoleExposed = consoleExposed;
    this.consoleRegistrationSupplier = consoleRegistrationSupplier;
  }

  boolean isConsoleRequest(HttpServletRequest httpRequest) {
    ServletRegistrationBean<?> consoleServletRegistration = consoleRegistrationSupplier.get();
    if (consoleServletRegistration == null || !consoleServletRegistration.isEnabled()) {
      return false;
    }

    Servlet registeredServlet = consoleServletRegistration.getServlet();
    if (registeredServlet == null
        || !"org.h2.server.web.JakartaWebServlet".equals(registeredServlet.getClass().getName())) {
      return false;
    }

    HttpServletMapping servletMapping = httpRequest.getHttpServletMapping();
    // Actual container mapping, not a URL prefix or a client-controlled framework parameter.
    return servletMapping != null
        && consoleServletRegistration.getServletName().equals(servletMapping.getServletName())
        && consoleServletRegistration.getUrlMappings().contains(servletMapping.getPattern());
  }

  boolean allowsConsoleRequest(HttpServletRequest httpRequest) {
    return consoleExposed
        && isConsoleRequest(httpRequest)
        && eligibleMapping(httpRequest)
        && trustedSource(httpRequest);
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

    if (isConsoleRequest(httpRequest)) {
      HttpServletResponse consoleResponse = (HttpServletResponse) servletResponse;
      consoleResponse.setHeader(CACHE_CONTROL, NO_STORE);
      consoleResponse.addHeader("Vary", "Sec-Fetch-Site, Origin, Referer");
      if (!consoleExposed || !eligibleMapping(httpRequest) || !trustedSource(httpRequest)) {
        consoleResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      // H2 sets its own cache headers. Its native session URLs and SQL results must not be stored.
      HttpServletResponseWrapper uncachedResponse =
          new HttpServletResponseWrapper(consoleResponse) {
            @Override
            public void setHeader(String name, String value) {
              super.setHeader(name, CACHE_CONTROL.equalsIgnoreCase(name) ? NO_STORE : value);
            }

            @Override
            public void addHeader(String name, String value) {
              if (CACHE_CONTROL.equalsIgnoreCase(name)) {
                super.setHeader(name, NO_STORE);
              } else {
                super.addHeader(name, value);
              }
            }
          };
      filterChain.doFilter(servletRequest, uncachedResponse);
      return;
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }

  private boolean eligibleMapping(HttpServletRequest httpRequest) {
    String servletPattern = httpRequest.getHttpServletMapping().getPattern();
    return servletPattern.length() > 2
        && servletPattern.endsWith("/*")
        && !AppRoute.LOGIN.getSecurityRoute().equals(httpRequest.getServletPath())
        && !AppRoute.LOGOUT.getSecurityRoute().equals(httpRequest.getServletPath());
  }

  private boolean trustedSource(HttpServletRequest httpRequest) {
    // Avoid accepting arbitrary Host values (including DNS-rebinding origins) for this dev tool.
    if (!LOCAL_HOSTS.contains(
        unbracket(httpRequest.getServerName()).toLowerCase(java.util.Locale.ROOT))) {
      return false;
    }
    if (hasRepeatedSourceHeaders(httpRequest)) {
      return false;
    }

    String fetchSiteHeader = httpRequest.getHeader("Sec-Fetch-Site");
    if (fetchSiteHeader != null
        && !"same-origin".equals(fetchSiteHeader)
        && !"none".equals(fetchSiteHeader)) {
      return false;
    }

    String originHeader = httpRequest.getHeader("Origin");
    if (originHeader != null && !sameOrigin(httpRequest, originHeader, false)) {
      return false;
    }

    String refererHeader = httpRequest.getHeader("Referer");
    if (refererHeader != null && !sameOrigin(httpRequest, refererHeader, true)) {
      return false;
    }

    if (originHeader != null || refererHeader != null || "same-origin".equals(fetchSiteHeader)) {
      return true;
    }
    // Only the harmless entry document can be opened directly without source headers.
    // H2 also accepts mutating .do actions via GET, so do not exempt other GET requests.
    return ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()))
        && (httpRequest.getPathInfo() == null || "/".equals(httpRequest.getPathInfo()))
        && httpRequest.getQueryString() == null
        && httpRequest.getParameterMap().isEmpty();
  }

  private boolean hasRepeatedSourceHeaders(HttpServletRequest httpRequest) {
    for (String header : List.of("Origin", "Referer", "Sec-Fetch-Site")) {
      if (repeated(httpRequest, header)) {
        return true;
      }
    }
    return false;
  }

  private boolean repeated(HttpServletRequest httpRequest, String headerName) {
    Enumeration<String> headerValues = httpRequest.getHeaders(headerName);
    if (!headerValues.hasMoreElements()) {
      return false;
    }

    headerValues.nextElement();
    return headerValues.hasMoreElements();
  }

  private boolean sameOrigin(
      HttpServletRequest httpRequest, String sourceHeaderValue, boolean refererProvided) {
    try {
      URI sourceUri = new URI(sourceHeaderValue);
      String sourceScheme = sourceUri.getScheme();
      if (sourceScheme == null
          || (!"http".equalsIgnoreCase(sourceScheme) && !"https".equalsIgnoreCase(sourceScheme))
          || sourceUri.getHost() == null
          || sourceUri.getRawUserInfo() != null
          || sourceUri.getRawFragment() != null
          || (!refererProvided
              && (sourceUri.getRawQuery() != null || !sourceUri.getRawPath().isEmpty()))) {
        return false;
      }

      int sourcePort = sourceUri.getPort();
      if (sourcePort == -1) {
        sourcePort = "https".equalsIgnoreCase(sourceScheme) ? 443 : 80;
      }

      return sourceScheme.equalsIgnoreCase(httpRequest.getScheme())
          && unbracket(sourceUri.getHost()).equalsIgnoreCase(unbracket(httpRequest.getServerName()))
          && sourcePort == httpRequest.getServerPort();
    } catch (URISyntaxException _) {
      return false;
    }
  }

  private String unbracket(String host) {
    if (host.startsWith("[") && host.endsWith("]")) {
      return host.substring(1, host.length() - 1);
    }
    return host;
  }
}
