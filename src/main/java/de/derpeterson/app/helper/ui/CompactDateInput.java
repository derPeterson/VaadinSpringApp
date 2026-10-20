package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;
import com.vaadin.flow.data.binder.Result;
import de.derpeterson.app.i18n.MessageProperties;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;

/** Zusätzliche reine Zifferneingaben; normale Datumsformate bleiben bei Vaadin. */
public final class CompactDateInput {
  private CompactDateInput() {}

  public static void configure(DatePicker picker, MessageProperties messages) {
    DatePickerI18n i18n =
        picker.getI18n() == null ? new DatePicker.DatePickerI18n() : picker.getI18n();
    i18n.setReferenceDate(LocalDate.of(2000, 1, 1));
    // Die Ausgabe entspricht dem sichtbaren DE-Beispiel; kurze getrennte Eingaben bleiben erlaubt.
    boolean german = Locale.GERMAN.getLanguage().equals(picker.getLocale().getLanguage());
    if (german) {
      i18n.setDateFormats("dd.MM.yyyy", "d.M.yyyy", "dd.MM.yy", "d.M.yy");
    } else {
      i18n.setDateFormats("M/d/yyyy", "M/d/yy");
    }
    picker.setI18n(i18n);
    picker.setFallbackParser(
        input -> parse(input, picker.getLocale(), messages.getBaseValidationDateInvalidMessage()));
  }

  static Result<LocalDate> parse(String input, Locale locale, String error) {
    String digits = input == null ? "" : input.strip();
    int length = digits.length();
    if ((length != 4 && length != 6 && length != 8)
        || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
      return Result.error(error);
    }
    int partLength = length == 4 ? 1 : 2;
    int first = Integer.parseInt(digits.substring(0, partLength));
    int second = Integer.parseInt(digits.substring(partLength, partLength * 2));
    String yearDigits = digits.substring(partLength * 2);
    int year = Integer.parseInt(yearDigits);
    if (yearDigits.length() == 2) {
      year += year >= 50 ? 1900 : 2000;
    }
    if (year == 0) {
      return Result.error(error);
    }
    boolean german = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
    try {
      return Result.ok(LocalDate.of(year, german ? second : first, german ? first : second));
    } catch (DateTimeException _) {
      return Result.error(error);
    }
  }
}
