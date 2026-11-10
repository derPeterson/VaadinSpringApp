package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

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
  private final H2 dialogTitleHeading = new H2();
  private final Span dialogDescriptionSpan = new Span();
  private final Span passwordChangeHintSpan = new Span();
  private final Span developmentWarningSpan = new Span();
  private transient BooleanSupplier developmentExceptionSupplier = () -> false;
  private final PasswordField currentPasswordField = new PasswordField();
  private transient BooleanSupplier currentPasswordRequiredSupplier = () -> true;
  private final PasswordField confirmPasswordField = new PasswordField();
  private final PasswordField newPasswordField = new PasswordField();
  private final Button closeDialogButton =
      new Button(AppIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancelPasswordChangeButton = new Button("", event -> close());
  private final Button savePasswordButton = new Button();
  private SerializableFunction<PasswordChange, PasswordChanged> changePasswordFunction;

  public AccountPasswordChangeDialog(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    setId("account-password-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-password-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    addClassName(REFERENCE_OVERLAY);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    dialogTitleHeading.addClassName(PANEL_TITLE);
    dialogDescriptionSpan.addClassName(PANEL_DESCRIPTION);

    Div dialogHeadingDiv = new Div(dialogTitleHeading, dialogDescriptionSpan);
    dialogHeadingDiv.addClassName(PANEL_HEADING);

    SvgIcon dialogSvgIcon = AppIcon.KEY.create();
    dialogSvgIcon.getElement().setAttribute("aria-hidden", "true");
    Div dialogIconDiv = new Div(dialogSvgIcon);
    dialogIconDiv.addClassName(PANEL_ICON);
    closeDialogButton.setId("account-password-close");
    closeDialogButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    closeDialogButton.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    Div dialogHeaderDiv = new Div(dialogIconDiv, dialogHeadingDiv, closeDialogButton);
    dialogHeaderDiv.addClassName("account-change-dialog-header");
    getHeader().add(dialogHeaderDiv);

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

    SvgIcon securityInfoSvgIcon = AppIcon.INFO_CIRCLE.create();
    securityInfoSvgIcon.getElement().setAttribute("aria-hidden", "true");
    Div securityInfoDiv = new Div(securityInfoSvgIcon, passwordChangeHintSpan);
    securityInfoDiv.addClassName("cosmos-panel-hint");
    developmentWarningSpan.addClassName(PANEL_DESCRIPTION);
    developmentWarningSpan.setVisible(false);

    Div changeFormDiv =
        new Div(
            newPasswordField,
            confirmPasswordField,
            currentPasswordField,
            securityInfoDiv,
            developmentWarningSpan);
    changeFormDiv.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(changeFormDiv);

    cancelPasswordChangeButton.setId("account-password-cancel");
    cancelPasswordChangeButton.setPrefixComponent(AppIcon.CLOSE.create());
    savePasswordButton.setId("account-password-save");
    savePasswordButton.setPrefixComponent(AppIcon.CHECK.create());
    savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    savePasswordButton.addClickListener(event -> submitPasswordChange());

    Div dialogActionsDiv = new Div(cancelPasswordChangeButton, savePasswordButton);
    dialogActionsDiv.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(dialogActionsDiv);
    getFooter().add(dialogActionsDiv);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        clearDraft();
      }
    });

    refreshTexts();
  }

  public void setChangeAction(SerializableFunction<PasswordChange, PasswordChanged> action) {
    changePasswordFunction = Objects.requireNonNull(action);
  }

  public void setCurrentPasswordRequired(BooleanSupplier required) {
    currentPasswordRequiredSupplier = Objects.requireNonNull(required);
  }

  public void setDevelopmentWarning(BooleanSupplier active) {
    developmentExceptionSupplier = Objects.requireNonNull(active);
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
      Objects.requireNonNull(changePasswordFunction, "Password change action required")
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
    developmentWarningSpan.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentWarningSpan.setVisible(developmentExceptionSupplier.getAsBoolean());
    boolean required = currentPasswordRequiredSupplier.getAsBoolean();
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
    dialogTitleHeading.setText(messageProperties.getAccountSettingsChangePassword());
    getElement().setProperty("ariaLabel", dialogTitleHeading.getText());
    dialogDescriptionSpan.setText(messageProperties.getAccountPasswordDialogDescription());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    currentPasswordField.setLabel(messageProperties.getAccountPasswordDialogCurrentPassword());
    currentPasswordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    confirmPasswordField.setLabel(messageProperties.getAccountPasswordDialogConfirmPassword());
    confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
    newPasswordField.setLabel(messageProperties.getResetPasswordNewPasswordField());
    newPasswordField.setPlaceholder(messageProperties.getAccountNewPasswordPlaceholder());
    newPasswordField.setHelperText(messageProperties.getBaseValidationPasswordInvalidMessage());
    passwordChangeHintSpan.setText(messageProperties.getAccountPasswordDialogHint());
    cancelPasswordChangeButton.setText(messageProperties.getAdminUsersCancel());
    savePasswordButton.setText(messageProperties.getAccountSettingsChangePassword());
  }
}
