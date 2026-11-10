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
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.addHeader("Origin", origin);
    assertDenied(mockHttpRequest);
  }

  @ParameterizedTest
  @ValueSource(strings = {"same-site", "cross-site", "unknown", "same-origin, cross-site", ""})
  void untrustedFetchContextCannotOverrideMatchingOrigin(String site) throws Exception {
    MockHttpServletRequest mockHttpRequest = request("GET", "/query.do");
    mockHttpRequest.addHeader("Sec-Fetch-Site", site);
    mockHttpRequest.addHeader("Origin", "http://localhost");
    assertDenied(mockHttpRequest);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Origin", "Referer", "Sec-Fetch-Site"})
  void duplicateSecurityHeadersFailClosed(String header) throws Exception {
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    String value = header.equals("Sec-Fetch-Site") ? "same-origin" : "http://localhost";
    mockHttpRequest.addHeader(header, value);
    mockHttpRequest.addHeader(header, value);
    assertDenied(mockHttpRequest);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Origin", "Referer", "Sec-Fetch-Site"})
  void nativePostWorksWithVerifiedBrowserSource(String header) throws Exception {
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    String value = "http://localhost";
    if (header.equals("Referer")) {
      value += "/h2/login.jsp?jsessionid=own-test-session";
    } else if (header.equals("Sec-Fetch-Site")) {
      value = "same-origin";
    }
    mockHttpRequest.addHeader(header, value);

    assertTrue(filter.allowsConsoleRequest(mockHttpRequest));
    AtomicInteger reached = new AtomicInteger();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    filter.doFilter(
        mockHttpRequest, mockHttpResponse, (incoming, outgoing) -> reached.incrementAndGet());

    assertEquals(1, reached.get());
    assertEquals("no-store", mockHttpResponse.getHeader("Cache-Control"));
    assertTrue(mockHttpResponse.getHeaders("Vary").contains("Sec-Fetch-Site, Origin, Referer"));
  }

  @Test
  void missingOriginCannotMutateEvenThroughGetOrForgedFrameworkMarker() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
      MockHttpServletRequest mockHttpRequest = request(method, "/query.do");
      mockHttpRequest.setParameter("v-r", "uidl");
      assertDenied(mockHttpRequest);
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
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.addHeader("Sec-Fetch-Site", "same-origin");
    mockHttpRequest.addHeader("Origin", "http://localhost");
    mockHttpRequest.addHeader("Referer", "http://evil.test/h2/");
    assertDenied(mockHttpRequest);
  }

  @Test
  void defaultPortsIpv6ContextAndCustomizedServletMappingWork() {
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.setScheme("https");
    mockHttpRequest.setServerPort(443);
    mockHttpRequest.addHeader("Origin", "https://localhost");

    assertTrue(filter.allowsConsoleRequest(mockHttpRequest));
    mockHttpRequest.setServerName("::1");
    mockHttpRequest.removeHeader("Origin");
    mockHttpRequest.addHeader("Origin", "https://[::1]:443");

    assertTrue(filter.allowsConsoleRequest(mockHttpRequest));
    registration.setUrlMappings(java.util.List.of("/console/*"));
    mockHttpRequest.setRequestURI("/app/console/login.do");
    mockHttpRequest.setContextPath("/app");
    mockHttpRequest.setServletPath("/console");
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/console/*", "h2Console", MappingMatch.PATH));

    assertTrue(filter.allowsConsoleRequest(mockHttpRequest));
  }

  @Test
  void flagsActualServletAndReservedApplicationEndpointsAreRequired() {
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.addHeader("Origin", "http://localhost");

    assertFalse(
        new H2ConsoleOriginFilter(false, () -> registration).allowsConsoleRequest(mockHttpRequest));
    assertFalse(new H2ConsoleOriginFilter(true, () -> null).isConsoleRequest(mockHttpRequest));
    registration.setEnabled(false);

    assertFalse(filter.isConsoleRequest(mockHttpRequest));
    registration.setEnabled(true);
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/h2/*", "otherServlet", MappingMatch.PATH));

    assertFalse(filter.isConsoleRequest(mockHttpRequest));
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/h2-spoof/*", "h2Console", MappingMatch.PATH));

    assertFalse(filter.isConsoleRequest(mockHttpRequest));
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("login", "/h2/*", "h2Console", MappingMatch.PATH));
    mockHttpRequest.setServletPath("/login");

    assertFalse(filter.allowsConsoleRequest(mockHttpRequest));
    mockHttpRequest.setServletPath("/logout");

    assertFalse(filter.allowsConsoleRequest(mockHttpRequest));
  }

  @Test
  void nativeConsoleSubclassDoesNotReceiveTheExactServletExemption() {
    ServletRegistrationBean<JakartaWebServlet> alternative =
        new ServletRegistrationBean<>(new JakartaWebServlet() {}, "/h2/*");
    alternative.setName("h2Console");
    H2ConsoleOriginFilter exactType = new H2ConsoleOriginFilter(true, () -> alternative);
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.addHeader("Origin", "http://localhost");

    assertFalse(exactType.isConsoleRequest(mockHttpRequest));
    assertFalse(exactType.allowsConsoleRequest(mockHttpRequest));
  }

  @Test
  void registrationWithoutServletDoesNotGrantAConsoleExemption() throws Exception {
    ServletRegistrationBean<JakartaWebServlet> incomplete = new ServletRegistrationBean<>();
    incomplete.setName("h2Console");
    incomplete.setUrlMappings(java.util.List.of("/h2/*"));
    H2ConsoleOriginFilter incompleteFilter = new H2ConsoleOriginFilter(true, () -> incomplete);
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.addHeader("Origin", "http://localhost");

    assertFalse(incompleteFilter.isConsoleRequest(mockHttpRequest));
    assertFalse(incompleteFilter.allowsConsoleRequest(mockHttpRequest));
    AtomicInteger continued = new AtomicInteger();
    incompleteFilter.doFilter(
        mockHttpRequest,
        new MockHttpServletResponse(),
        (incoming, outgoing) -> continued.incrementAndGet());

    assertEquals(1, continued.get(), "The normal application security chain must continue");
  }

  @Test
  void disabledExposureRejectsNativeEntryEvenAtCustomPath() throws Exception {
    registration.setUrlMappings(java.util.List.of("/console/*"));
    MockHttpServletRequest mockHttpRequest = request("GET", "/");
    mockHttpRequest.setServletPath("/console");
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("", "/console/*", "h2Console", MappingMatch.PATH));
    H2ConsoleOriginFilter closed = new H2ConsoleOriginFilter(false, () -> registration);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);
    closed.doFilter(mockHttpRequest, mockHttpResponse, servlet);

    assertEquals(403, mockHttpResponse.getStatus());
    verifyNoInteractions(servlet);
  }

  @Test
  void rootMappedConsoleFailsClosedInsteadOfExemptingApplication() throws Exception {
    registration.setUrlMappings(java.util.List.of("/*"));
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.setServletPath("");
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping("login.do", "/*", "h2Console", MappingMatch.PATH));
    mockHttpRequest.addHeader("Origin", "http://localhost");
    assertDenied(mockHttpRequest);
  }

  @Test
  void arbitraryTargetHostIsDeniedEvenWithMatchingOrigin() throws Exception {
    MockHttpServletRequest mockHttpRequest = request("POST", "/login.do");
    mockHttpRequest.setServerName("rebound.example");
    mockHttpRequest.addHeader("Origin", "http://rebound.example");
    assertDenied(mockHttpRequest);
  }

  @Test
  void ordinaryApplicationRequestsAreNotHandledByConsoleFilter() throws Exception {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/login");
    mockHttpRequest.setServletPath("/login");
    AtomicInteger reached = new AtomicInteger();
    filter.doFilter(
        mockHttpRequest,
        new MockHttpServletResponse(),
        (incoming, outgoing) -> reached.incrementAndGet());

    assertEquals(1, reached.get());
    assertFalse(filter.allowsConsoleRequest(mockHttpRequest));
  }

  private void assertDenied(MockHttpServletRequest mockHttpRequest) throws Exception {
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);

    assertFalse(filter.allowsConsoleRequest(mockHttpRequest));
    filter.doFilter(mockHttpRequest, mockHttpResponse, servlet);

    assertEquals(403, mockHttpResponse.getStatus());
    verifyNoInteractions(servlet);
  }

  private MockHttpServletRequest request(String method, String pathInfo) {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest(method, "/h2" + pathInfo);
    mockHttpRequest.setServletPath("/h2");
    mockHttpRequest.setPathInfo(pathInfo);
    mockHttpRequest.setHttpServletMapping(
        new MockHttpServletMapping(pathInfo.substring(1), "/h2/*", "h2Console", MappingMatch.PATH));
    return mockHttpRequest;
  }
}
