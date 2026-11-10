package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;

/** Only the currently open create/edit form, including its native footer. */
public final class AdminUserDialog {
  private final Page browserPage;
  private final UiTexts uiTexts;

  public AdminUserDialog(Page browserPage, UiTexts uiTexts) {
    this.browserPage = browserPage;
    this.uiTexts = uiTexts;
  }

  public Locator root() {
    return browserPage.locator("vaadin-dialog.admin-user-dialog[opened]");
  }

  public Locator firstName() {
    return field(uiTexts.getAdminUsersFieldFirstName());
  }

  public Locator lastName() {
    return field(uiTexts.getAdminUsersFieldLastName());
  }

  public Locator email() {
    return field(uiTexts.getAdminUsersFieldEmail());
  }

  public Locator password() {
    return field(uiTexts.getAdminUsersFieldPassword());
  }

  public Locator confirmPassword() {
    return field(uiTexts.getPublicConfirmPasswordField());
  }

  public Locator saveButton() {
    return button(uiTexts.getAdminUsersSave());
  }

  public Locator deleteButton() {
    return button(uiTexts.getAdminUsersDelete());
  }

  @Step("Gültige Stammdaten und normale Benutzerrolle im echten Formular eingeben")
  public void fillNewUser(String email, String password, boolean enabled, boolean administrator) {
    firstName().fill("Browser");
    lastName().fill("Testuser");
    this.email().fill(email);
    this.password().fill(password);

    assertThat(confirmPassword()).isEnabled();
    confirmPassword().fill(password);
    field(uiTexts.getRegistrationGenderCombobox()).click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.getGenderOther()).setExact(true))
        .click();
    Locator birthday = field(uiTexts.getRegistrationBirthDateField());

    assertThat(birthday).hasAttribute("placeholder", uiTexts.getRegistrationBirthDatePlaceholder());
    birthday.click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    birthday.fill(uiTexts.dateInput(LocalDate.of(1990, 1, 1)));
    // A real focus change commits the birthday; no Enter or popup cancellation.
    firstName().click();

    assertThat(root().locator("vaadin-date-picker")).hasJSProperty("value", "1990-01-01");
    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    check(uiTexts.getAdminUsersFieldEnabled(), enabled);
    setRole(uiTexts.getRoleUser(), true);
    setRole(uiTexts.getRoleAdmin(), administrator);
  }

  @Step("Freigabe im Benutzerformular ändern")
  public void setEnabled(boolean enabled) {
    check(uiTexts.getAdminUsersFieldEnabled(), enabled);
  }

  public Locator enabled() {
    return root()
        .getByRole(
            AriaRole.CHECKBOX,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersFieldEnabled())
                .setExact(true));
  }

  @Step("Administratorrolle im Benutzerformular vergeben oder entziehen")
  public void setAdministrator(boolean selected) {
    setRole(uiTexts.getRoleAdmin(), selected);
  }

  public Locator administrator() {
    return role(uiTexts.getRoleAdmin());
  }

  @Step("Explizite Rolle im Benutzerformular vergeben oder entziehen")
  public void setRole(String translatedRole, boolean selected) {
    Locator roleOptionLocator = role(translatedRole);
    if (selected != "true".equals(roleOptionLocator.getAttribute("aria-selected"))) {
      roleOptionLocator.click();
    }
    assertThat(roleOptionLocator).hasAttribute("aria-selected", Boolean.toString(selected));
    rolesInput().press("Escape");
  }

  public Locator role(String translatedRole) {
    Locator rolesInputLocator = rolesInput();
    if (!"true".equals(rolesInputLocator.getAttribute("aria-expanded"))) {
      rolesInputLocator.click();
    }
    return browserPage.getByRole(
        AriaRole.OPTION, new Page.GetByRoleOptions().setName(translatedRole).setExact(true));
  }

  private Locator rolesInput() {
    return root().locator("vaadin-multi-select-combo-box.admin-user-roles input");
  }

  @Step("Speichern versuchen und erwarteten fachlichen Hinweis prüfen")
  public void expectSaveError(String message) {
    assertThat(saveButton()).isEnabled();
    saveButton().click();

    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(message);
    assertThat(root()).hasCount(1);
    Locator notificationCardLocator =
        browserPage.locator("vaadin-notification-card:not([closing])");
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  @Step("Benutzerformular speichern und dessen tatsächliches Schließen abwarten")
  public void save() {
    closeRoleOptions();

    assertThat(saveButton()).isEnabled();
    saveButton().click();

    assertThat(root()).hasCount(0);
  }

  @Step("Benutzerformular ohne Speicherung abbrechen")
  public void cancel() {
    closeRoleOptions();
    button(uiTexts.getAdminUsersCancel()).click();

    assertThat(root()).hasCount(0);
  }

  @Step("Löschbestätigung aus dem Bearbeitungsformular öffnen")
  public AdminUserDeleteDialog requestDelete() {
    assertThat(deleteButton()).isEnabled();
    deleteButton().click();
    AdminUserDeleteDialog confirmation = new AdminUserDeleteDialog(browserPage, uiTexts);

    assertThat(confirmation.root()).hasCount(1);
    assertThat(
            confirmation
                .root()
                .getByRole(
                    AriaRole.HEADING,
                    new Locator.GetByRoleOptions()
                        .setName(uiTexts.getAdminUsersDeleteDialogTitle())
                        .setExact(true)))
        .isVisible();
    return confirmation;
  }

  private Locator field(String label) {
    return root().getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
  }

  private void closeRoleOptions() {
    if ("true".equals(rolesInput().getAttribute("aria-expanded"))) {
      rolesInput().press("Escape");
    }
  }

  private Locator button(String name) {
    return root()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private void check(String label, boolean checked) {
    root()
        .getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(label).setExact(true))
        .setChecked(checked);
  }
}
