package de.derpeterson.app.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.*;

@Component
public class CustomI18NProvider implements I18NProvider {
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final Map<Locale, ResourceBundle> bundles = new HashMap<>();

    public CustomI18NProvider() {
        for (Locale locale : SUPPORTED_LOCALES) {
            bundles.put(locale, ResourceBundle.getBundle("i18n/messages", locale));
        }
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = bundles.getOrDefault(locale, bundles.get(DEFAULT_LOCALE));
        if (bundle.containsKey(key)) {
            return MessageFormat.format(bundle.getString(key), params);
        }
        return key;
    }

    public String getTranslation(String key, Object... params) {
        return getTranslation(key, getCurrentLocale(), params);
    }

    public static Locale getCurrentLocale() {
        if (VaadinSession.getCurrent() != null) {
            return VaadinSession.getCurrent().getLocale();
        } else {
            return DEFAULT_LOCALE;
        }
    }
}
