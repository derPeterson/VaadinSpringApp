package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorPolicies;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.PageNavigationComponent;
import de.derpeterson.app.ui.components.PortalPageComponent;
import de.derpeterson.app.ui.components.SupportTicketsComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.views.admin.AdminConfigurationSection;
import de.derpeterson.app.views.admin.AdminDashboardSection;
import de.derpeterson.app.views.admin.AdminExternalLoginSection;
import de.derpeterson.app.views.admin.AdminMailTestSection;
import de.derpeterson.app.views.admin.AdminTwoFactorSection;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.userdetails.UserDetails;

@StyleSheet("custom-theme/admin-users.css")
@StyleSheet("custom-theme/cosmos-admin-reference.css")
@Route(AppRouteConstants.ADMIN_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.ADMIN_PAGE_TITLE)
@RolesAllowed(AppRouteConstants.ADMIN_ROUTE_ROLES)
public class AdminView extends IsAuthenticatedBaseView {
  private static final String ARIA_CURRENT = "aria-current";

  private enum AdminSection {
    DASHBOARD,
    CONFIGURATION,
    MAIL_TEST,
    USER_MANAGEMENT,
    NOTIFICATIONS,
    SUPPORT
  }

  private final transient ConfigService configService;

  private final Div pageHeading = new Div();
  private final Map<AdminSection, com.vaadin.flow.component.Component> pageHeadings =
      new EnumMap<>(AdminSection.class);
  private final Map<AdminConfigGroup, com.vaadin.flow.component.Component>
      specialConfigurationHeadings = new EnumMap<>(AdminConfigGroup.class);
  private PageNavigationComponent sideNavigation;
  private final Span administrationHeading = new Span();
  private final Span portalHeading = new Span();
  private final SideNavItem portalHome = new SideNavItem("");
  private SideNavItem dashboardNavItem;
  private SideNavItem configurationNavItem;
  private SideNavItem mailTestNavItem;
  private SideNavItem userManagementNavItem;
  private SideNavItem notificationsNavItem;
  private de.derpeterson.app.views.admin.AdminUserNotificationsSection notificationsSection;
  private transient de.derpeterson.app.service.UserNotificationService notificationsService;
  private SupportTicketsComponent supportSection;
  private SideNavItem supportNavItem;
  private Tooltip twoFactorNavigationTooltip;

