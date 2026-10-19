package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.mail.MailpitInbox.CapturedMail;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.TokenConfigurationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.VerificationPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Tatsächlicher Tokenablauf und erneuter Mailversand")
public class TokenExpiryBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("VER-03A: Abgelaufener Mailbutton, erneuter Versand und tatsächliche Aktivierung")
  @Description(
      """
        1. Verifikationsgültigkeit als Admin vorübergehend auf die echte Mindestdauer eine Minute setzen.
        2. Eigenes Konto registrieren und reguläre SMTP-Mail einschließlich ersetztem Branding-Footer erfassen.
        3. 65 Sekunden tatsächlich abwarten, alten Mailbutton öffnen: abgelaufen, kein Aktivierungserfolg.
        4. Erneut senden anklicken; nur neue Mail-ID erfassen, deren echten Button nutzen und anmelden.
        5. Originaldauer über Adminoberfläche wiederherstellen.
        **Erwartet:** Ablauf und erneuter Versand funktionieren ohne künstliche Zeit-/Tokenmanipulation.
        """)
  void expiredVerificationCanBeResentAndActivated() {
    TokenConfigurationPage config = admin();
    String label = texts.getAdminConfigEntryVerificationTokenValidDuration();
    String original = config.value(label);
    try {
      config.enter(label, "PT1M");
      config.save();
      try (BrowserActor actor = new BrowserActor(browser)) {
        String email = email();
        RegistrationPage registration = new RegistrationPage(actor.page);
        registration.open();
        registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
        registration.submit();
        registration.assertSubmitted(email);
        MailpitInbox inbox = new MailpitInbox(actor.context);
        CapturedMail old = inbox.awaitVerificationMail(email);
        waitForExpiry(actor.context);
        try (Page expired = inbox.openVerificationAction(old)) {
          assertThat(
                  expired.getByRole(
                      AriaRole.HEADING,
                      new Page.GetByRoleOptions()
                          .setName(texts.getVerificationExpiredTitle())
                          .setExact(true)))
              .isVisible();
          expired
              .getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions().setName(texts.getBaseResendButton()).setExact(true))
              .click();
          assertThat(expired.locator("vaadin-notification-card:not([closing])"))
              .containsText(
                  texts.visibleText(expired, texts.getVerificationExpiredSuccessMessage()));
          CapturedMail fresh = inbox.awaitVerificationMailAfter(email, old);
          assertNotEquals(old.id(), fresh.id());
          inbox.openVerificationLink(fresh).close();
          try (Page stale = inbox.openVerificationAction(old)) {
            new VerificationPage(stale).assertExpired();
          }
        }
        actor.login(email, BrowserTestAccounts.USER_PASSWORD);
        assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(actor.page).selectTestLanguage();
        new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      }
    } finally {
      config.open();
      config.enter(label, original);
      config.save();
    }
  }

  @Test
  @DisplayName(
      "RESET-06A: Bereits geöffnetes Resetformular akzeptiert keinen inzwischen abgelaufenen Token")
  @Description(
      """
        1. Resetgültigkeit vorübergehend auf eine Minute setzen; eigenes Konto registrieren und aktivieren.
        2. Passwort vergessen über UI anfordern; echte Mail mit Branding-Footer öffnen: Formular sichtbar.
        3. 65 Sekunden tatsächlich abwarten; neues gültiges Passwort absenden: neutraler Ablaufhinweis.
        4. Ursprüngliches Passwort funktioniert weiterhin; Originaldauer wiederherstellen.
        **Erwartet:** Auch ein zuvor gültiges offenes Formular verlängert den Token nicht.
        """)
  void openResetFormCannotExtendItsToken() {
    TokenConfigurationPage config = admin();
    String label = texts.getAdminConfigEntryPasswordResetTokenValidDuration();
    String original = config.value(label);
    try {
      config.enter(label, "PT1M");
      config.save();
      try (BrowserActor actor = new BrowserActor(browser)) {
        String email = email();
        RegistrationPage registration = new RegistrationPage(actor.page);
        registration.open();
        registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
        registration.submit();
        registration.assertSubmitted(email);
        MailpitInbox inbox = new MailpitInbox(actor.context);
        inbox.openVerificationLink(inbox.awaitVerificationMail(email)).close();
        ForgotPasswordPage forgot = new ForgotPasswordPage(actor.page);
        forgot.open();
        forgot.enterEmail(email);
        forgot.submit();
        forgot.requestedResponse();
        try (Page reset = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email))) {
          waitForExpiry(actor.context);
          ResetPasswordPage form = new ResetPasswordPage(reset);
          form.fillPasswords("ChangedPassword!", "ChangedPassword!");
          form.submit();
          assertThat(reset.locator("vaadin-notification-card:not([closing])"))
              .containsText(texts.getResetPasswordInvalidText());
          assertThat(reset.locator("vaadin-notification-card:not([closing])"))
              .not()
              .containsText(texts.getBaseSuccessTitle());
          reset.reload();
          new ResetPasswordPage(reset).assertInvalid();
        }
        actor.login(email, "ChangedPassword!");
        new LoginPage(actor.page).assertRejected();
        actor.login(email, BrowserTestAccounts.USER_PASSWORD);
        assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(actor.page).selectTestLanguage();
        new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      }
    } finally {
      config.open();
      config.enter(label, original);
      config.save();
    }
  }

  @Test
  @DisplayName(
      "VER-02B: Verifikationsanfrage mit Pflichtfeld, Syntaxfehler und korrigierter echter Mail")
  @Description(
      """
        1. Eigenes unverifiziertes Konto registrieren, echte Mail erfassen und nicht aktivieren.
        2. Unbekannten Verifikationslink öffnen; angebotenes Anfrageformular leer absenden: Pflichtfeldfehler.
        3. Ungültige Adresse versuchen: Syntaxfehler; echte eigene Adresse korrigieren und senden.
        4. Neue reguläre Mail-ID erfassen und deren echten Button zur Aktivierung nutzen.
        **Erwartet:** Korrigierbares Formular mit lokalisierten Fehlern; erneuter Versand über echte UI.
        """)
  void verificationRequestValidatesAndCanBeCorrected() {
    String email = email();
    RegistrationPage registration = new RegistrationPage(page);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    MailpitInbox inbox = new MailpitInbox(context);
    CapturedMail old = inbox.awaitVerificationMail(email);
    page.navigate(path(VERIFICATION_ROUTE) + "/" + UUID.randomUUID());
    new VerificationPage(page).assertUnknownToken();
    Locator input =
        page.getByLabel(
            texts.getVerificationEmailField(), new Page.GetByLabelOptions().setExact(true));
    Locator send =
        page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getBaseSendButton()).setExact(true));
    send.click();
    checkRequestError(texts.getBaseValidationRequiredMessage());
    input.fill("invalid");
    send.click();
    checkRequestError(texts.getBaseValidationEmailInvalidMessage());
    input.fill(email);
    send.click();
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.visibleText(page, texts.getVerificationNotFoundSuccessMessage()));
    inbox.openVerificationLink(inbox.awaitVerificationMailAfter(email, old)).close();
  }

  private void checkRequestError(String message) {
    Locator notice = page.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).containsText(texts.visibleText(page, message));
    assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(1);
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  private TokenConfigurationPage admin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    TokenConfigurationPage config = new TokenConfigurationPage(page);
    config.open();
    return config;
  }

  private void waitForExpiry(BrowserContext context) {
    long end = System.nanoTime() + java.time.Duration.ofSeconds(65).toNanos();
    context.waitForCondition(
        () -> System.nanoTime() >= end,
        new BrowserContext.WaitForConditionOptions().setTimeout(70_000));
  }

  private String email() {
    return "expiry-" + UUID.randomUUID() + "@example.test";
  }
}
