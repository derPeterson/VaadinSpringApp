package de.derpeterson.app.manual;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.AccountSecurityMail;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.mail.MessagingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Nur manuell gestartete Mailansicht; ohne Spring-Kontext, Datenbank, Queue oder gültige Tokens.
 */
final class MailVisualPreview {
  private static final String BASE_URL = "https://preview.invalid/";
  private final ConfigService configuration;
  private final String recipient;
  private final List<Example> examples;

  record Example(String template, Locale locale, String subject, String body, boolean html) {}

  private MailVisualPreview(ConfigService configuration, String recipient, List<Example> examples) {
    this.configuration = configuration;
    this.recipient = recipient;
    this.examples = List.copyOf(examples);
  }

  static MailVisualPreview prepare(Function<String, String> environment) {
    String recipient = address(required(environment, "TO"));
    String from = address(required(environment, "FROM"));
    List<Locale> languages = languages(value(environment, "LANGUAGES", "de,en"));
    String host = required(environment, "SMTP_HOST");
    int port = port(value(environment, "SMTP_PORT", "587"));
    String security = value(environment, "SECURITY", "starttls");
    if (!List.of("starttls", "ssl", "none").contains(security)) {
      throw new IllegalArgumentException("MAIL_PREVIEW_SECURITY: starttls, ssl oder none wählen.");
    }
    String authValue = value(environment, "SMTP_AUTH", "true");
    if (!List.of("true", "false").contains(authValue)) {
      throw new IllegalArgumentException("MAIL_PREVIEW_SMTP_AUTH: true oder false wählen.");
    }
    boolean auth = Boolean.parseBoolean(authValue);
    String username = auth ? required(environment, "SMTP_USERNAME") : "";
    String password = auth ? required(environment, "SMTP_PASSWORD") : "";

    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("VaadinSpringApp");
    when(config.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("© {year} {serviceName}");
    when(config.getString(ConfigEntry.BASE_URL)).thenReturn(BASE_URL);
    when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn(from);
    when(config.getString(ConfigEntry.MAIL_HOST)).thenReturn(host);
    when(config.getInteger(ConfigEntry.MAIL_PORT)).thenReturn(port);
    when(config.getString(ConfigEntry.MAIL_USERNAME)).thenReturn(username);
    when(config.getString(ConfigEntry.MAIL_PASSWORD)).thenReturn(password);
    when(config.getBoolean(ConfigEntry.MAIL_SMTP_AUTH)).thenReturn(auth);
    when(config.getBoolean(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE))
        .thenReturn("starttls".equals(security));
    when(config.getBoolean(ConfigEntry.MAIL_SMTP_SSL_ENABLE)).thenReturn("ssl".equals(security));
    when(config.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST)).thenReturn(host);
    when(config.getString(ConfigEntry.MAIL_DEBUG)).thenReturn("false");
    return new MailVisualPreview(config, recipient, examples(config, languages));
  }

  private static List<Example> examples(ConfigService configuration, List<Locale> languages) {
    AccountSecurityMail renderer = new AccountSecurityMail(configuration, new CustomI18NProvider());
    List<Example> examples = new ArrayList<>();
    for (Locale locale : languages) {
      UserEntity user =
          UserEntity.builder()
              .firstName("Alex")
              .lastName("Muster & Beispiel")
              .email("alex@example.invalid")
              .preferredLocale(locale)
              .build();
      MessageProperties texts = renderer.texts(user);
      examples.add(
          html(
              renderer,
              user,
              "welcome",
              texts.getEmailVerificationSubject(),
              Map.of(
                  "FIRST_NAME",
                  user.getFirstName(),
                  "LAST_NAME",
                  user.getLastName(),
                  "VERIFICATION_LINK",
                  link(AppRoute.VERIFICATION))));
      examples.add(
          html(
              renderer,
              user,
              "reset_password",
              texts.getEmailResetPasswordSubject(),
              Map.of("RESET_PASSWORD_LINK", link(AppRoute.RESET_PASSWORD))));
      examples.add(
          html(
              renderer,
              user,
              "password_changed",
              texts.getAccountPasswordChangedSubject(),
              Map.of()));
      examples.add(
          html(
              renderer,
              user,
              "email_change_request",
              texts.getAccountEmailChangeSubject(),
              Map.of(
                  "CONFIRM_EMAIL_LINK",
                  link(AppRoute.EMAIL_CHANGE),
                  "NEW_EMAIL",
                  "new@example.invalid")));
      examples.add(
          html(
              renderer,
              user,
              "email_changed",
              texts.getAccountEmailChangedSubject(),
              Map.of("OLD_EMAIL", "old@example.invalid", "NEW_EMAIL", "new@example.invalid")));
      examples.add(
          new Example(
              "admin-test",
              locale,
              texts.getAdminMailDefaultSubject(),
              texts.getAdminMailDefaultBody(),
              false));
    }
    // Der technische Fehlerhinweis der Queue ist in der Anwendung ausschließlich englisch.
    // Seine Beispieldaten lösen weder einen Queuefehler noch einen erneuten Versandversuch aus.
    examples.add(
        new Example(
            "dispatch-failure",
            Locale.ENGLISH,
            "Email dispatch failed",
            """
            The email to alex@example.invalid with the subject 'Example' could not be delivered after 3 attempts.

            Error message: Example SMTP failure
            Email type: VERIFICATION
            Sending time: 2026-01-01T12:00:00""",
            false));
    return examples;
  }

