package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/** Human checklist from Allure results; Allure itself still produces the HTML. */
final class MarkdownSummary {
  static void writeRunReport(Path run, Map<String, Object> metadata) throws IOException {
    Files.writeString(run.resolve("report.md"), renderRunReport(metadata, readAllureResults(run)));
  }

  static List<JsonNode> readAllureResults(Path run) throws IOException {
    List<JsonNode> results = new ArrayList<>();
    try (Stream<Path> files = Files.list(run.resolve("allure-results"))) {
      for (Path file :
          files
              .filter(path -> path.getFileName().toString().endsWith("-result.json"))
              .sorted()
              .toList()) {
        results.add(new ObjectMapper().readTree(file.toFile()));
      }
    }
    return results;
  }

  static String renderRunReport(Map<String, Object> metadata, List<JsonNode> results) {
    Path browserTestModulePath = Path.of((String) metadata.get("browserTestModulePath"));
    StringBuilder reportMarkdown =
        new StringBuilder(
            "# Automatisierter Browser-Testlauf (Java)\n\n**Status: "
                + reportStatus(metadata, results)
                + "**\n\n| Angabe | Wert |\n| --- | --- |\n");

    appendRunMetadata(reportMarkdown, metadata);
    appendParallelWorkerResults(reportMarkdown, metadata);
    appendCoverageSummary(reportMarkdown, metadata);
    reportMarkdown
        .append("\n## Umfang und Grenzen\n\n")
        .append("Dieses Protokoll bewertet nur die ausgewählten, unten aufgeführten Browserfälle. ")
        .append(
            "Ein bestandener Lauf belegt nur diese Fälle, keine vollständige Prüfung der Anwendung. ")
        .append("Nicht ausgewählte Suites wurden in diesem Lauf nicht geprüft. ")
        .append(
            "Die verbleibenden Teilfälle und Betriebsgrenzen stehen in [Abdeckung und offene Punkte](")
        .append(browserTestModulePath.resolve("abdeckung.md").toUri().toASCIIString())
        .append("). Ein grüner Lauf bestätigt nur die tatsächlich aufgeführten Schritte.\n\n")
        .append("Suites, Testfälle und Grenzen: [Browser-Testanleitung](")
        .append(browserTestModulePath.resolve("README.md").toUri().toASCIIString())
        .append(").\n\n## Ergebnisse\n\n");
    appendTestResults(reportMarkdown, results);
    appendRunDiagnostics(reportMarkdown, metadata);
    reportMarkdown
        .append(
            "\n## Belege des Durchlaufs\n\n- [HTML-Bericht starten](html/open-report.bat) — im Explorer doppelklicken; benötigt den lokalen Testordner.\n")
        .append(
            "- [HTML-Einstiegsdatei](html/index.html) — über den Berichtserver öffnen, nicht direkt als Datei.\n")
        .append("- [Laufdaten und Git-Stand](metadata.json)\n- [Ablauflog](runner.log)\n\n")
        .append(
            "Screenshots, Browseraktionen und Traces befinden sich auch im Allure-Bericht. Lokale Belege sind von Git ausgeschlossen.\n");

    return reportMarkdown.toString();
  }

  private static String reportStatus(Map<String, Object> metadata, List<JsonNode> results) {
    boolean finished = metadata.containsKey("finishedAt");
    boolean passed =
        finished
            && List.of("setup", "tests", "cleanup", "html").stream()
                .allMatch(key -> "passed".equals(metadata.get(key)))
            && (!metadata.containsKey("coverage")
                || List.of("disabled", "passed").contains(metadata.get("coverage")))
            && !results.isEmpty()
            && (!metadata.containsKey("testsFound")
                || results.size() == ((Number) metadata.get("testsFound")).longValue())
            && results.stream().allMatch(test -> "passed".equals(test.path("status").asText()));
    if (!finished) {
      return "IN BEARBEITUNG";
    }
    return passed ? "BESTANDEN" : "UNVOLLSTÄNDIG / FEHLER";
  }

