package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Assertions on the actual activation page, including its existing idempotent success contract. */
public final class VerificationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public VerificationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Aktivierungsseite bestätigt die Verifikation des Kontos")
  public void assertActivated() {
    assertThat(browserPage).hasURL(urlPattern(VERIFICATION_ROUTE, "/[0-9a-f-]+"));
    assertThat(browserPage).hasTitle(VERIFICATION_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getVerificationSuccessTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByText(
                uiTexts.getVerificationSuccessText(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Aufbewahrter abgelaufener Link zeigt keinen Aktivierungserfolg")
  public void assertExpired() {
    assertThat(browserPage).hasURL(urlPattern(VERIFICATION_ROUTE, "/[0-9a-f-]+"));
    assertThat(browserPage).hasTitle(VERIFICATION_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getVerificationExpiredTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getVerificationSuccessTitle())
                    .setExact(true)))
        .hasCount(0);
  }

  @Step("Unbekannter Verifikationslink zeigt einen verständlichen Fehler")
  public void assertUnknownToken() {
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getVerificationNotFoundTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByText(
                uiTexts.getVerificationNotFoundText1(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getVerificationSuccessTitle())
                    .setExact(true)))
        .hasCount(0);
    assertThat(
            browserPage.getByLabel(
                uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
  }
}
