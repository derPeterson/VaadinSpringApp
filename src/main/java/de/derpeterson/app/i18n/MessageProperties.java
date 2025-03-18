package de.derpeterson.app.i18n;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class MessageProperties {

    private final CustomI18NProvider i18nProvider;

    public MessageProperties(CustomI18NProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    // Login View
    public String getLoginTitle() {
        return i18nProvider.getTranslation("loginView.title");
    }

    public String getLoginCreateAccountQuestion() {
        return i18nProvider.getTranslation("loginView.create_account_question");
    }

    public String getLoginCreateAccountLink() {
        return i18nProvider.getTranslation("loginView.create_account_link");
    }

    public String getLoginEmailField() {
        return i18nProvider.getTranslation("loginView.email_field");
    }

    public String getLoginPasswordField() {
        return i18nProvider.getTranslation("loginView.password_field");
    }

    public String getLoginRememberMeCheckbox() {
        return i18nProvider.getTranslation("loginView.remember_me_checkbox");
    }

    public String getLoginForgotPasswordLink() {
        return i18nProvider.getTranslation("loginView.forgot_password_link");
    }

    public String getLoginFailedMessage() {
        return i18nProvider.getTranslation("loginView.login.failed_message");
    }

    public String getLoginSuccessMessage() {
        return i18nProvider.getTranslation("loginView.login.success_message");
    }

    // Forgot Password View
    public String getForgotPasswordTitle() {
        return i18nProvider.getTranslation("forgotPasswordView.title");
    }

    public String getForgotPasswordText() {
        return i18nProvider.getTranslation("forgotPasswordView.text");
    }

    public String getForgotPasswordEmailField() {
        return i18nProvider.getTranslation("forgotPasswordView.email_field");
    }

    public String getForgotPasswordSuccessMessage() {
        return i18nProvider.getTranslation("forgotPasswordView.success_message");
    }

    // Email Subjects
    public String getEmailResetPasswordSubject() {
        return i18nProvider.getTranslation("email.reset_password.subject");
    }

    public String getEmailVerificationSubject() {
        return i18nProvider.getTranslation("email.verification.subject");
    }

    // Registration View
    public String getRegistrationTitle() {
        return i18nProvider.getTranslation("registrationView.title");
    }

    public String getRegistrationLoginQuestion() {
        return i18nProvider.getTranslation("registrationView.login_question");
    }

    public String getRegistrationLoginLink() {
        return i18nProvider.getTranslation("registrationView.login_link");
    }

    public String getRegistrationFirstNameField() {
        return i18nProvider.getTranslation("registrationView.first_name_field");
    }

    public String getRegistrationLastNameField() {
        return i18nProvider.getTranslation("registrationView.last_name_field");
    }

    public String getRegistrationEmailField() {
        return i18nProvider.getTranslation("registrationView.email_field");
    }

    public String getRegistrationPasswordField() {
        return i18nProvider.getTranslation("registrationView.password_field");
    }

    public String getRegistrationConfirmField() {
        return i18nProvider.getTranslation("registrationView.confirm_field");
    }

    public String getRegistrationGenderCombobox() {
        return i18nProvider.getTranslation("registrationView.gender_combobox");
    }

    public String getRegistrationBirthDateField() {
        return i18nProvider.getTranslation("registrationView.birth_date_field");
    }

    public String getRegistrationSuccessMessage(String email) {
        return MessageFormat.format(i18nProvider.getTranslation("registrationView.registration.success_message"), email);
    }

    // Verification View
    public String getVerificationSuccessTitle() {
        return i18nProvider.getTranslation("verificationView.success.title");
    }

    public String getVerificationSuccessText() {
        return i18nProvider.getTranslation("verificationView.success.text");
    }

    public String getVerificationExpiredTitle() {
        return i18nProvider.getTranslation("verificationView.expired.title");
    }

    public String getVerificationExpiredText1() {
        return i18nProvider.getTranslation("verificationView.expired.text1");
    }

    public String getVerificationExpiredText2() {
        return i18nProvider.getTranslation("verificationView.expired.text2");
    }

    public String getVerificationExpiredSuccessMessage() {
        return i18nProvider.getTranslation("verificationView.expired.success_message");
    }

    public String getVerificationNotFoundTitle() {
        return i18nProvider.getTranslation("verificationView.not_found.title");
    }

    public String getVerificationNotFoundText1() {
        return i18nProvider.getTranslation("verificationView.not_found.text1");
    }

    public String getVerificationNotFoundText2() {
        return i18nProvider.getTranslation("verificationView.not_found.text2");
    }

    public String getVerificationNotFoundSuccessMessage() {
        return i18nProvider.getTranslation("verificationView.not_found.success_message");
    }

    public String getVerificationEmailField() {
        return i18nProvider.getTranslation("verificationView.email_field");
    }

    // Reset Password View
    public String getResetPasswordInvalidTitle() {
        return i18nProvider.getTranslation("resetPasswordView.invalid.title");
    }

    public String getResetPasswordInvalidText() {
        return i18nProvider.getTranslation("resetPasswordView.invalid.text");
    }

    public String getResetPasswordTitle() {
        return i18nProvider.getTranslation("resetPasswordView.reset.title");
    }

    public String getResetPasswordNewPasswordField() {
        return i18nProvider.getTranslation("resetPasswordView.reset.new_password_field");
    }

    public String getResetPasswordConfirmField() {
        return i18nProvider.getTranslation("resetPasswordView.reset.confirm_field");
    }

    public String getResetPasswordSuccessMessage() {
        return i18nProvider.getTranslation("resetPasswordView.reset.success_message");
    }

    // Base Buttons
    public String getBaseApplyButton() {
        return i18nProvider.getTranslation("base.apply_button");
    }

    public String getBaseLoginButton() {
        return i18nProvider.getTranslation("base.login_button");
    }

    public String getBaseLogoutButton() {
        return i18nProvider.getTranslation("base.logout_button");
    }

    public String getBaseHomeButton() {
        return i18nProvider.getTranslation("base.home_button");
    }

    public String getBaseSendButton() {
        return i18nProvider.getTranslation("base.send_button");
    }

    public String getBaseResendButton() {
        return i18nProvider.getTranslation("base.resend_button");
    }

    public String getBaseRegistrationButton() {
        return i18nProvider.getTranslation("base.registration_button");
    }

    public String getBaseResetButton() {
        return i18nProvider.getTranslation("base.reset_button");
    }

    public String getBaseManageAccountButton() {
        return i18nProvider.getTranslation("base.manage_account_button");
    }

    // Status Messages
    public String getBaseSuccessTitle() {
        return i18nProvider.getTranslation("base.success.title");
    }

    public String getBaseFailedTitle() {
        return i18nProvider.getTranslation("base.failed.title");
    }

    public String getBaseFailedMessage() {
        return i18nProvider.getTranslation("base.failed.message");
    }

    public String getBaseOnlineText() {
        return i18nProvider.getTranslation("base.online_text");
    }

    public String getBaseOfflineText() {
        return i18nProvider.getTranslation("base.offline_text");
    }

    // Validation Messages
    public String getBaseValidationRequiredMessage() {
        return i18nProvider.getTranslation("base.validation.required_message");
    }

    public String getBaseValidationEmailInvalidMessage() {
        return i18nProvider.getTranslation("base.validation.email_invalid_message");
    }

    public String getBaseValidationPasswordInvalidMessage() {
        return i18nProvider.getTranslation("base.validation.password_invalid_message");
    }

    public String getBaseValidationPasswordConfirmMessage() {
        return i18nProvider.getTranslation("base.validation.password_confirm_message");
    }

    public String getBaseValidationEmailExistsMessage() {
        return i18nProvider.getTranslation("base.validation.email_exists_message");
    }

    public String getBaseValidationEmailConfirmMessage() {
        return i18nProvider.getTranslation("base.validation.email_confirm_message");
    }

    public String getBaseValidationBirthDatePastMessage() {
        return i18nProvider.getTranslation("base.validation.birth_date_past_message");
    }

    // User Status
    public String getUserStatusOffline() {
        return i18nProvider.getTranslation("userStatus.offline");
    }

    public String getUserStatusAbsent() {
        return i18nProvider.getTranslation("userStatus.absent");
    }

    public String getUserStatusEmployed() {
        return i18nProvider.getTranslation("userStatus.employed");
    }

    public String getUserStatusAvailable() {
        return i18nProvider.getTranslation("userStatus.available");
    }

    public String getTranslation(String textKey) {
        return i18nProvider.getTranslation(textKey);
    }
}