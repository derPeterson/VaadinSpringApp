package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
      AccountProfileService profiles,
      AccountSecurityService accountSecurity,
      SessionManagementService sessions,
      UserStatusBroadcaster broadcaster,
      UserAvatarService avatars) {
    super(messages, security, users, broadcaster, request, avatars);
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
    var userMenu = new HorizontalLayout(accountHeaderActions, userButton);
    userMenu.setSpacing(false);
    userMenu.addClassName("cosmos-topbar-account-menu");
    var navbar = new HorizontalLayout(brand, userMenu);
    navbar.setSpacing(false);
    navbar.addClassName("cosmos-topbar");
    addToNavbar(navbar);

    accountSettings =
        new AccountSettingsComponent(
            messages,
            currentUser,
            changes -> profiles.save(request, changes),
            config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER));
    accountSettings.configureAvatars(avatars, request);
    accountSettings.configureSecurityActions(
        change -> {
          var changed = accountSecurity.changePassword(request, change);
          if (Objects.equals(currentUser.getVersion(), changed.previousVersion()))
            currentUser.setVersion(changed.version());
          return changed;
        },
        change -> accountSecurity.requestEmailChange(request, change));
    accountSettings.configureSessionActions(
        () -> sessions.list(request),
        handle -> sessions.revoke(request, handle),
        () -> sessions.revokeOthers(request));
    accountSettings.addSectionChangeListener(
        section ->
            UI.getCurrent()
                .navigate(
                    AccountView.class,
                    QueryParameters.simple(
                        Map.of("tab", section.name().toLowerCase(Locale.ROOT)))));
    // Erst nach bestätigtem Commit den sichtbaren UI-Zustand und die Sitzungssprache ändern.
    accountSettings.addProfileSavedListener(
        saved -> {
          currentUser.setFirstName(saved.firstName());
          currentUser.setLastName(saved.lastName());
          currentUser.setBirthDate(saved.birthDate());
          currentUser.setPreferredLocale(saved.language());
          currentUser.setVersion(saved.version());
          userPopoverMenu.refreshProfileName();
          refreshTrackedUserAvatar();
          if (!saved.language().equals(VaadinSession.getCurrent().getLocale())) {
            LanguageChangeEvent.fire(UI.getCurrent(), saved.language());
          }
        });
    var copyright =
        new CosmosCopyrightComponent(
            config, Clock.systemDefaultZone(), messages::getAccountCopyrightTemplate);
    var main = new Div(accountSettings, copyright);
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    setContent(new Div(new CosmosBackgroundComponent(), main));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    super.beforeEnter(event);
    if (event.hasForwardTarget()) return;
    var values = event.getLocation().getQueryParameters().getParameters().get("tab");
    AccountSection section = AccountSection.PROFILE;
    // Fehlende, unbekannte und mehrdeutige Tabwerte öffnen immer das Profil.
    if (values != null && values.size() == 1) {
      try {
        section = AccountSection.valueOf(values.getFirst().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException invalid) {
        section = AccountSection.PROFILE;
      }
    }
    accountSettings.selectSection(section);
  }

  @Override
  protected void onTrackedUserProfileChanged(String first, String last) {
    if (accountSettings != null) accountSettings.refreshSummaryName(first, last);
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus status) {
    super.updateHeaderStatusIcon(status);
    if (accountSettings != null) accountSettings.updateUserStatus(status);
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    if (accountSettings != null) accountSettings.refreshTexts();
  }
}
