package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Human checklist from Allure results; Allure itself still produces the HTML. */
final class MarkdownSummary {
    static void write(Path run, Map<String, Object> metadata) throws IOException {
        Files.writeString(run.resolve("report.md"), render(metadata, readResults(run)));
    }

    static List<JsonNode> readResults(Path run) throws IOException {
        List<JsonNode> results = new ArrayList<>();
        try (var files = Files.list(run.resolve("allure-results"))) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith("-result.json")).sorted().toList())
                results.add(new ObjectMapper().readTree(file.toFile()));
        }
        return results;
    }

    static String render(Map<String, Object> metadata, List<JsonNode> results) {
        boolean finished = metadata.containsKey("finishedAt");
        boolean passed = finished && List.of("setup", "tests", "cleanup", "html").stream().allMatch(key -> "passed".equals(metadata.get(key)))
                && !results.isEmpty() && (!metadata.containsKey("testsFound") || results.size() == ((Number) metadata.get("testsFound")).longValue())
                && results.stream().allMatch(test -> "passed".equals(test.path("status").asText()));
        String state = !finished ? "IN BEARBEITUNG" : passed ? "BESTANDEN" : "UNVOLLSTÄNDIG / FEHLER";
        var text = new StringBuilder("# Automatisierter Browser-Testlauf (Java)\n\n**Status: " + state + "**\n\n| Angabe | Wert |\n| --- | --- |\n");
        Map<String, String> labels = Map.ofEntries(Map.entry("runId", "Run-ID"), Map.entry("startedAt", "Beginn (UTC)"),
                Map.entry("finishedAt", "Ende (UTC)"), Map.entry("commit", "Git-Commit"), Map.entry("branch", "Branch"),
                Map.entry("imageId", "App-Image-ID"), Map.entry("build", "Image-Build aufgerufen (Cache möglich)"),
                Map.entry("failureProbe", "Absichtlicher Fehler-Probelauf"), Map.entry("suite", "Ausgewählte Suite"),
                Map.entry("browser", "Browser"), Map.entry("viewport", "Bildschirmgröße"),
                Map.entry("browserLocale", "Browser-Locale"), Map.entry("browserTimezone", "Browser-Zeitzone"),
                Map.entry("appUrl", "Anwendung"), Map.entry("mailpitUrl", "Mailpit"), Map.entry("project", "Docker-Projekt"),
                Map.entry("setup", "Aufbau"), Map.entry("tests", "Testausführung"), Map.entry("cleanup", "Abbau"), Map.entry("html", "HTML-Bericht"));
        for (String key : List.of("runId", "startedAt", "finishedAt", "commit", "branch", "imageId", "build", "suite", "failureProbe", "browser", "viewport", "browserLocale", "browserTimezone", "appUrl", "mailpitUrl", "project", "setup", "tests", "cleanup", "html"))
            text.append("| ").append(labels.get(key)).append(" | ").append(cell(metadata.get(key))).append(" |\n");
        text.append("| Lokale Änderungen | ").append(metadata.getOrDefault("gitStatus", "").toString().isBlank() ? "nein" : "ja; siehe metadata.json").append(" |\n");
        text.append("\n## Umfang und Grenzen\n\n")
                .append("Dieses Protokoll bewertet nur die ausgewählten, unten aufgeführten Browserfälle. ")
                .append("Ein bestandener Lauf belegt nur diese Fälle, keine vollständige Prüfung der Anwendung. ")
                .append("Nicht ausgewählte Suites wurden in diesem Lauf nicht geprüft. ")
                .append("Weitere Eingabevarianten, Verifikationsablauf und erneuter Versand, Adminsperren, Passwortreset, Remember-me, dynamischer Rollenentzug und weitere Betriebsfälle sind noch nicht automatisiert.\n\n")
                .append("Suites, Testfälle und Grenzen: [Browser-Testanleitung](../../README.md).\n\n## Ergebnisse\n\n");
        if (results.isEmpty()) text.append("Keine Testfälle vom Reporter erfasst.\n");
        List<JsonNode> ordered = new ArrayList<>(results);
        ordered.sort(Comparator.comparing((JsonNode test) -> label(test, "feature")).thenComparing(test -> test.path("name").asText()));
        String previousSuite = null;
        for (JsonNode test : ordered) {
            String suite = label(test, "feature");
            if (suite.isBlank()) suite = "Weitere Prüfungen";
            if (!suite.equals(previousSuite)) {
                text.append("### ").append(cell(suite)).append("\n\n");
                previousSuite = suite;
            }
            String status = test.path("status").asText("unknown");
            String readable = switch (status) {
                case "passed" -> "BESTANDEN";
                case "failed", "broken" -> "FEHLER";
                case "skipped" -> "NICHT AUSGEFÜHRT";
                default -> "UNBEKANNT";
            };
            text.append("#### ").append(cell(test.path("name").asText())).append("\n\n");
            String story = label(test, "story");
            if (!story.isBlank()) text.append("**Prüfbereich:** ").append(cell(story)).append("\n\n");
            String description = test.path("description").asText();
            if (!description.isBlank()) text.append(description).append("\n\n");
            text.append("- [").append("passed".equals(status) ? "x" : " ").append("] **").append(readable).append("**\n")
                    .append("- **Status:** ").append(readable).append("\n")
                    .append("- **Zeitpunkt (UTC):** ").append(timestamp(test, "start")).append(" → ").append(timestamp(test, "stop")).append("\n")
                    .append("- **Ist / Grund:** ").append(switch (status) {
                        case "passed" -> "Alle ausgeführten Assertions dieses Testfalls erfüllt.";
                        case "skipped" -> "Testfall nicht ausgeführt; kein erfolgreicher Nachweis.";
                        default -> "Erwartung nicht vollständig bestätigt; Diagnose siehe unten.";
                    }).append("\n\n");
            String error = test.path("statusDetails").path("message").asText();
            if (!error.isBlank()) text.append("~~~~text\n").append(error.replace("~~~~", "''''")).append("\n~~~~\n\n");
            Map<String, String> attachments = new LinkedHashMap<>();
            for (JsonNode array : test.findValues("attachments")) {
                for (JsonNode attachment : array) {
                    String source = attachment.path("source").asText();
                    if (source.matches("[\\w.-]+")) attachments.put(source, attachment.path("name").asText("Beleg"));
                }
            }
            if (!attachments.isEmpty()) {
                text.append("**Belege:**\n\n");
                attachments.forEach((source, name) -> text.append("- [").append(cell(name)).append("](allure-results/").append(source).append(")\n"));
                text.append("\n");
            }
        }
        for (String key : List.of("error", "cleanupError", "reportError")) {
            if (metadata.containsKey(key)) text.append("\n**").append(key).append("**\n\n~~~~text\n")
                    .append(metadata.get(key).toString().replace("~~~~", "''''")).append("\n~~~~\n");
        }
        text.append("\n## Belege des Durchlaufs\n\n- [HTML-Bericht starten](html/open-report.bat) — im Explorer doppelklicken; benötigt den lokalen Testordner.\n")
                .append("- [HTML-Einstiegsdatei](html/index.html) — über den Berichtserver öffnen, nicht direkt als Datei.\n")
                .append("- [Laufdaten und Git-Stand](metadata.json)\n- [Ablauflog](runner.log)\n\n")
                .append("Screenshots, Browseraktionen und Traces befinden sich auch im Allure-Bericht. Lokale Belege sind von Git ausgeschlossen.\n");
        return text.toString();
    }

    private static String label(JsonNode test, String name) {
        List<String> values = new ArrayList<>();
        for (JsonNode label : test.path("labels"))
            if (name.equals(label.path("name").asText())) values.add(label.path("value").asText());
        return String.join("; ", values);
    }

    private static String timestamp(JsonNode test, String key) {
        return test.path(key).isNumber() ? Instant.ofEpochMilli(test.path(key).asLong()).toString() : "nicht verfügbar";
    }

    private static String cell(Object value) {
        return Objects.toString(value, "nicht verfügbar").replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
    }
}