  private static void appendRunMetadata(
      StringBuilder reportMarkdown, Map<String, Object> metadata) {
    Map<String, String> labels =
        Map.ofEntries(
            Map.entry("runId", "Run-ID"),
            Map.entry("startedAt", "Beginn (UTC)"),
            Map.entry("finishedAt", "Ende (UTC)"),
            Map.entry("commit", "Git-Commit"),
            Map.entry("branch", "Branch"),
            Map.entry("imageId", "App-Image-ID"),
            Map.entry("build", "Image-Build aufgerufen (Cache möglich)"),
            Map.entry("failureProbe", "Absichtlicher Fehler-Probelauf"),
            Map.entry("suite", "Ausgewählte Suite"),
            Map.entry("browser", "Browser"),
            Map.entry("viewport", "Bildschirmgröße"),
            Map.entry("uiLanguage", "Geprüfte Oberflächensprache"),
            Map.entry("browserLocale", "Browser-Locale"),
            Map.entry("browserTimezone", "Browser-Zeitzone"),
            Map.entry("appUrl", "Anwendung"),
            Map.entry("mailpitUrl", "Mailpit"),
            Map.entry("project", "Docker-Projekt"),
            Map.entry("setup", "Aufbau"),
            Map.entry("tests", "Testausführung"),
            Map.entry("cleanup", "Abbau"),
            Map.entry("html", "HTML-Bericht"));
    for (String key :
        List.of(
            "runId",
            "startedAt",
            "finishedAt",
            "commit",
            "branch",
            "imageId",
            "build",
            "suite",
            "failureProbe",
            "browser",
            "viewport",
            "uiLanguage",
            "browserLocale",
            "browserTimezone",
            "appUrl",
            "mailpitUrl",
            "project",
            "setup",
            "tests",
            "cleanup",
            "html")) {
      reportMarkdown
          .append("| ")
          .append(labels.get(key))
          .append(" | ")
          .append(escapeMarkdownTableCell(metadata.get(key)))
          .append(" |\n");
    }
    reportMarkdown
        .append("| Lokale Änderungen | ")
        .append(
            metadata.getOrDefault("gitStatus", "").toString().isBlank()
                ? "nein"
                : "ja; siehe metadata.json")
        .append(" |\n");
    if ("fast".equals(metadata.get("suite"))) {
      reportMarkdown.append(
          "\n**Schnellprüfung:** Nur explizit mit `fast` markierte Kernfälle. Lange Ablaufzeiten, vollständige Rechte-/Sitzungsprüfungen und weitere Spezialfälle sind hier nicht enthalten; `all` bleibt die vollständige Auswahl.\n");
    }
  }

  private static void appendParallelWorkerResults(
      StringBuilder reportMarkdown, Map<String, Object> metadata) {
    if (metadata.containsKey("workersActual")) {
      reportMarkdown
          .append("\n## Parallele Worker\n\n")
          .append("Angefordert: ")
          .append(metadata.get("workersRequested"))
          .append("; gestartet: ")
          .append(metadata.get("workersActual"))
          .append(
              ". Eigene JVM, Browser, Docker-App, H2 und Mailpit je Worker; ein gemeinsames Image.\n\n")
          .append(
              "| Worker | Bestanden / gefunden | Aufbau | Abbau | Exitcode | Diagnose |\n| --- | --- | --- | --- | --- | --- |\n");
      if (metadata.get("workerResults") instanceof List<?> workers) {
        for (Object entry : workers) {
          if (!(entry instanceof Map<?, ?> data)) {
            continue;
          }
          String id = escapeMarkdownTableCell(data.get("worker"));
          reportMarkdown
              .append("| ")
              .append(id)
              .append(" | ")
              .append(data.get("testsPassed"))
              .append(" / ")
              .append(data.get("testsFound"))
              .append(" | ")
              .append(escapeMarkdownTableCell(data.get("setup")))
              .append(" | ")
              .append(escapeMarkdownTableCell(data.get("cleanup")))
              .append(" | ")
              .append(data.get("exitCode"))
              .append(" | [Laufdaten](workers/")
              .append(id)
              .append("/metadata.json), [Konsole](workers/")
              .append(id)
              .append("/console.log) |\n");
        }
      }
    }
  }

  private static void appendCoverageSummary(
      StringBuilder reportMarkdown, Map<String, Object> metadata) {
    if ("passed".equals(metadata.get("coverage"))) {
      reportMarkdown
          .append(
              "\n## Gemessene Browser-Coverage\n\n[JaCoCo-Bericht](coverage/html/index.html), [XML](coverage/jacoco.xml), [CSV](coverage/jacoco.csv). ")
          .append(
              "Gemessen wurde der serverseitige Anwendungscode im getesteten Docker-JAR, einschließlich Start und Browserabläufen. ")
          .append("Dies ist keine CSS-/JavaScript- oder vollständige fachliche Abdeckung.\n");
    } else if ("failed".equals(metadata.get("coverage"))) {
      reportMarkdown
          .append("\n**Angeforderte Coverage konnte nicht erstellt werden:** ")
          .append(escapeMarkdownTableCell(metadata.get("coverageError")))
          .append("\n");
    }
  }

