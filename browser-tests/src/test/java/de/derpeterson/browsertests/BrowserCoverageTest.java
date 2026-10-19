package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    try (ZipOutputStream zip =
        new ZipOutputStream(Files.newOutputStream(temporary.resolve("app.jar")))) {
      zip.putNextEntry(new ZipEntry("BOOT-INF/classes/de/derpeterson/app/Example.class"));
      zip.write(new byte[] {1, 2, 3});
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("BOOT-INF/lib/dependency.jar"));
      zip.write(new byte[] {4});
      zip.closeEntry();
    }
    BrowserCoverage.extractApplicationClasses(temporary);
    assertArrayEquals(
        new byte[] {1, 2, 3},
        Files.readAllBytes(temporary.resolve("classes/de/derpeterson/app/Example.class")));
    List<String> command = BrowserCoverage.reportCommand(temporary, temporary, temporary);
    assertTrue(command.contains(temporary.resolve("classes").toString()));
    assertFalse(command.contains(temporary.resolve("target/classes").toString()));
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
    assertThrows(IOException.class, () -> BrowserCoverage.extractApplicationClasses(temporary));
    assertFalse(Files.exists(temporary.resolve("escape.class")));
  }
}
