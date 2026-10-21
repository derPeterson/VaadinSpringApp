package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/** Actual HTTP filters and isolated H2; no running application or real accounts. */
@SpringJUnitConfig(SecurityChainRegressionTest.Config.class)
@WebAppConfiguration
class CsrfSecurityChainTest {
  @Autowired SecurityFilterChain chain;
  @Autowired SessionRegistry registry;
  @Autowired LoginAttemptRepository attempts;
  @Autowired UserRepository users;
  @Autowired ConfigService settings;
  @Autowired PasswordEncoder encoder;
  @Autowired TransactionTemplate transaction;
  @Autowired SecurityService security;
  @Autowired PersistentTokenRepository tokens;
  @PersistenceContext EntityManager entityManager;
  private final HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "invalid", "foreign", "forged-flow"})
  void invalidLoginTokenCannotAuthenticateCountPasswordFailuresOrPublishCookie(String kind)
      throws Exception {
    String email = user();
    Exchange bootstrap = bootstrap(null);
    MockHttpServletRequest login = request("POST", "/login", session(bootstrap));
    login.setParameter("username", email);
    login.setParameter("password", "SecretPassword!");
    login.setParameter("remember-me", "true");
    if (kind.equals("invalid")) {
      login.addHeader("X-CSRF-TOKEN", "invalid");
    } else if (kind.equals("foreign")) {
      login.addHeader("X-CSRF-TOKEN", bootstrap(null).response().getHeader("X-CSRF-TOKEN"));
    } else if (kind.equals("forged-flow")) {
      login.setParameter("v-r", "uidl");
    }
    FilterChain downstream = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(login, response, downstream);
    assertEquals(403, response.getStatus());
    assertNull(response.getRedirectedUrl());
    verifyNoInteractions(downstream);
    assertTrue(registry.getAllSessions(email, true).isEmpty());
    assertTrue(attempts.findById(LoginAttemptService.identityKey(email)).isEmpty());
    assertNull(
        session(bootstrap)
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(0, response.getCookies().length);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void headerAndBodyTokensLoginRotateAndOldTokenCannotLogout(boolean body) throws Exception {
    String email = user();
    Exchange bootstrap = bootstrap(null);
    String oldToken = bootstrap.response().getHeader("X-CSRF-TOKEN");
    String oldRaw = csrf.loadToken(bootstrap.request()).getToken();
    String oldId = session(bootstrap).getId();
    Exchange login = login(email, bootstrap, body, false);
    assertEquals("/home", login.response().getRedirectedUrl());
    assertNotEquals(oldId, session(login).getId());
    assertEquals(1, registry.getAllSessions(email, false).size());
    Exchange fresh = bootstrap(session(login));
    String currentRaw = csrf.loadToken(fresh.request()).getToken();
    assertNotEquals(oldRaw, currentRaw);
    MockHttpServletRequest stale = request("POST", "/logout", session(login));
    stale.addHeader("X-CSRF-TOKEN", oldToken);
    assertEquals(403, execute(stale).response().getStatus());
    assertFalse(session(login).isInvalid());
    execute(request("GET", "/home", session(login)));
    assertEquals(currentRaw, csrf.loadToken(fresh.request()).getToken());
    MockHttpServletRequest logout = request("POST", "/logout", session(login));
    logout.addHeader("X-CSRF-TOKEN", fresh.response().getHeader("X-CSRF-TOKEN"));
    MockHttpSession loggedIn = session(login);
    assertEquals("/login", execute(logout).response().getRedirectedUrl());
    assertTrue(loggedIn.isInvalid());
    assertTrue(registry.getAllSessions(email, true).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "missing", "invalid", "forged-flow"})
  void logoutRequiresValidPostAndPreservesAuthenticationOtherwise(String mode) throws Exception {
    String email = user();
    Exchange login = login(email, bootstrap(null), false, true);
    MockHttpServletRequest logout =
        request(mode.equals("GET") ? "GET" : "POST", "/logout", session(login));
    if (mode.equals("invalid")) {
      logout.addHeader("X-CSRF-TOKEN", "invalid");
    } else if (mode.equals("forged-flow")) {
      logout.setParameter("v-r", "uidl");
    }
    Exchange result = execute(logout);
    if (!mode.equals("GET")) {
      assertEquals(403, result.response().getStatus());
    }
    assertFalse(session(login).isInvalid());
    assertNotNull(
        session(login)
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(1, registry.getAllSessions(email, false).size());
    assertNotNull(tokens.getTokenForSeries(series(rememberCookie(login))));
  }

  @Test
  void validLogoutRemovesOnlyOwnRememberSeriesAndClearsCookies() throws Exception {
    String email = user();
    Exchange first = login(email, bootstrap(null), false, true);
    Exchange other = login(email, bootstrap(null), false, true);
    Exchange fresh = bootstrap(session(first));
    MockHttpServletRequest logout = request("POST", "/logout", session(first));
    logout.addHeader("X-CSRF-TOKEN", fresh.response().getHeader("X-CSRF-TOKEN"));
    Exchange result = execute(logout);
    assertEquals("/login", result.response().getRedirectedUrl());
    assertNull(tokens.getTokenForSeries(series(rememberCookie(first))));
    assertNotNull(tokens.getTokenForSeries(series(rememberCookie(other))));
    assertFalse(session(other).isInvalid());
    assertEquals(1, registry.getAllSessions(email, false).size());
    assertEquals(0, result.response().getCookie("remember-me").getMaxAge());
    assertEquals(0, result.response().getCookie("JSESSIONID").getMaxAge());
  }

  @Test
  void rememberMeRestoresSessionAndRotatesTokenBeforeBootstrapHeader() throws Exception {
    String email = user();
    Exchange first = login(email, bootstrap(null), false, true);
    Exchange anonymous = bootstrap(null);
    String oldRaw = csrf.loadToken(anonymous.request()).getToken();
    MockHttpServletRequest returning = request("GET", "/login", session(anonymous));
    returning.setCookies(rememberCookie(first));
    Exchange restored = execute(returning);
    assertNotEquals(oldRaw, csrf.loadToken(returning).getToken());
    assertNotNull(
        session(restored)
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(2, registry.getAllSessions(email, false).size());
    MockHttpServletRequest logout = request("POST", "/logout", session(restored));
    logout.addHeader("X-CSRF-TOKEN", restored.response().getHeader("X-CSRF-TOKEN"));
    assertEquals("/login", execute(logout).response().getRedirectedUrl());
  }

  @Test
  void flowLoginRotatesSpringTokenAndOrdinaryContextRefreshDoesNot() throws Exception {
    String email = user();
    Exchange bootstrap = bootstrap(null);
    String oldRaw = csrf.loadToken(bootstrap.request()).getToken();
    security.storeAuthenticatedUser(
        bootstrap.request(),
        new MockHttpServletResponse(),
        new CustomUserDetailsService(users).loadUserByUsername(email),
        false);
    Exchange fresh = bootstrap(session(bootstrap));
    String current = csrf.loadToken(fresh.request()).getToken();
    assertNotEquals(oldRaw, current);
    assertEquals(1, registry.getAllSessions(email, false).size());
    execute(request("GET", "/account", session(fresh)));
    assertEquals(current, csrf.loadToken(fresh.request()).getToken());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/home", "/custom-theme/cosmos-theme.css", "/VAADIN/build/app.js"})
  void publicGetsDoNotCreateCsrfSession(String path) throws Exception {
    Exchange result = execute(request("GET", path, null));
    assertNull(result.request().getSession(false));
    assertNull(result.response().getHeader("X-CSRF-TOKEN"));
  }

  @Test
  void loginBootstrapIsMaskedUncachedAndBoundToSameSession() throws Exception {
    MockHttpServletRequest contextLogin = request("GET", "/app/login", null);
    contextLogin.setContextPath("/app");
    contextLogin.setServletPath("/login");
    Exchange underContext = execute(contextLogin);
    assertNotNull(underContext.response().getHeader("X-CSRF-TOKEN"));
    assertNotNull(underContext.request().getSession(false));
    Exchange first = bootstrap(null);
    assertNotNull(first.response().getHeader("X-CSRF-TOKEN"));
    assertTrue(first.response().getHeader("Cache-Control").contains("no-store"));
    String raw = csrf.loadToken(first.request()).getToken();
    assertNotEquals(raw, first.response().getHeader("X-CSRF-TOKEN"));
    Exchange second = bootstrap(session(first));
    assertEquals(session(first), session(second));
    assertEquals(raw, csrf.loadToken(second.request()).getToken());
    assertNotEquals(
        first.response().getHeader("X-CSRF-TOKEN"), second.response().getHeader("X-CSRF-TOKEN"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/h2/login.do", "/HILLA/push", "/VAADIN/uidl", "/"})
  void forgedTransportOrUnknownRootTypeCannotReachServlet(String path) throws Exception {
    MockHttpServletRequest request = request("POST", path, null);
    request.setParameter("v-r", path.equals("/") ? "invented" : "uidl");
    assertEquals(403, execute(request).response().getStatus());
  }

  @Test
  void httpsPasswordLoginIssuesSecureHttpOnlySameSiteRememberCookie() throws Exception {
    String email = user();
    Exchange bootstrap = bootstrap(null);
    MockHttpServletRequest request = request("POST", "/login", session(bootstrap));
    request.setSecure(true);
    request.addHeader("X-CSRF-TOKEN", bootstrap.response().getHeader("X-CSRF-TOKEN"));
    request.setParameter("username", email);
    request.setParameter("password", "SecretPassword!");
    request.setParameter("remember-me", "true");
    Exchange authenticated = execute(request);
    assertEquals(302, authenticated.response().getStatus());
    Cookie cookie = rememberCookie(authenticated);
    assertTrue(cookie.getSecure());
    assertTrue(cookie.isHttpOnly());
    assertEquals("Lax", cookie.getAttribute("SameSite"));
  }

  private Exchange login(String email, Exchange bootstrap, boolean body, boolean remember)
      throws Exception {
    MockHttpServletRequest request = request("POST", "/login", session(bootstrap));
    String token = bootstrap.response().getHeader("X-CSRF-TOKEN");
    if (body) {
      request.setParameter("_csrf", token);
    } else {
      request.addHeader("X-CSRF-TOKEN", token);
    }
    request.setParameter("username", email);
    request.setParameter("password", "SecretPassword!");
    if (remember) {
      request.setParameter("remember-me", "true");
    }
    Exchange result = execute(request);
    assertEquals(302, result.response().getStatus());
    return result;
  }

  private Exchange bootstrap(MockHttpSession session) throws Exception {
    return execute(request("GET", "/login", session));
  }

  private MockHttpSession session(Exchange result) {
    return (MockHttpSession) result.request().getSession(false);
  }

  private MockHttpServletRequest request(String method, String path, MockHttpSession session) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    request.setSession(session);
    return request;
  }

  private Exchange execute(MockHttpServletRequest request) throws Exception {
    SecurityContextHolder.clearContext();
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(request, response, (incoming, outgoing) -> {});
    return new Exchange(request, response);
  }

  private Cookie rememberCookie(Exchange login) {
    Cookie cookie =
        Arrays.stream(login.response().getCookies())
            .filter(value -> "remember-me".equals(value.getName()) && value.getMaxAge() > 0)
            .reduce((previous, current) -> current)
            .orElse(null);
    assertNotNull(cookie);
    return cookie;
  }

  private String series(Cookie cookie) {
    String decoded =
        new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
    return URLDecoder.decode(decoded.split(":")[0], StandardCharsets.UTF_8);
  }

  private String user() {
    String email = "csrf-" + UUID.randomUUID() + "@example.test";
    transaction.executeWithoutResult(
        tx -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
                  .setParameter("role", RoleType.ROLE_USER)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(role);
          }
          entityManager.persist(
              UserEntity.builder()
                  .email(email)
                  .password(encoder.encode("SecretPassword!"))
                  .firstName("CSRF")
                  .lastName("Test")
                  .enabled(true)
                  .gender(Gender.OTHER)
                  .birthDate(LocalDate.of(1990, 1, 1))
                  .roleEntities(new ArrayList<>(List.of(role)))
                  .build());
        });
    return email;
  }

  private record Exchange(MockHttpServletRequest request, MockHttpServletResponse response) {}
}
