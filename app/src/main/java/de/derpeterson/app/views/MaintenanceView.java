package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.STATUS_ICON;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;

@Route(AppRouteConstants.MAINTENANCE_ROUTE)
@PageTitle(AppRouteConstants.MAINTENANCE_PAGE_TITLE)
@AnonymousAllowed
public class MaintenanceView extends VerticalLayout implements BeforeEnterObserver {
  private final transient MaintenanceService maintenanceService;
  private final transient HttpServletRequest httpServletRequest;

  public MaintenanceView(
      ConfigService configService,
      MessageProperties messageProperties,
      MaintenanceService maintenanceService,
      SecurityService securityService,
      HttpServletRequest httpServletRequest) {
    this.maintenanceService = maintenanceService;
    this.httpServletRequest = httpServletRequest;

    setPadding(false);
    setSpacing(false);

    PublicPageComponent maintenancePageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.MAINTENANCE);

    SvgIcon maintenanceSvgIcon = AppIcon.WRENCH.create();
    maintenanceSvgIcon.addClassName(STATUS_ICON);
    H1 maintenanceTitleH1 = new H1();
    maintenanceTitleH1.addClassName("cosmos-headline");
    Span maintenanceMessageSpan = new Span();
    maintenanceMessageSpan.addClassName("cosmos-form-text");

    Button maintenanceLoginButton =
        new Button(
            "",
            _ ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(
                        httpServletRequest.getContextPath() + AppRoute.LOGIN.getSecurityRoute()));
    boolean isAuthenticatedUser =
        securityService.getAuthenticatedUser(httpServletRequest).isPresent();
    maintenanceLoginButton.setVisible(!isAuthenticatedUser);
    maintenanceLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    maintenanceLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    maintenanceLoginButton.setWidthFull();

    Button retryHomeButton =
        new Button(
            "",
            _ ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(
                        httpServletRequest.getContextPath() + AppRoute.HOME.getSecurityRoute()));
    retryHomeButton.setPrefixComponent(AppIcon.REFRESH.create());
    retryHomeButton.setWidthFull();

    Button maintenanceLogoutButton =
        new Button(
            "",
            _ -> {
              securityService.logout();
              UI.getCurrent()
                  .getPage()
                  .setLocation(
                      httpServletRequest.getContextPath() + AppRoute.LOGIN.getSecurityRoute());
            });
    maintenanceLogoutButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    maintenanceLogoutButton.setWidthFull();
    maintenanceLogoutButton.setVisible(isAuthenticatedUser);

    Runnable refreshMaintenanceTextsAction =
        () -> {
          maintenanceTitleH1.setText(messageProperties.getMaintenanceTitle());
          maintenanceMessageSpan.setText(
              maintenanceService.state() == MaintenanceService.State.UNAVAILABLE
                  ? messageProperties.getMaintenanceUnavailableMessage()
                  : messageProperties.getMaintenanceDescription());
          maintenanceLoginButton.setText(messageProperties.getMaintenanceLoginButton());
          retryHomeButton.setText(messageProperties.getMaintenanceRetryButton());
          maintenanceLogoutButton.setText(messageProperties.getBaseLogoutButton());
        };
    refreshMaintenanceTextsAction.run();
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, _ -> refreshMaintenanceTextsAction.run());

    maintenancePageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                maintenanceSvgIcon,
                maintenanceTitleH1,
                messageProperties::getPublicMaintenanceSubtitle,
                new com.vaadin.flow.component.Component[] {maintenanceMessageSpan},
                retryHomeButton,
                maintenanceLoginButton,
                maintenanceLogoutButton));
    add(maintenancePageComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    if (!maintenanceService.blocks(httpServletRequest)) {
      beforeEnterEvent.forwardTo(HomeView.class);
    }
  }
}
