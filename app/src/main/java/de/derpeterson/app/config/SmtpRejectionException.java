package de.derpeterson.app.config;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;

/** Explizite negative SMTP-Antwort für genau die eine versandte Nachricht. */
public final class SmtpRejectionException extends MailException {
  private final boolean temporaryRejection;

  private SmtpRejectionException(int responseCode, MailSendException originalFailure) {
    super("SMTP server explicitly rejected the message: code=" + responseCode, originalFailure);
    temporaryRejection = responseCode < 500;
  }

  public boolean isTemporaryRejection() {
    return temporaryRejection;
  }

  public static SmtpRejectionException classify(
      MailSendException mailSendFailure, MimeMessage mimeMessage) throws MessagingException {
    if (mailSendFailure.getFailedMessages().size() != 1
        || !(mailSendFailure.getFailedMessages().get(mimeMessage)
            instanceof SendFailedException sendFailure)
        || sendFailure.getValidSentAddresses() != null
            && sendFailure.getValidSentAddresses().length > 0) {
      return null;
    }
    Address[] recipientAddresses = mimeMessage.getAllRecipients();
    if (recipientAddresses == null || recipientAddresses.length != 1) {
      return null;
    }

    // Keine Auswertung freier Fehlertexte. Teilversand, EOF und unbekannte Antworten bleiben
    // unklar.
    if (sendFailure instanceof SMTPSendFailedException smtpFailure
        && isNegativeResponse(smtpFailure.getReturnCode())
        && isMessageCommand(smtpFailure.getCommand())) {
      return new SmtpRejectionException(smtpFailure.getReturnCode(), mailSendFailure);
    }
    Set<Exception> visitedFailures = Collections.newSetFromMap(new IdentityHashMap<>());
    Exception nestedFailure = sendFailure.getNextException();
    while (nestedFailure instanceof MessagingException messagingFailure
        && visitedFailures.add(nestedFailure)) {
      if (nestedFailure instanceof SMTPAddressFailedException addressFailure
          && recipientAddresses[0].equals(addressFailure.getAddress())
          && addressFailure.getCommand() != null
          && addressFailure.getCommand().startsWith("RCPT TO:")
          && isNegativeResponse(addressFailure.getReturnCode())) {
        return new SmtpRejectionException(addressFailure.getReturnCode(), mailSendFailure);
      }
      nestedFailure = messagingFailure.getNextException();
    }
    return null;
  }

  private static boolean isNegativeResponse(int responseCode) {
    return responseCode >= 400 && responseCode <= 599;
  }

  private static boolean isMessageCommand(String smtpCommand) {
    return smtpCommand != null
        && (smtpCommand.startsWith("MAIL FROM:")
            || smtpCommand.equals("DATA")
            || smtpCommand.equals("."));
  }
}
