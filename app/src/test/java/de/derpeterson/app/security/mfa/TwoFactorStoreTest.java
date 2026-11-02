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
  @Autowired TwoFactorStore store;
  @Autowired TwoFactorSecrets secrets;
  @Autowired TwoFactorAccountRepository accounts;
  @Autowired TwoFactorRecoveryCodeRepository codes;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transaction;
  @Autowired TestClock clock;
  @Autowired DataSource source;
  @PersistenceContext EntityManager entityManager;
  private UserEntity user;
  private byte[] secret;
  private UUID initialRevision;

  @BeforeEach
  void setup() {
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    user =
        transaction.execute(
            status -> {
              UserEntity account =
                  UserEntity.builder()
                      .firstName("Test")
                      .lastName("Account")
                      .email(UUID.randomUUID() + "@example.test")
                      .enabled(true)
                      .roleEntities(List.of())
                      .build();
              return users.saveAndFlush(account);
            });
    secret = Totp.newSecret();
    initialRevision = store.initialize(user.getId(), user.getAccessVersion());
  }

  private TwoFactorStore.Enrollment enroll() {
    return store.enroll(
        user.getId(),
        user.getAccessVersion(),
        initialRevision,
        secret,
        Totp.code(secret, clock.instant().getEpochSecond() / 30),
        false);
  }

  @Test
  void persistsOnlyEncryptedSecretAndDigestsAndRejectsCodeReplay() {
    TwoFactorStore.Enrollment result = enroll();
    String encrypted = accounts.findById(user.getId()).orElseThrow().getEncryptedSecret();
    assertFalse(encrypted.contains(Totp.base32(secret)));
    JdbcTemplate jdbc = new JdbcTemplate(source);
    List<String> digests =
        jdbc.queryForList(
            "select digest from two_factor_recovery_codes where user_id = ?",
            String.class,
            user.getId());
    assertEquals(10, digests.size());
    assertTrue(digests.stream().noneMatch(result.recoveryCodes()::contains));
    String firstCode = Totp.code(secret, clock.instant().getEpochSecond() / 30);
    assertThrows(
        BadCredentialsException.class,
        () -> store.verify(user.getId(), user.getAccessVersion(), firstCode));
    clock.now.updateAndGet(now -> now.plusSeconds(30));
    String nextCode = Totp.code(secret, clock.instant().getEpochSecond() / 30);
    assertFalse(store.verify(user.getId(), user.getAccessVersion(), nextCode).recoveryCode());
    assertThrows(
        BadCredentialsException.class,
        () -> store.verify(user.getId(), user.getAccessVersion(), nextCode));
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
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> useCode =
          () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            try {
              assertEquals(
                  recovery,
                  store.verify(user.getId(), user.getAccessVersion(), code).recoveryCode());
              return true;
            } catch (BadCredentialsException expected) {
              return false;
            }
          };
      Future<Boolean> first = executor.submit(useCode);
      Future<Boolean> second = executor.submit(useCode);
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    }
    assertEquals(recovery ? 9 : 10, codes.remaining(user.getId()));
  }

  @Test
  void concurrentFirstEnrollmentKeepsExactlyOneFactorAndRecoverySet() throws Exception {
    byte[] otherSecret = Totp.newSecret();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<TwoFactorStore.Enrollment> first =
          executor.submit(() -> enrollAfterStart(ready, start, secret));
      Future<TwoFactorStore.Enrollment> second =
          executor.submit(() -> enrollAfterStart(ready, start, otherSecret));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      TwoFactorStore.Enrollment firstResult = first.get(10, TimeUnit.SECONDS);
      TwoFactorStore.Enrollment secondResult = second.get(10, TimeUnit.SECONDS);
      assertNotEquals(firstResult == null, secondResult == null);
      byte[] expected = firstResult != null ? secret : otherSecret;
      String encrypted = accounts.findById(user.getId()).orElseThrow().getEncryptedSecret();
      assertArrayEquals(expected, secrets.decrypt(user.getId(), "factor", encrypted));
      assertEquals(10, codes.remaining(user.getId()));
      TwoFactorStore.Enrollment winner = firstResult != null ? firstResult : secondResult;
      List<String> stored =
          new JdbcTemplate(source)
              .queryForList(
                  "select digest from two_factor_recovery_codes where user_id = ?",
                  String.class,
                  user.getId());
      assertTrue(
          winner.recoveryCodes().stream()
              .map(code -> TwoFactorSecrets.recoveryDigest(user.getId(), code))
              .allMatch(stored::contains));
    }
  }

  private TwoFactorStore.Enrollment enrollAfterStart(
      CountDownLatch ready, CountDownLatch start, byte[] seed) throws InterruptedException {
    ready.countDown();
    assertTrue(start.await(10, TimeUnit.SECONDS));
    try {
      return store.enroll(
          user.getId(),
          user.getAccessVersion(),
          initialRevision,
          seed,
          Totp.code(seed, clock.instant().getEpochSecond() / 30),
          false);
    } catch (BadCredentialsException expected) {
      return null;
    }
  }

  @Test
  void failedChecksRemainCommittedAndThrottleAcrossCalls() {
    enroll();
    for (int attempt = 0; attempt < 4; attempt++) {
      assertThrows(
          BadCredentialsException.class,
          () -> store.verify(user.getId(), user.getAccessVersion(), "invalid"));
    }
    assertThrows(
        LockedException.class,
        () -> store.verify(user.getId(), user.getAccessVersion(), "invalid"));
    clock.now.updateAndGet(now -> now.plusSeconds(30));
    String valid = Totp.code(secret, clock.instant().getEpochSecond() / 30);
    assertThrows(
        LockedException.class, () -> store.verify(user.getId(), user.getAccessVersion(), valid));
    clock.now.updateAndGet(now -> now.plusSeconds(901));
    store.verify(
        user.getId(),
        user.getAccessVersion(),
        Totp.code(secret, clock.instant().getEpochSecond() / 30));
    assertEquals(0, accounts.findById(user.getId()).orElseThrow().getFailedAttempts());
  }

  @Test
  void regenerationInvalidatesEveryOldCodeAndChangesTheFactorRevision() {
    TwoFactorStore.Enrollment enrolled = enroll();
    List<String> renewed =
        store.renewRecoveryCodes(user.getId(), user.getAccessVersion(), enrolled.revision());
    assertNotEquals(enrolled.revision(), accounts.state(user.getId()).orElseThrow().getRevision());
    assertThrows(
        BadCredentialsException.class,
        () ->
            store.verify(
                user.getId(), user.getAccessVersion(), enrolled.recoveryCodes().getFirst()));
    assertTrue(
        store.verify(user.getId(), user.getAccessVersion(), renewed.getFirst()).recoveryCode());
    assertEquals(9, codes.remaining(user.getId()));
  }

  @Test
  void staleCredentialsOrEnrollmentCannotReplaceAFactor() {
    TwoFactorStore.Enrollment enrolled = enroll();
    assertThrows(
        BadCredentialsException.class,
        () ->
            store.enroll(
                user.getId(),
                user.getAccessVersion(),
                initialRevision,
                Totp.newSecret(),
                "123456",
                false));
    assertThrows(
        BadCredentialsException.class,
        () -> store.verify(user.getId(), "changed", enrolled.recoveryCodes().getFirst()));
    store.disable(user.getId(), user.getAccessVersion(), enrolled.revision());
    assertFalse(accounts.state(user.getId()).orElseThrow().getEnrolled());
    assertTrue(accounts.state(user.getId()).orElseThrow().getPreviouslyEnrolled());
    assertEquals(0, codes.remaining(user.getId()));
    UUID revision = accounts.state(user.getId()).orElseThrow().getRevision();
    assertThrows(
        BadCredentialsException.class,
        () ->
            store.enroll(
                user.getId(),
                user.getAccessVersion(),
                revision,
                secret,
                Totp.code(secret, clock.instant().getEpochSecond() / 30),
                false));
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
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
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
        UserRepository users,
        TwoFactorAccountRepository accounts,
        TwoFactorRecoveryCodeRepository codes,
        EntityManagerFactory factory,
        TwoFactorSecrets secrets,
        JpaTransactionManager manager,
        TestClock clock) {
      return new TwoFactorStore(
          users,
          accounts,
          codes,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          secrets,
          manager,
          clock);
    }
  }
}
