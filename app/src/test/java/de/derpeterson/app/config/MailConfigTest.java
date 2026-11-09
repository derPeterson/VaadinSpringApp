package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Calls the real bean factory with fixture-only configuration; no context or transport. */
class MailConfigTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void productionSenderSetsUtf8AndPreservesConnectionProperties(boolean enabled) {
    ConfigService config = mock(ConfigService.class);
    when(config.snapshot(anyList()))
        .thenReturn(
            Map.of(
                ConfigEntry.MAIL_HOST, "smtp.invalid",
                ConfigEntry.MAIL_PORT, "2525",
                ConfigEntry.MAIL_USERNAME, "fixture-user",
                ConfigEntry.MAIL_PASSWORD, "fixture-only-not-a-secret",
                ConfigEntry.MAIL_SMTP_SSL_TRUST, "smtp.invalid,backup.invalid",
                ConfigEntry.MAIL_DEBUG, "false",
                ConfigEntry.MAIL_SMTP_AUTH, Boolean.toString(enabled),
                ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE, Boolean.toString(enabled),
                ConfigEntry.MAIL_SMTP_SSL_ENABLE, Boolean.toString(enabled)));

    MailConfig mail = new MailConfig(config);
    ConnectionAwareJavaMailSender live =
        assertInstanceOf(ConnectionAwareJavaMailSender.class, mail.javaMailSender());
    ConnectionAwareJavaMailSender sender = mail.configuredSender();
    assertEquals("UTF-8", new MimeMessageHelper(live.createMimeMessage()).getEncoding());
    assertEquals("UTF-8", sender.getDefaultEncoding());
    assertEquals("UTF-8", new MimeMessageHelper(sender.createMimeMessage()).getEncoding());
    assertEquals("smtp.invalid", sender.getHost());
    assertEquals(2525, sender.getPort());
    assertEquals("fixture-user", sender.getUsername());
    assertEquals("fixture-only-not-a-secret", sender.getPassword());
    Properties properties = sender.getJavaMailProperties();
    assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_AUTH.getKey()));
    assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE.getKey()));
    assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_SSL_ENABLE.getKey()));
    assertEquals(
        "smtp.invalid backup.invalid", properties.get(ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey()));
    assertEquals("false", properties.get(ConfigEntry.MAIL_DEBUG.getKey()));
    assertEquals(5, properties.size());
  }
}
