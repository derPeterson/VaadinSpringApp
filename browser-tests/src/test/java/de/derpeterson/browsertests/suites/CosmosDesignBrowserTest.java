package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.RESET_PASSWORD_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminNavigation;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Gemeinsames Cosmos-Design")
public class CosmosDesignBrowserTest extends BrowserFixture {
  @Tag("fast")
  @ParameterizedTest(name = "{0}: Fünf Kontoansichten bei {1}×{2}")
  @CsvSource({
    "COSMOS-01A,320,568",
    "COSMOS-01B,390,844",
    "COSMOS-01C,844,390",
    "COSMOS-01D,1280,900",
    "COSMOS-01E,1280,1200"
  })
  @Story("Einheitliches responsives Design")
  @Description(
      """
      1. Login, Registrierung, Passwortanfrage und unbekannte Verifikations-/Resetlinks öffnen.
      2. Geladenes Service-Logo, mittige Card, Inter-Font, CSS-Hintergrund, Fußtext und übersetzte Platzhalter prüfen.
      3. Alle Formularfelder/Buttons auf horizontale Erreichbarkeit prüfen und Screenshots je Ansicht aufnehmen.
      **Erwartet:** Die Card selbst bleibt bei ausreichend Platz in der Bildschirmmitte. Logo/Überschrift stehen darüber; bei wenig Höhe wächst die Seite ohne Abschneidung.
      Ein simuliertes Fenster ersetzt keine menschliche Beurteilung auf einem echten Gerät.
      """)
  void accountViewsShareResponsiveDesign(String id, int width, int height) {
    browserPage.setViewportSize(width, height);
    for (String route :
        new String[] {
          path(LOGIN_ROUTE),
          path(REGISTRATION_ROUTE),
          path(FORGOT_PASSWORD_ROUTE),
          path(VERIFICATION_ROUTE) + "/" + UUID.randomUUID(),
          path(RESET_PASSWORD_ROUTE) + "/" + UUID.randomUUID()
        }) {
      browserPage.navigate(route);
      checkDesign(browserPage);
      for (Locator input : browserPage.locator(".public-panel vaadin-email-field").all()) {
        assertThat(input)
            .hasJSProperty(
                "placeholder",
                uiTexts.getPublicConfirmEmailField().equals(input.evaluate("el => el.label"))
                    ? uiTexts.getAccountConfirmEmailPlaceholder()
                    : uiTexts.getAccountEmailPlaceholder());
      }
      if (route.equals(path(LOGIN_ROUTE))) {
        assertThat(browserPage.locator("vaadin-password-field"))
            .hasJSProperty("placeholder", uiTexts.getAccountPasswordPlaceholder());
      }
      if (route.equals(path(REGISTRATION_ROUTE))) {
        assertThat(
                browserPage.getByLabel(
                    uiTexts.getRegistrationFirstNameField(),
                    new Page.GetByLabelOptions().setExact(true)))
            .hasJSProperty("placeholder", uiTexts.getAccountFirstNamePlaceholder());
        assertThat(
                browserPage.getByLabel(
                    uiTexts.getRegistrationLastNameField(),
                    new Page.GetByLabelOptions().setExact(true)))
            .hasJSProperty("placeholder", uiTexts.getAccountLastNamePlaceholder());
      }
      AllurePlaywright.attachScreenshot(id + " " + route.split("/")[1], browserPage);
    }
  }

