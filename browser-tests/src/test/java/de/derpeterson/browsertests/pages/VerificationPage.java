package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Assertions on the actual activation page, including its existing idempotent success contract. */
public final class VerificationPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public VerificationPage(Page page) {
    this.page = page;
  }

  @Step("Aktivierungsseite bestätigt die Verifikation des Kontos")
  public void assertActivated() {
    assertThat(page).hasURL(urlPattern(VERIFICATION_ROUTE, "/[0-9a-f-]+"));
    assertThat(page).hasTitle(VERIFICATION_PAGE_TITEL);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getVerificationSuccessTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            page.getByText(
                texts.getVerificationSuccessText(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Aufbewahrter abgelaufener Link zeigt keinen Aktivierungserfolg")
  public void assertExpired() {
    assertThat(page).hasURL(urlPattern(VERIFICATION_ROUTE, "/[0-9a-f-]+"));
    assertThat(page).hasTitle(VERIFICATION_PAGE_TITEL);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getVerificationExpiredTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getVerificationSuccessTitle())
                    .setExact(true)))
        .hasCount(0);
  }

  @Step("Unbekannter Verifikationslink zeigt einen verständlichen Fehler")
  public void assertUnknownToken() {
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getVerificationNotFoundTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            page.getByText(
                texts.getVerificationNotFoundText1(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getVerificationSuccessTitle())
                    .setExact(true)))
        .hasCount(0);
    assertThat(
            page.getByLabel(
                texts.getVerificationEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
  }
}
