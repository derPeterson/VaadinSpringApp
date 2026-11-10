package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.shared.HasAllowedCharPattern;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.ValueContext;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.validation.UserInputRules;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Strings;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationHelper {

  /**
   * Zentrale Passwortregel: - mindestens 8 Zeichen - mindestens 1 Großbuchstabe - mindestens 1
   * Sonderzeichen
   */
  public static final String PASSWORD_REGEX = UserInputRules.PASSWORD_REGEX;

  public static boolean validateRequiredInputs(
      List<
              ? extends
                  AbstractSinglePropertyField<
                      ? extends HasAllowedCharPattern, ? extends Serializable>>
          requiredInputs,
      MessageProperties messageProperties) {
    if (requiredInputs.stream().anyMatch(ValidationHelper::isRequiredInputInvalid)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationRequiredMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      requiredInputs.stream()
          .filter(ValidationHelper::isRequiredInputInvalid)
          .map(HasValidationProperties.class::cast)
          .forEach(inputField -> inputField.setInvalid(true));

      requiredInputs.stream()
          .filter(inputField -> !isRequiredInputInvalid(inputField))
          .map(HasValidationProperties.class::cast)
          .forEach(inputField -> inputField.setInvalid(false));

      return false;
    }

    requiredInputs.stream()
        .map(HasValidationProperties.class::cast)
        .forEach(inputField -> inputField.setInvalid(false));

    return true;
  }

  private static boolean isRequiredInputInvalid(AbstractField<?, ?> inputField) {
    return inputField.getValue() instanceof String inputText
        ? !UserInputRules.isRequiredTextValid(inputText)
        : inputField.isEmpty();
  }

  public static boolean validatePasswordSecureInputs(
      List<PasswordField> passwordFields, MessageProperties messageProperties) {
    if (passwordFields.stream()
        .anyMatch(passwordField -> !isPasswordSecure(passwordField.getValue()))) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationPasswordInvalidMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      passwordFields.forEach(
          passwordField -> passwordField.setInvalid(!isPasswordSecure(passwordField.getValue())));
      return false;
    }

    passwordFields.forEach(passwordField -> passwordField.setInvalid(false));
    return true;
  }

  public static boolean validateEmailValidInputs(
      List<EmailField> emailFields, MessageProperties messageProperties) {
    if (emailFields.stream().anyMatch(emailField -> !isEmailValid(emailField.getValue()))) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationEmailInvalidMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      emailFields.forEach(
          emailField -> emailField.setInvalid(!isEmailValid(emailField.getValue())));
      return false;
    }

    emailFields.forEach(emailField -> emailField.setInvalid(false));
    return true;
  }

  public static boolean validatePasswordConfirmInputs(
      PasswordField passwordField,
      PasswordField confirmationPasswordField,
      MessageProperties messageProperties) {
    if (!Strings.CS.equals(passwordField.getValue(), confirmationPasswordField.getValue())) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationPasswordConfirmMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      passwordField.setInvalid(true);
      confirmationPasswordField.setInvalid(true);
      return false;
    }

    passwordField.setInvalid(false);
    confirmationPasswordField.setInvalid(false);
    return true;
  }

  public static boolean validateEmailConfirmInputs(
      EmailField emailField,
      EmailField confirmationEmailField,
      MessageProperties messageProperties) {
    if (!Strings.CS.equals(emailField.getValue(), confirmationEmailField.getValue())) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationEmailConfirmMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      emailField.setInvalid(true);
      confirmationEmailField.setInvalid(true);
      return false;
    }

    emailField.setInvalid(false);
    confirmationEmailField.setInvalid(false);
    return true;
  }

  public static boolean validateEmailNotExistsInput(
      EmailField emailField,
      EmailField confirmEmailField,
      UserService userService,
      MessageProperties messageProperties) {
    if (userService.findByEmail(normalize(emailField.getValue())).isPresent()) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationEmailExistsMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      emailField.setInvalid(true);
      confirmEmailField.setInvalid(true);
      return false;
    }

    emailField.setInvalid(false);
    confirmEmailField.setInvalid(false);
    return true;
  }

  public static boolean validateBirthDateBeforeInput(
      DatePicker datePicker, MessageProperties messageProperties) {
    // Ein früher gesetztes Fehlerflag unterscheidet nicht zwischen leerem und unlesbarem Datum.
    // Der native Validator prüft die aktuelle Eingabe auch dann, wenn ihr Modellwert null ist.
    if (datePicker
            .getDefaultValidator()
            .apply(datePicker.getValue(), new ValueContext(datePicker))
            .isError()
        || !isBirthDateValid(datePicker.getValue())) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseValidationBirthDatePastMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);

      datePicker.setInvalid(true);
      return false;
    }

    datePicker.setInvalid(false);
    return true;
  }

  public static boolean isPasswordSecure(String passwordText) {
    return UserInputRules.isPasswordSecure(passwordText);
  }

  public static boolean isPasswordSecure(String passwordText, boolean allowBlank) {
    return UserInputRules.isPasswordSecure(passwordText, allowBlank);
  }

  public static boolean isEmailValid(String emailAddress) {
    return UserInputRules.isEmailValid(emailAddress);
  }

  public static boolean isBirthDateValid(LocalDate birthDate) {
    return UserInputRules.isBirthDateValid(birthDate);
  }

  public static boolean isRequiredTextValid(String inputValue) {
    return UserInputRules.isRequiredTextValid(inputValue);
  }

  public static String normalize(String inputValue) {
    return UserInputRules.normalize(inputValue);
  }
}
