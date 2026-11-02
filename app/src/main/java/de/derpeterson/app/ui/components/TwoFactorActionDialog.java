package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

/** Gemeinsamer nativer Bestätigungsdialog für Faktorverwaltung und Sicherheitskonfiguration. */
@StyleSheet("custom-theme/cosmos-account.css")
public final class TwoFactorActionDialog extends Dialog {
  private final transient MessageProperties texts;
  private final transient TwoFactorAuthorization authorization;
  private final transient HttpServletRequest request;
  private final TextField code = new TextField();
  private final H2 title = new H2();
  private final Span explanation = new Span();
  private final Button cancel = new Button();
  private final Button confirm = new Button();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create());
  private TwoFactorAction purpose;
  private SerializableRunnable action;

  public TwoFactorActionDialog(
      MessageProperties texts, TwoFactorAuthorization authorization, HttpServletRequest request) {
    this.texts = texts;
    this.authorization = authorization;
    this.request = request;
    setId("two-factor-action-dialog");
    addClassNames("cosmos-theme", CosmosClasses.REFERENCE_OVERLAY, "account-change-dialog");
    getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    title.addClassName(CosmosClasses.PANEL_TITLE);
    Div icon = new Div(AppIcon.LOCK.create());
    icon.addClassName(CosmosClasses.PANEL_ICON);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(event -> close());
    Div header = new Div(icon, title, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    code.setId("two-factor-action-code");
    code.setWidthFull();
    code.setMaxLength(35);
    code.setManualValidation(true);
    code.setRequiredIndicatorVisible(true);
    code.getElement().setAttribute("autocomplete", "one-time-code");
    explanation.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    Div body = new Div(explanation, code);
    body.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(body);
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> close());
    confirm.setId("two-factor-action-confirm");
    confirm.setPrefixComponent(AppIcon.CHECK.create());
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirm.addClickListener(event -> submit());
    Div actions = new Div(cancel, confirm);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            code.clear();
            code.setInvalid(false);
            purpose = null;
            action = null;
          }
        });
    refreshTexts();
  }

  public void open(TwoFactorAction purpose, SerializableRunnable action) {
    this.purpose = Objects.requireNonNull(purpose);
    this.action = Objects.requireNonNull(action);
    code.clear();
    code.setInvalid(false);
    refreshTexts();
    super.open();
  }

  public void refreshTexts() {
    title.setText(texts.getExternalProof());
    explanation.setText(texts.getTwoFactorActionHint());
    code.setLabel(texts.getTwoFactorCodeLabel());
    code.setPlaceholder(texts.getTwoFactorCodePlaceholder());
    cancel.setText(texts.getAdminUsersCancel());
    confirm.setText(texts.getTwoFactorConfirm());
    close.setAriaLabel(texts.getNotificationsClose());
  }

  private void submit() {
    if (purpose == null
        || action == null
        || !ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
      return;
    }
    try {
      authorization.confirmAction(request, purpose, code.getValue());
      SerializableRunnable confirmedAction = action;
      confirmedAction.run();
      close();
    } catch (RuntimeException failure) {
      code.setInvalid(true);
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseFailedTitle,
              failure instanceof LockedException
                  ? texts::getTwoFactorThrottled
                  : failure instanceof BadCredentialsException
                      ? texts::getTwoFactorInvalidCode
                      : texts::getTwoFactorFailed,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      code.clear();
    }
  }
}
