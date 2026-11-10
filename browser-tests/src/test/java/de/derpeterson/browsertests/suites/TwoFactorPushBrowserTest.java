package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Echte Inaktivitätsgrenze auch für serverseitige Updates ohne Browser-RPC. */
@Tag("browser")
public class TwoFactorPushBrowserTest extends BrowserFixture {
  @Test
  @Tag("follow-up-regression")
  @Tag("ticket-browser-detail")
  @DisplayName("MFA-24: Ticketpunkt erscheint und verschwindet außerhalb des Ticketsystems")
  void ticketActivityChangesWhileTheDashboardStaysOpen() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String suffix = UUID.randomUUID() + "@example.test";
    String observerEmail = "ticket-activity-admin-" + suffix;
    String requesterEmail = "ticket-activity-user-" + suffix;
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.createUser(observerEmail, BrowserTestAccounts.USER_PASSWORD, true, true);
    users.createActiveUser(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor observer = new BrowserActor(testBrowser);
        BrowserActor requester = new BrowserActor(testBrowser)) {
      observer.login(observerEmail, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(observer.browserPage).selectTestLanguage();
      observer.browserPage.navigate("/admin");
      assertThat(observer.browserPage.locator("#admin-nav-support")).isVisible();
      if (observer.browserPage.locator("#admin-nav-support-activity").isVisible()) {
        clearObserverUnreadTickets(observer.browserPage);
      }
      try (Page mobile = observer.browserContext.newPage()) {
        mobile.setViewportSize(390, 900);
        observer.browserPage.navigate("/admin");
        mobile.navigate("/admin");
        UiActions.complete(mobile, () -> mobile.locator("#account-navigation-toggle").click());
        for (Page view : List.of(observer.browserPage, mobile)) {
          assertThat(view.locator("#admin-nav-support")).isVisible();
          assertThat(view.locator("#admin-nav-support-activity")).isHidden();
        }
        requester.login(requesterEmail, BrowserTestAccounts.USER_PASSWORD);
        requester.browserPage.navigate("/recovery-help");
        requester
            .browserPage
            .locator("#support-reply textarea")
            .fill("Fresh dashboard ticket activity.");
        UiActions.complete(
            requester.browserPage, () -> requester.browserPage.locator("#support-create").click());
        String number =
            requester
                .browserPage
                .locator("#support-ticket-grid")
                .getByText(Pattern.compile("^#[0-9]+$"))
                .first()
                .innerText();
        for (Page view : List.of(observer.browserPage, mobile)) {
          assertThat(view.locator("#admin-nav-support-activity")).isVisible();
          view.locator("#admin-nav-support").hover();
          assertThat(ticketActivityTooltip(view)).hasText(uiTexts.getSupportNewActivity());
        }
        browserPage.navigate("/admin?section=support&ticket=" + number.substring(1));
        UiActions.complete(browserPage, () -> browserPage.locator("#support-claim").click());
        for (Page view : List.of(observer.browserPage, mobile)) {
          assertThat(view.locator("#admin-nav-support-activity")).isHidden();
          assertThat(view.locator("#support-ticket-categories")).hasCount(0);
          Locator activityTooltip = ticketActivityTooltip(view);
          assertThat(activityTooltip).isHidden();
          view.mouse().move(0, 0);
          view.locator("#admin-nav-support").hover();
          // Der Mauszeiger bleibt länger als die normale Öffnungsverzögerung auf dem Link.
          view.waitForTimeout(700);
          assertFalse(activityTooltip.isVisible());
        }
        UiActions.complete(browserPage, () -> browserPage.locator("#support-close").click());
      }
      new UserMenu(observer.browserPage).signOut();
    }
  }

  private Locator ticketActivityTooltip(Page view) {
    return view.getByRole(AriaRole.TOOLTIP)
        .filter(new Locator.FilterOptions().setHasText(uiTexts.getSupportNewActivity()));
  }

