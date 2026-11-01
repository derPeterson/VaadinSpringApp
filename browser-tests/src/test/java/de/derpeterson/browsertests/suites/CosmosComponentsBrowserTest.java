package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminNavigation;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Gemeinsame Cosmos-Komponenten")
public class CosmosComponentsBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("COMPONENT-01: Rollenfilter erhält Auswahl und Anzahl in Anlage und Bearbeitung")
  @Description(
      "Auf Desktop und Smartphone in beiden Dialogen eine ausgewählte Rolle erhalten, nach einer anderen Rolle tippen und sie ergänzen. Eine erfolglose Suche und das Leeren des Suchtexts ändern die zwei ausgewählten Rollen nicht. Nach Wiederöffnen sind beide Optionen ausgewählt; Abbrechen speichert nichts.")
  void roleSearchPreservesSelectedRolesInBothDialogs() {
    signIn();
    AdminUsersPage users = new AdminUsersPage(page);
    String email = "role-search-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      checkRoleSearch(users.openCreate());
      checkRoleSearch(users.openEdit(email));
    }
  }

  private void checkRoleSearch(AdminUserDialog dialog) {
    dialog.setRole(texts.getRoleUser(), true);
    Locator input = dialog.root().locator(".admin-user-roles input");
    Locator summary = dialog.root().locator(".admin-user-role-summary");
    assertThat(summary).hasText(texts.getAdminUsersSelectedRoles(1));
    input.fill(texts.getRoleAdmin().substring(0, 4));
    Locator admin =
        page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(texts.getRoleAdmin()).setExact(true));
    assertThat(admin).isVisible();
    admin.click();
    assertThat(summary).hasText(texts.getAdminUsersSelectedRoles(2));
    input.fill("no-role-matches-this");
    assertThat(page.locator("vaadin-multi-select-combo-box-item:visible")).hasCount(0);
    assertThat(summary).hasText(texts.getAdminUsersSelectedRoles(2));
    input.fill("");
    assertThat(dialog.role(texts.getRoleUser())).hasAttribute("aria-selected", "true");
    assertThat(admin).hasAttribute("aria-selected", "true");
    input.press("Escape");
    input.click();
    assertThat(dialog.role(texts.getRoleUser())).hasAttribute("aria-selected", "true");
    assertThat(admin).hasAttribute("aria-selected", "true");
    assertThat(summary).hasText(texts.getAdminUsersSelectedRoles(2));
    AllurePlaywright.attachScreenshot("Rollen nach Suche und Wiederöffnen", page);
    dialog.cancel();
  }

  @Test
  @DisplayName(
      "COMPONENT-02: Konfigurationsswitches bleiben links beschriftet und rechts bedienbar")
  @Description(
      "Alle fünf Boolean-Einstellungen der Sicherheits- und SMTP-Gruppen auf Desktop und Smartphone vermessen: Text links, Switch rechts, Info außerhalb. Label und Schalter schalten denselben Zustand. Der Marker bleibt innerhalb der Bahn vertikal zentriert. Keine Einstellung wird gespeichert.")
  void configurationSwitchGeometryAndBothClickTargets() {
    signIn();
    ConfigurationPage settings = new ConfigurationPage(page);
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      int checkedFields = 0;
      for (AdminConfigGroup group : List.of(AdminConfigGroup.SECURITY, AdminConfigGroup.SMTP)) {
        settings.open(group);
        for (Locator row : page.locator(".admin-config-switch .cosmos-config-input-row").all()) {
          Locator toggle = row.locator("vaadin-switch");
          Locator input = toggle.locator("input[slot=input]");
          Locator label = toggle.locator("[id^=config-label-]");
          Locator track = toggle.locator("[part=switch]");
          Locator info = row.locator(".cosmos-config-info");
          BoundingBox textBounds = label.boundingBox(), trackBounds = track.boundingBox();
          assertThat(label).hasCSS("font-size", "12px");
          assertThat(label).hasCSS("font-weight", "500");
          assertTrue(textBounds.x + textBounds.width <= trackBounds.x + 1);
          assertEquals(
              toggle.boundingBox().x + toggle.boundingBox().width,
              trackBounds.x + trackBounds.width,
              1);
          assertEquals(
              textBounds.y + textBounds.height / 2, trackBounds.y + trackBounds.height / 2, 2);
          assertTrue(info.boundingBox().x >= toggle.boundingBox().x + toggle.boundingBox().width);
          assertEquals(
              trackBounds.y + trackBounds.height / 2,
              info.boundingBox().y + info.boundingBox().height / 2,
              1);
          boolean original = input.isChecked();
          label.click();
          assertThat(input)
              .isChecked(
                  new com.microsoft.playwright.assertions.LocatorAssertions.IsCheckedOptions()
                      .setChecked(!original));
          if (!original) {
            assertSwitchMarker(track);
          }
          BoundingBox clickTarget = track.boundingBox();
          page.mouse()
              .click(clickTarget.x + clickTarget.width / 2, clickTarget.y + clickTarget.height / 2);
          assertThat(input)
              .isChecked(
                  new com.microsoft.playwright.assertions.LocatorAssertions.IsCheckedOptions()
                      .setChecked(original));
          if (original) {
            assertSwitchMarker(track);
          }
          checkedFields++;
        }
      }
      assertEquals(5, checkedFields);
      AllurePlaywright.attachScreenshot("Switchzeilen bei " + width + "px", page);
    }
  }

  private void assertSwitchMarker(Locator track) {
    Locator marker = track.locator("[part=marker]");
    assertThat(track).hasCSS("width", "34px");
    assertThat(track).hasCSS("height", "20px");
    assertThat(marker).hasCSS("width", "14px");
    assertThat(marker).hasCSS("height", "14px");
    BoundingBox area = track.boundingBox();
    BoundingBox thumb = marker.boundingBox();
    assertEquals(area.y + area.height / 2, thumb.y + thumb.height / 2, 1);
    assertTrue(thumb.x >= area.x && thumb.x + thumb.width <= area.x + area.width);
  }

  @Test
  @DisplayName(
      "COMPONENT-03: SideNav-Hover verschiebt nichts; aktive Haupt- und Unterpunkte bleiben blau")
  @Description(
      "Portal und Admin bei 1280/390px prüfen. Handzeiger, linke Icon-/Textausrichtung, vertikale Mitte und stabile Klickfläche bei Hover. Aktive Hauptpunkte sind 600 statt 500, Unterpunkte 500 statt 400. Hover erhält die aktive blaue Fläche.")
  void navigationStatesAreConsistentInPortalAndAdministration() {
    signIn();
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      AdminNavigation navigation = new AdminNavigation(page);
      navigation.openGroup(AdminConfigGroup.GENERAL);
      navigation.openDrawer();
      checkNav(page.locator("#admin-nav-mail_test"), "500", false);
      checkNav(navigation.configuration(), "600", true);
      checkNav(page.locator("#admin-nav-configuration-general"), "500", true);
      checkNav(page.locator("#admin-nav-configuration-security"), "400", false);
      navigation.closeOverlayDrawer();
      page.navigate(path(HOME_ROUTE));
      assertThat(page.locator("#portal-home-account")).isVisible();
      page.waitForFunction(
          """
          () => {
            const buttons = [...document.querySelectorAll('.portal-home-actions vaadin-button')];
            return buttons.length === 2 && Math.abs(buttons[0].getBoundingClientRect().width
              - buttons[1].getBoundingClientRect().width) < 1;
          }
          """);
      openPortalNavigation();
      checkNav(page.locator("#account-nav-home"), "600", true);
      checkNav(page.locator("#account-nav-notifications"), "500", false);
      checkNav(page.locator("#account-nav-profile"), "500", false);
      UiActions.complete(page, () -> page.locator("#account-nav-profile [part=link]").click());
      openPortalNavigation();
      checkNav(page.locator("#account-tab-profile"), "600", true);
      checkNav(page.locator("#account-tab-security"), "500", false);
      AllurePlaywright.attachScreenshot("Portalnavigation bei " + width + "px", page);
      page.navigate(path(HOME_ROUTE));
      page.navigate(path(ADMIN_ROUTE));
    }
  }

  private void openPortalNavigation() {
    Locator toggle = page.locator("#account-navigation-toggle");
    assertThat(toggle).hasCount(1);
    if (toggle.isVisible() && !"true".equals(toggle.getAttribute("aria-expanded"))) {
      UiActions.complete(page, toggle::click);
    }
    assertThat(page.locator("#account-sidebar")).isVisible();
  }

  private void checkNav(Locator item, String weight, boolean active) {
    Locator link = item.locator(":scope > [part=content] > [part=link]");
    Locator content = item.locator(":scope > [part=content]");
    assertThat(link).hasCSS("cursor", "pointer");
    assertThat(link).hasCSS("font-weight", weight);
    assertThat(link).hasCSS("text-align", "left");
    BoundingBox before = content.boundingBox();
    page.mouse().move(1, 1);
    String background = (String) content.evaluate("el => getComputedStyle(el).backgroundColor");
    link.hover();
    BoundingBox after = content.boundingBox();
    assertEquals(before.x, after.x, 1);
    assertEquals(before.y, after.y, 1);
    assertEquals(before.width, after.width, 1);
    assertEquals(before.height, after.height, 1);
    assertThat(content).hasCSS("background-color", active ? background : "rgb(21, 34, 56)");
    if (active) {
      assertNotEquals("rgba(0, 0, 0, 0)", background);
    }
    BoundingBox icon = item.locator(":scope > vaadin-icon").boundingBox();
    List<?> label =
        (List<?>)
            item.evaluate(
                """
        el => {
          const node = el.shadowRoot.querySelector('slot:not([name])').assignedNodes({flatten:true})
            .find(node => node.textContent.trim());
          const range = document.createRange(); range.selectNodeContents(node);
          const box = range.getBoundingClientRect();
          return [box.x, box.y + box.height / 2];
        }
        """);
    assertEquals(8, ((Number) label.get(0)).doubleValue() - icon.x - icon.width, 2);
    assertEquals(icon.y + icon.height / 2, ((Number) label.get(1)).doubleValue(), 2);
    page.mouse().move(1, 1);
  }

  @Test
  @DisplayName("COMPONENT-04: Tooltip A gilt für Aktionen, Feldinfos und HTML-Referenzen")
  @Description(
      "Kurze Aktionshinweise per Hover und Tastatur öffnen, lange Mailinfos per Klick prüfen. Auf Desktop und Smartphone gleiche deckende Fläche, Schrift und Rahmen ohne Überlauf. Die drei bedienbaren HTML-Referenzen verwenden denselben Tooltipstil und erhalten ihre zugänglichen Beschriftungen.")
  void tooltipDesignCoversActionsFieldHintsAndReferences() {
    signIn();
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      Locator account =
          page.locator(".cosmos-topbar")
              .getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions()
                      .setName(texts.getBaseManageAccountButton())
                      .setExact(true));
      account.hover();
      assertTooltip(page.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      page.mouse().move(1, 1);
      account.focus();
      assertTooltip(page.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      page.keyboard().press("Escape");
      new AdminMailPage(page).open();
      page.getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName(texts.getAdminMailRecipient()).setExact(true))
          .click();
      assertThat(page.getByRole(AriaRole.TOOLTIP)).containsText(texts.getAdminMailRecipientHint());
      assertTooltip(page.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      AllurePlaywright.attachScreenshot("Feldtooltip A bei " + width + "px", page);
      page.getByRole(
              AriaRole.HEADING,
              new Page.GetByRoleOptions()
                  .setName(texts.getAdminMailTitle())
                  .setLevel(1)
                  .setExact(true))
          .click();
      assertThat(page.getByRole(AriaRole.TOOLTIP)).hasCount(0);
    }
    Path references = references();
    List<String> referenceErrors = new ArrayList<>();
    page.onPageError(referenceErrors::add);
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      for (String name : List.of("public.html", "portal.html", "admin.html")) {
        page.navigate(
            references.resolve(name).toUri().toString()
                + (name.equals("public.html") ? "#public-login" : ""));
        page.evaluate("() => document.fonts.ready");
        Locator trigger =
            page.locator("button[aria-label]:visible").all().stream()
                .filter(
                    button ->
                        !button.getAttribute("aria-label").equals(button.textContent().trim()))
                .findFirst()
                .orElseThrow();
        String label = trigger.getAttribute("aria-label");
        String hint =
            (String)
                trigger.evaluate(
                    "el => el.dataset.refTooltip || el.dataset.tooltip || el.getAttribute('aria-label')");
        trigger.focus();
        Locator tooltip = page.locator("#reference-tooltip:visible");
        assertThat(tooltip).hasText(hint);
        assertTooltip(tooltip);
        page.keyboard().press("Escape");
        assertThat(tooltip).hasCount(0);
        assertThat(trigger).hasAttribute("aria-label", label);
        AllurePlaywright.attachScreenshot("Referenz " + name + " bei " + width + "px", page);
      }
    }
    assertEquals(List.of(), referenceErrors, "Die ausgelagerten Referenzcontroller müssen laufen");
  }

  @Test
  @DisplayName(
      "COMPONENT-05: Bereinigtes Benutzermenü bleibt auf allen angemeldeten Seiten bedienbar")
  @Description(
      "Admin, Startseite und Konto auf Desktop und Smartphone öffnen. Identität, vier sichtbare Statusoptionen, Sprachwahl und rechtsbündiges Abmelden bleiben innerhalb des Viewports. Das Menü enthält keine zweite versteckte Statusauswahl oder alte Kopfzeile. Mein Konto navigiert, Escape und Außenklick schließen. Bei vorausgewähltem Deutsch und Englisch aktualisiert ein Sprachwechsel sowohl Feldwert als auch Optionen, ohne die Auswahl zu ändern. Keine Statusänderung und kein Speichern des Profilformulars.")
  void userMenuKeepsItsLayoutNavigationAndNativeClosing() {
    signIn();
    UserMenu menu = new UserMenu(page);
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, 900);
      for (String route : List.of(ADMIN_ROUTE, HOME_ROUTE, ACCOUNT_ROUTE)) {
        page.navigate(path(route));
        menu.open();
        Locator body = page.locator(".portal-user-menu-body:visible");
        Locator overlay = page.locator("vaadin-popover-overlay[opened] > [part~='overlay']");
        assertThat(body.locator(".portal-menu-name")).isVisible();
        assertThat(body.locator(".portal-menu-email")).isVisible();
        assertThat(body.locator(".portal-menu-language-label"))
            .hasCSS("color", "rgb(226, 232, 240)");
        assertThat(body.locator(".portal-menu-language-label .portal-menu-section"))
            .hasCSS("color", "rgb(226, 232, 240)");
        assertThat(body.locator(".portal-status-option")).hasCount(4);
        assertThat(body.locator(".portal-status-option[aria-pressed=true]")).hasCount(1);
        assertThat(body.locator(".portal-language-switch vaadin-button")).hasCount(2);
        assertThat(body.locator("vaadin-select, .user-popover-header")).hasCount(0);
        Locator account =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(texts.getBaseManageAccountButton())
                    .setExact(true));
        assertThat(account).isVisible();
        Locator home =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(texts.getBaseHomeButton()).setExact(true));
        assertThat(home).hasCount(route.equals(HOME_ROUTE) ? 0 : 1);
        Locator admin =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(texts.getBaseManageApplicationButton())
                    .setExact(true));
        assertThat(admin).hasCount(route.equals(ADMIN_ROUTE) ? 0 : 1);
        BoundingBox bounds = overlay.boundingBox();
        assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= width + 1);
        assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= 901);
        BoundingBox footer = body.locator(".portal-menu-footer").boundingBox();
        BoundingBox logout = body.locator(".portal-menu-logout").boundingBox();
        assertEquals(footer.x + footer.width - 8, logout.x + logout.width, 1);
        AllurePlaywright.attachScreenshot("Benutzermenü " + route + " bei " + width + "px", page);
        menu.close();
        menu.open();
        page.mouse().click(1, 880);
        assertThat(page.locator("vaadin-popover-overlay[opened], vaadin-popover-overlay[closing]"))
            .hasCount(0);
        menu.open();
        account.click();
        assertThat(page.locator("#account-panel-profile")).isVisible();
        assertThat(page.locator("vaadin-popover-overlay[opened]")).hasCount(0);
      }
      assertPreferredLanguageLabels(menu);
    }
  }

  private void assertPreferredLanguageLabels(UserMenu menu) {
    Locator field = page.locator("#account-language");
    Locator input = field.locator("input");
    Locale originalInterface = Locale.forLanguageTag(texts.language());
    String otherLanguage = texts.language().equals("de") ? "en" : "de";
    for (Locale selected : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      field.click();
      page.getByRole(
              AriaRole.OPTION,
              new Page.GetByRoleOptions()
                  .setName(selected.getDisplayLanguage(originalInterface))
                  .setExact(true))
          .click();
      page.locator("#account-first-name input").click();
      for (String language : List.of(otherLanguage, texts.language())) {
        menu.selectLanguage(language);
        Locale currentInterface = Locale.forLanguageTag(language);
        String expected = selected.getDisplayLanguage(currentInterface);
        assertThat(input).hasValue(expected);
        field.click();
        for (Locale option : List.of(Locale.GERMAN, Locale.ENGLISH)) {
          assertThat(
                  page.getByRole(
                      AriaRole.OPTION,
                      new Page.GetByRoleOptions()
                          .setName(option.getDisplayLanguage(currentInterface))
                          .setExact(true)))
              .isVisible();
        }
        input.press("Escape");
        assertThat(input).hasValue(expected);
      }
    }
    AllurePlaywright.attachScreenshot("Vorausgewählte Sprache nach Sprachwechsel", page);
  }

  private void assertTooltip(Locator tooltip) {
    assertThat(tooltip).isVisible();
    assertThat(tooltip).hasCSS("background-color", "rgb(15, 23, 42)");
    assertThat(tooltip).hasCSS("border-top-color", "rgb(51, 65, 85)");
    assertThat(tooltip).hasCSS("border-top-width", "1px");
    assertThat(tooltip).hasCSS("border-radius", "4px");
    assertThat(tooltip).hasCSS("font-size", "11px");
    assertThat(tooltip).hasCSS("font-weight", "400");
    BoundingBox bounds = tooltip.boundingBox();
    assertTrue(bounds.width <= 221);
    assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= page.viewportSize().width + 1);
  }

  private Path references() {
    Path directory = Path.of("").toAbsolutePath();
    while (directory != null) {
      Path references = directory.resolve("docs/design/frontend-referenzen");
      if (Files.exists(references.resolve("public.html"))) {
        return references;
      }
      directory = directory.getParent();
    }
    throw new IllegalStateException("Die freigegebenen HTML-Referenzen fehlen.");
  }

  private void signIn() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
  }
}
