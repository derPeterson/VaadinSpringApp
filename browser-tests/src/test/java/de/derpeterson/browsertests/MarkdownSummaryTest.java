package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

class MarkdownSummaryTest {
  private Map<String, Object> complete() {
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
            "2026-10-11"));
  }

  @Test
  void successfulCaseIsChecked() throws Exception {
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.render(complete(), new ArrayList<>(List.of(test)));
    assertTrue(report.contains("**Status: BESTANDEN**"));
    assertTrue(report.contains("- [x] **BESTANDEN**"));
  }

  @Test
  void requestedCoverageFailurePreventsAGreenOverallReport() throws Exception {
    var metadata = complete();
    metadata.put("coverage", "failed");
    metadata.put("coverageError", "Missing execution data");
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.render(metadata, List.of(test));
    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertTrue(report.contains("Missing execution data"));
  }

  @Test
  void measuredCoverageLinksToTheArchivedReportAndStatesItsScope() throws Exception {
    var metadata = complete();
    metadata.put("coverage", "passed");
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.render(metadata, List.of(test));
    assertTrue(report.contains("coverage/html/index.html"));
    assertTrue(report.contains("serverseitige Anwendungscode"));
    assertTrue(report.contains("**Status: BESTANDEN**"));
  }

  @Test
  void skippedCaseIsNotSuccess() throws Exception {
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"skipped\"}");
    String report = MarkdownSummary.render(complete(), new ArrayList<>(List.of(test)));
    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertFalse(report.contains("- [x]"));
  }

  @Test
  void cleanupFailurePreventsSuccess() throws Exception {
    var metadata = complete();
    metadata.put("cleanup", "failed");
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    assertTrue(
        MarkdownSummary.render(metadata, new ArrayList<>(List.of(test)))
            .contains("UNVOLLSTÄNDIG / FEHLER"));
  }

  @Test
  void emptyOrUnfinishedRunIsNotSuccess() {
    assertTrue(
        MarkdownSummary.render(complete(), new ArrayList<>()).contains("UNVOLLSTÄNDIG / FEHLER"));
    var metadata = complete();
    metadata.remove("finishedAt");
    assertTrue(MarkdownSummary.render(metadata, new ArrayList<>()).contains("IN BEARBEITUNG"));
  }

  @Test
  void missingCaseOrDiagnosticCannotBeHidden() throws Exception {
    var metadata = complete();
    metadata.put("testsFound", 2);
    var test =
        new ObjectMapper()
            .readTree(
                "{\"name\":\"Login\",\"status\":\"failed\",\"statusDetails\":{\"message\":\"Falscher Titel\"},\"attachments\":[{\"name\":\"Screenshot\",\"source\":\"probe.png\"}]}");
    String report = MarkdownSummary.render(metadata, new ArrayList<>(List.of(test)));
    assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
    assertTrue(report.contains("Falscher Titel"));
    assertTrue(report.contains("[Screenshot](allure-results/probe.png)"));
    var passed = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    assertTrue(
        MarkdownSummary.render(metadata, new ArrayList<>(List.of(passed)))
            .contains("UNVOLLSTÄNDIG / FEHLER"));
  }

  @Test
  void checklistKeepsStepsAreaAndTimeWithoutDependingOnManualTests() throws Exception {
    var test =
        new ObjectMapper()
            .readTree(
                """
                {"name":"AUTH-01B: Falsches Passwort","status":"passed","start":1791748800000,"stop":1791748801000,
                 "description":"1. Login öffnen.\\n\\n**Erwartet:** Keine Anmeldung.",
                 "labels":[{"name":"feature","value":"Anmeldung"},{"name":"story","value":"Abgelehnte Anmeldung"}]}
                """);
    var metadata = complete();
    metadata.put("uiLanguage", "en");
    String report = MarkdownSummary.render(metadata, List.of(test));
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
    var test =
        new ObjectMapper()
            .readTree(
                """
                {"name":"AUTH-01A","status":"skipped","description":"1. Admin anmelden.",
                 "statusDetails":{"message":"Testumgebung nicht erreichbar."}}
                """);
    String report = MarkdownSummary.render(complete(), List.of(test));
    assertTrue(report.contains("1. Admin anmelden."));
    assertTrue(report.contains("**Status:** NICHT AUSGEFÜHRT"));
    assertTrue(report.contains("Testumgebung nicht erreichbar."));
    assertFalse(report.contains("- [x]"));
  }

  @Test
  void fastParallelReportShowsItsScopeAndWorkerDiagnostics() throws Exception {
    var metadata = complete();
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
    var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
    String report = MarkdownSummary.render(metadata, List.of(test));
    assertTrue(report.contains("Schnellprüfung"));
    assertTrue(report.contains("`all` bleibt die vollständige Auswahl"));
    assertTrue(report.contains("Angefordert: 2; gestartet: 2"));
    assertTrue(report.contains("workers/1/metadata.json"));
    metadata.put("tests", "failed");
    assertTrue(MarkdownSummary.render(metadata, List.of(test)).contains("UNVOLLSTÄNDIG / FEHLER"));
  }
}
