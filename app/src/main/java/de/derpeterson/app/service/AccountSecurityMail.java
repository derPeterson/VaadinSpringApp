package de.derpeterson.app.service;

import de.derpeterson.app.helper.email.EmailFonts;
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
  private final ConfigService configService;
  private final CustomI18NProvider i18nProvider;

  public MessageProperties messagePropertiesForRecipient(UserEntity recipientUserEntity) {
    return MessageProperties.forLocale(i18nProvider, recipientLocale(recipientUserEntity));
  }

  private Locale recipientLocale(UserEntity recipientUserEntity) {
    return recipientUserEntity.getPreferredLocale() != null
            && "en".equals(recipientUserEntity.getPreferredLocale().getLanguage())
        ? Locale.ENGLISH
        : Locale.GERMAN;
  }

  public String render(
      String mailTemplateName, UserEntity recipientUserEntity, Map<String, String> templateValues) {
    String serviceName = configService.getString(ConfigEntry.SERVICE_NAME);
    HashMap<String, String> templateReplacements = new HashMap<>(templateValues);

    try {
      templateReplacements.put("SERVICE_LOGO", EmailLogo.validatedContentId());
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read account security mail logo", failure);
    }

    templateReplacements.put("SERVICE_NAME", serviceName);
    templateReplacements.put(
        "INTER_FONT_STYLESHEET",
        EmailFonts.styleSheetUrl(configService.getString(ConfigEntry.BASE_URL)));
    templateReplacements.put(
        "LOGIN_LINK",
        UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
            .pathSegment(AppRoute.LOGIN.getRoute())
            .toUriString());
    templateReplacements.put(
        "COPYRIGHT_TEXT",
        CopyrightTextFormatter.format(
            configService.getString(ConfigEntry.COPYRIGHT_TEXT),
            serviceName,
            Clock.systemDefaultZone(),
            messagePropertiesForRecipient(recipientUserEntity)::getAccountCopyrightTemplate));

    try (InputStream templateInputStream =
        new ClassPathResource(
                "email/"
                    + mailTemplateName
                    + "_"
                    + recipientLocale(recipientUserEntity).getLanguage()
                    + ".html")
            .getInputStream()) {
      String mailBodyHtml = new String(templateInputStream.readAllBytes(), StandardCharsets.UTF_8);

      for (Entry<String, String> templateReplacement : templateReplacements.entrySet()) {
        mailBodyHtml =
            mailBodyHtml.replace(
                "{{" + templateReplacement.getKey() + "}}",
                HtmlUtils.htmlEscape(templateReplacement.getValue()));
      }

      return mailBodyHtml;
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read account security mail template", failure);
    }
  }
}
