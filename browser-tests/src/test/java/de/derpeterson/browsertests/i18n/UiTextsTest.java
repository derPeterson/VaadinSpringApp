package de.derpeterson.browsertests.i18n;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UiTextsTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requiredKeysNeverFallBackToAnotherLanguageOrTheKeyItself(String language) {
    UiTexts texts = new UiTexts(language);
    assertThrows(
        IllegalArgumentException.class, () -> texts.getTranslation("missing.browser.test.key"));
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
    assertFalse(
        result.contains("\uFFFD"),
        "Original translation must not contain decoding replacement characters.");
    assertNotEquals("registrationView.registration.success_message", result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void tokenMinimumMessagesAndExistingTooltipGettersAreFormattedInBothLanguages(String language) {
    UiTexts texts = new UiTexts(language);
    for (String value :
        new String[] {
          texts.getAdminConfigValidationDurationMinimum(1),
          texts.getAdminConfigValidationTokenDuration(2),
          texts.getAdminConfigHelperTokenDurationFormats(1),
          texts.getAdminConfigEntryVerificationTokenValidDurationTooltip(),
          texts.getAdminConfigEntryVerificationTokenLiveDurationTooltip(),
          texts.getAdminConfigEntryPasswordResetTokenValidDurationTooltip(),
          texts.getAdminConfigEntryPasswordResetTokenLiveDurationTooltip()
        }) {
      assertFalse(value.contains("{0}"));
      assertFalse(value.contains("\uFFFD"));
      assertTrue(value.contains("min"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void adminPasswordExplanationsUseTheSharedRuleAndContainNoUnexpandedPlaceholder(String language) {
    UiTexts texts = new UiTexts(language);
    String rule = texts.getBaseValidationPasswordInvalidMessage();
    for (String value :
        new String[] {
          texts.getAdminUsersPasswordHelperEdit(),
          texts.getAdminUsersPasswordHelperCreate(),
          texts.getAdminUsersPasswordValidationEdit(),
          texts.getAdminUsersPasswordValidationCreate()
        }) {
      assertTrue(value.contains(rule));
      assertFalse(value.contains("{0}"));
      assertFalse(value.contains("\uFFFD"));
    }
  }

  @Test
  void timeEntryUsesTheChosenLanguagesClockFormat() {
    assertEquals("00:00", new UiTexts("de").timeInput(java.time.LocalTime.MIDNIGHT));
    assertEquals("12:00 AM", new UiTexts("en").timeInput(java.time.LocalTime.MIDNIGHT));
    assertEquals("14:45", new UiTexts("de").timeInput(java.time.LocalTime.of(14, 45)));
    assertEquals("2:45 PM", new UiTexts("en").timeInput(java.time.LocalTime.of(14, 45)));
  }

  @Test
  void dateEntryMatchesTheExplicitUiLanguage() {
    var date = LocalDate.of(1990, 1, 1);
    assertEquals("01.01.1990", new UiTexts("de").dateInput(date));
    assertEquals("1/1/1990", new UiTexts("en").dateInput(date));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void cosmosPlaceholdersAndCopyrightTooltipUseTheSharedCatalogue(String language) {
    UiTexts texts = new UiTexts(language);
    for (String value :
        new String[] {
          texts.getAccountEmailPlaceholder(),
          texts.getAccountConfirmEmailPlaceholder(),
          texts.getAccountPasswordPlaceholder(),
          texts.getAccountConfirmPasswordPlaceholder(),
          texts.getAccountNewPasswordPlaceholder(),
          texts.getAccountFirstNamePlaceholder(),
          texts.getAccountLastNamePlaceholder(),
          texts.getAccountGenderPlaceholder(),
          texts.getAdminConfigEntryCopyrightText()
        }) {
      assertFalse(value.isBlank());
      assertFalse(value.contains("\uFFFD"));
      assertFalse(value.contains("account.form."));
    }
    String tooltip = texts.getAdminConfigEntryCopyrightTextTooltip();
    assertTrue(tooltip.contains("{year}"));
    assertTrue(tooltip.contains("{serviceName}"));
    assertFalse(
        tooltip.contains("'"), "MessageFormat quotes must not appear in the visible explanation.");
  }
}
