package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.model.EmailQueueCleanupRunEntity;
import de.derpeterson.app.model.EmailQueueCleanupStateEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailQueueCleanupStatus;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.EmailQueueCleanupRunRepository;
import de.derpeterson.app.repository.EmailQueueCleanupStateRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Dauerhafte Gesamtläufe mit kurzen Pakettransaktionen, ohne Neuversand oder Zustellbehauptung. */
@Service
public class EmailQueueCleanupService {
  private static final Logger logger = LoggerFactory.getLogger(EmailQueueCleanupService.class);
  private static final int BATCH_SIZE = 100;
  private static final List<ConfigEntry> CLEANUP_CONFIG_ENTRIES =
      List.of(
          ConfigEntry.EMAIL_QUEUE_CLEANUP_ENABLED,
          ConfigEntry.EMAIL_QUEUE_CLEANUP_INTERVAL,
          ConfigEntry.EMAIL_QUEUE_CLEANUP_MINIMUM_AGE);

  private final EmailQueueRepository emailQueueRepository;
  private final EmailQueueCleanupStateRepository cleanupStateRepository;
  private final EmailQueueCleanupRunRepository cleanupRunRepository;
  private final ConfigService configService;
  private final AdminRolePolicy adminRolePolicy;
  private final EmailQueueDispatchGuard dispatchGuard;
  private final TransactionTemplate cleanupTransactionTemplate;
  private final Clock cleanupClock;
  private final Executor cleanupExecutor;
  private final AtomicBoolean cleanupTaskSubmitted = new AtomicBoolean();
  private volatile Instant nextSchedulerAttempt = Instant.MIN;

  public record CleanupResult(
      Long runId,
      EmailQueueCleanupStatus status,
      LocalDateTime requestedAt,
      LocalDateTime startedAt,
      LocalDateTime finishedAt,
      long resolvedCount,
      boolean automatic)
      implements Serializable {
    public boolean active() {
      return status.isActive();
    }
  }

  private record CleanupSettings(
      boolean automaticCleanupEnabled,
      Duration runIntervalDuration,
      Duration minimumClaimAgeDuration) {}

  private record CleanupRequest(CleanupResult result, boolean created) {}

  @Autowired
  public EmailQueueCleanupService(
      EmailQueueRepository emailQueueRepository,
      EmailQueueCleanupStateRepository cleanupStateRepository,
      EmailQueueCleanupRunRepository cleanupRunRepository,
      ConfigService configService,
      AdminRolePolicy adminRolePolicy,
      EmailQueueDispatchGuard dispatchGuard,
      PlatformTransactionManager transactionManager,
      @Qualifier("emailQueueCleanupExecutor") Executor cleanupExecutor) {
    this(
        emailQueueRepository,
        cleanupStateRepository,
        cleanupRunRepository,
        configService,
        adminRolePolicy,
        dispatchGuard,
        transactionManager,
        Clock.systemDefaultZone(),
        cleanupExecutor);
  }

