package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;

@Tag("browser") @Feature("Weitere sichtbare Konfigurationsverträge")
public class ConfigurationBrowserTest extends BrowserFixture {
    @Test @DisplayName("CONFIG-04A: Dienstname und Basis-URL validieren; Name speichern und wiederherstellen")
    @Description("""
        1. Allgemeine Gruppe öffnen; Originalnamen und URL merken.
        2. Blanken Namen bzw. ungültige URL versuchen: Feldfehler und unveränderte Werte nach Neuladen.
        3. Eigenen kurzen Unicode-Dienstnamen speichern und frisch laden: identischer Wert.
        4. Originalnamen wieder speichern; Basis-URL bleibt unverändert.
        **Erwartet:** Stringvalidierung und Speicherung sind sichtbar wirksam; kein Neustartnachweis.
        """)
    void stringValidationAndPersistence() {
        ConfigurationPage settings = admin(); settings.open(AdminConfigGroup.GENERAL);
        String name = texts.getAdminConfigEntryServiceName(), url = texts.getAdminConfigEntryBaseUrl();
        String original = settings.value(name), originalUrl = settings.value(url);
        try {
            settings.enter(name, ""); settings.reject("vaadin-text-field", texts.getAdminConfigValidationServiceName()); settings.reload();
            assertThat(settings.input(name)).hasValue(original);
            settings.enter(url, "invalid"); settings.reject("vaadin-text-field", texts.getAdminConfigValidationUrl()); settings.reload();
            assertThat(settings.input(url)).hasValue(originalUrl);
            settings.enter(name, "Browserprüfung ÄÖÜ"); settings.save(); settings.reload();
            assertThat(settings.input(name)).hasValue("Browserprüfung ÄÖÜ");
            try (Page freshHome = context.newPage()) {
                freshHome.navigate(path(HOME_ROUTE)); assertThat(freshHome).hasTitle(HOME_PAGE_TITLE);
                assertThat(freshHome.getByText("Browserprüfung ÄÖÜ", new Page.GetByTextOptions().setExact(true))).isVisible();
            }
        } finally {settings.enter(name, original); settings.enter(url, originalUrl); settings.save();}
    }
    @Test @DisplayName("CONFIG-04B: Boolean umschalten, speichern, neu laden und wiederherstellen")
    @Description("""
        1. Sicherheitsgruppe öffnen und aktuellen Wartungsmodus merken.
        2. Checkbox umschalten, speichern und neu laden: exakt geänderter Checkboxzustand.
        3. Originalzustand wieder speichern und neu prüfen.
        **Erwartet:** Boolean-Bedienung und Persistenz; keine Behauptung einer Wartungsfunktion.
        """)
    void booleanPersistsAcrossReload() {
        ConfigurationPage settings = admin(); settings.open(AdminConfigGroup.SECURITY);
        Locator checkbox = page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions()
                .setName(texts.getAdminConfigEntryMaintenanceMode()).setExact(true)); boolean original = checkbox.isChecked();
        try {checkbox.setChecked(!original); settings.save(); settings.reload(); assertEquals(!original, checkbox.isChecked());}
        finally {checkbox.setChecked(original); settings.save(); settings.reload(); assertEquals(original, checkbox.isChecked());}
    }
    @Test @DisplayName("CONFIG-04C: Positive Ganzzahl mit Nullfehler, Speicherung und Wiederherstellung")
    @Description("""
        1. Sicherheitsgruppe öffnen und maximales Sitzungslimit merken.
        2. Null versuchen: Fehler mindestens eins; nach Neuladen Originalwert.
        3. Positiven Wert vier speichern, frisch laden und Original wiederherstellen.
        **Erwartet:** Echter IntegerField-Vertrag; gleichzeitig eingeloggte Konten werden hier nicht verändert.
        """)
    void integerValidationAndPersistence() {
        ConfigurationPage settings = admin(); settings.open(AdminConfigGroup.SECURITY);
        String label = texts.getAdminConfigEntryMaxSessionsPerUser(), original = settings.value(label);
        try {
            settings.enter(label, "0"); settings.reject("vaadin-integer-field", texts.getAdminConfigValidationMinOne()); settings.reload();
            assertThat(settings.input(label)).hasValue(original);
            settings.enter(label, "4"); settings.save(); settings.reload(); assertThat(settings.input(label)).hasValue("4");
        } finally {settings.enter(label, original); settings.save();}
    }
    private ConfigurationPage admin() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage(); login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
        return new ConfigurationPage(page);
    }
}
