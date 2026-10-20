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

  static String format(Instant activity, Instant now, MessageTexts texts) {
    if (activity == null) {
      return "-";
    }
    Duration age = Duration.between(activity, now);
    if (age.toMinutes() < 1) {
      return texts.getAdminUsersActivityNow();
    }
    if (age.toHours() < 1) {
      return texts.getAdminUsersActivityMinutes(age.toMinutes());
    }
    if (age.toDays() < 1) {
      return texts.getAdminUsersActivityHours(age.toHours());
    }
    if (age.toDays() < 7) {
      return texts.getAdminUsersActivityDays(age.toDays());
    }
    ZonedDateTime localActivity = activity.atZone(UserActivityTime.DISPLAY_ZONE);
    ZonedDateTime localNow = now.atZone(UserActivityTime.DISPLAY_ZONE);
    long months = ChronoUnit.MONTHS.between(localActivity, localNow);
    if (months < 1) {
      return texts.getAdminUsersActivityWeeks(age.toDays() / 7);
    }
    long years = ChronoUnit.YEARS.between(localActivity, localNow);
    if (years < 1) {
      return texts.getAdminUsersActivityMonths(months);
    }
    return texts.getAdminUsersActivityYears(years);
  }
}
