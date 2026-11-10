package de.derpeterson.app.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCssClasses;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.SupportTicketsComponent;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;

/** Ein erster Anmeldenachweis erlaubt ausschließlich Hilfe zum eigenen Konto. */
@Route(RecoveryHelpView.ROUTE)
@PermitAll
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-mfa.css")
@StyleSheet("custom-theme/cosmos-mfa-confirmation.css")
public final class RecoveryHelpView extends VerticalLayout implements BeforeEnterObserver {
  public static final String ROUTE = "recovery-help";
  private final transient HttpServletRequest httpServletRequest;
  private final transient SecurityService securityService;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final PublicPageComponent recoveryHelpPageComponent;
  private final Button returnFromRecoveryHelpButton;
  private final Button recoveryHelpLogoutButton;
  private final transient MessageProperties messageProperties;
  private TwoFactorFlow twoFactorFlow;

  public RecoveryHelpView(
      ConfigService configService,
      MessageProperties messageProperties,
      SupportTicketService supportTicketService,
      SecurityService securityService,
      TwoFactorAuthorization twoFactorAuthorization,
      HttpServletRequest httpServletRequest) {
    this.httpServletRequest = httpServletRequest;
    this.securityService = securityService;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.messageProperties = messageProperties;

    setPadding(false);
    setSpacing(false);
    setWidthFull();

    recoveryHelpPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.RECOVERY_HELP);
    SupportTicketsComponent recoveryHelpTicketsComponent =
        new SupportTicketsComponent(
            messageProperties, supportTicketService, httpServletRequest, false);

    returnFromRecoveryHelpButton =
        new Button(messageProperties.getTwoFactorReturnToCode(), _ -> returnFromRecoveryHelp());
    returnFromRecoveryHelpButton.setId("support-back");
    returnFromRecoveryHelpButton.setPrefixComponent(AppIcon.ANGLE_LEFT.create());
    returnFromRecoveryHelpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    recoveryHelpTicketsComponent.addBackAction(returnFromRecoveryHelpButton);

    recoveryHelpLogoutButton =
        new Button(
            messageProperties.getBaseLogoutButton(),
            _ -> {
              securityService.logout();
              UI.getCurrent().getPage().setLocation(httpServletRequest.getContextPath() + "/login");
            });
    recoveryHelpLogoutButton.setId("support-sign-out");
    recoveryHelpLogoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    recoveryHelpLogoutButton.addClassName(CosmosCssClasses.DELETE_BUTTON);
    recoveryHelpTicketsComponent.addUserFooterAction(recoveryHelpLogoutButton);

    recoveryHelpPageComponent.getCardContainer().add(recoveryHelpTicketsComponent);
    add(recoveryHelpPageComponent);

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        _ -> {
          recoveryHelpTicketsComponent.refreshTexts();
          refreshRecoveryHelpActions();
        });
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    twoFactorFlow = TwoFactorFlow.current(httpServletRequest);
    if (twoFactorFlow == null) {
      twoFactorFlow =
          twoFactorAuthorization.sessionConfirmed(httpServletRequest)
              ? TwoFactorFlow.ACCOUNT_HELP
              : TwoFactorFlow.LOGIN_HELP;
      twoFactorFlow.enter(httpServletRequest);
    }

    if (!twoFactorFlow.help()) {
      beforeEnterEvent.forwardTo(TwoFactorView.class);
      return;
    }

    recoveryHelpPageComponent.setAccountSecurity(
        twoFactorFlow.account(), securityService.getCurrentUser(httpServletRequest).orElse(null));
    refreshRecoveryHelpActions();
  }

  public boolean matchesActiveTwoFactorFlow(TwoFactorFlow activeTwoFactorFlow) {
    return twoFactorFlow == activeTwoFactorFlow;
  }

  private void refreshRecoveryHelpActions() {
    boolean isAccountRecoveryHelp = twoFactorFlow != null && twoFactorFlow.account();
    returnFromRecoveryHelpButton.setText(
        isAccountRecoveryHelp
            ? messageProperties.getTwoFactorBack()
            : messageProperties.getTwoFactorReturnToCode());

    recoveryHelpLogoutButton.setText(messageProperties.getBaseLogoutButton());
    recoveryHelpLogoutButton.setVisible(!isAccountRecoveryHelp);
  }

  private void returnFromRecoveryHelp() {
    String destinationLocation;
    if (twoFactorFlow.account()) {
      TwoFactorFlow.finish(httpServletRequest);
      destinationLocation = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    } else {
      TwoFactorFlow nextTwoFactorFlow =
          twoFactorFlow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN;
      nextTwoFactorFlow.enter(httpServletRequest);
      destinationLocation = nextTwoFactorFlow.location();
    }

    UI.getCurrent()
        .getPage()
        .setLocation(httpServletRequest.getContextPath() + destinationLocation);
  }
}
