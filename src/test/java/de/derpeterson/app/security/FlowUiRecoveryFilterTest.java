package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class FlowUiRecoveryFilterTest {
  private final SessionAdmissionService admission = mock(SessionAdmissionService.class);
  private final MaintenanceService maintenance = mock(MaintenanceService.class);

  private MockHttpServletRequest request(String path, String type) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    request.setParameter("v-r", type);
    request.getSession().setAttribute(DurableSessionRepository.ACTIVE, "fixture");
    User user = (User) User.withUsername("fixture@example.test").password("").roles("USER").build();
    request
        .getSession()
        .setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(
                    user, null, user.getAuthorities())));
    return request;
  }

  private MockHttpServletResponse apply(
      String mapping, MockHttpServletRequest request, boolean rebuild) throws Exception {
    AtomicInteger dispatched = new AtomicInteger();
    FilterChain chain = (input, output) -> dispatched.incrementAndGet();
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FlowUiRecoveryFilter(mapping, admission, maintenance).doFilter(request, response, chain);
    assertEquals(rebuild ? 0 : 1, dispatched.get());
    if (rebuild) {
      assertEquals("no-store", response.getHeader("Cache-Control"));
    } else {
      assertEquals("", response.getContentAsString());
    }
    return response;
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
    MockHttpServletRequest request = request("/", "uidl");
    switch (state) {
      case "no-admission" -> request.getSession().removeAttribute(DurableSessionRepository.ACTIVE);
      case "no-context" ->
          request
              .getSession()
              .removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      case "expired" -> when(admission.isExpired(request)).thenReturn(true);
      case "live-ui" -> request.getSession().setAttribute("runtime-ui", mock(VaadinSession.class));
      default -> throw new IllegalArgumentException(state);
    }
    apply("/*", request, false);
  }
}
