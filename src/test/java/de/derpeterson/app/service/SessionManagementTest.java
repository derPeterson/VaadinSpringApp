package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Frische eigene H2-Datenbank; echte JDBC-Token, Cookies, Zulassung und SecurityService, kein
 * Appstart.
 */
class SessionManagementTest {
  private final ConfigService config = mock(ConfigService.class);
  private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private final AtomicReference<UserDetails> account = new AtomicReference<>();
  private DeviceTokenRepository tokens;
  private JdbcTemplate jdbc;
  private SessionRegistryImpl registry;
  private SessionAdmissionService admissions;
  private DeviceRememberMeServices remembered;
  private SecurityService security;
  private SessionManagementService management;
  private static final String IDENTITY = "sessions@example.test";
  private static final String PASSWORD = "Strong!Password8";
  private static final Instant NOW = Instant.parse("2026-10-17T03:00:00Z");

  private record Login(MockHttpServletRequest request, MockHttpServletResponse response) {}

  @BeforeEach
  void setup() throws Exception {
    when(config.getInteger(any()))
        .thenAnswer(
            call -> Integer.parseInt(((ConfigEntry) call.getArgument(0)).getDefaultValueString()));
    when(config.getInteger(ConfigEntry.REMEMBER_ME_DURATION)).thenReturn(3600);
    account.set(
        User.withUsername(IDENTITY).password(encoder.encode(PASSWORD)).roles("USER").build());
    when(users.loadUserByUsername(IDENTITY)).thenAnswer(call -> account.get());
    var source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:session-management-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    jdbc = new JdbcTemplate(source);
    jdbc.execute(
        "create table persistent_logins (username varchar(255) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
    tokens = new DeviceTokenRepository(source, new DataSourceTransactionManager(source));
    tokens.afterPropertiesSet();
    restart();
  }

  private void restart() throws Exception {
    registry = new SessionRegistryImpl();
    admissions = new SessionAdmissionService(config, registry);
    admissions.setDeviceTokens(tokens);
    remembered =
        new DeviceRememberMeServices("isolated-remember-me-key", users, tokens, admissions);
    remembered.setTokenValiditySeconds(config.getInteger(ConfigEntry.REMEMBER_ME_DURATION));
    remembered.afterPropertiesSet();
    security =
        new SecurityService(
            remembered,
            mock(UserService.class),
            mock(UserRepository.class),
            users,
            new AdmittedSecurityContextRepository(admissions),
            admissions);
    AuthenticationManager authentication =
        request -> {
          if (!account.get().isEnabled()
              || !encoder.matches(request.getCredentials().toString(), account.get().getPassword()))
            throw new BadCredentialsException("Invalid credentials");
          return UsernamePasswordAuthenticationToken.authenticated(
              account.get(), null, account.get().getAuthorities());
        };
    management =
        new SessionManagementService(
            admissions, tokens, security, users, authentication, encoder, config, remembered);
    ReflectionTestUtils.setField(management, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
    SecurityContextHolder.clearContext();
  }

  @Test
  void changingSavedLoginDurationOnlyTakesEffectAfterRestart() throws Exception {
    var owner = login(true);
    String series = tokens.saved(IDENTITY, 3600).getFirst().series();
    jdbc.update(
        "update persistent_logins set last_used=? where series=?",
        Timestamp.from(Instant.now().minusSeconds(120)),
        series);
    when(config.getInteger(ConfigEntry.REMEMBER_ME_DURATION)).thenReturn(1);
    management.list(owner.request());
    management.deleteExpiredSavedLogins();
    assertNotNull(
        tokens.getTokenForSeries(series),
        "Eine nur gespeicherte startgebundene Konfiguration darf keinen gültigen Zugang löschen");
    restart();
    management.deleteExpiredSavedLogins();
    assertNull(tokens.getTokenForSeries(series));
    assertEquals(0, jdbc.queryForObject("select count(*) from remembered_device", Integer.class));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private Login login(boolean remember, String agent) {
    var request = new MockHttpServletRequest();
    request.addHeader("User-Agent", agent);
    var response = new MockHttpServletResponse();
    var principal = User.withUserDetails(account.get()).password("").build();
    security.storeAuthenticatedUser(request, response, principal, remember);
    SecurityContextHolder.clearContext();
    return new Login(next(request), response);
  }

  private Login login(boolean remember) {
    return login(remember, "Mozilla/5.0 (Windows NT 10.0) Chrome/130.0");
  }

  private MockHttpServletRequest next(MockHttpServletRequest previous) {
    var next = new MockHttpServletRequest();
    next.setSession((MockHttpSession) previous.getSession(false));
    return next;
  }

  private Cookie cookie(Login login) {
    return Arrays.stream(login.response().getCookies())
        .filter(item -> item.getName().equals("remember-me") && item.getMaxAge() > 0)
        .reduce((first, last) -> last)
        .orElseThrow();
  }

  private Authentication returnWith(Cookie cookie, MockHttpServletRequest request) {
    request.setCookies(cookie);
    return remembered.autoLogin(request, new MockHttpServletResponse());
  }

  @Test
  void listsRealOwnedSessionsWithMetadataCurrentMarkerAndOpaqueHandles() {
    var first = login(true);
    // MockHttpSession vergibt sonst kurze Ziffernfolgen, die zufällig in jeder UUID vorkommen
    // können.
    // Für den Geheimhaltungsnachweis eine eindeutige interne Kennung samt echtem ID-Ereignis
    // verwenden.
    var privateSession = first.request().getSession();
    String previous = privateSession.getId();
    String privateId = "private-http-session-" + UUID.randomUUID();
    ReflectionTestUtils.setField(privateSession, "id", privateId);
    admissions.sessionIdChanged(
        previous,
        privateId,
        () ->
            registry.onApplicationEvent(
                new org.springframework.security.web.session.HttpSessionIdChangedEvent(
                    privateSession, previous)));
    var second = login(false, "Mozilla/5.0 (Android) Chrome/130.0 Mobile");
    var snapshot = management.list(first.request());
    assertEquals(2, snapshot.active());
    assertEquals(3, snapshot.limit());
    assertEquals(2, snapshot.entries().size());
    assertEquals(1, snapshot.entries().stream().filter(entry -> entry.current()).count());
    assertTrue(snapshot.entries().getFirst().current());
    assertEquals("Windows", snapshot.entries().getFirst().device().system());
    assertTrue(snapshot.entries().getFirst().remembered());
    assertTrue(snapshot.entries().stream().anyMatch(entry -> entry.device().mobile()));
    assertFalse(snapshot.toString().contains(first.request().getSession().getId()));
    assertFalse(snapshot.toString().contains(cookie(first).getValue()));
    assertFalse(snapshot.toString().contains(account.get().getPassword()));
    assertTrue(
        snapshot.entries().stream().allMatch(entry -> UUID.fromString(entry.handle()) != null));
  }

  @Test
  void revokesOnlySelectedSessionAndItsSavedSignInAndImmediatelyFreesSlot() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(2);
    var first = login(true);
    var second = login(true);
    var other =
        management.list(first.request()).entries().stream()
            .filter(entry -> !entry.current())
            .findFirst()
            .orElseThrow();
    management.revoke(first.request(), other.handle());
    assertTrue(admissions.isExpired(second.request()));
    assertFalse(admissions.isExpired(first.request()));
    assertEquals(1, management.list(first.request()).active());
    assertNull(returnWith(cookie(second), new MockHttpServletRequest()));
    assertEquals(1, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertNotNull(login(false));
    management.revoke(first.request(), other.handle());
    assertEquals(2, management.list(first.request()).active());
  }

  @Test
  void cannotRevokeCurrentSessionOrAHandleBelongingToAnotherAccount() {
    var first = login(true);
    var own = management.list(first.request()).entries().getFirst();
    assertThrows(
        IllegalArgumentException.class, () -> management.revoke(first.request(), own.handle()));
    var foreign =
        User.withUsername("foreign@example.test").password("unused").roles("USER").build();
    when(users.loadUserByUsername("foreign@example.test")).thenReturn(foreign);
    var foreignRequest = new MockHttpServletRequest();
    admissions.onAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(foreign, null, foreign.getAuthorities()),
        foreignRequest,
        new MockHttpServletResponse());
    var foreignHandle = admissions.active("foreign@example.test").getFirst().handle();
    management.revoke(first.request(), foreignHandle);
    assertFalse(admissions.isExpired(foreignRequest));
    assertFalse(admissions.isExpired(first.request()));
    assertEquals(1, management.list(first.request()).entries().size());
  }

  @Test
  void logoutOthersKeepsCurrentSessionAndItsOwnRememberedSignIn() {
    var first = login(true);
    var second = login(true);
    var third = login(false);
    management.revokeOthers(first.request());
    assertFalse(admissions.isExpired(first.request()));
    assertTrue(admissions.isExpired(second.request()));
    assertTrue(admissions.isExpired(third.request()));
    assertEquals(1, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertEquals(1, management.list(first.request()).active());
    assertNotNull(returnWith(cookie(first), new MockHttpServletRequest()));
    assertNull(returnWith(cookie(second), new MockHttpServletRequest()));
  }

  @Test
  void sessionsSharingOneSavedCookieHaveUniqueRowsAndRevocationKeepsCurrentSession() {
    var owner = login(true);
    var returning = new MockHttpServletRequest();
    var authentication = returnWith(cookie(owner), returning);
    assertNotNull(authentication);
    admissions.onAuthentication(authentication, returning, new MockHttpServletResponse());
    var snapshot = management.list(owner.request());
    assertEquals(2, snapshot.active());
    assertEquals(
        2,
        snapshot.entries().stream().map(SessionManagementService.Entry::handle).distinct().count());
    var other =
        snapshot.entries().stream().filter(entry -> !entry.current()).findFirst().orElseThrow();
    management.revoke(owner.request(), other.savedHandle());
    assertTrue(admissions.isExpired(returning));
    assertFalse(admissions.isExpired(owner.request()));
    assertEquals(0, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertEquals(1, management.list(owner.request()).active());
  }

  @Test
  void normalLogoutRevokesOnlyCurrentBrowserSavedSignIn() {
    var first = login(true);
    var second = login(true);
    remembered.logout(
        second.request(),
        new MockHttpServletResponse(),
        UsernamePasswordAuthenticationToken.authenticated(
            account.get(), null, account.get().getAuthorities()));
    second.request().getSession().invalidate();
    assertNotNull(returnWith(cookie(first), new MockHttpServletRequest()));
    assertNull(returnWith(cookie(second), new MockHttpServletRequest()));
    assertEquals(1, management.list(first.request()).active());
  }

  @Test
  void closingSessionKeepsSavedEntryWithoutOccupyingSlotAndItCanBeRevoked() {
    var first = login(true);
    var second = login(true);
    second.request().getSession().invalidate();
    var snapshot = management.list(first.request());
    assertEquals(1, snapshot.active());
    var saved =
        snapshot.entries().stream().filter(entry -> !entry.active()).findFirst().orElseThrow();
    assertTrue(saved.remembered());
    management.revoke(first.request(), saved.handle());
    assertNull(returnWith(cookie(second), new MockHttpServletRequest()));
    assertEquals(1, management.list(first.request()).entries().size());
  }

  @Test
  void restartKeepsSavedHandlesButDoesNotInventActiveSessions() throws Exception {
    var old = login(true);
    String handle = tokens.saved(IDENTITY, 3600).getFirst().handle();
    restart();
    var current = login(false);
    var snapshot = management.list(current.request());
    assertEquals(1, snapshot.active());
    assertTrue(
        snapshot.entries().stream()
            .anyMatch(entry -> !entry.active() && entry.handle().equals(handle)));
    var returning = new MockHttpServletRequest();
    var auth = returnWith(cookie(old), returning);
    assertNotNull(auth);
    admissions.onAuthentication(auth, returning, new MockHttpServletResponse());
    assertEquals(2, registry.getAllSessions(IDENTITY, false).size());
    assertEquals(handle, tokens.saved(IDENTITY, 3600).getFirst().handle());
  }

  @Test
  void revokedCookieCannotBeAdmittedAfterItPassedItsInitialCheck() {
    var owner = login(false);
    var old = login(true);
    old.request().getSession().invalidate();
    var request = new MockHttpServletRequest();
    var auth = returnWith(cookie(old), request);
    assertNotNull(auth);
    var saved =
        management.list(owner.request()).entries().stream()
            .filter(entry -> !entry.active())
            .findFirst()
            .orElseThrow();
    management.revoke(owner.request(), saved.handle());
    assertThrows(
        AuthenticationServiceException.class,
        () -> admissions.onAuthentication(auth, request, new MockHttpServletResponse()));
    assertEquals(1, registry.getAllSessions(IDENTITY, false).size());
  }

  @Test
  void concurrentRevokeAndAutomaticReturnNeverRestoreTheRevokedSavedLogin() throws Exception {
    var owner = login(false);
    var old = login(true);
    old.request().getSession().invalidate();
    String handle =
        management.list(owner.request()).entries().stream()
            .filter(entry -> !entry.active())
            .findFirst()
            .orElseThrow()
            .handle();
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var returning = new MockHttpServletRequest();
      var automatic =
          pool.submit(
              () -> {
                start.await();
                var auth = returnWith(cookie(old), returning);
                if (auth != null) {
                  try {
                    admissions.onAuthentication(auth, returning, new MockHttpServletResponse());
                  } catch (AuthenticationServiceException revoked) {
                    /* Expected if the revocation won. */
                  }
                }
                return null;
              });
      var revoke =
          pool.submit(
              () -> {
                start.await();
                management.revoke(owner.request(), handle);
                return null;
              });
      start.countDown();
      automatic.get(5, TimeUnit.SECONDS);
      revoke.get(5, TimeUnit.SECONDS);
      // Eine Rückkehr vor dem Widerruf hat eine neue aktive Zeile mit derselben Tokenserie.
      assertNull(returnWith(cookie(old), new MockHttpServletRequest()));
      assertEquals(1, registry.getAllSessions(IDENTITY, false).size());
    }
  }

  @Test
  void passwordProofPermitsOnlyManagementAndThenRegularAdmissionAfterFreeingSlot() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    var old = login(true);
    var request = new MockHttpServletRequest();
    request.getSession().setAttribute("public-ui", "preserved");
    String anonymousId = request.getSession().getId();
    management.startRecovery(request, IDENTITY, PASSWORD, true);
    assertNotEquals(anonymousId, request.getSession().getId());
    assertEquals("preserved", request.getSession().getAttribute("public-ui"));
    assertTrue(management.hasRecovery(request));
    assertTrue(security.getAuthenticatedUser(request).isEmpty());
    assertNull(request.getSession().getAttribute("authenticatedUser"));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertThrows(
        SessionLimitException.class,
        () -> management.finishRecovery(request, new MockHttpServletResponse()));
    var snapshot = management.recoveryList(request);
    assertEquals(1, snapshot.active());
    assertFalse(snapshot.entries().getFirst().current());
    management.recoveryRevoke(request, snapshot.entries().getFirst().handle());
    assertTrue(admissions.isExpired(old.request()));
    assertFalse(management.finishRecovery(request, new MockHttpServletResponse()));
    assertFalse(management.hasRecovery(request));
    assertTrue(security.getAuthenticatedUser(request).isPresent());
    assertEquals(1, registry.getAllSessions(IDENTITY, false).size());
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> management.recoveryList(request));
  }

  @Test
  void wrongPasswordNeverCreatesManagementProof() {
    var request = new MockHttpServletRequest();
    assertThrows(
        BadCredentialsException.class,
        () -> management.startRecovery(request, IDENTITY, "wrong", false));
    assertFalse(management.hasRecovery(request));
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> management.recoveryList(request));
    assertTrue(security.getAuthenticatedUser(request).isEmpty());
  }

