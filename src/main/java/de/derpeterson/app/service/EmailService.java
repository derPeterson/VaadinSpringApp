package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final ConfigService configService;

    private final JavaMailSender mailSender;

    @Async("emailTaskExecutor")
    public void sendEmail(UserEntity userEntity, String subject, String body) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEntity.getEmail());
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

        mailSender.send(message);
    }

    @Async("emailTaskExecutor")
    public void sendAdminEmail(String adminEmail, String subject, String body) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(configService.getString(ConfigEntry.EMAIL_FROM));

        mailSender.send(message);
    }
}
