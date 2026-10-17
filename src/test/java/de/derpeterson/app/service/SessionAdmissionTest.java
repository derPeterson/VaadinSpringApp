package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
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
    var user =
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
    var request = new MockHttpServletRequest();
    admissions.onAuthentication(authentication(), request, response);
    return request;
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void exactlyNIndependentSessionsAreAllowed(int limit) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(limit);
    for (int i = 0; i < limit; i++) admit();
    assertThrows(SessionLimitException.class, this::admit);
    assertEquals(limit, registry.getAllSessions("session@example.test", true).size());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void everyRoleHasTheSameLimit(RoleType role) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    admissions.onAuthentication(
        authentication("role@example.test", role), new MockHttpServletRequest(), response);
    assertThrows(
        SessionLimitException.class,
        () ->
            admissions.onAuthentication(
                authentication("role@example.test", role), new MockHttpServletRequest(), response));
  }

  @Test
  void normalizedPrincipalAndExtraTabsReuseOneSlotWithoutRepeatingFixation() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    var first = new MockHttpServletRequest();
    first.getSession().setAttribute("vaadin-ui", "preserved");
    String old = first.getSession().getId();
    admissions.onAuthentication(
        authentication(" Session@Example.Test ", RoleType.ROLE_ADMIN), first, response);
    String current = first.getSession().getId();
    assertNotEquals(old, current);
    var tab = new MockHttpServletRequest();
    tab.setSession(first.getSession());
    admissions.onAuthentication(authentication(), tab, response);
    assertEquals(current, tab.getSession().getId());
    assertEquals("preserved", tab.getSession().getAttribute("vaadin-ui"));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
    assertThrows(SessionLimitException.class, this::admit);
  }

  @Test
  void changedLimitDoesNotEvictExistingSessions() {
    var first = admit();
    var second = admit();
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
    var request = new MockHttpServletRequest();
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(authentication(), request, response));
    assertNull(request.getSession(false));
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void storageFailureAndStatusFailureReleaseOnlyTheNewSlot() {
    var existing = admit();
    var remember = mock(PersistentTokenBasedRememberMeServices.class);
    var repo = mock(org.springframework.security.web.context.SecurityContextRepository.class);
    var users = mock(UserService.class);
    var data = mock(UserRepository.class);
    var security =
        new SecurityService(
            remember, users, data, mock(CustomUserDetailsService.class), repo, admissions);
    doThrow(new IllegalStateException("fixture save failure"))
        .when(repo)
        .saveContext(any(), any(), any());
    var failed = new MockHttpServletRequest();
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            security.storeAuthenticatedUser(
                failed,
                response,
                (org.springframework.security.core.userdetails.UserDetails)
                    authentication().getPrincipal(),
                true));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
    assertNotNull(existing.getSession(false));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(remember, never()).loginSuccess(any(), any(), any());
    reset(repo);
    doThrow(new IllegalStateException("fixture status failure"))
        .when(users)
        .recordLogin(anyString());
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            security.storeAuthenticatedUser(
                new MockHttpServletRequest(),
                response,
                (org.springframework.security.core.userdetails.UserDetails)
                    authentication().getPrincipal(),
                false));
    assertEquals(1, registry.getAllSessions("session@example.test", true).size());
  }

  @Test
  void cookieFailureAlsoRemovesThePersistedPrincipalAndReservation() {
    var remember = mock(PersistentTokenBasedRememberMeServices.class);
    doThrow(new IllegalStateException("fixture cookie failure"))
        .when(remember)
        .loginSuccess(any(), any(), any());
    var security =
        new SecurityService(
            remember,
            mock(UserService.class),
            mock(UserRepository.class),
            mock(CustomUserDetailsService.class),
            new AdmittedSecurityContextRepository(admissions),
            admissions);
    var request = new MockHttpServletRequest();
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            security.storeAuthenticatedUser(
                request,
                response,
                (org.springframework.security.core.userdetails.UserDetails)
                    authentication().getPrincipal(),
                true));
    assertTrue(registry.getAllPrincipals().isEmpty());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(remember).loginFail(eq(request), eq(response));
    verify(remember, never()).setAlwaysRemember(anyBoolean());
  }

  @Test
  void invalidationTimeoutAndIdentifierChangeReleaseTheCorrectSlot() {
    var first = admit();
    String old = first.getSession().getId();
    var session = (MockHttpSession) first.getSession();
    session.changeSessionId();
    registry.onApplicationEvent(new HttpSessionIdChangedEvent(session, old));
    assertNull(registry.getSessionInformation(old));
    assertNotNull(registry.getSessionInformation(session.getId()));
    session.invalidate();
    assertTrue(registry.getAllPrincipals().isEmpty());
    var timedOut = admit();
    registry.onApplicationEvent(new HttpSessionDestroyedEvent(timedOut.getSession()));
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void concurrentAdmissionsCannotClaimTheSameLastSlot() throws Exception {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    int total = 8;
    var ready = new CountDownLatch(total);
    var start = new CountDownLatch(1);
    var allowed = new AtomicInteger();
    var rejected = new AtomicInteger();
    try (var pool = Executors.newFixedThreadPool(total)) {
      var futures = new ArrayList<Future<?>>();
      for (int i = 0; i < total; i++)
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  try {
                    admit();
                    allowed.incrementAndGet();
                  } catch (SessionLimitException expected) {
                    rejected.incrementAndGet();
                  }
                  return null;
                }));
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      for (var future : futures) future.get(10, TimeUnit.SECONDS);
    }
    assertEquals(1, allowed.get());
    assertEquals(7, rejected.get());
  }

  @Test
  void concurrentDifferentAccountsCannotOverwriteOneBrowserSession() throws Exception {
    var shared = new MockHttpSession();
    var reserved = new CountDownLatch(1);
    var competitor = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () -> {
                var request = new MockHttpServletRequest();
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
      var second =
          pool.submit(
              () -> {
                assertTrue(reserved.await(5, TimeUnit.SECONDS));
                var request = new MockHttpServletRequest();
                request.setSession(shared);
                competitor.countDown();
                assertThrows(
                    AuthenticationServiceException.class,
                    () ->
                        admissions.onAuthentication(
                            authentication("other@example.test", RoleType.ROLE_USER),
                            request,
                            new MockHttpServletResponse()));
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
    var request = new MockHttpServletRequest();
    var remember = mock(org.springframework.security.web.authentication.RememberMeServices.class);
    var filter = new LoginCompletionFilter(admissions, remember);
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
    verify(remember).loginFail(eq(request), eq(response));
  }

  @Test
  void alreadyRegisteredOtherAccountCannotBeReplaced() {
    var request = admit();
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            admissions.onAuthentication(
                authentication("different@example.test", RoleType.ROLE_USER), request, response));
    // Same request rollback undoes its initial reservation. A separate existing device stays
    // intact.
    assertTrue(registry.getAllPrincipals().isEmpty());
  }

  @Test
  void revokedSessionsAreRejectedAndImmediatelyFreeTheirAdmissionSlot() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    var old = new MockHttpServletRequest();
    admissions.onAuthentication(authentication(), old, response);
    registry.getSessionInformation(old.getSession().getId()).expireNow();
    assertTrue(admissions.isExpired(old));
    var fresh = new MockHttpServletRequest();
    assertDoesNotThrow(() -> admissions.onAuthentication(authentication(), fresh, response));
    assertFalse(admissions.isExpired(fresh));
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(authentication(), old, response));
  }
}
