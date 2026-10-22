package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BrowserHttpsTest {
  @TempDir Path directory;

  @ParameterizedTest
  @CsvSource({
    "rights,false",
    "rights,true",
    "denied,false",
    "denied,true",
    "cancelled,false",
    "cancelled,true",
    "partial,false",
    "partial,true"
  })
  @EnabledOnOs(OS.WINDOWS)
  void actualTrustHelperCleansFailedImportsWithoutTouchingForeignEntries(
      String scenario, boolean rootOnly) throws Exception {
    Process process =
        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "RemoteSigned",
                "-File",
                Path.of("src/test/resources/https-trust-failure.ps1").toAbsolutePath().toString(),
                "-Repository",
                Path.of("..").toAbsolutePath().normalize().toString(),
                "-Directory",
                directory.toString(),
                "-Scenario",
                scenario,
                "-RootOnlyMode",
                String.valueOf(rootOnly))
            .redirectErrorStream(true)
            .start();
    try {
      assertTrue(process.waitFor(20, TimeUnit.SECONDS));
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), output);
      assertTrue(output.contains("cleanup verified"), output);
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(process);
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void trustHelperUsesScopedPolicyAndPreservesPathsWithSpaces() throws Exception {
    Path repo = directory.resolve("test project");
    Path scripts = Files.createDirectories(repo.resolve("scripts/https"));
    Path certificates = repo.resolve("test certificates");
    // Der Ersatz liest nur Argumente und Richtlinie, ohne den Zertifikatsspeicher zu öffnen.
    Files.writeString(
        scripts.resolve("trust.ps1"),
        """
        param([string]$Action, [string]$Certificate, [switch]$RootOnly)
        Write-Output $Action
        Write-Output $Certificate
        Write-Output ([bool]$RootOnly)
        Write-Output $env:PSExecutionPolicyPreference
        """);
    ProcessBuilder builder =
        new ProcessBuilder(BrowserHttps.trustCommand(repo, certificates, "Add"))
            .redirectErrorStream(true);
    builder.environment().put("PSExecutionPolicyPreference", "Restricted");
    Process command = builder.start();
    try {
      assertTrue(command.waitFor(10, TimeUnit.SECONDS));
      String output = new String(command.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, command.exitValue(), output);
      assertEquals(
          List.of("Add", certificates.resolve("root.crt").toString(), "True", "RemoteSigned"),
          output.lines().toList());
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(command);
    }
  }

  @Test
  void cleanupRequiresTheExactOwnershipMarkerRatherThanOnlyACertificate() throws Exception {
    Files.writeString(directory.resolve("root.crt"), "not a trust marker");
    assertFalse(BrowserHttps.ownsTrust(directory));
    Files.writeString(directory.resolve("root.crt.owned"), "owned");
    assertTrue(BrowserHttps.ownsTrust(directory));
  }

  @Test
  void waitingImportProcessMustBeDeadBeforeTrustCleanupCanContinue() throws Exception {
    Process waiting =
        new ProcessBuilder(
                BrowserParallelWorkers.javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                WaitingChild.class.getName())
            .start();
    try {
      assertEquals("ready", waiting.inputReader().readLine());
      assertTrue(waiting.isAlive());
      BrowserHttps.stopBeforeTrustCleanup(waiting);
      assertFalse(waiting.isAlive());
      BrowserHttps.stopBeforeTrustCleanup(waiting);
      BrowserHttps.stopBeforeTrustCleanup(null);
    } finally {
      waiting.destroyForcibly();
      waiting.waitFor();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 1223})
  void deniedCancelledOrUnauthorizedImportCannotPassSetup(int exitCode) throws Exception {
    Process failed = child(ImportChild.class, String.valueOf(exitCode));
    try {
      IOException error =
          assertThrows(
              IOException.class,
              () -> BrowserHttps.awaitTrustCommand(failed, Duration.ofSeconds(10)));
      assertTrue(error.getMessage().contains(String.valueOf(exitCode)));
      assertFalse(failed.isAlive());
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(failed);
    }
  }

  @Test
  void confirmationTimeoutStopsImporterBeforeCleanup() throws Exception {
    Process waiting = child(WaitingChild.class);
    try {
      assertEquals("ready", waiting.inputReader().readLine());
      IOException error =
          assertThrows(
              IOException.class,
              () -> BrowserHttps.awaitTrustCommand(waiting, Duration.ofMillis(50)));
      assertTrue(error.getMessage().contains("timed out"));
      assertFalse(waiting.isAlive());
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(waiting);
    }
  }

  @Test
  void hardWorkerAbortAlsoStopsWaitingImporterBeforeParentCleanup() throws Exception {
    Process worker = child(WorkerChild.class);
    ProcessHandle importer = null;
    try {
      importer = ProcessHandle.of(Long.parseLong(worker.inputReader().readLine())).orElseThrow();
      assertTrue(importer.isAlive());
      BrowserParallelWorkers.stopWorker(worker);
      assertFalse(worker.isAlive());
      assertFalse(importer.isAlive());
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(worker);
      if (importer != null && importer.isAlive()) {
        importer.destroyForcibly();
        importer.onExit().get(10, TimeUnit.SECONDS);
      }
    }
  }

  private static Process child(Class<?> type, String... arguments) throws IOException {
    java.util.ArrayList<String> command =
        new java.util.ArrayList<>(
            List.of(
                BrowserParallelWorkers.javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                type.getName()));
    command.addAll(List.of(arguments));
    return new ProcessBuilder(command).start();
  }

  public static class ImportChild {
    public static void main(String[] args) {
      System.exit(Integer.parseInt(args[0]));
    }
  }

  public static class WorkerChild {
    public static void main(String[] args) throws Exception {
      Process importer = child(WaitingChild.class);
      if (!"ready".equals(importer.inputReader().readLine())) {
        throw new IOException("Importer did not start");
      }
      System.out.println(importer.pid());
      System.out.flush();
      // Die Fixture bleibt bis zum Elternabbruch bereit, spätestens 60 Sekunden.
      new CountDownLatch(1).await(60, TimeUnit.SECONDS);
    }
  }

  public static class WaitingChild {
    public static void main(String[] args) throws InterruptedException {
      System.out.println("ready");
      new CountDownLatch(1).await(60, TimeUnit.SECONDS);
    }
  }

  @Test
  void readinessRejectsHttpBeforeAnyConnectionOrCertificateRead() {
    Path absentCertificate = directory.resolve("absent.crt");
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserHttps.awaitReady("http://127.0.0.1:18081", absentCertificate));
  }
}
