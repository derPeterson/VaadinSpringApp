package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;
import com.vaadin.flow.data.binder.Result;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

/** Zusätzliche reine Zifferneingaben; normale Datumsformate bleiben bei Vaadin. */
public final class CompactDateInput {
  private CompactDateInput() {}

  public static void configureDatePickerInput(
      DatePicker datePicker, MessageProperties messageProperties) {
    DatePickerI18n datePickerI18n =
        datePicker.getI18n() == null ? new DatePicker.DatePickerI18n() : datePicker.getI18n();
    datePickerI18n.setReferenceDate(LocalDate.of(2000, Month.JANUARY, 1));
    // Die Ausgabe entspricht dem sichtbaren DE-Beispiel; kurze getrennte Eingaben bleiben erlaubt.
    boolean usesGermanDateOrder =
        Locale.GERMAN.getLanguage().equals(datePicker.getLocale().getLanguage());
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
    int partLength = dateDigitCount == 4 ? 1 : 2;
    int firstDatePart = Integer.parseInt(dateDigits.substring(0, partLength));
    int secondDatePart = Integer.parseInt(dateDigits.substring(partLength, partLength * 2));
    String yearDigits = dateDigits.substring(partLength * 2);
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
