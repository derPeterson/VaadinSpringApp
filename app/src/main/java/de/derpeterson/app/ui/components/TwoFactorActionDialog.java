package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

/** Gemeinsamer nativer Bestätigungsdialog für Faktorverwaltung und Sicherheitskonfiguration. */
@StyleSheet("custom-theme/cosmos-account.css")
public final class TwoFactorActionDialog extends Dialog {
  private final transient MessageProperties texts;
  private final transient TwoFactorAuthorization authorization;
  private final transient HttpServletRequest request;
  private final TwoFactorCodeInput code;
  private final H2 title = new H2();
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
    Div heading = new Div(title);
    heading.addClassName(CosmosClasses.PANEL_HEADING);
    Div icon = new Div(AppIcon.LOCK.create());
    icon.addClassName(CosmosClasses.PANEL_ICON);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(event -> close());
    Div header = new Div(icon, heading, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    code =
        new TwoFactorCodeInput(
            texts, request.getContextPath(), "two-factor-action", "two-factor-action");
    Div body = new Div(code);
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
            code.reset();
            purpose = null;
            action = null;
          }
        });
    refreshTexts();
  }

  public void open(TwoFactorAction purpose, SerializableRunnable action) {
    this.purpose = Objects.requireNonNull(purpose);
    this.action = Objects.requireNonNull(action);
    code.reset();
    refreshTexts();
    super.open();
    code.focusAppCode();
  }

  public void refreshTexts() {
    title.setText(texts.getExternalProof());
    code.refreshTexts();
    cancel.setText(texts.getAdminUsersCancel());
    confirm.setText(texts.getTwoFactorConfirm());
    close.setAriaLabel(texts.getNotificationsClose());
  }

  private void submit() {
    if (purpose == null || action == null || !code.validate()) {
      return;
    }
    try {
      authorization.confirmAction(request, purpose, code.getValue());
      SerializableRunnable confirmedAction = action;
      confirmedAction.run();
      close();
    } catch (RuntimeException failure) {
      code.markInvalid();
      Supplier<String> message = texts::getTwoFactorFailed;
      if (failure instanceof LockedException) {
        message = texts::getTwoFactorThrottled;
      } else if (failure instanceof BadCredentialsException) {
        message = texts::getTwoFactorInvalidCode;
      }
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseFailedTitle, message, NotificationHelper.NotificationType.ERROR);
    } finally {
      code.clear();
    }
  }
}