  @Test
  @DisplayName("COSMOS-02: Mobile Aktivierung und Passwortänderung über echte Testmails")
  @Story("Gestaltung der gültigen und erfolgreichen Tokenansichten")
  @Description(
      """
      1. Bei 390×844 ein eigenes Konto registrieren und seinen echten Mailbutton öffnen.
      2. Die erfolgreiche Verifikationscard und ihr gemeinsames Design prüfen.
      3. Passwortanfrage absenden, echten Reset-Mailbutton öffnen und beide Passwort-Platzhalter prüfen.
      4. Neues Passwort setzen, Cosmos-Erfolgsnotification prüfen und normal anmelden.
      **Erwartet:** Der vollständige mobile Ablauf bleibt bedienbar, ohne Datenbank-/Service-Abkürzungen.
      """)
  void mobileActivationAndResetRemainUsable() {
    browserPage.setViewportSize(390, 844);
    String email = "cosmos-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(browserPage);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    MailpitInbox inbox = new MailpitInbox(browserContext);
    Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
    activated.setViewportSize(390, 844);
    checkDesign(activated);
    AllurePlaywright.attachScreenshot("Mobile Verifikation erfolgreich", activated);
    ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(activated);
    forgotPasswordPage.open();
    forgotPasswordPage.enterEmail(email);
    forgotPasswordPage.submit();
    forgotPasswordPage.requestedResponse();
    Page resetPage = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
    resetPage.setViewportSize(390, 844);
    checkDesign(resetPage);

    assertThat(
            resetPage.getByLabel(
                uiTexts.getResetPasswordNewPasswordField(),
                new Page.GetByLabelOptions().setExact(true)))
        .hasJSProperty("placeholder", uiTexts.getAccountNewPasswordPlaceholder());
    assertThat(
            resetPage.getByLabel(
                uiTexts.getResetPasswordConfirmField(),
                new Page.GetByLabelOptions().setExact(true)))
        .hasJSProperty("placeholder", uiTexts.getAccountConfirmPasswordPlaceholder());
    AllurePlaywright.attachScreenshot("Mobiles gültiges Resetformular", resetPage);
    ResetPasswordPage reset = new ResetPasswordPage(resetPage);
    reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
    reset.submit();
    reset.assertChanged();

    assertThat(resetPage.locator("vaadin-notification-card:not([closing])"))
        .hasAttribute("theme", java.util.regex.Pattern.compile(".*\\bcosmos\\b.*"));
    AllurePlaywright.attachScreenshot("Mobiler Passwortreset erfolgreich", resetPage);
    LoginPage login = new LoginPage(resetPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);

    assertThat(resetPage).hasURL(urlPattern(HOME_ROUTE));
  }

  @Test
  @DisplayName(
      "COSMOS-03: Copyright über Admin speichern, neu laden und als sicheren Text anzeigen")
  @Story("Persistente Konfiguration des Fußtexts")
  @Description(
      """
      1. Als Admin anmelden; unter Allgemein den Copyright-Text mit year/serviceName und HTML-Zeichen speichern.
      2. Neu laden und den gespeicherten Wert bestätigen; abmelden und die Login-Fußzeile prüfen.
      3. Über Registrierung und Passwortanfrage echte Mails erzeugen; geladenes Inline-Logo und denselben sicheren DB-Fußtext prüfen.
      4. Ursprünglichen Wert anschließend über die Oberfläche wiederherstellen.
      **Erwartet:** Platzhalter sind ausgefüllt, HTML bleibt Text, Home/Admin verwenden dieselbe Cosmos-Palette; die globale HTML-Palette bleibt unverändert.
      """)
  void copyrightCanBeConfiguredThroughTheUi() {
    String homePrimary = primary(browserPage, "html");
    AdminConfigurationPage adminConfigurationPage = adminConfig();

    assertEquals(homePrimary, primary(browserPage, "html"));
    assertThat(browserPage.locator(".cosmos-app-shell.cosmos-theme")).isVisible();
    assertEquals("#38bdf8", primary(browserPage, ".cosmos-page-frame"));
    String label = uiTexts.getAdminConfigEntryCopyrightText();
    String original = adminConfigurationPage.value(label);
    String template = "© {year} {serviceName} · <b>Browser-Test</b>";
    try {
      adminConfigurationPage.enter(label, template);
      adminConfigurationPage.save();
      adminConfigurationPage.reload();

      assertEquals(template, adminConfigurationPage.value(label));
      new UserMenu(browserPage).signOut();
      Locator footer = browserPage.locator(".cosmos-footer");

      assertThat(footer).containsText("VaadinSpringApp (Browser-Test) · <b>Browser-Test</b>");
      assertFalse(footer.innerText().contains("{year}"));
      assertThat(footer.locator("b")).hasCount(0);
      AllurePlaywright.attachScreenshot("Konfigurierter sicherer Copyright Text", browserPage);
      String expectedFooter = footer.innerText();
      String email = "copyright-" + UUID.randomUUID() + "@example.test";
      RegistrationPage registration = new RegistrationPage(browserPage);
      registration.open();
      registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
      registration.submit();
      registration.assertSubmitted(email);
      MailpitInbox inbox = new MailpitInbox(browserContext, expectedFooter);
      Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
      ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(activated);
      forgotPasswordPage.open();
      forgotPasswordPage.enterEmail(email);
      forgotPasswordPage.submit();
      forgotPasswordPage.requestedResponse();
      inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
    } finally {
      AdminConfigurationPage restore = adminConfig();
      restore.enter(label, original);
      restore.save();
    }
  }

