package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownSummaryTest {
  @TempDir Path temporaryDirectoryPath;

  private Map<String, Object> successfulRunMetadata() {
    return new HashMap<>(
        Map.of(
            "setup",
            "passed",
            "tests",
            "passed",
            "cleanup",
            "passed",
            "html",
            "passed",
            "finishedAt",
            "2026-10-11",
            "browserTestModulePath",
            temporaryDirectoryPath.resolve("browser module").toString()));
  }

  @Test
  void externalReportLinksToOriginalDocumentationAndKeepsLocalEvidenceRelative() throws Exception {
    Path browserTestModulePath =
        Files.createDirectory(temporaryDirectoryPath.resolve("Projekt mit Leerzeichen und Ü"));
    Files.writeString(browserTestModulePath.resolve("README.md"), "Testanleitung");
    Files.writeString(browserTestModulePath.resolve("abdeckung.md"), "Prüfgrenzen");
    Path externalRunPath =
        Files.createDirectory(temporaryDirectoryPath.resolve("external evidence"));
    Path allureResultsPath = Files.createDirectory(externalRunPath.resolve("allure-results"));
    Files.writeString(
        allureResultsPath.resolve("case-result.json"),
        "{\"name\":\"Login\",\"status\":\"passed\"}");
    Map<String, Object> runMetadata = successfulRunMetadata();
    runMetadata.put("browserTestModulePath", browserTestModulePath.toString());

    MarkdownSummary.writeRunReport(externalRunPath, runMetadata);
    String reportMarkdown = Files.readString(externalRunPath.resolve("report.md"));

    assertTrue(
        reportMarkdown.contains(
            "[Browser-Testanleitung]("
                + browserTestModulePath.resolve("README.md").toUri().toASCIIString()
                + ")"));
    assertTrue(
        reportMarkdown.contains(
            "[Abdeckung und offene Punkte]("
                + browserTestModulePath.resolve("abdeckung.md").toUri().toASCIIString()
                + ")"));
    assertFalse(reportMarkdown.contains("../../README.md"));
    assertFalse(reportMarkdown.contains("../../abdeckung.md"));
    assertTrue(reportMarkdown.contains("[HTML-Bericht starten](html/open-report.bat)"));
    assertTrue(reportMarkdown.contains("[Laufdaten und Git-Stand](metadata.json)"));
  }

  @Test
  void successfulCaseIsChecked() throws Exception {
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report =
        MarkdownSummary.renderRunReport(successfulRunMetadata(), new ArrayList<>(List.of(test)));

    assertTrue(report.contains("**Status: BESTANDEN**"));
    assertTrue(report.contains("- [x] **BESTANDEN**"));
  }

  @Test
  void requestedCoverageFailurePreventsAGreenOverallReport() throws Exception {
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("coverage", "failed");
    metadata.put("coverageError", "Missing execution data");
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.renderRunReport(metadata, List.of(test));

    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertTrue(report.contains("Missing execution data"));
  }

  @Test
  void measuredCoverageLinksToTheArchivedReportAndStatesItsScope() throws Exception {
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("coverage", "passed");
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.renderRunReport(metadata, List.of(test));

    assertTrue(report.contains("coverage/html/index.html"));
    assertTrue(report.contains("serverseitige Anwendungscode"));
    assertTrue(report.contains("**Status: BESTANDEN**"));
  }

  @Test
  void skippedCaseIsNotSuccess() throws Exception {
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"skipped\"}");
    String report =
        MarkdownSummary.renderRunReport(successfulRunMetadata(), new ArrayList<>(List.of(test)));

    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertFalse(report.contains("- [x]"));
  }

  @Test
  void cleanupFailurePreventsSuccess() throws Exception {
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("cleanup", "failed");
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");

    assertTrue(
        MarkdownSummary.renderRunReport(metadata, new ArrayList<>(List.of(test)))
            .contains("UNVOLLSTÄNDIG / FEHLER"));
  }

  @Test
  void emptyOrUnfinishedRunIsNotSuccess() {
    assertTrue(
        MarkdownSummary.renderRunReport(successfulRunMetadata(), new ArrayList<>())
            .contains("UNVOLLSTÄNDIG / FEHLER"));
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.remove("finishedAt");

    assertTrue(
        MarkdownSummary.renderRunReport(metadata, new ArrayList<>()).contains("IN BEARBEITUNG"));
  }

  @Test
  void missingCaseOrDiagnosticCannotBeHidden() throws Exception {
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("testsFound", 2);
    JsonNode test =
        new ObjectMapper()
            .readTree(
                "{\"name\":\"Login\",\"status\":\"failed\",\"statusDetails\":{\"message\":\"Falscher Titel\"},\"attachments\":[{\"name\":\"Screenshot\",\"source\":\"probe.png\"}]}");
    String report = MarkdownSummary.renderRunReport(metadata, new ArrayList<>(List.of(test)));

    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertTrue(report.contains("Falscher Titel"));
    assertTrue(report.contains("[Screenshot](allure-results/probe.png)"));
    JsonNode passed = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");

    assertTrue(
        MarkdownSummary.renderRunReport(metadata, new ArrayList<>(List.of(passed)))
            .contains("UNVOLLSTÄNDIG / FEHLER"));
  }

  @Test
  void checklistKeepsStepsAreaAndTimeWithoutDependingOnManualTests() throws Exception {
    JsonNode test =
        new ObjectMapper()
            .readTree(
                """
                {"name":"AUTH-01B: Falsches Passwort","status":"passed","start":1791748800000,"stop":1791748801000,
                 "description":"1. Login öffnen.\\n\\n**Erwartet:** Keine Anmeldung.",
                 "labels":[{"name":"feature","value":"Anmeldung"},{"name":"story","value":"Abgelehnte Anmeldung"}]}
                """);
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("uiLanguage", "en");
    String report = MarkdownSummary.renderRunReport(metadata, List.of(test));

    assertTrue(report.contains("| Geprüfte Oberflächensprache | en |"));
    assertTrue(report.contains("### Anmeldung"));
    assertTrue(report.contains("**Prüfbereich:** Abgelehnte Anmeldung"));
    assertTrue(report.contains("**Erwartet:** Keine Anmeldung."));
    assertTrue(report.contains("**Zeitpunkt (UTC):** 2026-10-11T"));
    assertTrue(report.contains("keine vollständige Prüfung der Anwendung"));
    assertFalse(report.contains("docs/manual-tests"));
    assertFalse(report.contains("Manuelle Vorlage"));
  }

  @Test
  void skippedChecklistRetainsItsInstructionsAndReason() throws Exception {
    JsonNode test =
        new ObjectMapper()
            .readTree(
                """
                {"name":"AUTH-01A","status":"skipped","description":"1. Admin anmelden.",
                 "statusDetails":{"message":"Testumgebung nicht erreichbar."}}
                """);
    String report = MarkdownSummary.renderRunReport(successfulRunMetadata(), List.of(test));

    assertTrue(report.contains("1. Admin anmelden."));
    assertTrue(report.contains("**Status:** NICHT AUSGEFÜHRT"));
    assertTrue(report.contains("Testumgebung nicht erreichbar."));
    assertFalse(report.contains("- [x]"));
  }

  @Test
  void fastParallelReportShowsItsScopeAndWorkerDiagnostics() throws Exception {
    Map<String, Object> metadata = successfulRunMetadata();
    metadata.put("suite", "fast");
    metadata.put("workersRequested", 2);
    metadata.put("workersActual", 2);
    metadata.put(
        "workerResults",
        List.of(
            Map.of(
                "worker",
                "1",
                "testsPassed",
                2,
                "testsFound",
                2,
                "setup",
                "passed",
                "cleanup",
                "passed",
                "exitCode",
                0)));
    JsonNode test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.renderRunReport(metadata, List.of(test));

    assertTrue(report.contains("Schnellprüfung"));
    assertTrue(report.contains("`all` bleibt die vollständige Auswahl"));
    assertTrue(report.contains("Angefordert: 2; gestartet: 2"));
    assertTrue(report.contains("workers/1/metadata.json"));
    metadata.put("tests", "failed");

    assertTrue(
        MarkdownSummary.renderRunReport(metadata, List.of(test))
            .contains("UNVOLLSTÄNDIG / FEHLER"));
  }
}
