package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.Owner;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.State;
import de.derpeterson.app.service.AccountLifecycleService.AccountAction;
import de.derpeterson.app.service.AccountLifecycleService.LastSuperadminException;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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

  private final EmailQueueService emailQueueService = mock(EmailQueueService.class);
  private final ConfigService configService = mock(ConfigService.class);
  private final TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
  private final LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
  private final SessionRegistry sessionRegistry = mock(SessionRegistry.class);
  private AccountLifecycleService accountLifecycleService;

  @BeforeEach
  void setUpLifecycleService() {
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS)).thenReturn(30);
    when(configService.getInteger(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS)).thenReturn(7);
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.test/");
    when(passwordEncoder.matches("correct-password", "test-encoded-password")).thenReturn(true);
    when(loginAttemptService.authenticate(any(), any()))
        .thenAnswer(invocation -> {
          Supplier<Authentication> passwordCheck = invocation.getArgument(1);
          return passwordCheck.get();
        });
    accountLifecycleService =
        new AccountLifecycleService(
            userRepository,
            emailQueueRepository,
            emailQueueService,
            configService,
            new CustomI18NProvider(),
            twoFactorAuthorization,
            loginAttemptService,
            passwordEncoder,
            deviceTokenRepository,
            sessionRegistry,
            new JdbcTemplate(dataSource),
            transactionManager,
            mock(AvatarDraftStore.class));
  }

  @Test
  void reactivationLinkClearsDeletionDeadlineAndCanOnlyBeUsedOnce() {
    UserEntity accountUser = createAccountUser(true);
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
    String reactivationUrl = Jsoup.parse(emailBodyCaptor.getValue()).select("a").attr("href");
    String reactivationToken =
        java.net.URI.create(reactivationUrl).getQuery().substring("token=".length());

    assertTrue(accountLifecycleService.reactivateAccount(reactivationToken));
    assertFalse(accountLifecycleService.reactivateAccount(reactivationToken));
    UserEntity reactivatedAccountUser = userRepository.findById(accountUser.getId()).orElseThrow();
    assertTrue(reactivatedAccountUser.isEnabled());
    assertNull(reactivatedAccountUser.getAccountDeactivation());
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
    UserEntity accountUser =
        UserEntity.builder()
            .email("lifecycle-" + UUID.randomUUID() + "@example.test")
            .firstName("Lifecycle")
            .lastName("Test")
            .preferredLocale(Locale.ENGLISH)
            .password("test-encoded-password")
            .enabled(enabled)
            .build();
    return userRepository.saveAndFlush(accountUser);
  }
}
