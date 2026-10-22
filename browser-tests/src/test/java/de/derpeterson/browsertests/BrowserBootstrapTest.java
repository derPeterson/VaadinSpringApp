package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Prüft den echten PowerShell-Einstieg mit einem eigenen Maven-Ersatz ohne Build oder Appstart. */
@EnabledOnOs(OS.WINDOWS)
class BrowserBootstrapTest {
  @TempDir Path directory;

  @Test
  void reportingUsesOnlyTheDependentBrowserReactor() throws Exception {
    List<List<String>> calls = run("25.0.4", 0, 0, "-ReportingTests");
    assertEquals(
        List.of(
            List.of("--version"),
            List.of(
                "-f",
                directory.resolve("pom.xml").toString(),
                "-pl",
                "browser-tests",
                "-am",
                "test")),
        calls);
  }

  @Test
  void compilationFailureStopsBeforeStartingTheJavaRunner() throws Exception {
    List<List<String>> calls = run("25.0.4", 23, 23);
    assertEquals(2, calls.size());
    assertTrue(
        calls
            .get(1)
            .containsAll(
                List.of(
                    "-pl",
                    "browser-tests",
                    "-am",
                    "package",
                    "dependency:build-classpath",
                    "-DskipTests")));
  }

  @Test
  void wrongJavaIsRejectedBeforeAnyBuild() throws Exception {
    assertEquals(List.of(List.of("--version")), run("21.0.1", 0, 1, "-ReportingTests"));
  }

  private List<List<String>> run(
      String javaVersion, int buildExit, int expectedExit, String... options) throws Exception {
    Path module = Files.createDirectories(directory.resolve("browser-tests"));
    Files.copy(Path.of("run.ps1"), module.resolve("run.ps1"));
    Files.writeString(
        directory.resolve("mvnw.cmd"),
        """
        @echo off
        powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0maven-fixture.ps1" %*
        exit /b %errorlevel%
        """);
    Files.writeString(
        directory.resolve("maven-fixture.ps1"),
        """
        $ErrorActionPreference = 'Stop'
        [System.IO.File]::AppendAllText($env:BOOTSTRAP_FIXTURE_CALLS, (ConvertTo-Json -InputObject @($args) -Compress) + [Environment]::NewLine)
        if ($args -contains '--version') {
            Write-Output ('Java version: ' + $env:BOOTSTRAP_FIXTURE_JAVA + ', vendor: fixture')
            exit 0
        }
        exit ([int]$env:BOOTSTRAP_FIXTURE_EXIT)
        """);
    Path calls = directory.resolve("calls.jsonl");
    Path output = directory.resolve("output.txt");
    List<String> command =
        new ArrayList<>(
            List.of(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "RemoteSigned",
                "-File",
                module.resolve("run.ps1").toString()));
    command.addAll(Arrays.asList(options));
    ProcessBuilder builder =
        new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(output.toFile());
    builder.environment().put("BOOTSTRAP_FIXTURE_CALLS", calls.toString());
    builder.environment().put("BOOTSTRAP_FIXTURE_JAVA", javaVersion);
    builder.environment().put("BOOTSTRAP_FIXTURE_EXIT", Integer.toString(buildExit));
    Process process = builder.start();
    try {
      assertTrue(process.waitFor(30, TimeUnit.SECONDS), "PowerShell fixture did not terminate");
      assertEquals(
          expectedExit,
          process.exitValue(),
          () -> {
            try {
              return Files.readString(output);
            } catch (Exception failure) {
              return failure.toString();
            }
          });
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
    ObjectMapper json = new ObjectMapper();
    List<List<String>> result = new ArrayList<>();
    for (String line : Files.readAllLines(calls)) {
      result.add(Arrays.asList(json.readValue(line, String[].class)));
    }
    return result;
  }
}