  private static void appendTestResults(StringBuilder reportMarkdown, List<JsonNode> results) {
    if (results.isEmpty()) {
      reportMarkdown.append("Keine Testfälle vom Reporter erfasst.\n");
    }
    List<JsonNode> ordered = new ArrayList<>(results);
    ordered.sort(
        Comparator.comparing((JsonNode test) -> readAllureLabel(test, "feature"))
            .thenComparing(test -> test.path("name").asText()));
    String previousSuite = null;
    for (JsonNode test : ordered) {
      String suite = readAllureLabel(test, "feature");
      if (suite.isBlank()) {
        suite = "Weitere Prüfungen";
      }
      if (!suite.equals(previousSuite)) {
        reportMarkdown.append("### ").append(escapeMarkdownTableCell(suite)).append("\n\n");
        previousSuite = suite;
      }
      appendTestResult(reportMarkdown, test);
    }
  }

  private static void appendTestResult(StringBuilder reportMarkdown, JsonNode test) {
    String status = test.path("status").asText("unknown");
    String readable =
        switch (status) {
          case "passed" -> "BESTANDEN";
          case "failed", "broken" -> "FEHLER";
          case "skipped" -> "NICHT AUSGEFÜHRT";
          default -> "UNBEKANNT";
        };
    reportMarkdown
        .append("#### ")
        .append(escapeMarkdownTableCell(test.path("name").asText()))
        .append("\n\n");
    String story = readAllureLabel(test, "story");
    if (!story.isBlank()) {
      reportMarkdown
          .append("**Prüfbereich:** ")
          .append(escapeMarkdownTableCell(story))
          .append("\n\n");
    }
    String description = test.path("description").asText();
    if (!description.isBlank()) {
      reportMarkdown.append(description).append("\n\n");
    }
    reportMarkdown
        .append("- [")
        .append("passed".equals(status) ? "x" : " ")
        .append("] **")
        .append(readable)
        .append("**\n")
        .append("- **Status:** ")
        .append(readable)
        .append("\n")
        .append("- **Zeitpunkt (UTC):** ")
        .append(formatResultTimestamp(test, "start"))
        .append(" → ")
        .append(formatResultTimestamp(test, "stop"))
        .append("\n")
        .append("- **Ist / Grund:** ")
        .append(
            switch (status) {
              case "passed" -> "Alle ausgeführten Assertions dieses Testfalls erfüllt.";
              case "skipped" -> "Testfall nicht ausgeführt; kein erfolgreicher Nachweis.";
              default -> "Erwartung nicht vollständig bestätigt; Diagnose siehe unten.";
            })
        .append("\n\n");
    String error = test.path("statusDetails").path("message").asText();
    if (!error.isBlank()) {
      reportMarkdown
          .append("~~~~text\n")
          .append(error.replace("~~~~", "''''"))
          .append("\n~~~~\n\n");
    }
    appendTestAttachments(reportMarkdown, test);
  }

  private static void appendTestAttachments(StringBuilder reportMarkdown, JsonNode test) {
    Map<String, String> attachments = new LinkedHashMap<>();
    for (JsonNode array : test.findValues("attachments")) {
      for (JsonNode attachment : array) {
        String source = attachment.path("source").asText();
        // Nur einfache Dateinamen aus ASCII-Buchstaben/Ziffern/Unterstrich/Punkt/Minus;
        // Pfadtrenner sind ausgeschlossen.
        if (source.matches("[\\w.-]+")) {
          attachments.put(source, attachment.path("name").asText("Beleg"));
        }
      }
    }
    if (!attachments.isEmpty()) {
      reportMarkdown.append("**Belege:**\n\n");
      attachments.forEach((source, name) ->
          reportMarkdown
              .append("- [")
              .append(escapeMarkdownTableCell(name))
              .append("](allure-results/")
              .append(source)
              .append(")\n"));
      reportMarkdown.append("\n");
    }
  }

  private static void appendRunDiagnostics(
      StringBuilder reportMarkdown, Map<String, Object> metadata) {
    for (String key : List.of("error", "cleanupError", "reportError")) {
      if (metadata.containsKey(key)) {
        reportMarkdown
            .append("\n**")
            .append(key)
            .append("**\n\n~~~~text\n")
            .append(metadata.get(key).toString().replace("~~~~", "''''"))
            .append("\n~~~~\n");
      }
    }
  }

  private static String readAllureLabel(JsonNode testResultNode, String labelName) {
    List<String> values = new ArrayList<>();
    for (JsonNode labelNode : testResultNode.path("labels")) {
      if (labelName.equals(labelNode.path("name").asText())) {
        values.add(labelNode.path("value").asText());
      }
    }
    return String.join("; ", values);
  }

  private static String formatResultTimestamp(JsonNode test, String key) {
    return test.path(key).isNumber()
        ? Instant.ofEpochMilli(test.path(key).asLong()).toString()
        : "nicht verfügbar";
  }

  private static String escapeMarkdownTableCell(Object value) {
    return Objects.toString(value, "nicht verfügbar")
        .replace("|", "\\|")
        .replace('\n', ' ')
        .replace('\r', ' ');
  }
}
