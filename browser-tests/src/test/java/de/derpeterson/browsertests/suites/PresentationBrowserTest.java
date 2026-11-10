package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Sprache und Bedienung")
public class PresentationBrowserTest extends BrowserFixture {
  @Tag("fast")
  @Test
  @DisplayName("LANG-01C: Sichtbares Startseitenmenü wechselt in beide Sprachen")
  @Story("Verfügbarer Sprachschalter")
  @Description(
      """
      1. Anonyme Startseite öffnen und ihren DE-/EN-Schalter bedienen.
      2. Erst andere, dann Ausgangssprache wählen; jeweils übersetzten Loginbutton prüfen.
      3. Ausgewählten Sprachbutton und erhaltene Startseitenroute prüfen.
      **Erwartet:** Tatsächlich vorhandener Schalter funktioniert in beiden Richtungen ohne Neuladen.
      Nicht angebotene Schalter auf anonymen Formular-/Tokenseiten bleiben ausdrücklich offen.
      """)
  void homeLanguageMenuChangesBothDirections() {
    for (UiTexts expected : languages()) {
      new UserMenu(browserPage).selectLanguage(expected.language());

      assertThat(
              browserPage
                  .locator(".public-navigation")
                  .getByRole(
                      AriaRole.BUTTON,
                      new Locator.GetByRoleOptions()
                          .setName(expected.getBaseLoginButton())
                          .setExact(true)))
          .isVisible();
      assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
      List<Cookie> durableCookies =
          browserContext.cookies().stream()
              .filter(cookie -> cookie.expires != null && cookie.expires > 0)
              .toList();
      try (BrowserContext reopened =
          testBrowser.newContext(
              new Browser.NewContextOptions()
                  .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                  .setLocale(uiTexts.browserLocale()))) {
        reopened.addCookies(durableCookies);
        Page visitor = reopened.newPage();
        visitor.navigate(path(HOME_ROUTE));

        assertThat(visitor.locator(".public-languages vaadin-button[aria-pressed=true]"))
            .hasAttribute("lang", expected.language());
      }
    }
  }

