package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mail.MailSendException;

class SmtpRejectionExceptionTest {
  @ParameterizedTest
  @CsvSource({
    "MAIL FROM:<sender@example.test>,451,true",
    "DATA,450,true",
    ".,550,false",
    ".,421,true",
    "DATA,554,false"
  })
  void explicitNegativeResponsesAreClassifiedWithoutReadingTheirText(
      String command, int code, boolean temporary) throws Exception {
    MimeMessage mimeMessage = newMessage();
    SMTPSendFailedException smtpFailure =
        new SMTPSendFailedException(
            command, code, "irrelevant response", null, null, mimeMessage.getAllRecipients(), null);
    MailSendException mailFailure = new MailSendException(Map.of(mimeMessage, smtpFailure));

    SmtpRejectionException rejection = SmtpRejectionException.classify(mailFailure, mimeMessage);

    assertNotNull(rejection);
    assertEquals(temporary, rejection.isTemporaryRejection());
    assertSame(mailFailure, rejection.getCause());
  }

  @Test
  void recipientRejectionIsRecognizedForTheOnlyRecipient() throws Exception {
    MimeMessage mimeMessage = newMessage();
    SMTPAddressFailedException addressFailure =
        new SMTPAddressFailedException(
            (InternetAddress) mimeMessage.getAllRecipients()[0],
            "RCPT TO:<recipient@example.test>",
            550,
            "ignored");
    SendFailedException sendFailure =
        new SendFailedException(
            "ignored", addressFailure, null, null, mimeMessage.getAllRecipients());

    SmtpRejectionException rejection =
        SmtpRejectionException.classify(
            new MailSendException(Map.of(mimeMessage, sendFailure)), mimeMessage);

    assertNotNull(rejection);
    assertEquals(false, rejection.isTemporaryRejection());
  }

  @Test
  void lostReplyAndPartialDeliveryRemainUncertain() throws Exception {
    MimeMessage mimeMessage = newMessage();
    Address[] recipients = mimeMessage.getAllRecipients();
    for (SMTPSendFailedException smtpFailure :
        new SMTPSendFailedException[] {
          new SMTPSendFailedException(".", -1, "550 fake error text", null, null, recipients, null),
          new SMTPSendFailedException("QUIT", 550, "ignored", null, null, recipients, null),
          new SMTPSendFailedException(".", 550, "ignored", null, recipients, null, null)
        }) {
      assertNull(
          SmtpRejectionException.classify(
              new MailSendException(Map.of(mimeMessage, smtpFailure)), mimeMessage));
    }
    assertNull(
        SmtpRejectionException.classify(new MailSendException("451 fake text"), mimeMessage));
  }

  private MimeMessage newMessage() throws Exception {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    mimeMessage.setRecipient(
        Message.RecipientType.TO, new InternetAddress("recipient@example.test"));
    return mimeMessage;
  }
}
