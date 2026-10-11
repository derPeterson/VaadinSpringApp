package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Visible registration form; no service calls or database preparation. */
public final class RegistrationPage {
    private final Page page;

    public RegistrationPage(Page page) { this.page = page; }

    @Step("Registrierungsformular öffnen")
    public void open() {
        page.navigate("/registration");
        assertThat(page).hasTitle("Registration");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Registrierung|Registration)$")))).isVisible();
    }

    @Step("Pflichtfelder mit gültigen Testangaben ausfüllen")
    public void fillValid(String email, String password) {
        page.getByLabel(Pattern.compile("^(Vorname|First Name)$")).fill("Browser");
        page.getByLabel(Pattern.compile("^(Nachname|Last Name)$")).fill("Testuser");
        setEmails(email, email);
        setPasswords(password, password);
        page.getByLabel(Pattern.compile("^(Geschlecht|Gender)$")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile("^(Divers|Other)$"))).click();
        boolean germanForm = page.getByLabel("Geburtsdatum", new Page.GetByLabelOptions().setExact(true)).count() > 0;
        page.getByLabel(Pattern.compile("^(Geburtsdatum|Birth Date)$")).fill(germanForm ? "01.01.1990" : "1/1/1990");
        // Enter commits the typed date and closes its popup; Escape can close surrounding dialogs.
        page.getByLabel(Pattern.compile("^(Geburtsdatum|Birth Date)$")).press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(page.locator("vaadin-date-picker")).hasJSProperty("value", "1990-01-01");
    }

    @Step("E-Mail und E-Mail-Bestätigung eingeben")
    public void setEmails(String email, String confirmation) {
        page.locator("vaadin-email-field").nth(0).locator("input").fill(email);
        page.locator("vaadin-email-field").nth(1).locator("input").fill(confirmation);
    }

    @Step("Passwort und Passwortbestätigung eingeben")
    public void setPasswords(String password, String confirmation) {
        page.locator("vaadin-password-field").nth(0).locator("input").fill(password);
        page.locator("vaadin-password-field").nth(1).locator("input").fill(confirmation);
    }

    @Step("Registrierung über den Button absenden")
    public void submit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Registrieren|Register)$"))).click();
    }

    @Step("Bestätigung nennt die eigene Testadresse und fordert zur Verifikation auf")
    public void assertSubmitted(String email) {
        assertThat(page).hasURL(Pattern.compile(".*/registration(?:\\?.*)?$"));
        Locator notice = page.locator("vaadin-notification-card").filter(new Locator.FilterOptions().setHasText(email));
        assertThat(notice).containsText(Pattern.compile("(verify your account|verifizieren Sie Ihren Account)"));
    }

    @Step("Pflichtfeldfehler und markierte Felder prüfen")
    public void assertRequiredError() {
        assertError("^(Bitte füllen Sie alle erforderlichen Felder aus\\.|Please fill in all required fields\\.)$");
        assertThat(page.locator("vaadin-text-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
        assertThat(page.locator("vaadin-combo-box[invalid]")).isVisible();
        assertThat(page.locator("vaadin-date-picker[invalid]")).isVisible();
    }

    @Step("Ungültige E-Mail verständlich anzeigen und beide E-Mail-Felder markieren")
    public void assertEmailError() {
        assertError("^(Bitte geben Sie eine gültige E-Mail-Adresse ein\\.|Please enter a valid email address\\.)$");
        assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(2);
    }

    @Step("Abweichende Passwortbestätigung verständlich anzeigen und Felder markieren")
    public void assertPasswordConfirmationError() {
        assertError("^(Das Kennwort und das Bestätigungskennwort stimmen nicht überein\\.|The password and confirm password does not match\\.)$");
        assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
    }

    private void assertError(String message) {
        assertThat(page).hasURL(Pattern.compile(".*/registration(?:\\?.*)?$"));
        assertThat(page.getByText(Pattern.compile(message))).isVisible();
        assertThat(page.getByText(Pattern.compile("(Please check your inbox|Bitte überprüfen Sie Ihren Posteingang)"))).hasCount(0);
    }
}
