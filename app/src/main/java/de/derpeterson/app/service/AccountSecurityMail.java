package de.derpeterson.app.service;

import de.derpeterson.app.helper.email.EmailLogo;
import de.derpeterson.app.helper.text.CopyrightTextFormatter;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** Feste Kontosprache und HTML-Escaping, unabhängig von Vaadin-/Schedulerthreads. */
@Component
@RequiredArgsConstructor
public class AccountSecurityMail {
  private final ConfigService config;
  private final CustomI18NProvider i18n;

  public MessageProperties texts(UserEntity user) {
    return MessageProperties.forLocale(i18n, locale(user));
  }

  private Locale locale(UserEntity user) {
    return user.getPreferredLocale() != null && "en".equals(user.getPreferredLocale().getLanguage())
        ? Locale.ENGLISH
        : Locale.GERMAN;
  }

  public String render(String template, UserEntity user, Map<String, String> values) {
    String service = config.getString(ConfigEntry.SERVICE_NAME);
    HashMap<String, String> replacements = new HashMap<>(values);
    try {
      replacements.put("SERVICE_LOGO", EmailLogo.validatedContentId());
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read account security mail logo", failure);
    }
    replacements.put("SERVICE_NAME", service);
    replacements.put(
        "LOGIN_LINK",
        UriComponentsBuilder.fromUriString(config.getString(ConfigEntry.BASE_URL))
            .pathSegment(AppRoute.LOGIN.getRoute())
            .toUriString());
    replacements.put(
        "COPYRIGHT_TEXT",
        CopyrightTextFormatter.format(
            config.getString(ConfigEntry.COPYRIGHT_TEXT),
            service,
            Clock.systemDefaultZone(),
            texts(user)::getAccountCopyrightTemplate));
    try (InputStream input =
        new ClassPathResource("email/" + template + "_" + locale(user).getLanguage() + ".html")
            .getInputStream()) {
      String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      for (Entry<String, String> entry : replacements.entrySet()) {
        html = html.replace("{{" + entry.getKey() + "}}", HtmlUtils.htmlEscape(entry.getValue()));
      }
      return html;
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read account security mail template", failure);
    }
  }
}
