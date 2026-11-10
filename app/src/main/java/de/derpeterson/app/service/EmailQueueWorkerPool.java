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
  private ThreadPoolExecutor executor;
  private final IntSupplier configuredSize;
  private final Duration shutdownGrace;
  private final ReentrantLock batch = new ReentrantLock();
  private final Set<FutureTask<Void>> inFlight = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean shutdownStarted = new AtomicBoolean();
  private volatile boolean stopping;

  @Autowired
  public EmailQueueWorkerPool(
      @Qualifier("emailTaskExecutor") ThreadPoolExecutor executor, ConfigService config) {
    this(executor, SHUTDOWN_GRACE, () -> config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE));
  }

  public EmailQueueWorkerPool(ThreadPoolExecutor executor) {
    this(executor, SHUTDOWN_GRACE);
  }

  EmailQueueWorkerPool(ThreadPoolExecutor executor, Duration shutdownGrace) {
    this(executor, shutdownGrace, executor::getMaximumPoolSize);
  }

  private EmailQueueWorkerPool(
      ThreadPoolExecutor executor, Duration shutdownGrace, IntSupplier configuredSize) {
    this.executor = java.util.Objects.requireNonNull(executor, "executor");
    this.configuredSize = configuredSize;
    this.shutdownGrace = java.util.Objects.requireNonNull(shutdownGrace, "shutdownGrace");
    if (shutdownGrace.isNegative()) {
      throw new IllegalArgumentException("Negative shutdown grace.");
    }
  }

  public void dispatchEmailBatch(List<Long> pendingEmailIds, LongConsumer dispatchEmail) {
    // Zusätzliche manuelle Läufe überlappen keinen laufenden Batch; offene IDs bleiben in der DB.
    if (stopping || !batch.tryLock()) {
      return;
    }
    BatchExecution execution = null;
    try {
      execution = new BatchExecution(executorForBatch());
      execution.dispatchUntilBatchEnds(pendingEmailIds.iterator(), dispatchEmail);
    } finally {
      batch.unlock();
      if (execution != null && execution.interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private synchronized ThreadPoolExecutor executorForBatch() {
    if (!stopping) {
      int size = configuredSize.getAsInt();
      if (executor.getMaximumPoolSize() != size) {
        // Erst der nächste Batch nutzt die neue Größe. Die bisherige Queue war auf den alten
        // Pool ausgelegt und wird mit ihm beendet.
        ThreadPoolExecutor previous = executor;
        executor = ThreadPoolConfig.newExecutor(size);
        previous.shutdown();
      }
    }
    return executor;
  }

  /** Annahme und Abschluss gehören zu genau diesem Lauf; kein geteilter Koordinatorzustand. */
  private final class BatchExecution {
    private final ThreadPoolExecutor batchExecutor;
    private final int parallelism;
    private final BlockingQueue<FutureTask<Void>> completed = new LinkedBlockingQueue<>();
    private int active;
    private boolean refill = true;
    private boolean interrupted;

    private BatchExecution(ThreadPoolExecutor batchExecutor) {
      this.batchExecutor = batchExecutor;
      this.parallelism = batchExecutor.getMaximumPoolSize();
    }

    private void dispatchUntilBatchEnds(Iterator<Long> candidates, LongConsumer dispatch) {
      while ((refill && !stopping && candidates.hasNext()) || active > 0) {
        submitAvailable(candidates, dispatch);
        if (active == 0) {
          break;
        }
        awaitCompletion();
      }
    }

    private void submitAvailable(Iterator<Long> candidates, LongConsumer dispatch) {
      while (refill && !stopping && active < parallelism && candidates.hasNext()) {
        // Bereits fertige Futures melden einen Interrupt nicht zwingend über get().
        // Vor jeder Annahme prüfen; zum Abwarten kurz löschen und im finally wiederherstellen.
        if (Thread.interrupted()) {
          interrupted = true;
          refill = false;
          break;
        }
        submit(candidates.next(), dispatch);
      }
    }

    private void submit(long id, LongConsumer dispatch) {
      FutureTask<Void> dispatchTask =
          new FutureTask<Void>(
              () -> {
                if (!stopping) {
                  dispatch.accept(id);
                }
                return null;
              }) {
            @Override
            protected void done() {
              inFlight.remove(this);
              completed.add(this);
            }
          };
      inFlight.add(dispatchTask);
      try {
        batchExecutor.execute(dispatchTask);
        active++;
      } catch (RejectedExecutionException failure) {
        inFlight.remove(dispatchTask);
        refill = false;
        logger.warn(
            "Queue worker rejected entry {}; remaining entries stay pending.",
            id,
            safeFailure(failure));
      }
    }

    private void awaitCompletion() {
      try {
        FutureTask<Void> finished = completed.take();
        active--;
        finished.get();
      } catch (InterruptedException _) {
        // Angenommene Aufgaben erst abwarten; der Batch stellt den Interrupt im finally wieder her.
        // Sofortiges Wiederherstellen würde das blockierende Abwarten wiederholt unterbrechen.
        interrupted = true;
        refill = false;
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
    stopping = true;
  }

  @PreDestroy
  public void shutdown() {
    stopping = true;
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
