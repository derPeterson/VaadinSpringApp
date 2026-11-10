package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.RESET_PASSWORD_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Passwort-Zurücksetzen")
public class PasswordResetBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("RESET-05: Neue anonyme Reset-Mails folgen der Sitzungssprache desselben Kontos")
  @Description(
      """
      1. Eigenes Konto aktivieren und anonym eine Reset-Mail in der Testsprache anfordern.
      2. Auf Home die andere Sprache wählen und für dasselbe Konto erneut anfordern.
      3. Neue Mail-ID, übersetzten Betreff und Inhalt prüfen; die erste Mail bleibt unverändert.
      **Erwartet:** Die auslösende anonyme Sitzung bestimmt die Sprache neuer Reset-Mails.
      """)
  void anonymousResetMailsFollowCurrentSessionLanguage() {
    String email = activateAccount();
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();
    MailpitInbox firstInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail first = firstInbox.awaitPasswordResetMail(email);
    try (Page oldMail = browserContext.newPage()) {
      oldMail.navigate(
          System.getProperty("browser.tests.mailpitUrl") + "/view/" + first.id() + ".html");
      String original = oldMail.locator("body").innerText();
      de.derpeterson.browsertests.i18n.UiTexts other =
          new de.derpeterson.browsertests.i18n.UiTexts(
              uiTexts.language().equals("de") ? "en" : "de");
      browserPage.navigate(path(HOME_ROUTE));
      new UserMenu(browserPage).selectLanguage(other.language());
      browserPage.navigate(path(de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_ROUTE));
      browserPage
          .getByLabel(other.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true))
          .fill(email);
      browserPage
          .getByRole(
              com.microsoft.playwright.options.AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName(other.getPublicRequestLink()).setExact(true))
          .click();
      MailpitInbox.CapturedMail second =
          new MailpitInbox(browserContext, null, other).awaitPasswordResetMail(email);
      org.junit.jupiter.api.Assertions.assertNotEquals(first.id(), second.id());
      try (Page target =
          new MailpitInbox(browserContext, null, other).openPasswordResetAction(second)) {
        org.junit.jupiter.api.Assertions.assertTrue(
            target.url().startsWith(System.getProperty("browser.tests.baseUrl")));
      }
      try (Page newMail = browserContext.newPage()) {
        newMail.navigate(
            System.getProperty("browser.tests.mailpitUrl") + "/view/" + second.id() + ".html");
        org.junit.jupiter.api.Assertions.assertNotEquals(
            original, newMail.locator("body").innerText());
      }
      oldMail.reload();

      assertEquals(original, oldMail.locator("body").innerText());
    } finally {
      browserPage.navigate(path(HOME_ROUTE));
      new UserMenu(browserPage).selectTestLanguage();
    }
  }

  @Tag("fast")
  @Test
  @DisplayName("RESET-01A: Echte Reset-Mail ermöglicht ein neues Passwort und die Anmeldung")
  @Story("Vom vergessenen Passwort zur erneuten Anmeldung")
  @Description(
      """
      1. Ein eigenes Konto über Registrierung und echten Verifikationsmailbutton aktivieren.
      2. Im Loginformular Passwort vergessen öffnen und seine E-Mail absenden.
      3. Neutralen Anfragehinweis und die eigene tatsächlich versandte Reset-Mail prüfen.
      4. Den sichtbaren Mailbutton anklicken, neues Passwort samt Bestätigung absenden.
      5. Mit dem alten Passwort anmelden: verständliche Ablehnung prüfen.
      6. Mit dem neuen Passwort anmelden: Startseite und eigenes Konto ohne Adminrechte prüfen.

      **Erwartet:** Der Reset-Mailbutton bleibt auf dem exakten Testserver. Nur das neue
      Passwort ermöglicht die Anmeldung. Keine externe SMTP-/Mailclient-Prüfung.
      """)
  void realResetMailChangesTheLoginPassword() {
    String email = activateAccount();
    ResetPasswordPage reset = openResetForm(email);
    reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    reset.submit();
    reset.assertChanged();
    AllurePlaywright.attachScreenshot("Bestätigte Passwortänderung", browserPage);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    login.assertRejected();
    assertNewPasswordLogin(email);
  }

  @Test
  @DisplayName("RESET-02A: Leere und ungültige E-Mail sind im Anfrageformular korrigierbar")
  @Story("Anfrageformular validieren")
  @Description(
      """
      1. Ein eigenes Konto registrieren und über seine Verifikationsmail aktivieren.
      2. Passwort vergessen öffnen und die leere Anfrage absenden.
      3. Pflichtfeldhinweis prüfen; ungültige E-Mail eingeben und erneut absenden.
      4. E-Mail-Hinweis und Markierung prüfen; eigene gültige Adresse eingeben und absenden.
      5. Neutralen Anfragehinweis und Eingang der eigenen Reset-Mail prüfen.

      **Erwartet:** Ungültige Eingaben zeigen keinen Anfrageerfolg. Nach Korrektur wird
      die gültige Anfrage über die Oberfläche angenommen und ihre Mail tatsächlich erfasst.
      """)
  void requestEmailCanBeCorrected() {
    String email = activateAccount();
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.submit();
    forgotPasswordPage.assertRequiredError();
    forgotPasswordPage.enterEmail("not-an-email");
    forgotPasswordPage.submit();
    forgotPasswordPage.assertEmailError();
    AllurePlaywright.attachScreenshot("Ungültige Reset-Anfrage", browserPage);
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();
    new MailpitInbox(browserContext).awaitPasswordResetMail(email);
  }

  @Test
  @DisplayName(
      "RESET-02B: Passwortfehler sind korrigierbar und verbrauchen den Link nicht vorzeitig")
  @Story("Tokengebundenes Formular validieren")
  @Description(
      """
      1. Eigenes Konto über UI und echte Mail aktivieren; Reset-Mail über Passwort vergessen anfordern.
      2. Ihren Mailbutton öffnen und die leeren Passwortfelder absenden; Pflichtfeldhinweis prüfen.
      3. Zwei ungültige Passwörter absenden; Passwortregel und Feldmarkierungen prüfen.
      4. Zwei gültige, aber verschiedene Passwörter absenden; Bestätigungshinweis prüfen.
      5. Beide Felder gültig und identisch füllen; Änderung und neue Anmeldung prüfen.

      **Erwartet:** Alle drei Fehler bleiben korrigierbar. Derselbe Mail-Link ermöglicht
      danach die Passwortänderung; technische Tokenzustände werden nicht per SQL geprüft.
      """)
  void passwordErrorsCanBeCorrectedUsingTheSameLink() {
    String email = activateAccount();
    ResetPasswordPage reset = openResetForm(email);
    reset.submit();
    reset.assertRequiredError();
    reset.fillPasswords("weak", "weak");
    reset.submit();
    reset.assertPasswordError();
    reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, "DifferentPassword3!");
    reset.submit();
    reset.assertConfirmationError();
    AllurePlaywright.attachScreenshot("Abweichende Passwortbestätigung", browserPage);
    reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    reset.submit();
    reset.assertChanged();
    assertNewPasswordLogin(email);
  }

  @Test
  @DisplayName("RESET-03A: Vorhandene und unbekannte E-Mail bekommen denselben neutralen Hinweis")
  @Story("Keine Kontoauskunft im sichtbaren Anfrageergebnis")
  @Description(
      """
      1. Ein eigenes Konto über Registrierung und Verifikationsmail aktivieren.
      2. Reset-Anfrage für dieses Konto absenden, sichtbare Antwort erfassen und eigene Mail abwarten.
      3. Die Bestätigung über ihren Schließen-Button schließen.
      4. Formular erneut öffnen, eine andere neue UUID-Adresse absenden und Antwort erfassen.
      5. Sichtbaren Titel und Nachricht mit der ersten Antwort vergleichen.

      **Erwartet:** Beide Antworten sind identisch und neutral. Die unbekannte Adresse
      verrät kein Konto. Timing-Unterschiede, technische Fehler und dauerhafte Mailabwesenheit
      werden durch diesen UI-Fall nicht nachgewiesen.
      """)
  void unknownEmailHasTheSameVisibleResponse() {
    String email = activateAccount();
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    String knownResponse = forgotPasswordPage.requestedResponse();
    new MailpitInbox(browserContext).awaitPasswordResetMail(email);
    forgotPasswordPage.dismissRequestedNotice();
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail("unknown-reset-" + UUID.randomUUID() + "@example.test");
    forgotPasswordPage.submit();

    assertEquals(knownResponse, forgotPasswordPage.requestedResponse());
  }

  @Test
  @DisplayName(
      "RESET-04A: Unbekannter Token wird abgelehnt und ohne Token erscheint die Startseite")
  @Story("Ungültiger oder fehlender Reset-Link")
  @Description(
      """
      1. /reset-password mit einem neuen, nicht vergebenen UUID-Token öffnen.
      2. Neutralen Fehlerhinweis und fehlendes Passwortformular prüfen.
      3. /reset-password ohne Token öffnen.
      4. Tatsächlichen Startseiteninhalt, URL und Titel prüfen.

      **Erwartet:** Unbekannte Links erlauben keine Passwortänderung. Ohne Token
      erscheint die vollständige Startseite. Exakte Ablaufgrenzen bleiben JUnit-Aufgabe.
      """)
  void unknownAndMissingTokensHaveDefinedUiBehavior() {
    browserPage.navigate(path(RESET_PASSWORD_ROUTE) + "/" + UUID.randomUUID());
    new ResetPasswordPage(browserPage).assertInvalid();
    AllurePlaywright.attachScreenshot("Ungültiger Reset-Link", browserPage);
    browserPage.navigate(path(RESET_PASSWORD_ROUTE));

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    assertThat(browserPage).hasTitle(HOME_PAGE_TITLE);
    assertThat(
            browserPage.getByText(
                "VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Test
  @DisplayName("RESET-04B: Bereits benutzter Reset-Link bietet kein weiteres Passwortformular")
  @Story("Einmalige sichtbare Verwendung")
  @Description(
      """
      1. Eigenes Konto über UI aktivieren; seine echte Reset-Mail anfordern und ihren Button öffnen.
      2. Ein neues Passwort samt Bestätigung setzen und den Erfolg prüfen.
      3. Dieselbe tokengebundene Seite neu laden.
      4. Neutralen Ablehnungshinweis und fehlendes Passwortformular prüfen.

      **Erwartet:** Nach einem erfolgreichen Reset lässt sich der Link nicht erneut
      zur Passwortänderung verwenden. Paralleler Verbrauch bleibt in Persistenztests.
      """)
  void usedResetLinkCannotBeUsedAgain() {
    String email = activateAccount();
    ResetPasswordPage reset = openResetForm(email);
    reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    reset.submit();
    reset.assertChanged();
    browserPage.reload();
    reset.assertInvalid();
  }

  private String activateAccount() {
    String email = "reset-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(browserPage);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    MailpitInbox inbox = new MailpitInbox(browserContext);
    browserPage = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
    return email;
  }

  private ResetPasswordPage openResetForm(String email) {
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    browserPage = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
    return new ResetPasswordPage(browserPage);
  }

  private void assertNewPasswordLogin(String email) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    new UserMenu(browserPage).assertUserSignedInWithoutAdminAction(email);
  }
}
