package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.STATUS_ICON;

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
  private final transient HttpServletRequest httpRequest;

  public MaintenanceView(
      ConfigService config,
      MessageProperties messages,
      MaintenanceService maintenanceService,
      SecurityService security,
      HttpServletRequest httpRequest) {
    this.maintenanceService = maintenanceService;
    this.httpRequest = httpRequest;
    setPadding(false);
    setSpacing(false);
    PublicPageComponent page =
        new PublicPageComponent(config, messages, PublicPageComponent.Page.MAINTENANCE);
    SvgIcon icon = AppIcon.WRENCH.create();
    icon.addClassName(STATUS_ICON);
    H1 title = new H1();
    title.addClassName("cosmos-headline");
    Span text = new Span();
    text.addClassName("cosmos-form-text");
    Button login =
        new Button(
            "",
            event ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(httpRequest.getContextPath() + AppRoute.LOGIN.getSecurityRoute()));
    boolean authenticated = security.getAuthenticatedUser(httpRequest).isPresent();
    login.setVisible(!authenticated);
    login.setPrefixComponent(AppIcon.SIGN_IN.create());
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    login.setWidthFull();
    Button retry =
        new Button(
            "",
            event ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(httpRequest.getContextPath() + AppRoute.HOME.getSecurityRoute()));
    retry.setPrefixComponent(AppIcon.REFRESH.create());
    retry.setWidthFull();
    Button logout =
        new Button(
            "",
            event -> {
              security.logout();
              UI.getCurrent()
                  .getPage()
                  .setLocation(httpRequest.getContextPath() + AppRoute.LOGIN.getSecurityRoute());
            });
    logout.setPrefixComponent(AppIcon.SIGN_OUT.create());
    logout.setWidthFull();
    logout.setVisible(authenticated);
    Runnable translate =
        () -> {
          title.setText(messages.getMaintenanceTitle());
          text.setText(
              maintenanceService.state() == MaintenanceService.State.UNAVAILABLE
                  ? messages.getMaintenanceUnavailableMessage()
                  : messages.getMaintenanceDescription());
          login.setText(messages.getMaintenanceLoginButton());
          retry.setText(messages.getMaintenanceRetryButton());
          logout.setText(messages.getBaseLogoutButton());
        };
    translate.run();
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
    page.getCardContainer()
        .add(
            new PublicPanelComponent(
                icon,
                title,
                messages::getPublicMaintenanceSubtitle,
                new com.vaadin.flow.component.Component[] {text},
                retry,
                login,
                logout));
    add(page);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!maintenanceService.blocks(httpRequest)) {
      event.forwardTo(HomeView.class);
    }
  }
}
