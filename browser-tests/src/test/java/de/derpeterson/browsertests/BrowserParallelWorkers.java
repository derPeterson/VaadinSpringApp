package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Processes and evidence only; each worker delegates its Docker lifecycle to BrowserTestRunner. */
final class BrowserParallelWorkers {
  record Worker(
      Path directory, String project, List<Class<?>> classes, Process process, Instant startedAt) {}

  private final List<Worker> workerProcesses = new ArrayList<>();

  synchronized List<Worker> workers() {
    return List.copyOf(workerProcesses);
  }

  synchronized void start(
      Path browserTestModulePath,
      Path runDirectory,
      String suiteName,
      String testLanguage,
      List<List<Class<?>>> classesByWorker,
      Map<String, String> workerEnvironment,
      String composeProjectName,
      boolean headed,
      boolean coverage,
      boolean failureProbe)
      throws IOException {
    // Ports während der Planung reservieren. Nach Freigabe bindet nur Docker daran;
    // bei einer externen Kollision scheitert Compose, bevor ein Browser startet.
    List<ServerSocket> reservedPortSockets = reservePorts(classesByWorker.size());
    try {
      for (int workerGroupIndex = 0;
          workerGroupIndex < classesByWorker.size();
          workerGroupIndex++) {
        Path workerRunDirectory = runDirectory.resolve("workers/" + (workerGroupIndex + 1));
        Files.createDirectories(workerRunDirectory);
        String workerComposeProject = composeProjectName + "-w" + (workerGroupIndex + 1);
        List<String> workerCommandArguments =
            new ArrayList<>(
                List.of(
                    javaExecutable(),
                    "-Dfile.encoding=UTF-8",
                    "-Dbrowser.tests.workerRun=" + workerRunDirectory,
                    "-Dbrowser.tests.workerClasses="
                        + String.join(
                            ",",
                            classesByWorker.get(workerGroupIndex).stream()
                                .map(Class::getSimpleName)
                                .toList()),
                    "-Dbrowser.tests.workerProject=" + workerComposeProject,
                    "-Dbrowser.tests.workerJar=" + (workerGroupIndex == 0),
                    "-javaagent:" + browserTestModulePath.resolve("target/tools/aspectjweaver.jar"),
                    "-cp",
                    System.getProperty("java.class.path"),
                    BrowserTestRunner.class.getName(),
                    browserTestModulePath.toString(),
                    "--suite",
                    suiteName,
                    "--language",
                    testLanguage,
                    "--no-build"));

        if (headed) {
          workerCommandArguments.add("--headed");
        }
        if (coverage) {
          workerCommandArguments.add("--coverage");
        }
        if (failureProbe) {
          workerCommandArguments.add("--failure-probe");
        }

        ProcessBuilder workerProcessBuilder =
            new ProcessBuilder(workerCommandArguments)
                .directory(browserTestModulePath.getParent().toFile())
                .redirectErrorStream(true)
                .redirectOutput(workerRunDirectory.resolve("console.log").toFile());
        workerProcessBuilder.environment().putAll(workerEnvironment);
        workerProcessBuilder
            .environment()
            .put(
                "BROWSER_TEST_APP_PORT",
                String.valueOf(reservedPortSockets.get(workerGroupIndex * 3).getLocalPort()));
        workerProcessBuilder
            .environment()
            .put(
                "BROWSER_TEST_MAILPIT_PORT",
                String.valueOf(reservedPortSockets.get(workerGroupIndex * 3 + 1).getLocalPort()));
        workerProcessBuilder
            .environment()
            .put(
                "BROWSER_TEST_HTTP_PORT",
                String.valueOf(reservedPortSockets.get(workerGroupIndex * 3 + 2).getLocalPort()));

        reservedPortSockets.get(workerGroupIndex * 3).close();
        reservedPortSockets.get(workerGroupIndex * 3 + 1).close();
        reservedPortSockets.get(workerGroupIndex * 3 + 2).close();

        Process workerProcess = workerProcessBuilder.start();
        workerProcesses.add(
            new Worker(
                workerRunDirectory,
                workerComposeProject,
                classesByWorker.get(workerGroupIndex),
                workerProcess,
                Instant.now()));

        System.out.println(
            "Worker "
                + (workerGroupIndex + 1)
                + " gestartet: "
                + classesByWorker.get(workerGroupIndex).stream()
                    .map(Class::getSimpleName)
                    .toList());
      }
    } finally {
      for (ServerSocket reservedPortSocket : reservedPortSockets) {
        reservedPortSocket.close();
      }
    }
  }

