package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.AccountSecurityService.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Eigene H2-Datenbank, echte Transaktionen und Outbox; kein SMTP und keine Bestandsdaten. */
@SpringJUnitConfig(AccountSecurityServicePersistenceTest.Config.class)
class AccountSecurityServicePersistenceTest {
  static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);

  @Configuration
  @Import({
    UserServicePersistenceTest.Config.class,
    AccountSecurityService.class,
    AccountSecurityMail.class,
    CustomUserDetailsService.class,
    LoginAttemptService.class,
    CustomI18NProvider.class
  })
  static class Config {
    @Bean
    ConfigService config() {
      var config = mock(ConfigService.class);
      when(config.getString(any()))
          .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
      when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://account.example.test");
      when(config.getInteger(any()))
          .thenAnswer(
              call ->
                  Integer.parseInt(((ConfigEntry) call.getArgument(0)).getDefaultValueString()));
      return config;
    }

    @Bean
    PasswordEncoder encoder() {
      return new BCryptPasswordEncoder(4);
    }

    @Bean
    SessionRegistryImpl registry() {
      return new SessionRegistryImpl();
    }

    @Bean
    SessionAdmissionService admission(ConfigService config, SessionRegistryImpl registry) {
      return new SessionAdmissionService(config, registry);
    }

    @Bean
    EmailService sender() {
      return mock(EmailService.class);
    }

    @Bean
    EmailQueueService queue(
        ConfigService config,
        EmailQueueRepository queue,
        EmailService sender,
        UserRepository users,
        PlatformTransactionManager manager,
        AdminNotificationAttemptRepository attempts) {
      return spy(new EmailQueueService(config, queue, sender, users, manager, attempts, FIXED));
    }

    @Bean
    PersistentTokenRepository remembered(DataSource dataSource) {
      new JdbcTemplate(dataSource)
          .execute(
              "create table persistent_logins (username varchar(255) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
      var repository = new JdbcTokenRepositoryImpl();
      repository.setDataSource(dataSource);
      return repository;
    }
  }

  @Autowired AccountSecurityService service;
  @Autowired SecurityService security;
  @Autowired UserRepository users;
  @Autowired EmailChangeTokenRepository tokens;
  @Autowired PasswordResetTokenRepository resets;
  @Autowired EmailQueueRepository emails;
  @Autowired LoginAttemptRepository attempts;
  @Autowired PasswordEncoder encoder;
  @Autowired CustomUserDetailsService details;
  @Autowired EmailQueueService queue;
  @Autowired EmailService sender;
  @Autowired ConfigService config;
  @Autowired PersistentTokenRepository remembered;
  @Autowired SessionRegistryImpl registry;
  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;
  UserEntity account;
  MockHttpServletRequest request;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(queue, sender);
    registry
        .getAllPrincipals()
        .forEach(
            principal ->
                registry
                    .getAllSessions(principal, true)
                    .forEach(info -> registry.removeSessionInformation(info.getSessionId())));
    ReflectionTestUtils.setField(service, "clock", FIXED);
    tx.executeWithoutResult(
        status -> {
          em.createQuery("delete from EmailQueueEntity").executeUpdate();
          em.createQuery("delete from EmailChangeTokenEntity").executeUpdate();
          em.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
          users.deleteAll();
          users.flush();
          attempts.deleteAll();
          account = newUser("old@example.test");
          users.saveAndFlush(account);
        });
    account = users.findById(account.getId()).orElseThrow();
    remembered.removeUserTokens(account.getEmail());
    request = new MockHttpServletRequest();
    registry.registerNewSession(request.getSession().getId(), account.getEmail());
    registry.registerNewSession("other-device", account.getEmail());
    remembered.createNewToken(
        new PersistentRememberMeToken(
            account.getEmail(), "remember-series", "remember-token", Date.from(FIXED.instant())));
    authenticate(account);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  UserEntity newUser(String email) {
    return UserEntity.builder()
        .email(email)
        .firstName("Own")
        .lastName("Account")
        .password(encoder.encode("Old-secret!"))
        .enabled(true)
        .preferredLocale(Locale.GERMAN)
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender(Gender.OTHER)
        .roleEntities(new ArrayList<>())
        .build();
  }

  void authenticate(UserEntity user) {
    var principal = details.loadUserByUsername(user.getEmail());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  UserEntity stored() {
    return users.findById(account.getId()).orElseThrow();
  }

  void requestChange(String email) {
    service.requestEmailChange(request, new EmailChange(email, email, "Old-secret!"));
  }

  String linkToken() {
    var html =
        emails.findAll().stream()
            .filter(mail -> mail.getBody().contains("confirm-email-change/"))
            .reduce((first, second) -> second)
            .orElseThrow()
            .getBody();
    var link =
        Jsoup.parse(html).select("a[href]").stream()
            .map(a -> a.attr("href"))
            .filter(href -> href.contains("/confirm-email-change/"))
            .findFirst()
            .orElseThrow();
    return link.substring(link.lastIndexOf('/') + 1);
  }

  @Test
  void passwordChangeIsAtomicQueuesNoticeAndKeepsOnlyCurrentSession() {
    requestChange("new@example.test");
    tx.executeWithoutResult(
        status -> {
          var reset = new PasswordResetTokenEntity();
          reset.setToken("reset-before-change");
          reset.setUserEntity(users.findById(account.getId()).orElseThrow());
          reset.setExpiryDate(LocalDateTime.now(FIXED).plusHours(1));
          resets.save(reset);
        });
    var result =
        service.changePassword(
            request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));
    assertEquals(account.getVersion(), result.previousVersion());
    assertTrue(result.version() > result.previousVersion());
    assertTrue(encoder.matches("New-secret!", stored().getPassword()));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.INACTIVE, tokens.findAll().getFirst().getStatus());
    assertEquals(
        TokenStatus.INACTIVE, resets.findByToken("reset-before-change").orElseThrow().getStatus());
    var notice =
        emails.findAll().stream()
            .filter(mail -> mail.getSubject().contains("Passwort"))
            .findFirst()
            .orElseThrow();
    assertEquals("old@example.test", notice.getRecipient());
    assertFalse(notice.getBody().contains("New-secret!"));
    assertFalse(notice.getBody().contains("Old-secret!"));
    assertTrue(notice.getBody().contains("cid:"));
    assertFalse(registry.getSessionInformation(request.getSession().getId()).isExpired());
    assertTrue(registry.getSessionInformation("other-device").isExpired());
    assertNull(remembered.getTokenForSeries("remember-series"));
  }

  @Test
  void wrongCurrentPasswordPersistsTheAttemptButChangesNothing() {
    assertThrows(
        BadCredentialsException.class,
        () ->
            service.changePassword(
                request, new PasswordChange("Wrong!", "New-secret!", "New-secret!")));
    assertTrue(encoder.matches("Old-secret!", stored().getPassword()));
    assertEquals(1, attempts.findAll().getFirst().getFailures());
    assertEquals(0, emails.count());
    assertFalse(registry.getSessionInformation("other-device").isExpired());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
  }

  @Test
  void invalidPasswordOrConfirmationNeverPersistsOrAuthenticates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.changePassword(request, new PasswordChange("Old-secret!", "x", "x")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.changePassword(
                request, new PasswordChange("Old-secret!", "New-secret!", "Different!")));
    assertEquals(0, attempts.count());
    assertEquals(0, emails.count());
  }

  @Test
  void outboxFailureRollsBackHashTokenRevocationAndRememberedSignins() {
    requestChange("new@example.test");
    doThrow(new IllegalStateException("Own simulated outbox failure"))
        .when(queue)
        .addAddressedNotification(any(), anyString(), anyString(), anyString());
    assertThrows(
        IllegalStateException.class,
        () ->
            service.changePassword(
                request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!")));
    assertTrue(encoder.matches("Old-secret!", stored().getPassword()));
    assertEquals(TokenStatus.ACTIVE, tokens.findAll().getFirst().getStatus());
    assertEquals(1, emails.count());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
    assertFalse(registry.getSessionInformation("other-device").isExpired());
  }

  @Test
  void emailRequestOnlyQueuesToNewAddressAndGetDoesNotConsumeTheHashedToken() {
    requestChange("  New@Example.Test  ");
    assertEquals("old@example.test", stored().getEmail());
    assertEquals("new@example.test", emails.findAll().getFirst().getRecipient());
    var raw = linkToken();
    var token = tokens.findAll().getFirst();
    assertNotEquals(raw, token.getDigest());
    assertEquals(64, token.getDigest().length());
    assertEquals(Confirmation.READY, service.inspect(raw));
    assertEquals(Confirmation.READY, service.inspect(raw));
    assertEquals(TokenStatus.ACTIVE, tokens.findAll().getFirst().getStatus());
    assertFalse(registry.getSessionInformation("other-device").isExpired());
  }

  @Test
  void confirmationChangesIdentityOnceNotifiesBothAddressesAndRevokesEveryOldSession() {
    requestChange("new@example.test");
    var raw = linkToken();
    assertEquals(Confirmation.SUCCESS, service.confirm(raw));
    assertEquals("new@example.test", stored().getEmail());
    assertEquals(TokenStatus.USED, tokens.findAll().getFirst().getStatus());
    assertEquals(3, emails.count());
    assertEquals(
        1,
        emails.findAll().stream().filter(m -> m.getRecipient().equals("old@example.test")).count());
    assertEquals(
        2,
        emails.findAll().stream().filter(m -> m.getRecipient().equals("new@example.test")).count());
    assertTrue(registry.getSessionInformation(request.getSession().getId()).isExpired());
    assertTrue(registry.getSessionInformation("other-device").isExpired());
    assertNull(remembered.getTokenForSeries("remember-series"));
    assertEquals(Confirmation.INVALID, service.confirm(raw));
    assertEquals(3, emails.count());
  }

  @Test
  void expiredTokenAtExactBoundaryCannotChangeTheAddress() {
    requestChange("new@example.test");
    var raw = linkToken();
    tx.executeWithoutResult(
        status -> {
          var t = tokens.findAll().getFirst();
          t.setExpiryDate(LocalDateTime.now(FIXED));
          tokens.save(t);
        });
    assertEquals(Confirmation.EXPIRED, service.inspect(raw));
    assertEquals(Confirmation.EXPIRED, service.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.EXPIRED, tokens.findAll().getFirst().getStatus());
  }

  @Test
  void addressClaimedAfterRequestIsRejectedWithoutChangingEitherAccount() {
    requestChange("new@example.test");
    var raw = linkToken();
    users.saveAndFlush(newUser("new@example.test"));
    assertEquals(Confirmation.CONFLICT, service.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(1, emails.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"password", "email", "disabled"})
  void staleOrDisabledAccountCannotUseTheOldLink(String mutation) {
    requestChange("new@example.test");
    var raw = linkToken();
    tx.executeWithoutResult(
        status -> {
          var user = users.findById(account.getId()).orElseThrow();
          if (mutation.equals("password")) user.setPassword(encoder.encode("Another-secret!"));
          if (mutation.equals("email")) user.setEmail("admin-changed@example.test");
          if (mutation.equals("disabled")) user.setEnabled(false);
          users.saveAndFlush(user);
        });
    assertEquals(Confirmation.INVALID, service.confirm(raw));
    assertEquals(1, emails.count());
  }

  @Test
  void duplicateRequestsAreRateLimitedAndNewerRequestInvalidatesPreviousLink() {
    requestChange("first@example.test");
    var first = linkToken();
    assertEquals(
        Problem.REQUEST_PENDING,
        assertThrows(Rejected.class, () -> requestChange("second@example.test")).problem());
    assertEquals(1, emails.count());
    ReflectionTestUtils.setField(service, "clock", Clock.offset(FIXED, Duration.ofSeconds(61)));
    requestChange("second@example.test");
    var second = linkToken();
    assertNotEquals(first, second);
    assertEquals(Confirmation.INVALID, service.inspect(first));
    assertEquals(Confirmation.READY, service.inspect(second));
  }

  @Test
  void sameOrTakenAddressAndMismatchNeverCreateAnEmailChange() {
    assertEquals(
        Problem.SAME_EMAIL,
        assertThrows(Rejected.class, () -> requestChange("OLD@example.test")).problem());
    users.saveAndFlush(newUser("taken@example.test"));
    assertEquals(
        Problem.EMAIL_UNAVAILABLE,
        assertThrows(Rejected.class, () -> requestChange("TAKEN@example.test")).problem());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.requestEmailChange(
                request, new EmailChange("new@example.test", "other@example.test", "Old-secret!")));
    assertEquals(0, tokens.count());
    assertEquals(0, emails.count());
  }

  @Test
  void confirmationOutboxFailureRollsBackAddressTokenAndSessionRevocation() {
    requestChange("new@example.test");
    var raw = linkToken();
    doThrow(new IllegalStateException("Own simulated outbox failure"))
        .when(queue)
        .addAddressedNotification(any(), anyString(), anyString(), anyString());
    assertThrows(IllegalStateException.class, () -> service.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.ACTIVE, tokens.findAll().getFirst().getStatus());
    assertFalse(registry.getSessionInformation("other-device").isExpired());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
    assertEquals(1, emails.count());
  }

  @Test
  void concurrentConfirmationHasOneWinnerAndOneConsumedLink() throws Exception {
    requestChange("new@example.test");
    var raw = linkToken();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var start = new CountDownLatch(1);
      Callable<Confirmation> confirm =
          () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return service.confirm(raw);
          };
      var a = executor.submit(confirm);
      var b = executor.submit(confirm);
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      var results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter(r -> r == Confirmation.SUCCESS).count());
      assertEquals(1, results.stream().filter(r -> r == Confirmation.INVALID).count());
    }
    assertEquals(3, emails.count());
  }

  @Test
  void queuedRecipientNeverFollowsALaterUnrelatedAddressChange() throws Exception {
    requestChange("new@example.test");
    tx.executeWithoutResult(
        status -> {
          var user = users.findById(account.getId()).orElseThrow();
          user.setEmail("later@example.test");
          users.saveAndFlush(user);
        });
    queue.processQueue();
    verify(sender).sendEmailTo(eq("new@example.test"), anyString(), anyString());
    verify(sender, never()).sendEmail(any(), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newMailsUseFixedAccountLanguageAndEscapeConfiguredHtml(String language) {
    tx.executeWithoutResult(
        status -> {
          var user = users.findById(account.getId()).orElseThrow();
          user.setPreferredLocale(Locale.forLanguageTag(language));
          users.saveAndFlush(user);
        });
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Own <script>text</script>");
    try {
      service.changePassword(
          request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));
      var mail = emails.findAll().getFirst();
      assertTrue(mail.getBody().contains("&lt;script&gt;text&lt;/script&gt;"));
      assertFalse(mail.getBody().contains("<script>text</script>"));
      assertTrue(
          mail.getBody()
              .contains(
                  language.equals("en")
                      ? "Your password has been changed"
                      : "Ihr Passwort wurde geändert"));
      assertFalse(mail.getBody().contains("{{"));
    } finally {
      when(config.getString(ConfigEntry.SERVICE_NAME))
          .thenReturn(ConfigEntry.SERVICE_NAME.getDefaultValueString());
    }
  }

  @Test
  void currentPasswordChecksUseTheConfiguredPersistentAttemptLimit() {
    when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(1);
    try {
      assertThrows(
          BadCredentialsException.class,
          () ->
              service.changePassword(
                  request, new PasswordChange("Wrong!", "New-secret!", "New-secret!")));
      assertThrows(
          LoginThrottledException.class,
          () ->
              service.changePassword(
                  request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!")));
      assertTrue(encoder.matches("Old-secret!", stored().getPassword()));
      assertEquals(0, emails.count());
    } finally {
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT))
          .thenReturn(Integer.parseInt(ConfigEntry.LOGIN_ATTEMPTS_LIMIT.getDefaultValueString()));
    }
  }

  @Test
  void anonymousAndMaintenanceRequestsDoNotChangeCredentialsOrConsumeLinks() {
    requestChange("new@example.test");
    var raw = linkToken();
    SecurityContextHolder.clearContext();
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.changePassword(
                request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!")));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    try {
      assertThrows(MaintenanceAccessException.class, () -> service.confirm(raw));
      assertEquals(TokenStatus.ACTIVE, tokens.findAll().getFirst().getStatus());
      assertEquals("old@example.test", stored().getEmail());
    } finally {
      when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    }
  }

  @Test
  void twoAccountsCannotConfirmTheSameTargetAddressConcurrently() throws Exception {
    requestChange("target@example.test");
    var first = linkToken();
    var secondAccount = users.saveAndFlush(newUser("second@example.test"));
    authenticate(secondAccount);
    requestChange("target@example.test");
    var second = linkToken();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var start = new CountDownLatch(1);
      var a =
          executor.submit(
              () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return service.confirm(first);
              });
      var b =
          executor.submit(
              () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return service.confirm(second);
              });
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      var results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter(r -> r == Confirmation.SUCCESS).count());
      assertEquals(1, results.stream().filter(r -> r == Confirmation.CONFLICT).count());
    }
    assertEquals(
        1,
        users.findAll().stream().filter(u -> u.getEmail().equals("target@example.test")).count());
    assertEquals(4, emails.count());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void revokedSessionsFailHttpAndPushGuardsWhileTheCurrentSessionStaysValid(boolean push)
      throws Exception {
    var context = SecurityContextHolder.getContext();
    request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
    request
        .getSession()
        .setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    var otherSession = new MockHttpSession(null, "other-device");
    otherSession.setAttribute("SPRING_SECURITY_CONTEXT", context);
    otherSession.setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    var other = new MockHttpServletRequest();
    other.setSession(otherSession);
    service.changePassword(
        request, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));
    assertTrue(security.hasCurrentSessionPrivileges(request));
    assertTrue(security.getAuthenticatedUser(request).isPresent());
    if (push) {
      var interceptor = new PushSecurityInterceptor(security, new MaintenanceService(config));
      var servletRequest = new VaadinServletRequest(other, mock(VaadinServletService.class));
      assertThrows(
          AccessDeniedException.class, () -> interceptor.requestStart(servletRequest, null));
    } else {
      ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
      when(provider.getObject()).thenReturn(security);
      var response = new MockHttpServletResponse();
      var chain = mock(FilterChain.class);
      new VaadinSecurityFilter(provider).doFilter(other, response, chain);
      assertEquals(401, response.getStatus());
      verifyNoInteractions(chain);
    }
    assertTrue(otherSession.isInvalid());
  }

  @Test
  void revokedIdentityCanLoadLoginDocumentAfterTheOldSessionHasBeenCleared() throws Exception {
    var context = SecurityContextHolder.getContext();
    request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
    request
        .getSession()
        .setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    requestChange("new@example.test");
    assertEquals(Confirmation.SUCCESS, service.confirm(linkToken()));
    var session = (MockHttpSession) request.getSession();
    request.setMethod("GET");
    request.setServletPath(AppRoute.LOGIN.getSecurityRoute());
    ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(security);
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    new VaadinSecurityFilter(provider).doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
    assertEquals(200, response.getStatus());
    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
