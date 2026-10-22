package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.ui.components.AccountSecurityFeedback.Field;
import de.derpeterson.app.validation.UserInputRules;
import java.util.List;
import java.util.Objects;

/** Fordert eine verifizierte Adressänderung an; die bisherige Kontoadresse bleibt erhalten. */
public class AccountEmailChangeDialog extends Dialog {
  private final transient MessageProperties messages;
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span hint = new Span();
  private final EmailField email = new EmailField();
  private final EmailField confirmEmail = new EmailField();
  private final PasswordField password = new PasswordField();
  private final Button close = new Button(VaadinIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancel = new Button("", event -> close());
  private final Button request = new Button();
  private SerializableConsumer<EmailChange> requestAction;

  public AccountEmailChangeDialog(MessageProperties messages) {
    this.messages = messages;
    email.setPattern(UserInputRules.EMAIL_REGEX);
    confirmEmail.setPattern(UserInputRules.EMAIL_REGEX);
    setId("account-email-change-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-email-dialog");
    getThemeNames().add("cosmos");
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    Div heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    Icon icon = VaadinIcon.ENVELOPE.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    Div badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    close.setId("account-email-close");
    close.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Div header = new Div(badge, heading, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);

    email.setId("account-email-new");
    confirmEmail.setId("account-email-confirm");
    password.setId("account-email-password");
    email.setRequiredIndicatorVisible(true);
    confirmEmail.setRequiredIndicatorVisible(true);
    password.setRequiredIndicatorVisible(true);
    // Escape löst nicht zuverlässig einen Blur aus. Auch unbestätigte Eingaben
    // rechtzeitig synchronisieren, damit Schließen sie tatsächlich verwerfen kann.
    email.setValueChangeMode(ValueChangeMode.EAGER);
    confirmEmail.setValueChangeMode(ValueChangeMode.EAGER);
    password.setValueChangeMode(ValueChangeMode.EAGER);
    email.setWidthFull();
    confirmEmail.setWidthFull();
    password.setWidthFull();
    password.setRevealButtonVisible(true);
    password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    Div info = new Div(infoIcon, hint);
    info.addClassName("cosmos-panel-hint");
    Div form = new Div(email, confirmEmail, password, info);
    form.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(form);

    cancel.setId("account-email-cancel");
    cancel.setPrefixComponent(VaadinIcon.CLOSE.create());
    request.setId("account-email-request");
    request.setPrefixComponent(VaadinIcon.CHECK.create());
    request.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    request.addClickListener(event -> submit());
    Div actions = new Div(cancel, request);
    actions.addClassName("cosmos-dialog-actions");
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            clearDraft();
          }
        });
    refreshTexts();
  }

  public void setRequestAction(SerializableConsumer<EmailChange> action) {
    requestAction = Objects.requireNonNull(action);
  }

  private void submit() {
    if (!ValidationHelper.validateRequiredInputs(List.of(email, confirmEmail, password), messages)
        || !ValidationHelper.validateEmailValidInputs(List.of(email, confirmEmail), messages)
        || !ValidationHelper.validateEmailConfirmInputs(email, confirmEmail, messages)) {
      return;
    }
    request.setEnabled(false);
    try {
      Objects.requireNonNull(requestAction, "Email change action required")
          .accept(new EmailChange(email.getValue(), confirmEmail.getValue(), password.getValue()));
      close();
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseSuccessTitle,
              messages::getAccountSecurityEmailRequested,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      Field field = AccountSecurityFeedback.failure(messages, failure);
      if (field == AccountSecurityFeedback.Field.CURRENT_PASSWORD) {
        password.setInvalid(true);
      }
      if (field == AccountSecurityFeedback.Field.EMAIL) {
        email.setInvalid(true);
        confirmEmail.setInvalid(true);
      }
    } finally {
      request.setEnabled(true);
    }
  }

  @Override
  public void open() {
    clearDraft();
    super.open();
  }

  private void clearDraft() {
    email.clear();
    confirmEmail.clear();
    password.clear();
    email.setInvalid(false);
    confirmEmail.setInvalid(false);
    password.setInvalid(false);
  }

  public void refreshTexts() {
    title.setText(messages.getAccountSettingsChangeEmail());
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(messages.getAccountEmailDialogDescription());
    close.setAriaLabel(messages.getAdminUsersCloseDialog());
    email.setLabel(messages.getAccountEmailDialogNewEmail());
    email.setPlaceholder(messages.getAccountEmailPlaceholder());
    confirmEmail.setLabel(messages.getAccountEmailDialogConfirmEmail());
    confirmEmail.setPlaceholder(messages.getAccountConfirmEmailPlaceholder());
    password.setLabel(messages.getAccountEmailDialogPassword());
    password.setPlaceholder(messages.getAccountPasswordPlaceholder());
    hint.setText(messages.getAccountEmailDialogHint());
    cancel.setText(messages.getAdminUsersCancel());
    request.setText(messages.getAccountEmailDialogRequest());
  }
}
