package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final ConfigService configService;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(configService.getString(ConfigEntry.MAIL_HOST));
        mailSender.setPort(configService.getInteger(ConfigEntry.MAIL_PORT));
        mailSender.setUsername(configService.getString(ConfigEntry.MAIL_USERNAME));
        mailSender.setPassword(configService.getString(ConfigEntry.MAIL_PASSWORD));

        Properties props = mailSender.getJavaMailProperties();
        props.put(ConfigEntry.MAIL_SMTP_AUTH.getKey(), configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
        props.put(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE.getKey(), configService.getBoolean(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE));
        props.put(ConfigEntry.MAIL_SMTP_SSL_ENABLE.getKey(), configService.getBoolean(ConfigEntry.MAIL_SMTP_SSL_ENABLE));
        props.put(ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey(), configService.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST));

        props.put(ConfigEntry.MAIL_DEBUG.getKey(), configService.getString(ConfigEntry.MAIL_DEBUG));

        return mailSender;
    }
}
