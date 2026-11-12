package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.EmailChangeTokenEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailChangeTokenRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.PushSecurityInterceptor;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.VaadinSecurityFilter;
import de.derpeterson.app.service.AccountSecurityService.Confirmation;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.service.AccountSecurityService.Problem;
import de.derpeterson.app.service.AccountSecurityService.Rejected;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
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
    UserNotificationService.class,
    UserNotificationPolicy.class,
    de.derpeterson.app.websocket.UserNotificationBroadcaster.class,
    AccountSecurityMail.class,
    CustomUserDetailsService.class,
    LoginAttemptService.class,
    CustomI18NProvider.class
  })
  static class Config {
    @Bean
    de.derpeterson.app.security.mfa.TwoFactorAuthorization twoFactorAuthorization() {
      // Diese Fälle prüfen die vorhandenen Konto-/Mailtransaktionen. Die echten MFA-Grenzen
      // werden mit Datenbank und aktueller Regelmatrix separat geprüft.
      return mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class);
    }

    @Bean
    ConfigService config() {
      ConfigService configService = mock(ConfigService.class);
      when(configService.getString(any()))
          .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
      when(configService.getString(ConfigEntry.BASE_URL))
          .thenReturn("https://account.example.test");
      when(configService.getInteger(any()))
          .thenAnswer(call ->
              Integer.parseInt(((ConfigEntry) call.getArgument(0)).getDefaultValueString()));
      return configService;
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
    SessionAdmissionService admission(
        ConfigService configService, SessionRegistryImpl sessionRegistry) {
      return new SessionAdmissionService(configService, sessionRegistry);
    }

    @Bean
    EmailService sender() {
      return mock(EmailService.class);
    }

    @Bean
    EmailQueueService queue(
        ConfigService configService,
        EmailQueueRepository queue,
        EmailService emailService,
        UserRepository userRepository,
        PlatformTransactionManager manager,
        AdminNotificationAttemptRepository attempts) {
      return spy(
          new EmailQueueService(
              configService,
              queue,
              emailService,
              userRepository,
              manager,
              attempts,
              FIXED,
              QueueTestWorkers.inline()));
    }

    @Bean
    PersistentTokenRepository remembered(DataSource dataSource) {
      new JdbcTemplate(dataSource)
          .execute(
              "create table if not exists persistent_logins (username varchar(255) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
      JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
      repository.setDataSource(dataSource);
      return repository;
    }
  }

  @Autowired AccountSecurityService accountSecurityService;
  @Autowired UserNotificationService userNotifications;
  @Autowired UserNotificationRepository userNotificationRepository;

  @Autowired SecurityService securityService;
  @Autowired UserRepository userRepository;
  @Autowired EmailChangeTokenRepository emailChangeTokenRepository;
  @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired EmailQueueRepository emailQueueRepository;
  @Autowired LoginAttemptRepository attempts;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired CustomUserDetailsService details;
  @Autowired EmailQueueService queue;
  @Autowired EmailService emailService;
  @Autowired ConfigService configService;
  @Autowired PersistentTokenRepository remembered;
  @Autowired SessionRegistryImpl sessionRegistry;
  @Autowired TransactionTemplate transactionTemplate;
  @PersistenceContext EntityManager entityManager;
  UserEntity testUserEntity;
  MockHttpServletRequest mockHttpRequest;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(queue, emailService);
    sessionRegistry
        .getAllPrincipals()
        .forEach(principal ->
            sessionRegistry
                .getAllSessions(principal, true)
                .forEach(info -> sessionRegistry.removeSessionInformation(info.getSessionId())));
    ReflectionTestUtils.setField(accountSecurityService, "accountSecurityClock", FIXED);
    transactionTemplate.executeWithoutResult(status -> {
      entityManager.createQuery("delete from UserNotificationRecipientEntity").executeUpdate();
      entityManager.createQuery("delete from UserNotificationEntity").executeUpdate();
      entityManager.createQuery("delete from EmailQueueEntity").executeUpdate();
      entityManager.createQuery("delete from EmailChangeTokenEntity").executeUpdate();
      entityManager.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
      userRepository.deleteAll();
      userRepository.flush();
      attempts.deleteAll();
      testUserEntity = newUser("old@example.test");
      userRepository.saveAndFlush(testUserEntity);
    });
    testUserEntity = userRepository.findById(testUserEntity.getId()).orElseThrow();
    remembered.removeUserTokens(testUserEntity.getEmail());
    mockHttpRequest = new MockHttpServletRequest();
    sessionRegistry.registerNewSession(
        mockHttpRequest.getSession().getId(), testUserEntity.getEmail());
    sessionRegistry.registerNewSession("other-device", testUserEntity.getEmail());
    remembered.createNewToken(
        new PersistentRememberMeToken(
            testUserEntity.getEmail(),
            "remember-series",
            "remember-token",
            Date.from(FIXED.instant())));
    authenticate(testUserEntity);
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
        .password(passwordEncoder.encode("Old-secret!"))
        .enabled(true)
        .preferredLocale(Locale.GERMAN)
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender(Gender.OTHER)
        .roleEntities(new ArrayList<>())
        .build();
  }

  void authenticate(UserEntity user) {
    UserDetails principal = details.loadUserByUsername(user.getEmail());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  UserEntity stored() {
    return userRepository.findById(testUserEntity.getId()).orElseThrow();
  }

  void requestChange(String email) {
    accountSecurityService.requestEmailChange(
        mockHttpRequest, new EmailChange(email, email, "Old-secret!"));
  }

  String linkToken() {
    String html =
        emailQueueRepository.findAll().stream()
            .filter(mail -> mail.getBody().contains("confirm-email-change/"))
            .reduce((first, second) -> second)
            .orElseThrow()
            .getBody();
    String link =
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
    transactionTemplate.executeWithoutResult(status -> {
      PasswordResetTokenEntity reset = new PasswordResetTokenEntity();
      reset.setToken("reset-before-change");
      reset.setUserEntity(userRepository.findById(testUserEntity.getId()).orElseThrow());
      reset.setExpiryDate(LocalDateTime.now(FIXED).plusHours(1));
      passwordResetTokenRepository.save(reset);
    });
    PasswordChanged result =
        accountSecurityService.changePassword(
            mockHttpRequest, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));

    assertEquals(1, userNotificationRepository.count());
    assertEquals(
        UserNotificationType.PASSWORD_CHANGED,
        userNotificationRepository.findAll().getFirst().getType());
    assertEquals(testUserEntity.getVersion(), result.previousVersion());
    assertTrue(result.version() > result.previousVersion());
    assertTrue(passwordEncoder.matches("New-secret!", stored().getPassword()));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.INACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
    assertEquals(
        TokenStatus.INACTIVE,
        passwordResetTokenRepository.findByToken("reset-before-change").orElseThrow().getStatus());
    EmailQueueEntity notice =
        emailQueueRepository.findAll().stream()
            .filter(mail -> mail.getSubject().contains("Passwort"))
            .findFirst()
            .orElseThrow();

    assertEquals("old@example.test", notice.getRecipient());
    assertFalse(notice.getBody().contains("New-secret!"));
    assertFalse(notice.getBody().contains("Old-secret!"));
    assertTrue(notice.getBody().contains("cid:"));
    assertFalse(
        sessionRegistry.getSessionInformation(mockHttpRequest.getSession().getId()).isExpired());
    assertTrue(sessionRegistry.getSessionInformation("other-device").isExpired());
    assertNull(remembered.getTokenForSeries("remember-series"));
  }

  @Test
  void failedRequiredMessageRollsBackPasswordAndOutboxWithoutRevokingSessions() {
    UserNotificationService failing = mock(UserNotificationService.class);
    doThrow(new IllegalStateException("message storage unavailable"))
        .when(failing)
        .recordSecurityEvent(any(), any());
    ReflectionTestUtils.setField(accountSecurityService, "userNotificationService", failing);
    try {
      PasswordChange passwordChange =
          new PasswordChange("Old-secret!", "New-secret!", "New-secret!");

      assertThrows(
          IllegalStateException.class,
          () -> accountSecurityService.changePassword(mockHttpRequest, passwordChange));
      assertTrue(passwordEncoder.matches("Old-secret!", stored().getPassword()));
      assertEquals(0, emailQueueRepository.count());
      assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
      assertNotNull(remembered.getTokenForSeries("remember-series"));
    } finally {
      ReflectionTestUtils.setField(
          accountSecurityService, "userNotificationService", userNotifications);
    }
  }

  @Test
  void failedRequiredMessageRollsBackConfirmedEmailAndBothNotices() {
    requestChange("new@example.test");
    String raw = linkToken();
    UserNotificationService failing = mock(UserNotificationService.class);
    doThrow(new IllegalStateException("message storage unavailable"))
        .when(failing)
        .recordSecurityEvent(any(), any());
    ReflectionTestUtils.setField(accountSecurityService, "userNotificationService", failing);
    try {
      assertThrows(IllegalStateException.class, () -> accountSecurityService.confirm(raw));
      assertEquals("old@example.test", stored().getEmail());
      assertEquals(TokenStatus.ACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
      assertEquals(1, emailQueueRepository.count());
      assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
    } finally {
      ReflectionTestUtils.setField(
          accountSecurityService, "userNotificationService", userNotifications);
    }
  }

  @Test
  void wrongCurrentPasswordPersistsTheAttemptButChangesNothing() {
    PasswordChange passwordChange = new PasswordChange("Wrong!", "New-secret!", "New-secret!");

    assertThrows(
        BadCredentialsException.class,
        () -> accountSecurityService.changePassword(mockHttpRequest, passwordChange));
    assertTrue(passwordEncoder.matches("Old-secret!", stored().getPassword()));
    assertEquals(1, attempts.findAll().getFirst().getFailures());
    assertEquals(0, emailQueueRepository.count());
    assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
  }

  @Test
  void invalidPasswordOrConfirmationNeverPersistsOrAuthenticates() {
    PasswordChange shortPasswordChange = new PasswordChange("Old-secret!", "x", "x");

    assertThrows(
        IllegalArgumentException.class,
        () -> accountSecurityService.changePassword(mockHttpRequest, shortPasswordChange));
    PasswordChange mismatchedConfirmation =
        new PasswordChange("Old-secret!", "New-secret!", "Different!");

    assertThrows(
        IllegalArgumentException.class,
        () -> accountSecurityService.changePassword(mockHttpRequest, mismatchedConfirmation));
    assertEquals(0, attempts.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void outboxFailureRollsBackHashTokenRevocationAndRememberedSignins() {
    requestChange("new@example.test");
    doThrow(new IllegalStateException("Own simulated outbox failure"))
        .when(queue)
        .addAddressedNotification(any(), anyString(), anyString(), anyString());
    PasswordChange passwordChange = new PasswordChange("Old-secret!", "New-secret!", "New-secret!");

    assertThrows(
        IllegalStateException.class,
        () -> accountSecurityService.changePassword(mockHttpRequest, passwordChange));
    assertTrue(passwordEncoder.matches("Old-secret!", stored().getPassword()));
    assertEquals(TokenStatus.ACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
    assertEquals(1, emailQueueRepository.count());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
    assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
  }

  @Test
  void emailRequestOnlyQueuesToNewAddressAndGetDoesNotConsumeTheHashedToken() {
    requestChange("  New@Example.Test  ");

    assertEquals("old@example.test", stored().getEmail());
    assertEquals("new@example.test", emailQueueRepository.findAll().getFirst().getRecipient());
    String raw = linkToken();
    EmailChangeTokenEntity token = emailChangeTokenRepository.findAll().getFirst();

    assertNotEquals(raw, token.getDigest());
    assertEquals(64, token.getDigest().length());
    assertEquals(Confirmation.READY, accountSecurityService.inspect(raw));
    assertEquals(Confirmation.READY, accountSecurityService.inspect(raw));
    assertEquals(TokenStatus.ACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
    assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
  }

  @Test
  void confirmationChangesIdentityOnceNotifiesBothAddressesAndRevokesEveryOldSession() {
    requestChange("new@example.test");
    String raw = linkToken();

    assertEquals(0, userNotificationRepository.count());
    assertEquals(Confirmation.SUCCESS, accountSecurityService.confirm(raw));
    assertEquals(1, userNotificationRepository.count());
    assertEquals(
        UserNotificationType.EMAIL_CHANGED,
        userNotificationRepository.findAll().getFirst().getType());
    assertEquals("new@example.test", stored().getEmail());
    assertEquals(TokenStatus.USED, emailChangeTokenRepository.findAll().getFirst().getStatus());
    assertEquals(3, emailQueueRepository.count());
    assertEquals(
        1,
        emailQueueRepository.findAll().stream()
            .filter(m -> m.getRecipient().equals("old@example.test"))
            .count());
    assertEquals(
        2,
        emailQueueRepository.findAll().stream()
            .filter(m -> m.getRecipient().equals("new@example.test"))
            .count());
    assertTrue(
        sessionRegistry.getSessionInformation(mockHttpRequest.getSession().getId()).isExpired());
    assertTrue(sessionRegistry.getSessionInformation("other-device").isExpired());
    assertNull(remembered.getTokenForSeries("remember-series"));
    assertEquals(Confirmation.INVALID, accountSecurityService.confirm(raw));
    assertEquals(3, emailQueueRepository.count());
  }

  @Test
  void expiredTokenAtExactBoundaryCannotChangeTheAddress() {
    requestChange("new@example.test");
    String raw = linkToken();
    transactionTemplate.executeWithoutResult(status -> {
      EmailChangeTokenEntity t = emailChangeTokenRepository.findAll().getFirst();
      t.setExpiryDate(LocalDateTime.now(FIXED));
      emailChangeTokenRepository.save(t);
    });

    assertEquals(Confirmation.EXPIRED, accountSecurityService.inspect(raw));
    assertEquals(Confirmation.EXPIRED, accountSecurityService.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.EXPIRED, emailChangeTokenRepository.findAll().getFirst().getStatus());
  }

  @Test
  void addressClaimedAfterRequestIsRejectedWithoutChangingEitherAccount() {
    requestChange("new@example.test");
    String raw = linkToken();
    userRepository.saveAndFlush(newUser("new@example.test"));

    assertEquals(Confirmation.CONFLICT, accountSecurityService.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(1, emailQueueRepository.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"password", "email", "disabled"})
  void staleOrDisabledAccountCannotUseTheOldLink(String mutation) {
    requestChange("new@example.test");
    String raw = linkToken();
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity user = userRepository.findById(testUserEntity.getId()).orElseThrow();
      if (mutation.equals("password")) {
        user.setPassword(passwordEncoder.encode("Another-secret!"));
      }
      if (mutation.equals("email")) {
        user.setEmail("admin-changed@example.test");
      }
      if (mutation.equals("disabled")) {
        user.setEnabled(false);
      }
      userRepository.saveAndFlush(user);
    });

    assertEquals(Confirmation.INVALID, accountSecurityService.confirm(raw));
    assertEquals(1, emailQueueRepository.count());
  }

  @Test
  void duplicateRequestsAreRateLimitedAndNewerRequestInvalidatesPreviousLink() {
    requestChange("first@example.test");
    String first = linkToken();

    assertEquals(
        Problem.REQUEST_PENDING,
        assertThrows(Rejected.class, () -> requestChange("second@example.test")).problem());
    assertEquals(1, emailQueueRepository.count());
    ReflectionTestUtils.setField(
        accountSecurityService,
        "accountSecurityClock",
        Clock.offset(FIXED, Duration.ofSeconds(61)));
    requestChange("second@example.test");
    String second = linkToken();

    assertNotEquals(first, second);
    assertEquals(Confirmation.INVALID, accountSecurityService.inspect(first));
    assertEquals(Confirmation.READY, accountSecurityService.inspect(second));
  }

  @Test
  void sameOrTakenAddressAndMismatchNeverCreateAnEmailChange() {
    assertEquals(
        Problem.SAME_EMAIL,
        assertThrows(Rejected.class, () -> requestChange("OLD@example.test")).problem());
    userRepository.saveAndFlush(newUser("taken@example.test"));

    assertEquals(
        Problem.EMAIL_UNAVAILABLE,
        assertThrows(Rejected.class, () -> requestChange("TAKEN@example.test")).problem());
    EmailChange emailChange =
        new EmailChange("new@example.test", "other@example.test", "Old-secret!");

    assertThrows(
        IllegalArgumentException.class,
        () -> accountSecurityService.requestEmailChange(mockHttpRequest, emailChange));
    assertEquals(0, emailChangeTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void confirmationOutboxFailureRollsBackAddressTokenAndSessionRevocation() {
    requestChange("new@example.test");
    String raw = linkToken();
    doThrow(new IllegalStateException("Own simulated outbox failure"))
        .when(queue)
        .addAddressedNotification(any(), anyString(), anyString(), anyString());

    assertThrows(IllegalStateException.class, () -> accountSecurityService.confirm(raw));
    assertEquals("old@example.test", stored().getEmail());
    assertEquals(TokenStatus.ACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
    assertFalse(sessionRegistry.getSessionInformation("other-device").isExpired());
    assertNotNull(remembered.getTokenForSeries("remember-series"));
    assertEquals(1, emailQueueRepository.count());
  }

  @Test
  void concurrentConfirmationHasOneWinnerAndOneConsumedLink() throws Exception {
    requestChange("new@example.test");
    String raw = linkToken();
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      Callable<Confirmation> confirm =
          () -> {
            ready.countDown();

            assertTrue(start.await(10, TimeUnit.SECONDS));
            return accountSecurityService.confirm(raw);
          };
      Future<Confirmation> a = executorService.submit(confirm);
      Future<Confirmation> b = executorService.submit(confirm);

      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Confirmation> results =
          List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));

      assertEquals(1, results.stream().filter(r -> r == Confirmation.SUCCESS).count());
      assertEquals(1, results.stream().filter(r -> r == Confirmation.INVALID).count());
    }
    assertEquals(3, emailQueueRepository.count());
  }

  @Test
  void queuedRecipientNeverFollowsALaterUnrelatedAddressChange() throws Exception {
    requestChange("new@example.test");
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity user = userRepository.findById(testUserEntity.getId()).orElseThrow();
      user.setEmail("later@example.test");
      userRepository.saveAndFlush(user);
    });
    queue.processQueue();

    verify(emailService).sendEmailTo(eq("new@example.test"), anyString(), anyString());
    verify(emailService, never()).sendEmail(any(), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newMailsUseFixedAccountLanguageAndEscapeConfiguredHtml(String language) {
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity user = userRepository.findById(testUserEntity.getId()).orElseThrow();
      user.setPreferredLocale(Locale.forLanguageTag(language));
      userRepository.saveAndFlush(user);
    });
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Own <script>text</script>");
    try {
      accountSecurityService.changePassword(
          mockHttpRequest, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));
      EmailQueueEntity mail = emailQueueRepository.findAll().getFirst();

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
      when(configService.getString(ConfigEntry.SERVICE_NAME))
          .thenReturn(ConfigEntry.SERVICE_NAME.getDefaultValueString());
    }
  }

  @Test
  void currentPasswordChecksUseTheConfiguredPersistentAttemptLimit() {
    when(configService.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(1);
    when(configService.getInteger(ConfigEntry.LOGIN_ATTEMPT_WINDOW_SECONDS)).thenReturn(900);
    try {
      PasswordChange wrongPasswordChange =
          new PasswordChange("Wrong!", "New-secret!", "New-secret!");

      assertThrows(
          BadCredentialsException.class,
          () -> accountSecurityService.changePassword(mockHttpRequest, wrongPasswordChange));
      PasswordChange blockedPasswordChange =
          new PasswordChange("Old-secret!", "New-secret!", "New-secret!");

      assertThrows(
          LoginThrottledException.class,
          () -> accountSecurityService.changePassword(mockHttpRequest, blockedPasswordChange));
      assertTrue(passwordEncoder.matches("Old-secret!", stored().getPassword()));
      assertEquals(0, emailQueueRepository.count());
    } finally {
      when(configService.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT))
          .thenReturn(Integer.parseInt(ConfigEntry.LOGIN_ATTEMPTS_LIMIT.getDefaultValueString()));
    }
  }

  @Test
  void anonymousAndMaintenanceRequestsDoNotChangeCredentialsOrConsumeLinks() {
    requestChange("new@example.test");
    String raw = linkToken();
    SecurityContextHolder.clearContext();
    MockHttpServletRequest anonymous = new MockHttpServletRequest();
    PasswordChange passwordChange = new PasswordChange("Old-secret!", "New-secret!", "New-secret!");

    assertThrows(
        AccessDeniedException.class,
        () -> accountSecurityService.changePassword(anonymous, passwordChange));
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    try {
      assertThrows(MaintenanceAccessException.class, () -> accountSecurityService.confirm(raw));
      assertEquals(TokenStatus.ACTIVE, emailChangeTokenRepository.findAll().getFirst().getStatus());
      assertEquals("old@example.test", stored().getEmail());
    } finally {
      when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    }
  }

  @Test
  void twoAccountsCannotConfirmTheSameTargetAddressConcurrently() throws Exception {
    requestChange("target@example.test");
    String first = linkToken();
    UserEntity secondAccount = userRepository.saveAndFlush(newUser("second@example.test"));
    mockHttpRequest = new MockHttpServletRequest();
    sessionRegistry.registerNewSession(
        mockHttpRequest.getSession().getId(), secondAccount.getEmail());
    authenticate(secondAccount);
    requestChange("target@example.test");
    String second = linkToken();
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      Future<Confirmation> a =
          executorService.submit(() -> {
            ready.countDown();

            assertTrue(start.await(10, TimeUnit.SECONDS));
            return accountSecurityService.confirm(first);
          });
      Future<Confirmation> b =
          executorService.submit(() -> {
            ready.countDown();

            assertTrue(start.await(10, TimeUnit.SECONDS));
            return accountSecurityService.confirm(second);
          });

      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Confirmation> results =
          List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));

      assertEquals(1, results.stream().filter(r -> r == Confirmation.SUCCESS).count());
      assertEquals(1, results.stream().filter(r -> r == Confirmation.CONFLICT).count());
    }
    assertEquals(
        1,
        userRepository.findAll().stream()
            .filter(u -> u.getEmail().equals("target@example.test"))
            .count());
    assertEquals(4, emailQueueRepository.count());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void revokedSessionsFailHttpAndPushGuardsWhileTheCurrentSessionStaysValid(boolean push)
      throws Exception {
    SecurityContext context = SecurityContextHolder.getContext();
    mockHttpRequest.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
    mockHttpRequest
        .getSession()
        .setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    MockHttpSession otherSession = new MockHttpSession(null, "other-device");
    otherSession.setAttribute("SPRING_SECURITY_CONTEXT", context);
    otherSession.setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    MockHttpServletRequest other = new MockHttpServletRequest();
    other.setSession(otherSession);
    accountSecurityService.changePassword(
        mockHttpRequest, new PasswordChange("Old-secret!", "New-secret!", "New-secret!"));

    assertTrue(securityService.hasCurrentSessionPrivileges(mockHttpRequest));
    assertTrue(securityService.getAuthenticatedUser(mockHttpRequest).isPresent());
    if (push) {
      PushSecurityInterceptor interceptor =
          new PushSecurityInterceptor(securityService, new MaintenanceService(configService));
      VaadinServletRequest servletRequest =
          new VaadinServletRequest(other, mock(VaadinServletService.class));

      assertThrows(
          AccessDeniedException.class, () -> interceptor.requestStart(servletRequest, null));
    } else {
      ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
      when(provider.getObject()).thenReturn(securityService);
      MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);
      new VaadinSecurityFilter(provider).doFilter(other, mockHttpResponse, chain);

      assertEquals(401, mockHttpResponse.getStatus());
      verifyNoInteractions(chain);
    }
    assertTrue(otherSession.isInvalid());
  }

  @Test
  void revokedIdentityCanLoadLoginDocumentAfterTheOldSessionHasBeenCleared() throws Exception {
    SecurityContext context = SecurityContextHolder.getContext();
    mockHttpRequest.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
    mockHttpRequest
        .getSession()
        .setAttribute("authenticatedUser", context.getAuthentication().getPrincipal());
    requestChange("new@example.test");

    assertEquals(Confirmation.SUCCESS, accountSecurityService.confirm(linkToken()));
    MockHttpSession mockHttpSession = (MockHttpSession) mockHttpRequest.getSession();
    mockHttpRequest.setMethod("GET");
    mockHttpRequest.setServletPath(AppRoute.LOGIN.getSecurityRoute());
    ObjectProvider<SecurityService> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(securityService);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new VaadinSecurityFilter(provider).doFilter(mockHttpRequest, mockHttpResponse, chain);

    verify(chain).doFilter(mockHttpRequest, mockHttpResponse);
    assertEquals(200, mockHttpResponse.getStatus());
    assertTrue(mockHttpSession.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
