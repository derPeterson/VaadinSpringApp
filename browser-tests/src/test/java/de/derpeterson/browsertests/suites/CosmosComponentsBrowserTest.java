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
import com.microsoft.playwright.options.ForcedColors;
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
  @Tag("scrollbar-style")
  @DisplayName("COMPONENT-06: Globale Scrollleisten teilen Stil und erhalten die native Bedienung")
  @Description(
      "Synthetische native Komponenten in der isolierten Anwendung prüfen: Dokument, Chat-Scroller, Grid, TextArea und Dialog erhalten dieselbe Scrollgestaltung. Chatabstände bleiben lokal; horizontales und vertikales Scrollen sowie Systemkontrast bleiben bedienbar.")
  void globalScrollbarsPreserveNativeScrollingAndLocalChatSpacing() {
    signIn();
    browserPage.navigate("/admin?section=support");
    assertThat(browserPage.locator("#support-ticket-categories")).isVisible();
    browserPage.evaluate(
        """
        async () => {
          await Promise.all(['vaadin-scroller', 'vaadin-grid', 'vaadin-grid-column',
            'vaadin-text-area', 'vaadin-dialog'].map(tag => customElements.whenDefined(tag)));
          const fixture = document.createElement('vaadin-vertical-layout');
          fixture.id = 'scrollbar-test';
          fixture.className = 'cosmos-theme cosmos-reference-portal';
          fixture.style.cssText = 'position:fixed;inset:80px 16px 16px;z-index:100;background:#091120;padding:16px;gap:16px;overflow:auto';
          const chat = document.createElement('vaadin-scroller');
          chat.id = 'scrollbar-test-chat';
          chat.className = 'support-message-scroll';
          chat.style.cssText = 'height:130px;min-height:130px;width:100%';
          const thread = document.createElement('vaadin-vertical-layout');
          thread.className = 'support-message-thread';
          for (let index = 0; index < 12; index++) {
            const line = document.createElement('span');
            line.textContent = `Synthetic message ${index + 1}`;
            thread.append(line);
          }
          chat.append(thread);
          const grid = document.createElement('vaadin-grid');
          grid.id = 'scrollbar-test-grid';
          grid.style.cssText = 'height:130px;min-height:130px;width:100%';
          const column = document.createElement('vaadin-grid-column');
          column.header = 'Synthetic rows';
          column.width = '2000px';
          column.flexGrow = 0;
          column.renderer = (root, _column, data) => root.textContent = data.item.label;
          grid.append(column);
          grid.items = Array.from({length:30}, (_, index) => ({label:`Row ${index + 1}`}));
          const field = document.createElement('vaadin-text-area');
          field.id = 'scrollbar-test-field';
          field.label = 'Synthetic text';
          field.style.cssText = 'width:100%;max-height:130px;min-height:130px';
          field.value = Array.from({length:30}, (_, index) => `Line ${index + 1}`).join('\\n');
          fixture.append(chat, grid, field);
          const spacer = document.createElement('div');
          spacer.id = 'scrollbar-test-spacer';
          spacer.style.height = '200vh';
          document.body.append(fixture, spacer);
        }
        """);
    try {
      for (int width : new int[] {1280, 390}) {
        browserPage.setViewportSize(width, 900);
        Locator chat = browserPage.locator("#scrollbar-test-chat");
        Locator grid = browserPage.locator("#scrollbar-test-grid #table");
        Locator field = browserPage.locator("#scrollbar-test-field [part=input-field]");
        for (Locator area : List.of(chat, grid, field)) {
          assertScrollbarStyleAndMovement(area);
        }
        browserPage.waitForCondition(() ->
            (Boolean)
                browserPage.evaluate(
                    """
                    () => {
                      const thread = document.querySelector('#scrollbar-test-chat .support-message-thread');
                      const grid = document.querySelector('#scrollbar-test-grid');
                      return getComputedStyle(thread).paddingRight === '23px'
                        && getComputedStyle(grid).paddingRight === '0px'
                        && getComputedStyle(document.documentElement, '::-webkit-scrollbar').width
                          === getComputedStyle(document.documentElement).getPropertyValue('--cosmos-scrollbar-size').trim();
                    }
                    """));
        grid.evaluate("element => element.scrollLeft = 40");
        assertTrue((Boolean) grid.evaluate("element => element.scrollLeft > 0"));
        chat.evaluate("element => { element.scrollTop = 0; element.focus(); }");
        browserPage.keyboard().press("PageDown");
        browserPage.waitForCondition(
            () -> (Boolean) chat.evaluate("element => element.scrollTop > 0"));
        AllurePlaywright.attachScreenshot(
            "Gemeinsame native Scrollleisten bei " + width + "px", browserPage);
        browserPage.evaluate(
            """
            () => {
              const dialog = document.createElement('vaadin-dialog');
              dialog.id = 'scrollbar-test-dialog';
              dialog.className = 'cosmos-reference-overlay';
              dialog.setAttribute('theme', 'cosmos');
              dialog.height = '180px';
              dialog.width = '280px';
              dialog.renderer = root => {
                const content = document.createElement('vaadin-vertical-layout');
                content.style.minHeight = '600px';
                content.textContent = 'Synthetic dialog content';
                root.replaceChildren(content);
              };
              document.body.append(dialog);
              dialog.opened = true;
            }
            """);
        Locator content = browserPage.locator("#scrollbar-test-dialog [part=content]").first();
        assertScrollbarStyleAndMovement(content);
        AllurePlaywright.attachScreenshot(
            "Native Dialogscrollleiste bei " + width + "px", browserPage);
        browserPage.evaluate("document.querySelector('#scrollbar-test-dialog').remove()");
      }
      browserPage.evaluate("window.scrollTo(0, 50)");
      assertTrue((Boolean) browserPage.evaluate("window.scrollY > 0"));
      browserPage.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.ACTIVE));
      assertThat(browserPage.locator("#scrollbar-test-chat")).hasCSS("scrollbar-color", "auto");
      assertThat(browserPage.locator("#scrollbar-test-chat")).hasCSS("scrollbar-width", "auto");
    } finally {
      browserPage.emulateMedia(new Page.EmulateMediaOptions().setForcedColors(ForcedColors.NONE));
      browserPage.evaluate(
          "document.querySelectorAll('[id^=scrollbar-test]').forEach(element => element.remove())");
    }
  }

  private void assertScrollbarStyleAndMovement(Locator area) {
    assertThat(area).isVisible();
    browserPage.waitForCondition(() ->
        (Boolean)
            area.evaluate(
                """
                element => {
                  const size = getComputedStyle(element).getPropertyValue('--cosmos-scrollbar-size').trim();
                  return element.scrollHeight > element.clientHeight
                    && getComputedStyle(element, '::-webkit-scrollbar').width === size
                    && getComputedStyle(element, '::-webkit-scrollbar-thumb').backgroundColor
                      === getComputedStyle(document.documentElement, '::-webkit-scrollbar-thumb').backgroundColor;
                }
                """));
    area.evaluate("element => element.scrollTop = 0");
    area.hover();
    browserPage.mouse().wheel(0, 80);
    browserPage.waitForCondition(() -> (Boolean) area.evaluate("element => element.scrollTop > 0"));
  }

  @Test
  @DisplayName("COMPONENT-01: Rollenfilter erhält Auswahl und Anzahl in Anlage und Bearbeitung")
  @Description(
      "Auf Desktop und Smartphone in beiden Dialogen eine ausgewählte Rolle erhalten, nach einer anderen Rolle tippen und sie ergänzen. Eine erfolglose Suche und das Leeren des Suchtexts ändern die zwei ausgewählten Rollen nicht. Nach Wiederöffnen sind beide Optionen ausgewählt; Abbrechen speichert nichts.")
  void roleSearchPreservesSelectedRolesInBothDialogs() {
    signIn();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    String email = "role-search-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      checkRoleSearch(users.openCreate());
      checkRoleSearch(users.openEdit(email));
    }
  }

  private void checkRoleSearch(AdminUserDialog dialog) {
    dialog.setRole(uiTexts.getRoleUser(), true);
    Locator input = dialog.root().locator(".admin-user-roles input");
    Locator summary = dialog.root().locator(".admin-user-role-summary");
    assertThat(summary).hasText(uiTexts.getAdminUsersSelectedRoles(1));
    input.fill(uiTexts.getRoleAdmin().substring(0, 4));
    Locator admin =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getRoleAdmin()).setExact(true));
    assertThat(admin).isVisible();
    admin.click();
    assertThat(summary).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    input.fill("no-role-matches-this");
    assertThat(browserPage.locator("vaadin-multi-select-combo-box-item:visible")).hasCount(0);
    assertThat(summary).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    input.fill("");
    assertThat(dialog.role(uiTexts.getRoleUser())).hasAttribute("aria-selected", "true");
    assertThat(admin).hasAttribute("aria-selected", "true");
    input.press("Escape");
    input.click();
    assertThat(dialog.role(uiTexts.getRoleUser())).hasAttribute("aria-selected", "true");
    assertThat(admin).hasAttribute("aria-selected", "true");
    assertThat(summary).hasText(uiTexts.getAdminUsersSelectedRoles(2));
    AllurePlaywright.attachScreenshot("Rollen nach Suche und Wiederöffnen", browserPage);
    dialog.cancel();
  }

  @Test
  @DisplayName(
      "COMPONENT-02: Konfigurationsswitches bleiben links beschriftet und rechts bedienbar")
  @Description(
      "Alle fünf Boolean-Einstellungen der Sicherheits- und SMTP-Gruppen auf Desktop und Smartphone vermessen: Text links, Switch rechts, Info außerhalb. Label und Schalter schalten denselben Zustand. Der Marker bleibt innerhalb der Bahn vertikal zentriert. Keine Einstellung wird gespeichert.")
  void configurationSwitchGeometryAndBothClickTargets() {
    signIn();
    ConfigurationPage settings = new ConfigurationPage(browserPage);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      int checkedFields = 0;
      for (AdminConfigGroup group : List.of(AdminConfigGroup.SECURITY, AdminConfigGroup.SMTP)) {
        settings.open(group);
        for (Locator row :
            browserPage.locator(".admin-config-switch .cosmos-config-input-row").all()) {
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
          browserPage
              .mouse()
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
      AllurePlaywright.attachScreenshot("Switchzeilen bei " + width + "px", browserPage);
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
      browserPage.setViewportSize(width, 900);
      AdminNavigation navigation = new AdminNavigation(browserPage);
      navigation.openGroup(AdminConfigGroup.GENERAL);
      navigation.openDrawer();
      checkNav(browserPage.locator("#admin-nav-mail_test"), "500", false);
      checkNav(navigation.configuration(), "600", true);
      checkNav(browserPage.locator("#admin-nav-configuration-general"), "500", true);
      checkNav(browserPage.locator("#admin-nav-configuration-security"), "400", false);
      navigation.closeOverlayDrawer();
      browserPage.navigate(path(HOME_ROUTE));
      assertThat(browserPage.locator("#portal-home-account")).isVisible();
      browserPage.waitForFunction(
          """
          () => {
            const buttons = [...document.querySelectorAll('.portal-home-actions vaadin-button')];
            return buttons.length === 2 && Math.abs(buttons[0].getBoundingClientRect().width
              - buttons[1].getBoundingClientRect().width) < 1;
          }
          """);
      openPortalNavigation();
      checkNav(browserPage.locator("#account-nav-home"), "600", true);
      checkNav(browserPage.locator("#account-nav-notifications"), "500", false);
      checkNav(browserPage.locator("#account-nav-profile"), "500", false);
      UiActions.complete(
          browserPage, () -> browserPage.locator("#account-nav-profile [part=link]").click());
      openPortalNavigation();
      checkNav(browserPage.locator("#account-tab-profile"), "600", true);
      checkNav(browserPage.locator("#account-tab-security"), "500", false);
      AllurePlaywright.attachScreenshot("Portalnavigation bei " + width + "px", browserPage);
      browserPage.navigate(path(HOME_ROUTE));
      browserPage.navigate(path(ADMIN_ROUTE));
    }
  }

  private void openPortalNavigation() {
    Locator toggle = browserPage.locator("#account-navigation-toggle");
    assertThat(toggle).hasCount(1);
    if (toggle.isVisible() && !"true".equals(toggle.getAttribute("aria-expanded"))) {
      UiActions.complete(browserPage, toggle::click);
    }
    assertThat(browserPage.locator("#account-sidebar")).isVisible();
  }

  private void checkNav(Locator item, String weight, boolean active) {
    Locator link = item.locator(":scope > [part=content] > [part=link]");
    Locator content = item.locator(":scope > [part=content]");
    assertThat(link).hasCSS("cursor", "pointer");
    assertThat(link).hasCSS("font-weight", weight);
    assertThat(link).hasCSS("text-align", "left");
    BoundingBox before = content.boundingBox();
    browserPage.mouse().move(1, 1);
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
    browserPage.mouse().move(1, 1);
  }

  @Test
  @DisplayName("COMPONENT-04: Aktions-Tooltips und seitliche Feldhilfe bleiben unterscheidbar")
  @Description(
      "Auf Desktop und Smartphone die Gestaltung der Aktions-Tooltips bei Hover und Fokus prüfen. Die Mail-Feldhilfe öffnet als eigener Dialog und schließt per Escape.")
  void actionTooltipsAndFieldHelpUseDistinctOverlays() {
    signIn();
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      Locator account =
          browserPage
              .locator(".cosmos-topbar")
              .getByRole(
                  AriaRole.BUTTON,
                  new Locator.GetByRoleOptions()
                      .setName(uiTexts.getBaseManageAccountButton())
                      .setExact(true));
      account.hover();
      assertTooltip(browserPage.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      browserPage.mouse().move(1, 1);
      account.focus();
      assertTooltip(browserPage.locator("vaadin-tooltip-overlay[opened] [part=overlay]"));
      browserPage.keyboard().press("Escape");
      new AdminMailPage(browserPage).open();
      browserPage
          .getByRole(
              AriaRole.BUTTON,
              new Page.GetByRoleOptions().setName(uiTexts.getAdminMailRecipient()).setExact(true))
          .click();
      Locator fieldHelp = browserPage.locator("#admin-mail-field-help[opened]");
      assertThat(fieldHelp.locator("vaadin-dialog-overlay > [part~=overlay]")).isVisible();
      assertThat(fieldHelp.locator(".admin-help-description"))
          .hasText(uiTexts.getAdminMailRecipientHint());
      assertThat(browserPage.getByRole(AriaRole.TOOLTIP)).hasCount(0);
      AllurePlaywright.attachScreenshot(
          "Seitliche Mail-Feldhilfe bei " + width + "px", browserPage);
      browserPage.keyboard().press("Escape");
      assertThat(fieldHelp).hasCount(0);
    }
  }

  @Test
  @DisplayName(
      "COMPONENT-05: Bereinigtes Benutzermenü bleibt auf allen angemeldeten Seiten bedienbar")
  @Description(
      "Admin, Startseite und Konto auf Desktop und Smartphone öffnen. Identität, vier sichtbare Statusoptionen, Sprachwahl und rechtsbündiges Abmelden bleiben innerhalb des Viewports. Das Menü enthält keine zweite versteckte Statusauswahl oder alte Kopfzeile. Mein Konto navigiert, Escape und Außenklick schließen. Bei vorausgewähltem Deutsch und Englisch aktualisiert ein Sprachwechsel sowohl Feldwert als auch Optionen, ohne die Auswahl zu ändern. Keine Statusänderung und kein Speichern des Profilformulars.")
  void userMenuKeepsItsLayoutNavigationAndNativeClosing() {
    signIn();
    UserMenu menu = new UserMenu(browserPage);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      for (String route : List.of(ADMIN_ROUTE, HOME_ROUTE, ACCOUNT_ROUTE)) {
        browserPage.navigate(path(route));
        menu.open();
        Locator body = browserPage.locator(".portal-user-menu-body:visible");
        Locator overlay = browserPage.locator("vaadin-popover-overlay[opened] > [part~='overlay']");
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
                    .setName(uiTexts.getBaseManageAccountButton())
                    .setExact(true));
        assertThat(account).isVisible();
        Locator home =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(uiTexts.getBaseHomeButton()).setExact(true));
        assertThat(home).hasCount(route.equals(HOME_ROUTE) ? 0 : 1);
        Locator admin =
            body.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getBaseManageApplicationButton())
                    .setExact(true));
        assertThat(admin).hasCount(route.equals(ADMIN_ROUTE) ? 0 : 1);
        BoundingBox bounds = overlay.boundingBox();
        assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= width + 1);
        assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= 901);
        BoundingBox footer = body.locator(".portal-menu-footer").boundingBox();
        BoundingBox logout = body.locator(".portal-menu-logout").boundingBox();
        assertEquals(footer.x + footer.width - 8, logout.x + logout.width, 1);
        AllurePlaywright.attachScreenshot(
            "Benutzermenü " + route + " bei " + width + "px", browserPage);
        menu.close();
        menu.open();
        browserPage.mouse().click(1, 880);
        assertThat(
                browserPage.locator(
                    "vaadin-popover-overlay[opened], vaadin-popover-overlay[closing]"))
            .hasCount(0);
        menu.open();
        account.click();
        assertThat(browserPage.locator("#account-panel-profile")).isVisible();
        assertThat(browserPage.locator("vaadin-popover-overlay[opened]")).hasCount(0);
      }
      assertPreferredLanguageLabels(menu);
    }
  }

  private void assertPreferredLanguageLabels(UserMenu menu) {
    Locator field = browserPage.locator("#account-language");
    Locator input = field.locator("input");
    Locale originalInterface = Locale.forLanguageTag(uiTexts.language());
    String otherLanguage = uiTexts.language().equals("de") ? "en" : "de";
    for (Locale selected : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      field.click();
      browserPage
          .getByRole(
              AriaRole.OPTION,
              new Page.GetByRoleOptions()
                  .setName(selected.getDisplayLanguage(originalInterface))
                  .setExact(true))
          .click();
      browserPage.locator("#account-first-name input").click();
      for (String language : List.of(otherLanguage, uiTexts.language())) {
        menu.selectLanguage(language);
        Locale currentInterface = Locale.forLanguageTag(language);
        String expected = selected.getDisplayLanguage(currentInterface);
        assertThat(input).hasValue(expected);
        field.click();
        for (Locale option : List.of(Locale.GERMAN, Locale.ENGLISH)) {
          assertThat(
                  browserPage.getByRole(
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
    AllurePlaywright.attachScreenshot("Vorausgewählte Sprache nach Sprachwechsel", browserPage);
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
    assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= browserPage.viewportSize().width + 1);
  }

  private void signIn() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
  }
}
