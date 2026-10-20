package de.derpeterson.app.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.security.SessionLimitException;
import de.derpeterson.app.service.ConfigService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.util.WebUtils;

/** Echte JDBC-Commits, getrennte Repositoryinstanzen, eigene H2-Daten; kein Anwendungsserver. */
class DurableSessionPersistenceTest {
  private static final String CONTEXT =
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
  private static final String CSRF =
      "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";
  private DataSource source;
  private JdbcTemplate jdbc;
  private DurableSessionRepository<?> repository;
  private final List<JdbcIndexedSessionRepository> backends = new ArrayList<>();
  private final AtomicReference<UserDetails> account = new AtomicReference<>();
  private final AtomicReference<String> key = new AtomicReference<>("fixture-key");
  private ConfigService config;

  @BeforeEach
  void setup() {
    source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:durable-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    jdbc = new JdbcTemplate(source);
    account.set(
        User.withUsername("fixture@example.test")
            .password("fixture-encoded-password")
            .roles("USER")
            .build());
    repository = restart();
    config = mock(ConfigService.class);
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
  }

  @AfterEach
  void cleanup() throws Exception {
    for (JdbcIndexedSessionRepository backend : backends) {
      backend.destroy();
    }
    SecurityContextHolder.clearContext();
  }

  private DurableSessionRepository<?> restart() {
    JdbcIndexedSessionRepository backend =
        new SessionPersistenceConfiguration()
            .durableJdbcSessions(source, new DataSourceTransactionManager(source), 1800);
    backends.add(backend);
    return new DurableSessionRepository<>(
        backend,
        username -> {
          UserDetails current = account.get();
          if (current == null || !current.getUsername().equals(username)) {
            throw new UsernameNotFoundException("Fixture account absent");
          }
          return current;
        },
        key::get);
  }

  private SecurityContext context(boolean remembered) {
    UserDetails current = account.get();
    List<GrantedAuthority> authorities = new ArrayList<>(current.getAuthorities());
    authorities.add(
        FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
            .issuedAt(Instant.parse("2026-10-20T10:00:00Z"))
            .build());
    Authentication auth =
        remembered
            ? new RememberMeAuthenticationToken(key.get(), current, authorities)
            : UsernamePasswordAuthenticationToken.authenticated(current, null, authorities);
    return new SecurityContextImpl(auth);
  }

  private Session admitted(boolean remembered) {
    Session session = repository.createSession();
    session.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, account.get().getUsername());
    session.setAttribute(
        DurableSessionRepository.ACTIVE,
        new Active(
            session.getId(),
            "fixture-handle",
            account.get().getUsername(),
            new DeviceDescription("Linux", "Firefox", false),
            Instant.now(),
            null));
    session.setAttribute(CONTEXT, context(remembered));
    return session;
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void authenticationAndOriginalFactorTimeSurviveAFreshRepositoryWithoutRuntimeObjects(
      boolean remembered) {
    Session before = admitted(remembered);
    before.setAttribute("runtime-only", new Object());
    before.setAttribute(CSRF, new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "fixture-csrf"));
    Instant created = before.getCreationTime();
    DurableSessionRepository<?> afterRestart = restart();
    Session after = afterRestart.findById(before.getId());
    assertNotNull(after);
    assertEquals(Instant.ofEpochMilli(created.toEpochMilli()), after.getCreationTime());
    assertNull(after.getAttribute("runtime-only"));
    SecurityContext restored = after.getAttribute(CONTEXT);
    assertEquals(account.get().getUsername(), restored.getAuthentication().getName());
    assertEquals(remembered, restored.getAuthentication() instanceof RememberMeAuthenticationToken);
    FactorGrantedAuthority factor =
        restored.getAuthentication().getAuthorities().stream()
            .filter(FactorGrantedAuthority.class::isInstance)
            .map(FactorGrantedAuthority.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(Instant.parse("2026-10-20T10:00:00Z"), factor.getIssuedAt());
    CsrfToken csrf = after.getAttribute(CSRF);
    assertEquals("fixture-csrf", csrf.getToken());
    String stored =
        new String(
            jdbc.queryForObject(
                "select ATTRIBUTE_BYTES from SPRING_SESSION_ATTRIBUTES where ATTRIBUTE_NAME = ?",
                byte[].class,
                CONTEXT),
            StandardCharsets.UTF_8);
    assertFalse(stored.contains("fixture-encoded-password"));
    assertFalse(stored.contains("javaClass"));
    assertTrue(stored.startsWith("{"));
  }