  @Tag("fast")
  @Test
  @DisplayName("COSMOS-04: Fehlermeldung und Home verwenden das gemeinsame Cosmos-Theme")
  @Story("Gemeinsames Theme und Notificationgestaltung")
  @Description(
      """
      1. Home-Farbe erfassen, bei 320×568 eine falsche Anmeldung absenden.
      2. Cosmos-Notification, gleichen Verlauf wie Card und erreichbaren Schließen-Button prüfen.
      3. Zur Startseite wechseln und die gemeinsame Palette, Dekoration sowie globale Inter-Schrift bestätigen.
      **Erwartet:** Konto-Notifications passen zur Card; Home verwendet dieselbe Palette und Dekoration; globale HTML-Variablen bleiben unverändert.
      """)
  void notificationAndHomeShareTheTheme() {
    String homePrimary = primary(browserPage, "html");
    browserPage.setViewportSize(320, 568);
    LoginPage login = new LoginPage(browserPage);
    login.open();

    assertEquals("#38bdf8", primary(browserPage, ".cosmos-page-frame"));
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!");
    login.assertRejected();
    Locator notice = browserPage.locator("vaadin-notification-card:not([closing])");

    assertThat(notice).hasAttribute("theme", java.util.regex.Pattern.compile(".*\\bcosmos\\b.*"));
    String cardBackground =
        browserPage
            .locator(".public-panel")
            .evaluate("el => getComputedStyle(el).backgroundImage")
            .toString();

    assertEquals(
        cardBackground,
        notice.evaluate(
            "el => getComputedStyle(el.shadowRoot.querySelector('[part=overlay]')).backgroundImage"));
    assertWithinViewport(notice);
    AllurePlaywright.attachScreenshot("Schmale Cosmos-Fehlermeldung", browserPage);
    notice.getByRole(AriaRole.BUTTON).click();
    browserPage.navigate(path(HOME_ROUTE));

    assertThat(browserPage).hasTitle(HOME_PAGE_TITLE);
    assertEquals(homePrimary, primary(browserPage, "html"));
    assertThat(browserPage.locator(".cosmos-page-frame.cosmos-reference-public")).isVisible();
    assertEquals("#38bdf8", primary(browserPage, ".cosmos-page-frame"));
    assertTrue(
        browserPage
            .locator("body")
            .evaluate("el => getComputedStyle(el).fontFamily")
            .toString()
            .contains("Inter"));
  }

