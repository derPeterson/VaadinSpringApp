package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;

@Tag("browser")
@Feature("Sprache und Bedienung")
public class PresentationBrowserTest extends BrowserFixture {
  @Tag("fast")
  @Test
  @DisplayName("LANG-01C: Sichtbares Startseitenmenü wechselt in beide Sprachen")
  @Story("Verfügbarer Sprachschalter")
  @Description(
      """
        1. Anonyme Startseite öffnen und ihr Flaggenmenü bedienen.
        2. Erst andere, dann Ausgangssprache wählen; jeweils übersetzten Loginbutton prüfen.
        3. Ausgewählte Flagge und erhaltene Startseitenroute prüfen.
        **Erwartet:** Tatsächlich vorhandener Schalter funktioniert in beiden Richtungen ohne Neuladen.
        Nicht angebotene Schalter auf anonymen Formular-/Tokenseiten bleiben ausdrücklich offen.
        """)
  void homeLanguageMenuChangesBothDirections() {
    for (UiTexts expected : languages()) {
      new UserMenu(page).selectLanguage(expected.language());
      new UserMenu(page).open();
      assertThat(
              page.getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions()
                      .setName(expected.getBaseLoginButton())
                      .setExact(true)))
          .isVisible();
      assertThat(page).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(page).close();
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
        4. Ohne erneuten Sprachklick gespeicherte Sprache an Logoutbutton/Flagge prüfen.
        **Erwartet:** Kontopräferenz bestimmt nach neuer Anmeldung die Sprache.
        """)
  void preferredLanguageIsLoadedAfterLogin() {
    String email = create();
    new UserMenu(page).signOut();
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(page).selectTestLanguage();
    UiTexts chosen = languages()[0];
    new UserMenu(page).selectLanguage(chosen.language());
    new UserMenu(page).open();
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(chosen.getBaseLogoutButton()).setExact(true))
        .click();
    // Normal Home logout invalidates the session but need not change the displayed route.
    page.navigate(path(LOGIN_ROUTE));
    assertThat(page).hasURL(urlPattern(LOGIN_ROUTE));
    try (BrowserContext other =
        browser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setLocale(texts.browserLocale()))) {
      AllurePlaywright.register(other);
      try (var otherTrace = AllurePlaywright.startTracing(other)) {
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
        assertThat(
                visitor.getByRole(
                    AriaRole.IMG,
                    new Page.GetByRoleOptions()
                        .setName(
                            chosen.language().equals("de")
                                ? "German Flag Icon"
                                : "English Flag Icon")
                        .setExact(true)))
            .hasCSS("filter", "none");
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
    page.setViewportSize(390, 844);
    new LoginPage(page).open();
    assertThat(
            page.getByLabel(
                texts.getLoginEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    RegistrationPage form = new RegistrationPage(page);
    form.open();
    form.submit();
    form.assertRequiredError();
    AllurePlaywright.attachScreenshot("Schmales Hochformat mit Fehlern", page);
    String email = "mobile-" + UUID.randomUUID() + "@example.test";
    form.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    page.setViewportSize(844, 390);
    form.submit();
    form.assertBirthDateCommitted(LocalDate.of(1990, 1, 1));
    form.assertSubmitted(email);
    AllurePlaywright.attachScreenshot("Bedienbares Querformat", page);
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
    page.setViewportSize(390, 844);
    RegistrationPage form = new RegistrationPage(page);
    form.open();
    String email = "calendar-" + UUID.randomUUID() + "@example.test";
    LocalDate selected = form.fillValidUsingCalendar(email, BrowserTestAccounts.USER_PASSWORD);
    AllurePlaywright.attachScreenshot("Über Kalender gewählter Geburtstag", page);
    form.submit();
    form.assertBirthDateCommitted(selected);
    form.assertSubmitted(email);
    AllurePlaywright.attachScreenshot("Registrierung nach Kalenderauswahl", page);
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
    new LoginPage(page).open();
    Locator email =
        page.getByLabel(texts.getLoginEmailField(), new Page.GetByLabelOptions().setExact(true));
    Locator password =
        page.getByLabel(texts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true));
    email.focus();
    assertThat(email).isFocused();
    email.fill(BrowserTestAccounts.ADMIN_EMAIL);
    page.keyboard().press("Tab");
    assertThat(password).isFocused();
    password.fill(BrowserTestAccounts.ADMIN_PASSWORD);
    password.press("Enter");
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    new LoginPage(page).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }

  private UiTexts[] languages() {
    return new UiTexts[] {new UiTexts(texts.language().equals("de") ? "en" : "de"), texts};
  }

  private void assertNotice(UiTexts expected, String message) {
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(expected.getBaseFailedTitle());
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(expected.visibleText(page, message));
  }

  private String create() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    String email = "language-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }
}
