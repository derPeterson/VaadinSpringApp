package de.derpeterson.browsertests.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UiTextsTest {
    @ParameterizedTest
    @ValueSource(strings = {"de", "en"})
    void requiredKeysNeverFallBackToAnotherLanguageOrTheKeyItself(String language) {
        UiTexts texts = new UiTexts(language);
        assertThrows(IllegalArgumentException.class, () -> texts.getTranslation("missing.browser.test.key"));
        assertEquals(language, texts.language());
    }

    @Test
    void unsupportedLanguageIsRejectedInsteadOfSilentlyUsingEnglish() {
        assertThrows(IllegalArgumentException.class, () -> new UiTexts("fr"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"de", "en"})
    void formattedMessageKeepsItsRecipientAndSubstitutesItsPlaceholder(String language) {
        String result = new UiTexts(language).getRegistrationSuccessMessage("test@example.test");
        assertTrue(result.contains("test@example.test"));
        assertFalse(result.contains("{0}"));
        assertFalse(result.contains("\uFFFD"), "Original translation must not contain decoding replacement characters.");
        assertNotEquals("registrationView.registration.success_message", result);
    }

    @Test
    void dateEntryMatchesTheExplicitUiLanguage() {
        var date = LocalDate.of(1990, 1, 1);
        assertEquals("01.01.1990", new UiTexts("de").dateInput(date));
        assertEquals("1/1/1990", new UiTexts("en").dateInput(date));
    }
}
