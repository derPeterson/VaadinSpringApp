package de.derpeterson.app.validation;

import de.derpeterson.app.model.EmailIdentity;
import java.time.LocalDate;
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

  // Dieselbe Adressregel wie Flow 25.3 EmailValidator, ohne rekursive Wiederholungsgruppen:
  // vor @ ASCII-Buchstaben/Ziffern, _, Punkt, Minus oder Plus; danach Domainteile aus
  // Buchstaben/Ziffern/Minus mit Punkten dazwischen, letzter Teil mindestens zwei Zeichen.
  // ^/$ begrenzen die Adresse. Die beiden negativen Vorschauen schließen einen führenden
  // Domainpunkt und doppelte Domainpunkte aus. Der letzte Teil enthält selbst keinen Punkt;
  // dadurch werden die flachen Wiederholungen nur linear durchlaufen, ohne verschachtelte Gruppen.
  // Die Regel ist auch als HTML-/JavaScript-Pattern verwendbar und schützt deshalb ebenfalls
  // EmailFields automatische serverseitige Validierung, bevor unsere Formularprüfung läuft.
  public static final String EMAIL_REGEX =
      "^[a-zA-Z0-9_.+\\-]+@(?!\\.)(?![a-zA-Z0-9.\\-]*\\.\\.)[a-zA-Z0-9.\\-]+\\.[a-zA-Z0-9\\-]{2,}$";

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
    return birthDate != null && birthDate.isBefore(LocalDate.now());
  }

  public static boolean isRequiredTextValid(String value) {
    return StringUtils.isNotBlank(value);
  }

  public static String normalize(String value) {
    return StringUtils.defaultString(EmailIdentity.canonicalize(value));
  }
}
