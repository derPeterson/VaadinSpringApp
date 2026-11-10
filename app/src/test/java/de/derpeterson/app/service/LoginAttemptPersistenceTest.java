package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.LoginAttemptEntity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.LoginAttemptGuardRepository;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.LoginAuthenticationService;
import de.derpeterson.app.security.LoginThrottledException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(LoginAttemptPersistenceTest.Config.class)
class LoginAttemptPersistenceTest {
  @Autowired LoginAttemptRepository loginAttemptRepository;
  @Autowired LoginAttemptGuardRepository guard;
  @Autowired LoginAttemptService loginAttemptService;
  @Autowired ConfigService config;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired MutableQueueClock clock;
  @Autowired AtomicInteger limit;
  @Autowired AtomicInteger loginWindow;
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
  @PersistenceContext EntityManager entityManager;
  private static final String EMAIL = "login-fixture@example.test";

  @BeforeEach
  void clean() {
    loginAttemptRepository.deleteAll();
    guard.deleteAll();
    limit.set(3);
    loginWindow.set(900);
    clock.set(Instant.parse("2026-10-15T12:00:00Z"));
    transactionTemplate.executeWithoutResult(tx -> {
      entityManager.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
      userRepository.deleteAll();
      userRepository.flush();
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
            loginAttemptService.authenticate(
                identity,
                () -> {
                  throw new BadCredentialsException("fixture");
                }));
  }

  private LoginAttemptEntity state(String identity) {
    return loginAttemptRepository.findById(LoginAttemptService.identityKey(identity)).orElseThrow();
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
            loginAttemptService.authenticate(
                EMAIL,
                () -> {
                  fail("Must not check password");
                  return accepted();
                }));
    assertEquals(until, state(EMAIL).getBlockedUntil());
    assertEquals(2, state(EMAIL).getFailures());
    clock.set(until.minusMillis(1));

    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    clock.set(until);

    assertNotNull(loginAttemptService.authenticate(EMAIL, this::accepted));
    assertEquals(0, state(EMAIL).getFailures());
  }

  @Test
  void exactWindowBoundaryAndSuccessfulPasswordStartFreshCounters() {
    bad(EMAIL);
    clock.advance(Duration.ofSeconds(loginWindow.get()).minusMillis(1));
    bad(EMAIL);

    assertEquals(2, state(EMAIL).getFailures());
    clock.advance(Duration.ofMillis(1));
    bad(EMAIL);

    assertEquals(1, state(EMAIL).getFailures());
    loginAttemptService.authenticate(EMAIL, this::accepted);

    assertEquals(0, state(EMAIL).getFailures());
    bad(EMAIL);

    assertEquals(1, state(EMAIL).getFailures());
  }

  @Test
  void normalizedAndUnknownIdentitiesReceiveIdenticalCountersWithoutCreatingAccounts() {
    bad(" Unknown@Example.Test ");
    bad("unknown@example.test");

    assertEquals(1, loginAttemptRepository.count());
    assertEquals(2, state("UNKNOWN@example.test").getFailures());
    assertEquals(0, userRepository.count());
    assertEquals(64, state("unknown@example.test").getIdentityHash().length());
  }

  @Test
  void changedLimitIsReadOnNextAttemptAndDoesNotShortenAnExistingBlock() {
    limit.set(5);
    bad(EMAIL);
    bad(EMAIL);
    limit.set(2);

    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    Instant until = state(EMAIL).getBlockedUntil();
    limit.set(100);
    loginWindow.set(1);

    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    assertEquals(until, state(EMAIL).getBlockedUntil());
  }

  @Test
  void shorteningTheWindowResetsAnExpiredCounterOnTheNextAttempt() {
    bad(EMAIL);
    clock.advance(Duration.ofMinutes(2));
    loginWindow.set(60);
    bad(EMAIL);

    assertEquals(1, state(EMAIL).getFailures());
  }

