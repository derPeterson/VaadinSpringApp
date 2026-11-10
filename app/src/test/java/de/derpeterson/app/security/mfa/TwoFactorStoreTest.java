package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.TwoFactorAccountRepository;
import de.derpeterson.app.repository.TwoFactorRecoveryCodeRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(TwoFactorStoreTest.Config.class)
class TwoFactorStoreTest {
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired TwoFactorSecrets secrets;
  @Autowired TwoFactorAccountRepository twoFactorAccountRepository;
  @Autowired TwoFactorRecoveryCodeRepository codes;
  @Autowired UserRepository userRepository;
  @Autowired TransactionTemplate transaction;
  @Autowired TestClock clock;
  @Autowired DataSource testDataSource;
  @PersistenceContext EntityManager entityManager;
  private UserEntity testUserEntity;
  private byte[] secret;
  private UUID initialRevision;

  @BeforeEach
  void setup() {
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    testUserEntity =
        transaction.execute(status -> {
          UserEntity testAccountEntity =
              UserEntity.builder()
                  .firstName("Test")
                  .lastName("Account")
                  .email(UUID.randomUUID() + "@example.test")
                  .enabled(true)
                  .roleEntities(List.of())
                  .build();
          return userRepository.saveAndFlush(testAccountEntity);
        });
    secret = Totp.newSecret();
    initialRevision =
        twoFactorStore.initialize(testUserEntity.getId(), testUserEntity.getAccessVersion());
  }

  private TwoFactorStore.Enrollment enroll() {
    return twoFactorStore.enroll(
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        initialRevision,
        secret,
        Totp.code(secret, clock.instant().getEpochSecond() / 30),
        false);
  }

