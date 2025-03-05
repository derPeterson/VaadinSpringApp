package de.derpeterson.app.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConfigSetting {
    APP_NAME("app.name", "ApplicationName"),
    REMEMBER_ME_DURATION("rememberMe.duration", 1209600),
    REMEMBER_ME_SECRET_KEY("rememberMe.secret.key", "czgwigh12t"),
    LOGIN_ATTEMPTS_LIMIT("login.attempts.limit", 10),
    MAX_SESSIONS_PER_USER("max.sessions.per.user", 3),
    MAINTENANCE_MODE("maintenance.mode", false);

    private final String key;
    private final Object defaultValue;

    public String getDefaultValueString() {
        return (defaultValue instanceof String defaultString) ? defaultString : String.valueOf(defaultValue);
    }

    public Integer getDefaultValueInteger() {
        return (defaultValue instanceof Integer defaultInteger) ? defaultInteger : Integer.parseInt(defaultValue.toString());
    }

    public Boolean getDefaultValueBoolean() {
        return (defaultValue instanceof Boolean defaultBoolean) ? defaultBoolean : Boolean.parseBoolean(defaultValue.toString());
    }
}
