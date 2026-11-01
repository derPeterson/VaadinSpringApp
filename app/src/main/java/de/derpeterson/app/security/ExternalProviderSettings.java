package de.derpeterson.app.security;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.ExternalProviderEntity;
import de.derpeterson.app.repository.ExternalProviderRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  private final ExternalProviderRepository repository;
  private final ExternalLoginProperties properties;
  private final AdminRolePolicy administrators;

  public record Settings(
      String provider, String clientId, boolean secretStored, boolean enabled, String revision) {}

  @Transactional(readOnly = true)
  public Settings load(String provider) {
    administrators.authorizeMutation(null, Set.of());
    return describe(readCredentials(provider));
  }

  /** Ein leeres Geheimnis behält den bisherigen Wert. Veraltete Formulare überschreiben nichts. */
  @Transactional
  public Settings save(
      String provider, String clientId, String newSecret, boolean enabled, String revision) {
    administrators.authorizeMutation(null, Set.of());
    ExternalProviderEntity current = readCredentials(provider);
    if (!Objects.equals(revision, revision(current))) {
      throw new OptimisticLockingFailureException("Provider configuration changed");
    }
    String id = Objects.requireNonNull(clientId).trim();
    String secret =
        Objects.requireNonNull(newSecret).isBlank() ? current.getClientSecret() : newSecret;
    if (id.length() > 512
        || secret.length() > 2048
        || id.chars().anyMatch(Character::isISOControl)
        || secret.chars().anyMatch(Character::isISOControl)
        || enabled && (id.isBlank() || secret.isBlank())) {
      throw new IllegalArgumentException("Complete provider credentials required");
    }
    current.setClientId(id);
    current.setClientSecret(secret);
    current.setEnabled(enabled);
    ExternalProviderEntity saved = repository.saveAndFlush(current);
    AfterCommitLog.info(
        log, "Sign-in provider configuration updated: provider={}, enabled={}", provider, enabled);
    return describe(saved);
  }

  // Nur der Anmeldeadapter benötigt Geheimnisse. Kein UI-/JSON-Modell gibt sie zurück.
  @Transactional(readOnly = true)
  ExternalProviderEntity credentials(String provider) {
    return readCredentials(provider);
  }

  private ExternalProviderEntity readCredentials(String provider) {
    if (!PROVIDERS.contains(provider)) {
      throw new IllegalArgumentException("Unsupported sign-in provider");
    }
    return repository.findById(provider).orElseGet(() -> initialCredentials(provider));
  }

  private ExternalProviderEntity initialCredentials(String provider) {
    ExternalLoginProperties.Provider defaults =
        provider.equals("google") ? properties.getGoogle() : properties.getMicrosoft();
    ExternalProviderEntity entity = new ExternalProviderEntity();
    entity.setProvider(provider);
    entity.setClientId(Objects.requireNonNullElse(defaults.getClientId(), ""));
    entity.setClientSecret(Objects.requireNonNullElse(defaults.getClientSecret(), ""));
    entity.setEnabled(defaults.configured());
    return entity;
  }

  private Settings describe(ExternalProviderEntity entity) {
    return new Settings(
        entity.getProvider(),
        entity.getClientId(),
        !entity.getClientSecret().isBlank(),
        entity.isEnabled(),
        revision(entity));
  }

  static String revision(ExternalProviderEntity entity) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      // Längenpräfixe machen die Felder eindeutig. Das Ergebnis enthält kein lesbares Geheimnis.
      for (String value :
          List.of(
              entity.getProvider(),
              entity.getClientId(),
              entity.getClientSecret(),
              Boolean.toString(entity.isEnabled()),
              Objects.toString(entity.getVersion(), "initial"))) {
        digest.update((value.length() + ":" + value).getBytes(StandardCharsets.UTF_8));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }
}
