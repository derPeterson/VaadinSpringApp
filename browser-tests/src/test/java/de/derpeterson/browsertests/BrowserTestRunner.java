package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Playwright;
import de.derpeterson.browsertests.BrowserParallelWorkers.Worker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Starts only disposable Docker data, then delegates tests to JUnit and HTML to Allure. */
public final class BrowserTestRunner {
  private static BrowserTestRunner activeTestRunner;
  private static final ObjectMapper JSON = new ObjectMapper();
  private final Path browserTestModulePath, repositoryPath, runDirectory;
  private final Map<String, Object> runMetadata = new LinkedHashMap<>();
  private final String dockerProjectName;
  private final String dockerExecutable;
  private final List<String> composeCommand;
  private final Map<String, String> testEnvironment = new HashMap<>(System.getenv());
  private final boolean buildImage;
  private final boolean collectServerCoverage;
  private String appContainer;
  private final List<Class<?>> testClasses;
  private boolean environmentStarted;
  private volatile boolean shuttingDown;
  private volatile Process activeCommand;
  private final int workerCount;
  private final boolean worker;
  private final String suiteName;
  private BrowserParallelWorkers browserParallelWorkers;
  private final Thread shutdownHook;

  private BrowserTestRunner(Path browserTestModulePath, Set<String> options, String suiteName)
      throws Exception {
    this.browserTestModulePath = browserTestModulePath.toAbsolutePath().normalize();
    this.suiteName = options.contains("--failure-probe") ? "reporting-probe" : suiteName;
    workerCount = Integer.parseInt(System.getProperty("browser.tests.workers", "1"));
    if (workerCount < 1 || workerCount > 4) {
      throw new IllegalArgumentException("Use 1 to 4 workers.");
    }
    worker = System.getProperty("browser.tests.workerRun") != null;
    repositoryPath = this.browserTestModulePath.getParent();
    buildImage = !options.contains("--no-build");
    collectServerCoverage = options.contains("--coverage");
    testClasses =
        options.contains("--failure-probe")
            ? List.of(FailureProbeTest.class)
            : BrowserExecutionPlan.workerClasses(
                suiteName, System.getProperty("browser.tests.workerClasses"));
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String commit = git("rev-parse", "HEAD");
    // Ersetzt einen Punkt plus Sekundenbruchteil am Ende vor Z durch _UTC; $ verlangt das
    // Zeichenfolgenende.
    String runId =
        Instant.now().toString().replace(':', '-').replaceAll("\\.\\d+Z$", "_UTC")
            + "__"
            + commit.substring(0, 8)
            + "__"
            + suffix;
    Path runsRootPath =
        BrowserRunPaths.prepareRoot(BrowserRunPaths.configuredRoot(this.browserTestModulePath));
    Path requestedRunPath =
        worker
            ? Path.of(System.getProperty("browser.tests.workerRun"))
            : runsRootPath.resolve(runId);
    runDirectory = BrowserRunPaths.createRunDirectory(runsRootPath, requestedRunPath);
    testEnvironment.put(BrowserRunPaths.ROOT_ENVIRONMENT, runsRootPath.toString());
    testEnvironment.put("BROWSER_TEST_TLS_DIRECTORY", runDirectory.resolve("https").toString());
    // Auch die koordinierende Compose-Prüfung benötigt einen Wert. Jeder Worker erzeugt
    // beim eigenen Start erneut einen getrennten Schlüssel und übernimmt keinen Elternschlüssel.
    byte[] totpKey = new byte[32];
    new java.security.SecureRandom().nextBytes(totpKey);
    testEnvironment.put(
        "BROWSER_TEST_TOTP_KEY", java.util.Base64.getEncoder().encodeToString(totpKey));
    byte[] providerKey = new byte[32];
    new java.security.SecureRandom().nextBytes(providerKey);
    testEnvironment.put(
        "BROWSER_TEST_PROVIDER_KEY", java.util.Base64.getEncoder().encodeToString(providerKey));
    testEnvironment.put("BROWSER_TEST_DEVELOPMENT_OPTIONS_ENABLED", "false");
    Files.createDirectories(runDirectory.resolve("allure-results"));
    if (!worker) {
      Files.writeString(runsRootPath.resolve("latest.txt"), runId);
    }
    dockerProjectName =
        worker
            ? System.getProperty("browser.tests.workerProject")
            : "vaadinspringapp-e2e-java-" + suffix;
    // Nur den eigenen fest definierten Projektnamensraum für Start/Abbau zulassen.
    // ASCII-Kleinbuchstaben, Ziffern und Minus; + verlangt mindestens ein Suffixzeichen.
    if (dockerProjectName == null
        || !dockerProjectName.matches("vaadinspringapp-e2e-java-[a-z0-9-]+")) {
      throw new IllegalArgumentException("Invalid worker project.");
    }
    dockerExecutable = dockerExecutable();
    ArrayList<String> composeArguments =
        new ArrayList<>(
            List.of(
                dockerExecutable,
                "compose",
                "-f",
                repositoryPath.resolve("compose.browser-tests.yaml").toString()));
    if (collectServerCoverage) {
      composeArguments.addAll(
          List.of("-f", BrowserCoverage.prepare(browserTestModulePath, runDirectory).toString()));
    }
    composeArguments.addAll(List.of("-p", dockerProjectName));
    composeCommand = List.copyOf(composeArguments);
    runMetadata.put("runId", worker ? runDirectory.getFileName().toString() : runId);
    runMetadata.put("runsRoot", runsRootPath.toString());
    runMetadata.put("workersRequested", workerCount);
    runMetadata.put("testClasses", testClasses.stream().map(Class::getSimpleName).toList());
    runMetadata.put("startedAt", Instant.now().toString());
    runMetadata.put("commit", commit);
    runMetadata.put("branch", git("branch", "--show-current"));
    runMetadata.put("gitStatus", git("status", "--porcelain"));
    runMetadata.put("project", dockerProjectName);
    runMetadata.put("build", buildImage);
    runMetadata.put("coverage", collectServerCoverage ? "pending" : "disabled");
    runMetadata.put("failureProbe", options.contains("--failure-probe"));
    runMetadata.put("uiLanguage", System.getProperty("browser.tests.language"));
    runMetadata.put("suite", options.contains("--failure-probe") ? "reporting-probe" : suiteName);
    runMetadata.put("setup", "not-started");
    runMetadata.put("tests", "not-started");
    runMetadata.put("cleanup", "not-needed");
    runMetadata.put("html", "not-started");
    save();
    shutdownHook =
        new Thread(
            () -> {
              shuttingDown = true;
              try {
                BrowserHttps.stopBeforeTrustCleanup(activeCommand);
                if (browserParallelWorkers != null) {
                  cleanupWorkers();
                } else {
                  cleanup();
                }
              } catch (Exception error) {
                runMetadata.put("cleanup", "failed");
                runMetadata.put("cleanupError", error.toString());
              }
              if (oidcTestProvider != null) {
                oidcTestProvider.close();
              }
              runMetadata.put("tests", "interrupted");
              runMetadata.put(
                  "error",
                  "Runner interrupted before normal completion; this run is not a passed test result.");
              runMetadata.put("finishedAt", Instant.now().toString());
              try {
                save();
              } catch (IOException _) {
                /* Existing metadata still cannot certify success. */
              }
            },
            "browser-test-cleanup");
  }