  private static Example html(
      AccountSecurityMail renderer,
      UserEntity user,
      String template,
      String subject,
      Map<String, String> values) {
    String body = renderer.render(template, user, values);
    if (body.contains("{{") || body.contains("}}")) {
      throw new IllegalStateException("Unvollständige Beispielvorlage: " + template);
    }
    return new Example(template, user.getPreferredLocale(), subject, body, true);
  }

  private static String link(AppRoute route) {
    return UriComponentsBuilder.fromUriString(BASE_URL)
        .pathSegment(route.getRoute(), "visual-preview-not-a-real-token")
        .toUriString();
  }

  List<Example> examples() {
    return examples;
  }

  JavaMailSenderImpl createSender() {
    JavaMailSenderImpl sender = (JavaMailSenderImpl) new MailConfig(configuration).javaMailSender();
    sender.getJavaMailProperties().remove(ConfigEntry.MAIL_SMTP_SSL_TRUST.getKey());
    sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "10000");
    sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "10000");
    sender.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "10000");
    // STARTTLS darf bei einem unpassenden Server nicht still auf Klartext zurückfallen.
    sender
        .getJavaMailProperties()
        .setProperty(
            "mail.smtp.starttls.required",
            Boolean.toString(configuration.getBoolean(ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE)));
    sender.getJavaMailProperties().setProperty("mail.smtp.ssl.checkserveridentity", "true");
    return sender;
  }

  void send(JavaMailSender sender) {
    EmailService service = new EmailService(configuration, sender);
    for (Example example : examples) {
      String subject =
          "[SICHTTEST]["
              + example.locale().getLanguage().toUpperCase(Locale.ROOT)
              + "] "
              + example.subject();
      try {
        if (example.html()) {
          service.sendEmailTo(recipient, subject, example.body());
        } else {
          service.sendAdminEmail(recipient, subject, example.body());
        }
      } catch (MessagingException | MailException failure) {
        // SMTP-Exceptiontexte können Adresse/Zugangsdaten enthalten; nicht in Testreports
        // übernehmen.
        throw new AssertionError(
            "Versand abgebrochen bei "
                + example.template()
                + "/"
                + example.locale().getLanguage()
                + " ("
                + failure.getClass().getSimpleName()
                + "). Bereits versandte Beispiele im Postfach prüfen; kein automatischer Retry.");
      }
    }
  }

  private static List<Locale> languages(String selected) {
    LinkedHashSet<Locale> locales = new LinkedHashSet<>();
    for (String entry : selected.split(",", -1)) {
      String language = entry.strip().toLowerCase(Locale.ROOT);
      if (!List.of("de", "en").contains(language)) {
        throw new IllegalArgumentException("MAIL_PREVIEW_LANGUAGES: de, en oder de,en wählen.");
      }
      locales.add(Locale.forLanguageTag(language));
    }
    return List.copyOf(locales);
  }

  private static int port(String selected) {
    try {
      int port = Integer.parseInt(selected);
      if (port >= 1 && port <= 65535) {
        return port;
      }
    } catch (NumberFormatException _) {
      // Keine rohen Konfigurationswerte in Fehlerausgaben übernehmen.
    }
    throw new IllegalArgumentException("MAIL_PREVIEW_SMTP_PORT muss zwischen 1 und 65535 liegen.");
  }

  private static String address(String selected) {
    if (!UserInputRules.isEmailValid(selected)) {
      throw new IllegalArgumentException(
          "MAIL_PREVIEW_TO und FROM benötigen jeweils genau eine gültige E-Mail-Adresse.");
    }
    return selected;
  }

  private static String required(Function<String, String> environment, String key) {
    String value = environment.apply("MAIL_PREVIEW_" + key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("MAIL_PREVIEW_" + key + " fehlt.");
    }
    return value;
  }

  private static String value(Function<String, String> environment, String key, String fallback) {
    String value = environment.apply("MAIL_PREVIEW_" + key);
    return value == null ? fallback : value;
  }
}
