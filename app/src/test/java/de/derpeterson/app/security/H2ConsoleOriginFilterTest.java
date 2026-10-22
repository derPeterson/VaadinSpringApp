package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.MappingMatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.server.web.JakartaWebServlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class H2ConsoleOriginFilterTest {
  private final ServletRegistrationBean<JakartaWebServlet> registration = registration();
  private final H2ConsoleOriginFilter filter = new H2ConsoleOriginFilter(true, () -> registration);

  private ServletRegistrationBean<JakartaWebServlet> registration() {
    ServletRegistrationBean<JakartaWebServlet> result =
        new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2/*");
    result.setName("h2Console");
    return result;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://evil.test",
        "http://localhost.evil.test",
        "https://localhost",
        "http://localhost:81",
        "http://127.0.0.1",
        "null",
        "",
        "http://localhost/path",
        "http://localhost?x=1",
        "http://localhost#fragment",
        "http://user@localhost",
        "http://localhost http://evil.test",
        "http://[invalid",
        "/localhost",
        "file://localhost"
      })
  void invalidOriginCannotReachNativeConsole(String origin) throws Exception {
    MockHttpServletRequest request = request("POST", "/login.do");
    request.addHeader("Origin", origin);
    assertDenied(request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"same-site", "cross-site", "unknown", "same-origin, cross-site", ""})
  void untrustedFetchContextCannotOverrideMatchingOrigin(String site) throws Exception {
    MockHttpServletRequest request = request("GET", "/query.do");
    request.addHeader("Sec-Fetch-Site", site);
    request.addHeader("Origin", "http://localhost");
    assertDenied(request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Origin", "Referer", "Sec-Fetch-Site"})
  void duplicateSecurityHeadersFailClosed(String header) throws Exception {
    MockHttpServletRequest request = request("POST", "/login.do");
    String value = header.equals("Sec-Fetch-Site") ? "same-origin" : "http://localhost";
    request.addHeader(header, value);
    request.addHeader(header, value);
    assertDenied(request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Origin", "Referer", "Sec-Fetch-Site"})
  void nativePostWorksWithVerifiedBrowserSource(String header) throws Exception {
    MockHttpServletRequest request = request("POST", "/login.do");
    String value = "http://localhost";
    if (header.equals("Referer")) {
      value += "/h2/login.jsp?jsessionid=own-test-session";
    } else if (header.equals("Sec-Fetch-Site")) {
      value = "same-origin";
    }
    request.addHeader(header, value);
    assertTrue(filter.allowsConsoleRequest(request));
    AtomicInteger reached = new AtomicInteger();
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, (incoming, outgoing) -> reached.incrementAndGet());
    assertEquals(1, reached.get());
    assertEquals("no-store", response.getHeader("Cache-Control"));
    assertTrue(response.getHeaders("Vary").contains("Sec-Fetch-Site, Origin, Referer"));
  }

  @Test
  void missingOriginCannotMutateEvenThroughGetOrForgedFrameworkMarker() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
      MockHttpServletRequest request = request(method, "/query.do");
      request.setParameter("v-r", "uidl");
      assertDenied(request);
    }
  }

  @Test
  void directEntryIsAllowedButSourceLessDeepLinksAndQueryParametersAreNot() throws Exception {
    MockHttpServletRequest entry = request("GET", "/");
    assertTrue(filter.allowsConsoleRequest(entry));
    entry.addHeader("Sec-Fetch-Site", "none");
    assertTrue(filter.allowsConsoleRequest(entry));
    entry.setParameter("jsessionid", "not-an-entry");
    assertDenied(entry);
    assertDenied(request("GET", "/logout.do"));
    assertDenied(request("POST", "/"));
  }

  @Test
  void fetchHeaderDoesNotHideConflictingOriginOrReferer() throws Exception {
    MockHttpServletRequest request = request("POST", "/login.do");
    request.addHeader("Sec-Fetch-Site", "same-origin");
    request.addHeader("Origin", "http://localhost");
    request.addHeader("Referer", "http://evil.test/h2/");
    assertDenied(request);
  }

  @Test
  void defaultPortsIpv6ContextAndCustomizedServletMappingWork() {
    MockHttpServletRequest request = request("POST", "/login.do");
    request.setScheme("https");
    request.setServerPort(443);
    request.addHeader("Origin", "https://localhost");
    assertTrue(filter.allowsConsoleRequest(request));
    request.setServerName("::1");
    request.removeHeader("Origin");
    request.addHeader("Origin", "https://[::1]:443");
    assertTrue(filter.allowsConsoleRequest(request));
    registration.setUrlMappings(java.util.List.of("/console/*"));
    request.setRequestURI("/app/console/login.do");
    request.setContextPath("/app");
    request.setServletPath("/console");
    request.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/console/*", "h2Console", MappingMatch.PATH));
    assertTrue(filter.allowsConsoleRequest(request));
  }

  @Test
  void flagsActualServletAndReservedApplicationEndpointsAreRequired() {
    MockHttpServletRequest request = request("POST", "/login.do");
    request.addHeader("Origin", "http://localhost");
    assertFalse(new H2ConsoleOriginFilter(false, () -> registration).allowsConsoleRequest(request));
    assertFalse(new H2ConsoleOriginFilter(true, () -> null).isConsoleRequest(request));
    registration.setEnabled(false);
    assertFalse(filter.isConsoleRequest(request));
    registration.setEnabled(true);
    request.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/h2/*", "otherServlet", MappingMatch.PATH));
    assertFalse(filter.isConsoleRequest(request));
    request.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/h2-spoof/*", "h2Console", MappingMatch.PATH));
    assertFalse(filter.isConsoleRequest(request));
    request.setHttpServletMapping(
        new MockHttpServletMapping("login", "/h2/*", "h2Console", MappingMatch.PATH));
    request.setServletPath("/login");
    assertFalse(filter.allowsConsoleRequest(request));
    request.setServletPath("/logout");
    assertFalse(filter.allowsConsoleRequest(request));
  }

  @Test
  void nativeConsoleSubclassDoesNotReceiveTheExactServletExemption() {
    ServletRegistrationBean<JakartaWebServlet> alternative =
        new ServletRegistrationBean<>(new JakartaWebServlet() {}, "/h2/*");
    alternative.setName("h2Console");
    H2ConsoleOriginFilter exactType = new H2ConsoleOriginFilter(true, () -> alternative);
    MockHttpServletRequest request = request("POST", "/login.do");
    request.addHeader("Origin", "http://localhost");
    assertFalse(exactType.isConsoleRequest(request));
    assertFalse(exactType.allowsConsoleRequest(request));
  }

  @Test
  void registrationWithoutServletDoesNotGrantAConsoleExemption() throws Exception {
    ServletRegistrationBean<JakartaWebServlet> incomplete = new ServletRegistrationBean<>();
    incomplete.setName("h2Console");
    incomplete.setUrlMappings(java.util.List.of("/h2/*"));
    H2ConsoleOriginFilter incompleteFilter = new H2ConsoleOriginFilter(true, () -> incomplete);
    MockHttpServletRequest request = request("POST", "/login.do");
    request.addHeader("Origin", "http://localhost");
    assertFalse(incompleteFilter.isConsoleRequest(request));
    assertFalse(incompleteFilter.allowsConsoleRequest(request));
    AtomicInteger continued = new AtomicInteger();
    incompleteFilter.doFilter(
        request,
        new MockHttpServletResponse(),
        (incoming, outgoing) -> continued.incrementAndGet());
    assertEquals(1, continued.get(), "The normal application security chain must continue");
  }

  @Test
  void disabledExposureRejectsNativeEntryEvenAtCustomPath() throws Exception {
    registration.setUrlMappings(java.util.List.of("/console/*"));
    MockHttpServletRequest request = request("GET", "/");
    request.setServletPath("/console");
    request.setHttpServletMapping(
        new MockHttpServletMapping("", "/console/*", "h2Console", MappingMatch.PATH));
    H2ConsoleOriginFilter closed = new H2ConsoleOriginFilter(false, () -> registration);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);
    closed.doFilter(request, response, servlet);
    assertEquals(403, response.getStatus());
    verifyNoInteractions(servlet);
  }

  @Test
  void rootMappedConsoleFailsClosedInsteadOfExemptingApplication() throws Exception {
    registration.setUrlMappings(java.util.List.of("/*"));
    MockHttpServletRequest request = request("POST", "/login.do");
    request.setServletPath("");
    request.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/*", "h2Console", MappingMatch.PATH));
    request.addHeader("Origin", "http://localhost");
    assertDenied(request);
  }

  @Test
  void arbitraryTargetHostIsDeniedEvenWithMatchingOrigin() throws Exception {
    MockHttpServletRequest request = request("POST", "/login.do");
    request.setServerName("rebound.example");
    request.addHeader("Origin", "http://rebound.example");
    assertDenied(request);
  }

  @Test
  void ordinaryApplicationRequestsAreNotHandledByConsoleFilter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
    request.setServletPath("/login");
    AtomicInteger reached = new AtomicInteger();
    filter.doFilter(
        request, new MockHttpServletResponse(), (incoming, outgoing) -> reached.incrementAndGet());
    assertEquals(1, reached.get());
    assertFalse(filter.allowsConsoleRequest(request));
  }

  private void assertDenied(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);
    assertFalse(filter.allowsConsoleRequest(request));
    filter.doFilter(request, response, servlet);
    assertEquals(403, response.getStatus());
    verifyNoInteractions(servlet);
  }

  private MockHttpServletRequest request(String method, String pathInfo) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/h2" + pathInfo);
    request.setServletPath("/h2");
    request.setPathInfo(pathInfo);
    request.setHttpServletMapping(
        new MockHttpServletMapping(pathInfo.substring(1), "/h2/*", "h2Console", MappingMatch.PATH));
    return request;
  }
}
