package de.derpeterson.app.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
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
  void everyTypedMessageExistsAndEveryBundledKeyIsReachable(String languageTag) throws Exception {
    Locale translationLocale = Locale.forLanguageTag(languageTag);
    ResourceBundle messageBundle = bundle(translationLocale);
    Set<String> requestedTranslationKeys = new HashSet<>();
    CustomI18NProvider i18nProvider = new CustomI18NProvider();
    MessageTexts messageTexts =
        new MessageTexts(translationKey -> {
          assertTrue(
              messageBundle.containsKey(translationKey),
              () -> languageTag + ": Fehlende Übersetzung: " + translationKey);
          assertFalse(
              messageBundle.getString(translationKey).isBlank(),
              () -> languageTag + ": Leere Übersetzung: " + translationKey);
          requestedTranslationKeys.add(translationKey);
          return i18nProvider.getTranslation(translationKey, translationLocale);
        });

    for (Method messageGetter : MessageTexts.class.getDeclaredMethods()) {
      if (!messageGetter.getName().startsWith("get")
          || messageGetter.getName().equals("getTranslation")
          || messageGetter.getName().equals("getFormattedTranslation")) {
        continue;
      }
      int variantCount =
          Arrays.stream(messageGetter.getParameterTypes())
              .filter(Class::isEnum)
              .mapToInt(parameterType -> parameterType.getEnumConstants().length)
              .max()
              .orElse(3);
      // Null, eins und mehrere decken Singular/Plural ab; Gruppen benötigen alle Enumwerte.
      for (int variantIndex = 0; variantIndex < variantCount; variantIndex++) {
        Object translatedResult =
            messageGetter.invoke(messageTexts, arguments(messageGetter, variantIndex));

        assertFalse(((String) translatedResult).isBlank(), messageGetter.getName());
      }
    }

    // Der Titel einer Ticketbenachrichtigung hat eine allgemeine Variante ohne Ticketnummer.
    assertFalse(messageTexts.getSupportReplyNotificationTitle(null).isBlank());
    Set<String> orphanedTranslationKeys = new HashSet<>(messageBundle.keySet());
    orphanedTranslationKeys.removeAll(requestedTranslationKeys);

    assertEquals(
        Set.of(),
        orphanedTranslationKeys,
        () -> languageTag + ": Nicht mehr erreichbare Übersetzungseinträge");
  }

  private static ResourceBundle bundle(Locale locale) {
    ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", locale);

    assertEquals(locale.getLanguage(), bundle.getLocale().getLanguage());
    return bundle;
  }

  private static Object[] arguments(Method messageGetter, int variantIndex) {
    return Arrays.stream(messageGetter.getParameterTypes())
        .map(parameterType -> {
          if (parameterType == String.class) {
            return (Object) "test@example.test";
          }
          if (parameterType == int.class) {
            return (Object) variantIndex;
          }
          if (parameterType == boolean.class) {
            return (Object) (variantIndex > 0);
          }
          if (parameterType == Number.class) {
            return (Object) BigDecimal.valueOf(variantIndex);
          }
          if (parameterType == long.class) {
            return (Object) (long) variantIndex;
          }
          if (parameterType.isEnum()) {
            return parameterType.getEnumConstants()[variantIndex];
          }
          throw new IllegalArgumentException(
              "Testargument fehlt für " + messageGetter.getName() + ": " + parameterType);
        })
        .toArray();
  }
}