  @Test
  void currentRolesAreReadInsteadOfRestoringOldAdministratorPrivileges() {
    account.set(User.withUserDetails(account.get()).roles("ADMIN").build());
    Session before = admitted(false);
    account.set(User.withUserDetails(account.get()).roles("USER").build());
    Session restored = restart().findById(before.getId());
    SecurityContext context = restored.getAttribute(CONTEXT);
    assertFalse(
        context.getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    assertTrue(
        context.getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"disabled", "deleted", "password", "key"})
  void invalidatedAccountsAndChangedRememberMeKeysCannotBeRestored(String change) {
    Session before = admitted(true);
    switch (change) {
      case "disabled" -> account.set(User.withUserDetails(account.get()).disabled(true).build());
      case "deleted" -> account.set(null);
      case "password" ->
          account.set(User.withUserDetails(account.get()).password("new-password-version").build());
      case "key" -> key.set("changed-fixture-key");
      default -> throw new IllegalArgumentException("Unknown fixture change");
    }
    Session rejected = restart().findById(before.getId());
    assertNotNull(rejected);
    assertNull(rejected.getAttribute(CONTEXT));
    assertEquals(Boolean.TRUE, rejected.getAttribute(DurableSessionRepository.EXPIRED));
    assertTrue(
        new DurableSessionRegistry<>(repository)
            .getAllSessions("fixture@example.test", false)
            .isEmpty());
  }

  @Test
  void revokedSessionStaysRevokedAfterRestartAndDoesNotOccupyAnActiveSlot() {
    Session before = admitted(false);
    DurableSessionRegistry<?> registry = new DurableSessionRegistry<>(repository);
    registry.getSessionInformation(before.getId()).expireNow();
    DurableSessionRegistry<?> restoredRegistry = new DurableSessionRegistry<>(restart());
    assertTrue(restoredRegistry.getSessionInformation(before.getId()).isExpired());
    assertTrue(restoredRegistry.getAllSessions(account.get().getUsername(), false).isEmpty());
    assertEquals(1, restoredRegistry.getAllSessions(account.get().getUsername(), true).size());
  }

  @Test
  void expiredSessionDoesNotReceiveANewLifetimeOnRestart() {
    Session before = admitted(false);
    jdbc.update(
        "update SPRING_SESSION set LAST_ACCESS_TIME = ?, EXPIRY_TIME = ? where SESSION_ID = ?",
        Instant.now().minusSeconds(1801).toEpochMilli(),
        Instant.now().minusSeconds(1).toEpochMilli(),
        before.getId());
    assertNull(restart().findById(before.getId()));
  }

  @Test
  void idRotationPreservesRuntimeMutexAndMakesExistingWrappersFollowTheNewId() {
    Session first = admitted(false);
    Session second = repository.findById(first.getId());
    Object mutex = first.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
    first.setAttribute("live-ui", new Object());
    String old = first.getId();
    String changed = first.changeSessionId();
    assertNull(repository.findById(old));
    assertEquals(changed, second.getId());
    assertSame(mutex, second.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE));
    assertSame(first.getAttribute("live-ui"), second.getAttribute("live-ui"));
    assertNotNull(restart().findById(changed));
  }

  @Test
  void aLateSaveCannotRecreateAnExplicitlyDeletedSession() {
    assertNoResurrection(repository);
  }

  private <S extends Session> void assertNoResurrection(DurableSessionRepository<S> sessions) {
    DurableSessionRepository<S>.LiveSession existing = sessions.createSession();
    existing.setAttribute(CSRF, new DefaultCsrfToken("header", "parameter", "old"));
    DurableSessionRepository<S>.LiveSession stale = sessions.findById(existing.getId());
    sessions.deleteById(existing.getId());
    assertThrows(IllegalStateException.class, () -> stale.setLastAccessedTime(Instant.now()));
    assertThrows(IllegalStateException.class, () -> sessions.save(stale));
    assertNull(sessions.findById(existing.getId()));
  }

