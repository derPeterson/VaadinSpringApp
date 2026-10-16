package de.derpeterson.browsertests;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipFile;

/** Optional JaCoCo measurement of the disposable application's exact production classes. */
final class BrowserCoverage {
  static Path prepare(Path module, Path run) throws IOException {
    Path agent = module.resolve("target/tools/jacocoagent.jar").toAbsolutePath().normalize();
    if (!Files.isRegularFile(agent)) throw new IOException("JaCoCo agent missing: " + agent);
    Files.createDirectories(run.resolve("coverage"));
    Path override = run.resolve("compose.coverage.yaml");
    Files.writeString(override, overrideContents(agent));
    return override;
  }

  static String overrideContents(Path agent) {
    String source =
        agent.toAbsolutePath().normalize().toString().replace('\\', '/').replace("'", "''");
    // Jeder Lauf besitzt frische eigene Volumes; nach einem App-Neustart Messdaten ergänzen.
    return """
                services:
                  app:
                    environment:
                      JAVA_TOOL_OPTIONS: "-javaagent:/opt/jacoco/jacocoagent.jar=destfile=/app/logs/browser-jacoco.exec,append=true,includes=de.derpeterson.app.*"
                    volumes:
                      - type: bind
                        source: '%s'
                        target: /opt/jacoco/jacocoagent.jar
                        read_only: true
                """
        .formatted(source);
  }

  static void extractApplicationClasses(Path folder) throws IOException {
    Path classes = folder.resolve("classes").toAbsolutePath().normalize();
    int count = 0;
    try (ZipFile jar = new ZipFile(folder.resolve("app.jar").toFile())) {
      var entries = jar.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        if (!entry.getName().startsWith("BOOT-INF/classes/de/derpeterson/app/")
            || !entry.getName().endsWith(".class")) continue;
        Path output =
            classes.resolve(entry.getName().substring("BOOT-INF/classes/".length())).normalize();
        if (!output.startsWith(classes))
          throw new IOException("Invalid class path in application JAR.");
        Files.createDirectories(output.getParent());
        try (var input = jar.getInputStream(entry)) {
          Files.copy(input, output);
        }
        count++;
      }
    }
    if (count == 0) throw new IOException("No application classes found in tested JAR.");
  }

  static List<String> reportCommand(Path module, Path repo, Path folder) {
    String java =
        Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
            .toString();
    return List.of(
        java,
        "-jar",
        module.resolve("target/tools/jacococli.jar").toString(),
        "report",
        folder.resolve("browser-jacoco.exec").toString(),
        "--classfiles",
        folder.resolve("classes").toString(),
        "--sourcefiles",
        repo.resolve("src/main/java").toString(),
        "--html",
        folder.resolve("html").toString(),
        "--xml",
        folder.resolve("jacoco.xml").toString(),
        "--csv",
        folder.resolve("jacoco.csv").toString(),
        "--name",
        "Browser journeys: server-side application code");
  }
}
