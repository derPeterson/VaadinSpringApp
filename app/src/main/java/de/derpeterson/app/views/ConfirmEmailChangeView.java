package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.STATUS_ICON;

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
  private final SvgIcon statusIcon = AppIcon.ENVELOPE.create();
  private final H1 confirmationPageTitle = new H1();
  private final Span confirmationMessage = new Span();
  private final Span consequenceText = new Span();
  private final Div accountChangeConsequencePanel = new Div();
  private final Button confirmEmailChangeButton = new Button();
  private final Button goToLoginButton = new Button();
  private final Div confirmationActions = new Div();
  private String token;
  private Confirmation confirmationState = Confirmation.INVALID;
  private boolean technicalFailure;

  public ConfirmEmailChangeView(
      MessageProperties messageProperties,
      AccountSecurityService accountSecurityService,
      ConfigService config) {
    this.messageProperties = messageProperties;
    this.accountSecurityService = accountSecurityService;
    setId("account-email-confirmation");
    PublicPageComponent page =
        new PublicPageComponent(config, messageProperties, PublicPageComponent.Page.EMAIL_CONFIRM);
    statusIcon.addClassName(STATUS_ICON);
    statusIcon.getElement().setAttribute("aria-hidden", "true");
    confirmationMessage.addClassName("cosmos-form-text");
    confirmationPageTitle.addClassName("cosmos-headline");
    confirmEmailChangeButton.setId("account-email-confirm-action");
    confirmEmailChangeButton.setPrefixComponent(AppIcon.CHECK.create());
    confirmEmailChangeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirmEmailChangeButton.addClickListener(event -> apply());
    goToLoginButton.setId("account-email-confirm-login");
    goToLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    accountChangeConsequencePanel.setId("account-email-confirm-hint");
    accountChangeConsequencePanel.addClassName("cosmos-panel-hint");
    accountChangeConsequencePanel.add(AppIcon.INFO_CIRCLE.create(), consequenceText);
    confirmationActions.add(goToLoginButton, confirmEmailChangeButton);
    // Nach der Bestätigung ist die bisherige Sitzung widerrufen. Zur Anmeldung deshalb
    // direkt im Browser wechseln, ohne einen weiteren RPC dieser Sitzung zu benötigen.
    goToLoginButton.addAttachListener(event ->
        goToLoginButton
            .getElement()
            .executeJs(
                "this.onclick = () => window.location.assign(new URL($0, document.baseURI).href)",
                AppRoute.LOGIN.getRoute()));
    page.getCardContainer()
        .add(
            new PublicPanelComponent(
                statusIcon,
                confirmationPageTitle,
                messageProperties::getPublicEmailSubtitle,
                new com.vaadin.flow.component.Component[] {
                  confirmationMessage, accountChangeConsequencePanel
                },
                confirmationActions));
    add(page);
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());
          refreshTexts();
          NotificationHelper.getInstance().updateText();
        });
    refreshTexts();
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    this.token = token;
    technicalFailure = false;
    try {
      confirmationState = accountSecurityService.inspect(token);
    } catch (RuntimeException failure) {
      technicalFailure = true;
      log.error("Email change inspection failed", safeFailure(failure));
    }
    refreshTexts();
  }

  private void apply() {
    confirmEmailChangeButton.setEnabled(false);
    try {
      technicalFailure = false;
      confirmationState = accountSecurityService.confirm(token);
      refreshTexts();
      NotificationHelper.getInstance()
          .showNotification(
              confirmationState == Confirmation.SUCCESS
                  ? messageProperties::getBaseSuccessTitle
                  : messageProperties::getBaseFailedTitle,
              message(),
              confirmationState == Confirmation.SUCCESS
                  ? NotificationHelper.NotificationType.SUCCESS
                  : NotificationHelper.NotificationType.ERROR);
    } catch (RuntimeException failure) {
      technicalFailure = true;
      log.error("Email change confirmation failed", safeFailure(failure));
      refreshTexts();
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

  private Supplier<String> message() {
    if (technicalFailure) {
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

  private void updateStatusIcon() {
    AppIcon icon =
        technicalFailure
            ? AppIcon.WARNING
            : switch (confirmationState) {
              case READY -> AppIcon.ENVELOPE;
              case SUCCESS -> AppIcon.CHECK_CIRCLE;
              case EXPIRED, INVALID, CONFLICT -> AppIcon.WARNING;
            };
    String color =
        technicalFailure
            ? LumoUtility.TextColor.ERROR
            : switch (confirmationState) {
              case READY -> LumoUtility.TextColor.PRIMARY;
              case SUCCESS -> LumoUtility.TextColor.SUCCESS;
              case EXPIRED -> LumoUtility.TextColor.WARNING;
              case INVALID, CONFLICT -> LumoUtility.TextColor.ERROR;
            };
    statusIcon.setSrc(icon.getSource());
    statusIcon.removeClassNames(
        LumoUtility.TextColor.PRIMARY,
        LumoUtility.TextColor.SUCCESS,
        LumoUtility.TextColor.WARNING,
        LumoUtility.TextColor.ERROR);
    statusIcon.addClassName(color);
    statusIcon.setClassName(
        "cosmos-status-icon--warning",
        confirmationState == Confirmation.EXPIRED && !technicalFailure);
  }

  private String confirmationTitle() {
    if (technicalFailure) {
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

  private void refreshTexts() {
    updateStatusIcon();
    boolean ready = confirmationState == Confirmation.READY && !technicalFailure;
    setClassName("public-email-outcome", !ready);
    confirmationPageTitle.setText(confirmationTitle());
    confirmationMessage.setText(message().get());
    consequenceText.setText(messageProperties.getPublicEmailConfirmationHint());
    accountChangeConsequencePanel.setVisible(ready);
    confirmationActions.setClassName("public-panel-actions", ready);
    confirmEmailChangeButton.setText(messageProperties.getAccountEmailConfirmationAction());
    confirmEmailChangeButton.setVisible(ready);
    goToLoginButton.setText(messageProperties.getBaseLoginButton());
    if (ready) {
      goToLoginButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      goToLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
  }
}
