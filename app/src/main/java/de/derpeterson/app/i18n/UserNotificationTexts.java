package de.derpeterson.app.i18n;

import de.derpeterson.app.model.enums.UserNotificationRelevance;
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

  public static String title(MessageProperties messageProperties, Item item) {
    return switch (item.type()) {
      case ADMIN_ANNOUNCEMENT -> item.title();
      case PASSWORD_CHANGED -> messageProperties.getNotificationsPasswordChangedTitle();
      case PASSWORD_RESET -> messageProperties.getNotificationsPasswordResetTitle();
      case EMAIL_CHANGED -> messageProperties.getNotificationsEmailChangedTitle();
      case ROLES_CHANGED -> messageProperties.getNotificationsRolesChangedTitle();
      case RECOVERY_REQUEST -> messageProperties.getSupportRecoverySubject();
      case SUPPORT_REPLY ->
          messageProperties.getSupportReplyNotificationTitle(item.supportTicketNumber());
      case SUPPORT_REQUESTER_CLOSED ->
          messageProperties.getSupportRequesterClosedNotificationTitle(item.supportTicketNumber());
    };
  }

  public static String body(MessageProperties messageProperties, Item item) {
    return switch (item.type()) {
      case ADMIN_ANNOUNCEMENT -> item.body();
      case PASSWORD_CHANGED -> messageProperties.getNotificationsPasswordChangedBody();
      case PASSWORD_RESET -> messageProperties.getNotificationsPasswordResetBody();
      case EMAIL_CHANGED -> messageProperties.getNotificationsEmailChangedBody();
      case ROLES_CHANGED -> messageProperties.getNotificationsRolesChangedBody();
      case RECOVERY_REQUEST -> messageProperties.getSupportRecoveryNotification();
      case SUPPORT_REPLY -> messageProperties.getSupportReplyNotificationBody();
      case SUPPORT_REQUESTER_CLOSED ->
          messageProperties.getSupportRequesterClosedNotificationBody();
    };
  }

  public static String relevance(MessageProperties messageProperties, Item item) {
    return switch (item.relevance()) {
      case ACTIVE ->
          item.read()
              ? messageProperties.getNotificationsRead()
              : messageProperties.getNotificationsUnread();
      case TICKET_ASSIGNED -> messageProperties.getNotificationsTicketAssigned();
      case REQUESTER_CLOSED -> messageProperties.getSupportClosedByRequester();
      case TICKET_CLOSED -> messageProperties.getSupportClosed();
    };
  }

  /** Systemtexte und Herkunft in der sichtbaren Sprache für die Datenbanksuche abgleichen. */
  public static Set<UserNotificationType> matchingTypes(
      MessageProperties messageProperties, String query) {
    String term = query.strip().toLowerCase(Locale.ROOT);
    EnumSet<UserNotificationType> matches = EnumSet.noneOf(UserNotificationType.class);
    if (term.isEmpty()) {
      return matches;
    }
    for (UserNotificationType type : UserNotificationType.values()) {
      String visible;
      if (type == UserNotificationType.ADMIN_ANNOUNCEMENT) {
        visible = messageProperties.getNotificationsSourceAdministration();
      } else {
        // Sichtbare Titelvorlage ohne Nummer; konkrete Nummern durchsucht das Repository.
        Item item =
            new Item(
                0L,
                type,
                null,
                null,
                java.time.Instant.EPOCH,
                false,
                "",
                UserNotificationRelevance.ACTIVE);
        visible =
            title(messageProperties, item)
                + " "
                + body(messageProperties, item)
                + " "
                + messageProperties.getNotificationsSourceSystem();
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
