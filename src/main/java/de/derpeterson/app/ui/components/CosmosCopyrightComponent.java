package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.text.CopyrightTextFormatter;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * Gemeinsame sichere Fußzeile; gespeicherte Texte bleiben erhalten, der Standard folgt der Sprache.
 */
public class CosmosCopyrightComponent extends Span {
  private transient Registration languageRegistration;

  public CosmosCopyrightComponent(
      ConfigService config, Clock clock, Supplier<String> localizedDefaultTemplate) {
    addClassName("cosmos-footer");
    String serviceName = config.getString(ConfigEntry.SERVICE_NAME);
    String template = config.getString(ConfigEntry.COPYRIGHT_TEXT);
    Runnable updateCopyright =
        () -> {
          String copyright =
              CopyrightTextFormatter.format(template, serviceName, clock, localizedDefaultTemplate);
          // Plain text statt HTML: auch eine bearbeitete Konfiguration bleibt sicherer Text.
          setText(copyright);
          setVisible(!copyright.isBlank());
        };
    updateCopyright.run();
    if (localizedDefaultTemplate != null) {
      addAttachListener(
          event -> {
            updateCopyright.run();
            languageRegistration =
                ComponentUtil.addListener(
                    event.getUI(),
                    LanguageChangeEvent.class,
                    languageEvent -> updateCopyright.run());
          });
      addDetachListener(
          event -> {
            if (languageRegistration != null) {
              languageRegistration.remove();
              languageRegistration = null;
            }
          });
    }
  }
}
