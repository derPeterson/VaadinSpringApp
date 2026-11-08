package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Echte Inaktivitätsgrenze auch für serverseitige Updates ohne Browser-RPC. */
@Tag("browser")
public class TwoFactorPushBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-13: Nur echte Eingaben verlängern 2FA; abgelaufene Vorschauen erhalten keine Push-Inhalte")
  void inactivityAlsoProtectsServerSideNotificationUpdates(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "push-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(browser)) {
      user.page.setViewportSize(width, 900);
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      user.page.navigate("/account?section=security&tab=portal");
      user.page.locator("#account-two-factor-setup").click();
      user.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      user.page.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(user.page, email);
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      try {
        configureInactivity(true);
        user.page.navigate("/home");
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        user.page.navigate("/home");
        NotificationInboxPage inbox = new NotificationInboxPage(user.page);
        inbox.openPreview();
        user.page.waitForTimeout(35_000);
        // Playwright-Tastatur erzeugt einen echten isTrusted-Keydown, kein synthetisches DOM-Event.
        user.page.keyboard().press("Shift");
        user.page.waitForTimeout(35_000);
        String first = "Still confirmed " + UUID.randomUUID();
        send(email, first);
        assertThat(inbox.row(inbox.preview(), first)).isVisible();
        // Hintergrund-GETs dürfen den Nachweis nicht erneuern. Keine Benutzerinteraktion.
        user.page.waitForTimeout(30_000);
        user.page.evaluate("async () => { await fetch('/home', {cache:'no-store'}); }");
        user.page.waitForTimeout(35_000);
        String denied = "Must remain hidden " + UUID.randomUUID();
        send(email, denied);
        assertThat(inbox.preview()).isHidden();
        // Das Schließen des nativen Popovers meldet dessen Zustand zurück. Der alte RPC wird
        // ebenfalls gesperrt und führt die ganze Ansicht unmittelbar zur erneuten Bestätigung.
        assertThat(user.page).hasURL(Pattern.compile(".*/two-factor.*"));
        assertThat(user.page.getByText(denied, new Page.GetByTextOptions().setExact(true)))
            .hasCount(0);
        AllurePlaywright.attachScreenshot(
            "Abgelaufene 2FA sperrt serverseitige Vorschau", user.page);
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        user.page.navigate("/home");
        inbox.openPreview();
        assertThat(inbox.row(inbox.preview(), denied)).isVisible();
      } finally {
        configureInactivity(false);
      }
    }
  }

  private void openConfiguration() {
    page.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    page.navigate("/admin?section=configuration&group=two_factor");
  }

  private void configureInactivity(boolean enabled) {
    openConfiguration();
    page.getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(texts.getTwoFactorInactivityEnabled())
                .setExact(true))
        .setChecked(true);
    Locator duration = page.locator("#two-factor-inactivity-duration");
    duration.locator("vaadin-select").click();
    String unit =
        enabled
            ? texts.getAdminConfigDurationUnitMinutes()
            : texts.getAdminConfigDurationUnitHours();
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(unit).setExact(true))
        .click();
    duration.locator("vaadin-integer-field input").fill(enabled ? "1" : "12");
    page.locator("#two-factor-inactivity-label").click();
    page.getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(texts.getTwoFactorInactivityEnabled())
                .setExact(true))
        .setChecked(enabled);
    page.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        page, TwoFactorPage.nextCode(page, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(page, () -> page.locator("#two-factor-action-confirm").click());
    page.waitForCondition(
        () ->
            java.net.URI.create(page.url()).getPath().equals("/two-factor")
                || page.locator("vaadin-notification-card:not([closing])")
                    .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorSaved()))
                    .isVisible());
    openConfiguration();
    assertThat(page.locator("#two-factor-inactivity-enabled")).hasJSProperty("checked", enabled);
    assertThat(page.locator("#two-factor-inactivity-duration vaadin-integer-field input"))
        .hasValue(enabled ? "1" : "12");
    if (!enabled) {
      assertThat(page.locator("#two-factor-inactivity-duration vaadin-integer-field input"))
          .isDisabled();
      // Ausschalten erhält den Entwurf. Neuladen stellt den gespeicherten Stundenwert wieder her.
      page.getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(true);
      duration = page.locator("#two-factor-inactivity-duration");
      duration.locator("vaadin-select").click();
      page.getByRole(
              AriaRole.OPTION,
              new Page.GetByRoleOptions()
                  .setName(texts.getAdminConfigDurationUnitDays())
                  .setExact(true))
          .click();
      page.getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(false);
      assertThat(duration.locator("vaadin-integer-field input")).hasValue("12");
      assertThat(duration.locator("vaadin-select"))
          .containsText(texts.getAdminConfigDurationUnitDays());
      page.locator("#two-factor-reload").click();
      assertThat(duration.locator("vaadin-integer-field input")).hasValue("12");
      assertThat(duration.locator("vaadin-select"))
          .containsText(texts.getAdminConfigDurationUnitHours());
    }
  }

  private void send(String email, String title) {
    page.navigate("/admin?section=notifications");
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    NotificationComposePage compose = new NotificationComposePage(page);
    compose.open();
    compose.select(email);
    compose.draft(title, "Own isolated notification for the second-factor boundary.");
    compose.send().click();
    compose.notice(texts.getNotificationsSent());
  }
}