  @Tag("fast")
  @ParameterizedTest(name = "{0}: Home und Admin bei {1}×{2}")
  @CsvSource({"COSMOS-05A,390,844", "COSMOS-05B,844,390", "COSMOS-05C,1280,900"})
  @Story("App-Kopfzeile und erreichbare Adminnavigation")
  @Description(
      """
      1. Home bei der angegebenen Fenstergröße öffnen: Logo, Avatar, Fußzeile und fehlenden horizontalen Überlauf prüfen.
      2. Avatar-Popup öffnen, seine Erreichbarkeit prüfen und über die normale Anmeldung zum Admin wechseln.
      3. Die echte Navigation öffnen; Dashboard und Allgemein anklicken. Der Drawer darf den Inhalt nur im mobilen Overlaymodus überlagern.
      4. Benutzerverwaltung über die Navigation öffnen, Add User anklicken und die sichtbaren Formularfelder innerhalb der Fensterbreite prüfen.
      **Erwartet:** Hintergrund verdeckt keine Navigation. Topbar, Inhalt, Avatar-Popup und Benutzerformular bleiben erreichbar.
      Die Prüfung beurteilt keine gestalterische Perfektion und ersetzt kein echtes Mobilgerät.
      """)
  void appShellAndNavigationRemainUsable(String id, int width, int height) {
    browserPage.setViewportSize(width, height);
    browserPage.navigate(path(HOME_ROUTE));
    assertAppShell();

    assertThat(browserPage.locator(".public-navigation")).isVisible();
    assertThat(browserPage.locator(".public-languages vaadin-button")).hasCount(2);
    assertThat(browserPage.locator(".cosmos-topbar-account-menu")).hasCount(0);
    AllurePlaywright.attachScreenshot(id + " Öffentliche Startseite", browserPage);
    UserMenu userMenu = new UserMenu(browserPage);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    userMenu.selectTestLanguage();
    assertAppShell();
    userMenu.open();
    Locator popup = browserPage.locator("vaadin-popover-overlay[opened] > [part~='overlay']");

    assertThat(popup).isVisible();
    assertWithinViewport(popup);
    AllurePlaywright.attachScreenshot(id + " Admin mit Avatar-Popup", browserPage);
    userMenu.close();
    openDrawerIfNeeded();
    Locator drawer = browserPage.locator("#admin-sidebar");

    assertThat(drawer).isVisible();
    assertWithinViewport(drawer);
    new AdminNavigation(browserPage).openSection(uiTexts.getAdminNavDashboard());
    closeOverlayDrawerIfNeeded();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    if (!browserPage.locator("#account-navigation-toggle").isVisible()) {
      double right =
          ((Number) drawer.evaluate("el => el.getBoundingClientRect().right")).doubleValue();
      double left =
          ((Number)
                  browserPage
                      .locator(".cosmos-admin-sections")
                      .evaluate("el => el.getBoundingClientRect().left"))
              .doubleValue();

      assertTrue(left >= right - 1, "Desktopinhalt muss neben der Navigation beginnen.");
    }
    AllurePlaywright.attachScreenshot(id + " Adminnavigation", browserPage);
    openDrawerIfNeeded();
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.GENERAL);
    closeOverlayDrawerIfNeeded();
    assertWithinViewport(browserPage.locator(".cosmos-admin-form-card"));
    assertAppShell();
    openDrawerIfNeeded();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    closeOverlayDrawerIfNeeded();
    users.openCreate();
    Locator dialog =
        browserPage.locator(
            "vaadin-dialog.admin-user-dialog vaadin-dialog-overlay[opened] > [part~='overlay']");

