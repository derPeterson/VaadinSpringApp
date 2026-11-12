package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Location;
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
import de.derpeterson.app.service.AdminAccountMailService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PageContentLayout;
import de.derpeterson.app.ui.components.PageNavigationComponent;
import de.derpeterson.app.ui.components.SupportTicketActivityIndicator;
import de.derpeterson.app.ui.components.SupportTicketsComponent;
import de.derpeterson.app.ui.components.SupportTicketsComponent.Category;
import de.derpeterson.app.ui.components.SupportTicketsComponent.NavigationState;
import de.derpeterson.app.ui.components.UnsavedChangesDialog;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.views.admin.AdminConfigurationSection;
import de.derpeterson.app.views.admin.AdminDashboardSection;
import de.derpeterson.app.views.admin.AdminExternalProviderSettingsSection;
import de.derpeterson.app.views.admin.AdminMailTestSection;
import de.derpeterson.app.views.admin.AdminTwoFactorSection;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.views.admin.AdminUserNotificationsSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

@StyleSheet("custom-theme/admin-users.css")
@StyleSheet("custom-theme/cosmos-admin-reference.css")
@Route(AppRouteConstants.ADMIN_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.ADMIN_PAGE_TITLE)
@RolesAllowed(AppRouteConstants.ADMIN_ROUTE_ROLES)
public class AdminView extends IsAuthenticatedBaseView implements BeforeLeaveObserver {
  private static final String SECTION_PARAMETER = "section";
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
  private transient UserNotificationService userNotificationService;

  private final Div adminHeadingDiv = new Div();
  private final Map<AdminSection, Component> headingsByAdminSection =
      new EnumMap<>(AdminSection.class);
  private final Map<AdminConfigGroup, Component> headingsByConfigurationGroup =
      new EnumMap<>(AdminConfigGroup.class);
  private PageNavigationComponent adminPageNavigationComponent;
  private final Span administrationHeadingSpan = new Span();
  private final Span portalHeadingSpan = new Span();
  private final SideNavItem portalHomeNavItem = new SideNavItem("");
  private SideNavItem dashboardNavItem;
  private SideNavItem configurationNavItem;
  private SideNavItem mailTestNavItem;
  private SideNavItem userManagementNavItem;
  private SideNavItem notificationsNavItem;
  private AdminUserNotificationsSection notificationsSection;
  private SupportTicketsComponent supportTicketsComponent;
  private SideNavItem supportNavItem;
  private SupportTicketActivityIndicator supportTicketActivityIndicator;
  private Tooltip twoFactorNavigationTooltip;

  private final Map<AdminConfigGroup, SideNavItem> configurationSubmenuNavItems =
      new LinkedHashMap<>();

  private VerticalLayout adminSectionContentLayout = null;
  private PageContentLayout adminPageContentLayout;
  private AdminDashboardSection dashboardSection = null;
  private AdminConfigurationSection configurationSection = null;
  private AdminExternalProviderSettingsSection externalProviderSettingsSection;
  private AdminTwoFactorSection twoFactorSection;

  private AdminMailTestSection mailTestSection = null;
  private AdminUserManagementSection userManagementSection = null;

  // Aktiver Bereich und Wiederherstellung aus der URL
  private boolean isRestoringTicketNavigation;
  private AdminSection activeAdminSection = AdminSection.DASHBOARD;
  private AdminConfigGroup activeConfigurationGroup = null;

