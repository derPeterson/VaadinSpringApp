package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class BrowserHttpsTest {
  @TempDir Path directory;

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
        param([string]$Action, [string]$Certificate, [string]$Intermediate)
        Write-Output $Action
        Write-Output $Certificate
        Write-Output $Intermediate
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
          List.of(
              "Add",
              certificates.resolve("root.crt").toString(),
              certificates.resolve("intermediate.crt").toString(),
              "RemoteSigned"),
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

  public static class WaitingChild {
    public static void main(String[] args) throws InterruptedException {
      System.out.println("ready");
      Thread.sleep(60000);
    }
  }

  @Test
  void readinessRejectsHttpBeforeAnyConnectionOrCertificateRead() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserHttps.awaitReady("http://127.0.0.1:18081", directory.resolve("absent.crt")));
  }
}
