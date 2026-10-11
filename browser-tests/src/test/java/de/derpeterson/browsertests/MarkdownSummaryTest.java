package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownSummaryTest {
    private Map<String, Object> complete() {
        return new HashMap<>(Map.of("setup", "passed", "tests", "passed", "cleanup", "passed", "html", "passed", "finishedAt", "2026-10-11"));
    }

    @Test void successfulCaseIsChecked() throws Exception {
        var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
        String report = MarkdownSummary.render(complete(), new ArrayList<>(List.of(test)));
        assertTrue(report.contains("**Status: BESTANDEN**"));
        assertTrue(report.contains("- [x] **BESTANDEN**"));
    }

    @Test void skippedCaseIsNotSuccess() throws Exception {
        var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"skipped\"}");
        String report = MarkdownSummary.render(complete(), new ArrayList<>(List.of(test)));
        assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
        assertFalse(report.contains("- [x]"));
    }

    @Test void cleanupFailurePreventsSuccess() throws Exception {
        var metadata = complete();
        metadata.put("cleanup", "failed");
        var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
        assertTrue(MarkdownSummary.render(metadata, new ArrayList<>(List.of(test))).contains("UNVOLLSTÄNDIG / FEHLER"));
    }

    @Test void emptyOrUnfinishedRunIsNotSuccess() {
        assertTrue(MarkdownSummary.render(complete(), new ArrayList<>()).contains("UNVOLLSTÄNDIG / FEHLER"));
        var metadata = complete();
        metadata.remove("finishedAt");
        assertTrue(MarkdownSummary.render(metadata, new ArrayList<>()).contains("IN BEARBEITUNG"));
    }

    @Test void missingCaseOrDiagnosticCannotBeHidden() throws Exception {
        var metadata = complete();
        metadata.put("testsFound", 2);
        var test = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"failed\",\"statusDetails\":{\"message\":\"Falscher Titel\"},\"attachments\":[{\"name\":\"Screenshot\",\"source\":\"probe.png\"}]}");
        String report = MarkdownSummary.render(metadata, new ArrayList<>(List.of(test)));
        assertTrue(report.contains("UNVOLLSTÄNDIG / FEHLER"));
        assertTrue(report.contains("Falscher Titel"));
        assertTrue(report.contains("[Screenshot](allure-results/probe.png)"));
        var passed = new ObjectMapper().readTree("{\"name\":\"Login\",\"status\":\"passed\"}");
        assertTrue(MarkdownSummary.render(metadata, new ArrayList<>(List.of(passed))).contains("UNVOLLSTÄNDIG / FEHLER"));
    }
}
