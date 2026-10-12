package de.derpeterson.browsertests.pages;

import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Visible registration form; no service calls or database preparation. */
public final class RegistrationPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public RegistrationPage(Page page) { this.page = page; }

    @Step("Registrierungsformular öffnen")
    public void open() {
        page.navigate(path(REGISTRATION_ROUTE));
        assertThat(page).hasTitle(REGISTRATION_PAGE_TITLE);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.getRegistrationTitle()).setExact(true))).isVisible();
    }

    @Step("Pflichtfelder mit gültigen Testangaben ausfüllen")
    public void fillValid(String email, String password) {
        field(texts.getRegistrationFirstNameField()).fill("Browser");
        field(texts.getRegistrationLastNameField()).fill("Testuser");
        setEmails(email, email);
        setPasswords(password, password);
        field(texts.getRegistrationGenderCombobox()).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(texts.getGenderOther()).setExact(true)).click();
        field(texts.getRegistrationBirthDateField()).fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
        // Enter commits the typed date and closes its popup; Escape can close surrounding dialogs.
        field(texts.getRegistrationBirthDateField()).press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(page.locator("vaadin-date-picker")).hasJSProperty("value", "1990-01-01");
    }

    @Step("E-Mail und E-Mail-Bestätigung eingeben")
    public void setEmails(String email, String confirmation) {
        field(texts.getRegistrationEmailField()).fill(email);
        Locator confirm = page.locator("vaadin-email-field").nth(1);
        assertThat(confirm).hasJSProperty("label", texts.getRegistrationConfirmField());
        confirm.locator("input").fill(confirmation);
    }

    @Step("Passwort und Passwortbestätigung eingeben")
    public void setPasswords(String password, String confirmation) {
        field(texts.getRegistrationPasswordField()).fill(password);
        Locator confirm = page.locator("vaadin-password-field").nth(1);
        assertThat(confirm).hasJSProperty("label", texts.getRegistrationConfirmField());
        confirm.locator("input").fill(confirmation);
    }

    @Step("Registrierung über den Button absenden")
    public void submit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.getBaseRegistrationButton()).setExact(true)).click();
    }

    @Step("Bestätigung nennt die eigene Testadresse und fordert zur Verifikation auf")
    public void assertSubmitted(String email) {
        assertThat(page).hasURL(urlPattern(REGISTRATION_ROUTE));
        Locator notice = page.locator("vaadin-notification-card").filter(new Locator.FilterOptions().setHasText(email));
        assertThat(notice).containsText(texts.visibleText(page, texts.getRegistrationSuccessMessage(email)));
    }

    @Step("Pflichtfeldfehler und markierte Felder prüfen")
    public void assertRequiredError() {
        assertError(texts.getBaseValidationRequiredMessage());
        assertThat(page.locator("vaadin-text-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-combo-box[invalid]")).isVisible();
        assertThat(page.locator("vaadin-date-picker[invalid]")).isVisible();
    }

    @Step("Ungültige E-Mail verständlich anzeigen und beide E-Mail-Felder markieren")
    public void assertEmailError() {
        assertError(texts.getBaseValidationEmailInvalidMessage());
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
    }

    @Step("Abweichende Passwortbestätigung verständlich anzeigen und Felder markieren")
    public void assertPasswordConfirmationError() {
        assertError(texts.getBaseValidationPasswordConfirmMessage());
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
    }

    @Step("Einzelne Registrierungsangabe ändern")
    public void change(String label, String value) { field(label).fill(value); field(label).press("Tab"); }

    @Step("Verständlichen Registrierungsfehler ohne Erfolg prüfen")
    public void assertValidationError(String message) {
        Locator notice = page.locator("vaadin-notification-card:not([closing])");
        assertThat(notice).hasCount(1);
        assertThat(notice).containsText(texts.visibleText(page, message));
        assertThat(notice).containsText(texts.getBaseFailedTitle());
        notice.getByRole(AriaRole.BUTTON).click();
        assertThat(notice).not().isVisible();
    }

    private Locator field(String label) {
        return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
    }

    private void assertError(String message) {
        assertThat(page).hasURL(urlPattern(REGISTRATION_ROUTE));
        assertThat(page.getByText(message, new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.locator("vaadin-notification-card").filter(new Locator.FilterOptions()
                .setHas(page.getByText(texts.getBaseSuccessTitle(), new Page.GetByTextOptions().setExact(true))))).hasCount(0);
    }
}
