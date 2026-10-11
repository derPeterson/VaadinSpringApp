package de.derpeterson.app.service;

import de.derpeterson.app.config.ConnectionAwareJavaMailSender;
import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Startup configuration with an empty repository; no application, database or SMTP. */
@ExtendWith(MockitoExtension.class)
class ConfigServiceConfiguredDefaultsTest {
    @Mock
    private ConfigRepository repository;
    @Mock
    private PlatformTransactionManager transactionManager;
    private MockEnvironment environment;
    private ConfigService service;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        service = new ConfigService(repository, transactionManager, environment);
    }

    @Test
    void freshDatabaseMailBeanUsesConfiguredTransportBeforeSeeding() {
        configure(ConfigEntry.MAIL_HOST, "mailpit");
        configure(ConfigEntry.MAIL_PORT, "1025");
        configure(ConfigEntry.MAIL_SMTP_AUTH, "false");
        configure(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE, "false");
        configure(ConfigEntry.MAIL_SMTP_SSL_ENABLE, "false");
        configure(ConfigEntry.REMEMBER_ME_SECRET_KEY, "fixture-only-key");

        var sender = assertInstanceOf(ConnectionAwareJavaMailSender.class,
                new MailConfig(service).javaMailSender());

        assertEquals("mailpit", sender.getHost());
        assertEquals(1025, sender.getPort());
        assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.auth"));
        assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.starttls.enable"));
        assertEquals(false, sender.getJavaMailProperties().get("mail.smtp.ssl.enable"));
        assertEquals("UTF-8", sender.getDefaultEncoding());
        assertEquals("fixture-only-key", service.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY));
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
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

        assertEquals("", service.getString(ConfigEntry.MAIL_HOST));
        assertEquals(2525, service.getInteger(ConfigEntry.MAIL_PORT));
        assertTrue(service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
    }

    @Test
    void explicitFallbacksRetainTheirContract() {
        configure(ConfigEntry.MAIL_HOST, "mailpit");
        configure(ConfigEntry.MAIL_PORT, "2525");
        configure(ConfigEntry.MAIL_SMTP_AUTH, "false");

        assertEquals("explicit", service.getString(ConfigEntry.MAIL_HOST, "explicit"));
        assertNull(service.getString(ConfigEntry.MAIL_HOST, null));
        assertEquals(999, service.getInteger(ConfigEntry.MAIL_PORT, 999));
        assertTrue(service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, true));
    }

    @Test
    void legacyNullUsesConfiguredFallbackAndAllowsBlankStrings() {
        configure(ConfigEntry.MAIL_HOST, "");
        configure(ConfigEntry.MAIL_PORT, "2525");
        stored(ConfigEntry.MAIL_HOST, null);
        stored(ConfigEntry.MAIL_PORT, null);

        assertEquals("", service.getString(ConfigEntry.MAIL_HOST));
        assertEquals(2525, service.getInteger(ConfigEntry.MAIL_PORT));
    }

    @Test
    void malformedConfiguredValuesFailWhenTheyAreNeeded() {
        configure(ConfigEntry.MAIL_PORT, "bad-port");
        configure(ConfigEntry.MAIL_SMTP_AUTH, " false ");

        assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.MAIL_PORT));
        assertThrows(IllegalArgumentException.class, () -> service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
    }

    @Test
    void storedStringDoesNotResolveAnUnusedPlaceholderDefault() {
        configure(ConfigEntry.MAIL_HOST, "${missing.test.property}");
        stored(ConfigEntry.MAIL_HOST, "stored-host");

        assertEquals("stored-host", service.getString(ConfigEntry.MAIL_HOST));
    }
    private void configure(ConfigEntry entry, String value) {
        environment.setProperty("app.config.defaults." + entry.getKey(), value);
    }

    private void stored(ConfigEntry entry, String value) {
        when(repository.findByKey(entry.getKey()))
                .thenReturn(Optional.of(new ConfigEntity(null, entry.getKey(), value)));
    }
}
