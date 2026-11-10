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

  private final Div twoFactorAccountCard = new Div();
  private final TwoFactorActionDialog twoFactorConfirmationDialog;

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
    twoFactorConfirmationDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    add(twoFactorAccountCard, twoFactorConfirmationDialog);

    refreshTwoFactorAccountSection();
  }

  void refreshTwoFactorAccountSection() {
    twoFactorAccountCard.removeAll();
    TwoFactorAuthorization.State twoFactorState =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpRequest));
    Div twoFactorIcon = new Div(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorIcon.addClassName(CosmosClasses.PANEL_ICON);

    Div twoFactorDescription = createTwoFactorDescription(twoFactorState.enrolled());
    Div twoFactorActions = createTwoFactorActions(twoFactorState.enrolled());
    twoFactorAccountCard.add(twoFactorIcon, twoFactorDescription, twoFactorActions);
    twoFactorAccountCard.addClassNames("account-entry", "two-factor-account-entry");
    twoFactorConfirmationDialog.refreshTexts();
  }

  private Div createTwoFactorDescription(boolean authenticatorEnrolled) {
    Span twoFactorTitle = new Span(messageProperties.getTwoFactorSettingsTitle());
    twoFactorTitle.addClassName("account-entry-title");

    Badge enrollmentBadge =
        new Badge(
            authenticatorEnrolled
                ? messageProperties.getTwoFactorEnrolled()
                : messageProperties.getTwoFactorNotEnrolled());
    if (authenticatorEnrolled) {
      enrollmentBadge.addThemeVariants(BadgeVariant.SUCCESS);
    }

    Span twoFactorHint = new Span(messageProperties.getTwoFactorAccountHint());
    twoFactorHint.addClassName("account-entry-description");
    Div twoFactorDescription = new Div(new Div(twoFactorTitle, enrollmentBadge), twoFactorHint);
    twoFactorDescription.addClassName("account-entry-copy");
    return twoFactorDescription;
  }

  private Div createTwoFactorActions(boolean authenticatorEnrolled) {
    Div twoFactorActions = new Div();
    twoFactorActions.addClassName("two-factor-account-actions");
    EqualButtonWidths.configure(twoFactorActions);

    if (!authenticatorEnrolled) {
      Button setupAuthenticatorButton =
          new Button(messageProperties.getTwoFactorSetupAction(), AppIcon.LOCK.create());
      setupAuthenticatorButton.setId("account-two-factor-setup");
      setupAuthenticatorButton.addClickListener(event -> openAuthenticatorSetup());
      twoFactorActions.add(setupAuthenticatorButton);
    } else {
      Button regenerateRecoveryCodesButton =
          new Button(
              messageProperties.getTwoFactorRegenerateAction(), AppIcon.CLIPBOARD_COPY.create());
      regenerateRecoveryCodesButton.setId("account-two-factor-renew");
      regenerateRecoveryCodesButton.addClickListener(event ->
          twoFactorConfirmationDialog.open(
              TwoFactorAction.MANAGE_RECOVERY_CODES, this::regenerateRecoveryCodes));

      Button replaceAuthenticatorButton =
          new Button(messageProperties.getTwoFactorReplaceAction(), AppIcon.EDIT.create());
      replaceAuthenticatorButton.setId("account-two-factor-replace");
      replaceAuthenticatorButton.addClickListener(event ->
          twoFactorConfirmationDialog.open(
              TwoFactorAction.MANAGE_FACTOR, this::beginAuthenticatorReplacement));

      Button removeAuthenticatorButton = createRemoveAuthenticatorButton();
      twoFactorActions.add(
          replaceAuthenticatorButton, regenerateRecoveryCodesButton, removeAuthenticatorButton);
    }
    return twoFactorActions;
  }

  private Button createRemoveAuthenticatorButton() {
    Button removeAuthenticatorButton =
        new Button(messageProperties.getTwoFactorDisableAction(), AppIcon.TRASH.create());
    removeAuthenticatorButton.setId("account-two-factor-remove");
    removeAuthenticatorButton.addClassName(CosmosClasses.DELETE_BUTTON);

    TwoFactorManagement.DisableProblem disableProblem =
        twoFactorManagement.disableProblem(httpRequest);
    removeAuthenticatorButton.setEnabled(disableProblem == TwoFactorManagement.DisableProblem.NONE);
    removeAuthenticatorButton.setTooltipText(
        disableProblem == TwoFactorManagement.DisableProblem.NO_INDEPENDENT_ACCESS
            ? messageProperties.getTwoFactorIndependentAccessRequired()
            : messageProperties.getTwoFactorDisableHint());
    removeAuthenticatorButton.addClickListener(event ->
        twoFactorConfirmationDialog.open(
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
