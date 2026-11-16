package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UnsavedChangesPrompt;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
public class DurationCustomFieldBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void inactivityDurationPreservesPresentationAndItsSavedValue(int width) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, width == 390 ? 844 : 900);
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.TWO_FACTOR);
    assertThat(amountInput()).isDisabled();
    assertThat(amountInput()).hasValue("12");
    capture("Ausgeschaltet");
    automaticSwitch().setChecked(true);
    assertThat(amountInput()).isEnabled();
    assertThat(amountInput()).hasAccessibleName(uiTexts.getTwoFactorInactivityDuration());
    capture("Eingeschaltet");
    durationField().locator("vaadin-select").click();
    Locator unitOptions = browserPage.getByRole(AriaRole.OPTION);
    assertThat(unitOptions)
        .hasText(
            new String[] {
              uiTexts.getAdminConfigDurationUnitMinutes(),
              uiTexts.getAdminConfigDurationUnitHours(),
              uiTexts.getAdminConfigDurationUnitDays()
            });
    capture("Einheiten");
    UiActions.completeValueChange(
        browserPage,
        () ->
            unitOptions
                .filter(
                    new Locator.FilterOptions()
                        .setHasText(uiTexts.getAdminConfigDurationUnitDays()))
                .click());
    assertThat(amountInput()).hasValue("12");
    UiActions.completeValueChange(browserPage, () -> amountInput().fill("2"));
    selectHours();
    assertThat(amountInput()).hasValue("2");
    savePolicy();
    browserPage.reload();
    assertThat(amountInput()).hasValue("2");
    assertThat(automaticSwitch()).hasJSProperty("checked", true);
    assertThat(durationField().locator("vaadin-select-value-button > vaadin-select-item"))
        .hasText(uiTexts.getAdminConfigDurationUnitHours());
    UiActions.completeValueChange(browserPage, () -> amountInput().clear());
    browserPage.locator("#two-factor-save").click();
    closeNotice(uiTexts.getTwoFactorFailed());
    assertThat(amountInput()).hasValue("");
    browserPage.locator("#two-factor-reload").click();
    UnsavedChangesPrompt unsavedChangesPrompt = new UnsavedChangesPrompt(browserPage, uiTexts);
    unsavedChangesPrompt.keepEditing();
    assertThat(amountInput()).hasValue("");
    browserPage.locator("#two-factor-reload").click();
    unsavedChangesPrompt.discard();
    closeNotice(uiTexts.getAdminConfigReloadSuccess());
    assertThat(amountInput()).hasValue("2");
    UiActions.completeValueChange(browserPage, () -> amountInput().fill("12"));
    automaticSwitch().setChecked(false);
    savePolicy();
    assertThat(amountInput()).isDisabled();
  }

  private void savePolicy() {
    browserPage.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        browserPage, TwoFactorPage.nextCode(browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(
        browserPage, () -> browserPage.locator("#two-factor-action-confirm").click());
    browserPage.waitForCondition(() ->
        URI.create(browserPage.url()).getPath().equals("/two-factor")
            || browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorSaved()))
                .isVisible());
    String configurationLocation = "/admin?section=configuration&group=two_factor";
    int viewportWidth = browserPage.viewportSize().width;
    int viewportHeight = browserPage.viewportSize().height;
    browserPage = browserContext.newPage();
    browserPage.setViewportSize(viewportWidth, viewportHeight);
    browserPage.navigate(configurationLocation);
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    if (!browserPage.url().endsWith(configurationLocation)) {
      browserPage.navigate(configurationLocation);
    }
    assertThat(browserPage.locator("#two-factor-save")).isVisible();
  }

  private Locator durationField() {
    return browserPage.locator("#two-factor-inactivity-duration");
  }

  private Locator amountInput() {
    return durationField().locator("vaadin-integer-field input");
  }

  private Locator automaticSwitch() {
    return browserPage.getByRole(
        AriaRole.SWITCH,
        new Page.GetByRoleOptions()
            .setName(uiTexts.getTwoFactorInactivityEnabled())
            .setExact(true));
  }

  private void selectHours() {
    durationField().locator("vaadin-select-value-button").click();
    UiActions.completeValueChange(
        browserPage,
        () ->
            browserPage
                .getByRole(
                    AriaRole.OPTION,
                    new Page.GetByRoleOptions()
                        .setName(uiTexts.getAdminConfigDurationUnitHours())
                        .setExact(true))
                .click());
    assertThat(durationField().locator("vaadin-select-value-button > vaadin-select-item"))
        .hasText(uiTexts.getAdminConfigDurationUnitHours());
  }

  private void closeNotice(String expectedText) {
    Locator notice = browserPage.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).containsText(expectedText);
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(browserPage.locator("vaadin-notification-card")).hasCount(0);
  }

  private void capture(String state) {
    Locator section = browserPage.locator(".mfa-inactivity-section");
    section.scrollIntoViewIfNeeded();
    browserPage.mouse().move(0, 0);
    String name = "Zeitfeld " + state + " " + browserPage.viewportSize().width;
    Allure.attachment(
        name + " Messwerte",
        "application/json",
        (String)
            section.evaluate(
                """
                section => JSON.stringify([...section.querySelectorAll(
                  '#two-factor-inactivity-label,vaadin-integer-field,vaadin-select')].map(element => {
                    const bounds = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {width:bounds.width,height:bounds.height,font:style.font,color:style.color};
                  }))
                """));
    Allure.attachment(
        name,
        "image/png",
        new ByteArrayInputStream(
            section.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
    Allure.attachment(
        name + " Seite",
        "image/png",
        new ByteArrayInputStream(
            browserPage.screenshot(
                new Page.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
