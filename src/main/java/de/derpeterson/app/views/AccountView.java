package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;

@Route(AppRouteConstants.ACCOUNT_ROUTE)
@PageTitle(AppRouteConstants.ACCOUNT_PAGE_TITLE)
@StyleSheet("custom-theme/cosmos-account.css")
@UIScope
@PermitAll
public class AccountView extends IsAuthenticatedBaseView {
  private final AccountSettingsComponent accountSettings;

  public AccountView(
      MessageProperties messages,
      SecurityService security,
      HttpServletRequest request,
      ConfigService config,
      UserService users,
      UserStatusBroadcaster broadcaster) {
    super(messages, security, users, broadcaster, request);
    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-account");
    var logo = new Image("custom-theme/service_logo.png", "Service Icon");
    logo.addClassName("cosmos-topbar-logo");
    var serviceName = new Span(config.getString(ConfigEntry.SERVICE_NAME));
    serviceName.addClassName("cosmos-topbar-service-name");
    var brand = new HorizontalLayout(logo, serviceName);
    brand.setSpacing(false);
    brand.addClassName("cosmos-topbar-brand");
    Button userButton = createTrackedUserButton();
    Runnable administration =
        currentUser != null && currentUser.hasAdministrativeRole()
            ? () -> UI.getCurrent().navigate(AdminView.class)
            : null;
    createTrackedUserPopover(
        new UserPopoverMenu.Actions(
            administration, () -> UI.getCurrent().navigate(HomeView.class), null, null));
    userPopoverMenu.addThemeName("cosmos");
    var userMenu = new HorizontalLayout(userButton);
    userMenu.setSpacing(false);
    userMenu.addClassName("cosmos-topbar-account-menu");
    var navbar = new HorizontalLayout(brand, userMenu);
    navbar.setSpacing(false);
    navbar.addClassName("cosmos-topbar");
    addToNavbar(navbar);

    accountSettings = new AccountSettingsComponent(messages, currentUser);
    var copyright =
        new CosmosCopyrightComponent(
            config, Clock.systemDefaultZone(), messages::getAccountCopyrightTemplate);
    var main = new Div(accountSettings, copyright);
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    setContent(new Div(new CosmosBackgroundComponent(), main));
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    if (accountSettings != null) accountSettings.refreshTexts();
  }
}
