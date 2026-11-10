package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.ui.components.AccountSecurityFeedback.RejectedSecurityInput;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Eigene Passwortänderung mit expliziter Validierung erst beim Absenden. */
public class AccountPasswordChangeDialog extends Dialog {
  private final transient MessageProperties messageProperties;
  private final H2 dialogTitle = new H2();
  private final Span dialogDescription = new Span();
  private final Span passwordChangeHint = new Span();
  private final Span developmentWarning = new Span();
  private transient BooleanSupplier developmentException = () -> false;
  private final PasswordField currentPasswordField = new PasswordField();
  private transient BooleanSupplier currentPasswordRequired = () -> true;
  private final PasswordField confirmPasswordField = new PasswordField();
  private final PasswordField newPasswordField = new PasswordField();
  private final Button closeDialogButton =
      new Button(AppIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancelPasswordChangeButton = new Button("", event -> close());
  private final Button savePasswordButton = new Button();
  private SerializableFunction<PasswordChange, PasswordChanged> changeAction;

  public AccountPasswordChangeDialog(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    setId("account-password-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-password-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    addClassName(REFERENCE_OVERLAY);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    dialogTitle.addClassName(PANEL_TITLE);
    dialogDescription.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(dialogTitle, dialogDescription);
    heading.addClassName(PANEL_HEADING);
    SvgIcon icon = AppIcon.KEY.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    Div badge = new Div(icon);
    badge.addClassName(PANEL_ICON);
    closeDialogButton.setId("account-password-close");
    closeDialogButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    closeDialogButton.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Div header = new Div(badge, heading, closeDialogButton);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);

    currentPasswordField.setId("account-password-current");
    confirmPasswordField.setId("account-password-confirm");
    newPasswordField.setId("account-password-new");
    currentPasswordField.setRequiredIndicatorVisible(true);
    confirmPasswordField.setRequiredIndicatorVisible(true);
    newPasswordField.setRequiredIndicatorVisible(true);
    // Escape löst nicht zuverlässig einen Blur aus. Auch unbestätigte Eingaben
    // rechtzeitig synchronisieren, damit Schließen sie tatsächlich verwerfen kann.
    currentPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
    confirmPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
    newPasswordField.setValueChangeMode(ValueChangeMode.EAGER);
    currentPasswordField.setWidthFull();
    confirmPasswordField.setWidthFull();
    newPasswordField.setWidthFull();
    newPasswordField.setRevealButtonVisible(true);
    currentPasswordField.setRevealButtonVisible(true);
    confirmPasswordField.setRevealButtonVisible(true);
    currentPasswordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    newPasswordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
    confirmPasswordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
    SvgIcon infoIcon = AppIcon.INFO_CIRCLE.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    Div info = new Div(infoIcon, passwordChangeHint);
    info.addClassName("cosmos-panel-hint");
    developmentWarning.addClassName(PANEL_DESCRIPTION);
    developmentWarning.setVisible(false);
    Div form =
        new Div(
            newPasswordField, confirmPasswordField, currentPasswordField, info, developmentWarning);
    form.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(form);

    cancelPasswordChangeButton.setId("account-password-cancel");
    cancelPasswordChangeButton.setPrefixComponent(AppIcon.CLOSE.create());
    savePasswordButton.setId("account-password-save");
    savePasswordButton.setPrefixComponent(AppIcon.CHECK.create());
    savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    savePasswordButton.addClickListener(event -> submitPasswordChange());
    Div actions = new Div(cancelPasswordChangeButton, savePasswordButton);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    getFooter().add(actions);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        clearDraft();
      }
    });
    refreshTexts();
  }

  public void setChangeAction(SerializableFunction<PasswordChange, PasswordChanged> action) {
    changeAction = Objects.requireNonNull(action);
  }

  public void setCurrentPasswordRequired(BooleanSupplier required) {
    currentPasswordRequired = Objects.requireNonNull(required);
  }

  public void setDevelopmentWarning(BooleanSupplier active) {
    developmentException = Objects.requireNonNull(active);
  }

  private void submitPasswordChange() {
    if (!ValidationHelper.validateRequiredInputs(
            currentPasswordField.isVisible()
                ? List.of(newPasswordField, confirmPasswordField, currentPasswordField)
                : List.of(newPasswordField, confirmPasswordField),
            messageProperties)
        || !ValidationHelper.validatePasswordSecureInputs(
            List.of(newPasswordField), messageProperties)
        || !ValidationHelper.validatePasswordConfirmInputs(
            newPasswordField, confirmPasswordField, messageProperties)) {
      return;
    }
    savePasswordButton.setEnabled(false);
    try {
      Objects.requireNonNull(changeAction, "Password change action required")
          .apply(
              new PasswordChange(
                  currentPasswordField.getValue(),
                  newPasswordField.getValue(),
                  confirmPasswordField.getValue()));
      close();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAccountSecurityPasswordSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      RejectedSecurityInput rejectedSecurityInput =
          AccountSecurityFeedback.showSecurityActionFailure(messageProperties, failure);
      if (rejectedSecurityInput == AccountSecurityFeedback.RejectedSecurityInput.CURRENT_PASSWORD) {
        currentPasswordField.setInvalid(true);
      }
      if (rejectedSecurityInput == AccountSecurityFeedback.RejectedSecurityInput.NEW_PASSWORD) {
        newPasswordField.setInvalid(true);
      }
    } finally {
      savePasswordButton.setEnabled(true);
    }
  }

  @Override
  public void open() {
    developmentWarning.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentWarning.setVisible(developmentException.getAsBoolean());
    boolean required = currentPasswordRequired.getAsBoolean();
    currentPasswordField.setVisible(required);
    currentPasswordField.setRequiredIndicatorVisible(required);
    clearDraft();
    super.open();
  }

  private void clearDraft() {
    currentPasswordField.clear();
    confirmPasswordField.clear();
    newPasswordField.clear();
    currentPasswordField.setInvalid(false);
    confirmPasswordField.setInvalid(false);
    newPasswordField.setInvalid(false);
  }

  public void refreshTexts() {
    dialogTitle.setText(messageProperties.getAccountSettingsChangePassword());
    getElement().setProperty("ariaLabel", dialogTitle.getText());
    dialogDescription.setText(messageProperties.getAccountPasswordDialogDescription());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    currentPasswordField.setLabel(messageProperties.getAccountPasswordDialogCurrentPassword());
    currentPasswordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    confirmPasswordField.setLabel(messageProperties.getAccountPasswordDialogConfirmPassword());
    confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
    newPasswordField.setLabel(messageProperties.getResetPasswordNewPasswordField());
    newPasswordField.setPlaceholder(messageProperties.getAccountNewPasswordPlaceholder());
    newPasswordField.setHelperText(messageProperties.getBaseValidationPasswordInvalidMessage());
    passwordChangeHint.setText(messageProperties.getAccountPasswordDialogHint());
    cancelPasswordChangeButton.setText(messageProperties.getAdminUsersCancel());
    savePasswordButton.setText(messageProperties.getAccountSettingsChangePassword());
  }
}