  private void clearObserverUnreadTickets(Page observer) {
    String route = "/admin?section=support";
    Locator cards = observer.locator("#support-ticket-grid .support-admin-ticket-card:visible");
    Set<String> seen = new HashSet<>();
    while (true) {
      observer.navigate(route);
      observer.locator("#support-filter").click();
      Locator panel = observer.locator("#support-filter-panel");
      panel
          .locator("#support-filter-reading")
          .getByText(
              uiTexts.getNotificationsUnread(), new Locator.GetByTextOptions().setExact(true))
          .click();
      UiActions.complete(observer, () -> panel.locator("#support-filter-apply").click());
      if (cards.count() == 0) {
        return;
      }
      String number = cards.first().getByText(Pattern.compile("^#[0-9]+$")).innerText().strip();
      org.junit.jupiter.api.Assertions.assertTrue(
          seen.add(number), "Ticket bleibt ungelesen: " + number);
      observer.navigate(route + "&ticket=" + number.substring(1));
      assertThat(observer.locator("#support-ticket-panel .support-ticket-number"))
          .hasText(uiTexts.getSupportTicketNumber(number));
      assertThat(
              cards
                  .filter(new Locator.FilterOptions().setHasText(number))
                  .locator(".support-ticket-unread"))
          .hasCount(0);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-13: Nur echte Eingaben verlängern 2FA; abgelaufene Vorschauen erhalten keine Push-Inhalte")
  void inactivityAlsoProtectsServerSideNotificationUpdates(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "push-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(testBrowser)) {
      user.browserPage.setViewportSize(width, 900);
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      user.browserPage.navigate("/account?section=security&tab=portal");
      user.browserPage.locator("#account-two-factor-setup").click();
      user.browserPage
          .locator("#two-factor-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      user.browserPage.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(user.browserPage, email);
      assertThat(user.browserPage)
          .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
      try {
        configureInactivity(true);
        user.browserPage.navigate("/home");
        TwoFactorPage.completeIfRequired(
            user.browserPage, email, BrowserTestAccounts.USER_PASSWORD);
        user.browserPage.navigate("/home");
        NotificationInboxPage inbox = new NotificationInboxPage(user.browserPage);
        inbox.openPreview();
        user.browserPage.waitForTimeout(35_000);
        // Playwright-Tastatur erzeugt einen echten isTrusted-Keydown, kein synthetisches DOM-Event.
        user.browserPage.keyboard().press("Shift");
        user.browserPage.waitForTimeout(35_000);
        String first = "Still confirmed " + UUID.randomUUID();
        send(email, first);
        assertThat(inbox.row(inbox.preview(), first)).isVisible();
        // Hintergrund-GETs dürfen den Nachweis nicht erneuern. Keine Benutzerinteraktion.
        user.browserPage.waitForTimeout(30_000);
        user.browserPage.evaluate("async () => { await fetch('/home', {cache:'no-store'}); }");
        user.browserPage.waitForTimeout(35_000);
        String denied = "Must remain hidden " + UUID.randomUUID();
        send(email, denied);
        assertThat(inbox.preview()).isHidden();
        // Das Schließen des nativen Popovers meldet dessen Zustand zurück. Der alte RPC wird
        // ebenfalls gesperrt und führt die ganze Ansicht unmittelbar zur erneuten Bestätigung.
        assertThat(user.browserPage).hasURL(Pattern.compile(".*/two-factor.*"));
        assertThat(user.browserPage.getByText(denied, new Page.GetByTextOptions().setExact(true)))
            .hasCount(0);
        AllurePlaywright.attachScreenshot(
            "Abgelaufene 2FA sperrt serverseitige Vorschau", user.browserPage);
        TwoFactorPage.completeIfRequired(
            user.browserPage, email, BrowserTestAccounts.USER_PASSWORD);
        user.browserPage.navigate("/home");
        inbox.openPreview();
        assertThat(inbox.row(inbox.preview(), denied)).isVisible();
      } finally {
        configureInactivity(false);
      }
    }
  }

  private void openConfiguration() {
    String location = "/admin?section=configuration&group=two_factor";
    // Eine vom alten Prüftab bereits ausgelöste Weiterleitung darf den neuen Abruf nicht abbrechen.
    // Die getrennte Nutzersitzung mit der alten Benachrichtigungsvorschau bleibt geöffnet.
    Page next = browserContext.newPage();
    browserPage.close();
    browserPage = next;
    browserPage.navigate(location);
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    if (!browserPage.url().endsWith(location)) {
      browserPage.navigate(location);
    }
    assertThat(browserPage.locator("#two-factor-save")).isVisible();
  }

  private void configureInactivity(boolean enabled) {
    openConfiguration();
    browserPage
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getTwoFactorInactivityEnabled())
                .setExact(true))
        .setChecked(true);
    Locator duration = browserPage.locator("#two-factor-inactivity-duration");
    duration.locator("vaadin-select").click();
    String unit =
        enabled
            ? uiTexts.getAdminConfigDurationUnitMinutes()
            : uiTexts.getAdminConfigDurationUnitHours();
    browserPage
        .getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(unit).setExact(true))
        .click();
    duration.locator("vaadin-integer-field input").fill(enabled ? "1" : "12");
    browserPage.locator("#two-factor-inactivity-label").click();
    browserPage
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getTwoFactorInactivityEnabled())
                .setExact(true))
        .setChecked(enabled);
    browserPage.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        browserPage, TwoFactorPage.nextCode(browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(
        browserPage, () -> browserPage.locator("#two-factor-action-confirm").click());
    browserPage.waitForCondition(() ->
        java.net.URI.create(browserPage.url()).getPath().equals("/two-factor")
            || browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorSaved()))
                .isVisible());
    openConfiguration();
    assertThat(browserPage.locator("#two-factor-inactivity-enabled"))
        .hasJSProperty("checked", enabled);
    assertThat(browserPage.locator("#two-factor-inactivity-duration vaadin-integer-field input"))
        .hasValue(enabled ? "1" : "12");
    if (!enabled) {
      assertThat(browserPage.locator("#two-factor-inactivity-duration vaadin-integer-field input"))
          .isDisabled();
      // Ausschalten erhält den Entwurf. Neuladen stellt den gespeicherten Stundenwert wieder her.
      browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(true);
      duration = browserPage.locator("#two-factor-inactivity-duration");
      duration.locator("vaadin-select").click();
      browserPage
          .getByRole(
              AriaRole.OPTION,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getAdminConfigDurationUnitDays())
                  .setExact(true))
          .click();
      browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(false);
      assertThat(duration.locator("vaadin-integer-field input")).hasValue("12");
      assertThat(duration.locator("vaadin-select"))
          .containsText(uiTexts.getAdminConfigDurationUnitDays());
      browserPage.locator("#two-factor-reload").click();
      assertThat(duration.locator("vaadin-integer-field input")).hasValue("12");
      assertThat(duration.locator("vaadin-select"))
          .containsText(uiTexts.getAdminConfigDurationUnitHours());
    }
  }

  private void send(String email, String title) {
    browserPage.navigate("/admin?section=notifications");
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    NotificationComposePage compose = new NotificationComposePage(browserPage);
    compose.open();
    compose.select(email);
    compose.draft(title, "Own isolated notification for the second-factor boundary.");
    compose.send().click();
    compose.notice(uiTexts.getNotificationsSent());
  }
}
