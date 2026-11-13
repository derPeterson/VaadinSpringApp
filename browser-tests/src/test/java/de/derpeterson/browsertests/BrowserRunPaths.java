package de.derpeterson.browsertests;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Gemeinsamer Ablageort für Koordinator, Worker und den letzten Browserbericht. */
final class BrowserRunPaths {
  static final String ROOT_PROPERTY = "browser.tests.runsDirectory";
  static final String ROOT_ENVIRONMENT = "BROWSER_TEST_RUNS_DIRECTORY";

  private BrowserRunPaths() {}

  static Path configuredRoot(Path browserTestModulePath) throws IOException {
    return configuredRoot(browserTestModulePath, System.getProperties(), System.getenv());
  }

  static Path configuredRoot(
      Path browserTestModulePath,
      Properties systemProperties,
      Map<String, String> environmentVariables)
      throws IOException {
    Path normalizedModulePath = browserTestModulePath.toAbsolutePath().normalize();
    String configuredRunDirectoryText = systemProperties.getProperty(ROOT_PROPERTY);
    if (configuredRunDirectoryText == null) {
      configuredRunDirectoryText = environmentVariables.get(ROOT_ENVIRONMENT);
    }

    if (configuredRunDirectoryText == null) {
      Path localSettingsPath = normalizedModulePath.resolve("local.properties");
      if (Files.exists(localSettingsPath)) {
        Properties localSettingsProperties = new Properties();
        String settingsText = Files.readString(localSettingsPath, StandardCharsets.UTF_8);
        if (settingsText.startsWith("\uFEFF")) {
          settingsText = settingsText.substring(1);
        }
        localSettingsProperties.load(new StringReader(settingsText));
        configuredRunDirectoryText = localSettingsProperties.getProperty("runs.directory");
        if (configuredRunDirectoryText == null) {
          throw new IOException("Missing runs.directory in " + localSettingsPath);
        }
      }
    }

    if (configuredRunDirectoryText == null) {
      return normalizedModulePath.resolve("runs");
    }
    if (configuredRunDirectoryText.isBlank()) {
      throw new IOException("Browser run directory must not be empty.");
    }

    Path configuredRootPath =
        normalizedModulePath.resolve(configuredRunDirectoryText.trim()).normalize();
    if (configuredRootPath.getParent() == null) {
      throw new IOException("Use a dedicated browser run directory, not a filesystem root.");
    }
    return configuredRootPath;
  }

  /** Prüft Schreibbarkeit vor Dockerstart; ein Fehler darf keinen Ersatzpfad auswählen. */
  static Path prepareRoot(Path configuredRootPath) throws IOException {
    try {
      Files.createDirectories(configuredRootPath);
      Path canonicalRootPath = configuredRootPath.toRealPath();
      Path writeProbePath = Files.createTempFile(canonicalRootPath, ".write-probe-", ".tmp");
      Files.delete(writeProbePath);

      return canonicalRootPath;
    } catch (IOException writeFailure) {
      throw new IOException(
          "Browser run directory is not writable: " + configuredRootPath, writeFailure);
    }
  }

  static Path createRunDirectory(Path rootDirectoryPath, Path requestedRunPath) throws IOException {
    Path canonicalRootPath = rootDirectoryPath.toRealPath();
    Path normalizedRunPath = requestedRunPath.toAbsolutePath().normalize();
    requireChildPath(canonicalRootPath, normalizedRunPath);

    // Vor dem Anlegen auch existierende Junctions/Symlinks im Elternpfad prüfen.
    Path existingParentPath = normalizedRunPath;
    while (!Files.exists(existingParentPath)) {
      existingParentPath = existingParentPath.getParent();
    }
    Path canonicalParentPath = existingParentPath.toRealPath();
    if (!canonicalParentPath.startsWith(canonicalRootPath)) {
      throw new IOException("Browser worker path escapes the configured run directory.");
    }

    Files.createDirectories(normalizedRunPath);
    Path canonicalRunPath = normalizedRunPath.toRealPath();
    requireChildPath(canonicalRootPath, canonicalRunPath);
    return canonicalRunPath;
  }

  static Path latestReport(Path rootDirectoryPath) throws IOException {
    String latestRunId = Files.readString(rootDirectoryPath.resolve("latest.txt")).trim();
    // Nur eine Laufkennung mit ASCII-Buchstaben/Ziffern, Unterstrich, Punkt und Minus;
    // keine Pfadtrenner und keine besonderen Eltern-/Aktuellverzeichnisse.
    if (!latestRunId.matches("[\\w.-]+") || latestRunId.equals(".") || latestRunId.equals("..")) {
      throw new IOException("Invalid browser report identifier in latest.txt.");
    }

    Path canonicalRootPath = rootDirectoryPath.toRealPath();
    Path reportDirectoryPath = canonicalRootPath.resolve(latestRunId).resolve("html").toRealPath();
    requireChildPath(canonicalRootPath, reportDirectoryPath);
    return reportDirectoryPath;
  }

  private static void requireChildPath(Path rootDirectoryPath, Path childDirectoryPath)
      throws IOException {
    if (childDirectoryPath.equals(rootDirectoryPath)
        || !childDirectoryPath.startsWith(rootDirectoryPath)) {
      throw new IOException("Browser output must remain within its configured run directory.");
    }
  }
}
