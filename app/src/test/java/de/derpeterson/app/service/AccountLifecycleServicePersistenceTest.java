package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.AccountDeactivation;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.PortalUserDetails;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.Owner;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.State;
import de.derpeterson.app.service.AccountLifecycleService.AccountAction;
import de.derpeterson.app.service.AccountLifecycleService.LastSuperadminException;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(AccountLifecycleServicePersistenceTest.Config.class)
class AccountLifecycleServicePersistenceTest {
  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      DriverManagerDataSource lifecycleTestDataSource =
          new DriverManagerDataSource(
              "jdbc:h2:mem:lifecycle-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
              "sa",
              "");
      new ResourceDatabasePopulator(
              new ClassPathResource("db/application-schema-h2.sql"),
              new ClassPathResource("db/durable-sessions-h2.sql"))
          .execute(lifecycleTestDataSource);
      return lifecycleTestDataSource;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(
        DataSource lifecycleTestDataSource) {
      LocalContainerEntityManagerFactoryBean entityManagerFactory =
          new LocalContainerEntityManagerFactoryBean();
      entityManagerFactory.setDataSource(lifecycleTestDataSource);
      entityManagerFactory.setPackagesToScan("de.derpeterson.app.model");
      entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      entityManagerFactory.setJpaPropertyMap(
          Map.of(
              "hibernate.hbm2ddl.auto",
              "validate",
              "hibernate.physical_naming_strategy",
              "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
              "hibernate.implicit_naming_strategy",
              "org.springframework.boot.hibernate.SpringImplicitNamingStrategy"));
      return entityManagerFactory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
    }
  }

  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private EmailQueueRepository emailQueueRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private DataSource dataSource;

  private EmailQueueService emailQueueService = mock(EmailQueueService.class);
  private final ConfigService configService = mock(ConfigService.class);
  private final TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
  private final LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private DeviceTokenRepository deviceTokenRepository;
  private AccountDeletionService accountDeletionService;
  private final SessionRegistry sessionRegistry = mock(SessionRegistry.class);
  private AccountLifecycleService accountLifecycleService;

  @BeforeEach
  void setUpLifecycleService() {
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS)).thenReturn(30);
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS)).thenReturn(7);
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.test/");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Cosmos Test");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("Cosmos Test");
    when(passwordEncoder.matches("correct-password", "test-encoded-password")).thenReturn(true);
    when(loginAttemptService.authenticate(any(), any()))
        .thenAnswer(invocation -> {
          Supplier<Authentication> passwordCheck = invocation.getArgument(1);
          return passwordCheck.get();
        });
    deviceTokenRepository = spy(new DeviceTokenRepository(dataSource, transactionManager));
    accountDeletionService =
        new AccountDeletionService(
            userRepository,
            new JdbcTemplate(dataSource),
            deviceTokenRepository,
            sessionRegistry,
            mock(AvatarDraftStore.class));
    createLifecycleService();
  }

  private void createLifecycleService() {
    accountLifecycleService =
        new AccountLifecycleService(
            userRepository,
            emailQueueRepository,
            emailQueueService,
            configService,
            new AccountSecurityMail(configService, new CustomI18NProvider()),
            twoFactorAuthorization,
            loginAttemptService,
            passwordEncoder,
            deviceTokenRepository,
            sessionRegistry,
            new JdbcTemplate(dataSource),
            transactionManager,
            mock(AvatarDraftStore.class),
            accountDeletionService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reactivationLinkClearsDeletionDeadlineAndCanOnlyBeUsedOnce(String language) {
    UserEntity accountUser = createAccountUser(true);
    accountUser.setPreferredLocale(Locale.forLanguageTag(language));
    userRepository.saveAndFlush(accountUser);
    MockHttpServletRequest accountRequest = authenticatedAccountRequest(accountUser);
    Instant beforeDeactivation = Instant.now();

    accountLifecycleService.changeOwnAccount(
        accountRequest, AccountAction.DEACTIVATE, "correct-password");

    UserEntity disabledAccountUser = userRepository.findById(accountUser.getId()).orElseThrow();
    assertFalse(disabledAccountUser.isEnabled());
    AccountDeactivation deactivation = disabledAccountUser.getAccountDeactivation();
    assertTrue(deactivation.deletionAt().isAfter(beforeDeactivation.plus(29, ChronoUnit.DAYS)));
    assertEquals(7, ChronoUnit.DAYS.between(deactivation.reminderAt(), deactivation.deletionAt()));
    verify(deviceTokenRepository).removeUserTokens(accountUser.getEmail());
    ArgumentCaptor<String> emailBodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailQueueService)
        .addEmailToQueue(
            any(UserEntity.class),
            any(),
            emailBodyCaptor.capture(),
            eq(EmailType.ACCOUNT_REACTIVATION));
    String reactivationUrl =
        Jsoup.parse(emailBodyCaptor.getValue()).select(".cosmos-email-button a").attr("href");
    String reactivationToken =
        java.net.URI.create(reactivationUrl).getQuery().substring("token=".length());

    assertTrue(accountLifecycleService.reactivateAccount(reactivationToken));
    assertFalse(accountLifecycleService.reactivateAccount(reactivationToken));
    UserEntity reactivatedAccountUser = userRepository.findById(accountUser.getId()).orElseThrow();
    assertTrue(reactivatedAccountUser.isEnabled());
    assertNull(reactivatedAccountUser.getAccountDeactivation());
    verify(emailQueueService)
        .addEmailToQueue(
            any(UserEntity.class),
            eq(
                language.equals("de")
                    ? "Ihr Konto wurde wieder aktiviert"
                    : "Your account was reactivated"),
            any(),
            eq(EmailType.NOTIFICATION));
  }

  @Test
  void resendRequiresCurrentProofAndRespectsDelayAndOpenMail() {
    UserEntity accountUser = createAccountUser(false);
    Instant deletionDeadline = Instant.now().plus(30, ChronoUnit.DAYS);
    accountUser.setAccountDeactivation(
        new AccountDeactivation(
            deletionDeadline, null, "initial-digest", null, null, Instant.now()));
    userRepository.saveAndFlush(accountUser);
    String credentialVersion =
        PortalUserDetails.credentialVersion(
            accountUser.getAccessVersion(), accountUser.getPassword());
    assertTrue(
        userRepository.findSecurityUser(accountUser.getEmail()).getFirst().getSelfDeactivated());
    assertFalse(
        accountLifecycleService.requestReactivationLink(accountUser.getId(), credentialVersion));
    verifyNoInteractions(emailQueueService);

    UserEntity delayedAccountUser = userRepository.findById(accountUser.getId()).orElseThrow();
    delayedAccountUser.setAccountDeactivation(
        new AccountDeactivation(
            deletionDeadline, null, "initial-digest", null, null, Instant.now().minusSeconds(301)));
    userRepository.saveAndFlush(delayedAccountUser);
    when(emailQueueService.hasOpenEmailForUserAndType(any(), eq(EmailType.ACCOUNT_REACTIVATION)))
        .thenReturn(true);
    assertFalse(
        accountLifecycleService.requestReactivationLink(accountUser.getId(), credentialVersion));
    when(emailQueueService.hasOpenEmailForUserAndType(any(), eq(EmailType.ACCOUNT_REACTIVATION)))
        .thenReturn(false);
    assertTrue(
        accountLifecycleService.requestReactivationLink(accountUser.getId(), credentialVersion));
    verify(emailQueueService)
        .addEmailToQueue(any(), any(), any(), eq(EmailType.ACCOUNT_REACTIVATION));
    assertFalse(
        accountLifecycleService.requestReactivationLink(accountUser.getId(), credentialVersion));
    assertThrows(
        AccessDeniedException.class,
        () -> accountLifecycleService.requestReactivationLink(accountUser.getId(), "old-proof"));

    UserEntity disabledAccount = userRepository.findById(accountUser.getId()).orElseThrow();
    disabledAccount.setEnabled(false);
    userRepository.saveAndFlush(disabledAccount);
    assertFalse(
        userRepository.findSecurityUser(accountUser.getEmail()).getFirst().getSelfDeactivated());
    assertThrows(
        AccessDeniedException.class,
        () ->
            accountLifecycleService.requestReactivationLink(
                accountUser.getId(), credentialVersion));
  }

  @Test
  void expiredSelfDeactivationIsDeletedAndAdministratorDeactivationIsPreserved() {
    UserEntity selfDeactivatedAccount = createAccountUser(false);
    selfDeactivatedAccount.setAccountDeactivation(
        new AccountDeactivation(
            Instant.now().minusSeconds(1), null, "initial-digest", null, null, null));
    userRepository.saveAndFlush(selfDeactivatedAccount);
    UserEntity administratorDeactivatedAccount = createAccountUser(false);

    accountLifecycleService.processDueDeactivations();

    assertFalse(userRepository.existsById(selfDeactivatedAccount.getId()));
    assertTrue(userRepository.existsById(administratorDeactivatedAccount.getId()));
    verify(emailQueueService)
        .addAccountDeletionConfirmation(eq(selfDeactivatedAccount.getEmail()), any(), any());
  }

  @Test
  void dueReminderIsQueuedOnlyOnceAndKeepsTheInitialReactivationLink() {
    UserEntity selfDeactivatedAccount = createAccountUser(false);
    selfDeactivatedAccount.setAccountDeactivation(
        new AccountDeactivation(
            Instant.now().plus(7, ChronoUnit.DAYS),
            Instant.now().minusSeconds(1),
            "initial-digest",
            null,
            null,
            null));
    userRepository.saveAndFlush(selfDeactivatedAccount);

    accountLifecycleService.processDueDeactivations();
    accountLifecycleService.processDueDeactivations();

    AccountDeactivation updatedDeactivation =
        userRepository
            .findById(selfDeactivatedAccount.getId())
            .orElseThrow()
            .getAccountDeactivation();
    assertNull(updatedDeactivation.reminderAt());
    assertEquals("initial-digest", updatedDeactivation.initialTokenDigest());
    verify(emailQueueService)
        .addEmailToQueue(
            any(UserEntity.class),
            eq("Your deactivated account will be deleted soon"),
            any(),
            eq(EmailType.ACCOUNT_REACTIVATION));
  }

  @Test
  void deletionRemovesPersonalMailAndNotificationDataAndOtherDurableSessions() {
    UserEntity accountUser = createAccountUser(true);
    UserEntity otherAccountUser = createAccountUser(true);
    MockHttpServletRequest accountRequest = authenticatedAccountRequest(accountUser);
    JdbcTemplate lifecycleJdbcTemplate = new JdbcTemplate(dataSource);
    Long accountUserId = accountUser.getId();
    lifecycleJdbcTemplate.update(
        "INSERT INTO email_queue (user_id, subject, body, email_type, status, created_at, retry_count) "
            + "VALUES (?, 'personal mail', 'personal body', 'NOTIFICATION', 'FAILED', ?, 0)",
        accountUserId,
        LocalDateTime.now());
    Long personalQueueId =
        lifecycleJdbcTemplate.queryForObject(
            "SELECT id FROM email_queue WHERE user_id = ?", Long.class, accountUserId);
    lifecycleJdbcTemplate.update(
        "INSERT INTO admin_notification_attempt (queue_id, started_at, status) VALUES (?, ?, 'SENT')",
        personalQueueId,
        LocalDateTime.now());

    String personalNotificationKey = "personal-" + UUID.randomUUID();
    lifecycleJdbcTemplate.update(
        "INSERT INTO user_notifications "
            + "(request_key, type, created_at, broadcast, recipient_count, sender_id, body) "
            + "VALUES (?, 'ADMIN_ANNOUNCEMENT', ?, false, 1, ?, 'private notice')",
        personalNotificationKey,
        Instant.now(),
        otherAccountUser.getId());
    Long personalNotificationId =
        lifecycleJdbcTemplate.queryForObject(
            "SELECT id FROM user_notifications WHERE request_key = ?",
            Long.class,
            personalNotificationKey);
    lifecycleJdbcTemplate.update(
        "INSERT INTO user_notification_recipients (notification_id, user_id, relevance) "
            + "VALUES (?, ?, 'ACTIVE')",
        personalNotificationId,
        accountUserId);
    String expiredSessionId = UUID.randomUUID().toString();
    lifecycleJdbcTemplate.update(
        "INSERT INTO SPRING_SESSION (PRIMARY_ID, SESSION_ID, CREATION_TIME, LAST_ACCESS_TIME, "
            + "MAX_INACTIVE_INTERVAL, EXPIRY_TIME, PRINCIPAL_NAME) VALUES (?, ?, 0, 0, 10, 10, ?)",
        UUID.randomUUID().toString(),
        expiredSessionId,
        accountUser.getEmail());

    accountLifecycleService.changeOwnAccount(
        accountRequest, AccountAction.DELETE, "correct-password");

    assertFalse(userRepository.existsById(accountUserId));
    assertTrue(userRepository.existsById(otherAccountUser.getId()));
    assertEquals(
        0,
        lifecycleJdbcTemplate.queryForObject(
            "SELECT count(*) FROM email_queue WHERE id = ?", Integer.class, personalQueueId));
    assertEquals(
        0,
        lifecycleJdbcTemplate.queryForObject(
            "SELECT count(*) FROM admin_notification_attempt WHERE queue_id = ?",
            Integer.class,
            personalQueueId));
    assertEquals(
        0,
        lifecycleJdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_notifications WHERE id = ?",
            Integer.class,
            personalNotificationId));
    assertEquals(
        0,
        lifecycleJdbcTemplate.queryForObject(
            "SELECT count(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
            Integer.class,
            expiredSessionId));
    verify(emailQueueService)
        .addAccountDeletionConfirmation(eq(accountUser.getEmail()), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"SELF", "ADMIN", "EXPIRED"})
  void allDeletionPathsRemoveConnectedAccountDataAndPreserveOtherAccounts(String deletionPath) {
    UserEntity deletedAccountUser = createAccountUser(true);
    UserEntity controlAccountUser = createAccountUser(true);
    JdbcTemplate accountJdbcTemplate = new JdbcTemplate(dataSource);
    seedAccountCredentials(accountJdbcTemplate, deletedAccountUser);
    seedAccountCredentials(accountJdbcTemplate, controlAccountUser);
    DeletionContentFixture contentFixture =
        seedMixedContent(accountJdbcTemplate, deletedAccountUser, controlAccountUser);
    usePersistentDeletionQueue();

    // Wie ein frisch geöffnetes Benutzerformular den Stand nach der Rollenanlage verwenden.
    UserEntity currentAccountUser =
        userRepository.findById(deletedAccountUser.getId()).orElseThrow();
    deleteThroughRequestedPath(deletionPath, currentAccountUser);

    assertAccountCredentialsRemoved(accountJdbcTemplate, deletedAccountUser);
    assertEquals(
        2,
        countRows(
            accountJdbcTemplate, "external_identities", "user_id", controlAccountUser.getId()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate, "persistent_logins", "username", controlAccountUser.getEmail()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate,
            "SPRING_SESSION",
            "PRINCIPAL_NAME",
            controlAccountUser.getEmail()));
    assertTrue(userRepository.existsById(controlAccountUser.getId()));
    assertMixedContentAfterDeletion(accountJdbcTemplate, contentFixture, controlAccountUser);
    assertEquals(
        deletionPath.equals("ADMIN") ? 0 : 1,
        countRows(accountJdbcTemplate, "email_queue", "recipient", deletedAccountUser.getEmail()));
  }

  @Test
  void deletionQueueFailureRollsBackAllPersistentCleanup() {
    UserEntity deletedAccountUser = createAccountUser(true);
    JdbcTemplate accountJdbcTemplate = new JdbcTemplate(dataSource);
    seedAccountCredentials(accountJdbcTemplate, deletedAccountUser);
    doThrow(new IllegalStateException("Test queue unavailable"))
        .when(emailQueueService)
        .addAccountDeletionConfirmation(any(), any(), any());

    assertThrows(
        IllegalStateException.class,
        () ->
            accountLifecycleService.changeOwnAccount(
                authenticatedAccountRequest(deletedAccountUser),
                AccountAction.DELETE,
                "correct-password"));

    assertTrue(userRepository.existsById(deletedAccountUser.getId()));
    assertEquals(
        2,
        countRows(
            accountJdbcTemplate, "external_identities", "user_id", deletedAccountUser.getId()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate, "persistent_logins", "username", deletedAccountUser.getEmail()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate,
            "SPRING_SESSION",
            "PRINCIPAL_NAME",
            deletedAccountUser.getEmail()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate, "verification_token", "user_id", deletedAccountUser.getId()));
    verifyNoInteractions(sessionRegistry);
  }

  private void usePersistentDeletionQueue() {
    emailQueueService =
        new EmailQueueService(
            configService,
            emailQueueRepository,
            mock(EmailService.class),
            userRepository,
            transactionManager,
            mock(AdminNotificationAttemptRepository.class),
            mock(EmailQueueWorkerPool.class));
    createLifecycleService();
  }

  private void deleteThroughRequestedPath(String deletionPath, UserEntity accountUser) {
    if (deletionPath.equals("SELF")) {
      accountLifecycleService.changeOwnAccount(
          authenticatedAccountRequest(accountUser), AccountAction.DELETE, "correct-password");
      return;
    }
    if (deletionPath.equals("EXPIRED")) {
      accountUser.setEnabled(false);
      accountUser.setAccountDeactivation(
          new AccountDeactivation(
              Instant.now().minusSeconds(1),
              null,
              "expired-" + accountUser.getId(),
              null,
              null,
              null));
      userRepository.saveAndFlush(accountUser);
      accountLifecycleService.processDueDeactivations();
      return;
    }

    UserService administrativeUserService =
        new UserService(
            userRepository,
            mock(UserStatusBroadcaster.class),
            passwordEncoder,
            mock(UserProfileBroadcaster.class),
            mock(AdminRolePolicy.class),
            mock(ApplicationEventPublisher.class),
            accountDeletionService);
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            transactionStatus -> administrativeUserService.deleteUser(accountUser));
  }

  private void seedAccountCredentials(JdbcTemplate accountJdbcTemplate, UserEntity accountUser) {
    seedProviderAndFactorData(accountJdbcTemplate, accountUser);
    seedActionTokensAndLoginAttempt(accountJdbcTemplate, accountUser);
    seedRememberedLoginAndSession(accountJdbcTemplate, accountUser);
  }

  private void seedProviderAndFactorData(JdbcTemplate accountJdbcTemplate, UserEntity accountUser) {
    Long accountUserId = accountUser.getId();
    Instant createdAt = Instant.now();
    for (String providerName : List.of("GOOGLE", "MICROSOFT")) {
      accountJdbcTemplate.update(
          "INSERT INTO external_identities (user_id, provider, issuer, subject) VALUES (?, ?, 'test-issuer', ?)",
          accountUserId,
          providerName,
          UUID.randomUUID().toString());
    }
    accountJdbcTemplate.update(
        "INSERT INTO user_avatar (user_id, version, has_image, mode, image_revision, image_data, uploaded_at, original_format, original_size_bytes, original_width, original_height) VALUES (?, 0, true, 'CUSTOM', ?, ?, ?, 'PNG', 3, 1, 1)",
        accountUserId,
        UUID.randomUUID(),
        new byte[] {1, 2, 3},
        createdAt);
    accountJdbcTemplate.update(
        "INSERT INTO two_factor_accounts (user_id, version, revision, previously_enrolled, operator_enrollment, accepted_step, failed_attempts) VALUES (?, 0, ?, false, false, -1, 0)",
        accountUserId,
        UUID.randomUUID());
    accountJdbcTemplate.update(
        "INSERT INTO two_factor_recovery_codes (user_id, digest) VALUES (?, 'test-recovery-digest')",
        accountUserId);
    accountJdbcTemplate.update(
        "INSERT INTO two_factor_challenges (id, version, user_id, session_digest, access_version, factor_revision, policy_revision, action, kind, created_at, expires_at, consumed, attempts, operator_enrollment) VALUES (?, 0, ?, 'test-session', ?, ?, 0, 'LOGIN', 'ACTION', ?, ?, false, 0, false)",
        UUID.randomUUID(),
        accountUserId,
        accountUser.getAccessVersion().toString(),
        UUID.randomUUID(),
        createdAt,
        createdAt.plusSeconds(600));
  }

  private void seedActionTokensAndLoginAttempt(
      JdbcTemplate accountJdbcTemplate, UserEntity accountUser) {
    Long accountUserId = accountUser.getId();
    Instant createdAt = Instant.now();
    for (String tokenTable : List.of("verification_token", "password_reset_token")) {
      accountJdbcTemplate.update(
          "INSERT INTO "
              + tokenTable
              + " (user_id, token, status, expiry_date) VALUES (?, ?, 'ACTIVE', ?)",
          accountUserId,
          UUID.randomUUID().toString(),
          LocalDateTime.now().plusDays(1));
    }
    accountJdbcTemplate.update(
        "INSERT INTO email_change_token (user_id, created_at, expiry_date, credential_digest, digest, old_email, new_email, status) VALUES (?, ?, ?, 'test-credential', ?, ?, 'new@example.test', 'ACTIVE')",
        accountUserId,
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(1),
        UUID.randomUUID().toString(),
        accountUser.getEmail());
    accountJdbcTemplate.update(
        "INSERT INTO login_attempts (identity_hash, failures, last_activity, window_started_at) VALUES (?, 1, ?, ?)",
        LoginAttemptService.identityKey(accountUser.getEmail()),
        createdAt,
        createdAt);
  }

  private void seedRememberedLoginAndSession(
      JdbcTemplate accountJdbcTemplate, UserEntity accountUser) {
    Instant createdAt = Instant.now();
    String rememberedSeries = UUID.randomUUID().toString();
    deviceTokenRepository.createNewToken(
        new PersistentRememberMeToken(
            accountUser.getEmail(), rememberedSeries, "test-token", Date.from(createdAt)));
    deviceTokenRepository.describe(
        rememberedSeries,
        accountUser.getEmail(),
        new DeviceDescription("Windows", "Chrome", false),
        createdAt);
    String sessionPrimaryId = UUID.randomUUID().toString();
    accountJdbcTemplate.update(
        "INSERT INTO SPRING_SESSION (PRIMARY_ID, SESSION_ID, CREATION_TIME, LAST_ACCESS_TIME, MAX_INACTIVE_INTERVAL, EXPIRY_TIME, PRINCIPAL_NAME) VALUES (?, ?, 0, 0, 600, 600000, ?)",
        sessionPrimaryId,
        UUID.randomUUID().toString(),
        accountUser.getEmail());
    accountJdbcTemplate.update(
        "INSERT INTO SPRING_SESSION_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) VALUES (?, 'test-attribute', ?)",
        sessionPrimaryId,
        new byte[] {1, 2});
  }

  private record DeletionContentFixture(
      Long ownTicketId,
      Long foreignTicketId,
      Long ownReplyId,
      Long foreignReplyId,
      Long sharedNotificationId,
      Long personalNotificationId,
      Long ticketNotificationId,
      Long personalQueueId) {}

  private DeletionContentFixture seedMixedContent(
      JdbcTemplate accountJdbcTemplate, UserEntity accountUser, UserEntity controlAccountUser) {
    Long ownTicketId = insertTicket(accountJdbcTemplate, accountUser.getId(), null, "OPEN");
    Long foreignTicketId =
        insertTicket(
            accountJdbcTemplate, controlAccountUser.getId(), accountUser.getId(), "IN_PROGRESS");
    insertReply(accountJdbcTemplate, ownTicketId, controlAccountUser.getId());
    Long ownReplyId = insertReply(accountJdbcTemplate, foreignTicketId, accountUser.getId());
    Long foreignReplyId =
        insertReply(accountJdbcTemplate, foreignTicketId, controlAccountUser.getId());
    accountJdbcTemplate.update(
        "INSERT INTO support_ticket_viewers (ticket_id, user_id, requester_read_through, admin_read_through, subscribed) VALUES (?, ?, 0, 0, true)",
        foreignTicketId,
        accountUser.getId());
    accountJdbcTemplate.update(
        "INSERT INTO support_ticket_viewers (ticket_id, user_id, requester_read_through, admin_read_through, subscribed) VALUES (?, ?, 0, 0, true)",
        foreignTicketId,
        controlAccountUser.getId());
    Long sharedNotificationId =
        insertNotification(accountJdbcTemplate, controlAccountUser.getId(), accountUser.getId());
    accountJdbcTemplate.update(
        "INSERT INTO user_notification_recipients (notification_id, user_id, relevance) VALUES (?, ?, 'ACTIVE')",
        sharedNotificationId,
        controlAccountUser.getId());
    accountJdbcTemplate.update(
        "UPDATE user_notifications SET recipient_count = 2 WHERE id = ?", sharedNotificationId);
    Long personalNotificationId =
        insertNotification(accountJdbcTemplate, controlAccountUser.getId(), accountUser.getId());
    Long ticketNotificationId =
        insertNotification(
            accountJdbcTemplate, controlAccountUser.getId(), controlAccountUser.getId());
    accountJdbcTemplate.update(
        "UPDATE user_notifications SET support_ticket_number = ?, support_reply_id = ? WHERE id = ?",
        SupportTicketService.ticketNumber(ownTicketId),
        ownReplyId,
        ticketNotificationId);
    insertNotification(accountJdbcTemplate, accountUser.getId(), controlAccountUser.getId());
    accountJdbcTemplate.update(
        "INSERT INTO email_queue (user_id, subject, body, email_type, status, created_at, retry_count) VALUES (?, 'old mail', 'old body', 'NOTIFICATION', 'FAILED', ?, 0)",
        accountUser.getId(),
        LocalDateTime.now());
    Long personalQueueId =
        accountJdbcTemplate.queryForObject(
            "SELECT id FROM email_queue WHERE user_id = ?", Long.class, accountUser.getId());
    accountJdbcTemplate.update(
        "INSERT INTO admin_notification_attempt (queue_id, started_at, status) VALUES (?, ?, 'SENT')",
        personalQueueId,
        LocalDateTime.now());
    RoleEntity userRole =
        roleRepository
            .findByName(RoleType.ROLE_USER)
            .orElseGet(() -> {
              RoleEntity createdRole = new RoleEntity();
              createdRole.setName(RoleType.ROLE_USER);
              return roleRepository.saveAndFlush(createdRole);
            });
    accountUser.setRoleEntities(List.of(userRole));
    userRepository.saveAndFlush(accountUser);
    return new DeletionContentFixture(
        ownTicketId,
        foreignTicketId,
        ownReplyId,
        foreignReplyId,
        sharedNotificationId,
        personalNotificationId,
        ticketNotificationId,
        personalQueueId);
  }

  private Long insertTicket(
      JdbcTemplate accountJdbcTemplate, Long ownerId, Long assigneeId, String statusName) {
    String ticketSubject = "ticket-" + UUID.randomUUID();
    accountJdbcTemplate.update(
        "INSERT INTO support_tickets (version, owner_id, assignee_id, type, status, subject, created_at, updated_at, public_updated_at) VALUES (0, ?, ?, 'RECOVERY', ?, ?, ?, ?, ?)",
        ownerId,
        assigneeId,
        statusName,
        ticketSubject,
        Instant.now(),
        Instant.now(),
        Instant.now());
    return accountJdbcTemplate.queryForObject(
        "SELECT id FROM support_tickets WHERE subject = ?", Long.class, ticketSubject);
  }

  private Long insertReply(JdbcTemplate accountJdbcTemplate, Long ticketId, Long authorId) {
    String replyBody = "reply-" + UUID.randomUUID();
    accountJdbcTemplate.update(
        "INSERT INTO support_replies (ticket_id, author_id, support_author, kind, body, internal, created_at) VALUES (?, ?, true, 'MESSAGE', ?, false, ?)",
        ticketId,
        authorId,
        replyBody,
        Instant.now());
    return accountJdbcTemplate.queryForObject(
        "SELECT id FROM support_replies WHERE body = ?", Long.class, replyBody);
  }

  private Long insertNotification(
      JdbcTemplate accountJdbcTemplate, Long senderId, Long recipientId) {
    String notificationKey = UUID.randomUUID().toString();
    accountJdbcTemplate.update(
        "INSERT INTO user_notifications (request_key, type, created_at, broadcast, recipient_count, sender_id, body) VALUES (?, 'ADMIN_ANNOUNCEMENT', ?, false, 1, ?, 'test notice')",
        notificationKey,
        Instant.now(),
        senderId);
    Long notificationId =
        accountJdbcTemplate.queryForObject(
            "SELECT id FROM user_notifications WHERE request_key = ?", Long.class, notificationKey);
    accountJdbcTemplate.update(
        "INSERT INTO user_notification_recipients (notification_id, user_id, relevance) VALUES (?, ?, 'ACTIVE')",
        notificationId,
        recipientId);
    return notificationId;
  }

  private void assertAccountCredentialsRemoved(
      JdbcTemplate accountJdbcTemplate, UserEntity accountUser) {
    assertFalse(userRepository.existsById(accountUser.getId()));
    for (String accountTable :
        List.of(
            "external_identities",
            "user_avatar",
            "users_roles",
            "two_factor_accounts",
            "two_factor_recovery_codes",
            "two_factor_challenges",
            "verification_token",
            "password_reset_token",
            "email_change_token",
            "support_ticket_viewers",
            "user_notification_recipients",
            "email_queue")) {
      assertEquals(
          0,
          countRows(accountJdbcTemplate, accountTable, "user_id", accountUser.getId()),
          accountTable);
    }
    for (String rememberedTable : List.of("persistent_logins", "remembered_device")) {
      assertEquals(
          0,
          countRows(accountJdbcTemplate, rememberedTable, "username", accountUser.getEmail()),
          rememberedTable);
    }
    assertEquals(
        0,
        countRows(accountJdbcTemplate, "SPRING_SESSION", "PRINCIPAL_NAME", accountUser.getEmail()));
    assertEquals(
        0,
        accountJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES a WHERE NOT EXISTS (SELECT 1 FROM SPRING_SESSION s WHERE s.PRIMARY_ID = a.SESSION_PRIMARY_ID)",
            Integer.class));
    assertEquals(
        0,
        countRows(
            accountJdbcTemplate,
            "login_attempts",
            "identity_hash",
            LoginAttemptService.identityKey(accountUser.getEmail())));
    assertEquals(
        0, countRows(accountJdbcTemplate, "support_replies", "author_id", accountUser.getId()));
    assertEquals(
        0, countRows(accountJdbcTemplate, "user_notifications", "sender_id", accountUser.getId()));
  }

  private void assertMixedContentAfterDeletion(
      JdbcTemplate accountJdbcTemplate,
      DeletionContentFixture contentFixture,
      UserEntity controlAccountUser) {
    assertEquals(
        0, countRows(accountJdbcTemplate, "support_tickets", "id", contentFixture.ownTicketId()));
    assertEquals(
        0,
        countRows(
            accountJdbcTemplate, "support_replies", "ticket_id", contentFixture.ownTicketId()));
    assertEquals(
        0, countRows(accountJdbcTemplate, "support_replies", "id", contentFixture.ownReplyId()));
    assertEquals(
        1,
        countRows(accountJdbcTemplate, "support_replies", "id", contentFixture.foreignReplyId()));
    assertEquals(
        "OPEN",
        accountJdbcTemplate.queryForObject(
            "SELECT status FROM support_tickets WHERE id = ?",
            String.class,
            contentFixture.foreignTicketId()));
    assertNull(
        accountJdbcTemplate.queryForObject(
            "SELECT assignee_id FROM support_tickets WHERE id = ?",
            Long.class,
            contentFixture.foreignTicketId()));
    assertEquals(
        1,
        countRows(
            accountJdbcTemplate, "support_ticket_viewers", "user_id", controlAccountUser.getId()));
    assertEquals(
        1,
        accountJdbcTemplate.queryForObject(
            "SELECT recipient_count FROM user_notifications WHERE id = ?",
            Integer.class,
            contentFixture.sharedNotificationId()));
    assertEquals(
        0,
        countRows(
            accountJdbcTemplate,
            "user_notifications",
            "id",
            contentFixture.personalNotificationId()));
    assertEquals(
        0,
        countRows(
            accountJdbcTemplate,
            "user_notifications",
            "id",
            contentFixture.ticketNotificationId()));
    assertEquals(
        0,
        countRows(
            accountJdbcTemplate,
            "admin_notification_attempt",
            "queue_id",
            contentFixture.personalQueueId()));
    assertTrue(roleRepository.findByName(RoleType.ROLE_USER).isPresent());
  }

  private int countRows(
      JdbcTemplate accountJdbcTemplate, String tableName, String columnName, Object expectedValue) {
    // Tabellen- und Spaltennamen stammen ausschließlich aus diesem festen Testkatalog.
    return accountJdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
        Integer.class,
        expectedValue);
  }

  @Test
  void configuredRetentionIsFrozenAndZeroDisablesTheReminder() {
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS)).thenReturn(10);
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS)).thenReturn(0);
    UserEntity accountUser = createAccountUser(true);
    MockHttpServletRequest accountRequest = authenticatedAccountRequest(accountUser);
    Instant beforeDeactivation = Instant.now();

    accountLifecycleService.changeOwnAccount(
        accountRequest, AccountAction.DEACTIVATE, "correct-password");

    AccountDeactivation savedDeactivation =
        userRepository.findById(accountUser.getId()).orElseThrow().getAccountDeactivation();
    assertTrue(savedDeactivation.deletionAt().isAfter(beforeDeactivation.plus(9, ChronoUnit.DAYS)));
    assertTrue(savedDeactivation.deletionAt().isBefore(Instant.now().plus(10, ChronoUnit.DAYS)));
    assertNull(savedDeactivation.reminderAt());
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS)).thenReturn(1);

    assertEquals(
        savedDeactivation.deletionAt(),
        userRepository
            .findById(accountUser.getId())
            .orElseThrow()
            .getAccountDeactivation()
            .deletionAt());
  }

  @Test
  void configuredAuthenticatorTakesPrecedenceOverTheCorrectPortalPassword() {
    UserEntity accountUser = createAccountUser(true);
    MockHttpServletRequest accountRequest = authenticatedAccountRequest(accountUser);
    Owner accountOwner = twoFactorAuthorization.owner(accountRequest);
    UUID factorRevision = UUID.randomUUID();
    when(twoFactorAuthorization.state(accountOwner))
        .thenReturn(new State(true, true, false, factorRevision, Instant.now()));
    when(twoFactorAuthorization.verifyAccountLifecycleCode(accountOwner, "correct-password"))
        .thenThrow(new BadCredentialsException("Invalid authenticator code"));

    assertThrows(
        BadCredentialsException.class,
        () ->
            accountLifecycleService.changeOwnAccount(
                accountRequest, AccountAction.DELETE, "correct-password"));
    assertTrue(userRepository.existsById(accountUser.getId()));
    verifyNoInteractions(loginAttemptService, emailQueueService);
  }

  @Test
  void lastEnabledSuperadminCannotDeactivateOrDeleteOwnAccount() {
    RoleEntity superadminRole = new RoleEntity();
    superadminRole.setName(RoleType.ROLE_SUPERADMIN);
    RoleEntity savedSuperadminRole = roleRepository.saveAndFlush(superadminRole);
    UserEntity superadminUser = createAccountUser(true);
    superadminUser.setRoleEntities(List.of(savedSuperadminRole));
    userRepository.saveAndFlush(superadminUser);
    MockHttpServletRequest superadminRequest = authenticatedAccountRequest(superadminUser);

    assertThrows(
        LastSuperadminException.class,
        () ->
            accountLifecycleService.changeOwnAccount(
                superadminRequest, AccountAction.DEACTIVATE, "correct-password"));
    assertThrows(
        LastSuperadminException.class,
        () ->
            accountLifecycleService.changeOwnAccount(
                superadminRequest, AccountAction.DELETE, "correct-password"));
    assertTrue(userRepository.findById(superadminUser.getId()).orElseThrow().isEnabled());
    verifyNoInteractions(emailQueueService, deviceTokenRepository);
  }

  private MockHttpServletRequest authenticatedAccountRequest(UserEntity accountUser) {
    MockHttpServletRequest accountRequest = new MockHttpServletRequest();
    accountRequest.getSession();
    Owner accountOwner =
        new Owner(
            accountUser.getId(),
            accountUser.getEmail(),
            accountUser.getAccessVersion(),
            true,
            true,
            List.of());
    when(twoFactorAuthorization.owner(accountRequest)).thenReturn(accountOwner);
    when(twoFactorAuthorization.state(accountOwner))
        .thenReturn(new State(false, false, false, null, null));
    return accountRequest;
  }

  private UserEntity createAccountUser(boolean enabled) {
    return createAccountUser(enabled, false);
  }

  private UserEntity createAccountUser(boolean enabled, boolean developmentAccount) {
    UserEntity accountUser =
        UserEntity.builder()
            .email("lifecycle-" + UUID.randomUUID() + "@example.test")
            .firstName("Lifecycle")
            .lastName("Test")
            .preferredLocale(Locale.ENGLISH)
            .password("test-encoded-password")
            .enabled(enabled)
            .developmentAccount(developmentAccount)
            .build();
    return userRepository.saveAndFlush(accountUser);
  }

  @Test
  void developerDeactivationAndDeletionNeverQueueMailIncludingAfterAccountRemoval() {
    UserEntity developmentUserEntity = createAccountUser(true, true);
    MockHttpServletRequest developmentAccountRequest =
        authenticatedAccountRequest(developmentUserEntity);

    accountLifecycleService.changeOwnAccount(
        developmentAccountRequest, AccountAction.DEACTIVATE, "correct-password");
    UserEntity deactivatedUserEntity =
        userRepository.findById(developmentUserEntity.getId()).orElseThrow();
    accountLifecycleService.requestReactivationLink(
        deactivatedUserEntity.getId(),
        PortalUserDetails.credentialVersion(
            deactivatedUserEntity.getAccessVersion(), deactivatedUserEntity.getPassword()));
    deactivatedUserEntity.setAccountDeactivation(
        new AccountDeactivation(
            Instant.now().minusSeconds(1), null, "expired-test-token", null, null, null));
    userRepository.saveAndFlush(deactivatedUserEntity);
    accountLifecycleService.processDueDeactivations();

    assertFalse(userRepository.existsById(developmentUserEntity.getId()));
    verifyNoInteractions(emailQueueService);
  }
}
