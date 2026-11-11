package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;

/** The four visible token settings; no database or service access. */
public final class TokenConfigurationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public TokenConfigurationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Token-Laufzeiten über die Adminnavigation öffnen")
  public void open() {
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.TOKENS);

    assertThat(findActionButtonLocator(uiTexts.getAdminConfigSave())).isVisible();
  }

  public Locator field(String fieldLabelText) {
    return new AdminConfigurationPage(browserPage)
        .duration(fieldLabelText)
        .locator("vaadin-big-decimal-field");
  }

  public String value(String fieldLabelText) {
    BigDecimal tokenDurationSeconds =
        new AdminConfigurationPage(browserPage).seconds(fieldLabelText);
    BigInteger[] secondsAndNanoseconds =
        tokenDurationSeconds
            .movePointRight(9)
            .toBigIntegerExact()
            .divideAndRemainder(BigInteger.valueOf(1_000_000_000));

    return Duration.ofSeconds(
            secondsAndNanoseconds[0].longValueExact(), secondsAndNanoseconds[1].longValueExact())
        .toString();
  }

  @Step("Dauer als Zahl und Einheit eingeben")
  public void enter(String fieldLabelText, String tokenDurationText) {
    Duration tokenDuration =
        tokenDurationText.startsWith("P")
            ? Duration.parse(tokenDurationText)
            : Duration.ofMinutes(
                Long.parseLong(tokenDurationText.substring(0, tokenDurationText.length() - 1)));
    BigDecimal tokenDurationSeconds =
        BigDecimal.valueOf(tokenDuration.getSeconds())
            .add(BigDecimal.valueOf(tokenDuration.getNano(), 9));

    new AdminConfigurationPage(browserPage).enterSeconds(fieldLabelText, tokenDurationSeconds);
  }

  @Step("Dauer ablehnen und lokalisierten Feldfehler prüfen")
  public void reject(String fieldLabelText, String expectedErrorText) {
    findActionButtonLocator(uiTexts.getAdminConfigSave()).click();

    assertThat(field(fieldLabelText)).hasAttribute("invalid", "");
    assertThat(field(fieldLabelText)).hasJSProperty("errorMessage", expectedErrorText);
    assertThat(notificationCardLocator())
        .containsText(uiTexts.getAdminConfigValidationInvalidFields());
    dismissNotificationCard();
  }

  @Step("Konfiguration speichern")
  public void save() {
    findActionButtonLocator(uiTexts.getAdminConfigSave()).click();

    assertThat(notificationCardLocator()).containsText(uiTexts.getAdminConfigSaveSuccess());
    dismissNotificationCard();
  }

  @Step("Gespeicherte Konfiguration erneut laden")
  public void reload() {
    Locator reloadSuccessNoticeLocator =
        notificationCardLocator()
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigReloadSuccess()));

    UiActions.complete(browserPage, findActionButtonLocator(uiTexts.getAdminConfigReload())::click);
    new UnsavedChangesPrompt(browserPage, uiTexts)
        .discardAfterActionIfNeeded(() -> reloadSuccessNoticeLocator.count() > 0);

    assertThat(notificationCardLocator()).containsText(uiTexts.getAdminConfigReloadSuccess());
    dismissNotificationCard();
  }

  @Step("Mindestdauer und Erklärung im neuen Klick-Tooltip prüfen")
  public void explain(
      String fieldLabelText, long minimumDurationMinutes, String helpDescriptionText) {
    new AdminConfigurationPage(browserPage)
        .explain(
            fieldLabelText,
            helpDescriptionText,
            uiTexts.getAdminConfigValidationDurationMinimum(minimumDurationMinutes));
  }

  private Locator notificationCardLocator() {
    return browserPage.locator("vaadin-notification-card:not([closing])");
  }

  private void dismissNotificationCard() {
    notificationCardLocator().getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator()).not().isVisible();
  }

  private Locator findActionButtonLocator(String buttonLabelText) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonLabelText).setExact(true));
  }
}
