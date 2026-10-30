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
  private final Page page;
  private final UiTexts texts;

  public AdminUserDialog(Page page, UiTexts texts) {
    this.page = page;
    this.texts = texts;
  }

  public Locator root() {
    return page.locator("vaadin-dialog.admin-user-dialog[opened]");
  }

  public Locator firstName() {
    return field(texts.getAdminUsersFieldFirstName());
  }

  public Locator lastName() {
    return field(texts.getAdminUsersFieldLastName());
  }

  public Locator email() {
    return field(texts.getAdminUsersFieldEmail());
  }

  public Locator password() {
    return field(texts.getAdminUsersFieldPassword());
  }

  public Locator confirmPassword() {
    return field(texts.getPublicConfirmPasswordField());
  }

  public Locator saveButton() {
    return button(texts.getAdminUsersSave());
  }

  public Locator deleteButton() {
    return button(texts.getAdminUsersDelete());
  }

  @Step("Gültige Stammdaten und normale Benutzerrolle im echten Formular eingeben")
  public void fillNewUser(String email, String password, boolean enabled, boolean administrator) {
    firstName().fill("Browser");
    lastName().fill("Testuser");
    this.email().fill(email);
    this.password().fill(password);
    assertThat(confirmPassword()).isEnabled();
    confirmPassword().fill(password);
    field(texts.getRegistrationGenderCombobox()).click();
    page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(texts.getGenderOther()).setExact(true))
        .click();
    Locator birthday = field(texts.getRegistrationBirthDateField());
    assertThat(birthday).hasAttribute("placeholder", texts.getRegistrationBirthDatePlaceholder());
    birthday.click();
    assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    birthday.fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
    // A real focus change commits the birthday; no Enter or popup cancellation.
    firstName().click();
    assertThat(root().locator("vaadin-date-picker")).hasJSProperty("value", "1990-01-01");
    assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    check(texts.getAdminUsersFieldEnabled(), enabled);
    setRole(texts.getRoleUser(), true);
    setRole(texts.getRoleAdmin(), administrator);
  }

  @Step("Freigabe im Benutzerformular ändern")
  public void setEnabled(boolean enabled) {
    check(texts.getAdminUsersFieldEnabled(), enabled);
  }

  public Locator enabled() {
    return root()
        .getByRole(
            AriaRole.CHECKBOX,
            new Locator.GetByRoleOptions()
                .setName(texts.getAdminUsersFieldEnabled())
                .setExact(true));
  }

  @Step("Administratorrolle im Benutzerformular vergeben oder entziehen")
  public void setAdministrator(boolean selected) {
    setRole(texts.getRoleAdmin(), selected);
  }

  public Locator administrator() {
    return role(texts.getRoleAdmin());
  }

  @Step("Explizite Rolle im Benutzerformular vergeben oder entziehen")
  public void setRole(String translatedRole, boolean selected) {
    Locator option = role(translatedRole);
    if (selected != "true".equals(option.getAttribute("aria-selected"))) {
      option.click();
    }
    assertThat(option).hasAttribute("aria-selected", Boolean.toString(selected));
    rolesInput().press("Escape");
  }

  public Locator role(String translatedRole) {
    Locator input = rolesInput();
    if (!"true".equals(input.getAttribute("aria-expanded"))) {
      input.click();
    }
    return page.getByRole(
        AriaRole.OPTION, new Page.GetByRoleOptions().setName(translatedRole).setExact(true));
  }

  private Locator rolesInput() {
    return root().locator("vaadin-multi-select-combo-box.admin-user-roles input");
  }

  @Step("Speichern versuchen und erwarteten fachlichen Hinweis prüfen")
  public void expectSaveError(String message) {
    assertThat(saveButton()).isEnabled();
    saveButton().click();
    assertThat(page.locator("vaadin-notification-card:not([closing])")).containsText(message);
    assertThat(root()).hasCount(1);
    Locator notice = page.locator("vaadin-notification-card:not([closing])");
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
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
    button(texts.getAdminUsersCancel()).click();
    assertThat(root()).hasCount(0);
  }

  @Step("Löschbestätigung aus dem Bearbeitungsformular öffnen")
  public AdminUserDeleteDialog requestDelete() {
    assertThat(deleteButton()).isEnabled();
    deleteButton().click();
    AdminUserDeleteDialog confirmation = new AdminUserDeleteDialog(page, texts);
    assertThat(confirmation.root()).hasCount(1);
    assertThat(
            confirmation
                .root()
                .getByRole(
                    AriaRole.HEADING,
                    new Locator.GetByRoleOptions()
                        .setName(texts.getAdminUsersDeleteDialogTitle())
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
