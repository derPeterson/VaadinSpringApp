package de.derpeterson.app.validation;

import de.derpeterson.app.model.EmailIdentity;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

/** Shared business rules; deliberately independent of Vaadin components and sessions. */
public final class UserInputRules {
    private UserInputRules() {
    }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$";
    // Same pattern as the previously used Flow 25.3 EmailValidator, not a new mail policy.
    private static final String EMAIL_REGEX = "^([a-zA-Z0-9_\\.\\-+])+@([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$";

    public static boolean isPasswordSecure(String password) {
        return StringUtils.isNotBlank(password) && password.trim().matches(PASSWORD_REGEX);
    }

    public static boolean isPasswordSecure(String password, boolean allowBlank) {
        return StringUtils.isBlank(password) ? allowBlank : isPasswordSecure(password);
    }

    public static boolean isEmailValid(String email) {
        return StringUtils.isNotBlank(email) && email.trim().matches(EMAIL_REGEX);
    }

    public static boolean isBirthDateValid(LocalDate birthDate) {
        return birthDate != null && birthDate.isBefore(LocalDate.now());
    }

    public static boolean isRequiredTextValid(String value) {
        return StringUtils.isNotBlank(value);
    }

    public static String normalize(String value) {
        return StringUtils.defaultString(EmailIdentity.canonicalize(value));
    }
}
