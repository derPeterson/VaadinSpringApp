package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.PENDING_EMAIL_VERIFICATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AccountFormAppearanceChecks;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationAppearanceChecks;
import de.derpeterson.browsertests.pages.RegistrationPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Registrierung")
public class RegistrationBrowserTest extends BrowserFixture {
  @Tag("account-forms")
  @Tag("notification-variants")
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("REG-01A: Registrierung bestätigt Mailversand, Anmeldung bleibt zunächst gesperrt")
  @Story("Neues Konto registrieren")
  @Description(
      """
      1. In einer frischen Browsersitzung alle Registrierungsfelder gültig ausfüllen.
      2. Absenden und Bestätigung mit der eigenen Testadresse prüfen.
      3. Auf die tatsächliche SMTP-Mail an genau diese Adresse in Mailpit warten.
      4. Vor dem Öffnen des Aktivierungslinks eine Anmeldung versuchen.

      **Erwartet:** Registrierung bestätigt, Mail mit passendem Empfänger und Betreff
      eingegangen; gültige Zugangsdaten öffnen die Bestätigungsseite, aber keine Kontofunktionen.
      Datenbankzustände werden hier nicht direkt untersucht.
      """)
  void registrationNeedsVerificationBeforeLogin(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    String emailAddress = "notification-registration-" + viewportWidth + "@example.test";
    RegistrationPage registrationPage = new RegistrationPage(browserPage);
    registrationPage.open();
    AccountFormAppearanceChecks.captureRegistration(browserPage);
    registrationPage.fillValid(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    registrationPage.submit();
    registrationPage.assertSubmitted(emailAddress);
    NotificationAppearanceChecks.capture(browserPage, "Hervorgehobene-Adresse", "success", -1);
    AllurePlaywright.attachScreenshot("Registrierung bestätigt", browserPage);
    new MailpitInbox(browserContext).awaitVerificationMail(emailAddress);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage).hasURL(urlPattern(PENDING_EMAIL_VERIFICATION_ROUTE));
    assertThat(browserPage.locator("#pending-verification-email")).hasText(emailAddress);

    browserPage.navigate(path(ACCOUNT_ROUTE));
    // Der HTTP-Zugriff auf Kontoseiten verlangt bereits vor dem Vaadin-Routing eine Anmeldung.
    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(browserPage.locator("#account-panel-security")).hasCount(0);

    browserPage.navigate(path(PENDING_EMAIL_VERIFICATION_ROUTE));
    assertThat(browserPage.locator("#pending-verification-email")).hasText(emailAddress);
  }

  @Tag("account-forms")
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
    RegistrationPage registration = new RegistrationPage(browserPage);
    registration.open();
    browserPage.setViewportSize(390, 844);
    Locator requiredFields =
        browserPage
            .locator(".public-panel")
            .locator("vaadin-text-field, vaadin-email-field, vaadin-password-field");
    assertThat(requiredFields).hasCount(6);
    requiredFields.all().forEach(CosmosFollowUpChecks::assertNativeRequiredIndicator);
    assertThat(browserPage.locator(".public-panel vaadin-date-picker [part=required-indicator]"))
        .isHidden();
    assertThat(browserPage.locator(".public-panel vaadin-combo-box [part=required-indicator]"))
        .isHidden();
    registration.change(uiTexts.getRegistrationFirstNameField(), "Browser");
    registration.change(uiTexts.getRegistrationFirstNameField(), "");
    assertRegistrationHasNoValidationFeedback();
    registration.submit();
    registration.assertRequiredError();
    AllurePlaywright.attachScreenshot("Fehlende Pflichtangaben", browserPage);
    registration.change(uiTexts.getRegistrationFirstNameField(), "Browser");
    assertThat(browserPage.locator("vaadin-text-field[invalid]")).hasCount(1);
    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(2);
    assertThat(browserPage.locator("vaadin-password-field[invalid]")).hasCount(2);
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("vaadin-combo-box [part~='clear-button']").click();
    browserPage.locator("vaadin-date-picker input").fill("");
    registration.submit();
    registration.assertSubmitted(email);
  }

  @Tag("account-forms")
  @Tag("fast")
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("REG-02B: Ungültige E-Mail und abweichende Passwortbestätigung sind korrigierbar")
  @Story("Eingabehinweise und Korrektur")
  @Description(
      """
      1. Auf Desktop und schmaler Ansicht alle Pflichtfelder füllen, aber in beiden
         E-Mail-Feldern eine ungültige Adresse verwenden.
      2. Absenden; verständlichen E-Mail-Hinweis und Feldmarkierungen prüfen.
      3. E-Mail korrigieren, abweichende Passwortbestätigung eingeben und erneut absenden.
      4. Passwort-Hinweis und Markierungen prüfen; Bestätigung korrigieren und absenden.

      **Erwartet:** Fehler verständlich und an den betroffenen Feldern sichtbar.
      Erst der korrigierte Versuch zeigt eine Registrierungsbestätigung.
      Weitere Passwort-, E-Mail-Bestätigungs- und Dublettenvarianten bleiben offen.
      """)
  void emailAndPasswordConfirmationCanBeCorrected(int viewportWidth) {
    String email = "correction-" + UUID.randomUUID() + "@example.test";
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    RegistrationPage registration = new RegistrationPage(browserPage);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.setEmails("not-an-email", "not-an-email");
    browserPage.locator("vaadin-email-field").last().locator("input").press("Tab");
    assertRegistrationHasNoValidationFeedback();
    registration.submit();
    registration.assertEmailError();
    AllurePlaywright.attachScreenshot("Ungültige E-Mail", browserPage);
    NotificationAppearanceChecks.close(browserPage);
    registration.change(uiTexts.getPublicEmailField(), email);
    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(1);
    registration.setEmails(email, email);
    registration.setPasswords(BrowserTestAccounts.USER_PASSWORD, "DifferentPassword1!");
    registration.submit();
    registration.assertPasswordConfirmationError();
    AllurePlaywright.attachScreenshot("Abweichende Passwortbestätigung", browserPage);
    NotificationAppearanceChecks.close(browserPage);
    registration.setPasswords(BrowserTestAccounts.USER_PASSWORD, BrowserTestAccounts.USER_PASSWORD);

    registration.change(uiTexts.getRegistrationBirthDateField(), "");
    assertThat(browserPage.locator("vaadin-date-picker")).hasJSProperty("value", "");
    registration.change(uiTexts.getRegistrationBirthDateField(), "kein-datum");
    assertThat(browserPage.locator("vaadin-date-picker")).not().hasAttribute("invalid", "");
    registration.submit();
    assertThat(browserPage.locator("vaadin-date-picker")).hasAttribute("invalid", "");
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getBaseValidationBirthDatePastMessage());
    NotificationAppearanceChecks.close(browserPage);
    registration.change(uiTexts.getRegistrationBirthDateField(), "");
    registration.submit();
    registration.assertSubmitted(email);
  }

  private void assertRegistrationHasNoValidationFeedback() {
    assertThat(browserPage.locator(".public-panel [invalid]")).hasCount(0);
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])")).hasCount(0);
  }
}
