package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminNavigation;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Gemeinsame native Karten")
public class NativeCardsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "CARD-01: Konto- und Verwaltungskarten bleiben erreichbar und behalten ihre Geometrie")
  @Description(
      "Konto mit drei Sicherheitsreitern, Sitzungen, Postfach und administrative Karten über die UI öffnen. Ansichten und Messwerte für den Vorher-/Nachher-Vergleich sichern; Aktionen und Karten bleiben horizontal erreichbar.")
  void accountAndAdministrativeCardsKeepTheirArrangement(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    AccountPage accountPage = new AccountPage(browserPage);
    accountPage.open();
    captureCards("account-profile", "#account-panel-profile");
    accountPage.section("account-tab-security");
    for (String securityTab : new String[] {"portal", "providers", "lifecycle"}) {
      browserPage.locator("#account-security-tab-" + securityTab).click();
      captureCards("account-security-" + securityTab, "#account-panel-security");
    }
    accountPage.section("account-tab-sessions");
    captureCards("account-sessions", "#account-panel-sessions");
    browserPage.navigate(path(NOTIFICATIONS_ROUTE));
    captureCards("inbox", ".cosmos-notification-inbox-card");

    browserPage.navigate(path(ADMIN_ROUTE));
    new AdminUsersPage(browserPage).open();
    captureCards("users", ".cosmos-admin-users-card");
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    for (AdminConfigGroup configurationGroup : AdminConfigGroup.values()) {
      configurationPage.open(configurationGroup);
      captureCards(
          "configuration-" + configurationGroup.name(),
          ".cosmos-admin-section .cosmos-card:visible");
      if (configurationGroup == AdminConfigGroup.QUEUE) {
        browserPage
            .locator("#admin-queue-tabs")
            .getByRole(AriaRole.TAB)
            .filter(
                new Locator.FilterOptions().setHasText(uiTexts.getAdminQueueCleanupAssistantsTab()))
            .click();
        captureCards("queue-assistant", "#admin-mail-queue-resolution");
      }
    }
    new AdminMailPage(browserPage).open();
    captureCards("mail-test", ".cosmos-mail-card");
    new NotificationComposePage(browserPage).open();
    captureCards("notification-compose", ".cosmos-admin-section .cosmos-card:visible");
    String ownTicketNumber = createUnreadTicket();
    new NotificationComposePage(browserPage).open();
    AdminNavigation adminNavigation = new AdminNavigation(browserPage);
    adminNavigation.openDrawer();
    assertThat(browserPage.locator("#admin-nav-support-activity")).isVisible();
    adminNavigation.openSection(uiTexts.getSupportTitle());
    captureCards("tickets", "#support-panel");

    browserPage.navigate(
        path(ADMIN_ROUTE) + "?section=support&ticket=" + ownTicketNumber.substring(1));
    assertThat(browserPage.locator("#support-ticket-panel .support-ticket-number"))
        .hasText(uiTexts.getSupportTicketNumber(ownTicketNumber));
    UiActions.complete(browserPage, () -> browserPage.locator("#support-claim").click());
    UiActions.complete(browserPage, () -> browserPage.locator("#support-close").click());
  }

  private String createUnreadTicket() {
    String requesterEmailAddress = "card-navigation-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage)
        .createActiveUser(requesterEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor requestingActor = new BrowserActor(testBrowser)) {
      requestingActor.login(requesterEmailAddress, BrowserTestAccounts.USER_PASSWORD);
      requestingActor.browserPage.navigate("/recovery-help");
      requestingActor
          .browserPage
          .locator("#support-reply textarea")
          .fill("Native card navigation with unread ticket activity.");
      UiActions.complete(
          requestingActor.browserPage,
          () -> requestingActor.browserPage.locator("#support-create").click());
      // Die angezeigte Vorgangsnummer besteht aus einer Raute und mindestens einer Ziffer.
      return requestingActor
          .browserPage
          .locator("#support-ticket-grid")
          .getByText(Pattern.compile("^#[0-9]+$"))
          .first()
          .innerText();
    }
  }

  private void captureCards(String viewName, String panelSelector) {
    Locator panelLocator = browserPage.locator(panelSelector).first();
    assertThat(panelLocator).isVisible();
    panelLocator.scrollIntoViewIfNeeded();
    browserPage.evaluate("() => document.fonts.ready");
    browserPage.mouse().move(0, 0);
    assertTrue(
        (Boolean)
            panelLocator.evaluate(
                "panel => { const r = panel.getBoundingClientRect(); return r.left >= -1 && r.right <= innerWidth + 1; }"),
        "Karten müssen horizontal erreichbar bleiben: " + viewName);

    String evidenceName = viewName + " " + browserPage.viewportSize().width;
    String measurementsJson =
        (String)
            panelLocator.evaluate(
                """
                panel => JSON.stringify([panel, ...panel.querySelectorAll('.cosmos-panel-header, .cosmos-panel-footer, .account-entry, .portal-context-card, vaadin-button')]
                  .filter(element => element.getBoundingClientRect().height > 0).map(element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {id: element.id, classes: element.className, x: rect.x, y: rect.y,
                      width: rect.width, height: rect.height, padding: style.padding, gap: style.gap,
                      background: style.backgroundColor, border: style.border, font: style.font};
                  }))
                """);
    Allure.attachment(evidenceName + " Messwerte", "application/json", measurementsJson);
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(
            browserPage.screenshot(
                new Page.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
