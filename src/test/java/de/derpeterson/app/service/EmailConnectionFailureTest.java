package de.derpeterson.app.service;

import de.derpeterson.app.config.ConnectionAwareJavaMailSender;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring send/doSend/connectTransport; only Jakarta Transport is mocked, no network. */
class EmailConnectionFailureTest {
    static class Sender extends ConnectionAwareJavaMailSender {
        final Transport transport = mock(Transport.class);
        @Override protected Transport getTransport(Session session) { return transport; }
    }

    @Test
    void plainSpringAdapterAlsoWrapsConnectionFailureInMailSendException() throws Exception {
        var transport = mock(Transport.class);
        var sender = new JavaMailSenderImpl() {
            @Override protected Transport getTransport(Session session) { return transport; }
        };
        var cause = new MessagingException("arbitrary", new ConnectException("fixture"));
        doThrow(cause).when(transport).connect(nullable(String.class), anyInt(), nullable(String.class), nullable(String.class));
        var message = sender.createMimeMessage();
        var failure = assertThrows(MailSendException.class, () -> sender.send(message));
        assertSame(cause, failure.getCause());
        assertSame(cause, failure.getFailedMessages().get(message));
        assertFalse(ConnectionAwareJavaMailSender.failedBeforeSending(failure, message));
        verify(transport, never()).sendMessage(any(), any());
    }

    @Test
    void connectionFailureIsTranslatedOnlyForTheExactSingleMessage() throws Exception {
        var sender = new Sender();
        var cause = new MessagingException("unrelated wording", new ConnectException("fixture"));
        doThrow(cause).when(sender.transport).connect(nullable(String.class), anyInt(), nullable(String.class), nullable(String.class));
        var message = sender.createMimeMessage();
        var raw = assertThrows(MailSendException.class, () -> sender.send(message));
        assertTrue(ConnectionAwareJavaMailSender.failedBeforeSending(raw, message));
        assertFalse(ConnectionAwareJavaMailSender.failedBeforeSending(raw, sender.createMimeMessage()));
        var multiple = assertThrows(MailSendException.class, () -> sender.send(message, sender.createMimeMessage()));
        assertFalse(ConnectionAwareJavaMailSender.failedBeforeSending(multiple, message));
        var translated = assertThrows(MailPreparationException.class, () -> service(sender).sendEmail(user(), "subject", "body"));
        assertInstanceOf(MailSendException.class, translated.getCause());
        assertSame(cause, translated.getCause().getCause().getCause());
        verify(sender.transport, never()).sendMessage(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"send", "close", "sendAndClose"})
    void sendAndCloseFailuresRemainUnknownEvenWithConnectionErrorCauseAndText(String stage) throws Exception {
        var sender = new Sender();
        // Deliberately looks like a connect error, but occurs after connection establishment.
        var cause = new MessagingException("Mail server connection failed", new ConnectException("fixture"));
        if (!stage.equals("close")) doThrow(cause).when(sender.transport).sendMessage(any(), any());
        if (!stage.equals("send")) doThrow(cause).when(sender.transport).close();
        var failure = assertThrows(MailSendException.class, () -> service(sender).sendEmail(user(), "subject", "body"));
        assertFalse(ConnectionAwareJavaMailSender.failedBeforeSending(failure, sender.createMimeMessage()));
        verify(sender.transport).sendMessage(any(), any());
    }

    @Test
    void authenticationContractIsPreserved() throws Exception {
        var sender = new Sender();
        var cause = new AuthenticationFailedException("fixture");
        doThrow(cause).when(sender.transport).connect(nullable(String.class), anyInt(), nullable(String.class), nullable(String.class));
        var failure = assertThrows(MailAuthenticationException.class, () -> service(sender).sendEmail(user(), "subject", "body"));
        assertSame(cause, failure.getCause());
        verify(sender.transport, never()).sendMessage(any(), any());
    }

    private EmailService service(Sender sender) {
        var config = mock(ConfigService.class);
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("sender@example.com");
        return new EmailService(config, sender);
    }

    private UserEntity user() { return UserEntity.builder().email("recipient@example.com").build(); }
}
