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
    ServletRegistrationBean<?> registration = consoleRegistrationSupplier.get();
    if (registration == null || !registration.isEnabled()) {
      return false;
    }
    Servlet servlet = registration.getServlet();
    if (servlet == null
        || !"org.h2.server.web.JakartaWebServlet".equals(servlet.getClass().getName())) {
      return false;
    }
    HttpServletMapping mapping = httpRequest.getHttpServletMapping();
    // Actual container mapping, not a URL prefix or a client-controlled framework parameter.
    return mapping != null
        && registration.getServletName().equals(mapping.getServletName())
        && registration.getUrlMappings().contains(mapping.getPattern());
  }

  boolean allowsConsoleRequest(HttpServletRequest httpRequest) {
    return consoleExposed
        && isConsoleRequest(httpRequest)
        && eligibleMapping(httpRequest)
        && trustedSource(httpRequest);
  }

  @Override
  public void doFilter(
      ServletRequest httpRequest, ServletResponse httpResponse, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) httpRequest;
    if (isConsoleRequest(http)) {
      HttpServletResponse outgoing = (HttpServletResponse) httpResponse;
      outgoing.setHeader(CACHE_CONTROL, NO_STORE);
      outgoing.addHeader("Vary", "Sec-Fetch-Site, Origin, Referer");
      if (!consoleExposed || !eligibleMapping(http) || !trustedSource(http)) {
        outgoing.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      // H2 sets its own cache headers. Its native session URLs and SQL results must not be stored.
      HttpServletResponseWrapper uncached =
          new HttpServletResponseWrapper(outgoing) {
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
      filterChain.doFilter(httpRequest, uncached);
      return;
    }
    filterChain.doFilter(httpRequest, httpResponse);
  }

  private boolean eligibleMapping(HttpServletRequest httpRequest) {
    String pattern = httpRequest.getHttpServletMapping().getPattern();
    return pattern.length() > 2
        && pattern.endsWith("/*")
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
    String site = httpRequest.getHeader("Sec-Fetch-Site");
    if (site != null && !"same-origin".equals(site) && !"none".equals(site)) {
      return false;
    }
    String origin = httpRequest.getHeader("Origin");
    if (origin != null && !sameOrigin(httpRequest, origin, false)) {
      return false;
    }
    String referer = httpRequest.getHeader("Referer");
    if (referer != null && !sameOrigin(httpRequest, referer, true)) {
      return false;
    }
    if (origin != null || referer != null || "same-origin".equals(site)) {
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

  private boolean repeated(HttpServletRequest httpRequest, String header) {
    Enumeration<String> values = httpRequest.getHeaders(header);
    if (!values.hasMoreElements()) {
      return false;
    }
    values.nextElement();
    return values.hasMoreElements();
  }

  private boolean sameOrigin(HttpServletRequest httpRequest, String value, boolean referer) {
    try {
      URI source = new URI(value);
      String scheme = source.getScheme();
      if (scheme == null
          || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
          || source.getHost() == null
          || source.getRawUserInfo() != null
          || source.getRawFragment() != null
          || (!referer && (source.getRawQuery() != null || !source.getRawPath().isEmpty()))) {
        return false;
      }
      int port = source.getPort();
      if (port == -1) {
        port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
      }
      return scheme.equalsIgnoreCase(httpRequest.getScheme())
          && unbracket(source.getHost()).equalsIgnoreCase(unbracket(httpRequest.getServerName()))
          && port == httpRequest.getServerPort();
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
