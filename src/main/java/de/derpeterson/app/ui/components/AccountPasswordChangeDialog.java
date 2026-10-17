package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import java.util.List;
import java.util.Objects;

/** Eigene Passwortänderung mit expliziter Validierung erst beim Absenden. */
public class AccountPasswordChangeDialog extends Dialog {
  private final transient MessageProperties messages;
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span hint = new Span();
  private final PasswordField currentPassword = new PasswordField();
  private final PasswordField confirmPassword = new PasswordField();
  private final PasswordField newPassword = new PasswordField();
  private final Button close = new Button(VaadinIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancel = new Button("", event -> close());
  private final Button save = new Button();
  private SerializableFunction<PasswordChange, PasswordChanged> changeAction;

  public AccountPasswordChangeDialog(MessageProperties messages) {
    this.messages = messages;
    setId("account-password-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-password-dialog");
    getThemeNames().add("cosmos");
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    var heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    var icon = VaadinIcon.LOCK.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    var badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    close.setId("account-password-close");
    close.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    var header = new Div(badge, heading, close);
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
    var infoIcon = VaadinIcon.INFO_CIRCLE_O.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    var info = new Div(infoIcon, hint);
    info.addClassName("cosmos-panel-hint");
    var form = new Div(newPassword, confirmPassword, currentPassword, info);
    form.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(form);

    cancel.setId("account-password-cancel");
    cancel.setPrefixComponent(VaadinIcon.CLOSE_SMALL.create());
    save.setId("account-password-save");
    save.setPrefixComponent(VaadinIcon.CHECK.create());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClickListener(event -> submit());
    var actions = new Div(cancel, save);
    actions.addClassName("cosmos-dialog-actions");
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) clearDraft();
        });
    refreshTexts();
  }

  public void setChangeAction(SerializableFunction<PasswordChange, PasswordChanged> action) {
    changeAction = Objects.requireNonNull(action);
  }

  private void submit() {
    if (!ValidationHelper.validateRequiredInputs(
            List.of(newPassword, confirmPassword, currentPassword), messages)
        || !ValidationHelper.validatePasswordSecureInputs(List.of(newPassword), messages)
        || !ValidationHelper.validatePasswordConfirmInputs(newPassword, confirmPassword, messages))
      return;
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
      var field = AccountSecurityFeedback.failure(messages, failure);
      if (field == AccountSecurityFeedback.Field.CURRENT_PASSWORD) currentPassword.setInvalid(true);
      if (field == AccountSecurityFeedback.Field.NEW_PASSWORD) newPassword.setInvalid(true);
    } finally {
      save.setEnabled(true);
    }
  }

  @Override
  public void open() {
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
    hint.setText(messages.getAccountPasswordDialogHint());
    cancel.setText(messages.getAdminUsersCancel());
    save.setText(messages.getAccountSettingsChangePassword());
  }
}
