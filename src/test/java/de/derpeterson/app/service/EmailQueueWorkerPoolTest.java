package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
    var executor = executor(limit);
    var pool = new EmailQueueWorkerPool(executor);
    var started = new CountDownLatch(limit);
    var release = new CountDownLatch(1);
    var active = new AtomicInteger();
    var maximum = new AtomicInteger();
    Set<Long> seen = ConcurrentHashMap.newKeySet();
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run =
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
    var pool = new EmailQueueWorkerPool(executor(2));
    var slow = new CountDownLatch(1);
    var firstStarted = new CountDownLatch(1);
    var thirdStarted = new CountDownLatch(1);
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L, 2L, 3L),
                      id -> {
                        if (id == 1) {
                          firstStarted.countDown();
                          await(slow);
                        }
                        if (id == 2) await(firstStarted);
                        if (id == 3) thirdStarted.countDown();
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
    var pool = new EmailQueueWorkerPool(executor(1));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var calls = new AtomicInteger();
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run =
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
    var pool = new EmailQueueWorkerPool(executor(1));
    var completed = new AtomicInteger();
    try {
      pool.process(
          List.of(1L, 2L),
          id -> {
            if (id == 1) throw new IllegalStateException("isolated worker failure");
            completed.incrementAndGet();
          });
      assertEquals(1, completed.get());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void executorRejectionBeforeAdmissionDoesNotDispatch() {
    var executor = executor(2);
    var pool = new EmailQueueWorkerPool(executor);
    executor.shutdown();
    var calls = new AtomicInteger();
    try {
      pool.process(List.of(1L, 2L), id -> calls.incrementAndGet());
      assertEquals(0, calls.get());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void partialRejectionDrainsAcceptedWorkAndStopsRefilling() throws Exception {
    var accepted = new AtomicInteger();
    var executor =
        new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)) {
          @Override
          public void execute(Runnable task) {
            if (accepted.incrementAndGet() == 2)
              throw new RejectedExecutionException("fixture rejection");
            super.execute(task);
          }
        };
    var pool = new EmailQueueWorkerPool(executor);
    var calls = new AtomicInteger();
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
    var pool = new EmailQueueWorkerPool(executor(1));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var calls = new AtomicInteger();
    try (var caller = Executors.newSingleThreadExecutor();
        var context = new GenericApplicationContext()) {
      var run =
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
    var shutdownCalled = new CountDownLatch(1);
    var executor =
        new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1)) {
          @Override
          public void shutdown() {
            super.shutdown();
            shutdownCalled.countDown();
          }
        };
    var pool = new EmailQueueWorkerPool(executor);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var callers = Executors.newFixedThreadPool(2)) {
      var run =
          callers.submit(
              () ->
                  pool.process(
                      List.of(1L),
                      id -> {
                        entered.countDown();
                        await(release);
                      }));
      await(entered);
      var close = callers.submit(pool::shutdown);
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
    var executor = executor(1);
    var pool = new EmailQueueWorkerPool(executor, Duration.ZERO);
    var entered = new CountDownLatch(1);
    var interrupted = new CountDownLatch(1);
    var calls = new AtomicInteger();
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run =
          caller.submit(
              () ->
                  pool.process(
                      List.of(1L, 2L),
                      id -> {
                        calls.incrementAndGet();
                        entered.countDown();
                        try {
                          new CountDownLatch(1).await();
                        } catch (InterruptedException failure) {
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

  @Test
  void interruptedCoordinatorDrainsAcceptedWorkAndRestoresItsInterruptFlag() throws Exception {
    var pool = new EmailQueueWorkerPool(executor(1));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var owner = new AtomicReference<Thread>();
    var calls = new AtomicInteger();
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run =
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
