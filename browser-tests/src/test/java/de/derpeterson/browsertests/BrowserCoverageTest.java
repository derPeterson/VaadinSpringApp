package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BrowserCoverageTest {
  @TempDir Path temporary;

  @Test
  void missingAgentCannotProduceAnApparentlyMeasuredRun() {
    assertThrows(
        IOException.class, () -> BrowserCoverage.prepare(temporary, temporary.resolve("run")));
  }

  @Test
  void overrideMountsOnlyTheAgentReadOnlyAndKeepsCoverageInsideTheTestContainer() {
    String yaml =
        BrowserCoverage.overrideContents(temporary.resolve("agent's directory/agent.jar"));
    assertTrue(yaml.contains("agent''s directory/agent.jar"));
    assertTrue(yaml.contains("read_only: true"));
    assertTrue(yaml.contains("destfile=/app/logs/browser-jacoco.exec"));
    assertTrue(
        yaml.contains("append=true"), "Eigene frische Messdaten nach Serverneustart erhalten");
    assertTrue(yaml.contains("includes=de.derpeterson.app.*"));
    assertFalse(yaml.contains("ports:"));
  }

  @Test
  void reportUsesClassesFromTheTestedJarRatherThanTheLocalBuild() throws Exception {
    testedJar("de/derpeterson/app/config/Shared.class");
    sourceJar("app/src/main/java/de/derpeterson/app/Example.java");
    BrowserCoverage.extractTestedCode(temporary);
    assertArrayEquals(
        new byte[] {1, 2, 3},
        Files.readAllBytes(temporary.resolve("classes/de/derpeterson/app/Example.class")));
    assertArrayEquals(
        new byte[] {4},
        Files.readAllBytes(temporary.resolve("classes/de/derpeterson/app/config/Shared.class")));
    assertTrue(
        Files.isRegularFile(
            temporary.resolve("sources/app/src/main/java/de/derpeterson/app/Example.java")));
    assertTrue(
        Files.isRegularFile(
            temporary.resolve(
                "sources/shared/src/main/java/de/derpeterson/app/config/Shared.java")));
    List<String> command = BrowserCoverage.reportCommand(temporary, temporary);
    assertTrue(command.contains(temporary.resolve("classes").toString()));
    assertFalse(command.contains(temporary.resolve("target/classes").toString()));
    assertTrue(command.contains(temporary.resolve("sources/app/src/main/java").toString()));
    assertTrue(command.contains(temporary.resolve("sources/shared/src/main/java").toString()));
    assertFalse(command.contains(temporary.resolve("src/main/java").toString()));
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
  }

  @Test
  void classesCannotEscapeTheCoverageDirectory() throws Exception {
    try (ZipOutputStream zip =
        new ZipOutputStream(Files.newOutputStream(temporary.resolve("app.jar")))) {
      zip.putNextEntry(
          new ZipEntry("BOOT-INF/classes/de/derpeterson/app/../../../../escape.class"));
      zip.write(1);
      zip.closeEntry();
    }
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
    assertFalse(Files.exists(temporary.resolve("escape.class")));
  }

  @Test
  void sharedClassesCannotEscapeTheCoverageDirectory() throws Exception {
    testedJar("de/derpeterson/app/../../../../escape.class");
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
    assertFalse(Files.exists(temporary.resolve("escape.class")));
  }

  @Test
  void sourcesCannotEscapeTheirModule() throws Exception {
    testedJar("de/derpeterson/app/config/Shared.class");
    sourceJar("app/src/main/java/../../../../escape.java");
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
    assertFalse(Files.exists(temporary.resolve("sources/escape.java")));
  }

  @Test
  void missingSharedCannotProduceAPartialApplicationReport() throws Exception {
    Files.write(
        temporary.resolve("app.jar"),
        zip(Map.of("BOOT-INF/classes/de/derpeterson/app/Example.class", new byte[] {1})));
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
  }

  @Test
  void missingImageSourcesCannotFallBackToLocalSources() throws Exception {
    testedJar("de/derpeterson/app/config/Shared.class");
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
  }

  @Test
  void missingSharedSourcesCannotProduceAPartialReport() throws Exception {
    testedJar("de/derpeterson/app/config/Shared.class");
    Files.write(
        temporary.resolve("coverage-sources.jar"),
        zip(Map.of("app/src/main/java/Example.java", new byte[] {1})));
    assertThrows(IOException.class, () -> BrowserCoverage.extractTestedCode(temporary));
  }

  private void testedJar(String sharedClass) throws IOException {
    Files.write(
        temporary.resolve("app.jar"),
        zip(
            Map.of(
                "BOOT-INF/classes/de/derpeterson/app/Example.class", new byte[] {1, 2, 3},
                "BOOT-INF/lib/shared-1.0-SNAPSHOT.jar", zip(Map.of(sharedClass, new byte[] {4})),
                "BOOT-INF/lib/dependency.jar", new byte[] {5})));
  }

  private void sourceJar(String applicationSource) throws IOException {
    Files.write(
        temporary.resolve("coverage-sources.jar"),
        zip(
            Map.of(
                applicationSource,
                new byte[] {1},
                "shared/src/main/java/de/derpeterson/app/config/Shared.java",
                new byte[] {2})));
  }

  private static byte[] zip(Map<String, byte[]> entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
    return bytes.toByteArray();
  }
}
