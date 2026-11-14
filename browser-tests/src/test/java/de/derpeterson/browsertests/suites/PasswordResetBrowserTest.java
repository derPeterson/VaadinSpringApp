package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.RESET_PASSWORD_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
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
@Feature("Passwort-Zurücksetzen")
public class PasswordResetBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "RESET-06: Anfragekarte erhält Darstellung, Tastaturbedienung und korrigierbare Fehler")
  @Description(
      """
      1. Passwort vergessen über den sichtbaren Anmeldelink auf Desktop/Smartphone öffnen.
      2. Kopf, Inhalt, Footer, Feld und Aktionen vermessen und als Vergleichsaufnahme erfassen.
      3. Vom leeren E-Mail-Feld per Tab zur Anmeldung und dann zum Anfordern wechseln.
      4. Eine leere und anschließend ungültige Anfrage prüfen; Fehlerdarstellung erfassen.
      5. Über den bestehenden Anmeldebutton zur vollständigen Anmeldung zurückkehren.
      **Erwartet:** Die Kartenstruktur ändert weder Geometrie noch Fokusfolge oder Validierung.
      Der tatsächliche Mailversand wird in den übrigen Reset-Fällen geprüft.
      """)
  void forgotPasswordCardPreservesLayoutAndKeyboardActions(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    captureForgotPasswordCardEvidence("initial", viewportWidth);

    Locator cardPanelLocator = browserPage.locator(".public-panel");
    Locator emailFieldLocator =
        cardPanelLocator.getByLabel(
            uiTexts.getPublicEmailField(), new Locator.GetByLabelOptions().setExact(true));
    Locator returnToLoginButtonLocator =
        cardPanelLocator.getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(uiTexts.getBaseLoginButton()).setExact(true));
    Locator requestLinkButtonLocator =
        cardPanelLocator.getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(uiTexts.getPublicRequestLink()).setExact(true));

    emailFieldLocator.focus();
    browserPage.keyboard().press("Tab");
    assertThat(returnToLoginButtonLocator).isFocused();
    browserPage.keyboard().press("Tab");
    assertThat(requestLinkButtonLocator).isFocused();
    browserPage.keyboard().press("Enter");
    forgotPasswordPage.assertRequiredError();

    forgotPasswordPage.enterEmail("not-an-email");
    forgotPasswordPage.submit();
    forgotPasswordPage.assertEmailError();
    captureForgotPasswordCardEvidence("invalid", viewportWidth);

    returnToLoginButtonLocator.click();
    new LoginPage(browserPage).assertFormVisible();
  }

  private void captureForgotPasswordCardEvidence(String formStateName, int viewportWidth) {
    Locator cardPanelLocator = browserPage.locator(".public-panel");
    browserPage.evaluate("() => document.fonts.ready");
    browserPage.mouse().move(0, 0);

    String measurementsJson =
        (String)
            cardPanelLocator.evaluate(
                """
                cardElement => {
                  const measureElement = element => {
                    const bounds = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height,
                      font: style.font, color: style.color, background: style.backgroundColor,
                      border: style.border, padding: style.padding, gap: style.gap};
                  };
                  return JSON.stringify({card: measureElement(cardElement), header: measureElement(cardElement.querySelector('.cosmos-panel-header')),
                    body: measureElement(cardElement.querySelector('.public-panel-body')), footer: measureElement(cardElement.querySelector('.public-panel-footer')),
                    field: measureElement(cardElement.querySelector('vaadin-email-field')),
                    buttons: Array.from(cardElement.querySelectorAll('vaadin-button')).map(measureElement)});
                }
                """);
    String evidenceName = "forgot-card-" + formStateName + "-" + viewportWidth;
    Allure.attachment(evidenceName + " measurements", "application/json", measurementsJson);
    AllurePlaywright.attachScreenshot(evidenceName, browserPage);
  }

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
    String email = registerAndVerifyAccountEmail();
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();

    MailpitInbox firstMailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail firstResetMail = firstMailpitInbox.awaitPasswordResetMail(email);
    try (Page originalMailPage = browserContext.newPage()) {
      originalMailPage.navigate(
          System.getProperty("browser.tests.mailpitUrl")
              + "/view/"
              + firstResetMail.id()
              + ".html");
      String originalMailBodyText = originalMailPage.locator("body").innerText();

      UiTexts otherUiTexts = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
      browserPage.navigate(path(HOME_ROUTE));
      new UserMenu(browserPage).selectLanguage(otherUiTexts.language());

      browserPage.navigate(path(FORGOT_PASSWORD_ROUTE));
      browserPage
          .getByLabel(
              otherUiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true))
          .fill(email);
      browserPage
          .getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions()
                  .setName(otherUiTexts.getPublicRequestLink())
                  .setExact(true))
          .click();

      MailpitInbox.CapturedMail secondResetMail =
          new MailpitInbox(browserContext, null, otherUiTexts).awaitPasswordResetMail(email);
      assertNotEquals(firstResetMail.id(), secondResetMail.id());

      try (Page resetActionPage =
          new MailpitInbox(browserContext, null, otherUiTexts)
              .openPasswordResetAction(secondResetMail)) {
        assertTrue(resetActionPage.url().startsWith(System.getProperty("browser.tests.baseUrl")));
      }
      try (Page newMailPage = browserContext.newPage()) {
        newMailPage.navigate(
            System.getProperty("browser.tests.mailpitUrl")
                + "/view/"
                + secondResetMail.id()
                + ".html");
        assertNotEquals(originalMailBodyText, newMailPage.locator("body").innerText());
      }

      originalMailPage.reload();
      assertEquals(originalMailBodyText, originalMailPage.locator("body").innerText());
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
    String email = registerAndVerifyAccountEmail();
    ResetPasswordPage resetPasswordPage = requestResetMailAndOpenPasswordResetPage(email);
    resetPasswordPage.fillPasswords(
        BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    resetPasswordPage.submit();
    resetPasswordPage.assertChanged();
    AllurePlaywright.attachScreenshot("Bestätigte Passwortänderung", browserPage);

    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    loginPage.assertRejected();
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
    String email = registerAndVerifyAccountEmail();
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
    String email = registerAndVerifyAccountEmail();
    ResetPasswordPage resetPasswordPage = requestResetMailAndOpenPasswordResetPage(email);
    resetPasswordPage.submit();
    resetPasswordPage.assertRequiredError();

    resetPasswordPage.fillPasswords("weak", "weak");
    resetPasswordPage.submit();
    resetPasswordPage.assertPasswordError();

    resetPasswordPage.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, "DifferentPassword3!");
    resetPasswordPage.submit();
    resetPasswordPage.assertConfirmationError();
    AllurePlaywright.attachScreenshot("Abweichende Passwortbestätigung", browserPage);

    resetPasswordPage.fillPasswords(
        BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    resetPasswordPage.submit();
    resetPasswordPage.assertChanged();
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
    String email = registerAndVerifyAccountEmail();
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    String knownResponseText = forgotPasswordPage.requestedResponse();
    new MailpitInbox(browserContext).awaitPasswordResetMail(email);

    forgotPasswordPage.dismissRequestedNotice();
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail("unknown-reset-" + UUID.randomUUID() + "@example.test");
    forgotPasswordPage.submit();

    assertEquals(knownResponseText, forgotPasswordPage.requestedResponse());
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
    String email = registerAndVerifyAccountEmail();
    ResetPasswordPage resetPasswordPage = requestResetMailAndOpenPasswordResetPage(email);
    resetPasswordPage.fillPasswords(
        BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    resetPasswordPage.submit();
    resetPasswordPage.assertChanged();

    browserPage.reload();
    resetPasswordPage.assertInvalid();
  }

  private String registerAndVerifyAccountEmail() {
    String accountEmail = "reset-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registrationPage = new RegistrationPage(browserPage);
    registrationPage.open();
    registrationPage.fillValid(accountEmail, BrowserTestAccounts.USER_PASSWORD);
    registrationPage.submit();
    registrationPage.assertSubmitted(accountEmail);

    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    browserPage =
        mailpitInbox.openVerificationLink(mailpitInbox.awaitVerificationMail(accountEmail));
    return accountEmail;
  }

  private ResetPasswordPage requestResetMailAndOpenPasswordResetPage(String accountEmail) {
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(accountEmail);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();

    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    browserPage =
        mailpitInbox.openPasswordResetLink(mailpitInbox.awaitPasswordResetMail(accountEmail));
    return new ResetPasswordPage(browserPage);
  }

  private void assertNewPasswordLogin(String accountEmail) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(accountEmail, BrowserTestAccounts.RESET_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    new UserMenu(browserPage).assertUserSignedInWithoutAdminAction(accountEmail);
  }
}
