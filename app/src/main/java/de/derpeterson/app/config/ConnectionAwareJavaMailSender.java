package de.derpeterson.app.config;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.util.function.Supplier;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Records failure at the connection boundary, not from exception text or cause type. */
public class ConnectionAwareJavaMailSender extends JavaMailSenderImpl {

  private final Supplier<ConnectionAwareJavaMailSender> configuredMailSenderSupplier;

  public ConnectionAwareJavaMailSender() {
    configuredMailSenderSupplier = null;
  }

  ConnectionAwareJavaMailSender(
      Supplier<ConnectionAwareJavaMailSender> configuredMailSenderSupplier) {
    this.configuredMailSenderSupplier = configuredMailSenderSupplier;
  }

  @Override
  protected Transport connectTransport() throws MessagingException {
    if (configuredMailSenderSupplier != null) {
      try {
        ConnectionAwareJavaMailSender configuredMailSender = configuredMailSenderSupplier.get();
        configuredMailSender.getJavaMailProperties().putAll(getJavaMailProperties());
        return configuredMailSender.connectTransport();
      } catch (RuntimeException mailConnectionFailure) {
        throw new ConnectionFailure(
            new MessagingException("SMTP settings unavailable", mailConnectionFailure));
      }
    }

    try {
      return super.connectTransport();
    } catch (AuthenticationFailedException authenticationFailure) {
      // Preserve Spring's existing MailAuthenticationException contract.
      throw authenticationFailure;
    } catch (MessagingException transportFailure) {
      throw new ConnectionFailure(transportFailure);
    }
  }

  /** Only the single message passed by EmailService is eligible for safe retry. */
  public static boolean failedBeforeSending(
      MailSendException mailSendFailure, MimeMessage mimeMessage) {
    return mailSendFailure.getCause() instanceof ConnectionFailure
        && mailSendFailure.getFailedMessages().size() == 1
        && mailSendFailure.getFailedMessages().get(mimeMessage) == mailSendFailure.getCause();
  }

  private static final class ConnectionFailure extends MessagingException {
    private ConnectionFailure(MessagingException messagingCause) {
      super("Mail connection failed before sending", messagingCause);
    }
  }
}
