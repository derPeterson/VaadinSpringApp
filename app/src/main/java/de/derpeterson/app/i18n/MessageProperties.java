package de.derpeterson.app.i18n;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SignInMethod;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Application adapter: existing getters remain available, with a live session locale by default.
 */
@Component
public class MessageProperties extends MessageTexts implements Serializable {
  private final Locale fixedLocale;

  @Autowired
  public MessageProperties(CustomI18NProvider i18nProvider) {
    super(i18nProvider::getTranslation);
    fixedLocale = null;
  }

  private MessageProperties(UnaryOperator<String> translationLookup, Locale translationLocale) {
    super(translationLookup);
    fixedLocale = translationLocale;
  }

  @Serial
  private Object writeReplace() {
    return new SerializedMessages(fixedLocale);
  }

  @Serial
  private void readObject(ObjectInputStream serializedMessageInput) throws InvalidObjectException {
    throw new InvalidObjectException("Message serialization proxy required");
  }

  private record SerializedMessages(Locale locale) implements Serializable {
    @Serial
    private Object readResolve() {
      CustomI18NProvider i18nProvider = new CustomI18NProvider();
      return locale == null ? new MessageProperties(i18nProvider) : forLocale(i18nProvider, locale);
    }
  }

  /** Fixed locale for explicit translations; does not read or change the Vaadin session. */
  public static MessageProperties forLocale(
      CustomI18NProvider i18nProvider, Locale translationLocale) {
    Objects.requireNonNull(i18nProvider, "Translation provider required.");
    Objects.requireNonNull(translationLocale, "Locale required.");
    return new MessageProperties(
        translationKey -> i18nProvider.getTranslation(translationKey, translationLocale),
        translationLocale);
  }

  @Override
  protected Locale formattingLocale() {
    return fixedLocale == null ? CustomI18NProvider.getCurrentLocale() : fixedLocale;
  }

  public String getUserStatusLabel(UserStatus userStatus) {
    return switch (userStatus) {
      case AVAILABLE -> getUserStatusAvailable();
      case EMPLOYED -> getUserStatusEmployed();
      case ABSENT -> getUserStatusAbsent();
      case OFFLINE -> getUserStatusOffline();
    };
  }

  public String getRegistrationChannelLabel(RegistrationChannel registrationChannel) {
    return switch (registrationChannel) {
      case null -> getSignInAccountCreated();
      case PORTAL -> getSignInPortal();
      case GOOGLE -> getExternalGoogle();
      case MICROSOFT -> getExternalMicrosoft();
    };
  }

  public String getSignInMethodLabel(SignInMethod signInMethod) {
    return switch (signInMethod) {
      case null -> getSignInUnknown();
      case PORTAL -> getSignInPortalPassword();
      case GOOGLE -> getExternalGoogle();
      case MICROSOFT -> getExternalMicrosoft();
      case SAVED_LOGIN -> getSignInSavedLogin();
    };
  }

  public String getUserStatusDescription(UserStatus userStatus) {
    return switch (userStatus) {
      case AVAILABLE -> getUserStatusAvailableDescription();
      case EMPLOYED -> getUserStatusEmployedDescription();
      case ABSENT -> getUserStatusAbsentDescription();
      case OFFLINE -> getUserStatusOfflineDescription();
    };
  }

