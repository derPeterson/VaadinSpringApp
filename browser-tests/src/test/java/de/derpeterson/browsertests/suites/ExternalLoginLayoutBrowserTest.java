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
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Externe Anmeldung")
public class ExternalLoginLayoutBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @CsvSource({"1280", "390"})
  @DisplayName("OIDC-10: Native Anbietertabs erhalten ungespeicherte Eingaben")
  void providerTabsKeepIndependentDrafts(int width) {
    page.setViewportSize(width, 1000);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/admin?section=configuration&group=external_login");
    Locator section = page.locator("#admin-provider-settings");
    Locator tabs = section.locator("vaadin-tabsheet");
    assertThat(tabs).hasCount(1);
    assertThat(tabs.getByRole(AriaRole.TAB)).hasCount(2);
    assertThat(section.locator(".provider-panel-header vaadin-icon"))
        .hasAttribute("data-tabler", "cloud-lock");
    assertThat(page.locator("vaadin-side-nav-item vaadin-icon[data-tabler=cloud-lock]"))
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
        page.locator("#provider-tab-google")
            .evaluate("e => getComputedStyle(e, '::before').display"));
    assertProviderTabAppearance(page.locator("#provider-tab-google"));
    assertThat(section.locator(".provider-panel-header h2"))
        .hasText(texts.getProviderSettingsPanelTitle());
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasText(texts.getProviderSettingsPanelDescription());
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasCSS("white-space", "pre-line");
    assertThat(section.locator(".provider-panel-header .cosmos-panel-description"))
        .hasCSS("line-height", "20px");
    assertThat(section.locator(".provider-panel-header vaadin-badge")).hasCount(2);
    for (String provider : new String[] {"google", "microsoft"}) {
      Locator badge = page.locator("#provider-" + provider + "-status");
      assertThat(badge).hasAttribute("data-state", "active");
      assertThat(badge).hasText(texts.getProviderSettingsActive());
      assertThat(badge).hasCSS("height", "30px");
      assertThat(badge.locator("img")).hasCount(1);
      page.keyboard().press("Tab");
      badge.focus();
      assertThat(page.getByRole(AriaRole.TOOLTIP))
          .containsText(
              texts.getProviderSettingsActiveHint(
                  provider.equals("google") ? "Google" : "Microsoft"));
      page.keyboard().press("Escape");
      page.locator("#provider-tab-" + provider).click();
      Locator assistant = page.locator("#provider-" + provider + "-content");
      assertThat(assistant.locator(".provider-assistant-kicker"))
          .hasText(texts.getProviderSettingsAssistantKicker());
      assertThat(assistant.locator(".provider-assistant-kicker vaadin-icon"))
          .hasAttribute("data-lucide", "bot");
      assertThat(assistant.locator(".provider-step-label")).hasCount(3);
      assertThat(assistant.locator(".provider-step-label").first())
          .hasText(texts.getProviderSettingsSetupProvider());
      assertThat(assistant.locator(".provider-step-label").nth(1))
          .hasText(texts.getProviderSettingsSetupCredentials());
      assertThat(assistant.locator(".provider-step-label").nth(2))
          .hasText(texts.getProviderSettingsEnableSignIn());
      for (int step = 0; step < 3; step++) {
        assertThat(assistant.locator(".provider-step-label vaadin-icon").nth(step))
            .hasAttribute("data-tabler", "square-rounded-number-" + (step + 1));
      }
      assertThat(assistant.locator(".provider-field-label.admin-config-required")).hasCount(2);
      Locator consoleLink = assistant.locator("a.provider-console-link");
      assertThat(consoleLink).hasText(texts.getProviderSettingsOpenConsole());
      assertThat(consoleLink).hasAttribute("target", "_blank");
      assertThat(consoleLink).hasAttribute("rel", "noopener noreferrer");
      assertThat(consoleLink).hasCSS("border-top-width", "0px");
      assertThat(consoleLink).hasCSS("font-weight", "600");
      assertThat(consoleLink).hasCSS("color", "rgb(226, 232, 240)");
      assertThat(consoleLink.locator("vaadin-icon")).hasAttribute("data-tabler", "external-link");
      assertThat(consoleLink.locator("vaadin-icon")).hasCSS("color", "rgb(56, 189, 248)");
      assertThat(assistant.locator(".provider-console-guide")).hasCSS("row-gap", "4px");
      assertLinkAlignmentAndStableHover(consoleLink);
      assertProviderFieldHelp(provider);
    }
    page.locator("#provider-tab-google").click();
    Locator google = page.locator("#provider-google-content");
    Locator microsoft = page.locator("#provider-microsoft-content");
    assertThat(google).isVisible();
    assertThat(microsoft).isHidden();
    attachPanels("Anmeldeanbieter: Google-Tab bei " + width + " px");
    Locator clientId = page.locator("#provider-google-client-id input");
    clientId.fill("unsaved-google-example");
    page.locator("#provider-tab-microsoft").click();
    assertThat(google).isHidden();
    assertThat(microsoft).isVisible();
    assertThat(page.locator("#provider-tab-microsoft")).hasAttribute("aria-selected", "true");
    assertProviderTabAppearance(page.locator("#provider-tab-microsoft"));
    Locator microsoftId = page.locator("#provider-microsoft-client-id input");
    microsoftId.fill("unsaved-microsoft-example");
    page.locator("#provider-tab-google").click();
    assertThat(clientId).hasValue("unsaved-google-example");
    page.locator("#provider-tab-microsoft").click();
    assertThat(microsoftId).hasValue("unsaved-microsoft-example");
    assertTrue(
        (Boolean)
            tabs.locator("vaadin-tabsheet-scroller[part=content]")
                .evaluate(
                    "e => e.scrollHeight <= e.clientHeight + 1 && e.scrollWidth <= e.clientWidth + 1"),
        "Der Tabinhalt wächst ohne eigenen Scrollbereich");
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth"),
        "Anbietertabs erzeugen keinen horizontalen Seitenüberlauf");

    page.navigate(
        Path.of("docs/design/frontend-referenzen/admin.html").toAbsolutePath().toUri().toString());
    if (width == 390) {
      page.locator("#mobileMenuBtn").click();
    }
    page.locator("[data-admin-route=settings]:visible").first().click();
    if (!page.locator("[data-ad-group=EXTERNAL_LOGIN]:visible").first().isVisible()) {
      page.locator("#mobileMenuBtn").click();
    }
    page.locator("[data-ad-group=EXTERNAL_LOGIN]:visible").first().click();
    Locator referenceTabs = page.locator(".ad-provider-tabs");
    assertThat(referenceTabs.getByRole(AriaRole.TAB)).hasCount(2);
    assertThat(page.locator(".ad-provider-panel-header .ad-tabler-icon")).isVisible();
    assertThat(page.locator("[data-ad-group=EXTERNAL_LOGIN] .ad-tabler-icon")).hasCount(1);
    assertThat(referenceTabs).hasCSS("border-bottom-width", "0px");
    assertThat(page.locator(".ad-provider-panel-header")).hasCSS("border-bottom-width", "0px");
    assertThat(page.locator(".ad-provider-panel-header h2")).hasText("Anmeldeanbieter");
    assertThat(page.locator(".ad-provider-panel-header p br")).hasCount(1);
    assertThat(page.locator(".ad-provider-status")).hasCount(2);
    for (String provider : new String[] {"google", "microsoft"}) {
      Locator badge =
          page.locator(
              ".ad-provider-status[data-state="
                  + (provider.equals("google") ? "active" : "inactive")
                  + "]");
      assertThat(badge).hasCSS("height", "30px");
      badge.scrollIntoViewIfNeeded();
      badge.evaluate(
          "e => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
      badge.evaluate("e => e.focus({preventScroll:true})");
      assertThat(page.locator("#reference-tooltip")).isVisible();
      assertThat(page.locator("#reference-tooltip"))
          .containsText(
              provider.equals("google")
                  ? "Google ist für neue Anmeldungen freigegeben."
                  : "Microsoft ist für neue Anmeldungen deaktiviert.");
      page.keyboard().press("Escape");
      page.locator("#ad-provider-tab-" + provider).click();
      assertThat(page.locator("#ad-provider-" + provider + " .ad-provider-assistant-kicker"))
          .hasText("Einrichtungsassistent");
      assertThat(page.locator("#ad-provider-" + provider + " .ad-provider-step-label")).hasCount(3);
      assertThat(page.locator("#ad-provider-" + provider + " .ad-provider-step-label"))
          .hasText(
              new String[] {
                "Anbieter einrichten", "Zugangsdaten hinterlegen", "Anmeldung aktivieren"
              });
      assertThat(page.locator("#ad-provider-" + provider + " .ad-provider-step-label svg"))
          .hasCount(3);
      assertThat(page.locator("#ad-provider-" + provider + " label .ad-required")).hasCount(2);
      Locator referenceConsoleLink =
          page.locator("#ad-provider-" + provider + " a.ad-provider-console-link");
      assertThat(referenceConsoleLink).hasCSS("border-top-width", "0px");
      assertThat(referenceConsoleLink).hasCSS("font-weight", "600");
      assertThat(referenceConsoleLink).hasCSS("color", "rgb(226, 232, 240)");
      assertThat(referenceConsoleLink.locator("svg")).hasCSS("color", "rgb(56, 189, 248)");
      assertThat(page.locator("#ad-provider-" + provider + " .ad-provider-console-guide"))
          .hasCSS("row-gap", "4px");
      assertLinkAlignmentAndStableHover(referenceConsoleLink);
      for (String field : new String[] {"client", "secret", "enabled"}) {
        Locator wrapper =
            page.locator("#ad-provider-" + provider + "-" + field)
                .locator(
                    "xpath=ancestor::div[contains(concat(' ',normalize-space(@class),' '),' hc-field ')][1]");
        wrapper.locator(".hc-trigger").click();
        Locator help = page.locator("#configurationHelp");
        assertThat(help).isVisible();
        assertThat(help.locator(".hc-group"))
            .hasText(provider.equals("google") ? "Google" : "Microsoft");
        assertThat(help.locator(".hc-rules li")).hasCount(1);
        help.locator("[data-hc-close]").click();
      }
      Locator referenceEnabled = page.locator("#ad-provider-" + provider + "-enabled");
      Locator referenceClientInput = page.locator("#ad-provider-" + provider + "-client");
      String initialClient = referenceClientInput.inputValue();
      boolean initialEnabled = referenceEnabled.isChecked();
      referenceEnabled.uncheck();
      referenceClientInput.fill("");
      assertThat(referenceEnabled).isDisabled();
      referenceClientInput.fill(initialClient);
      assertThat(referenceEnabled).isEnabled();
      referenceEnabled.setChecked(initialEnabled);
    }
    page.locator("#ad-provider-tab-google").click();
    BoundingBox referenceClient = page.locator("#ad-provider-google-client").boundingBox();
    BoundingBox referenceSecret = page.locator("#ad-provider-google-secret").boundingBox();
    if (width >= 600) {
      assertEquals(
          referenceClient.y,
          referenceSecret.y,
          1,
          "Die Eingaben stehen auch mit Feldhilfe auf gleicher Höhe");
    } else {
      assertTrue(
          referenceSecret.y > referenceClient.y + referenceClient.height,
          "Mobil stehen die Eingaben untereinander");
    }
    BoundingBox referenceCredentials =
        page.locator("#ad-provider-google .ad-provider-credentials").boundingBox();
    BoundingBox referenceActivation =
        page.locator("#ad-provider-google .hc-toggle-control").boundingBox();
    assertTrue(
        referenceActivation.width >= referenceCredentials.width * .85,
        "Die Freigabezeile nutzt die volle verfügbare Formularbreite");
    assertProviderTabAppearance(page.locator("#ad-provider-tab-google"));
    assertThat(page.locator("#ad-provider-google")).isVisible();
    assertThat(page.locator("#ad-provider-microsoft")).isHidden();
    attachPanels("Anmeldeanbieter: Google-Tab HTML-Referenz bei " + width + " px");
    page.locator("#ad-provider-google-client").fill("unsaved-reference-example");
    page.locator("#ad-provider-tab-microsoft").click();
    assertProviderTabAppearance(page.locator("#ad-provider-tab-microsoft"));
    assertThat(page.locator("#ad-provider-microsoft")).isVisible();
    referenceTabs
        .getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Microsoft"))
        .press("Home");
    assertThat(page.locator("#ad-provider-tab-google")).isFocused();
    assertThat(page.locator("#ad-provider-google-client")).hasValue("unsaved-reference-example");
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth"),
        "HTML-Anbietertabs erzeugen keinen horizontalen Seitenüberlauf");

    Locator referenceMail = page.locator("[data-admin-route=mail]").first();
    if (!referenceMail.isVisible()) {
      page.locator("#mobileMenuBtn").click();
    }
    referenceMail.click();
    Locator referenceSmtp = page.locator(".ad-mail-settings-link");
    assertThat(referenceSmtp).hasText("SMTP Einstellungen");
    assertLinkAlignmentAndStableHover(referenceSmtp);
    attachPanels("SMTP-Direktzugang HTML bei " + width + " px");
    referenceSmtp.click();
    assertThat(page.locator("#admin-proposal .ad-config")).isVisible();

    page.navigate("/admin?section=mail_test");
    Locator smtp = page.locator("a.admin-smtp-link");
    assertThat(smtp).hasText(texts.getAdminConfigGroupSmtp());
    assertLinkAlignmentAndStableHover(smtp);
    attachPanels("SMTP-Direktzugang Vaadin bei " + width + " px");
    smtp.click();
    assertThat(page).hasURL(java.util.regex.Pattern.compile(".*[?&]group=smtp(?:&.*)?$"));
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
    page.mouse().move(1, 1);
  }

  private void assertProviderFieldHelp(String provider) {
    for (String field : new String[] {"client_id", "secret", "enabled"}) {
      Locator info = page.locator("#provider-" + provider + "-" + field + "-info");
      if (!field.equals("enabled")) {
        Locator input =
            page.locator(
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
      Locator help = page.locator("#provider-field-help");
      assertThat(help.locator("vaadin-dialog-overlay > [part~=overlay]")).isVisible();
      String explanation =
          switch (field) {
            case "client_id" -> texts.getProviderSettingsClientIdHelp(provider.equals("microsoft"));
            case "secret" -> texts.getProviderSettingsSecretHelp(provider.equals("microsoft"));
            default -> texts.getProviderSettingsEnableHelp();
          };
      assertThat(help.locator(".admin-help-description")).hasText(explanation);
      help.locator("[slot=footer] vaadin-button").click();
    }
    Locator enabled =
        page.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.CHECKBOX);
    Locator client = page.locator("#provider-" + provider + "-client-id input");
    String originalClient = client.inputValue();
    enabled.uncheck();
    client.fill(" ");
    assertThat(enabled).isDisabled();
    client.fill(originalClient);
    assertThat(enabled).isEnabled();
    assertThat(page.locator("#provider-" + provider + "-status"))
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
    page.setViewportSize(width, 1000);
    page.navigate(path(registration ? REGISTRATION_ROUTE : LOGIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    Locator local = page.locator(".public-auth-panels > .public-panel");
    Locator external = page.locator(".public-auth-panels > .external-login-panel");
    assertThat(local.locator(".external-login-buttons")).hasCount(0);
    assertThat(external.locator("h2")).hasText(texts.getExternalPanelTitle());
    assertThat(external.locator(".external-login-hint")).hasText(texts.getExternalPanelHint());
    assertThat(external.locator("#external-login-google"))
        .hasText(texts.getExternalContinueGoogle());
    assertThat(external.locator("#external-login-microsoft"))
        .hasText(texts.getExternalContinueMicrosoft());
    assertThat(external.locator(".external-login-alternative")).hasCount(0);
    assertThat(page.locator(".public-auth-panels > .external-login-divider"))
        .hasText(texts.getExternalOr());
    assertThat(local.locator(".public-panel-footer")).isVisible();
    assertNotEquals(
        local.locator(".public-panel-body").evaluate("el => getComputedStyle(el).backgroundColor"),
        local
            .locator(".cosmos-panel-header")
            .evaluate("el => getComputedStyle(el).backgroundColor"));
    assertPlacement(local, external, width, registration);
    assertProviderButtonsFlow(external, false);
    assertThat(page.locator(".cosmos-footer")).hasCSS("text-align", "center");
    assertEquals(
        width,
        page.locator(".cosmos-footer").boundingBox().width,
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

    page.setViewportSize(width, 1000);
    page.navigate(
        Path.of("docs/design/frontend-referenzen/public.html").toAbsolutePath().toUri()
            + "#public-"
            + route);
    Locator referencePage = page.locator("#public-" + route);
    Locator referenceLocal = referencePage.locator(".vp-auth-panels > .vp-card");
    Locator referenceExternal = referencePage.locator(".vp-auth-panels > .external-login-panel");
    assertThat(referenceLocal.locator("[data-external-preview]")).hasCount(0);
    assertThat(referenceLocal.locator(".vp-card-foot")).isVisible();
    assertThat(referenceExternal.locator("[data-external-preview]")).hasCount(2);
    assertPlacement(referenceLocal, referenceExternal, width, registration);
    assertProviderButtonsFlow(referenceExternal, false);
    if (registration) {
      assertRegistrationFields(referenceLocal, width);
    } else {
      assertRememberSwitch(referenceLocal);
    }
    attachPanels(route + ": Zwei Panels in der HTML-Referenz");
    if (width == 390) {
      assertResponsiveActions(referenceLocal, registration);
    }
  }

  private void assertRememberSwitch(Locator panel) {
    Locator remember = panel.getByRole(AriaRole.SWITCH);
    assertThat(remember).hasCount(1);
    assertThat(remember).not().isChecked();
    remember.check();
    assertThat(remember).isChecked();
    page.reload();
    assertThat(remember).isChecked();
    remember.focus();
    remember.press("Space");
    assertThat(remember).not().isChecked();
    page.reload();
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
        "Schalterbahn hat in Anwendung und Referenz dieselben kompakten Abmessungen");
  }

  private void attachPanels(String title) {
    Allure.attachment(
        title,
        "image/png",
        new ByteArrayInputStream(page.screenshot(new Page.ScreenshotOptions().setFullPage(true))),
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
      page.setViewportSize(width, 1000);
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
          (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
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
        (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
        "Kein horizontaler Seitenüberlauf");
  }
}
