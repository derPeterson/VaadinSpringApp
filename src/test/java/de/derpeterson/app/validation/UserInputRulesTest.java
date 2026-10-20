package de.derpeterson.app.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.validator.EmailValidator;
import de.derpeterson.app.helper.ui.ValidationHelper;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserInputRulesTest {
  @Test
  void passwordAndBirthDateRulesRemainUnchanged() {
    assertEquals("^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$", UserInputRules.PASSWORD_REGEX);
    assertTrue(UserInputRules.isPasswordSecure(" Password! "));
    assertFalse(UserInputRules.isPasswordSecure("password!"));
    assertTrue(UserInputRules.isPasswordSecure(null, true));
    assertFalse(UserInputRules.isPasswordSecure(null, false));
    assertTrue(UserInputRules.isBirthDateValid(LocalDate.now().minusDays(1)));
    assertFalse(UserInputRules.isBirthDateValid(LocalDate.now()));
    assertFalse(UserInputRules.isBirthDateValid(null));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "\t",
        "Test",
        " Test ",
        "USER+tag@example.com",
        " User@example.com ",
        "invalid",
        "a@b.c",
        "a_b@some-domain.co"
      })
  void uiDelegatesPreserveExactlyThePureRules(String value) {
    assertEquals(
        UserInputRules.isRequiredTextValid(value), ValidationHelper.isRequiredTextValid(value));
    assertEquals(UserInputRules.isEmailValid(value), ValidationHelper.isEmailValid(value));
    // Vergleicht die zentrale Regel mit der bestehenden Flow-E-Mail-Regex; definiert keine zweite
    // Passwortregel.
    assertEquals(
        value != null && !value.isBlank() && value.trim().matches(EmailValidator.PATTERN),
        UserInputRules.isEmailValid(value));
    assertEquals(UserInputRules.isPasswordSecure(value), ValidationHelper.isPasswordSecure(value));
    assertEquals(UserInputRules.normalize(value), ValidationHelper.normalize(value));
  }

  @Test
  void shortEmailAddressesKeepTheExistingFlowPolicy() {
    String[] localParts = {"a", "A_1.+-", ".a.", "a..b", "ümlaut", "a b", "a@b", "a!b", "a\tb", ""};
    String[] domains = {
      "example.test",
      "EXAMPLE.TEST",
      "-b-.co",
      "b.12",
      "b.c",
      "b..co",
      ".b.co",
      "b.co.",
      "b.co.test",
      "b.ümlaut",
      "b@c.test",
      "b c.test",
      "b.\nco",
      "b",
      ""
    };
    for (String localPart : localParts) {
      for (String domain : domains) {
        String email = localPart + "@" + domain;
        boolean expected = email.matches(EmailValidator.PATTERN);
        assertEquals(expected, UserInputRules.isEmailValid(email), () -> "Address: " + email);
        assertEquals(expected, UserInputRules.isEmailValid(" \t" + email + "\r\n"));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {2048, 8192, 32768})
  void longDomainChainsAreValidatedWithoutStackOverflowOrExcessiveBacktracking(int segments) {
    String domain = "b.".repeat(segments);
    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          assertTrue(UserInputRules.isEmailValid("a@" + domain + "test"));
          assertFalse(UserInputRules.isEmailValid("a@" + domain + "t"));
          assertFalse(UserInputRules.isEmailValid("a@" + domain + ".test"));
          assertFalse(UserInputRules.isEmailValid("a@" + domain + "test x"));
          assertFalse(UserInputRules.isEmailValid("a@" + domain + "test@other.test"));
        });
  }

  @Test
  void longLocalPartsAndMissingSeparatorsKeepTheirPreviousMeaning() {
    String localPart = "a".repeat(100_000);
    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          assertTrue(UserInputRules.isEmailValid(localPart + "@example.test"));
          assertFalse(UserInputRules.isEmailValid(localPart));
          assertFalse(UserInputRules.isEmailValid(localPart + "!@example.test"));
          assertFalse(UserInputRules.isEmailValid("a@" + localPart));
        });
  }

  @Test
  void sharedPatternAlsoProtectsEmailFieldsAutomaticValidator() {
    EmailField field = new EmailField();
    field.setPattern(UserInputRules.EMAIL_REGEX);
    String domain = "b.".repeat(32_768);
    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          field.setValue("a@" + domain + "test");
          assertFalse(field.isInvalid());
          field.setValue("a@" + domain + "test x");
          assertTrue(field.isInvalid());
          field.setValue("a@" + domain + ".test");
          assertTrue(field.isInvalid());
        });
  }
}
