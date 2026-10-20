package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.server.HandlerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class FlowCsrfRequestMatcherTest {
  private final FlowCsrfRequestMatcher matcher = new FlowCsrfRequestMatcher("/*");

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/login",
        "/logout",
        "/admin",
        "/h2/login.do",
        "/VAADIN/uidl",
        "/HILLA/push",
        "/VAADIN/other"
      })
  void forgedFrameworkMarkerNeverExemptsClassicOrUnknownEndpoints(String path) {
    MockHttpServletRequest request = request("POST", path, "uidl");
    assertTrue(matcher.matches(request));
    request.setParameter("v-r", "push");
    assertTrue(matcher.matches(request));
  }

  @Test
  void onlyKnownRootTransportTypesAreExempt() {
    for (HandlerHelper.RequestType type : HandlerHelper.RequestType.values()) {
      assertFalse(matcher.matches(request("POST", "/", type.getIdentifier())), type.name());
    }
    MockHttpServletRequest forwardedRoot = request("POST", "/", "uidl");
    forwardedRoot.setServletPath("");
    forwardedRoot.setPathInfo("/");
    assertFalse(matcher.matches(forwardedRoot));
    assertTrue(matcher.matches(request("POST", "/", "invented")));
    assertTrue(matcher.matches(request("POST", "/", null)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "HEAD", "OPTIONS", "TRACE"})
  void safeMethodsDoNotRequireToken(String method) {
    assertFalse(matcher.matches(request(method, "/login", null)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
  void unsafeClassicMethodsRequireToken(String method) {
    assertTrue(matcher.matches(request(method, "/login", null)));
  }

  @Test
  void actualPushAndDynamicResourcePathsAreNarrowExceptions() {
    assertFalse(matcher.matches(request("POST", "/VAADIN/push", "push")));
    assertTrue(matcher.matches(request("POST", "/VAADIN/push", "uidl")));
    assertTrue(matcher.matches(request("POST", "/VAADIN/push", null)));
    assertFalse(matcher.matches(request("POST", "/VAADIN/dynamic/resource/0/key/upload", null)));
    assertTrue(matcher.matches(request("POST", "/VAADIN/dynamic/resource/../login", "uidl")));
  }

  @Test
  void mappingAndContextPathAreHonored() {
    FlowCsrfRequestMatcher mapped = new FlowCsrfRequestMatcher("/ui/*");
    MockHttpServletRequest inside = request("POST", "/app/ui/", "uidl");
    inside.setContextPath("/app");
    inside.setServletPath("/ui");
    inside.setPathInfo("/");
    assertFalse(mapped.matches(inside));
    MockHttpServletRequest outside = request("POST", "/app/login", "uidl");
    outside.setContextPath("/app");
    outside.setServletPath("/login");
    assertTrue(mapped.matches(outside));
    MockHttpServletRequest nested = request("POST", "/app/ui/login", "uidl");
    nested.setContextPath("/app");
    nested.setServletPath("/ui");
    nested.setPathInfo("/login");
    assertTrue(mapped.matches(nested));
  }

  private MockHttpServletRequest request(String method, String path, String type) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    if (type != null) {
      request.setParameter("v-r", type);
    }
    return request;
  }
}
