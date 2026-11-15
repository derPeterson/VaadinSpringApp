package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
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
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Aktionen in der Kontoverwaltung")
public class AccountActionCardsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "ACCOUNT-CARDS-01: Beide Kontoaktionen bleiben erreichbar und ihre Bestätigung abbrechbar")
  void accountActionCardsKeepTheirActionsReachable(int viewportWidth) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String accountEmailAddress = "action-cards-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage)
        .createActiveUser(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    loginPage.open();
    loginPage.signIn(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    AccountPage accountPage = new AccountPage(browserPage);
    accountPage.open();
    accountPage.section("account-tab-security");
    browserPage.locator("#account-security-tab-lifecycle").click();

    assertThat(browserPage.locator(".account-lifecycle-action")).hasCount(2);
    for (String accountAction : new String[] {"DEACTIVATE", "DELETE"}) {
      Locator actionCardLocator =
          browserPage.locator(".account-lifecycle-action[data-action=" + accountAction + "]");
      Locator actionButtonLocator = actionCardLocator.locator("vaadin-button");
      assertThat(actionButtonLocator)
          .hasText(
              accountAction.equals("DELETE")
                  ? uiTexts.getAccountDeleteAction()
                  : uiTexts.getAccountDeactivateAction());
      actionCardLocator.scrollIntoViewIfNeeded();
      browserPage.mouse().move(0, 0);
      Allure.attachment(
          "Kontokarte " + accountAction + " " + viewportWidth,
          "image/png",
          new ByteArrayInputStream(
              actionCardLocator.screenshot(
                  new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
          AttachmentOptions.empty());

      if (viewportWidth <= 600) {
        assertTrue(
            (boolean)
                actionCardLocator.evaluate(
                    """
                    card => {
                      const cardBounds = card.getBoundingClientRect();
                      const buttonBounds = card.querySelector('vaadin-button').getBoundingClientRect();
                      const style = getComputedStyle(card);
                      const rightEdge = cardBounds.right - parseFloat(style.paddingRight) - parseFloat(style.borderRightWidth);
                      const bottomEdge = cardBounds.bottom - parseFloat(style.paddingBottom) - parseFloat(style.borderBottomWidth);
                      return Math.abs(buttonBounds.right - rightEdge) < 1
                          && Math.abs(buttonBounds.bottom - bottomEdge) < 1;
                    }
                    """),
            "Die mobile Kontoaktion muss innerhalb der Kartenabstände rechts stehen.");
      }

      actionButtonLocator.click();
      Locator identityDialogLocator = browserPage.locator("#account-lifecycle-proof[opened]");
      assertThat(identityDialogLocator.locator("#account-lifecycle-password")).isVisible();
      identityDialogLocator
          .getByRole(
              AriaRole.BUTTON,
              new Locator.GetByRoleOptions().setName(uiTexts.getAdminUsersCancel()).setExact(true))
          .click();
      assertThat(identityDialogLocator).hasCount(0);
      assertThat(actionButtonLocator).isEnabled();
    }
    assertTrue(
        (boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"));
  }
}