  @Test
  @DisplayName("LANG-02A: Gespeicherte Kontosprache überlebt Logout und frische Anmeldung")
  @Story("Sprachpräferenz speichern")
  @Description(
      """
      1. Eigenes normales Konto als Admin anlegen, abmelden und als Benutzer anmelden.
      2. Andere Sprache ausdrücklich wählen; über den übersetzten Logoutbutton abmelden.
      3. In einer frischen Sitzung die entgegengesetzte Sprache wählen und dasselbe Konto anmelden.
      4. Ohne erneuten Sprachklick gespeicherte Sprache an Logoutbutton/Sprachbutton prüfen.
      **Erwartet:** Kontopräferenz bestimmt nach neuer Anmeldung die Sprache.
      """)
  void preferredLanguageIsLoadedAfterLogin() {
    String email = create();
    new UserMenu(browserPage).signOut();
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    UiTexts chosen = languages()[0];
    new UserMenu(browserPage).selectLanguage(chosen.language());
    new UserMenu(browserPage).open();
    // Vor der Navigation muss die tatsächliche Abmeldung auf dem Server abgeschlossen sein.
    UiActions.complete(
        browserPage,
        () ->
            browserPage
                .getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(chosen.getBaseLogoutButton())
                        .setExact(true))
                .click());
    // Normal Home logout invalidates the session but need not change the displayed route.
    browserPage.navigate(path(LOGIN_ROUTE));

    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    try (BrowserContext other =
        testBrowser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setLocale(uiTexts.browserLocale()))) {
      AllurePlaywright.register(other);
      try (var _ = AllurePlaywright.startTracing(other)) {
        Page visitor = other.newPage();
        visitor.navigate(path(HOME_ROUTE));
        new UserMenu(visitor).selectTestLanguage();
        LoginPage fresh = new LoginPage(visitor);
        fresh.open();
        fresh.signIn(email, BrowserTestAccounts.USER_PASSWORD);

        assertThat(visitor).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(visitor).open();

        assertThat(
                visitor.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                        .setName(chosen.getBaseLogoutButton())
                        .setExact(true)))
            .isVisible();
        assertThat(visitor.locator("#portal-language-" + chosen.language()))
            .hasAttribute("aria-pressed", "true");
        AllurePlaywright.attachScreenshot("Geladene Kontosprache", visitor);
      }
    }
  }

  @Test
  @DisplayName("UI-01A: Registrierung bleibt bei schmalen Hoch-/Querformaten bedienbar")
  @Story("Schmale Viewports")
  @Description(
      """
      1. Browser auf 390×844 setzen, Loginfelder bedienen und zur Registrierung navigieren.
      2. Pflichtfeldfehler erzeugen, DE/EN-Datumsbeispiel prüfen; gültige Angaben und Geburtstag eintippen.
      3. Ohne Enter/Escape auf 844×390 wechseln und Registrieren klicken; übernommenen Geburtstag und Bestätigung prüfen.
      **Erwartet:** Kernaktionen erreichbar und bedienbar. Emulierte Fenstergrößen ersetzen kein echtes Handy oder Qualitätsurteil.
      """)
  void narrowRegistrationCanBeUsed() {
    browserPage.setViewportSize(390, 844);
    new LoginPage(browserPage).open();

    assertThat(
            browserPage.getByLabel(
                uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    RegistrationPage form = new RegistrationPage(browserPage);
    form.open();
    form.submit();
    form.assertRequiredError();
    AllurePlaywright.attachScreenshot("Schmales Hochformat mit Fehlern", browserPage);
    String email = "mobile-" + UUID.randomUUID() + "@example.test";
    form.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    browserPage.setViewportSize(844, 390);
    form.submit();
    form.assertBirthDateCommitted(LocalDate.of(1990, 1, 1));
    form.assertSubmitted(email);
    AllurePlaywright.attachScreenshot("Bedienbares Querformat", browserPage);
  }

  @Tag("fast")
  @Test
  @DisplayName("UI-01B: Geburtstag über das Kalender-Icon auswählen und wiederfinden")
  @Story("Mobiler Kalender")
  @Description(
      """
      1. Registrierung bei 390×844 öffnen; gültige Angaben außer Geburtstag eingeben.
      2. Klick ins Datumsfeld öffnet keinen Kalender; erst das rechte Icon öffnet ihn.
      3. Im Kalender zum Vortag navigieren und den Tag anklicken; Datum wird ohne Texteingabe übernommen.
      4. Kalender erneut über das Icon öffnen: Datum ist markiert; denselben Tag wählen.
      5. Registrieren klicken; übernommenes Datum und tatsächliche Erfolgsbestätigung prüfen.
      **Erwartet:** Kalenderauswahl und Wiederöffnen funktionieren ohne künstliche Zustandsinjektion.
      """)
  void birthDateCanBePickedWithoutTypingAndIsSelectedAfterReopening() {
    browserPage.setViewportSize(390, 844);
    RegistrationPage form = new RegistrationPage(browserPage);
    form.open();
    String email = "calendar-" + UUID.randomUUID() + "@example.test";
    LocalDate selected = form.fillValidUsingCalendar(email, BrowserTestAccounts.USER_PASSWORD);
    AllurePlaywright.attachScreenshot("Über Kalender gewählter Geburtstag", browserPage);
    form.submit();
    form.assertBirthDateCommitted(selected);
    form.assertSubmitted(email);
    AllurePlaywright.attachScreenshot("Registrierung nach Kalenderauswahl", browserPage);
  }

  @Tag("fast")
  @Test
  @DisplayName("UI-02A: Loginfelder sind per Tab und Enter bedienbar")
  @Story("Tastaturbedienung")
  @Description(
      """
      1. Login öffnen und das E-Mail-Feld fokussieren, Testadmin eingeben.
      2. Mit Tab ins Passwortfeld wechseln und dessen tatsächlichen Fokus prüfen.
      3. Passwort eingeben und ausschließlich mit Enter absenden.
      **Erwartet:** Admin-Dashboard erreichbar; kein unbeabsichtigter anderer Ablauf. Kein vollständiges Accessibility- oder Zoomaudit.
      """)
  void loginWithKeyboard() {
    new LoginPage(browserPage).open();
    Locator email =
        browserPage.getByLabel(
            uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true));
    Locator password =
        browserPage.getByLabel(
            uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true));
    email.focus();

    assertThat(email).isFocused();
    email.fill(BrowserTestAccounts.ADMIN_EMAIL);
    browserPage.keyboard().press("Tab");

    assertThat(password).isFocused();
    password.fill(BrowserTestAccounts.ADMIN_PASSWORD);
    password.press("Enter");
    browserPage.waitForURL(url -> !URI.create(url).getPath().equals(path(LOGIN_ROUTE)));
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    new LoginPage(browserPage).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }

  private UiTexts[] languages() {
    return new UiTexts[] {new UiTexts(uiTexts.language().equals("de") ? "en" : "de"), uiTexts};
  }

  private String create() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    String email = "language-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }
}
