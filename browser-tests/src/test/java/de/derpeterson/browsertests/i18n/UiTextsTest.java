package de.derpeterson.browsertests.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UiTextsTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void durationNumbersUseTheTestLanguageInsteadOfTheOperatingSystemLocale(String language) {
    UiTexts uiTexts = new UiTexts(language);
    String expectedGroupedNumberText = language.equals("de") ? "172.800" : "172,800";
    String expectedDecimalNumberText = language.equals("de") ? "1,5" : "1.5";

    assertTrue(uiTexts.getAdminConfigDurationSeconds(172800).contains(expectedGroupedNumberText));
    assertTrue(
        uiTexts
            .getAdminConfigDurationMinutes(new java.math.BigDecimal("1.5"))
            .contains(expectedDecimalNumberText));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requiredKeysNeverFallBackToAnotherLanguageOrTheKeyItself(String language) {
    UiTexts uiTexts = new UiTexts(language);

    assertThrows(
        IllegalArgumentException.class, () -> uiTexts.getTranslation("missing.browser.test.key"));
    assertEquals(language, uiTexts.language());
  }

  @Test
  void unsupportedLanguageIsRejectedInsteadOfSilentlyUsingEnglish() {
    assertThrows(IllegalArgumentException.class, () -> new UiTexts("fr"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void formattedMessageKeepsItsRecipientAndSubstitutesItsPlaceholder(String language) {
    String registrationMessageText =
        new UiTexts(language).getRegistrationSuccessMessage("test@example.test");

    assertTrue(registrationMessageText.contains("test@example.test"));
    assertFalse(registrationMessageText.contains("{0}"));
    assertFalse(
        registrationMessageText.contains("\uFFFD"),
        "Original translation must not contain decoding replacement characters.");
    assertNotEquals("registrationView.registration.success_message", registrationMessageText);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void tokenMinimumMessagesAndExistingTooltipGettersAreFormattedInBothLanguages(String language) {
    UiTexts uiTexts = new UiTexts(language);
    for (String value :
        new String[] {
          uiTexts.getAdminConfigValidationDurationMinimum(1),
          uiTexts.getAdminConfigValidationTokenDuration(2),
          uiTexts.getAdminConfigEntryVerificationTokenValidDurationTooltip(),
          uiTexts.getAdminConfigEntryVerificationTokenLiveDurationTooltip(),
          uiTexts.getAdminConfigEntryPasswordResetTokenValidDurationTooltip(),
          uiTexts.getAdminConfigEntryPasswordResetTokenLiveDurationTooltip()
        }) {
      assertFalse(value.contains("{0}"));
      assertFalse(value.contains("\uFFFD"));
      assertTrue(value.toLowerCase(Locale.ROOT).contains("min"), value);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void adminPasswordValidationUsesTheFullRuleAndEditReusesTheApprovedCreateHint(String language) {
    UiTexts uiTexts = new UiTexts(language);
    String passwordRuleText = uiTexts.getBaseValidationPasswordInvalidMessage();
    // Der freigegebene Hinweis ist kürzer als der Validierungsfehler. Beim Bearbeiten
    // folgt nach dem Leer-lassen-Hinweis exakt dieselbe neue Passwortregel wie beim Anlegen.
    String createPasswordHintText = uiTexts.getAdminUsersPasswordHelperCreate();

    assertFalse(createPasswordHintText.isBlank());
    assertTrue(uiTexts.getAdminUsersPasswordHelperEdit().endsWith(createPasswordHintText));
    assertNotEquals(createPasswordHintText, uiTexts.getAdminUsersPasswordHelperEdit());
    assertTrue(uiTexts.getAdminUsersPasswordValidationEdit().contains(passwordRuleText));
    assertTrue(uiTexts.getAdminUsersPasswordValidationCreate().contains(passwordRuleText));
    for (String value :
        new String[] {
          uiTexts.getAdminUsersPasswordHelperEdit(),
          uiTexts.getAdminUsersPasswordHelperCreate(),
          uiTexts.getAdminUsersPasswordValidationEdit(),
          uiTexts.getAdminUsersPasswordValidationCreate()
        }) {
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
    LocalDate date = LocalDate.of(1990, 1, 1);

    assertEquals("01.01.1990", new UiTexts("de").dateInput(date));
    assertEquals("1/1/1990", new UiTexts("en").dateInput(date));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void cosmosPlaceholdersAndCopyrightTooltipUseTheSharedCatalogue(String language) {
    UiTexts uiTexts = new UiTexts(language);
    for (String value :
        new String[] {
          uiTexts.getAccountEmailPlaceholder(),
          uiTexts.getAccountConfirmEmailPlaceholder(),
          uiTexts.getAccountPasswordPlaceholder(),
          uiTexts.getAccountConfirmPasswordPlaceholder(),
          uiTexts.getAccountNewPasswordPlaceholder(),
          uiTexts.getAccountFirstNamePlaceholder(),
          uiTexts.getAccountLastNamePlaceholder(),
          uiTexts.getAccountGenderPlaceholder(),
          uiTexts.getAdminConfigEntryCopyrightText()
        }) {
      assertFalse(value.isBlank());
      assertFalse(value.contains("\uFFFD"));
      assertFalse(value.contains("account.form."));
    }
    String tooltip = uiTexts.getAdminConfigEntryCopyrightTextTooltip();

    assertTrue(tooltip.contains("{year}"));
    assertTrue(tooltip.contains("{serviceName}"));
    assertFalse(
        tooltip.contains("'"), "MessageFormat quotes must not appear in the visible explanation.");
  }
}
