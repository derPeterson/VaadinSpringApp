package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Gezielter Vergleich der Reiterleisten bei ausreichender und knapper Breite. */
@Tag("browser")
public class TabOverflowBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 1024, 390, 320})
  @DisplayName("Reiterleisten behalten ihren Stil und bleiben bei Platzmangel bedienbar")
  void tabBarsRemainReachableAtDifferentWidths(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();

    AccountPage accountPage = new AccountPage(browserPage);
    accountPage.open();
    accountPage.section("account-tab-security");
    captureTabs("Kontosicherheit", "#account-security-tabs > vaadin-tabs");

    browserPage.navigate("/admin");
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.TWO_FACTOR);
    captureTabs("2FA-Rollen", "#two-factor-role-tabs > vaadin-tabs");
    browserPage.navigate("/admin?section=configuration&group=external_login");
    captureTabs("Anmeldeanbieter", "#admin-provider-tabs > vaadin-tabs");
    configurationPage.open(AdminConfigGroup.QUEUE);
    captureTabs("Mailqueue", "#admin-queue-tabs > vaadin-tabs");
  }

  private void captureTabs(String tabBarName, String tabBarSelector) {
    Locator tabsLocator = browserPage.locator(tabBarSelector);
    assertThat(tabsLocator).isVisible();
    tabsLocator.scrollIntoViewIfNeeded();
    assertTrue(
        (Boolean)
            tabsLocator.evaluate(
                """
                tabs => Array.from(tabs.children)
                  .filter(tab => tab.localName === 'vaadin-tab')
                  .every(tab => tab.scrollWidth <= tab.clientWidth + 1)
                """),
        tabBarName + ": Beschriftungen passen vollständig in die einzelnen Reiter");
    assertTrue(
        (Boolean)
            tabsLocator.evaluate(
                "tabs => tabs.getBoundingClientRect().right <= document.documentElement.clientWidth + 1"),
        tabBarName + ": Die Reiterleiste verbreitert die Seite nicht");

    // Vaadin blendet den Gegenpfeil erst ab mehr als einem gescrollten Pixel ein.
    // Reine Rundungsdifferenzen sind kein sinnvoller Scrollweg für diese Prüfung.
    boolean hasScrollableContent =
        (Boolean)
            tabsLocator.evaluate(
                """
                tabs => {
                  const strip = tabs.shadowRoot.querySelector('[part=tabs]');
                  return strip.scrollWidth > strip.clientWidth + 2;
                }
                """);
    if (hasScrollableContent) {
      assertThat(tabsLocator).hasAttribute("overflow", Pattern.compile(".*end.*"));
      Locator forwardButtonLocator = tabsLocator.locator("[part=forward-button]");
      assertThat(forwardButtonLocator).hasCSS("opacity", "1");
      forwardButtonLocator.click();
      assertThat(tabsLocator).hasAttribute("overflow", Pattern.compile(".*start.*"));
      Locator lastTabLocator = tabsLocator.locator("vaadin-tab").last();
      lastTabLocator.focus();
      UiActions.complete(browserPage, () -> lastTabLocator.press("Enter"));
      assertThat(lastTabLocator).hasAttribute("selected", "");
      UiActions.complete(browserPage, () -> tabsLocator.locator("vaadin-tab").first().click());
      if (browserPage.viewportSize().width <= 390) {
        UiActions.swipeLeft(browserPage, tabsLocator);
        assertThat(tabsLocator).hasAttribute("overflow", Pattern.compile(".*start.*"));
        // Wischen ändert die Auswahl nicht; der bereits ausgewählte Reiter erzeugt keine
        // RPC-Antwort.
        tabsLocator.locator("vaadin-tab").first().click();
      }
    }
    browserPage.mouse().move(0, 0);
    Allure.attachment(
        tabBarName,
        "image/png",
        new ByteArrayInputStream(
            tabsLocator.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
