package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BrowserRunPathsTest {
  @TempDir Path temporaryDirectoryPath;

  @Test
  void defaultPathResolutionDoesNotCreateEvidenceDirectories() throws Exception {
    Path modulePath = temporaryDirectoryPath.resolve("browser-tests");

    assertEquals(modulePath.resolve("runs"), configuredRoot(modulePath));
    assertFalse(Files.exists(modulePath));
  }

  @Test
  void localSettingsSupportExternalPathsWithSpacesAndRelativePaths() throws Exception {
    Path modulePath = Files.createDirectory(temporaryDirectoryPath.resolve("module"));
    Path externalRootPath = temporaryDirectoryPath.resolve("external evidence");
    Path localSettingsPath = modulePath.resolve("local.properties");
    Files.writeString(
        localSettingsPath, "runs.directory=" + externalRootPath.toString().replace('\\', '/'));

    assertEquals(externalRootPath, configuredRoot(modulePath));

    Files.writeString(localSettingsPath, "\uFEFFruns.directory=../relative evidence");
    assertEquals(temporaryDirectoryPath.resolve("relative evidence"), configuredRoot(modulePath));
  }

  @Test
  void explicitPropertyOverridesEnvironmentAndLocalSettings() throws Exception {
    Path modulePath = Files.createDirectory(temporaryDirectoryPath.resolve("module"));
    Files.writeString(modulePath.resolve("local.properties"), "runs.directory=from-file");
    Properties systemProperties = new Properties();
    systemProperties.setProperty(BrowserRunPaths.ROOT_PROPERTY, "from-property");
    Map<String, String> environmentVariables = Map.of(BrowserRunPaths.ROOT_ENVIRONMENT, "from-env");

    assertEquals(
        modulePath.resolve("from-property"),
        BrowserRunPaths.configuredRoot(modulePath, systemProperties, environmentVariables));

    systemProperties.clear();
    assertEquals(
        modulePath.resolve("from-env"),
        BrowserRunPaths.configuredRoot(modulePath, systemProperties, environmentVariables));
  }

  @Test
  void invalidExplicitConfigurationNeverFallsBackToModuleRuns() throws Exception {
    Path modulePath = Files.createDirectory(temporaryDirectoryPath.resolve("module"));
    Path localSettingsPath = modulePath.resolve("local.properties");
    Files.writeString(localSettingsPath, "runs.directory=   ");
    assertThrows(IOException.class, () -> configuredRoot(modulePath));

    Files.writeString(localSettingsPath, "mistyped.directory=somewhere");
    assertThrows(IOException.class, () -> configuredRoot(modulePath));
    assertFalse(Files.exists(modulePath.resolve("runs")));
  }

  @Test
  void rootAndWorkersUseTheExternalDirectoryWithoutLeavingWriteProbes() throws Exception {
    Path externalRootPath = temporaryDirectoryPath.resolve("external evidence");
    Path preparedRootPath = BrowserRunPaths.prepareRoot(externalRootPath);
    Path coordinatorPath =
        BrowserRunPaths.createRunDirectory(preparedRootPath, preparedRootPath.resolve("run-1"));
    Path workerPath =
        BrowserRunPaths.createRunDirectory(preparedRootPath, coordinatorPath.resolve("workers/1"));

    assertEquals(coordinatorPath.resolve("workers/1"), workerPath);
    assertTrue(Files.isDirectory(workerPath));
    try (Stream<Path> rootEntries = Files.list(preparedRootPath)) {
      assertEquals(1, rootEntries.count());
    }
  }

  @Test
  void workerCannotEscapeTheConfiguredRootOrUseTheRootItself() throws Exception {
    Path preparedRootPath = BrowserRunPaths.prepareRoot(temporaryDirectoryPath.resolve("runs"));
    Path siblingPath = temporaryDirectoryPath.resolve("runs-other/worker");

    assertThrows(
        IOException.class, () -> BrowserRunPaths.createRunDirectory(preparedRootPath, siblingPath));
    assertThrows(
        IOException.class,
        () -> BrowserRunPaths.createRunDirectory(preparedRootPath, preparedRootPath));
    assertThrows(
        IOException.class,
        () ->
            BrowserRunPaths.createRunDirectory(
                preparedRootPath, preparedRootPath.resolve("../escape")));
    assertFalse(Files.exists(siblingPath.getParent()));
    assertFalse(Files.exists(temporaryDirectoryPath.resolve("escape")));
  }

  @Test
  void aFileInsteadOfADirectoryFailsBeforeCreatingAnyRun() throws Exception {
    Path occupiedPath = Files.writeString(temporaryDirectoryPath.resolve("occupied"), "existing");

    IOException pathFailure =
        assertThrows(IOException.class, () -> BrowserRunPaths.prepareRoot(occupiedPath));

    assertTrue(pathFailure.getMessage().contains(occupiedPath.toString()));
    assertEquals("existing", Files.readString(occupiedPath));
  }

  @Test
  void latestReportIsResolvedFromExternalRoot() throws Exception {
    Path rootPath =
        BrowserRunPaths.prepareRoot(temporaryDirectoryPath.resolve("external evidence"));
    Path reportPath = Files.createDirectories(rootPath.resolve("run-1/html"));
    Files.writeString(rootPath.resolve("latest.txt"), "run-1\n");

    assertEquals(reportPath.toRealPath(), BrowserRunPaths.latestReport(rootPath));
  }

  @ParameterizedTest
  @ValueSource(strings = {"..", ".", "../escape", "run/other", "run\\other", ""})
  void latestReportRejectsPathTraversal(String invalidRunId) throws Exception {
    Path rootPath = BrowserRunPaths.prepareRoot(temporaryDirectoryPath.resolve("runs"));
    Files.writeString(rootPath.resolve("latest.txt"), invalidRunId);

    assertThrows(IOException.class, () -> BrowserRunPaths.latestReport(rootPath));
  }

  @Test
  void externalReportLauncherPreservesSpacesAndQuotesWithoutShellInterpolation() throws Exception {
    Path modulePath = temporaryDirectoryPath.resolve("project with ' quote/browser-tests");
    Path htmlPath = Files.createDirectories(temporaryDirectoryPath.resolve("external report/html"));
    BrowserTestRunner.writeReportLauncher(htmlPath, modulePath);
    String launcherText = Files.readString(htmlPath.resolve("open-report.bat"));
    String encodedCommandText =
        launcherText
            .lines()
            .filter(commandLineText -> commandLineText.startsWith("powershell.exe "))
            .findFirst()
            .orElseThrow()
            .split("-EncodedCommand ")[1];
    String decodedCommandText =
        new String(Base64.getDecoder().decode(encodedCommandText), StandardCharsets.UTF_16LE);

    assertEquals(
        "& '"
            + modulePath
                .resolve("run.ps1")
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace("'", "''")
            + "' -Report -ReportPath $env:BROWSER_REPORT_DIRECTORY; exit $LASTEXITCODE",
        decodedCommandText);
    assertTrue(launcherText.contains("set \"BROWSER_REPORT_DIRECTORY=%~dp0.\""));
    assertTrue(launcherText.contains("set \"reportExit=%errorlevel%\""));
    assertFalse(launcherText.contains("..\\..\\.."));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void aWindowsJunctionCannotRedirectWorkerOutputOutsideTheConfiguredRoot() throws Exception {
    Path rootPath = BrowserRunPaths.prepareRoot(temporaryDirectoryPath.resolve("runs"));
    Path outsidePath = Files.createDirectory(temporaryDirectoryPath.resolve("outside"));
    Path junctionPath = rootPath.resolve("junction");
    Process junctionProcess =
        new ProcessBuilder(
                "cmd.exe", "/c", "mklink", "/J", junctionPath.toString(), outsidePath.toString())
            .redirectErrorStream(true)
            .start();
    try {
      assertTrue(junctionProcess.waitFor(10, TimeUnit.SECONDS));
      assertEquals(
          0,
          junctionProcess.exitValue(),
          new String(junctionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
      assertThrows(
          IOException.class,
          () -> BrowserRunPaths.createRunDirectory(rootPath, junctionPath.resolve("worker")));
      assertFalse(Files.exists(outsidePath.resolve("worker")));
    } finally {
      junctionProcess.destroy();
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void externalBatchLauncherInvokesTheCorrectProjectAndPassesItsOwnReportPath() throws Exception {
    Path modulePath =
        Files.createDirectories(
            temporaryDirectoryPath.resolve("project with ' quote/browser-tests"));
    Path reportPath =
        Files.createDirectories(temporaryDirectoryPath.resolve("external report/html"));
    Path capturedPathFile = modulePath.resolve("captured-report-path.txt");
    Files.writeString(
        modulePath.resolve("run.ps1"),
        """
        param([switch]$Report, [string]$ReportPath)
        if (-not $Report) { exit 19 }
        Set-Content -LiteralPath (Join-Path $PSScriptRoot 'captured-report-path.txt') -Value $ReportPath -NoNewline -Encoding UTF8
        exit 0
        """);
    BrowserTestRunner.writeReportLauncher(reportPath, modulePath);

    Process reportProcess =
        new ProcessBuilder("cmd.exe", "/c", reportPath.resolve("open-report.bat").toString())
            .redirectErrorStream(true)
            .redirectOutput(temporaryDirectoryPath.resolve("launcher-output.txt").toFile())
            .start();
    try {
      assertTrue(reportProcess.waitFor(20, TimeUnit.SECONDS));
      assertEquals(0, reportProcess.exitValue());
      String capturedPathText = Files.readString(capturedPathFile).replace("\uFEFF", "");
      assertEquals(reportPath, Path.of(capturedPathText).normalize());
    } finally {
      reportProcess.destroy();
    }
  }

  private static Path configuredRoot(Path modulePath) throws IOException {
    return BrowserRunPaths.configuredRoot(modulePath, new Properties(), Map.of());
  }
}
