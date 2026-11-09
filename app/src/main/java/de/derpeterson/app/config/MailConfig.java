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
          .filter(entry -> ConfigurationCatalog.group(entry) == AdminConfigGroup.SMTP)
          .toList();
  private final ConfigService configService;

  @Bean
  public JavaMailSender javaMailSender() {
    ConnectionAwareJavaMailSender sender =
        new ConnectionAwareJavaMailSender(this::configuredSender);
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    return sender;
  }

  /** Erstellt einen Transport für einen festen SMTP-Stand, etwa für die manuelle Mailvorschau. */
  public ConnectionAwareJavaMailSender configuredSender() {
    Map<ConfigEntry, String> settings = configService.snapshot(SMTP_SETTINGS);
    ConnectionAwareJavaMailSender mailSender = new ConnectionAwareJavaMailSender();
    mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    mailSender.setHost(settings.get(ConfigEntry.MAIL_HOST));
    mailSender.setPort(Integer.parseInt(settings.get(ConfigEntry.MAIL_PORT)));
    mailSender.setUsername(settings.get(ConfigEntry.MAIL_USERNAME));
    mailSender.setPassword(settings.get(ConfigEntry.MAIL_PASSWORD));

    Properties props = mailSender.getJavaMailProperties();
    putBoolean(props, settings, ConfigEntry.MAIL_SMTP_AUTH);
    putBoolean(props, settings, ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE);
    putBoolean(props, settings, ConfigEntry.MAIL_SMTP_SSL_ENABLE);
    props.setProperty(
        ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey(),
        settings.get(ConfigEntry.MAIL_SMTP_SSL_TRUST).replace(',', ' '));
    props.setProperty(ConfigEntry.MAIL_DEBUG.getKey(), settings.get(ConfigEntry.MAIL_DEBUG));

    return mailSender;
  }

  private static void putBoolean(
      Properties properties, Map<ConfigEntry, String> settings, ConfigEntry entry) {
    String value = settings.get(entry);
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      throw new IllegalArgumentException("Invalid boolean for configuration key " + entry.getKey());
    }
    properties.put(entry.getKey(), Boolean.valueOf(value));
  }
}
