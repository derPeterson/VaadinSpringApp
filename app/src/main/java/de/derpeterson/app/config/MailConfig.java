package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@RequiredArgsConstructor
public class MailConfig {

  private static final List<ConfigEntry> SMTP_SETTINGS =
      Arrays.stream(ConfigEntry.values())
          .filter(configEntry -> ConfigurationCatalog.group(configEntry) == AdminConfigGroup.SMTP)
          .toList();
  private final ConfigService configService;

  @Bean
  public JavaMailSender javaMailSender() {
    ConnectionAwareJavaMailSender connectionAwareMailSender =
        new ConnectionAwareJavaMailSender(this::createConfiguredMailSender);
    connectionAwareMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

    return connectionAwareMailSender;
  }

  /** Erstellt einen Transport für einen festen SMTP-Stand, etwa für die manuelle Mailvorschau. */
  public ConnectionAwareJavaMailSender createConfiguredMailSender() {
    Map<ConfigEntry, String> smtpSettings = configService.snapshot(SMTP_SETTINGS);

    ConnectionAwareJavaMailSender configuredMailSender = new ConnectionAwareJavaMailSender();
    configuredMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    configuredMailSender.setHost(smtpSettings.get(ConfigEntry.MAIL_HOST));
    configuredMailSender.setPort(Integer.parseInt(smtpSettings.get(ConfigEntry.MAIL_PORT)));
    configuredMailSender.setUsername(smtpSettings.get(ConfigEntry.MAIL_USERNAME));
    configuredMailSender.setPassword(smtpSettings.get(ConfigEntry.MAIL_PASSWORD));

    Properties javaMailProperties = configuredMailSender.getJavaMailProperties();
    putSmtpBooleanProperty(javaMailProperties, smtpSettings, ConfigEntry.MAIL_SMTP_AUTH);
    putSmtpBooleanProperty(javaMailProperties, smtpSettings, ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE);
    putSmtpBooleanProperty(javaMailProperties, smtpSettings, ConfigEntry.MAIL_SMTP_SSL_ENABLE);
    javaMailProperties.setProperty(
        ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey(),
        smtpSettings.get(ConfigEntry.MAIL_SMTP_SSL_TRUST).replace(',', ' '));
    javaMailProperties.setProperty(
        ConfigEntry.MAIL_DEBUG.getKey(), smtpSettings.get(ConfigEntry.MAIL_DEBUG));

    return configuredMailSender;
  }

  private static void putSmtpBooleanProperty(
      Properties javaMailProperties,
      Map<ConfigEntry, String> smtpSettings,
      ConfigEntry configEntry) {
    String configuredBooleanValue = smtpSettings.get(configEntry);
    if (!"true".equalsIgnoreCase(configuredBooleanValue)
        && !"false".equalsIgnoreCase(configuredBooleanValue)) {
      throw new IllegalArgumentException(
          "Invalid boolean for configuration key " + configEntry.getKey());
    }

    javaMailProperties.put(configEntry.getKey(), Boolean.valueOf(configuredBooleanValue));
  }
}
