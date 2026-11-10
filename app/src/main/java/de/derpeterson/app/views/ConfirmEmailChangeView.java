package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.STATUS_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.Confirmation;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/** Öffnen eines Mail-Links zeigt nur die Vorschau; eine Änderung verlangt den Buttonklick. */
@Route(AppRouteConstants.EMAIL_CHANGE_ROUTE)
@PageTitle(AppRouteConstants.EMAIL_CHANGE_PAGE_TITLE)
@AnonymousAllowed
@Slf4j
public class ConfirmEmailChangeView extends Div implements HasUrlParameter<String> {
  private final transient MessageProperties messageProperties;
  private final transient AccountSecurityService accountSecurityService;
  private final SvgIcon confirmationStatusSvgIcon = AppIcon.ENVELOPE.create();
  private final H1 confirmationTitleH1 = new H1();
  private final Span confirmationMessageSpan = new Span();
  private final Span accountChangeConsequenceSpan = new Span();
  private final Div accountChangeConsequenceDiv = new Div();
  private final Button confirmEmailChangeButton = new Button();
  private final Button goToLoginButton = new Button();
  private final Div confirmationActionsDiv = new Div();
  private String emailChangeToken;
  private Confirmation confirmationState = Confirmation.INVALID;
  private boolean hasConfirmationTechnicalFailure;