  public static void main(String[] args) throws Exception {
    if (Runtime.version().feature() != 25) {
      throw new IllegalStateException("Java 25 is required.");
    }
    Path browserTestModulePath = Path.of(args[0]).toAbsolutePath().normalize();
    Set<String> options = new HashSet<>(Arrays.asList(args).subList(1, args.length));
    if (options.contains("--setup-https") || options.contains("--remove-https")) {
      if (options.contains("--setup-https") && options.contains("--remove-https")) {
        throw new IllegalArgumentException("Choose HTTPS setup or removal, not both");
      }
      if (options.contains("--setup-https")) {
        BrowserHttpsSetup.setup(browserTestModulePath.getParent());
      } else {
        BrowserHttpsSetup.remove(browserTestModulePath.getParent());
      }
      return;
    }
    if (options.contains("--report")) {
      Path report;
      int reportPathIndex = Arrays.asList(args).indexOf("--report-path");
      if (reportPathIndex >= 0) {
        if (reportPathIndex + 1 >= args.length) {
          throw new IllegalArgumentException("Report path missing.");
        }
        report = Path.of(args[reportPathIndex + 1]).toAbsolutePath().normalize();
      } else {
        report =
            BrowserRunPaths.latestReport(BrowserRunPaths.configuredRoot(browserTestModulePath));
      }
      if (!Files.isRegularFile(report.resolve("index.html"))) {
        throw new IOException("HTML report missing: " + report);
      }
      Process testProcess =
          new ProcessBuilder(
                  allureCommand(
                      browserTestModulePath, "open", report.toString(), "--host", "127.0.0.1"))
              .inheritIO()
              .start();
      Runtime.getRuntime().addShutdownHook(new Thread(testProcess::destroy));
      System.exit(testProcess.waitFor());
    }

    int languageIndex = Arrays.asList(args).indexOf("--language");
    if (languageIndex >= 0 && languageIndex + 1 >= args.length) {
      throw new IllegalArgumentException("Language missing.");
    }
    String language = languageIndex >= 0 ? args[languageIndex + 1] : "de";
    new de.derpeterson.browsertests.i18n.UiTexts(
        language); // Validate before creating a Docker environment.
    System.setProperty("browser.tests.language", language);

    int suiteIndex = Arrays.asList(args).indexOf("--suite");
    if (suiteIndex >= 0 && suiteIndex + 1 >= args.length) {
      throw new IllegalArgumentException("Suite name missing.");
    }
    String suiteName = suiteIndex >= 0 ? args[suiteIndex + 1] : "all";

    int workersIndex = Arrays.asList(args).indexOf("--workers");
    if (workersIndex >= 0 && workersIndex + 1 >= args.length) {
      throw new IllegalArgumentException("Worker count missing.");
    }
    System.setProperty("browser.tests.workers", workersIndex >= 0 ? args[workersIndex + 1] : "1");

    Path authority = BrowserTestCa.directory(browserTestModulePath.getParent());
    BrowserTestCa.requireTrusted(authority);
    int exit;
    try (var _ = BrowserTestCa.lease(authority, true)) {
      BrowserTestCa.requireTrusted(authority);
      BrowserTestRunner browserTestRunner =
          new BrowserTestRunner(browserTestModulePath, options, suiteName);
      exit =
          browserTestRunner.workerCount > 1
              ? browserTestRunner.executeParallel(options)
              : browserTestRunner.execute(options);
    }
    System.exit(exit);
  }

