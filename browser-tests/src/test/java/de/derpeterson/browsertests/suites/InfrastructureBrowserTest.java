package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import de.derpeterson.browsertests.BrowserFixture;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Infrastruktur")
public class InfrastructureBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("INFRA-04: HTTPS ist vertrauenswürdig und HTTP führt keine Fachaktionen aus")
  @Story("HTTPS-Zugang")
  @Description(
      """
      1. Die Startseite ohne Zertifikatsausnahme über HTTPS laden.
      2. GET am eigenen HTTP-Einstieg ohne automatische Weiterleitung abrufen.
      3. POST /login am HTTP-Einstieg ohne automatische Weiterleitung absenden.

      **Erwartet:** Die Browserseite verwendet HTTPS. GET antwortet mit 308 und
      der exakten HTTPS-Origin, POST mit 403 ohne Weiterleitung oder Anmeldung.
      """)
  void httpsIsTrustedAndUnsafeHttpIsRejectedBeforeRedirect() {
    String origin = System.getProperty("browser.tests.baseUrl");
    assertTrue(page.url().startsWith(origin + "/"));
    assertTrue(origin.startsWith("https://"));
    String plain = System.getProperty("browser.tests.httpUrl");
    APIResponse navigation =
        context.request().get(plain + "/login", RequestOptions.create().setMaxRedirects(0));
    try {
      assertEquals(308, navigation.status());
      assertEquals(origin + "/login", navigation.headers().get("location"));
    } finally {
      navigation.dispose();
    }
    APIResponse login =
        context.request().post(plain + "/login", RequestOptions.create().setMaxRedirects(0));
    try {
      assertEquals(403, login.status());
    } finally {
      login.dispose();
    }
  }

  @Tag("fast")
  @Test
  @DisplayName("INFRA-01: Öffentliche Startseite, Logo und Sprachwahl werden geladen")
  @Story("Startseite und Bilder")
  @Description(
      """
            1. Die Startseite öffnen und den konfigurierten Servicenamen prüfen.
            2. Die öffentliche Kopfnavigation und ihre Sprachwahl prüfen.
            3. Geladenes Service-Logo und die ausdrücklich gewählte Sprache prüfen.

            **Erwartet:** Startseite korrekt gerendert, Logo geladen und Sprachwahl sichtbar. Besucher haben kein Avatar-Popup.
            Prüft keine vollständige responsive Darstellung, Rollen- oder Tabelleninitialisierung.
            """)
  void homeAndImagesLoad() {
    page.navigate(path(HOME_ROUTE));
    assertThat(page).hasTitle(HOME_PAGE_TITLE);
    assertThat(
            page.getByText(
                "VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertImageLoaded(page.locator(".public-brand img"));
    assertThat(page.locator(".public-navigation")).isVisible();
    assertThat(page.locator(".public-languages vaadin-button")).hasCount(2);
    assertThat(page.locator(".public-languages vaadin-button[aria-pressed='true']"))
        .hasText(texts.language().toUpperCase(java.util.Locale.ROOT));
    assertThat(page.locator(".tracked-user-button")).hasCount(0);
  }

  @Tag("fast")
  @Test
  @DisplayName("INFRA-02: Loginformular wird im Browser gerendert")
  @Story("Loginformular")
  @Description(
      """
            1. /login öffnen.
            2. E-Mail-Feld, Passwortfeld und Anmeldebutton prüfen.

            **Erwartet:** Das Loginformular ist sichtbar und bedienbar.
            Dieser Infrastrukturfall führt noch keine Anmeldung aus.
            """)
  void loginFormLoads() {
    new de.derpeterson.browsertests.pages.LoginPage(page).open();
    assertThat(
            page.locator(".public-panel")
                .getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions()
                        .setName(texts.getBaseLoginButton())
                        .setExact(true)))
        .isVisible();
  }

  @Test
  @DisplayName("INFRA-03: Isolierte Mailpit-Instanz liefert eine lesbare Nachrichtenliste")
  @Story("Mailpit-Testumgebung")
  @Description(
      """
            1. Die Mailpit-API der eigenen Testumgebung aufrufen.
            2. Erfolgreiche Antwort, Nachrichtenliste und plausible Gesamtzahl prüfen.

            **Erwartet:** Mailpit erreichbar; die Nachrichtenliste ist lesbar, auch nach vorherigen Versandtests.
            Das beweist noch keinen SMTP-Versand der Anwendung.
            """)
  void mailpitIsReachable() throws Exception {
    APIResponse response =
        context.request().get(System.getProperty("browser.tests.mailpitUrl") + "/api/v1/messages");
    assertTrue(response.ok());
    JsonNode inbox = new ObjectMapper().readTree(response.body());
    assertTrue(inbox.path("messages").isArray());
    assertTrue(inbox.path("total").asInt(-1) >= inbox.path("messages").size());
  }

  private void assertImageLoaded(Locator image) {
    assertThat(image).isVisible();
    page.waitForFunction(
        "image => image.complete && image.naturalWidth > 0", image.elementHandle());
  }
}
