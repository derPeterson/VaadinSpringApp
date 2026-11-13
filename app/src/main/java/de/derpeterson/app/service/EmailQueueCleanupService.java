package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.model.EmailQueueCleanupRunEntity;
import de.derpeterson.app.model.EmailQueueCleanupStateEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Gemeinsamer Abschluss alter Claims ohne Zustellbehauptung oder erneuten Versand. */
@Service
public class EmailQueueCleanupService {
  private static final Logger logger = LoggerFactory.getLogger(EmailQueueCleanupService.class);
  private static final List<ConfigEntry> CLEANUP_CONFIG_ENTRIES =
      List.of(
          ConfigEntry.EMAIL_QUEUE_CLEANUP_ENABLED,
          ConfigEntry.EMAIL_QUEUE_CLEANUP_INTERVAL,
          ConfigEntry.EMAIL_QUEUE_CLEANUP_MINIMUM_AGE);

  private final EmailQueueRepository emailQueueRepository;
  private final EmailQueueCleanupStateRepository emailQueueCleanupStateRepository;
  private final EmailQueueCleanupRunRepository emailQueueCleanupRunRepository;
  private final ConfigService configService;
  private final AdminRolePolicy adminRolePolicy;
  private final EmailQueueDispatchGuard emailQueueDispatchGuard;
  private final TransactionTemplate cleanupTransactionTemplate;
  private final Clock cleanupClock;
  private Instant nextSchedulerAttempt = Instant.MIN;

  public record CleanupResult(LocalDateTime finishedAt, int resolvedCount, boolean busy)
      implements Serializable {}

  private record CleanupSettings(
      boolean automaticCleanupEnabled,
      Duration runIntervalDuration,
      Duration minimumClaimAgeDuration) {}

  @Autowired
  public EmailQueueCleanupService(
      EmailQueueRepository emailQueueRepository,
      EmailQueueCleanupStateRepository emailQueueCleanupStateRepository,
      EmailQueueCleanupRunRepository emailQueueCleanupRunRepository,
      ConfigService configService,
      AdminRolePolicy adminRolePolicy,
      EmailQueueDispatchGuard emailQueueDispatchGuard,
      PlatformTransactionManager transactionManager) {
    this(
        emailQueueRepository,
        emailQueueCleanupStateRepository,
        emailQueueCleanupRunRepository,
        configService,
        adminRolePolicy,
        emailQueueDispatchGuard,
        transactionManager,
        Clock.systemDefaultZone());
  }

