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

  private final List<Worker> workers = new ArrayList<>();

  synchronized List<Worker> workers() {
    return List.copyOf(workers);
  }

  synchronized void start(
      Path module,
      Path run,
      String suite,
      String language,
      List<List<Class<?>>> groups,
      Map<String, String> environment,
      String project,
      boolean headed,
      boolean coverage,
      boolean failureProbe)
      throws IOException {
    // Ports während der Planung reservieren. Nach Freigabe bindet nur Docker daran;
    // bei einer externen Kollision scheitert Compose, bevor ein Browser startet.
    List<ServerSocket> sockets = new ArrayList<>();
    try {
      for (int i = 0; i < groups.size() * 2; i++) {
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        sockets.add(socket);
      }
      for (int i = 0; i < groups.size(); i++) {
        Path directory = run.resolve("workers/" + (i + 1));
        Files.createDirectories(directory);
        String workerProject = project + "-w" + (i + 1);
        List<String> command =
            new ArrayList<>(
                List.of(
                    javaExecutable(),
                    "-Dfile.encoding=UTF-8",
                    "-Dbrowser.tests.workerRun=" + directory,
                    "-Dbrowser.tests.workerClasses="
                        + String.join(
                            ",", groups.get(i).stream().map(Class::getSimpleName).toList()),
                    "-Dbrowser.tests.workerProject=" + workerProject,
                    "-Dbrowser.tests.workerJar=" + (i == 0),
                    "-javaagent:" + module.resolve("target/tools/aspectjweaver.jar"),
                    "-cp",
                    System.getProperty("java.class.path"),
                    BrowserTestRunner.class.getName(),
                    module.toString(),
                    "--suite",
                    suite,
                    "--language",
                    language,
                    "--no-build"));
        if (headed) {
          command.add("--headed");
        }
        if (coverage) {
          command.add("--coverage");
        }
        if (failureProbe) {
          command.add("--failure-probe");
        }
        ProcessBuilder builder =
            new ProcessBuilder(command)
                .directory(module.getParent().toFile())
                .redirectErrorStream(true)
                .redirectOutput(directory.resolve("console.log").toFile());
        builder.environment().putAll(environment);
        builder
            .environment()
            .put("BROWSER_TEST_APP_PORT", String.valueOf(sockets.get(i * 2).getLocalPort()));
        builder
            .environment()
            .put(
                "BROWSER_TEST_MAILPIT_PORT", String.valueOf(sockets.get(i * 2 + 1).getLocalPort()));
        sockets.get(i * 2).close();
        sockets.get(i * 2 + 1).close();
        Process process = builder.start();
        workers.add(new Worker(directory, workerProject, groups.get(i), process, Instant.now()));
        System.out.println(
            "Worker "
                + (i + 1)
                + " gestartet: "
                + groups.get(i).stream().map(Class::getSimpleName).toList());
      }
    } finally {
      for (ServerSocket socket : sockets) {
        socket.close();
      }
    }
  }

  void await() throws InterruptedException, IOException {
    Set<Process> reported = new HashSet<>();
    while (reported.size() < workers.size()) {
      for (Worker worker : workers) {
        if (reported.contains(worker.process())) {
          continue;
        }
        if (worker.process().waitFor(1, TimeUnit.SECONDS)) {
          reported.add(worker.process());
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
    for (Worker worker : workers) {
      if (worker.process().isAlive()) {
        // Auch laufende Compose-/Browser-Kindprozesse beenden, bevor Compose down läuft.
        List<ProcessHandle> descendants = worker.process().descendants().toList();
        descendants.reversed().forEach(ProcessHandle::destroy);
        worker.process().destroy();
        try {
          if (!worker.process().waitFor(10, TimeUnit.SECONDS)) {
            worker.process().destroyForcibly();
          }
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
        }
        descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
      }
    }
    // Auf Windows garantiert destroy() keinen JVM-Shutdown-Hook. Der Elternrunner
    // baut deshalb anschließend jedes eigene Compose-Projekt ausdrücklich ab.
  }

  static String javaExecutable() {
    return Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
        .toString();
  }

  static void collect(Path source, Path destination) throws IOException {
    try (Stream<Path> files = Files.list(source)) {
      for (Path file : files.toList()) {
        if (file.getFileName().toString().equals("environment.properties")) {
          continue;
        }
        if (!Files.isRegularFile(file)) {
          throw new IOException("Unexpected evidence directory: " + file);
        }
        // Verschieben statt duplizieren: nur ein gemeinsamer Satz Screenshots/Traces.
        // Keine REPLACE_EXISTING-Option: Kollisionen dürfen keinen Nachweis überschreiben.
        Files.move(file, destination.resolve(file.getFileName()));
      }
    }
  }

  static boolean matchesClasses(JsonNode metadata, List<Class<?>> expected) {
    Set<String> actual = new HashSet<>();
    for (JsonNode name : metadata.path("testClasses")) {
      actual.add(name.asText());
    }
    return metadata.path("testClasses").size() == expected.size()
        && actual.equals(new HashSet<>(expected.stream().map(Class::getSimpleName).toList()));
  }

  static boolean successful(JsonNode metadata, int exit, String image, int evidenceCount) {
    return exit == 0
        && metadata.hasNonNull("finishedAt")
        && List.of("setup", "tests", "cleanup").stream()
            .allMatch(key -> metadata.path(key).asText().equals("passed"))
        && metadata.path("imageId").asText().equals(image)
        && metadata.path("testsFound").asInt() > 0
        && metadata.path("testsFound").asInt() == metadata.path("testsPassed").asInt()
        && metadata.path("testsFound").asInt() == evidenceCount
        && metadata.path("testsFailed").asInt(-1) == 0
        && metadata.path("testsSkipped").asInt(-1) == 0;
  }
}
