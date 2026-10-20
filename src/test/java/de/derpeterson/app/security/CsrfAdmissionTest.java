package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

class CsrfAdmissionTest {
  private final ConfigService config = mock(ConfigService.class);
  private final SessionRegistryImpl registry = new SessionRegistryImpl();
  private final SessionAdmissionService admissions = new SessionAdmissionService(config, registry);
  private final HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  CsrfAdmissionTest() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
  }

  @ParameterizedTest
  @ValueSource(strings = {"http", "remember", "flow"})
  void explicitLoginRotatesAfterAdmissionButContextPersistenceKeepsTokenAndId(String path) {
    AtomicInteger fixation = new AtomicInteger();
    MockHttpServletRequest request =
        new MockHttpServletRequest() {
          @Override
          public String changeSessionId() {
            fixation.incrementAndGet();
            return super.changeSessionId();
          }
        };
    CsrfToken before = csrf.generateToken(request);
    csrf.saveToken(before, request, response);
    String oldId = request.getSession().getId();
    Authentication authentication = authentication(path.equals("remember"));
    if (path.equals("flow")) {
      admissions.completeLogin(
          authentication,
          request,
          response,
          () -> {
            assertEquals(1, registry.getAllSessions(authentication.getName(), false).size());
            assertNotEquals(before.getToken(), currentToken(request));
          });
    } else {
      admissions.onAuthentication(authentication, request, response);
    }
    String current = currentToken(request);
    assertNotEquals(before.getToken(), current);
    assertNotEquals(oldId, request.getSession().getId());
    assertEquals(1, fixation.get());
    String admittedId = request.getSession().getId();
    MockHttpServletRequest next = new MockHttpServletRequest();
    next.setSession(request.getSession());
    admissions.complete(authentication, next, response, () -> {});
    assertEquals(current, csrf.loadToken(next).getToken());
    assertEquals(admittedId, next.getSession().getId());
    assertEquals(1, registry.getAllSessions(authentication.getName(), true).size());
  }

  @Test
  void failedCompletionAfterRotationReleasesOnlyNewSessionAndPreservesOtherToken() {
    Authentication authentication = authentication(false);
    MockHttpServletRequest existing = new MockHttpServletRequest();
    csrf.saveToken(csrf.generateToken(existing), existing, response);
    admissions.onAuthentication(authentication, existing, response);
    String existingToken = currentToken(existing);
    MockHttpSession previous = (MockHttpSession) existing.getSession();
    MockHttpServletRequest failing = new MockHttpServletRequest();
    csrf.saveToken(csrf.generateToken(failing), failing, response);
    MockHttpSession attempted = (MockHttpSession) failing.getSession();
    assertThrows(
        IllegalStateException.class,
        () ->
            admissions.completeLogin(
                authentication,
                failing,
                response,
                () -> {
                  assertNotNull(currentToken(failing));
                  throw new IllegalStateException("isolated completion failure");
                }));
    assertTrue(attempted.isInvalid());
    assertFalse(previous.isInvalid());
    assertEquals(existingToken, csrf.loadToken(existing).getToken());
    assertEquals(1, registry.getAllSessions(authentication.getName(), true).size());
  }

  @Test
  void rejectedAdmissionNeverRotatesAnonymousTokenOrPublishesCompletion() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    Authentication authentication = authentication(false);
    admissions.onAuthentication(authentication, new MockHttpServletRequest(), response);
    MockHttpServletRequest rejected = new MockHttpServletRequest();
    CsrfToken before = csrf.generateToken(rejected);
    csrf.saveToken(before, rejected, response);
    AtomicInteger published = new AtomicInteger();
    assertThrows(
        SessionLimitException.class,
        () ->
            admissions.completeLogin(
                authentication, rejected, response, published::incrementAndGet));
    assertEquals(0, published.get());
    assertEquals(before.getToken(), csrf.loadToken(rejected).getToken());
    assertEquals(1, registry.getAllSessions(authentication.getName(), false).size());
  }

  private String currentToken(MockHttpServletRequest request) {
    CsrfToken masked = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    assertNotNull(masked);
    masked.getToken();
    return csrf.loadToken(request).getToken();
  }

  private Authentication authentication(boolean remember) {
    UserDetails user =
        User.withUsername("csrf-admission@example.test").password("unused").roles("USER").build();
    if (remember) {
      return new RememberMeAuthenticationToken("isolated", user, user.getAuthorities());
    }
    return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
  }
}
