package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The same avatar menu a person uses to sign out or inspect their account. */
public final class UserMenu {
    private final Page page;

    public UserMenu(Page page) { this.page = page; }

    @Step("Benutzermenü über den Avatar öffnen")
    public void open() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("User Icon").setExact(true)).click();
    }

    @Step("Über die Oberfläche abmelden")
    public void signOut() {
        open();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Abmelden|Logout)$"))).click();
        new LoginPage(page).assertFormVisible();
    }

    @Step("Normales Konto ist angemeldet und hat keine Adminaktion im Menü")
    public void assertUserSignedInWithoutAdminAction(String email) {
        open();
        assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Abmelden|Logout)$")))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Anwendung verwalten|Manage application)$")))).hasCount(0);
    }
}
