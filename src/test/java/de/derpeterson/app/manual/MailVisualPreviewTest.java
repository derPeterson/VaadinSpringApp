package de.derpeterson.app.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Ausschließlich Offlineprüfungen; auch hier wird niemals ein SMTP-Transport aufgerufen. */
class MailVisualPreviewTest {
  private Map<String, String> settings() {
    return new HashMap<>(
        Map.of(
            "MAIL_PREVIEW_TO", "reader@example.test",
            "MAIL_PREVIEW_FROM", "sender@example.test",
            "MAIL_PREVIEW_SMTP_HOST", "smtp.example.test",
            "MAIL_PREVIEW_SMTP_USERNAME", "fixture-user",
            "MAIL_PREVIEW_SMTP_PASSWORD", "fixture-secret"));
  }

  @Test
  void everyRealHtmlTemplateIsCoveredInBothLanguagesWithoutUnresolvedPlaceholders()
      throws Exception {
    MailVisualPreview preview = MailVisualPreview.prepare(settings()::get);
    assertEquals(13, preview.examples().size());
    Set<String> expected =
        Arrays.stream(
                new PathMatchingResourcePatternResolver().getResources("classpath*:email/*.html"))
            .map(Resource::getFilename)
            .collect(Collectors.toSet());
    Set<String> actual =
        preview.examples().stream()
            .filter(MailVisualPreview.Example::html)
            .map(example -> example.template() + "_" + example.locale().getLanguage() + ".html")
            .collect(Collectors.toSet());
    assertEquals(expected, actual);
    assertEquals(10, actual.size());
    for (MailVisualPreview.Example example : preview.examples()) {
      assertFalse(example.body().contains("{{"));
      assertFalse(example.body().contains("}}"));
      assertFalse(example.subject().isBlank());
      if (example.html()) {
        assertTrue(example.body().contains("cid:"));
        assertTrue(example.body().contains("preview.invalid"));
      }
      if (example.template().equals("welcome")) {
        assertTrue(example.body().contains("Muster &amp; Beispiel"));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en", "de,de", " EN "})
  void oneLanguageSendsSixLocalizedExamplesAndOneEnglishTechnicalAlert(String language) {
    Map<String, String> values = settings();
    values.put("MAIL_PREVIEW_LANGUAGES", language);
    MailVisualPreview preview = MailVisualPreview.prepare(values::get);
    assertEquals(7, preview.examples().size());
    assertEquals("en", preview.examples().getLast().locale().getLanguage());
    assertEquals("dispatch-failure", preview.examples().getLast().template());
    assertEquals(
        language.strip().substring(0, 2).toLowerCase(java.util.Locale.ROOT),
        preview.examples().getFirst().locale().getLanguage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "fr", "de,", "de,en,fr"})
  void unknownOrEmptyLanguageSelectionFailsBeforeSending(String language) {
    Map<String, String> values = settings();
    values.put("MAIL_PREVIEW_LANGUAGES", language);
    assertThrows(IllegalArgumentException.class, () -> MailVisualPreview.prepare(values::get));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "one@example.test,two@example.test",
        "one@example.test\r\nBcc: two@example.test",
        "invalid",
        ""
      })
  void onlyOneExplicitValidRecipientIsAccepted(String recipient) {
    Map<String, String> values = settings();
    values.put("MAIL_PREVIEW_TO", recipient);
    assertThrows(IllegalArgumentException.class, () -> MailVisualPreview.prepare(values::get));
  }

  @Test
  void allMessagesUseTheNormalMimeSenderAndOnlyTheExplicitRecipient() throws Exception {
    MailVisualPreview preview = MailVisualPreview.prepare(settings()::get);
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
    preview.send(sender);
    ArgumentCaptor<MimeMessage> html = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender, times(10)).send(html.capture());
    for (MimeMessage message : html.getAllValues()) {
      message.saveChanges();
      assertEquals(
          "reader@example.test", message.getRecipients(Message.RecipientType.TO)[0].toString());
      assertEquals(1, message.getAllRecipients().length);
      assertEquals("sender@example.test", message.getFrom()[0].toString());
      assertTrue(message.getSubject().startsWith("[SICHTTEST]["));
      assertTrue(message.isMimeType("multipart/related"));
    }
    ArgumentCaptor<SimpleMailMessage> plain = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender, times(3)).send(plain.capture());
    for (SimpleMailMessage message : plain.getAllValues()) {
      assertEquals(List.of("reader@example.test"), Arrays.asList(message.getTo()));
      assertNull(message.getCc());
      assertNull(message.getBcc());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"starttls", "ssl", "none"})
  void transportUsesExplicitTlsModeTimeoutsAndNormalCertificateVerification(String security) {
    Map<String, String> values = settings();
    values.put("MAIL_PREVIEW_SECURITY", security);
    JavaMailSenderImpl sender = MailVisualPreview.prepare(values::get).createSender();
    Properties properties = sender.getJavaMailProperties();
    assertEquals("false", properties.getProperty("mail.debug"));
    assertFalse(properties.containsKey("mail.smtp.ssl.trust"));
    assertEquals("true", properties.getProperty("mail.smtp.ssl.checkserveridentity"));
    assertEquals("10000", properties.getProperty("mail.smtp.connectiontimeout"));
    assertEquals(security.equals("starttls"), properties.get("mail.smtp.starttls.enable"));
    assertEquals(
        Boolean.toString(security.equals("starttls")),
        properties.getProperty("mail.smtp.starttls.required"));
    assertEquals(security.equals("ssl"), properties.get("mail.smtp.ssl.enable"));
  }

  @Test
  void missingConfigurationNeverFallsBackToApplicationOrLocalCredentials() {
    for (String key : settings().keySet()) {
      Map<String, String> values = settings();
      values.remove(key);
      assertThrows(IllegalArgumentException.class, () -> MailVisualPreview.prepare(values::get));
    }
    Map<String, String> relay = settings();
    relay.put("MAIL_PREVIEW_SMTP_AUTH", "false");
    relay.remove("MAIL_PREVIEW_SMTP_PASSWORD");
    relay.remove("MAIL_PREVIEW_SMTP_USERNAME");
    assertEquals(
        false,
        MailVisualPreview.prepare(relay::get)
            .createSender()
            .getJavaMailProperties()
            .get("mail.smtp.auth"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "65536", "not-a-port"})
  void invalidPortsAreRejected(String port) {
    Map<String, String> values = settings();
    values.put("MAIL_PREVIEW_SMTP_PORT", port);
    assertThrows(IllegalArgumentException.class, () -> MailVisualPreview.prepare(values::get));
  }

  @Test
  void transportFailureStopsWithoutRetryOrLeakingTheOriginalMessage() {
    MailVisualPreview preview = MailVisualPreview.prepare(settings()::get);
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("fixture-secret reader@example.test"))
        .when(sender)
        .send(any(MimeMessage.class));
    AssertionError failure = assertThrows(AssertionError.class, () -> preview.send(sender));
    verify(sender).send(any(MimeMessage.class));
    assertFalse(failure.getMessage().contains("fixture-secret"));
    assertFalse(failure.getMessage().contains("reader@example.test"));
    assertNull(failure.getCause());
  }
}
