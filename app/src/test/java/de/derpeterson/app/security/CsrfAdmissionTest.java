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
  private final ConfigService configService = mock(ConfigService.class);
  private final SessionRegistryImpl sessionRegistry = new SessionRegistryImpl();
  private final SessionAdmissionService sessionAdmissionService =
      new SessionAdmissionService(configService, sessionRegistry);
  private final HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
  private final MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();

  CsrfAdmissionTest() {
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
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
    csrf.saveToken(before, request, mockHttpResponse);
    String oldId = request.getSession().getId();
    request.setRequestedSessionId(oldId);
    Authentication authentication = authentication(path.equals("remember"));
    if (path.equals("flow")) {
      sessionAdmissionService.completeLogin(
          authentication,
          request,
          mockHttpResponse,
          () -> {
            assertEquals(1, sessionRegistry.getAllSessions(authentication.getName(), false).size());
            assertNotEquals(before.getToken(), currentToken(request));
          });
    } else {
      sessionAdmissionService.onAuthentication(authentication, request, mockHttpResponse);
    }
    String current = currentToken(request);
    assertNotEquals(before.getToken(), current);
    assertNotEquals(oldId, request.getSession().getId());
    assertEquals(1, fixation.get());
    String admittedId = request.getSession().getId();
    MockHttpServletRequest next = new MockHttpServletRequest();
    next.setSession(request.getSession());
    next.setRequestedSessionId(admittedId);
    sessionAdmissionService.complete(authentication, next, mockHttpResponse, () -> {});
    assertEquals(current, csrf.loadToken(next).getToken());
    assertEquals(admittedId, next.getSession().getId());
    assertEquals(1, sessionRegistry.getAllSessions(authentication.getName(), true).size());
  }

  @Test
  void failedCompletionAfterRotationReleasesOnlyNewSessionAndPreservesOtherToken() {
    Authentication authentication = authentication(false);
    MockHttpServletRequest existing = new MockHttpServletRequest();
    csrf.saveToken(csrf.generateToken(existing), existing, mockHttpResponse);
    sessionAdmissionService.onAuthentication(authentication, existing, mockHttpResponse);
    String existingToken = currentToken(existing);
    MockHttpSession previous = (MockHttpSession) existing.getSession();
    MockHttpServletRequest failing = new MockHttpServletRequest();
    csrf.saveToken(csrf.generateToken(failing), failing, mockHttpResponse);
    MockHttpSession attempted = (MockHttpSession) failing.getSession();
    assertThrows(
        IllegalStateException.class,
        () ->
            sessionAdmissionService.completeLogin(
                authentication,
                failing,
                mockHttpResponse,
                () -> {
                  assertNotNull(currentToken(failing));
                  throw new IllegalStateException("isolated completion failure");
                }));
    assertTrue(attempted.isInvalid());
    assertFalse(previous.isInvalid());
    assertEquals(existingToken, csrf.loadToken(existing).getToken());
    assertEquals(1, sessionRegistry.getAllSessions(authentication.getName(), true).size());
  }

  @Test
  void rejectedAdmissionNeverRotatesAnonymousTokenOrPublishesCompletion() {
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    Authentication authentication = authentication(false);
    sessionAdmissionService.onAuthentication(
        authentication, new MockHttpServletRequest(), mockHttpResponse);
    MockHttpServletRequest rejected = new MockHttpServletRequest();
    CsrfToken before = csrf.generateToken(rejected);
    csrf.saveToken(before, rejected, mockHttpResponse);
    AtomicInteger published = new AtomicInteger();
    assertThrows(
        SessionLimitException.class,
        () ->
            sessionAdmissionService.completeLogin(
                authentication, rejected, mockHttpResponse, published::incrementAndGet));
    assertEquals(0, published.get());
    assertEquals(before.getToken(), csrf.loadToken(rejected).getToken());
    assertEquals(1, sessionRegistry.getAllSessions(authentication.getName(), false).size());
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
