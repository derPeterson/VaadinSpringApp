package de.derpeterson.app.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinSession;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.springframework.stereotype.Component;

@Component
public class CustomI18NProvider implements I18NProvider {
  private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  private final Map<Locale, ResourceBundle> messageBundlesByLocale = new HashMap<>();

  public CustomI18NProvider() {
    for (Locale translationLocale : SUPPORTED_LOCALES) {
      messageBundlesByLocale.put(
          translationLocale, ResourceBundle.getBundle("i18n/messages", translationLocale));
    }
  }

  @Override
  public List<Locale> getProvidedLocales() {
    return SUPPORTED_LOCALES;
  }

  @Override
  public String getTranslation(
      String translationKey, Locale translationLocale, Object... formatArguments) {
    ResourceBundle messageBundle =
        messageBundlesByLocale.getOrDefault(
            translationLocale, messageBundlesByLocale.get(DEFAULT_LOCALE));
    if (messageBundle.containsKey(translationKey)) {
      return MessageFormat.format(messageBundle.getString(translationKey), formatArguments);
    }
    return translationKey;
  }

  public String getTranslation(String translationKey, Object... formatArguments) {
    return getTranslation(translationKey, getCurrentLocale(), formatArguments);
  }

  public static Locale getCurrentLocale() {
    if (VaadinSession.getCurrent() != null) {
      return VaadinSession.getCurrent().getLocale();
    } else {
      return DEFAULT_LOCALE;
    }
  }
}