  @Test
  void persistsOnlyEncryptedSecretAndDigestsAndRejectsCodeReplay() {
    TwoFactorStore.Enrollment result = enroll();
    String encrypted =
        twoFactorAccountRepository
            .findById(testUserEntity.getId())
            .orElseThrow()
            .getEncryptedSecret();

    assertFalse(encrypted.contains(Totp.base32(secret)));
    JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
    List<String> digests =
        jdbcTemplate.queryForList(
            "select digest from two_factor_recovery_codes where user_id = ?",
            String.class,
            testUserEntity.getId());

    assertEquals(10, digests.size());
    assertTrue(digests.stream().noneMatch(result.recoveryCodes()::contains));
    String firstCode = Totp.code(secret, clock.instant().getEpochSecond() / 30);
    Long userId = testUserEntity.getId();
    String accessVersion = testUserEntity.getAccessVersion();

    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, firstCode));
    clock.now.updateAndGet(now -> now.plusSeconds(30));
    String nextCode = Totp.code(secret, clock.instant().getEpochSecond() / 30);

    assertFalse(
        twoFactorStore
            .verify(testUserEntity.getId(), testUserEntity.getAccessVersion(), nextCode)
            .recoveryCode());
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, nextCode));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void concurrentCodeUseSucceedsExactlyOnce(boolean recovery) throws Exception {
    TwoFactorStore.Enrollment enrolled = enroll();
    clock.now.updateAndGet(now -> now.plusSeconds(30));
    String code =
        recovery
            ? enrolled.recoveryCodes().getFirst()
            : Totp.code(secret, clock.instant().getEpochSecond() / 30);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> useCode =
          () -> {
            ready.countDown();

            assertTrue(start.await(10, TimeUnit.SECONDS));
            try {
              assertEquals(
                  recovery,
                  twoFactorStore
                      .verify(testUserEntity.getId(), testUserEntity.getAccessVersion(), code)
                      .recoveryCode());
              return true;
            } catch (BadCredentialsException _) {
              return false;
            }
          };
      Future<Boolean> first = executorService.submit(useCode);
      Future<Boolean> second = executorService.submit(useCode);

      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();

      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    }
    assertEquals(recovery ? 9 : 10, codes.remaining(testUserEntity.getId()));
  }

  @Test
  void concurrentFirstEnrollmentKeepsExactlyOneFactorAndRecoverySet() throws Exception {
    byte[] otherSecret = Totp.newSecret();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Future<TwoFactorStore.Enrollment> first =
          executorService.submit(() -> enrollAfterStart(ready, start, secret));
      Future<TwoFactorStore.Enrollment> second =
          executorService.submit(() -> enrollAfterStart(ready, start, otherSecret));

      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      TwoFactorStore.Enrollment firstResult = first.get(10, TimeUnit.SECONDS);
      TwoFactorStore.Enrollment secondResult = second.get(10, TimeUnit.SECONDS);

      assertNotEquals(firstResult == null, secondResult == null);
      byte[] expected = firstResult != null ? secret : otherSecret;
      String encrypted =
          twoFactorAccountRepository
              .findById(testUserEntity.getId())
              .orElseThrow()
              .getEncryptedSecret();

      assertArrayEquals(expected, secrets.decrypt(testUserEntity.getId(), "factor", encrypted));
      assertEquals(10, codes.remaining(testUserEntity.getId()));
      TwoFactorStore.Enrollment winner = firstResult != null ? firstResult : secondResult;
      List<String> stored =
          new JdbcTemplate(testDataSource)
              .queryForList(
                  "select digest from two_factor_recovery_codes where user_id = ?",
                  String.class,
                  testUserEntity.getId());

      assertTrue(
          winner.recoveryCodes().stream()
              .map(code -> TwoFactorSecrets.recoveryDigest(testUserEntity.getId(), code))
              .allMatch(stored::contains));
    }
  }

  private TwoFactorStore.Enrollment enrollAfterStart(
      CountDownLatch ready, CountDownLatch start, byte[] seed) throws InterruptedException {
    ready.countDown();

    assertTrue(start.await(10, TimeUnit.SECONDS));
    try {
      return twoFactorStore.enroll(
          testUserEntity.getId(),
          testUserEntity.getAccessVersion(),
          initialRevision,
          seed,
          Totp.code(seed, clock.instant().getEpochSecond() / 30),
          false);
    } catch (BadCredentialsException _) {
      return null;
    }
  }

  @Test
  void failedChecksRemainCommittedAndThrottleAcrossCalls() {
    enroll();
    Long userId = testUserEntity.getId();
    String accessVersion = testUserEntity.getAccessVersion();
    for (int attempt = 0; attempt < 4; attempt++) {
      assertThrows(
          BadCredentialsException.class,
          () -> twoFactorStore.verify(userId, accessVersion, "invalid"));
    }
    assertThrows(
        LockedException.class, () -> twoFactorStore.verify(userId, accessVersion, "invalid"));
    clock.now.updateAndGet(now -> now.plusSeconds(30));
    String valid = Totp.code(secret, clock.instant().getEpochSecond() / 30);

    assertThrows(LockedException.class, () -> twoFactorStore.verify(userId, accessVersion, valid));
    clock.now.updateAndGet(now -> now.plusSeconds(901));
    twoFactorStore.verify(
        testUserEntity.getId(),
        testUserEntity.getAccessVersion(),
        Totp.code(secret, clock.instant().getEpochSecond() / 30));

    assertEquals(
        0,
        twoFactorAccountRepository
            .findById(testUserEntity.getId())
            .orElseThrow()
            .getFailedAttempts());
  }

  @Test
  void regenerationInvalidatesEveryOldCodeAndChangesTheFactorRevision() {
    TwoFactorStore.Enrollment enrolled = enroll();
    List<String> renewed =
        twoFactorStore.renewRecoveryCodes(
            testUserEntity.getId(), testUserEntity.getAccessVersion(), enrolled.revision());

    assertNotEquals(
        enrolled.revision(),
        twoFactorAccountRepository.state(testUserEntity.getId()).orElseThrow().getRevision());
    Long userId = testUserEntity.getId();
    String accessVersion = testUserEntity.getAccessVersion();
    String firstRecoveryCode = enrolled.recoveryCodes().getFirst();

    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, firstRecoveryCode));
    assertTrue(
        twoFactorStore
            .verify(testUserEntity.getId(), testUserEntity.getAccessVersion(), renewed.getFirst())
            .recoveryCode());
    assertEquals(9, codes.remaining(testUserEntity.getId()));
  }

  @Test
  void staleCredentialsOrEnrollmentCannotReplaceAFactor() {
    TwoFactorStore.Enrollment enrolled = enroll();
    Long userId = testUserEntity.getId();
    String accessVersion = testUserEntity.getAccessVersion();
    byte[] replacementSecret = Totp.newSecret();

    assertThrows(
        BadCredentialsException.class,
        () ->
            twoFactorStore.enroll(
                userId, accessVersion, initialRevision, replacementSecret, "123456", false));
    String recoveryCode = enrolled.recoveryCodes().getFirst();

    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, "changed", recoveryCode));
    twoFactorStore.disable(
        testUserEntity.getId(), testUserEntity.getAccessVersion(), enrolled.revision());

    assertFalse(
        twoFactorAccountRepository.state(testUserEntity.getId()).orElseThrow().getEnrolled());
    assertTrue(
        twoFactorAccountRepository
            .state(testUserEntity.getId())
            .orElseThrow()
            .getPreviouslyEnrolled());
    assertEquals(0, codes.remaining(testUserEntity.getId()));
    UUID revision =
        twoFactorAccountRepository.state(testUserEntity.getId()).orElseThrow().getRevision();
    String replacementCode = Totp.code(secret, clock.instant().getEpochSecond() / 30);

    assertThrows(
        BadCredentialsException.class,
        () ->
            twoFactorStore.enroll(userId, accessVersion, revision, secret, replacementCode, false));
  }

  static class TestClock extends Clock {
    final AtomicReference<Instant> now = new AtomicReference<>();

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  }

  @Configuration
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          "jdbc:h2:mem:two-factor-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource testDataSource) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(testDataSource);
      factory.setPackagesToScan("de.derpeterson.app.model");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(
          Map.of(
              "hibernate.hbm2ddl.auto",
              "create-drop",
              "hibernate.physical_naming_strategy",
              "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"));
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
    TestClock testClock() {
      return new TestClock();
    }

    @Bean
    TwoFactorSecrets secrets() {
      byte[] key = new byte[32];
      new java.security.SecureRandom().nextBytes(key);
      return new TwoFactorSecrets(
          new MockEnvironment()
              .withProperty(
                  TwoFactorSecrets.KEY_PROPERTY, Base64.getEncoder().encodeToString(key)));
    }

    @Bean
    TwoFactorStore store(
        UserRepository userRepository,
        TwoFactorAccountRepository twoFactorAccountRepository,
        TwoFactorRecoveryCodeRepository codes,
        EntityManagerFactory factory,
        TwoFactorSecrets secrets,
        JpaTransactionManager manager,
        TestClock clock) {
      return new TwoFactorStore(
          userRepository,
          twoFactorAccountRepository,
          codes,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          secrets,
          manager,
          clock);
    }
  }
}
