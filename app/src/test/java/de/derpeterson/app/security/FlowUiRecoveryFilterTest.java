package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.security.session.DurableSessionRepository;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class FlowUiRecoveryFilterTest {
  private final SessionAdmissionService sessionAdmissionService =
      mock(SessionAdmissionService.class);
  private final MaintenanceService maintenance = mock(MaintenanceService.class);

  private MockHttpServletRequest request(String path, String type) {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", path);
    mockHttpRequest.setServletPath(path);
    mockHttpRequest.setParameter("v-r", type);
    mockHttpRequest.getSession().setAttribute(DurableSessionRepository.ACTIVE, "fixture");
    User user = (User) User.withUsername("fixture@example.test").password("").roles("USER").build();
    mockHttpRequest
        .getSession()
        .setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(
                    user, null, user.getAuthorities())));
    return mockHttpRequest;
  }

  private MockHttpServletResponse apply(
      String mapping, MockHttpServletRequest mockHttpRequest, boolean rebuild) throws Exception {
    AtomicInteger dispatched = new AtomicInteger();
    FilterChain chain = (input, output) -> dispatched.incrementAndGet();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    new FlowUiRecoveryFilter(mapping, sessionAdmissionService, maintenance)
        .doFilter(mockHttpRequest, mockHttpResponse, chain);

    assertEquals(rebuild ? 0 : 1, dispatched.get());
    if (rebuild) {
      assertEquals("no-store", mockHttpResponse.getHeader("Cache-Control"));
    } else {
      assertEquals("", mockHttpResponse.getContentAsString());
    }
    return mockHttpResponse;
  }

  @Test
  void missingAuthenticationDoesNotRequestRecovery() throws Exception {
    MockHttpServletRequest mockHttpRequest = request("/", "uidl");
    mockHttpRequest
        .getSession()
        .setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl());
    apply("/*", mockHttpRequest, false);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void recoveryUsesOneAuthenticationSnapshot(boolean authenticated) throws Exception {
    MockHttpServletRequest mockHttpRequest = request("/", "uidl");
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(authenticated);
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(authentication, null);
    mockHttpRequest
        .getSession()
        .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    apply("/*", mockHttpRequest, authenticated);

    verify(context).getAuthentication();
  }

  @ParameterizedTest
  @CsvSource({
    "/,uidl,true",
    "/,push,true",
    "/VAADIN/push,push,true",
    "/VAADIN/push,uidl,false",
    "/admin,uidl,false",
    "/logout,push,false",
    "/login,uidl,false",
    "/VAADIN/other,push,false",
    "/,invented,false"
  })
  void onlyKnownStaleTransportsMayRequestANewDocument(String path, String type, boolean rebuild)
      throws Exception {
    apply("/*", request(path, type), rebuild);
  }

  @Test
  void mappedTransportHonorsContextPathAndNeverDispatchesStaleActions() throws Exception {
    MockHttpServletRequest inside = request("/app/ui/", "uidl");
    inside.setContextPath("/app");
    inside.setServletPath("/ui");
    inside.setPathInfo("/");
    when(maintenance.blocks(inside)).thenReturn(true);

    assertTrue(apply("/ui/*", inside, true).getContentAsString().contains("/app/maintenance"));
    MockHttpServletRequest outside = request("/app/login", "uidl");
    outside.setContextPath("/app");
    outside.setServletPath("/login");
    apply("/ui/*", outside, false);
    MockHttpServletRequest nested = request("/app/ui/admin", "uidl");
    nested.setContextPath("/app");
    nested.setServletPath("/ui");
    nested.setPathInfo("/admin");
    apply("/ui/*", nested, false);
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-admission", "no-context", "expired", "live-ui"})
  void onlyARestoredValidAuthenticationWithoutALiveUiGetsRecovery(String state) throws Exception {
    MockHttpServletRequest mockHttpRequest = request("/", "uidl");
    switch (state) {
      case "no-admission" ->
          mockHttpRequest.getSession().removeAttribute(DurableSessionRepository.ACTIVE);
      case "no-context" ->
          mockHttpRequest
              .getSession()
              .removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      case "expired" -> when(sessionAdmissionService.isExpired(mockHttpRequest)).thenReturn(true);
      case "live-ui" ->
          mockHttpRequest.getSession().setAttribute("runtime-ui", mock(VaadinSession.class));
      default -> throw new IllegalArgumentException(state);
    }
    apply("/*", mockHttpRequest, false);
  }
}
