package de.derpeterson.app.service;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigRepository configRepository;

    public boolean exists(ConfigEntry configEntry) {
        return configRepository.existsByKey(configEntry.getKey());
    }

    public String getString(ConfigEntry configEntry) {
        return getString(configEntry, configEntry.getDefaultValueString());
    }

    public String getString(ConfigEntry configEntry, String defaultValue) {
        return configRepository.findByKey(configEntry.getKey())
                .map(ConfigEntity::getValue)
                .orElse(defaultValue);
    }

    public int getInteger(ConfigEntry configEntry) {
        return getInteger(configEntry, configEntry.getDefaultValueInteger());
    }

    public int getInteger(ConfigEntry configEntry, int defaultValue) {
        return configRepository.findByKey(configEntry.getKey())
                .map(ConfigEntity::getValue)
                .map(Integer::parseInt)
                .orElse(defaultValue);
    }

    public boolean getBoolean(ConfigEntry configEntry) {
        return getBoolean(configEntry, configEntry.getDefaultValueBoolean());
    }

    public boolean getBoolean(ConfigEntry configEntry, boolean defaultValue) {
        return configRepository.findByKey(configEntry.getKey())
                .map(ConfigEntity::getValue)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public void set(ConfigEntry configEntry, String value) {
        ConfigEntity configEntity = configRepository.findByKey(configEntry.getKey())
                .orElse(new ConfigEntity(null, configEntry.getKey(), value));
        configEntity.setValue(value);
        configRepository.save(configEntity);
    }
}
