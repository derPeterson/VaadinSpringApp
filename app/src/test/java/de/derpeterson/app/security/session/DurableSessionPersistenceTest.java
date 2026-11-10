package de.derpeterson.app.security.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.server.WrappedHttpSession;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.ExternalAuthenticationToken;
import de.derpeterson.app.security.PortalUserDetails;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.SessionAdmissionService.Active;
import de.derpeterson.app.security.SessionLimitException;
import de.derpeterson.app.security.SignInMethod;
import de.derpeterson.app.security.VaadinSecurityFilter;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
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
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.WebUtils;

/** Echte JDBC-Commits, getrennte Repositoryinstanzen, eigene H2-Daten; kein Anwendungsserver. */
class DurableSessionPersistenceTest {
  private static final String CONTEXT =
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
  private static final String CSRF =
      "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";
  private DataSource source;
  private JdbcTemplate jdbcTemplate;
  private DurableSessionRepository<?> repository;
  private final List<JdbcIndexedSessionRepository> backends = new ArrayList<>();
  private final AtomicReference<UserDetails> account = new AtomicReference<>();
  private final AtomicReference<String> key = new AtomicReference<>("fixture-key");
  private ConfigService configService;

  @BeforeEach
  void setup() {
    source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:durable-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    jdbcTemplate = new JdbcTemplate(source);
    account.set(
        User.withUsername("fixture@example.test")
            .password("fixture-encoded-password")
            .roles("USER")
            .build());
    configService = mock(ConfigService.class);
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    when(configService.getInteger(ConfigEntry.SESSION_TIMEOUT_SECONDS)).thenReturn(1800);
    repository = restart();
  }

  @AfterEach
  void cleanup() {
    for (JdbcIndexedSessionRepository backend : backends) {
      backend.destroy();
    }
    SecurityContextHolder.clearContext();
  }

