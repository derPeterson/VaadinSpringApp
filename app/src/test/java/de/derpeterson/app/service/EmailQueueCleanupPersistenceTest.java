package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.AccountDeactivation;
import de.derpeterson.app.model.EmailQueueCleanupRunEntity;
import de.derpeterson.app.model.EmailQueueCleanupStateEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailQueueCleanupStatus;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.EmailQueueCleanupRunRepository;
import de.derpeterson.app.repository.EmailQueueCleanupStateRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Eigene H2-Daten, echte Transaktionen und kontrollierte Zeit; kein Scheduler oder SMTP. */
@SpringJUnitConfig({
  EmailQueueServicePersistenceTest.Config.class,
  EmailQueueCleanupPersistenceTest.CleanupConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailQueueCleanupPersistenceTest {
  @Autowired private EmailQueueCleanupService emailQueueCleanupService;
  @Autowired private EmailQueueRepository emailQueueRepository;
  @Autowired private EmailQueueCleanupRunRepository emailQueueCleanupRunRepository;
  @Autowired private EmailQueueCleanupStateRepository emailQueueCleanupStateRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private AdminRolePolicy adminRolePolicy;
  @Autowired private ConfigService configService;
  @Autowired private MutableQueueClock cleanupClock;
  @Autowired private EmailQueueDispatchGuard dispatchGuard;
  @Autowired private PlatformTransactionManager transactionManager;
  private UserEntity accountUserEntity;

  @BeforeEach
  void createIsolatedAccountAndRunState() {
    reset(adminRolePolicy, configService);
    cleanupClock.set(Instant.parse("2030-01-01T12:00:00Z"));
    emailQueueRepository.deleteAll();
    emailQueueCleanupRunRepository.deleteAll();
    emailQueueCleanupStateRepository.deleteAll();
    userRepository.deleteAll();
    emailQueueCleanupStateRepository.saveAndFlush(new EmailQueueCleanupStateEntity());
    accountUserEntity =
        userRepository.saveAndFlush(
            UserEntity.builder()
                .email("cleanup@example.test")
                .firstName("Queue")
                .lastName("Test")
                .enabled(true)
                .build());
    configureCleanup(false, "PT15M", "PT30M");
    when(adminRolePolicy.currentSuperadminId()).thenReturn(accountUserEntity.getId());
  }

  private void configureCleanup(boolean enabled, String interval, String minimumAge) {
    when(configService.snapshot(anyList()))
        .thenReturn(
            Map.of(
                ConfigEntry.EMAIL_QUEUE_CLEANUP_ENABLED, Boolean.toString(enabled),
                ConfigEntry.EMAIL_QUEUE_CLEANUP_INTERVAL, interval,
                ConfigEntry.EMAIL_QUEUE_CLEANUP_MINIMUM_AGE, minimumAge));
  }

  @Test
  void minimumAgeUsesLastClaimAndIncludesExactBoundary() {
    EmailQueueEntity eligibleEmail = createRequest(EmailStatus.IN_PROGRESS, Duration.ofMinutes(30));
    EmailQueueEntity freshClaimEmail =
        createRequest(EmailStatus.IN_PROGRESS, Duration.ofMinutes(29));
    freshClaimEmail.setCreatedAt(LocalDateTime.now(cleanupClock).minusDays(5));
    emailQueueRepository.saveAndFlush(freshClaimEmail);
    EmailQueueEntity pendingEmail = createRequest(EmailStatus.PENDING, Duration.ofHours(2));
    EmailQueueEntity sentEmail = createRequest(EmailStatus.SENT, Duration.ofHours(2));

    EmailQueueCleanupService.CleanupResult result = emailQueueCleanupService.runManually();

    assertEquals(1, result.resolvedCount());
    assertEquals(EmailQueueCleanupStatus.COMPLETED, result.status());
    assertEquals(EmailStatus.UNKNOWN, storedStatus(eligibleEmail));
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(freshClaimEmail));
    assertEquals(EmailStatus.PENDING, storedStatus(pendingEmail));
    assertEquals(EmailStatus.SENT, storedStatus(sentEmail));
    assertEquals(4, emailQueueRepository.count());
    assertEquals(result, emailQueueCleanupService.latestResult());
  }

  @Test
  void disabledAutomationDoesNothingButManualCleanupStillWorks() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    emailQueueCleanupService.runAutomatically();
    assertNull(emailQueueCleanupService.latestResult());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));

    assertEquals(1, emailQueueCleanupService.runManually().resolvedCount());
  }

  @Test
  void automaticIntervalAndStoredResultSurviveServiceRecreation() {
    configureCleanup(true, "PT15M", "PT30M");
    createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    emailQueueCleanupService.runAutomatically();
    EmailQueueCleanupService.CleanupResult firstResult = emailQueueCleanupService.latestResult();
    assertEquals(1, firstResult.resolvedCount());
    EmailQueueCleanupService recreatedCleanupService =
        new EmailQueueCleanupService(
            emailQueueRepository,
            emailQueueCleanupStateRepository,
            emailQueueCleanupRunRepository,
            configService,
            adminRolePolicy,
            dispatchGuard,
            transactionManager,
            cleanupClock,
            Runnable::run);
    assertEquals(firstResult, recreatedCleanupService.latestResult());
    EmailQueueEntity nextEmail = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    cleanupClock.advance(Duration.ofMinutes(14));
    recreatedCleanupService.runAutomatically();
    assertEquals(firstResult, recreatedCleanupService.latestResult());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(nextEmail));

    cleanupClock.advance(Duration.ofMinutes(1));
    recreatedCleanupService.runAutomatically();
    assertEquals(EmailStatus.UNKNOWN, storedStatus(nextEmail));
    assertEquals(
        LocalDateTime.now(cleanupClock), emailQueueCleanupService.latestResult().finishedAt());
  }

  @Test
  void aRunningTransportKeepsTheRequestUntilTheSchedulerCanExecuteIt() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    dispatchGuard.runDispatch(() ->
        assertEquals(
            EmailQueueCleanupStatus.WAITING, emailQueueCleanupService.runManually().status()));
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertEquals(0, emailQueueCleanupService.latestResult().resolvedCount());

    emailQueueCleanupService.runAutomatically();
    assertEquals(1, emailQueueCleanupService.latestResult().resolvedCount());
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Test
  void roleRevocationBeforeTheFirstBatchFailsTheQueuedRunWithoutChangingClaims() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    doNothing()
        .doThrow(new AccessDeniedException("Role revoked"))
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    assertEquals(EmailQueueCleanupStatus.FAILED, emailQueueCleanupService.runManually().status());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Test
  void ordinaryAdminCannotReadOrStartCleanup() {
    doThrow(new AccessDeniedException("Denied")).when(adminRolePolicy).requireSuperadmin();
    when(adminRolePolicy.currentSuperadminId()).thenThrow(new AccessDeniedException("Denied"));
    assertThrows(AccessDeniedException.class, emailQueueCleanupService::runManually);
    assertThrows(AccessDeniedException.class, emailQueueCleanupService::latestResult);
  }

  @Test
  void failedCommitRollsBackClaimsAndRunState() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    doAnswer(
            invocation -> {
              if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                      @Override
                      public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("Controlled commit rejection");
                      }
                    });
              }
              return null;
            })
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());

    assertThrows(IllegalStateException.class, emailQueueCleanupService::runManually);
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertNull(emailQueueCleanupStateRepository.findById(1L).orElseThrow().getLastRunAt());
    assertEquals(0, emailQueueCleanupRunRepository.count());
  }

  @Test
  void deletionConfirmationDoesNotRetainDeletedAccountContent() {
    EmailQueueEntity deletionEmail = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    deletionEmail.setUserEntity(null);
    deletionEmail.setEmailType(EmailType.ACCOUNT_DELETED);
    emailQueueRepository.saveAndFlush(deletionEmail);

    assertEquals(1, emailQueueCleanupService.runManually().resolvedCount());
    assertFalse(emailQueueRepository.existsById(deletionEmail.getId()));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100, 101, 250})
  void allBatchesBelongToOneLogicalRun(int requestCount) {
    for (int requestIndex = 0; requestIndex < requestCount; requestIndex++) {
      createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    }
    EmailQueueCleanupService.CleanupResult result = emailQueueCleanupService.runManually();
    assertEquals(requestCount, result.resolvedCount());
    assertEquals(EmailQueueCleanupStatus.COMPLETED, result.status());
    assertEquals(1, emailQueueCleanupRunRepository.count());
    assertEquals(
        requestCount,
        emailQueueRepository.findAll().stream()
            .filter(entry -> entry.getStatus() == EmailStatus.UNKNOWN)
            .count());
  }

  @Test
  void historyRecordsLogicalRunsAndDoesNotInventSkippedSchedulerRuns() {
    configureCleanup(true, "PT15M", "PT30M");
    emailQueueCleanupService.runManually();
    emailQueueCleanupService.runAutomatically();
    assertEquals(1, emailQueueCleanupRunRepository.count());
    assertFalse(emailQueueCleanupRunRepository.findAll().getFirst().isAutomatic());

    cleanupClock.advance(Duration.ofMinutes(15));
    emailQueueCleanupService.runAutomatically();
    assertEquals(2, emailQueueCleanupRunRepository.count());
    assertEquals(
        1,
        emailQueueCleanupRunRepository.findAll().stream().filter(run -> run.isAutomatic()).count());

    dispatchGuard.runDispatch(() -> emailQueueCleanupService.runManually());
    assertEquals(3, emailQueueCleanupRunRepository.count());
    assertEquals(EmailQueueCleanupStatus.WAITING, emailQueueCleanupService.latestResult().status());
    emailQueueCleanupService.runAutomatically();
    assertEquals(3, emailQueueCleanupRunRepository.count());
    assertEquals(
        EmailQueueCleanupStatus.COMPLETED, emailQueueCleanupService.latestResult().status());
  }

  @Test
  void retentionRemovesOnlyOlderRunsAndPreservesLatestState() {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CLEANUP_HISTORY_DAYS)).thenReturn(30);
    emailQueueCleanupService.runManually();
    cleanupClock.advance(Duration.ofDays(30));
    emailQueueCleanupService.deleteExpiredRunHistory();
    assertEquals(1, emailQueueCleanupRunRepository.count());

    cleanupClock.advance(Duration.ofSeconds(1));
    emailQueueCleanupService.runManually();
    EmailQueueCleanupService.CleanupResult latestResult = emailQueueCleanupService.latestResult();
    emailQueueCleanupService.deleteExpiredRunHistory();
    assertEquals(1, emailQueueCleanupRunRepository.count());
    assertEquals(
        LocalDateTime.now(cleanupClock),
        emailQueueCleanupRunRepository.findAll().getFirst().getAttemptedAt());
    assertEquals(latestResult, emailQueueCleanupService.latestResult());

    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CLEANUP_HISTORY_DAYS)).thenReturn(0);
    assertThrows(IllegalArgumentException.class, emailQueueCleanupService::deleteExpiredRunHistory);
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  private EmailQueueEntity createRequest(EmailStatus status, Duration claimAge) {
    EmailQueueEntity queueEntry = new EmailQueueEntity();
    queueEntry.setUserEntity(accountUserEntity);
    queueEntry.setRecipient(accountUserEntity.getEmail());
    queueEntry.setSubject("Synthetic request");
    queueEntry.setBody("Synthetic body");
    queueEntry.setEmailType(EmailType.VERIFICATION);
    queueEntry.setStatus(status);
    queueEntry.setProcessingStartedAt(LocalDateTime.now(cleanupClock).minus(claimAge));
    return emailQueueRepository.saveAndFlush(queueEntry);
  }

  private EmailStatus storedStatus(EmailQueueEntity queueEntry) {
    return emailQueueRepository.findById(queueEntry.getId()).orElseThrow().getStatus();
  }

  @Test
  void repeatedRequestsJoinThePersistedJobAndRestartResumesItWithoutAutomation() {
    createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    List<Runnable> submittedTasks = new ArrayList<>();
    EmailQueueCleanupService deferredService = recreateService(submittedTasks::add);
    EmailQueueCleanupService.CleanupResult waitingResult = deferredService.runManually();
    assertEquals(waitingResult.runId(), deferredService.runManually().runId());
    assertEquals(1, submittedTasks.size());
    assertEquals(EmailQueueCleanupStatus.WAITING, waitingResult.status());

    recreateService(Runnable::run).runAutomatically();
    assertEquals(EmailQueueCleanupStatus.COMPLETED, deferredService.latestResult().status());
    submittedTasks.getFirst().run();
    assertEquals(1, deferredService.latestResult().resolvedCount());
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Test
  void rejectedExecutorSubmissionRemainsDurableForTheScheduler() {
    AtomicInteger submissions = new AtomicInteger();
    EmailQueueCleanupService deferredService =
        recreateService(command -> {
          if (submissions.getAndIncrement() == 0) {
            throw new RejectedExecutionException("Controlled temporary rejection");
          }
          command.run();
        });
    assertEquals(EmailQueueCleanupStatus.WAITING, deferredService.runManually().status());
    deferredService.runAutomatically();
    assertEquals(EmailQueueCleanupStatus.COMPLETED, deferredService.latestResult().status());
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Test
  void revokedPendingActorFailsInsteadOfUsingTheWorkersMissingPrincipal() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    List<Runnable> submittedTasks = new ArrayList<>();
    recreateService(submittedTasks::add).runManually();
    doThrow(new AccessDeniedException("Role revoked"))
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    submittedTasks.getFirst().run();
    assertEquals(EmailQueueCleanupStatus.FAILED, emailQueueCleanupService.latestResult().status());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertNull(emailQueueCleanupRunRepository.findAll().getFirst().getRequestedById());
  }

  @Test
  void laterBatchRollbackRetainsCommittedCountAndAnExplicitRetryHandlesTheRest() {
    for (int index = 0; index < 250; index++) {
      createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    }
    AtomicInteger authorizationChecks = new AtomicInteger();
    doAnswer(
            invocation -> {
              if (authorizationChecks.incrementAndGet() == 3) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                      @Override
                      public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("Controlled second batch commit rejection");
                      }
                    });
              }
              return null;
            })
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    EmailQueueCleanupService.CleanupResult partialResult = emailQueueCleanupService.runManually();
    assertEquals(EmailQueueCleanupStatus.PARTIAL, partialResult.status());
    assertEquals(100, partialResult.resolvedCount());
    assertEquals(
        150,
        emailQueueRepository.findAll().stream()
            .filter(entry -> entry.getStatus() == EmailStatus.IN_PROGRESS)
            .count());

    doNothing().when(adminRolePolicy).requireSuperadmin(accountUserEntity.getId());
    assertEquals(150, emailQueueCleanupService.runManually().resolvedCount());
    assertEquals(100, emailQueueCleanupService.resultFor(partialResult.runId()).resolvedCount());
    assertEquals(2, emailQueueCleanupRunRepository.count());
  }

  @Test
  void batchCommitFailureProducesAnHonestFailedResultAndRollsBackItsClaims() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    AtomicInteger authorizationChecks = new AtomicInteger();
    doAnswer(
            invocation -> {
              if (authorizationChecks.incrementAndGet() == 2) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                      @Override
                      public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("Controlled batch commit failure");
                      }
                    });
              }
              return null;
            })
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    EmailQueueCleanupService.CleanupResult result = emailQueueCleanupService.runManually();
    assertEquals(EmailQueueCleanupStatus.FAILED, result.status());
    assertEquals(0, result.resolvedCount());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
  }

  @Test
  void interruptedWorkerResumesTheSameRunAfterItsCommittedBatch() {
    for (int index = 0; index < 250; index++) {
      createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    }
    AtomicInteger authorizationChecks = new AtomicInteger();
    doAnswer(
            invocation -> {
              if (authorizationChecks.incrementAndGet() == 2) {
                Thread.currentThread().interrupt();
              }
              return null;
            })
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    EmailQueueCleanupService.CleanupResult interruptedResult;
    try {
      interruptedResult = emailQueueCleanupService.runManually();
    } finally {
      Thread.interrupted();
    }
    assertEquals(EmailQueueCleanupStatus.RUNNING, interruptedResult.status());
    assertEquals(100, interruptedResult.resolvedCount());
    recreateService(Runnable::run).runAutomatically();
    assertEquals(interruptedResult.runId(), emailQueueCleanupService.latestResult().runId());
    assertEquals(250, emailQueueCleanupService.latestResult().resolvedCount());
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Test
  void theFirstBatchFreezesAgeAndHighWaterMarkForTheEntireRun() {
    for (int index = 0; index < 101; index++) {
      createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    }
    EmailQueueEntity youngClaim = createRequest(EmailStatus.IN_PROGRESS, Duration.ofMinutes(29));
    List<EmailQueueEntity> newClaims = new ArrayList<>();
    AtomicInteger authorizationChecks = new AtomicInteger();
    doAnswer(
            invocation -> {
              if (authorizationChecks.incrementAndGet() == 3) {
                cleanupClock.advance(Duration.ofHours(1));
                configureCleanup(false, "PT15M", "PT1M");
                newClaims.add(createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(2)));
              }
              return null;
            })
        .when(adminRolePolicy)
        .requireSuperadmin(accountUserEntity.getId());
    assertEquals(101, emailQueueCleanupService.runManually().resolvedCount());
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(youngClaim));
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(newClaims.getFirst()));
  }

  private EmailQueueCleanupService recreateService(Executor executor) {
    return new EmailQueueCleanupService(
        emailQueueRepository,
        emailQueueCleanupStateRepository,
        emailQueueCleanupRunRepository,
        configService,
        adminRolePolicy,
        dispatchGuard,
        transactionManager,
        cleanupClock,
        executor);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"disabled", "blocked", "self-deactivated", "pending", "revoked", "deleted"})
  void storedActorAvailabilityIsCheckedFreshWithoutAnyWorkerPrincipal(String unavailableReason) {
    RoleEntity superadminRole =
        roleRepository
            .findByName(RoleType.ROLE_SUPERADMIN)
            .orElseGet(() ->
                roleRepository.saveAndFlush(
                    RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build()));
    accountUserEntity.setRoleEntities(new ArrayList<>(List.of(superadminRole)));
    accountUserEntity = userRepository.saveAndFlush(accountUserEntity);
    AdminRolePolicy freshPolicy = new AdminRolePolicy(userRepository);
    Long requestingAccountId = accountUserEntity.getId();
    assertDoesNotThrow(() -> freshPolicy.requireSuperadmin(requestingAccountId));

    switch (unavailableReason) {
      case "disabled" -> accountUserEntity.setEnabled(false);
      case "blocked" -> accountUserEntity.setAdministrativelyBlocked(true);
      case "self-deactivated" ->
          accountUserEntity.setAccountDeactivation(
              new AccountDeactivation(
                  cleanupClock.instant().plus(Duration.ofDays(1)), null, null, null, null, null));
      case "pending" ->
          accountUserEntity.setEmailVerificationStatus(EmailVerificationStatus.PENDING);
      case "revoked" -> accountUserEntity.setRoleEntities(new ArrayList<>());
      case "deleted" -> userRepository.deleteById(accountUserEntity.getId());
      default -> throw new IllegalArgumentException("Unknown fixture state");
    }
    if (!unavailableReason.equals("deleted")) {
      userRepository.saveAndFlush(accountUserEntity);
    }
    assertThrows(
        AccessDeniedException.class, () -> freshPolicy.requireSuperadmin(requestingAccountId));
  }

  @Test
  void concurrentManualRequestsCreateOnlyOnePersistedRunAndOneWorkerTask() throws Exception {
    createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    ConcurrentLinkedQueue<Runnable> submittedTasks = new ConcurrentLinkedQueue<>();
    EmailQueueCleanupService deferredService = recreateService(submittedTasks::add);
    CountDownLatch startRequests = new CountDownLatch(1);
    try (ExecutorService requestThreads = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<EmailQueueCleanupService.CleanupResult> firstRequest =
          requestThreads.submit(() -> {
            if (!startRequests.await(10, TimeUnit.SECONDS)) {
              throw new IllegalStateException("Fixture requests did not start");
            }
            return deferredService.runManually();
          });
      Future<EmailQueueCleanupService.CleanupResult> secondRequest =
          requestThreads.submit(() -> {
            if (!startRequests.await(10, TimeUnit.SECONDS)) {
              throw new IllegalStateException("Fixture requests did not start");
            }
            return deferredService.runManually();
          });
      startRequests.countDown();
      assertEquals(
          firstRequest.get(10, TimeUnit.SECONDS).runId(),
          secondRequest.get(10, TimeUnit.SECONDS).runId());
    }
    assertEquals(1, submittedTasks.size());
    assertEquals(1, emailQueueCleanupRunRepository.count());
    submittedTasks.remove().run();
    assertEquals(1, emailQueueCleanupService.latestResult().resolvedCount());
  }

  @Test
  void nonAdvancingCandidateWindowFailsInsteadOfSpinningForever() {
    EmailQueueRepository nonAdvancingRepository =
        mock(EmailQueueRepository.class, delegatesTo(emailQueueRepository));
    doReturn(List.of(0L))
        .when(nonAdvancingRepository)
        .findUnresolvedWindowIds(any(), anyLong(), anyLong(), any());
    EmailQueueCleanupService stalledService =
        new EmailQueueCleanupService(
            nonAdvancingRepository,
            emailQueueCleanupStateRepository,
            emailQueueCleanupRunRepository,
            configService,
            adminRolePolicy,
            dispatchGuard,
            transactionManager,
            cleanupClock,
            Runnable::run);
    assertEquals(EmailQueueCleanupStatus.FAILED, stalledService.runManually().status());
    assertEquals(0, stalledService.latestResult().resolvedCount());
    verify(nonAdvancingRepository, times(1))
        .findUnresolvedWindowIds(any(), anyLong(), anyLong(), any());
  }

  @Test
  void aCandidateRemovedBeforeItsLockDoesNotStallOrInflateTheResult() {
    EmailQueueEntity candidate = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    EmailQueueRepository racedRepository =
        mock(EmailQueueRepository.class, delegatesTo(emailQueueRepository));
    doAnswer(
            invocation -> {
              emailQueueRepository.deleteById(candidate.getId());
              return Optional.empty();
            })
        .when(racedRepository)
        .lockById(candidate.getId());
    EmailQueueCleanupService racedService =
        new EmailQueueCleanupService(
            racedRepository,
            emailQueueCleanupStateRepository,
            emailQueueCleanupRunRepository,
            configService,
            adminRolePolicy,
            dispatchGuard,
            transactionManager,
            cleanupClock,
            Runnable::run);
    EmailQueueCleanupService.CleanupResult result = racedService.runManually();
    assertEquals(EmailQueueCleanupStatus.COMPLETED, result.status());
    assertEquals(0, result.resolvedCount());
    assertFalse(emailQueueRepository.existsById(candidate.getId()));
  }

  @Test
  void aDueAutomaticCheckDoesNotOverwriteOrDuplicateThePendingManualJob() {
    List<Runnable> submittedTasks = new ArrayList<>();
    EmailQueueCleanupService deferredService = recreateService(submittedTasks::add);
    EmailQueueCleanupService.CleanupResult pending = deferredService.runManually();
    configureCleanup(true, "PT1M", "PT30M");
    cleanupClock.advance(Duration.ofMinutes(2));
    deferredService.runAutomatically();
    assertEquals(pending, deferredService.latestResult());
    assertEquals(1, submittedTasks.size());
    assertEquals(1, emailQueueCleanupRunRepository.count());
    submittedTasks.getFirst().run();
    assertEquals(pending.runId(), deferredService.latestResult().runId());
    assertFalse(deferredService.latestResult().automatic());
  }

  @Test
  void anUnrecordableWorkerFailureKeepsTheJobDurableAndBacksOffBeforeRetrying() {
    createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    EmailQueueRepository failingQueueRepository =
        mock(EmailQueueRepository.class, delegatesTo(emailQueueRepository));
    EmailQueueCleanupRunRepository failingRunRepository =
        mock(EmailQueueCleanupRunRepository.class, delegatesTo(emailQueueCleanupRunRepository));
    doThrow(new IllegalStateException("Controlled candidate read failure"))
        .when(failingQueueRepository)
        .findUnresolvedWindowIds(any(), anyLong(), anyLong(), any());
    doAnswer(
            invocation -> {
              EmailQueueCleanupRunEntity run = invocation.getArgument(0);
              if (run.getStatus() == EmailQueueCleanupStatus.FAILED) {
                throw new IllegalStateException("Controlled failure journal rejection");
              }
              return emailQueueCleanupRunRepository.saveAndFlush(run);
            })
        .when(failingRunRepository)
        .saveAndFlush(any());
    List<Runnable> submittedTasks = new ArrayList<>();
    EmailQueueCleanupService recoveringService =
        new EmailQueueCleanupService(
            failingQueueRepository,
            emailQueueCleanupStateRepository,
            failingRunRepository,
            configService,
            adminRolePolicy,
            dispatchGuard,
            transactionManager,
            cleanupClock,
            submittedTasks::add);
    recoveringService.runManually();
    submittedTasks.getFirst().run();
    assertEquals(EmailQueueCleanupStatus.WAITING, recoveringService.latestResult().status());
    recoveringService.runAutomatically();
    cleanupClock.advance(Duration.ofSeconds(59));
    recoveringService.runAutomatically();
    assertEquals(1, submittedTasks.size());

    reset(failingQueueRepository, failingRunRepository);
    cleanupClock.advance(Duration.ofSeconds(1));
    recoveringService.runAutomatically();
    assertEquals(2, submittedTasks.size());
    submittedTasks.get(1).run();
    assertEquals(EmailQueueCleanupStatus.COMPLETED, recoveringService.latestResult().status());
    assertEquals(1, recoveringService.latestResult().resolvedCount());
    assertEquals(1, emailQueueCleanupRunRepository.count());
  }

  @Configuration
  static class CleanupConfig {
    @Bean
    AdminRolePolicy adminRolePolicy() {
      return mock(AdminRolePolicy.class);
    }

    @Bean
    EmailQueueDispatchGuard dispatchGuard() {
      return new EmailQueueDispatchGuard(null);
    }

    @Bean
    EmailQueueCleanupService emailQueueCleanupService(
        EmailQueueRepository queueRepository,
        EmailQueueCleanupStateRepository stateRepository,
        EmailQueueCleanupRunRepository emailQueueCleanupRunRepository,
        ConfigService configService,
        AdminRolePolicy adminRolePolicy,
        EmailQueueDispatchGuard dispatchGuard,
        PlatformTransactionManager transactionManager,
        MutableQueueClock cleanupClock) {
      return new EmailQueueCleanupService(
          queueRepository,
          stateRepository,
          emailQueueCleanupRunRepository,
          configService,
          adminRolePolicy,
          dispatchGuard,
          transactionManager,
          cleanupClock,
          Runnable::run);
    }
  }
}
