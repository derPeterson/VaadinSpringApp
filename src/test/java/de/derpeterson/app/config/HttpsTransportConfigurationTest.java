package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.security.session.SessionPersistenceConfiguration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

/** Echter isolierter Tomcat: Peer-Vertrauen vor den Servletfiltern und der Sicherheitskette. */
class HttpsTransportConfigurationTest {
  @TempDir static Path directory;
  private static WebServer server;

  @BeforeAll
  static void start() throws Exception {
    HttpsTransportConfiguration config = new HttpsTransportConfiguration();
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    factory.setAddress(InetAddress.getByName("127.0.0.1"));
    factory.setBaseDirectory(directory.toFile());
    config.httpsProxy("127.0.0.1").customize(factory);
    server =
        factory.getWebServer(
            context -> {
              config.httpsOrigin("https://localhost:8443").onStartup(context);
              context
                  .addServlet(
                      "probe",
                      new HttpServlet() {
                        @Override
                        protected void service(
                            HttpServletRequest request, HttpServletResponse response)
                            throws IOException {
                          response
                              .getWriter()
                              .write(
                                  request.getScheme()
                                      + "://"
                                      + request.getServerName()
                                      + ":"
                                      + request.getServerPort());
                        }
                      })
                  .addMapping("/*");
            });
    server.start();
  }

  @AfterAll
  static void stop() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void exactProxyCanSupplyTheExternalHttpsOrigin() throws Exception {
    String response = exchange("127.0.0.1", "GET", "/login", forwarded("localhost", "8443"));
    assertTrue(response.startsWith("HTTP/1.1 200"), response);
    assertTrue(response.endsWith("https://localhost:8443"), response);
  }

  @Test
  void otherLoopbackPeerCannotForgeForwardedHttps() throws Exception {
    String response = exchange("127.0.0.2", "POST", "/login", forwarded("localhost", "8443"));
    assertTrue(response.startsWith("HTTP/1.1 403"), response);
  }

  @Test
  void directHttpAndForeignOriginCannotReachTheApplication() throws Exception {
    assertTrue(exchange("127.0.0.1", "POST", "/login", "").startsWith("HTTP/1.1 403"));
    assertTrue(
        exchange("127.0.0.1", "GET", "/login", forwarded("evil.test", "8443"))
            .startsWith("HTTP/1.1 403"));
    assertTrue(
        exchange("127.0.0.1", "GET", "/login", forwarded("localhost", "443"))
            .startsWith("HTTP/1.1 403"));
    assertTrue(
        exchange("127.0.0.1", "GET", "/login", "Forwarded: proto=https;host=localhost:8443\r\n")
            .startsWith("HTTP/1.1 403"));
  }

  @Test
  void onlyHarmlessExactInternalHealthRequestBypassesHttps() throws Exception {
    assertTrue(exchange("127.0.0.1", "GET", "/internal/health", "").endsWith("OK"));
    assertTrue(exchange("127.0.0.1", "POST", "/internal/health", "").startsWith("HTTP/1.1 403"));
    assertTrue(
        exchange("127.0.0.1", "GET", "/internal/health?change=true", "")
            .startsWith("HTTP/1.1 403"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:8443",
        "https://user@localhost",
        "https://localhost/path",
        "https://localhost?x=1",
        "https://localhost#fragment"
      })
  void invalidExternalOriginsFailConfiguration(String origin) {
    HttpsTransportConfiguration configuration = new HttpsTransportConfiguration();
    assertThrows(IllegalArgumentException.class, () -> configuration.httpsOrigin(origin));
  }

  @Test
  void httpsProfileUsesTheSameExternalOriginForMailDefaults() throws Exception {
    MockEnvironment environment =
        new MockEnvironment().withProperty("APP_HTTPS_ORIGIN", "https://127.0.0.1:28443");
    environment
        .getPropertySources()
        .addLast(new ResourcePropertySource("classpath:application-https.properties"));
    assertEquals(
        "https://127.0.0.1:28443/", environment.getProperty("app.config.defaults.base.url"));
  }

  @Test
  void sessionCookieUsesSecureHttpOnlyAndSameSiteOnHttps() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new SessionPersistenceConfiguration()
        .cookieSerializer()
        .writeCookieValue(new CookieValue(request, response, "owned-test-session"));
    String cookie = response.getHeader("Set-Cookie");
    assertTrue(cookie.contains("Secure"));
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Lax"));
  }

  private static String forwarded(String host, String port) {
    return "X-Forwarded-For: 192.0.2.10\r\nX-Forwarded-Proto: https\r\nX-Forwarded-Host: "
        + host
        + "\r\nX-Forwarded-Port: "
        + port
        + "\r\n";
  }

  private String exchange(String source, String method, String path, String headers)
      throws Exception {
    try (Socket socket = new Socket()) {
      socket.bind(new InetSocketAddress(InetAddress.getByName(source), 0));
      socket.connect(new InetSocketAddress("127.0.0.1", server.getPort()));
      socket.setSoTimeout(5000);
      socket
          .getOutputStream()
          .write(
              (method
                      + " "
                      + path
                      + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\nContent-Length: 0\r\n"
                      + headers
                      + "\r\n")
                  .getBytes(StandardCharsets.US_ASCII));
      return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
