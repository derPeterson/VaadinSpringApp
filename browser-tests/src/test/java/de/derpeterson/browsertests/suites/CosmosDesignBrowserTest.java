package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("browser")
@Feature("Gemeinsames Cosmos-Design")
public class CosmosDesignBrowserTest extends BrowserFixture {
    @ParameterizedTest(name = "{0}: Fünf Kontoansichten bei {1}×{2}")
    @CsvSource({"COSMOS-01A,320,568", "COSMOS-01B,390,844", "COSMOS-01C,844,390", "COSMOS-01D,1280,900", "COSMOS-01E,1280,1200"})
    @Story("Einheitliches responsives Design")
    @Description("""
        1. Login, Registrierung, Passwortanfrage und unbekannte Verifikations-/Resetlinks öffnen.
        2. Geladenes Service-Logo, mittige Card, Inter-Font, CSS-Hintergrund, Fußtext und übersetzte Platzhalter prüfen.
        3. Alle Formularfelder/Buttons auf horizontale Erreichbarkeit prüfen und Screenshots je Ansicht aufnehmen.
        **Erwartet:** Die Card selbst bleibt bei ausreichend Platz in der Bildschirmmitte. Logo/Überschrift stehen darüber; bei wenig Höhe wächst die Seite ohne Abschneidung.
        Ein simuliertes Fenster ersetzt keine menschliche Beurteilung auf einem echten Gerät.
        """)
    void accountViewsShareResponsiveDesign(String id, int width, int height) {
        page.setViewportSize(width, height);
        for (String route : new String[] {path(LOGIN_ROUTE), path(REGISTRATION_ROUTE), path(FORGOT_PASSWORD_ROUTE),
                path(VERIFICATION_ROUTE) + "/" + UUID.randomUUID(), path(RESET_PASSWORD_ROUTE) + "/" + UUID.randomUUID()}) {
            page.navigate(route);
            checkDesign(page);
            for (Locator input : page.locator(".cosmos-card vaadin-email-field").all())
                assertThat(input).hasJSProperty("placeholder", texts.getRegistrationConfirmField().equals(input.evaluate("el => el.label"))
                        ? texts.getAccountConfirmEmailPlaceholder() : texts.getAccountEmailPlaceholder());
            if (route.equals(path(LOGIN_ROUTE)))
                assertThat(page.locator("vaadin-password-field")).hasJSProperty("placeholder", texts.getAccountPasswordPlaceholder());
            if (route.equals(path(REGISTRATION_ROUTE))) {
                assertThat(page.getByLabel(texts.getRegistrationFirstNameField(), new Page.GetByLabelOptions().setExact(true)))
                        .hasJSProperty("placeholder", texts.getAccountFirstNamePlaceholder());
                assertThat(page.getByLabel(texts.getRegistrationLastNameField(), new Page.GetByLabelOptions().setExact(true)))
                        .hasJSProperty("placeholder", texts.getAccountLastNamePlaceholder());
            }
            AllurePlaywright.attachScreenshot(id + " " + route.split("/")[1], page);
        }
    }

