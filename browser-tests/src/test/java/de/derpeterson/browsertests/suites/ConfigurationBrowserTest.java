package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.*;

@Tag("browser")
@Feature("Weitere sichtbare Konfigurationsverträge")
public class ConfigurationBrowserTest extends BrowserFixture {
  @Test
  @DisplayName(
      "CONFIG-04A: Dienstname und Basis-URL validieren; Name speichern und wiederherstellen")
  @Description(
      """
        1. Allgemeine Gruppe öffnen; Originalnamen und URL merken.
        2. Blanken Namen bzw. ungültige URL versuchen: Feldfehler und unveränderte Werte nach Neuladen.
        3. Eigenen kurzen Unicode-Dienstnamen speichern und frisch laden: identischer Wert.
        4. Originalnamen wieder speichern; Basis-URL bleibt unverändert.
        **Erwartet:** Stringvalidierung und Speicherung sind sichtbar wirksam; kein Neustartnachweis.
        """)
  void stringValidationAndPersistence() {
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String name = texts.getAdminConfigEntryServiceName(), url = texts.getAdminConfigEntryBaseUrl();
    String original = settings.value(name), originalUrl = settings.value(url);
    try {
      settings.enter(name, "");
      settings.reject("vaadin-text-field", texts.getAdminConfigValidationServiceName());
      settings.reload();
      assertThat(settings.input(name)).hasValue(original);
      settings.enter(url, "invalid");
      settings.reject("vaadin-text-field", texts.getAdminConfigValidationUrl());
      settings.reload();
      assertThat(settings.input(url)).hasValue(originalUrl);
      settings.enter(name, "Browserprüfung ÄÖÜ");
      settings.save();
      settings.reload();
      assertThat(settings.input(name)).hasValue("Browserprüfung ÄÖÜ");
      try (Page freshHome = context.newPage()) {
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
        2. Checkbox umschalten, speichern und neu laden: exakt geänderter Checkboxzustand.
        3. Originalzustand wieder speichern und neu prüfen.
        **Erwartet:** Boolean-Bedienung und Persistenz; keine Behauptung einer Wartungsfunktion.
        """)
  void booleanPersistsAcrossReload() {
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.SECURITY);
    Locator checkbox =
        page.getByRole(
            AriaRole.CHECKBOX,
            new Page.GetByRoleOptions()
                .setName(texts.getAdminConfigEntryMaintenanceMode())
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
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.SECURITY);
    String label = texts.getAdminConfigEntryMaxSessionsPerUser(), original = settings.value(label);
    try {
      settings.enter(label, "0");
      settings.reject("vaadin-integer-field", texts.getAdminConfigValidationMinOne());
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
  @DisplayName("CONFIG-05A: SideNav auf- und zuklappen, Tastatur und Sprache ohne Formularverlust")
  @Description(
      """
        1. Allgemein über die native Konfigurationshierarchie öffnen und einen ungespeicherten Namen eingeben.
        2. Elternpunkt per Klick einklappen: Allgemein bleibt sichtbar und die Eingabe erhalten.
        3. Per Enter öffnen, per Leertaste schließen und erneut öffnen; alle sieben Gruppen bleiben erreichbar.
        4. Über das Avatarmenü die andere Sprache wählen und wieder zurück: Gruppe und Eingabe bleiben erhalten.
        **Erwartet:** COGS-Elternpunkt navigiert nicht, native Tastaturbedienung und Sprachwechsel funktionieren.
        """)
  void hierarchyTogglePreservesDraftAndSupportsKeyboardAndLanguage() {
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    AdminNavigation navigation = new AdminNavigation(page);
    String label = texts.getAdminConfigEntryServiceName(),
        draft = "Draft " + UUID.randomUUID().toString().substring(0, 8);
    String original = settings.value(label);
    settings.enter(label, draft);
    Locator parent = navigation.configuration(), toggle = parent.getByRole(AriaRole.BUTTON);
    toggle.click();
    assertThat(parent).hasJSProperty("expanded", false);
    assertThat(settings.input(label)).hasValue(draft);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getAdminConfigGroupGeneral())
                    .setLevel(2)
                    .setExact(true)))
        .isVisible();
    toggle.focus();
    page.keyboard().press("Enter");
    assertThat(parent).hasJSProperty("expanded", true);
    page.keyboard().press("Space");
    assertThat(parent).hasJSProperty("expanded", false);
    page.keyboard().press("Enter");
    assertThat(parent).hasJSProperty("expanded", true);
    for (AdminConfigGroup group : AdminConfigGroup.values())
      assertThat(
              parent.getByRole(
                  AriaRole.LINK,
                  new Locator.GetByRoleOptions()
                      .setName(texts.getAdminConfigGroupLabel(group))
                      .setExact(true)))
          .isVisible();
    var other =
        new de.derpeterson.browsertests.i18n.UiTexts(texts.language().equals("de") ? "en" : "de");
    try {
      new UserMenu(page).selectLanguage(other.language());
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
          "SideNav und ungespeicherte Eingabe nach Sprachwechsel", page);
    } finally {
      new UserMenu(page).selectTestLanguage();
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
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String label = texts.getAdminConfigEntryServiceName(), draft = "Tooltip draft";
    assertThat(settings.input(label)).hasAttribute("placeholder", "VaadinSpringApp (Browser-Test)");
    settings.enter(label, draft);
    settings.info(label).click();
    assertThat(settings.tooltip()).isVisible();
    Locator overlay = page.locator("vaadin-tooltip-overlay[opened] [part='overlay']");
    Locator card = page.locator(".cosmos-config-card");
    for (String property :
        new String[] {"background-image", "border-radius", "border-top-color", "box-shadow"})
      assertThat(overlay)
          .hasCSS(
              property,
              (String)
                  card.evaluate("(el, p) => getComputedStyle(el).getPropertyValue(p)", property));
    assertThat(overlay).hasCSS("width", "360px");
    assertThat(settings.tooltip()).hasCSS("font-size", "12px");
    assertThat(settings.tooltip()).hasCSS("font-weight", "400");
    assertThat(settings.tooltip()).containsText(texts.getAdminConfigEntryServiceNameTooltip());
    assertThat(settings.tooltip())
        .containsText(texts.getAdminConfigDefault() + ": VaadinSpringApp (Browser-Test)");
    assertThat(settings.tooltip()).containsText(texts.getAdminConfigHelperServiceNameMax(30));
    assertThat(page.locator("vaadin-dialog-overlay[opened]")).hasCount(0);
    AllurePlaywright.attachScreenshot("Klick-Tooltip mit tatsächlichem Default und Regeln", page);
    page.getByRole(
            AriaRole.HEADING,
            new Page.GetByRoleOptions()
                .setLevel(1)
                .setName(texts.getAdminConfigTitle())
                .setExact(true))
        .click();
    assertThat(settings.tooltip()).hasCount(0);
    settings.explain(
        label,
        texts.getAdminConfigEntryServiceNameTooltip(),
        texts.getAdminConfigHelperServiceNameMax(30));
    assertThat(settings.input(label)).hasValue(draft);
    var other =
        new de.derpeterson.browsertests.i18n.UiTexts(texts.language().equals("de") ? "en" : "de");
    try {
      new UserMenu(page).selectLanguage(other.language());
      settings.info(other.getAdminConfigEntryServiceName()).click();
      assertThat(settings.tooltip()).containsText(other.getAdminConfigEntryServiceNameTooltip());
      assertThat(settings.tooltip()).containsText(other.getAdminConfigHelperServiceNameMax(30));
      assertThat(settings.input(other.getAdminConfigEntryServiceName())).hasValue(draft);
      settings.info(other.getAdminConfigEntryServiceName()).click();
    } finally {
      new UserMenu(page).selectTestLanguage();
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
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.QUEUE);
    String label = texts.getAdminConfigEntryEmailQueueSentLiveDuration(),
        original = settings.value(label);
    try {
      settings.enter(label, "PT0S");
      settings.reject("vaadin-text-field", texts.getAdminConfigValidationPositiveDuration());
      settings.reload();
      assertThat(settings.input(label)).hasValue(original);
      settings.enter(label, "1s");
      settings.save();
      settings.reload();
      assertThat(settings.input(label)).hasValue("PT1S");
    } finally {
      settings.enter(label, original);
      settings.save();
    }
  }

  @Test
  @DisplayName("CONFIG-05D: Alle 29 Feldinfos bleiben in ihren sieben Gruppen erreichbar")
  @Description(
      """
        1. Alle sieben Konfigurationsgruppen über die native SideNav öffnen.
        2. Jede Feldinfo per Klick öffnen: übersetzter Standardwert und nicht leere Eingaberegeln.
        3. Nur eine Info gleichzeitig sichtbar; per Info-Button wieder schließen.
        4. Ein gesetzter Standard-Geheimschlüssel bleibt vertraulich; das leere SMTP-Passwort zeigt ausdrücklich den Leerhinweis.
        5. Jede Card zeigt die Anzahl ihrer tatsächlichen Felder; insgesamt sind 29 Infos erreichbar.
        **Erwartet:** Keine Gruppe verliert ihre Bedienung; alle bisherigen Felder erhalten die gemeinsame Info-Funktion.
        """)
  void everyConfigurationFieldHasAnAccessibleLocalizedInfo() {
    ConfigurationPage settings = admin();
    int total = 0;
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      settings.open(group);
      Locator infos = page.locator(".cosmos-config-fields .cosmos-config-info");
      int count = infos.count();
      assertTrue(count > 0);
      assertThat(page.locator(".cosmos-config-setting-count"))
          .hasText(texts.getAdminConfigSettingsCount(count));
      for (Locator info : infos.all()) {
        info.click();
        assertThat(settings.tooltip()).hasCount(1);
        assertThat(settings.tooltip()).isVisible();
        assertThat(settings.tooltip()).containsText(texts.getAdminConfigDefault());
        assertThat(settings.tooltip()).containsText(texts.getAdminConfigInputRules() + ":");
        assertTrue(
            settings.tooltip().innerText().contains("• "),
            "Feldinfo hat keine Eingaberegel: " + info.getAttribute("aria-labelledby"));
        if (info.getAttribute("aria-labelledby").equals("config-label-remember_me_secret_key"))
          assertThat(settings.tooltip()).containsText(texts.getAdminConfigDefaultConfidential());
        else if (info.getAttribute("aria-labelledby").equals("config-label-mail_password"))
          assertThat(settings.tooltip()).containsText(texts.getAdminConfigDefaultEmpty());
        info.click();
        assertThat(settings.tooltip()).hasCount(0);
      }
      total += count;
    }
    assertEquals(29, total);
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
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.GENERAL);
    String label = texts.getAdminConfigEntryServiceName(), original = settings.value(label);
    String changed = "Keyboard " + UUID.randomUUID().toString().substring(0, 8);
    try {
      settings.input(label).fill(changed);
      settings.input(label).press("Enter");
      Locator notice =
          page.locator("vaadin-notification-card:not([closing])")
              .filter(new Locator.FilterOptions().setHasText(texts.getAdminConfigSaveSuccess()));
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
        **Erwartet:** Die zentrale Tooltip-Breite berücksichtigt den schmalen Viewport ohne horizontalen Überlauf.
        """)
  void manualInfoRemainsWithinTheMobileViewport() {
    ConfigurationPage settings = admin();
    page.setViewportSize(320, 568);
    // Vaadin schaltet nach dem Resize asynchron vom Desktop-Drawer in den
    // geschlossenen Overlaymodus. Erst danach die Navigation tatsächlich öffnen.
    assertThat(page.locator("vaadin-app-layout")).hasAttribute("overlay", "");
    assertThat(page.locator("vaadin-app-layout")).not().hasAttribute("drawer-opened", "");
    settings.open(AdminConfigGroup.GENERAL);
    String label = texts.getAdminConfigEntryServiceName();
    settings.info(label).click();
    assertThat(settings.tooltip()).isVisible();
    assertThat(settings.tooltip()).containsText(texts.getAdminConfigEntryServiceNameTooltip());
    Locator overlay = page.locator("vaadin-tooltip-overlay[opened] [part='overlay']");
    assertThat(overlay).hasCSS("width", "288px");
    var bounds = overlay.boundingBox();
    assertTrue(
        bounds.x >= -1
            && bounds.y >= -1
            && bounds.x + bounds.width <= 321
            && bounds.y + bounds.height <= 569,
        "Feldinfo liegt teilweise außerhalb des mobilen Fensters: " + bounds);
    assertTrue(
        (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
    AllurePlaywright.attachScreenshot("Mobiler Klick-Tooltip bei 320×568", page);
    settings.info(label).click();
    assertThat(settings.tooltip()).hasCount(0);
  }

  private ConfigurationPage admin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    return new ConfigurationPage(page);
  }
}
