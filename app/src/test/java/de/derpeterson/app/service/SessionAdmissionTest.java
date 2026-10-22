package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdmittedSecurityContextRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.LoginCompletionFilter;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.SessionLimitException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionIdChangedEvent;

class SessionAdmissionTest {
  private final ConfigService config = mock(ConfigService.class);
  private final SessionRegistryImpl registry = new SessionRegistryImpl();
  private final SessionAdmissionService admissions = new SessionAdmissionService(config, registry);
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  void setup() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private Authentication authentication(String email, RoleType role) {
    UserDetails user =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("unused")
            .authorities(role.getGrantedRoles().stream().map(Enum::name).toArray(String[]::new))
            .build();
    return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
  }

  private Authentication authentication() {
    return authentication("session@example.test", RoleType.ROLE_USER);
  }

  private MockHttpServletRequest admit() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    admissions.onAuthentication(authentication(), request, response);
    return request;
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void exactlyNIndependentSessionsAreAllowed(int limit) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(limit);
    for (int i = 0; i < limit; i++) {
      admit();
    }
    assertThrows(SessionLimitException.class, this::admit);
    assertEquals(limit, registry.getAllSessions("session@example.test", true).size());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void everyRoleHasTheSameLimit(RoleType role) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    admissions.onAuthentication(
        authentication("role@example.test", role), new MockHttpServletRequest(), response);
    Authentication attemptAuthentication = authentication("role@example.test", role);
    MockHttpServletRequest attemptRequest = new MockHttpServletRequest();
    assertThrows(
        SessionLimitException.class,
        () -> admissions.onAuthentication(attemptAuthentication, attemptRequest, response));
  }

  @Test
  void normalizedPrincipalAndExtraTabsReuseOneSlotWithoutRepeatingFixation() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    MockHttpServletRequest first = new MockHttpServletRequest();
    first.getSession().setAttribute("vaadin-ui", "preserved");
    String old = first.getSession().getId();
    admissions.onAuthentication(
        authentication(" Session@Example.Test ", RoleType.ROLE_ADMIN), first, response);
    String current = first.getSession().getId();
    assertNotEquals(old, current);
    MockHttpServletRequest tab = new MockHttpServletRequest();
    tab.setSession(first.getSession());
    admissions.onAuthentication(authentication(), tab, response);
    assertEquals(current, tab.getSession().getId());
    assertEquals("preserved", tab.getSession().getAttribute("vaadin-ui"));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
    assertThrows(SessionLimitException.class, this::admit);
  }

  @Test
  void changedLimitDoesNotEvictExistingSessions() {
    MockHttpServletRequest first = admit();
    MockHttpServletRequest second = admit();
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    admissions.onAuthentication(authentication(), first, response);
    assertFalse(((MockHttpSession) second.getSession()).isInvalid());
    assertThrows(SessionLimitException.class, this::admit);
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    admit();
    assertThrows(SessionLimitException.class, this::admit);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidLimitFailsClosedWithoutCreatingSlot(int limit) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(limit);
    MockHttpServletRequest request = new MockHttpServletRequest();
    Authentication attemptAuthentication = authentication();
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(attemptAuthentication, request, response));
    assertNull(request.getSession(false));
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void storageFailureAndStatusFailureReleaseOnlyTheNewSlot() {
    MockHttpServletRequest existing = admit();
    PersistentTokenBasedRememberMeServices remember =
        mock(PersistentTokenBasedRememberMeServices.class);
    SecurityContextRepository repo =
        mock(org.springframework.security.web.context.SecurityContextRepository.class);
    UserService users = mock(UserService.class);
    UserRepository data = mock(UserRepository.class);
    SecurityService security =
        new SecurityService(
            remember, users, data, mock(CustomUserDetailsService.class), repo, admissions);
    doThrow(new IllegalStateException("fixture save failure"))
        .when(repo)
        .saveContext(any(), any(), any());
    MockHttpServletRequest failed = new MockHttpServletRequest();
    UserDetails userDetails =
        (org.springframework.security.core.userdetails.UserDetails) authentication().getPrincipal();
    assertThrows(
        AuthenticationServiceException.class,
        () -> security.storeAuthenticatedUser(failed, response, userDetails, true));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
    assertNotNull(existing.getSession(false));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(remember, never()).loginSuccess(any(), any(), any());
    reset(repo);
    doThrow(new IllegalStateException("fixture status failure"))
        .when(users)
        .recordLogin(anyString());
    MockHttpServletRequest attemptRequest = new MockHttpServletRequest();
    UserDetails userDetails2 =
        (org.springframework.security.core.userdetails.UserDetails) authentication().getPrincipal();
    assertThrows(
        AuthenticationServiceException.class,
        () -> security.storeAuthenticatedUser(attemptRequest, response, userDetails2, false));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
  }

  @Test
  void cookieFailureAlsoRemovesThePersistedPrincipalAndReservation() {
    PersistentTokenBasedRememberMeServices remember =
        mock(PersistentTokenBasedRememberMeServices.class);
    doThrow(new IllegalStateException("fixture cookie failure"))
        .when(remember)
        .loginSuccess(any(), any(), any());
    SecurityService security =
        new SecurityService(
            remember,
            mock(UserService.class),
            mock(UserRepository.class),
            mock(CustomUserDetailsService.class),
            new AdmittedSecurityContextRepository(admissions),
            admissions);
    MockHttpServletRequest request = new MockHttpServletRequest();
    UserDetails userDetails3 =
        (org.springframework.security.core.userdetails.UserDetails) authentication().getPrincipal();
    assertThrows(
        AuthenticationServiceException.class,
        () -> security.storeAuthenticatedUser(request, response, userDetails3, true));
    assertTrue(registry.getAllPrincipals().isEmpty());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(remember).loginFail(request, response);
    verify(remember, never()).setAlwaysRemember(anyBoolean());
  }

  @Test
  void invalidationTimeoutAndIdentifierChangeReleaseTheCorrectSlot() {
    MockHttpServletRequest first = admit();
    String old = first.getSession().getId();
    MockHttpSession session = (MockHttpSession) first.getSession();
    session.changeSessionId();
    registry.onApplicationEvent(new HttpSessionIdChangedEvent(session, old));
    assertNull(registry.getSessionInformation(old));
    assertNotNull(registry.getSessionInformation(session.getId()));
    session.invalidate();
    assertTrue(registry.getAllPrincipals().isEmpty());
    MockHttpServletRequest timedOut = admit();
    registry.onApplicationEvent(new HttpSessionDestroyedEvent(timedOut.getSession()));
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void concurrentAdmissionsCannotClaimTheSameLastSlot() throws Exception {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    int total = 8;
    CountDownLatch ready = new CountDownLatch(total);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger allowed = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    try (ExecutorService pool = Executors.newFixedThreadPool(total)) {
      ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
      for (int i = 0; i < total; i++) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  try {
                    admit();
                    allowed.incrementAndGet();
                  } catch (SessionLimitException _) {
                    rejected.incrementAndGet();
                  }
                  return null;
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      for (Future<?> future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
    }
    assertEquals(1, allowed.get());
    assertEquals(7, rejected.get());
  }

  @Test
  void concurrentDifferentAccountsCannotOverwriteOneBrowserSession() throws Exception {
    MockHttpSession shared = new MockHttpSession();
    CountDownLatch reserved = new CountDownLatch(1);
    CountDownLatch competitor = new CountDownLatch(1);
    try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
      Future<?> first =
          pool.submit(
              () -> {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setSession(shared);
                admissions.complete(
                    authentication(),
                    request,
                    new MockHttpServletResponse(),
                    () -> {
                      reserved.countDown();
                      try {
                        assertTrue(competitor.await(5, TimeUnit.SECONDS));
                      } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                      }
                    });
              });
      Future<Object> second =
          pool.submit(
              () -> {
                assertTrue(reserved.await(5, TimeUnit.SECONDS));
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setSession(shared);
                competitor.countDown();
                Authentication attemptAuthentication =
                    authentication("other@example.test", RoleType.ROLE_USER);
                MockHttpServletResponse attemptResponse = new MockHttpServletResponse();
                assertThrows(
                    AuthenticationServiceException.class,
                    () ->
                        admissions.onAuthentication(
                            attemptAuthentication, request, attemptResponse));
                return null;
              });
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    }
    assertEquals(
        "session@example.test", registry.getSessionInformation(shared.getId()).getPrincipal());
    assertTrue(registry.getAllSessions("other@example.test", true).isEmpty());
  }

  @Test
  void completionFilterReleasesReservationAfterLateFailure() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RememberMeServices remember =
        mock(org.springframework.security.web.authentication.RememberMeServices.class);
    LoginCompletionFilter filter = new LoginCompletionFilter(admissions, remember);
    assertThrows(
        jakarta.servlet.ServletException.class,
        () ->
            filter.doFilter(
                request,
                response,
                (req, res) -> {
                  admissions.onAuthentication(authentication(), request, response);
                  throw new jakarta.servlet.ServletException("fixture failure after save");
                }));
    assertTrue(registry.getAllPrincipals().isEmpty());
    verify(remember).loginFail(request, response);
  }

  @Test
  void alreadyRegisteredOtherAccountCannotBeReplaced() {
    MockHttpServletRequest request = admit();
    Authentication attemptAuthentication =
        authentication("different@example.test", RoleType.ROLE_USER);
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(attemptAuthentication, request, response));
    // Same request rollback undoes its initial reservation. A separate existing device stays
    // intact.
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void revokedSessionsAreRejectedAndImmediatelyFreeTheirAdmissionSlot() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    MockHttpServletRequest old = new MockHttpServletRequest();
    admissions.onAuthentication(authentication(), old, response);
    registry.getSessionInformation(old.getSession().getId()).expireNow();
    assertTrue(admissions.isExpired(old));
    MockHttpServletRequest fresh = new MockHttpServletRequest();
    assertDoesNotThrow(() -> admissions.onAuthentication(authentication(), fresh, response));
    assertFalse(admissions.isExpired(fresh));
    Authentication attemptAuthentication = authentication();
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(attemptAuthentication, old, response));
  }
}
