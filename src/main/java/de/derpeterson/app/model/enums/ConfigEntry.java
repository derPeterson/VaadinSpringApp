package de.derpeterson.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum ConfigEntry {
    // General Konfiguration
    SERVICE_NAME("service.name", "{ServiceName}"),
    BASE_URL("base.url", "http://localhost:8080/"),
    REMEMBER_ME_DURATION("rememberMe.duration", 1209600),
    REMEMBER_ME_SECRET_KEY("rememberMe.secret.key", "czgwigh12t"),
    USER_AUTO_ABSENT_TIMEOUT("user.auto.absent.timeout", Duration.ofMinutes(10).toString()),
    LOGIN_ATTEMPTS_LIMIT("login.attempts.limit", 10),
    MAX_SESSIONS_PER_USER("max.sessions.per.user", 3),
    MAINTENANCE_MODE("maintenance.mode", false),
    EMAIL_FROM("email.from", "derpetersondev@yandex.com"),
    EMAIL_ADMIN("email.admin", "derpetersondev@yandex.com"),
    VERIFICATION_TOKEN_VALID_DURATION("verificationToken.valid.duration", Duration.ofHours(24).toString()),
    VERIFICATION_TOKEN_LIVE_DURATION("verificationToken.live.duration", Duration.ofDays(7).toString()),
    PASSWORD_RESET_TOKEN_VALID_DURATION("passwordResetToken.valid.duration", Duration.ofHours(3).toString()),
    PASSWORD_RESET_TOKEN_LIVE_DURATION("passwordResetToken.live.duration", Duration.ofDays(3).toString()),

    // EMail Konfiguration
    MAIL_HOST("mail.host", "smtp.yandex.com"),
    MAIL_PORT("mail.port", 465),
    MAIL_USERNAME("mail.username", "derpetersondev@yandex.com"),
    MAIL_PASSWORD("mail.password", "kiduwcggwlbnydnl"),
    MAIL_SMTP_AUTH("mail.smtp.auth", true),
    MAIL_SMTP_STARTTLS_ENABLE("mail.smtp.starttls.enable", false),
    MAIL_SMTP_SSL_ENABLE("mail.smtp.ssl.enable", true),
    MAIL_SMTP_SSL_TRUST("mail.smtp.ssl.trust", "smtp.yandex.com"),
    MAIL_SMTP_SOCKETFACTORY_CLASS("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"),
    MAIL_DEBUG("mail.debug", "true"),

    // Thread-Pool Konfiguration
    EMAIL_QUEUE_POOL_SIZE("email.queue.pool.size", 5),
    EMAIL_QUEUE_CAPACITY("email.queue.capacity", 50),
    EMAIL_QUEUE_MAX_RETRY("email.queue.max_retry", 3),
    EMAIL_QUEUE_SENT_LIVE_DURATION("email.queue.sent.live.duration", Duration.ofDays(7).toString());

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
