package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.LoginAuthenticationService;
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.security.SecurityConfig;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.SessionLimitException;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.security.SessionManagementService.Snapshot;
import de.derpeterson.app.security.VaadinSecurityFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/** Actual SecurityConfig/filter order in an isolated web context, without a running server. */
@SpringJUnitConfig(SecurityChainRegressionTest.Config.class)
@WebAppConfiguration
class SecurityChainRegressionTest {
  @Autowired SecurityFilterChain chain;
  @Autowired FilterRegistrationBean<VaadinSecurityFilter> registration;
  @Autowired UserService users;
  @Autowired UserRepository repository;
  @Autowired TransactionTemplate transaction;
  @Autowired ConfigService settings;
  @Autowired AuthenticationManager passwordManager;
  @Autowired PasswordEncoder encoder;
  @Autowired LoginAttemptRepository attempts;
  @PersistenceContext EntityManager entityManager;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(10);
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
  }

  @Test
  void accountRouteRequiresAuthenticationAndUsesTheNormalLogin() throws Exception {
    for (String path :
        java.util.List.of(
            AppRoute.ACCOUNT.getSecurityRoute(), AppRoute.NOTIFICATIONS.getSecurityRoute())) {
      when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
      request.setServletPath(path);
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain servlet = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(request, response, servlet);
      assertTrue(response.getRedirectedUrl().endsWith(AppRoute.LOGIN.getSecurityRoute()));
      verifyNoInteractions(servlet);
    }
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void accountRouteAllowsEveryAccountRoleAndRespectsMaintenance(RoleType role) throws Exception {
    String email = sessionUser(role);
    PasswordResponse login = postPassword(email, "OriginalPassword!");
    MockHttpSession session = (MockHttpSession) login.request().getSession(false);
    for (String path :
        java.util.List.of(
            AppRoute.ACCOUNT.getSecurityRoute(), AppRoute.NOTIFICATIONS.getSecurityRoute())) {
      when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
      request.setServletPath(path);
      request.setSession(session);
      FilterChain servlet = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(request, new MockHttpServletResponse(), servlet);
      verify(servlet).doFilter(any(), any());
      clearInvocations(servlet);
      when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
      MockHttpServletResponse response = new MockHttpServletResponse();
      new FilterChainProxy(chain).doFilter(request, response, servlet);
      if (role.isAdministrative()) {
        verify(servlet).doFilter(any(), any());
      } else {
        assertEquals(503, response.getStatus());
        assertEquals(AppRoute.MAINTENANCE.getSecurityRoute(), response.getForwardedUrl());
        verifyNoInteractions(servlet);
      }
    }
  }

  @Test
  void revalidationRunsAfterRememberMeBeforeAuthorizationAndNotAsAContainerFilter() {
    List<Filter> filters = chain.getFilters();
    assertTrue(
        index(filters, RememberMeAuthenticationFilter.class)
            < index(filters, VaadinSecurityFilter.class));
    assertTrue(
        index(filters, VaadinSecurityFilter.class) < index(filters, AuthorizationFilter.class));
    assertFalse(registration.isEnabled());
  }

  private int index(List<Filter> filters, Class<?> type) {
    for (int i = 0; i < filters.size(); i++) {
      if (type.isInstance(filters.get(i))) {
        return i;
      }
    }
    fail("Missing filter: " + type.getSimpleName());
    return -1;
  }

  @ParameterizedTest
  @ValueSource(strings = {"/admin", "/"})
  void actualChainUsesFreshRolesAndKeepsTheLogin(String path) throws Exception {
    Long victimId =
        transaction.execute(
            tx -> {
              repository.deleteAll();
              repository.flush();
              entityManager.createQuery("delete from RoleEntity").executeUpdate();
              RoleEntity admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
              entityManager.persist(admin);
              Long victim = create("chain-victim@example.com", admin);
              create("chain-survivor@example.com", admin);
              return victim;
            });
    UserDetails principal =
        new CustomUserDetailsService(repository).loadUserByUsername("chain-victim@example.com");
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    request.setSession(session);
    if (path.equals("/admin")) {
      // Bootstrap before revocation: the POST itself must refresh the old authorities.
      addCsrfToken(request);
    } else {
      request.setParameter("v-r", "uidl");
    }
    UserEntity edited =
        repository.findAll().stream()
            .filter(user -> user.getId().equals(victimId))
            .findFirst()
            .orElseThrow();
    edited.setRoleEntities(new ArrayList<>());
    users.updateAdminUser(edited, edited.getVersion(), "");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain action = mock(FilterChain.class);
    new FilterChainProxy(chain).doFilter(request, response, action);
    assertFalse(session.isInvalid());
    if (path.equals("/admin")) {
      assertEquals("/home?error=admin-access-denied", response.getRedirectedUrl());
      verifyNoInteractions(action);
    } else {
      assertEquals(200, response.getStatus());
      verify(action).doFilter(any(), any());
    }
    SecurityContext refreshed =
        (org.springframework.security.core.context.SecurityContext)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    assertNotNull(refreshed.getAuthentication());
    assertTrue(refreshed.getAuthentication().getAuthorities().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void currentRoleGrantsHomeAndOnlyAdministratorsReachAdmin(RoleType roleType) throws Exception {
    transaction.executeWithoutResult(
        tx -> {
          repository.deleteAll();
          repository.flush();
          entityManager.createQuery("delete from RoleEntity").executeUpdate();
          RoleEntity role = RoleEntity.builder().name(roleType).build();
          entityManager.persist(role);
          create("role-access@example.test", role);
        });
    UserDetails principal =
        new CustomUserDetailsService(repository).loadUserByUsername("role-access@example.test");
    Set<String> expected =
        roleType == RoleType.ROLE_SUPERADMIN
            ? Set.of("ROLE_SUPERADMIN", "ROLE_ADMIN")
            : Set.of(roleType.name());
    assertEquals(
        expected,
        principal.getAuthorities().stream().map(Object::toString).collect(Collectors.toSet()));
    assertTrue(repository.findAll().getFirst().hasRole(roleType));
    if (roleType == RoleType.ROLE_SUPERADMIN) {
      assertFalse(
          repository.findAll().getFirst().hasRole(RoleType.ROLE_ADMIN),
          "Inherited permission must not add a stored assignment");
    }
    for (String path : List.of("/home", "/admin")) {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated(
              principal, null, principal.getAuthorities()));
      MockHttpSession session = new MockHttpSession();
      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
      request.setServletPath(path);
      request.setSession(session);
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain action = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(request, response, action);
      if (path.equals("/home")
          || roleType == RoleType.ROLE_ADMIN
          || roleType == RoleType.ROLE_SUPERADMIN) {
        verify(action).doFilter(any(), any());
        assertEquals(200, response.getStatus());
      } else {
        verifyNoInteractions(action);
        assertEquals(302, response.getStatus());
        assertEquals("/home?error=admin-access-denied", response.getRedirectedUrl());
      }
    }
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void actualHttpPasswordFilterCannotBypassThrottleAndExistingSessionSurvives(RoleType roleType)
      throws Exception {
    ProviderManager providers = assertInstanceOf(ProviderManager.class, passwordManager);
    assertTrue(
        providers.getProviders().stream().anyMatch(LoginAuthenticationService.class::isInstance));
    String email = "password-" + UUID.randomUUID() + "@example.test";
    transaction.executeWithoutResult(
        tx -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
                  .setParameter("role", roleType)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(roleType).build();
            entityManager.persist(role);
          }
          Long id = create(email, role);
          entityManager.find(UserEntity.class, id).setPassword(encoder.encode("OriginalPassword!"));
        });
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(2);
    PasswordResponse successful = postPassword(email, "OriginalPassword!");
    assertEquals(
        roleType.isAdministrative() ? "/admin" : "/home", successful.response().getRedirectedUrl());
    HttpSession originalSession = successful.request().getSession(false);
    assertNotNull(
        originalSession.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(
        "/login?error=invalid",
        postPassword(email, "WrongPassword!").response().getRedirectedUrl());
    assertEquals(
        "/login?error=invalid",
        postPassword(email, "WrongPassword!").response().getRedirectedUrl());
    PasswordResponse blocked = postPassword(email, "OriginalPassword!");
    assertEquals("/login?error=throttled", blocked.response().getRedirectedUrl());
    assertFalse(
        Arrays.stream(blocked.response().getCookies())
            .anyMatch(cookie -> cookie.getName().equals("remember-me") && cookie.getMaxAge() != 0));
    assertTrue(
        blocked.request().getSession(false) == null
            || blocked
                    .request()
                    .getSession(false)
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                == null);
    assertEquals(
        2, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
    SecurityContext saved =
        (SecurityContext)
            originalSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    UserDetails oldPrincipal = (UserDetails) saved.getAuthentication().getPrincipal();
    UserDetails currentPrincipal =
        new CustomUserDetailsService(repository).loadUserByUsername(email);
    Set<String> currentRoles =
        currentPrincipal.getAuthorities().stream()
            .map(Object::toString)
            .collect(Collectors.toSet());
    assertEquals(
        currentRoles,
        oldPrincipal.getAuthorities().stream()
            .filter(authority -> !(authority instanceof FactorGrantedAuthority))
            .map(Object::toString)
            .collect(Collectors.toSet()));
    assertTrue(
        saved.getAuthentication().getAuthorities().stream()
            .anyMatch(FactorGrantedAuthority.class::isInstance));
    assertTrue(currentPrincipal.isEnabled());
    assertTrue(oldPrincipal.isEnabled());
    MockHttpServletRequest oldRequest =
        new MockHttpServletRequest("GET", roleType.isAdministrative() ? "/admin" : "/home");
    oldRequest.setServletPath(oldRequest.getRequestURI());
    oldRequest.setSession(originalSession);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);
    new FilterChainProxy(chain).doFilter(oldRequest, response, servlet);
    assertEquals(
        200,
        response.getStatus(),
        "Existing session: " + response.getRedirectedUrl() + " / " + response.getErrorMessage());
    verify(servlet).doFilter(any(), any());
    // Fixture-only state change proves framework factor evidence cannot preserve a disabled
    // account.
    transaction.executeWithoutResult(
        tx -> repository.findByEmail(email).orElseThrow().setEnabled(false));
    MockHttpServletRequest revoked = new MockHttpServletRequest("GET", "/home");
    revoked.setServletPath("/home");
    revoked.setSession(originalSession);
    MockHttpServletResponse denied = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(revoked, denied, mock(FilterChain.class));
    assertEquals(401, denied.getStatus());
  }

  @Test
  void unknownHttpIdentityIsLimitedAndTechnicalConfigurationFailureDoesNotCount() throws Exception {
    String email = "unknown-" + UUID.randomUUID() + "@example.test";
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(1);
    assertEquals(
        "/login?error=invalid",
        postPassword(email, "WrongPassword!").response().getRedirectedUrl());
    assertEquals(
        "/login?error=throttled",
        postPassword(email, "WrongPassword!").response().getRedirectedUrl());
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(0);
    String other = "technical-" + UUID.randomUUID() + "@example.test";
    assertEquals(
        "/login?error=unavailable",
        postPassword(other, "WrongPassword!").response().getRedirectedUrl());
    assertFalse(attempts.existsById(LoginAttemptService.identityKey(other)));
  }

  @Test
  void rememberMeUsesItsOwnProviderWithoutPasswordCountersEvenWithInvalidPasswordLimit() {
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(0);
    long before = attempts.count();
    UserDetails principal =
        User.withUsername("remember-fixture@example.test").password("unused").roles("USER").build();
    RememberMeAuthenticationToken token =
        new RememberMeAuthenticationToken(
            "isolated-test-key", principal, principal.getAuthorities());
    assertTrue(passwordManager.authenticate(token).isAuthenticated());
    RememberMeAuthenticationToken forged =
        new RememberMeAuthenticationToken("wrong-key", principal, principal.getAuthorities());
    assertThrows(BadCredentialsException.class, () -> passwordManager.authenticate(forged));
    assertEquals(before, attempts.count());
  }

  @Autowired SessionManagementService management;

  @Test
  void reachedLimitOffersManagementButCannotOpenAccountUntilNormalAdmission() throws Exception {
    String email = sessionUser(RoleType.ROLE_USER);
    when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    try {
      PasswordResponse old = postPassword(email, "OriginalPassword!");
      PasswordResponse restricted = postPassword(email, "OriginalPassword!");
      assertEquals("/login?error=sessions", restricted.response().getRedirectedUrl());
      assertTrue(management.hasRecovery(restricted.request()));
      assertNull(
          restricted
              .request()
              .getSession()
              .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
      request.setServletPath("/account");
      request.setSession(restricted.request().getSession(false));
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain business = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(request, response, business);
      verifyNoInteractions(business);
      assertTrue(response.getRedirectedUrl().endsWith("/login"));
      Snapshot list = management.recoveryList(request);
      assertEquals(1, list.active());
      management.recoveryRevoke(request, list.entries().getFirst().handle());
      assertTrue(admissions.isExpired(old.request()));
      assertFalse(management.finishRecovery(request, new MockHttpServletResponse()));
      assertFalse(management.hasRecovery(request));
      MockHttpServletRequest admitted = new MockHttpServletRequest("GET", "/account");
      admitted.setServletPath("/account");
      admitted.setSession(request.getSession(false));
      new FilterChainProxy(chain).doFilter(admitted, new MockHttpServletResponse(), business);
      verify(business).doFilter(any(), any());
    } finally {
      when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    }
  }

  @Test
  void restrictedEntryPointUsesTheSamePersistentPasswordAttemptProtection() {
    String email = sessionUser(RoleType.ROLE_USER);
    when(settings.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(1);
    MockHttpServletRequest request = new MockHttpServletRequest();
    assertThrows(
        BadCredentialsException.class,
        () -> management.startRecovery(request, email, "WrongPassword!", false));
    assertThrows(
        LoginThrottledException.class,
        () -> management.startRecovery(request, email, "OriginalPassword!", false));
    assertFalse(management.hasRecovery(request));
    assertEquals(
        1, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
  }

  @Autowired SessionAdmissionService admissions;
  @Autowired SessionRegistry sessionRegistry;

  @Autowired PersistentTokenRepository tokens;

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void httpAndVaadinShareTheLimitAndLogoutFreesTheSlot(RoleType roleType) throws Exception {
    String email = sessionUser(roleType);
    when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    try {
      PasswordResponse first = postPassword(email, "OriginalPassword!");
      HttpSession session = first.request().getSession(false);
      assertNotNull(session);
      UserDetails principal = new CustomUserDetailsService(repository).loadUserByUsername(email);
      PasswordResponse denied = postPassword(email, "OriginalPassword!");
      assertEquals("/login?error=sessions", denied.response().getRedirectedUrl());
      assertEquals(
          0, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
      MockHttpServletRequest attemptRequest = new MockHttpServletRequest();
      MockHttpServletResponse attemptResponse = new MockHttpServletResponse();
      assertThrows(
          SessionLimitException.class,
          () ->
              actualSecurity.storeAuthenticatedUser(
                  attemptRequest, attemptResponse, principal, true));
      assertEquals(1, sessionRegistry.getAllSessions(email, true).size());
      MockHttpServletRequest logout = new MockHttpServletRequest("POST", "/logout");
      logout.setServletPath("/logout");
      logout.setSession(session);
      addCsrfToken(logout);
      new FilterChainProxy(chain)
          .doFilter(logout, new MockHttpServletResponse(), mock(FilterChain.class));
      assertTrue(sessionRegistry.getAllSessions(email, true).isEmpty());
      assertEquals(
          roleType.isAdministrative() ? "/admin" : "/home",
          postPassword(email, "OriginalPassword!").response().getRedirectedUrl());
    } finally {
      when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    }
  }

  @Autowired SecurityService actualSecurity;

  @Test
  void rememberMeIsRejectedAtTheSameLimitWithoutDeletingOtherTokenSeries() throws Exception {
    String email = sessionUser(RoleType.ROLE_USER);
    when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    try {
      PasswordResponse first = postPassword(email, "OriginalPassword!");
      String series = "series-" + UUID.randomUUID();
      String otherSeries = "other-" + UUID.randomUUID();
      tokens.createNewToken(
          new PersistentRememberMeToken(email, series, "token-value", new Date()));
      tokens.createNewToken(
          new PersistentRememberMeToken(email, otherSeries, "other-token", new Date()));
      String cookie =
          Base64.getEncoder()
              .encodeToString((series + ":token-value").getBytes(StandardCharsets.UTF_8));
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
      request.setServletPath("/home");
      request.setCookies(new Cookie("remember-me", cookie));
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain servlet = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(request, response, servlet);
      assertEquals("/login?error=sessions", response.getRedirectedUrl());
      verifyNoInteractions(servlet);
      assertTrue(
          request.getSession(false) == null
              || request
                      .getSession(false)
                      .getAttribute(
                          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                  == null);
      assertNotNull(tokens.getTokenForSeries(otherSeries));
      assertNotNull(tokens.getTokenForSeries(series));
      assertEquals(1, sessionRegistry.getAllSessions(email, true).size());
      assertNotNull(
          first
              .request()
              .getSession(false)
              .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
      assertEquals(
          0,
          Arrays.stream(response.getCookies())
              .filter(cookieValue -> cookieValue.getName().equals("remember-me"))
              .toList()
              .getLast()
              .getMaxAge());
      // Actual Remember-me admission is allowed immediately after raising the limit.
      when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(2);
      PersistentRememberMeToken currentToken = tokens.getTokenForSeries(series);
      String updatedCookie =
          Base64.getEncoder()
              .encodeToString(
                  (series
                          + ":"
                          + URLEncoder.encode(currentToken.getTokenValue(), StandardCharsets.UTF_8))
                      .getBytes(StandardCharsets.UTF_8));
      MockHttpServletRequest allowed = new MockHttpServletRequest("GET", "/home");
      allowed.setServletPath("/home");
      allowed.setCookies(new Cookie("remember-me", updatedCookie));
      FilterChain target = mock(FilterChain.class);
      new FilterChainProxy(chain).doFilter(allowed, new MockHttpServletResponse(), target);
      verify(target).doFilter(any(), any());
      assertEquals(2, sessionRegistry.getAllSessions(email, true).size());
      assertNotNull(
          allowed
              .getSession(false)
              .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    } finally {
      when(settings.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    }
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void realPasswordChainChecksMaintenanceAfterPasswordButBeforeAuthenticatedAdmission(RoleType role)
      throws Exception {
    String email = sessionUser(role);
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    PasswordResponse login = postPassword(email, "OriginalPassword!");
    assertEquals(
        role.isAdministrative() ? "/admin" : "/login?error=maintenance",
        login.response().getRedirectedUrl());
    if (!role.isAdministrative()) {
      HttpSession anonymous = login.request().getSession(false);
      assertNotNull(anonymous);
      assertNull(
          anonymous.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
      assertNull(anonymous.getAttribute("authenticatedUser"));
      assertTrue(sessionRegistry.getAllSessions(email, false).isEmpty());
      assertEquals(
          0, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
      assertEquals(
          "/login?error=invalid",
          postPassword(email, "WrongPassword!").response().getRedirectedUrl());
      assertEquals(
          1, attempts.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
    }
  }

  @Test
  void realChainPausesExistingUserWithoutLosingContextAndAllowsLogout() throws Exception {
    String email = sessionUser(RoleType.ROLE_USER);
    PasswordResponse login = postPassword(email, "OriginalPassword!");
    MockHttpSession session = (MockHttpSession) login.request().getSession(false);
    Object context =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
    request.setServletPath("/home");
    request.setSession(session);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain servlet = mock(FilterChain.class);
    new FilterChainProxy(chain).doFilter(request, response, servlet);
    assertEquals(503, response.getStatus());
    assertEquals("/maintenance", response.getForwardedUrl());
    assertFalse(session.isInvalid());
    assertSame(
        context,
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    new FilterChainProxy(chain).doFilter(request, new MockHttpServletResponse(), servlet);
    verify(servlet).doFilter(any(), any());
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest logout = new MockHttpServletRequest("POST", "/logout");
    logout.setServletPath("/logout");
    logout.setSession(session);
    addCsrfToken(logout);
    new FilterChainProxy(chain)
        .doFilter(logout, new MockHttpServletResponse(), mock(FilterChain.class));
    assertTrue(session.isInvalid());
  }

  @Test
  void explicitReloginCannotReuseOldUserSlotToBypassMaintenance() throws Exception {
    String email = sessionUser(RoleType.ROLE_USER);
    PasswordResponse previous = postPassword(email, "OriginalPassword!");
    MockHttpSession session = (MockHttpSession) previous.request().getSession(false);
    when(settings.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
    request.setServletPath("/login");
    request.setSession(session);
    addCsrfToken(request);
    request.setParameter("username", email);
    request.setParameter("password", "OriginalPassword!");
    request.setParameter("remember-me", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(request, response, mock(FilterChain.class));
    assertEquals("/login?error=maintenance", response.getRedirectedUrl());
    assertFalse(session.isInvalid());
    assertNotNull(
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    assertTrue(
        Arrays.stream(response.getCookies())
            .noneMatch(
                cookie -> cookie.getName().equals("remember-me") && cookie.getMaxAge() != 0));
  }

  private String sessionUser(RoleType roleType) {
    String email = "session-" + UUID.randomUUID() + "@example.test";
    transaction.executeWithoutResult(
        tx -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
                  .setParameter("role", roleType)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(roleType).build();
            entityManager.persist(role);
          }
          Long id = create(email, role);
          entityManager.find(UserEntity.class, id).setPassword(encoder.encode("OriginalPassword!"));
        });
    return email;
  }

  private void addCsrfToken(MockHttpServletRequest target) throws Exception {
    MockHttpServletRequest bootstrap = new MockHttpServletRequest("GET", "/login");
    bootstrap.setServletPath("/login");
    bootstrap.setSession(target.getSession(false));
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(bootstrap, response, mock(FilterChain.class));
    target.setSession(bootstrap.getSession(false));
    assertNotNull(response.getHeader("X-CSRF-TOKEN"));
    target.addHeader("X-CSRF-TOKEN", response.getHeader("X-CSRF-TOKEN"));
  }

  private PasswordResponse postPassword(String email, String password) throws Exception {
    SecurityContextHolder.clearContext();
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
    request.setServletPath("/login");
    addCsrfToken(request);
    request.setParameter("username", email);
    request.setParameter("password", password);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(request, response, mock(FilterChain.class));
    assertEquals(302, response.getStatus());
    return new PasswordResponse(request, response);
  }

  private record PasswordResponse(
      MockHttpServletRequest request, MockHttpServletResponse response) {}

  private Long create(String email, RoleEntity role) {
    UserEntity user =
        UserEntity.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("hash")
            .enabled(true)
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>(List.of(role)))
            .build();
    entityManager.persist(user);
    return user.getId();
  }

  @Configuration
  @EnableWebSecurity
  @Import({
    UserServicePersistenceTest.Config.class,
    SecurityConfig.class,
    LoginAttemptService.class,
    SessionManagementService.class
  })
  static class Config {
    @Bean
    CustomUserDetailsService userDetailsService(UserRepository repository) {
      return new CustomUserDetailsService(repository);
    }

    @Bean
    VaadinSecurityFilter vaadinSecurityFilter(ObjectProvider<SecurityService> security) {
      return new VaadinSecurityFilter(security);
    }

    @Bean
    ConfigService configService() {
      ConfigService config = mock(ConfigService.class);
      when(config.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY)).thenReturn("isolated-test-key");
      when(config.getInteger(ConfigEntry.REMEMBER_ME_DURATION)).thenReturn(3600);
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(10);
      when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
      return config;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }
  }
}