    @Test
    @DisplayName("COSMOS-02: Mobile Aktivierung und Passwortänderung über echte Testmails")
    @Story("Gestaltung der gültigen und erfolgreichen Tokenansichten")
    @Description("""
        1. Bei 390×844 ein eigenes Konto registrieren und seinen echten Mailbutton öffnen.
        2. Die erfolgreiche Verifikationscard und ihr gemeinsames Design prüfen.
        3. Passwortanfrage absenden, echten Reset-Mailbutton öffnen und beide Passwort-Platzhalter prüfen.
        4. Neues Passwort setzen, Cosmos-Erfolgsnotification prüfen und normal anmelden.
        **Erwartet:** Der vollständige mobile Ablauf bleibt bedienbar, ohne Datenbank-/Service-Abkürzungen.
        """)
    void mobileActivationAndResetRemainUsable() {
        page.setViewportSize(390, 844);
        String email = "cosmos-" + UUID.randomUUID() + "@example.test";
        RegistrationPage registration = new RegistrationPage(page);
        registration.open(); registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
        registration.submit(); registration.assertSubmitted(email);
        MailpitInbox inbox = new MailpitInbox(context);
        Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
        activated.setViewportSize(390, 844);
        checkDesign(activated);
        AllurePlaywright.attachScreenshot("Mobile Verifikation erfolgreich", activated);
        ForgotPasswordPage request = new ForgotPasswordPage(activated);
        request.open(); request.enterEmail(email); request.submit(); request.requestedResponse();
        Page resetPage = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
        resetPage.setViewportSize(390, 844);
        checkDesign(resetPage);
        assertThat(resetPage.getByLabel(texts.getResetPasswordNewPasswordField(), new Page.GetByLabelOptions().setExact(true)))
                .hasJSProperty("placeholder", texts.getAccountNewPasswordPlaceholder());
        assertThat(resetPage.getByLabel(texts.getResetPasswordConfirmField(), new Page.GetByLabelOptions().setExact(true)))
                .hasJSProperty("placeholder", texts.getAccountConfirmPasswordPlaceholder());
        AllurePlaywright.attachScreenshot("Mobiles gültiges Resetformular", resetPage);
        ResetPasswordPage reset = new ResetPasswordPage(resetPage);
        reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
        reset.submit(); reset.assertChanged();
        assertThat(resetPage.locator("vaadin-notification-card:not([closing])")).hasAttribute("theme", "cosmos");
        AllurePlaywright.attachScreenshot("Mobiler Passwortreset erfolgreich", resetPage);
        LoginPage login = new LoginPage(resetPage);
        login.open(); login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);
        assertThat(resetPage).hasURL(urlPattern(HOME_ROUTE));
    }

    @Test
    @DisplayName("COSMOS-03: Copyright über Admin speichern, neu laden und als sicheren Text anzeigen")
    @Story("Persistente Konfiguration des Fußtexts")
    @Description("""
        1. Als Admin anmelden; unter Allgemein den Copyright-Text mit year/serviceName und HTML-Zeichen speichern.
        2. Neu laden und den gespeicherten Wert bestätigen; abmelden und die Login-Fußzeile prüfen.
        3. Über Registrierung und Passwortanfrage echte Mails erzeugen; geladenes Inline-Logo und denselben sicheren DB-Fußtext prüfen.
        4. Ursprünglichen Wert anschließend über die Oberfläche wiederherstellen.
        **Erwartet:** Platzhalter sind ausgefüllt, HTML bleibt Text, Home/Admin besitzen keine Cosmos-Palette.
        """)
    void copyrightCanBeConfiguredThroughTheUi() {
        String homePrimary = primary(page, "html");
        ConfigurationPage config = adminConfig();
        assertEquals(homePrimary, primary(page, "html"));
        assertThat(page.locator(".cosmos-page")).hasCount(0);
        String label = texts.getAdminConfigEntryCopyrightText();
        String original = config.value(label);
        String template = "© {year} {serviceName} · <b>Browser-Test</b>";
        try {
            config.enter(label, template); config.save(); config.reload();
            assertEquals(template, config.value(label));
            new UserMenu(page).signOut();
            Locator footer = page.locator(".cosmos-footer");
            assertThat(footer).containsText("VaadinSpringApp (Browser-Test) · <b>Browser-Test</b>");
            assertFalse(footer.innerText().contains("{year}"));
            assertThat(footer.locator("b")).hasCount(0);
            AllurePlaywright.attachScreenshot("Konfigurierter sicherer Copyright-Fußtext", page);
            String expectedFooter = footer.innerText();
            String email = "copyright-" + UUID.randomUUID() + "@example.test";
            RegistrationPage registration = new RegistrationPage(page);
            registration.open(); registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
            registration.submit(); registration.assertSubmitted(email);
            MailpitInbox inbox = new MailpitInbox(context, expectedFooter);
            Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
            ForgotPasswordPage request = new ForgotPasswordPage(activated);
            request.open(); request.enterEmail(email); request.submit(); request.requestedResponse();
            inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
        } finally {
            ConfigurationPage restore = adminConfig();
            restore.enter(label, original); restore.save();
        }
    }

    @Test
    @DisplayName("COSMOS-04: Fehlermeldung erhält Card-Hintergrund; Home behält seine Lumo-Farbe")
    @Story("Begrenztes Theme und Notificationgestaltung")
    @Description("""
        1. Home-Farbe erfassen, bei 320×568 eine falsche Anmeldung absenden.
        2. Cosmos-Notification, gleichen Verlauf wie Card und erreichbaren Schließen-Button prüfen.
        3. Zur Startseite wechseln und die bisherige Farbe sowie globale Inter-Schrift bestätigen.
        **Erwartet:** Konto-Notifications passen zur Card; Palette und Hintergrund greifen nicht auf Home über.
        """)
    void notificationMatchesTheCardWithoutChangingHome() {
        String homePrimary = primary(page, "html");
        page.setViewportSize(320, 568);
        LoginPage login = new LoginPage(page); login.open();
        assertEquals("#526ebc", primary(page, ".cosmos-theme"));
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!"); login.assertRejected();
        Locator notice = page.locator("vaadin-notification-card:not([closing])");
        assertThat(notice).hasAttribute("theme", "cosmos");
        String cardBackground = page.locator(".cosmos-card").evaluate("el => getComputedStyle(el).backgroundImage").toString();
        assertEquals(cardBackground, notice.evaluate("el => getComputedStyle(el.shadowRoot.querySelector('[part=overlay]')).backgroundImage"));
        assertWithinViewport(notice);
        AllurePlaywright.attachScreenshot("Schmale Cosmos-Fehlermeldung", page);
        notice.getByRole(AriaRole.BUTTON).click();
        page.navigate(path(HOME_ROUTE));
        assertThat(page).hasTitle(HOME_PAGE_TITLE);
        assertEquals(homePrimary, primary(page, "html"));
        assertThat(page.locator(".cosmos-page")).hasCount(0);
        assertTrue(page.locator("body").evaluate("el => getComputedStyle(el).fontFamily").toString().contains("Inter"));
    }

    private ConfigurationPage adminConfig() {
        page.setViewportSize(1280, 900);
        LoginPage login = new LoginPage(page); login.open();
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        ConfigurationPage config = new ConfigurationPage(page); config.open(AdminConfigGroup.GENERAL);
        return config;
    }
    private String primary(Page target, String selector) {
        return target.locator(selector).evaluate("el => getComputedStyle(el).getPropertyValue('--lumo-primary-color').trim()").toString();
    }
    private void checkDesign(Page target) {
        assertThat(target.locator(".cosmos-card")).hasCount(1);
        assertThat(target.locator(".cosmos-card")).isVisible();
        assertThat(target.locator(".cosmos-sky")).hasAttribute("aria-hidden", "true");
        assertThat(target.locator(".cosmos-page img")).hasCount(1);
        assertThat(target.locator(".cosmos-sky img, .cosmos-card img")).hasCount(0);
        Locator logo = target.locator(".cosmos-logo");
        assertThat(logo).isVisible();
        target.waitForFunction("image => image.complete && image.naturalWidth > 0", logo.elementHandle());
        assertWithinViewport(logo);
        assertTrue((Boolean) target.evaluate("""
            () => {
                const card = document.querySelector('.cosmos-card').getBoundingClientRect();
                const logo = document.querySelector('.cosmos-logo').getBoundingClientRect();
                const name = document.querySelector('.cosmos-brand-name').getBoundingClientRect();
                return logo.top >= -1 && logo.bottom <= name.top + 1 && name.bottom <= card.top + 1;
            }
            """), "Logo und Servicebezeichnung müssen ohne Überlappung oberhalb der Card stehen.");
        // Die breite Registrierung darf bei zu wenig Höhe scrollen; Fensterhöhe allein beweist keinen Platz.
        if ((Boolean) target.evaluate(
                "() => document.querySelector('.cosmos-page').getBoundingClientRect().height <= innerHeight + 1")) {
            assertTrue((Boolean) target.locator(".cosmos-card").evaluate(
                    "el => { const r=el.getBoundingClientRect(); return Math.abs(r.top + r.height / 2 - innerHeight / 2) <= 2; }"),
                    "Die Card selbst muss mittig bleiben, unabhängig von Logo und Copyright.");
        }
        assertThat(target.locator(".cosmos-footer")).isVisible();
        for (Locator input : target.locator(".cosmos-card input[placeholder]:placeholder-shown").all()) {
            assertEquals("13px", input.evaluate("el => getComputedStyle(el, '::placeholder').fontSize"));
            assertEquals("400", input.evaluate("el => getComputedStyle(el, '::placeholder').fontWeight"));
        }
        target.evaluate("() => document.fonts.ready");
        assertTrue((Boolean) target.evaluate("() => document.fonts.check('450 16px Inter')"));
        assertTrue(target.locator(".cosmos-card").evaluate("el => getComputedStyle(el).fontFamily").toString().contains("Inter"));
        assertTrue((Boolean) target.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        for (Locator control : target.locator(".cosmos-card, .cosmos-footer, .cosmos-card vaadin-button, .cosmos-card vaadin-text-field, .cosmos-card vaadin-email-field, .cosmos-card vaadin-password-field, .cosmos-card vaadin-date-picker, .cosmos-card vaadin-combo-box").all())
            assertWithinViewport(control);
    }
    private void assertWithinViewport(Locator control) {
        assertTrue((Boolean) control.evaluate("el => { const r=el.getBoundingClientRect(); return r.left >= -1 && r.right <= innerWidth + 1; }"),
                "Element liegt außerhalb der Fensterbreite: " + control);
    }
}
