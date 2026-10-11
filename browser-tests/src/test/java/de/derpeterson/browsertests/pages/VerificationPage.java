package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertions on the actual activation page, including its existing idempotent success contract. */
public final class VerificationPage {
    private final Page page;

    public VerificationPage(Page page) { this.page = page; }

    @Step("Aktivierungsseite bestätigt die Verifikation des Kontos")
    public void assertActivated() {
        assertThat(page).hasURL(Pattern.compile(".*/verification/[0-9a-f-]+$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Verifiziert!|Verified!)$")))).isVisible();
        assertThat(page.getByText(Pattern.compile("^(Dein Account wurde erfolgreich aktiviert\\.|Your account has been successfully activated\\.)$"))).isVisible();
    }

    @Step("Unbekannter Verifikationslink zeigt einen verständlichen Fehler")
    public void assertUnknownToken() {
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Fehlgeschlagen!|Failed!)$")))).isVisible();
        assertThat(page.getByText(Pattern.compile("^(Verifizierungsschlüssel existiert nicht\\.|Verification token does not exist\\.)$"))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Verifiziert!|Verified!)$")))).hasCount(0);
        assertThat(page.getByLabel(Pattern.compile("^(E-Mail|Email)$"))).isVisible();
    }
}
