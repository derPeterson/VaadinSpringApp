package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;

import de.derpeterson.app.config.AdminConfigGroup;

@Tag("browser") @Feature("Manueller und automatischer Benutzerstatus")
public class UserStatusBrowserTest extends BrowserFixture {
    @Test @DisplayName("STATUS-01: Manuelle Statuswahl wird gespeichert und in zweitem Tab live angezeigt")
    @Description("""
        1. Eigenes Konto über Adminformular anlegen und separat anmelden.
        2. Zwei echte Tabs derselben Sitzung öffnen und deren Benutzermenüs anzeigen.
        3. Verfügbar, abwesend, beschäftigt und offline über die Optionen wählen: zweiter Tab folgt live.
        4. Beschäftigt einstellen, Seite neu laden und gespeicherten aktuellen Wert prüfen.
        **Erwartet:** Ausgewählter Wert, nicht versteckte Optionsliste, zeigt den Status in beiden Tabs.
        """)
    void manualStatusIsStoredAndPushedToAnotherTab() {
        String email = createUser();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD); new UserMenu(actor.page).selectTestLanguage();
            Page second = actor.context.newPage(); second.navigate(path(HOME_ROUTE));
            UserStatusMenu firstMenu = new UserStatusMenu(actor.page), secondMenu = new UserStatusMenu(second);
            secondMenu.expect(texts.getUserStatusAvailable());
            for (String status : new String[]{texts.getUserStatusAbsent(), texts.getUserStatusEmployed(), texts.getUserStatusOffline(), texts.getUserStatusAvailable()}) {
                firstMenu.select(status); secondMenu.expect(status);
            }
            firstMenu.select(texts.getUserStatusEmployed()); secondMenu.expect(texts.getUserStatusEmployed());
            actor.page.reload(); firstMenu.expect(texts.getUserStatusEmployed()); second.close();
        }
    }
    @Test @DisplayName("STATUS-02: Automatische Abwesenheit und Rückkehr; manuell beschäftigt bleibt erhalten")
    @Description("""
        1. Als Admin Abwesenheitsdauer merken und im UI auf zehn Sekunden setzen.
        2. Eigenes Konto anmelden; ohne Benutzeraktion tatsächliche automatische Abwesenheit abwarten.
        3. Seite über echten Browseraufruf neu laden: verfügbar. Manuell beschäftigt wählen.
        4. Weitere zwanzig Sekunden real abwarten: beschäftigt bleibt. Manuell abwesend wählen und neu laden: auch nach Rückkehrlauf erhalten. Original wiederherstellen.
        **Erwartet:** Scheduler und Live-Anzeige wirken; keine Uhr-/Datenbankmanipulation.
        """)
    void automaticAbsenceAndReturnRespectManualStatus() {
        String email = createUser(); ConfigurationPage config = new ConfigurationPage(page);
        config.open(AdminConfigGroup.USER_STATUS); String label = texts.getAdminConfigEntryUserAutoAbsentTimeout();
        String original = config.value(label);
        try {
            config.enter(label, "PT10S"); config.save();
            try (BrowserActor actor = new BrowserActor(browser)) {
                actor.login(email, BrowserTestAccounts.USER_PASSWORD); new UserMenu(actor.page).selectTestLanguage();
                UserStatusMenu menu = new UserStatusMenu(actor.page); menu.expect(texts.getUserStatusAvailable());
                assertThat(menu.current()).containsText(texts.getUserStatusAbsent(), new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions().setTimeout(40_000));
                actor.page.reload(); menu.expect(texts.getUserStatusAvailable()); menu.select(texts.getUserStatusEmployed());
                long end = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
                actor.context.waitForCondition(() -> System.nanoTime() >= end, new BrowserContext.WaitForConditionOptions().setTimeout(25_000));
                menu.expect(texts.getUserStatusEmployed()); menu.select(texts.getUserStatusAbsent());
                actor.page.reload(); menu.expect(texts.getUserStatusAbsent());
                long returnScan = System.nanoTime() + java.time.Duration.ofSeconds(3).toNanos();
                actor.context.waitForCondition(() -> System.nanoTime() >= returnScan);
                menu.expect(texts.getUserStatusAbsent());
            }
        } finally {config.open(AdminConfigGroup.USER_STATUS); config.enter(label, original); config.save();}
    }
    private String createUser() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage(); String email = "status-" + UUID.randomUUID() + "@example.test";
        new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD); return email;
    }
}