  public List<String> getAdminConfigEffectiveUse(ConfigEntry configEntry) {
    return switch (configEntry) {
      case SERVICE_NAME -> List.of(getAdminConfigEffectiveServiceName());
      case COPYRIGHT_TEXT -> List.of(getAdminConfigEffectiveCopyright());
      case BASE_URL -> List.of(getAdminConfigEffectiveBaseUrl());
      case SESSION_TIMEOUT_SECONDS ->
          List.of(getAdminConfigEffectiveSessionNew(), getAdminConfigEffectiveSessionExisting());
      case SELF_DEACTIVATION_RETENTION_DAYS, SELF_DEACTIVATION_REMINDER_DAYS ->
          List.of(getAccountDeactivationEffective());
      case LOGIN_ATTEMPTS_LIMIT, LOGIN_ATTEMPT_WINDOW_SECONDS ->
          List.of(getAdminConfigEffectiveLoginAttempt(), getAdminConfigEffectiveLoginBlock());
      case MAX_SESSIONS_PER_USER ->
          List.of(
              getAdminConfigEffectiveSessionLimit(), getAdminConfigEffectiveSessionLimitExisting());
      case VERIFICATION_TOKEN_VALID_DURATION, PASSWORD_RESET_TOKEN_VALID_DURATION ->
          List.of(getAdminConfigEffectiveTokenNew(), getAdminConfigEffectiveTokenExisting());
      case VERIFICATION_TOKEN_LIVE_DURATION, PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          List.of(getAdminConfigEffectiveTokenRetention());
      case EMAIL_QUEUE_POOL_SIZE, EMAIL_QUEUE_CAPACITY, EMAIL_QUEUE_MAX_RETRY ->
          List.of(
              getAdminConfigEffectiveNextQueueRun(), getAdminConfigEffectiveQueueCurrentBatch());
      case EMAIL_QUEUE_RETRY_DELAY_SECONDS ->
          List.of(getAdminConfigEffectiveNextQueueRun(), getAdminConfigEffectiveWaitingRetries());
      case EMAIL_QUEUE_SENT_LIVE_DURATION -> List.of(getAdminConfigEffectiveCleanupExisting());
      case REMEMBER_ME_DURATION ->
          List.of(
              getAdminConfigEffectiveRememberNew(),
              getAdminConfigEffectiveRememberShortened(),
              getAdminConfigEffectiveRememberExisting());
      case REMEMBER_ME_SECRET_KEY -> List.of(getAdminConfigEffectiveRestartRemembered());
      case MAINTENANCE_MODE -> List.of(getAdminConfigEffectiveMaintenance());
      case USER_AUTO_ABSENT_TIMEOUT -> List.of(getAdminConfigEffectiveAutoAway());
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS ->
          List.of(
              getAdminConfigEffectiveInboxRetention(), getAdminConfigEffectiveCleanupExisting());
      case USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          List.of(
              getAdminConfigEffectiveHistoryRetention(), getAdminConfigEffectiveCleanupExisting());
      case EMAIL_FROM -> List.of(getAdminConfigEffectiveMailFrom());
      case EMAIL_ADMIN -> List.of(getAdminConfigEffectiveMailAdmin());
      case MAIL_HOST,
          MAIL_PORT,
          MAIL_USERNAME,
          MAIL_PASSWORD,
          MAIL_SMTP_AUTH,
          MAIL_SMTP_STARTTLS_ENABLE,
          MAIL_SMTP_SSL_ENABLE,
          MAIL_SMTP_SSL_TRUST,
          MAIL_DEBUG ->
          List.of(getAdminConfigEffectiveMailTransport(), getAdminConfigEffectiveQueueInFlight());
    };
  }

  public String getAdminConfigEntryLabel(ConfigEntry configEntry) {
    return switch (configEntry) {
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS ->
          getAdminConfigEntryNotificationInboxRetention();
      case USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          getAdminConfigEntryNotificationHistoryRetention();
      case SERVICE_NAME -> getAdminConfigEntryServiceName();
      case COPYRIGHT_TEXT -> getAdminConfigEntryCopyrightText();
      case BASE_URL -> getAdminConfigEntryBaseUrl();

      case REMEMBER_ME_DURATION -> getAdminConfigEntryRememberMeDuration();
      case REMEMBER_ME_SECRET_KEY -> getAdminConfigEntryRememberMeSecretKey();
      case USER_AUTO_ABSENT_TIMEOUT -> getAdminConfigEntryUserAutoAbsentTimeout();
      case LOGIN_ATTEMPTS_LIMIT -> getAdminConfigEntryLoginAttemptsLimit();
      case LOGIN_ATTEMPT_WINDOW_SECONDS -> getAdminConfigEntryLoginAttemptWindow();
      case MAX_SESSIONS_PER_USER -> getAdminConfigEntryMaxSessionsPerUser();
      case SESSION_TIMEOUT_SECONDS -> getAdminConfigEntrySessionTimeout();
      case SELF_DEACTIVATION_RETENTION_DAYS -> getAccountDeactivationRetentionLabel();
      case SELF_DEACTIVATION_REMINDER_DAYS -> getAccountDeactivationReminderLabel();
      case MAINTENANCE_MODE -> getAdminConfigEntryMaintenanceMode();

      case EMAIL_FROM -> getAdminConfigEntryEmailFrom();
      case EMAIL_ADMIN -> getAdminConfigEntryEmailAdmin();

      case VERIFICATION_TOKEN_VALID_DURATION -> getAdminConfigEntryVerificationTokenValidDuration();
      case VERIFICATION_TOKEN_LIVE_DURATION -> getAdminConfigEntryVerificationTokenLiveDuration();
      case PASSWORD_RESET_TOKEN_VALID_DURATION ->
          getAdminConfigEntryPasswordResetTokenValidDuration();
      case PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          getAdminConfigEntryPasswordResetTokenLiveDuration();

      case MAIL_HOST -> getAdminConfigEntryMailHost();
      case MAIL_PORT -> getAdminConfigEntryMailPort();
      case MAIL_USERNAME -> getAdminConfigEntryMailUsername();
      case MAIL_PASSWORD -> getAdminConfigEntryMailPassword();
      case MAIL_SMTP_AUTH -> getAdminConfigEntryMailSmtpAuth();
      case MAIL_SMTP_STARTTLS_ENABLE -> getAdminConfigEntryMailSmtpStarttlsEnable();
      case MAIL_SMTP_SSL_ENABLE -> getAdminConfigEntryMailSmtpSslEnable();
      case MAIL_SMTP_SSL_TRUST -> getAdminConfigEntryMailSmtpSslTrust();
      case MAIL_DEBUG -> getAdminConfigEntryMailDebug();

      case EMAIL_QUEUE_POOL_SIZE -> getAdminConfigEntryEmailQueuePoolSize();
      case EMAIL_QUEUE_CAPACITY -> getAdminConfigEntryEmailQueueCapacity();
      case EMAIL_QUEUE_MAX_RETRY -> getAdminConfigEntryEmailQueueMaxRetry();
      case EMAIL_QUEUE_RETRY_DELAY_SECONDS -> getAdminConfigEntryEmailQueueRetryDelay();
      case EMAIL_QUEUE_SENT_LIVE_DURATION -> getAdminConfigEntryEmailQueueSentLiveDuration();
    };
  }

