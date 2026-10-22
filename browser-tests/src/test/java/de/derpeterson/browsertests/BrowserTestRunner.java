package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final Path module, repo, run;
  private final Map<String, Object> metadata = new LinkedHashMap<>();
  private final String project;
  private final String docker;
  private final List<String> compose;
  private final Map<String, String> environment = new HashMap<>(System.getenv());
  private final boolean build;
  private final boolean coverage;
  private String appContainer;
  private final List<Class<?>> testClasses;
  private boolean environmentStarted;
  private volatile boolean shuttingDown;
  private volatile Process activeCommand;
  private final int workers;
  private final boolean worker;
  private final String suite;
  private BrowserParallelWorkers parallel;
  private final Thread shutdownHook;

  private BrowserTestRunner(Path module, Set<String> options, String suite) throws Exception {
    this.module = module.toAbsolutePath().normalize();
    this.suite = options.contains("--failure-probe") ? "reporting-probe" : suite;
    workers = Integer.parseInt(System.getProperty("browser.tests.workers", "1"));
    if (workers < 1 || workers > 4) {
      throw new IllegalArgumentException("Use 1 to 4 workers.");
    }
    worker = System.getProperty("browser.tests.workerRun") != null;
    repo = this.module.getParent();
    build = !options.contains("--no-build");
    coverage = options.contains("--coverage");
    testClasses =
        options.contains("--failure-probe")
            ? List.of(FailureProbeTest.class)
            : BrowserExecutionPlan.workerClasses(
                suite, System.getProperty("browser.tests.workerClasses"));
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
    run =
        worker
            ? Path.of(System.getProperty("browser.tests.workerRun")).toAbsolutePath().normalize()
            : this.module.resolve("runs").resolve(runId);
    environment.put("BROWSER_TEST_TLS_DIRECTORY", run.resolve("https").toString());
    if (worker && !run.startsWith(this.module.resolve("runs"))) {
      throw new IllegalArgumentException("Worker output must be within runs/.");
    }
    Files.createDirectories(run.resolve("allure-results"));
    if (!worker) {
      Files.writeString(this.module.resolve("runs/latest.txt"), runId);
    }
    project =
        worker
            ? System.getProperty("browser.tests.workerProject")
            : "vaadinspringapp-e2e-java-" + suffix;
    // Nur den eigenen fest definierten Projektnamensraum für Start/Abbau zulassen.
    // ASCII-Kleinbuchstaben, Ziffern und Minus; + verlangt mindestens ein Suffixzeichen.
    if (project == null || !project.matches("vaadinspringapp-e2e-java-[a-z0-9-]+")) {
      throw new IllegalArgumentException("Invalid worker project.");
    }
    docker = dockerExecutable();
    ArrayList<String> composeArguments =
        new ArrayList<>(
            List.of(
                docker, "compose", "-f", repo.resolve("compose.browser-tests.yaml").toString()));
    if (coverage) {
      composeArguments.addAll(List.of("-f", BrowserCoverage.prepare(module, run).toString()));
    }
    composeArguments.addAll(List.of("-p", project));
    compose = List.copyOf(composeArguments);
    metadata.put("runId", worker ? run.getFileName().toString() : runId);
    metadata.put("workersRequested", workers);
    metadata.put("testClasses", testClasses.stream().map(Class::getSimpleName).toList());
    metadata.put("startedAt", Instant.now().toString());
    metadata.put("commit", commit);
    metadata.put("branch", git("branch", "--show-current"));
    metadata.put("gitStatus", git("status", "--porcelain"));
    metadata.put("project", project);
    metadata.put("build", build);
    metadata.put("coverage", coverage ? "pending" : "disabled");
    metadata.put("failureProbe", options.contains("--failure-probe"));
    metadata.put("uiLanguage", System.getProperty("browser.tests.language"));
    metadata.put("suite", options.contains("--failure-probe") ? "reporting-probe" : suite);
    metadata.put("setup", "not-started");
    metadata.put("tests", "not-started");
    metadata.put("cleanup", "not-needed");
    metadata.put("html", "not-started");
    save();
    shutdownHook =
        new Thread(
            () -> {
              shuttingDown = true;
              try {
                BrowserHttps.stopBeforeTrustCleanup(activeCommand);
                if (parallel != null) {
                  cleanupWorkers();
                } else {
                  cleanup();
                }
              } catch (Exception error) {
                metadata.put("cleanup", "failed");
                metadata.put("cleanupError", error.toString());
              }
              metadata.put("tests", "interrupted");
              metadata.put(
                  "error",
                  "Runner interrupted before normal completion; this run is not a passed test result.");
              metadata.put("finishedAt", Instant.now().toString());
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
    Path module = Path.of(args[0]).toAbsolutePath().normalize();
    Set<String> options = new HashSet<>(Arrays.asList(args).subList(1, args.length));
    if (options.contains("--setup-https") || options.contains("--remove-https")) {
      if (options.contains("--setup-https") && options.contains("--remove-https")) {
        throw new IllegalArgumentException("Choose HTTPS setup or removal, not both");
      }
      if (options.contains("--setup-https")) {
        BrowserHttpsSetup.setup(module.getParent());
      } else {
        BrowserHttpsSetup.remove(module.getParent());
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
        String latest = Files.readString(module.resolve("runs/latest.txt")).trim();
        // Erlaubt nur ASCII-Buchstaben/Ziffern/Unterstrich, Punkt und Minus, also keine
        // Pfadtrenner; mindestens ein Zeichen.
        if (!latest.matches("[\\w.-]+")) {
          throw new IllegalArgumentException("Invalid report path.");
        }
        report = module.resolve("runs").resolve(latest).resolve("html");
      }
      if (!Files.isRegularFile(report.resolve("index.html"))) {
        throw new IOException("HTML report missing: " + report);
      }
      Process process =
          new ProcessBuilder(
                  allureCommand(module, "open", report.toString(), "--host", "127.0.0.1"))
              .inheritIO()
              .start();
      Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
      System.exit(process.waitFor());
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
    String suite = suiteIndex >= 0 ? args[suiteIndex + 1] : "all";
    int workersIndex = Arrays.asList(args).indexOf("--workers");
    if (workersIndex >= 0 && workersIndex + 1 >= args.length) {
      throw new IllegalArgumentException("Worker count missing.");
    }
    System.setProperty("browser.tests.workers", workersIndex >= 0 ? args[workersIndex + 1] : "1");
    Path authority = BrowserTestCa.directory(module.getParent());
    BrowserTestCa.requireTrusted(authority);
    int exit;
    try (var _ = BrowserTestCa.lease(authority, true)) {
      BrowserTestCa.requireTrusted(authority);
      BrowserTestRunner runner = new BrowserTestRunner(module, options, suite);
      exit = runner.workers > 1 ? runner.executeParallel(options) : runner.execute(options);
    }
    System.exit(exit);
  }

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
      environment.put("BROWSER_TEST_APP_PORT", String.valueOf(appPort));
      environment.put("BROWSER_TEST_MAILPIT_PORT", String.valueOf(mailPort));
      environment.put("BROWSER_TEST_HTTP_PORT", String.valueOf(httpPort));
      String appUrl = "https://127.0.0.1:" + appPort;
      String mailUrl = "http://127.0.0.1:" + mailPort;
      metadata.put("appUrl", appUrl);
      metadata.put("mailpitUrl", mailUrl);
      System.setProperty("browser.tests.managed", "true");
      System.setProperty("browser.tests.baseUrl", appUrl);
      System.setProperty("browser.tests.httpUrl", "http://127.0.0.1:" + httpPort);
      System.setProperty("browser.tests.mailpitUrl", mailUrl);
      System.setProperty("browser.tests.headed", String.valueOf(options.contains("--headed")));
      System.setProperty("allure.results.directory", run.resolve("allure-results").toString());
      save();
      Path certificates = run.resolve("https");
      BrowserTestCa.issue(BrowserTestCa.directory(repo), certificates);
      command(withCompose("config", "--quiet"));
      environmentStarted = true;
      command(
          withCompose(
              "up", "-d", "--wait", "--wait-timeout", "180", build ? "--build" : "--no-build"));
      BrowserHttps.awaitReady(appUrl, certificates.resolve("root.crt"));
      metadata.put("setup", "passed");
      metadata.put("https", "certificate-chain-and-hostname-verified");
      appContainer = command(withCompose("ps", "-q", "app")).trim();
      metadata.put(
          "imageId",
          command(List.of(docker, "inspect", "--format", "{{.Image}}", appContainer)).trim());
      save();
      SummaryGeneratingListener listener = new SummaryGeneratingListener();
      LauncherDiscoveryRequest request = BrowserExecutionPlan.request(testClasses, suite);
      activeTestRunner = this;
      try {
        LauncherFactory.create().execute(request, listener);
      } finally {
        activeTestRunner = null;
      }
      TestExecutionSummary result = listener.getSummary();
      metadata.put(
          "browser",
          "Chromium " + System.getProperty("browser.tests.browserVersion", "nicht verfügbar"));
      metadata.put("viewport", "1280 × 900");
      metadata.put(
          "browserLocale", de.derpeterson.browsertests.i18n.UiTexts.selected().browserLocale());
      metadata.put("browserTimezone", "Europe/Berlin");
      metadata.put("testsFound", result.getTestsFoundCount());
      metadata.put("testsPassed", result.getTestsSucceededCount());
      metadata.put("testsSkipped", result.getTestsSkippedCount());
      metadata.put("testsFailed", result.getTestsFailedCount());
      boolean passed =
          result.getTestsFoundCount() > 0
              && result.getTestsSucceededCount() == result.getTestsFoundCount()
              && result.getTotalFailureCount() == 0;
      List<JsonNode> collected = MarkdownSummary.readResults(run);
      if (collected.size() != result.getTestsFoundCount()) {
        passed = false;
        metadata.put("error", "JUnit and Allure case counts differ; incomplete evidence.");
      }
      if (result.getTotalFailureCount() > 0) {
        metadata.put(
            "error",
            result.getFailures().stream()
                .map(failure -> failure.getException().toString())
                .collect(java.util.stream.Collectors.joining("\n")));
      }
      metadata.put("tests", passed ? "passed" : "failed");
      result.printTo(new java.io.PrintWriter(System.out, true));
      result.printFailuresTo(new java.io.PrintWriter(System.out, true));
      exitCode = passed ? 0 : 1;
    } catch (Exception error) {
      metadata.put("error", error.toString());
      metadata.put("passed".equals(metadata.get("setup")) ? "tests" : "setup", "failed");
      System.err.println(error.getMessage());
    } finally {
      if (coverage) {
        try {
          if (appContainer == null || appContainer.isBlank()) {
            throw new IOException("No test application started; coverage unavailable.");
          }
          command(withCompose("stop", "--timeout", "30", "app"));
          Path coverageFolder = run.resolve("coverage");
          command(
              List.of(
                  docker,
                  "cp",
                  appContainer + ":/app/logs/browser-jacoco.exec",
                  coverageFolder.resolve("browser-jacoco.exec").toString()));
          if (!worker || Boolean.getBoolean("browser.tests.workerJar")) {
            command(
                List.of(
                    docker,
                    "cp",
                    appContainer + ":/app/app.jar",
                    coverageFolder.resolve("app.jar").toString()));
            command(
                List.of(
                    docker,
                    "cp",
                    appContainer + ":/app/coverage-sources.jar",
                    coverageFolder.resolve("coverage-sources.jar").toString()));
          }
          if (!worker) {
            BrowserCoverage.extractTestedCode(coverageFolder);
            command(BrowserCoverage.reportCommand(module, coverageFolder));
          }
          metadata.put("coverage", "passed");
          metadata.put(
              "coverageReport",
              worker ? "../../coverage/html/index.html" : "coverage/html/index.html");
        } catch (Exception error) {
          metadata.put("coverage", "failed");
          metadata.put("coverageError", error.toString());
          exitCode = 1;
        }
      }
      try {
        // Auch ein Fehler vor Compose-up kann bereits einen eigenen Worker-Schlüssel hinterlassen.
        cleanup();
      } catch (Exception error) {
        metadata.put("cleanup", "failed");
        metadata.put("cleanupError", error.toString());
        exitCode = 1;
      }
      try {
        Files.writeString(
            run.resolve("allure-results/environment.properties"),
            "Java="
                + Runtime.version()
                + "\nPlaywright=1.63.0\nGit="
                + metadata.get("commit")
                + "\nUI-Language="
                + metadata.get("uiLanguage")
                + "\nBranch="
                + metadata.get("branch")
                + "\nDocker="
                + project
                + "\n");
        if (!worker) {
          generateHtml();
        } else {
          metadata.put("html", "delegated-to-parent");
        }
      } catch (Exception error) {
        metadata.put("html", "failed");
        metadata.put("reportError", error.toString());
        exitCode = 1;
      }
      metadata.put("finishedAt", Instant.now().toString());
      save();
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      System.out.println("\nJava-Browser-Testbericht: " + run.resolve("report.md"));
    }
    return exitCode;
  }

  private int executeParallel(Set<String> options) throws Exception {
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    int exitCode = 1;
    parallel = new BrowserParallelWorkers();
    List<Path> coverageData = new ArrayList<>();
    try {
      command(withCompose("config", "--quiet"));
      if (build) {
        command(withCompose("build", "app"));
      }
      String image =
          command(
                  List.of(
                      docker,
                      "image",
                      "inspect",
                      "--format",
                      "{{.Id}}",
                      environment.getOrDefault(
                          "BROWSER_TEST_APP_IMAGE", "vaadinspringapp-browser-tests-app:local")))
              .trim();
      // Alle Worker prüfen exakt dasselbe unveränderliche Image, auch wenn jemand das Tag neu baut.
      environment.put("BROWSER_TEST_APP_IMAGE", image);
      metadata.put("imageId", image);
      List<List<Class<?>>> groups = BrowserExecutionPlan.partition(testClasses, workers, suite);
      metadata.put("workersActual", groups.size());
      metadata.put(
          "workerPlan",
          groups.stream().map(group -> group.stream().map(Class::getSimpleName).toList()).toList());
      metadata.put("setup", "running");
      metadata.put("tests", "running");
      save();
      parallel.start(
          module,
          run,
          suite,
          System.getProperty("browser.tests.language"),
          groups,
          environment,
          project,
          options.contains("--headed"),
          coverage,
          options.contains("--failure-probe"));
      parallel.await();
      boolean passed = true, setupPassed = true;
      long found = 0, succeeded = 0, failed = 0, skipped = 0;
      List<Map<String, Object>> details = new ArrayList<>();
      for (Worker child : parallel.workers()) {
        JsonNode data = JSON.readTree(child.directory().resolve("metadata.json").toFile());
        List<JsonNode> evidence = MarkdownSummary.readResults(child.directory());
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
            child.directory().resolve("allure-results"), run.resolve("allure-results"));
        if (coverage) {
          if (!data.path("coverage").asText().equals("passed")) {
            throw new IOException("Worker coverage incomplete: " + child.directory());
          }
          coverageData.add(child.directory().resolve("coverage/browser-jacoco.exec"));
        }
        if (metadata.get("browser") == null) {
          for (String key : List.of("browser", "viewport", "browserLocale", "browserTimezone")) {
            metadata.put(key, data.path(key).asText());
          }
        }
      }
      metadata.put("workerResults", details);
      metadata.put("testsFound", found);
      metadata.put("testsPassed", succeeded);
      metadata.put("testsFailed", failed);
      metadata.put("testsSkipped", skipped);
      metadata.put("setup", setupPassed ? "passed" : "failed");
      // Jeder Fall genau einmal: fehlende und doppelte Nachweise verhindern ein grünes Ergebnis.
      List<JsonNode> results = MarkdownSummary.readResults(run);
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
      passed &= found > 0 && results.size() == found && parallel.workers().size() == groups.size();
      metadata.put("tests", passed ? "passed" : "failed");
      if (!passed) {
        metadata.put(
            "error",
            "At least one worker, test or evidence check failed; inspect workers/*/metadata.json and console.log.");
      }
      if (coverage) {
        Path folder = run.resolve("coverage");
        Files.createDirectories(folder);
        Files.move(
            parallel.workers().getFirst().directory().resolve("coverage/app.jar"),
            folder.resolve("app.jar"));
        Files.move(
            parallel.workers().getFirst().directory().resolve("coverage/coverage-sources.jar"),
            folder.resolve("coverage-sources.jar"));
        ArrayList<String> merge =
            new ArrayList<>(
                List.of(
                    BrowserParallelWorkers.javaExecutable(),
                    "-jar",
                    module.resolve("target/tools/jacococli.jar").toString(),
                    "merge"));
        merge.addAll(coverageData.stream().map(Path::toString).toList());
        merge.addAll(List.of("--destfile", folder.resolve("browser-jacoco.exec").toString()));
        command(merge);
        BrowserCoverage.extractTestedCode(folder);
        command(BrowserCoverage.reportCommand(module, folder));
        metadata.put("coverage", "passed");
        metadata.put("coverageReport", "coverage/html/index.html");
      }
      exitCode = passed ? 0 : 1;
    } catch (Exception error) {
      metadata.put("error", error.toString());
      metadata.put("tests", "failed");
      if (coverage) {
        metadata.put("coverage", "failed");
        metadata.put("coverageError", error.toString());
      }
      System.err.println(error.getMessage());
    } finally {
      try {
        cleanupWorkers();
      } catch (Exception error) {
        metadata.put("cleanup", "failed");
        metadata.put("cleanupError", error.toString());
        exitCode = 1;
      }
      try {
        generateHtml();
      } catch (Exception error) {
        metadata.put("html", "failed");
        metadata.put("reportError", error.toString());
        exitCode = 1;
      }
      metadata.put("finishedAt", Instant.now().toString());
      save();
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      System.out.println("Gemeinsamer Browser-Testbericht: " + run.resolve("report.md"));
    }
    return exitCode;
  }

  private synchronized void cleanupWorkers() throws Exception {
    if (parallel == null) {
      return;
    }
    parallel.stop();
    List<String> errors = new ArrayList<>();
    for (Worker child : parallel.workers()) {
      try {
        // Auch nach einem Prozessabbruch nur das eigene, vorher festgelegte Projekt abbauen.
        ArrayList<String> down =
            new ArrayList<>(
                List.of(
                    docker,
                    "compose",
                    "-f",
                    repo.resolve("compose.browser-tests.yaml").toString(),
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
    metadata.put("cleanup", "passed");
    save();
  }

  private void generateHtml() throws Exception {
    Files.writeString(
        run.resolve("allure-results/environment.properties"),
        "Java="
            + Runtime.version()
            + "\nPlaywright=1.63.0\nGit="
            + metadata.get("commit")
            + "\nUI-Language="
            + metadata.get("uiLanguage")
            + "\nBranch="
            + metadata.get("branch")
            + "\nImage="
            + metadata.get("imageId")
            + "\nWorkers="
            + metadata.getOrDefault("workersActual", 1)
            + "\n");
    command(
        allureCommand(
            module,
            "generate",
            run.resolve("allure-results").toString(),
            "--output",
            run.resolve("html").toString()));
    writeReportLauncher(run.resolve("html"));
    metadata.put("html", "passed");
  }

  private synchronized void cleanup() throws Exception {
    try {
      if (environmentStarted) {
        command(withCompose("down", "--volumes", "--remove-orphans"));
        environmentStarted = false;
      }
    } finally {
      BrowserTestCa.cleanupWorker(run.resolve("https"));
    }
    metadata.put("cleanup", "passed");
    save();
  }

  private void save() throws IOException {
    JSON.writerWithDefaultPrettyPrinter()
        .writeValue(run.resolve("metadata.json").toFile(), metadata);
    if (!worker) {
      MarkdownSummary.write(run, metadata);
    }
  }

  private String command(List<String> arguments) throws Exception {
    Path temporaryOutput = Files.createTempFile(run, "command-", ".tmp");
    Files.writeString(
        run.resolve("runner.log"),
        "\n> " + String.join(" ", arguments) + "\n",
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
    ProcessBuilder builder =
        new ProcessBuilder(arguments)
            .directory(repo.toFile())
            .redirectErrorStream(true)
            .redirectOutput(temporaryOutput.toFile());
    builder.environment().putAll(environment);
    if (Path.of(docker).isAbsolute()) {
      builder.environment().keySet().removeIf(key -> key.equalsIgnoreCase("PATH"));
      builder
          .environment()
          .put(
              "Path",
              Path.of(docker).getParent() + java.io.File.pathSeparator + System.getenv("PATH"));
    }
    Process child = builder.start();
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
    int exit;
    try {
      exit = child.waitFor();
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
    String output = Files.readString(temporaryOutput);
    Files.writeString(run.resolve("runner.log"), output, StandardOpenOption.APPEND);
    Files.delete(temporaryOutput);
    System.out.print(output);
    if (exit != 0) {
      throw new IOException(
          "Process exited with " + exit + "; see runner.log: " + arguments.getFirst());
    }
    return output;
  }

  /** Neustart ausschließlich der gerade vom Runner selbst erzeugten isolierten Testanwendung. */
  public static void restartTestApplication() throws Exception {
    BrowserTestRunner runner = activeTestRunner;
    if (runner == null || !runner.environmentStarted || runner.appContainer == null) {
      throw new IllegalStateException("No owned test application is running");
    }
    runner.environment.put("BROWSER_TEST_BOOTSTRAP_ENABLED", "false");
    runner.command(
        runner.withCompose(
            "up",
            "-d",
            "--no-build",
            "--no-deps",
            "--force-recreate",
            "--wait",
            "--wait-timeout",
            "180",
            "app"));
    runner.appContainer = runner.command(runner.withCompose("ps", "-q", "app")).trim();
  }

  /** Ausschließlich der Maildienst des aktiven eigenen Workers; keine fremden Dienste. */
  public static void setTestMailpitAvailable(boolean available) throws Exception {
    BrowserTestRunner runner = ownedRunner();
    runner.command(
        available
            ? runner.withCompose("up", "-d", "--no-deps", "--wait", "mailpit")
            : runner.withCompose("stop", "mailpit"));
  }

  public static String testApplicationLogs() throws Exception {
    BrowserTestRunner runner = ownedRunner();
    return runner.command(runner.withCompose("logs", "--no-color", "--tail", "250", "app"));
  }

  public static JsonNode testMailpitMessages(boolean secondary) throws Exception {
    BrowserTestRunner runner = ownedRunner();
    String host = secondary ? "mailpit-secondary" : "mailpit";
    return JSON.readTree(
        runner.command(
            runner.withCompose(
                "exec",
                "-T",
                "app",
                "curl",
                "--fail",
                "--silent",
                "http://" + host + ":8025/api/v1/messages?limit=100")));
  }

  private static BrowserTestRunner ownedRunner() {
    BrowserTestRunner runner = activeTestRunner;
    if (runner == null || !runner.environmentStarted || runner.appContainer == null) {
      throw new IllegalStateException("No owned test application is running");
    }
    return runner;
  }

  private List<String> withCompose(String... args) {
    ArrayList<String> result = new ArrayList<>(compose);
    result.addAll(List.of(args));
    return result;
  }

  private String git(String... args) throws Exception {
    ArrayList<String> command = new ArrayList<>(List.of("git"));
    command.addAll(List.of(args));
    Process process =
        new ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true).start();
    String output =
        new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
            .trim();
    if (process.waitFor() != 0) {
      throw new IOException("Cannot determine Git state: " + output);
    }
    return output;
  }

  private static int port(String key, int fallback) {
    int value = Integer.parseInt(System.getenv().getOrDefault(key, String.valueOf(fallback)));
    if (value < 1024
        || value > 65535
        || value == 18080
        || value == 18025
        || value == 18443
        || value == 8443) {
      throw new IllegalArgumentException(
          "Use free test ports from 1024 to 65535, excluding manual ports 8443/18080/18025/18443.");
    }
    return value;
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

  /** Uses the HTML folder itself, so every archived run opens its own report. */
  static void writeReportLauncher(Path html) throws IOException {
    String launcher =
        """
                @echo off
                setlocal
                pushd "%~dp0..\\..\\..\\.." || exit /b 1
                powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\\..\\..\\run.ps1" -Report -ReportPath "%~dp0."
                set "reportExit=%errorlevel%"
                popd
                if not "%reportExit%"=="0" pause
                exit /b %reportExit%
                """;
    Files.writeString(
        html.resolve("open-report.bat"),
        launcher.replace("\n", "\r\n"),
        java.nio.charset.StandardCharsets.US_ASCII);
  }

  private static List<String> allureCommand(Path module, String... args) throws IOException {
    Path tools = module.resolve("target/tools");
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
