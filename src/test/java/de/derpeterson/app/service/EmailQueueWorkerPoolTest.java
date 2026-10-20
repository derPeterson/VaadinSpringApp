package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;

class EmailQueueWorkerPoolTest {
  static ThreadPoolExecutor executor(int size) {
    return new ThreadPoolExecutor(
        size, size, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(size));
  }

  static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(10, TimeUnit.SECONDS), "Controlled operation did not arrive");
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(failure);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void reachesTheConfiguredLimitAndRefillsOnlyCompletedSlots(int limit) throws Exception {
    ThreadPoolExecutor executor = executor(limit);
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    CountDownLatch started = new CountDownLatch(limit);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maximum = new AtomicInteger();
    Set<Long> seen = ConcurrentHashMap.newKeySet();
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<?> run =
          caller.submit(
              () ->
                  pool.process(
                      LongStream.rangeClosed(1, limit + 2).boxed().toList(),
                      id -> {
                        int concurrent = active.incrementAndGet();
                        maximum.accumulateAndGet(concurrent, Math::max);
                        assertTrue(seen.add(id));
                        try {
                          if (id <= limit) {
                            started.countDown();
                            await(release);
                          }
                        } finally {
                          active.decrementAndGet();
                        }
                      }));
      try {
        await(started);
        assertEquals(limit, seen.size());
        assertEquals(0, executor.getQueue().size());
        assertFalse(run.isDone());
      } finally {
        release.countDown();
      }
      run.get(10, TimeUnit.SECONDS);
      assertEquals(limit + 2, seen.size());
      assertEquals(limit, maximum.get());
    } finally {
      release.countDown();
      pool.shutdown();
    }
  }

  @Test
  void aSlowFirstTaskDoesNotPreventRefillingTheOtherWorker() throws Exception {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(2));
    CountDownLatch slow = new CountDownLatch(1);
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch thirdStarted = new CountDownLatch(1);
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<?> run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L, 2L, 3L),
                      id -> {
                        if (id == 1) {
                          firstStarted.countDown();
                          await(slow);
                        }
                        if (id == 2) {
                          await(firstStarted);
                        }
                        if (id == 3) {
                          thirdStarted.countDown();
                        }
                      }));
      try {
        await(thirdStarted);
        assertFalse(run.isDone());
      } finally {
        slow.countDown();
      }
      run.get(10, TimeUnit.SECONDS);
    } finally {
      slow.countDown();
      pool.shutdown();
    }
  }

  @Test
  void anOverlappingBatchIsSkippedAndCanRunOnTheNextCycle() throws Exception {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(1));
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<?> run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L),
                      id -> {
                        entered.countDown();
                        await(release);
                      }));
      try {
        await(entered);
        pool.process(List.of(2L), id -> calls.incrementAndGet());
        assertEquals(0, calls.get());
      } finally {
        release.countDown();
      }
      run.get(10, TimeUnit.SECONDS);
      pool.process(List.of(2L), id -> calls.incrementAndGet());
      assertEquals(1, calls.get());
    } finally {
      release.countDown();
      pool.shutdown();
    }
  }

  @Test
  void taskFailureDoesNotDiscardHealthyCandidates() {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(1));
    AtomicInteger completed = new AtomicInteger();
    try {
      pool.process(
          List.of(1L, 2L),
          id -> {
            if (id == 1) {
              throw new IllegalStateException("isolated worker failure");
            }
            completed.incrementAndGet();
          });
      assertEquals(1, completed.get());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void executorRejectionBeforeAdmissionDoesNotDispatch() {
    ThreadPoolExecutor executor = executor(2);
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    executor.shutdown();
    AtomicInteger calls = new AtomicInteger();
    try {
      pool.process(List.of(1L, 2L), id -> calls.incrementAndGet());
      assertEquals(0, calls.get());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void partialRejectionDrainsAcceptedWorkAndStopsRefilling() {
    AtomicInteger accepted = new AtomicInteger();
    var executor =
        new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)) {
          @Override
          public void execute(Runnable task) {
            if (accepted.incrementAndGet() == 2) {
              throw new RejectedExecutionException("fixture rejection");
            }
            super.execute(task);
          }
        };
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    AtomicInteger calls = new AtomicInteger();
    try {
      pool.process(List.of(1L, 2L, 3L), id -> calls.incrementAndGet());
      assertEquals(1, calls.get());
      assertEquals(2, accepted.get());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void contextCloseStopsNewCandidatesButLetsTheRunningTaskFinish() throws Exception {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(1));
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    try (ExecutorService caller = Executors.newSingleThreadExecutor();
        GenericApplicationContext context = new GenericApplicationContext()) {
      Future<?> run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L, 2L),
                      id -> {
                        calls.incrementAndGet();
                        entered.countDown();
                        await(release);
                      }));
      try {
        await(entered);
        pool.contextClosed(new ContextClosedEvent(context));
      } finally {
        release.countDown();
      }
      run.get(10, TimeUnit.SECONDS);
      pool.process(List.of(3L), id -> calls.incrementAndGet());
      assertEquals(1, calls.get());
    } finally {
      release.countDown();
      pool.shutdown();
    }
  }

  @Test
  void shutdownWaitsForAnAcceptedTaskWithinTheGracePeriod() throws Exception {
    CountDownLatch shutdownCalled = new CountDownLatch(1);
    var executor =
        new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1)) {
          @Override
          public void shutdown() {
            super.shutdown();
            shutdownCalled.countDown();
          }
        };
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService callers = Executors.newFixedThreadPool(2)) {
      Future<?> run =
          callers.submit(
              () ->
                  pool.process(
                      List.of(1L),
                      id -> {
                        entered.countDown();
                        await(release);
                      }));
      await(entered);
      Future<?> close = callers.submit(pool::shutdown);
      try {
        await(shutdownCalled);
        assertFalse(close.isDone());
      } finally {
        release.countDown();
      }
      close.get(10, TimeUnit.SECONDS);
      run.get(10, TimeUnit.SECONDS);
      assertTrue(executor.isTerminated());
      pool.shutdown();
    } finally {
      release.countDown();
      pool.shutdown();
    }
  }

  @Test
  void forcedShutdownCancelsTheFutureAndUnblocksTheCoordinator() throws Exception {
    ThreadPoolExecutor executor = executor(1);
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor, Duration.ZERO);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<?> run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L, 2L),
                      id -> {
                        calls.incrementAndGet();
                        entered.countDown();
                        try {
                          new CountDownLatch(1).await();
                        } catch (InterruptedException _) {
                          interrupted.countDown();
                          Thread.currentThread().interrupt();
                        }
                      }));
      await(entered);
      pool.shutdown();
      await(interrupted);
      run.get(10, TimeUnit.SECONDS);
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
      assertEquals(1, calls.get());
    } finally {
      pool.shutdown();
    }
  }

  @ParameterizedTest
  @CsvSource({"1,false", "1,true", "2,false", "2,true"})
  void interruptBetweenCompletedResultAndRefillPreventsAnotherAdmission(int limit, boolean fail) {
    AtomicInteger accepted = new AtomicInteger();
    AtomicBoolean injected = new AtomicBoolean();
    // Sofort abgeschlossene Futures erzwingen den Rückweg ohne unterbrechbares Warten.
    var executor =
        new ThreadPoolExecutor(
            limit, limit, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(limit)) {
          @Override
          public void execute(Runnable task) {
            accepted.incrementAndGet();
            task.run();
          }
        };
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    List<Long> values = LongStream.rangeClosed(1, limit + 2).boxed().toList();
    List<Long> candidates =
        new AbstractList<>() {
          @Override
          public Long get(int index) {
            return values.get(index);
          }

          @Override
          public int size() {
            return values.size();
          }

          @Override
          public Iterator<Long> iterator() {
            Iterator<Long> delegate = values.iterator();
            return new Iterator<>() {
              @Override
              public boolean hasNext() {
                // Wird nach dem ersten Ergebnis vor der nächsten Nachfüllung erreicht:
                // Bei vollem Pool überspringt die innere Schleife hasNext().
                if (accepted.get() == limit && injected.compareAndSet(false, true)) {
                  Thread.currentThread().interrupt();
                }
                return delegate.hasNext();
              }

              @Override
              public Long next() {
                return delegate.next();
              }
            };
          }
        };
    AtomicInteger calls = new AtomicInteger();
    try {
      pool.process(
          candidates,
          id -> {
            calls.incrementAndGet();
            if (fail) {
              throw new IllegalStateException("controlled worker failure");
            }
          });
      boolean preserved = Thread.interrupted();
      assertTrue(injected.get(), "The interruption must hit the controlled refill boundary");
      assertTrue(preserved, "The coordinator must restore its interrupt flag");
      assertEquals(limit, accepted.get(), "No extra task may be accepted after interruption");
      assertEquals(limit, calls.get());
      // Der Batchlock bleibt nach dem Abwarten für einen späteren Lauf nutzbar.
      pool.process(List.of(99L), id -> calls.incrementAndGet());
      assertEquals(limit + 1, calls.get());
    } finally {
      Thread.interrupted();
      pool.shutdown();
    }
  }

  @Test
  void coordinatorAlreadyInterruptedDoesNotAcceptWorkAndKeepsTheFlag() {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(1));
    AtomicInteger calls = new AtomicInteger();
    try {
      Thread.currentThread().interrupt();
      pool.process(List.of(1L), id -> calls.incrementAndGet());
      boolean preserved = Thread.interrupted();
      assertTrue(preserved);
      assertEquals(0, calls.get());
      pool.process(List.of(2L), id -> calls.incrementAndGet());
      assertEquals(1, calls.get());
    } finally {
      Thread.interrupted();
      pool.shutdown();
    }
  }

  @Test
  void interruptWhileFillingTheFirstSlotsStopsFurtherAdmission() {
    AtomicInteger accepted = new AtomicInteger();
    var executor =
        new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)) {
          @Override
          public void execute(Runnable task) {
            accepted.incrementAndGet();
            task.run();
            Thread.currentThread().interrupt();
          }
        };
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    AtomicInteger calls = new AtomicInteger();
    try {
      pool.process(List.of(1L, 2L, 3L), id -> calls.incrementAndGet());
      boolean preserved = Thread.interrupted();
      assertTrue(preserved);
      assertEquals(1, accepted.get());
      assertEquals(1, calls.get());
    } finally {
      Thread.interrupted();
      pool.shutdown();
    }
  }

  @Test
  void interruptedCoordinatorDrainsAcceptedWorkAndRestoresItsInterruptFlag() throws Exception {
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor(1));
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Thread> owner = new AtomicReference<Thread>();
    AtomicInteger calls = new AtomicInteger();
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<Boolean> run =
          caller.submit(
              () -> {
                owner.set(Thread.currentThread());
                pool.process(
                    List.of(1L, 2L),
                    id -> {
                      calls.incrementAndGet();
                      entered.countDown();
                      await(release);
                    });
                return Thread.currentThread().isInterrupted();
              });
      try {
        await(entered);
        owner.get().interrupt();
      } finally {
        release.countDown();
      }
      assertTrue(run.get(10, TimeUnit.SECONDS));
      assertEquals(1, calls.get());
    } finally {
      release.countDown();
      pool.shutdown();
    }
  }
}
