package de.derpeterson.browsertests.i18n;

import com.microsoft.playwright.Page;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;

/** Reads the application's original translation resources, without Spring or a Vaadin session. */
public final class UiTexts {
    private final Locale locale;
    private final PropertyResourceBundle messages;

    public UiTexts(String language) {
        locale = switch (language) {
            case "de" -> Locale.GERMAN;
            case "en" -> Locale.ENGLISH;
            default -> throw new IllegalArgumentException("Unsupported test language: " + language);
        };
        String resource = "/i18n/messages_" + language + ".properties";
        try (var input = Objects.requireNonNull(UiTexts.class.getResourceAsStream(resource),
                "Translation resource missing: " + resource)) {
            // Same decoder as the application's ResourceBundle: UTF-8 with legacy ISO-8859-1 support.
            messages = new PropertyResourceBundle(input);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read translations: " + resource, failure);
        }
    }

    public static UiTexts selected() {
        return new UiTexts(System.getProperty("browser.tests.language", "de"));
    }

    public String language() { return locale.getLanguage(); }
    public String browserLocale() { return language().equals("de") ? "de-DE" : "en-US"; }

    public String text(String key, Object... arguments) {
        String pattern = messages.containsKey(key) ? messages.getString(key) : null;
        if (pattern == null || pattern.isBlank())
            throw new IllegalArgumentException("Missing or empty translation: " + language() + "/" + key);
        return new MessageFormat(pattern, locale).format(arguments);
    }

    /** Render notification markup as text in an inert template, without changing the page. */
    public String visibleText(Page page, String key, Object... arguments) {
        return (String) page.evaluate("html => { const t = document.createElement('template'); "
                + "t.innerHTML = html; return t.content.textContent; }", text(key, arguments));
    }

    public String dateInput(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(language().equals("de") ? "dd.MM.uuuu" : "M/d/uuuu", locale));
    }
}