  @org.springframework.beans.factory.annotation.Autowired
  void configureSupport(SupportTicketService support) {
    supportNavItem = createNavigationItem(AppIcon.MESSAGES, AdminSection.SUPPORT, null);
    supportNavItem.setLabel(messageProperties.getSupportTitle());
    sideNavigation.addItem(supportNavItem);
    supportSection = new SupportTicketsComponent(messageProperties, support, request, true);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public void configureAdminNotifications(
      de.derpeterson.app.service.UserNotificationService service) {
    notificationsService = service;
    notificationsNavItem = createNavigationItem(AppIcon.BELL, AdminSection.NOTIFICATIONS, null);
    notificationsNavItem.setLabel(messageProperties.getNotificationsTitle());
    notificationsNavItem.setVisible(service.canSend(request.getSession(false)));
    sideNavigation.addItem(notificationsNavItem);
    notificationsSection =
        new de.derpeterson.app.views.admin.AdminUserNotificationsSection(
            messageProperties, service, request);
  }

  private final Map<AdminConfigGroup, SideNavItem> configurationSubmenuItems =
      new LinkedHashMap<>();

  private VerticalLayout sectionLayout = null;
  private PortalPageComponent page;
  private AdminDashboardSection dashboardSection = null;
  private AdminConfigurationSection configurationSection = null;
  private AdminExternalLoginSection externalLoginSection;
  private AdminTwoFactorSection twoFactorSection;

  @org.springframework.beans.factory.annotation.Autowired
  public void configureExternalProviders(
      ExternalProviderSettings settings, TwoFactorAuthorization authorization) {
    if (canConfigureSecurity()) {
      externalLoginSection =
          new AdminExternalLoginSection(messageProperties, settings, configService);
      externalLoginSection.configureConfirmation(authorization, request);
    }
  }

  @org.springframework.beans.factory.annotation.Autowired
  public void configureTwoFactor(TwoFactorPolicies policies, TwoFactorAuthorization authorization) {
    if (canConfigureSecurity()) {
      twoFactorSection =
          new AdminTwoFactorSection(messageProperties, policies, authorization, request);
    }
  }

  private boolean canConfigureSecurity() {
    return securityService.getAuthenticatedUser(request).stream()
        .flatMap(user -> user.getAuthorities().stream())
        .anyMatch(authority -> "ROLE_SUPERADMIN".equals(authority.getAuthority()));
  }

  private AdminMailTestSection mailTestSection = null;
  private AdminUserManagementSection userManagementSection = null;

  private AdminSection activeSection = AdminSection.DASHBOARD;
  private AdminConfigGroup activeConfigurationGroup = null;

  public AdminView(
      MessageProperties messageProperties,
      SecurityService securityService,
      ConfigService configService,
      MaintenanceService maintenance,
      EmailService emailService,
      UserService userService,
      RoleService roleService,
      HttpServletRequest request,
      UserStatusBroadcaster userStatusBroadcaster,
      UserAvatarService avatars) {
    super(messageProperties, securityService, userService, userStatusBroadcaster, request, avatars);

    this.configService = configService;
    addClassNames(
        "admin-view",
        "cosmos-page",
        "cosmos-theme",
        "cosmos-app-shell",
        "cosmos-admin",
        REFERENCE_PORTAL,
        "cosmos-reference-admin");

    UserDetails authenticatedUser = securityService.getAuthenticatedUser(request).orElse(null);

    HorizontalLayout header = createNavbar();
    pageHeading.addClassName("admin-reference-page-heading");
    VerticalLayout sidebar = createSideNavigation();

    sectionLayout = new VerticalLayout();
    sectionLayout.setWidthFull();
    sectionLayout.setPadding(false);
    sectionLayout.setSpacing(false);
    sectionLayout.addClassName("cosmos-admin-sections");

    dashboardSection = new AdminDashboardSection(messageProperties, authenticatedUser);
    configurationSection =
        new AdminConfigurationSection(
            messageProperties, configService, this::refreshConfigurationState);
    mailTestSection =
        new AdminMailTestSection(messageProperties, emailService, maintenance, authenticatedUser);
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties, userService, roleService, securityService);
    userManagementSection.configureAvatars(avatars);

    page = new PortalPageComponent(pageHeading, sidebar, sectionLayout);
    page.addClassName("admin-reference-frame");
    page.configureNavigationToggle(accountNavigationToggle);
    setPortalPage(header, page, configService);

    selectSection(AdminSection.DASHBOARD, null);
    refreshTexts();
    configurationSection.loadConfigValues();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    super.beforeEnter(event);
    if (event.hasForwardTarget()) {
      return;
    }

    QueryParameters parameters = event.getLocation().getQueryParameters();
    AdminSection section =
        parameter(parameters, "section", AdminSection.class, AdminSection.DASHBOARD);
    AdminConfigGroup group =
        section == AdminSection.CONFIGURATION
            ? parameter(parameters, "group", AdminConfigGroup.class, AdminConfigGroup.GENERAL)
            : null;
    selectSection(section, group);
    List<String> ticketNumbers = parameters.getParameters().get("ticket");
    if (activeSection == AdminSection.SUPPORT
        && ticketNumbers != null
        && ticketNumbers.size() == 1) {
      supportSection.openTicket(ticketNumbers.getFirst());
    }
  }