  @Test
  void increasedLimitAllowsFurtherAttemptsBeforeAnyBlock() {
    bad(EMAIL);
    bad(EMAIL);
    limit.set(4);
    bad(EMAIL);

    assertNull(state(EMAIL).getBlockedUntil());
    loginAttemptService.authenticate(EMAIL, this::accepted);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidLimitFailsBeforePasswordCheckingOrStateCreation(int value) {
    limit.set(value);

    assertThrows(
        AuthenticationServiceException.class,
        () ->
            loginAttemptService.authenticate(
                EMAIL,
                () -> {
                  fail();
                  return accepted();
                }));
    assertEquals(0, loginAttemptRepository.count());
  }

  @Test
  void technicalProviderFailureRollsBackWithoutCountingACredentialFailure() {
    bad(EMAIL);

    assertThrows(
        AuthenticationServiceException.class,
        () ->
            loginAttemptService.authenticate(
                EMAIL,
                () -> {
                  throw new IllegalStateException("fixture storage outage");
                }));
    assertEquals(1, state(EMAIL).getFailures());
    assertThrows(
        AuthenticationServiceException.class,
        () ->
            loginAttemptService.authenticate(
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
    CyclicBarrier start = new CyclicBarrier(8);
    AtomicInteger callCount = new AtomicInteger();
    LoginAttemptService other =
        new LoginAttemptService(
            loginAttemptRepository,
            guard,
            config,
            transactionManager,
            clock,
            LoginAttemptService.MAX_IDENTITIES);
    try (ExecutorService workers = Executors.newFixedThreadPool(8)) {
      ArrayList<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
      for (int loginAttemptIndex = 0; loginAttemptIndex < 8; loginAttemptIndex++) {
        LoginAttemptService target = loginAttemptIndex % 2 == 0 ? loginAttemptService : other;
        tasks.add(
            workers.submit(() -> {
              start.await(10, TimeUnit.SECONDS);
              try {
                target.authenticate(
                    EMAIL,
                    () -> {
                      callCount.incrementAndGet();
                      throw new BadCredentialsException("fixture");
                    });
                throw new AssertionError("Must reject");
              } catch (LoginThrottledException _) {
                return true;
              } catch (BadCredentialsException _) {
                return false;
              }
            }));
      }
      int blocked = 0;
      for (Future<Boolean> task : tasks) {
        if (task.get(20, TimeUnit.SECONDS)) {
          blocked++;
        }
      }
      assertEquals(6, blocked);
    }
    assertEquals(2, callCount.get());
    assertEquals(2, state(EMAIL).getFailures());
    assertEquals(1, loginAttemptRepository.count());
  }

  @Test
  void independentFailuresSurviveCallerRollbackAndANewServiceInstance() {
    transactionTemplate.executeWithoutResult(tx -> {
      bad(EMAIL);
      tx.setRollbackOnly();
    });
    LoginAttemptService restarted =
        new LoginAttemptService(
            loginAttemptRepository,
            guard,
            config,
            transactionManager,
            clock,
            LoginAttemptService.MAX_IDENTITIES);
    restarted.authenticate(EMAIL, this::accepted);

    assertEquals(0, state(EMAIL).getFailures());
    limit.set(1);
    bad(EMAIL);

    assertThrows(
        LoginThrottledException.class, () -> restarted.authenticate(EMAIL, this::accepted));
  }

  @Test
  void boundedCapacityRefusesNewKeysButKeepsExistingKeysUsableAndCleanupReclaimsSpace() {
    LoginAttemptService bounded =
        new LoginAttemptService(
            loginAttemptRepository, guard, config, transactionManager, clock, 2);
    bounded.authenticate("first@example.test", this::accepted);
    bounded.authenticate("second@example.test", this::accepted);

    assertThrows(
        AuthenticationServiceException.class,
        () -> bounded.authenticate("third@example.test", this::accepted));
    bounded.authenticate("first@example.test", this::accepted);
    clock.advance(LoginAttemptService.RETENTION);
    bounded.deleteExpiredAttempts();

    assertEquals(2, loginAttemptRepository.count());
    clock.advance(Duration.ofMillis(1));
    bounded.deleteExpiredAttempts();

    assertEquals(0, loginAttemptRepository.count());
    bounded.authenticate("third@example.test", this::accepted);
  }

  @Test
  void cleanupNeverDeletesAnActiveBlockEvenWithOldActivity() {
    bad(EMAIL);
    transactionTemplate.executeWithoutResult(tx -> {
      LoginAttemptEntity row =
          loginAttemptRepository.lockIdentity(LoginAttemptService.identityKey(EMAIL)).orElseThrow();
      row.setLastActivity(clock.instant().minus(Duration.ofDays(2)));
      row.setBlockedUntil(clock.instant().plus(Duration.ofHours(1)));
    });
    loginAttemptService.deleteExpiredAttempts();

    assertEquals(1, loginAttemptRepository.count());
  }

  @Test
  void passwordResetClearParticipatesInTransactionAndRollbackRetainsTheBlock() {
    limit.set(1);
    bad(EMAIL);
    transactionTemplate.executeWithoutResult(tx -> {
      loginAttemptService.clearAfterPasswordReset(EMAIL);
      tx.setRollbackOnly();
    });

    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    transactionTemplate.executeWithoutResult(
        tx -> loginAttemptService.clearAfterPasswordReset(EMAIL));

    assertNotNull(loginAttemptService.authenticate(EMAIL, this::accepted));
  }

  @Test
  void concurrentDifferentKeysNeverExceedThePersistentCapacity() throws Exception {
    LoginAttemptService bounded =
        new LoginAttemptService(
            loginAttemptRepository, guard, config, transactionManager, clock, 2);
    CyclicBarrier start = new CyclicBarrier(6);
    try (ExecutorService workers = Executors.newFixedThreadPool(6)) {
      ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
      for (int emailAddressIndex = 0; emailAddressIndex < 6; emailAddressIndex++) {
        String email = "capacity-" + emailAddressIndex + "@example.test";
        results.add(
            workers.submit(() -> {
              start.await(10, TimeUnit.SECONDS);
              try {
                bounded.authenticate(email, this::accepted);
                return true;
              } catch (AuthenticationServiceException _) {
                return false;
              }
            }));
      }
      int accepted = 0;
      for (Future<Boolean> result : results) {
        if (result.get(20, TimeUnit.SECONDS)) {
          accepted++;
        }
      }
      assertEquals(2, accepted);
      assertEquals(2, loginAttemptRepository.count());
    }
  }

  @Test
  void concurrentSuccessAndFailureAreSerializedWithoutLosingTheLaterFailure() throws Exception {
    bad(EMAIL);
    CountDownLatch inside = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      Future<Authentication> success =
          workers.submit(() ->
              loginAttemptService.authenticate(
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
      Future<?> failure = workers.submit(() -> bad(EMAIL));
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
    return transactionTemplate.execute(tx -> {
      RoleEntity role = RoleEntity.builder().name(type).build();
      entityManager.persist(role);
      UserEntity testUserEntity =
          UserEntity.builder()
              .email(EMAIL)
              .firstName("Login")
              .lastName("Fixture")
              .password(passwordEncoder.encode("OriginalPassword!"))
              .enabled(true)
              .gender(Gender.OTHER)
              .birthDate(LocalDate.of(1990, 1, 1))
              .preferredLocale(Locale.ENGLISH)
              .roleEntities(new ArrayList<>(List.of(role)))
              .build();
      entityManager.persist(testUserEntity);
      return testUserEntity;
    });
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void realPasswordProviderHasNoRoleExemptionsAndErasesCredentials(RoleType role) {
    createAccount(role);
    DaoAuthenticationProvider provider =
        new org.springframework.security.authentication.dao.DaoAuthenticationProvider(
            new CustomUserDetailsService(userRepository)::loadPasswordUserByUsername);
    provider.setPasswordEncoder(passwordEncoder);
    LoginAuthenticationService authentication =
        new LoginAuthenticationService(provider, loginAttemptService);
    Authentication result =
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
    UsernamePasswordAuthenticationToken wrongCredentials =
        UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "WrongPassword!");

    assertThrows(
        BadCredentialsException.class, () -> authentication.authenticate(wrongCredentials));
    UsernamePasswordAuthenticationToken blockedCredentials =
        UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "OriginalPassword!");

    assertThrows(
        LoginThrottledException.class, () -> authentication.authenticate(blockedCredentials));
    assertTrue(userRepository.findByEmail(EMAIL).orElseThrow().isEnabled());
  }

  @Test
  void actualPasswordResetClearsOnlyOnCommitTogetherWithTokenAndPassword() {
    UserEntity testAccountEntity = createAccount(RoleType.ROLE_USER);
    transactionTemplate.executeWithoutResult(tx ->
        entityManager.persist(
            new PasswordResetTokenEntity(
                null,
                "fixture-reset",
                entityManager.find(UserEntity.class, testAccountEntity.getId()),
                LocalDateTime.now(clock).plusHours(1),
                TokenStatus.ACTIVE)));
    PasswordResetService reset =
        new PasswordResetService(
            mock(de.derpeterson.app.i18n.MessageProperties.class),
            passwordResetTokenRepository,
            userRepository,
            config,
            passwordEncoder,
            mock(EmailQueueService.class),
            loginAttemptService,
            mock(UserNotificationService.class),
            clock);
    limit.set(1);
    bad(EMAIL);
    boolean invalid =
        transactionTemplate.execute(tx -> reset.resetPassword("invalid", "ChangedPassword!"));

    assertFalse(invalid);
    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    transactionTemplate.executeWithoutResult(tx -> {
      assertTrue(reset.resetPassword("fixture-reset", "ChangedPassword!"));
      tx.setRollbackOnly();
    });

    assertEquals(
        TokenStatus.ACTIVE,
        passwordResetTokenRepository.findByToken("fixture-reset").orElseThrow().getStatus());
    assertTrue(
        passwordEncoder.matches(
            "OriginalPassword!", userRepository.findByEmail(EMAIL).orElseThrow().getPassword()));
    assertThrows(
        LoginThrottledException.class,
        () -> loginAttemptService.authenticate(EMAIL, this::accepted));
    boolean changed =
        transactionTemplate.execute(tx -> reset.resetPassword("fixture-reset", "ChangedPassword!"));

    assertTrue(changed);
    assertEquals(
        TokenStatus.USED,
        passwordResetTokenRepository.findByToken("fixture-reset").orElseThrow().getStatus());
    assertTrue(
        passwordEncoder.matches(
            "ChangedPassword!", userRepository.findByEmail(EMAIL).orElseThrow().getPassword()));
    assertNotNull(loginAttemptService.authenticate(EMAIL, this::accepted));
  }

  @Configuration
  @Import(UserServicePersistenceTest.Config.class)
  static class Config {
    @Bean
    AtomicInteger limit() {
      return new AtomicInteger(3);
    }

    @Bean
    AtomicInteger loginWindow() {
      return new AtomicInteger(900);
    }

    @Bean
    ConfigService configService(AtomicInteger limit, AtomicInteger loginWindow) {
      ConfigService config = mock(ConfigService.class);
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenAnswer(call -> limit.get());
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPT_WINDOW_SECONDS))
          .thenAnswer(call -> loginWindow.get());
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
        LoginAttemptRepository loginAttemptRepository,
        LoginAttemptGuardRepository guard,
        ConfigService config,
        PlatformTransactionManager transactionManager,
        MutableQueueClock clock) {
      return new LoginAttemptService(
          loginAttemptRepository,
          guard,
          config,
          transactionManager,
          clock,
          LoginAttemptService.MAX_IDENTITIES);
    }
  }
}
