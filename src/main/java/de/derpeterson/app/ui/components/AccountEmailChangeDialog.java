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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.MessageProperties;

/** Erster Oberflächenentwurf; fordert noch keine Bestätigung an und verändert kein Konto. */
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

  public AccountEmailChangeDialog(MessageProperties messages) {
    this.messages = messages;
    setId("account-email-change-dialog");
    addClassNames("cosmos-theme", "account-email-dialog");
    getThemeNames().add("cosmos");
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    var heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    var icon = VaadinIcon.ENVELOPE.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    var badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    close.setId("account-email-close");
    close.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    var header = new Div(badge, heading, close);
    header.addClassName("account-email-dialog-header");
    getHeader().add(header);

    email.setId("account-email-new");
    confirmEmail.setId("account-email-confirm");
    password.setId("account-email-password");
    email.setRequiredIndicatorVisible(true);
    confirmEmail.setRequiredIndicatorVisible(true);
    password.setRequiredIndicatorVisible(true);
    email.setWidthFull();
    confirmEmail.setWidthFull();
    password.setWidthFull();
    password.setRevealButtonVisible(true);
    password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    var infoIcon = VaadinIcon.INFO_CIRCLE_O.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    var info = new Div(infoIcon, hint);
    info.addClassName("cosmos-panel-hint");
    var form = new Div(email, confirmEmail, password, info);
    form.addClassNames("cosmos-dialog-form", "account-email-dialog-form");
    add(form);

    cancel.setId("account-email-cancel");
    cancel.setPrefixComponent(VaadinIcon.CLOSE_SMALL.create());
    request.setId("account-email-request");
    request.setPrefixComponent(VaadinIcon.CHECK.create());
    request.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    // Für die Gestaltungsabnahme aktiv. Versand/Validierung folgen im nächsten Schritt.
    var actions = new Div(cancel, request);
    actions.addClassName("cosmos-dialog-actions");
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            email.clear();
            confirmEmail.clear();
            password.clear();
            email.setInvalid(false);
            confirmEmail.setInvalid(false);
            password.setInvalid(false);
          }
        });
    refreshTexts();
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