  EmailQueueCleanupService(
      EmailQueueRepository emailQueueRepository,
      EmailQueueCleanupStateRepository cleanupStateRepository,
      EmailQueueCleanupRunRepository cleanupRunRepository,
      ConfigService configService,
      AdminRolePolicy adminRolePolicy,
      EmailQueueDispatchGuard dispatchGuard,
      PlatformTransactionManager transactionManager,
      Clock cleanupClock,
      Executor cleanupExecutor) {
    this.emailQueueRepository = emailQueueRepository;
    this.cleanupStateRepository = cleanupStateRepository;
    this.cleanupRunRepository = cleanupRunRepository;
    this.configService = configService;
    this.adminRolePolicy = adminRolePolicy;
    this.dispatchGuard = dispatchGuard;
    this.cleanupClock = cleanupClock;
    this.cleanupExecutor = cleanupExecutor;
    cleanupTransactionTemplate = new TransactionTemplate(transactionManager);
    cleanupTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public boolean canManageCleanup() {
    return adminRolePolicy.canManageSuperadmins();
  }

  @Transactional(readOnly = true)
  public @Nullable CleanupResult latestResult() {
    adminRolePolicy.requireSuperadmin();
    return currentResult(cleanupStateRepository.findById(1L).orElseThrow());
  }

  @Transactional(readOnly = true)
  public @Nullable CleanupResult resultFor(Long runId) {
    adminRolePolicy.requireSuperadmin();
    return cleanupRunRepository.findById(runId).map(this::resultOf).orElse(null);
  }

  /** Der bestätigte Auftrag ist vor der Hintergrundübergabe bereits dauerhaft gespeichert. */
  public CleanupResult runManually() {
    Long requestingAccountId = adminRolePolicy.currentSuperadminId();
    CleanupSettings cleanupSettings = readSettings();
    CleanupRequest cleanupRequest =
        cleanupTransactionTemplate.execute(transactionStatus -> {
          adminRolePolicy.requireSuperadmin(requestingAccountId);
          EmailQueueCleanupStateEntity cleanupState = cleanupStateRepository.lockState();
          CleanupResult activeResult = currentResult(cleanupState);
          if (activeResult != null && activeResult.active()) {
            return new CleanupRequest(activeResult, false);
          }
          return new CleanupRequest(
              createRun(cleanupState, cleanupSettings, requestingAccountId), true);
        });
    Objects.requireNonNull(cleanupRequest);
    if (cleanupRequest.created()) {
      submitCleanupRun(cleanupRequest.result().runId());
    }
    return cleanupTransactionTemplate.execute(transactionStatus ->
        resultOf(cleanupRunRepository.findById(cleanupRequest.result().runId()).orElseThrow()));
  }

  private void submitCleanupRun(Long runId) {
    if (!cleanupTaskSubmitted.compareAndSet(false, true)) {
      return;
    }
    try {
      cleanupExecutor.execute(() -> {
        try {
          executeRunWhenIdle(runId);
        } catch (RuntimeException cleanupFailure) {
          // Der persistente Auftrag bleibt bei nicht speicherbarem Ausgang wiederaufnehmbar.
          nextSchedulerAttempt = cleanupClock.instant().plusSeconds(60);
          logger.error(
              "Mail queue cleanup could not progress", LogSafety.safeFailure(cleanupFailure));
        } finally {
          cleanupTaskSubmitted.set(false);
        }
      });
    } catch (RejectedExecutionException _) {
      cleanupTaskSubmitted.set(false);
      // Etwa beim Herunterfahren: die dauerhafte Vormerkung übernimmt der nächste Scheduler.
      logger.debug("Mail queue cleanup retained for scheduled execution");
    }
  }

  @Scheduled(fixedDelay = 10_000)
  public synchronized void runAutomatically() {
    if (cleanupClock.instant().isBefore(nextSchedulerAttempt)) {
      return;
    }
    try {
      CleanupResult currentCleanupResult =
          cleanupTransactionTemplate.execute(transactionStatus ->
              currentResult(cleanupStateRepository.findById(1L).orElseThrow()));
      if (currentCleanupResult != null && currentCleanupResult.active()) {
        submitCleanupRun(currentCleanupResult.runId());
        return;
      }
      CleanupSettings cleanupSettings = readSettings();
      if (!cleanupSettings.automaticCleanupEnabled()) {
        return;
      }
      submitAutomaticRunWhenIdle(cleanupSettings);
    } catch (RuntimeException cleanupFailure) {
      nextSchedulerAttempt = cleanupClock.instant().plusSeconds(60);
      logger.error(
          "Scheduled mail queue cleanup could not progress", LogSafety.safeFailure(cleanupFailure));
    }
  }

  private void submitAutomaticRunWhenIdle(CleanupSettings cleanupSettings) {
    try {
      CleanupResult selectedResult =
          dispatchGuard.runWhenIdle(() -> selectOrCreateAutomaticRun(cleanupSettings));
      if (selectedResult != null) {
        submitCleanupRun(selectedResult.runId());
      }
    } catch (EmailQueueDispatchGuard.DispatchStillRunningException _) {
      // Eine ausgelassene automatische Prüfung ersetzt keinen vorgemerkten manuellen Lauf.
    }
  }

  private @Nullable CleanupResult selectOrCreateAutomaticRun(CleanupSettings cleanupSettings) {
    return cleanupTransactionTemplate.execute(transactionStatus -> {
      EmailQueueCleanupStateEntity cleanupState = cleanupStateRepository.lockState();
      CleanupResult latestResult = currentResult(cleanupState);
      if (latestResult != null && latestResult.active()) {
        return latestResult;
      }
      if (!automaticRunIsDue(cleanupState, cleanupSettings)) {
        return null;
      }
      return createRun(cleanupState, cleanupSettings, null);
    });
  }

  private void executeRunWhenIdle(Long runId) {
    try {
      dispatchGuard.runWhenIdle(() -> {
        executeBatches(runId);
        return null;
      });
    } catch (EmailQueueDispatchGuard.DispatchStillRunningException _) {
      // Kein erneuter Klick nötig: der Scheduler berücksichtigt aktive Aufträge auch ohne
      // Automatik.
    }
  }

  /** Die exklusive Versandsperre umfasst den Gesamtlauf, die Datenbanktransaktion nur ein Paket. */
  private void executeBatches(Long runId) {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        CleanupResult batchResult =
            Objects.requireNonNull(
                cleanupTransactionTemplate.execute(transactionStatus -> processBatch(runId)));
        if (!batchResult.active()) {
          return;
        }
      }
    } catch (RuntimeException cleanupFailure) {
      // Noch unter der Versandsperre terminalisieren: kein konkurrierendes Weiterarbeiten
      // dazwischen.
      try {
        cleanupTransactionTemplate.executeWithoutResult(transactionStatus -> failRun(runId));
      } catch (RuntimeException stateFailure) {
        // Beide Ursachen erhalten; ohne gespeicherten Ausgang bleibt das Journal aktiv.
        stateFailure.addSuppressed(cleanupFailure);
        throw stateFailure;
      }
      if (cleanupFailure instanceof AccessDeniedException) {
        logger.info("Mail queue cleanup stopped because authorization changed");
      } else {
        logger.error(
            "Mail queue cleanup could not be completed", LogSafety.safeFailure(cleanupFailure));
      }
    }
  }

  private CleanupResult processBatch(Long runId) {
    EmailQueueCleanupStateEntity cleanupState = cleanupStateRepository.lockState();
    EmailQueueCleanupRunEntity cleanupRun = cleanupRunRepository.findById(runId).orElseThrow();
    if (!cleanupRun.getStatus().isActive()) {
      return resultOf(cleanupRun);
    }
    if (!Objects.equals(cleanupState.getCurrentRunId(), runId)) {
      throw new IllegalStateException("Active mail cleanup run was superseded");
    }
    if (!cleanupRun.isAutomatic()) {
      adminRolePolicy.requireSuperadmin(cleanupRun.getRequestedById());
    }
    if (cleanupRun.getStartedAt() == null) {
      freezeRunScope(cleanupRun);
    }
    List<Long> candidateIds =
        emailQueueRepository.findUnresolvedWindowIds(
            cleanupRun.getClaimCutoffAt(),
            cleanupRun.getCursorId(),
            cleanupRun.getHighWaterId(),
            PageRequest.of(0, BATCH_SIZE));
    if (candidateIds.isEmpty()) {
      finishRun(cleanupState, cleanupRun, EmailQueueCleanupStatus.COMPLETED);
      AfterCommitLog.info(
          logger,
          "Mail queue cleanup completed without resending: count={}, automatic={}",
          cleanupRun.getResolvedCount(),
          cleanupRun.isAutomatic());
      return resultOf(cleanupRun);
    }
    long nextCursorId = candidateIds.getLast();
    if (nextCursorId <= cleanupRun.getCursorId()) {
      throw new IllegalStateException("Mail cleanup cursor did not advance");
    }
    for (Long candidateId : candidateIds) {
      if (resolveCandidate(candidateId, cleanupRun.getClaimCutoffAt())) {
        cleanupRun.setResolvedCount(Math.addExact(cleanupRun.getResolvedCount(), 1));
      }
    }
    cleanupRun.setCursorId(nextCursorId);
    cleanupRunRepository.saveAndFlush(cleanupRun);
    return resultOf(cleanupRun);
  }

  /** Sperren und Revalidierung bleiben in derselben Pakettransaktion wie Anzahl und Cursor. */
  private boolean resolveCandidate(Long candidateId, LocalDateTime claimCutoffAt) {
    EmailQueueEntity queueEntry = emailQueueRepository.lockById(candidateId).orElse(null);
    if (queueEntry == null
        || queueEntry.getStatus() != EmailStatus.IN_PROGRESS
        || queueEntry.getProcessingStartedAt() == null
        || queueEntry.getProcessingStartedAt().isAfter(claimCutoffAt)) {
      return false;
    }
    if (queueEntry.getEmailType() == EmailType.ACCOUNT_DELETED) {
      emailQueueRepository.delete(queueEntry);
    } else {
      queueEntry.setStatus(EmailStatus.UNKNOWN);
      emailQueueRepository.save(queueEntry);
    }
    return true;
  }

  private void freezeRunScope(EmailQueueCleanupRunEntity cleanupRun) {
    LocalDateTime startedAt = LocalDateTime.now(cleanupClock);
    cleanupRun.setStartedAt(startedAt);
    cleanupRun.setStatus(EmailQueueCleanupStatus.RUNNING);
    cleanupRun.setClaimCutoffAt(startedAt.minus(Duration.parse(cleanupRun.getMinimumClaimAge())));
    cleanupRun.setHighWaterId(
        emailQueueRepository.maximumUnresolvedIdBefore(cleanupRun.getClaimCutoffAt()));
  }

  private void failRun(Long runId) {
    EmailQueueCleanupStateEntity cleanupState = cleanupStateRepository.lockState();
    EmailQueueCleanupRunEntity cleanupRun = cleanupRunRepository.findById(runId).orElseThrow();
    if (cleanupRun.getStatus().isActive()) {
      finishRun(
          cleanupState,
          cleanupRun,
          cleanupRun.getResolvedCount() == 0
              ? EmailQueueCleanupStatus.FAILED
              : EmailQueueCleanupStatus.PARTIAL);
    }
  }

  private void finishRun(
      EmailQueueCleanupStateEntity cleanupState,
      EmailQueueCleanupRunEntity cleanupRun,
      EmailQueueCleanupStatus finalStatus) {
    cleanupRun.setStatus(finalStatus);
    cleanupRun.setFinishedAt(LocalDateTime.now(cleanupClock));
    cleanupRun.setRequestedById(null);
    cleanupState.setLastRunAt(cleanupRun.getFinishedAt());
    cleanupRunRepository.saveAndFlush(cleanupRun);
  }

  private CleanupResult createRun(
      EmailQueueCleanupStateEntity cleanupState,
      CleanupSettings cleanupSettings,
      Long requestingAccountId) {
    EmailQueueCleanupRunEntity cleanupRun = new EmailQueueCleanupRunEntity();
    cleanupRun.setAttemptedAt(LocalDateTime.now(cleanupClock));
    cleanupRun.setAutomatic(requestingAccountId == null);
    cleanupRun.setRequestedById(requestingAccountId);
    cleanupRun.setMinimumClaimAge(cleanupSettings.minimumClaimAgeDuration().toString());
    cleanupRun.setStatus(EmailQueueCleanupStatus.WAITING);
    cleanupRunRepository.saveAndFlush(cleanupRun);
    cleanupState.setCurrentRunId(cleanupRun.getId());
    cleanupStateRepository.saveAndFlush(cleanupState);
    return resultOf(cleanupRun);
  }

  private boolean automaticRunIsDue(
      EmailQueueCleanupStateEntity cleanupState, CleanupSettings cleanupSettings) {
    return cleanupState.getLastRunAt() == null
        || !LocalDateTime.now(cleanupClock)
            .isBefore(cleanupState.getLastRunAt().plus(cleanupSettings.runIntervalDuration()));
  }

  private CleanupSettings readSettings() {
    Map<ConfigEntry, String> configuredValues = configService.snapshot(CLEANUP_CONFIG_ENTRIES);
    String enabledValue = configuredValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_ENABLED);
    if (!"true".equalsIgnoreCase(enabledValue) && !"false".equalsIgnoreCase(enabledValue)) {
      throw new IllegalArgumentException("Invalid mail cleanup switch");
    }
    return new CleanupSettings(
        Boolean.parseBoolean(enabledValue),
        parseDuration(configuredValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_INTERVAL)),
        parseDuration(configuredValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_MINIMUM_AGE)));
  }

  private Duration parseDuration(String configuredDuration) {
    Duration parsedDuration = Duration.parse(configuredDuration);
    if (parsedDuration.compareTo(Duration.ofMinutes(1)) < 0) {
      throw new IllegalArgumentException("Mail cleanup durations must be at least one minute");
    }
    return parsedDuration;
  }

  @Scheduled(cron = "0 15 3 * * *")
  @Transactional
  public void deleteExpiredRunHistory() {
    int retentionDays = configService.getInteger(ConfigEntry.EMAIL_QUEUE_CLEANUP_HISTORY_DAYS);
    if (retentionDays < 1) {
      throw new IllegalArgumentException("Mail cleanup history retention must be positive");
    }
    int deletedRunCount =
        cleanupRunRepository.deleteFinishedBefore(
            LocalDateTime.now(cleanupClock).minusDays(retentionDays));
    if (deletedRunCount > 0) {
      AfterCommitLog.info(logger, "Mail cleanup history expired: count={}", deletedRunCount);
    }
  }

  private @Nullable CleanupResult currentResult(EmailQueueCleanupStateEntity cleanupState) {
    return cleanupState.getCurrentRunId() == null
        ? null
        : resultOf(cleanupRunRepository.findById(cleanupState.getCurrentRunId()).orElseThrow());
  }

  private CleanupResult resultOf(EmailQueueCleanupRunEntity cleanupRun) {
    return new CleanupResult(
        cleanupRun.getId(),
        cleanupRun.getStatus(),
        cleanupRun.getAttemptedAt(),
        cleanupRun.getStartedAt(),
        cleanupRun.getFinishedAt(),
        cleanupRun.getResolvedCount(),
        cleanupRun.isAutomatic());
  }
}