  public ConfirmEmailChangeView(
      MessageProperties messageProperties,
      AccountSecurityService accountSecurityService,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.accountSecurityService = accountSecurityService;
    setId("account-email-confirmation");
    PublicPageComponent emailConfirmationPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.EMAIL_CONFIRM);
    confirmationStatusSvgIcon.addClassName(STATUS_ICON);
    confirmationStatusSvgIcon.getElement().setAttribute("aria-hidden", "true");
    confirmationMessageSpan.addClassName("cosmos-form-text");
    confirmationTitleH1.addClassName("cosmos-headline");
    confirmEmailChangeButton.setId("account-email-confirm-action");
    confirmEmailChangeButton.setPrefixComponent(AppIcon.CHECK.create());
    confirmEmailChangeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirmEmailChangeButton.addClickListener(_ -> confirmEmailChange());
    goToLoginButton.setId("account-email-confirm-login");
    goToLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    accountChangeConsequenceDiv.setId("account-email-confirm-hint");
    accountChangeConsequenceDiv.addClassName("cosmos-panel-hint");
    accountChangeConsequenceDiv.add(AppIcon.INFO_CIRCLE.create(), accountChangeConsequenceSpan);
    confirmationActionsDiv.add(goToLoginButton, confirmEmailChangeButton);
    // Nach der Bestätigung ist die bisherige Sitzung widerrufen. Zur Anmeldung deshalb
    // direkt im Browser wechseln, ohne einen weiteren RPC dieser Sitzung zu benötigen.
    goToLoginButton.addAttachListener(_ ->
        goToLoginButton
            .getElement()
            .executeJs(
                "this.onclick = () => window.location.assign(new URL($0, document.baseURI).href)",
                AppRoute.LOGIN.getRoute()));
    emailConfirmationPageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                confirmationStatusSvgIcon,
                confirmationTitleH1,
                messageProperties::getPublicEmailSubtitle,
                new Component[] {confirmationMessageSpan, accountChangeConsequenceDiv},
                confirmationActionsDiv));
    add(emailConfirmationPageComponent);
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        languageChangeEvent -> {
          VaadinSession.getCurrent().setLocale(languageChangeEvent.getNewLocale());
          refreshConfirmationPanel();
          NotificationHelper.getInstance().updateText();
        });
    refreshConfirmationPanel();
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String emailChangeToken) {
    this.emailChangeToken = emailChangeToken;
    hasConfirmationTechnicalFailure = false;
    try {
      confirmationState = accountSecurityService.inspect(emailChangeToken);
    } catch (RuntimeException confirmationFailure) {
      hasConfirmationTechnicalFailure = true;
      log.error("Email change inspection failed", safeFailure(confirmationFailure));
    }
    refreshConfirmationPanel();
  }

  private void confirmEmailChange() {
    confirmEmailChangeButton.setEnabled(false);
    try {
      hasConfirmationTechnicalFailure = false;
      confirmationState = accountSecurityService.confirm(emailChangeToken);
      refreshConfirmationPanel();
      NotificationHelper.getInstance()
          .showNotification(
              confirmationState == Confirmation.SUCCESS
                  ? messageProperties::getBaseSuccessTitle
                  : messageProperties::getBaseFailedTitle,
              resolveConfirmationMessageSupplier(),
              confirmationState == Confirmation.SUCCESS
                  ? NotificationHelper.NotificationType.SUCCESS
                  : NotificationHelper.NotificationType.ERROR);
    } catch (RuntimeException confirmationFailure) {
      hasConfirmationTechnicalFailure = true;
      log.error("Email change confirmation failed", safeFailure(confirmationFailure));
      refreshConfirmationPanel();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAccountEmailConfirmationFailed,
              -1,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      confirmEmailChangeButton.setEnabled(true);
    }
  }

  private Supplier<String> resolveConfirmationMessageSupplier() {
    if (hasConfirmationTechnicalFailure) {
      return messageProperties::getAccountEmailConfirmationFailed;
    }
    return switch (confirmationState) {
      case READY -> messageProperties::getAccountEmailConfirmationReady;
      case SUCCESS -> messageProperties::getAccountEmailConfirmationSuccess;
      case EXPIRED -> messageProperties::getAccountEmailConfirmationExpired;
      case CONFLICT -> messageProperties::getAccountEmailConfirmationConflict;
      case INVALID -> messageProperties::getAccountEmailConfirmationInvalid;
    };
  }

  private void updateConfirmationStatusSvgIcon() {
    AppIcon confirmationAppIcon =
        hasConfirmationTechnicalFailure
            ? AppIcon.WARNING
            : switch (confirmationState) {
              case READY -> AppIcon.ENVELOPE;
              case SUCCESS -> AppIcon.CHECK_CIRCLE;
              case EXPIRED, INVALID, CONFLICT -> AppIcon.WARNING;
            };
    String confirmationColorCssClass =
        hasConfirmationTechnicalFailure
            ? LumoUtility.TextColor.ERROR
            : switch (confirmationState) {
              case READY -> LumoUtility.TextColor.PRIMARY;
              case SUCCESS -> LumoUtility.TextColor.SUCCESS;
              case EXPIRED -> LumoUtility.TextColor.WARNING;
              case INVALID, CONFLICT -> LumoUtility.TextColor.ERROR;
            };
    confirmationStatusSvgIcon.setSrc(confirmationAppIcon.getSource());
    confirmationStatusSvgIcon.removeClassNames(
        LumoUtility.TextColor.PRIMARY,
        LumoUtility.TextColor.SUCCESS,
        LumoUtility.TextColor.WARNING,
        LumoUtility.TextColor.ERROR);
    confirmationStatusSvgIcon.addClassName(confirmationColorCssClass);
    confirmationStatusSvgIcon.setClassName(
        "cosmos-status-icon--warning",
        confirmationState == Confirmation.EXPIRED && !hasConfirmationTechnicalFailure);
  }

  private String resolveConfirmationTitleText() {
    if (hasConfirmationTechnicalFailure) {
      return messageProperties.getBaseFailedTitle();
    }
    return switch (confirmationState) {
      case READY -> messageProperties.getPublicEmailFormTitle();
      case SUCCESS -> messageProperties.getPublicEmailSuccessTitle();
      case EXPIRED -> messageProperties.getPublicEmailExpiredTitle();
      case INVALID -> messageProperties.getPublicEmailInvalidTitle();
      case CONFLICT -> messageProperties.getPublicEmailConflictTitle();
    };
  }

  private void refreshConfirmationPanel() {
    updateConfirmationStatusSvgIcon();
    boolean canConfirmEmailChange =
        confirmationState == Confirmation.READY && !hasConfirmationTechnicalFailure;
    setClassName("public-email-outcome", !canConfirmEmailChange);
    confirmationTitleH1.setText(resolveConfirmationTitleText());
    confirmationMessageSpan.setText(resolveConfirmationMessageSupplier().get());
    accountChangeConsequenceSpan.setText(messageProperties.getPublicEmailConfirmationHint());
    accountChangeConsequenceDiv.setVisible(canConfirmEmailChange);
    confirmationActionsDiv.setClassName("public-panel-actions", canConfirmEmailChange);
    confirmEmailChangeButton.setText(messageProperties.getAccountEmailConfirmationAction());
    confirmEmailChangeButton.setVisible(canConfirmEmailChange);
    goToLoginButton.setText(messageProperties.getBaseLoginButton());
    if (canConfirmEmailChange) {
      goToLoginButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      goToLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
  }
}
