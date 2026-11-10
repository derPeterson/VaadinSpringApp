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
  private final transient MessageProperties messageProperties;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final transient HttpServletRequest httpRequest;
  private final TwoFactorCodeInput twoFactorCodeInput;
  private final H2 dialogTitleHeading = new H2();
  private final Button cancelButton = new Button();
  private final Button confirmIdentityButton = new Button();
  private final Button closeDialogButton = new Button(AppIcon.CLOSE_CIRCLE.create());
  private TwoFactorAction twoFactorAction;
  private SerializableRunnable protectedAction;

  public TwoFactorActionDialog(
      MessageProperties messageProperties,
      TwoFactorAuthorization twoFactorAuthorization,
      HttpServletRequest httpRequest) {
    this.messageProperties = messageProperties;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.httpRequest = httpRequest;
    setId("two-factor-action-dialog");
    addClassNames("cosmos-theme", CosmosClasses.REFERENCE_OVERLAY, "account-change-dialog");
    getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    dialogTitleHeading.addClassName(CosmosClasses.PANEL_TITLE);

    Div dialogHeadingDiv = new Div(dialogTitleHeading);
    dialogHeadingDiv.addClassName(CosmosClasses.PANEL_HEADING);
    Div dialogIconDiv = new Div(AppIcon.LOCK.create());
    dialogIconDiv.addClassName(CosmosClasses.PANEL_ICON);
    closeDialogButton.addClassName("cosmos-overlay-close");
    closeDialogButton.addClickListener(event -> close());
    Div dialogHeaderDiv = new Div(dialogIconDiv, dialogHeadingDiv, closeDialogButton);
    dialogHeaderDiv.addClassName("account-change-dialog-header");
    getHeader().add(dialogHeaderDiv);
    twoFactorCodeInput =
        new TwoFactorCodeInput(
            messageProperties, httpRequest, "two-factor-action", "two-factor-action", this::close);

    Div dialogBodyDiv = new Div(twoFactorCodeInput);
    dialogBodyDiv.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(dialogBodyDiv);
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelButton.addClickListener(event -> close());
    confirmIdentityButton.setId("two-factor-action-confirm");
    confirmIdentityButton.setPrefixComponent(AppIcon.CHECK.create());
    confirmIdentityButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirmIdentityButton.addClickListener(event -> confirmProtectedAction());

    Div dialogActionsDiv = new Div(cancelButton, confirmIdentityButton);
    dialogActionsDiv.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(dialogActionsDiv);
    getFooter().add(dialogActionsDiv);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        twoFactorCodeInput.reset();
        twoFactorAction = null;
        protectedAction = null;
      }
    });
    refreshTexts();
  }

  public void open(TwoFactorAction twoFactorAction, SerializableRunnable protectedAction) {
    this.twoFactorAction = Objects.requireNonNull(twoFactorAction);
    this.protectedAction = Objects.requireNonNull(protectedAction);
    twoFactorCodeInput.reset();
    refreshTexts();
    super.open();
    twoFactorCodeInput.focusAppCode();
  }

  public void refreshTexts() {
    dialogTitleHeading.setText(messageProperties.getExternalProof());
    twoFactorCodeInput.refreshTexts();
    cancelButton.setText(messageProperties.getAdminUsersCancel());
    confirmIdentityButton.setText(messageProperties.getTwoFactorConfirm());
    closeDialogButton.setAriaLabel(messageProperties.getNotificationsClose());
  }

  private void confirmProtectedAction() {
    if (twoFactorAction == null || protectedAction == null || !twoFactorCodeInput.validate()) {
      return;
    }

    try {
      twoFactorAuthorization.confirmAction(
          httpRequest, twoFactorAction, twoFactorCodeInput.getValue());

      SerializableRunnable confirmedProtectedAction = protectedAction;
      confirmedProtectedAction.run();

      close();
    } catch (RuntimeException protectedActionFailure) {
      twoFactorCodeInput.markInvalid();

      Supplier<String> protectedActionFailureTextSupplier = messageProperties::getTwoFactorFailed;
      if (protectedActionFailure instanceof LockedException) {
        protectedActionFailureTextSupplier = messageProperties::getTwoFactorThrottled;
      } else if (protectedActionFailure instanceof BadCredentialsException) {
        protectedActionFailureTextSupplier = messageProperties::getTwoFactorInvalidCode;
      }

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              protectedActionFailureTextSupplier,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      twoFactorCodeInput.clear();
    }
  }
}
