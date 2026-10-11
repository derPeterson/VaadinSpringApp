package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Only the visible login form; does not bypass the application's authentication. */
public final class LoginPage {
    private final Page page;

    public LoginPage(Page page) { this.page = page; }

    @Step("Loginformular öffnen")
    public void open() {
        page.navigate("/login");
        assertFormVisible();
    }

    @Step("E-Mail und Passwort eingeben und Anmeldung absenden")
    public void signIn(String email, String password) {
        page.getByLabel(Pattern.compile("^(E-Mail|Email)$")).fill(email);
        page.getByLabel(Pattern.compile("^(Passwort|Password)$")).fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Anmelden|Login|Log in)$"))).click();
    }

    @Step("Loginformular ist sichtbar")
    public void assertFormVisible() {
        assertThat(page).hasURL(Pattern.compile(".*/login(?:\\?.*)?$"));
        assertThat(page.locator("vaadin-email-field")).isVisible();
        assertThat(page.locator("vaadin-password-field")).isVisible();
    }

    @Step("Verständlicher Anmeldefehler ohne Zugriff auf den Adminbereich")
    public void assertRejected() {
        assertFormVisible();
        assertThat(page.getByText(Pattern.compile(
                "^(Falsche E-Mail oder Passwort\\.|Incorrect email or password\\.)$"))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).hasCount(0);
    }

    @Step("Admin-Dashboard und angemeldetes Konto sind sichtbar")
    public void assertAdminSignedIn(String email) {
        assertThat(page).hasURL(Pattern.compile(".*/admin(?:\\?.*)?$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
        assertThat(page.getByText("Welcome: " + email, new Page.GetByTextOptions().setExact(true))
                .or(page.getByText("Willkommen: " + email, new Page.GetByTextOptions().setExact(true)))).isVisible();
    }
}