  private OidcTestProvider oidcTestProvider;

  private int execute(Set<String> options) throws Exception {
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    int exitCode = 1;
    try {
      int appPort = port("BROWSER_TEST_APP_PORT", 18081);
      int mailPort = port("BROWSER_TEST_MAILPIT_PORT", 18026);
      int httpPort = port("BROWSER_TEST_HTTP_PORT", 18082);
      if (appPort == mailPort || appPort == httpPort || mailPort == httpPort) {
        throw new IllegalArgumentException("Test ports must differ.");
      }
      testEnvironment.put("BROWSER_TEST_APP_PORT", String.valueOf(appPort));
      testEnvironment.put("BROWSER_TEST_MAILPIT_PORT", String.valueOf(mailPort));
      testEnvironment.put("BROWSER_TEST_HTTP_PORT", String.valueOf(httpPort));
      String appUrl = "https://127.0.0.1:" + appPort;
      String mailUrl = "http://127.0.0.1:" + mailPort;
      if (testClasses.contains(de.derpeterson.browsertests.suites.ExternalLoginBrowserTest.class)
          || testClasses.contains(de.derpeterson.browsertests.suites.TwoFactorBrowserTest.class)
          || testClasses.contains(
              de.derpeterson.browsertests.suites.TwoFactorLifecycleBrowserTest.class)
          || testClasses.contains(
              de.derpeterson.browsertests.suites.ExternalLoginLayoutBrowserTest.class)) {
        oidcTestProvider = new OidcTestProvider(appUrl);
        oidcTestProvider.configure(testEnvironment);
        runMetadata.put("oidcFixture", testEnvironment.get("BROWSER_TEST_OIDC_GOOGLE_AUTH"));
      }
      runMetadata.put("appUrl", appUrl);
      runMetadata.put("mailpitUrl", mailUrl);
      System.setProperty("browser.tests.managed", "true");
      System.setProperty("browser.tests.baseUrl", appUrl);
      System.setProperty("browser.tests.httpUrl", "http://127.0.0.1:" + httpPort);
      System.setProperty("browser.tests.mailpitUrl", mailUrl);
      System.setProperty("browser.tests.headed", String.valueOf(options.contains("--headed")));
      System.setProperty(
          "allure.results.directory", runDirectory.resolve("allure-results").toString());
      save();

      Path certificates = runDirectory.resolve("https");
      BrowserTestCa.issue(BrowserTestCa.directory(repositoryPath), certificates);
      command(withCompose("config", "--quiet"));
      environmentStarted = true;
      command(
          withCompose(
              "up",
              "-d",
              "--wait",
              "--wait-timeout",
              "180",
              buildImage ? "--build" : "--no-build"));
      BrowserHttps.awaitReady(appUrl, certificates.resolve("root.crt"));
      runMetadata.put("setup", "passed");
      runMetadata.put("https", "certificate-chain-and-hostname-verified");
      appContainer = command(withCompose("ps", "-q", "app")).trim();
      runMetadata.put(
          "imageId",
          command(List.of(dockerExecutable, "inspect", "--format", "{{.Image}}", appContainer))
              .trim());
      save();
      SummaryGeneratingListener listener = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request = BrowserExecutionPlan.request(testClasses, suiteName);
      activeTestRunner = this;
      try {
        LauncherFactory.create().execute(request, listener);
      } finally {
        activeTestRunner = null;
      }
      TestExecutionSummary result = listener.getSummary();
      runMetadata.put(
          "browser",
          "Chromium " + System.getProperty("browser.tests.browserVersion", "nicht verfügbar"));
      runMetadata.put("viewport", "1280 × 900");
      runMetadata.put(
          "browserLocale", de.derpeterson.browsertests.i18n.UiTexts.selected().browserLocale());
      runMetadata.put("browserTimezone", "Europe/Berlin");
      runMetadata.put("testsFound", result.getTestsFoundCount());
      runMetadata.put("testsPassed", result.getTestsSucceededCount());
      runMetadata.put("testsSkipped", result.getTestsSkippedCount());
      runMetadata.put("testsFailed", result.getTestsFailedCount());
      boolean passed =
          result.getTestsFoundCount() > 0
              && result.getTestsSucceededCount() == result.getTestsFoundCount()
              && result.getTotalFailureCount() == 0;
      List<JsonNode> collected = MarkdownSummary.readAllureResults(runDirectory);

      if (collected.size() != result.getTestsFoundCount()) {
        passed = false;
        runMetadata.put("error", "JUnit and Allure case counts differ; incomplete evidence.");
      }

      if (result.getTotalFailureCount() > 0) {
        runMetadata.put(
            "error",
            result.getFailures().stream()
                .map(failure -> failure.getException().toString())
                .collect(java.util.stream.Collectors.joining("\n")));
      }
      runMetadata.put("tests", passed ? "passed" : "failed");
      result.printTo(new java.io.PrintWriter(System.out, true));
      result.printFailuresTo(new java.io.PrintWriter(System.out, true));
      exitCode = passed ? 0 : 1;
    } catch (Exception error) {
      runMetadata.put("error", error.toString());
      runMetadata.put("passed".equals(runMetadata.get("setup")) ? "tests" : "setup", "failed");
      System.err.println(error.getMessage());
    } finally {
      if (collectServerCoverage) {
        try {
          if (appContainer == null || appContainer.isBlank()) {
            throw new IOException("No test application started; coverage unavailable.");
          }
          command(withCompose("stop", "--timeout", "30", "app"));
          Path coverageFolder = runDirectory.resolve("coverage");
          command(
              List.of(
                  dockerExecutable,
                  "cp",
                  appContainer + ":/app/logs/browser-jacoco.exec",
                  coverageFolder.resolve("browser-jacoco.exec").toString()));
          if (!worker || Boolean.getBoolean("browser.tests.workerJar")) {
            command(
                List.of(
                    dockerExecutable,
                    "cp",
                    appContainer + ":/app/app.jar",
                    coverageFolder.resolve("app.jar").toString()));
            command(
                List.of(
                    dockerExecutable,
                    "cp",
                    appContainer + ":/app/coverage-sources.jar",
                    coverageFolder.resolve("coverage-sources.jar").toString()));
          }
          if (!worker) {
            BrowserCoverage.extractTestedCode(coverageFolder);
            command(BrowserCoverage.reportCommand(browserTestModulePath, coverageFolder));
          }
          runMetadata.put("coverage", "passed");
          runMetadata.put(
              "coverageReport",
              worker ? "../../coverage/html/index.html" : "coverage/html/index.html");
        } catch (Exception error) {
          runMetadata.put("coverage", "failed");
          runMetadata.put("coverageError", error.toString());
          exitCode = 1;
        }
      }
      try {
        // Auch ein Fehler vor Compose-up kann bereits einen eigenen Worker-Schlüssel hinterlassen.
        cleanup();
      } catch (Exception error) {
        runMetadata.put("cleanup", "failed");
        runMetadata.put("cleanupError", error.toString());
        exitCode = 1;
      }
      try {
        Files.writeString(
            runDirectory.resolve("allure-results/environment.properties"),
            "Java="
                + Runtime.version()
                + "\nPlaywright="
                + Playwright.class.getPackage().getImplementationVersion()
                + "\nGit="
                + runMetadata.get("commit")
                + "\nUI-Language="
                + runMetadata.get("uiLanguage")
                + "\nBranch="
                + runMetadata.get("branch")
                + "\nDocker="
                + dockerProjectName
                + "\n");
        if (!worker) {
          generateHtml();
        } else {
          runMetadata.put("html", "delegated-to-parent");
        }
      } catch (Exception error) {
        runMetadata.put("html", "failed");
        runMetadata.put("reportError", error.toString());
        exitCode = 1;
      }
      if (oidcTestProvider != null) {
        oidcTestProvider.close();
        oidcTestProvider = null;
      }
      runMetadata.put("finishedAt", Instant.now().toString());
      save();
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      System.out.println("\nJava-Browser-Testbericht: " + runDirectory.resolve("report.md"));
    }
    return exitCode;
  }

