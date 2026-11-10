package de.derpeterson.app.security;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.ExternalProviderEntity;
import de.derpeterson.app.repository.ExternalProviderRepository;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anbieterverwaltung mit frischen Administratorrechten und geheimnisfreien UI-Daten. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalProviderSettings {
  public static final String GOOGLE = "google";
  public static final String MICROSOFT = "microsoft";
  public static final String GOOGLE_NAME = "Google";
  public static final String MICROSOFT_NAME = "Microsoft";
  public static final List<String> PROVIDERS = List.of(GOOGLE, MICROSOFT);
  private final ExternalProviderRepository externalProviderRepository;
  private final ExternalLoginProperties externalLoginProperties;
  private final ExternalProviderSecrets externalProviderSecrets;
  private final AdminRolePolicy adminRolePolicy;
  private final TwoFactorAuthorization twoFactor;
  private final jakarta.servlet.http.HttpServletRequest httpRequest;

  public record Settings(
      String provider, String clientId, boolean secretStored, boolean enabled, String revision)
      implements Serializable {}

  /** Nur für den serverseitigen Anmeldeadapter, niemals für die Oberfläche oder Sitzungen. */
  record Credentials(
      String provider, String clientId, String clientSecret, boolean enabled, Long version) {
    @Override
    public String toString() {
      return "Provider credentials: " + provider;
    }
  }

  @Transactional(readOnly = true)
  public Settings load(String provider) {
    adminRolePolicy.requireSuperadmin();
    return describe(readCredentials(provider));
  }

  /** Ein leeres Geheimnis behält den bisherigen Wert. Veraltete Formulare überschreiben nichts. */
  @Transactional
  public Settings save(
      String provider, String clientId, String newSecret, boolean enabled, String revision) {
    adminRolePolicy.requireSuperadmin();
    requireProvider(provider);
    ExternalProviderEntity entity = externalProviderRepository.findById(provider).orElse(null);
    Credentials current = entity == null ? defaultCredentials(provider) : decrypted(entity);
    if (!Objects.equals(revision, revision(current))) {
      throw new OptimisticLockingFailureException("Provider configuration changed");
    }
    String id = Objects.requireNonNull(clientId).trim();
    String secret =
        Objects.requireNonNull(newSecret).isBlank() ? current.clientSecret() : newSecret;
    validateCredentials(id, secret, enabled);
    twoFactor.authorizeCurrent(httpRequest, TwoFactorAction.SECURITY_CONFIGURATION, true);
    if (entity == null) {
      entity = new ExternalProviderEntity();
    }
    entity.setProvider(provider);
    entity.setClientId(id);
    if (entity.getEncryptedClientSecret() == null || !newSecret.isBlank()) {
      entity.setEncryptedClientSecret(externalProviderSecrets.encrypt(provider, secret));
    }
    entity.setEnabled(enabled);
    ExternalProviderEntity saved = externalProviderRepository.saveAndFlush(entity);
    AfterCommitLog.info(
        log, "Sign-in provider configuration updated: provider={}, enabled={}", provider, enabled);
    return describe(decrypted(saved));
  }

  /** Nur der Startinitialisierer: fehlende Anbieter gemeinsam anlegen, bestehende nie ändern. */
  @Transactional
  public void initializeDefaults() {
    // Ein verlorener/falscher Schlüssel darf bestehende Secrets weder ersetzen noch umgehen.
    externalProviderRepository.findAll().forEach(this::decrypted);
    List<ExternalProviderEntity> missing =
        PROVIDERS.stream()
            .filter(provider -> !externalProviderRepository.existsById(provider))
            .map(provider -> {
              Credentials defaults = defaultCredentials(provider);
              ExternalProviderEntity entity = new ExternalProviderEntity();
              entity.setProvider(provider);
              entity.setClientId(defaults.clientId());
              entity.setEncryptedClientSecret(
                  externalProviderSecrets.encrypt(provider, defaults.clientSecret()));
              entity.setEnabled(defaults.enabled());
              return entity;
            })
            .toList();
    externalProviderRepository.saveAllAndFlush(missing);
  }

  private void validateCredentials(String id, String secret, boolean enabled) {
    if (id.length() > 512
        || secret.length() > 2048
        || id.chars().anyMatch(Character::isISOControl)
        || secret.chars().anyMatch(Character::isISOControl)
        || enabled && (id.isBlank() || secret.isBlank())) {
      throw new IllegalArgumentException("Complete provider credentials required");
    }
  }

  // Nur der Anmeldeadapter benötigt Geheimnisse. Kein UI-/JSON-Modell gibt sie zurück.
  @Transactional(readOnly = true)
  Credentials credentials(String provider) {
    return readCredentials(provider);
  }

  private Credentials readCredentials(String provider) {
    requireProvider(provider);
    return externalProviderRepository
        .findById(provider)
        .map(this::decrypted)
        .orElseGet(() -> defaultCredentials(provider));
  }

  private void requireProvider(String provider) {
    if (!PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("Unsupported sign-in provider");
    }
  }

  private Credentials defaultCredentials(String provider) {
    ExternalLoginProperties.Provider defaults =
        provider.equals(GOOGLE)
            ? externalLoginProperties.getGoogle()
            : externalLoginProperties.getMicrosoft();
    String clientId = Objects.requireNonNullElse(defaults.getClientId(), "").trim();
    String secret = Objects.requireNonNullElse(defaults.getClientSecret(), "");
    validateCredentials(clientId, secret, defaults.isEnabled());
    return new Credentials(provider, clientId, secret, defaults.isEnabled(), null);
  }

  private Credentials decrypted(ExternalProviderEntity entity) {
    return new Credentials(
        entity.getProvider(),
        entity.getClientId(),
        externalProviderSecrets.decrypt(entity.getProvider(), entity.getEncryptedClientSecret()),
        entity.isEnabled(),
        entity.getVersion());
  }

  private Settings describe(Credentials entity) {
    return new Settings(
        entity.provider(),
        entity.clientId(),
        !entity.clientSecret().isBlank(),
        entity.enabled(),
        revision(entity));
  }

  static String revision(Credentials entity) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      // Längenpräfixe machen die Felder eindeutig. Das Ergebnis enthält kein lesbares Geheimnis.
      for (String value :
          List.of(
              entity.provider(),
              entity.clientId(),
              entity.clientSecret(),
              Boolean.toString(entity.enabled()),
              Objects.toString(entity.version(), "initial"))) {
        digest.update((value.length() + ":" + value).getBytes(StandardCharsets.UTF_8));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }
}
