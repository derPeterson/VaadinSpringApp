package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.session.DurableSessionRegistry;
import de.derpeterson.app.security.session.SessionPersistenceConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/** Zwei echte Spring-Kontexte über derselben isolierten H2, echte HTTP-Session-/Securityfilter. */
class DurableSessionSecurityChainTest {
  private static final String EMAIL = "restart-fixture@example.test";
  private static final String PASSWORD = "RestartFixturePassword!";
  private AnnotationConfigWebApplicationContext context;
  private DataSource database;

  @BeforeEach
  void setup() {
    database =
        new DriverManagerDataSource(
            "jdbc:h2:mem:restart-chain-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    start();
    createUser();
  }

  @AfterEach
  void cleanup() {
    if (context != null) {
      context.close();
    }
    SecurityContextHolder.clearContext();
  }

  private void start() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(
        SecurityChainRegressionTest.Config.class, SessionPersistenceConfiguration.class);
    context.addBeanFactoryPostProcessor(
        factory -> {
          // Nur diese neue Testdatenbank; create-drop des gemeinsamen Fixtures hier durch update
          // ersetzen.
          factory.registerSingleton("dataSource", database);
          factory
              .getBeanDefinition("entityManagerFactory")
              .getPropertyValues()
              .add("jpaPropertyMap", Map.of("hibernate.hbm2ddl.auto", "update"));
        });
    context.refresh();
    when(context.getBean(ConfigService.class).getInteger(ConfigEntry.MAX_SESSIONS_PER_USER))
        .thenReturn(1);
    assertTrue(context.getBean(SessionRegistry.class) instanceof DurableSessionRegistry<?>);
  }

  private void restart() {
    context.close();
    SecurityContextHolder.clearContext();
    start();
  }

  private void createUser() {
    context
        .getBean(TransactionTemplate.class)
        .executeWithoutResult(
            status -> {
              EntityManager manager =
                  EntityManagerFactoryUtils.getTransactionalEntityManager(
                      context.getBean(EntityManagerFactory.class));
              assertNotNull(manager);
              {
                RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
                manager.persist(role);
                UserEntity user =
                    UserEntity.builder()
                        .firstName("Restart")
                        .lastName("Fixture")
                        .email(EMAIL)
                        .password(context.getBean(PasswordEncoder.class).encode(PASSWORD))
                        .gender(Gender.OTHER)
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .enabled(true)
                        .preferredLocale(Locale.ENGLISH)
                        .roleEntities(new ArrayList<>(List.of(role)))
                        .build();
                manager.persist(user);
                manager.flush();
              }
            });
  }

  private record Result(MockHttpServletResponse response, Cookie cookie, String id, int reached) {}

