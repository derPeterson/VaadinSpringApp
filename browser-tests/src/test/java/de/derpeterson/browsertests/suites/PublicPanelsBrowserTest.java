package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.MAINTENANCE_PAGE_TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AccountSecurityPage;
import de.derpeterson.browsertests.pages.ActionGridChecks;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.VerificationPage;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Öffentliche Karten über echte UI-Abläufe und eigene Mailpit-Links vergleichen. */
@Tag("browser")
@Feature("Öffentliche Karten")
public class PublicPanelsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-01: Anmeldung und Registrierung behalten Formular und Anbieterkarten")
  void loginAndRegistrationPanels(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    new LoginPage(browserPage).open();
    assertThat(browserPage.locator("#external-login-google")).isVisible();
    assertThat(browserPage.locator("#external-login-microsoft")).isVisible();
    capturePanelEvidence(browserPage, "login", viewportWidth);

    new RegistrationPage(browserPage).open();
    assertThat(browserPage.locator("#external-login-google")).isVisible();
    capturePanelEvidence(browserPage, "registration", viewportWidth);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-02: Ausstehende, erfolgreiche und unbekannte E-Mail-Bestätigung")
  void verificationPanels(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    String accountEmailAddress = registerAccountEmail("verification", viewportWidth);
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail verificationMail =
        mailpitInbox.awaitVerificationMail(accountEmailAddress);

    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signInFirstFactor(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage.locator("#pending-verification-email"))
        .containsText(accountEmailAddress);
    ActionGridChecks.assertActions(
        browserPage.locator(".pending-verification-actions"), 2, viewportWidth <= 479);
    capturePanelEvidence(browserPage, "verification-pending", viewportWidth);

    try (Page verificationPage = mailpitInbox.openVerificationLink(verificationMail)) {
      setViewport(verificationPage, viewportWidth);
      capturePanelEvidence(verificationPage, "verification-success", viewportWidth);
    }
    // Eigener neuer Kontext: die noch vorläufige Anmeldung darf fremde Routen sperren.
    try (BrowserActor visitorActor = new BrowserActor(testBrowser)) {
      setViewport(visitorActor.browserPage, viewportWidth);
      visitorActor.browserPage.navigate("/verification/00000000-0000-0000-0000-000000000001");
      new VerificationPage(visitorActor.browserPage).assertUnknownToken();
      capturePanelEvidence(visitorActor.browserPage, "verification-unknown", viewportWidth);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-03: Passwortanfrage, Resetformular, Eingabefehler und Ergebnis")
  void passwordResetPanels(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    String accountEmailAddress = registerVerifiedAccountEmail("reset", viewportWidth);
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserPage);
    forgotPasswordPage.open();
    capturePanelEvidence(browserPage, "forgot-password", viewportWidth);
    forgotPasswordPage.enterEmail(accountEmailAddress);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();

    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    try (Page resetActionPage =
        mailpitInbox.openPasswordResetLink(
            mailpitInbox.awaitPasswordResetMail(accountEmailAddress))) {
      setViewport(resetActionPage, viewportWidth);
      ResetPasswordPage resetPasswordPage = new ResetPasswordPage(resetActionPage);
      capturePanelEvidence(resetActionPage, "reset-form", viewportWidth);
      resetPasswordPage.submit();
      resetPasswordPage.assertRequiredError();
      capturePanelEvidence(resetActionPage, "reset-invalid-input", viewportWidth);

      resetPasswordPage.fillPasswords("ChangedPassword123!", "ChangedPassword123!");
      resetPasswordPage.submit();
      resetPasswordPage.assertChanged();
      capturePanelEvidence(resetActionPage, "reset-success", viewportWidth);
      resetActionPage.reload();
      resetPasswordPage.assertInvalid();
      capturePanelEvidence(resetActionPage, "reset-used-link", viewportWidth);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-04: Neue E-Mail-Adresse wird erst durch den Bestätigungsbutton übernommen")
  void emailChangeConfirmationPanels(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    String accountEmailAddress = registerVerifiedAccountEmail("email-change", viewportWidth);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);

    String changedEmailAddress = "cards-changed-" + viewportWidth + "@example.test";
    new UserMenu(browserPage).selectTestLanguage();
    new AccountPage(browserPage).open();
    AccountSecurityPage accountSecurityPage = new AccountSecurityPage(browserPage);
    accountSecurityPage.openEmail();
    accountSecurityPage.email(
        changedEmailAddress, changedEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    accountSecurityPage.requestEmail();

    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail confirmationMail =
        mailpitInbox.awaitSecurityMail(changedEmailAddress, uiTexts.getAccountEmailChangeSubject());
    try (Page confirmationPage = mailpitInbox.openEmailChangeAction(confirmationMail)) {
      setViewport(confirmationPage, viewportWidth);
      assertThat(confirmationPage.locator("#account-email-confirm-action")).isVisible();
      capturePanelEvidence(confirmationPage, "email-change-confirm", viewportWidth);
      UiActions.complete(
          confirmationPage,
          () -> confirmationPage.locator("#account-email-confirm-action").click());
      assertThat(confirmationPage.locator("#account-email-confirmation"))
          .containsText(uiTexts.getAccountEmailConfirmationSuccess());
      // Die Bestätigung widerruft die bisherige Sitzung. Die Erfolgsmeldung schließt clientseitig.
      assertThat(confirmationPage.locator("vaadin-notification-card"))
          .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(10_000));
      capturePanelEvidence(confirmationPage, "email-change-success", viewportWidth);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-05: Selbstdeaktivierung führt über Hinweisseite und echten Reaktivierungslink")
  void accountReactivationPanels(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    String accountEmailAddress = registerVerifiedAccountEmail("reactivation", viewportWidth);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);

    browserPage.navigate("/account?section=security&tab=lifecycle");
    browserPage.locator("#account-deactivate").click();
    browserPage
        .locator("#account-lifecycle-password input")
        .fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#account-lifecycle-submit").click();
    assertThat(browserPage.getByText(uiTexts.getAccountDeactivatedSuccess())).isVisible();

    loginPage.open();
    loginPage.signInFirstFactor(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage.locator("#pending-reactivation-email"))
        .containsText(accountEmailAddress);
    ActionGridChecks.assertActions(
        browserPage.locator(".pending-verification-actions"), 2, viewportWidth <= 479);
    capturePanelEvidence(browserPage, "reactivation-pending", viewportWidth);

    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail deactivationMail =
        mailpitInbox.awaitSecurityMail(
            accountEmailAddress, uiTexts.getAccountDeactivatedMailSubject());
    try (Page reactivationPage = mailpitInbox.openAccountReactivationAction(deactivationMail)) {
      setViewport(reactivationPage, viewportWidth);
      assertThat(reactivationPage.locator("#account-reactivation-submit")).isVisible();
      capturePanelEvidence(reactivationPage, "reactivation-confirm", viewportWidth);
      reactivationPage.locator("#account-reactivation-submit").click();
      assertThat(reactivationPage.locator(".public-panel"))
          .containsText(uiTexts.getAccountReactivateSuccess());
      capturePanelEvidence(reactivationPage, "reactivation-success", viewportWidth);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-06: Google-Erstregistrierung behält Felder und Abschlussaktion")
  void externalRegistrationPanel(int viewportWidth) {
    setViewport(browserPage, viewportWidth);
    new LoginPage(browserPage).open();
    browserPage.locator("#external-login-google").click();
    assertThat(browserPage.locator("#fixture-email")).isVisible();
    browserPage.locator("#fixture-email").fill("cards-google-" + viewportWidth + "@gmail.com");
    browserPage.locator("#fixture-mode").selectOption("valid");
    browserPage.locator("#fixture-signin").click();

    assertThat(browserPage.locator("#external-register-submit")).isVisible();
    capturePanelEvidence(browserPage, "external-registration", viewportWidth);
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage.locator("#external-register-submit")).hasCount(0);
    new UserMenu(browserPage)
        .assertUserSignedInWithoutAdminAction("cards-google-" + viewportWidth + "@gmail.com");
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("CARD-07: Wartungskarte erhält Hinweis und HTTP-Sperre")
  void maintenancePanel(int viewportWidth) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.SECURITY);
    boolean originalMaintenanceMode =
        configurationPage.input(uiTexts.getAdminConfigEntryMaintenanceMode()).isChecked();

    try {
      configurationPage.input(uiTexts.getAdminConfigEntryMaintenanceMode()).setChecked(true);
      configurationPage.save();
      try (BrowserActor visitorActor = new BrowserActor(testBrowser, MAINTENANCE_PAGE_TITLE)) {
        setViewport(visitorActor.browserPage, viewportWidth);
        new UserMenu(visitorActor.browserPage).selectTestLanguage();
        Response maintenanceResponse = visitorActor.browserPage.navigate("/home");
        assertEquals(503, maintenanceResponse.status());
        assertThat(visitorActor.browserPage.locator(".public-panel"))
            .containsText(uiTexts.getMaintenanceDescription());
        capturePanelEvidence(visitorActor.browserPage, "maintenance", viewportWidth);
      }
    } finally {
      configurationPage.open(AdminConfigGroup.SECURITY);
      configurationPage
          .input(uiTexts.getAdminConfigEntryMaintenanceMode())
          .setChecked(originalMaintenanceMode);
      configurationPage.save();
    }
  }

  private String registerAccountEmail(String scenarioName, int viewportWidth) {
    String accountEmailAddress = "cards-" + scenarioName + "-" + viewportWidth + "@example.test";
    RegistrationPage registrationPage = new RegistrationPage(browserPage);
    registrationPage.open();
    registrationPage.fillValid(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    registrationPage.submit();
    registrationPage.assertSubmitted(accountEmailAddress);
    return accountEmailAddress;
  }

  private String registerVerifiedAccountEmail(String scenarioName, int viewportWidth) {
    String accountEmailAddress = registerAccountEmail(scenarioName, viewportWidth);
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    try (Page verificationPage =
        mailpitInbox.openVerificationLink(
            mailpitInbox.awaitVerificationMail(accountEmailAddress))) {
      new VerificationPage(verificationPage).assertActivated();
    }
    return accountEmailAddress;
  }

  private void setViewport(Page targetPage, int viewportWidth) {
    targetPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
  }

  private void capturePanelEvidence(Page targetPage, String stateName, int viewportWidth) {
    Locator notificationCardsLocator =
        targetPage.locator("vaadin-notification-card:not([closing])");
    while (notificationCardsLocator.count() > 0) {
      notificationCardsLocator.first().getByRole(AriaRole.BUTTON).click();
      assertThat(notificationCardsLocator).hasCount(0);
    }

    targetPage.evaluate("() => document.fonts.ready");
    targetPage.mouse().move(0, 0);
    targetPage.evaluate("() => window.scrollTo(0, 0)");
    assertThat(targetPage.locator(".public-panel")).isVisible();

    String measurementsJson =
        (String)
            targetPage.evaluate(
                """
                () => {
                  const measure = element => {
                    const bounds = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height,
                      font: style.font, color: style.color, background: style.backgroundColor,
                      border: style.border, padding: style.padding};
                  };
                  return JSON.stringify({viewport: {width: innerWidth, height: innerHeight},
                    cards: [...document.querySelectorAll('.public-panel, .external-login-panel')].map(card => ({
                      card: measure(card),
                      elements: [...card.querySelectorAll('.cosmos-panel-header, .cosmos-panel-heading, .public-panel-body, .public-panel-footer, .external-login-kicker, h2, .external-login-hint, .external-provider-actions, vaadin-text-field, vaadin-email-field, vaadin-password-field, vaadin-button')].map(measure)
                    }))});
                }
                """);

    String evidenceName = "public-card-" + stateName + "-" + viewportWidth;
    Allure.attachment(evidenceName + " measurements", "application/json", measurementsJson);
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(
            targetPage.screenshot(
                new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
