package de.derpeterson.app.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinSession;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Locale lifecycle contracts; UI rendering remains in the browser suites. */
class MessagePropertiesLocaleTest {
  private final CustomI18NProvider provider =
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

  @Test
  void normalGettersFollowTheSessionLanguageWithoutRecreatingTheBean() {
    AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
    VaadinSession session = mock(VaadinSession.class);
    when(session.getLocale()).thenAnswer(ignored -> locale.get());
    VaadinSession.setCurrent(session);
    MessageProperties messages = new MessageProperties(provider);
    assertEquals("de", messages.getLoginTitle());
    locale.set(Locale.ENGLISH);
    assertEquals("en", messages.getLoginTitle());
  }

  @Test
  void explicitLocaleWorksWithoutSessionAndDoesNotFollowLaterSessionChanges() {
    MessageProperties messages = MessageProperties.forLocale(provider, Locale.GERMAN);
    assertEquals("de", messages.getLoginTitle());
    VaadinSession session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.ENGLISH);
    VaadinSession.setCurrent(session);
    assertEquals("de", messages.getLoginTitle());
    assertEquals(Locale.ENGLISH, session.getLocale());
  }
}
