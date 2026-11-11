package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import de.derpeterson.app.security.PendingEmailVerificationFlow;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Real token AND queue persistence; no scheduler or SMTP sender is started. */
@SpringJUnitConfig(VerificationQueueConcurrencyTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VerificationQueueConcurrencyTest {
  @Autowired private VerificationService verification;
  @Autowired private PendingEmailVerificationFlow pendingEmailVerificationFlow;
  @Autowired private EmailQueueService mailQueue;
  @Autowired private UserService admin;
  @Autowired private UserRepository userRepository;
  @Autowired private VerificationTokenRepository verificationTokenRepository;
  @Autowired private EmailQueueRepository emailQueueRepository;
  @Autowired private TransactionTemplate tx;
  @Autowired private Probes probes;
  private Long id;

  @BeforeEach
  void seed() {
    reset(
        probes.userRepository(),
        probes.verificationTokenRepository(),
        probes.emailQueueRepository());
    tx.executeWithoutResult(status -> {
      emailQueueRepository.deleteAll();
      verificationTokenRepository.deleteAll();
      userRepository.deleteAll();
      userRepository.flush();
      UserEntity testUserEntity =
          UserEntity.builder()
              .firstName("Test")
              .lastName("User")
              .email("test@example.com")
              .password("hash")
              .gender(Gender.OTHER)
              .birthDate(LocalDate.of(1990, 1, 1))
              .preferredLocale(Locale.ENGLISH)
              .roleEntities(new ArrayList<>())
              .build();
      id = userRepository.saveAndFlush(testUserEntity).getId();
      verificationTokenRepository.saveAndFlush(
          new VerificationTokenEntity(
              null, "old", testUserEntity, LocalDateTime.now().plusDays(1), TokenStatus.ACTIVE));
    });
  }

  @Test
  void restrictedProofResendCommitsOneMailAndRotatesTheToken() throws IOException {
    MockHttpServletRequest httpRequest = requestWithPendingVerificationProof();

    assertTrue(pendingEmailVerificationFlow.currentAccount(httpRequest).isPresent());
    assertTrue(pendingEmailVerificationFlow.resendVerification(httpRequest));

    assertCoherentQueue();
    assertEquals(TokenStatus.INACTIVE, tokenStatus("old"));
    assertNotNull(snapshot().getVerificationMailRequestedAt());
  }

  @Test
  void restrictedProofResendDoesNotReplaceAnUnresolvedTransportAttempt() throws IOException {
    MockHttpServletRequest httpRequest = requestWithPendingVerificationProof();
    assertTrue(pendingEmailVerificationFlow.resendVerification(httpRequest));
    tx.executeWithoutResult(transactionStatus -> {
      EmailQueueEntity unresolvedEmailQueueEntity = emailQueueRepository.findAll().getFirst();
      unresolvedEmailQueueEntity.setStatus(EmailStatus.IN_PROGRESS);
      emailQueueRepository.saveAndFlush(unresolvedEmailQueueEntity);
    });
    long verificationTokenCount = verificationTokenRepository.count();

    assertTrue(pendingEmailVerificationFlow.resendVerification(httpRequest));

    assertEquals(1, emailQueueRepository.count());
    assertEquals(EmailStatus.IN_PROGRESS, emailQueueRepository.findAll().getFirst().getStatus());
    assertEquals(verificationTokenCount, verificationTokenRepository.count());
  }

  @Test
  void invalidatedRestrictedProofCannotQueueAMail() {
    MockHttpServletRequest httpRequest = requestWithPendingVerificationProof();
    tx.executeWithoutResult(transactionStatus -> {
      UserEntity storedUserEntity = userRepository.findById(id).orElseThrow();
      storedUserEntity.setAccessVersion("changed-after-first-factor");
      userRepository.saveAndFlush(storedUserEntity);
    });

    assertThrows(
        AccessDeniedException.class,
        () -> pendingEmailVerificationFlow.resendVerification(httpRequest));

    assertEquals(0, emailQueueRepository.count());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
  }

  private MockHttpServletRequest requestWithPendingVerificationProof() {
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    PendingEmailVerificationFlow.startForProviderAccount(httpRequest, snapshot());
    return httpRequest;
  }

  @ParameterizedTest
  @ValueSource(strings = {"token", "email", "user"})
  void concurrentResendsCommitOneMailWhoseLinkIsTheOnlyActiveToken(String entry) throws Exception {
    rendezvous();

    assertEquals(List.of(true, true), compete(() -> resend(entry), () -> resend(entry)));
    assertCoherentQueue();

    assertEquals(TokenStatus.INACTIVE, tokenStatus("old"));
    assertEquals(2, verificationTokenRepository.count());
  }

  @Test
  void mixedResendEntrypointsAlsoShareTheSameAccountLock() throws Exception {
    rendezvous();

    assertEquals(List.of(true, true), compete(() -> resend("token"), () -> resend("email")));
    assertCoherentQueue();
  }

  @Test
  void activationAgainstResendHasACoherentStoredOutcomeInEitherOrder() throws Exception {
    rendezvous();
    List<Object> results = compete(() -> verification.validateToken("old"), () -> resend("email"));
    if (Boolean.TRUE.equals(results.get(0))) {
      assertEquals(List.of(true, false), results);
      assertTrue(snapshot().isEnabled());
      assertFalse(snapshot().isVerificationPending());
      assertEquals(TokenStatus.USED, tokenStatus("old"));
      assertEquals(0, emailQueueRepository.count());
    } else {
      assertEquals(List.of(false, true), results);
      assertCoherentQueue();

      assertEquals(TokenStatus.INACTIVE, tokenStatus("old"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "token,logo",
    "email,logo",
    "user,logo",
    "token,template",
    "email,template",
    "user,template"
  })
  void checkedResourceFailureAlsoRollsBackWithTheRealQueueService(String entry, String resource) {
    IOException failure = new IOException("simulated image failure");
    try (MockedStatic<ImageHelper> image = mockStatic(ImageHelper.class, CALLS_REAL_METHODS);
        MockedStatic<CustomI18NProvider> locale = mockStatic(CustomI18NProvider.class)) {
      locale
          .when(CustomI18NProvider::getCurrentLocale)
          .thenReturn(resource.equals("template") ? Locale.FRENCH : Locale.ENGLISH);
      if (resource.equals("logo")) {
        image
            .when(() -> ImageHelper.readImageBytes("META-INF/resources/icons/service-logo.png"))
            .thenThrow(failure);
      }
      IOException thrown = assertThrows(IOException.class, () -> resend(entry));
      if (resource.equals("logo")) {
        assertSame(failure, thrown);
      } else {
        assertInstanceOf(java.io.FileNotFoundException.class, thrown);
      }
    }
    assertEquals(0, emailQueueRepository.count());
    assertEquals(1, verificationTokenRepository.count());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
    assertFalse(snapshot().isEnabled());
    assertTrue(snapshot().isVerificationPending());
  }

  @Test
  void standaloneCreationCannotInvalidateAnAlreadyQueuedLink() throws IOException {
    assertTrue(resend("user"));
    String active = activeTokens().getFirst().getToken();
    UserEntity targetUser = snapshot();

    assertThrows(IllegalStateException.class, () -> verification.createToken(targetUser));
    assertEquals(active, activeTokens().getFirst().getToken());
    assertCoherentQueue();
  }

  @Test
  void standaloneCreationAgainstResendKeepsQueueAndTokenCoherentInEitherOrder() throws Exception {
    rendezvous();
    List<Object> results =
        compete(() -> verification.createToken(snapshot()), () -> resend("email"));

    assertEquals(true, results.get(1));
    assertTrue(results.get(0) instanceof String || results.get(0) instanceof IllegalStateException);
    assertCoherentQueue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"token", "email", "user"})
  void queueInsertFailureRollsBackBothTheRealQueueAndTokenRotation(String entry) {
    IllegalStateException failure =
        new IllegalStateException("simulated failure after queue insert");
    doAnswer(
            call -> {
              emailQueueRepository.save(call.getArgument(0));
              emailQueueRepository.flush();
              throw failure;
            })
        .when(probes.emailQueueRepository())
        .save(any());

    assertSame(failure, assertThrows(IllegalStateException.class, () -> resend(entry)));
    assertEquals(0, emailQueueRepository.count());
    assertEquals(1, verificationTokenRepository.count());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
    assertTrue(snapshot().isVerificationPending());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void technicalValidationFailureIsNotConvertedToSuccessOrInvalidLink(boolean optimistic) {
    RuntimeException failure =
        optimistic
            ? new OptimisticLockingFailureException("unrelated optimistic conflict")
            : new IllegalStateException("simulated database failure");
    doThrow(failure)
        .when(probes.verificationTokenRepository())
        .findByTokenAndStatus("old", TokenStatus.ACTIVE);

    assertSame(failure, assertThrows(failure.getClass(), () -> verification.validateToken("old")));
    assertFalse(snapshot().isEnabled());
    assertTrue(snapshot().isVerificationPending());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
  }

  @Test
  void adminGrantThenDisableCannotBeUndoneByAnOldUnusedLinkOrStaleResendUser() throws IOException {
    UserEntity stale = snapshot();
    editEnabled(true);

    assertFalse(snapshot().isVerificationPending());
    editEnabled(false);

    assertFalse(verification.validateToken("old"));
    assertFalse(verification.sendVerificationEmailByToken("old"));
    assertFalse(verification.sendVerificationEmailByEmail("test@example.com"));
    assertFalse(verification.sendVerificationEmailByUser(stale));
    assertThrows(IllegalStateException.class, () -> verification.createToken(stale));
    assertFalse(snapshot().isEnabled());
    assertFalse(snapshot().isVerificationPending());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
    assertEquals(1, verificationTokenRepository.count());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void adminDisableAlsoBlocksAnAccountThatWasNeverEnabled() throws IOException {
    editEnabled(false);

    assertFalse(snapshot().isVerificationPending());
    assertFalse(verification.validateToken("old"));
    assertFalse(resend("user"));
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void adminCreatedDisabledAccountKeepsItsStoredIneligibility() throws IOException {
    UserEntity created =
        UserEntity.builder()
            .firstName("Created")
            .lastName("User")
            .email("created@example.com")
            .password("hash")
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>())
            .enabled(false)
            .verificationPending(false)
            .build();
    admin.saveUser(created);
    tx.executeWithoutResult(status ->
        verificationTokenRepository.saveAndFlush(
            new VerificationTokenEntity(
                null,
                "created-link",
                userRepository.findById(created.getId()).orElseThrow(),
                LocalDateTime.now().plusDays(1),
                TokenStatus.ACTIVE)));

    assertFalse(userRepository.findById(created.getId()).orElseThrow().isVerificationPending());
    assertFalse(verification.validateToken("created-link"));
    assertFalse(verification.sendVerificationEmailByUser(created));
    assertFalse(userRepository.findById(created.getId()).orElseThrow().isEnabled());
    assertEquals(0, emailQueueRepository.count());
  }

  @Test
  void staleManagedAccountIsRefreshedBeforeTheEligibilityDecision() throws Exception {
    tx.executeWithoutResult(status -> {
      UserEntity cached = userRepository.findById(id).orElseThrow();

      assertTrue(cached.isVerificationPending());
      try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
        executorService
            .submit(() -> {
              editEnabled(false);
              return null;
            })
            .get(10, TimeUnit.SECONDS);
      } catch (Exception failure) {
        throw new IllegalStateException(failure);
      }
      assertTrue(cached.isVerificationPending());
      assertFalse(verification.validateToken("old"));
      assertFalse(cached.isVerificationPending());
    });

    assertFalse(snapshot().isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"validate", "token", "email", "user"})
  void committedAdminDisableWinsAgainstAWaitingVerificationOperation(String entry)
      throws Exception {
    CountDownLatch adminHasWritten = new CountDownLatch(1);
    CountDownLatch verificationAttempted = new CountDownLatch(1);
    doAnswer(
            call -> {
              verificationAttempted.countDown();
              return userRepository.lockVerificationUser(call.getArgument(0));
            })
        .when(probes.userRepository())
        .lockVerificationUser(id);
    List<Object> results =
        compete(
            () -> {
              tx.executeWithoutResult(status -> {
                editEnabled(false);
                adminHasWritten.countDown();
                await(verificationAttempted);
              });
              return true;
            },
            () -> {
              await(adminHasWritten);
              return entry.equals("validate") ? verification.validateToken("old") : resend(entry);
            });

    assertEquals(List.of(true, false), results);
    assertFalse(snapshot().isEnabled());
    assertFalse(snapshot().isVerificationPending());
    assertEquals(0, emailQueueRepository.count());
    assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
  }

  @Test
  void activationFirstCannotMakeAFollowingAdminDisableReactivatable() throws Exception {
    CountDownLatch verificationHasLock = new CountDownLatch(1);
    CountDownLatch adminAttempted = new CountDownLatch(1);
    AtomicBoolean first = new AtomicBoolean(true);
    doAnswer(
            call -> {
              Optional<UserEntity> user = userRepository.lockVerificationUser(call.getArgument(0));
              if (first.getAndSet(false)) {
                verificationHasLock.countDown();
                await(adminAttempted);
              }
              return user;
            })
        .when(probes.userRepository())
        .lockVerificationUser(id);
    doAnswer(
            call -> {
              adminAttempted.countDown();
              return userRepository.findByIdForUpdate(call.getArgument(0));
            })
        .when(probes.userRepository())
        .findByIdForUpdate(id);
    List<Object> results =
        compete(
            () -> verification.validateToken("old"),
            () -> {
              await(verificationHasLock);
              tx.executeWithoutResult(status -> {
                UserEntity fresh = probes.userRepository().findByIdForUpdate(id).orElseThrow();
                Long version = fresh.getVersion();
                fresh.setEnabled(false);
                admin.updateAdminUser(fresh, version, "");
              });
              return true;
            });

    assertEquals(List.of(true, true), results);
    assertFalse(snapshot().isEnabled());
    assertFalse(snapshot().isVerificationPending());
    assertEquals(TokenStatus.USED, tokenStatus("old"));
    // Remove synchronization hooks before the sequential regression check.
    reset(probes.userRepository());

    assertFalse(verification.validateToken("old"));
    assertFalse(resend("email"));
  }

  @Test
  void staleAdminFormStillFailsInsteadOfOverwritingCommittedVerification() {
    UserEntity stale = snapshot();
    Long version = stale.getVersion();
    stale.setEnabled(false);

    assertTrue(verification.validateToken("old"));
    assertThrows(
        OptimisticLockingFailureException.class, () -> admin.updateAdminUser(stale, version, ""));
    assertTrue(snapshot().isEnabled());
    editEnabled(false);

    assertFalse(verification.validateToken("old"));
  }

  @Test
  void directQueueProducersAreSerializedAndCannotQueueForBlockedAccounts() throws Exception {
    rendezvous();
    Callable<Object> producer =
        () -> {
          mailQueue.addEmailToQueue(snapshot(), "Subject", "Body", EmailType.VERIFICATION);
          return true;
        };

    assertEquals(List.of(true, true), compete(producer, producer));
    assertEquals(1, emailQueueRepository.count());
    reset(probes.userRepository());
    editEnabled(false);
    tx.executeWithoutResult(status -> emailQueueRepository.deleteAll());
    mailQueue.addEmailToQueue(snapshot(), "Subject", "Body", EmailType.VERIFICATION);

    assertEquals(0, emailQueueRepository.count());
  }

  private void rendezvous() {
    CyclicBarrier barrier = new CyclicBarrier(2);
    Set<Long> threads = ConcurrentHashMap.newKeySet();
    doAnswer(
            call -> {
              if (threads.add(Thread.currentThread().threadId())) {
                barrier.await(10, TimeUnit.SECONDS);
              }
              return userRepository.lockVerificationUser(call.getArgument(0));
            })
        .when(probes.userRepository())
        .lockVerificationUser(id);
  }

  private List<Object> compete(Callable<Object> one, Callable<Object> two) throws Exception {
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Future<Object> first = executorService.submit(() -> capture(one));
      Future<Object> second = executorService.submit(() -> capture(two));
      return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
    }
  }

  private Object capture(Callable<Object> work) {
    try {
      return work.call();
    } catch (Exception failure) {
      return failure;
    }
  }

  private void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(10, TimeUnit.SECONDS), "controlled operation did not arrive");
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(failure);
    }
  }

  private boolean resend(String entry) throws IOException {
    return switch (entry) {
      case "token" -> verification.sendVerificationEmailByToken("old");
      case "email" -> verification.sendVerificationEmailByEmail("test@example.com");
      case "user" -> verification.sendVerificationEmailByUser(snapshot());
      default -> throw new IllegalArgumentException(entry);
    };
  }

  private UserEntity snapshot() {
    return tx.execute(status -> {
      UserEntity testUserEntity = userRepository.findById(id).orElseThrow();
      testUserEntity.getRoleEntities().size();
      return testUserEntity;
    });
  }

  private void editEnabled(boolean enabled) {
    UserEntity edited = snapshot();
    Long version = edited.getVersion();
    edited.setEnabled(enabled);
    admin.updateAdminUser(edited, version, "");
  }

  private TokenStatus tokenStatus(String value) {
    return verificationTokenRepository.findByToken(value).orElseThrow().getStatus();
  }

  private List<VerificationTokenEntity> activeTokens() {
    return tx.execute(status ->
        verificationTokenRepository.findAllByUserEntityAndStatus(snapshot(), TokenStatus.ACTIVE));
  }

  private void assertCoherentQueue() {
    List<VerificationTokenEntity> active = activeTokens();

    assertEquals(1, active.size());
    List<EmailQueueEntity> queued = emailQueueRepository.findAll();

    assertEquals(1, queued.size());
    assertEquals(EmailStatus.PENDING, queued.getFirst().getStatus());
    assertTrue(
        queued.getFirst().getBody().contains("/verification/" + active.getFirst().getToken()));
    assertFalse(snapshot().isEnabled());
    assertTrue(snapshot().isVerificationPending());
  }

  record Probes(
      UserRepository userRepository,
      VerificationTokenRepository verificationTokenRepository,
      EmailQueueRepository emailQueueRepository) {}

  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  static class Config {
    @Bean
    PendingEmailVerificationFlow pendingEmailVerificationFlow(
        Probes probes, VerificationService verificationService) {
      return new PendingEmailVerificationFlow(probes.userRepository(), verificationService);
    }

    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          "jdbc:h2:mem:verification-queue-"
              + UUID.randomUUID()
              + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
          "sa",
          "");
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(dataSource);
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
    Probes probes(
        UserRepository userRepository,
        VerificationTokenRepository verificationTokenRepository,
        EmailQueueRepository emailQueueRepository) {
      return new Probes(
          mock(UserRepository.class, delegatesTo(userRepository)),
          mock(VerificationTokenRepository.class, delegatesTo(verificationTokenRepository)),
          mock(EmailQueueRepository.class, delegatesTo(emailQueueRepository)));
    }

    @Bean
    ConfigService configService() {
      ConfigService config = mock(ConfigService.class);
      when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
      when(config.getString(ConfigEntry.COPYRIGHT_TEXT))
          .thenReturn("© {year} {serviceName} (isolated fixture)");
      when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
      when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/");
      return config;
    }

    @Bean
    EmailQueueService emailQueueService(
        ConfigService config, Probes probes, JpaTransactionManager manager) {
      return new EmailQueueService(
          config,
          probes.emailQueueRepository(),
          mock(EmailService.class),
          probes.userRepository(),
          manager,
          mock(de.derpeterson.app.repository.AdminNotificationAttemptRepository.class),
          QueueTestWorkers.inline());
    }

    @Bean
    VerificationService verificationService(
        ConfigService config, Probes probes, EmailQueueService queue) {
      MessageProperties messages = mock(MessageProperties.class);
      when(messages.getEmailVerificationSubject()).thenReturn("Verify account");
      return new VerificationService(
          messages, probes.verificationTokenRepository(), probes.userRepository(), config, queue);
    }

    @Bean
    UserService userService(Probes probes) {
      return new UserService(
          probes.userRepository(),
          mock(UserStatusBroadcaster.class),
          mock(PasswordEncoder.class),
          mock(UserProfileBroadcaster.class),
          mock(de.derpeterson.app.security.AdminRolePolicy.class),
          mock(org.springframework.context.ApplicationEventPublisher.class));
    }
  }
}
