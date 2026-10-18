package de.derpeterson.app.i18n;

import de.derpeterson.app.service.UserMessageService.Item;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Automatische Ereignisse folgen der aktuellen UI-Sprache; manuelle Texte bleiben unverändert. */
public final class UserMessageTexts {
  private UserMessageTexts() {}

  public static String title(MessageProperties texts, Item item) {
    return switch (item.type()) {
      case ADMIN_MESSAGE -> item.title();
      case PASSWORD_CHANGED -> texts.getMessagesPasswordChangedTitle();
      case PASSWORD_RESET -> texts.getMessagesPasswordResetTitle();
      case EMAIL_CHANGED -> texts.getMessagesEmailChangedTitle();
    };
  }

  public static String body(MessageProperties texts, Item item) {
    return switch (item.type()) {
      case ADMIN_MESSAGE -> item.body();
      case PASSWORD_CHANGED -> texts.getMessagesPasswordChangedBody();
      case PASSWORD_RESET -> texts.getMessagesPasswordResetBody();
      case EMAIL_CHANGED -> texts.getMessagesEmailChangedBody();
    };
  }

  public static String time(java.time.Instant time, Locale locale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(time);
  }
}
