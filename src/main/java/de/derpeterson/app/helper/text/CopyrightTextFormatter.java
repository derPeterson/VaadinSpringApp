package de.derpeterson.app.helper.text;

import de.derpeterson.app.model.enums.ConfigEntry;

import java.time.Clock;
import java.time.Year;
import java.util.function.Supplier;

/** Gemeinsamer Plain-Text-Vertrag für den Copyright-Fußtext der Oberfläche und Mails. */
public final class CopyrightTextFormatter {

    private CopyrightTextFormatter() {
    }

    public static String format(String configuredTemplate, String serviceName, Clock clock,
                                Supplier<String> localizedDefaultTemplate) {
        // Nur der unveränderte Standard wird übersetzt; eigene DB-Texte bleiben unangetastet.
        String displayTemplate = localizedDefaultTemplate != null
                && configuredTemplate.equals(ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString())
                ? localizedDefaultTemplate.get() : configuredTemplate;
        return displayTemplate.replace("{year}", String.valueOf(Year.now(clock).getValue()))
                .replace("{serviceName}", serviceName);
    }
}