  private Result request(
      String method,
      String path,
      Cookie cookie,
      Map<String, String> parameters,
      String csrf,
      FilterChain action)
      throws Exception {
    SecurityContextHolder.clearContext();
    MockHttpServletRequest request =
        new MockHttpServletRequest(context.getServletContext(), method, path);
    request.setServletPath(path);
    if (cookie != null) {
      request.setCookies(cookie);
    }
    parameters.forEach(request::setParameter);
    if (csrf != null) {
      request.addHeader("X-CSRF-TOKEN", csrf);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> id = new AtomicReference<>();
    AtomicInteger reached = new AtomicInteger();
    SessionRepositoryFilter<?> sessions = context.getBean(SessionRepositoryFilter.class);
    FilterChainProxy security = new FilterChainProxy(context.getBean(SecurityFilterChain.class));
    sessions.doFilter(
        request,
        response,
        (wrapped, output) -> {
          security.doFilter(
              wrapped,
              output,
              (accepted, target) -> {
                reached.incrementAndGet();
                if (action != null) {
                  action.doFilter(accepted, target);
                }
              });
          HttpSession current = ((HttpServletRequest) wrapped).getSession(false);
          if (current != null) {
            id.set(current.getId());
          }
        });
    Cookie latest =
        Arrays.stream(response.getCookies())
            .filter(candidate -> "JSESSIONID".equals(candidate.getName()))
            .reduce((first, second) -> second)
            .orElse(cookie);
    return new Result(response, latest, id.get(), reached.get());
  }

  private Result login(boolean remembered) throws Exception {
    Result bootstrap = request("GET", "/login", null, Map.of(), null, null);
    assertNotNull(bootstrap.cookie());
    assertTrue(bootstrap.cookie().isHttpOnly());
    Result logged =
        request(
            "POST",
            "/login",
            bootstrap.cookie(),
            Map.of(
                "username",
                EMAIL,
                "password",
                PASSWORD,
                "remember-me",
                Boolean.toString(remembered)),
            bootstrap.response().getHeader("X-CSRF-TOKEN"),
            null);
    assertEquals("/home", logged.response().getRedirectedUrl());
    assertEquals(0, logged.reached());
    assertNotNull(logged.id());
    return logged;
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void completeContextRestartKeepsAuthenticationSlotAndCurrentDevice(boolean remembered)
      throws Exception {
    Result before = login(remembered);
    String handle =
        context.getBean(SessionAdmissionService.class).active(EMAIL).getFirst().handle();
    request(
        "GET",
        "/account",
        before.cookie(),
        Map.of(),
        null,
        (req, res) ->
            ((HttpServletRequest) req).getSession().setAttribute("unsaved-ui", new Object()));
    restart();
    Result restored =
        request(
            "GET",
            "/account",
            before.cookie(),
            Map.of(),
            null,
            (req, res) -> {
              assertEquals(EMAIL, SecurityContextHolder.getContext().getAuthentication().getName());
              assertNull(((HttpServletRequest) req).getSession().getAttribute("unsaved-ui"));
            });
    assertEquals(200, restored.response().getStatus());
    assertEquals(1, restored.reached());
    assertEquals(before.id(), restored.id());
    assertEquals(
        handle, context.getBean(SessionAdmissionService.class).active(EMAIL).getFirst().handle());
    Result denied = loginWithoutSuccessAssertion();
    assertEquals("/login?error=sessions", denied.response().getRedirectedUrl());
    assertEquals(1, context.getBean(SessionAdmissionService.class).active(EMAIL).size());
  }

  private Result loginWithoutSuccessAssertion() throws Exception {
    Result bootstrap = request("GET", "/login", null, Map.of(), null, null);
    return request(
        "POST",
        "/login",
        bootstrap.cookie(),
        Map.of("username", EMAIL, "password", PASSWORD),
        bootstrap.response().getHeader("X-CSRF-TOKEN"),
        null);
  }

  @Test
  void disabledDurableAuthenticationRejectsAnOldRpcBeforeApplicationDispatch() throws Exception {
    Result login = login(false);
    new TransactionTemplate(
            context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
        .executeWithoutResult(
            status -> {
              EntityManager em =
                  EntityManagerFactoryUtils.getTransactionalEntityManager(
                      context.getBean(EntityManagerFactory.class));
              UserEntity user =
                  em.createQuery(
                          "select u from UserEntity u where u.email = :email", UserEntity.class)
                      .setParameter("email", EMAIL)
                      .getSingleResult();
              user.setEnabled(false);
              em.flush();
            });
    Result denied = request("POST", "/", login.cookie(), Map.of("v-r", "uidl"), null, null);
    assertEquals(401, denied.response().getStatus());
    assertEquals(0, denied.reached());
  }

  @Test
  void oldUidlLoadsAFreshDocumentWithoutDispatchingItsOldCallback() throws Exception {
    Result before = login(false);
    restart();
    Result uidl =
        request("POST", "/", before.cookie(), Map.of("v-r", "uidl", "v-uiId", "0"), null, null);
    assertEquals(200, uidl.response().getStatus());
    assertEquals(0, uidl.reached());
    assertTrue(uidl.response().getContentAsString().contains("/home"));
    assertEquals("no-store", uidl.response().getHeader("Cache-Control"));
    Result forged = request("POST", "/admin", before.cookie(), Map.of("v-r", "uidl"), null, null);
    assertEquals(403, forged.response().getStatus());
    assertEquals(0, forged.reached());
  }

  @Test
  void restoredLoginStillRespectsMaintenanceAndCanLogoutWithCsrf() throws Exception {
    Result before = login(false);
    restart();
    when(context.getBean(ConfigService.class).getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenReturn(true);
    Result blocked = request("GET", "/account", before.cookie(), Map.of(), null, null);
    assertEquals(503, blocked.response().getStatus());
    assertEquals("/maintenance", blocked.response().getForwardedUrl());
    assertEquals(0, blocked.reached());
    Result bootstrap = request("GET", "/login", before.cookie(), Map.of(), null, null);
    Result logout =
        request(
            "POST",
            "/logout",
            before.cookie(),
            Map.of(),
            bootstrap.response().getHeader("X-CSRF-TOKEN"),
            null);
    assertEquals("/login", logout.response().getRedirectedUrl());
    assertTrue(context.getBean(SessionAdmissionService.class).active(EMAIL).isEmpty());
    restart();
    Result missing = request("GET", "/account", before.cookie(), Map.of(), null, null);
    assertEquals("/login", missing.response().getRedirectedUrl());
    assertEquals(0, missing.reached());
  }

  @Test
  void revokedSessionCannotReturnAfterRestart() throws Exception {
    Result before = login(false);
    context.getBean(SessionAdmissionService.class).expire(EMAIL, before.id());
    restart();
    Result refused = request("GET", "/account", before.cookie(), Map.of(), null, null);
    assertEquals(401, refused.response().getStatus());
    assertEquals(0, refused.reached());
    assertTrue(context.getBean(SessionAdmissionService.class).active(EMAIL).isEmpty());
  }

  @Test
  void flowLoginHelperPublishesADurableSessionWithoutAFormPost() throws Exception {
    Result bootstrap = request("GET", "/login", null, Map.of(), null, null);
    Result logged =
        request(
            "GET",
            "/login",
            bootstrap.cookie(),
            Map.of(),
            null,
            (req, res) -> {
              Authentication authenticated =
                  context
                      .getBean(AuthenticationManager.class)
                      .authenticate(new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD));
              context
                  .getBean(SecurityService.class)
                  .storeAuthenticatedUser(
                      (HttpServletRequest) req,
                      (HttpServletResponse) res,
                      (UserDetails) authenticated.getPrincipal(),
                      false);
            });
    assertEquals(1, logged.reached());
    restart();
    Result restored = request("GET", "/account", logged.cookie(), Map.of(), null, null);
    assertEquals(200, restored.response().getStatus());
    assertEquals(1, restored.reached());
  }
}
