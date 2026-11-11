package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;
import com.vaadin.flow.data.binder.Result;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/** Zusätzliche reine Zifferneingaben; normale Datumsformate bleiben bei Vaadin. */
public final class CompactDateInput {
  private CompactDateInput() {}

  public static void configureDatePickerInput(
      DatePicker datePicker, MessageProperties messageProperties) {
    DatePickerI18n datePickerI18n =
        datePicker.getI18n() == null ? new DatePicker.DatePickerI18n() : datePicker.getI18n();
    Locale datePickerLocale = datePicker.getLocale();
    boolean usesGermanDateOrder =
        Locale.GERMAN.getLanguage().equals(datePickerLocale.getLanguage());
    configureCalendarTexts(datePickerI18n, datePickerLocale, messageProperties);
    datePickerI18n.setFirstDayOfWeek(usesGermanDateOrder ? 1 : 0);
    datePickerI18n.setReferenceDate(LocalDate.of(2000, Month.JANUARY, 1));
    // Die Ausgabe entspricht dem sichtbaren DE-Beispiel; kurze getrennte Eingaben bleiben erlaubt.
    if (usesGermanDateOrder) {
      datePickerI18n.setDateFormats("dd.MM.yyyy", "d.M.yyyy", "dd.MM.yy", "d.M.yy");
    } else {
      datePickerI18n.setDateFormats("M/d/yyyy", "M/d/yy");
    }

    datePicker.setI18n(datePickerI18n);
    datePicker.setFallbackParser(dateInputText ->
        parseCompactDate(
            dateInputText,
            datePicker.getLocale(),
            messageProperties.getBaseValidationDateInvalidMessage()));
  }

  private static void configureCalendarTexts(
      DatePickerI18n datePickerI18n, Locale datePickerLocale, MessageProperties messageProperties) {
    List<String> calendarMonthNames =
        Arrays.stream(Month.values())
            .map(calendarMonth ->
                calendarMonth.getDisplayName(TextStyle.FULL_STANDALONE, datePickerLocale))
            .toList();

    // Vaadin erwartet die Wochentage ab Sonntag, unabhängig vom lokalen Wochenbeginn.
    List<DayOfWeek> calendarWeekdays =
        IntStream.range(0, 7).mapToObj(DayOfWeek.SUNDAY::plus).toList();
    datePickerI18n
        .setMonthNames(calendarMonthNames)
        .setWeekdays(
            calendarWeekdays.stream()
                .map(calendarWeekday ->
                    calendarWeekday.getDisplayName(TextStyle.FULL, datePickerLocale))
                .toList())
        .setWeekdaysShort(
            calendarWeekdays.stream()
                .map(calendarWeekday ->
                    calendarWeekday.getDisplayName(TextStyle.SHORT, datePickerLocale))
                .toList())
        .setToday(messageProperties.getDatePickerToday())
        .setCancel(messageProperties.getDatePickerCancel())
        .setDialogAccessibleName(messageProperties.getDatePickerCalendar());
  }

  static Result<LocalDate> parseCompactDate(
      String dateInputText, Locale dateLocale, String validationErrorText) {
    String dateDigits = dateInputText == null ? "" : dateInputText.strip();
    int dateDigitCount = dateDigits.length();
    if ((dateDigitCount != 4 && dateDigitCount != 6 && dateDigitCount != 8)
        || !dateDigits
            .chars()
            .allMatch(digitCodePoint -> digitCodePoint >= '0' && digitCodePoint <= '9')) {
      return Result.error(validationErrorText);
    }

    int datePartLength = dateDigitCount == 4 ? 1 : 2;
    int firstDatePart = Integer.parseInt(dateDigits.substring(0, datePartLength));
    int secondDatePart = Integer.parseInt(dateDigits.substring(datePartLength, datePartLength * 2));
    String yearDigits = dateDigits.substring(datePartLength * 2);
    int parsedYear = Integer.parseInt(yearDigits);
    if (yearDigits.length() == 2) {
      parsedYear += parsedYear >= 50 ? 1900 : 2000;
    }

    if (parsedYear == 0) {
      return Result.error(validationErrorText);
    }

    boolean usesGermanDateOrder = Locale.GERMAN.getLanguage().equals(dateLocale.getLanguage());

    try {
      return Result.ok(
          LocalDate.of(
              parsedYear,
              usesGermanDateOrder ? secondDatePart : firstDatePart,
              usesGermanDateOrder ? firstDatePart : secondDatePart));
    } catch (DateTimeException _) {
      return Result.error(validationErrorText);
    }
  }
}
