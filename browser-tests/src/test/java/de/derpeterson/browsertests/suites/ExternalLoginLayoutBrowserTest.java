package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Externe Anmeldung")
public class ExternalLoginLayoutBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @CsvSource({"1280", "390"})
  @DisplayName("Kontomenü und Sicherheitsreiter bleiben nach dem Neuladen ausgewählt")
  void accountNavigationSurvivesReload(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AccountPage account = new AccountPage(browserPage);
    account.open();
    for (String section : List.of("sessions", "profile")) {
      account.section("account-tab-" + section);

      assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=" + section + "$"));
      browserPage.reload();

      assertThat(account.host("account-panel-" + section)).isVisible();
    }

    account.section("account-tab-security");
    for (String tab : List.of("providers", "portal")) {
      Locator tabItem = account.host("account-security-tab-" + tab);
      tabItem.click();

      assertThat(browserPage)
          .hasURL(
              Pattern.compile(
                  ".*/account\\?(section=security&tab="
                      + tab
                      + "|tab="
                      + tab
                      + "&section=security)$"));
      browserPage.reload();

      assertThat(tabItem).hasAttribute("selected", "");
      assertThat(account.host("account-security-" + tab)).isVisible();
    }
    browserPage.navigate("/account?tab=providers&section=security");

    assertThat(account.host("account-security-providers")).isVisible();
  }

  @ParameterizedTest
  @CsvSource({"1280", "390"})
  @DisplayName(
      "OIDC-12: Anbieterzeilen und Kontobestätigung verwenden die bestehenden Dialogflächen")
  void contextualAccountDialogUsesCosmosLayout(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security&tab=portal");
    Locator securityTabs = browserPage.locator("#account-security-tabs");

    assertThat(securityTabs.getByRole(AriaRole.TAB)).hasCount(2);
    Locator portalTab = browserPage.locator("#account-security-tab-portal");
    Locator providersTab = browserPage.locator("#account-security-tab-providers");

    assertThat(portalTab).hasText(uiTexts.getAccountSecurityPortalAccess());
    assertThat(providersTab).hasText(uiTexts.getProviderSettingsPanelTitle());
    assertThat(portalTab).hasAttribute("selected", "");
    assertThat(browserPage.locator("#account-security-portal .account-security-hint")).isVisible();
    assertThat(browserPage.locator("#account-password-edit-security")).isVisible();
    // Auch nach einem vorherigen Anbieter-/Adminaufruf darf die zuletzt geladene
    // Kontogestaltung die Anbieteraktionen nicht auf die schmale Änderungsbreite begrenzen.
    browserPage.evaluate(
        """
        async () => {
          const href = [...document.styleSheets].map(sheet => sheet.href)
            .find(href => href?.includes('/cosmos-account.css'));
          if (!href) throw new Error('Account stylesheet not found');
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.href = href;
          const loaded = new Promise((resolve, reject) => {
            link.onload = resolve;
            link.onerror = reject;
          });
          document.head.append(link);
          await loaded;
        }
        """);
    attachPanels("Kontosicherheit: Portalzugang bei " + width + " px");
    portalTab.focus();
    portalTab.press("End");

    assertThat(providersTab).isFocused();
    providersTab.press("Space");

    assertThat(providersTab).hasAttribute("selected", "");
    assertThat(browserPage.locator("#account-security-portal .account-security-hint")).isHidden();
    assertThat(browserPage.locator("#account-password-edit-security")).isHidden();
    assertThat(browserPage.locator("#external-security-action")).hasCount(0);
    assertThat(browserPage.locator(".external-connection-row vaadin-badge")).hasCount(2);
    assertAccountProviderRows(portalTab, providersTab);
    attachPanels("Kontosicherheit: Anbieterübersicht bei " + width + " px");
    browserPage.locator("#external-connect-google").click();
    Locator dialog = browserPage.locator("#external-identity-dialog");
    assertProviderConfirmationDialog(dialog, width);

    browserPage.locator("#external-connect-google").click();
    browserPage.locator("#external-proof-help").click();

    assertThat(browserPage.locator("#account-support")).isVisible();
    browserPage.locator("#support-back").click();

    assertThat(browserPage.locator("#account-security-portal")).isVisible();
    assertThat(dialog.locator("vaadin-dialog-overlay")).isHidden();

    browserPage.navigate("/admin");
    new AdminUsersPage(browserPage).open();
    AdminUserDialog editor = new AdminUsersPage(browserPage).openCreate();
    assertDynamicTextWidths(editor.root().locator(".cosmos-dialog-actions"));
    editor.cancel();

    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-security-tab-providers").click();
    browserPage.locator("#external-connect-google").click();
    Locator otpInput = browserPage.locator("#external-confirm-factor input");

    assertThat(otpInput).isFocused();
    String code = TwoFactorPage.totp(browserPage, BrowserTestAccounts.ADMIN_EMAIL);
    if (width == 390) {
      browserContext.grantPermissions(List.of("clipboard-read", "clipboard-write"));
      browserPage.evaluate("code => navigator.clipboard.writeText(code)", code);
      browserPage.keyboard().press("ControlOrMeta+V");
    } else {
      otpInput.pressSequentially(code);
    }
    assertThat(browserPage.locator("#fixture-email")).isVisible();
  }

  private void assertProviderConfirmationDialog(Locator dialog, int width) {
    Locator content = dialog.locator("vaadin-dialog-overlay [part=content]");

    assertThat(content).hasCSS("background-color", "rgb(5, 8, 17)");
    Locator appCode = browserPage.locator("fc-otp-field#external-confirm-factor");

    assertThat(appCode.locator("[part=slot]")).hasCount(6);
    assertThat(appCode.locator("[part=slot]").first())
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    assertThat(appCode.locator("input[slot=input]")).isFocused();
    Locator recoveryTab = browserPage.locator("#external-proof-recovery-code-tab");
    UiActions.complete(browserPage, recoveryTab::click);

    assertThat(browserPage.locator("#external-confirm-recovery-code")).isVisible();
    assertThat(browserPage.locator("#external-confirm-recovery-code input"))
        .hasCSS("font-family", Pattern.compile(".*Space Mono.*"));
    assertThat(appCode).isHidden();
    UiActions.complete(browserPage, browserPage.locator("#external-proof-app-code-tab")::click);

    assertThat(appCode).isVisible();
    assertThat(dialog.locator(".account-change-dialog-header .cosmos-panel-icon img"))
        .hasAttribute("src", "icons/external-login/google.png");
    assertDynamicTextWidths(dialog.locator(".cosmos-dialog-actions"));
    attachPanels("Kontosicherheit: stabiler Bestätigungsdialog bei " + width + " px");
    BoundingBox bounds = dialog.locator("vaadin-dialog-overlay > [part~=overlay]").boundingBox();

    assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= width);
    browserPage.keyboard().press("Escape");

    assertThat(dialog).hasJSProperty("opened", false);
  }

  private void assertAccountProviderRows(Locator portalTab, Locator providersTab) {
    Locator connectionBadge = browserPage.locator(".external-connection-row vaadin-badge").first();

    assertThat(connectionBadge).hasCSS("border-radius", "4px");
    assertThat(connectionBadge).hasCSS("border-top-width", "1px");
    assertThat(connectionBadge).hasCSS("font-size", "10px");
    assertThat(connectionBadge).hasCSS("font-weight", "500");
    assertThat(connectionBadge).hasCSS("color", "rgb(56, 189, 248)");
    assertThat(browserPage.locator(".external-connection-row").first())
        .hasCSS("background-color", "rgb(9, 17, 32)");
    assertThat(portalTab.locator("img")).hasCSS("width", "20px");
    assertThat(providersTab.locator("vaadin-icon")).hasCSS("width", "20px");
    Locator google = browserPage.locator("#external-connect-google");
    Locator microsoft = browserPage.locator("#external-connect-microsoft");
    browserPage.waitForCondition(
        () -> Math.abs(google.boundingBox().width - microsoft.boundingBox().width) < 1);

    assertTrue(
        microsoft.boundingBox().width > 110,
        "Anbieteraktionen erhalten ihre eigene natürliche Breite statt der Änderungsbreite");
    for (Locator button : List.of(google, microsoft)) {
      assertTrue(
          (Boolean)
              button
                  .locator("[part=label]")
                  .evaluate("label => label.scrollWidth <= label.clientWidth + 1"),
          "Der gesamte Anbietertext bleibt sichtbar");
    }
    double bothWidth = google.boundingBox().width;
    Locator microsoftRow = microsoft.locator("xpath=..");
    microsoftRow.evaluate("row => row.style.display = 'none'");
    browserPage.waitForCondition(() -> google.boundingBox().width < bothWidth - 1);

    assertTrue(
        (Boolean)
            google
                .locator("[part=label]")
                .evaluate("label => label.scrollWidth <= label.clientWidth + 1"));
    microsoftRow.evaluate("row => row.style.removeProperty('display')");
    browserPage.waitForCondition(
        () -> Math.abs(google.boundingBox().width - microsoft.boundingBox().width) < 1);

    assertTrue(
        (Boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"));
  }

  private void assertDynamicTextWidths(Locator group) {
    group.evaluate(
        """
        async element => {
          const animations = new Set();
          for (let node = element; node; node = node.parentElement || node.getRootNode().host) {
            node.getAnimations?.().forEach(animation => animations.add(animation));
          }
          await Promise.all([...animations]
            .filter(animation => animation.effect?.getComputedTiming().endTime !== Infinity)
            .map(animation => animation.finished.catch(() => {})));
        }
        """);
    Locator buttons = group.locator("vaadin-button,button");
    browserPage.waitForCondition(() ->
        (Boolean)
            buttons
                .nth(0)
                .evaluate("button => button.style.getPropertyPriority('width') === 'important'"));
    browserPage.waitForCondition(() ->
        Math.abs(buttons.nth(0).boundingBox().width - buttons.nth(1).boundingBox().width) < 1);
    double original = buttons.nth(0).boundingBox().width;

    assertTrue(original < 200, "Textbuttons erhalten keine feste 200-px-Breite");
    group.evaluate(
        """
        root => {
          const extra = document.createElement('button');
          extra.dataset.widthProbe = 'text';
          extra.textContent = 'Synthetic action with a much longer caption';
          extra.hidden = true;
          const icon = document.createElement('button');
          icon.dataset.widthProbe = 'icon';
          icon.setAttribute('aria-label', 'Synthetic icon action');
          icon.style.width = '32px';
          root.append(extra, icon);
        }
        """);

    assertEquals(original, buttons.nth(0).boundingBox().width, 1);
    assertEquals(32, group.locator("[data-width-probe=icon]").boundingBox().width, 1);
    Locator extra = group.locator("[data-width-probe=text]");
    extra.evaluate("button => button.hidden = false");
    browserPage.waitForCondition(() -> buttons.nth(0).boundingBox().width > original + 20);
    extra.evaluate("button => button.style.display = 'none'");
    browserPage.waitForCondition(() -> Math.abs(buttons.nth(0).boundingBox().width - original) < 1);

    assertEquals(32, group.locator("[data-width-probe=icon]").boundingBox().width, 1);
    group
        .locator("[data-width-probe]")
        .evaluateAll("buttons => buttons.forEach(button => button.remove())");
  }

  @ParameterizedTest
  @CsvSource({"1280", "390"})
  @Tag("configuration-field-help")
  @DisplayName("OIDC-10: Native Anbietertabs erhalten ungespeicherte Eingaben")
  void providerTabsKeepIndependentDrafts(int width) {
    browserPage.setViewportSize(width, 1000);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/admin?section=configuration&group=external_login");
    Locator section = browserPage.locator("#admin-provider-settings");
    Locator tabs = section.locator("vaadin-tabsheet");
    assertProviderPanelHeader(section, tabs);
    for (String provider : new String[] {"google", "microsoft"}) {
      assertNativeProviderSetup(provider);
    }
    browserPage.locator("#provider-tab-google").click();
    Locator google = browserPage.locator("#provider-google-content");
    Locator microsoft = browserPage.locator("#provider-microsoft-content");

    assertThat(google).isVisible();
    assertThat(microsoft).isHidden();
    attachPanels("Anmeldeanbieter: Google-Tab bei " + width + " px");
    Locator clientId = browserPage.locator("#provider-google-client-id input");
    clientId.fill("unsaved-google-example");
    browserPage.locator("#provider-tab-microsoft").click();

    assertThat(google).isHidden();
    assertThat(microsoft).isVisible();
    assertThat(browserPage.locator("#provider-tab-microsoft"))
        .hasAttribute("aria-selected", "true");
    assertProviderTabAppearance(browserPage.locator("#provider-tab-microsoft"));
    Locator microsoftId = browserPage.locator("#provider-microsoft-client-id input");
    microsoftId.fill("unsaved-microsoft-example");
    browserPage.locator("#provider-tab-google").click();

    assertThat(clientId).hasValue("unsaved-google-example");
    browserPage.locator("#provider-tab-microsoft").click();

    assertThat(microsoftId).hasValue("unsaved-microsoft-example");
    assertTrue(
        (Boolean)
            tabs.locator("vaadin-tabsheet-scroller[part=content]")
                .evaluate(
                    "e => e.scrollHeight <= e.clientHeight + 1 && e.scrollWidth <= e.clientWidth + 1"),
        "Der Tabinhalt wächst ohne eigenen Scrollbereich");
    assertTrue(
        (Boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"),
        "Anbietertabs erzeugen keinen horizontalen Seitenüberlauf");

    browserPage.navigate("/admin?section=mail_test");
    Locator smtp = browserPage.locator("a.admin-smtp-link");

    assertThat(smtp).hasText(uiTexts.getAdminConfigGroupSmtp());
    assertLinkAlignmentAndStableHover(smtp);
    attachPanels("SMTP-Direktzugang Vaadin bei " + width + " px");
    String settingsUrl =
        URI.create(browserPage.url()).resolve(smtp.getAttribute("href")).toString();
    smtp.click();

    assertThat(browserPage).hasURL(settingsUrl);
  }

  private void assertNativeProviderSetup(String provider) {
    Locator badge = browserPage.locator("#provider-" + provider + "-status");

    assertThat(badge).hasAttribute("data-state", "active");
    assertThat(badge).hasText(uiTexts.getProviderSettingsActive());
    assertThat(badge).hasCSS("height", "30px");
    assertThat(badge.locator("img")).hasCount(1);
    browserPage.keyboard().press("Tab");
    badge.focus();

    assertThat(browserPage.getByRole(AriaRole.TOOLTIP))
        .containsText(
            uiTexts.getProviderSettingsActiveHint(
                provider.equals("google") ? "Google" : "Microsoft"));
    browserPage.keyboard().press("Escape");
    browserPage.locator("#provider-tab-" + provider).click();
    Locator assistant = browserPage.locator("#provider-" + provider + "-content");

    assertThat(assistant.locator(".provider-assistant-kicker"))
        .hasText(uiTexts.getProviderSettingsAssistantKicker());
    assertThat(assistant.locator(".provider-assistant-kicker vaadin-icon"))
        .hasAttribute("data-lucide", "bot");
    assertThat(assistant.locator(".provider-step-label")).hasCount(3);
    assertThat(assistant.locator(".provider-step-label").first())
        .hasText(uiTexts.getProviderSettingsSetupProvider());
    assertThat(assistant.locator(".provider-step-label").nth(1))
        .hasText(uiTexts.getProviderSettingsSetupCredentials());
    assertThat(assistant.locator(".provider-step-label").nth(2))
        .hasText(uiTexts.getProviderSettingsEnableSignIn());
    for (int step = 0; step < 3; step++) {
      assertThat(assistant.locator(".provider-step-label vaadin-icon").nth(step))
          .hasAttribute("data-tabler", "square-rounded-number-" + (step + 1));
    }
    assertThat(assistant.locator(".provider-field-label.admin-config-required")).hasCount(2);
    Locator consoleLink = assistant.locator("a.provider-console-link");

    assertThat(consoleLink).hasText(uiTexts.getProviderSettingsOpenConsole());
    assertThat(consoleLink).hasAttribute("target", "_blank");
    assertThat(consoleLink).hasAttribute("rel", "noopener noreferrer");
    assertThat(consoleLink).hasCSS("border-top-width", "0px");
    assertThat(consoleLink).hasCSS("font-weight", "600");
    assertThat(consoleLink).hasCSS("color", "rgb(226, 232, 240)");
    assertThat(consoleLink.locator("vaadin-icon")).hasAttribute("data-tabler", "external-link");
    assertThat(consoleLink.locator("vaadin-icon")).hasCSS("color", "rgb(56, 189, 248)");
    assertThat(assistant.locator(".provider-console-guide")).hasCSS("row-gap", "4px");
    assertThat(assistant.locator(".provider-redirect label"))
        .hasText(uiTexts.getProviderSettingsRedirect());
    assertThat(assistant.locator(".provider-copy vaadin-icon"))
        .hasAttribute("data-lucide", "clipboard-copy");
    assertThat(assistant.getByRole(AriaRole.SWITCH)).hasCount(1);
    assertProviderControlGeometry(
        assistant.locator(".provider-redirect [part=input-field]"),
        assistant.locator(".provider-copy"),
        assistant.locator(".provider-help-toggle"),
        assistant.locator(".provider-activation"),
        assistant.locator(".provider-credentials > div").first());
    assertLinkAlignmentAndStableHover(consoleLink);
    assertProviderFieldHelp(provider);
  }

  private void assertProviderControlGeometry(
      Locator input,
      Locator copy,
      Locator activationRow,
      Locator activationControl,
      Locator credentialField) {
    assertEquals(
        input.boundingBox().height,
        copy.boundingBox().height,
        1,
        "Kopierbutton und Eingabefläche haben dieselbe Höhe");
    assertEquals(
        credentialField.boundingBox().width,
        activationRow.boundingBox().width,
        1,
        "Die Aktivierung nutzt genau eine Formularspalte, mobil die volle Breite");
    assertThat(activationControl).hasCSS("border-top-width", "0px");
  }

  private void assertProviderPanelHeader(Locator section, Locator tabs) {
    assertThat(tabs).hasCount(1);
    assertThat(tabs.getByRole(AriaRole.TAB)).hasCount(2);
    assertThat(section.locator(".provider-panel-header vaadin-icon"))
        .hasAttribute("data-tabler", "cloud-lock");
    assertThat(browserPage.locator("vaadin-side-nav-item vaadin-icon[data-tabler=cloud-lock]"))
        .hasCount(1);
    assertThat(tabs).hasCSS("border-top-width", "0px");
    assertThat(tabs).hasCSS("border-bottom-width", "0px");
    assertThat(tabs.locator("vaadin-tabs")).hasCSS("border-bottom-width", "0px");
    assertThat(tabs.locator("[part=tabs-container]")).hasCSS("border-bottom-width", "0px");
    assertThat(section.locator(".provider-panel-header")).hasCSS("border-bottom-width", "0px");
    assertTrue(
        tabs.locator("[part=tabs-container]")
            .evaluate("e => getComputedStyle(e).backgroundImage")
            .toString()
            .contains("linear-gradient"),
        "Die Trennlinie bleibt außerhalb des aktiven Tabs erhalten");
    assertThat(tabs.locator("[part=tabs-container]")).hasCSS("box-shadow", "none");
    assertEquals(
        "none",
        browserPage
            .locator("#provider-tab-google")
            .evaluate("e => getComputedStyle(e, '::before').display"));
    assertProviderTabAppearance(browserPage.locator("#provider-tab-google"));

    assertThat(section.locator(".provider-panel-header h2"))
        .hasText(uiTexts.getProviderSettingsPanelTitle());
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasText(uiTexts.getProviderSettingsPanelDescription());
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasCSS("white-space", "pre-line");
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasCSS("line-height", "20px");
    assertThat(section.locator(".provider-panel-header vaadin-badge")).hasCount(2);
  }

  private void assertProviderTabAppearance(Locator tab) {
    assertThat(tab).hasCSS("border-top-width", "2px");
    assertThat(tab).hasCSS("border-top-color", "rgb(56, 189, 248)");
    assertThat(tab).hasCSS("border-top-left-radius", "10px");
    assertThat(tab).hasCSS("border-bottom-width", "0px");
    assertThat(tab).hasCSS("font-weight", "600");
  }

  private void assertLinkAlignmentAndStableHover(Locator link) {
    link.scrollIntoViewIfNeeded();
    List<?> geometry =
        (List<?>)
            link.evaluate(
                """
                element => {
                  const icon = element.querySelector('vaadin-icon, svg');
                  const caption = element.querySelector('span');
                  const range = document.createRange();
                  if (caption) {
                    range.selectNodeContents(caption);
                  } else {
                    range.selectNode([...element.childNodes].find(node => node.nodeType === Node.TEXT_NODE && node.textContent.trim()));
                  }
                  const text = range.getBoundingClientRect();
                  const symbol = icon.getBoundingClientRect();
                  const box = element.getBoundingClientRect();
                  return [
                    Math.abs((symbol.top + symbol.bottom - text.top - text.bottom) / 2),
                    text.left - symbol.right,
                    Math.abs((symbol.left + text.right - box.left - box.right) / 2)
                  ];
                }
                """);

    assertTrue(
        ((Number) geometry.get(0)).doubleValue() <= 1.5,
        "Icon und sichtbarer Text stehen vertikal mittig: " + geometry);
    assertEquals(8, ((Number) geometry.get(1)).doubleValue(), 1, "Einheitlicher Iconabstand");
    assertTrue(
        ((Number) geometry.get(2)).doubleValue() <= 1,
        "Icon und Text sind als Gruppe in der Klickfläche zentriert: " + geometry);
    String color = link.evaluate("element => getComputedStyle(element).color").toString();
    Locator symbol = link.locator(":scope > vaadin-icon, :scope > svg");
    String iconColor = symbol.evaluate("element => getComputedStyle(element).color").toString();
    link.hover();

    assertThat(link).hasCSS("background-color", "rgba(0, 0, 0, 0)");
    assertThat(link).hasCSS("color", color);
    assertThat(symbol).hasCSS("color", iconColor);
    browserPage.mouse().move(1, 1);
  }

  private void assertProviderFieldHelp(String provider) {
    for (String field : new String[] {"client_id", "secret", "enabled"}) {
      Locator info = browserPage.locator("#provider-" + provider + "-" + field + "-info");
      if (!field.equals("enabled")) {
        Locator input =
            browserPage.locator(
                "#provider-"
                    + provider
                    + "-"
                    + (field.equals("client_id") ? "client-id" : field)
                    + " [part=input-field]");
        info.scrollIntoViewIfNeeded();
        BoundingBox inputBounds = input.boundingBox();
        BoundingBox infoBounds = info.boundingBox();

        assertTrue(
            Math.abs(inputBounds.y + inputBounds.height / 2 - infoBounds.y - infoBounds.height / 2)
                < 2,
            "Infoicon ist mittig zum Eingabefeld: Feld "
                + (inputBounds.y + inputBounds.height / 2)
                + ", Info "
                + (infoBounds.y + infoBounds.height / 2));
      }
      info.click();
      Locator help = browserPage.locator("#provider-field-help");

      assertThat(help.locator("vaadin-dialog-overlay > [part~=overlay]")).isVisible();
      String explanation =
          switch (field) {
            case "client_id" ->
                uiTexts.getProviderSettingsClientIdHelp(provider.equals("microsoft"));
            case "secret" -> uiTexts.getProviderSettingsSecretHelp(provider.equals("microsoft"));
            default -> uiTexts.getProviderSettingsEnableHelp();
          };

      assertThat(help.locator(".admin-help-description")).hasText(explanation);
      String timing =
          switch (field) {
            case "client_id" -> uiTexts.getAdminConfigEffectiveProviderClient();
            case "secret" -> uiTexts.getAdminConfigEffectiveProviderSecret();
            default -> uiTexts.getAdminConfigEffectiveProviderEnabled();
          };
      AdminConfigurationPage.assertEffectiveUse(
          help, uiTexts, timing, uiTexts.getAdminConfigEffectiveProviderExisting());
      help.locator("[slot=footer] vaadin-button").click();
    }
    Locator enabled =
        browserPage.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.SWITCH);
    Locator client = browserPage.locator("#provider-" + provider + "-client-id input");
    String originalClient = client.inputValue();
    enabled.uncheck();
    client.fill(" ");

    assertThat(enabled).isDisabled();
    client.fill(originalClient);

    assertThat(enabled).isEnabled();
    assertThat(browserPage.locator("#provider-" + provider + "-status"))
        .hasAttribute("data-state", "active");
    enabled.check();
    client.fill("");

    assertThat(enabled).isEnabled();
    enabled.uncheck();

    assertThat(enabled).isDisabled();
    client.fill(originalClient);
    enabled.check();
  }

  @ParameterizedTest
  @CsvSource({"login,1280", "login,390", "register,1280", "register,390"})
  @DisplayName("OIDC-09: Anmeldung und Registrierung behalten getrennte responsive Panels")
  void separateSignInPanels(String route, int width) {
    boolean registration = route.equals("register");
    browserPage.setViewportSize(width, 1000);
    browserPage.navigate(path(registration ? REGISTRATION_ROUTE : LOGIN_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    Locator local = browserPage.locator(".public-auth-panels > .public-panel");
    Locator external = browserPage.locator(".public-auth-panels > .external-login-panel");

    assertThat(local.locator(".external-login-buttons")).hasCount(0);
    assertThat(external.locator("h2")).hasText(uiTexts.getExternalPanelTitle());
    assertThat(external.locator(".external-login-hint")).hasText(uiTexts.getExternalPanelHint());
    assertThat(external.locator("#external-login-google"))
        .hasText(uiTexts.getExternalContinueGoogle());
    assertThat(external.locator("#external-login-microsoft"))
        .hasText(uiTexts.getExternalContinueMicrosoft());
    assertThat(external.locator(".external-login-alternative")).hasCount(0);
    assertThat(browserPage.locator(".public-auth-panels > .external-login-divider"))
        .hasText(uiTexts.getExternalOr());
    assertThat(local.locator(".public-panel-footer")).isVisible();
    assertNotEquals(
        local.locator(".public-panel-body").evaluate("el => getComputedStyle(el).backgroundColor"),
        local
            .locator(".cosmos-panel-header")
            .evaluate("el => getComputedStyle(el).backgroundColor"));
    assertPlacement(local, external, width, registration);
    assertProviderButtonsFlow(external, false);

    assertThat(browserPage.locator(".cosmos-footer")).hasCSS("text-align", "center");
    Number contentWidth = (Number) browserPage.evaluate("document.documentElement.clientWidth");

    assertEquals(
        contentWidth.doubleValue(),
        browserPage.locator(".cosmos-footer").boundingBox().width,
        1,
        "Copyrightzeile erstreckt sich über den gemeinsamen Seitenrahmen");
    if (registration) {
      assertRegistrationFields(local, width);
    } else {
      assertRememberSwitch(local);
    }
    attachPanels(route + ": Zwei Panels in Vaadin");
    if (width == 390) {
      assertResponsiveActions(local, registration);
    }
  }

  private void assertRememberSwitch(Locator panel) {
    Locator remember = panel.getByRole(AriaRole.SWITCH);

    assertThat(remember).hasCount(1);
    assertThat(remember).not().isChecked();
    remember.check();

    assertThat(remember).isChecked();
    browserPage.reload();

    assertThat(remember).isChecked();
    remember.focus();
    remember.press("Space");

    assertThat(remember).not().isChecked();
    browserPage.reload();

    assertThat(remember).not().isChecked();
    assertTrue(
        (Boolean)
            remember.evaluate(
                """
                el => {
                  const track = el.closest('vaadin-switch')?.shadowRoot.querySelector('[part="switch"]') || el;
                  const r = track.getBoundingClientRect();
                  return Math.abs(r.width - 34) < 1 && Math.abs(r.height - 20) < 1;
                }
                """),
        "Schalterbahn hat die vereinbarten kompakten Abmessungen");
  }

  private void attachPanels(String title) {
    Allure.attachment(
        title,
        "image/png",
        new ByteArrayInputStream(
            browserPage.screenshot(
                new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }

  private void assertRegistrationFields(Locator panel, int width) {
    Locator names = panel.locator("vaadin-text-field, #register-firstname, #register-lastname");

    assertThat(names).hasCount(2);
    assertThat(panel.locator("vaadin-email-field, .vp-field input[type=email]")).hasCount(2);
    assertThat(panel.locator("vaadin-password-field, .vp-field input[type=password]")).hasCount(2);
    BoundingBox first = names.nth(0).boundingBox();
    BoundingBox last = names.nth(1).boundingBox();
    if (width >= 640) {
      assertTrue(Math.abs(first.y - last.y) < 1, "Registrierungsformular behält zwei Feldspalten");
      assertTrue(last.x >= first.x + first.width + 15, "Namensfelder bleiben voneinander getrennt");
    } else {
      assertTrue(
          last.y >= first.y + first.height, "Registrierungsfelder stehen mobil untereinander");
    }
  }

  private void assertResponsiveActions(Locator panel, boolean registration) {
    Locator footer = panel.locator(".public-panel-footer, .vp-card-foot");
    Locator question = footer.locator(".cosmos-intro > span, .vp-account-switch > span");
    Locator link = footer.locator(".cosmos-intro > a, .vp-account-switch > a");
    Locator submit = footer.locator("vaadin-button, button[data-form-submit]");
    Locator external = panel.locator("..").locator(".external-login-panel");
    for (int width : new int[] {1100, 600, 390, 280}) {
      browserPage.setViewportSize(width, 1000);
      assertPlacement(panel, external, width, registration);
      assertProviderButtonsFlow(external, width == 600 || registration && width == 1100);
      BoundingBox label = question.boundingBox();
      BoundingBox register = link.boundingBox();
      BoundingBox button = submit.boundingBox();

      assertTrue(
          register.x >= label.x + label.width + 6 || register.y >= label.y + label.height - 1,
          "Formularlink steht neben oder unter dem vollständigen Kontohinweis");
      if (width == 600) {
        assertTrue(
            Math.abs(register.y - label.y) < 1, "Bei Platz bleiben Hinweis und Link nebeneinander");
        assertTrue(
            button.x >= register.x + register.width,
            "Primäraktion bleibt rechts in derselben Zeile");
      }
      if (width == 280) {
        assertTrue(
            button.y >= register.y + register.height,
            "Bei sehr wenig Platz steht die Primäraktion darunter");
      }
      assertTrue(
          (Boolean)
              browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
          "Footer bleibt ohne horizontalen Überlauf erreichbar");
      attachPanels("Formularaktionen und Schnellzugang bei " + width + " px");
    }
  }

  private void assertProviderButtonsFlow(Locator external, boolean sideBySide) {
    Locator buttons = external.locator(".external-provider-button");

    assertThat(buttons).hasCount(2);
    BoundingBox google = buttons.nth(0).boundingBox();
    BoundingBox microsoft = buttons.nth(1).boundingBox();

    assertEquals(google.width, microsoft.width, 1, "Anbieterbuttons behalten gleiche Breite");
    if (sideBySide) {
      assertTrue(
          Math.abs(google.y - microsoft.y) < 1, "Bei Platz stehen Anbieterbuttons nebeneinander");
      assertTrue(microsoft.x >= google.x + google.width + 7, "Abstand zwischen Anbieterbuttons");
    } else {
      assertTrue(
          microsoft.y >= google.y + google.height + 7,
          "Bei wenig Platz stehen Anbieterbuttons untereinander");
      assertTrue(
          Math.abs(google.x - microsoft.x) < 1, "Gestapelte Anbieterbuttons sind links bündig");
    }
  }

  private void assertPlacement(Locator local, Locator external, int width, boolean registration) {
    assertThat(local).isVisible();
    assertThat(external).isVisible();
    Locator signIn =
        local.locator(".public-panel-footer vaadin-button, .vp-card-foot button[data-form-submit]");
    String typography =
        "el => { const s = getComputedStyle(el); return [s.fontSize, s.fontWeight, s.fontFamily]; }";
    for (Locator button : external.locator(".external-provider-button").all()) {
      assertEquals(40, button.boundingBox().height, 0.5, "Gleiche Anbieterbuttonhöhe");
      assertEquals(signIn.evaluate(typography), button.evaluate(typography));
      BoundingBox mark = button.locator("img").boundingBox();

      assertEquals(16, mark.width, 0.5, "Anbieterlogo entspricht der normalen Iconbreite");
      assertEquals(16, mark.height, 0.5, "Anbieterlogo entspricht der normalen Iconhöhe");
    }
    BoundingBox form = local.boundingBox();
    BoundingBox providers = external.boundingBox();
    Locator divider = local.locator("..").locator(".external-login-divider");

    assertThat(divider).isVisible();
    BoundingBox separator = divider.boundingBox();

    assertEquals(
        width >= (registration ? 1200 : 1024) ? "column" : "row",
        divider.evaluate("el => getComputedStyle(el).flexDirection"),
        "Trennlinie folgt der Anordnung der Panels");
    if (width >= (registration ? 1200 : 1024)) {
      assertTrue(providers.x >= form.x + form.width + 23, "Sichtbarer Abstand zwischen den Panels");
      assertTrue(
          Math.abs(providers.y + providers.height / 2 - form.y - form.height / 2) < 1,
          "Schnellzugang ist vertikal zum lokalen Panel zentriert");
      assertTrue(
          separator.x >= form.x + form.width && separator.x + separator.width <= providers.x,
          "Trenner bleibt zwischen den getrennten Panels");
    } else {
      assertTrue(
          providers.y >= form.y + form.height + 23, "Schnellzugang steht unter dem Formular");
      assertTrue(Math.abs(providers.x - form.x) < 1, "Gestapelte Panels beginnen links bündig");
      assertTrue(
          separator.y >= form.y + form.height && separator.y + separator.height <= providers.y,
          "Trenner steht zwischen den gestapelten Panels");
    }
    assertEquals(
        // Im HTML ist der Kopf transparent und zeigt die Panelfläche des Elternartikels.
        local.evaluate(
            "el => getComputedStyle(el.querySelector('.cosmos-panel-header') || el).backgroundColor"),
        external.evaluate("el => getComputedStyle(el).backgroundColor"),
        "Schnellzugang und Panelkopf verwenden dieselbe Panelfläche");
    assertTrue(
        (Boolean)
            browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
        "Kein horizontaler Seitenüberlauf");
  }
}
