package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Alte Mailbuttons nach Adminänderung")
public class BlockedLinksBrowserTest extends BrowserFixture {
  @ParameterizedTest(name = "{0}: Verifikationslink nach Sperre, vorher aktiviert={1}")
  @CsvSource({"SEC-LINK-01A,false", "SEC-LINK-01B,true"})
  @Description(
      """
        1. Eigenes Konto registrieren; echten Mailbutton entweder unbenutzt lassen oder zur Aktivierung nutzen.
        2. Als Admin genau dieses Konto ausdrücklich deaktivieren und speichern.
        3. Alten echten Mailbutton erneut öffnen und erneuten Versand versuchen: kein Aktivierungserfolg.
        4. In unabhängiger Sitzung anmelden: Konto bleibt gesperrt.
        **Erwartet:** Alte unbenutzte wie bereits genutzte Links heben eine Adminsperre nicht auf.
        """)
  void verificationCannotUndoAnAdminBlock(String caseId, boolean previouslyActivated) {
    String account = email("old-verification");
    try (BrowserActor actor = new BrowserActor(browser)) {
      RegistrationPage registration = new RegistrationPage(actor.page);
      registration.open();
      registration.fillValid(account, BrowserTestAccounts.USER_PASSWORD);
      registration.submit();
      registration.assertSubmitted(account);
      MailpitInbox inbox = new MailpitInbox(actor.context);
      var mail = inbox.awaitVerificationMail(account);
      if (previouslyActivated) inbox.openVerificationLink(mail).close();
      AdminUserDialog form = admin().openEdit(account);
      form.setEnabled(false);
      form.save();
      try (Page result = inbox.openVerificationAction(mail)) {
        assertThat(
                result.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                        .setName(texts.getVerificationExpiredTitle())
                        .setExact(true)))
            .isVisible();
        assertThat(
                result.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                        .setName(texts.getVerificationSuccessTitle())
                        .setExact(true)))
            .hasCount(0);
        result
            .getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(texts.getBaseResendButton()).setExact(true))
            .click();
        assertThat(result.locator("vaadin-notification-card:not([closing])"))
            .containsText(texts.visibleText(result, texts.getBaseFailedMessage()));
      }
      AdminUserDialog stored = new AdminUsersPage(page).openEdit(account);
      assertThat(stored.enabled()).not().isChecked();
      stored.cancel();
      actor.login(account, BrowserTestAccounts.USER_PASSWORD);
      new LoginPage(actor.page).assertRejected();
    }
  }

  @ParameterizedTest(name = "{0}: Resetlink nach {1}")
  @CsvSource({"SEC-LINK-02A,disable", "SEC-LINK-02B,delete"})
  @Description(
      """
        1. Eigenes aktives Konto im Adminformular anlegen; separat Reset anfordern und SMTP-Mail erfassen.
        2. Konto als Admin sperren oder mit Bestätigung löschen.
        3. Alten echten Mailbutton öffnen: neutraler Fehler ohne Passwortformular; Anmeldung abgewiesen.
        4. Nur beim gesperrten Konto wieder freigeben: ursprüngliches Passwort funktioniert weiterhin.
        **Erwartet:** Alte Resetlinks ändern weder gesperrte noch gelöschte Konten.
        """)
  void resetCannotChangeABlockedOrDeletedAccount(String caseId, String change) {
    AdminUsersPage users = admin();
    String account = email("old-reset");
    users.createActiveUser(account, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor actor = new BrowserActor(browser)) {
      ForgotPasswordPage forgot = new ForgotPasswordPage(actor.page);
      forgot.open();
      forgot.enterEmail(account);
      forgot.submit();
      forgot.requestedResponse();
      MailpitInbox inbox = new MailpitInbox(actor.context);
      var mail = inbox.awaitPasswordResetMail(account);
      AdminUserDialog form = users.openEdit(account);
      if (change.equals("delete")) form.requestDelete().confirm();
      else {
        form.setEnabled(false);
        form.save();
      }
      try (Page result = inbox.openPasswordResetAction(mail)) {
        new ResetPasswordPage(result).assertInvalid();
      }
      actor.login(account, BrowserTestAccounts.USER_PASSWORD);
      new LoginPage(actor.page).assertRejected();
      if (change.equals("disable")) {
        form = users.openEdit(account);
        form.setEnabled(true);
        form.save();
        actor.login(account, BrowserTestAccounts.USER_PASSWORD);
        assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(actor.page).selectTestLanguage();
        new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(account);
      } else {
        users.open();
        users.search(account);
        assertThat(users.emailCells(account)).hasCount(0);
      }
    }
  }

  @Test
  @DisplayName(
      "RESET-03C: Administrativ gesperrte und unbekannte Adresse erhalten denselben Anfragehinweis")
  @Description(
      """
        1. Eigenes Konto anlegen und als Admin ausdrücklich sperren.
        2. In separatem anonymem Kontext Reset anfordern und sichtbaren Hinweis erfassen.
        3. Hinweis schließen und unbekannte eigene Adresse anfragen: exakt derselbe Hinweis.
        **Erwartet:** Keine sichtbare Kontoauskunft; keine Aussage zu Timing oder Mailabwesenheit.
        """)
  void blockedResetRequestIsNeutral() {
    AdminUsersPage users = admin();
    String account = email("neutral-blocked");
    users.createActiveUser(account, BrowserTestAccounts.USER_PASSWORD);
    AdminUserDialog form = users.openEdit(account);
    form.setEnabled(false);
    form.save();
    try (BrowserActor actor = new BrowserActor(browser)) {
      ForgotPasswordPage forgot = new ForgotPasswordPage(actor.page);
      forgot.open();
      forgot.enterEmail(account);
      forgot.submit();
      String blocked = forgot.requestedResponse();
      forgot.dismissRequestedNotice();
      forgot.enterEmail(email("unknown"));
      forgot.submit();
      assertEquals(blocked, forgot.requestedResponse());
    }
  }

  private AdminUsersPage admin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    return new AdminUsersPage(page);
  }

  private String email(String purpose) {
    return purpose + "-" + UUID.randomUUID() + "@example.test";
  }
}
