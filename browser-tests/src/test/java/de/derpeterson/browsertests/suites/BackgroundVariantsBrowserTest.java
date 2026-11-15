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
import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.Set;
import java.util.regex.Pattern;
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
      "BACKGROUND-01: Gewählte Variante bleibt hinter allen Rahmen und beachtet reduzierte Bewegung")
  void chosenBackgroundRemainsDecorativeAcrossPublicPortalAndAdministration(int viewportWidth) {
    String expectedVariant =
        System.getenv().getOrDefault("BROWSER_TEST_BACKGROUND_VARIANT", "advanced");
    assertTrue(Set.of("advanced", "base").contains(expectedVariant));
    browserPage.setViewportSize(viewportWidth, 900);
    new LoginPage(browserPage).open();
    assertBackground("Anmeldung", expectedVariant);
    navigate(path(HOME_ROUTE));
    assertBackground("Besucher-Startseite", expectedVariant);
    navigate(path(REGISTRATION_ROUTE));
    assertBackground("Registrierung", expectedVariant);

    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    navigate(path(ACCOUNT_ROUTE));
    assertBackground("Portal", expectedVariant);
    navigate(path(ADMIN_ROUTE));
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.GENERAL);
    assertBackground("Administration", expectedVariant);
  }

  private void navigate(String route) {
    browserPage.navigate(
        route, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    assertThat(browserPage.locator(".cosmos-page-frame")).isVisible();
  }

  private void assertBackground(String context, String expectedVariant) {
    Locator scene = browserPage.locator(".cosmos-sky");
    assertThat(scene).hasCount(1);
    assertThat(scene).hasAttribute("aria-hidden", "true");
    browserPage.evaluate("() => document.fonts.ready");
    browserPage.emulateMedia(
        new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.NO_PREFERENCE));
    if (expectedVariant.equals("advanced")) {
      assertThat(scene).hasClass(Pattern.compile(".*cosmos-sky--advanced.*"));
      assertThat(scene).hasCSS("position", "fixed");
      assertThat(scene).hasCSS("pointer-events", "none");
      assertThat(scene.locator(".cosmos-stars")).hasCount(2);
      assertThat(scene.locator(".cosmos-spark")).hasCount(4);
      assertThat(scene.locator(".cosmos-shoot")).hasCount(2);
      assertThat(scene.locator(".cosmos-satellite")).hasCount(1);
      assertThat(scene.locator(".cosmos-moon")).isVisible();
      browserPage.mouse().move(10, 120);
      browserPage.mouse().move(browserPage.viewportSize().width - 10, 180);
      browserPage.waitForCondition(() ->
          !scene
              .evaluate(
                  "element => element.style.getPropertyValue('--cosmos-background-parallax-x')")
              .toString()
              .isEmpty());
    } else {
      assertThat(browserPage.locator(".cosmos-sky--advanced")).hasCount(0);
      // Base bewahrt den bisherigen Referenzhintergrund aus den öffentlichen
      // Rahmen-Pseudoelementen.
      Locator referenceFrame = browserPage.locator(".cosmos-reference-portal").first();
      assertTrue(
          (boolean)
              referenceFrame.evaluate(
                  "element => getComputedStyle(element, '::after').backgroundImage !== 'none'"));
      assertEquals(
          "none",
          referenceFrame.evaluate("element => getComputedStyle(element, '::after').pointerEvents"));
    }
    assertNoHorizontalOverflow();
    AllurePlaywright.attachScreenshot(
        context + " " + expectedVariant + " " + browserPage.viewportSize().width, browserPage);

    browserPage.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    if (expectedVariant.equals("advanced")) {
      assertThat(scene.locator(".cosmos-satellite")).isHidden();
      for (Locator shootingStar : scene.locator(".cosmos-shoot").all()) {
        assertThat(shootingStar).isHidden();
      }
      browserPage.waitForCondition(() ->
          scene
              .evaluate(
                  "element => element.style.getPropertyValue('--cosmos-background-parallax-x')")
              .toString()
              .isEmpty());
      assertTrue(
          (boolean)
              scene.evaluate(
                  "element => element.getAnimations({subtree:true}).every(animation => animation.playState !== 'running')"));
      browserPage.mouse().wheel(0, 600);
      assertEquals(0, scene.boundingBox().y, 1);
    }
    assertNoHorizontalOverflow();
    Allure.attachment(context + " Hintergrundvariante", expectedVariant);
    AllurePlaywright.attachScreenshot(
        context + " reduzierte Bewegung " + browserPage.viewportSize().width, browserPage);
  }

  private void assertNoHorizontalOverflow() {
    assertTrue(
        (boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth + 1"));
  }
}