  private DurableSessionRepository<?> restart() {
    JdbcIndexedSessionRepository backend =
        new SessionPersistenceConfiguration()
            .durableJdbcSessions(source, new DataSourceTransactionManager(source), configService);
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
        key::get,
        () -> SessionPersistenceConfiguration.sessionTimeout(configService));
  }

  @Test
  void changedTimeoutAppliesImmediatelyOnlyToNewSessions() {
    Session existing = repository.createSession();

    assertEquals(Duration.ofMinutes(30), existing.getMaxInactiveInterval());

    when(configService.getInteger(ConfigEntry.SESSION_TIMEOUT_SECONDS)).thenReturn(10);

    assertEquals(
        Duration.ofMinutes(30), repository.findById(existing.getId()).getMaxInactiveInterval());
    assertEquals(Duration.ofSeconds(10), repository.createSession().getMaxInactiveInterval());

    DurableSessionRepository<?> restarted = restart();

    assertEquals(
        Duration.ofMinutes(30), restarted.findById(existing.getId()).getMaxInactiveInterval());
    assertEquals(Duration.ofSeconds(10), restarted.createSession().getMaxInactiveInterval());
  }

  @Test
  void invalidTimeoutIsRejectedAtStartup() {
    when(configService.getInteger(ConfigEntry.SESSION_TIMEOUT_SECONDS)).thenReturn(0);

    assertThrows(IllegalArgumentException.class, this::restart);
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

  @Test
  void passwordlessExternalAuthenticationSurvivesJdbcRestartAndAccessVersionRevokesIt() {
    String version = UUID.randomUUID().toString();
    UserDetails external =
        new de.derpeterson.app.security.PortalUserDetails(
            "external@example.test",
            null,
            version,
            true,
            List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_USER")));
    account.set(external);
    Session before = repository.createSession();
    before.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, external.getUsername());
    before.setAttribute(
        DurableSessionRepository.ACTIVE,
        new Active(
            before.getId(),
            "external-handle",
            external.getUsername(),
            new DeviceDescription("Linux", "Firefox", false),
            Instant.now(),
            null));
    before.setAttribute(
        CONTEXT,
        new SecurityContextImpl(
            new de.derpeterson.app.security.ExternalAuthenticationToken(external, "microsoft")));
    DurableSessionRepository<?> restarted = restart();
    Session restored = restarted.findById(before.getId());

    assertNotNull(restored);
    assertEquals(
        SignInMethod.MICROSOFT,
        new DurableSessionRegistry<>(restarted).metadata(before.getId()).signInMethod());
    SecurityContext security = restored.getAttribute(CONTEXT);

    assertTrue(
        security.getAuthentication()
            instanceof de.derpeterson.app.security.ExternalAuthenticationToken);
    assertTrue(
        security.getAuthentication().getAuthorities().stream()
            .noneMatch(FactorGrantedAuthority.class::isInstance));
    account.set(
        new de.derpeterson.app.security.PortalUserDetails(
            "external@example.test",
            null,
            UUID.randomUUID().toString(),
            true,
            external.getAuthorities()));
    Session revoked = restart().findById(before.getId());

    assertNull(revoked.getAttribute(CONTEXT));
    assertEquals(Boolean.TRUE, revoked.getAttribute(DurableSessionRepository.EXPIRED));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void maximumAccountIdentityRemainsAuthenticatedAfterRepositoryRestart(boolean remembered) {
    String identity =
        "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62);

    assertEquals(255, identity.length());
    account.set(
        User.withUsername(identity).password("fixture-encoded-password").roles("USER").build());
    Session session = admitted(remembered);
    DurableSessionRepository<?> restarted = restart();
    Session restored = restarted.findById(session.getId());

    assertNotNull(restored);
    SecurityContext restoredContext = restored.getAttribute(CONTEXT);

    assertNotNull(restoredContext);
    assertEquals(identity, restoredContext.getAuthentication().getName());
    assertTrue(restoredContext.getAuthentication().isAuthenticated());
    assertEquals(
        remembered ? SignInMethod.SAVED_LOGIN : SignInMethod.PORTAL,
        new DurableSessionRegistry<>(restarted).metadata(session.getId()).signInMethod());
    assertEquals(1, restarted.findByPrincipalName(identity).size());
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
            jdbcTemplate.queryForObject(
                "select ATTRIBUTE_BYTES from SPRING_SESSION_ATTRIBUTES where ATTRIBUTE_NAME = ?",
                byte[].class,
                CONTEXT),
            StandardCharsets.UTF_8);

    assertFalse(stored.contains("fixture-encoded-password"));
    assertFalse(stored.contains("javaClass"));
    assertTrue(stored.startsWith("{"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"null-value", "remove", "empty-context"})
  void removedSecurityAndRuntimeAttributesStayAbsentAfterRestart(String mode) {
    DurableSessionRepository<?>.LiveSession session = repository.findById(admitted(false).getId());

    assertNotNull(session);
    session.setAttribute(CSRF, new DefaultCsrfToken("header", "parameter", "fixture"));
    session.setAttribute("runtime-only", new Object());
    session.setAttribute(CSRF, null);
    session.setAttribute("runtime-only", null);
    switch (mode) {
      case "null-value" -> session.setAttribute(CONTEXT, null);
      case "remove" -> session.removeAttribute(CONTEXT);
      case "empty-context" -> session.setAttribute(CONTEXT, new SecurityContextImpl());
      default -> throw new IllegalArgumentException(mode);
    }
    assertNull(session.getAttribute(CONTEXT));
    assertNull(session.getAttribute(CSRF));
    assertNull(session.getAttribute("runtime-only"));
    assertFalse(session.getAttributeNames().contains(CONTEXT));
    Session restored = restart().findById(session.getId());

    assertNotNull(restored);
    assertNotNull(restored.getAttribute(DurableSessionRepository.ACTIVE));
    assertNull(restored.getAttribute(CONTEXT));
    assertNull(restored.getAttribute(CSRF));
    assertNull(restored.getAttribute(DurableSessionRepository.EXPIRED));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from SPRING_SESSION_ATTRIBUTES where ATTRIBUTE_NAME in (?, ?)",
            Integer.class,
            CONTEXT,
            CSRF));
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
    jdbcTemplate.update(
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
    Instant accessed = Instant.now();

    assertThrows(IllegalStateException.class, () -> stale.setLastAccessedTime(accessed));
    assertDoesNotThrow(() -> sessions.save(stale));
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
    jdbcTemplate.update(
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
        new SessionAdmissionService(configService, new DurableSessionRegistry<>(repository));
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
              mockHttpRequest -> {
                try {
                  admissions.complete(
                      context(false).getAuthentication(),
                      mockHttpRequest,
                      new MockHttpServletResponse(),
                      () -> {});
                  admitted.set(true);
                } catch (SessionLimitException _) {
                  admitted.set(false);
                }
              });
          return admitted.get();
        };
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first = executorService.submit(login);
      Future<Boolean> second = executorService.submit(login);

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
        new SessionAdmissionService(configService, new DurableSessionRegistry<>(restarted));

    assertEquals(1, admissions.active(account.get().getUsername()).size());
    assertEquals(
        "fixture-handle", admissions.active(account.get().getUsername()).getFirst().handle());
    Authentication auth = context(false).getAuthentication();
    withRequest(
        restarted,
        previous.getId(),
        mockHttpRequest -> {
          admissions.complete(auth, mockHttpRequest, new MockHttpServletResponse(), () -> {});

          assertEquals(previous.getId(), mockHttpRequest.getSession(false).getId());
        });
    withRequest(
        restarted,
        null,
        mockHttpRequest -> {
          MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();

          assertThrows(
              SessionLimitException.class,
              () -> admissions.complete(auth, mockHttpRequest, mockHttpResponse, () -> {}));
        });

    assertEquals(1, admissions.active(account.get().getUsername()).size());
  }

  @Test
  void failedCompletionReleasesTheDurableReservationImmediately() throws Exception {
    SessionAdmissionService admissions =
        new SessionAdmissionService(configService, new DurableSessionRegistry<>(repository));
    Authentication authentication = context(false).getAuthentication();
    withRequest(
        repository,
        null,
        mockHttpRequest -> {
          MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
          Runnable completion =
              () -> {
                throw new IllegalStateException("fixture completion failure");
              };

          assertThrows(
              IllegalStateException.class,
              () ->
                  admissions.complete(
                      authentication, mockHttpRequest, mockHttpResponse, completion));
        });

    assertTrue(admissions.active(account.get().getUsername()).isEmpty());
    assertEquals(
        0, jdbcTemplate.queryForObject("select count(*) from SPRING_SESSION", Integer.class));
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

  @ParameterizedTest
  @CsvSource({"PORTAL,true", "GOOGLE,false", "MICROSOFT,true", "SAVED_LOGIN,false"})
  void disconnectRetainsOnlyAStillUsableSignIn(SignInMethod source, boolean survives) {
    UserDetails user = account.get();
    Session current = repository.createSession();
    current.setAttribute(
        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.getUsername());
    SecurityContext signIn =
        switch (source) {
          case PORTAL -> context(false);
          case SAVED_LOGIN -> context(true);
          case GOOGLE, MICROSOFT ->
              new SecurityContextImpl(
                  new ExternalAuthenticationToken(
                      user, source.name().toLowerCase(java.util.Locale.ROOT)));
        };
    current.setAttribute(CONTEXT, signIn);
    account.set(
        User.withUsername(user.getUsername()).password("new-version").roles("USER").build());
    DurableSessionRegistry<?> registry = new DurableSessionRegistry<>(repository);

    assertEquals(
        survives,
        registry.retainAfterProviderUnlink(
            current.getId(),
            user.getUsername(),
            PortalUserDetails.version(user),
            PortalUserDetails.version(account.get()),
            Set.of(SignInMethod.PORTAL, SignInMethod.MICROSOFT)));
    SecurityContext restored = restart().findById(current.getId()).getAttribute(CONTEXT);
    if (survives) {
      assertEquals(
          source,
          DurableAuthentication.capture(restored.getAuthentication(), ignored -> account.get())
              .signInMethod());
    } else {
      assertNull(restored);
    }
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
      byte[] bytes = invalid.getBytes(StandardCharsets.UTF_8);

      assertThrows(IllegalArgumentException.class, () -> codec.decode(bytes));
    }
    String oversized = "x".repeat(65536);

    assertThrows(IllegalArgumentException.class, () -> codec.encode(oversized));
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

    verify(service).fireSessionDestroy(ui);
    assertNull(repository.findById(session.getId()));
  }

  @Test
  void concurrentLogoutDoesNotFailTheOtherHttpRequestsFinalCommitOrRecreateItsSession()
      throws Exception {
    Session existing = admitted(false);
    String id = existing.getId();
    withRequest(
        repository,
        id,
        mockHttpRequest -> {
          assertNotNull(mockHttpRequest.getSession(false));
          repository.deleteById(id);
        });

    assertNull(repository.findById(id));
    assertNull(restart().findById(id));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from SPRING_SESSION where SESSION_ID = ?", Integer.class, id));
  }

  @ParameterizedTest
  @ValueSource(strings = {"login", "uidl", "anonymous-uidl", "privileges"})
  void concurrentDeletionRejectsOldCallbacksButKeepsTheLoginDocumentAvailable(String entry)
      throws Exception {
    Session existing = admitted(false);
    String id = existing.getId();
    SessionAdmissionService admissions =
        new SessionAdmissionService(configService, new DurableSessionRegistry<>(repository));
    SecurityService security =
        new SecurityService(
            mock(PersistentTokenBasedRememberMeServices.class),
            mock(UserService.class),
            mock(UserRepository.class),
            mock(CustomUserDetailsService.class),
            new HttpSessionSecurityContextRepository(),
            admissions);
    @SuppressWarnings("unchecked")
    ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(security);
    withRequest(
        repository,
        id,
        mockHttpRequest -> {
          assertNotNull(mockHttpRequest.getSession(false));
          if (!"anonymous-uidl".equals(entry)) {
            SecurityContextHolder.setContext(context(false));
          }
          repository.deleteById(id);
          if ("privileges".equals(entry)) {
            assertFalse(security.hasCurrentSessionPrivileges(mockHttpRequest));
          } else {
            boolean login = "login".equals(entry);
            HttpServletRequestWrapper callback =
                new HttpServletRequestWrapper(mockHttpRequest) {
                  @Override
                  public String getMethod() {
                    return login ? "GET" : "POST";
                  }

                  @Override
                  public String getServletPath() {
                    return login ? "/login" : "/account";
                  }

                  @Override
                  public String getParameter(String name) {
                    return "v-r".equals(name) && !login ? "uidl" : super.getParameter(name);
                  }
                };
            MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
            AtomicBoolean reachedApplication = new AtomicBoolean();

            assertDoesNotThrow(() ->
                new VaadinSecurityFilter(provider)
                    .doFilter(
                        callback, mockHttpResponse, (req, res) -> reachedApplication.set(true)));
            assertEquals(login, reachedApplication.get());
            assertEquals(login ? 200 : 401, mockHttpResponse.getStatus());
          }
          assertNull(mockHttpRequest.getSession(false));
          assertNull(SecurityContextHolder.getContext().getAuthentication());
          assertTrue(security.getAuthenticatedUser(mockHttpRequest).isEmpty());
        });

    assertNull(restart().findById(id));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from SPRING_SESSION where SESSION_ID = ?", Integer.class, id));
  }

  @Test
  void logoutKeepsTheCurrentVaadinUiAttachedUntilItsResponseHasBeenWritten() {
    Session stored = admitted(false);
    VaadinService vaadinService = mock(VaadinService.class);
    DeploymentConfiguration configuration = mock(DeploymentConfiguration.class);
    when(vaadinService.getDeploymentConfiguration()).thenReturn(configuration);
    VaadinSession live = new VaadinSession(vaadinService);
    ReflectionTestUtils.setField(live, "lock", new ReentrantLock());
    ReflectionTestUtils.setField(live, "sessionLockCheckStrategy", SessionLockCheckStrategy.THROW);
    ReflectionTestUtils.setField(live, "session", new WrappedHttpSession(new MockHttpSession()));
    UI ui = new UI();
    live.lock();
    VaadinSession.setCurrent(live);
    CurrentInstance.set(VaadinRequest.class, mock(VaadinRequest.class));
    try {
      ui.getInternals().setSession(live);
      stored.setAttribute("runtime-ui", live);
      repository.deleteById(stored.getId());

      assertSame(live, ui.getSession());
      assertEquals(VaadinSessionState.CLOSING, live.getState());
      assertNull(live.getSession());
      verify(vaadinService, never()).fireSessionDestroy(live);
    } finally {
      CurrentInstance.set(VaadinRequest.class, null);
      VaadinSession.setCurrent(null);
      live.unlock();
    }
    assertNull(repository.findById(stored.getId()));
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
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    if (id != null) {
      mockHttpRequest.setCookies(new jakarta.servlet.http.Cookie("JSESSIONID", id));
    }
    filter.doFilter(
        mockHttpRequest,
        new MockHttpServletResponse(),
        (req, res) -> action.accept((jakarta.servlet.http.HttpServletRequest) req));
  }
}
