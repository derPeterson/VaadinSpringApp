package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.config.ThreadPoolConfig;
import de.derpeterson.app.model.enums.ConfigEntry;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Ein Batch je Instanz, höchstens N angenommene Aufgaben; kein Entity-/Session-Transfer. */
@Component
public class EmailQueueWorkerPool {
  private static final Logger logger = LoggerFactory.getLogger(EmailQueueWorkerPool.class);
  private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(30);
  private ThreadPoolExecutor workerExecutor;
  private final IntSupplier configuredSize;
  private final Duration shutdownGrace;
  private final ReentrantLock batchLock = new ReentrantLock();
  private final Set<FutureTask<Void>> inFlight = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean shutdownStarted = new AtomicBoolean();
  private volatile boolean workerShutdownInProgress;

  @Autowired
  public EmailQueueWorkerPool(
      @Qualifier("emailTaskExecutor") ThreadPoolExecutor workerExecutor, ConfigService config) {
    this(
        workerExecutor, SHUTDOWN_GRACE, () -> config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE));
  }

  public EmailQueueWorkerPool(ThreadPoolExecutor workerExecutor) {
    this(workerExecutor, SHUTDOWN_GRACE);
  }

  EmailQueueWorkerPool(ThreadPoolExecutor workerExecutor, Duration shutdownGrace) {
    this(workerExecutor, shutdownGrace, workerExecutor::getMaximumPoolSize);
  }

  private EmailQueueWorkerPool(
      ThreadPoolExecutor workerExecutor, Duration shutdownGrace, IntSupplier configuredSize) {
    this.workerExecutor = java.util.Objects.requireNonNull(workerExecutor, "executor");
    this.configuredSize = configuredSize;
    this.shutdownGrace = java.util.Objects.requireNonNull(shutdownGrace, "shutdownGrace");
    if (shutdownGrace.isNegative()) {
      throw new IllegalArgumentException("Negative shutdown grace.");
    }
  }

  public void dispatchEmailBatch(List<Long> pendingEmailIds, LongConsumer dispatchEmail) {
    // Zusätzliche manuelle Läufe überlappen keinen laufenden Batch; offene IDs bleiben in der DB.
    if (workerShutdownInProgress || !batchLock.tryLock()) {
      return;
    }
    BatchExecution execution = null;
    try {
      execution = new BatchExecution(executorForBatch());
      execution.dispatchUntilBatchEnds(pendingEmailIds.iterator(), dispatchEmail);
    } finally {
      batchLock.unlock();
      if (execution != null && execution.batchInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private synchronized ThreadPoolExecutor executorForBatch() {
    if (!workerShutdownInProgress) {
      int size = configuredSize.getAsInt();
      if (workerExecutor.getMaximumPoolSize() != size) {
        // Erst der nächste Batch nutzt die neue Größe. Die bisherige Queue war auf den alten
        // Pool ausgelegt und wird mit ihm beendet.
        ThreadPoolExecutor previous = workerExecutor;
        workerExecutor = ThreadPoolConfig.newExecutor(size);
        previous.shutdown();
      }
    }
    return workerExecutor;
  }

  /** Annahme und Abschluss gehören zu genau diesem Lauf; kein geteilter Koordinatorzustand. */
  private final class BatchExecution {
    private final ThreadPoolExecutor batchExecutor;
    private final int batchParallelism;
    private final BlockingQueue<FutureTask<Void>> completedDispatchTasks =
        new LinkedBlockingQueue<>();
    private int activeDispatchCount;
    private boolean acceptMoreDispatchTasks = true;
    private boolean batchInterrupted;

    private BatchExecution(ThreadPoolExecutor batchExecutor) {
      this.batchExecutor = batchExecutor;
      this.batchParallelism = batchExecutor.getMaximumPoolSize();
    }

    private void dispatchUntilBatchEnds(Iterator<Long> candidates, LongConsumer dispatch) {
      while ((acceptMoreDispatchTasks && !workerShutdownInProgress && candidates.hasNext())
          || activeDispatchCount > 0) {
        submitAvailable(candidates, dispatch);
        if (activeDispatchCount == 0) {
          break;
        }
        awaitCompletedDispatchTask();
      }
    }

    private void submitAvailable(Iterator<Long> candidates, LongConsumer dispatch) {
      while (acceptMoreDispatchTasks
          && !workerShutdownInProgress
          && activeDispatchCount < batchParallelism
          && candidates.hasNext()) {
        // Bereits fertige Futures melden einen Interrupt nicht zwingend über get().
        // Vor jeder Annahme prüfen; zum Abwarten kurz löschen und im finally wiederherstellen.
        if (Thread.interrupted()) {
          batchInterrupted = true;
          acceptMoreDispatchTasks = false;
          break;
        }
        submitEmailDispatchTask(candidates.next(), dispatch);
      }
    }

    private void submitEmailDispatchTask(long queueEntryId, LongConsumer dispatchEmailEntry) {
      FutureTask<Void> dispatchTask =
          new FutureTask<Void>(
              () -> {
                if (!workerShutdownInProgress) {
                  dispatchEmailEntry.accept(queueEntryId);
                }
                return null;
              }) {
            @Override
            protected void done() {
              inFlight.remove(this);
              completedDispatchTasks.add(this);
            }
          };

      inFlight.add(dispatchTask);
      try {
        batchExecutor.execute(dispatchTask);
        activeDispatchCount++;
      } catch (RejectedExecutionException dispatchRejected) {
        inFlight.remove(dispatchTask);
        acceptMoreDispatchTasks = false;
        logger.warn(
            "Queue worker rejected entry {}; remaining entries stay pending.",
            queueEntryId,
            safeFailure(dispatchRejected));
      }
    }

    private void awaitCompletedDispatchTask() {
      try {
        FutureTask<Void> finished = completedDispatchTasks.take();
        activeDispatchCount--;
        finished.get();
      } catch (InterruptedException _) {
        // Angenommene Aufgaben erst abwarten; der Batch stellt den Interrupt im finally wieder her.
        // Sofortiges Wiederherstellen würde das blockierende Abwarten wiederholt unterbrechen.
        batchInterrupted = true;
        acceptMoreDispatchTasks = false;
      } catch (ExecutionException failure) {
        logger.error(
            "Queue worker failed; no automatic release of committed claims.",
            safeFailure(failure.getCause()));
      } catch (CancellationException _) {
        logger.warn("Queue worker cancelled during shutdown; committed claims remain unchanged.");
      }
    }
  }

  @EventListener
  public void contextClosed(ContextClosedEvent event) {
    workerShutdownInProgress = true;
  }

  @PreDestroy
  public void shutdown() {
    workerShutdownInProgress = true;
    if (!shutdownStarted.compareAndSet(false, true)) {
      return;
    }
    ThreadPoolExecutor closing = executorForBatch();
    closing.shutdown();
    try {
      if (closing.awaitTermination(shutdownGrace.toMillis(), TimeUnit.MILLISECONDS)) {
        return;
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
    // Auch nie gestartete FutureTasks abschließen, damit der Koordinator nicht endlos wartet.
    closing.shutdownNow();
    inFlight.forEach(task -> task.cancel(true));
    logger.warn(
        "Queue shutdown grace expired or interrupted; unresolved claims require investigation.");
  }
}
