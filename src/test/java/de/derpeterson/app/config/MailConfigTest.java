package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Calls the real bean factory with fixture-only configuration; no context or transport. */
class MailConfigTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productionSenderSetsUtf8AndPreservesConnectionProperties(boolean enabled) throws Exception {
        var config = mock(ConfigService.class);
        when(config.getString(ConfigEntry.MAIL_HOST)).thenReturn("smtp.invalid");
        when(config.getInteger(ConfigEntry.MAIL_PORT)).thenReturn(2525);
        when(config.getString(ConfigEntry.MAIL_USERNAME)).thenReturn("fixture-user");
        when(config.getString(ConfigEntry.MAIL_PASSWORD)).thenReturn("fixture-only-not-a-secret");
        when(config.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST)).thenReturn("smtp.invalid");
        when(config.getString(ConfigEntry.MAIL_DEBUG)).thenReturn("false");
        when(config.getBoolean(ConfigEntry.MAIL_SMTP_AUTH)).thenReturn(enabled);
        when(config.getBoolean(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE)).thenReturn(enabled);
        when(config.getBoolean(ConfigEntry.MAIL_SMTP_SSL_ENABLE)).thenReturn(enabled);

        var sender = assertInstanceOf(JavaMailSenderImpl.class, new MailConfig(config).javaMailSender());

        assertEquals("UTF-8", sender.getDefaultEncoding());
        assertEquals("UTF-8", new MimeMessageHelper(sender.createMimeMessage()).getEncoding());
        assertEquals("smtp.invalid", sender.getHost());
        assertEquals(2525, sender.getPort());
        assertEquals("fixture-user", sender.getUsername());
        assertEquals("fixture-only-not-a-secret", sender.getPassword());
        var properties = sender.getJavaMailProperties();
        assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_AUTH.getKey()));
        assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE.getKey()));
        assertEquals(enabled, properties.get(ConfigEntry.MAIL_SMTP_SSL_ENABLE.getKey()));
        assertEquals("smtp.invalid", properties.get(ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey()));
        assertEquals("false", properties.get(ConfigEntry.MAIL_DEBUG.getKey()));
        assertEquals(5, properties.size());
    }
}
