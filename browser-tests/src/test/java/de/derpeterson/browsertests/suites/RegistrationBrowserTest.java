package de.derpeterson.browsertests.suites;

import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.*;

@Tag("browser")
@Feature("Registrierung")
public class RegistrationBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("REG-01A: Registrierung bestätigt Mailversand, Anmeldung bleibt zunächst gesperrt")
  @Story("Neues Konto registrieren")
  @Description(
      """
            1. In einer frischen Browsersitzung alle Registrierungsfelder gültig ausfüllen.
            2. Absenden und Bestätigung mit der eigenen Testadresse prüfen.
            3. Auf die tatsächliche SMTP-Mail an genau diese Adresse in Mailpit warten.
            4. Vor dem Öffnen des Aktivierungslinks eine Anmeldung versuchen.

            **Erwartet:** Registrierung bestätigt, Mail mit passendem Empfänger und Betreff
            eingegangen; ohne Verifikation bleibt die Anmeldung verständlich abgelehnt.
            Datenbankzustände werden hier nicht direkt untersucht.
            """)
  void registrationNeedsVerificationBeforeLogin() {
    String email = "registration-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(page);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    AllurePlaywright.attachScreenshot("Registrierung bestätigt", page);
    new MailpitInbox(context).awaitVerificationMail(email);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    login.assertRejected();
  }

  @Tag("fast")
  @Test
  @DisplayName("REG-02A: Fehlende Pflichtangaben werden markiert und lassen sich korrigieren")
  @Story("Pflichtangaben und Korrektur")
  @Description(
      """
            1. Das leere Registrierungsformular absenden.
            2. Verständlichen Pflichtfeldhinweis und Markierungen aller Pflichtfelder prüfen.
            3. Alle Felder gültig ausfüllen und erneut absenden.

            **Erwartet:** Der leere Versuch zeigt keinen Erfolg. Nach Korrektur erscheint
            die Registrierungsbestätigung für die neue Testadresse.
            Einzelvarianten wie blanke Namen oder zukünftige Geburtstage bleiben offen.
            """)
  void requiredFieldsCanBeCorrected() {
    String email = "required-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(page);
    registration.open();
    registration.submit();
    registration.assertRequiredError();
    AllurePlaywright.attachScreenshot("Fehlende Pflichtangaben", page);
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
  }

  @Tag("fast")
  @Test
  @DisplayName("REG-02B: Ungültige E-Mail und abweichende Passwortbestätigung sind korrigierbar")
  @Story("Eingabehinweise und Korrektur")
  @Description(
      """
            1. Alle Pflichtfelder füllen, aber in beiden E-Mail-Feldern eine ungültige Adresse verwenden.
            2. Absenden; verständlichen E-Mail-Hinweis und Feldmarkierungen prüfen.
            3. E-Mail korrigieren, abweichende Passwortbestätigung eingeben und erneut absenden.
            4. Passwort-Hinweis und Markierungen prüfen; Bestätigung korrigieren und absenden.

            **Erwartet:** Fehler verständlich und an den betroffenen Feldern sichtbar.
            Erst der korrigierte Versuch zeigt eine Registrierungsbestätigung.
            Weitere Passwort-, E-Mail-Bestätigungs- und Dublettenvarianten bleiben offen.
            """)
  void emailAndPasswordConfirmationCanBeCorrected() {
    String email = "correction-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(page);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.setEmails("not-an-email", "not-an-email");
    registration.submit();
    registration.assertEmailError();
    AllurePlaywright.attachScreenshot("Ungültige E-Mail", page);
    registration.setEmails(email, email);
    registration.setPasswords(BrowserTestAccounts.USER_PASSWORD, "DifferentPassword1!");
    registration.submit();
    registration.assertPasswordConfirmationError();
    AllurePlaywright.attachScreenshot("Abweichende Passwortbestätigung", page);
    registration.setPasswords(BrowserTestAccounts.USER_PASSWORD, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
  }
}
