package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.security.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(LoginAttemptPersistenceTest.Config.class)
class LoginAttemptPersistenceTest {
  @Autowired LoginAttemptRepository attempts;
  @Autowired LoginAttemptGuardRepository guard;
  @Autowired LoginAttemptService service;
  @Autowired ConfigService config;
  @Autowired PlatformTransactionManager manager;
  @Autowired TransactionTemplate transaction;
  @Autowired MutableQueueClock clock;
  @Autowired AtomicInteger limit;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired PasswordResetTokenRepository tokens;
  @PersistenceContext EntityManager entityManager;
  private static final String EMAIL = "login-fixture@example.test";

  @BeforeEach
  void clean() {
    attempts.deleteAll();
    guard.deleteAll();
    limit.set(3);
    clock.set(Instant.parse("2026-10-15T12:00:00Z"));
    transaction.executeWithoutResult(
        tx -> {
          entityManager.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
          users.deleteAll();
          users.flush();
          entityManager.createQuery("delete from RoleEntity").executeUpdate();
        });
  }

  private Authentication accepted() {
    return UsernamePasswordAuthenticationToken.authenticated(
        "fixture",
        null,
        List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
  }

  private void bad(String identity) {
    assertThrows(
        BadCredentialsException.class,
        () ->
            service.authenticate(
                identity,
                () -> {
                  throw new BadCredentialsException("fixture");
                }));
  }

  private LoginAttemptEntity state(String identity) {
    return attempts.findById(LoginAttemptService.identityKey(identity)).orElseThrow();
  }

  @Test
  void thresholdPersistsAndBlockedRetriesNeverExtendExpiryOrCheckPassword() {
    limit.set(2);
    bad(EMAIL);
    clock.advance(Duration.ofMinutes(1));
    bad(EMAIL);
    Instant until = state(EMAIL).getBlockedUntil();
    clock.advance(Duration.ofMinutes(4));
    assertThrows(
        LoginThrottledException.class,
        () ->
            service.authenticate(
                EMAIL,
                () -> {
                  fail("Must not check password");
                  return accepted();
                }));
    assertEquals(until, state(EMAIL).getBlockedUntil());
    assertEquals(2, state(EMAIL).getFailures());
    clock.set(until.minusMillis(1));
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    clock.set(until);
    assertNotNull(service.authenticate(EMAIL, this::accepted));
    assertEquals(0, state(EMAIL).getFailures());
  }

  @Test
  void exactWindowBoundaryAndSuccessfulPasswordStartFreshCounters() {
    bad(EMAIL);
    clock.advance(LoginAttemptService.WINDOW.minusMillis(1));
    bad(EMAIL);
    assertEquals(2, state(EMAIL).getFailures());
    clock.advance(Duration.ofMillis(1));
    bad(EMAIL);
    assertEquals(1, state(EMAIL).getFailures());
    service.authenticate(EMAIL, this::accepted);
    assertEquals(0, state(EMAIL).getFailures());
    bad(EMAIL);
    assertEquals(1, state(EMAIL).getFailures());
  }

  @Test
  void normalizedAndUnknownIdentitiesReceiveIdenticalCountersWithoutCreatingAccounts() {
    bad(" Unknown@Example.Test ");
    bad("unknown@example.test");
    assertEquals(1, attempts.count());
    assertEquals(2, state("UNKNOWN@example.test").getFailures());
    assertEquals(0, users.count());
    assertEquals(64, state("unknown@example.test").getIdentityHash().length());
  }

  @Test
  void changedLimitIsReadOnNextAttemptAndDoesNotShortenAnExistingBlock() {
    limit.set(5);
    bad(EMAIL);
    bad(EMAIL);
    limit.set(2);
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    Instant until = state(EMAIL).getBlockedUntil();
    limit.set(100);
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    assertEquals(until, state(EMAIL).getBlockedUntil());
  }

  @Test
  void increasedLimitAllowsFurtherAttemptsBeforeAnyBlock() {
    bad(EMAIL);
    bad(EMAIL);
    limit.set(4);
    bad(EMAIL);
    assertNull(state(EMAIL).getBlockedUntil());
    service.authenticate(EMAIL, this::accepted);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidLimitFailsBeforePasswordCheckingOrStateCreation(int value) {
    limit.set(value);
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            service.authenticate(
                EMAIL,
                () -> {
                  fail();
                  return accepted();
                }));
    assertEquals(0, attempts.count());
  }

  @Test
  void technicalProviderFailureRollsBackWithoutCountingACredentialFailure() {
    bad(EMAIL);
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            service.authenticate(
                EMAIL,
                () -> {
                  throw new IllegalStateException("fixture storage outage");
                }));
    assertEquals(1, state(EMAIL).getFailures());
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            service.authenticate(
                EMAIL,
                () -> {
                  throw new InternalAuthenticationServiceException("fixture");
                }));
    assertEquals(1, state(EMAIL).getFailures());
  }

  @Test
  void concurrentFirstAttemptsAcrossServiceInstancesCountExactlyOnceUntilThreshold()
      throws Exception {
    limit.set(2);
    var start = new CyclicBarrier(8);
    var calls = new AtomicInteger();
    var other =
        new LoginAttemptService(
            attempts, guard, config, manager, clock, LoginAttemptService.MAX_IDENTITIES);
    try (var workers = Executors.newFixedThreadPool(8)) {
      var tasks = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 8; i++) {
        var target = i % 2 == 0 ? service : other;
        tasks.add(
            workers.submit(
                () -> {
                  start.await(10, TimeUnit.SECONDS);
                  try {
                    target.authenticate(
                        EMAIL,
                        () -> {
                          calls.incrementAndGet();
                          throw new BadCredentialsException("fixture");
                        });
                    throw new AssertionError("Must reject");
                  } catch (LoginThrottledException blocked) {
                    return true;
                  } catch (BadCredentialsException bad) {
                    return false;
                  }
                }));
      }
      int blocked = 0;
      for (var task : tasks) if (task.get(20, TimeUnit.SECONDS)) blocked++;
      assertEquals(6, blocked);
    }
    assertEquals(2, calls.get());
    assertEquals(2, state(EMAIL).getFailures());
    assertEquals(1, attempts.count());
  }

  @Test
  void independentFailuresSurviveCallerRollbackAndANewServiceInstance() {
    transaction.executeWithoutResult(
        tx -> {
          bad(EMAIL);
          tx.setRollbackOnly();
        });
    var restarted =
        new LoginAttemptService(
            attempts, guard, config, manager, clock, LoginAttemptService.MAX_IDENTITIES);
    restarted.authenticate(EMAIL, this::accepted);
    assertEquals(0, state(EMAIL).getFailures());
    limit.set(1);
    bad(EMAIL);
    assertThrows(
        LoginThrottledException.class, () -> restarted.authenticate(EMAIL, this::accepted));
  }

  @Test
  void boundedCapacityRefusesNewKeysButKeepsExistingKeysUsableAndCleanupReclaimsSpace() {
    var bounded = new LoginAttemptService(attempts, guard, config, manager, clock, 2);
    bounded.authenticate("first@example.test", this::accepted);
    bounded.authenticate("second@example.test", this::accepted);
    assertThrows(
        AuthenticationServiceException.class,
        () -> bounded.authenticate("third@example.test", this::accepted));
    bounded.authenticate("first@example.test", this::accepted);
    clock.advance(LoginAttemptService.RETENTION);
    bounded.deleteExpiredAttempts();
    assertEquals(2, attempts.count());
    clock.advance(Duration.ofMillis(1));
    bounded.deleteExpiredAttempts();
    assertEquals(0, attempts.count());
    bounded.authenticate("third@example.test", this::accepted);
  }

  @Test
  void cleanupNeverDeletesAnActiveBlockEvenWithOldActivity() {
    bad(EMAIL);
    transaction.executeWithoutResult(
        tx -> {
          var row = attempts.lockIdentity(LoginAttemptService.identityKey(EMAIL)).orElseThrow();
          row.setLastActivity(clock.instant().minus(Duration.ofDays(2)));
          row.setBlockedUntil(clock.instant().plus(Duration.ofHours(1)));
        });
    service.deleteExpiredAttempts();
    assertEquals(1, attempts.count());
  }

  @Test
  void passwordResetClearParticipatesInTransactionAndRollbackRetainsTheBlock() {
    limit.set(1);
    bad(EMAIL);
    transaction.executeWithoutResult(
        tx -> {
          service.clearAfterPasswordReset(EMAIL);
          tx.setRollbackOnly();
        });
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    transaction.executeWithoutResult(tx -> service.clearAfterPasswordReset(EMAIL));
    assertNotNull(service.authenticate(EMAIL, this::accepted));
  }

  @Test
  void concurrentDifferentKeysNeverExceedThePersistentCapacity() throws Exception {
    var bounded = new LoginAttemptService(attempts, guard, config, manager, clock, 2);
    var start = new CyclicBarrier(6);
    try (var workers = Executors.newFixedThreadPool(6)) {
      var results = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 6; i++) {
        String email = "capacity-" + i + "@example.test";
        results.add(
            workers.submit(
                () -> {
                  start.await(10, TimeUnit.SECONDS);
                  try {
                    bounded.authenticate(email, this::accepted);
                    return true;
                  } catch (AuthenticationServiceException unavailable) {
                    return false;
                  }
                }));
      }
      int accepted = 0;
      for (var result : results) if (result.get(20, TimeUnit.SECONDS)) accepted++;
      assertEquals(2, accepted);
      assertEquals(2, attempts.count());
    }
  }

  @Test
  void concurrentSuccessAndFailureAreSerializedWithoutLosingTheLaterFailure() throws Exception {
    bad(EMAIL);
    var inside = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var success =
          workers.submit(
              () ->
                  service.authenticate(
                      EMAIL,
                      () -> {
                        inside.countDown();
                        try {
                          assertTrue(release.await(10, TimeUnit.SECONDS));
                        } catch (InterruptedException error) {
                          throw new AssertionError(error);
                        }
                        return accepted();
                      }));
      assertTrue(inside.await(10, TimeUnit.SECONDS));
      var failure = workers.submit(() -> bad(EMAIL));
      release.countDown();
      assertNotNull(success.get(10, TimeUnit.SECONDS));
      failure.get(10, TimeUnit.SECONDS);
    } finally {
      release.countDown();
    }
    assertEquals(1, state(EMAIL).getFailures());
    assertNull(state(EMAIL).getBlockedUntil());
  }

  private UserEntity createAccount(RoleType type) {
    return transaction.execute(
        tx -> {
          var role = RoleEntity.builder().name(type).build();
          entityManager.persist(role);
          var user =
              UserEntity.builder()
                  .email(EMAIL)
                  .firstName("Login")
                  .lastName("Fixture")
                  .password(encoder.encode("OriginalPassword!"))
                  .enabled(true)
                  .gender(Gender.OTHER)
                  .birthDate(LocalDate.of(1990, 1, 1))
                  .preferredLocale(Locale.ENGLISH)
                  .roleEntities(new ArrayList<>(List.of(role)))
                  .build();
          entityManager.persist(user);
          return user;
        });
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void realPasswordProviderHasNoRoleExemptionsAndErasesCredentials(RoleType role) {
    createAccount(role);
    var provider =
        new org.springframework.security.authentication.dao.DaoAuthenticationProvider(
            new CustomUserDetailsService(users)::loadPasswordUserByUsername);
    provider.setPasswordEncoder(encoder);
    var authentication = new LoginAuthenticationService(provider, service);
    var result =
        authentication.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                " LOGIN-FIXTURE@EXAMPLE.TEST ", "OriginalPassword!"));
    assertNull(result.getCredentials());
    assertNull(
        ((org.springframework.security.core.userdetails.UserDetails) result.getPrincipal())
            .getPassword());
    assertTrue(
        result.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role.name())));
    limit.set(1);
    assertThrows(
        BadCredentialsException.class,
        () ->
            authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "WrongPassword!")));
    assertThrows(
        LoginThrottledException.class,
        () ->
            authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "OriginalPassword!")));
    assertTrue(users.findByEmail(EMAIL).orElseThrow().isEnabled());
  }

  @Test
  void actualPasswordResetClearsOnlyOnCommitTogetherWithTokenAndPassword() {
    var account = createAccount(RoleType.ROLE_USER);
    transaction.executeWithoutResult(
        tx ->
            entityManager.persist(
                new PasswordResetTokenEntity(
                    null,
                    "fixture-reset",
                    entityManager.find(UserEntity.class, account.getId()),
                    LocalDateTime.now(clock).plusHours(1),
                    TokenStatus.ACTIVE)));
    var reset =
        new PasswordResetService(
            mock(de.derpeterson.app.i18n.MessageProperties.class),
            tokens,
            users,
            config,
            encoder,
            mock(EmailQueueService.class),
            service,
            clock);
    limit.set(1);
    bad(EMAIL);
    boolean invalid = transaction.execute(tx -> reset.resetPassword("invalid", "ChangedPassword!"));
    assertFalse(invalid);
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    transaction.executeWithoutResult(
        tx -> {
          assertTrue(reset.resetPassword("fixture-reset", "ChangedPassword!"));
          tx.setRollbackOnly();
        });
    assertEquals(TokenStatus.ACTIVE, tokens.findByToken("fixture-reset").orElseThrow().getStatus());
    assertTrue(
        encoder.matches("OriginalPassword!", users.findByEmail(EMAIL).orElseThrow().getPassword()));
    assertThrows(LoginThrottledException.class, () -> service.authenticate(EMAIL, this::accepted));
    boolean changed =
        transaction.execute(tx -> reset.resetPassword("fixture-reset", "ChangedPassword!"));
    assertTrue(changed);
    assertEquals(TokenStatus.USED, tokens.findByToken("fixture-reset").orElseThrow().getStatus());
    assertTrue(
        encoder.matches("ChangedPassword!", users.findByEmail(EMAIL).orElseThrow().getPassword()));
    assertNotNull(service.authenticate(EMAIL, this::accepted));
  }

  @Configuration
  @Import(UserServicePersistenceTest.Config.class)
  static class Config {
    @Bean
    AtomicInteger limit() {
      return new AtomicInteger(3);
    }

    @Bean
    ConfigService configService(AtomicInteger limit) {
      var config = mock(ConfigService.class);
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenAnswer(call -> limit.get());
      return config;
    }

    @Bean
    MutableQueueClock clock() {
      return new MutableQueueClock(ZoneOffset.UTC);
    }

    @Bean
    PasswordEncoder encoder() {
      return new BCryptPasswordEncoder(4);
    }

    @Bean
    LoginAttemptService attempts(
        LoginAttemptRepository attempts,
        LoginAttemptGuardRepository guard,
        ConfigService config,
        PlatformTransactionManager manager,
        MutableQueueClock clock) {
      return new LoginAttemptService(
          attempts, guard, config, manager, clock, LoginAttemptService.MAX_IDENTITIES);
    }
  }
}
