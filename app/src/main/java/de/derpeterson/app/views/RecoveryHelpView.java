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
  private final transient HttpServletRequest httpRequest;
  private final transient SecurityService securityService;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final PublicPageComponent recoveryHelpPage;
  private final Button backButton;
  private final Button logoutButton;
  private final transient MessageProperties messageProperties;
  private TwoFactorFlow twoFactorFlow;

  public RecoveryHelpView(
      ConfigService config,
      MessageProperties messageProperties,
      SupportTicketService tickets,
      SecurityService securityService,
      TwoFactorAuthorization twoFactorAuthorization,
      HttpServletRequest httpRequest) {
    this.httpRequest = httpRequest;
    this.securityService = securityService;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.messageProperties = messageProperties;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    recoveryHelpPage =
        new PublicPageComponent(config, messageProperties, PublicPageComponent.Page.RECOVERY_HELP);
    SupportTicketsComponent support =
        new SupportTicketsComponent(messageProperties, tickets, httpRequest, false);
    backButton = new Button(messageProperties.getTwoFactorReturnToCode(), event -> leaveHelp());
    backButton.setId("support-back");
    backButton.setPrefixComponent(AppIcon.ANGLE_LEFT.create());
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    support.addBackAction(backButton);
    logoutButton =
        new Button(
            messageProperties.getBaseLogoutButton(),
            event -> {
              securityService.logout();
              UI.getCurrent().getPage().setLocation(httpRequest.getContextPath() + "/login");
            });
    logoutButton.setId("support-sign-out");
    logoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logoutButton.addClassName(de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON);
    support.addUserFooterAction(logoutButton);
    recoveryHelpPage.getCardContainer().add(support);
    add(recoveryHelpPage);
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          support.refreshTexts();
          refreshNavigation();
        });
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    twoFactorFlow = TwoFactorFlow.current(httpRequest);
    if (twoFactorFlow == null) {
      twoFactorFlow =
          twoFactorAuthorization.sessionConfirmed(httpRequest)
              ? TwoFactorFlow.ACCOUNT_HELP
              : TwoFactorFlow.LOGIN_HELP;
      twoFactorFlow.enter(httpRequest);
    }
    if (!twoFactorFlow.help()) {
      event.forwardTo(TwoFactorView.class);
      return;
    }
    recoveryHelpPage.setAccountSecurity(
        twoFactorFlow.account(), securityService.getCurrentUser(httpRequest).orElse(null));
    refreshNavigation();
  }

  public boolean isCurrent(TwoFactorFlow active) {
    return twoFactorFlow == active;
  }

  private void refreshNavigation() {
    boolean account = twoFactorFlow != null && twoFactorFlow.account();
    backButton.setText(
        account
            ? messageProperties.getTwoFactorBack()
            : messageProperties.getTwoFactorReturnToCode());
    logoutButton.setText(messageProperties.getBaseLogoutButton());
    logoutButton.setVisible(!account);
  }

  private void leaveHelp() {
    String destination;
    if (twoFactorFlow.account()) {
      TwoFactorFlow.finish(httpRequest);
      destination = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    } else {
      TwoFactorFlow next =
          twoFactorFlow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN;
      next.enter(httpRequest);
      destination = next.location();
    }
    UI.getCurrent().getPage().setLocation(httpRequest.getContextPath() + destination);
  }
}
