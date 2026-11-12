package de.derpeterson.app.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinSession;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Locale lifecycle contracts; UI rendering remains in the browser suites. */
class MessagePropertiesLocaleTest {
  private final CustomI18NProvider i18nProvider =
      new CustomI18NProvider() {
        @Override
        public String getTranslation(String key, Locale locale, Object... parameters) {
          return locale.getLanguage();
        }
      };

  @AfterEach
  void cleanup() {
    VaadinSession.setCurrent(null);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de, 0, 0 Konten", "de, 1, 1 Konto", "de, 2, 2 Konten", "de, 102, 102 Konten",
    "en, 0, 0 Accounts", "en, 1, 1 Account", "en, 2, 2 Accounts", "en, 102, 102 Accounts"
  })
  void accountCountSurvivesTheRealTwoStageTranslation(String language, int count, String expected) {
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));

    assertEquals(expected, messages.getAdminUsersAccountCount(count));
    assertFalse(messages.getAdminUsersAccountCount(count).contains("{"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de, '1,23456789 Sekunden'",
    "en, '1.23456789 seconds'"
  })
  void durationNumbersUseTheSelectedLocaleAndKeepAllDigits(String language, String expected) {
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));

    assertEquals(expected, messages.getAdminConfigDurationSeconds(new BigDecimal("1.23456789")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de, Endgültige Löschung am 31.12.2026 UTC",
    "en, Permanent deletion on 31.12.2026 UTC"
  })
  void scheduledDeletionLabelPreservesUmlautsAndInsertsTheDeadline(
      String languageTag, String expectedLabelText) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));

    assertEquals(
        expectedLabelText, messageProperties.getAdminUserScheduledDeletion("31.12.2026 UTC"));
  }

  @Test
  void normalGettersFollowTheSessionLanguageWithoutRecreatingTheBean() {
    AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenAnswer(ignored -> localeReference.get());
    VaadinSession.setCurrent(vaadinSession);
    MessageProperties messages = new MessageProperties(i18nProvider);

    assertEquals("de", messages.getLoginTitle());
    localeReference.set(Locale.ENGLISH);

    assertEquals("en", messages.getLoginTitle());
  }

  @Test
  void explicitLocaleWorksWithoutSessionAndDoesNotFollowLaterSessionChanges() {
    MessageProperties messages = MessageProperties.forLocale(i18nProvider, Locale.GERMAN);

    assertEquals("de", messages.getLoginTitle());
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(Locale.ENGLISH);
    VaadinSession.setCurrent(vaadinSession);

    assertEquals("de", messages.getLoginTitle());
    assertEquals(Locale.ENGLISH, vaadinSession.getLocale());
  }
}
