package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmailQueueDispatchGuardTest {
  @TempDir Path temporaryDirectory;

  @Test
  void parallelDispatchesShareTheGateButBlockAdministrativeResolution() throws Exception {
    EmailQueueDispatchGuard dispatchGuard = createFileDispatchGuard();
    CountDownLatch enteredDispatches = new CountDownLatch(2);
    CountDownLatch releaseDispatches = new CountDownLatch(1);
    try (ExecutorService dispatchExecutor = Executors.newFixedThreadPool(2)) {
      Runnable dispatchAction =
          () ->
              dispatchGuard.runDispatch(() -> {
                enteredDispatches.countDown();
                awaitLatchCompletion(releaseDispatches);
              });
      Future<?> firstDispatch = dispatchExecutor.submit(dispatchAction);
      Future<?> secondDispatch = dispatchExecutor.submit(dispatchAction);
      try {
        awaitLatchCompletion(enteredDispatches);
        assertThrows(
            EmailQueueDispatchGuard.DispatchStillRunningException.class,
            () -> createFileDispatchGuard().runWhenIdle(() -> "must not resolve"));
      } finally {
        releaseDispatches.countDown();
      }
      firstDispatch.get(10, TimeUnit.SECONDS);
      secondDispatch.get(10, TimeUnit.SECONDS);
    }
    assertEquals("resolved", dispatchGuard.runWhenIdle(() -> "resolved"));
  }

  @Test
  void cancellationDoesNotPretendAnUninterruptibleTransportHasStopped() throws Exception {
    EmailQueueDispatchGuard dispatchGuard = createFileDispatchGuard();
    CountDownLatch transportStarted = new CountDownLatch(1);
    CountDownLatch releaseTransport = new CountDownLatch(1);
    CountDownLatch transportFinished = new CountDownLatch(1);
    try (ExecutorService dispatchExecutor = Executors.newSingleThreadExecutor()) {
      Future<?> dispatchTask =
          dispatchExecutor.submit(() -> {
            try {
              dispatchGuard.runDispatch(() -> {
                transportStarted.countDown();
                while (releaseTransport.getCount() != 0) {
                  try {
                    releaseTransport.await();
                  } catch (InterruptedException ignoredInterrupt) {
                    // Modelliert einen SMTP-Aufruf, der trotz Future.cancel weiterläuft.
                  }
                }
              });
            } finally {
              transportFinished.countDown();
            }
          });
      try {
        awaitLatchCompletion(transportStarted);
        dispatchTask.cancel(true);
        assertThrows(
            EmailQueueDispatchGuard.DispatchStillRunningException.class,
            () -> dispatchGuard.runWhenIdle(() -> "must not resolve"));
      } finally {
        releaseTransport.countDown();
      }
      awaitLatchCompletion(transportFinished);
    }
    assertEquals("resolved", dispatchGuard.runWhenIdle(() -> "resolved"));
  }

  @Test
  void resolutionGatePreventsNewClaimsUntilItsTransactionActionEnds() throws Exception {
    EmailQueueDispatchGuard dispatchGuard = createFileDispatchGuard();
    AtomicBoolean dispatchEntered = new AtomicBoolean();
    CountDownLatch dispatchSubmitted = new CountDownLatch(1);
    try (ExecutorService dispatchExecutor = Executors.newSingleThreadExecutor()) {
      Future<?> dispatchTask =
          dispatchGuard.runWhenIdle(() -> {
            Future<?> submittedTask =
                dispatchExecutor.submit(() -> {
                  dispatchSubmitted.countDown();
                  dispatchGuard.runDispatch(() -> dispatchEntered.set(true));
                });
            awaitLatchCompletion(dispatchSubmitted);
            assertFalse(dispatchEntered.get());
            return submittedTask;
          });
      dispatchTask.get(10, TimeUnit.SECONDS);
    }
    assertTrue(dispatchEntered.get());
  }

  @Test
  void foreignProcessDispatchIsProtectedAndItsCrashReleasesOnlyTheFileLock() throws Exception {
    String databaseUrl = "jdbc:h2:file:" + temporaryDirectory.resolve("mail-queue");
    String testClasspath =
        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    Path javaArgumentsFile = temporaryDirectory.resolve("dispatch-java.args");
    Files.writeString(
        javaArgumentsFile,
        "-cp\n\""
            + testClasspath.replace('\\', '/')
            + "\"\n"
            + EmailQueueDispatchGuardTest.class.getName()
            + "\n\""
            + databaseUrl.replace('\\', '/')
            + "\"\n",
        StandardCharsets.UTF_8);

    Process dispatchProcess =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "@" + javaArgumentsFile)
            .redirectErrorStream(true)
            .start();
    try (ExecutorService readExecutor = Executors.newSingleThreadExecutor();
        BufferedReader processOutputReader =
            new BufferedReader(new InputStreamReader(dispatchProcess.getInputStream()))) {
      Future<String> readyLine = readExecutor.submit(processOutputReader::readLine);
      assertEquals("DISPATCH_STARTED", readyLine.get(20, TimeUnit.SECONDS));
      EmailQueueDispatchGuard dispatchGuard = new EmailQueueDispatchGuard(databaseUrl);
      assertThrows(
          EmailQueueDispatchGuard.DispatchStillRunningException.class,
          () -> dispatchGuard.runWhenIdle(() -> "must not resolve"));

      // Ausschließlich der eigene Kindprozess wird wie bei einem Absturz beendet.
      dispatchProcess.destroyForcibly();
      assertTrue(dispatchProcess.waitFor(10, TimeUnit.SECONDS));
      assertEquals("resolved", dispatchGuard.runWhenIdle(() -> "resolved"));
    } finally {
      if (dispatchProcess.isAlive()) {
        dispatchProcess.destroyForcibly();
        dispatchProcess.waitFor(10, TimeUnit.SECONDS);
      }
    }
  }

  public static void main(String[] arguments) {
    new EmailQueueDispatchGuard(arguments[0])
        .runDispatch(() -> {
          System.out.println("DISPATCH_STARTED");
          System.out.flush();
          awaitLatchCompletion(new CountDownLatch(1));
        });
  }

  private EmailQueueDispatchGuard createFileDispatchGuard() {
    return new EmailQueueDispatchGuard("jdbc:h2:file:" + temporaryDirectory.resolve("mail-queue"));
  }

  @Test
  void failedResolutionAndDispatchReleaseTheirLocks() {
    EmailQueueDispatchGuard dispatchGuard = createFileDispatchGuard();
    assertThrows(
        IllegalStateException.class,
        () ->
            dispatchGuard.runWhenIdle(() -> {
              throw new IllegalStateException("Controlled resolution failure");
            }));
    assertThrows(
        IllegalStateException.class,
        () ->
            dispatchGuard.runDispatch(() -> {
              throw new IllegalStateException("Controlled dispatch failure");
            }));

    assertEquals("resolved", createFileDispatchGuard().runWhenIdle(() -> "resolved"));
  }

  private static void awaitLatchCompletion(CountDownLatch completionLatch) {
    try {
      assertTrue(completionLatch.await(30, TimeUnit.SECONDS));
    } catch (InterruptedException interruptedFailure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interruptedFailure);
    }
  }
}
