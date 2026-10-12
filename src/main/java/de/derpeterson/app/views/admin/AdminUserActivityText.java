package de.derpeterson.app.views.admin;

import de.derpeterson.app.i18n.MessageTexts;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Relative display only; filtering and sorting continue to use the stored timestamp. */
final class AdminUserActivityText {
    private AdminUserActivityText() {
    }

    static String format(LocalDateTime activity, LocalDateTime now, MessageTexts texts) {
        if (activity == null) return "-";
        Duration age = Duration.between(activity, now);
        if (age.toMinutes() < 1) return texts.getAdminUsersActivityNow();
        if (age.toHours() < 1) return texts.getAdminUsersActivityMinutes(age.toMinutes());
        if (age.toDays() < 1) return texts.getAdminUsersActivityHours(age.toHours());
        if (age.toDays() < 7) return texts.getAdminUsersActivityDays(age.toDays());
        long months = ChronoUnit.MONTHS.between(activity, now);
        if (months < 1) return texts.getAdminUsersActivityWeeks(age.toDays() / 7);
        long years = ChronoUnit.YEARS.between(activity, now);
        if (years < 1) return texts.getAdminUsersActivityMonths(months);
        return texts.getAdminUsersActivityYears(years);
    }
}
