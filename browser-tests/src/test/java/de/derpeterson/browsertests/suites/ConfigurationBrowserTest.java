package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminNavigation;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.UserStatusMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Weitere sichtbare Konfigurationsverträge")
public class ConfigurationBrowserTest extends BrowserFixture {
  @Test
  @DisplayName(
      "CONFIG-04A: Dienstname und Basis-URL validieren; Name speichern und wiederherstellen")
  @Description(
      """
      1. Allgemeine Gruppe öffnen; Originalnamen und URL merken.
      2. Blanken Namen bzw. ungültige URL versuchen: Feldfehler, Sprachwechsel mit erhaltenem ungültigem Entwurf und unveränderte Werte nach Neuladen.
      3. Eigenen kurzen Unicode-Dienstnamen speichern und frisch laden: identischer Wert.
      4. Originalnamen wieder speichern; Basis-URL bleibt unverändert.
      **Erwartet:** Stringvalidierung und Speicherung sind sichtbar wirksam; kein Neustartnachweis.
      """)
  void stringValidationAndPersistence() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String name = uiTexts.getAdminConfigEntryServiceName(),
        url = uiTexts.getAdminConfigEntryBaseUrl();
    String original = settings.value(name), originalUrl = settings.value(url);
    try {
      settings.enter(name, "");
      settings.reject("vaadin-text-field", uiTexts.getAdminConfigValidationServiceName());
      settings.reload();

      assertThat(settings.input(name)).hasValue(original);
      settings.enter(url, "invalid");
      settings.reject("vaadin-text-field", uiTexts.getAdminConfigValidationUrl());
      UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
      try {
        new UserMenu(browserPage).selectLanguage(other.language());

        assertThat(settings.input(other.getAdminConfigEntryBaseUrl())).hasValue("invalid");
        assertThat(browserPage.locator("vaadin-text-field[invalid]"))
            .hasJSProperty("errorMessage", other.getAdminConfigValidationUrl());
      } finally {
        new UserMenu(browserPage).selectTestLanguage();
      }
      assertThat(settings.input(url)).hasValue("invalid");
      assertThat(browserPage.locator("vaadin-text-field[invalid]"))
          .hasJSProperty("errorMessage", uiTexts.getAdminConfigValidationUrl());
      settings.reload();

      assertThat(settings.input(url)).hasValue(originalUrl);
      settings.enter(name, "Browserprüfung ÄÖÜ");
      settings.save();
      settings.reload();

      assertThat(settings.input(name)).hasValue("Browserprüfung ÄÖÜ");
      try (Page freshHome = browserContext.newPage()) {
        freshHome.navigate(path(HOME_ROUTE));

        assertThat(freshHome).hasTitle(HOME_PAGE_TITLE);
        assertThat(
                freshHome.getByText(
                    "Browserprüfung ÄÖÜ", new Page.GetByTextOptions().setExact(true)))
            .isVisible();
      }
    } finally {
      settings.enter(name, original);
      settings.enter(url, originalUrl);
      settings.save();
    }
  }

  @Test
  @DisplayName("CONFIG-04B: Boolean umschalten, speichern, neu laden und wiederherstellen")
  @Description(
      """
      1. Sicherheitsgruppe öffnen und aktuellen Wartungsmodus merken.
      2. Switch umschalten, speichern und neu laden: exakt geänderter Switchzustand.
      3. Originalzustand wieder speichern und neu prüfen.
      **Erwartet:** Boolean-Bedienung und Persistenz; keine Behauptung einer Wartungsfunktion.
      """)
  void booleanPersistsAcrossReload() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.SECURITY);
    Locator checkbox =
        browserPage.getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigEntryMaintenanceMode())
                .setExact(true));
    boolean original = checkbox.isChecked();
    try {
      checkbox.setChecked(!original);
      settings.save();
      settings.reload();

      assertEquals(!original, checkbox.isChecked());
    } finally {
      checkbox.setChecked(original);
      settings.save();
      settings.reload();

      assertEquals(original, checkbox.isChecked());
    }
  }

  @Test
  @DisplayName("CONFIG-04C: Positive Ganzzahl mit Nullfehler, Speicherung und Wiederherstellung")
  @Description(
      """
      1. Sicherheitsgruppe öffnen und maximales Sitzungslimit merken.
      2. Null versuchen: Fehler mindestens eins; nach Neuladen Originalwert.
      3. Positiven Wert vier speichern, frisch laden und Original wiederherstellen.
      **Erwartet:** Echter IntegerField-Vertrag; gleichzeitig eingeloggte Konten werden hier nicht verändert.
      """)
  void integerValidationAndPersistence() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.SECURITY);
    String label = uiTexts.getAdminConfigEntryMaxSessionsPerUser(),
        original = settings.value(label);
    try {
      settings.enter(label, "0");
      settings.reject("vaadin-integer-field", uiTexts.getAdminConfigValidationMinOne());
      settings.reload();

      assertThat(settings.input(label)).hasValue(original);
      settings.enter(label, "4");
      settings.save();
      settings.reload();

      assertThat(settings.input(label)).hasValue("4");
    } finally {
      settings.enter(label, original);
      settings.save();
    }
  }

  @Tag("fast")
  @Test
  @DisplayName("CONFIG-05A: SideNav, Sprache, URL, Refresh und Browserhistorie")
  @Description(
      """
      1. Allgemein über die native Konfigurationshierarchie öffnen und einen ungespeicherten Namen eingeben.
      2. Elternpunkt per Klick einklappen: Allgemein bleibt sichtbar und die Eingabe erhalten.
      3. Per Enter öffnen, per Leertaste schließen und erneut öffnen; alle acht freigegebenen Gruppen bleiben erreichbar, die beiden Superadmin-Gruppen fehlen.
      4. Über das Avatarmenü die andere Sprache wählen und wieder zurück: Gruppe und Eingabe bleiben erhalten.
      5. Benutzerverwaltung und Security öffnen: URL, Refresh, Zurück/Vorwärts und Direktlink im neuen Tab prüfen.
      **Erwartet:** Elternpunkt navigiert nicht; Bedienung und Sprache erhalten Entwürfe. URL stellt den gewählten Bereich wieder her.
      """)
  void hierarchyTogglePreservesDraftAndSupportsKeyboardAndLanguage() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    AdminNavigation navigation = new AdminNavigation(browserPage);
    String label = uiTexts.getAdminConfigEntryServiceName(),
        draft = "Draft " + UUID.randomUUID().toString().substring(0, 8);
    String original = settings.value(label);
    settings.enter(label, draft);
    Locator parent = navigation.configuration(), toggle = parent.getByRole(AriaRole.BUTTON);
    toggle.click();

    assertThat(parent).hasJSProperty("expanded", false);
    assertThat(settings.input(label)).hasValue(draft);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminConfigGroupGeneral())
                    .setLevel(2)
                    .setExact(true)))
        .isVisible();
    toggle.focus();
    browserPage.keyboard().press("Enter");

    assertThat(parent).hasJSProperty("expanded", true);
    browserPage.keyboard().press("Space");

    assertThat(parent).hasJSProperty("expanded", false);
    browserPage.keyboard().press("Enter");

    assertThat(parent).hasJSProperty("expanded", true);
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      Locator groupLink = navigation.groupLink(group);
      if (group.isSuperadminOnly()) {
        assertThat(groupLink).hasCount(0);
      } else {
        assertThat(groupLink).isVisible();
      }
    }
    UiTexts other =
        new de.derpeterson.browsertests.i18n.UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    try {
      new UserMenu(browserPage).selectLanguage(other.language());

      assertThat(settings.input(other.getAdminConfigEntryServiceName())).hasValue(draft);
      assertThat(
              parent.getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions()
                      .setName(
                          other.getAdminNavConfiguration()
                              + " "
                              + other.getAdminNavToggleChildren())
                      .setExact(true)))
          .isVisible();
      AllurePlaywright.attachScreenshot(
          "SideNav und ungespeicherte Eingabe nach Sprachwechsel", browserPage);
    } finally {
      new UserMenu(browserPage).selectTestLanguage();
    }
    assertThat(settings.input(label)).hasValue(draft);
    // Neuladen prüft die tatsächliche Persistenz: Enter am Navigationspunkt darf
    // die ungespeicherte Eingabe nicht über den Formularshortcut speichern.
    try {
      settings.reload();

      assertThat(settings.input(label)).hasValue(original);
    } finally {
      settings.enter(label, original);
      settings.save();
    }
    navigation.openSection(uiTexts.getAdminUsersTitle());
    // Die UI-Antwort kann auch von einem Poll stammen. Auf die echte Ziel-URL warten.
    // Das maskierte Fragezeichen startet genau das feste Querypaar; BrowserRoutes verankert die
    // URL.
    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE, "\\?section=user_management"));
    browserPage.reload();
    assertAdminHeading(browserPage, uiTexts.getAdminUsersTitle(), 1);

    assertThat(browserPage.locator(".cosmos-nav-item--active[aria-current='page']")).hasCount(1);
    navigation.openGroup(AdminConfigGroup.SECURITY);
    // Die nichtfangende Alternative erlaubt nur die beiden Reihenfolgen der festen Querypaare.
    assertThat(browserPage)
        .hasURL(
            urlPattern(
                ADMIN_ROUTE,
                "\\?(?:section=configuration&group=security|group=security&section=configuration)"));
    String directLink = browserPage.url();
    browserPage.reload();
    assertAdminHeading(browserPage, uiTexts.getAdminConfigGroupSecurity(), 2);

    assertThat(navigation.configuration()).hasJSProperty("expanded", true);
    browserPage.goBack();
    assertAdminHeading(browserPage, uiTexts.getAdminUsersTitle(), 1);
    browserPage.goForward();
    assertAdminHeading(browserPage, uiTexts.getAdminConfigGroupSecurity(), 2);
    try (Page direct = browserContext.newPage()) {
      direct.navigate(directLink);
      assertAdminHeading(direct, uiTexts.getAdminConfigGroupSecurity(), 2);
    }
  }

  private void assertAdminHeading(Page target, String title, int level) {
    assertThat(
            target.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(title).setExact(true).setLevel(level)))
        .isVisible();
  }

  @Tag("fast")
  @Test
  @DisplayName(
      "CONFIG-05B: Klick-Tooltip mit Backend-Standardwert, Regeln, Außenklick und Sprachwechsel")
  @Description(
      """
      1. Allgemein öffnen: Dienstname verwendet den tatsächlichen Default der isolierten Umgebung als Platzhalter.
      2. Ungespeicherte Eingabe setzen und Info-Button anklicken: Beschreibung, Standardwert und 30-Zeichen-Regel.
      3. Außerhalb klicken, erneut öffnen und per Info-Button schließen; kein Dialog und kein Eingabeverlust.
      4. Sprache wechseln: Tooltip und Eingabebeschriftung aktualisieren sich, die Eingabe bleibt erhalten.
      **Erwartet:** Echte manuelle Tooltips im Card-Stil; Öffnen/Schließen speichert keine Konfiguration.
      """)
  void manualTooltipUsesDefaultAndCanReopenAfterOutsideClick() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String label = uiTexts.getAdminConfigEntryServiceName(), draft = "Tooltip draft";

    assertThat(settings.input(label)).hasAttribute("placeholder", "VaadinSpringApp (Browser-Test)");
    settings.enter(label, draft);
    settings.info(label).click();
    Locator overlay =
        browserPage.locator("#admin-config-help vaadin-dialog-overlay > [part~='overlay']");

    assertThat(overlay).isVisible();
    assertThat(overlay).hasCSS("width", "380px");
    assertThat(settings.helpPanel().locator(".admin-help-description")).hasCSS("font-size", "12px");
    assertThat(settings.helpPanel().locator(".admin-help-description"))
        .hasCSS("font-weight", "400");
    assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigEntryServiceNameTooltip());
    assertThat(settings.helpPanel()).containsText("VaadinSpringApp (Browser-Test)");
    assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigHelperServiceNameMax(30));
    assertThat(browserPage.locator("#admin-config-help[opened]")).hasCount(1);
    AllurePlaywright.attachScreenshot(
        "Klick-Tooltip mit tatsächlichem Default und Regeln", browserPage);
    browserPage.mouse().click(4, 90);

    assertThat(settings.helpPanel()).hasCount(0);
    settings.explain(
        label,
        uiTexts.getAdminConfigEntryServiceNameTooltip(),
        uiTexts.getAdminConfigHelperServiceNameMax(30));

    assertThat(settings.input(label)).hasValue(draft);
    UiTexts other =
        new de.derpeterson.browsertests.i18n.UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    try {
      new UserMenu(browserPage).selectLanguage(other.language());
      settings.info(other.getAdminConfigEntryServiceName()).click();

      assertThat(settings.helpPanel()).containsText(other.getAdminConfigEntryServiceNameTooltip());
      assertThat(settings.helpPanel()).containsText(other.getAdminConfigHelperServiceNameMax(30));
      assertThat(settings.input(other.getAdminConfigEntryServiceName())).hasValue(draft);
      settings.closeHelp(other.getNotificationsClose());
    } finally {
      if (settings.helpPanel().count() > 0) {
        browserPage.keyboard().press("Escape");

        assertThat(settings.helpPanel()).hasCount(0);
      }
      new UserMenu(browserPage).selectTestLanguage();
    }
  }

  @Test
  @DisplayName("CONFIG-05C: Mailqueue-Aufbewahrung lehnt Null ab und akzeptiert positive Sekunden")
  @Description(
      """
      1. E-Mail-Queue öffnen und ursprüngliche Aufbewahrungsdauer merken.
      2. PT0S ablehnen: Feldfehler größer als null; nach Neuladen ursprünglicher Wert.
      3. Eine Sekunde speichern und neu laden: PT1S ohne zusätzliche Minutengrenze.
      4. Ursprünglichen Wert im finally-Block über die Oberfläche wiederherstellen.
      **Erwartet:** UI und Backend verlangen dieselbe strikt positive Dauer.
      """)
  void queueRetentionMustBePositiveWithoutAnInventedMinimum() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.QUEUE);
    String label = uiTexts.getAdminConfigEntryEmailQueueSentLiveDuration(),
        original =
            new de.derpeterson.browsertests.pages.TokenConfigurationPage(browserPage).value(label);
    try {
      settings.enterSeconds(label, java.math.BigDecimal.ZERO);
      settings.reject(
          "vaadin-big-decimal-field", uiTexts.getAdminConfigValidationPositiveDuration());
      settings.reload();

      assertEquals(
          original,
          new de.derpeterson.browsertests.pages.TokenConfigurationPage(browserPage).value(label));
      settings.enterSeconds(label, java.math.BigDecimal.ONE);
      settings.save();
      settings.reload();

      assertEquals(
          "PT1S",
          new de.derpeterson.browsertests.pages.TokenConfigurationPage(browserPage).value(label));
    } finally {
      new de.derpeterson.browsertests.pages.TokenConfigurationPage(browserPage)
          .enter(label, original);
      settings.save();
    }
  }

  @org.junit.jupiter.params.ParameterizedTest(name = "CONFIG-05D: Feldhilfen bei {0} px")
  @org.junit.jupiter.params.provider.ValueSource(ints = {1280, 390})
  @Tag("configuration-follow-up")
  @Tag("configuration-field-help")
  @DisplayName("CONFIG-05D: Normale Admins erreichen alle Feldinfos ihrer acht Gruppen")
  @Description(
      """
      1. Alle acht für normale Admins freigegebenen Konfigurationsgruppen über die native SideNav öffnen.
      2. Jede Feldinfo per Klick öffnen: übersetzter Standardwert, Eingaberegeln und Wirksamkeit.
      3. Nur eine Info gleichzeitig sichtbar; per Info-Button wieder schließen.
      4. Ein gesetzter Standard-Geheimschlüssel bleibt vertraulich; das leere SMTP-Passwort zeigt ausdrücklich den Leerhinweis.
      5. Jede Card zeigt die Anzahl ihrer tatsächlichen Felder, einschließlich Sitzungs-Timeout.
      **Erwartet:** Keine Gruppe verliert ihre Bedienung; alle bisherigen Felder erhalten die gemeinsame Info-Funktion.
      """)
  void everyConfigurationFieldHasAnAccessibleLocalizedInfo(int width) {
    AdminConfigurationPage settings = admin();
    browserPage.setViewportSize(width, 900);
    int total = 0;
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      if (group.isSuperadminOnly()) {
        // Anbieter- und 2FA-Konfiguration bleiben dem Superadmin vorbehalten.
        // Ihre eigenen Formulare werden in OIDC-08 und MFA-03 geprüft.
        continue;
      }
      settings.open(group);
      Locator infos = browserPage.locator(".cosmos-config-fields .cosmos-config-info");
      int count = infos.count();

      assertTrue(count > 0);
      assertThat(browserPage.locator(".cosmos-config-setting-count"))
          .hasText(uiTexts.getAdminConfigSettingsCount(count));
      for (Locator info : infos.all()) {
        info.click();

        assertThat(settings.helpPanel()).hasCount(1);
        assertThat(settings.helpPanel().locator("vaadin-dialog-overlay > [part~='overlay']"))
            .isVisible();
        if (group == AdminConfigGroup.NOTIFICATIONS) {
          assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigInitialValue());
          assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigRuleEmptyUnlimited());
        } else {
          assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigDefault());
        }
        assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigInputRules());
        assertTrue(
            settings
                    .helpPanel()
                    .locator(".admin-help-rules:not(.admin-help-effective-use) li")
                    .count()
                > 0,
            "Feldinfo hat keine Eingaberegel: " + info.getAttribute("aria-labelledby"));
        AdminConfigurationPage.assertEffectiveUse(settings.helpPanel(), uiTexts);
        if (info.getAttribute("aria-labelledby").equals("config-label-remember_me_secret_key")) {
          assertThat(settings.helpPanel())
              .containsText(uiTexts.getAdminConfigDefaultConfidential());
        } else if (info.getAttribute("aria-labelledby").equals("config-label-mail_password")) {
          assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigDefaultEmpty());
        }
        settings.closeHelp();

        assertThat(settings.helpPanel()).hasCount(0);
      }
      total += count;
    }
    assertEquals(33, total);
    settings.open(AdminConfigGroup.SECURITY);
    UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    UserMenu userMenu = new UserMenu(browserPage);
    try {
      userMenu.selectLanguage(other.language());
      settings.info(other.getAdminConfigEntrySessionTimeout()).click();
      AdminConfigurationPage.assertEffectiveUse(
          settings.helpPanel(),
          other,
          other.getAdminConfigEffectiveSessionNew(),
          other.getAdminConfigEffectiveSessionExisting());
      settings.closeHelp(other.getNotificationsClose());
    } finally {
      userMenu.selectTestLanguage();
    }
  }

  @Tag("fast")
  @Test
  @DisplayName(
      "CONFIG-05E: Enter im Eingabefeld speichert den aktuellen Wert ohne vorherigen Fokuswechsel")
  @Description(
      """
      1. Allgemein öffnen und den ursprünglichen Servicenamen merken.
      2. Einen neuen Namen eintippen und direkt im Feld Enter drücken, ohne vorher Tab oder den Speicherbutton.
      3. Erfolgshinweis schließen und Werte neu laden: die letzte Eingabe wurde gespeichert.
      4. Originalwert im finally-Block über das Formular wiederherstellen.
      **Erwartet:** Der auf Eingabefelder begrenzte Shortcut übernimmt auch noch nicht per Blur übertragene Werte.
      """)
  void fieldEnterSavesTheLatestValueWithoutPriorBlur() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String label = uiTexts.getAdminConfigEntryServiceName(), original = settings.value(label);
    String changed = "Keyboard " + UUID.randomUUID().toString().substring(0, 8);
    try {
      settings.input(label).fill(changed);
      settings.input(label).press("Enter");
      Locator notice =
          browserPage
              .locator("vaadin-notification-card:not([closing])")
              .filter(new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigSaveSuccess()));

      assertThat(notice).isVisible();
      notice.getByRole(AriaRole.BUTTON).click();

      assertThat(notice).not().isVisible();
      settings.reload();

      assertThat(settings.input(label)).hasValue(changed);
    } finally {
      settings.enter(label, original);
      settings.save();
    }
  }

  @Test
  @DisplayName("CONFIG-05F: Feldinfo bleibt bei 320×568 innerhalb des mobilen Fensters")
  @Description(
      """
      1. Als Admin anmelden und das Fenster auf 320×568 verkleinern.
      2. Allgemein über den echten mobilen Drawer öffnen und diesen wieder schließen.
      3. Service-Name-Info öffnen: Text lesbar, Breite 288 px und vollständig innerhalb des Fensters.
      4. Screenshot aufnehmen und die Info wieder über ihren Button schließen.
      5. Benutzermenü öffnen, bis Abmelden scrollen, seine Position im Fenster prüfen und tatsächlich abmelden.
      **Erwartet:** Tooltip und Benutzermenü bleiben schmal bedienbar; keine abgeschnittene Abmeldeaktion.
      """)
  void manualInfoRemainsWithinTheMobileViewport() {
    AdminConfigurationPage settings = admin();
    browserPage.setViewportSize(320, 568);

    assertThat(browserPage.locator("#account-navigation-toggle")).isVisible();
    assertThat(browserPage.locator("#account-navigation-toggle"))
        .hasAttribute("aria-expanded", "false");
    settings.open(AdminConfigGroup.GENERAL);
    String label = uiTexts.getAdminConfigEntryServiceName();
    settings.info(label).click();

    assertThat(settings.helpPanel().locator("vaadin-dialog-overlay > [part~='overlay']"))
        .isVisible();
    assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigEntryServiceNameTooltip());
    Locator overlay =
        browserPage.locator("#admin-config-help vaadin-dialog-overlay > [part~='overlay']");

    assertThat(overlay).hasCSS("width", "320px");
    BoundingBox bounds = overlay.boundingBox();

    assertTrue(
        bounds.x >= -1
            && bounds.y >= -1
            && bounds.x + bounds.width <= 321
            && bounds.y + bounds.height <= 569,
        "Feldinfo liegt teilweise außerhalb des mobilen Fensters: " + bounds);
    assertTrue(
        (Boolean)
            browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
    AllurePlaywright.attachScreenshot("Mobiler Klick-Tooltip bei 320×568", browserPage);
    settings.closeHelp();

    assertThat(settings.helpPanel()).hasCount(0);
    UserMenu userMenu = new UserMenu(browserPage);
    userMenu.open();
    Locator menuContent =
        browserPage.locator(
            "vaadin-popover-overlay[opened] > [part~='overlay'] > [part~='content']");

    assertTrue(
        (Boolean) menuContent.evaluate("e => e.scrollWidth <= e.clientWidth + 1"),
        "Das schmale Benutzermenü darf keinen horizontalen Überlauf haben.");
    Locator status = new UserStatusMenu(browserPage).current();
    status.scrollIntoViewIfNeeded();
    BoundingBox statusBounds = status.boundingBox();

    assertTrue(
        statusBounds.x >= 0 && statusBounds.x + statusBounds.width <= 321,
        "Statusauswahl muss im mobilen Fenster bleiben.");
    userMenu.close();
    userMenu.open();
    Locator logout =
        browserPage.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getBaseLogoutButton()).setExact(true));
    logout.scrollIntoViewIfNeeded();
    BoundingBox logoutBounds = logout.boundingBox();

    assertTrue(
        logoutBounds.y >= 0 && logoutBounds.y + logoutBounds.height <= 569,
        "Abmelden muss im gescrollten Benutzermenü sichtbar bleiben: " + logoutBounds);
    AllurePlaywright.attachScreenshot(
        "Mobiles Benutzermenü mit erreichbarer Abmeldung", browserPage);
    userMenu.signOut();
  }

  @org.junit.jupiter.params.ParameterizedTest(name = "CONFIG-06: Weitere Zeitfelder bei {0}px")
  @org.junit.jupiter.params.provider.ValueSource(ints = {1280, 390})
  @Tag("configuration-follow-up")
  @Description(
      "Zeitfelder in Sicherheit, E-Mail-Queue, Benutzerstatus und Benachrichtigungen speichern und neu laden. Sitzungs-Feldhilfe und Neustartwarnung bei unverändertem und geändertem Anmeldeschlüssel prüfen. Originalwerte werden über die UI wiederhergestellt.")
  void remainingDurationFieldsPersistWithoutExposingTheCalculationBelowTheInput(int width) {
    AdminConfigurationPage settings = admin();
    browserPage.setViewportSize(width, 844);
    assertSessionTimeoutUnitFits(settings);
    assertRestartHelpAndNotice(settings);
    AdminConfigGroup[] groups = {
      AdminConfigGroup.SECURITY,
      AdminConfigGroup.USER_STATUS,
      AdminConfigGroup.SECURITY,
      AdminConfigGroup.QUEUE,
      AdminConfigGroup.NOTIFICATIONS,
      AdminConfigGroup.NOTIFICATIONS
    };
    String[] labels = {
      uiTexts.getAdminConfigEntryRememberMeDuration(),
      uiTexts.getAdminConfigEntryUserAutoAbsentTimeout(),
      uiTexts.getAdminConfigEntryLoginAttemptWindow(),
      uiTexts.getAdminConfigEntryEmailQueueRetryDelay(),
      uiTexts.getAdminConfigEntryNotificationInboxRetention(),
      uiTexts.getAdminConfigEntryNotificationHistoryRetention()
    };
    for (int index = 0; index < labels.length; index++) {
      settings.open(groups[index]);
      String label = labels[index];
      boolean daysOnly = groups[index] == AdminConfigGroup.NOTIFICATIONS;
      String original = settings.value(label);
      java.math.BigDecimal originalSeconds = daysOnly ? null : settings.seconds(label);
      try {
        if (daysOnly) {
          settings.enter(label, "2");
        } else {
          settings.enterSeconds(label, java.math.BigDecimal.valueOf(120));
        }
        settings.save();
        settings.reload();
        java.math.BigDecimal expected = java.math.BigDecimal.valueOf(daysOnly ? 172800 : 120);

        assertEquals(0, expected.compareTo(settings.seconds(label)));
        assertThat(settings.duration(label).locator(".admin-help-default")).hasCount(0);
        settings.info(label).click();

        assertThat(settings.helpPanel())
            .containsText(uiTexts.getAdminConfigDurationSeconds(expected));
        settings.closeHelp();
        if (daysOnly) {
          settings.enter(label, "");
          settings.save();
          settings.reload();

          assertThat(settings.input(label)).hasValue("");
          settings.info(label).click();

          assertThat(settings.helpPanel()).containsText(uiTexts.getAdminConfigUnlimited());
          settings.closeHelp();
        }
      } finally {
        if (settings.helpPanel().count() > 0) {
          settings.closeHelp();
        }
        if (daysOnly) {
          settings.enter(label, original);
        } else {
          settings.enterSeconds(label, originalSeconds);
        }
        settings.save();
        settings.reload();
        if (daysOnly) {
          assertThat(settings.input(label)).hasValue(original);
        } else {
          assertEquals(0, originalSeconds.compareTo(settings.seconds(label)));
        }
      }
    }
  }

  private void assertRestartHelpAndNotice(AdminConfigurationPage settings) {
    settings.open(AdminConfigGroup.SECURITY);
    String timeout = uiTexts.getAdminConfigEntrySessionTimeout();
    settings.info(timeout).click();
    Locator timing = settings.helpPanel().locator(".admin-help-effective-use");

    assertThat(timing.getByRole(AriaRole.HEADING)).hasText(uiTexts.getAdminConfigEffectiveTitle());
    assertThat(timing.locator("li"))
        .hasText(
            new String[] {
              uiTexts.getAdminConfigEffectiveSessionNew(),
              uiTexts.getAdminConfigEffectiveSessionExisting()
            });
    assertThat(settings.helpPanel().locator(".admin-help-restart-warning")).hasCount(0);
    settings.closeHelp();

    String key = uiTexts.getAdminConfigEntryRememberMeSecretKey();
    settings.info(key).click();
    Locator warning =
        settings.helpPanel().locator(".admin-help-restart-warning.cosmos-security-warning");

    assertThat(warning).isVisible();
    assertThat(warning).hasCSS("border-inline-start-width", "4px");
    assertThat(warning).containsText(uiTexts.getAdminConfigRestartRequired());
    assertThat(warning).containsText(uiTexts.getAdminConfigRestartDetail());
    settings.closeHelp();

    BigDecimal originalTimeout = settings.seconds(timeout);
    String originalKey = settings.value(key);
    Locator notice =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigSaveSuccess()));
    Locator restartNotice =
        notice.filter(new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigRestartSaved()));
    try {
      settings.enterSeconds(timeout, originalTimeout.add(BigDecimal.ONE));
      settings.submit();

      assertThat(notice).isVisible();
      assertThat(restartNotice).hasCount(0);
      notice.getByRole(AriaRole.BUTTON).click();

      assertThat(notice).hasCount(0);

      settings.enter(key, "browser-restart-probe-" + UUID.randomUUID());
      settings.enterSeconds(timeout, originalTimeout);
      settings.submit();

      assertThat(restartNotice).isVisible();
      assertThat(restartNotice.locator("p.cosmos-notification-message"))
          .hasText(uiTexts.getAdminConfigRestartSaved());
      Locator affectedFields = restartNotice.locator("ul.cosmos-notification-details > li");

      assertThat(affectedFields).hasCount(1);
      assertThat(affectedFields).hasText(key);
      assertThat(affectedFields).hasCSS("font-weight", "700");
      assertThat(restartNotice).not().containsText(timeout);
      restartNotice.getByRole(AriaRole.BUTTON).click();

      assertThat(notice).hasCount(0);
    } finally {
      if (notice.count() > 0) {
        notice.getByRole(AriaRole.BUTTON).click();
      }
      settings.enter(key, originalKey);
      settings.enterSeconds(timeout, originalTimeout);
      settings.save();
    }
  }

  private void assertSessionTimeoutUnitFits(AdminConfigurationPage settings) {
    settings.open(AdminConfigGroup.SECURITY);
    String label = uiTexts.getAdminConfigEntrySessionTimeout();
    settings.enterSeconds(label, java.math.BigDecimal.valueOf(119));
    Locator select = settings.duration(label).locator("vaadin-select");
    Locator selectedUnit = select.locator("vaadin-select-value-button");

    assertThat(selectedUnit).hasText(uiTexts.getAdminConfigDurationUnitSeconds());
    assertTrue(
        (Boolean)
            select.evaluate(
                """
                select => {
                  const value = select.querySelector('vaadin-select-value-button');
                  const item = value.querySelector('vaadin-select-item');
                  const toggle = select.shadowRoot.querySelector('[part~="toggle-button"]');
                  const range = document.createRange();
                  range.selectNodeContents(item);
                  const text = range.getBoundingClientRect();
                  return text.width > 0
                    && text.right <= value.getBoundingClientRect().right + 1
                    && text.right <= toggle.getBoundingClientRect().left;
                }
                """),
        "Die ausgeschriebene Zeiteinheit muss vollständig vor dem Auswahlpfeil stehen");
    settings.reload();
  }

  private AdminConfigurationPage admin() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    return new AdminConfigurationPage(browserPage);
  }

  @Test
  @DisplayName("CONFIG-06: Offline-Speichern meldet keinen Erfolg; Bearbeitung bleibt fortsetzbar")
  @Description(
      """
      1. Eigenen Konfigurationsentwurf öffnen und nur diesen Browser offline schalten.
      2. Speichern auslösen und den fehlgeschlagenen UIDL-Transport beobachten: keine Erfolgsmeldung.
      3. Verbindung wiederherstellen und frisch laden: der ursprüngliche Wert ist gespeichert.
      4. Den Entwurf bewusst erneut speichern, neu laden und den Originalwert wiederherstellen.
      **Erwartet:** Kein falscher Offline-Erfolg; nach Wiederverbindung ist Weiterarbeiten möglich.
      """)
  void offlineSaveHasNoFalseSuccessAndCanBeContinued() {
    AdminConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String label = uiTexts.getAdminConfigEntryServiceName();
    String original = settings.value(label);
    String draft = "Offline " + UUID.randomUUID().toString().substring(0, 8);
    java.util.concurrent.atomic.AtomicBoolean failed =
        new java.util.concurrent.atomic.AtomicBoolean();
    browserPage.onRequestFailed(request -> {
      if (request.method().equals("POST") && request.url().contains("v-r=uidl")) {
        failed.set(true);
      }
    });
    try {
      settings.enter(label, draft);
      browserContext.setOffline(true);
      browserPage
          .getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName(uiTexts.getAdminConfigSave()).setExact(true))
          .click(new Locator.ClickOptions().setForce(true));
      browserContext.waitForCondition(failed::get);

      assertThat(
              browserPage
                  .locator("vaadin-notification-card")
                  .filter(
                      new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigSaveSuccess())))
          .hasCount(0);
      browserContext.setOffline(false);
      browserPage.reload();

      assertThat(settings.input(label)).hasValue(original);
      settings.enter(label, draft);
      settings.save();
      settings.reload();

      assertThat(settings.input(label)).hasValue(draft);
    } finally {
      browserContext.setOffline(false);
      browserPage.reload();
      settings.enter(label, original);
      settings.save();
    }
  }
}