  private int executeParallel(Set<String> options) throws Exception {
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    int exitCode = 1;
    browserParallelWorkers = new BrowserParallelWorkers();
    List<Path> coverageData = new ArrayList<>();
    try {
      command(withCompose("config", "--quiet"));
      if (buildImage) {
        command(withCompose("build", "app"));
      }
      String image =
          command(
                  List.of(
                      dockerExecutable,
                      "image",
                      "inspect",
                      "--format",
                      "{{.Id}}",
                      testEnvironment.getOrDefault(
                          "BROWSER_TEST_APP_IMAGE", "vaadinspringapp-browser-tests-app:local")))
              .trim();
      // Alle Worker prüfen exakt dasselbe unveränderliche Image, auch wenn jemand das Tag neu baut.
      testEnvironment.put("BROWSER_TEST_APP_IMAGE", image);
      runMetadata.put("imageId", image);
      List<List<Class<?>>> groups =
          BrowserExecutionPlan.partition(testClasses, workerCount, suiteName);
      runMetadata.put("workersActual", groups.size());
      runMetadata.put(
          "workerPlan",
          groups.stream().map(group -> group.stream().map(Class::getSimpleName).toList()).toList());
      runMetadata.put("setup", "running");
      runMetadata.put("tests", "running");
      save();
      browserParallelWorkers.start(
          browserTestModulePath,
          runDirectory,
          suiteName,
          System.getProperty("browser.tests.language"),
          groups,
          testEnvironment,
          dockerProjectName,
          options.contains("--headed"),
          collectServerCoverage,
          options.contains("--failure-probe"));
      browserParallelWorkers.await();
      boolean passed = true, setupPassed = true;
      long found = 0, succeeded = 0, failed = 0, skipped = 0;
      List<Map<String, Object>> details = new ArrayList<>();

      for (Worker child : browserParallelWorkers.workers()) {
        JsonNode data = JSON.readTree(child.directory().resolve("metadata.json").toFile());
        List<JsonNode> evidence = MarkdownSummary.readAllureResults(child.directory());
        int exit = child.process().exitValue();
        passed &= BrowserParallelWorkers.successful(data, exit, image, evidence.size());
        passed &= BrowserParallelWorkers.matchesClasses(data, child.classes());
        passed &=
            data.path("uiLanguage").asText().equals(System.getProperty("browser.tests.language"));
        setupPassed &= data.path("setup").asText().equals("passed");
        found += data.path("testsFound").asLong();
        succeeded += data.path("testsPassed").asLong();
        failed += data.path("testsFailed").asLong();
        skipped += data.path("testsSkipped").asLong();
        details.add(
            Map.of(
                "worker",
                child.directory().getFileName().toString(),
                "project",
                child.project(),
                "exitCode",
                exit,
                "testsFound",
                data.path("testsFound").asInt(),
                "testsPassed",
                data.path("testsPassed").asInt(),
                "setup",
                data.path("setup").asText(),
                "cleanup",
                data.path("cleanup").asText(),
                "startedAt",
                data.path("startedAt").asText(),
                "finishedAt",
                data.path("finishedAt").asText()));
        BrowserParallelWorkers.collect(
            child.directory().resolve("allure-results"), runDirectory.resolve("allure-results"));
        if (collectServerCoverage) {
          if (!data.path("coverage").asText().equals("passed")) {
            throw new IOException("Worker coverage incomplete: " + child.directory());
          }
          coverageData.add(child.directory().resolve("coverage/browser-jacoco.exec"));
        }
        if (runMetadata.get("browser") == null) {
          for (String key : List.of("browser", "viewport", "browserLocale", "browserTimezone")) {
            runMetadata.put(key, data.path(key).asText());
          }
        }
      }
      runMetadata.put("workerResults", details);
      runMetadata.put("testsFound", found);
      runMetadata.put("testsPassed", succeeded);
      runMetadata.put("testsFailed", failed);
      runMetadata.put("testsSkipped", skipped);
      runMetadata.put("setup", setupPassed ? "passed" : "failed");
      // Jeder Fall genau einmal: fehlende und doppelte Nachweise verhindern ein grünes Ergebnis.
      List<JsonNode> results = MarkdownSummary.readAllureResults(runDirectory);
      Set<String> identities = new HashSet<>();

      for (JsonNode result : results) {
        String identity = result.path("fullName").asText() + result.path("parameters").toString();
        if (!identities.add(identity)) {
          passed = false;
        }
        if (!result.path("status").asText().equals("passed")) {
          passed = false;
        }
      }
      passed &=
          found > 0
              && results.size() == found
              && browserParallelWorkers.workers().size() == groups.size();
      runMetadata.put("tests", passed ? "passed" : "failed");
      if (!passed) {
        runMetadata.put(
            "error",
            "At least one worker, test or evidence check failed; inspect workers/*/metadata.json and console.log.");
      }
      if (collectServerCoverage) {
        Path folder = runDirectory.resolve("coverage");
        Files.createDirectories(folder);
        Files.move(
            browserParallelWorkers.workers().getFirst().directory().resolve("coverage/app.jar"),
            folder.resolve("app.jar"));
        Files.move(
            browserParallelWorkers
                .workers()
                .getFirst()
                .directory()
                .resolve("coverage/coverage-sources.jar"),
            folder.resolve("coverage-sources.jar"));
        ArrayList<String> merge =
            new ArrayList<>(
                List.of(
                    BrowserParallelWorkers.javaExecutable(),
                    "-jar",
                    browserTestModulePath.resolve("target/tools/jacococli.jar").toString(),
                    "merge"));
        merge.addAll(coverageData.stream().map(Path::toString).toList());
        merge.addAll(List.of("--destfile", folder.resolve("browser-jacoco.exec").toString()));
        command(merge);
        BrowserCoverage.extractTestedCode(folder);
        command(BrowserCoverage.reportCommand(browserTestModulePath, folder));
        runMetadata.put("coverage", "passed");
        runMetadata.put("coverageReport", "coverage/html/index.html");
      }
      exitCode = passed ? 0 : 1;
    } catch (Exception error) {
      runMetadata.put("error", error.toString());
      runMetadata.put("tests", "failed");
      if (collectServerCoverage) {
        runMetadata.put("coverage", "failed");
        runMetadata.put("coverageError", error.toString());
      }
      System.err.println(error.getMessage());
    } finally {
      try {
        cleanupWorkers();
      } catch (Exception error) {
        runMetadata.put("cleanup", "failed");
        runMetadata.put("cleanupError", error.toString());
        exitCode = 1;
      }
      try {
        generateHtml();
      } catch (Exception error) {
        runMetadata.put("html", "failed");
        runMetadata.put("reportError", error.toString());
        exitCode = 1;
      }
      runMetadata.put("finishedAt", Instant.now().toString());
      save();
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      System.out.println("Gemeinsamer Browser-Testbericht: " + runDirectory.resolve("report.md"));
    }
    return exitCode;
  }

