package de.derpeterson.browsertests.i18n;

import com.microsoft.playwright.Page;
import de.derpeterson.app.i18n.MessageTexts;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.function.Function;

/** Reads the application's original translation resources, without Spring or a Vaadin session. */
public final class UiTexts extends MessageTexts {
  private final Locale locale;

  public UiTexts(String language) {
    this(
        switch (language) {
          case "de" -> Locale.GERMAN;
          case "en" -> Locale.ENGLISH;
          default -> throw new IllegalArgumentException("Unsupported test language: " + language);
        });
  }

  private UiTexts(Locale locale) {
    super(lookup(locale));
    this.locale = locale;
  }

  private static Function<String, String> lookup(Locale locale) {
    String resource = "/i18n/messages_" + locale.getLanguage() + ".properties";
    final PropertyResourceBundle messages;
    try (var input =
        Objects.requireNonNull(
            UiTexts.class.getResourceAsStream(resource),
            "Translation resource missing: " + resource)) {
      messages = new PropertyResourceBundle(input);
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot read translations: " + resource, failure);
    }
    return key -> {
      String pattern = messages.containsKey(key) ? messages.getString(key) : null;
      if (pattern == null || pattern.isBlank())
        throw new IllegalArgumentException(
            "Missing or empty translation: " + locale.getLanguage() + "/" + key);
      // Same translation/formatting sequence as CustomI18NProvider + MessageProperties.
      return MessageFormat.format(pattern, new Object[0]);
    };
  }

  public static UiTexts selected() {
    return new UiTexts(System.getProperty("browser.tests.language", "de"));
  }

  public String language() {
    return locale.getLanguage();
  }

  public String browserLocale() {
    return language().equals("de") ? "de-DE" : "en-US";
  }

  /** Render notification markup as text in an inert template, without changing the page. */
  public String visibleText(Page page, String html) {
    return (String)
        page.evaluate(
            "html => { const t = document.createElement('template'); "
                + "t.innerHTML = html; return t.content.textContent; }",
            html);
  }

  public String timeInput(LocalTime time) {
    return time.format(
        DateTimeFormatter.ofPattern(language().equals("de") ? "HH:mm" : "h:mm a", locale));
  }

  public String dateInput(LocalDate date) {
    return date.format(
        DateTimeFormatter.ofPattern(language().equals("de") ? "dd.MM.uuuu" : "M/d/uuuu", locale));
  }
}
