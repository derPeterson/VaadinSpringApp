package de.derpeterson.app.service;

import de.derpeterson.app.config.ConfigurationCatalog;
import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Missing values use the requested default. Without an explicit default, configured application
 * defaults take precedence over the enum default, including before database initialization. Stored
 * values win. Strings are preserved verbatim; typed getters reject malformed values rather than
 * silently falling back. Every method requires a non-null ConfigEntry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

  private final ConfigRepository configRepository;
  private final PlatformTransactionManager transactionManager;
  private final Environment springEnvironment;

  public boolean exists(ConfigEntry configEntry) {
    return configRepository.existsByKey(configEntry.getKey());
  }

  public String getString(ConfigEntry configEntry) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .orElseGet(() -> getDefaultValue(configEntry));
  }

  /** Liest zusammengehörige Einstellungen mit einer einzigen Datenbankabfrage. */
  public Map<ConfigEntry, String> snapshot(List<ConfigEntry> configEntries) {
    Map<String, String> storedValuesByConfigKey =
        configRepository
            .findByKeyIn(configEntries.stream().map(ConfigEntry::getKey).toList())
            .stream()
            .collect(Collectors.toMap(ConfigEntity::getKey, ConfigEntity::getValue));
    Map<ConfigEntry, String> configSnapshot = new EnumMap<>(ConfigEntry.class);
    for (ConfigEntry configEntry : configEntries) {
      String configValue = storedValuesByConfigKey.get(configEntry.getKey());
      configSnapshot.put(
          configEntry, configValue == null ? getDefaultValue(configEntry) : configValue);
    }
    return configSnapshot;
  }

  /** SMTP darf neue Zugangsdaten und Transportoptionen nur als vollständigen Stand sehen. */
  public void saveSmtpSettings(Map<ConfigEntry, String> smtpConfigValues) {
    TransactionTemplate configWriteTransaction = new TransactionTemplate(transactionManager);
    configWriteTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    configWriteTransaction.executeWithoutResult(transactionStatus -> {
      for (Map.Entry<ConfigEntry, String> smtpSetting : smtpConfigValues.entrySet()) {
        String configKey = smtpSetting.getKey().getKey();
        String configValue = smtpSetting.getValue();
        ConfigEntity configEntity =
            configRepository
                .findByKey(configKey)
                .orElseGet(() -> new ConfigEntity(null, configKey, configValue));
        persistConfigEntity(configEntity, configValue);
      }
    });
  }

  /** Backend-Standard aus app.config.defaults.*, unabhängig vom gespeicherten Wert. */
  public String getDefaultValue(ConfigEntry configEntry) {
    Objects.requireNonNull(configEntry, "configEntry");
    return springEnvironment.getProperty(
        ConfigurationCatalog.property(configEntry), configEntry.getDefaultValueString());
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
    return parseConfigBoolean(configEntry, getString(configEntry));
  }

  public boolean getBoolean(ConfigEntry configEntry, boolean defaultValue) {
    return configRepository
        .findByKey(configEntry.getKey())
        .map(ConfigEntity::getValue)
        .map(configValue -> parseConfigBoolean(configEntry, configValue))
        .orElse(defaultValue);
  }

  /** Accepts only true/false, case-insensitively, without trimming. */
  private boolean parseConfigBoolean(ConfigEntry configEntry, String configValue) {
    if ("true".equalsIgnoreCase(configValue)) {
      return true;
    }
    if ("false".equalsIgnoreCase(configValue)) {
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
  public void set(ConfigEntry configEntry, String configValue) {
    Objects.requireNonNull(configEntry, "configEntry");
    Objects.requireNonNull(configValue, "value");
    TransactionTemplate configWriteTransaction = new TransactionTemplate(transactionManager);
    configWriteTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    boolean[] insertingConfigEntry = {false};
    boolean[] configValueChanged = {false};
    try {
      configWriteTransaction.executeWithoutResult(transactionStatus -> {
        ConfigEntity configEntity =
            configRepository
                .findByKey(configEntry.getKey())
                .orElseGet(() -> {
                  insertingConfigEntry[0] = true;
                  return new ConfigEntity(null, configEntry.getKey(), configValue);
                });
        configValueChanged[0] =
            insertingConfigEntry[0] || !Objects.equals(configEntity.getValue(), configValue);
        persistConfigEntity(configEntity, configValue);
      });
    } catch (DataIntegrityViolationException concurrentInsertFailure) {
      if (!insertingConfigEntry[0]) {
        throw concurrentInsertFailure;
      }
      // The failed insert has rolled back before starting this retry.
      configWriteTransaction.executeWithoutResult(transactionStatus -> {
        ConfigEntity configEntity =
            configRepository
                .findByKey(configEntry.getKey())
                .orElseThrow(() -> concurrentInsertFailure);
        configValueChanged[0] =
            insertingConfigEntry[0] || !Objects.equals(configEntity.getValue(), configValue);
        persistConfigEntity(configEntity, configValue);
      });
    }
    if (configValueChanged[0]) {
      if (configEntry == ConfigEntry.MAINTENANCE_MODE
          && ("true".equalsIgnoreCase(configValue) || "false".equalsIgnoreCase(configValue))) {
        log.info(
            "Maintenance configuration committed: enabled={}", Boolean.parseBoolean(configValue));
      } else {
        log.debug("Configuration entry committed: key={}", configEntry.name());
      }
    }
  }

  private void persistConfigEntity(ConfigEntity configEntity, String configValue) {
    configEntity.setValue(configValue);

    configRepository.saveAndFlush(configEntity);
  }
}
