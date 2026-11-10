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
import de.derpeterson.app.ui.components.AccountSecurityFeedback.Field;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Eigene Passwortänderung mit expliziter Validierung erst beim Absenden. */
public class AccountPasswordChangeDialog extends Dialog {
  private final transient MessageProperties messages;
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span hint = new Span();
  private final Span developmentWarning = new Span();
  private transient BooleanSupplier developmentException = () -> false;
  private final PasswordField currentPassword = new PasswordField();
  private transient BooleanSupplier currentPasswordRequired = () -> true;
  private final PasswordField confirmPassword = new PasswordField();
  private final PasswordField newPassword = new PasswordField();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancel = new Button("", event -> close());
  private final Button save = new Button();
  private SerializableFunction<PasswordChange, PasswordChanged> changeAction;

  public AccountPasswordChangeDialog(MessageProperties messages) {
    this.messages = messages;
    setId("account-password-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-password-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    addClassName(REFERENCE_OVERLAY);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    title.addClassName(PANEL_TITLE);
    description.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(title, description);
    heading.addClassName(PANEL_HEADING);
    SvgIcon icon = AppIcon.KEY.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    Div badge = new Div(icon);
    badge.addClassName(PANEL_ICON);
    close.setId("account-password-close");
    close.setIcon(AppIcon.CLOSE_CIRCLE.create());
    close.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Div header = new Div(badge, heading, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);

    currentPassword.setId("account-password-current");
    confirmPassword.setId("account-password-confirm");
    newPassword.setId("account-password-new");
    currentPassword.setRequiredIndicatorVisible(true);
    confirmPassword.setRequiredIndicatorVisible(true);
    newPassword.setRequiredIndicatorVisible(true);
    // Escape löst nicht zuverlässig einen Blur aus. Auch unbestätigte Eingaben
    // rechtzeitig synchronisieren, damit Schließen sie tatsächlich verwerfen kann.
    currentPassword.setValueChangeMode(ValueChangeMode.EAGER);
    confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);
    newPassword.setValueChangeMode(ValueChangeMode.EAGER);
    currentPassword.setWidthFull();
    confirmPassword.setWidthFull();
    newPassword.setWidthFull();
    newPassword.setRevealButtonVisible(true);
    currentPassword.setRevealButtonVisible(true);
    confirmPassword.setRevealButtonVisible(true);
    currentPassword.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    newPassword.setAutocomplete(Autocomplete.NEW_PASSWORD);
    confirmPassword.setAutocomplete(Autocomplete.NEW_PASSWORD);
    SvgIcon infoIcon = AppIcon.INFO_CIRCLE.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    Div info = new Div(infoIcon, hint);
    info.addClassName("cosmos-panel-hint");
    developmentWarning.addClassName(PANEL_DESCRIPTION);
    developmentWarning.setVisible(false);
    Div form = new Div(newPassword, confirmPassword, currentPassword, info, developmentWarning);
    form.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(form);

    cancel.setId("account-password-cancel");
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    save.setId("account-password-save");
    save.setPrefixComponent(AppIcon.CHECK.create());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClickListener(event -> submit());
    Div actions = new Div(cancel, save);
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

  private void submit() {
    if (!ValidationHelper.validateRequiredInputs(
            currentPassword.isVisible()
                ? List.of(newPassword, confirmPassword, currentPassword)
                : List.of(newPassword, confirmPassword),
            messages)
        || !ValidationHelper.validatePasswordSecureInputs(List.of(newPassword), messages)
        || !ValidationHelper.validatePasswordConfirmInputs(
            newPassword, confirmPassword, messages)) {
      return;
    }
    save.setEnabled(false);
    try {
      Objects.requireNonNull(changeAction, "Password change action required")
          .apply(
              new PasswordChange(
                  currentPassword.getValue(), newPassword.getValue(), confirmPassword.getValue()));
      close();
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseSuccessTitle,
              messages::getAccountSecurityPasswordSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      Field field = AccountSecurityFeedback.failure(messages, failure);
      if (field == AccountSecurityFeedback.Field.CURRENT_PASSWORD) {
        currentPassword.setInvalid(true);
      }
      if (field == AccountSecurityFeedback.Field.NEW_PASSWORD) {
        newPassword.setInvalid(true);
      }
    } finally {
      save.setEnabled(true);
    }
  }

  @Override
  public void open() {
    developmentWarning.setText(messages.getTwoFactorDevelopmentWarning());
    developmentWarning.setVisible(developmentException.getAsBoolean());
    boolean required = currentPasswordRequired.getAsBoolean();
    currentPassword.setVisible(required);
    currentPassword.setRequiredIndicatorVisible(required);
    clearDraft();
    super.open();
  }

  private void clearDraft() {
    currentPassword.clear();
    confirmPassword.clear();
    newPassword.clear();
    currentPassword.setInvalid(false);
    confirmPassword.setInvalid(false);
    newPassword.setInvalid(false);
  }

  public void refreshTexts() {
    title.setText(messages.getAccountSettingsChangePassword());
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(messages.getAccountPasswordDialogDescription());
    close.setAriaLabel(messages.getAdminUsersCloseDialog());
    currentPassword.setLabel(messages.getAccountPasswordDialogCurrentPassword());
    currentPassword.setPlaceholder(messages.getAccountPasswordPlaceholder());
    confirmPassword.setLabel(messages.getAccountPasswordDialogConfirmPassword());
    confirmPassword.setPlaceholder(messages.getAccountConfirmPasswordPlaceholder());
    newPassword.setLabel(messages.getResetPasswordNewPasswordField());
    newPassword.setPlaceholder(messages.getAccountNewPasswordPlaceholder());
    newPassword.setHelperText(messages.getBaseValidationPasswordInvalidMessage());
    hint.setText(messages.getAccountPasswordDialogHint());
    cancel.setText(messages.getAdminUsersCancel());
    save.setText(messages.getAccountSettingsChangePassword());
  }
}
