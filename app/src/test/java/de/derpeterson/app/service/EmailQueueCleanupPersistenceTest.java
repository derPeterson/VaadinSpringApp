package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.EmailQueueCleanupStateEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.EmailQueueCleanupRunRepository;
import de.derpeterson.app.repository.EmailQueueCleanupStateRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    assertFalse(result.busy());
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
            cleanupClock);
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
  void aRunningTransportIsSkippedWithoutChangingAnyClaim() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    dispatchGuard.runDispatch(() -> assertTrue(emailQueueCleanupService.runManually().busy()));
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertEquals(0, emailQueueCleanupService.latestResult().resolvedCount());

    assertEquals(1, emailQueueCleanupService.runManually().resolvedCount());
  }

  @Test
  void roleRevocationInsideTransactionPreventsCleanupAndRunStateChange() {
    EmailQueueEntity queueEntry = createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    doNothing()
        .doThrow(new AccessDeniedException("Role revoked"))
        .when(adminRolePolicy)
        .requireSuperadmin();
    assertThrows(AccessDeniedException.class, emailQueueCleanupService::runManually);
    assertEquals(EmailStatus.IN_PROGRESS, storedStatus(queueEntry));
    assertNull(emailQueueCleanupStateRepository.findById(1L).orElseThrow().getLastRunAt());
    assertEquals(0, emailQueueCleanupRunRepository.count());
  }

  @Test
  void ordinaryAdminCannotReadOrStartCleanup() {
    doThrow(new AccessDeniedException("Denied")).when(adminRolePolicy).requireSuperadmin();
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
        .requireSuperadmin();

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

  @Test
  void boundedBatchLeavesRemainingRequestsForTheNextRun() {
    for (int requestIndex = 0; requestIndex < 101; requestIndex++) {
      createRequest(EmailStatus.IN_PROGRESS, Duration.ofHours(1));
    }
    assertEquals(100, emailQueueCleanupService.runManually().resolvedCount());
    assertEquals(1, emailQueueCleanupService.runManually().resolvedCount());
    assertEquals(0, emailQueueCleanupService.runManually().resolvedCount());
  }

  @Test
  void historyRecordsManualAutomaticAndBusyAttemptsButNotSchedulerChecks() {
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
    assertEquals(
        1, emailQueueCleanupRunRepository.findAll().stream().filter(run -> run.isBusy()).count());
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
          cleanupClock);
    }
  }
}
