package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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

  @Autowired private PasswordResetTokenRepository tokens;
  @Autowired private UserRepository users;
  @Autowired private TransactionTemplate transaction;
  @Autowired private Probes probes;
  @Autowired private EmailQueueRepository mails;
  @Autowired private Clock clock;
  private Long userId;

  @BeforeEach
  void setup() {
    reset(clock);
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    when(clock.instant()).thenAnswer(call -> Instant.now());
    reset(probes.tokens(), probes.users(), probes.config(), probes.encoder(), probes.queue());
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
    transaction.executeWithoutResult(
        tx -> {
          userRecipientsRepository.deleteAll();
          userNotificationsRepository.deleteAll();
          mails.deleteAll();
          tokens.deleteAll();
          users.deleteAll();
          users.flush();
          userId =
              users
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
    transaction.executeWithoutResult(
        tx ->
            tokens.saveAndFlush(
                new PasswordResetTokenEntity(
                    null, value, users.findById(userId).orElseThrow(), expiry, status)));
  }

  private TokenStatus status(String value) {
    return transaction.execute(tx -> tokens.findByToken(value).orElseThrow().getStatus());
  }

  private String password() {
    return users.findById(userId).orElseThrow().getPassword();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"missing"})
  void absentTokenLookupsAndEntrypointsRemainEmpty(String token) {
    assertTrue(tokens.findByToken(token).isEmpty());
    assertTrue(tokens.findByTokenAndStatus(token, TokenStatus.ACTIVE).isEmpty());
    assertFalse(service.validateToken(token));
    assertFalse(service.resetPassword(token, "Password!"));
    assertFalse(tokens.findByToken(token).isPresent());
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
    assertTrue(tokens.findByToken("valid").isPresent());
    assertFalse(service.validateToken("valid"));
    assertFalse(service.resetPassword("valid", "OtherPassword!"));
    assertEquals("hash-Password!", password());
    verify(probes.encoder(), times(1)).encode(any());
  }

  @Test
  void messagePersistenceFailureRollsBackPasswordAndResetConsumption() {
    seed("message-failure", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var target = org.springframework.test.util.AopTestUtils.getTargetObject(service);
    var failure = mock(UserNotificationService.class);
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
    var created = new AtomicReference<String>();
    transaction.executeWithoutResult(
        tx -> {
          assertTrue(service.resetPassword("outer", "Password!"));
          var managed = users.findById(userId).orElseThrow();
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
    assertEquals("Updated", users.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.USED, status("outer"));
    assertFalse(service.resetPassword("outer", "OtherPassword!"));
    if (next.equals("create")) {
      assertEquals(TokenStatus.ACTIVE, status(created.get()));
      assertEquals(2, tokens.count());
    } else {
      assertEquals(1, tokens.count());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "invalidate"})
  void outerRollbackRestoresPasswordAndTokensAfterRepeatedAccountLock(String next) {
    seed("outer", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    seed("other", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var created = new AtomicReference<String>();
    transaction.executeWithoutResult(
        tx -> {
          assertTrue(service.resetPassword("outer", "Password!"));
          var managed = users.findById(userId).orElseThrow();
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
    assertEquals("Test", users.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.ACTIVE, status("outer"));
    assertEquals(TokenStatus.ACTIVE, status("other"));
    assertEquals(2, tokens.count());
    if (next.equals("create")) {
      assertFalse(tokens.findByToken(created.get()).isPresent());
    }
    assertTrue(service.resetPassword("outer", "RetryPassword!"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void realMailQueueRelockPreservesResetAndParticipatesInOuterRollback(boolean rollback) {
    seed("outer-mail", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    transaction.executeWithoutResult(
        tx -> {
          assertTrue(service.resetPassword("outer-mail", "Password!"));
          try {
            assertTrue(service.sendPasswordResetEmail("test@example.com"));
          } catch (IOException failure) {
            throw new IllegalStateException(failure);
          }
          assertEquals("hash-Password!", users.findById(userId).orElseThrow().getPassword());
          assertEquals(1, mails.count());
          if (rollback) {
            tx.setRollbackOnly();
          }
        });
    assertEquals(rollback ? "old-hash" : "hash-Password!", password());
    assertEquals(rollback ? TokenStatus.ACTIVE : TokenStatus.USED, status("outer-mail"));
    assertEquals(rollback ? 0 : 1, mails.count());
    assertEquals(rollback ? 1 : 2, tokens.count());
    if (!rollback) {
      var active =
          transaction.execute(
              tx ->
                  tokens
                      .findAllByUserEntityAndStatus(
                          users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                      .getFirst()
                      .getToken());
      assertTrue(mails.findAll().getFirst().getBody().contains("/reset-password/" + active));
    }
  }

  @Test
  void firstAccountLockPreservesOwnPendingChangesWithoutCommittingThem() {
    transaction.executeWithoutResult(
        tx -> {
          var managed = users.findById(userId).orElseThrow();
          managed.setFirstName("Local");
          assertSame(managed, users.lockVerificationUser(userId).orElseThrow());
          assertEquals("Local", managed.getFirstName());
          tx.setRollbackOnly();
        });
    assertEquals("Test", users.findById(userId).orElseThrow().getFirstName());
  }

  @Test
  void firstAccountLockRefreshesAnUnchangedStaleEnabledAccountBeforeReset() throws Exception {
    seed("blocked", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var loaded = new CountDownLatch(1);
    var disabled = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var result =
          executor.submit(
              () ->
                  transaction.execute(
                      tx -> {
                        var stale = users.findById(userId).orElseThrow();
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
          tx -> users.findById(userId).orElseThrow().setEnabled(false));
      disabled.countDown();
      assertFalse(result.get(10, TimeUnit.SECONDS));
    } finally {
      disabled.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("old-hash", password());
    assertEquals(TokenStatus.ACTIVE, status("blocked"));
    assertFalse(users.findById(userId).orElseThrow().isEnabled());
  }

  @Test
  void dirtyStaleAccountCannotOverwriteAConcurrentDisableDuringLockFlush() throws Exception {
    var loaded = new CountDownLatch(1);
    var disabled = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var result =
          executor.submit(
              () ->
                  transaction.execute(
                      tx -> {
                        var stale = users.findById(userId).orElseThrow();
                        stale.setFirstName("Stale edit");
                        loaded.countDown();
                        try {
                          assertTrue(disabled.await(10, TimeUnit.SECONDS));
                        } catch (InterruptedException failure) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(failure);
                        }
                        return users.lockVerificationUser(userId);
                      }));
      assertTrue(loaded.await(10, TimeUnit.SECONDS));
      transaction.executeWithoutResult(
          tx -> users.findById(userId).orElseThrow().setEnabled(false));
      disabled.countDown();
      var failure = assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
      assertInstanceOf(OptimisticLockingFailureException.class, failure.getCause());
    } finally {
      disabled.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    var stored = users.findById(userId).orElseThrow();
    assertFalse(stored.isEnabled());
    assertEquals("Test", stored.getFirstName());
    assertEquals("old-hash", stored.getPassword());
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"USED", "INACTIVE", "EXPIRED"})
  void nonActiveTokensExistButCannotValidateOrChangePasswords(TokenStatus tokenStatus) {
    seed("closed", tokenStatus, LocalDateTime.now().plusDays(1));
    assertTrue(tokens.findByToken("closed").isPresent());
    assertFalse(service.validateToken("closed"));
    assertFalse(service.resetPassword("closed", "Password!"));
    assertEquals(tokenStatus, status("closed"));
    assertEquals("old-hash", password());
    verifyNoInteractions(probes.encoder());
  }

  @Test
  void expiredTokensAreMarkedExpiredByBothEntrypointsWithoutChangingPassword() {
    seed("validate-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
    seed("reset-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
    assertFalse(service.validateToken("validate-expired"));
    assertFalse(service.resetPassword("reset-expired", "Password!"));
    assertEquals(TokenStatus.EXPIRED, status("validate-expired"));
    assertEquals(TokenStatus.EXPIRED, status("reset-expired"));
    assertEquals("old-hash", password());
  }

  @Test
  void disabledAccountRejectsResetAndValidationInsideAnAmbientTransaction() {
    seed("disabled", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    transaction.executeWithoutResult(tx -> users.findById(userId).orElseThrow().setEnabled(false));
    assertFalse(service.resetPassword("disabled", "Password!"));
    boolean valid = transaction.execute(tx -> service.validateToken("disabled"));
    assertFalse(valid);
    assertEquals(TokenStatus.ACTIVE, status("disabled"));
    assertEquals("old-hash", password());
  }

  @Test
  void validTokenValidationWorksWithoutAnAmbientPersistenceContext() {
    seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    assertTrue(service.validateToken("valid"));
    boolean valid = transaction.execute(tx -> service.validateToken("valid"));
    assertTrue(valid);
    assertEquals(TokenStatus.ACTIVE, status("valid"));
  }

  @Test
  void newTokenInvalidatesOnlyOldActiveTokensAndCanBeUsed() {
    seed("active", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    seed("used", TokenStatus.USED, LocalDateTime.now().plusDays(1));
    seed("expired", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(1));
    String first = service.createToken(users.findById(userId).orElseThrow());
    String second = service.createToken(users.findById(userId).orElseThrow());
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
    var failure = new DataAccessResourceFailureException("isolated " + stage);
    if (stage.equals("encoder")) {
      doThrow(failure).when(probes.encoder()).encode("Password!");
    } else if (stage.equals("user-save")) {
      doThrow(failure).when(probes.users()).save(any());
    } else {
      doThrow(failure).when(probes.tokens()).save(any());
    }
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.resetPassword("valid", "Password!")));
    assertEquals("old-hash", password());
    assertEquals(TokenStatus.ACTIVE, status("valid"));
    reset(probes.encoder(), probes.users(), probes.tokens());
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
    assertThrows(
        DateTimeParseException.class,
        () -> service.createToken(users.findById(userId).orElseThrow()));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
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
    assertThrows(
        RuntimeException.class, () -> service.createToken(users.findById(userId).orElseThrow()));
    assertThrows(RuntimeException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
    assertEquals(0, mails.count());
    when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION))
        .thenReturn(duration);
    assertThrows(RuntimeException.class, service::deleteExpiredTokens);
    assertEquals(1, tokens.count());
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1})
  void actualPersistenceUsesExclusiveExpiryForValidationAndReset(long offsetSeconds) {
    var instant = Instant.parse("2026-10-08T10:00:00Z");
    when(clock.getZone()).thenReturn(ZoneId.of("Europe/Berlin"));
    when(clock.instant()).thenReturn(instant);
    var expiry = LocalDateTime.now(clock).plusSeconds(offsetSeconds);
    seed("validate-boundary", TokenStatus.ACTIVE, expiry);
    seed("reset-boundary", TokenStatus.ACTIVE, expiry);
    assertEquals(offsetSeconds > 0, service.validateToken("validate-boundary"));
    assertEquals(offsetSeconds > 0, service.resetPassword("reset-boundary", "Password!"));
    assertEquals(
        offsetSeconds > 0 ? TokenStatus.ACTIVE : TokenStatus.EXPIRED, status("validate-boundary"));
    assertEquals(
        offsetSeconds > 0 ? TokenStatus.USED : TokenStatus.EXPIRED, status("reset-boundary"));
    assertEquals(offsetSeconds > 0 ? "hash-Password!" : "old-hash", password());
  }

  @Test
  void resetRechecksTimeAfterValidationWithoutConsumingAnExpiredToken() {
    var instant = Instant.parse("2026-10-08T10:00:00Z");
    when(clock.instant()).thenReturn(instant);
    seed("later", TokenStatus.ACTIVE, LocalDateTime.now(clock).plusSeconds(1));
    assertTrue(service.validateToken("later"));
    when(clock.instant()).thenReturn(instant.plusSeconds(1));
    assertFalse(service.resetPassword("later", "Password!"));
    assertEquals(TokenStatus.EXPIRED, status("later"));
    assertEquals("old-hash", password());
    verifyNoInteractions(probes.encoder());
  }

  @Test
  void cleanupCommitsOnlyTokensStrictlyBeforeTheRetentionCutoffAndIsRepeatable() {
    var now = LocalDateTime.of(2026, 10, 8, 12, 0);
    var cutoff = now.minusDays(7);
    for (TokenStatus status : TokenStatus.values()) {
      seed("old-" + status, status, cutoff.minusSeconds(1));
    }
    seed("boundary", TokenStatus.USED, cutoff);
    seed("recent", TokenStatus.EXPIRED, cutoff.plusSeconds(1));
    seed("future", TokenStatus.ACTIVE, now.plusDays(1));
    var cutoffInstant = now.atZone(clock.getZone()).toInstant();
    when(clock.instant()).thenReturn(cutoffInstant);
    assertDoesNotThrow(service::deleteExpiredTokens);
    assertDoesNotThrow(service::deleteExpiredTokens);
    for (TokenStatus status : TokenStatus.values()) {
      assertFalse(tokens.findByToken("old-" + status).isPresent());
    }
    assertTrue(tokens.findByToken("boundary").isPresent());
    assertTrue(tokens.findByToken("recent").isPresent());
    assertTrue(tokens.findByToken("future").isPresent());
    assertEquals(3, tokens.count());
  }

  @Test
  void cleanupParticipatesInOuterRollback() {
    seed("old", TokenStatus.USED, LocalDateTime.now().minusDays(10));
    transaction.executeWithoutResult(
        tx -> {
          service.deleteExpiredTokens();
          assertEquals(0, tokens.count());
          tx.setRollbackOnly();
        });
    assertTrue(tokens.findByToken("old").isPresent());
  }

  @Test
  void checkedTemplateIoFailureRollsBackTokenRotationWithoutQueuingMailAndAllowsRetry()
      throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.FRENCH);
    VaadinSession.setCurrent(session);
    try {
      assertThrows(IOException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
    int active =
        transaction.execute(
            tx ->
                tokens
                    .findAllByUserEntityAndStatus(
                        users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                    .size());
    assertEquals(1, active);
    verify(probes.queue(), never()).addEmailToQueue(any(), any(), any(), any());
    assertEquals(0, mails.count());
    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.INACTIVE, status("old"));
    assertEquals(2, tokens.count());
    assertEquals(1, mails.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"read", "close"})
  void templateStreamIoFailuresRollbackTheRealRotationAndCloseTheStream(String stage) {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var failure = new IOException("isolated " + stage);
    var closed = new AtomicBoolean();
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
    try (var resources =
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
    assertEquals(1, tokens.count());
    assertEquals(0, mails.count());
  }

  @Test
  void checkedIoAfterActualQueueInsertionRollsBackQueueAndRotation() throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var failure = new IOException("isolated failure after queue write");
    doAnswer(
            call -> {
              var mail = new EmailQueueEntity();
              mail.setUserEntity(call.getArgument(0));
              mail.setSubject(call.getArgument(1));
              mail.setBody(call.getArgument(2));
              mail.setEmailType(call.getArgument(3));
              mail.setStatus(EmailStatus.PENDING);
              mails.saveAndFlush(mail);
              assertEquals(1, mails.count());
              throw failure;
            })
        .when(probes.queue())
        .addEmailToQueue(any(), any(), any(), any());
    assertSame(
        failure,
        assertThrows(IOException.class, () -> service.sendPasswordResetEmail("test@example.com")));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
    assertEquals(0, mails.count());
    reset(probes.queue());
    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    assertEquals(1, mails.count());
  }

  @Test
  void caughtCheckedIoMarksTheOuterTransactionRollbackOnly() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.FRENCH);
    VaadinSession.setCurrent(session);
    try {
      assertThrows(
          UnexpectedRollbackException.class,
          () ->
              transaction.executeWithoutResult(
                  tx -> {
                    users.findById(userId).orElseThrow().setFirstName("Must rollback");
                    assertThrows(
                        IOException.class,
                        () -> service.sendPasswordResetEmail("test@example.com"));
                  }));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals("Test", users.findById(userId).orElseThrow().getFirstName());
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
    assertEquals(0, mails.count());
  }

  @Test
  void runtimeQueueFailureRollsBackTokenRotation() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var failure = new DataAccessResourceFailureException("isolated queue failure");
    doThrow(failure).when(probes.queue()).addEmailToQueue(any(), any(), any(), any());
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.sendPasswordResetEmail("test@example.com")));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void successfulMailRequestCommitsOneNewTokenAndQueuesItsLinkWithoutSmtp(boolean german)
      throws IOException {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(german ? Locale.GERMAN : Locale.ENGLISH);
    VaadinSession.setCurrent(session);
    try {
      assertTrue(service.sendPasswordResetEmail("test@example.com"));
    } finally {
      VaadinSession.setCurrent(null);
    }
    assertEquals(TokenStatus.INACTIVE, status("old"));
    var value =
        transaction.execute(
            tx ->
                tokens
                    .findAllByUserEntityAndStatus(
                        users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                    .getFirst()
                    .getToken());
    var html = ArgumentCaptor.forClass(String.class);
    verify(probes.queue())
        .addEmailToQueue(
            argThat(user -> userId.equals(user.getId())),
            eq("Reset password"),
            html.capture(),
            eq(EmailType.PASSWORD_RESET));
    assertTrue(html.getValue().contains("https://example.invalid/reset-password/" + value));
    assertTrue(html.getValue().contains("Reset Test Service"));
    assertFalse(html.getValue().contains("{{"));
    assertEquals(1, mails.count());
    assertEquals(2, tokens.count());
  }

  @RepeatedTest(3)
  void secondTransactionResolvingTheAccountBeforeFirstCommitCannotReuseTheToken() throws Exception {
    seed("race", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var activeRead = new CountDownLatch(1);
    var firstCommitted = new CountDownLatch(1);
    var staleReader = new AtomicReference<Thread>();
    // Resolve the scalar account ID, then pause before acquiring its lock.
    doAnswer(
            call -> {
              var result = tokens.findUserIdByToken("race");
              if (Thread.currentThread() == staleReader.get()) {
                assertTrue(result.isPresent());
                activeRead.countDown();
                assertTrue(firstCommitted.await(10, TimeUnit.SECONDS));
              }
              return result;
            })
        .when(probes.tokens())
        .findUserIdByToken("race");
    var executor = Executors.newSingleThreadExecutor();
    try {
      var second =
          executor.submit(
              () -> {
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
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("hash-FirstPassword!", password());
    assertEquals(TokenStatus.USED, status("race"));
  }

  /** The first transaction holds the actual DB lock when the second attempts it. */
  private <T> List<T> overlap(Callable<T> first, Callable<T> second) throws Exception {
    var locked = new CountDownLatch(1);
    var contender = new CountDownLatch(1);
    var owner = new AtomicReference<Thread>();
    doAnswer(
            call -> {
              if (Thread.currentThread() != owner.get()) {
                contender.countDown();
              }
              var result = users.lockVerificationUser(userId);
              if (Thread.currentThread() == owner.get()) {
                locked.countDown();
                assertTrue(contender.await(10, TimeUnit.SECONDS));
              }
              return result;
            })
        .when(probes.users())
        .lockVerificationUser(userId);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () -> {
                owner.set(Thread.currentThread());
                return first.call();
              });
      assertTrue(locked.await(10, TimeUnit.SECONDS));
      var b = executor.submit(second);
      return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    } finally {
      contender.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @RepeatedTest(3)
  void overlappingResetsConsumeTheTokenExactlyOnce() throws Exception {
    seed("overlap", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    var results =
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
    var detached = users.findById(userId).orElseThrow();
    var results = overlap(() -> service.createToken(detached), () -> service.createToken(detached));
    assertNotEquals(results.getFirst(), results.getLast());
    assertEquals(TokenStatus.INACTIVE, status(results.getFirst()));
    assertEquals(TokenStatus.ACTIVE, status(results.getLast()));
    int active =
        transaction.execute(
            tx ->
                tokens
                    .findAllByUserEntityAndStatus(
                        users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                    .size());
    assertEquals(1, active);
  }

  @RepeatedTest(3)
  void overlappingMailRequestsPersistOneMailWhoseTokenRemainsActive() throws Exception {
    var results =
        overlap(
            () -> service.sendPasswordResetEmail("test@example.com"),
            () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(List.of(true, true), results);
    assertEquals(1, mails.count());
    assertEquals(1, tokens.count());
    var active =
        transaction.execute(
            tx ->
                tokens
                    .findAllByUserEntityAndStatus(
                        users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                    .getFirst()
                    .getToken());
    assertTrue(mails.findAll().getFirst().getBody().contains("/reset-password/" + active));
    assertTrue(service.validateToken(active));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void overlappingDirectCreationAndMailRequestKeepTheQueuedLinkActive(boolean mailFirst)
      throws Exception {
    var detached = users.findById(userId).orElseThrow();
    if (mailFirst) {
      assertEquals(
          List.of(true, false),
          overlap(
              () -> service.sendPasswordResetEmail("test@example.com"),
              () -> {
                assertThrows(IllegalStateException.class, () -> service.createToken(detached));
                return false;
              }));
      assertEquals(1, tokens.count());
    } else {
      var results =
          overlap(
              () -> service.createToken(detached),
              () -> {
                assertTrue(service.sendPasswordResetEmail("test@example.com"));
                return "mail-queued";
              });
      assertEquals(TokenStatus.INACTIVE, status(results.getFirst()));
      assertEquals(2, tokens.count());
    }
    assertEquals(1, mails.count());
    var active =
        transaction.execute(
            tx ->
                tokens.findAllByUserEntityAndStatus(
                    users.findById(userId).orElseThrow(), TokenStatus.ACTIVE));
    assertEquals(1, active.size());
    assertTrue(
        mails
            .findAll()
            .getFirst()
            .getBody()
            .contains("/reset-password/" + active.getFirst().getToken()));
  }

  @Test
  void detachedEnabledUserCannotCreateTokensAfterAccountWasDisabled() {
    var detached = users.findById(userId).orElseThrow();
    transaction.executeWithoutResult(tx -> users.findById(userId).orElseThrow().setEnabled(false));
    assertThrows(IllegalStateException.class, () -> service.createToken(detached));
    assertEquals(0, tokens.count());
    assertEquals(0, mails.count());
  }

  @Test
  void mailQueuePersistenceFailureRollsBackRotationAndTheQueuedRow() {
    seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    // Inject an actual NOT NULL constraint violation in the real queue save,
    // rather than replacing the queue with an exception-only mock.
    when(probes.config().getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    doAnswer(
            call -> {
              var user = (UserEntity) call.getArgument(0);
              var broken = new EmailQueueEntity();
              broken.setUserEntity(user);
              broken.setEmailType(EmailType.PASSWORD_RESET);
              broken.setStatus(EmailStatus.PENDING);
              mails.saveAndFlush(broken);
              return null;
            })
        .when(probes.queue())
        .addEmailToQueue(any(), any(), any(), any());
    assertThrows(
        DataIntegrityViolationException.class,
        () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(TokenStatus.ACTIVE, status("old"));
    assertEquals(1, tokens.count());
    assertEquals(0, mails.count());
  }

  @ParameterizedTest
  @EnumSource(
      value = EmailStatus.class,
      names = {"PENDING", "IN_PROGRESS"})
  void directRotationCannotInvalidateATokenInAnOpenMail(EmailStatus mailStatus) throws IOException {
    assertTrue(service.sendPasswordResetEmail("test@example.com"));
    transaction.executeWithoutResult(tx -> mails.findAll().getFirst().setStatus(mailStatus));
    var active =
        transaction.execute(
            tx ->
                tokens
                    .findAllByUserEntityAndStatus(
                        users.findById(userId).orElseThrow(), TokenStatus.ACTIVE)
                    .getFirst()
                    .getToken());
    assertThrows(
        IllegalStateException.class,
        () -> service.createToken(users.findById(userId).orElseThrow()));
    assertEquals(TokenStatus.ACTIVE, status(active));
    assertEquals(1, tokens.count());
    assertEquals(1, mails.count());
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"USED", "INACTIVE", "EXPIRED"})
  void terminalTokensRemainIrreversibleAndRepeatedSameStatusIsIdempotent(TokenStatus terminal) {
    seed("closed", terminal, LocalDateTime.now().plusDays(1));
    assertThrows(
        IllegalArgumentException.class, () -> service.setTokenStatus("closed", TokenStatus.ACTIVE));
    for (var target : List.of(TokenStatus.USED, TokenStatus.INACTIVE, TokenStatus.EXPIRED)) {
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
    var read = new CountDownLatch(1);
    var committed = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var stale =
          executor.submit(
              () ->
                  transaction.execute(
                      tx -> {
                        var managed = tokens.findByToken("stale").orElseThrow();
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
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    assertEquals("hash-FirstPassword!", password());
  }

  record Probes(
      PasswordResetTokenRepository tokens,
      UserRepository users,
      ConfigService config,
      PasswordEncoder encoder,
      EmailQueueService queue) {}

  @Test
  void maintenancePreservesResetTokenPasswordAndQueueAndAllowsResume() throws Exception {
    SecurityContextHolder.clearContext();
    String token = service.createToken(users.findById(userId).orElseThrow());
    String password = users.findById(userId).orElseThrow().getPassword();
    long tokenCount = tokens.count();
    long mailCount = mails.count();
    clearInvocations(probes.queue(), probes.encoder());
    when(probes.config().getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(
        MaintenanceAccessException.class, () -> service.resetPassword(token, "NewPassword!"));
    assertThrows(MaintenanceAccessException.class, () -> service.validateToken(token));
    assertThrows(
        MaintenanceAccessException.class, () -> service.sendPasswordResetEmail("test@example.com"));
    assertEquals(password, users.findById(userId).orElseThrow().getPassword());
    assertEquals(TokenStatus.ACTIVE, tokens.findByToken(token).orElseThrow().getStatus());
    assertEquals(tokenCount, tokens.count());
    assertEquals(mailCount, mails.count());
    verifyNoInteractions(probes.queue(), probes.encoder());
    service.deleteExpiredTokens();
    when(probes.config().getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    when(probes.encoder().encode("NewPassword!")).thenReturn("updated-hash");
    assertTrue(service.resetPassword(token, "NewPassword!"));
    assertEquals(TokenStatus.USED, tokens.findByToken(token).orElseThrow().getStatus());
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
      var factory = new LocalContainerEntityManagerFactoryBean();
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
        EmailQueueRepository mails,
        UserRepository users,
        JpaTransactionManager manager) {
      return new EmailQueueService(
          config,
          mails,
          mock(EmailService.class),
          users,
          manager,
          mock(AdminNotificationAttemptRepository.class),
          QueueTestWorkers.inline());
    }

    @Bean
    Probes probes(
        PasswordResetTokenRepository tokens,
        UserRepository users,
        ConfigService config,
        EmailQueueService queue) {
      return new Probes(
          mock(PasswordResetTokenRepository.class, delegatesTo(tokens)),
          mock(UserRepository.class, delegatesTo(users)),
          config,
          mock(PasswordEncoder.class),
          mock(EmailQueueService.class, delegatesTo(queue)));
    }

    @Bean
    UserNotificationService userNotifications(
        de.derpeterson.app.repository.UserNotificationRepository messages,
        de.derpeterson.app.repository.UserNotificationRecipientRepository recipients,
        UserRepository users,
        ConfigService config) {
      return new UserNotificationService(
          new UserNotificationPolicy(config),
          messages,
          recipients,
          users,
          mock(de.derpeterson.app.security.SecurityService.class),
          config,
          new de.derpeterson.app.websocket.UserNotificationBroadcaster());
    }

    @Bean
    Clock clock() {
      return mock(Clock.class);
    }

    @Bean
    PasswordResetService passwordResetService(
        Probes probes, Clock clock, UserNotificationService userNotifications) {
      var messages = mock(MessageProperties.class);
      when(messages.getEmailResetPasswordSubject()).thenReturn("Reset password");
      return new PasswordResetService(
          messages,
          probes.tokens(),
          probes.users(),
          probes.config(),
          probes.encoder(),
          probes.queue(),
          mock(LoginAttemptService.class),
          userNotifications,
          clock);
    }
  }
}
