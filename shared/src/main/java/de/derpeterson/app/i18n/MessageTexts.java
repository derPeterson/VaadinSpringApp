package de.derpeterson.app.i18n;

import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.TokenDurationPolicy;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Function;

/** Typed translation getters shared by the application and external browser tests. */
public class MessageTexts {

    private final Function<String, String> lookup;

    public MessageTexts(Function<String, String> lookup) {
        this.lookup = Objects.requireNonNull(lookup, "Translation lookup required.");
    }

    public String getTranslation(String key) {
        return lookup.apply(key);
    }

    public String getFormattedTranslation(String key, Object... args) {
        return MessageFormat.format(getTranslation(key), args);
    }

    // Startseite: gemeinsame Getter für Anwendung und Browserprüfungen
    public String getHomeTitle() {
        return getTranslation("homeView.title");
    }

    public String getHomeDescription() {
        return getTranslation("homeView.description");
    }

    public String getHomeOverview() {
        return getTranslation("homeView.overview");
    }

    // Gemeinsame Formulare und Copyright-Konfiguration
    public String getAccountEmailPlaceholder() {
        return getTranslation("account.form.email.placeholder");
    }

    public String getAccountConfirmEmailPlaceholder() {
        return getTranslation("account.form.confirm_email.placeholder");
    }

    public String getAccountPasswordPlaceholder() {
        return getTranslation("account.form.password.placeholder");
    }

    public String getAccountConfirmPasswordPlaceholder() {
        return getTranslation("account.form.confirm_password.placeholder");
    }

    public String getAccountNewPasswordPlaceholder() {
        return getTranslation("account.form.new_password.placeholder");
    }

    public String getAccountFirstNamePlaceholder() {
        return getTranslation("account.form.first_name.placeholder");
    }

    public String getAccountLastNamePlaceholder() {
        return getTranslation("account.form.last_name.placeholder");
    }

    public String getAccountGenderPlaceholder() {
        return getTranslation("account.form.gender.placeholder");
    }

    public String getAdminConfigEntryCopyrightText() {
        return getTranslation("adminView.config.entry.copyright_text");
    }