  private synchronized void cleanupWorkers() throws Exception {
    if (browserParallelWorkers == null) {
      return;
    }
    browserParallelWorkers.stop();
    List<String> errors = new ArrayList<>();
    for (Worker child : browserParallelWorkers.workers()) {
      try {
        // Auch nach einem Prozessabbruch nur das eigene, vorher festgelegte Projekt abbauen.
        ArrayList<String> down =
            new ArrayList<>(
                List.of(
                    dockerExecutable,
                    "compose",
                    "-f",
                    repositoryPath.resolve("compose.browser-tests.yaml").toString(),
                    "-p",
                    child.project(),
                    "down",
                    "--volumes",
                    "--remove-orphans"));
        command(down);
      } catch (Exception error) {
        errors.add(child.project() + ": " + error);
      }
      try {
        BrowserTestCa.cleanupWorker(child.directory().resolve("https"));
      } catch (Exception error) {
        errors.add(child.project() + " TLS key cleanup: " + error);
      }
    }
    if (!errors.isEmpty()) {
      throw new IOException(String.join("\n", errors));
    }
    runMetadata.put("cleanup", "passed");
    save();
  }

  private void generateHtml() throws Exception {
    Files.writeString(
        runDirectory.resolve("allure-results/environment.properties"),
        "Java="
            + Runtime.version()
            + "\nPlaywright="
            + Playwright.class.getPackage().getImplementationVersion()
            + "\nGit="
            + runMetadata.get("commit")
            + "\nUI-Language="
            + runMetadata.get("uiLanguage")
            + "\nBranch="
            + runMetadata.get("branch")
            + "\nImage="
            + runMetadata.get("imageId")
            + "\nWorkers="
            + runMetadata.getOrDefault("workersActual", 1)
            + "\n");
    command(
        allureCommand(
            browserTestModulePath,
            "generate",
            runDirectory.resolve("allure-results").toString(),
            "--output",
            runDirectory.resolve("html").toString()));
    writeReportLauncher(runDirectory.resolve("html"), browserTestModulePath);
    runMetadata.put("html", "passed");
  }

