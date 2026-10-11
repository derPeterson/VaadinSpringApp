package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Only the visible login form; does not bypass the application's authentication. */
public final class LoginPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public LoginPage(Page page) { this.page = page; }

    @Step("Loginformular öffnen")
    public void open() {
        page.navigate("/login");
        assertFormVisible();
    }

    @Step("E-Mail und Passwort eingeben und Anmeldung absenden")
    public void signIn(String email, String password) {
        page.getByLabel(texts.text("loginView.email_field"), new Page.GetByLabelOptions().setExact(true)).fill(email);
        page.getByLabel(texts.text("loginView.password_field"), new Page.GetByLabelOptions().setExact(true)).fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.text("base.login_button")).setExact(true)).click();
    }

    @Step("Loginformular ist in der ausgewählten Sprache sichtbar")
    public void assertFormVisible() {
        assertThat(page).hasURL(Pattern.compile(".*/login(?:\\?.*)?$"));
        assertThat(page.getByLabel(texts.text("loginView.email_field"), new Page.GetByLabelOptions().setExact(true))).isVisible();
        assertThat(page.getByLabel(texts.text("loginView.password_field"), new Page.GetByLabelOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("loginView.title")).setExact(true))).isVisible();
    }

    @Step("Verständlicher Anmeldefehler ohne Zugriff auf den Adminbereich")
    public void assertRejected() {
        assertFormVisible();
        assertThat(page.getByText(texts.text("loginView.login.failed_message"), new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("adminView.dashboard.title")).setExact(true))).hasCount(0);
    }

    @Step("Admin-Dashboard und angemeldetes Konto sind sichtbar")
    public void assertAdminSignedIn(String email) {
        assertThat(page).hasURL(Pattern.compile(".*/admin(?:\\?.*)?$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("adminView.dashboard.title")).setExact(true))).isVisible();
        assertThat(page.getByText(texts.text("adminView.dashboard.welcome") + ": " + email,
                new Page.GetByTextOptions().setExact(true))).isVisible();
    }
}
