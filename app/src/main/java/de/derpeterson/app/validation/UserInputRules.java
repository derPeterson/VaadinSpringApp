package de.derpeterson.app.validation;

import de.derpeterson.app.model.EmailIdentity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** Shared business rules; deliberately independent of Vaadin components and sessions. */
public final class UserInputRules {
  private UserInputRules() {}

  /**
   * Prüft die gesamte Zeichenfolge: ^ und $ markieren Anfang und Ende. (?=.*[A-Z]) fordert
   * mindestens einen lateinischen Großbuchstaben A–Z. (?=.*[^A-Za-z0-9]) fordert ein Zeichen
   * außerhalb der ASCII-Buchstaben/Ziffern; auch ein inneres Leerzeichen oder ein Umlaut erfüllt
   * diese Bedingung. .{8,} fordert mindestens acht Zeichen ohne Zeilenumbrüche. Kleinbuchstaben und
   * Ziffern sind ausdrücklich keine zusätzlichen Pflichten. isPasswordSecure prüft wie bisher die
   * mit trim() bereinigte Eingabe.
   */
  public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$";

  // Dieselbe Adressregel wie Flow EmailValidator, ohne rekursive Wiederholungsgruppen:
  // Vor @ sind ASCII-Buchstaben, Ziffern, _, Minus und Plus erlaubt, Punkte nur dazwischen.
  // Die negativen Vorschauen schließen einen Punkt am Anfang, vor @ und neben einem weiteren
  // Punkt aus. In der Domain trennen Punkte Teile aus Buchstaben, Ziffern und Minus, der letzte
  // Teil hat mindestens zwei Zeichen. ^/$ begrenzen die Adresse; die Wiederholungen sind flach.
  // Die Regel ist auch als HTML-/JavaScript-Pattern verwendbar und schützt deshalb ebenfalls
  // EmailFields automatische serverseitige Validierung, bevor unsere Formularprüfung läuft.
  public static final String EMAIL_REGEX =
      "^(?!\\.)(?![a-zA-Z0-9_.+\\-]*\\.[.@])[a-zA-Z0-9_.+\\-]+@(?!\\.)(?![a-zA-Z0-9.\\-]*\\.\\.)[a-zA-Z0-9.\\-]+\\.[a-zA-Z0-9\\-]{2,}$";

  private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

  public static boolean isPasswordSecure(String password) {
    return StringUtils.isNotBlank(password) && password.trim().matches(PASSWORD_REGEX);
  }

  public static boolean isPasswordSecure(String password, boolean allowBlank) {
    return StringUtils.isBlank(password) ? allowBlank : isPasswordSecure(password);
  }

  public static boolean isEmailValid(String email) {
    return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
  }

  public static boolean isBirthDateValid(LocalDate birthDate) {
    // Geburtsdaten sind Kalendertage der Server-Zeitzone, keine UTC-Zeitpunkte.
    return birthDate == null || birthDate.isBefore(LocalDate.now(ZoneId.systemDefault()));
  }

  public static boolean isRequiredTextValid(String value) {
    return StringUtils.isNotBlank(value);
  }

  public static String normalize(String value) {
    return StringUtils.defaultString(EmailIdentity.canonicalize(value));
  }
}
