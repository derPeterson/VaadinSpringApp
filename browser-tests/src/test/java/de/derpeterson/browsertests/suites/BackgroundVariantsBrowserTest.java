package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.WaitUntilState;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Konfigurierbarer Cosmos-Hintergrund")
public class BackgroundVariantsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @Tag("background-variants")
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "BACKGROUND-01: Gemeinsamer Hintergrund bleibt hinter allen Rahmen und beachtet reduzierte Bewegung")
  void chosenBackgroundRemainsDecorativeAcrossPublicPortalAndAdministration(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    new LoginPage(browserPage).open();
    assertBackground("Anmeldung");
    navigate(path(HOME_ROUTE));
    assertBackground("Besucher-Startseite");
    navigate(path(REGISTRATION_ROUTE));
    assertBackground("Registrierung");

    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    navigate(path(ACCOUNT_ROUTE));
    assertBackground("Portal");
    navigate(path(ADMIN_ROUTE));
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.GENERAL);
    assertBackground("Administration");
  }

  private void navigate(String route) {
    browserPage.navigate(
        route, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    assertThat(browserPage.locator(".cosmos-page-frame")).isVisible();
  }

  private void assertBackground(String context) {
    Locator scene = browserPage.locator(".cosmos-sky");
    assertThat(scene).hasCount(1);
    assertThat(scene).hasAttribute("aria-hidden", "true");
    browserPage.evaluate("() => document.fonts.ready");
    browserPage.emulateMedia(
        new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.NO_PREFERENCE));
    assertThat(scene).hasCSS("position", "fixed");
    assertThat(scene).hasCSS("pointer-events", "none");
    assertThat(scene.locator(".cosmos-stars")).hasCount(2);
    assertThat(scene.locator(".cosmos-spark")).hasCount(4);
    assertThat(scene.locator(".cosmos-shoot")).hasCount(3);
    assertThat(scene.locator(".cosmos-satellite")).hasCount(1);
    assertThat(scene.locator(".cosmos-drift")).hasCount(1);
    assertThat(scene.locator(".cosmos-nova")).isVisible();
    assertThat(scene.locator(".cosmos-moon")).isVisible();
    browserPage.mouse().move(10, 120);
    browserPage.mouse().move(browserPage.viewportSize().width - 10, 180);
    browserPage.waitForCondition(() ->
        !scene
            .evaluate("element => element.style.getPropertyValue('--cosmos-background-parallax-x')")
            .toString()
            .isEmpty());
    assertNoHorizontalOverflow();
    AllurePlaywright.attachScreenshot(
        context + " " + browserPage.viewportSize().width, browserPage);

    browserPage.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    assertThat(scene.locator(".cosmos-satellite")).isHidden();
    assertThat(scene.locator(".cosmos-drift")).isHidden();
    for (Locator shootingStar : scene.locator(".cosmos-shoot").all()) {
      assertThat(shootingStar).isHidden();
    }
    browserPage.waitForCondition(() ->
        scene
            .evaluate("element => element.style.getPropertyValue('--cosmos-background-parallax-x')")
            .toString()
            .isEmpty());
    assertTrue(
        (boolean)
            scene.evaluate(
                "element => element.getAnimations({subtree:true}).every(animation => animation.playState !== 'running')"));
    browserPage.mouse().wheel(0, 600);
    assertEquals(0, scene.boundingBox().y, 1);
    assertNoHorizontalOverflow();
    AllurePlaywright.attachScreenshot(
        context + " reduzierte Bewegung " + browserPage.viewportSize().width, browserPage);
  }

  private void assertNoHorizontalOverflow() {
    assertTrue(
        (boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth + 1"));
  }
}