  static List<ServerSocket> reservePorts(int workerCount) throws IOException {
    List<ServerSocket> reservedPortSockets = new ArrayList<>();
    try {
      for (int portReservationIndex = 0;
          portReservationIndex < workerCount * 3;
          portReservationIndex++) {
        ServerSocket portReservationSocket = new ServerSocket();
        reservedPortSockets.add(portReservationSocket);
        portReservationSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
      }

      return reservedPortSockets;
    } catch (IOException reservationFailure) {
      for (ServerSocket portReservationSocket : reservedPortSockets) {
        try {
          portReservationSocket.close();
        } catch (IOException socketCloseFailure) {
          reservationFailure.addSuppressed(socketCloseFailure);
        }
      }
      throw reservationFailure;
    }
  }

  void await() throws InterruptedException, IOException {
    Set<Process> reportedWorkerProcesses = new HashSet<>();
    while (reportedWorkerProcesses.size() < workerProcesses.size()) {
      for (Worker worker : workerProcesses) {
        if (reportedWorkerProcesses.contains(worker.process())) {
          continue;
        }
        if (worker.process().waitFor(1, TimeUnit.SECONDS)) {
          reportedWorkerProcesses.add(worker.process());
          System.out.println(
              "Worker "
                  + worker.directory().getFileName()
                  + " beendet, Exitcode "
                  + worker.process().exitValue());
        } else if (Duration.between(worker.startedAt(), Instant.now()).toMinutes() >= 60) {
          throw new IOException("Worker timeout after 60 minutes: " + worker.directory());
        }
      }
    }
  }

  synchronized void stop() {
    for (Worker worker : workerProcesses) {
      if (worker.process().isAlive()) {
        // Auch laufende Compose-/Browser-Kindprozesse beenden, bevor Compose down läuft.
        try {
          stopWorker(worker.process());
        } catch (IOException error) {
          throw new IllegalStateException("Worker remains alive; trust cleanup is unsafe", error);
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Worker cleanup interrupted", error);
        }
      }
    }
    // Auf Windows garantiert destroy() keinen JVM-Shutdown-Hook. Der Elternrunner
    // baut deshalb anschließend jedes eigene Compose-Projekt ausdrücklich ab.
  }

  static void stopWorker(Process workerProcess) throws IOException, InterruptedException {
    List<ProcessHandle> childProcessHandles = workerProcess.descendants().toList();
    childProcessHandles.reversed().forEach(ProcessHandle::destroyForcibly);
    BrowserHttps.stopBeforeTrustCleanup(workerProcess);

    for (ProcessHandle childProcessHandle : childProcessHandles) {
      try {
        childProcessHandle.onExit().get(10, TimeUnit.SECONDS);
      } catch (java.util.concurrent.ExecutionException
          | java.util.concurrent.TimeoutException childExitFailure) {
        throw new IOException(
            "Worker child remains alive; trust cleanup is unsafe", childExitFailure);
      }
    }
  }

  static String javaExecutable() {
    return Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
        .toString();
  }

  static void collect(Path sourceEvidenceDirectory, Path destinationEvidenceDirectory)
      throws IOException {
    try (Stream<Path> evidencePaths = Files.list(sourceEvidenceDirectory)) {
      for (Path evidencePath : evidencePaths.toList()) {
        if (evidencePath.getFileName().toString().equals("environment.properties")) {
          continue;
        }
        if (!Files.isRegularFile(evidencePath)) {
          throw new IOException("Unexpected evidence directory: " + evidencePath);
        }
        // Verschieben statt duplizieren: nur ein gemeinsamer Satz Screenshots/Traces.
        // Keine REPLACE_EXISTING-Option: Kollisionen dürfen keinen Nachweis überschreiben.
        Files.move(evidencePath, destinationEvidenceDirectory.resolve(evidencePath.getFileName()));
      }
    }
  }

  static boolean matchesClasses(JsonNode workerMetadata, List<Class<?>> expectedTestClasses) {
    Set<String> actualClassNames = new HashSet<>();
    for (JsonNode testClassNameNode : workerMetadata.path("testClasses")) {
      actualClassNames.add(testClassNameNode.asText());
    }

    return workerMetadata.path("testClasses").size() == expectedTestClasses.size()
        && actualClassNames.equals(
            new HashSet<>(expectedTestClasses.stream().map(Class::getSimpleName).toList()));
  }

  static boolean successful(
      JsonNode workerMetadata, int workerExitCode, String expectedImageId, int evidenceCount) {
    return workerExitCode == 0
        && workerMetadata.hasNonNull("finishedAt")
        && List.of("setup", "tests", "cleanup").stream()
            .allMatch(phaseKey -> workerMetadata.path(phaseKey).asText().equals("passed"))
        && workerMetadata.path("imageId").asText().equals(expectedImageId)
        && workerMetadata.path("testsFound").asInt() > 0
        && workerMetadata.path("testsFound").asInt() == workerMetadata.path("testsPassed").asInt()
        && workerMetadata.path("testsFound").asInt() == evidenceCount
        && workerMetadata.path("testsFailed").asInt(-1) == 0
        && workerMetadata.path("testsSkipped").asInt(-1) == 0;
  }
}
