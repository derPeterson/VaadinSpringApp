package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Responsiver Login")
public class LoginLayoutBrowserTest extends BrowserFixture {
  @ParameterizedTest(name = "{0}: Login bei {1}×{2} ohne horizontales Abschneiden")
  @CsvSource({
    "UI-03A,320,568",
    "UI-03B,390,844",
    "UI-03C,844,390",
    "UI-03D,360,240",
    "UI-03E,1280,900"
  })
  @Story("Schmale und niedrige Fenster")
  @Description(
      """
      1. Login bei der angegebenen Fenstergröße öffnen; ohne Welcome-Bild und mit dekorativem CSS-Hintergrund.
      2. Formular, Felder und Links auf horizontale Erreichbarkeit prüfen; kurze Fenster dürfen vertikal scrollen.
      3. Remember-me tatsächlich auswählen und abwählen, mit falschem Passwort anmelden; Fehlermeldung prüfen.
      4. Passwort korrigieren und bei kleiner Größe anmelden; danach Dashboard bei Desktopbreite bestätigen.
      **Erwartet:** Kein horizontales Abschneiden, echte Bedienung und unveränderter Anmeldeablauf.
      Kleine Fenster simulieren wenig Platz/Zoom/Tastatur, keinen echten Geräte- oder Performancebenchmark.
      """)
  void loginRemainsUsable(String id, int width, int height) {
    browserPage.setViewportSize(width, height);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    assertThat(browserPage.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
    assertThat(browserPage.locator(".public-brand img")).isVisible();
    assertThat(browserPage.locator(".cosmos-sky img, .public-panel img")).hasCount(0);
    assertTrue(
        (Boolean)
            browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
    for (Locator control :
        new Locator[] {
          browserPage.locator(".public-panel"),
          browserPage.getByLabel(
              uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true)),
          browserPage.getByLabel(
              uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)),
          browserPage.getByRole(
              AriaRole.LINK,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getLoginCreateAccountLink())
                  .setExact(true)),
          browserPage.getByRole(
              AriaRole.LINK,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getLoginForgotPasswordLink())
                  .setExact(true))
        }) {
      assertThat(control).isVisible();
      assertTrue(
          (Boolean)
              control.evaluate(
                  "el => { const r=el.getBoundingClientRect(); return r.left >= -1 && r.right <= innerWidth + 1; }"),
          "Formularelement muss innerhalb der Fensterbreite liegen: " + id);
    }
    AllurePlaywright.attachScreenshot("Login " + width + "×" + height, browserPage);
    Locator remember =
        browserPage.getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getLoginRememberMeCheckbox())
                .setExact(true));
    remember.check();
    assertThat(remember).isChecked();
    remember.uncheck();
    assertThat(remember).not().isChecked();
    remember.focus();
    remember.press("Space");
    assertThat(remember).isChecked();
    remember.press("Space");
    assertThat(remember).not().isChecked();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!");
    login.assertRejected();
    Locator notice =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getLoginFailedMessage()));
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    // Die anschließende Adminansicht ist kein Teil des responsiven Loginauftrags.
    browserPage.setViewportSize(1280, 900);
    new UserMenu(browserPage).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }

  @Test
  @DisplayName("UI-03F: Gemeinsamer Referenzhintergrund bleibt dekorativ und bewegungsarm")
  @Story("Dekoration und reduzierte Bewegung")
  @Description(
      """
      1. Login öffnen: der alte animierte Hintergrund ist ausgeblendet. Die beiden Referenzebenen sind dekorativ, statisch und nicht klickbar.
      2. Reduzierte Bewegung emulieren: der statische Hintergrund bleibt unverändert ohne Animation.
      3. Über den echten Kontoerstellungslink zur Registrierung wechseln.
      **Erwartet:** Statischer Referenzhintergrund ohne Interaktionsfläche; gemeinsame Darstellung auf Registrierung und Home.
      """)
  void reducedMotionAndBackgroundScope() {
    browserPage.emulateMedia(
        new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.NO_PREFERENCE));
    new LoginPage(browserPage).open();
    Locator sky = browserPage.locator(".cosmos-sky");
    assertThat(sky).hasAttribute("aria-hidden", "true");
    assertThat(sky).isHidden();
    Locator frame = browserPage.locator(".cosmos-page-frame.cosmos-reference-portal");
    for (String pseudo : new String[] {"::before", "::after"}) {
      assertEquals(
          "none",
          frame.evaluate("(el, pseudo) => getComputedStyle(el,pseudo).pointerEvents", pseudo));
      assertEquals(
          "none",
          frame.evaluate("(el, pseudo) => getComputedStyle(el,pseudo).animationName", pseudo));
      assertTrue(
          frame
              .evaluate("(el, pseudo) => getComputedStyle(el,pseudo).backgroundImage", pseudo)
              .toString()
              .contains("radial-gradient"));
    }
    browserPage.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    assertEquals("none", frame.evaluate("el => getComputedStyle(el,'::after').animationName"));
    AllurePlaywright.attachScreenshot("Referenzhintergrund mit reduzierter Bewegung", browserPage);
    browserPage
        .getByRole(
            AriaRole.LINK,
            new Page.GetByRoleOptions().setName(uiTexts.getLoginCreateAccountLink()).setExact(true))
        .click();
    assertThat(browserPage).hasURL(urlPattern(REGISTRATION_ROUTE));
    assertThat(browserPage.locator(".cosmos-reference-public[data-public-page=register]"))
        .isVisible();
    browserPage.navigate(path(HOME_ROUTE));
    assertThat(browserPage).hasTitle(HOME_PAGE_TITLE);
    assertThat(browserPage.locator(".cosmos-home.cosmos-theme")).isVisible();
    assertThat(browserPage.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
    for (Locator layer : browserPage.locator(".cosmos-stars, .cosmos-galaxy, .cosmos-glow").all()) {
      assertThat(layer).hasCSS("animation-name", "none");
    }
  }
}
