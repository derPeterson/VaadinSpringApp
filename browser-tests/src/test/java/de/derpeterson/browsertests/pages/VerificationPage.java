package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertions on the actual activation page, including its existing idempotent success contract. */
public final class VerificationPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public VerificationPage(Page page) { this.page = page; }

    @Step("Aktivierungsseite bestätigt die Verifikation des Kontos")
    public void assertActivated() {
        assertThat(page).hasURL(Pattern.compile(".*/verification/[0-9a-f-]+$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("verificationView.success.title")).setExact(true))).isVisible();
        assertThat(page.getByText(texts.text("verificationView.success.text"), new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    @Step("Unbekannter Verifikationslink zeigt einen verständlichen Fehler")
    public void assertUnknownToken() {
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("verificationView.not_found.title")).setExact(true))).isVisible();
        assertThat(page.getByText(texts.text("verificationView.not_found.text1"), new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(texts.text("verificationView.success.title")).setExact(true))).hasCount(0);
        assertThat(page.getByLabel(texts.text("verificationView.email_field"), new Page.GetByLabelOptions().setExact(true))).isVisible();
    }
}
