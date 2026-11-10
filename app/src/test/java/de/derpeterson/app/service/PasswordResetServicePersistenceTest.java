package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Real service transaction proxy and isolated H2; no Boot, scheduler or SMTP. */
@SpringJUnitConfig(PasswordResetServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetServicePersistenceTest {
  @Autowired private PasswordResetService service;
  @Autowired private UserNotificationService userNotifications;

  @Autowired
  private de.derpeterson.app.repository.UserNotificationRepository userNotificationsRepository;

  @Autowired
  private de.derpeterson.app.repository.UserNotificationRecipientRepository
      userRecipientsRepository;

  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionTemplate transaction;
  @Autowired private Probes probes;
  @Autowired private EmailQueueRepository emailQueueRepository;
  @Autowired private Clock testClock;
  private Long userId;

  @BeforeEach
  void setup() {
    reset(testClock);
    when(testClock.getZone()).thenReturn(ZoneId.systemDefault());
    when(testClock.instant()).thenAnswer(call -> Instant.now());
    reset(
        probes.passwordResetTokenRepository(),
        probes.userRepository(),
        probes.config(),
        probes.encoder(),
        probes.queue());
    when(probes.config().getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("180");
    when(probes.config().getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn("365");
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION))
        .thenReturn("P7D");
    when(probes.config().getString(ConfigEntry.COPYRIGHT_TEXT))
        .thenReturn("© {year} {serviceName} (isolated fixture)");
    when(probes.config().getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
    when(probes.config().getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    when(probes.encoder().encode(any())).thenAnswer(call -> "hash-" + call.getArgument(0));
    transaction.executeWithoutResult(tx -> {
      userRecipientsRepository.deleteAll();
      userNotificationsRepository.deleteAll();
      emailQueueRepository.deleteAll();
      passwordResetTokenRepository.deleteAll();
      userRepository.deleteAll();
      userRepository.flush();
      userId =
          userRepository
              .saveAndFlush(
                  UserEntity.builder()
                      .firstName("Test")
                      .lastName("User")
                      .email("test@example.com")
                      .password("old-hash")
                      .gender(Gender.OTHER)
                      .birthDate(LocalDate.of(1990, 1, 1))
                      .preferredLocale(Locale.ENGLISH)
                      .enabled(true)
                      .build())
              .getId();
    });
  }

  private void seed(String value, TokenStatus status, LocalDateTime expiry) {
    transaction.executeWithoutResult(tx ->
        passwordResetTokenRepository.saveAndFlush(
            new PasswordResetTokenEntity(
                null, value, userRepository.findById(userId).orElseThrow(), expiry, status)));
  }

  private TokenStatus status(String value) {
    return transaction.execute(
        tx -> passwordResetTokenRepository.findByToken(value).orElseThrow().getStatus());
  }

  private String password() {
    return userRepository.findById(userId).orElseThrow().getPassword();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"missing"})
  void absentTokenLookupsAndEntrypointsRemainEmpty(String token) {
    assertTrue(passwordResetTokenRepository.findByToken(token).isEmpty());
    assertTrue(
        passwordResetTokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE).isEmpty());
    assertEquals(PasswordResetService.LinkState.INVALID, service.inspectToken(token));
    assertFalse(service.resetPassword(token, "Password!"));
    assertFalse(passwordResetTokenRepository.findByToken(token).isPresent());
    service.setTokenStatus(token, TokenStatus.USED);

    assertEquals("old-hash", password());
  }

  @Test
  void successfulResetCommitsPasswordAndConsumptionTogetherAndSequentialReuseFails() {
    seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

    assertTrue(service.resetPassword("valid", "Password!"));
    assertEquals("hash-Password!", password());
    assertEquals(TokenStatus.USED, status("valid"));
    assertEquals(1, userNotificationsRepository.count());
    assertEquals(
        de.derpeterson.app.model.enums.UserNotificationType.PASSWORD_RESET,
        userNotificationsRepository.findAll().getFirst().getType());
    assertTrue(passwordResetTokenRepository.findByToken("valid").isPresent());
    assertEquals(PasswordResetService.LinkState.INVALID, service.inspectToken("valid"));
    assertFalse(service.resetPassword("valid", "OtherPassword!"));
    assertEquals("hash-Password!", password());
    verify(probes.encoder(), times(1)).encode(any());
  }

  @Test
  void messagePersistenceFailureRollsBackPasswordAndResetConsumption() {
    seed("message-failure", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    Object target = org.springframework.test.util.AopTestUtils.getTargetObject(service);
    UserNotificationService failure = mock(UserNotificationService.class);
    doThrow(new IllegalStateException("message storage failed"))
        .when(failure)
        .recordSecurityEvent(any(), any());
    org.springframework.test.util.ReflectionTestUtils.setField(
        target, "userNotifications", failure);
    try {
      assertThrows(
          IllegalStateException.class, () -> service.resetPassword("message-failure", "Password!"));
      assertEquals("old-hash", password());
      assertEquals(TokenStatus.ACTIVE, status("message-failure"));
      assertEquals(0, userNotificationsRepository.count());
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(
          target, "userNotifications", userNotifications);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "invalidate"})
  void resetFollowedByAnotherAccountLockPreservesPasswordAtOuterCommit(String next) {
    seed("outer", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    AtomicReference<String> created = new AtomicReference<String>();
    transaction.executeWithoutResult(tx -> {
      assertTrue(service.resetPassword("outer", "Password!"));
      UserEntity managed = userRepository.findById(userId).orElseThrow();
      // Also preserve another local edit, not just the reset method's field.
      managed.setFirstName("Updated");
      if (next.equals("create")) {
        created.set(service.createToken(managed));
      } else {
        service.setInactiveTokensForUser(managed);
      }
      assertEquals("hash-Password!", managed.getPassword());
      assertEquals("Updated", managed.getFirstName());
    });

    assertEquals("hash-Password!", password());
    assertEquals("Updated", userRepository.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.USED, status("outer"));
    assertFalse(service.resetPassword("outer", "OtherPassword!"));
    if (next.equals("create")) {
      assertEquals(TokenStatus.ACTIVE, status(created.get()));
      assertEquals(2, passwordResetTokenRepository.count());
    } else {
      assertEquals(1, passwordResetTokenRepository.count());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "invalidate"})
  void outerRollbackRestoresPasswordAndTokensAfterRepeatedAccountLock(String next) {
    seed("outer", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    seed("other", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    AtomicReference<String> created = new AtomicReference<String>();
    transaction.executeWithoutResult(tx -> {
      assertTrue(service.resetPassword("outer", "Password!"));
      UserEntity managed = userRepository.findById(userId).orElseThrow();
      managed.setFirstName("Updated");
      if (next.equals("create")) {
        created.set(service.createToken(managed));
      } else {
        service.setInactiveTokensForUser(managed);
      }
      assertEquals("hash-Password!", managed.getPassword());
      assertEquals(TokenStatus.USED, status("outer"));
      assertEquals(TokenStatus.INACTIVE, status("other"));
      tx.setRollbackOnly();
    });

    assertEquals("old-hash", password());
    assertEquals("Test", userRepository.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.ACTIVE, status("outer"));
    assertEquals(TokenStatus.ACTIVE, status("other"));
    assertEquals(2, passwordResetTokenRepository.count());
    if (next.equals("create")) {
      assertFalse(passwordResetTokenRepository.findByToken(created.get()).isPresent());
    }
    assertTrue(service.resetPassword("outer", "RetryPassword!"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void realMailQueueRelockPreservesResetAndParticipatesInOuterRollback(boolean rollback) {
    seed("outer-mail", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    transaction.executeWithoutResult(tx -> {
      assertTrue(service.resetPassword("outer-mail", "Password!"));
      try {
        assertTrue(service.sendPasswordResetEmail("test@example.com"));
      } catch (IOException failure) {
        throw new IllegalStateException(failure);
      }
      assertEquals("hash-Password!", userRepository.findById(userId).orElseThrow().getPassword());
      assertEquals(1, emailQueueRepository.count());
      if (rollback) {
        tx.setRollbackOnly();
      }
    });

    assertEquals(rollback ? "old-hash" : "hash-Password!", password());
    assertEquals(rollback ? TokenStatus.ACTIVE : TokenStatus.USED, status("outer-mail"));
    assertEquals(rollback ? 0 : 1, emailQueueRepository.count());
    assertEquals(rollback ? 1 : 2, passwordResetTokenRepository.count());
    if (!rollback) {
      String active =
          transaction.execute(tx ->
              passwordResetTokenRepository
                  .findAllByUserEntityAndStatus(
                      userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                  .getFirst()
                  .getToken());

      assertTrue(
          emailQueueRepository
              .findAll()
              .getFirst()
              .getBody()
              .contains("/reset-password/" + active));
    }
  }

  @Test
  void firstAccountLockPreservesOwnPendingChangesWithoutCommittingThem() {
    transaction.executeWithoutResult(tx -> {
      UserEntity managed = userRepository.findById(userId).orElseThrow();
      managed.setFirstName("Local");

      assertSame(managed, userRepository.lockVerificationUser(userId).orElseThrow());
      assertEquals("Local", managed.getFirstName());
      tx.setRollbackOnly();
    });

    assertEquals("Test", userRepository.findById(userId).orElseThrow().getFirstName());
  }

  @Test
  void firstAccountLockRefreshesAnUnchangedStaleEnabledAccountBeforeReset() throws Exception {
    seed("blocked", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    CountDownLatch loaded = new CountDownLatch(1);
    CountDownLatch disabled = new CountDownLatch(1);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> result =
          executorService.submit(() ->
              transaction.execute(tx -> {
                UserEntity stale = userRepository.findById(userId).orElseThrow();

                assertTrue(stale.isEnabled());
                loaded.countDown();
                try {
                  assertTrue(disabled.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                  Thread.currentThread().interrupt();
                  throw new IllegalStateException(failure);
                }
                return service.resetPassword("blocked", "Password!");
              }));

      assertTrue(loaded.await(10, TimeUnit.SECONDS));
      transaction.executeWithoutResult(
          tx -> userRepository.findById(userId).orElseThrow().setEnabled(false));
      disabled.countDown();

      assertFalse(result.get(10, TimeUnit.SECONDS));
    } finally {
      disabled.countDown();
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("old-hash", password());
    assertEquals(TokenStatus.ACTIVE, status("blocked"));
    assertFalse(userRepository.findById(userId).orElseThrow().isEnabled());
  }

  @Test
  void dirtyStaleAccountCannotOverwriteAConcurrentDisableDuringLockFlush() throws Exception {
    CountDownLatch loaded = new CountDownLatch(1);
    CountDownLatch disabled = new CountDownLatch(1);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<Optional<UserEntity>> result =
          executorService.submit(() ->
              transaction.execute(tx -> {
                UserEntity stale = userRepository.findById(userId).orElseThrow();
                stale.setFirstName("Stale edit");
                loaded.countDown();
                try {
                  assertTrue(disabled.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                  Thread.currentThread().interrupt();
                  throw new IllegalStateException(failure);
                }
                return userRepository.lockVerificationUser(userId);
              }));

      assertTrue(loaded.await(10, TimeUnit.SECONDS));
      transaction.executeWithoutResult(
          tx -> userRepository.findById(userId).orElseThrow().setEnabled(false));
      disabled.countDown();
      ExecutionException failure =
          assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));

      assertInstanceOf(OptimisticLockingFailureException.class, failure.getCause());
    } finally {
      disabled.countDown();
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
    UserEntity storedUserEntity = userRepository.findById(userId).orElseThrow();

    assertFalse(storedUserEntity.isEnabled());
    assertEquals("Test", storedUserEntity.getFirstName());
    assertEquals("old-hash", storedUserEntity.getPassword());
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"USED", "INACTIVE", "EXPIRED"})
  void nonActiveTokensExistButCannotValidateOrChangePasswords(TokenStatus tokenStatus) {
    seed("closed", tokenStatus, LocalDateTime.now().plusDays(1));

    assertTrue(passwordResetTokenRepository.findByToken("closed").isPresent());
    assertEquals(
        tokenStatus == TokenStatus.EXPIRED
            ? PasswordResetService.LinkState.EXPIRED
            : PasswordResetService.LinkState.INVALID,
        service.inspectToken("closed"));
    assertFalse(service.resetPassword("closed", "Password!"));
    assertEquals(tokenStatus, status("closed"));
    assertEquals("old-hash", password());
    verifyNoInteractions(probes.encoder());
  }

  @Test
  void expiredTokensAreMarkedExpiredByBothEntrypointsWithoutChangingPassword() {
    seed("validate-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
    seed("reset-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));

    assertEquals(PasswordResetService.LinkState.EXPIRED, service.inspectToken("validate-expired"));
    assertEquals(PasswordResetService.LinkState.EXPIRED, service.inspectToken("validate-expired"));
    assertFalse(service.resetPassword("reset-expired", "Password!"));
    assertEquals(TokenStatus.EXPIRED, status("validate-expired"));
    assertEquals(TokenStatus.EXPIRED, status("reset-expired"));
    assertEquals("old-hash", password());
  }

  @Test
  void disabledAccountRejectsResetAndValidationInsideAnAmbientTransaction() {
    seed("disabled", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    transaction.executeWithoutResult(
        tx -> userRepository.findById(userId).orElseThrow().setEnabled(false));

    assertFalse(service.resetPassword("disabled", "Password!"));
    boolean valid =
        transaction.execute(
            tx -> service.inspectToken("disabled") == PasswordResetService.LinkState.VALID);

    assertFalse(valid);
    assertEquals(TokenStatus.ACTIVE, status("disabled"));
    assertEquals("old-hash", password());
  }

  @Test
  void validTokenValidationWorksWithoutAnAmbientPersistenceContext() {
    seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

    assertEquals(PasswordResetService.LinkState.VALID, service.inspectToken("valid"));
    boolean valid =
        transaction.execute(
            tx -> service.inspectToken("valid") == PasswordResetService.LinkState.VALID);

    assertTrue(valid);
    assertEquals(TokenStatus.ACTIVE, status("valid"));
  }

  @Test
  void newTokenInvalidatesOnlyOldActiveTokensAndCanBeUsed() {
    seed("active", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    seed("used", TokenStatus.USED, LocalDateTime.now().plusDays(1));
    seed("expired", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(1));
    String first = service.createToken(userRepository.findById(userId).orElseThrow());
    String second = service.createToken(userRepository.findById(userId).orElseThrow());

    assertNotEquals(first, second);
    assertEquals(TokenStatus.INACTIVE, status("active"));
    assertEquals(TokenStatus.INACTIVE, status(first));
    assertEquals(TokenStatus.ACTIVE, status(second));
    assertEquals(TokenStatus.USED, status("used"));
    assertEquals(TokenStatus.EXPIRED, status("expired"));
    assertFalse(service.resetPassword(first, "Password!"));
    assertTrue(service.resetPassword(second, "Password!"));
  }

  @Test
  void statusSetterCannotReactivateUsedTokensOrAllowASecondPasswordChange() {
    seed("reusable", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

    assertTrue(service.resetPassword("reusable", "Password!"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.setTokenStatus("reusable", TokenStatus.ACTIVE));
    assertFalse(service.resetPassword("reusable", "OtherPassword!"));
    assertEquals("hash-Password!", password());
    assertEquals(TokenStatus.USED, status("reusable"));
  }

  @Test
  void nullStatusIsRejectedBeforePersistenceWithoutChangingStoredStatus() {
    seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

    assertThrows(IllegalArgumentException.class, () -> service.setTokenStatus("valid", null));
    assertEquals(TokenStatus.ACTIVE, status("valid"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"encoder", "user-save", "token-save"})
  void runtimeFailuresRollBackBothPasswordAndTokenAndPermitASubsequentRetry(String stage) {
    seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("isolated " + stage);
    if (stage.equals("encoder")) {
      doThrow(failure).when(probes.encoder()).encode("Password!");
    } else if (stage.equals("user-save")) {
      doThrow(failure).when(probes.userRepository()).save(any());
    } else {
      doThrow(failure).when(probes.passwordResetTokenRepository()).save(any());
    }
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.resetPassword("valid", "Password!")));
    assertEquals("old-hash", password());
    assertEquals(TokenStatus.ACTIVE, status("valid"));
    reset(probes.encoder(), probes.userRepository(), probes.passwordResetTokenRepository());
    when(probes.encoder().encode("Password!")).thenReturn("retry-hash");

    assertTrue(service.resetPassword("valid", "Password!"));
    assertEquals("retry-hash", password());
    assertEquals(TokenStatus.USED, status("valid"));
  }

  @Test
  void invalidPasswordDoesNotConsumeEvenAnExpiredToken() {
    seed("expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));

    assertThrows(IllegalArgumentException.class, () -> service.resetPassword("expired", "weak"));
    assertEquals(TokenStatus.ACTIVE, status("expired"));
    assertEquals("old-hash", password());
    verifyNoInteractions(probes.encoder());
  }

  @Test
  void invalidDurationRollsBackInvalidationDuringCreation() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("invalid");
    UserEntity targetUser = userRepository.findById(userId).orElseThrow();

    assertThrows(DateTimeParseException.class, () -> service.createToken(targetUser));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "PT0S",
        "-PT1S",
        "PT1S",
        "PT59.999999999S",
        "invalid",
        "PT999999999999999999999S",
        "PT9223372036854775807S"
      })
  void invalidDurationsPreserveExistingTokensAndQueueInCreationAndMail(String duration) {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn(duration);
    UserEntity targetUser = userRepository.findById(userId).orElseThrow();

    assertThrows(RuntimeException.class, () -> service.createToken(targetUser));
    assertThrows(RuntimeException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION))
        .thenReturn(duration);

    assertThrows(RuntimeException.class, service::deleteExpiredTokens);
    assertEquals(1, passwordResetTokenRepository.count());
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1})
  void actualPersistenceUsesExclusiveExpiryForValidationAndReset(long offsetSeconds) {
    Instant instant = Instant.parse("2026-10-08T10:00:00Z");
    when(testClock.getZone()).thenReturn(ZoneId.of("Europe/Berlin"));
    when(testClock.instant()).thenReturn(instant);
    LocalDateTime expiry = LocalDateTime.now(testClock).plusSeconds(offsetSeconds);
    seed("validate-boundary", TokenStatus.ACTIVE, expiry);
    seed("reset-boundary", TokenStatus.ACTIVE, expiry);

    assertEquals(
        offsetSeconds > 0
            ? PasswordResetService.LinkState.VALID
            : PasswordResetService.LinkState.EXPIRED,
        service.inspectToken("validate-boundary"));
    assertEquals(offsetSeconds > 0, service.resetPassword("reset-boundary", "Password!"));
    assertEquals(
        offsetSeconds > 0 ? TokenStatus.ACTIVE : TokenStatus.EXPIRED, status("validate-boundary"));
    assertEquals(
        offsetSeconds > 0 ? TokenStatus.USED : TokenStatus.EXPIRED, status("reset-boundary"));
    assertEquals(offsetSeconds > 0 ? "hash-Password!" : "old-hash", password());
  }

  @Test
  void resetRechecksTimeAfterValidationWithoutConsumingAnExpiredToken() {
    Instant instant = Instant.parse("2026-10-08T10:00:00Z");
    when(testClock.instant()).thenReturn(instant);
    seed("later", TokenStatus.ACTIVE, LocalDateTime.now(testClock).plusSeconds(1));

    assertEquals(PasswordResetService.LinkState.VALID, service.inspectToken("later"));
    when(testClock.instant()).thenReturn(instant.plusSeconds(1));

    assertFalse(service.resetPassword("later", "Password!"));
    assertEquals(TokenStatus.EXPIRED, status("later"));
    assertEquals("old-hash", password());
    verifyNoInteractions(probes.encoder());
  }

  @Test
  void cleanupCommitsOnlyTokensStrictlyBeforeTheRetentionCutoffAndIsRepeatable() {
    LocalDateTime now = LocalDateTime.of(2026, 10, 8, 12, 0);
    LocalDateTime cutoff = now.minusDays(7);
    for (TokenStatus status : TokenStatus.values()) {
      seed("old-" + status, status, cutoff.minusSeconds(1));
    }
    seed("boundary", TokenStatus.USED, cutoff);
    seed("recent", TokenStatus.EXPIRED, cutoff.plusSeconds(1));
    seed("future", TokenStatus.ACTIVE, now.plusDays(1));
    Instant cutoffInstant = now.atZone(testClock.getZone()).toInstant();
    when(testClock.instant()).thenReturn(cutoffInstant);

    assertDoesNotThrow(service::deleteExpiredTokens);
    assertDoesNotThrow(service::deleteExpiredTokens);
    for (TokenStatus status : TokenStatus.values()) {
      assertFalse(passwordResetTokenRepository.findByToken("old-" + status).isPresent());
    }
    assertTrue(passwordResetTokenRepository.findByToken("boundary").isPresent());
    assertTrue(passwordResetTokenRepository.findByToken("recent").isPresent());
    assertTrue(passwordResetTokenRepository.findByToken("future").isPresent());
    assertEquals(3, passwordResetTokenRepository.count());
  }

  @Test
  void cleanupParticipatesInOuterRollback() {
    seed("old", TokenStatus.USED, LocalDateTime.now().minusDays(10));
    transaction.executeWithoutResult(tx -> {
      service.deleteExpiredTokens();

      assertEquals(0, passwordResetTokenRepository.count());
      tx.setRollbackOnly();
    });

    assertTrue(passwordResetTokenRepository.findByToken("old").isPresent());
  }

  @Test
  void checkedTemplateIoFailureRollsBackTokenRotationWithoutQueuingMailAndAllowsRetry()
      throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(Locale.FRENCH);
    VaadinSession.setCurrent(vaadinSession);
    try {
      assertThrows(IOException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    int active =
        transaction.execute(tx ->
            passwordResetTokenRepository
                .findAllByUserEntityAndStatus(
                    userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                .size());

    assertEquals(1, active);
    verify(probes.queue(), never()).addEmailToQueue(any(), any(), any(), any());
    assertEquals(0, emailQueueRepository.count());
    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.INACTIVE, status("old"));
    assertEquals(2, passwordResetTokenRepository.count());
    assertEquals(1, emailQueueRepository.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"read", "close"})
  void templateStreamIoFailuresRollbackTheRealRotationAndCloseTheStream(String stage) {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    IOException failure = new IOException("isolated " + stage);
    AtomicBoolean closed = new AtomicBoolean();
    InputStream template =
        new InputStream() {
          @Override
          public int read() throws IOException {
            if (stage.equals("read")) {
              throw failure;
            }
            return -1;
          }

          @Override
          public void close() throws IOException {
            closed.set(true);
            if (stage.equals("close")) {
              throw failure;
            }
          }
        };
    try (var _ =
        mockConstruction(
            ClassPathResource.class,
            (resource, context) ->
                when(resource.getInputStream())
                    .thenReturn(
                        context.arguments().getFirst().toString().startsWith("email/")
                            ? template
                            : new ByteArrayInputStream(new byte[] {1})))) {
      assertSame(
          failure,
          assertThrows(
              IOException.class, () -> service.sendPasswordResetEmail("test@example.com")));
    }
    assertTrue(closed.get());
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void checkedIoAfterActualQueueInsertionRollsBackQueueAndRotation() throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    IOException failure = new IOException("isolated failure after queue write");
    doAnswer(
            call -> {
              EmailQueueEntity mail = new EmailQueueEntity();
              mail.setUserEntity(call.getArgument(0));
              mail.setSubject(call.getArgument(1));
              mail.setBody(call.getArgument(2));
              mail.setEmailType(call.getArgument(3));
              mail.setStatus(EmailStatus.PENDING);
              emailQueueRepository.saveAndFlush(mail);

              assertEquals(1, emailQueueRepository.count());
              throw failure;
            })
        .when(probes.queue())
        .addEmailToQueue(any(), any(), any(), any());

    assertSame(
        failure,
        assertThrows(IOException.class, () -> service.sendPasswordResetEmail("test@example.com")));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
    reset(probes.queue());

    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    assertEquals(1, emailQueueRepository.count());
  }

  @Test
  void caughtCheckedIoMarksTheOuterTransactionRollbackOnly() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(Locale.FRENCH);
    VaadinSession.setCurrent(vaadinSession);
    try {
      assertThrows(
          UnexpectedRollbackException.class,
          () ->
              transaction.executeWithoutResult(tx -> {
                userRepository.findById(userId).orElseThrow().setFirstName("Must rollback");

                assertThrows(
                    IOException.class, () -> service.sendPasswordResetEmail("test@example.com"));
              }));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals("Test", userRepository.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void runtimeQueueFailureRollsBackTokenRotation() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("isolated queue failure");
    doThrow(failure).when(probes.queue()).addEmailToQueue(any(), any(), any(), any());

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.sendPasswordResetEmail("test@example.com")));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void successfulMailRequestCommitsOneNewTokenAndQueuesItsLinkWithoutSmtp(boolean german)
      throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(german ? Locale.GERMAN : Locale.ENGLISH);
    VaadinSession.setCurrent(vaadinSession);
    try {
      assertTrue(service.sendPasswordResetEmail("test@example.com"));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals(TokenStatus.INACTIVE, status("old"));
    String value =
        transaction.execute(tx ->
            passwordResetTokenRepository
                .findAllByUserEntityAndStatus(
                    userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                .getFirst()
                .getToken());
    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);

    verify(probes.queue())
        .addEmailToQueue(
            argThat(user -> userId.equals(user.getId())),
            eq("Reset password"),
            html.capture(),
            eq(EmailType.PASSWORD_RESET));
    assertTrue(html.getValue().contains("https://example.invalid/reset-password/" + value));
    assertTrue(html.getValue().contains("Reset Test Service"));
    assertFalse(html.getValue().contains("{{"));
    assertEquals(1, emailQueueRepository.count());
    assertEquals(2, passwordResetTokenRepository.count());
  }

  @RepeatedTest(3)
  void secondTransactionResolvingTheAccountBeforeFirstCommitCannotReuseTheToken() throws Exception {
    seed("race", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    CountDownLatch activeRead = new CountDownLatch(1);
    CountDownLatch firstCommitted = new CountDownLatch(1);
    AtomicReference<Thread> staleReader = new AtomicReference<Thread>();
    // Resolve the scalar account ID, then pause before acquiring its lock.
    doAnswer(
            call -> {
              Optional<Long> result = passwordResetTokenRepository.findUserIdByToken("race");
              if (Thread.currentThread() == staleReader.get()) {
                assertTrue(result.isPresent());
                activeRead.countDown();

                assertTrue(firstCommitted.await(10, TimeUnit.SECONDS));
              }
              return result;
            })
        .when(probes.passwordResetTokenRepository())
        .findUserIdByToken("race");
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> second =
          executorService.submit(() -> {
            staleReader.set(Thread.currentThread());
            return service.resetPassword("race", "SecondPassword!");
          });

      assertTrue(activeRead.await(10, TimeUnit.SECONDS));
      assertTrue(service.resetPassword("race", "FirstPassword!"));
      firstCommitted.countDown();

      assertFalse(
          second.get(10, TimeUnit.SECONDS),
          "The committed consumption must be observed after locking");
    } finally {
      firstCommitted.countDown();
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("hash-FirstPassword!", password());
    assertEquals(TokenStatus.USED, status("race"));
  }

  /** The first transaction holds the actual DB lock when the second attempts it. */
  private <T> List<T> overlap(Callable<T> first, Callable<T> second) throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch contender = new CountDownLatch(1);
    AtomicReference<Thread> owner = new AtomicReference<Thread>();
    doAnswer(
            call -> {
              if (Thread.currentThread() != owner.get()) {
                contender.countDown();
              }
              Optional<UserEntity> result = userRepository.lockVerificationUser(userId);
              if (Thread.currentThread() == owner.get()) {
                locked.countDown();

                assertTrue(contender.await(10, TimeUnit.SECONDS));
              }
              return result;
            })
        .when(probes.userRepository())
        .lockVerificationUser(userId);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      Future<T> a =
          executorService.submit(() -> {
            owner.set(Thread.currentThread());
            return first.call();
          });

      assertTrue(locked.await(10, TimeUnit.SECONDS));
      Future<T> b = executorService.submit(second);
      return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    } finally {
      contender.countDown();
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @RepeatedTest(3)
  void overlappingResetsConsumeTheTokenExactlyOnce() throws Exception {
    seed("overlap", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    List<Boolean> results =
        overlap(
            () -> service.resetPassword("overlap", "FirstPassword!"),
            () -> service.resetPassword("overlap", "SecondPassword!"));

    assertEquals(List.of(true, false), results);
    assertEquals("hash-FirstPassword!", password());
    assertEquals(TokenStatus.USED, status("overlap"));
    verify(probes.encoder(), times(1)).encode(any());
  }

  @RepeatedTest(3)
  void overlappingCreationLeavesOnlyTheLastTokenActive() throws Exception {
    UserEntity detached = userRepository.findById(userId).orElseThrow();
    List<String> results =
        overlap(() -> service.createToken(detached), () -> service.createToken(detached));

    assertNotEquals(results.getFirst(), results.getLast());
    assertEquals(TokenStatus.INACTIVE, status(results.getFirst()));
    assertEquals(TokenStatus.ACTIVE, status(results.getLast()));
    int active =
        transaction.execute(tx ->
            passwordResetTokenRepository
                .findAllByUserEntityAndStatus(
                    userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                .size());

    assertEquals(1, active);
  }

  @RepeatedTest(3)
  void overlappingMailRequestsPersistOneMailWhoseTokenRemainsActive() throws Exception {
    List<Boolean> results =
        overlap(
            () -> service.sendPasswordResetEmail("test@example.com"),
            () -> service.sendPasswordResetEmail("test@example.com"));

    assertEquals(List.of(true, true), results);
    assertEquals(1, emailQueueRepository.count());
    assertEquals(1, passwordResetTokenRepository.count());
    String active =
        transaction.execute(tx ->
            passwordResetTokenRepository
                .findAllByUserEntityAndStatus(
                    userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                .getFirst()
                .getToken());

    assertTrue(
        emailQueueRepository.findAll().getFirst().getBody().contains("/reset-password/" + active));
    assertEquals(PasswordResetService.LinkState.VALID, service.inspectToken(active));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void overlappingDirectCreationAndMailRequestKeepTheQueuedLinkActive(boolean mailFirst)
      throws Exception {
    UserEntity detached = userRepository.findById(userId).orElseThrow();
    if (mailFirst) {
      assertEquals(
          List.of(true, false),
          overlap(
              () -> service.sendPasswordResetEmail("test@example.com"),
              () -> {
                assertThrows(IllegalStateException.class, () -> service.createToken(detached));
                return false;
              }));
      assertEquals(1, passwordResetTokenRepository.count());
    } else {
      List<String> results =
          overlap(
              () -> service.createToken(detached),
              () -> {
                assertTrue(service.sendPasswordResetEmail("test@example.com"));
                return "mail-queued";
              });

      assertEquals(TokenStatus.INACTIVE, status(results.getFirst()));
      assertEquals(2, passwordResetTokenRepository.count());
    }
    assertEquals(1, emailQueueRepository.count());
    List<PasswordResetTokenEntity> active =
        transaction.execute(tx ->
            passwordResetTokenRepository.findAllByUserEntityAndStatus(
                userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE));

    assertEquals(1, active.size());
    assertTrue(
        emailQueueRepository
            .findAll()
            .getFirst()
            .getBody()
            .contains("/reset-password/" + active.getFirst().getToken()));
  }

  @Test
  void detachedEnabledUserCannotCreateTokensAfterAccountWasDisabled() {
    UserEntity detached = userRepository.findById(userId).orElseThrow();
    transaction.executeWithoutResult(
        tx -> userRepository.findById(userId).orElseThrow().setEnabled(false));

    assertThrows(IllegalStateException.class, () -> service.createToken(detached));
    assertEquals(0, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void mailQueuePersistenceFailureRollsBackRotationAndTheQueuedRow() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    // Inject an actual NOT NULL constraint violation in the real queue save,
    // rather than replacing the queue with an exception-only mock.
    when(probes.config().getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    doAnswer(
            call -> {
              UserEntity testUserEntity = (UserEntity) call.getArgument(0);
              EmailQueueEntity broken = new EmailQueueEntity();
              broken.setUserEntity(testUserEntity);
              broken.setEmailType(EmailType.PASSWORD_RESET);
              broken.setStatus(EmailStatus.PENDING);
              emailQueueRepository.saveAndFlush(broken);
              return null;
            })
        .when(probes.queue())
        .addEmailToQueue(any(), any(), any(), any());

    assertThrows(
        DataIntegrityViolationException.class,
        () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @ParameterizedTest
  @EnumSource(
      value = EmailStatus.class,
      names = {"PENDING", "IN_PROGRESS"})
  void directRotationCannotInvalidateATokenInAnOpenMail(EmailStatus mailStatus) throws IOException {
    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    transaction.executeWithoutResult(
        tx -> emailQueueRepository.findAll().getFirst().setStatus(mailStatus));
    String active =
        transaction.execute(tx ->
            passwordResetTokenRepository
                .findAllByUserEntityAndStatus(
                    userRepository.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                .getFirst()
                .getToken());
    UserEntity targetUser = userRepository.findById(userId).orElseThrow();

    assertThrows(IllegalStateException.class, () -> service.createToken(targetUser));
    assertEquals(TokenStatus.ACTIVE, status(active));
    assertEquals(1, passwordResetTokenRepository.count());
    assertEquals(1, emailQueueRepository.count());
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"USED", "INACTIVE", "EXPIRED"})
  void terminalTokensRemainIrreversibleAndRepeatedSameStatusIsIdempotent(TokenStatus terminal) {
    seed("closed", terminal, LocalDateTime.now().plusDays(1));

    assertThrows(
        IllegalArgumentException.class, () -> service.setTokenStatus("closed", TokenStatus.ACTIVE));
    for (TokenStatus target :
        List.of(TokenStatus.USED, TokenStatus.INACTIVE, TokenStatus.EXPIRED)) {
      if (target == terminal) {
        service.setTokenStatus("closed", target);
      } else {
        assertThrows(IllegalStateException.class, () -> service.setTokenStatus("closed", target));
      }
    }
    assertEquals(terminal, status("closed"));
  }

  @Test
  void staleManagedTokenCannotBeReusedAfterAnotherTransactionCommitsConsumption() throws Exception {
    seed("stale", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    CountDownLatch read = new CountDownLatch(1);
    CountDownLatch committed = new CountDownLatch(1);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> stale =
          executorService.submit(() ->
              transaction.execute(tx -> {
                PasswordResetTokenEntity managed =
                    passwordResetTokenRepository.findByToken("stale").orElseThrow();

                assertEquals(TokenStatus.ACTIVE, managed.getStatus());
                read.countDown();
                try {
                  assertTrue(committed.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                  Thread.currentThread().interrupt();
                  throw new IllegalStateException(failure);
                }
                return service.resetPassword("stale", "SecondPassword!");
              }));

      assertTrue(read.await(10, TimeUnit.SECONDS));
      assertTrue(service.resetPassword("stale", "FirstPassword!"));
      committed.countDown();

      assertFalse(stale.get(10, TimeUnit.SECONDS));
    } finally {
      committed.countDown();
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("hash-FirstPassword!", password());
  }

  record Probes(
      PasswordResetTokenRepository passwordResetTokenRepository,
      UserRepository userRepository,
      ConfigService config,
      PasswordEncoder encoder,
      EmailQueueService queue) {}

  @Test
  void maintenancePreservesResetTokenPasswordAndQueueAndAllowsResume() {
    SecurityContextHolder.clearContext();
    String token = service.createToken(userRepository.findById(userId).orElseThrow());
    String password = userRepository.findById(userId).orElseThrow().getPassword();
    long tokenCount = passwordResetTokenRepository.count();
    long mailCount = emailQueueRepository.count();
    clearInvocations(probes.queue(), probes.encoder());
    when(probes.config().getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);

    assertThrows(
        MaintenanceAccessException.class, () -> service.resetPassword(token, "NewPassword!"));
    assertThrows(MaintenanceAccessException.class, () -> service.inspectToken(token));
    assertThrows(
        MaintenanceAccessException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(password, userRepository.findById(userId).orElseThrow().getPassword());
    assertEquals(
        TokenStatus.ACTIVE,
        passwordResetTokenRepository.findByToken(token).orElseThrow().getStatus());
    assertEquals(tokenCount, passwordResetTokenRepository.count());
    assertEquals(mailCount, emailQueueRepository.count());
    verifyNoInteractions(probes.queue(), probes.encoder());
    service.deleteExpiredTokens();
    when(probes.config().getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    when(probes.encoder().encode("NewPassword!")).thenReturn("updated-hash");

    assertTrue(service.resetPassword(token, "NewPassword!"));
    assertEquals(
        TokenStatus.USED,
        passwordResetTokenRepository.findByToken(token).orElseThrow().getStatus());
  }

  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          "jdbc:h2:mem:password-reset-"
              + UUID.randomUUID()
              + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
          "sa",
          "");
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan("de.derpeterson.app.model");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }

    @Bean
    TransactionTemplate transactionTemplate(JpaTransactionManager manager) {
      return new TransactionTemplate(manager);
    }

    @Bean
    ConfigService configService() {
      return mock(ConfigService.class);
    }

    @Bean
    EmailQueueService emailQueueService(
        ConfigService config,
        EmailQueueRepository emailQueueRepository,
        UserRepository userRepository,
        JpaTransactionManager manager) {
      return new EmailQueueService(
          config,
          emailQueueRepository,
          mock(EmailService.class),
          userRepository,
          manager,
          mock(AdminNotificationAttemptRepository.class),
          QueueTestWorkers.inline());
    }

    @Bean
    Probes probes(
        PasswordResetTokenRepository passwordResetTokenRepository,
        UserRepository userRepository,
        ConfigService config,
        EmailQueueService queue) {
      return new Probes(
          mock(PasswordResetTokenRepository.class, delegatesTo(passwordResetTokenRepository)),
          mock(UserRepository.class, delegatesTo(userRepository)),
          config,
          mock(PasswordEncoder.class),
          mock(EmailQueueService.class, delegatesTo(queue)));
    }

    @Bean
    UserNotificationService userNotifications(
        de.derpeterson.app.repository.UserNotificationRepository messages,
        de.derpeterson.app.repository.UserNotificationRecipientRepository recipients,
        UserRepository userRepository,
        ConfigService config) {
      return new UserNotificationService(
          new UserNotificationPolicy(config),
          messages,
          recipients,
          userRepository,
          mock(de.derpeterson.app.security.SecurityService.class),
          config,
          new de.derpeterson.app.websocket.UserNotificationBroadcaster(),
          mock(TwoFactorAuthorization.class));
    }

    @Bean
    Clock clock() {
      return mock(Clock.class);
    }

    @Bean
    PasswordResetService passwordResetService(
        Probes probes, Clock testClock, UserNotificationService userNotifications) {
      MessageProperties messages = mock(MessageProperties.class);
      when(messages.getEmailResetPasswordSubject()).thenReturn("Reset password");
      return new PasswordResetService(
          messages,
          probes.passwordResetTokenRepository(),
          probes.userRepository(),
          probes.config(),
          probes.encoder(),
          probes.queue(),
          mock(LoginAttemptService.class),
          userNotifications,
          testClock);
    }
  }
}
