package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.validation.UserInputRules;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Strings;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationHelper {

    /**
     * Zentrale Passwortregel:
     * - mindestens 8 Zeichen
     * - mindestens 1 Großbuchstabe
     * - mindestens 1 Sonderzeichen
     */
    public static final String PASSWORD_REGEX = UserInputRules.PASSWORD_REGEX;

    public static boolean validateRequiredInputs(
            List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> requiredInputs,
            MessageProperties messageProperties
    ) {
        if (requiredInputs.stream().anyMatch(ValidationHelper::isRequiredInputInvalid)) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationRequiredMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            requiredInputs.stream()
                    .filter(ValidationHelper::isRequiredInputInvalid)
                    .map(HasValidationProperties.class::cast)
                    .forEach(field -> field.setInvalid(true));

            requiredInputs.stream()
                    .filter(field -> !isRequiredInputInvalid(field))
                    .map(HasValidationProperties.class::cast)
                    .forEach(field -> field.setInvalid(false));

            return false;
        }

        requiredInputs.stream()
                .map(HasValidationProperties.class::cast)
                .forEach(field -> field.setInvalid(false));

        return true;
    }

    private static boolean isRequiredInputInvalid(AbstractField<?, ?> field) {
        return field.getValue() instanceof String text ? !UserInputRules.isRequiredTextValid(text) : field.isEmpty();
    }

    public static boolean validatePasswordSecureInputs(List<PasswordField> passwordFields,
                                                       MessageProperties messageProperties) {
        if (passwordFields.stream().anyMatch(field -> !isPasswordSecure(field.getValue()))) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationPasswordInvalidMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            passwordFields.forEach(field -> field.setInvalid(!isPasswordSecure(field.getValue())));
            return false;
        }

        passwordFields.forEach(field -> field.setInvalid(false));
        return true;
    }

    public static boolean validateEmailValidInputs(List<EmailField> emailFields,
                                                   MessageProperties messageProperties) {
        if (emailFields.stream().anyMatch(field -> !isEmailValid(field.getValue()))) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailInvalidMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            emailFields.forEach(field -> field.setInvalid(!isEmailValid(field.getValue())));
            return false;
        }

        emailFields.forEach(field -> field.setInvalid(false));
        return true;
    }

    public static boolean validatePasswordConfirmInputs(PasswordField input,
                                                        PasswordField confirmInput,
                                                        MessageProperties messageProperties) {
        if (!Strings.CS.equals(input.getValue(), confirmInput.getValue())) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationPasswordConfirmMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            input.setInvalid(true);
            confirmInput.setInvalid(true);
            return false;
        }

        input.setInvalid(false);
        confirmInput.setInvalid(false);
        return true;
    }

    public static boolean validateEmailConfirmInputs(EmailField input,
                                                     EmailField confirmInput,
                                                     MessageProperties messageProperties) {
        if (!Strings.CS.equals(input.getValue(), confirmInput.getValue())) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailConfirmMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            input.setInvalid(true);
            confirmInput.setInvalid(true);
            return false;
        }

        input.setInvalid(false);
        confirmInput.setInvalid(false);
        return true;
    }

    public static boolean validateEmailNotExistsInput(EmailField emailField,
                                                      EmailField confirmEmailField,
                                                      UserService userService,
                                                      MessageProperties messageProperties) {
        if (userService.findByEmail(normalize(emailField.getValue())).isPresent()) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailExistsMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            emailField.setInvalid(true);
            confirmEmailField.setInvalid(true);
            return false;
        }

        emailField.setInvalid(false);
        confirmEmailField.setInvalid(false);
        return true;
    }

    public static boolean validateBirthDateBeforeInput(DatePicker datePicker,
                                                       MessageProperties messageProperties) {
        if (!isBirthDateValid(datePicker.getValue())) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationBirthDatePastMessage,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );

            datePicker.setInvalid(true);
            return false;
        }

        datePicker.setInvalid(false);
        return true;
    }

    public static boolean isPasswordSecure(String password) {
        return UserInputRules.isPasswordSecure(password);
    }

    public static boolean isPasswordSecure(String password, boolean allowBlank) {
        return UserInputRules.isPasswordSecure(password, allowBlank);
    }

    public static boolean isEmailValid(String email) {
        return UserInputRules.isEmailValid(email);
    }

    public static boolean isBirthDateValid(LocalDate birthDate) {
        return UserInputRules.isBirthDateValid(birthDate);
    }

    public static boolean isRequiredTextValid(String value) {
        return UserInputRules.isRequiredTextValid(value);
    }

    public static String normalize(String value) {
        return UserInputRules.normalize(value);
    }
}