  private synchronized void cleanup() throws Exception {
    try {
      if (environmentStarted) {
        command(withCompose("down", "--volumes", "--remove-orphans"));
        environmentStarted = false;
      }
    } finally {
      BrowserTestCa.cleanupWorker(runDirectory.resolve("https"));
    }
    runMetadata.put("cleanup", "passed");
    save();
  }

  private void save() throws IOException {
    JSON.writerWithDefaultPrettyPrinter()
        .writeValue(runDirectory.resolve("metadata.json").toFile(), runMetadata);
    if (!worker) {
      MarkdownSummary.writeRunReport(runDirectory, runMetadata);
    }
  }

  private String command(List<String> commandArguments) throws Exception {
    Path temporaryOutput = Files.createTempFile(runDirectory, "command-", ".tmp");
    Files.writeString(
        runDirectory.resolve("runner.log"),
        "\n> " + String.join(" ", commandArguments) + "\n",
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
    ProcessBuilder processBuilder =
        new ProcessBuilder(commandArguments)
            .directory(repositoryPath.toFile())
            .redirectErrorStream(true)
            .redirectOutput(temporaryOutput.toFile());
    processBuilder.environment().putAll(testEnvironment);
    if (Path.of(dockerExecutable).isAbsolute()) {
      processBuilder.environment().keySet().removeIf(key -> key.equalsIgnoreCase("PATH"));
      processBuilder
          .environment()
          .put(
              "Path",
              Path.of(dockerExecutable).getParent()
                  + java.io.File.pathSeparator
                  + System.getenv("PATH"));
    }
    Process child = processBuilder.start();
    activeCommand = child;
    Thread childShutdown = new Thread(child::destroy);
    boolean hookRegistered = false;
    if (!shuttingDown) {
      try {
        Runtime.getRuntime().addShutdownHook(childShutdown);
        hookRegistered = true;
      } catch (IllegalStateException _) {
        /* JVM shutdown began between the check and registration. */
      }
    }

    int commandExitCode;
    try {
      commandExitCode = child.waitFor();
    } finally {
      if (activeCommand == child) {
        activeCommand = null;
      }
      if (hookRegistered) {
        try {
          Runtime.getRuntime().removeShutdownHook(childShutdown);
        } catch (IllegalStateException _) {
          /* Shutdown hooks are already running. */
        }
      }
    }

    String commandOutputText = Files.readString(temporaryOutput);
    Files.writeString(
        runDirectory.resolve("runner.log"), commandOutputText, StandardOpenOption.APPEND);
    Files.delete(temporaryOutput);
    System.out.print(commandOutputText);
    if (commandExitCode != 0) {
      throw new IOException(
          "Process exited with "
              + commandExitCode
              + "; see runner.log: "
              + commandArguments.getFirst());
    }

    return commandOutputText;
  }

  /** Neustart ausschließlich der gerade vom Runner selbst erzeugten isolierten Testanwendung. */
  public static void restartTestApplication() throws Exception {
    BrowserTestRunner browserTestRunner = activeTestRunner;
    if (browserTestRunner == null
        || !browserTestRunner.environmentStarted
        || browserTestRunner.appContainer == null) {
      throw new IllegalStateException("No owned test application is running");
    }
    browserTestRunner.testEnvironment.put("BROWSER_TEST_BOOTSTRAP_ENABLED", "false");
    browserTestRunner.command(
        browserTestRunner.withCompose(
            "up",
            "-d",
            "--no-build",
            "--no-deps",
            "--force-recreate",
            "--wait",
            "--wait-timeout",
            "180",
            "app"));
    browserTestRunner.appContainer =
        browserTestRunner.command(browserTestRunner.withCompose("ps", "-q", "app")).trim();
  }

  /** Startfreigabe nur für die eigene isolierte Anwendung, mit unverändertem Schlüssel und H2. */
  public static void restartWithDevelopmentOptions(boolean enabled) throws Exception {
    BrowserTestRunner browserTestRunner = ownedRunner();
    browserTestRunner.testEnvironment.put(
        "BROWSER_TEST_DEVELOPMENT_OPTIONS_ENABLED", Boolean.toString(enabled));
    restartTestApplication();
  }

  /** Ausschließlich der Maildienst des aktiven eigenen Workers; keine fremden Dienste. */
  public static void setTestMailpitAvailable(boolean available) throws Exception {
    BrowserTestRunner browserTestRunner = ownedRunner();
    browserTestRunner.command(
        available
            ? browserTestRunner.withCompose("up", "-d", "--no-deps", "--wait", "mailpit")
            : browserTestRunner.withCompose("stop", "mailpit"));
  }

  public static String testApplicationLogs() throws Exception {
    BrowserTestRunner browserTestRunner = ownedRunner();
    return browserTestRunner.command(
        browserTestRunner.withCompose("logs", "--no-color", "--tail", "250", "app"));
  }

  public static JsonNode testMailpitMessages(boolean secondary) throws Exception {
    BrowserTestRunner browserTestRunner = ownedRunner();
    String host = secondary ? "mailpit-secondary" : "mailpit";
    return JSON.readTree(
        browserTestRunner.command(
            browserTestRunner.withCompose(
                "exec",
                "-T",
                "app",
                "curl",
                "--fail",
                "--silent",
                "http://" + host + ":8025/api/v1/messages?limit=100")));
  }

  private static BrowserTestRunner ownedRunner() {
    BrowserTestRunner browserTestRunner = activeTestRunner;
    if (browserTestRunner == null
        || !browserTestRunner.environmentStarted
        || browserTestRunner.appContainer == null) {
      throw new IllegalStateException("No owned test application is running");
    }
    return browserTestRunner;
  }

  private List<String> withCompose(String... args) {
    ArrayList<String> result = new ArrayList<>(composeCommand);
    result.addAll(List.of(args));
    return result;
  }

  private String git(String... gitArguments) throws Exception {
    ArrayList<String> gitCommandArguments = new ArrayList<>(List.of("git"));
    gitCommandArguments.addAll(List.of(gitArguments));
    Process gitProcess =
        new ProcessBuilder(gitCommandArguments)
            .directory(repositoryPath.toFile())
            .redirectErrorStream(true)
            .start();
    String gitOutputText =
        new String(
                gitProcess.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
            .trim();
    if (gitProcess.waitFor() != 0) {
      throw new IOException("Cannot determine Git state: " + gitOutputText);
    }

    return gitOutputText;
  }

  private static int port(String portVariableName, int defaultPort) {
    int configuredPort =
        Integer.parseInt(
            System.getenv().getOrDefault(portVariableName, String.valueOf(defaultPort)));
    if (configuredPort < 1024
        || configuredPort > 65535
        || configuredPort == 18080
        || configuredPort == 18025
        || configuredPort == 18443
        || configuredPort == 8443) {
      throw new IllegalArgumentException(
          "Use free test ports from 1024 to 65535, excluding manual ports 8443/18080/18025/18443.");
    }
    return configuredPort;
  }

  private static String dockerExecutable() {
    if (System.getenv("DOCKER_BIN") != null) {
      return System.getenv("DOCKER_BIN");
    }
    for (String candidate :
        List.of(
            System.getenv().getOrDefault("LOCALAPPDATA", "")
                + "/Programs/DockerDesktop/resources/bin/docker.exe",
            System.getenv().getOrDefault("ProgramFiles", "")
                + "/Docker/Docker/resources/bin/docker.exe")) {
      if (Files.isRegularFile(Path.of(candidate))) {
        return candidate;
      }
    }
    return "docker";
  }

  /** Öffnet auch externe Berichte über den ursprünglichen Projektstarter. */
  static void writeReportLauncher(Path htmlDirectoryPath, Path browserTestModulePath)
      throws IOException {
    String starterPathText =
        browserTestModulePath.resolve("run.ps1").toAbsolutePath().normalize().toString();
    String reportCommandText =
        "& '"
            + starterPathText.replace("'", "''")
            + "' -Report -ReportPath $env:BROWSER_REPORT_DIRECTORY; exit $LASTEXITCODE";
    String encodedCommandText =
        java.util.Base64.getEncoder()
            .encodeToString(reportCommandText.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));
    String launcherText =
        """
        @echo off
        setlocal DisableDelayedExpansion
        set "BROWSER_REPORT_DIRECTORY=%%~dp0."
        powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -EncodedCommand %s
        set "reportExit=%%errorlevel%%"
        if not "%%reportExit%%"=="0" pause
        exit /b %%reportExit%%
        """
            .formatted(encodedCommandText);
    Files.writeString(
        htmlDirectoryPath.resolve("open-report.bat"),
        launcherText.replace("\n", "\r\n"),
        java.nio.charset.StandardCharsets.US_ASCII);
  }

  private static List<String> allureCommand(Path browserTestModulePath, String... args)
      throws IOException {
    Path tools = browserTestModulePath.resolve("target/tools");
    Path lib;
    try (Stream<Path> entries = Files.list(tools)) {
      lib =
          entries
              .filter(path -> path.getFileName().toString().startsWith("allure-"))
              .map(path -> path.resolve("lib"))
              .filter(Files::isDirectory)
              .findFirst()
              .orElseThrow(() -> new IOException("Allure tools missing; run test-compile first."));
    }
    ArrayList<String> result =
        new ArrayList<>(
            List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dfile.encoding=UTF-8",
                "-Dallure.home=" + lib.getParent(),
                "-cp",
                lib
                    + java.io.File.separator
                    + "*"
                    + java.io.File.pathSeparator
                    + lib.resolve("config"),
                "io.qameta.allure.CommandLine"));
    result.addAll(List.of(args));
    return result;
  }
}
