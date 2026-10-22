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
  private final boolean exposed;
  private final Supplier<ServletRegistrationBean<?>> registrations;

  H2ConsoleOriginFilter(boolean exposed, Supplier<ServletRegistrationBean<?>> registrations) {
    this.exposed = exposed;
    this.registrations = registrations;
  }

  boolean isConsoleRequest(HttpServletRequest request) {
    ServletRegistrationBean<?> registration = registrations.get();
    if (registration == null || !registration.isEnabled()) {
      return false;
    }
    Servlet servlet = registration.getServlet();
    if (servlet == null
        || !"org.h2.server.web.JakartaWebServlet".equals(servlet.getClass().getName())) {
      return false;
    }
    HttpServletMapping mapping = request.getHttpServletMapping();
    // Actual container mapping, not a URL prefix or a client-controlled framework parameter.
    return mapping != null
        && registration.getServletName().equals(mapping.getServletName())
        && registration.getUrlMappings().contains(mapping.getPattern());
  }

  boolean allowsConsoleRequest(HttpServletRequest request) {
    return exposed
        && isConsoleRequest(request)
        && eligibleMapping(request)
        && trustedSource(request);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) request;
    if (isConsoleRequest(http)) {
      HttpServletResponse outgoing = (HttpServletResponse) response;
      outgoing.setHeader(CACHE_CONTROL, NO_STORE);
      outgoing.addHeader("Vary", "Sec-Fetch-Site, Origin, Referer");
      if (!exposed || !eligibleMapping(http) || !trustedSource(http)) {
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
      chain.doFilter(request, uncached);
      return;
    }
    chain.doFilter(request, response);
  }

  private boolean eligibleMapping(HttpServletRequest request) {
    String pattern = request.getHttpServletMapping().getPattern();
    return pattern.length() > 2
        && pattern.endsWith("/*")
        && !AppRoute.LOGIN.getSecurityRoute().equals(request.getServletPath())
        && !AppRoute.LOGOUT.getSecurityRoute().equals(request.getServletPath());
  }

  private boolean trustedSource(HttpServletRequest request) {
    // Avoid accepting arbitrary Host values (including DNS-rebinding origins) for this dev tool.
    if (!LOCAL_HOSTS.contains(
        unbracket(request.getServerName()).toLowerCase(java.util.Locale.ROOT))) {
      return false;
    }
    if (hasRepeatedSourceHeaders(request)) {
      return false;
    }
    String site = request.getHeader("Sec-Fetch-Site");
    if (site != null && !"same-origin".equals(site) && !"none".equals(site)) {
      return false;
    }
    String origin = request.getHeader("Origin");
    if (origin != null && !sameOrigin(request, origin, false)) {
      return false;
    }
    String referer = request.getHeader("Referer");
    if (referer != null && !sameOrigin(request, referer, true)) {
      return false;
    }
    if (origin != null || referer != null || "same-origin".equals(site)) {
      return true;
    }
    // Only the harmless entry document can be opened directly without source headers.
    // H2 also accepts mutating .do actions via GET, so do not exempt other GET requests.
    return ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
        && (request.getPathInfo() == null || "/".equals(request.getPathInfo()))
        && request.getQueryString() == null
        && request.getParameterMap().isEmpty();
  }

  private boolean hasRepeatedSourceHeaders(HttpServletRequest request) {
    for (String header : List.of("Origin", "Referer", "Sec-Fetch-Site")) {
      if (repeated(request, header)) {
        return true;
      }
    }
    return false;
  }

  private boolean repeated(HttpServletRequest request, String header) {
    Enumeration<String> values = request.getHeaders(header);
    if (!values.hasMoreElements()) {
      return false;
    }
    values.nextElement();
    return values.hasMoreElements();
  }

  private boolean sameOrigin(HttpServletRequest request, String value, boolean referer) {
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
      return scheme.equalsIgnoreCase(request.getScheme())
          && unbracket(source.getHost()).equalsIgnoreCase(unbracket(request.getServerName()))
          && port == request.getServerPort();
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
