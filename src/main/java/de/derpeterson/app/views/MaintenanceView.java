package de.derpeterson.app.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
import jakarta.servlet.http.HttpServletRequest;

@Route(AppRouteConstants.MAINTENANCE_ROUTE)
@PageTitle(AppRouteConstants.MAINTENANCE_PAGE_TITLE)
@AnonymousAllowed
public class MaintenanceView extends VerticalLayout implements BeforeEnterObserver {
  private final transient MaintenanceService maintenance;
  private final transient HttpServletRequest request;

  public MaintenanceView(
      ConfigService config,
      MessageProperties messages,
      MaintenanceService maintenance,
      SecurityService security,
      HttpServletRequest request) {
    this.maintenance = maintenance;
    this.request = request;
    setPadding(false);
    setSpacing(false);
    var page = new CosmosPageComponent(config, false, messages::getAccountCopyrightTemplate);
    var content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(false);
    content.addClassName("cosmos-form");
    content.setAlignItems(FlexComponent.Alignment.STRETCH);
    var icon = VaadinIcon.WARNING.create();
    icon.addClassNames("cosmos-status-icon", "cosmos-status-icon--warning");
    var iconRow = new HorizontalLayout(icon);
    iconRow.setWidthFull();
    iconRow.setPadding(false);
    iconRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    var title = new H1();
    title.addClassName("cosmos-headline");
    var titleRow = new HorizontalLayout(title);
    titleRow.addClassName("cosmos-title-row");
    titleRow.setWidthFull();
    titleRow.setPadding(false);
    titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    var text = new Span();
    text.addClassName("cosmos-form-text");
    var textRow = new VerticalLayout(text);
    textRow.addClassName("cosmos-intro");
    textRow.setWidthFull();
    textRow.setPadding(false);
    textRow.setSpacing(false);
    textRow.setAlignItems(FlexComponent.Alignment.CENTER);
    textRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    var login =
        new Button(
            "",
            event ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(request.getContextPath() + AppRoute.LOGIN.getSecurityRoute()));
    boolean authenticated = security.getAuthenticatedUser(request).isPresent();
    login.setVisible(!authenticated);
    login.setPrefixComponent(VaadinIcon.SIGN_IN.create());
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    login.setWidthFull();
    var retry =
        new Button(
            "",
            event ->
                UI.getCurrent()
                    .getPage()
                    .setLocation(request.getContextPath() + AppRoute.HOME.getSecurityRoute()));
    retry.setPrefixComponent(VaadinIcon.REFRESH.create());
    retry.setWidthFull();
    var logout =
        new Button(
            "",
            event -> {
              security.logout();
              UI.getCurrent()
                  .getPage()
                  .setLocation(request.getContextPath() + AppRoute.LOGIN.getSecurityRoute());
            });
    logout.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
    logout.setWidthFull();
    logout.setVisible(authenticated);
    Runnable translate =
        () -> {
          title.setText(messages.getMaintenanceTitle());
          text.setText(
              maintenance.state() == MaintenanceService.State.UNAVAILABLE
                  ? messages.getMaintenanceUnavailableMessage()
                  : messages.getMaintenanceDescription());
          login.setText(messages.getMaintenanceLoginButton());
          retry.setText(messages.getMaintenanceRetryButton());
          logout.setText(messages.getBaseLogoutButton());
        };
    translate.run();
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> translate.run());
    content.add(iconRow, titleRow, textRow, login, retry, logout);
    page.getCardContainer().add(new CosmosCardComponent(content));
    add(page);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!maintenance.blocks(request)) event.forwardTo(HomeView.class);
  }
}
