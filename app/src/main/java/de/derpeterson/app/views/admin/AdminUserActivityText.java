package de.derpeterson.app.views.admin;

import de.derpeterson.app.helper.time.UserActivityTime;
import de.derpeterson.app.i18n.MessageTexts;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/** Relative display only; filtering and sorting continue to use the stored timestamp. */
final class AdminUserActivityText {
  private AdminUserActivityText() {}

  static String format(Instant activityInstant, Instant currentInstant, MessageTexts messageTexts) {
    if (activityInstant == null) {
      return "-";
    }
    Duration activityAgeDuration = Duration.between(activityInstant, currentInstant);
    if (activityAgeDuration.toMinutes() < 1) {
      return messageTexts.getAdminUsersActivityNow();
    }
    if (activityAgeDuration.toHours() < 1) {
      return messageTexts.getAdminUsersActivityMinutes(activityAgeDuration.toMinutes());
    }
    if (activityAgeDuration.toDays() < 1) {
      return messageTexts.getAdminUsersActivityHours(activityAgeDuration.toHours());
    }
    if (activityAgeDuration.toDays() < 7) {
      return messageTexts.getAdminUsersActivityDays(activityAgeDuration.toDays());
    }
    ZonedDateTime activityZonedDateTime = activityInstant.atZone(UserActivityTime.DISPLAY_ZONE);
    ZonedDateTime currentZonedDateTime = currentInstant.atZone(UserActivityTime.DISPLAY_ZONE);
    long elapsedMonths = ChronoUnit.MONTHS.between(activityZonedDateTime, currentZonedDateTime);
    if (elapsedMonths < 1) {
      return messageTexts.getAdminUsersActivityWeeks(activityAgeDuration.toDays() / 7);
    }
    long elapsedYears = ChronoUnit.YEARS.between(activityZonedDateTime, currentZonedDateTime);
    if (elapsedYears < 1) {
      return messageTexts.getAdminUsersActivityMonths(elapsedMonths);
    }
    return messageTexts.getAdminUsersActivityYears(elapsedYears);
  }
}
