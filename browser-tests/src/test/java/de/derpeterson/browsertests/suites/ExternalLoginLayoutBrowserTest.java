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
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Externe Anmeldung")
public class ExternalLoginLayoutBrowserTest extends BrowserFixture {
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
    }
    attachPanels(route + ": Zwei Panels in der HTML-Referenz");
    if (width == 390) {
      assertResponsiveActions(referenceLocal, registration);
    }
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