  @Test
  void proofExpiresExactlyAtFiveMinutesAndCannotBeTransferredToAnotherSession() {
    var request = new MockHttpServletRequest();
    management.startRecovery(request, IDENTITY, PASSWORD, false);
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> management.recoveryList(new MockHttpServletRequest()));
    ReflectionTestUtils.setField(
        management, "clock", Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC));
    assertFalse(management.hasRecovery(request));
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> management.recoveryList(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"password", "disabled", "roles"})
  void changedSecurityDataInvalidatesProof(String change) {
    var request = new MockHttpServletRequest();
    management.startRecovery(request, IDENTITY, PASSWORD, false);
    var builder = User.withUserDetails(account.get());
    if (change.equals("password")) builder.password(encoder.encode("Another!Password9"));
    if (change.equals("disabled")) builder.disabled(true);
    if (change.equals("roles")) builder.roles("ADMIN");
    account.set(builder.build());
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> management.recoveryList(request));
    assertFalse(management.hasRecovery(request));
    assertEquals(0, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
  }

  @Test
  void maintenanceProtectsBothNormalManagementAndRestrictedAccess() {
    var owner = login(false);
    var request = new MockHttpServletRequest();
    management.startRecovery(request, IDENTITY, PASSWORD, false);
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(MaintenanceAuthenticationException.class, () -> management.list(owner.request()));
    assertThrows(MaintenanceAuthenticationException.class, () -> management.recoveryList(request));
    assertThrows(
        MaintenanceAuthenticationException.class,
        () -> management.finishRecovery(request, new MockHttpServletResponse()));
    assertFalse(admissions.isExpired(owner.request()));
  }

  @Test
  void canceledRecoveryCannotMutateSessions() {
    var owner = login(false);
    var request = new MockHttpServletRequest();
    management.startRecovery(request, IDENTITY, PASSWORD, false);
    management.cancelRecovery(request);
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> management.recoveryRevokeOthers(request));
    assertFalse(admissions.isExpired(owner.request()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void manualRevokeBlocksOldHttpRpcAndPushBeforeBusinessCallbacks(boolean push) throws Exception {
    var owner = login(false);
    var old = login(true);
    String handle =
        management.list(owner.request()).entries().stream()
            .filter(entry -> !entry.current())
            .findFirst()
            .orElseThrow()
            .handle();
    management.revoke(owner.request(), handle);
    assertTrue(security.hasCurrentSessionPrivileges(owner.request()));
    var oldSession = (MockHttpSession) old.request().getSession(false);
    if (push) {
      var interceptor = new PushSecurityInterceptor(security, new MaintenanceService(config));
      var request = new VaadinServletRequest(old.request(), mock(VaadinServletService.class));
      assertThrows(AccessDeniedException.class, () -> interceptor.requestStart(request, null));
    } else {
      ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
      when(provider.getObject()).thenReturn(security);
      old.request().setMethod("POST");
      old.request().setServletPath("/");
      old.request().setParameter("v-r", "uidl");
      var response = new MockHttpServletResponse();
      var business = mock(FilterChain.class);
      new VaadinSecurityFilter(provider).doFilter(old.request(), response, business);
      assertEquals(401, response.getStatus());
      verifyNoInteractions(business);
    }
    assertTrue(oldSession.isInvalid());
    assertFalse(admissions.isExpired(owner.request()));
  }

  @Test
  void failedTokenTransactionPreservesBothAuthenticationAndMetadata() {
    var owner = login(true);
    assertThrows(
        IllegalStateException.class,
        () ->
            tokens.atomic(
                () -> {
                  tokens.removeUserTokens(IDENTITY);
                  throw new IllegalStateException("force rollback");
                }));
    assertEquals(1, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertEquals(1, jdbc.queryForObject("select count(*) from remembered_device", Integer.class));
    assertFalse(admissions.isExpired(owner.request()));
  }

  @Test
  void sessionIdentifierChangePreservesDeviceHandleAndSavedLoginAssociation() {
    var owner = login(true);
    var before = management.list(owner.request()).entries().getFirst();
    String previous = owner.request().getSession().getId();
    String current = owner.request().changeSessionId();
    admissions.sessionIdChanged(
        previous,
        current,
        () ->
            registry.onApplicationEvent(
                new org.springframework.security.web.session.HttpSessionIdChangedEvent(
                    owner.request().getSession(), previous)));
    var after = management.list(owner.request()).entries().getFirst();
    assertEquals(before.handle(), after.handle());
    assertEquals(before.device(), after.device());
    assertTrue(after.current());
    assertTrue(after.remembered());
  }

  @Test
  void cleanupRemovesExpiredGrantsAndOrphanMetadataWithoutEndingActiveSessions() {
    var owner = login(true);
    var old = tokens.saved(IDENTITY, 3600).getFirst();
    jdbc.update(
        "update persistent_logins set last_used=? where series=?",
        Timestamp.from(Instant.now().minusSeconds(7200)),
        old.series());
    management.deleteExpiredSavedLogins();
    assertEquals(0, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertEquals(0, jdbc.queryForObject("select count(*) from remembered_device", Integer.class));
    assertFalse(admissions.isExpired(owner.request()));
    assertEquals(1, management.list(owner.request()).active());
  }

  @Test
  void accountWideTokenRevocationAlsoRemovesDeviceMetadata() {
    login(true);
    tokens.removeUserTokens(IDENTITY);
    assertEquals(0, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
    assertEquals(0, jdbc.queryForObject("select count(*) from remembered_device", Integer.class));
  }

  @Test
  void expiredLegacyTokensAreNotPresentedAsActiveOrSavedDevices() {
    var owner = login(false);
    tokens.createNewToken(
        new PersistentRememberMeToken(
            IDENTITY, "expired-series", "unused", Date.from(Instant.now().minusSeconds(7200))));
    assertEquals(1, management.list(owner.request()).entries().size());
    assertEquals(0, jdbc.queryForObject("select count(*) from persistent_logins", Integer.class));
  }
}
