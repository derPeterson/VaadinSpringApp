package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.h2.server.web.JakartaWebServlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Real H2 servlet + actual SecurityConfig over loopback; only own in-memory data and temporary
 * files.
 */
@SpringJUnitConfig(H2ConsoleHttpTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "app.security.expose-h2-console=true")
class H2ConsoleHttpTest {
  @Autowired SecurityFilterChain security;
  @Autowired ServletRegistrationBean<JakartaWebServlet> console;
  @TempDir Path temporary;
  private Tomcat server;
  private Connection database;
  private HttpClient client;
  private String origin;
  private String base;
  private String databaseUrl;
  private String previousCatalinaBase;
  private String previousCatalinaHome;

  @BeforeEach
  void start() throws Exception {
    databaseUrl = "jdbc:h2:mem:console-http-" + UUID.randomUUID();
    database = DriverManager.getConnection(databaseUrl, "sa", "");
    try (Statement statement = database.createStatement()) {
      statement.execute("create table console_probe (message varchar(80))");
    }
    previousCatalinaBase = System.getProperty("catalina.base");
    previousCatalinaHome = System.getProperty("catalina.home");
    server = new Tomcat();
    server.setBaseDir(temporary.resolve("tomcat").toString());
    server.setPort(0);
    server.getConnector().setProperty("address", "127.0.0.1");
    Context context = server.addContext("/app", temporary.toString());
    context.setParentClassLoader(getClass().getClassLoader());
    Wrapper h2 = Tomcat.addServlet(context, console.getServletName(), console.getServlet());
    // Never read or write the user's ~/.h2.server.properties, query history or saved connections.
    h2.addInitParameter("properties", "null");
    context.addServletMappingDecoded("/h2/*", console.getServletName());
    Tomcat.addServlet(
        context,
        "application",
        new HttpServlet() {
          @Override
          protected void doGet(HttpServletRequest request, HttpServletResponse response)
              throws IOException {
            response.getWriter().write("application reached");
          }
        });
    context.addServletMappingDecoded("/", "application");
    FilterDef definition = new FilterDef();
    definition.setFilterName("security");
    definition.setFilter(new FilterChainProxy(security));
    context.addFilterDef(definition);
    FilterMap mapping = new FilterMap();
    mapping.setFilterName("security");
    mapping.addURLPattern("/*");
    mapping.setDispatcher(DispatcherType.REQUEST.name());
    context.addFilterMap(mapping);
    server.start();
    origin = "http://127.0.0.1:" + server.getConnector().getLocalPort();
    base = origin + "/app";
    client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  @AfterEach
  void stop() throws Exception {
    try {
      if (client != null) {
        client.close();
      }
    } finally {
      try {
        if (server != null) {
          server.stop();
          server.destroy();
        }
      } finally {
        if (database != null) {
          database.close();
        }
        restoreProperty("catalina.base", previousCatalinaBase);
        restoreProperty("catalina.home", previousCatalinaHome);
        SecurityContextHolder.clearContext();
      }
    }
  }

  @Test
  void nativeLoginQueryAndLogoutWorkThroughRealSecurityChainWithoutSpringToken() throws Exception {
    String session = login();
    HttpResponse<String> inserted =
        post(
            "/h2/query.do?jsessionid=" + session,
            Map.of("sql", "insert into console_probe values ('H2_NATIVE_SQL_OK')"),
            Map.of("Origin", origin));
    assertEquals(200, inserted.statusCode());
    assertEquals(1, rows());
    HttpResponse<String> selected =
        post(
            "/h2/query.do?jsessionid=" + session,
            Map.of("sql", "select message from console_probe"),
            Map.of("Referer", base + "/h2/query.jsp?jsessionid=" + session));
    assertEquals(200, selected.statusCode());
    assertTrue(selected.body().contains("H2_NATIVE_SQL_OK"));
    assertEquals("no-store", selected.headers().firstValue("Cache-Control").orElseThrow());
    assertEquals("SAMEORIGIN", selected.headers().firstValue("X-Frame-Options").orElseThrow());
    HttpResponse<String> logout =
        get("/h2/logout.do?jsessionid=" + session, Map.of("Sec-Fetch-Site", "same-origin"));
    assertEquals(200, logout.statusCode());
    assertTrue(logout.body().contains("login.jsp?jsessionid=" + session));
    HttpResponse<String> loginPage =
        get("/h2/login.jsp?jsessionid=" + session, Map.of("Referer", base + "/h2/index.do"));
    assertTrue(loginPage.body().contains("name=\"login\""));
    post(
        "/h2/query.do?jsessionid=" + session,
        Map.of("sql", "insert into console_probe values ('after logout')"),
        Map.of("Origin", origin));
    assertEquals(1, rows());
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "POST"})
  void crossOriginCannotExecuteSqlEvenWithKnownConsoleSession(String method) throws Exception {
    String session = login();
    String path = "/h2/query.do?jsessionid=" + session;
    String sql = "insert into console_probe values ('forbidden')";
    HttpResponse<String> rejected;
    if (method.equals("GET")) {
      rejected =
          get(
              path + "&sql=" + encode(sql),
              Map.of("Referer", "http://evil.test/", "Sec-Fetch-Site", "cross-site"));
    } else {
      rejected =
          post(
              path,
              Map.of("sql", sql),
              Map.of("Origin", "http://evil.test", "Sec-Fetch-Site", "cross-site"));
    }
    assertEquals(403, rejected.statusCode());
    assertEquals(0, rows());
    HttpResponse<String> allowed = post(path, Map.of("sql", sql), Map.of("Origin", origin));
    assertEquals(200, allowed.statusCode());
    assertEquals(1, rows());
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "POST"})
  void sourceLessSqlIsDeniedWithoutAffectingLiveConsole(String method) throws Exception {
    String session = login();
    String path = "/h2/query.do?jsessionid=" + session;
    HttpResponse<String> response;
    if (method.equals("GET")) {
      response = get(path + "&sql=" + encode("delete from console_probe"), Map.of());
    } else {
      response = post(path, Map.of("sql", "delete from console_probe"), Map.of());
    }
    assertEquals(403, response.statusCode());
    assertEquals(0, rows());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/login", "/logout", "/login?v-r=uidl", "/h2-spoof/login.do"})
  void consoleSourceHeadersCannotBypassApplicationCsrf(String path) throws Exception {
    HttpResponse<String> response =
        post(
            path,
            Map.of("username", "unused@example.test", "password", "unused"),
            Map.of("Origin", origin, "Sec-Fetch-Site", "same-origin"));
    assertEquals(403, response.statusCode());
    assertEquals(
        "SAMEORIGIN",
        get("/login", Map.of()).headers().firstValue("X-Frame-Options").orElseThrow());
  }

  private String login() throws Exception {
    HttpResponse<String> entry = get("/h2/", Map.of());
    assertEquals(200, entry.statusCode());
    // Sucht die H2-Sitzungskennung im nativen Formular: Hexziffern, eine Fanggruppe, ohne Anker.
    Matcher identifier = Pattern.compile("jsessionid=([a-f0-9]+)").matcher(entry.body());
    assertTrue(identifier.find());
    String session = identifier.group(1);
    HttpResponse<String> login =
        post(
            "/h2/login.do?jsessionid=" + session,
            Map.of("driver", "org.h2.Driver", "url", databaseUrl, "user", "sa", "password", ""),
            Map.of("Origin", origin));
    assertEquals(200, login.statusCode());
    return session;
  }

  private int rows() throws Exception {
    try (Statement statement = database.createStatement();
        ResultSet result = statement.executeQuery("select count(*) from console_probe")) {
      assertTrue(result.next());
      return result.getInt(1);
    }
  }

  private HttpResponse<String> post(
      String path, Map<String, String> form, Map<String, String> headers) throws Exception {
    String body =
        form.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body));
    headers.forEach(request::header);
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path, Map<String, String> headers) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).GET();
    headers.forEach(request::header);
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  @Configuration
  @Import(SecurityChainRegressionTest.Config.class)
  static class Config {
    @Bean
    ServletRegistrationBean<JakartaWebServlet> h2Console() {
      ServletRegistrationBean<JakartaWebServlet> registration =
          new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2/*");
      registration.setName("h2Console");
      return registration;
    }
  }
}
