package de.derpeterson.app.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/** Lokaler TLS-Abschluss am Proxy; nur dessen beim Start aufgelöste Adressen werden vertraut. */
@Configuration
@Profile("https")
public class HttpsTransportConfiguration {
  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpsProxy(
      @Value("${app.https.proxy-host:127.0.0.1}") String proxyHost) throws UnknownHostException {
    String addresses =
        Arrays.stream(InetAddress.getAllByName(proxyHost))
            .map(InetAddress::getHostAddress)
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));
    RemoteIpValve proxy = new RemoteIpValve();
    // Ausschließlich die aufgelösten wörtlichen IP-Adressen, getrennt mit Regex-ODER.
    // Pattern.quote schützt insbesondere Punkte und IPv6-Sonderzeichen vor Regex-Auswertung.
    proxy.setInternalProxies(addresses);
    proxy.setRemoteIpHeader("X-Forwarded-For");
    proxy.setProtocolHeader("X-Forwarded-Proto");
    proxy.setHostHeader("X-Forwarded-Host");
    proxy.setPortHeader("X-Forwarded-Port");
    return factory -> factory.addEngineValves(proxy);
  }

  @Bean
  public FilterRegistrationBean<Filter> httpsOrigin(
      @Value("${app.https.origin}") String configuredOrigin) {
    URI origin = URI.create(configuredOrigin);
    if (!"https".equals(origin.getScheme())
        || origin.getHost() == null
        || origin.getUserInfo() != null
        || origin.getQuery() != null
        || origin.getFragment() != null
        || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))) {
      throw new IllegalArgumentException("HTTPS origin must be an absolute HTTPS origin");
    }
    int port = origin.getPort() == -1 ? 443 : origin.getPort();
    Filter filter =
        (request, response, chain) -> {
          HttpServletRequest incoming = (HttpServletRequest) request;
          HttpServletResponse outgoing = (HttpServletResponse) response;
          // Nur der interne harmlose Livenessabruf darf ohne TLS zur Anwendung gelangen.
          if (!incoming.isSecure()
              && "/internal/health".equals(incoming.getRequestURI())
              && "GET".equals(incoming.getMethod())
              && incoming.getQueryString() == null) {
            outgoing.setContentType("text/plain");
            outgoing.getWriter().write("OK");
            return;
          }
          if (!incoming.isSecure()
              || !origin.getHost().equalsIgnoreCase(incoming.getServerName())
              || port != incoming.getServerPort()) {
            outgoing.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }
          chain.doFilter(request, response);
        };
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
    registration.setName("httpsOrigin");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