  public String getAdminConfigEntryTooltip(ConfigEntry configEntry) {
    return switch (configEntry) {
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS ->
          getAdminConfigEntryNotificationInboxRetentionTooltip();
      case USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          getAdminConfigEntryNotificationHistoryRetentionTooltip();
      case SERVICE_NAME -> getAdminConfigEntryServiceNameTooltip();
      case COPYRIGHT_TEXT -> getAdminConfigEntryCopyrightTextTooltip();
      case BASE_URL -> getAdminConfigEntryBaseUrlTooltip();

      case REMEMBER_ME_DURATION -> getAdminConfigEntryRememberMeDurationTooltip();
      case REMEMBER_ME_SECRET_KEY -> getAdminConfigEntryRememberMeSecretKeyTooltip();
      case USER_AUTO_ABSENT_TIMEOUT -> getAdminConfigEntryUserAutoAbsentTimeoutTooltip();
      case LOGIN_ATTEMPTS_LIMIT -> getAdminConfigEntryLoginAttemptsLimitTooltip();
      case LOGIN_ATTEMPT_WINDOW_SECONDS -> getAdminConfigEntryLoginAttemptWindowTooltip();
      case MAX_SESSIONS_PER_USER -> getAdminConfigEntryMaxSessionsPerUserTooltip();
      case SESSION_TIMEOUT_SECONDS -> getAdminConfigEntrySessionTimeoutTooltip();
      case SELF_DEACTIVATION_RETENTION_DAYS -> getAccountDeactivationRetentionHelp();
      case SELF_DEACTIVATION_REMINDER_DAYS -> getAccountDeactivationReminderHelp();
      case MAINTENANCE_MODE -> getAdminConfigEntryMaintenanceModeTooltip();

      case EMAIL_FROM -> getAdminConfigEntryEmailFromTooltip();
      case EMAIL_ADMIN -> getAdminConfigEntryEmailAdminTooltip();

      case VERIFICATION_TOKEN_VALID_DURATION ->
          getAdminConfigEntryVerificationTokenValidDurationTooltip();
      case VERIFICATION_TOKEN_LIVE_DURATION ->
          getAdminConfigEntryVerificationTokenLiveDurationTooltip();
      case PASSWORD_RESET_TOKEN_VALID_DURATION ->
          getAdminConfigEntryPasswordResetTokenValidDurationTooltip();
      case PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          getAdminConfigEntryPasswordResetTokenLiveDurationTooltip();

      case MAIL_HOST -> getAdminConfigEntryMailHostTooltip();
      case MAIL_PORT -> getAdminConfigEntryMailPortTooltip();
      case MAIL_USERNAME -> getAdminConfigEntryMailUsernameTooltip();
      case MAIL_PASSWORD -> getAdminConfigEntryMailPasswordTooltip();
      case MAIL_SMTP_AUTH -> getAdminConfigEntryMailSmtpAuthTooltip();
      case MAIL_SMTP_STARTTLS_ENABLE -> getAdminConfigEntryMailSmtpStarttlsEnableTooltip();
      case MAIL_SMTP_SSL_ENABLE -> getAdminConfigEntryMailSmtpSslEnableTooltip();
      case MAIL_SMTP_SSL_TRUST -> getAdminConfigEntryMailSmtpSslTrustTooltip();
      case MAIL_DEBUG -> getAdminConfigEntryMailDebugTooltip();

      case EMAIL_QUEUE_POOL_SIZE -> getAdminConfigEntryEmailQueuePoolSizeTooltip();
      case EMAIL_QUEUE_CAPACITY -> getAdminConfigEntryEmailQueueCapacityTooltip();
      case EMAIL_QUEUE_MAX_RETRY -> getAdminConfigEntryEmailQueueMaxRetryTooltip();
      case EMAIL_QUEUE_RETRY_DELAY_SECONDS -> getAdminConfigEntryEmailQueueRetryDelayTooltip();
      case EMAIL_QUEUE_SENT_LIVE_DURATION -> getAdminConfigEntryEmailQueueSentLiveDurationTooltip();
    };
  }
}
