package de.derpeterson.app.config;

import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Gemeinsamer Metadatenkatalog. Enthält keine Konfigurationswerte oder Geheimnisse und ersetzt
 * weder die zuständigen Dienste noch deren Berechtigungs- und Speicherregeln.
 */
public final class ConfigurationCatalog {
  public enum Storage {
    GENERAL_SETTINGS,
    SIGN_IN_PROVIDER,
    TWO_FACTOR_POLICY,
    STARTUP_ONLY
  }

  /** Property und Umgebungsvariable sind bei ausschließlich gespeicherten 2FA-Regeln null. */
  public record Entry(
      String key,
      AdminConfigGroup group,
      Storage storage,
      boolean secret,
      String property,
      String environmentVariable,
      Function<MessageProperties, String> label,
      Function<MessageProperties, String> description) {}

  public enum ProviderField {
    CLIENT_ID("client-id", "CLIENT_ID"),
    SECRET("client-secret", "CLIENT_SECRET"),
    ENABLED("enabled", "ENABLED");

    private final String suffix;
    private final String environmentSuffix;

    ProviderField(String suffix, String environmentSuffix) {
      this.suffix = suffix;
      this.environmentSuffix = environmentSuffix;
    }

    public String property(String provider) {
      requireProvider(provider);
      return "app.external-login." + provider + "." + suffix;
    }

    public String environmentVariable(String provider) {
      requireProvider(provider);
      return "APP_EXTERNAL_LOGIN_" + provider.toUpperCase(Locale.ROOT) + "_" + environmentSuffix;
    }

    public String label(MessageProperties texts) {
      return switch (this) {
        case CLIENT_ID -> texts.getProviderSettingsClientId();
        case SECRET -> texts.getProviderSettingsSecret();
        case ENABLED -> texts.getProviderSettingsEnable();
      };
    }

    public String description(MessageProperties texts, String provider) {
      requireProvider(provider);
      boolean microsoft = ExternalProviderSettings.MICROSOFT.equals(provider);
      return switch (this) {
        case CLIENT_ID -> texts.getProviderSettingsClientIdHelp(microsoft);
        case SECRET -> texts.getProviderSettingsSecretHelp(microsoft);
        case ENABLED -> texts.getProviderSettingsEnableHelp();
      };
    }

    public String rule(MessageProperties texts) {
      return switch (this) {
        case CLIENT_ID -> texts.getProviderSettingsClientIdRule();
        case SECRET -> texts.getProviderSettingsSecretRule();
        case ENABLED -> texts.getProviderSettingsEnableRule();
      };
    }
  }

  public enum StartupSetting {
    PROVIDER_SECRETS_ENCRYPTION_KEY(
        "app.security.provider-secrets-encryption-key",
        "APP_SECURITY_PROVIDER_SECRETS_ENCRYPTION_KEY",
        true,
        "Eigener Schlüssel für Google-/Microsoft-Secrets. Außerhalb der Datenbank sichern."),
    TOTP_ENCRYPTION_KEY(
        "app.security.totp-encryption-key",
        "APP_SECURITY_TOTP_ENCRYPTION_KEY",
        true,
        "Verschlüsselungsschlüssel außerhalb der Datenbank sichern."),
    DEVELOPMENT_OPTIONS_ENABLED(
        "app.security.development-options-enabled",
        "APP_SECURITY_DEVELOPMENT_OPTIONS_ENABLED",
        false,
        "Gibt nur die Entwicklungsoptionen frei. Aktiviert selbst keine Sicherheitsausnahme.");

    private final String property;
    private final String environmentVariable;
    private final boolean secret;
    private final String description;

    StartupSetting(
        String property, String environmentVariable, boolean secret, String description) {
      this.property = property;
      this.environmentVariable = environmentVariable;
      this.secret = secret;
      this.description = description;
    }

    public String property() {
      return property;
    }
  }

  private static final List<Entry> ENTRIES = createEntries();

  private ConfigurationCatalog() {}

  public static List<Entry> entries() {
    return ENTRIES;
  }

  public static String property(ConfigEntry entry) {
    return "app.config.defaults." + entry.getKey();
  }

  public static String environmentVariable(ConfigEntry entry) {
    return "APP_CFG_" + entry.name();
  }

  public static boolean secret(ConfigEntry entry) {
    return entry == ConfigEntry.MAIL_PASSWORD || entry == ConfigEntry.REMEMBER_ME_SECRET_KEY;
  }

