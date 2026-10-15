package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.*;
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
    page.setViewportSize(width, height);
    LoginPage login = new LoginPage(page);
    login.open();
    assertThat(page.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
    assertThat(page.locator(".cosmos-logo")).isVisible();
    assertThat(page.locator(".cosmos-sky img, .cosmos-card img")).hasCount(0);
    assertTrue(
        (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
    for (Locator control :
        new Locator[] {
          page.locator(".cosmos-card"),
          page.getByLabel(texts.getLoginEmailField(), new Page.GetByLabelOptions().setExact(true)),
          page.getByLabel(
              texts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)),
          page.getByRole(
              AriaRole.LINK,
              new Page.GetByRoleOptions()
                  .setName(texts.getLoginCreateAccountLink())
                  .setExact(true)),
          page.getByRole(
              AriaRole.LINK,
              new Page.GetByRoleOptions()
                  .setName(texts.getLoginForgotPasswordLink())
                  .setExact(true))
        }) {
      assertThat(control).isVisible();
      assertTrue(
          (Boolean)
              control.evaluate(
                  "el => { const r=el.getBoundingClientRect(); return r.left >= -1 && r.right <= innerWidth + 1; }"),
          "Formularelement muss innerhalb der Fensterbreite liegen: " + id);
    }
    AllurePlaywright.attachScreenshot("Login " + width + "×" + height, page);
    Locator remember =
        page.getByRole(
            AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName(texts.getLoginRememberMeCheckbox()).setExact(true));
    remember.check();
    assertThat(remember).isChecked();
    remember.uncheck();
    assertThat(remember).not().isChecked();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!");
    login.assertRejected();
    Locator notice =
        page.locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(texts.getLoginFailedMessage()));
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    // Die anschließende Adminansicht ist kein Teil des responsiven Loginauftrags.
    page.setViewportSize(1280, 900);
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }

  @Test
  @DisplayName("UI-03F: Gemeinsame CSS-Animation respektiert reduzierte Bewegung")
  @Story("Dekoration und reduzierte Bewegung")
  @Description(
      """
        1. Login öffnen: beide Sternlagen und Nebellagen besitzen die CSS-Animation; Dekoration ist aria-hidden und nicht klickbar.
        2. Betriebssystemwunsch nach reduzierter Bewegung emulieren: Sterne/Nebel haben keine Animation mehr.
        3. Über den echten Kontoerstellungslink zur Registrierung wechseln.
        **Erwartet:** Reduzierte Bewegung ohne JavaScript-Animation; gemeinsamer Cosmos-Hintergrund auf Registrierung und Home.
        """)
  void reducedMotionAndBackgroundScope() {
    page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.NO_PREFERENCE));
    new LoginPage(page).open();
    Locator sky = page.locator(".cosmos-sky");
    assertThat(sky).hasAttribute("aria-hidden", "true");
    assertThat(sky).hasCSS("pointer-events", "none");
    Locator stars = page.locator(".cosmos-stars");
    assertThat(stars).hasCount(2);
    assertThat(page.locator(".cosmos-galaxy, .cosmos-glow")).hasCount(3);
    for (Locator layer : stars.all())
      assertThat(layer).hasCSS("animation-name", "cosmos-stars-float");
    for (Locator layer : page.locator(".cosmos-galaxy, .cosmos-glow").all())
      assertThat(layer).hasCSS("animation-name", "cosmos-nebula-float");
    page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
    for (Locator layer : page.locator(".cosmos-stars, .cosmos-galaxy, .cosmos-glow").all())
      assertThat(layer).hasCSS("animation-name", "none");
    AllurePlaywright.attachScreenshot("Login mit reduzierter Bewegung", page);
    page.getByRole(
            AriaRole.LINK,
            new Page.GetByRoleOptions().setName(texts.getLoginCreateAccountLink()).setExact(true))
        .click();
    assertThat(page).hasURL(urlPattern(REGISTRATION_ROUTE));
    assertThat(page.locator(".cosmos-page--wide")).isVisible();
    page.navigate(path(HOME_ROUTE));
    assertThat(page).hasTitle(HOME_PAGE_TITLE);
    assertThat(page.locator(".cosmos-home.cosmos-theme")).isVisible();
    assertThat(page.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
    for (Locator layer : page.locator(".cosmos-stars, .cosmos-galaxy, .cosmos-glow").all())
      assertThat(layer).hasCSS("animation-name", "none");
  }
}
