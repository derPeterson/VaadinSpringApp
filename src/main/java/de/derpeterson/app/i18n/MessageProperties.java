package de.derpeterson.app.i18n;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Application adapter: existing getters remain available, with a live session locale by default. */
@Component
public class MessageProperties extends MessageTexts {

    @Autowired
    public MessageProperties(CustomI18NProvider provider) {
        super(provider::getTranslation);
    }

    private MessageProperties(Function<String, String> lookup) {
        super(lookup);
    }

    /** Fixed locale for explicit translations; does not read or change the Vaadin session. */
    public static MessageProperties forLocale(CustomI18NProvider provider, Locale locale) {
        Objects.requireNonNull(provider, "Translation provider required.");
        Objects.requireNonNull(locale, "Locale required.");
        return new MessageProperties(key -> provider.getTranslation(key, locale));
    }

    public String getUserStatusLabel(UserStatus status) {
        return switch (status) {
            case AVAILABLE -> getUserStatusAvailable();
            case EMPLOYED -> getUserStatusEmployed();
            case ABSENT -> getUserStatusAbsent();
            case OFFLINE -> getUserStatusOffline();
        };
    }

    public String getUserStatusDescription(UserStatus status) {
        return switch (status) {
            case AVAILABLE -> getUserStatusAvailableDescription();
            case EMPLOYED -> getUserStatusEmployedDescription();
            case ABSENT -> getUserStatusAbsentDescription();
            case OFFLINE -> getUserStatusOfflineDescription();
        };
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
        };
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
        };
    }
}
