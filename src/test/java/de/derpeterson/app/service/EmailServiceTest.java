package de.derpeterson.app.service;

import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMailMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real in-memory MIME messages; sender/config mocked, no Spring startup, DB or SMTP. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    private static final String RECIPIENT = "recipient@example.com";
    private static final String FROM = "sender@example.com";
    @Mock
    private ConfigService config;
    @Mock
    private JavaMailSender sender;
    private EmailService service;

    @BeforeEach
    void setup() {
        service = new EmailService(config, sender);
    }

    private UserEntity user(String email) {
        return UserEntity.builder().email(email).build();
    }

    private MimeMessage prepareMime() {
        var message = spy(new MimeMessage(Session.getInstance(new Properties())));
        when(sender.createMimeMessage()).thenReturn(message);
        return message;
    }

    private MimeMessage sentMime(MimeMessage expected) {
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).createMimeMessage();
        verify(sender).send(captured.capture());
        verifyNoMoreInteractions(sender);
        assertSame(expected, captured.getValue());
        return captured.getValue();
    }

    private SimpleMailMessage sentSimple() {
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captured.capture());
        verifyNoMoreInteractions(sender);
        return captured.getValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Normal subject", "Grüße – 密码 🔐", ""})
    void htmlMailRoundTripsRealMimeHeadersBodyAndUtf8(String subject) throws Exception {
        var original = prepareMime();
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        String body = "<html><body>Grüße &amp; 密码 🔐\r\n<a href=\"https://example.com/reset?t=abc\">Link</a></body></html>";

        service.sendEmail(user(RECIPIENT), subject, body);

        var message = roundTrip(sentMime(original));
        assertEnvelope(message, RECIPIENT, FROM, subject);
        assertFalse(message.isMimeType("multipart/*"));
        assertEquals(1, message.getHeader("Content-Type").length);
        assertHtml(message, body);
        verify(original, never()).addHeader(anyString(), anyString());
        if (!subject.isEmpty() && !subject.equals("Normal subject")) {
            assertTrue(message.getHeader("Subject", null).toLowerCase().contains("=?utf-8?"));
        }
        verify(config).getString(ConfigEntry.EMAIL_FROM);
        verifyNoMoreInteractions(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain-looking text", "<b>Ä € 日本語 😀</b>", "<p>"})
    void htmlBodyIsPreservedEvenIfEmptyOrNotValidHtml(String body) throws Exception {
        var original = prepareMime();
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        service.sendEmail(user(RECIPIENT), "subject", body);
        var message = roundTrip(sentMime(original));
        assertHtml(message, body);
    }

    @Test
    void namedAddressesRoundTripWithoutAddingCcBccOrReplyTo() throws Exception {
        var original = prepareMime();
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("Sender <sender@example.com>");
        service.sendEmail(user("Recipient <recipient@example.com>"), "subject", "<p>body</p>");
        var message = roundTrip(sentMime(original));
        assertEnvelope(message, RECIPIENT, FROM, "subject");
        assertEquals("Recipient", ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getPersonal());
        assertEquals("Sender", ((InternetAddress) message.getFrom()[0]).getPersonal());
        assertNull(message.getHeader("Reply-To"));
    }

    @Test
    void htmlAddressParsingDoesNotEnforceAnInternetDomain() throws Exception {
        var message = prepareMime();
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("local-sender");
        service.sendEmail(user("local-recipient"), "subject", "body");
        assertEnvelope(roundTrip(sentMime(message)), "local-recipient", "local-sender", "subject");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Normal subject", "Grüße – 密码 🔐", ""})
    void adminMailPreservesAllFieldsAndUnicodeAsPlainText(String subject) {
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        String body = "Grüße – 密码 🔐\nFailure details\n<b>literal markup</b>";
        service.sendAdminEmail(RECIPIENT, subject, body);
        var message = sentSimple();
        assertArrayEquals(new String[]{RECIPIENT}, message.getTo());
        assertEquals(FROM, message.getFrom());
        assertEquals(subject, message.getSubject());
        assertEquals(body, message.getText());
        assertNull(message.getCc());
        assertNull(message.getBcc());
        assertNull(message.getReplyTo());
        verify(config).getString(ConfigEntry.EMAIL_FROM);
        verifyNoMoreInteractions(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Normal subject", "Grüße – 密码 🔐", ""})
    void adminMailRoundTripsWithTheProductionSenderAndRealSpringAdapter(String subject) throws Exception {
        var realSender = configuredSender();
        assertEquals("UTF-8", realSender.getDefaultEncoding());
        // Same conversion as JavaMailSenderImpl.send(SimpleMailMessage...), no transport.
        var mime = realSender.createMimeMessage();
        var adapter = new MimeMailMessage(mime);
        doAnswer(call -> {
            call.getArgument(0, SimpleMailMessage.class).copyTo(adapter);
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        String body = "Ä € 日本語 😀\r\n<b>literal markup</b>";
        service.sendAdminEmail(RECIPIENT, subject, body);
        var simple = sentSimple();
        assertEquals(body, simple.getText());
        var message = roundTrip(mime);
        assertEnvelope(message, RECIPIENT, FROM, subject);
        assertTrue(message.isMimeType("text/plain"));
        assertEquals("UTF-8", new ContentType(message.getContentType()).getParameter("charset"));
        assertEquals(body, message.getContent());
        assertEquals(1, message.getHeader("Content-Type").length);
        assertFalse(message.isMimeType("multipart/*"));
        if (subject.contains("Grüße")) {
            assertTrue(message.getHeader("Subject", null).toLowerCase().contains("=?utf-8?"));
        }
    }

    private JavaMailSenderImpl configuredSender() {
        when(config.getString(ConfigEntry.MAIL_HOST)).thenReturn("smtp.invalid");
        when(config.getString(ConfigEntry.MAIL_USERNAME)).thenReturn("fixture-user");
        when(config.getString(ConfigEntry.MAIL_PASSWORD)).thenReturn("fixture-only-not-a-secret");
        when(config.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST)).thenReturn("smtp.invalid");
        when(config.getString(ConfigEntry.MAIL_DEBUG)).thenReturn("false");
        return (JavaMailSenderImpl) new MailConfig(config).javaMailSender();
    }

    @ParameterizedTest
    @ValueSource(strings = {"recipient", "from"})
    void realAdminAdapterWrapsAddressErrorsAsMailParseException(String invalidField) {
        var realSender = configuredSender();
        var adapter = new MimeMailMessage(realSender.createMimeMessage());
        doAnswer(call -> {
            call.getArgument(0, SimpleMailMessage.class).copyTo(adapter);
            return null;
        }).when(sender).send(any(SimpleMailMessage.class));
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(invalidField.equals("from") ? "bad@@example.com" : FROM);
        var failure = assertThrows(MailParseException.class, () -> service.sendAdminEmail(
                invalidField.equals("recipient") ? "bad@@example.com" : RECIPIENT, "subject", "body"));
        assertInstanceOf(MessagingException.class, failure.getCause());
        sentSimple();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "first\r\nsecond", "<b>Ä € 日本語 😀</b>"})
    void adminMailForwardsBodyWithoutValidationOrHtmlInterpretation(String body) {
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        service.sendAdminEmail(RECIPIENT, "subject", body);
        assertEquals(body, sentSimple().getText());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bad@@example.com"})
    void adminMailForwardsUnvalidatedRecipientAndFrom(String address) {
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(address);
        service.sendAdminEmail(address, null, "body");
        var message = sentSimple();
        assertArrayEquals(new String[]{address}, message.getTo());
        assertEquals(address, message.getFrom());
        assertNull(message.getSubject());
    }

    @Test
    void subsequentCallsCreateIndependentMessagesAndReadCurrentFrom() throws Exception {
        var first = new MimeMessage(Session.getInstance(new Properties()));
        var second = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(first, second);
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM, "other@example.com", FROM, "other@example.com");
        service.sendEmail(user(RECIPIENT), "first", "one");
        service.sendEmail(user("other-recipient@example.com"), "second", "two");
        service.sendAdminEmail(RECIPIENT, "first", "one");
        service.sendAdminEmail("other-recipient@example.com", "second", "two");
        assertEnvelope(roundTrip(first), RECIPIENT, FROM, "first");
        assertEnvelope(roundTrip(second), "other-recipient@example.com", "other@example.com", "second");
        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(2)).send(messages.capture());
        var adminFirst = messages.getAllValues().get(0);
        var adminSecond = messages.getAllValues().get(1);
        assertNotSame(adminFirst, adminSecond);
        assertEquals(FROM, adminFirst.getFrom());
        assertEquals("other@example.com", adminSecond.getFrom());
        assertEquals("first", adminFirst.getSubject());
        assertEquals("one", adminFirst.getText());
        assertArrayEquals(new String[]{RECIPIENT}, adminFirst.getTo());
        assertEquals("second", adminSecond.getSubject());
        assertEquals("two", adminSecond.getText());
        assertArrayEquals(new String[]{"other-recipient@example.com"}, adminSecond.getTo());
        verify(sender, times(2)).createMimeMessage();
        verify(sender).send(first);
        verify(sender).send(second);
        verifyNoMoreInteractions(sender);
        verify(config, times(4)).getString(ConfigEntry.EMAIL_FROM);
    }

    static Stream<Arguments> sendFailures() {
        return Stream.of(false, true).flatMap(html -> Stream.of(
                new MailSendException("controlled send failure"),
                new MailAuthenticationException("controlled auth failure"),
                new MailParseException("controlled parse failure"))
                .map(failure -> Arguments.of(html, failure)));
    }

    @ParameterizedTest
    @MethodSource("sendFailures")
    void senderFailuresPropagateUnchangedWithoutRetry(boolean html, MailException failure) throws Exception {
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
        if (html) {
            var message = prepareMime();
            doThrow(failure).when(sender).send(message);
            assertSame(failure, assertThrows(MailException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body")));
            sentMime(message);
        } else {
            doThrow(failure).when(sender).send(any(SimpleMailMessage.class));
            assertSame(failure, assertThrows(MailException.class, () -> service.sendAdminEmail(RECIPIENT, "subject", "body")));
            assertEquals("body", sentSimple().getText());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void configFailuresPropagateBeforeSend(boolean html) {
        var failure = new IllegalStateException("controlled config failure");
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenThrow(failure);
        if (html) {
            prepareMime();
            assertSame(failure, assertThrows(IllegalStateException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body")));
            verify(sender).createMimeMessage();
        } else {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> service.sendAdminEmail(RECIPIENT, "subject", "body")));
        }
        verifyNoMoreInteractions(sender);
    }

    @Test
    void mimeCreationFailurePropagatesWithoutReadingConfigOrSending() {
        var failure = new MailParseException("controlled creation failure");
        when(sender.createMimeMessage()).thenThrow(failure);
        assertSame(failure, assertThrows(MailParseException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body")));
        verify(sender).createMimeMessage();
        verifyNoMoreInteractions(sender);
        verifyNoInteractions(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"content", "subject", "from"})
    void checkedMimeConstructionFailuresPropagateWithoutSending(String stage) throws Exception {
        var message = spy(new MimeMessage(Session.getInstance(new Properties())));
        when(sender.createMimeMessage()).thenReturn(message);
        var failure = new MessagingException("controlled " + stage + " failure");
        switch (stage) {
            case "content" -> doThrow(failure).when(message).setContent("body", "text/html;charset=UTF-8");
            case "subject" -> doThrow(failure).when(message).setSubject("subject", "UTF-8");
            case "from" -> {
                when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(FROM);
                doThrow(failure).when(message).setFrom(any(Address.class));
            }
            default -> fail("unknown fixture stage");
        }
        assertSame(failure, assertThrows(MessagingException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body")));
        verify(sender).createMimeMessage();
        verifyNoMoreInteractions(sender);
        if (!stage.equals("from")) {
            verifyNoInteractions(config);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"bad@@example.com", "a@example.com,b@example.com"})
    void invalidHtmlRecipientFailsBeforeConfigAndSend(String address) {
        prepareMime();
        if (address == null) {
            assertThrows(IllegalArgumentException.class, () -> service.sendEmail(user(null), "subject", "body"));
        } else {
            assertThrows(MessagingException.class, () -> service.sendEmail(user(address), "subject", "body"));
        }
        verify(sender).createMimeMessage();
        verifyNoMoreInteractions(sender);
        verifyNoInteractions(config);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"bad@@example.com", "a@example.com,b@example.com"})
    void invalidHtmlFromFailsBeforeSend(String address) {
        prepareMime();
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(address);
        if (address == null) {
            assertThrows(IllegalArgumentException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body"));
        } else {
            assertThrows(MessagingException.class, () -> service.sendEmail(user(RECIPIENT), "subject", "body"));
        }
        verify(sender).createMimeMessage();
        verifyNoMoreInteractions(sender);
        verify(config).getString(ConfigEntry.EMAIL_FROM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "subject", "body"})
    void nullHtmlArgumentsFailWithoutSendOrConfigRead(String argument) {
        prepareMime();
        Class<? extends RuntimeException> expected = argument.equals("user") ? NullPointerException.class : IllegalArgumentException.class;
        assertThrows(expected, () -> service.sendEmail(argument.equals("user") ? null : user(RECIPIENT),
                argument.equals("subject") ? null : "subject", argument.equals("body") ? null : "body"));
        verify(sender).createMimeMessage();
        verifyNoMoreInteractions(sender);
        verifyNoInteractions(config);
    }

    private MimeMessage roundTrip(MimeMessage message) throws Exception {
        try (var bytes = new ByteArrayOutputStream()) {
            message.saveChanges();
            message.writeTo(bytes);
            try (var input = new ByteArrayInputStream(bytes.toByteArray())) {
                return new MimeMessage(Session.getInstance(new Properties()), input);
            }
        }
    }

    private void assertEnvelope(MimeMessage message, String to, String from, String subject) throws Exception {
        assertEquals(1, message.getAllRecipients().length);
        assertEquals(to, ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertEquals(1, message.getFrom().length);
        assertEquals(from, ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals(subject, message.getSubject());
        assertNull(message.getRecipients(Message.RecipientType.CC));
        assertNull(message.getRecipients(Message.RecipientType.BCC));
    }

    private void assertHtml(Part part, String expected) throws Exception {
        assertTrue(part.isMimeType("text/html"));
        var type = new ContentType(part.getContentType());
        assertEquals(StandardCharsets.UTF_8.name(), type.getParameter("charset"));
        assertEquals(expected, part.getContent());
        assertNull(part.getDisposition());
        assertNull(part.getFileName());
    }
}
