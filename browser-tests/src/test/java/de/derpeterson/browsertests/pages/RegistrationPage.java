package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.time.ZoneId;

/** Visible registration form; no service calls or database preparation. */
public final class RegistrationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public RegistrationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Registrierungsformular öffnen")
  public void open() {
    browserPage.navigate(path(REGISTRATION_ROUTE));

    assertThat(browserPage).hasTitle(REGISTRATION_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(uiTexts.getRegistrationTitle()).setExact(true)))
        .isVisible();
    assertThat(field(uiTexts.getRegistrationBirthDateField()))
        .hasAttribute("placeholder", uiTexts.getRegistrationBirthDatePlaceholder());
  }

  @Step("Pflichtfelder mit gültigen Testangaben ausfüllen")
  public void fillValid(String email, String password) {
    fillOtherFields(email, password);
    Locator birthDate = field(uiTexts.getRegistrationBirthDateField());
    String birthDateInputText = uiTexts.dateInput(LocalDate.of(1990, 1, 1));
    birthDate.fill(birthDateInputText);

    assertThat(birthDate).hasValue(birthDateInputText);
    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    // The submit click commits the input naturally; no Enter, Escape or artificial blur.
  }

  private void fillOtherFields(String email, String password) {
    field(uiTexts.getRegistrationFirstNameField()).fill("Browser");
    field(uiTexts.getRegistrationLastNameField()).fill("Testuser");
    setEmails(email, email);
    setPasswords(password, password);
    field(uiTexts.getRegistrationGenderCombobox()).click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getGenderOther()).setExact(true))
        .click();
  }

  @Step("Geburtstag ohne Texteingabe über Kalender-Icon auswählen und erneut öffnen")
  public LocalDate fillValidUsingCalendar(String email, String password) {
    fillOtherFields(email, password);
    Locator birthDatePickerLocator = browserPage.locator("vaadin-date-picker");
    field(uiTexts.getRegistrationBirthDateField()).click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    birthDatePickerLocator.locator("[part~='toggle-button']").click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).isVisible();
    Locator focusedDayLocator =
        browserPage.getByRole(AriaRole.GRIDCELL).and(browserPage.locator("[part~='focused']"));

    assertThat(focusedDayLocator).hasCount(1);
    // One ordinary calendar navigation step chooses yesterday, including month/year boundaries.
    LocalDate selected = LocalDate.now(ZoneId.of("Europe/Berlin")).minusDays(1);
    focusedDayLocator.press("ArrowLeft");

    assertThat(focusedDayLocator).hasCount(1);
    clickSettledCalendarDay(focusedDayLocator);

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    assertBirthDateCommitted(selected);
    birthDatePickerLocator.locator("[part~='toggle-button']").click();
    Locator selectedDayLocator =
        browserPage.getByRole(AriaRole.GRIDCELL).and(browserPage.locator("[aria-selected='true']"));

    assertThat(selectedDayLocator).hasCount(1);
    clickSettledCalendarDay(selectedDayLocator);

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    assertBirthDateCommitted(selected);
    return selected;
  }

  private void clickSettledCalendarDay(Locator day) {
    day.scrollIntoViewIfNeeded();
    double[] previousDayCoordinates = {Double.NaN, Double.NaN};
    long[] stableSinceNanos = {System.nanoTime()};
    // Die virtuelle mobile Monatsliste scrollt nach Pfeiltasten noch nach. Erst eine
    // über 300 ms stabile sichtbare Position anklicken; keine erzwungenen Events oder Werte.
    browserPage.waitForCondition(() -> {
      BoundingBox dayBoundingBox = day.boundingBox();
      if (dayBoundingBox == null) {
        return false;
      }
      if (dayBoundingBox.x != previousDayCoordinates[0]
          || dayBoundingBox.y != previousDayCoordinates[1]) {
        previousDayCoordinates[0] = dayBoundingBox.x;
        previousDayCoordinates[1] = dayBoundingBox.y;
        stableSinceNanos[0] = System.nanoTime();
        return false;
      }
      return System.nanoTime() - stableSinceNanos[0] >= java.time.Duration.ofMillis(300).toNanos();
    });
    day.click();
  }

  @Step("Tatsächlich übernommenen Geburtstag prüfen")
  public void assertBirthDateCommitted(LocalDate expected) {
    assertThat(browserPage.locator("vaadin-date-picker"))
        .hasJSProperty("value", expected.toString());
  }

  @Step("E-Mail und E-Mail-Bestätigung eingeben")
  public void setEmails(String emailAddress, String confirmationEmailAddress) {
    field(uiTexts.getPublicEmailField()).fill(emailAddress);
    Locator confirmEmailFieldLocator = browserPage.locator("vaadin-email-field").nth(1);

    assertThat(confirmEmailFieldLocator)
        .hasJSProperty("label", uiTexts.getPublicConfirmEmailField());
    confirmEmailFieldLocator.locator("input").fill(confirmationEmailAddress);
  }

  @Step("Passwort und Passwortbestätigung eingeben")
  public void setPasswords(String testPassword, String confirmationPassword) {
    field(uiTexts.getRegistrationPasswordField()).fill(testPassword);
    Locator confirmPasswordFieldLocator = browserPage.locator("vaadin-password-field").nth(1);

    assertThat(confirmPasswordFieldLocator)
        .hasJSProperty("label", uiTexts.getPublicConfirmPasswordField());
    confirmPasswordFieldLocator.locator("input").fill(confirmationPassword);
  }

  @Step("Registrierung über den Button absenden")
  public void submit() {
    browserPage
        .locator(".public-panel")
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getBaseRegistrationButton())
                .setExact(true))
        .click();
  }

  @Step("Bestätigung nennt die eigene Testadresse und fordert zur Verifikation auf")
  public void assertSubmitted(String email) {
    assertThat(browserPage).hasURL(urlPattern(REGISTRATION_ROUTE));
    Locator notificationCardLocator =
        browserPage
            .locator("vaadin-notification-card")
            .filter(new Locator.FilterOptions().setHasText(email));

    assertThat(notificationCardLocator)
        .containsText(
            uiTexts.visibleText(browserPage, uiTexts.getRegistrationSuccessMessage(email)));
  }

  @Step("Pflichtfeldfehler und markierte Felder prüfen")
  public void assertRequiredError() {
    assertError(uiTexts.getBaseValidationRequiredMessage());

    assertThat(browserPage.locator("vaadin-text-field[invalid]")).hasCount(2);
    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(2);
    assertThat(browserPage.locator("vaadin-password-field[invalid]")).hasCount(2);
    assertThat(browserPage.locator("vaadin-combo-box[invalid]")).hasCount(0);
    assertThat(browserPage.locator("vaadin-date-picker[invalid]")).hasCount(0);
  }

  @Step("Ungültige E-Mail verständlich anzeigen und beide E-Mail-Felder markieren")
  public void assertEmailError() {
    assertError(uiTexts.getBaseValidationEmailInvalidMessage());

    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(2);
  }

  @Step("Abweichende Passwortbestätigung verständlich anzeigen und Felder markieren")
  public void assertPasswordConfirmationError() {
    assertError(uiTexts.getBaseValidationPasswordConfirmMessage());

    assertThat(browserPage.locator("vaadin-password-field[invalid]")).hasCount(2);
  }

  @Step("Einzelne Registrierungsangabe ändern")
  public void change(String label, String value) {
    field(label).fill(value);
    field(label).press("Tab");
  }

  @Step("Verständlichen Registrierungsfehler ohne Erfolg prüfen")
  public void assertValidationError(String message) {
    Locator notificationCardLocator =
        browserPage.locator("vaadin-notification-card:not([closing])");

    assertThat(notificationCardLocator).hasCount(1);
    assertThat(notificationCardLocator).containsText(uiTexts.visibleText(browserPage, message));
    assertThat(notificationCardLocator).containsText(uiTexts.getBaseFailedTitle());
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  private Locator field(String label) {
    return browserPage.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
  }

  private void assertError(String message) {
    assertThat(browserPage).hasURL(urlPattern(REGISTRATION_ROUTE));
    assertThat(browserPage.getByText(message, new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage
                .locator("vaadin-notification-card")
                .filter(
                    new Locator.FilterOptions()
                        .setHas(
                            browserPage.getByText(
                                uiTexts.getBaseSuccessTitle(),
                                new Page.GetByTextOptions().setExact(true)))))
        .hasCount(0);
  }
}
