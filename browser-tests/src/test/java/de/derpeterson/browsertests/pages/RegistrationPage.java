package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Visible registration form; no service calls or database preparation. */
public final class RegistrationPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public RegistrationPage(Page page) { this.page = page; }

    @Step("Registrierungsformular öffnen")
    public void open() {
        page.navigate("/registration");
        assertThat(page).hasTitle("Registration");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("registrationView.title")).setExact(true))).isVisible();
    }

    @Step("Pflichtfelder mit gültigen Testangaben ausfüllen")
    public void fillValid(String email, String password) {
        field("registrationView.first_name_field").fill("Browser");
        field("registrationView.last_name_field").fill("Testuser");
        setEmails(email, email);
        setPasswords(password, password);
        field("registrationView.gender_combobox").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(texts.text("gender.other")).setExact(true)).click();
        field("registrationView.birth_date_field").fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
        // Enter commits the typed date and closes its popup; Escape can close surrounding dialogs.
        field("registrationView.birth_date_field").press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(page.locator("vaadin-date-picker")).hasJSProperty("value", "1990-01-01");
    }

    @Step("E-Mail und E-Mail-Bestätigung eingeben")
    public void setEmails(String email, String confirmation) {
        field("registrationView.email_field").fill(email);
        Locator confirm = page.locator("vaadin-email-field").nth(1);
        assertThat(confirm).hasJSProperty("label", texts.text("registrationView.confirm_field"));
        confirm.locator("input").fill(confirmation);
    }

    @Step("Passwort und Passwortbestätigung eingeben")
    public void setPasswords(String password, String confirmation) {
        field("registrationView.password_field").fill(password);
        Locator confirm = page.locator("vaadin-password-field").nth(1);
        assertThat(confirm).hasJSProperty("label", texts.text("registrationView.confirm_field"));
        confirm.locator("input").fill(confirmation);
    }

    @Step("Registrierung über den Button absenden")
    public void submit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.text("base.registration_button")).setExact(true)).click();
    }

    @Step("Bestätigung nennt die eigene Testadresse und fordert zur Verifikation auf")
    public void assertSubmitted(String email) {
        assertThat(page).hasURL(Pattern.compile(".*/registration(?:\\?.*)?$"));
        Locator notice = page.locator("vaadin-notification-card").filter(new Locator.FilterOptions().setHasText(email));
        assertThat(notice).containsText(texts.visibleText(page, "registrationView.registration.success_message", email));
    }

    @Step("Pflichtfeldfehler und markierte Felder prüfen")
    public void assertRequiredError() {
        assertError("base.validation.required_message");
        assertThat(page.locator("vaadin-text-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-combo-box[invalid]")).isVisible();
        assertThat(page.locator("vaadin-date-picker[invalid]")).isVisible();
    }

    @Step("Ungültige E-Mail verständlich anzeigen und beide E-Mail-Felder markieren")
    public void assertEmailError() {
        assertError("base.validation.email_invalid_message");
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
    }

    @Step("Abweichende Passwortbestätigung verständlich anzeigen und Felder markieren")
    public void assertPasswordConfirmationError() {
        assertError("base.validation.password_confirm_message");
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
    }

    private Locator field(String key) {
        return page.getByLabel(texts.text(key), new Page.GetByLabelOptions().setExact(true));
    }

    private void assertError(String key) {
        assertThat(page).hasURL(Pattern.compile(".*/registration(?:\\?.*)?$"));
        assertThat(page.getByText(texts.text(key), new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.locator("vaadin-notification-card").filter(new Locator.FilterOptions()
                .setHas(page.getByText(texts.text("base.success.title"), new Page.GetByTextOptions().setExact(true))))).hasCount(0);
    }
}
