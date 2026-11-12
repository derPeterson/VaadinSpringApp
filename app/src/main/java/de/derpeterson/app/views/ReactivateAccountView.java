package de.derpeterson.app.views;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_REACTIVATION_ROUTE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AccountLifecycleService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;

@Route(ACCOUNT_REACTIVATION_ROUTE)
@AnonymousAllowed
public final class ReactivateAccountView extends IsNotAuthenticatedBaseView<HorizontalLayout>
    implements HasDynamicTitle {
  private enum ReactivationStep {
    CONFIRM_LINK,
    INVALID_LINK,
    REACTIVATED
  }

  private final transient MessageProperties messageProperties;
  private final transient AccountLifecycleService accountLifecycleService;
  private final H2 reactivationTitleH2 = new H2();
  private final Span reactivationMessageSpan = new Span();
  private final Button reactivationButton = new Button();
  private final Button goToSignInButton =
      new Button(AppIcon.SIGN_IN.create(), clickEvent -> UI.getCurrent().navigate(LoginView.class));
  private ReactivationStep reactivationStep = ReactivationStep.CONFIRM_LINK;
  private String reactivationToken;

  public ReactivateAccountView(
      MessageProperties messageProperties,
      AccountLifecycleService accountLifecycleService,
      SecurityService securityService,
      HttpServletRequest httpRequest,
      ConfigService configService) {
    super(securityService, httpRequest, new HorizontalLayout());
    this.messageProperties = messageProperties;
    this.accountLifecycleService = accountLifecycleService;
    getContent().setWidthFull();
    getContent().setPadding(false);

    reactivationTitleH2.addClassName("cosmos-panel-title");
    reactivationMessageSpan.addClassName("cosmos-panel-description");
    reactivationButton.setId("account-reactivation-submit");
    reactivationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    reactivationButton.addClickListener(clickEvent -> submitReactivationStep());

    PublicPanelComponent reactivationPanel =
        new PublicPanelComponent(
            AppIcon.LOCK.create(),
            reactivationTitleH2,
            messageProperties::getAccountReactivateIntro,
            new Component[] {reactivationMessageSpan},
            goToSignInButton,
            reactivationButton);
    PublicPageComponent publicPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.ACCOUNT_REACTIVATION);
    publicPageComponent.getCardContainer().add(reactivationPanel);
    add(publicPageComponent);

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, languageEvent -> refreshTexts());
    refreshTexts();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    super.beforeEnter(beforeEnterEvent);
    if (beforeEnterEvent.hasForwardTarget()) {
      return;
    }

    reactivationToken =
        beforeEnterEvent
            .getLocation()
            .getQueryParameters()
            .getSingleParameter("token")
            .orElse(null);
    if (reactivationToken == null) {
      beforeEnterEvent.forwardTo(LoginView.class);
      return;
    }
    reactivationStep = ReactivationStep.CONFIRM_LINK;
    refreshTexts();
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getAccountReactivateAction();
  }

  private void submitReactivationStep() {
    if (reactivationStep == ReactivationStep.CONFIRM_LINK) {
      reactivationStep =
          accountLifecycleService.reactivateAccount(reactivationToken)
              ? ReactivationStep.REACTIVATED
              : ReactivationStep.INVALID_LINK;
      reactivationToken = null;
      UI.getCurrent().getPage().getHistory().replaceState(null, "/" + ACCOUNT_REACTIVATION_ROUTE);
    }
    refreshTexts();
  }

  private void refreshTexts() {
    reactivationTitleH2.setText(messageProperties.getAccountReactivateAction());
    goToSignInButton.setText(messageProperties.getBaseLoginButton());
    reactivationMessageSpan.setText(
        switch (reactivationStep) {
          case CONFIRM_LINK -> messageProperties.getAccountReactivateConfirm();
          case INVALID_LINK -> messageProperties.getAccountReactivateInvalid();
          case REACTIVATED -> messageProperties.getAccountReactivateSuccess();
        });

    reactivationButton.setVisible(reactivationStep == ReactivationStep.CONFIRM_LINK);
    reactivationButton.setText(messageProperties.getAccountReactivateAction());
  }
}