  public static AdminConfigGroup group(ConfigEntry entry) {
    return switch (entry) {
      case SERVICE_NAME, COPYRIGHT_TEXT, BASE_URL -> AdminConfigGroup.GENERAL;
      case REMEMBER_ME_DURATION,
          REMEMBER_ME_SECRET_KEY,
          LOGIN_ATTEMPTS_LIMIT,
          MAX_SESSIONS_PER_USER,
          MAINTENANCE_MODE ->
          AdminConfigGroup.SECURITY;
      case VERIFICATION_TOKEN_VALID_DURATION,
          VERIFICATION_TOKEN_LIVE_DURATION,
          PASSWORD_RESET_TOKEN_VALID_DURATION,
          PASSWORD_RESET_TOKEN_LIVE_DURATION ->
          AdminConfigGroup.TOKENS;
      case USER_AUTO_ABSENT_TIMEOUT -> AdminConfigGroup.USER_STATUS;
      case USER_NOTIFICATIONS_INBOX_RETENTION_DAYS, USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS ->
          AdminConfigGroup.NOTIFICATIONS;
      case EMAIL_FROM, EMAIL_ADMIN -> AdminConfigGroup.EMAIL_SENDER;
      case MAIL_HOST,
          MAIL_PORT,
          MAIL_USERNAME,
          MAIL_PASSWORD,
          MAIL_SMTP_AUTH,
          MAIL_SMTP_STARTTLS_ENABLE,
          MAIL_SMTP_SSL_ENABLE,
          MAIL_SMTP_SSL_TRUST,
          MAIL_DEBUG ->
          AdminConfigGroup.SMTP;
      case EMAIL_QUEUE_POOL_SIZE,
          EMAIL_QUEUE_CAPACITY,
          EMAIL_QUEUE_MAX_RETRY,
          EMAIL_QUEUE_SENT_LIVE_DURATION ->
          AdminConfigGroup.QUEUE;
    };
  }

  public static String actionLabel(TwoFactorAction action, MessageProperties texts) {
    return switch (action) {
      case LOGIN -> texts.getTwoFactorRuleLogin();
      case ADMINISTRATION -> texts.getTwoFactorRuleAdministration();
      case PASSWORD -> texts.getTwoFactorRulePassword();
      case EMAIL -> texts.getTwoFactorRuleEmail();
      case LINK_PROVIDER -> texts.getTwoFactorRuleLink();
      case UNLINK_PROVIDER -> texts.getTwoFactorRuleUnlink();
      case MANAGE_FACTOR -> texts.getTwoFactorRuleFactor();
      case MANAGE_RECOVERY_CODES -> texts.getTwoFactorRuleRecovery();
      case SECURITY_CONFIGURATION -> texts.getTwoFactorRuleConfiguration();
    };
  }

  private static List<Entry> createEntries() {
    List<Entry> entries = new ArrayList<>();
    for (ConfigEntry entry : ConfigEntry.values()) {
      entries.add(
          new Entry(
              entry.getKey(),
              group(entry),
              Storage.GENERAL_SETTINGS,
              secret(entry),
              property(entry),
              environmentVariable(entry),
              texts -> texts.getAdminConfigEntryLabel(entry),
              texts -> texts.getAdminConfigEntryTooltip(entry)));
    }
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      for (ProviderField field : ProviderField.values()) {
        String property = field.property(provider);
        entries.add(
            new Entry(
                property,
                AdminConfigGroup.EXTERNAL_LOGIN,
                Storage.SIGN_IN_PROVIDER,
                field == ProviderField.SECRET,
                property,
                field.environmentVariable(provider),
                field::label,
                texts -> field.description(texts, provider)));
      }
    }
    entries.add(
        policy(
            "rules",
            MessageProperties::getTwoFactorRulesHint,
            MessageProperties::getTwoFactorRuleHelp));
    entries.add(
        policy(
            "inactivityEnabled",
            MessageProperties::getTwoFactorInactivityEnabled,
            MessageProperties::getTwoFactorInactivityHelp));
    entries.add(
        policy(
            "inactivitySeconds",
            MessageProperties::getTwoFactorInactivityDuration,
            MessageProperties::getTwoFactorInactivityHelp));
    entries.add(
        policy(
            "developmentPause",
            MessageProperties::getTwoFactorDevelopmentPause,
            MessageProperties::getTwoFactorDevelopmentWarning));
    entries.add(
        policy(
            "developmentGoogleConfirmation",
            MessageProperties::getTwoFactorDevelopmentGoogle,
            MessageProperties::getTwoFactorDevelopmentWarning));
    for (StartupSetting setting : StartupSetting.values()) {
      entries.add(
          new Entry(
              setting.property,
              setting == StartupSetting.PROVIDER_SECRETS_ENCRYPTION_KEY
                  ? AdminConfigGroup.EXTERNAL_LOGIN
                  : AdminConfigGroup.TWO_FACTOR,
              Storage.STARTUP_ONLY,
              setting.secret,
              setting.property,
              setting.environmentVariable,
              ignored -> setting.environmentVariable,
              ignored -> setting.description));
    }
    return List.copyOf(entries);
  }

  private static Entry policy(
      String storedField,
      Function<MessageProperties, String> label,
      Function<MessageProperties, String> description) {
    return new Entry(
        "two-factor." + storedField,
        AdminConfigGroup.TWO_FACTOR,
        Storage.TWO_FACTOR_POLICY,
        false,
        null,
        null,
        label,
        description);
  }

  private static void requireProvider(String provider) {
    if (!ExternalProviderSettings.PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("Unsupported sign-in provider");
    }
  }
}
