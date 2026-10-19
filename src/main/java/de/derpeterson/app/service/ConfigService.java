package de.derpeterson.app.service;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RetiredConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Missing entries (including legacy null values) use the requested default. Without an explicit
 * default, configured application defaults take precedence over the enum default, including before
 * database initialization. Stored values win. Strings are preserved verbatim; typed getters reject
 * malformed values rather than silently falling back. Every method requires a non-null ConfigEntry.
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

  private final ConfigRepository configRepository;
  private final PlatformTransactionManager transactionManager;
  private final Environment environment;

  /** Entfernt ausschließlich ausdrücklich ausgemusterte Einträge; wiederholbar beim Start. */
  public void removeRetiredEntries() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.executeWithoutResult(
        status -> {
          for (RetiredConfigEntry entry : RetiredConfigEntry.values()) {
            configRepository.findByKey(entry.getKey()).ifPresent(configRepository::delete);
          }
        });
  }

  public boolean exists(ConfigEntry configEntry) {
    return configRepository.existsByKey(configEntry.getKey());
  }

  public String getString(ConfigEntry configEntry) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .orElseGet(() -> getDefaultValue(configEntry));
  }

  /** Backend-Standard aus app.config.defaults.*, unabhängig vom gespeicherten Wert. */
  public String getDefaultValue(ConfigEntry configEntry) {
    Objects.requireNonNull(configEntry, "configEntry");
    return environment.getProperty(
        "app.config.defaults." + configEntry.getKey(), configEntry.getDefaultValueString());
  }

  public String getString(ConfigEntry configEntry, String defaultValue) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .orElse(defaultValue);
  }

  public int getInteger(ConfigEntry configEntry) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .map(Integer::parseInt)
        .orElseGet(() -> Integer.parseInt(getDefaultValue(configEntry)));
  }

  public int getInteger(ConfigEntry configEntry, int defaultValue) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .map(Integer::parseInt)
        .orElse(defaultValue);
  }

  public boolean getBoolean(ConfigEntry configEntry) {
    return parseBoolean(configEntry, getString(configEntry));
  }

  public boolean getBoolean(ConfigEntry configEntry, boolean defaultValue) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .map(value -> parseBoolean(configEntry, value))
        .orElse(defaultValue);
  }

  /** Accepts only true/false, case-insensitively, without trimming. */
  private boolean parseBoolean(ConfigEntry configEntry, String value) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(
        "Invalid boolean for configuration key " + configEntry.getKey());
  }

  /**
   * Stores a non-null, otherwise unmodified value (empty strings are allowed). Each call commits
   * independently of an ambient transaction. On concurrent creation the unique key constraint
   * chooses the insert winner; the loser updates that row in a fresh transaction. The last
   * committed write wins.
   */
  public void set(ConfigEntry configEntry, String value) {
    Objects.requireNonNull(configEntry, "configEntry");
    Objects.requireNonNull(value, "value");
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    boolean[] creating = {false};
    try {
      transaction.executeWithoutResult(
          status -> {
            ConfigEntity entity =
                configRepository
                    .findByKey(configEntry.getKey())
                    .orElseGet(
                        () -> {
                          creating[0] = true;
                          return new ConfigEntity(null, configEntry.getKey(), value);
                        });
            update(entity, value);
          });
    } catch (DataIntegrityViolationException failure) {
      if (!creating[0]) {
        throw failure;
      }
      // The failed insert has rolled back before starting this retry.
      transaction.executeWithoutResult(
          status -> {
            ConfigEntity entity =
                configRepository.findByKey(configEntry.getKey()).orElseThrow(() -> failure);
            update(entity, value);
          });
    }
  }

  private void update(ConfigEntity entity, String value) {
    entity.setValue(value);
    configRepository.saveAndFlush(entity);
  }
}
