package de.derpeterson.app.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sichert vollständige Sprachkataloge ohne verwaiste Texte nach einer Bereinigung. */
class MessageCatalogueTest {

  @Test
  void bothLanguagesProvideTheSameMessageKeys() {
    assertEquals(bundle(Locale.GERMAN).keySet(), bundle(Locale.ENGLISH).keySet());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void everyTypedMessageExistsAndEveryBundledKeyIsReachable(String language) throws Exception {
    Locale locale = Locale.forLanguageTag(language);
    ResourceBundle messages = bundle(locale);
    Set<String> requested = new HashSet<>();
    CustomI18NProvider provider = new CustomI18NProvider();
    MessageTexts texts =
        new MessageTexts(
            key -> {
              assertTrue(
                  messages.containsKey(key), () -> language + ": Fehlende Übersetzung: " + key);
              assertFalse(
                  messages.getString(key).isBlank(),
                  () -> language + ": Leere Übersetzung: " + key);
              requested.add(key);
              return provider.getTranslation(key, locale);
            });

    for (Method method : MessageTexts.class.getDeclaredMethods()) {
      if (!method.getName().startsWith("get")
          || method.getName().equals("getTranslation")
          || method.getName().equals("getFormattedTranslation")) {
        continue;
      }
      int variants =
          Arrays.stream(method.getParameterTypes())
              .filter(Class::isEnum)
              .mapToInt(type -> type.getEnumConstants().length)
              .max()
              .orElse(3);
      // Null, eins und mehrere decken Singular/Plural ab; Gruppen benötigen alle Enumwerte.
      for (int variant = 0; variant < variants; variant++) {
        Object result = method.invoke(texts, arguments(method, variant));
        assertFalse(((String) result).isBlank(), method.getName());
      }
    }

    Set<String> orphaned = new HashSet<>(messages.keySet());
    orphaned.removeAll(requested);
    assertEquals(
        Set.of(), orphaned, () -> language + ": Nicht mehr erreichbare Übersetzungseinträge");
  }

  private static ResourceBundle bundle(Locale locale) {
    ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", locale);
    assertEquals(locale.getLanguage(), bundle.getLocale().getLanguage());
    return bundle;
  }

  private static Object[] arguments(Method method, int variant) {
    return Arrays.stream(method.getParameterTypes())
        .map(
            type -> {
              if (type == String.class) {
                return (Object) "test@example.test";
              }
              if (type == int.class) {
                return (Object) variant;
              }
              if (type == long.class) {
                return (Object) (long) variant;
              }
              if (type.isEnum()) {
                return type.getEnumConstants()[variant];
              }
              throw new IllegalArgumentException(
                  "Testargument fehlt für " + method.getName() + ": " + type);
            })
        .toArray();
  }
}
