package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.validator.EmailValidator;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.service.UserService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationHelper {

    public static boolean validateRequiredInputs(List<? extends AbstractSinglePropertyField<? extends HasAllowedCharPattern, ? extends Serializable>> requiredInputs, MessageProperties messageProperties) {
        if (requiredInputs.stream().anyMatch(AbstractField::isEmpty)) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationRequiredMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            requiredInputs.stream().filter(AbstractField::isEmpty).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(true));
            requiredInputs.stream().filter(Predicate.not(AbstractField::isEmpty)).map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
            return false;
        } else {
            requiredInputs.stream().map(HasValidationProperties.class::cast).forEach(field -> field.setInvalid(false));
        }

        return true;
    }

    public static boolean validatePasswordSecureInputs(List<PasswordField> passwordFields, MessageProperties messageProperties) {
        if (passwordFields.stream().anyMatch(field -> !field.getValue().matches(UserEntity.PASSWORD_REGEX))) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationPasswordInvalidMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            passwordFields.forEach(field -> field.setInvalid(!field.getValue().matches(UserEntity.PASSWORD_REGEX)));

            return false;
        } else {
            passwordFields.forEach(field -> field.setInvalid(false));
        }
        return true;
    }

    public static boolean validateEmailValidInputs(List<EmailField> emailFieldsFields, MessageProperties messageProperties) {
        if (emailFieldsFields.stream().anyMatch(field -> !field.getValue().matches(EmailValidator.PATTERN))) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailInvalidMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            emailFieldsFields.forEach(field -> field.setInvalid(!field.getValue().matches(EmailValidator.PATTERN)));

            return false;
        } else {
            emailFieldsFields.forEach(field -> field.setInvalid(false));
        }
        return true;
    }

    public static boolean validatePasswordConfirmInputs(PasswordField input, PasswordField confirmInput, MessageProperties messageProperties) {
        if (!StringUtils.equals(input.getValue(), confirmInput.getValue())) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationPasswordConfirmMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            input.setInvalid(true);
            confirmInput.setInvalid(true);

            return false;
        } else {
            input.setInvalid(false);
            confirmInput.setInvalid(false);
        }

        return true;
    }

    public static boolean validateEmailConfirmInputs(EmailField input, EmailField confirmInput, MessageProperties messageProperties) {
        if (!StringUtils.equals(input.getValue(), confirmInput.getValue())) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailConfirmMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            input.setInvalid(true);
            confirmInput.setInvalid(true);

            return false;
        } else {
            input.setInvalid(false);
            confirmInput.setInvalid(false);
        }

        return true;
    }

    public static boolean validateEmailNotExistsInput(EmailField emailField, EmailField confirmEmailField, UserService userService, MessageProperties messageProperties) {
        if (userService.findByEmail(emailField.getValue()).isPresent()) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationEmailExistsMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            emailField.setInvalid(true);
            confirmEmailField.setInvalid(true);

            return false;
        } else {
            emailField.setInvalid(false);
            confirmEmailField.setInvalid(false);
        }

        return true;
    }

    public static boolean validateBirthDateBeforeInput(DatePicker datePicker, MessageProperties messageProperties) {
        if (datePicker.getValue().isAfter(LocalDate.now())) {
            NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle,
                    messageProperties::getBaseValidationBirthDatePastMessage,
                    -1, NotificationHelper.NotificationType.ERROR);

            datePicker.setInvalid(true);

            return false;
        } else {
            datePicker.setInvalid(false);
        }

        return true;
    }
}