  public AdminView(
      MessageProperties messageProperties,
      SecurityService securityService,
      ConfigService configService,
      MaintenanceService maintenanceService,
      EmailService emailService,
      UserService userService,
      RoleService roleService,
      HttpServletRequest httpServletRequest,
      UserStatusBroadcaster userStatusBroadcaster,
      UserAvatarService userAvatarService,
      ExternalIdentityService externalIdentityService) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        httpServletRequest,
        userAvatarService);

    this.configService = configService;

    addClassNames(
        "admin-view",
        "cosmos-page",
        "cosmos-theme",
        "cosmos-app-shell",
        "cosmos-admin",
        REFERENCE_PORTAL,
        "cosmos-reference-admin");

    UserDetails authenticatedUser =
        securityService.getAuthenticatedUser(httpServletRequest).orElse(null);

    HorizontalLayout adminHeaderLayout = createAdminHeaderLayout();
    adminHeadingDiv.addClassName("admin-reference-page-heading");
    VerticalLayout adminSidebarLayout = createAdminSidebarLayout();

    adminSectionContentLayout = new VerticalLayout();
    adminSectionContentLayout.setWidthFull();
    adminSectionContentLayout.setPadding(false);
    adminSectionContentLayout.setSpacing(false);
    adminSectionContentLayout.addClassName("cosmos-admin-sections");

    dashboardSection = new AdminDashboardSection(messageProperties, authenticatedUser);
    configurationSection =
        new AdminConfigurationSection(
            messageProperties, configService, this::refreshConfigurationState);
    mailTestSection =
        new AdminMailTestSection(
            messageProperties, emailService, maintenanceService, authenticatedUser);
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties, userService, roleService, securityService);
    userManagementSection.configureAvatars(userAvatarService);
    userManagementSection.configureSignInDetails(externalIdentityService);

    adminPageContentLayout =
        new PageContentLayout(adminHeadingDiv, adminSidebarLayout, adminSectionContentLayout);
    adminPageContentLayout.addClassName("admin-reference-frame");
    adminPageContentLayout.configureNavigationToggle(accountNavigationToggle);
    setPortalPage(adminHeaderLayout, adminPageContentLayout, configService);

    selectSection(AdminSection.DASHBOARD, null);

    refreshAdminViewTexts();
    configurationSection.loadConfigValues();
  }

  @Autowired
  void configureSupport(SupportTicketService supportTicketService) {
    supportNavItem = createNavigationItem(AppIcon.MESSAGES, AdminSection.SUPPORT, null);
    supportNavItem.setLabel(messageProperties.getSupportTitle());

    supportTicketActivityIndicator =
        new SupportTicketActivityIndicator(
            messageProperties, supportTicketService, request, supportNavItem);
    adminPageNavigationComponent.addItem(supportNavItem);

    supportTicketsComponent =
        new SupportTicketsComponent(messageProperties, supportTicketService, request, true);
    supportTicketsComponent.setNavigationListener(this::updateTicketLocation);
  }

  @Autowired
  public void configureAccountMailActions(AdminAccountMailService adminAccountMailService) {
    userManagementSection.configureAccountMailActions(adminAccountMailService);
  }

  @Autowired
  public void configureAdminNotifications(UserNotificationService userNotificationService) {
    this.userNotificationService = userNotificationService;

    notificationsNavItem = createNavigationItem(AppIcon.BELL, AdminSection.NOTIFICATIONS, null);
    notificationsNavItem.setLabel(messageProperties.getNotificationsTitle());
    notificationsNavItem.setVisible(userNotificationService.canSend(request.getSession(false)));
    adminPageNavigationComponent.addItem(notificationsNavItem);

    notificationsSection =
        new AdminUserNotificationsSection(messageProperties, userNotificationService, request);
  }

  @Autowired
  public void configureExternalProviders(
      ExternalProviderSettings externalProviderSettings,
      TwoFactorAuthorization twoFactorAuthorization) {
    if (canConfigureSecurity()) {
      externalProviderSettingsSection =
          new AdminExternalProviderSettingsSection(
              messageProperties, externalProviderSettings, configService);
      externalProviderSettingsSection.configureConfirmation(twoFactorAuthorization, request);
    }
  }

  @Autowired
  public void configureTwoFactor(
      TwoFactorPolicies twoFactorPolicies, TwoFactorAuthorization twoFactorAuthorization) {
    if (canConfigureSecurity()) {
      twoFactorSection =
          new AdminTwoFactorSection(
              messageProperties, twoFactorPolicies, twoFactorAuthorization, request);
    }
  }

  private boolean canConfigureSecurity() {
    return securityService.getAuthenticatedUser(request).stream()
        .flatMap(user -> user.getAuthorities().stream())
        .anyMatch(authority -> "ROLE_SUPERADMIN".equals(authority.getAuthority()));
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
    if (activeAdminSection != AdminSection.CONFIGURATION || activeConfigurationGroup == null) {
      return;
    }

    if (activeConfigurationGroup == AdminConfigGroup.TWO_FACTOR && twoFactorSection != null) {
      if (twoFactorSection.hasUnsavedChanges()) {
        UnsavedChangesDialog.confirmNavigation(
            beforeLeaveEvent, messageProperties, twoFactorSection::activate);
      }
    } else if (activeConfigurationGroup == AdminConfigGroup.EXTERNAL_LOGIN
        && externalProviderSettingsSection != null) {
      if (externalProviderSettingsSection.hasUnsavedChanges()) {
        UnsavedChangesDialog.confirmNavigation(
            beforeLeaveEvent, messageProperties, externalProviderSettingsSection::activate);
      }
    } else if (configurationSection.hasUnsavedChanges()) {
      UnsavedChangesDialog.confirmNavigation(
          beforeLeaveEvent, messageProperties, configurationSection::loadConfigValues);
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    super.beforeEnter(beforeEnterEvent);
    if (beforeEnterEvent.hasForwardTarget()) {
      return;
    }

    QueryParameters adminQueryParameters = beforeEnterEvent.getLocation().getQueryParameters();
    AdminSection requestedAdminSection =
        parseEnumQueryParameter(
            adminQueryParameters, SECTION_PARAMETER, AdminSection.class, AdminSection.DASHBOARD);
    AdminConfigGroup requestedConfigurationGroup =
        requestedAdminSection == AdminSection.CONFIGURATION
            ? parseEnumQueryParameter(
                adminQueryParameters, "group", AdminConfigGroup.class, AdminConfigGroup.GENERAL)
            : null;

    isRestoringTicketNavigation = true;
    try {
      selectSection(requestedAdminSection, requestedConfigurationGroup);
      if (activeAdminSection == AdminSection.SUPPORT) {
        Category requestedTicketCategory =
            parseEnumQueryParameter(adminQueryParameters, "tab", Category.class, Category.ALL);
        List<String> ticketNumberParameterValues =
            adminQueryParameters.getParameters().get("ticket");
        String requestedTicketNumber =
            ticketNumberParameterValues != null && ticketNumberParameterValues.size() == 1
                ? ticketNumberParameterValues.getFirst()
                : null;
        supportTicketsComponent.restoreNavigation(requestedTicketCategory, requestedTicketNumber);
      }
    } finally {
      isRestoringTicketNavigation = false;
    }
  }

  private void updateTicketLocation(NavigationState ticketNavigationState) {
    if (isRestoringTicketNavigation || activeAdminSection != AdminSection.SUPPORT) {
      return;
    }

    Map<String, String> ticketQueryParameters = new LinkedHashMap<>();
    ticketQueryParameters.put(SECTION_PARAMETER, "support");
    ticketQueryParameters.put(
        "tab", ticketNavigationState.category().name().toLowerCase(Locale.ROOT));
    if (ticketNavigationState.ticketNumber() != null) {
      ticketQueryParameters.put("ticket", ticketNavigationState.ticketNumber());
    }

    getUI()
        .ifPresent(adminUi ->
            adminUi
                .getPage()
                .getHistory()
                .replaceState(
                    null,
                    new Location(
                        AppRouteConstants.ADMIN_ROUTE,
                        QueryParameters.simple(ticketQueryParameters))));
  }

  private static <E extends Enum<E>> E parseEnumQueryParameter(
      QueryParameters queryParameters,
      String parameterName,
      Class<E> enumType,
      E defaultEnumValue) {
    List<String> parameterValues = queryParameters.getParameters().get(parameterName);
    // Fehlende, mehrdeutige oder veraltete Links bekommen einen festen Standard.
    if (parameterValues == null || parameterValues.size() != 1) {
      return defaultEnumValue;
    }
    try {
      return Enum.valueOf(enumType, parameterValues.getFirst().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException _) {
      return defaultEnumValue;
    }
  }

  @Override
  protected boolean hasRequiredAccess(UserDetails authenticatedUser) {
    return hasRole(authenticatedUser, AppRoute.ADMIN.getRoles());
  }

  private HorizontalLayout createAdminHeaderLayout() {
    return createApplicationHeader(
        configService,
        new UserPopoverMenu.UserPopoverActions(
            null, () -> UI.getCurrent().navigate(HomeView.class)));
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

  private VerticalLayout createAdminSidebarLayout() {
    adminPageNavigationComponent = new PageNavigationComponent();
    adminPageNavigationComponent.setWidthFull();
    adminPageNavigationComponent.setAutoExpand(false);

    dashboardNavItem = createNavigationItem(AppIcon.DASHBOARD, AdminSection.DASHBOARD, null);

    // Ohne Zielpfad klappt SideNav diesen Elternpunkt ausschließlich auf und zu.
    configurationNavItem = new SideNavItem("");
    configurationNavItem.setPrefixComponent(AppIcon.COGS.create());
    configurationNavItem.getElement().getClassList().add("admin-nav-item");

    mailTestNavItem = createNavigationItem(AppIcon.ENVELOPE, AdminSection.MAIL_TEST, null);
    userManagementNavItem = createNavigationItem(AppIcon.USERS, AdminSection.USER_MANAGEMENT, null);

    for (AdminConfigGroup configurationGroup : AdminConfigGroup.values()) {
      SideNavItem configurationGroupNavItem =
          createNavigationItem(
              ConfigGroupIcons.forGroup(configurationGroup),
              AdminSection.CONFIGURATION,
              configurationGroup);
      configurationGroupNavItem.getElement().getClassList().add("cosmos-nav-item--sub");
      configurationSubmenuNavItems.put(configurationGroup, configurationGroupNavItem);
      configurationNavItem.addItem(configurationGroupNavItem);
      if (configurationGroup == AdminConfigGroup.TWO_FACTOR) {
        twoFactorNavigationTooltip = Tooltip.forComponent(configurationGroupNavItem);
      }
    }

    adminPageNavigationComponent.addItem(
        dashboardNavItem, userManagementNavItem, configurationNavItem, mailTestNavItem);

    administrationHeadingSpan.addClassName("admin-portal-label");
    portalHeadingSpan.addClassName("admin-portal-label");
    portalHomeNavItem.setId("admin-nav-home");
    portalHomeNavItem.setPath(AppRoute.HOME.getSecurityRoute());
    portalHomeNavItem.setPrefixComponent(AppIcon.HOME.create());
    PageNavigationComponent portalPageNavigationComponent = new PageNavigationComponent();
    portalPageNavigationComponent.registerAction(
        portalHomeNavItem, () -> UI.getCurrent().navigate(HomeView.class));
    portalPageNavigationComponent.addItem(portalHomeNavItem);

    VerticalLayout adminSidebarLayout =
        new VerticalLayout(
            administrationHeadingSpan,
            adminPageNavigationComponent,
            portalHeadingSpan,
            portalPageNavigationComponent);
    adminSidebarLayout.setPadding(false);
    adminSidebarLayout.setSpacing(false);
    adminSidebarLayout.setWidthFull();
    adminSidebarLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    adminSidebarLayout.setId("admin-sidebar");
    adminSidebarLayout.addClassNames("cosmos-sidebar", "account-sidebar");

    updateNavigationState();
    return adminSidebarLayout;
  }

  private SideNavItem createNavigationItem(
      AppIcon navigationAppIcon,
      AdminSection targetAdminSection,
      AdminConfigGroup targetConfigurationGroup) {
    Map<String, String> navigationQueryParameters = new LinkedHashMap<>();
    navigationQueryParameters.put(
        SECTION_PARAMETER, targetAdminSection.name().toLowerCase(Locale.ROOT));
    if (targetConfigurationGroup != null) {
      navigationQueryParameters.put(
          "group", targetConfigurationGroup.name().toLowerCase(Locale.ROOT));
    }

    SideNavItem adminSideNavItem = new SideNavItem("", AdminView.class);
    adminSideNavItem.setId(
        "admin-nav-"
            + targetAdminSection.name().toLowerCase(Locale.ROOT)
            + (targetConfigurationGroup == null
                ? ""
                : "-" + targetConfigurationGroup.name().toLowerCase(Locale.ROOT)));
    // SideNavItem entfernt Query-Anteile aus setPath; deshalb die vorgesehene API nutzen.
    adminSideNavItem.setQueryParameters(QueryParameters.simple(navigationQueryParameters));
    if (navigationAppIcon != null) {
      adminSideNavItem.setPrefixComponent(navigationAppIcon.create());
    }
    adminSideNavItem.getElement().getClassList().add("admin-nav-item");

    adminPageNavigationComponent.registerAction(
        adminSideNavItem,
        () ->
            UI.getCurrent()
                .navigate(AdminView.class, QueryParameters.simple(navigationQueryParameters)));
    return adminSideNavItem;
  }

  private void selectSection(
      AdminSection requestedAdminSection, AdminConfigGroup configurationGroup) {
    AdminConfigGroup requestedConfigurationGroup =
        configurationGroup == null ? activeConfigurationGroup : configurationGroup;
    if (requestedAdminSection == AdminSection.CONFIGURATION
        && requestedConfigurationGroup != null
        && requestedConfigurationGroup.isSuperadminOnly()
        && !canConfigureSecurity()) {
      configurationGroup = AdminConfigGroup.GENERAL;
    }

    mailTestSection.refreshDeliveryState();
    boolean canSendNotifications =
        userNotificationService != null
            && userNotificationService.canSend(request.getSession(false));
    if (notificationsNavItem != null) {
      notificationsNavItem.setVisible(canSendNotifications);
    }

    if (requestedAdminSection == AdminSection.NOTIFICATIONS && !canSendNotifications) {
      requestedAdminSection = AdminSection.DASHBOARD;
    }
    activeAdminSection = requestedAdminSection;

    selectConfigurationGroup(requestedAdminSection, configurationGroup);

    if (adminSectionContentLayout == null) {
      return;
    }

    adminSectionContentLayout.removeAll();

    showAdminSectionContent(requestedAdminSection);

    updatePageHeading();
    adminPageContentLayout.closeNavigation();
    updateNavigationState();
  }

  private void showAdminSectionContent(AdminSection requestedAdminSection) {
    switch (requestedAdminSection) {
      case CONFIGURATION -> {
        if (activeConfigurationGroup == AdminConfigGroup.TWO_FACTOR && twoFactorSection != null) {
          twoFactorSection.activate();
          adminSectionContentLayout.add(twoFactorSection);
        } else if (activeConfigurationGroup == AdminConfigGroup.EXTERNAL_LOGIN
            && externalProviderSettingsSection != null) {
          externalProviderSettingsSection.activate();
          adminSectionContentLayout.add(externalProviderSettingsSection);
        } else {
          adminSectionContentLayout.add(configurationSection);
          configurationSection.showGroup(activeConfigurationGroup);
        }
      }
      case NOTIFICATIONS -> {
        adminSectionContentLayout.add(notificationsSection);
        notificationsSection.activate();
      }
      case SUPPORT -> {
        adminSectionContentLayout.add(supportTicketsComponent);
        supportTicketsComponent.refreshTicketWorkspace();
      }
      case MAIL_TEST -> adminSectionContentLayout.add(mailTestSection);
      case USER_MANAGEMENT -> adminSectionContentLayout.add(userManagementSection);
      default -> adminSectionContentLayout.add(dashboardSection);
    }
  }

  private void selectConfigurationGroup(
      AdminSection requestedAdminSection, AdminConfigGroup configurationGroup) {
    if (requestedAdminSection == AdminSection.CONFIGURATION) {
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
    VerticalLayout selectedSectionLayout =
        (VerticalLayout) adminSectionContentLayout.getChildren().findFirst().orElseThrow();
    selectedSectionLayout
        .getChildren()
        .filter(CosmosSectionHeaderComponent.class::isInstance)
        .findFirst()
        .ifPresent(sectionHeaderComponent -> {
          if (hasDedicatedConfigurationHeading()) {
            headingsByConfigurationGroup.put(activeConfigurationGroup, sectionHeaderComponent);
          } else {
            headingsByAdminSection.put(activeAdminSection, sectionHeaderComponent);
          }
          selectedSectionLayout.remove(sectionHeaderComponent);
        });

    adminHeadingDiv.removeAll();
    Component activeHeadingComponent =
        hasDedicatedConfigurationHeading()
            ? headingsByConfigurationGroup.get(activeConfigurationGroup)
            : headingsByAdminSection.get(activeAdminSection);
    if (activeHeadingComponent != null) {
      adminHeadingDiv.add(activeHeadingComponent);
    }
  }

  private boolean hasDedicatedConfigurationHeading() {
    return activeAdminSection == AdminSection.CONFIGURATION
        && (activeConfigurationGroup == AdminConfigGroup.TWO_FACTOR
            || activeConfigurationGroup == AdminConfigGroup.EXTERNAL_LOGIN);
  }

  private void updateNavigationState() {
    applyNavigationItemState(supportNavItem, activeAdminSection == AdminSection.SUPPORT);
    applyNavigationItemState(
        notificationsNavItem, activeAdminSection == AdminSection.NOTIFICATIONS);
    applyNavigationItemState(dashboardNavItem, activeAdminSection == AdminSection.DASHBOARD);
    applyNavigationItemState(
        configurationNavItem, activeAdminSection == AdminSection.CONFIGURATION);
    applyNavigationItemState(mailTestNavItem, activeAdminSection == AdminSection.MAIL_TEST);
    applyNavigationItemState(
        userManagementNavItem, activeAdminSection == AdminSection.USER_MANAGEMENT);

    // Der Elternpunkt klappt nur auf; die aktive Seite ist der Unterpunkt.
    if (configurationNavItem != null) {
      configurationNavItem.getElement().removeAttribute(ARIA_CURRENT);
    }

    configurationSubmenuNavItems.forEach((configurationGroup, configurationGroupNavItem) -> {
      configurationGroupNavItem.setVisible(
          !configurationGroup.isSuperadminOnly() || canConfigureSecurity());
      boolean isActive =
          activeAdminSection == AdminSection.CONFIGURATION
              && Objects.equals(activeConfigurationGroup, configurationGroup);
      applyNavigationItemState(configurationGroupNavItem, isActive);
    });
  }

  private void applyNavigationItemState(SideNavItem sectionNavItem, boolean isSectionActive) {
    if (sectionNavItem == null) {
      return;
    }

    PageNavigationComponent.setItemActive(sectionNavItem, isSectionActive);
  }

  private void refreshAdminViewTexts() {
    if (supportNavItem != null) {
      supportNavItem.setLabel(messageProperties.getSupportTitle());
      supportTicketActivityIndicator.refreshTexts();
      supportTicketsComponent.refreshTexts();
    }

    administrationHeadingSpan.setText(messageProperties.getAdminHeaderTitle());
    portalHeadingSpan.setText(messageProperties.getAccountNavigationPortal());
    portalHomeNavItem.setLabel(messageProperties.getHomeTitle());
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

    configurationSubmenuNavItems.forEach((configurationGroup, configurationGroupNavItem) ->
        configurationGroupNavItem.setLabel(
            configurationGroup == AdminConfigGroup.TWO_FACTOR
                ? messageProperties.getTwoFactorNavigationLabel()
                : messageProperties.getAdminConfigGroupLabel(configurationGroup)));
    twoFactorNavigationTooltip.setText(messageProperties.getTwoFactorSettingsTitle());

    adminPageNavigationComponent
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminHeaderTitle());
    adminPageNavigationComponent.setI18n(
        new SideNav.SideNavI18n().setToggle(messageProperties.getAdminNavToggleChildren()));

    if (dashboardSection != null) {
      dashboardSection.refreshTexts();
    }
    if (configurationSection != null) {
      configurationSection.refreshTexts();
    }
    if (externalProviderSettingsSection != null) {
      externalProviderSettingsSection.refreshTexts();
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
    refreshAdminViewTexts();
  }
}
