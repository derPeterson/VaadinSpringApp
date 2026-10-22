package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.derpeterson.app.helper.time.UserActivityTime;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class AdminUserActivityFilterTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-12T12:07:33Z"), ZoneId.of("America/New_York"));
  private UI ui;
  private AdminUserActivityFilter field;
  private final LocalDate day = LocalDate.of(2026, 10, 12);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    field = new AdminUserActivityFilter(CLOCK);
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
  }

  @Test
  void dateWithoutTimeFiltersFromMidnightAndLeavesTheTimeInputEmpty() {
    date().setValue(day);
    assertEquals(day.atStartOfDay(), field.getValue());
    assertNull(time().getValue());
    assertFalse(field.isInvalid());
  }

  @Test
  void selectedTimeIsUsedAndClearingOnlyTheTimeReturnsToMidnight() {
    date().setValue(day);
    time().setValue(LocalTime.of(14, 45));
    assertEquals(day.atTime(14, 45), field.getValue());
    time().clear();
    assertEquals(day.atStartOfDay(), field.getValue());
    assertNull(time().getValue());
  }

  @Test
  void timeWithoutDateRemainsDisabledAndCannotCreateAPartialFilter() {
    time().setValue(LocalTime.NOON);
    assertNull(field.getValue());
    assertFalse(time().isEnabled());
    assertNull(time().getValue());
    assertFalse(field.isInvalid());
    field.clear();
    assertNull(date().getValue());
    assertNull(time().getValue());
    assertNull(field.getValue());
    assertFalse(field.isInvalid());
  }

  @Test
  void programmaticValueProducesOneConsistentChangeAndClearRemovesBothParts() {
    List<LocalDateTime> changes = new ArrayList<>();
    field.addValueChangeListener(event -> changes.add(event.getValue()));
    LocalDateTime value = day.atTime(9, 30);
    field.setValue(value);
    assertEquals(day, date().getValue());
    assertEquals(LocalTime.of(9, 30), time().getValue());
    assertEquals(List.of(value), changes);
    field.clear();
    assertNull(field.getValue());
    assertNull(date().getValue());
    assertNull(time().getValue());
    assertEquals(Arrays.asList(value, null), changes);
  }

  @Test
  void invalidDateInputCannotBecomeAValidCutoffAndClearResetsTheError() {
    date().setInvalid(true);
    assertNull(field.generateModelValue());
    assertTrue(field.isInvalid());
    field.clear();
    assertFalse(date().isInvalid());
    assertFalse(field.isInvalid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageChangesUpdateBothInputLabelsWithoutChangingTheCutoff(String language) {
    Locale locale = Locale.forLanguageTag(language);
    MessageProperties texts = MessageProperties.forLocale(new CustomI18NProvider(), locale);
    LocalDateTime cutoff = day.atTime(10, 15);
    field.setValue(cutoff);
    field.setLocale(locale);
    field.refreshTexts(texts);
    assertEquals(cutoff, field.getValue());
    assertEquals(locale, date().getLocale());
    assertEquals(locale, time().getLocale());
    assertEquals(texts.getAdminUsersActivityDate(), date().getAriaLabel().orElseThrow());
    assertEquals(texts.getAdminUsersFilterActiveSince(), date().getLabel());
    assertEquals(
        LocalDateTime.ofInstant(CLOCK.instant(), UserActivityTime.DISPLAY_ZONE)
            .toLocalDate()
            .format(
                java.time.format.DateTimeFormatter.ofPattern(
                    language.equals("de") ? "dd.MM.uuuu" : "MM/dd/uuuu", locale)),
        date().getPlaceholder());
    LocalTime placeholder =
        LocalTime.parse(
            time().getPlaceholder(),
            java.time.format.DateTimeFormatter.ofPattern(
                language.equals("de") ? "HH:mm" : "h:mm a", locale));
    assertEquals(0, placeholder.getMinute() % 15);
    assertFalse(
        placeholder.isAfter(
            LocalDateTime.ofInstant(CLOCK.instant(), UserActivityTime.DISPLAY_ZONE).toLocalTime()));
    assertFalse(date().isAutoOpen());
    assertFalse(time().isAutoOpen());
    assertEquals(texts.getAdminUsersActivityTimeOptional(), time().getAriaLabel().orElseThrow());
  }

  @Test
  void clearingTheDateClearsAndDisablesAnExistingTimeAndReentryEnablesIt() {
    field.setValue(day.atTime(14, 30));
    assertTrue(time().isEnabled());
    date().clear();
    assertNull(time().getValue());
    assertFalse(time().isEnabled());
    assertNull(field.getValue());
    date().setValue(day);
    assertTrue(time().isEnabled());
    assertEquals(day.atStartOfDay(), field.getValue());
  }

  @Test
  void invalidDateDisablesTimeAndDoesNotExposeAValidCutoff() {
    field.setValue(day.atTime(14, 30));
    date().setInvalid(true);
    assertNull(field.generateModelValue());
    assertTrue(field.isInvalid());
    field.clear();
    assertFalse(time().isEnabled());
    assertFalse(field.isInvalid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void compactClientDatesUpdateTheCutoffAndInvalidInputClearsTheTime(String language) {
    Locale locale = Locale.forLanguageTag(language);
    field.setLocale(locale);
    field.refreshTexts(MessageProperties.forLocale(new CustomI18NProvider(), locale));
    date()
        .getElement()
        .setProperty("_inputElementValue", language.equals("de") ? "121026" : "101226");
    ReflectionTestUtils.invokeMethod(date(), "setModelValue", null, true);
    assertEquals(day.atStartOfDay(), field.getValue());
    assertTrue(time().isEnabled());
    time().setValue(LocalTime.of(9, 30));
    assertEquals(day.atTime(9, 30), field.getValue());
    date().getElement().setProperty("_inputElementValue", "000086");
    ReflectionTestUtils.invokeMethod(date(), "setModelValue", null, true);
    assertNull(field.getValue());
    assertTrue(field.isInvalid());
    assertFalse(time().isEnabled());
    assertNull(time().getValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void missingSpringHourIsInvalidAndCanBeCorrectedWithoutLosingTheDate(String language) {
    field.setLocale(Locale.forLanguageTag(language));
    date().setValue(LocalDate.of(2026, 3, 29));
    time().setValue(LocalTime.of(2, 30));
    assertNull(field.getValue());
    assertTrue(field.isInvalid());
    assertEquals(LocalDate.of(2026, 3, 29), date().getValue());
    assertEquals(LocalTime.of(2, 30), time().getValue());
    time().setValue(LocalTime.of(3, 30));
    assertFalse(field.isInvalid());
    assertEquals(
        Instant.parse("2026-03-29T01:30:00Z"), UserActivityTime.inclusiveCutoff(field.getValue()));
  }

  @Test
  void repeatedAutumnHourStartsAtTheFirstOccurrenceAndClearRemovesGapErrors() {
    date().setValue(LocalDate.of(2026, 10, 25));
    time().setValue(LocalTime.of(2, 30));
    assertFalse(field.isInvalid());
    assertEquals(
        Instant.parse("2026-10-25T00:30:00Z"), UserActivityTime.inclusiveCutoff(field.getValue()));
    field.setValue(LocalDateTime.of(2026, 3, 29, 2, 30));
    assertTrue(field.isInvalid());
    assertNull(field.generateModelValue());
    assertTrue(field.isInvalid());
    field.clear();
    assertFalse(field.isInvalid());
    assertNull(field.getValue());
    assertFalse(time().isEnabled());
  }

  private DatePicker date() {
    return (DatePicker) ReflectionTestUtils.getField(field, "date");
  }

  private TimePicker time() {
    return (TimePicker) ReflectionTestUtils.getField(field, "time");
  }
}
