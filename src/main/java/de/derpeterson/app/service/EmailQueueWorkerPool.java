package de.derpeterson.app.service;

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
  private final ThreadPoolExecutor executor;
  private final int parallelism;
  private final Duration shutdownGrace;
  private final ReentrantLock batch = new ReentrantLock();
  private final Set<FutureTask<Void>> inFlight = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean shutdownStarted = new AtomicBoolean();
  private volatile boolean stopping;

  @Autowired
  public EmailQueueWorkerPool(@Qualifier("emailTaskExecutor") ThreadPoolExecutor executor) {
    this(executor, SHUTDOWN_GRACE);
  }

  EmailQueueWorkerPool(ThreadPoolExecutor executor, Duration shutdownGrace) {
    this.executor = java.util.Objects.requireNonNull(executor, "executor");
    this.parallelism = executor.getMaximumPoolSize();
    this.shutdownGrace = java.util.Objects.requireNonNull(shutdownGrace, "shutdownGrace");
    if (shutdownGrace.isNegative()) {
      throw new IllegalArgumentException("Negative shutdown grace.");
    }
  }

  public void process(List<Long> ids, LongConsumer dispatch) {
    // Zusätzliche manuelle Läufe überlappen keinen laufenden Batch; offene IDs bleiben in der DB.
    if (stopping || !batch.tryLock()) {
      return;
    }
    boolean interrupted = false;
    try {
      Iterator<Long> candidates = ids.iterator();
      BlockingQueue<FutureTask<Void>> completed = new LinkedBlockingQueue<>();
      int active = 0;
      boolean refill = true;
      while ((refill && !stopping && candidates.hasNext()) || active > 0) {
        while (refill && !stopping && active < parallelism && candidates.hasNext()) {
          long id = candidates.next();
          var task =
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
          inFlight.add(task);
          try {
            executor.execute(task);
            active++;
          } catch (RejectedExecutionException failure) {
            inFlight.remove(task);
            refill = false;
            logger.warn(
                "Queue worker rejected entry {}; remaining entries stay pending.", id, failure);
          }
        }
        if (active == 0) {
          break;
        }
        try {
          FutureTask<Void> finished = completed.take();
          active--;
          finished.get();
        } catch (InterruptedException failure) {
          // Angenommene Aufgaben erst abwarten, statt ihren Ausgang zu erraten oder neu zu starten.
          interrupted = true;
          refill = false;
        } catch (ExecutionException failure) {
          logger.error(
              "Queue worker failed; no automatic release of committed claims.", failure.getCause());
        } catch (CancellationException failure) {
          logger.warn("Queue worker cancelled during shutdown; committed claims remain unchanged.");
        }
      }
    } finally {
      batch.unlock();
      if (interrupted) {
        Thread.currentThread().interrupt();
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
    executor.shutdown();
    try {
      if (executor.awaitTermination(shutdownGrace.toMillis(), TimeUnit.MILLISECONDS)) {
        return;
      }
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
    }
    // Auch nie gestartete FutureTasks abschließen, damit der Koordinator nicht endlos wartet.
    executor.shutdownNow();
    inFlight.forEach(task -> task.cancel(true));
    logger.warn(
        "Queue shutdown grace expired or interrupted; unresolved claims require investigation.");
  }
}
