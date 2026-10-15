package de.derpeterson.app.helper.text;

import static org.junit.jupiter.api.Assertions.*;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CopyrightTextFormatterTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2030-12-31T23:30:00Z"), ZoneId.of("Europe/Berlin"));

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void defaultUsesTypedTranslationAndTheClocksLocalYear(String language) {
    var texts =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    String result =
        CopyrightTextFormatter.format(
            ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString(),
            "Example",
            CLOCK,
            texts::getAccountCopyrightTemplate);
    assertTrue(result.startsWith("© 2031 Example."));
    assertFalse(result.contains("{year}"));
    assertFalse(result.contains("{serviceName}"));
    assertTrue(result.length() > "© 2031 Example.".length());
  }

  @Test
  void customTextDoesNotConsultTheDefaultTranslation() {
    assertEquals(
        "© 2031 Example & Co <b>custom</b>",
        CopyrightTextFormatter.format(
            "© {year} {serviceName} <b>custom</b>",
            "Example & Co",
            CLOCK,
            () -> {
              throw new AssertionError("Custom DB text must not be translated");
            }));
  }

  @Test
  void emptyConfiguredTextRemainsEmpty() {
    assertEquals(
        "",
        CopyrightTextFormatter.format(
            "",
            "Example",
            CLOCK,
            () -> {
              throw new AssertionError("Empty DB text must not be replaced");
            }));
  }
}
