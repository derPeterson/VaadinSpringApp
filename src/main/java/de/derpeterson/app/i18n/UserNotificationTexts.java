package de.derpeterson.app.i18n;

import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.service.UserNotificationService.Item;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

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

  /** Systemtexte und Herkunft in der sichtbaren Sprache für die Datenbanksuche abgleichen. */
  public static Set<UserNotificationType> matchingTypes(MessageProperties texts, String query) {
    String term = query.strip().toLowerCase(Locale.ROOT);
    EnumSet<UserNotificationType> matches = EnumSet.noneOf(UserNotificationType.class);
    if (term.isEmpty()) {
      return matches;
    }
    for (UserNotificationType type : UserNotificationType.values()) {
      String visible;
      if (type == UserNotificationType.ADMIN_ANNOUNCEMENT) {
        visible = texts.getNotificationsSourceAdministration();
      } else {
        Item item = new Item(0L, type, null, null, java.time.Instant.EPOCH, false);
        visible =
            title(texts, item)
                + " "
                + body(texts, item)
                + " "
                + texts.getNotificationsSourceSystem();
      }
      if (visible.toLowerCase(Locale.ROOT).contains(term)) {
        matches.add(type);
      }
    }
    return matches;
  }

  public static String time(java.time.Instant time, Locale locale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(time);
  }
}
