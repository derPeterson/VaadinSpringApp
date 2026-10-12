package de.derpeterson.browsertests.pages;

import static de.derpeterson.app.config.AppConstants.USER_ICON_ALT;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The same avatar menu a person uses to select a language or sign out. */
public final class UserMenu {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public UserMenu(Page page) { this.page = page; }

    @Step("Benutzermenü über den Avatar öffnen")
    public void open() {
        if (!page.locator("vaadin-popover-overlay[opened]").isVisible())
            avatar().click();
        assertThat(page.locator("vaadin-popover-overlay[opened]")).isVisible();
    }

    @Step("Testsprache über die sichtbare Sprachflagge auswählen")
    public void selectTestLanguage() { selectLanguage(texts.language()); }

    @Step("Sprache ausdrücklich über das Flaggenmenü wechseln")
    public void selectLanguage(String language) {
        UiTexts expected = new UiTexts(language);
        open();
        Locator flag = page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions()
                .setName(language.equals("de") ? "German Flag Icon" : "English Flag Icon").setExact(true));
        page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHas(flag)).click();
        assertThat(flag).hasCSS("filter", "none");
        // The menu may be anonymous or authenticated; both expected labels use exactly this language.
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(expected.getBaseLoginButton()).setExact(true))
                .or(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(expected.getBaseLogoutButton()).setExact(true)))).isVisible();
        page.keyboard().press("Escape");
        assertThat(page.locator("vaadin-popover-overlay[opened]")).not().isVisible();
    }

    @Step("Über die Oberfläche abmelden")
    public void signOut() {
        open();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.getBaseLogoutButton()).setExact(true)).click();
        new LoginPage(page).assertFormVisible();
    }

    @Step("Normales Konto ist angemeldet und hat keine Adminaktion im Menü")
    public void assertUserSignedInWithoutAdminAction(String email) {
        open();
        assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.getBaseLogoutButton()).setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(texts.getBaseManageApplicationButton()).setExact(true))).hasCount(0);
    }

    private Locator avatar() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(USER_ICON_ALT).setExact(true));
    }
}
