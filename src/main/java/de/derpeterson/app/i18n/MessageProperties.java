package de.derpeterson.app.i18n;

import de.derpeterson.app.model.enums.ConfigEntry;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class MessageProperties {

    private final CustomI18NProvider i18nProvider;

    public MessageProperties(CustomI18NProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public String getTranslation(String key) {
        return i18nProvider.getTranslation(key);
    }

    public String getFormattedTranslation(String key, Object... args) {
        return MessageFormat.format(getTranslation(key), args);
    }

    // Login View
    public String getLoginTitle() {
        return getTranslation("loginView.title");
    }

    public String getLoginCreateAccountQuestion() {
        return getTranslation("loginView.create_account_question");
    }

    public String getLoginCreateAccountLink() {
        return getTranslation("loginView.create_account_link");
    }

    public String getLoginEmailField() {
        return getTranslation("loginView.email_field");
    }

    public String getLoginPasswordField() {
        return getTranslation("loginView.password_field");
    }

    public String getLoginRememberMeCheckbox() {
        return getTranslation("loginView.remember_me_checkbox");
    }

    public String getLoginForgotPasswordLink() {
        return getTranslation("loginView.forgot_password_link");
    }

    public String getLoginFailedMessage() {
        return getTranslation("loginView.login.failed_message");
    }

    public String getLoginSuccessMessage() {
        return getTranslation("loginView.login.success_message");
    }

    // Forgot Password View
    public String getForgotPasswordTitle() {
        return getTranslation("forgotPasswordView.title");
    }

    public String getForgotPasswordText() {
        return getTranslation("forgotPasswordView.text");
    }

    public String getForgotPasswordEmailField() {
        return getTranslation("forgotPasswordView.email_field");
    }

    public String getForgotPasswordSuccessMessage() {
        return getTranslation("forgotPasswordView.success_message");
    }

    // Email Subjects
    public String getEmailResetPasswordSubject() {
        return getTranslation("email.reset_password.subject");
    }

    public String getEmailVerificationSubject() {
        return getTranslation("email.verification.subject");
    }

    // Registration View
    public String getRegistrationTitle() {
        return getTranslation("registrationView.title");
    }

    public String getRegistrationLoginQuestion() {
        return getTranslation("registrationView.login_question");
    }

    public String getRegistrationLoginLink() {
        return getTranslation("registrationView.login_link");
    }

    public String getRegistrationFirstNameField() {
        return getTranslation("registrationView.first_name_field");
    }

    public String getRegistrationLastNameField() {
        return getTranslation("registrationView.last_name_field");
    }

    public String getRegistrationEmailField() {
        return getTranslation("registrationView.email_field");
    }

    public String getRegistrationPasswordField() {
        return getTranslation("registrationView.password_field");
    }

    public String getRegistrationConfirmField() {
        return getTranslation("registrationView.confirm_field");
    }

    public String getRegistrationGenderCombobox() {
        return getTranslation("registrationView.gender_combobox");
    }

    public String getRegistrationBirthDateField() {
        return getTranslation("registrationView.birth_date_field");
    }

    public String getRegistrationSuccessMessage(String email) {
        return getFormattedTranslation("registrationView.registration.success_message", email);
    }

    // Verification View
    public String getVerificationSuccessTitle() {
        return getTranslation("verificationView.success.title");
    }

    public String getVerificationSuccessText() {
        return getTranslation("verificationView.success.text");
    }

    public String getVerificationExpiredTitle() {
        return getTranslation("verificationView.expired.title");
    }

    public String getVerificationExpiredText1() {
        return getTranslation("verificationView.expired.text1");
    }

    public String getVerificationExpiredText2() {
        return getTranslation("verificationView.expired.text2");
    }

    public String getVerificationExpiredSuccessMessage() {
        return getTranslation("verificationView.expired.success_message");
    }

    public String getVerificationNotFoundTitle() {
        return getTranslation("verificationView.not_found.title");
    }

    public String getVerificationNotFoundText1() {
        return getTranslation("verificationView.not_found.text1");
    }

    public String getVerificationNotFoundText2() {
        return getTranslation("verificationView.not_found.text2");
    }

    public String getVerificationNotFoundSuccessMessage() {
        return getTranslation("verificationView.not_found.success_message");
    }

    public String getVerificationEmailField() {
        return getTranslation("verificationView.email_field");
    }

    // Reset Password View
    public String getResetPasswordInvalidTitle() {
        return getTranslation("resetPasswordView.invalid.title");
    }

    public String getResetPasswordInvalidText() {
        return getTranslation("resetPasswordView.invalid.text");
    }

    public String getResetPasswordTitle() {
        return getTranslation("resetPasswordView.reset.title");
    }

    public String getResetPasswordNewPasswordField() {
        return getTranslation("resetPasswordView.reset.new_password_field");
    }

    public String getResetPasswordConfirmField() {
        return getTranslation("resetPasswordView.reset.confirm_field");
    }

    public String getResetPasswordSuccessMessage() {
        return getTranslation("resetPasswordView.reset.success_message");
    }

    // Base Buttons
    public String getBaseApplyButton() {
        return getTranslation("base.apply_button");
    }

    public String getBaseLoginButton() {
        return getTranslation("base.login_button");
    }

    public String getBaseLogoutButton() {
        return getTranslation("base.logout_button");
    }

    public String getBaseHomeButton() {
        return getTranslation("base.home_button");
    }

    public String getBaseSendButton() {
        return getTranslation("base.send_button");
    }

    public String getBaseResendButton() {
        return getTranslation("base.resend_button");
    }

    public String getBaseRegistrationButton() {
        return getTranslation("base.registration_button");
    }

    public String getBaseResetButton() {
        return getTranslation("base.reset_button");
    }

    public String getBaseManageAccountButton() {
        return getTranslation("base.manage_account_button");
    }

    public String getBaseManageApplicationButton() {
        return getTranslation("base.manage_application_button");
    }

    // Base Status / Errors
    public String getBaseSuccessTitle() {
        return getTranslation("base.success.title");
    }

    public String getBaseFailedTitle() {
        return getTranslation("base.failed.title");
    }

    public String getBaseFailedMessage() {
        return getTranslation("base.failed.message");
    }

    public String getBaseOnlineText() {
        return getTranslation("base.online_text");
    }

    public String getBaseOfflineText() {
        return getTranslation("base.offline_text");
    }

    public String getBaseErrorAdminAccessDenied() {
        return getTranslation("base.error.admin_access_denied");
    }

    public String getBaseErrorAccessDenied() {
        return getTranslation("base.error.access_denied");
    }

    public String getBaseErrorPathNotFound() {
        return getTranslation("base.error.path_not_found");
    }

    // Validation Messages
    public String getBaseValidationRequiredMessage() {
        return getTranslation("base.validation.required_message");
    }

    public String getBaseValidationEmailInvalidMessage() {
        return getTranslation("base.validation.email_invalid_message");
    }

    public String getBaseValidationPasswordInvalidMessage() {
        return getTranslation("base.validation.password_invalid_message");
    }

    public String getBaseValidationPasswordConfirmMessage() {
        return getTranslation("base.validation.password_confirm_message");
    }

    public String getBaseValidationEmailExistsMessage() {
        return getTranslation("base.validation.email_exists_message");
    }

    public String getBaseValidationEmailConfirmMessage() {
        return getTranslation("base.validation.email_confirm_message");
    }

    public String getBaseValidationBirthDatePastMessage() {
        return getTranslation("base.validation.birth_date_past_message");
    }

    // User Status
    public String getUserStatusOffline() {
        return getTranslation("userStatus.offline");
    }

    public String getUserStatusOfflineDescription() {
        return getTranslation("userStatus.offline.description");
    }

    public String getUserStatusAbsent() {
        return getTranslation("userStatus.absent");
    }

    public String getUserStatusAbsentDescription() {
        return getTranslation("userStatus.absent.description");
    }

    public String getUserStatusEmployed() {
        return getTranslation("userStatus.employed");
    }

    public String getUserStatusEmployedDescription() {
        return getTranslation("userStatus.employed.description");
    }

    public String getUserStatusAvailable() {
        return getTranslation("userStatus.available");
    }

    public String getUserStatusAvailableDescription() {
        return getTranslation("userStatus.available.description");
    }

    // Admin View
    public String getAdminHeaderTitle() {
        return getTranslation("adminView.header.title");
    }

    public String getAdminNavDashboard() {
        return getTranslation("adminView.nav.dashboard");
    }

    public String getAdminNavConfiguration() {
        return getTranslation("adminView.nav.configuration");
    }

    public String getAdminNavMailTest() {
        return getTranslation("adminView.nav.mail_test");
    }

    public String getAdminConfigGroupGeneral() {
        return getTranslation("adminView.config.group.general");
    }

    public String getAdminConfigGroupSecurity() {
        return getTranslation("adminView.config.group.security");
    }

    public String getAdminConfigGroupTokens() {
        return getTranslation("adminView.config.group.tokens");
    }

    public String getAdminConfigGroupUserStatus() {
        return getTranslation("adminView.config.group.user_status");
    }

    public String getAdminConfigGroupEmailSender() {
        return getTranslation("adminView.config.group.email_sender");
    }

    public String getAdminConfigGroupSmtp() {
        return getTranslation("adminView.config.group.smtp");
    }

    public String getAdminConfigGroupQueue() {
        return getTranslation("adminView.config.group.queue");
    }

    public String getAdminDashboardTitle() {
        return getTranslation("adminView.dashboard.title");
    }

    public String getAdminDashboardDescription() {
        return getTranslation("adminView.dashboard.description");
    }

    public String getAdminDashboardWelcome() {
        return getTranslation("adminView.dashboard.welcome");
    }

    public String getAdminDashboardRoles() {
        return getTranslation("adminView.dashboard.roles");
    }

    public String getAdminDashboardNotAuthenticated() {
        return getTranslation("adminView.dashboard.not_authenticated");
    }

    public String getAdminMailTitle() {
        return getTranslation("adminView.mail.title");
    }

    public String getAdminMailDescription() {
        return getTranslation("adminView.mail.description");
    }

    public String getAdminMailSend() {
        return getTranslation("adminView.mail.send");
    }

    public String getAdminMailRecipient() {
        return getTranslation("adminView.mail.recipient");
    }

    public String getAdminMailSubject() {
        return getTranslation("adminView.mail.subject");
    }

    public String getAdminMailBody() {
        return getTranslation("adminView.mail.body");
    }

    public String getAdminMailDefaultSubject() {
        return getTranslation("adminView.mail.default_subject");
    }

    public String getAdminMailDefaultBody() {
        return getTranslation("adminView.mail.default_body");
    }

    public String getAdminMailValidationRecipient() {
        return getTranslation("adminView.mail.validation.recipient");
    }

    public String getAdminMailValidationSubject() {
        return getTranslation("adminView.mail.validation.subject");
    }

    public String getAdminMailValidationBody() {
        return getTranslation("adminView.mail.validation.body");
    }

    public String getAdminMailSuccess() {
        return getTranslation("adminView.mail.success");
    }

    public String getAdminMailError() {
        return getTranslation("adminView.mail.error");
    }

    public String getAdminConfigTitle() {
        return getTranslation("adminView.config.title");
    }

    public String getAdminConfigDescription() {
        return getTranslation("adminView.config.description");
    }

    public String getAdminConfigSave() {
        return getTranslation("adminView.config.save");
    }

    public String getAdminConfigReload() {
        return getTranslation("adminView.config.reload");
    }

    public String getAdminConfigDefault() {
        return getTranslation("adminView.config.default");
    }

    public String getAdminConfigSaveSuccess() {
        return getTranslation("adminView.config.save_success");
    }

    public String getAdminConfigReloadSuccess() {
        return getTranslation("adminView.config.reload_success");
    }

    public String getAdminConfigEntryServiceName() {
        return getTranslation("adminView.config.entry.service_name");
    }

    public String getAdminConfigEntryBaseUrl() {
        return getTranslation("adminView.config.entry.base_url");
    }

    public String getAdminConfigEntryRememberMeDuration() {
        return getTranslation("adminView.config.entry.remember_me_duration");
    }

    public String getAdminConfigEntryRememberMeSecretKey() {
        return getTranslation("adminView.config.entry.remember_me_secret_key");
    }

    public String getAdminConfigEntryUserAutoAbsentTimeout() {
        return getTranslation("adminView.config.entry.user_auto_absent_timeout");
    }

    public String getAdminConfigEntryLoginAttemptsLimit() {
        return getTranslation("adminView.config.entry.login_attempts_limit");
    }

    public String getAdminConfigEntryMaxSessionsPerUser() {
        return getTranslation("adminView.config.entry.max_sessions_per_user");
    }

    public String getAdminConfigEntryMaintenanceMode() {
        return getTranslation("adminView.config.entry.maintenance_mode");
    }

    public String getAdminConfigEntryEmailFrom() {
        return getTranslation("adminView.config.entry.email_from");
    }

    public String getAdminConfigEntryEmailAdmin() {
        return getTranslation("adminView.config.entry.email_admin");
    }

    public String getAdminConfigEntryVerificationTokenValidDuration() {
        return getTranslation("adminView.config.entry.verification_token_valid_duration");
    }

    public String getAdminConfigEntryVerificationTokenLiveDuration() {
        return getTranslation("adminView.config.entry.verification_token_live_duration");
    }

    public String getAdminConfigEntryPasswordResetTokenValidDuration() {
        return getTranslation("adminView.config.entry.password_reset_token_valid_duration");
    }

    public String getAdminConfigEntryPasswordResetTokenLiveDuration() {
        return getTranslation("adminView.config.entry.password_reset_token_live_duration");
    }

    public String getAdminConfigEntryMailHost() {
        return getTranslation("adminView.config.entry.mail_host");
    }

    public String getAdminConfigEntryMailPort() {
        return getTranslation("adminView.config.entry.mail_port");
    }

    public String getAdminConfigEntryMailUsername() {
        return getTranslation("adminView.config.entry.mail_username");
    }

    public String getAdminConfigEntryMailPassword() {
        return getTranslation("adminView.config.entry.mail_password");
    }

    public String getAdminConfigEntryMailSmtpAuth() {
        return getTranslation("adminView.config.entry.mail_smtp_auth");
    }

    public String getAdminConfigEntryMailSmtpStarttlsEnable() {
        return getTranslation("adminView.config.entry.mail_smtp_starttls_enable");
    }

    public String getAdminConfigEntryMailSmtpSslEnable() {
        return getTranslation("adminView.config.entry.mail_smtp_ssl_enable");
    }

    public String getAdminConfigEntryMailSmtpSslTrust() {
        return getTranslation("adminView.config.entry.mail_smtp_ssl_trust");
    }

    public String getAdminConfigEntryMailSmtpSocketfactoryClass() {
        return getTranslation("adminView.config.entry.mail_smtp_socketfactory_class");
    }

    public String getAdminConfigEntryMailDebug() {
        return getTranslation("adminView.config.entry.mail_debug");
    }

    public String getAdminConfigEntryEmailQueuePoolSize() {
        return getTranslation("adminView.config.entry.email_queue_pool_size");
    }

    public String getAdminConfigEntryEmailQueueCapacity() {
        return getTranslation("adminView.config.entry.email_queue_capacity");
    }

    public String getAdminConfigEntryEmailQueueMaxRetry() {
        return getTranslation("adminView.config.entry.email_queue_max_retry");
    }

    public String getAdminConfigEntryEmailQueueSentLiveDuration() {
        return getTranslation("adminView.config.entry.email_queue_sent_live_duration");
    }

    public String getAdminConfigEntryLabel(ConfigEntry configEntry) {
        return switch (configEntry) {
            case SERVICE_NAME -> getAdminConfigEntryServiceName();
            case BASE_URL -> getAdminConfigEntryBaseUrl();

            case REMEMBER_ME_DURATION -> getAdminConfigEntryRememberMeDuration();
            case REMEMBER_ME_SECRET_KEY -> getAdminConfigEntryRememberMeSecretKey();
            case USER_AUTO_ABSENT_TIMEOUT -> getAdminConfigEntryUserAutoAbsentTimeout();
            case LOGIN_ATTEMPTS_LIMIT -> getAdminConfigEntryLoginAttemptsLimit();
            case MAX_SESSIONS_PER_USER -> getAdminConfigEntryMaxSessionsPerUser();
            case MAINTENANCE_MODE -> getAdminConfigEntryMaintenanceMode();

            case EMAIL_FROM -> getAdminConfigEntryEmailFrom();
            case EMAIL_ADMIN -> getAdminConfigEntryEmailAdmin();

            case VERIFICATION_TOKEN_VALID_DURATION -> getAdminConfigEntryVerificationTokenValidDuration();
            case VERIFICATION_TOKEN_LIVE_DURATION -> getAdminConfigEntryVerificationTokenLiveDuration();
            case PASSWORD_RESET_TOKEN_VALID_DURATION -> getAdminConfigEntryPasswordResetTokenValidDuration();
            case PASSWORD_RESET_TOKEN_LIVE_DURATION -> getAdminConfigEntryPasswordResetTokenLiveDuration();

            case MAIL_HOST -> getAdminConfigEntryMailHost();
            case MAIL_PORT -> getAdminConfigEntryMailPort();
            case MAIL_USERNAME -> getAdminConfigEntryMailUsername();
            case MAIL_PASSWORD -> getAdminConfigEntryMailPassword();
            case MAIL_SMTP_AUTH -> getAdminConfigEntryMailSmtpAuth();
            case MAIL_SMTP_STARTTLS_ENABLE -> getAdminConfigEntryMailSmtpStarttlsEnable();
            case MAIL_SMTP_SSL_ENABLE -> getAdminConfigEntryMailSmtpSslEnable();
            case MAIL_SMTP_SSL_TRUST -> getAdminConfigEntryMailSmtpSslTrust();
            case MAIL_SMTP_SOCKETFACTORY_CLASS -> getAdminConfigEntryMailSmtpSocketfactoryClass();
            case MAIL_DEBUG -> getAdminConfigEntryMailDebug();

            case EMAIL_QUEUE_POOL_SIZE -> getAdminConfigEntryEmailQueuePoolSize();
            case EMAIL_QUEUE_CAPACITY -> getAdminConfigEntryEmailQueueCapacity();
            case EMAIL_QUEUE_MAX_RETRY -> getAdminConfigEntryEmailQueueMaxRetry();
            case EMAIL_QUEUE_SENT_LIVE_DURATION -> getAdminConfigEntryEmailQueueSentLiveDuration();

            default -> configEntry.getKey();
        };
    }

    public String getAdminConfigEntryServiceNameTooltip() {
        return getTranslation("adminView.config.entry.service_name.tooltip");
    }

    public String getAdminConfigEntryBaseUrlTooltip() {
        return getTranslation("adminView.config.entry.base_url.tooltip");
    }

    public String getAdminConfigEntryRememberMeDurationTooltip() {
        return getTranslation("adminView.config.entry.remember_me_duration.tooltip");
    }

    public String getAdminConfigEntryRememberMeSecretKeyTooltip() {
        return getTranslation("adminView.config.entry.remember_me_secret_key.tooltip");
    }

    public String getAdminConfigEntryUserAutoAbsentTimeoutTooltip() {
        return getTranslation("adminView.config.entry.user_auto_absent_timeout.tooltip");
    }

    public String getAdminConfigEntryLoginAttemptsLimitTooltip() {
        return getTranslation("adminView.config.entry.login_attempts_limit.tooltip");
    }

    public String getAdminConfigEntryMaxSessionsPerUserTooltip() {
        return getTranslation("adminView.config.entry.max_sessions_per_user.tooltip");
    }

    public String getAdminConfigEntryMaintenanceModeTooltip() {
        return getTranslation("adminView.config.entry.maintenance_mode.tooltip");
    }

    public String getAdminConfigEntryEmailFromTooltip() {
        return getTranslation("adminView.config.entry.email_from.tooltip");
    }

    public String getAdminConfigEntryEmailAdminTooltip() {
        return getTranslation("adminView.config.entry.email_admin.tooltip");
    }

    public String getAdminConfigEntryVerificationTokenValidDurationTooltip() {
        return getTranslation("adminView.config.entry.verification_token_valid_duration.tooltip");
    }

    public String getAdminConfigEntryVerificationTokenLiveDurationTooltip() {
        return getTranslation("adminView.config.entry.verification_token_live_duration.tooltip");
    }

    public String getAdminConfigEntryPasswordResetTokenValidDurationTooltip() {
        return getTranslation("adminView.config.entry.password_reset_token_valid_duration.tooltip");
    }

    public String getAdminConfigEntryPasswordResetTokenLiveDurationTooltip() {
        return getTranslation("adminView.config.entry.password_reset_token_live_duration.tooltip");
    }

    public String getAdminConfigEntryMailHostTooltip() {
        return getTranslation("adminView.config.entry.mail_host.tooltip");
    }

    public String getAdminConfigEntryMailPortTooltip() {
        return getTranslation("adminView.config.entry.mail_port.tooltip");
    }

    public String getAdminConfigEntryMailUsernameTooltip() {
        return getTranslation("adminView.config.entry.mail_username.tooltip");
    }

    public String getAdminConfigEntryMailPasswordTooltip() {
        return getTranslation("adminView.config.entry.mail_password.tooltip");
    }

    public String getAdminConfigEntryMailSmtpAuthTooltip() {
        return getTranslation("adminView.config.entry.mail_smtp_auth.tooltip");
    }

    public String getAdminConfigEntryMailSmtpStarttlsEnableTooltip() {
        return getTranslation("adminView.config.entry.mail_smtp_starttls_enable.tooltip");
    }

    public String getAdminConfigEntryMailSmtpSslEnableTooltip() {
        return getTranslation("adminView.config.entry.mail_smtp_ssl_enable.tooltip");
    }

    public String getAdminConfigEntryMailSmtpSslTrustTooltip() {
        return getTranslation("adminView.config.entry.mail_smtp_ssl_trust.tooltip");
    }

    public String getAdminConfigEntryMailSmtpSocketfactoryClassTooltip() {
        return getTranslation("adminView.config.entry.mail_smtp_socketfactory_class.tooltip");
    }

    public String getAdminConfigEntryMailDebugTooltip() {
        return getTranslation("adminView.config.entry.mail_debug.tooltip");
    }

    public String getAdminConfigEntryEmailQueuePoolSizeTooltip() {
        return getTranslation("adminView.config.entry.email_queue_pool_size.tooltip");
    }

    public String getAdminConfigEntryEmailQueueCapacityTooltip() {
        return getTranslation("adminView.config.entry.email_queue_capacity.tooltip");
    }

    public String getAdminConfigEntryEmailQueueMaxRetryTooltip() {
        return getTranslation("adminView.config.entry.email_queue_max_retry.tooltip");
    }

    public String getAdminConfigEntryEmailQueueSentLiveDurationTooltip() {
        return getTranslation("adminView.config.entry.email_queue_sent_live_duration.tooltip");
    }

    public String getAdminConfigEntryTooltip(ConfigEntry configEntry) {
        return switch (configEntry) {
            case SERVICE_NAME -> getAdminConfigEntryServiceNameTooltip();
            case BASE_URL -> getAdminConfigEntryBaseUrlTooltip();

            case REMEMBER_ME_DURATION -> getAdminConfigEntryRememberMeDurationTooltip();
            case REMEMBER_ME_SECRET_KEY -> getAdminConfigEntryRememberMeSecretKeyTooltip();
            case USER_AUTO_ABSENT_TIMEOUT -> getAdminConfigEntryUserAutoAbsentTimeoutTooltip();
            case LOGIN_ATTEMPTS_LIMIT -> getAdminConfigEntryLoginAttemptsLimitTooltip();
            case MAX_SESSIONS_PER_USER -> getAdminConfigEntryMaxSessionsPerUserTooltip();
            case MAINTENANCE_MODE -> getAdminConfigEntryMaintenanceModeTooltip();

            case EMAIL_FROM -> getAdminConfigEntryEmailFromTooltip();
            case EMAIL_ADMIN -> getAdminConfigEntryEmailAdminTooltip();

            case VERIFICATION_TOKEN_VALID_DURATION -> getAdminConfigEntryVerificationTokenValidDurationTooltip();
            case VERIFICATION_TOKEN_LIVE_DURATION -> getAdminConfigEntryVerificationTokenLiveDurationTooltip();
            case PASSWORD_RESET_TOKEN_VALID_DURATION -> getAdminConfigEntryPasswordResetTokenValidDurationTooltip();
            case PASSWORD_RESET_TOKEN_LIVE_DURATION -> getAdminConfigEntryPasswordResetTokenLiveDurationTooltip();

            case MAIL_HOST -> getAdminConfigEntryMailHostTooltip();
            case MAIL_PORT -> getAdminConfigEntryMailPortTooltip();
            case MAIL_USERNAME -> getAdminConfigEntryMailUsernameTooltip();
            case MAIL_PASSWORD -> getAdminConfigEntryMailPasswordTooltip();
            case MAIL_SMTP_AUTH -> getAdminConfigEntryMailSmtpAuthTooltip();
            case MAIL_SMTP_STARTTLS_ENABLE -> getAdminConfigEntryMailSmtpStarttlsEnableTooltip();
            case MAIL_SMTP_SSL_ENABLE -> getAdminConfigEntryMailSmtpSslEnableTooltip();
            case MAIL_SMTP_SSL_TRUST -> getAdminConfigEntryMailSmtpSslTrustTooltip();
            case MAIL_SMTP_SOCKETFACTORY_CLASS -> getAdminConfigEntryMailSmtpSocketfactoryClassTooltip();
            case MAIL_DEBUG -> getAdminConfigEntryMailDebugTooltip();

            case EMAIL_QUEUE_POOL_SIZE -> getAdminConfigEntryEmailQueuePoolSizeTooltip();
            case EMAIL_QUEUE_CAPACITY -> getAdminConfigEntryEmailQueueCapacityTooltip();
            case EMAIL_QUEUE_MAX_RETRY -> getAdminConfigEntryEmailQueueMaxRetryTooltip();
            case EMAIL_QUEUE_SENT_LIVE_DURATION -> getAdminConfigEntryEmailQueueSentLiveDurationTooltip();

            default -> "";
        };
    }
}