package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorManagement;
import jakarta.servlet.http.HttpServletRequest;

/** Eigener Authenticator im bestehenden Portalzugang, ohne Geheimnisse im Komponentenbaum. */
@StyleSheet("custom-theme/cosmos-mfa.css")
public final class TwoFactorAccountComponent extends Div {
  private final transient MessageProperties messageProperties;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final transient TwoFactorManagement twoFactorManagement;
  private final transient HttpServletRequest httpRequest;

  private final Div twoFactorAccountCardDiv = new Div();
  private final TwoFactorActionDialog twoFactorConfirmationActionDialog;

  public TwoFactorAccountComponent(
      MessageProperties messageProperties,
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorManagement twoFactorManagement,
      HttpServletRequest httpRequest) {
    this.messageProperties = messageProperties;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.twoFactorManagement = twoFactorManagement;
    this.httpRequest = httpRequest;

    setId("account-two-factor");
    twoFactorConfirmationActionDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    add(twoFactorAccountCardDiv, twoFactorConfirmationActionDialog);

    refreshTwoFactorAccountSection();
  }

  void refreshTwoFactorAccountSection() {
    twoFactorAccountCardDiv.removeAll();
    TwoFactorAuthorization.State twoFactorState =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpRequest));

    Div twoFactorIconContainerDiv = new Div(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorIconContainerDiv.addClassName(CosmosClasses.PANEL_ICON);

    Div twoFactorDescriptionDiv = createTwoFactorDescriptionDiv(twoFactorState.enrolled());
    Div twoFactorActionsDiv = createTwoFactorActionsDiv(twoFactorState.enrolled());
    twoFactorAccountCardDiv.add(
        twoFactorIconContainerDiv, twoFactorDescriptionDiv, twoFactorActionsDiv);
    twoFactorAccountCardDiv.addClassNames("account-entry", "two-factor-account-entry");
    twoFactorConfirmationActionDialog.refreshTexts();
  }

  private Div createTwoFactorDescriptionDiv(boolean authenticatorEnrolled) {
    Span twoFactorTitleSpan = new Span(messageProperties.getTwoFactorSettingsTitle());
    twoFactorTitleSpan.addClassName("account-entry-title");

    Badge enrollmentBadge =
        new Badge(
            authenticatorEnrolled
                ? messageProperties.getTwoFactorEnrolled()
                : messageProperties.getTwoFactorNotEnrolled());
    if (authenticatorEnrolled) {
      enrollmentBadge.addThemeVariants(BadgeVariant.SUCCESS);
    }

    Span twoFactorHintSpan = new Span(messageProperties.getTwoFactorAccountHint());
    twoFactorHintSpan.addClassName("account-entry-description");
    Div twoFactorDescriptionDiv =
        new Div(new Div(twoFactorTitleSpan, enrollmentBadge), twoFactorHintSpan);
    twoFactorDescriptionDiv.addClassName("account-entry-copy");

    return twoFactorDescriptionDiv;
  }

  private Div createTwoFactorActionsDiv(boolean authenticatorEnrolled) {
    Div twoFactorActionsDiv = new Div();
    twoFactorActionsDiv.addClassName("two-factor-account-actions");
    EqualButtonWidths.configureEqualButtonWidths(twoFactorActionsDiv);

    if (!authenticatorEnrolled) {
      Button setupAuthenticatorButton =
          new Button(messageProperties.getTwoFactorSetupAction(), AppIcon.LOCK.create());
      setupAuthenticatorButton.setId("account-two-factor-setup");
      setupAuthenticatorButton.addClickListener(event -> openAuthenticatorSetup());
      twoFactorActionsDiv.add(setupAuthenticatorButton);
    } else {
      Button regenerateRecoveryCodesButton =
          new Button(
              messageProperties.getTwoFactorRegenerateAction(), AppIcon.CLIPBOARD_COPY.create());
      regenerateRecoveryCodesButton.setId("account-two-factor-renew");
      regenerateRecoveryCodesButton.addClickListener(event ->
          twoFactorConfirmationActionDialog.open(
              TwoFactorAction.MANAGE_RECOVERY_CODES, this::regenerateRecoveryCodes));

      Button replaceAuthenticatorButton =
          new Button(messageProperties.getTwoFactorReplaceAction(), AppIcon.EDIT.create());
      replaceAuthenticatorButton.setId("account-two-factor-replace");
      replaceAuthenticatorButton.addClickListener(event ->
          twoFactorConfirmationActionDialog.open(
              TwoFactorAction.MANAGE_FACTOR, this::beginAuthenticatorReplacement));

      Button removeAuthenticatorButton = createRemoveAuthenticatorButton();
      twoFactorActionsDiv.add(
          replaceAuthenticatorButton, regenerateRecoveryCodesButton, removeAuthenticatorButton);
    }

    return twoFactorActionsDiv;
  }

  private Button createRemoveAuthenticatorButton() {
    Button removeAuthenticatorButton =
        new Button(messageProperties.getTwoFactorDisableAction(), AppIcon.TRASH.create());
    removeAuthenticatorButton.setId("account-two-factor-remove");
    removeAuthenticatorButton.addClassName(CosmosClasses.DELETE_BUTTON);

    TwoFactorManagement.DisableProblem authenticatorDisableProblem =
        twoFactorManagement.disableProblem(httpRequest);
    removeAuthenticatorButton.setEnabled(
        authenticatorDisableProblem == TwoFactorManagement.DisableProblem.NONE);
    removeAuthenticatorButton.setTooltipText(
        authenticatorDisableProblem == TwoFactorManagement.DisableProblem.NO_INDEPENDENT_ACCESS
            ? messageProperties.getTwoFactorIndependentAccessRequired()
            : messageProperties.getTwoFactorDisableHint());
    removeAuthenticatorButton.addClickListener(event ->
        twoFactorConfirmationActionDialog.open(
            TwoFactorAction.MANAGE_FACTOR, this::disableAuthenticator));

    return removeAuthenticatorButton;
  }

  private void regenerateRecoveryCodes() {
    twoFactorManagement.regenerate(httpRequest);

    openAuthenticatorSetup();
  }

  private void beginAuthenticatorReplacement() {
    twoFactorManagement.beginReplacement(httpRequest);

    openAuthenticatorSetup();
  }

  private void disableAuthenticator() {
    twoFactorManagement.disable(httpRequest);
    refreshTwoFactorAccountSection();

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getTwoFactorDisabled, NotificationHelper.NotificationType.SUCCESS);
  }

  private void openAuthenticatorSetup() {
    TwoFactorFlow.SETUP.enter(httpRequest);

    UI.getCurrent()
        .getPage()
        .setLocation(httpRequest.getContextPath() + TwoFactorFlow.SETUP.location());
  }
}
