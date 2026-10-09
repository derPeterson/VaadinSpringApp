package de.derpeterson.app.config;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Records failure at the connection boundary, not from exception text or cause type. */
public class ConnectionAwareJavaMailSender extends JavaMailSenderImpl {

    @Override
    protected Transport connectTransport() throws MessagingException {
        try {
            return super.connectTransport();
        } catch (AuthenticationFailedException e) {
            // Preserve Spring's existing MailAuthenticationException contract.
            throw e;
        } catch (MessagingException e) {
            throw new ConnectionFailure(e);
        }
    }

    /** Only the single message passed by EmailService is eligible for safe retry. */
    public static boolean failedBeforeSending(MailSendException failure, MimeMessage message) {
        return failure.getCause() instanceof ConnectionFailure
                && failure.getFailedMessages().size() == 1
                && failure.getFailedMessages().get(message) == failure.getCause();
    }

    private static final class ConnectionFailure extends MessagingException {
        private ConnectionFailure(MessagingException cause) {
            super("Mail connection failed before sending", cause);
        }
    }
}
