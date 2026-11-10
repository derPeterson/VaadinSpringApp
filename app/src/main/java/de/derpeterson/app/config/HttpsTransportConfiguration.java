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
      @Value("${app.https.proxy-host:127.0.0.1}") String proxyHostName)
      throws UnknownHostException {
    String trustedProxyAddressPattern =
        Arrays.stream(InetAddress.getAllByName(proxyHostName))
            .map(InetAddress::getHostAddress)
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));

    RemoteIpValve remoteIpValve = new RemoteIpValve();
    // Ausschließlich die aufgelösten wörtlichen IP-Adressen, getrennt mit Regex-ODER.
    // Pattern.quote schützt insbesondere Punkte und IPv6-Sonderzeichen vor Regex-Auswertung.
    remoteIpValve.setInternalProxies(trustedProxyAddressPattern);
    remoteIpValve.setRemoteIpHeader("X-Forwarded-For");
    remoteIpValve.setProtocolHeader("X-Forwarded-Proto");
    remoteIpValve.setHostHeader("X-Forwarded-Host");
    remoteIpValve.setPortHeader("X-Forwarded-Port");

    return tomcatServerFactory -> tomcatServerFactory.addEngineValves(remoteIpValve);
  }

  @Bean
  public FilterRegistrationBean<Filter> httpsOrigin(
      @Value("${app.https.origin}") String configuredHttpsOriginText) {
    URI httpsOriginUri = URI.create(configuredHttpsOriginText);
    if (!"https".equals(httpsOriginUri.getScheme())
        || httpsOriginUri.getHost() == null
        || httpsOriginUri.getUserInfo() != null
        || httpsOriginUri.getQuery() != null
        || httpsOriginUri.getFragment() != null
        || !(httpsOriginUri.getPath().isEmpty() || "/".equals(httpsOriginUri.getPath()))) {
      throw new IllegalArgumentException("HTTPS origin must be an absolute HTTPS origin");
    }

    int httpsOriginPort = httpsOriginUri.getPort() == -1 ? 443 : httpsOriginUri.getPort();
    Filter httpsOriginFilter =
        (servletRequest, servletResponse, filterChain) -> {
          HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
          HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
          // Nur der interne harmlose Livenessabruf darf ohne TLS zur Anwendung gelangen.
          if (!httpRequest.isSecure()
              && "/internal/health".equals(httpRequest.getRequestURI())
              && "GET".equals(httpRequest.getMethod())
              && httpRequest.getQueryString() == null) {
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write("OK");
            return;
          }

          if (!httpRequest.isSecure()
              || !httpsOriginUri.getHost().equalsIgnoreCase(httpRequest.getServerName())
              || httpsOriginPort != httpRequest.getServerPort()) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
          }

          filterChain.doFilter(servletRequest, servletResponse);
        };

    FilterRegistrationBean<Filter> httpsFilterRegistration =
        new FilterRegistrationBean<>(httpsOriginFilter);
    httpsFilterRegistration.setName("httpsOrigin");
    httpsFilterRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return httpsFilterRegistration;
  }
}
