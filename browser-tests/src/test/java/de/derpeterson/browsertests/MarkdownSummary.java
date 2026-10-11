package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Small project-specific checklist; the interactive HTML report is produced by Allure. */
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
                Map.entry("failureProbe", "Absichtlicher Fehler-Probelauf"), Map.entry("appUrl", "Anwendung"),
                Map.entry("mailpitUrl", "Mailpit"), Map.entry("project", "Docker-Projekt"), Map.entry("setup", "Aufbau"),
                Map.entry("tests", "Testausführung"), Map.entry("cleanup", "Abbau"), Map.entry("html", "HTML-Bericht"));
        for (String key : List.of("runId", "startedAt", "finishedAt", "commit", "branch", "imageId", "build", "failureProbe", "appUrl", "mailpitUrl", "project", "setup", "tests", "cleanup", "html"))
            text.append("| ").append(labels.get(key)).append(" | ").append(cell(metadata.get(key))).append(" |\n");
        text.append("| Lokale Änderungen | ").append(metadata.getOrDefault("gitStatus", "").toString().isBlank() ? "nein" : "ja; siehe metadata.json").append(" |\n");
        text.append("\nUmfang: Infrastrukturprüfungen; keine vollständigen Login-, Registrierungs- oder Resetabläufe.\n\n## Ergebnisse\n\n");
        if (results.isEmpty()) text.append("Keine Testfälle vom Reporter erfasst.\n");
        results.sort(Comparator.comparing(test -> test.path("name").asText()));
        for (JsonNode test : results) {
            String status = test.path("status").asText("unknown");
            text.append("- [").append("passed".equals(status) ? "x" : " ").append("] **")
                    .append(switch (status) { case "passed" -> "BESTANDEN"; case "failed", "broken" -> "FEHLER"; case "skipped" -> "NICHT AUSGEFÜHRT"; default -> "UNBEKANNT"; })
                    .append("** — ").append(cell(test.path("name").asText())).append("\n");
            String error = test.path("statusDetails").path("message").asText();
            if (!error.isBlank()) text.append("\n````text\n").append(error.replace("````", "''''")).append("\n````\n\n");
            Map<String, String> attachments = new LinkedHashMap<>();
            for (JsonNode array : test.findValues("attachments")) {
                for (JsonNode attachment : array) {
                    String source = attachment.path("source").asText();
                    if (source.matches("[\\w.-]+")) attachments.put(source, attachment.path("name").asText("Beleg"));
                }
            }
            attachments.forEach((source, name) -> text.append("  - [").append(cell(name)).append("](allure-results/").append(source).append(")\n"));
        }
        for (String key : List.of("error", "cleanupError", "reportError")) {
            if (metadata.containsKey(key)) text.append("\n**").append(key).append("**\n\n````text\n")
                    .append(metadata.get(key).toString().replace("````", "''''")).append("\n````\n");
        }
        text.append("\n## Belege\n\n- [Allure-HTML-Bericht](html/index.html)\n- [Laufdaten und Git-Stand](metadata.json)\n- [Ablauflog](runner.log)\n\n")
                .append("Screenshots, Browseraktionen und Traces befinden sich im Allure-Bericht. Lokale Belege sind von Git ausgeschlossen.\n");
        return text.toString();
    }

    private static String cell(Object value) {
        return Objects.toString(value, "nicht verfügbar").replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
    }
}
