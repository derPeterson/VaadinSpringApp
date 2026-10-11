package de.derpeterson.browsertests.suites;

import de.derpeterson.browsertests.BrowserFixture;
import io.qameta.allure.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("browser")
@Feature("Infrastruktur")
public class InfrastructureBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("INFRA-01: Startseite, Logo, Avatare und Sprachflaggen werden geladen")
    @Story("Startseite und Bilder")
    @Description("""
            1. Die Startseite öffnen und den konfigurierten Servicenamen prüfen.
            2. Das Benutzermenü öffnen.
            3. Logo, beide Avatare und Sprachflaggen auf Sichtbarkeit und geladenen Bildinhalt prüfen.

            **Erwartet:** Startseite korrekt gerendert; Bilder sichtbar und nicht defekt.
            Prüft keine vollständige responsive Darstellung, Rollen- oder Tabelleninitialisierung.
            """)
    void homeAndImagesLoad() {
        page.navigate("/home");
        assertThat(page).hasTitle("Home");
        assertThat(page.getByText("VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("User Icon").setExact(true)).click();
        for (String name : new String[] { "Service Icon", "German Flag Icon", "English Flag Icon" }) {
            assertImageLoaded(page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(name).setExact(true)));
        }
        Locator avatars = page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("User Icon").setExact(true));
        assertThat(avatars).hasCount(2);
        for (Locator avatar : avatars.all()) assertImageLoaded(avatar);
    }

    @Test
    @DisplayName("INFRA-02: Loginformular wird im Browser gerendert")
    @Story("Loginformular")
    @Description("""
            1. /login öffnen.
            2. E-Mail-Feld, Passwortfeld und Anmeldebutton prüfen.

            **Erwartet:** Das Loginformular ist sichtbar und bedienbar.
            Dieser Infrastrukturfall führt noch keine Anmeldung aus.
            """)
    void loginFormLoads() {
        page.navigate("/login");
        assertThat(page.locator("vaadin-email-field")).isVisible();
        assertThat(page.locator("vaadin-password-field")).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^(Anmelden|Log in|Login)$", Pattern.CASE_INSENSITIVE)))).isVisible();
    }

    @Test
    @DisplayName("INFRA-03: Isolierte Mailpit-Instanz liefert eine lesbare Nachrichtenliste")
    @Story("Mailpit-Testumgebung")
    @Description("""
            1. Die Mailpit-API der eigenen Testumgebung aufrufen.
            2. Erfolgreiche Antwort, Nachrichtenliste und plausible Gesamtzahl prüfen.

            **Erwartet:** Mailpit erreichbar; die Nachrichtenliste ist lesbar, auch nach vorherigen Versandtests.
            Das beweist noch keinen SMTP-Versand der Anwendung.
            """)
    void mailpitIsReachable() throws Exception {
        var response = context.request().get(System.getProperty("browser.tests.mailpitUrl") + "/api/v1/messages");
        assertTrue(response.ok());
        var inbox = new ObjectMapper().readTree(response.body());
        assertTrue(inbox.path("messages").isArray());
        assertTrue(inbox.path("total").asInt(-1) >= inbox.path("messages").size());
    }

    private void assertImageLoaded(Locator image) {
        assertThat(image).isVisible();
        page.waitForFunction("image => image.complete && image.naturalWidth > 0", image.elementHandle());
    }
}
