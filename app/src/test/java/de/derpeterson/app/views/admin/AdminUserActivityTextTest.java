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
    MessageProperties texts = texts(language);
    assertEquals("-", format(null, texts));
    assertEquals(texts.getAdminUsersActivityNow(), format(now, texts));
    assertEquals(texts.getAdminUsersActivityNow(), format(now.minusSeconds(59), texts));
    assertEquals(texts.getAdminUsersActivityMinutes(1), format(now.minusSeconds(60), texts));
    assertEquals(texts.getAdminUsersActivityMinutes(59), format(now.minusMinutes(59), texts));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void hourDayAndWeekBoundariesUseTheCorrespondingSingularOrPluralGetter(String language) {
    MessageProperties texts = texts(language);
    assertEquals(texts.getAdminUsersActivityHours(1), format(now.minusHours(1), texts));
    assertEquals(texts.getAdminUsersActivityHours(23), format(now.minusHours(23), texts));
    assertEquals(texts.getAdminUsersActivityDays(1), format(now.minusDays(1), texts));
    assertEquals(texts.getAdminUsersActivityDays(6), format(now.minusDays(6), texts));
    assertEquals(texts.getAdminUsersActivityWeeks(1), format(now.minusDays(7), texts));
    assertEquals(texts.getAdminUsersActivityWeeks(3), format(now.minusDays(21), texts));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void monthsAndYearsFollowCalendarBoundariesInsteadOfFixedThirtyDayMonths(String language) {
    MessageProperties texts = texts(language);
    assertEquals(texts.getAdminUsersActivityMonths(1), format(now.minusMonths(1), texts));
    assertEquals(texts.getAdminUsersActivityMonths(2), format(now.minusMonths(2), texts));
    assertEquals(texts.getAdminUsersActivityYears(1), format(now.minusYears(1), texts));
    assertEquals(texts.getAdminUsersActivityYears(2), format(now.minusYears(2), texts));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void clockChangesUseElapsedHoursAndTwentyFourHourDays(String language) {
    MessageProperties texts = texts(language);
    assertEquals(
        texts.getAdminUsersActivityHours(1),
        AdminUserActivityText.format(
            Instant.parse("2026-03-29T00:30:00Z"), Instant.parse("2026-03-29T01:30:00Z"), texts));
    assertEquals(
        texts.getAdminUsersActivityHours(1),
        AdminUserActivityText.format(
            Instant.parse("2026-10-25T00:30:00Z"), Instant.parse("2026-10-25T01:30:00Z"), texts));
    assertEquals(
        texts.getAdminUsersActivityHours(23),
        AdminUserActivityText.format(
            Instant.parse("2026-03-28T11:00:00Z"), Instant.parse("2026-03-29T10:00:00Z"), texts));
    assertEquals(
        texts.getAdminUsersActivityDays(1),
        AdminUserActivityText.format(
            Instant.parse("2026-10-24T10:00:00Z"), Instant.parse("2026-10-25T11:00:00Z"), texts));
  }

  private String format(ZonedDateTime value, MessageProperties texts) {
    return AdminUserActivityText.format(
        value == null ? null : value.toInstant(), now.toInstant(), texts);
  }

  private MessageProperties texts(String language) {
    return MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
  }
}
