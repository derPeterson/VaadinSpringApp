package de.derpeterson.app.validation;

import com.vaadin.flow.data.validator.EmailValidator;
import de.derpeterson.app.helper.ui.ValidationHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UserInputRulesTest {
    @Test
    void extractedPatternsAreExactlyThePreviousRules() {
        assertEquals(EmailValidator.PATTERN, "^([a-zA-Z0-9_\\.\\-+])+@([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$");
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
    @ValueSource(strings = {" ", "\t", "Test", " Test ", "USER+tag@example.com", " User@example.com ", "invalid", "a@b.c", "a_b@some-domain.co"})
    void uiDelegatesPreserveExactlyThePureRules(String value) {
        assertEquals(UserInputRules.isRequiredTextValid(value), ValidationHelper.isRequiredTextValid(value));
        assertEquals(UserInputRules.isEmailValid(value), ValidationHelper.isEmailValid(value));
        assertEquals(value != null && !value.isBlank() && value.trim().matches(EmailValidator.PATTERN), UserInputRules.isEmailValid(value));
        assertEquals(UserInputRules.isPasswordSecure(value), ValidationHelper.isPasswordSecure(value));
        assertEquals(UserInputRules.normalize(value), ValidationHelper.normalize(value));
    }
}
