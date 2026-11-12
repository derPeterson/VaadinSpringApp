package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.config.ConnectionAwareJavaMailSender;
import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

/** Startup configuration with an empty repository; no application, database or SMTP. */
@ExtendWith(MockitoExtension.class)
class ConfigServiceConfiguredDefaultsTest {
  @Mock private ConfigRepository configRepository;
  @Mock private PlatformTransactionManager transactionManager;
  private MockEnvironment environment;
  private ConfigService configService;

  @BeforeEach
  void setUp() {
    environment = new MockEnvironment();
    configService =
        new ConfigService(
            configRepository, transactionManager, environment, mock(AdminRolePolicy.class));
  }

  @Test
  void freshDatabaseMailBeanUsesConfiguredTransportBeforeSeeding() {
    configure(ConfigEntry.MAIL_HOST, "mailpit");
    configure(ConfigEntry.MAIL_PORT, "1025");
    configure(ConfigEntry.MAIL_SMTP_AUTH, "false");
    configure(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE, "false");
    configure(ConfigEntry.MAIL_SMTP_SSL_ENABLE, "false");
    configure(ConfigEntry.REMEMBER_ME_SECRET_KEY, "fixture-only-key");

    ConnectionAwareJavaMailSender sender =
        new MailConfig(configService).createConfiguredMailSender();

    assertEquals("mailpit", sender.getHost());
    assertEquals(1025, sender.getPort());
    assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.auth"));
    assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.starttls.enable"));
    assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.ssl.enable"));
    assertEquals("UTF-8", sender.getDefaultEncoding());
    assertEquals("fixture-only-key", configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY));
    verify(configRepository, never()).save(any());
    verify(configRepository, never()).saveAndFlush(any());
    verifyNoInteractions(transactionManager);
  }

  @Test
  void persistedValuesWinEvenWhenConfiguredValuesAreInvalid() {
    configure(ConfigEntry.MAIL_HOST, "mailpit");
    configure(ConfigEntry.MAIL_PORT, "invalid");
    configure(ConfigEntry.MAIL_SMTP_AUTH, "invalid");
    stored(ConfigEntry.MAIL_HOST, "");
    stored(ConfigEntry.MAIL_PORT, "2525");
    stored(ConfigEntry.MAIL_SMTP_AUTH, "true");

    assertEquals("", configService.getString(ConfigEntry.MAIL_HOST));
    assertEquals(2525, configService.getInteger(ConfigEntry.MAIL_PORT));
    assertTrue(configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
  }

  @Test
  void explicitFallbacksRetainTheirContract() {
    configure(ConfigEntry.MAIL_HOST, "mailpit");
    configure(ConfigEntry.MAIL_PORT, "2525");
    configure(ConfigEntry.MAIL_SMTP_AUTH, "false");

    assertEquals("explicit", configService.getString(ConfigEntry.MAIL_HOST, "explicit"));
    assertNull(configService.getString(ConfigEntry.MAIL_HOST, null));
    assertEquals(999, configService.getInteger(ConfigEntry.MAIL_PORT, 999));
    assertTrue(configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, true));
  }

  @Test
  void missingValueUsesConfiguredFallbackAndAllowsBlankStrings() {
    configure(ConfigEntry.MAIL_HOST, "");
    configure(ConfigEntry.MAIL_PORT, "2525");
    stored(ConfigEntry.MAIL_HOST, null);
    stored(ConfigEntry.MAIL_PORT, null);

    assertEquals("", configService.getString(ConfigEntry.MAIL_HOST));
    assertEquals(2525, configService.getInteger(ConfigEntry.MAIL_PORT));
  }

  @Test
  void malformedConfiguredValuesFailWhenTheyAreNeeded() {
    configure(ConfigEntry.MAIL_PORT, "bad-port");
    configure(ConfigEntry.MAIL_SMTP_AUTH, " false ");

    assertThrows(
        NumberFormatException.class, () -> configService.getInteger(ConfigEntry.MAIL_PORT));
    assertThrows(
        IllegalArgumentException.class, () -> configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
  }

  @Test
  void storedStringDoesNotResolveAnUnusedPlaceholderDefault() {
    configure(ConfigEntry.MAIL_HOST, "${missing.test.property}");
    stored(ConfigEntry.MAIL_HOST, "stored-host");

    assertEquals("stored-host", configService.getString(ConfigEntry.MAIL_HOST));
  }

  private void configure(ConfigEntry entry, String value) {
    environment.setProperty("app.config.defaults." + entry.getKey(), value);
  }

  private void stored(ConfigEntry entry, String value) {
    when(configRepository.findByKey(entry.getKey()))
        .thenReturn(Optional.of(new ConfigEntity(null, entry.getKey(), value)));
  }
}