    public String getAdminConfigEntryCopyrightTextTooltip() {
        return getTranslation("adminView.config.entry.copyright_text.tooltip");
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

    public String getLoginThrottledMessage() {
        return getTranslation("login.throttled.message");
    }

    public String getLoginUnavailableMessage() {
        return getTranslation("login.unavailable.message");
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
        return getTranslation("forgotPasswordView.request_message");
    }

    public String getResetPasswordTechnicalErrorMessage() {
        return getTranslation("resetPasswordView.technical_error_message");
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

    public String getRegistrationBirthDatePlaceholder() {
        return getTranslation("registrationView.birth_date_placeholder");
    }

    public String getRegistrationSuccessMessage(String email) {
        return getFormattedTranslation("registrationView.registration.success_message", email);
    }

    public String getRegistrationUnavailableMessage() {
        return getTranslation("registrationView.registration.unavailable_message");
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

    public String getBaseUserUpdateConflict() {
        return getTranslation("base.user.update.conflict");
    }

    public String getBaseUserStatusFailed() {
        return getTranslation("base.user.status.failed");
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

    public String getAdminNavToggleChildren() {
        return getTranslation("adminView.nav.toggle_children");
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

    public String getAdminDashboardWelcomeBack() {
        return getTranslation("adminView.dashboard.welcome_back");
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

    public String getAdminMailCardTitle() {
        return getTranslation("adminView.mail.card_title");
    }

    public String getAdminMailCardDescription() {
        return getTranslation("adminView.mail.card_description");
    }

    public String getAdminMailCardBadge() {
        return getTranslation("adminView.mail.card_badge");
    }

    public String getAdminMailSendHint() {
        return getTranslation("adminView.mail.send_hint");
    }

    public String getAdminMailRecipientHint() {
        return getTranslation("adminView.mail.recipient_hint");
    }

    public String getAdminMailSubjectHint() {
        return getTranslation("adminView.mail.subject_hint");
    }

    public String getAdminMailBodyHint() {
        return getTranslation("adminView.mail.body_hint");
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

    public String getAdminConfigGroupGeneralDescription() {
        return getTranslation("adminView.config.group.general.description");
    }

    public String getAdminConfigGroupSecurityDescription() {
        return getTranslation("adminView.config.group.security.description");
    }

    public String getAdminConfigGroupTokensDescription() {
        return getTranslation("adminView.config.group.tokens.description");
    }

    public String getAdminConfigGroupUserStatusDescription() {
        return getTranslation("adminView.config.group.user_status.description");
    }

    public String getAdminConfigGroupEmailSenderDescription() {
        return getTranslation("adminView.config.group.email_sender.description");
    }

    public String getAdminConfigGroupSmtpDescription() {
        return getTranslation("adminView.config.group.smtp.description");
    }

    public String getAdminConfigGroupQueueDescription() {
        return getTranslation("adminView.config.group.queue.description");
    }

    public String getAdminConfigGroupDescription(AdminConfigGroup group) {
        return switch (group) {
            case GENERAL -> getAdminConfigGroupGeneralDescription();
            case SECURITY -> getAdminConfigGroupSecurityDescription();
            case TOKENS -> getAdminConfigGroupTokensDescription();
            case USER_STATUS -> getAdminConfigGroupUserStatusDescription();
            case EMAIL_SENDER -> getAdminConfigGroupEmailSenderDescription();
            case SMTP -> getAdminConfigGroupSmtpDescription();
            case QUEUE -> getAdminConfigGroupQueueDescription();
        };
    }

    public String getAdminConfigSettingsCount(int count) {
        return count == 1
                ? getFormattedTranslation("adminView.config.settings_count.one", count)
                : getFormattedTranslation("adminView.config.settings_count.other", count);
    }

    public String getAdminConfigSaveHint() {
        return getTranslation("adminView.config.save_hint");
    }

    public String getAdminConfigSave() {
        return getTranslation("adminView.config.save");
    }

    public String getAdminConfigReload() {
        return getTranslation("adminView.config.reload");
    }

    public String getAdminConfigRuleOptional() {
        return getTranslation("adminView.config.rule.optional");
    }

    public String getAdminConfigRuleEmptyUsesDefault() {
        return getTranslation("adminView.config.rule.empty_uses_default");
    }

    public String getAdminConfigRuleBoolean() {
        return getTranslation("adminView.config.rule.boolean");
    }

    public String getAdminConfigRuleHost() {
        return getTranslation("adminView.config.rule.host");
    }

    public String getAdminConfigRuleSslTrust() {
        return getTranslation("adminView.config.rule.ssl_trust");
    }

    public String getAdminConfigRuleJavaClass() {
        return getTranslation("adminView.config.rule.java_class");
    }

    public String getAdminConfigRuleDurationNoMinimum() {
        return getTranslation("adminView.config.rule.duration_no_minimum");
    }

    public String getAdminConfigDefaultEmpty() {
        return getTranslation("adminView.config.default_empty");
    }

    public String getAdminConfigDefaultConfidential() {
        return getTranslation("adminView.config.default_confidential");
    }

    public String getAdminConfigPlaceholderEmpty() {
        return getTranslation("adminView.config.placeholder.empty");
    }

    public String getAdminConfigRuleInteger(int min, int max) {
        return getFormattedTranslation("adminView.config.rule.integer", Integer.toString(min), Integer.toString(max));
    }

    public String getAdminConfigInputRules() {
        return getTranslation("adminView.config.input_rules");
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
        return getFormattedTranslation("adminView.config.entry.verification_token_valid_duration.tooltip", TokenDurationPolicy.MIN_VALIDITY.toMinutes());
    }

    public String getAdminConfigEntryVerificationTokenLiveDurationTooltip() {
        return getFormattedTranslation("adminView.config.entry.verification_token_live_duration.tooltip", TokenDurationPolicy.MIN_RETENTION.toMinutes());
    }

    public String getAdminConfigEntryPasswordResetTokenValidDurationTooltip() {
        return getFormattedTranslation("adminView.config.entry.password_reset_token_valid_duration.tooltip", TokenDurationPolicy.MIN_VALIDITY.toMinutes());
    }

    public String getAdminConfigEntryPasswordResetTokenLiveDurationTooltip() {
        return getFormattedTranslation("adminView.config.entry.password_reset_token_live_duration.tooltip", TokenDurationPolicy.MIN_RETENTION.toMinutes());
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


    public String getAdminConfigValidationInvalidFields() {
        return getTranslation("adminView.config.validation.invalid_fields");
    }

    public String getAdminConfigValidationUrl() {
        return getTranslation("adminView.config.validation.url");
    }

    public String getAdminConfigValidationEmail() {
        return getTranslation("adminView.config.validation.email");
    }

    public String getAdminConfigValidationHost() {
        return getTranslation("adminView.config.validation.host");
    }

    public String getAdminConfigValidationPort() {
        return getTranslation("adminView.config.validation.port");
    }

    public String getAdminConfigValidationMinOne() {
        return getTranslation("adminView.config.validation.min_one");
    }

    public String getAdminConfigValidationNonNegative() {
        return getTranslation("adminView.config.validation.non_negative");
    }

    public String getAdminConfigValidationSecretKey(int min) {
        return getFormattedTranslation("adminView.config.validation.secret_key", min);
    }

    public String getAdminConfigValidationDurationMinimum(long minutes) {
        return getFormattedTranslation("adminView.config.validation.duration_minimum", minutes);
    }

    public String getAdminConfigValidationTokenDuration(long minutes) {
        return getFormattedTranslation("adminView.config.validation.token_duration", minutes);
    }

    public String getAdminConfigHelperTokenDurationFormats(long minutes) {
        return getFormattedTranslation("adminView.config.helper.token_duration_formats", minutes);
    }

    public String getAdminConfigValidationPositiveDuration() {
        return getTranslation("adminView.config.validation.positive_duration");
    }

    public String getAdminConfigValidationDuration() {
        return getTranslation("adminView.config.validation.duration");
    }

    public String getAdminConfigValidationJavaClass() {
        return getTranslation("adminView.config.validation.java_class");
    }

    public String getAdminConfigValidationServiceName() {
        return getTranslation("adminView.config.validation.service_name");
    }

    public String getAdminConfigValidationServiceNameMax(int max) {
        return getFormattedTranslation("adminView.config.validation.service_name_max", max);
    }

    public String getAdminConfigValidationSeconds() {
        return getTranslation("adminView.config.validation.seconds");
    }

    public String getAdminConfigHelperDurationFormats() {
        return getTranslation("adminView.config.helper.duration_formats");
    }

    public String getAdminConfigHelperSeconds() {
        return getTranslation("adminView.config.helper.seconds");
    }

    public String getAdminConfigHelperServiceNameMax(int max) {
        return getFormattedTranslation("adminView.config.helper.service_name_max", max);
    }

    public String getAdminUsersTitle() {
        return getTranslation("admin.users.title");
    }

    public String getAdminUsersCardTitle() {
        return getTranslation("admin.users.card_title");
    }

    public String getAdminUsersCardDescription() {
        return getTranslation("admin.users.card_description");
    }

    public String getAdminUsersAccountCount(int count) {
        return count == 1
                ? getFormattedTranslation("admin.users.account_count.one", count)
                : getFormattedTranslation("admin.users.account_count.other", count);
    }

    public String getAdminUsersDescription() {
        return getTranslation("admin.users.description");
    }

    public String getAdminUsersAdd() {
        return getTranslation("admin.users.add");
    }

    public String getAdminUsersEdit() {
        return getTranslation("admin.users.edit");
    }

    public String getAdminUsersDelete() {
        return getTranslation("admin.users.delete");
    }

    public String getAdminUsersSave() {
        return getTranslation("admin.users.save");
    }

    public String getAdminUsersCancel() {
        return getTranslation("admin.users.cancel");
    }

    public String getAdminUsersYes() {
        return getTranslation("admin.users.yes");
    }

    public String getAdminUsersNo() {
        return getTranslation("admin.users.no");
    }

    public String getAdminUsersGridEmail() {
        return getTranslation("admin.users.grid.email");
    }

    public String getAdminUsersGridEnabled() {
        return getTranslation("admin.users.grid.enabled");
    }

    public String getAdminUsersGridRoles() {
        return getTranslation("admin.users.grid.roles");
    }

    public String getAdminUsersGridActions() {
        return getTranslation("admin.users.grid.actions");
    }

    public String getAdminUsersFieldFirstName() {
        return getTranslation("admin.users.field.first_name");
    }

    public String getAdminUsersFieldLastName() {
        return getTranslation("admin.users.field.last_name");
    }

    public String getAdminUsersFieldEmail() {
        return getTranslation("admin.users.field.email");
    }

    public String getAdminUsersFieldPassword() {
        return getTranslation("admin.users.field.password");
    }

    public String getAdminUsersFieldEnabled() {
        return getTranslation("admin.users.field.enabled");
    }

    public String getAdminUsersEnabledHint() {
        return getTranslation("admin.users.field.enabled_hint");
    }

    public String getAdminUsersFieldRoles() {
        return getTranslation("admin.users.field.roles");
    }

    public String getAdminUsersCreateDialogTitle() {
        return getTranslation("admin.users.dialog.create_title");
    }

    public String getAdminUsersEditDialogTitle() {
        return getTranslation("admin.users.dialog.edit_title");
    }

    public String getAdminUsersDialogDescription() {
        return getTranslation("admin.users.dialog.description");
    }

    public String getAdminUsersCloseDialog() {
        return getTranslation("admin.users.dialog.close");
    }

    public String getAdminUsersPasswordPlaceholderEdit() {
        return getTranslation("admin.users.password.placeholder.edit");
    }

    public String getAdminUsersDeleteSuccess() {
        return getTranslation("admin.users.delete_success");
    }

    public String getAdminUsersSaveSuccessCreated() {
        return getTranslation("admin.users.save_success_created");
    }

    public String getAdminUsersSaveSuccessUpdated() {
        return getTranslation("admin.users.save_success_updated");
    }


    public String getGenderMale() {
        return getTranslation("gender.male");
    }

    public String getGenderFemale() {
        return getTranslation("gender.female");
    }

    public String getGenderOther() {
        return getTranslation("gender.other");
    }

    public String getRoleSuperadmin() {
        return getTranslation("role.superadmin");
    }

    public String getRoleGuest() {
        return getTranslation("role.guest");
    }

    public String getRoleAdmin() {
        return getTranslation("role.admin");
    }

    public String getRoleUser() {
        return getTranslation("role.user");
    }

    public String getAdminUsersGridName() {
        return getTranslation("admin.users.grid.name");
    }

    public String getAdminUsersGridStatus() {
        return getTranslation("admin.users.grid.status");
    }

    public String getAdminUsersGridLastActivity() {
        return getTranslation("admin.users.grid.last_activity");
    }

    public String getAdminUsersGridEmptyState() {
        return getTranslation("admin.users.grid.empty_state");
    }

    public String getAdminUsersSearchLabel() {
        return getTranslation("admin.users.search.label");
    }

    public String getAdminUsersSearchPlaceholder() {
        return getTranslation("admin.users.search.placeholder");
    }

    public String getAdminUsersClearFilters() {
        return getTranslation("admin.users.filter.clear");
    }

    public String getAdminUsersApplyFilters() {
        return getTranslation("admin.users.filter.apply");
    }

    public String getAdminUsersActiveFilters() {
        return getTranslation("admin.users.filter.active");
    }

    public String getAdminUsersActiveFilterCount(int count) {
        return getFormattedTranslation("admin.users.filter.active_count", count);
    }

    public String getAdminUsersFilterTimeHint() {
        return getTranslation("admin.users.filter.time_hint");
    }

    public String getAdminUsersAddFilter() {
        return getTranslation("admin.users.filter.add");
    }

    public String getAdminUsersFilterPanelTitle() {
        return getTranslation("admin.users.filter.title");
    }

    public String getAdminUsersRemoveFilter(String label) {
        return getFormattedTranslation("admin.users.filter.remove", label);
    }

    public String getAdminUsersFilterAll() {
        return getTranslation("admin.users.filter.all");
    }

    public String getAdminUsersFilterRole() {
        return getTranslation("admin.users.filter.role");
    }

    public String getAdminUsersActivityDate() {
        return getTranslation("admin.users.activity.date");
    }

    public String getAdminUsersActivityTimeOptional() {
        return getTranslation("admin.users.activity.time_optional");
    }

    public String getAdminUsersActivityNow() {
        return getTranslation("admin.users.activity.now");
    }

    public String getAdminUsersActivityMinutes(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.minutes.one" : "admin.users.activity.minutes.many", count);
    }

    public String getAdminUsersActivityHours(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.hours.one" : "admin.users.activity.hours.many", count);
    }

    public String getAdminUsersActivityDays(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.days.one" : "admin.users.activity.days.many", count);
    }

    public String getAdminUsersActivityWeeks(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.weeks.one" : "admin.users.activity.weeks.many", count);
    }

    public String getAdminUsersActivityMonths(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.months.one" : "admin.users.activity.months.many", count);
    }

    public String getAdminUsersActivityYears(long count) {
        return getFormattedTranslation(count == 1
                ? "admin.users.activity.years.one" : "admin.users.activity.years.many", count);
    }

    public String getAdminUsersFilterActiveSince() {
        return getTranslation("admin.users.filter.active_since");
    }

    public String getAdminUsersFilterActiveSinceHelp() {
        return getTranslation("admin.users.filter.active_since_help");
    }

    public String getAdminUsersFilterActiveSinceError() {
        return getTranslation("admin.users.filter.active_since_error");
    }

    public String getAdminUsersPaginationTotal(int totalItems) {
        return getFormattedTranslation("admin.users.pagination.total", totalItems);
    }

    public String getAdminUsersPaginationPage(int page) {
        return getFormattedTranslation("admin.users.pagination.page", page);
    }

    public String getAdminUsersRefreshTooltip() {
        return getTranslation("admin.users.refresh.tooltip");
    }

    public String getAdminUsersPaginationPerPage() {
        return getTranslation("admin.users.pagination.per_page");
    }

    public String getAdminUsersPaginationFirstPage() {
        return getTranslation("admin.users.pagination.first_page");
    }

    public String getAdminUsersPaginationPreviousPage() {
        return getTranslation("admin.users.pagination.previous_page");
    }

    public String getAdminUsersPaginationNextPage() {
        return getTranslation("admin.users.pagination.next_page");
    }

    public String getAdminUsersPaginationLastPage() {
        return getTranslation("admin.users.pagination.last_page");
    }

    public String getAdminUsersDeleteDialogTitle() {
        return getTranslation("admin.users.delete_dialog.title");
    }

    public String getAdminUsersDeleteDialogDescription() {
        return getTranslation("admin.users.delete_dialog.description");
    }

    public String getAdminUsersDeleteDialogQuestion() {
        return getTranslation("admin.users.delete_dialog.question");
    }

    public String getAdminUsersDeleteDialogWarning() {
        return getTranslation("admin.users.delete_dialog.warning");
    }

    public String getAdminUsersPasswordHelperEdit() {
        return getFormattedTranslation("admin.users.password.helper.edit", getBaseValidationPasswordInvalidMessage());
    }

    public String getAdminUsersPasswordHelperCreate() {
        return getFormattedTranslation("admin.users.password.helper.create", getBaseValidationPasswordInvalidMessage());
    }

    public String getAdminUsersPasswordValidationEdit() {
        return getFormattedTranslation("admin.users.password.validation.edit", getBaseValidationPasswordInvalidMessage());
    }

    public String getAdminUsersPasswordValidationCreate() {
        return getFormattedTranslation("admin.users.password.validation.create", getBaseValidationPasswordInvalidMessage());
    }

    public String getAdminUsersRolesHelper() {
        return getTranslation("admin.users.roles.helper");
    }

    public String getAdminUsersTechnicalInfoEdit() {
        return getTranslation("admin.users.technical_info.edit");
    }

    public String getAdminUsersTechnicalInfoCreate() {
        return getTranslation("admin.users.technical_info.create");
    }

    public String getAdminUsersValidationCheckFields() {
        return getTranslation("admin.users.validation.check_fields");
    }

    public String getAdminUsersErrorRoleMissing() {
        return getTranslation("admin.users.error.role_missing");
    }

    public String getAdminUsersErrorDisableOwnUser() {
        return getTranslation("admin.users.error.disable_own_user");
    }

    public String getAdminUsersErrorRemoveOwnAdminRole() {
        return getTranslation("admin.users.error.remove_own_admin_role");
    }

    public String getAdminUsersErrorLastActiveAdmin() {
        return getTranslation("admin.users.error.last_active_admin");
    }

    public String getAdminUsersErrorSaveFailed() {
        return getTranslation("admin.users.error.save_failed");
    }

    public String getAdminUsersErrorDeleteOwnUser() {
        return getTranslation("admin.users.error.delete_own_user");
    }

    public String getAdminUsersErrorDeleteLastAdmin() {
        return getTranslation("admin.users.error.delete_last_admin");
    }

    public String getAdminUsersErrorDeleteFailed() {
        return getTranslation("admin.users.error.delete_failed");
    }

    /** Group identity is independent of the properties key and the current label. */
    public String getAdminConfigGroupLabel(AdminConfigGroup group) {
        return switch (group) {
            case GENERAL -> getAdminConfigGroupGeneral();
            case SECURITY -> getAdminConfigGroupSecurity();
            case TOKENS -> getAdminConfigGroupTokens();
            case USER_STATUS -> getAdminConfigGroupUserStatus();
            case EMAIL_SENDER -> getAdminConfigGroupEmailSender();
            case SMTP -> getAdminConfigGroupSmtp();
            case QUEUE -> getAdminConfigGroupQueue();
        };
    }
    /** Sprachabhängiger Standard-Fußtext; die benannten Platzhalter ersetzt die Kontoansicht. */
    public String getAccountCopyrightTemplate() {
        return getTranslation("account.copyright.template");
    }

}
