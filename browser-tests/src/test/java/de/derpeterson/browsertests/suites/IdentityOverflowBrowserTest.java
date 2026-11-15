package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminUserDeleteDialog;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfirmationAppearanceChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Begrenzte Benutzerangaben")
public class IdentityOverflowBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @CsvSource({"1280,false", "390,false", "1280,true", "390,true"})
  @DisplayName(
      "IDENTITY-01: Normale und lange Kontodaten bleiben vollständig gespeichert und lesbar erreichbar")
  void identitySurfacesKeepFullValuesWithinTheirAvailableSpace(
      int viewportWidth, boolean longValues) {
    LoginPage administratorLoginPage = new LoginPage(browserPage);
    administratorLoginPage.open();
    administratorLoginPage.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage usersPage = new AdminUsersPage(browserPage);
    String firstName = longValues ? "Alexandermatthias".repeat(3) : "Browser";
    String lastName = longValues ? "Langnamennachname".repeat(3) : "Testuser";
    String accountEmailAddress =
        longValues
            ? "identity-"
                + UUID.randomUUID()
                + "@"
                + "d".repeat(60)
                + "."
                + "e".repeat(60)
                + "."
                + "f".repeat(60)
                + ".test"
            : "id-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    usersPage.createActiveUser(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    AdminUserDialog editUserDialog = usersPage.openEdit(accountEmailAddress);
    editUserDialog.firstName().fill(firstName);
    editUserDialog.lastName().fill(lastName);
    editUserDialog.save();
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    editUserDialog = usersPage.openEdit(accountEmailAddress);
    AdminUserDeleteDialog deleteUserDialog = editUserDialog.requestDelete();
    ConfirmationAppearanceChecks.capture(
        browserPage, deleteUserDialog.root(), "Benutzeridentität im Löschdialog");
    assertIdentityText(
        browserPage,
        deleteUserDialog.root().locator(".admin-user-delete-name"),
        firstName + " " + lastName,
        viewportWidth < 640 ? 2 : 1);
    assertIdentityText(
        browserPage,
        deleteUserDialog.root().locator(".admin-user-delete-identity .cosmos-panel-description"),
        accountEmailAddress,
        viewportWidth < 640 ? 2 : 1);
    assertTrue(
        (boolean)
            deleteUserDialog
                .root()
                .locator(".admin-user-delete-identity")
                .evaluate("element => element.scrollWidth <= element.clientWidth + 1"));
    deleteUserDialog.cancel();
    editUserDialog.cancel();

    try (BrowserActor accountActor = new BrowserActor(testBrowser)) {
      Page accountBrowserPage = accountActor.browserPage;
      accountBrowserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
      accountActor.login(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(accountBrowserPage).selectTestLanguage();
      accountBrowserPage.navigate(path("account?section=profile"));
      assertThat(accountBrowserPage.locator("#account-email input")).hasValue(accountEmailAddress);
      captureIdentity("Benutzerkopf", accountBrowserPage.locator(".cosmos-topbar"));
      setMobileSidebarOpened(accountBrowserPage, true);
      captureIdentity("Profilidentität", accountBrowserPage.locator(".account-summary"));
      setMobileSidebarOpened(accountBrowserPage, false);
      UserMenu accountUserMenu = new UserMenu(accountBrowserPage);
      accountUserMenu.open();
      assertThat(accountBrowserPage.locator("vaadin-popover.portal-user-menu-popover")).isFocused();
      Allure.attachment(
          "Nativer Anfangsfokus im Benutzermenü",
          "application/json",
          (String)
              accountBrowserPage.evaluate(
                  """
                  () => {
                    let element = document.activeElement;
                    while (element.shadowRoot?.activeElement) element = element.shadowRoot.activeElement;
                    return JSON.stringify({tag:element.localName, part:element.getAttribute('part'),
                      role:element.getAttribute('role'), classes:element.getAttribute('class')});
                  }
                  """));
      captureIdentity("Menüidentität", accountBrowserPage.locator(".portal-menu-identity"));
      assertIdentityText(
          accountBrowserPage,
          accountBrowserPage.locator(".portal-menu-name"),
          firstName + " " + lastName,
          1);
      assertIdentityText(
          accountBrowserPage,
          accountBrowserPage.locator(".portal-menu-email"),
          accountEmailAddress,
          1);
      accountUserMenu.close();

      setMobileSidebarOpened(accountBrowserPage, true);
      assertIdentityText(
          accountBrowserPage,
          accountBrowserPage.locator(".account-summary-name"),
          firstName + " " + lastName,
          1);
      assertIdentityText(
          accountBrowserPage,
          accountBrowserPage.locator(".account-summary-email"),
          accountEmailAddress,
          1);
      setMobileSidebarOpened(accountBrowserPage, false);
      assertNativeIdentityTooltip(
          accountBrowserPage,
          accountBrowserPage.locator(".tracked-user-button"),
          firstName + " " + lastName + "\n" + accountEmailAddress,
          viewportWidth >= 640);
      assertTrue(
          (boolean)
              accountBrowserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"));
      if (longValues && viewportWidth == 1280) {
        assertProfileNameRefreshesIdentityTooltips(
            accountBrowserPage, firstName, lastName, accountEmailAddress);
      }
      accountUserMenu.signOut();
    }
    // Abbruch hat das Konto weder gelöscht noch die vollständigen Stammdaten gekürzt.
    browserPage.setViewportSize(1280, 900);
    editUserDialog = usersPage.openEdit(accountEmailAddress);
    assertThat(editUserDialog.email()).hasValue(accountEmailAddress);
    assertThat(editUserDialog.firstName()).hasValue(firstName);
    assertThat(editUserDialog.lastName()).hasValue(lastName);
    editUserDialog.cancel();
  }

  private void setMobileSidebarOpened(Page accountBrowserPage, boolean opened) {
    Locator navigationToggleLocator = accountBrowserPage.locator("#account-navigation-toggle");
    if (navigationToggleLocator.isVisible()
        && !Boolean.toString(opened)
            .equals(navigationToggleLocator.getAttribute("aria-expanded"))) {
      navigationToggleLocator.click();
      assertThat(navigationToggleLocator).hasAttribute("aria-expanded", Boolean.toString(opened));
    }
  }

  private void assertProfileNameRefreshesIdentityTooltips(
      Page accountBrowserPage, String firstName, String lastName, String accountEmailAddress) {
    AccountPage accountPage = new AccountPage(accountBrowserPage);
    accountPage.input("account-first-name").fill("Updated");
    accountPage.save();
    assertIdentityText(
        accountBrowserPage,
        accountBrowserPage.locator(".account-summary-name"),
        "Updated " + lastName,
        1);
    UserMenu accountUserMenu = new UserMenu(accountBrowserPage);
    accountUserMenu.open();
    assertIdentityText(
        accountBrowserPage,
        accountBrowserPage.locator(".portal-menu-name"),
        "Updated " + lastName,
        1);
    // Ohne Fokuswechsel würde Escape zunächst nur den nativen Tooltip schließen.
    accountBrowserPage.locator(".portal-menu-primary").focus();
    assertThat(accountBrowserPage.getByRole(AriaRole.TOOLTIP)).not().isVisible();
    accountUserMenu.close();
    assertNativeIdentityTooltip(
        accountBrowserPage,
        accountBrowserPage.locator(".tracked-user-button"),
        "Updated " + lastName + "\n" + accountEmailAddress,
        true);
    accountPage.input("account-first-name").fill(firstName);
    accountPage.save();
    assertThat(accountBrowserPage.locator(".cosmos-header-identity > span").first())
        .hasText(firstName + " " + lastName);
  }

  private void captureIdentity(String evidenceName, Locator identityLocator) {
    identityLocator.scrollIntoViewIfNeeded();
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(
            identityLocator.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }

  private void assertIdentityText(
      Page accountBrowserPage, Locator identityTextLocator, String fullText, int maximumLines) {
    assertThat(identityTextLocator).hasText(fullText);
    assertTrue(
        (boolean)
            identityTextLocator.evaluate(
                """
                (element, maximumLines) => {
                  const bounds = element.getBoundingClientRect();
                  const lineHeight = parseFloat(getComputedStyle(element).lineHeight);
                  return bounds.height > 0 && bounds.height <= maximumLines * lineHeight + 1;
                }
                """,
                maximumLines),
        "Benutzerangaben dürfen die vereinbarte Zeilenanzahl nicht überschreiten.");
    assertNativeIdentityTooltip(accountBrowserPage, identityTextLocator, fullText, true);
  }

  private void assertNativeIdentityTooltip(
      Page accountBrowserPage, Locator identityTargetLocator, String fullText, boolean checkHover) {
    Locator tooltipTextLocator = accountBrowserPage.getByRole(AriaRole.TOOLTIP);
    if (checkHover) {
      identityTargetLocator.hover();
      assertThat(tooltipTextLocator).isVisible();
      assertThat(tooltipTextLocator).hasText(fullText);
    }
    accountBrowserPage.mouse().move(0, 0);
    identityTargetLocator.press("Tab");
    identityTargetLocator.focus();
    assertThat(tooltipTextLocator).isVisible();
    assertThat(tooltipTextLocator).hasText(fullText);
    identityTargetLocator.press("Tab");
  }
}
