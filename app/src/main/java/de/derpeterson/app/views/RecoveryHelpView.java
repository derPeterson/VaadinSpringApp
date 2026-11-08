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
  private final transient HttpServletRequest request;
  private final transient TwoFactorAuthorization authorization;
  private final PublicPageComponent page;
  private final Button back;
  private final Button logout;
  private final transient MessageProperties texts;
  private TwoFactorFlow flow;

  public RecoveryHelpView(
      ConfigService config,
      MessageProperties texts,
      SupportTicketService tickets,
      SecurityService security,
      TwoFactorAuthorization authorization,
      HttpServletRequest request) {
    this.request = request;
    this.authorization = authorization;
    this.texts = texts;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    page = new PublicPageComponent(config, texts, PublicPageComponent.Page.RECOVERY_HELP);
    SupportTicketsComponent support = new SupportTicketsComponent(texts, tickets, request, false);
    back = new Button(texts.getTwoFactorReturnToCode(), event -> leaveHelp());
    back.setId("support-back");
    back.setPrefixComponent(AppIcon.ANGLE_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    support.addBackAction(back);
    logout =
        new Button(
            texts.getBaseLogoutButton(),
            event -> {
              security.logout();
              UI.getCurrent().getPage().setLocation(request.getContextPath() + "/login");
            });
    logout.setId("support-sign-out");
    logout.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logout.addClassName(de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON);
    support.addBackAction(logout);
    page.getCardContainer().add(support);
    add(page);
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
    flow = TwoFactorFlow.current(request);
    if (flow == null) {
      flow =
          authorization.sessionConfirmed(request)
              ? TwoFactorFlow.ACCOUNT_HELP
              : TwoFactorFlow.LOGIN_HELP;
      flow.enter(request);
    }
    if (!flow.help()) {
      event.forwardTo(TwoFactorView.class);
      return;
    }
    page.setAccountSecurity(flow.account());
    refreshNavigation();
  }

  public boolean isCurrent(TwoFactorFlow active) {
    return flow == active;
  }

  private void refreshNavigation() {
    boolean account = flow != null && flow.account();
    back.setText(account ? texts.getTwoFactorBack() : texts.getTwoFactorReturnToCode());
    logout.setText(texts.getBaseLogoutButton());
    logout.setVisible(!account);
  }

  private void leaveHelp() {
    String destination;
    if (flow.account()) {
      TwoFactorFlow.finish(request);
      destination = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    } else {
      TwoFactorFlow next =
          flow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN;
      next.enter(request);
      destination = next.location();
    }
    UI.getCurrent().getPage().setLocation(request.getContextPath() + destination);
  }
}
