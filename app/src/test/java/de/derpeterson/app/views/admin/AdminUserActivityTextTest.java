package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminUserActivityTextTest {
  private final ZonedDateTime now = ZonedDateTime.parse("2026-10-12T14:30:00+02:00[Europe/Berlin]");

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void missingActivityAndTheMinuteBoundaryAreDisplayedWithoutInventingAnAge(String language) {
    MessageProperties messageProperties = texts(language);
    assertEquals("-", format(null, messageProperties));
    assertEquals(messageProperties.getAdminUsersActivityNow(), format(now, messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityNow(),
        format(now.minusSeconds(59), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityMinutes(1),
        format(now.minusSeconds(60), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityMinutes(59),
        format(now.minusMinutes(59), messageProperties));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void hourDayAndWeekBoundariesUseTheCorrespondingSingularOrPluralGetter(String language) {
    MessageProperties messageProperties = texts(language);
    assertEquals(
        messageProperties.getAdminUsersActivityHours(1),
        format(now.minusHours(1), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityHours(23),
        format(now.minusHours(23), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityDays(1),
        format(now.minusDays(1), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityDays(6),
        format(now.minusDays(6), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityWeeks(1),
        format(now.minusDays(7), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityWeeks(3),
        format(now.minusDays(21), messageProperties));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void monthsAndYearsFollowCalendarBoundariesInsteadOfFixedThirtyDayMonths(String language) {
    MessageProperties messageProperties = texts(language);
    assertEquals(
        messageProperties.getAdminUsersActivityMonths(1),
        format(now.minusMonths(1), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityMonths(2),
        format(now.minusMonths(2), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityYears(1),
        format(now.minusYears(1), messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityYears(2),
        format(now.minusYears(2), messageProperties));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void clockChangesUseElapsedHoursAndTwentyFourHourDays(String language) {
    MessageProperties messageProperties = texts(language);
    assertEquals(
        messageProperties.getAdminUsersActivityHours(1),
        AdminUserActivityText.format(
            Instant.parse("2026-03-29T00:30:00Z"),
            Instant.parse("2026-03-29T01:30:00Z"),
            messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityHours(1),
        AdminUserActivityText.format(
            Instant.parse("2026-10-25T00:30:00Z"),
            Instant.parse("2026-10-25T01:30:00Z"),
            messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityHours(23),
        AdminUserActivityText.format(
            Instant.parse("2026-03-28T11:00:00Z"),
            Instant.parse("2026-03-29T10:00:00Z"),
            messageProperties));
    assertEquals(
        messageProperties.getAdminUsersActivityDays(1),
        AdminUserActivityText.format(
            Instant.parse("2026-10-24T10:00:00Z"),
            Instant.parse("2026-10-25T11:00:00Z"),
            messageProperties));
  }

  private String format(ZonedDateTime value, MessageProperties messageProperties) {
    return AdminUserActivityText.format(
        value == null ? null : value.toInstant(), now.toInstant(), messageProperties);
  }

  private MessageProperties texts(String language) {
    return MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
  }
}