    assertThat(dialog).isVisible();
    assertWithinViewport(dialog);
    for (Locator field :
        browserPage
            .locator(
                "vaadin-dialog.admin-user-dialog vaadin-text-field, vaadin-dialog.admin-user-dialog vaadin-email-field, vaadin-dialog.admin-user-dialog vaadin-date-picker")
            .all()) {
      assertWithinViewport(field);
    }
    AllurePlaywright.attachScreenshot(id + " Benutzerformular", browserPage);
  }

  @ParameterizedTest(name = "COSMOS-06: Hinweisicons in Konto/Admin bei {0}px vertikal zentriert")
  @ValueSource(ints = {1280, 390})
  @Description(
      """
      1. Als Superadmin bei Desktop- bzw. schmaler Breite anmelden.
      2. Alle drei Kontotabs öffnen und die Mitte des Infoicons mit der sichtbaren Hinweiszeile vergleichen.
      3. Alle Konfigurationsgruppen über die native Navigation öffnen und denselben Footer-Vertrag prüfen.
      4. Mail-Test und Benachrichtigungsversand öffnen: Footer- und Versandhinweise ebenfalls vertikal mittig.
      Erwartet: Ein- und mehrzeilige Texte behalten ein mittig ausgerichtetes Infoicon; keine Datenänderung oder Sendung.
      """)
  void infoIconsStayCenteredAcrossAccountAndAdmin(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, 1000);
    browserPage.navigate(path(ACCOUNT_ROUTE));
    for (String section : new String[] {"profile", "security", "sessions"}) {
      new de.derpeterson.browsertests.pages.AccountPage(browserPage)
          .section("account-tab-" + section);
      Locator panel = browserPage.locator("#account-panel-" + section);

      assertThat(panel).isVisible();
      if (section.equals("profile")) {
        assertThat(browserPage.locator("#account-save")).isVisible();
      } else {
        assertHintCentered(panel.locator(".cosmos-panel-hint:visible"));
      }
    }
    browserPage.navigate(path(ADMIN_ROUTE));
    AdminConfigurationPage settings = new AdminConfigurationPage(browserPage);
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      settings.open(group);
      for (Locator hint : browserPage.locator(".cosmos-config-save-hint").all()) {
        assertHintCentered(hint);
      }
    }
    browserPage.navigate(path(ADMIN_ROUTE) + "?section=mail_test");
    assertHintCentered(browserPage.locator(".cosmos-admin-section--mail .cosmos-panel-hint"));
    browserPage.navigate(path(ADMIN_ROUTE) + "?section=notifications");
    for (Locator hint :
        browserPage
            .locator(".cosmos-admin-section--notifications .cosmos-panel-hint:visible")
            .all()) {
      assertHintCentered(hint);
    }
    assertHintCentered(browserPage.locator(".cosmos-notification-compose-info"));
    AllurePlaywright.attachScreenshot("Zentrierte Infohinweise", browserPage);
  }

  @ParameterizedTest(name = "COSMOS-07: Gemeinsamer Seitenrahmen bei {0}px")
  @ValueSource(ints = {2560, 1280, 390})
  @Description(
      "Servicekopf bleibt beim Scrollen stehen. Bereichskopf und Seitennavigation scrollen mit dem Inhalt. Feldhilfe und Benutzerfilter bleiben am rechten Inhaltsrand und respektieren den sichtbaren Copyrightfooter.")
  void sharedFrameScrollsContentAndKeepsTheServiceHeader(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, 600);
    new AdminNavigation(browserPage).openGroup(AdminConfigGroup.SMTP);
    if (width == 390) {
      Locator toggle = browserPage.locator("#account-navigation-toggle");
      toggle.click();

      assertThat(toggle).hasAttribute("aria-expanded", "true");
      browserPage.keyboard().press("Escape");

      assertThat(toggle).hasAttribute("aria-expanded", "false");
      assertThat(toggle).isFocused();
    }
    Locator header = browserPage.locator(".cosmos-frame-header");
    Locator heading = browserPage.locator(".admin-reference-page-heading");
    double headingTop = heading.boundingBox().y;
    browserPage.mouse().wheel(0, 600);
    browserContext.waitForCondition(() -> heading.boundingBox().y < headingTop - 20);

    assertEquals(0, header.boundingBox().y, 1);
    assertTrue(
        heading.boundingBox().y < headingTop - 20,
        "Der Bereichskopf darf nicht in einem eigenen festen Navbarbereich bleiben.");
    assertThat(browserPage.locator(".cosmos-app-main")).hasCSS("overflow-y", "visible");
    if (width == 1280) {
      assertTrue(
          browserPage.locator("#admin-sidebar").boundingBox().y < 189 - 20,
          "Auch die Desktopnavigation muss mit dem Seiteninhalt scrollen.");
    }
    AdminConfigurationPage settings = new AdminConfigurationPage(browserPage);
    settings.info(uiTexts.getAdminConfigEntryMailHost()).click();
    Locator overlay =
        browserPage.locator("#admin-config-help vaadin-dialog-overlay > [part~='overlay']");

    assertThat(overlay).isVisible();
    assertDrawerAtContentEdge(overlay, width);
    com.microsoft.playwright.options.BoundingBox bounds = overlay.boundingBox();

    assertTrue(bounds.y >= header.boundingBox().height - 1);
    com.microsoft.playwright.options.BoundingBox footer =
        browserPage.locator(".cosmos-footer").boundingBox();
    if (footer.y < 600 && footer.y + footer.height > 0) {
      assertTrue(
          bounds.y + bounds.height <= footer.y + 1,
          "Die Hilfe darf den sichtbaren Copyrightfooter nicht überdecken.");
    }
    settings.closeHelp();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    users.openFilters();
    Locator filter =
        browserPage.locator("#admin-user-filter-panel vaadin-dialog-overlay > [part~='overlay']");

    assertThat(filter).isVisible();
    assertDrawerAtContentEdge(filter, width);
    if (width == 2560) {
      browserPage.setViewportSize(1280, 600);
      assertDrawerAtContentEdge(filter, 1280);
      browserPage.setViewportSize(2560, 600);
      assertDrawerAtContentEdge(filter, 2560);
    }
    browserPage.keyboard().press("Escape");
    AllurePlaywright.attachScreenshot("Gemeinsamer scrollender Seitenrahmen", browserPage);
  }

  private void assertDrawerAtContentEdge(Locator overlay, int width) {
    browserPage.waitForCondition(() -> {
      com.microsoft.playwright.options.BoundingBox content =
          browserPage.locator(".cosmos-admin-sections").boundingBox();
      com.microsoft.playwright.options.BoundingBox drawer = overlay.boundingBox();
      double right =
          width >= 1024
              ? content.x + content.width
              : ((Number) browserPage.evaluate("document.body.getBoundingClientRect().right"))
                  .doubleValue();
      return drawer != null && Math.abs(drawer.x + drawer.width - right) < 2;
    });

    assertTrue(overlay.boundingBox().x >= 0, "Die Seitenleiste bleibt im sichtbaren Bereich");
  }

  private void assertHintCentered(Locator hint) {
    assertThat(hint).isVisible();
    assertThat(hint).hasCSS("align-items", "center");
    Locator icon = hint.locator(":scope > vaadin-icon");

    assertThat(icon).hasCount(1);
    assertEquals(
        hint.boundingBox().y + hint.boundingBox().height / 2,
        icon.boundingBox().y + icon.boundingBox().height / 2,
        1.0);
  }

  private void assertAppShell() {
    assertThat(browserPage.locator(".cosmos-app-shell.cosmos-theme")).isVisible();
    assertEquals("#38bdf8", primary(browserPage, ".cosmos-page-frame"));
    if (browserPage.locator(".public-header").isVisible()) {
      assertWithinViewport(browserPage.locator(".public-header"));

      assertThat(browserPage.locator(".public-brand img")).isVisible();
    } else {
      assertThat(browserPage.locator(".cosmos-topbar-logo")).isVisible();
      assertWithinViewport(browserPage.locator(".cosmos-topbar"));
      assertWithinViewport(browserPage.locator(".cosmos-topbar-account-menu"));
    }
    assertWithinViewport(browserPage.locator(".cosmos-app-main"));

    assertTrue(
        (Boolean)
            browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
        "AppLayout erzeugt horizontalen Überlauf.");
  }

  private void openDrawerIfNeeded() {
    new AdminNavigation(browserPage).openDrawer();
  }

  private void closeOverlayDrawerIfNeeded() {
    new AdminNavigation(browserPage).closeOverlayDrawer();
  }

  private AdminConfigurationPage adminConfig() {
    browserPage.setViewportSize(1280, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminConfigurationPage adminConfigurationPage = new AdminConfigurationPage(browserPage);
    adminConfigurationPage.open(AdminConfigGroup.GENERAL);
    return adminConfigurationPage;
  }

  private String primary(Page target, String selector) {
    return target
        .locator(selector)
        .evaluate("el => getComputedStyle(el).getPropertyValue('--lumo-primary-color').trim()")
        .toString();
  }

  private void checkDesign(Page target) {
    assertThat(target.locator(".public-panel")).hasCount(1);
    assertThat(target.locator(".public-panel")).isVisible();
    assertThat(target.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
    Locator logo = target.locator(".public-brand img");

    assertThat(logo).isVisible();
    target.waitForFunction(
        "image => image.complete && image.naturalWidth > 0", logo.elementHandle());
    assertWithinViewport(logo);

    assertThat(target.locator(".cosmos-frame-header")).hasCSS("position", "sticky");
    assertThat(target.locator(".cosmos-footer")).isVisible();
    for (Locator input :
        target.locator(".public-panel input[placeholder]:placeholder-shown").all()) {
      assertEquals("14px", input.evaluate("el => getComputedStyle(el, '::placeholder').fontSize"));
      assertEquals("400", input.evaluate("el => getComputedStyle(el, '::placeholder').fontWeight"));
    }
    target.evaluate("() => document.fonts.ready");

    assertTrue((Boolean) target.evaluate("() => document.fonts.check('450 16px Inter')"));
    assertTrue(
        target
            .locator(".public-panel")
            .evaluate("el => getComputedStyle(el).fontFamily")
            .toString()
            .contains("Inter"));
    assertTrue(
        (Boolean) target.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
    for (Locator control :
        target
            .locator(
                ".public-panel, .cosmos-footer, .public-panel vaadin-button, .public-panel vaadin-text-field, .public-panel vaadin-email-field, .public-panel vaadin-password-field, .public-panel vaadin-date-picker, .public-panel vaadin-combo-box")
            .all()) {
      assertWithinViewport(control);
    }
  }

  private void assertWithinViewport(Locator control) {
    assertTrue(
        (Boolean)
            control.evaluate(
                "el => { const r=el.getBoundingClientRect(); return r.left >= -1 && r.right <= innerWidth + 1; }"),
        "Element liegt außerhalb der Fensterbreite: " + control);
  }
}
