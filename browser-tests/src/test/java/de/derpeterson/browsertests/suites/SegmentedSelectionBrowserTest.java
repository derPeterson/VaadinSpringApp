package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Vergleich der segmentierten Profilbild-, Benachrichtigungs- und Filterauswahl. */
@Tag("browser")
public class SegmentedSelectionBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void segmentSelectorsPreserveTheirSurroundingViews(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    browserPage.navigate("/account?section=profile");
    new AccountPage(browserPage).section("account-avatar-edit");
    verifySegmentedTabsAndCaptureScreenshot(
        "Profilbild", browserPage.locator("#account-avatar-mode"));
    browserPage.locator("#account-avatar-close").click();

    NotificationInboxPage notificationInboxPage = new NotificationInboxPage(browserPage);
    notificationInboxPage.open();
    verifySegmentedTabsAndCaptureScreenshot(
        "Posteingang", browserPage.locator("#notifications-inbox-filter"));
    notificationInboxPage.openPreview();
    verifySegmentedTabsAndCaptureScreenshot(
        "Glockenvorschau", browserPage.locator("#notifications-preview-filter"));

    browserPage.navigate("/admin?section=user_management");
    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    adminUsersPage.openFilters();
    verifySegmentedTabsAndCaptureScreenshot(
        "Benutzerfilter", browserPage.locator(".admin-filter-enabled"));
    browserPage.keyboard().press("Escape");

    browserPage.navigate("/admin?section=support");
    browserPage.locator("#support-filter").click();
    verifySegmentedTabsAndCaptureScreenshot(
        "Ticketfilter", browserPage.locator("#support-filter-reading"));
    assertThat(browserPage.locator("#support-filter-assignment vaadin-radio-button")).hasCount(3);
  }

  private void verifySegmentedTabsAndCaptureScreenshot(
      String selectorName, Locator selectorLocator) {
    assertThat(selectorLocator).isVisible();
    selectorLocator.scrollIntoViewIfNeeded();
    Locator nativeTabsLocator = selectorLocator.locator("vaadin-tabs.cosmos-segmented-tabs");
    assertThat(nativeTabsLocator).hasCount(1);
    assertThat(nativeTabsLocator).hasAttribute("role", "tablist");
    assertThat(selectorLocator.locator("vaadin-radio-button")).hasCount(0);

    Locator firstTabLocator = nativeTabsLocator.locator("vaadin-tab").first();
    Locator secondTabLocator = nativeTabsLocator.locator("vaadin-tab").nth(1);
    assertThat(firstTabLocator).hasCSS("cursor", "pointer");
    UiActions.complete(browserPage, secondTabLocator::click);
    assertThat(secondTabLocator).hasAttribute("aria-selected", "true");
    assertThat(nativeTabsLocator.locator("vaadin-tab[selected]")).hasCount(1);

    secondTabLocator.focus();
    secondTabLocator.press("ArrowLeft");
    assertThat(firstTabLocator).isFocused();
    UiActions.complete(browserPage, () -> firstTabLocator.press("Enter"));
    assertThat(firstTabLocator).hasAttribute("aria-selected", "true");

    for (Locator countBadgeLocator : nativeTabsLocator.locator("vaadin-badge").all()) {
      assertThat(countBadgeLocator).hasCSS("cursor", "pointer");
    }

    firstTabLocator.press("Tab");
    browserPage.mouse().move(0, 0);
    AllurePlaywright.attachScreenshot(selectorName, browserPage);
  }
}
