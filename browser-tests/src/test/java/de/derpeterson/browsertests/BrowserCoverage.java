package de.derpeterson.browsertests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Optional JaCoCo measurement of the disposable application's exact production classes. */
final class BrowserCoverage {
  static Path prepare(Path module, Path run) throws IOException {
    Path agent = module.resolve("target/tools/jacocoagent.jar").toAbsolutePath().normalize();
    if (!Files.isRegularFile(agent)) {
      throw new IOException("JaCoCo agent missing: " + agent);
    }
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

  static void extractTestedCode(Path folder) throws IOException {
    Path classes = folder.resolve("classes").toAbsolutePath().normalize();
    Files.createDirectory(classes);
    int appClasses = 0;
    int sharedClasses = 0;
    int sharedJars = 0;
    try (ZipFile jar = new ZipFile(folder.resolve("app.jar").toFile())) {
      Enumeration<? extends ZipEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().startsWith("BOOT-INF/lib/shared-")
            && entry.getName().endsWith(".jar")) {
          sharedJars++;
          try (ZipInputStream shared = new ZipInputStream(jar.getInputStream(entry))) {
            ZipEntry sharedEntry;
            while ((sharedEntry = shared.getNextEntry()) != null) {
              if (sharedEntry.getName().startsWith("de/derpeterson/app/")
                  && sharedEntry.getName().endsWith(".class")) {
                copyEntry(shared, classes, sharedEntry.getName());
                sharedClasses++;
              }
            }
          }
        }
        if (entry.getName().startsWith("BOOT-INF/classes/de/derpeterson/app/")
            && entry.getName().endsWith(".class")) {
          try (InputStream input = jar.getInputStream(entry)) {
            copyEntry(input, classes, entry.getName().substring("BOOT-INF/classes/".length()));
          }
          appClasses++;
        }
      }
    }
    if (appClasses == 0 || sharedJars != 1 || sharedClasses == 0) {
      throw new IOException(
          "Tested JAR must contain application classes and exactly one Shared JAR.");
    }
    // Auch die Quellen stammen aus demselben Container; lokale target-/src-Stände bleiben außen
    // vor.
    Path sources = folder.resolve("sources").toAbsolutePath().normalize();
    Files.createDirectory(sources);
    try (ZipFile jar = new ZipFile(folder.resolve("coverage-sources.jar").toFile())) {
      for (String module : List.of("app", "shared")) {
        String prefix = module + "/src/main/java/";
        int sourceCount = 0;
        Enumeration<? extends ZipEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".java")) {
            try (InputStream input = jar.getInputStream(entry)) {
              copyEntry(input, sources.resolve(prefix), entry.getName().substring(prefix.length()));
            }
            sourceCount++;
          }
        }
        if (sourceCount == 0) {
          throw new IOException("Java sources missing from tested image: " + module);
        }
      }
    }
  }

  private static void copyEntry(InputStream input, Path root, String name) throws IOException {
    Path output = root.resolve(name).normalize();
    if (!output.startsWith(root)) {
      throw new IOException("Invalid path in tested code archive.");
    }
    Files.createDirectories(output.getParent());
    Files.copy(input, output);
  }

  static List<String> reportCommand(Path module, Path folder) {
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
        folder.resolve("sources/app/src/main/java").toString(),
        "--sourcefiles",
        folder.resolve("sources/shared/src/main/java").toString(),
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
