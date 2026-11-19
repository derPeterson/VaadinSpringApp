package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.browsertests.pages.DialogTabAssertions.assertDialogTabSurfaces;

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

/** Vergleich der Profilbildreiter und der segmentierten Benachrichtigungs- und Filterauswahl. */
@Tag("browser")
public class SegmentedSelectionBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390, 320})
  void segmentSelectorsPreserveTheirSurroundingViews(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    browserPage.navigate("/account?section=profile");
    new AccountPage(browserPage).section("account-avatar-edit");
    verifyAvatarHeaderTabs();
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

  private void verifyAvatarHeaderTabs() {
    Locator avatarDialog = browserPage.locator("#account-avatar-dialog");
    Locator avatarModeTabs =
        avatarDialog.locator(
            "[slot=header-content] > vaadin-tabs.cosmos-panel-tabs#account-avatar-mode");
    Locator avatarModePanel = avatarDialog.locator("#account-avatar-mode-panel");
    Locator dialogContent = avatarDialog.locator("vaadin-dialog-overlay [part=content]:visible");
    Locator initialsTab = avatarModeTabs.locator("#account-avatar-initials-tab");
    Locator customImageTab = avatarModeTabs.locator("#account-avatar-custom-tab");

    assertThat(avatarModeTabs).hasAttribute("role", "tablist");
    assertThat(avatarModeTabs).hasCSS("background-color", "rgba(0, 0, 0, 0)");
    assertThat(initialsTab).hasAttribute("aria-controls", "account-avatar-mode-panel");
    assertThat(initialsTab).hasAttribute("aria-selected", "true");
    assertThat(initialsTab).hasCSS("min-height", "48px");
    assertDialogTabSurfaces(browserPage, avatarModeTabs, initialsTab, dialogContent);

    UiActions.complete(browserPage, customImageTab::click);

    assertThat(customImageTab).hasAttribute("aria-selected", "true");
    assertThat(avatarModePanel).hasAttribute("aria-labelledby", "account-avatar-custom-tab");
    assertThat(avatarModePanel.locator("account-avatar-editor"))
        .hasAttribute("data-mode", "CUSTOM");
    assertThat(avatarModePanel.locator("#account-avatar-choose")).isVisible();
    assertDialogTabSurfaces(browserPage, avatarModeTabs, customImageTab, dialogContent);

    customImageTab.focus();
    customImageTab.press("ArrowLeft");
    assertThat(initialsTab).isFocused();
    UiActions.complete(browserPage, () -> initialsTab.press("Enter"));

    assertThat(initialsTab).hasAttribute("aria-selected", "true");
    assertThat(avatarModePanel).hasAttribute("aria-labelledby", "account-avatar-initials-tab");
    assertThat(avatarModePanel.locator("account-avatar-editor"))
        .hasAttribute("data-mode", "INITIALS");
    assertThat(avatarModePanel.locator("#account-avatar-choose")).isHidden();

    initialsTab.press("Tab");
    browserPage.mouse().move(0, 0);
    assertDialogTabSurfaces(browserPage, avatarModeTabs, initialsTab, dialogContent);
    AllurePlaywright.attachScreenshot("Profilbild", browserPage);
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