  private static <E extends Enum<E>> E parameter(
      QueryParameters parameters, String key, Class<E> type, E fallback) {
    List<String> values = parameters.getParameters().get(key);
    // Fehlende, mehrdeutige oder veraltete Links bekommen einen festen Standard.
    if (values == null || values.size() != 1) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, values.getFirst().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException _) {
      return fallback;
    }
  }

  @Override
  protected boolean hasRequiredAccess(UserDetails authenticatedUser) {
    return hasRole(authenticatedUser, AppRoute.ADMIN.getRoles());
  }

  private HorizontalLayout createNavbar() {
    return createApplicationHeader(
        configService,
        new UserPopoverMenu.Actions(null, () -> UI.getCurrent().navigate(HomeView.class)));
  }

  private void refreshConfigurationState() {
    refreshServiceName();
    mailTestSection.refreshDeliveryState();
    if (notificationsSection != null) {
      notificationsSection.refreshDeliveryState();
    }
  }

  private void refreshServiceName() {
    refreshApplicationServiceName(configService.getString(ConfigEntry.SERVICE_NAME));
  }

  private VerticalLayout createSideNavigation() {
    sideNavigation = new PageNavigationComponent();
    sideNavigation.setWidthFull();
    sideNavigation.setAutoExpand(false);

    dashboardNavItem = createNavigationItem(AppIcon.DASHBOARD, AdminSection.DASHBOARD, null);
    // Ohne Zielpfad klappt SideNav diesen Elternpunkt ausschließlich auf und zu.
    configurationNavItem = new SideNavItem("");
    configurationNavItem.setPrefixComponent(AppIcon.COGS.create());
    configurationNavItem.getElement().getClassList().add("admin-nav-item");
    mailTestNavItem = createNavigationItem(AppIcon.ENVELOPE, AdminSection.MAIL_TEST, null);
    userManagementNavItem = createNavigationItem(AppIcon.USERS, AdminSection.USER_MANAGEMENT, null);

    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      SideNavItem submenuItem =
          createNavigationItem(ConfigGroupIcons.forGroup(group), AdminSection.CONFIGURATION, group);
      submenuItem.getElement().getClassList().add("cosmos-nav-item--sub");
      configurationSubmenuItems.put(group, submenuItem);
      configurationNavItem.addItem(submenuItem);
      if (group == AdminConfigGroup.TWO_FACTOR) {
        twoFactorNavigationTooltip = Tooltip.forComponent(submenuItem);
      }
    }

    sideNavigation.addItem(
        dashboardNavItem, userManagementNavItem, configurationNavItem, mailTestNavItem);

    Span portalLabel = portalHeading;
    administrationHeading.addClassName("admin-portal-label");
    portalLabel.addClassName("admin-portal-label");
    SideNavItem portal = portalHome;
    portal.setId("admin-nav-home");
    portal.setPath(AppRoute.HOME.getSecurityRoute());
    portal.setPrefixComponent(AppIcon.HOME.create());
    PageNavigationComponent portalNavigation = new PageNavigationComponent();
    portalNavigation.registerAction(portal, () -> UI.getCurrent().navigate(HomeView.class));
    portalNavigation.addItem(portal);
    VerticalLayout drawerContent =
        new VerticalLayout(administrationHeading, sideNavigation, portalLabel, portalNavigation);
    drawerContent.setPadding(false);
    drawerContent.setSpacing(false);
    drawerContent.setWidthFull();
    drawerContent.setAlignItems(FlexComponent.Alignment.STRETCH);
    drawerContent.setId("admin-sidebar");
    drawerContent.addClassNames("cosmos-sidebar", "account-sidebar");
    updateNavigationState();
    return drawerContent;
  }

  private SideNavItem createNavigationItem(
      AppIcon iconType, AdminSection section, AdminConfigGroup group) {
    Map<String, String> parameters = new LinkedHashMap<>();
    parameters.put("section", section.name().toLowerCase(Locale.ROOT));
    if (group != null) {
      parameters.put("group", group.name().toLowerCase(Locale.ROOT));
    }
    SideNavItem item = new SideNavItem("", AdminView.class);
    item.setId(
        "admin-nav-"
            + section.name().toLowerCase(Locale.ROOT)
            + (group == null ? "" : "-" + group.name().toLowerCase(Locale.ROOT)));
    // SideNavItem entfernt Query-Anteile aus setPath; deshalb die vorgesehene API nutzen.
    item.setQueryParameters(QueryParameters.simple(parameters));
    if (iconType != null) {
      item.setPrefixComponent(iconType.create());
    }
    item.getElement().getClassList().add("admin-nav-item");
    sideNavigation.registerAction(
        item, () -> UI.getCurrent().navigate(AdminView.class, QueryParameters.simple(parameters)));
    return item;
  }

  private void selectSection(AdminSection section, AdminConfigGroup configurationGroup) {
    AdminConfigGroup requestedGroup =
        configurationGroup == null ? activeConfigurationGroup : configurationGroup;
    if (section == AdminSection.CONFIGURATION
        && requestedGroup != null
        && requestedGroup.isSuperadminOnly()
        && !canConfigureSecurity()) {
      configurationGroup = AdminConfigGroup.GENERAL;
    }
    mailTestSection.refreshDeliveryState();
    boolean canSend =
        notificationsService != null && notificationsService.canSend(request.getSession(false));
    if (notificationsNavItem != null) {
      notificationsNavItem.setVisible(canSend);
    }
    if (section == AdminSection.NOTIFICATIONS && !canSend) {
      section = AdminSection.DASHBOARD;
    }
    activeSection = section;

    selectConfigurationGroup(section, configurationGroup);

    if (sectionLayout == null) {
      return;
    }

    sectionLayout.removeAll();

    switch (section) {
      case CONFIGURATION -> {
        if (activeConfigurationGroup == AdminConfigGroup.TWO_FACTOR && twoFactorSection != null) {
          twoFactorSection.activate();
          sectionLayout.add(twoFactorSection);
        } else if (activeConfigurationGroup == AdminConfigGroup.EXTERNAL_LOGIN
            && externalLoginSection != null) {
          externalLoginSection.activate();
          sectionLayout.add(externalLoginSection);
        } else {
          sectionLayout.add(configurationSection);
          configurationSection.showGroup(activeConfigurationGroup);
        }
      }
      case NOTIFICATIONS -> {
        sectionLayout.add(notificationsSection);
        notificationsSection.activate();
      }
      case SUPPORT -> {
        sectionLayout.add(supportSection);
        supportSection.refresh();
      }
      case MAIL_TEST -> sectionLayout.add(mailTestSection);
      case USER_MANAGEMENT -> sectionLayout.add(userManagementSection);
      default -> sectionLayout.add(dashboardSection);
    }

    updatePageHeading();
    page.closeNavigation();
    updateNavigationState();
  }

  private void selectConfigurationGroup(AdminSection section, AdminConfigGroup configurationGroup) {
    if (section == AdminSection.CONFIGURATION) {
      configurationNavItem.setExpanded(true);
      if (configurationGroup != null) {
        activeConfigurationGroup = configurationGroup;
      } else if (activeConfigurationGroup == null) {
        activeConfigurationGroup = AdminConfigGroup.GENERAL;
      }
    } else {
      activeConfigurationGroup = null;
    }
  }

  private void updatePageHeading() {
    VerticalLayout selected =
        (VerticalLayout) sectionLayout.getChildren().findFirst().orElseThrow();
    selected
        .getChildren()
        .filter(de.derpeterson.app.ui.components.CosmosSectionHeaderComponent.class::isInstance)
        .findFirst()
        .ifPresent(
            header -> {
              if (activeSection == AdminSection.CONFIGURATION
                  && activeConfigurationGroup != null
                  && activeConfigurationGroup.isSuperadminOnly()) {
                specialConfigurationHeadings.put(activeConfigurationGroup, header);
              } else {
                pageHeadings.put(activeSection, header);
              }
              selected.remove(header);
            });
    pageHeading.removeAll();
    com.vaadin.flow.component.Component heading =
        activeSection == AdminSection.CONFIGURATION
                && activeConfigurationGroup != null
                && activeConfigurationGroup.isSuperadminOnly()
            ? specialConfigurationHeadings.get(activeConfigurationGroup)
            : pageHeadings.get(activeSection);
    if (heading != null) {
      pageHeading.add(heading);
    }
  }

  private void updateNavigationState() {
    applyNavigationItemState(supportNavItem, activeSection == AdminSection.SUPPORT);
    applyNavigationItemState(notificationsNavItem, activeSection == AdminSection.NOTIFICATIONS);
    applyNavigationItemState(dashboardNavItem, activeSection == AdminSection.DASHBOARD);
    applyNavigationItemState(configurationNavItem, activeSection == AdminSection.CONFIGURATION);
    applyNavigationItemState(mailTestNavItem, activeSection == AdminSection.MAIL_TEST);
    applyNavigationItemState(userManagementNavItem, activeSection == AdminSection.USER_MANAGEMENT);

    // Der Elternpunkt klappt nur auf; die aktive Seite ist der Unterpunkt.
    if (configurationNavItem != null) {
      configurationNavItem.getElement().removeAttribute(ARIA_CURRENT);
    }

    configurationSubmenuItems.forEach(
        (group, item) -> {
          item.setVisible(!group.isSuperadminOnly() || canConfigureSecurity());
          boolean isActive =
              activeSection == AdminSection.CONFIGURATION
                  && Objects.equals(activeConfigurationGroup, group);
          applyNavigationItemState(item, isActive);
        });
  }

  private void applyNavigationItemState(SideNavItem item, boolean active) {
    if (item == null) {
      return;
    }

    PageNavigationComponent.setItemActive(item, active);
  }

  private void refreshTexts() {
    if (supportNavItem != null) {
      supportNavItem.setLabel(messageProperties.getSupportTitle());
      supportSection.refreshTexts();
    }
    administrationHeading.setText(messageProperties.getAdminHeaderTitle());
    portalHeading.setText(messageProperties.getAccountNavigationPortal());
    portalHome.setLabel(messageProperties.getHomeTitle());
    mailTestSection.refreshDeliveryState();

    if (notificationsNavItem != null) {
      notificationsNavItem.setLabel(messageProperties.getNotificationsTitle());
    }
    if (notificationsSection != null) {
      notificationsSection.refreshTexts();
    }
    dashboardNavItem.setLabel(messageProperties.getAdminNavDashboard());
    configurationNavItem.setLabel(messageProperties.getAdminNavConfiguration());
    mailTestNavItem.setLabel(messageProperties.getAdminNavMailTest());
    userManagementNavItem.setLabel(messageProperties.getAdminUsersTitle());

    configurationSubmenuItems.forEach(
        (group, item) ->
            item.setLabel(
                group == AdminConfigGroup.TWO_FACTOR
                    ? messageProperties.getTwoFactorNavigationLabel()
                    : messageProperties.getAdminConfigGroupLabel(group)));
    twoFactorNavigationTooltip.setText(messageProperties.getTwoFactorSettingsTitle());
    sideNavigation.getElement().setAttribute("aria-label", messageProperties.getAdminHeaderTitle());
    sideNavigation.setI18n(
        new SideNav.SideNavI18n().setToggle(messageProperties.getAdminNavToggleChildren()));

    if (dashboardSection != null) {
      dashboardSection.refreshTexts();
    }
    if (configurationSection != null) {
      configurationSection.refreshTexts();
    }
    if (externalLoginSection != null) {
      externalLoginSection.refreshTexts();
    }
    if (twoFactorSection != null) {
      twoFactorSection.refreshTexts();
    }
    mailTestSection.refreshTexts();
    if (userManagementSection != null) {
      userManagementSection.refreshTexts();
    }

    updateNavigationState();
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    refreshTexts();
  }
}
