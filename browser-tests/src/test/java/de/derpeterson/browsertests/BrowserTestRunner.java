package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Starts only disposable Docker data, then delegates tests to JUnit and HTML to Allure. */
public final class BrowserTestRunner {
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
    private final Thread shutdownHook;

    private BrowserTestRunner(Path module, Set<String> options, String suite) throws Exception {
        this.module = module.toAbsolutePath().normalize();
        repo = this.module.getParent();
        build = !options.contains("--no-build");
        coverage = options.contains("--coverage");
        testClasses = options.contains("--failure-probe") ? List.of(FailureProbeTest.class) : BrowserSuites.select(suite);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String commit = git("rev-parse", "HEAD");
        // Ersetzt einen Punkt plus Sekundenbruchteil am Ende vor Z durch _UTC; $ verlangt das Zeichenfolgenende.
        String runId = Instant.now().toString().replace(':', '-').replaceAll("\\.\\d+Z$", "_UTC")
                + "__" + commit.substring(0, 8) + "__" + suffix;
        run = this.module.resolve("runs").resolve(runId);
        Files.createDirectories(run.resolve("allure-results"));
        Files.writeString(this.module.resolve("runs/latest.txt"), runId);
        project = "vaadinspringapp-e2e-java-" + suffix;
        docker = dockerExecutable();
        var composeArguments = new ArrayList<>(List.of(docker, "compose", "-f", repo.resolve("compose.browser-tests.yaml").toString()));
        if (coverage) composeArguments.addAll(List.of("-f", BrowserCoverage.prepare(module, run).toString()));
        composeArguments.addAll(List.of("-p", project));
        compose = List.copyOf(composeArguments);
        metadata.put("runId", runId);
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
        shutdownHook = new Thread(() -> {
            try { cleanup(); } catch (Exception ignored) { /* Last persisted status remains incomplete. */ }
        }, "browser-test-cleanup");
    }

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 25) throw new IllegalStateException("Java 25 is required.");
        Path module = Path.of(args[0]).toAbsolutePath().normalize();
        Set<String> options = new HashSet<>(Arrays.asList(args).subList(1, args.length));
        if (options.contains("--report")) {
            Path report;
            int reportPathIndex = Arrays.asList(args).indexOf("--report-path");
            if (reportPathIndex >= 0) {
                if (reportPathIndex + 1 >= args.length) throw new IllegalArgumentException("Report path missing.");
                report = Path.of(args[reportPathIndex + 1]).toAbsolutePath().normalize();
            } else {
                String latest = Files.readString(module.resolve("runs/latest.txt")).trim();
                // Erlaubt nur ASCII-Buchstaben/Ziffern/Unterstrich, Punkt und Minus, also keine Pfadtrenner; mindestens ein Zeichen.
                if (!latest.matches("[\\w.-]+")) throw new IllegalArgumentException("Invalid report path.");
                report = module.resolve("runs").resolve(latest).resolve("html");
            }
            if (!Files.isRegularFile(report.resolve("index.html"))) throw new IOException("HTML report missing: " + report);
            var process = new ProcessBuilder(allureCommand(module, "open", report.toString(), "--host", "127.0.0.1"))
                    .inheritIO().start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
            System.exit(process.waitFor());
        }
        int languageIndex = Arrays.asList(args).indexOf("--language");
        if (languageIndex >= 0 && languageIndex + 1 >= args.length) throw new IllegalArgumentException("Language missing.");
        String language = languageIndex >= 0 ? args[languageIndex + 1] : "de";
        new de.derpeterson.browsertests.i18n.UiTexts(language); // Validate before creating a Docker environment.
        System.setProperty("browser.tests.language", language);
        int suiteIndex = Arrays.asList(args).indexOf("--suite");
        if (suiteIndex >= 0 && suiteIndex + 1 >= args.length) throw new IllegalArgumentException("Suite name missing.");
        String suite = suiteIndex >= 0 ? args[suiteIndex + 1] : "all";
        System.exit(new BrowserTestRunner(module, options, suite).execute(options));
    }

    private int execute(Set<String> options) throws Exception {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        int exitCode = 1;
        try {
            int appPort = port("BROWSER_TEST_APP_PORT", 18081);
            int mailPort = port("BROWSER_TEST_MAILPIT_PORT", 18026);
            if (appPort == mailPort) throw new IllegalArgumentException("Test ports must differ.");
            environment.put("BROWSER_TEST_APP_PORT", String.valueOf(appPort));
            environment.put("BROWSER_TEST_MAILPIT_PORT", String.valueOf(mailPort));
            String appUrl = "http://127.0.0.1:" + appPort;
            String mailUrl = "http://127.0.0.1:" + mailPort;
            metadata.put("appUrl", appUrl);
            metadata.put("mailpitUrl", mailUrl);
            System.setProperty("browser.tests.managed", "true");
            System.setProperty("browser.tests.baseUrl", appUrl);
            System.setProperty("browser.tests.mailpitUrl", mailUrl);
            System.setProperty("browser.tests.headed", String.valueOf(options.contains("--headed")));
            System.setProperty("allure.results.directory", run.resolve("allure-results").toString());
            save();
            command(withCompose("config", "--quiet"));
            environmentStarted = true;
            command(withCompose("up", "-d", "--wait", "--wait-timeout", "180", build ? "--build" : "--no-build"));
            metadata.put("setup", "passed");
            appContainer = command(withCompose("ps", "-q", "app")).trim();
            metadata.put("imageId", command(List.of(docker, "inspect", "--format", "{{.Image}}", appContainer)).trim());
            save();
            var listener = new SummaryGeneratingListener();
            var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(testClasses.stream().map(org.junit.platform.engine.discovery.DiscoverySelectors::selectClass).toList())
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
            LauncherFactory.create().execute(request, listener);
            var result = listener.getSummary();
            metadata.put("browser", "Chromium " + System.getProperty("browser.tests.browserVersion", "nicht verfügbar"));
            metadata.put("viewport", "1280 × 900");
            metadata.put("browserLocale", de.derpeterson.browsertests.i18n.UiTexts.selected().browserLocale());
            metadata.put("browserTimezone", "Europe/Berlin");
            metadata.put("testsFound", result.getTestsFoundCount());
            metadata.put("testsPassed", result.getTestsSucceededCount());
            metadata.put("testsSkipped", result.getTestsSkippedCount());
            metadata.put("testsFailed", result.getTestsFailedCount());
            boolean passed = result.getTestsFoundCount() > 0 && result.getTestsSucceededCount() == result.getTestsFoundCount()
                    && result.getTotalFailureCount() == 0;
            var collected = MarkdownSummary.readResults(run);
            if (collected.size() != result.getTestsFoundCount()) {
                passed = false;
                metadata.put("error", "JUnit and Allure case counts differ; incomplete evidence.");
            }
            if (result.getTotalFailureCount() > 0) metadata.put("error", result.getFailures().stream().map(failure -> failure.getException().toString()).collect(java.util.stream.Collectors.joining("\n")));
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
                    if (appContainer == null || appContainer.isBlank()) throw new IOException("No test application started; coverage unavailable.");
                    command(withCompose("stop", "--timeout", "30", "app"));
                    Path coverageFolder = run.resolve("coverage");
                    command(List.of(docker, "cp", appContainer + ":/app/logs/browser-jacoco.exec", coverageFolder.resolve("browser-jacoco.exec").toString()));
                    command(List.of(docker, "cp", appContainer + ":/app/app.jar", coverageFolder.resolve("app.jar").toString()));
                    BrowserCoverage.extractApplicationClasses(coverageFolder);
                    command(BrowserCoverage.reportCommand(module, repo, coverageFolder));
                    metadata.put("coverage", "passed");
                    metadata.put("coverageReport", "coverage/html/index.html");
                } catch (Exception error) {
                    metadata.put("coverage", "failed");
                    metadata.put("coverageError", error.toString());
                    exitCode = 1;
                }
            }
            if (environmentStarted) {
                try { cleanup(); } catch (Exception error) {
                    metadata.put("cleanup", "failed");
                    metadata.put("cleanupError", error.toString());
                    exitCode = 1;
                }
            }
            try {
                Files.writeString(run.resolve("allure-results/environment.properties"),
                        "Java=" + Runtime.version() + "\nPlaywright=1.63.0\nGit=" + metadata.get("commit")
                        + "\nUI-Language=" + metadata.get("uiLanguage") + "\nBranch=" + metadata.get("branch") + "\nDocker=" + project + "\n");
                command(allureCommand(module, "generate", run.resolve("allure-results").toString(), "--output", run.resolve("html").toString()));
                writeReportLauncher(run.resolve("html"));
                metadata.put("html", "passed");
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

    private synchronized void cleanup() throws Exception {
        if (!environmentStarted) return;
        command(withCompose("down", "--volumes", "--remove-orphans"));
        environmentStarted = false;
        metadata.put("cleanup", "passed");
        save();
    }

    private void save() throws IOException {
        JSON.writerWithDefaultPrettyPrinter().writeValue(run.resolve("metadata.json").toFile(), metadata);
        MarkdownSummary.write(run, metadata);
    }

    private String command(List<String> arguments) throws Exception {
        Path temporaryOutput = Files.createTempFile(run, "command-", ".tmp");
        Files.writeString(run.resolve("runner.log"), "\n> " + String.join(" ", arguments) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        ProcessBuilder builder = new ProcessBuilder(arguments).directory(repo.toFile()).redirectErrorStream(true)
                .redirectOutput(temporaryOutput.toFile());
        builder.environment().putAll(environment);
        if (Path.of(docker).isAbsolute()) {
            builder.environment().keySet().removeIf(key -> key.equalsIgnoreCase("PATH"));
            builder.environment().put("Path", Path.of(docker).getParent() + java.io.File.pathSeparator + System.getenv("PATH"));
        }
        Process child = builder.start();
        Thread childShutdown = new Thread(child::destroy);
        Runtime.getRuntime().addShutdownHook(childShutdown);
        int exit;
        try { exit = child.waitFor(); } finally { Runtime.getRuntime().removeShutdownHook(childShutdown); }
        String output = Files.readString(temporaryOutput);
        Files.writeString(run.resolve("runner.log"), output, StandardOpenOption.APPEND);
        Files.delete(temporaryOutput);
        System.out.print(output);
        if (exit != 0) throw new IOException("Process exited with " + exit + "; see runner.log: " + arguments.getFirst());
        return output;
    }

    private List<String> withCompose(String... args) {
        var result = new ArrayList<>(compose);
        result.addAll(List.of(args));
        return result;
    }

    private String git(String... args) throws Exception {
        var command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) throw new IOException("Cannot determine Git state: " + output);
        return output;
    }

    private static int port(String key, int fallback) {
        int value = Integer.parseInt(System.getenv().getOrDefault(key, String.valueOf(fallback)));
        if (value < 1024 || value > 65535 || value == 18080 || value == 18025)
            throw new IllegalArgumentException("Use free test ports from 1024 to 65535, excluding manual ports 18080/18025.");
        return value;
    }

    private static String dockerExecutable() {
        if (System.getenv("DOCKER_BIN") != null) return System.getenv("DOCKER_BIN");
        for (String candidate : List.of(System.getenv().getOrDefault("LOCALAPPDATA", "") + "/Programs/DockerDesktop/resources/bin/docker.exe",
                System.getenv().getOrDefault("ProgramFiles", "") + "/Docker/Docker/resources/bin/docker.exe")) {
            if (Files.isRegularFile(Path.of(candidate))) return candidate;
        }
        return "docker";
    }

    /** Uses the HTML folder itself, so every archived run opens its own report. */
    static void writeReportLauncher(Path html) throws IOException {
        String launcher = """
                @echo off
                setlocal
                pushd "%~dp0..\\..\\..\\.." || exit /b 1
                powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\\..\\..\\run.ps1" -Report -ReportPath "%~dp0."
                set "reportExit=%errorlevel%"
                popd
                if not "%reportExit%"=="0" pause
                exit /b %reportExit%
                """;
        Files.writeString(html.resolve("open-report.bat"), launcher.replace("\n", "\r\n"), java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static List<String> allureCommand(Path module, String... args) throws IOException {
        Path tools = module.resolve("target/tools");
        Path lib;
        try (var entries = Files.list(tools)) {
            lib = entries.filter(path -> path.getFileName().toString().startsWith("allure-")).map(path -> path.resolve("lib"))
                    .filter(Files::isDirectory).findFirst().orElseThrow(() -> new IOException("Allure tools missing; run test-compile first."));
        }
        var result = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dfile.encoding=UTF-8", "-Dallure.home=" + lib.getParent(), "-cp", lib + java.io.File.separator + "*" + java.io.File.pathSeparator + lib.resolve("config"), "io.qameta.allure.CommandLine"));
        result.addAll(List.of(args));
        return result;
    }
}
