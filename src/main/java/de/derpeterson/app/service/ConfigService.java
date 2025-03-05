package de.derpeterson.app.service;

import de.derpeterson.app.config.ConfigSetting;
import de.derpeterson.app.model.Config;
import de.derpeterson.app.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigRepository configRepository;

    public boolean exists(ConfigSetting configSetting) {
        return configRepository.existsByKey(configSetting.getKey());
    }

    public String getString(ConfigSetting configSetting, String defaultValue) {
        return configRepository.findByKey(configSetting.getKey())
                .map(Config::getValue)
                .orElse(defaultValue);
    }

    public int getInt(ConfigSetting configSetting, int defaultValue) {
        return configRepository.findByKey(configSetting.getKey())
                .map(Config::getValue)
                .map(Integer::parseInt)
                .orElse(defaultValue);
    }

    public boolean getBoolean(ConfigSetting configSetting, boolean defaultValue) {
        return configRepository.findByKey(configSetting.getKey())
                .map(Config::getValue)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public void set(ConfigSetting setting, String value) {
        Config config = configRepository.findByKey(setting.getKey())
                .orElse(new Config(null, setting.getKey(), value));
        config.setValue(value);
        configRepository.save(config);
    }
}