  EmailQueueCleanupService(
      EmailQueueRepository emailQueueRepository,
      EmailQueueCleanupStateRepository emailQueueCleanupStateRepository,
      EmailQueueCleanupRunRepository emailQueueCleanupRunRepository,
      ConfigService configService,
      AdminRolePolicy adminRolePolicy,
      EmailQueueDispatchGuard emailQueueDispatchGuard,
      PlatformTransactionManager transactionManager,
      Clock cleanupClock) {
    this.emailQueueRepository = emailQueueRepository;
    this.emailQueueCleanupStateRepository = emailQueueCleanupStateRepository;
    this.emailQueueCleanupRunRepository = emailQueueCleanupRunRepository;
    this.configService = configService;
    this.adminRolePolicy = adminRolePolicy;
    this.emailQueueDispatchGuard = emailQueueDispatchGuard;
    this.cleanupClock = cleanupClock;
    cleanupTransactionTemplate = new TransactionTemplate(transactionManager);
    cleanupTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public boolean canManageCleanup() {
    return adminRolePolicy.canManageSuperadmins();
  }

  @Transactional(readOnly = true)
  public CleanupResult latestResult() {
    adminRolePolicy.requireSuperadmin();
    return createCleanupResult(emailQueueCleanupStateRepository.findById(1L).orElseThrow());
  }

  public CleanupResult runManually() {
    adminRolePolicy.requireSuperadmin();
    return runCleanup(false, readSettings());
  }

  @Scheduled(fixedDelay = 10_000)
  public synchronized void runAutomatically() {
    if (cleanupClock.instant().isBefore(nextSchedulerAttempt)) {
      return;
    }
    try {
      CleanupSettings cleanupSettings = readSettings();
      if (cleanupSettings.automaticCleanupEnabled()) {
        runCleanup(true, cleanupSettings);
      }
    } catch (RuntimeException cleanupFailure) {
      // Bei technischen Fehlern nicht bei jedem Scheduler-Takt dieselbe Störung protokollieren.
      nextSchedulerAttempt = cleanupClock.instant().plusSeconds(60);
      logger.error("Scheduled mail queue cleanup failed", LogSafety.safeFailure(cleanupFailure));
    }
  }

  private CleanupSettings readSettings() {
    Map<ConfigEntry, String> cleanupConfigValues = configService.snapshot(CLEANUP_CONFIG_ENTRIES);
    String enabledValue = cleanupConfigValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_ENABLED);
    if (!"true".equalsIgnoreCase(enabledValue) && !"false".equalsIgnoreCase(enabledValue)) {
      throw new IllegalArgumentException("Invalid mail cleanup switch");
    }
    return new CleanupSettings(
        Boolean.parseBoolean(enabledValue),
        parseCleanupDuration(cleanupConfigValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_INTERVAL)),
        parseCleanupDuration(cleanupConfigValues.get(ConfigEntry.EMAIL_QUEUE_CLEANUP_MINIMUM_AGE)));
  }

  private Duration parseCleanupDuration(String durationValue) {
    Duration duration = Duration.parse(durationValue);
    if (duration.compareTo(Duration.ofMinutes(1)) < 0) {
      throw new IllegalArgumentException("Mail cleanup durations must be at least one minute");
    }
    return duration;
  }

  private CleanupResult runCleanup(boolean automatic, CleanupSettings cleanupSettings) {
    try {
      return emailQueueDispatchGuard.runWhenIdle(() ->
          cleanupTransactionTemplate.execute(
              transactionStatus -> completeEligibleRequests(automatic, cleanupSettings)));
    } catch (EmailQueueDispatchGuard.DispatchStillRunningException _) {
      return cleanupTransactionTemplate.execute(transactionStatus -> {
        if (!automatic) {
          adminRolePolicy.requireSuperadmin();
        }
        EmailQueueCleanupStateEntity cleanupState = emailQueueCleanupStateRepository.lockState();
        LocalDateTime cleanupDateTime = LocalDateTime.now(cleanupClock);
        if (automatic && !isAutomaticCleanupDue(cleanupState, cleanupSettings, cleanupDateTime)) {
          return null;
        }
        return saveCleanupResult(cleanupState, cleanupDateTime, 0, true, automatic);
      });
    }
  }

  private CleanupResult completeEligibleRequests(
      boolean automatic, CleanupSettings cleanupSettings) {
    if (!automatic) {
      adminRolePolicy.requireSuperadmin();
    }
    EmailQueueCleanupStateEntity cleanupState = emailQueueCleanupStateRepository.lockState();
    LocalDateTime cleanupDateTime = LocalDateTime.now(cleanupClock);
    if (automatic && !isAutomaticCleanupDue(cleanupState, cleanupSettings, cleanupDateTime)) {
      return null;
    }

    LocalDateTime claimCutoffDateTime =
        cleanupDateTime.minus(cleanupSettings.minimumClaimAgeDuration());
    List<EmailQueueEntity> eligibleQueueEntries =
        emailQueueRepository.findUnresolvedBefore(claimCutoffDateTime, PageRequest.of(0, 100));
    int resolvedCount = 0;
    for (EmailQueueEntity candidateQueueEntry : eligibleQueueEntries) {
      EmailQueueEntity queueEntry =
          emailQueueRepository.lockById(candidateQueueEntry.getId()).orElse(null);
      if (queueEntry == null
          || queueEntry.getStatus() != EmailStatus.IN_PROGRESS
          || queueEntry.getProcessingStartedAt() == null
          || queueEntry.getProcessingStartedAt().isAfter(claimCutoffDateTime)) {
        continue;
      }
      if (queueEntry.getEmailType() == EmailType.ACCOUNT_DELETED) {
        emailQueueRepository.delete(queueEntry);
      } else {
        queueEntry.setStatus(EmailStatus.UNKNOWN);
        emailQueueRepository.save(queueEntry);
      }
      resolvedCount++;
    }
    if (resolvedCount > 0) {
      AfterCommitLog.info(
          logger,
          "Unresolved mail queue entries closed without resending: count={}, automatic={}",
          resolvedCount,
          automatic);
    }
    return saveCleanupResult(cleanupState, cleanupDateTime, resolvedCount, false, automatic);
  }

  private boolean isAutomaticCleanupDue(
      EmailQueueCleanupStateEntity cleanupState,
      CleanupSettings cleanupSettings,
      LocalDateTime cleanupDateTime) {
    return cleanupState.getLastRunAt() == null
        || !cleanupDateTime.isBefore(
            cleanupState.getLastRunAt().plus(cleanupSettings.runIntervalDuration()));
  }

  private CleanupResult saveCleanupResult(
      EmailQueueCleanupStateEntity cleanupState,
      LocalDateTime cleanupDateTime,
      int resolvedCount,
      boolean busy,
      boolean automatic) {
    EmailQueueCleanupRunEntity emailQueueCleanupRunEntity = new EmailQueueCleanupRunEntity();
    emailQueueCleanupRunEntity.setAttemptedAt(cleanupDateTime);
    emailQueueCleanupRunEntity.setAutomatic(automatic);
    emailQueueCleanupRunEntity.setResolvedCount(resolvedCount);
    emailQueueCleanupRunEntity.setBusy(busy);
    emailQueueCleanupRunRepository.save(emailQueueCleanupRunEntity);

    cleanupState.setLastRunAt(cleanupDateTime);
    cleanupState.setResolvedCount(resolvedCount);
    cleanupState.setBusy(busy);
    emailQueueCleanupStateRepository.saveAndFlush(cleanupState);
    return createCleanupResult(cleanupState);
  }

  /** Die letzte Statusanzeige und Intervallsteuerung bleiben von der Historienfrist unberührt. */
  @Scheduled(cron = "0 15 3 * * *")
  @Transactional
  public void deleteExpiredRunHistory() {
    int retentionDays = configService.getInteger(ConfigEntry.EMAIL_QUEUE_CLEANUP_HISTORY_DAYS);
    if (retentionDays < 1) {
      throw new IllegalArgumentException("Mail cleanup history retention must be positive");
    }
    int deletedRunCount =
        emailQueueCleanupRunRepository.deleteAttemptsBefore(
            LocalDateTime.now(cleanupClock).minusDays(retentionDays));
    if (deletedRunCount > 0) {
      AfterCommitLog.info(logger, "Mail cleanup history expired: count={}", deletedRunCount);
    }
  }

  private CleanupResult createCleanupResult(EmailQueueCleanupStateEntity cleanupState) {
    return cleanupState.getLastRunAt() == null
        ? null
        : new CleanupResult(
            cleanupState.getLastRunAt(), cleanupState.getResolvedCount(), cleanupState.isBusy());
  }
}