  @Test
  void separateRequestsKeepTheSameMutexAndIndependentAttributeChanges() {
    Session first = admitted(false);
    Session second = repository.findById(first.getId());
    assertSame(
        first.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE),
        second.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE));
    first.setAttribute(CSRF, new DefaultCsrfToken("header", "parameter", "fresh"));
    second.setAttribute(DurableSessionRepository.EXPIRED, true);
    Session restored = restart().findById(first.getId());
    CsrfToken csrf = restored.getAttribute(CSRF);
    assertEquals("fresh", csrf.getToken());
    assertTrue((Boolean) restored.getAttribute(DurableSessionRepository.EXPIRED));
  }

  @Test
  void loadedRequestsReadTheLatestCsrfValueAndCannotMoveActivityBackwards() {
    Session first = admitted(false);
    Session delayed = repository.findById(first.getId());
    Instant newer = Instant.now().plusSeconds(10);
    first.setLastAccessedTime(newer);
    delayed.setLastAccessedTime(newer.minusSeconds(5));
    first.setAttribute(CSRF, new DefaultCsrfToken("header", "parameter", "new-token"));
    CsrfToken observed = delayed.getAttribute(CSRF);
    assertEquals("new-token", observed.getToken());
    assertEquals(
        newer.toEpochMilli(),
        restart().findById(first.getId()).getLastAccessedTime().toEpochMilli());
  }

  @Test
  void malformedStoredAuthenticationIsDeletedRatherThanRestoredOrRetriedForever() {
    Session before = admitted(false);
    jdbc.update(
        "update SPRING_SESSION_ATTRIBUTES set ATTRIBUTE_BYTES = ? where ATTRIBUTE_NAME = ?",
        "{invalid-json".getBytes(StandardCharsets.UTF_8),
        CONTEXT);
    Session rejected = restart().findById(before.getId());
    assertNotNull(rejected);
    assertNull(rejected.getAttribute(CONTEXT));
    assertEquals(Boolean.TRUE, rejected.getAttribute(DurableSessionRepository.EXPIRED));
    assertTrue(
        new DurableSessionRegistry<>(repository)
            .getAllSessions("fixture@example.test", false)
            .isEmpty());
  }

  @Test
  void simultaneousAdmissionsReserveOnlyOneSlot() throws Exception {
    SessionAdmissionService admissions =
        new SessionAdmissionService(config, new DurableSessionRegistry<>(repository));
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Boolean> login =
        () -> {
          ready.countDown();
          assertTrue(start.await(10, TimeUnit.SECONDS));
          AtomicReference<Boolean> admitted = new AtomicReference<>(false);
          withRequest(
              repository,
              null,
              request -> {
                try {
                  admissions.complete(
                      context(false).getAuthentication(),
                      request,
                      new MockHttpServletResponse(),
                      () -> {});
                  admitted.set(true);
                } catch (SessionLimitException expected) {
                  admitted.set(false);
                }
              });
          return admitted.get();
        };
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first = executor.submit(login);
      Future<Boolean> second = executor.submit(login);
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      assertEquals(
          1,
          (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0));
      assertEquals(1, admissions.active(account.get().getUsername()).size());
    } finally {
      start.countDown();
    }
  }

  @Test
  void persistedReservationCountsBeforeAuthenticationIsPublished() {
    Session session = repository.createSession();
    DurableSessionRegistry<?> registry = new DurableSessionRegistry<>(repository);
    registry.registerNewSession(session.getId(), account.get().getUsername());
    DurableSessionRegistry<?> fresh = new DurableSessionRegistry<>(restart());
    assertEquals(1, fresh.getAllSessions(account.get().getUsername(), false).size());
    registry.removeSessionInformation(session.getId());
    assertTrue(fresh.getAllSessions(account.get().getUsername(), false).isEmpty());
  }

  @Test
  void durableAdmissionReusesTheOldSlotAfterRestartButRejectsAnotherSession() throws Exception {
    Session previous = admitted(false);
    DurableSessionRepository<?> restarted = restart();
    SessionAdmissionService admissions =
        new SessionAdmissionService(config, new DurableSessionRegistry<>(restarted));
    assertEquals(1, admissions.active(account.get().getUsername()).size());
    assertEquals(
        "fixture-handle", admissions.active(account.get().getUsername()).getFirst().handle());
    Authentication auth = context(false).getAuthentication();
    withRequest(
        restarted,
        previous.getId(),
        request -> {
          admissions.complete(auth, request, new MockHttpServletResponse(), () -> {});
          assertEquals(previous.getId(), request.getSession(false).getId());
        });
    withRequest(
        restarted,
        null,
        request ->
            assertThrows(
                SessionLimitException.class,
                () -> admissions.complete(auth, request, new MockHttpServletResponse(), () -> {})));
    assertEquals(1, admissions.active(account.get().getUsername()).size());
  }

  @Test
  void failedCompletionReleasesTheDurableReservationImmediately() throws Exception {
    SessionAdmissionService admissions =
        new SessionAdmissionService(config, new DurableSessionRegistry<>(repository));
    withRequest(
        repository,
        null,
        request ->
            assertThrows(
                IllegalStateException.class,
                () ->
                    admissions.complete(
                        context(false).getAuthentication(),
                        request,
                        new MockHttpServletResponse(),
                        () -> {
                          throw new IllegalStateException("fixture completion failure");
                        })));
    assertTrue(admissions.active(account.get().getUsername()).isEmpty());
    assertEquals(0, jdbc.queryForObject("select count(*) from SPRING_SESSION", Integer.class));
  }

  @Test
  void passwordChangedBetweenAuthenticationAndPersistenceCannotAdoptTheNewVersion() {
    SecurityContext authenticated = context(false);
    account.set(
        User.withUsername(account.get().getUsername())
            .password("different-encoded-password")
            .roles("USER")
            .build());
    Session session = repository.createSession();
    assertThrows(
        org.springframework.security.authentication.AuthenticationServiceException.class,
        () -> session.setAttribute(CONTEXT, authenticated));
    assertNull(repository.findById(session.getId()).getAttribute(CONTEXT));
  }

  @Test
  void ownPasswordChangeKeepsOnlyTheExplicitCurrentSessionAcrossRestart() {
    Session own = admitted(false);
    Session other = admitted(false);
    account.set(
        User.withUsername(account.get().getUsername())
            .password("changed-encoded-password")
            .roles("USER")
            .build());
    new DurableSessionRegistry<>(repository)
        .retainAfterPasswordChange(own.getId(), account.get().getUsername());
    DurableSessionRepository<?> fresh = restart();
    assertNotNull(fresh.findById(own.getId()).getAttribute(CONTEXT));
    Session rejected = fresh.findById(other.getId());
    assertNull(rejected.getAttribute(CONTEXT));
    assertEquals(Boolean.TRUE, rejected.getAttribute(DurableSessionRepository.EXPIRED));
  }

  @Test
  void durableCodecRejectsNullUnknownVersionAndPolymorphicKinds() {
    SessionAttributeCodec codec = new SessionAttributeCodec();
    for (String invalid :
        List.of(
            "null",
            "{\"version\":2,\"kind\":\"text\",\"value\":\"x\"}",
            "{\"version\":1,\"kind\":\"java.lang.Runtime\",\"value\":{}}",
            "{\"version\":1,\"kind\":\"text\",\"value\":null}")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> codec.decode(invalid.getBytes(StandardCharsets.UTF_8)));
    }
    assertThrows(IllegalArgumentException.class, () -> codec.encode("x".repeat(65536)));
  }

  @Test
  void deletingRuntimeDestroysEachVaadinSessionOnlyOnce() {
    Session session = admitted(false);
    com.vaadin.flow.server.VaadinSession ui = mock(com.vaadin.flow.server.VaadinSession.class);
    com.vaadin.flow.server.VaadinService service = mock(com.vaadin.flow.server.VaadinService.class);
    when(ui.getService()).thenReturn(service);
    session.setAttribute("runtime-a", ui);
    session.setAttribute("runtime-b", ui);
    repository.deleteById(session.getId());
    repository.deleteById(session.getId());
    org.mockito.Mockito.verify(service).fireSessionDestroy(ui);
    assertNull(repository.findById(session.getId()));
  }

  private <S extends Session> void withRequest(
      FindByIndexNameSessionRepository<S> sessions,
      String id,
      java.util.function.Consumer<jakarta.servlet.http.HttpServletRequest> action)
      throws Exception {
    SessionRepositoryFilter<S> filter = new SessionRepositoryFilter<>(sessions);
    org.springframework.session.web.http.CookieHttpSessionIdResolver cookies =
        new org.springframework.session.web.http.CookieHttpSessionIdResolver();
    org.springframework.session.web.http.DefaultCookieSerializer serializer =
        new SessionPersistenceConfiguration().cookieSerializer();
    serializer.setUseBase64Encoding(false);
    cookies.setCookieSerializer(serializer);
    filter.setHttpSessionIdResolver(cookies);
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (id != null) {
      request.setCookies(new jakarta.servlet.http.Cookie("JSESSIONID", id));
    }
    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> action.accept((jakarta.servlet.http.HttpServletRequest) req));
  }
}
