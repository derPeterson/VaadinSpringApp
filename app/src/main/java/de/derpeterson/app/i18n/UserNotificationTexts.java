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

  public static String getNotificationTitleText(
      MessageProperties messageProperties, Item notificationItem) {
    return switch (notificationItem.type()) {
      case ADMIN_ANNOUNCEMENT -> notificationItem.title();
      case PASSWORD_CHANGED -> messageProperties.getNotificationsPasswordChangedTitle();
      case PASSWORD_RESET -> messageProperties.getNotificationsPasswordResetTitle();
      case EMAIL_CHANGED -> messageProperties.getNotificationsEmailChangedTitle();
      case ROLES_CHANGED -> messageProperties.getNotificationsRolesChangedTitle();
      case RECOVERY_REQUEST -> messageProperties.getSupportRecoverySubject();
      case SUPPORT_REPLY ->
          messageProperties.getSupportReplyNotificationTitle(
              notificationItem.supportTicketNumber());
      case SUPPORT_REQUESTER_CLOSED ->
          messageProperties.getSupportRequesterClosedNotificationTitle(
              notificationItem.supportTicketNumber());
    };
  }

  public static String getNotificationBodyText(
      MessageProperties messageProperties, Item notificationItem) {
    return switch (notificationItem.type()) {
      case ADMIN_ANNOUNCEMENT -> notificationItem.body();
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

  public static String getNotificationRelevanceText(
      MessageProperties messageProperties, Item notificationItem) {
    return switch (notificationItem.relevance()) {
      case ACTIVE ->
          notificationItem.read()
              ? messageProperties.getNotificationsRead()
              : messageProperties.getNotificationsUnread();
      case TICKET_ASSIGNED -> messageProperties.getNotificationsTicketAssigned();
      case REQUESTER_CLOSED -> messageProperties.getSupportClosedByRequester();
      case TICKET_CLOSED -> messageProperties.getSupportClosed();
    };
  }

  /** Systemtexte und Herkunft in der sichtbaren Sprache für die Datenbanksuche abgleichen. */
  public static Set<UserNotificationType> findMatchingNotificationTypes(
      MessageProperties messageProperties, String searchQuery) {
    String normalizedSearchTerm = searchQuery.strip().toLowerCase(Locale.ROOT);
    EnumSet<UserNotificationType> matchingNotificationTypes =
        EnumSet.noneOf(UserNotificationType.class);
    if (normalizedSearchTerm.isEmpty()) {
      return matchingNotificationTypes;
    }
    for (UserNotificationType notificationType : UserNotificationType.values()) {
      String visibleNotificationText;
      if (notificationType == UserNotificationType.ADMIN_ANNOUNCEMENT) {
        visibleNotificationText = messageProperties.getNotificationsSourceAdministration();
      } else {
        // Sichtbare Titelvorlage ohne Nummer; konkrete Nummern durchsucht das Repository.
        Item notificationItem =
            new Item(
                0L,
                notificationType,
                null,
                null,
                java.time.Instant.EPOCH,
                false,
                "",
                UserNotificationRelevance.ACTIVE);
        visibleNotificationText =
            getNotificationTitleText(messageProperties, notificationItem)
                + " "
                + getNotificationBodyText(messageProperties, notificationItem)
                + " "
                + messageProperties.getNotificationsSourceSystem();
      }
      if (visibleNotificationText.toLowerCase(Locale.ROOT).contains(normalizedSearchTerm)) {
        matchingNotificationTypes.add(notificationType);
      }
    }
    return matchingNotificationTypes;
  }

  public static String formatNotificationTimestamp(
      java.time.Instant notificationTimestamp, Locale displayLocale) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(displayLocale)
        .withZone(ZoneId.systemDefault())
        .format(notificationTimestamp);
  }
}
