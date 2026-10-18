package de.derpeterson.app.i18n;

import de.derpeterson.app.service.UserNotificationService.Item;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Automatische Ereignisse folgen der aktuellen UI-Sprache; manuelle Texte bleiben unverändert. */
public final class UserNotificationTexts {
  private UserNotificationTexts() {}

  public static String title(MessageProperties texts, Item item) {
    return switch (item.type()) {
      case ADMIN_ANNOUNCEMENT -> item.title();
      case PASSWORD_CHANGED -> texts.getNotificationsPasswordChangedTitle();
      case PASSWORD_RESET -> texts.getNotificationsPasswordResetTitle();
      case EMAIL_CHANGED -> texts.getNotificationsEmailChangedTitle();
      case ROLES_CHANGED -> texts.getNotificationsRolesChangedTitle();
    };
  }

  public static String body(MessageProperties texts, Item item) {
    return switch (item.type()) {
      case ADMIN_ANNOUNCEMENT -> item.body();
      case PASSWORD_CHANGED -> texts.getNotificationsPasswordChangedBody();
      case PASSWORD_RESET -> texts.getNotificationsPasswordResetBody();
      case EMAIL_CHANGED -> texts.getNotificationsEmailChangedBody();
      case ROLES_CHANGED -> texts.getNotificationsRolesChangedBody();
    };
  }

  public static String time(java.time.Instant time, Locale locale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(time);
  }
}
