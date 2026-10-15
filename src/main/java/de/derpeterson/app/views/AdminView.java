package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.ConfigGroupIcons;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.views.admin.AdminConfigurationSection;
import de.derpeterson.app.views.admin.AdminDashboardSection;
import de.derpeterson.app.views.admin.AdminMailTestSection;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@StyleSheet("custom-theme/admin-users.css")
@Route(AppRouteConstants.ADMIN_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.ADMIN_PAGE_TITLE)
@RolesAllowed(AppRouteConstants.ADMIN_ROUTE_ROLES)
public class AdminView extends IsAuthenticatedBaseView {

    private enum AdminSection {
        DASHBOARD,
        CONFIGURATION,
        MAIL_TEST,
        USER_MANAGEMENT
    }


    private final transient ConfigService configService;

    private Span headerTitle = null;

    private final Span serviceNameSpan = new Span();

    private SideNav sideNavigation;
    private SideNavItem dashboardNavItem;
    private SideNavItem configurationNavItem;
    private SideNavItem mailTestNavItem;
    private SideNavItem userManagementNavItem;
    private final Map<AdminConfigGroup, SideNavItem> configurationSubmenuItems = new LinkedHashMap<>();
    private final Map<String, Runnable> navigationActions = new LinkedHashMap<>();

    private VerticalLayout sectionLayout = null;
    private AdminDashboardSection dashboardSection = null;
    private AdminConfigurationSection configurationSection = null;
    private AdminMailTestSection mailTestSection = null;
    private AdminUserManagementSection userManagementSection = null;

    private AdminSection activeSection = AdminSection.DASHBOARD;
    private AdminConfigGroup activeConfigurationGroup = null;

    public AdminView(MessageProperties messageProperties,
                     SecurityService securityService,
                     ConfigService configService,
                     EmailService emailService,
                     UserService userService,
                     RoleService roleService,
                     HttpServletRequest request,
                     UserStatusBroadcaster userStatusBroadcaster) {
        super(messageProperties, securityService, userService, userStatusBroadcaster, request);

        this.configService = configService;
        addClassNames("admin-view", "cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-admin");

        UserDetails authenticatedUser = securityService.getAuthenticatedUser(request).orElse(null);

        addToNavbar(createNavbar());
        addToDrawer(createSideNavigation());

        sectionLayout = new VerticalLayout();
        sectionLayout.setWidthFull();
        sectionLayout.setPadding(false);
        sectionLayout.setSpacing(false);
        sectionLayout.addClassName("cosmos-admin-sections");

        dashboardSection = new AdminDashboardSection(messageProperties, authenticatedUser);
        configurationSection = new AdminConfigurationSection(
                messageProperties,
                configService,
                this::refreshServiceName
        );
        mailTestSection = new AdminMailTestSection(messageProperties, emailService, authenticatedUser);
        userManagementSection = new AdminUserManagementSection(
                messageProperties,
                userService,
                roleService,
                securityService
        );

        CosmosCopyrightComponent copyright = new CosmosCopyrightComponent(configService,
                Clock.systemDefaultZone(), messageProperties::getAccountCopyrightTemplate);
        Div main = new Div(sectionLayout, copyright);
        main.addClassName("cosmos-app-main");
        main.getElement().setAttribute("role", "main");
        setContent(new Div(new CosmosBackgroundComponent(), main));

        selectSection(AdminSection.DASHBOARD, null);
        refreshTexts();
        configurationSection.loadConfigValues();
    }

    @Override
    protected boolean hasRequiredAccess(UserDetails authenticatedUser) {
        return hasRole(authenticatedUser, AppRouteConstants.ADMIN_ROUTE_ROLES);
    }

    private HorizontalLayout createNavbar() {
        HorizontalLayout navbar = new HorizontalLayout(createLeftNavbarLayout(), createRightNavbarLayout());
        navbar.setSpacing(false);
        navbar.addClassName("cosmos-topbar");
        return navbar;
    }

    private HorizontalLayout createLeftNavbarLayout() {
        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(false);
        leftLayout.addClassName("cosmos-topbar-brand");

        Image drawerToggleIcon = new Image("custom-theme/service_logo.png", "Service Icon");
        drawerToggleIcon.addClassName("cosmos-topbar-logo");
        Button drawerToggle = new Button(drawerToggleIcon);
        drawerToggle.addThemeVariants(LUMO_TERTIARY_INLINE);
        drawerToggle.addClassName("cosmos-drawer-toggle");
        drawerToggle.getElement().setAttribute("aria-label", "Toggle navigation");
        drawerToggle.addClickListener(event -> setDrawerOpened(!isDrawerOpened()));

        serviceNameSpan.addClassName("cosmos-topbar-service-name");
        refreshServiceName();
        headerTitle = new Span();
        headerTitle.addClassName("cosmos-topbar-badge");
        leftLayout.add(drawerToggle, serviceNameSpan, headerTitle);
        return leftLayout;
    }

    private void refreshServiceName() {
        serviceNameSpan.setText(configService.getString(ConfigEntry.SERVICE_NAME));
    }

    private HorizontalLayout createRightNavbarLayout() {
        HorizontalLayout rightLayout = new HorizontalLayout();
        rightLayout.setSpacing(false);
        rightLayout.addClassName("cosmos-topbar-account-menu");

        Button userButton = createTrackedUserButton();

        UserPopoverMenu.Actions popoverActions = new UserPopoverMenu.Actions(
                null,
                () -> UI.getCurrent().navigate(HomeView.class),
                null,
                null
        );

        createTrackedUserPopover(popoverActions);
        userPopoverMenu.addThemeName("cosmos");
        rightLayout.add(userButton);

        return rightLayout;
    }

    private VerticalLayout createSideNavigation() {
        sideNavigation = new SideNav();
        sideNavigation.setWidthFull();
        sideNavigation.addClassName("cosmos-nav");
        sideNavigation.setAutoExpand(false);

        dashboardNavItem = createNavigationItem(VaadinIcon.DASHBOARD, AdminSection.DASHBOARD, null);
        // Ohne Zielpfad klappt SideNav diesen Elternpunkt ausschließlich auf und zu.
        configurationNavItem = new SideNavItem("");
        configurationNavItem.setPrefixComponent(VaadinIcon.COGS.create());
        configurationNavItem.getElement().getClassList().add("admin-nav-item");
        mailTestNavItem = createNavigationItem(VaadinIcon.ENVELOPE, AdminSection.MAIL_TEST, null);
        userManagementNavItem = createNavigationItem(VaadinIcon.USERS, AdminSection.USER_MANAGEMENT, null);

        for (AdminConfigGroup group : AdminConfigGroup.values()) {
            SideNavItem submenuItem = createNavigationItem(ConfigGroupIcons.forGroup(group), AdminSection.CONFIGURATION, group);
            submenuItem.getElement().getClassList().add("cosmos-nav-item--sub");
            configurationSubmenuItems.put(group, submenuItem);
            configurationNavItem.addItem(submenuItem);
        }

        sideNavigation.addItem(dashboardNavItem, configurationNavItem, mailTestNavItem, userManagementNavItem);
        installSideNavigationActions();

        VerticalLayout drawerContent = new VerticalLayout(sideNavigation);
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);
        drawerContent.setWidthFull();
        drawerContent.setAlignItems(FlexComponent.Alignment.STRETCH);
        drawerContent.addClassName("cosmos-sidebar");
        updateNavigationState();
        return drawerContent;
    }

    private SideNavItem createNavigationItem(VaadinIcon iconType, AdminSection section, AdminConfigGroup group) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("section", section.name().toLowerCase(Locale.ROOT));
        if (group != null) {
            parameters.put("group", group.name().toLowerCase(Locale.ROOT));
        }
        SideNavItem item = new SideNavItem("", AdminView.class);
        // SideNavItem entfernt Query-Anteile aus setPath; deshalb die vorgesehene API nutzen.
        item.setQueryParameters(QueryParameters.simple(parameters));
        if (iconType != null) {
            item.setPrefixComponent(iconType.create());
        }
        item.getElement().getClassList().add("admin-nav-item");
        navigationActions.put(item.getPath(), () -> selectSection(section, group));
        return item;
    }

    private void installSideNavigationActions() {
        // Die Admin-Bereiche sind Panels derselben View. Der öffentliche onNavigate-
        // Hook delegiert ihren Wechsel an Flow und verhindert ein erneutes Laden der View.
        sideNavigation.getElement().addEventListener("cosmos-admin-navigate", event -> {
            String path = event.getEventData().path("event.detail.path").asString();
            Runnable action = navigationActions.get(path);
            if (action != null) {
                action.run();
            }
        }).addEventData("event.detail.path");
        sideNavigation.addAttachListener(event -> sideNavigation.getElement().executeJs("""
                this.onNavigate = ({path}) => {
                    this.dispatchEvent(new CustomEvent('cosmos-admin-navigate', {detail: {path}}));
                };
                """));
    }

    private void selectSection(AdminSection section, AdminConfigGroup configurationGroup) {
        activeSection = section;

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

        if (sectionLayout == null) {
            return;
        }

        sectionLayout.removeAll();

        switch (section) {
            case CONFIGURATION -> {
                sectionLayout.add(configurationSection);
                configurationSection.showGroup(activeConfigurationGroup);
            }
            case MAIL_TEST -> sectionLayout.add(mailTestSection);
            case USER_MANAGEMENT -> sectionLayout.add(userManagementSection);
            default -> sectionLayout.add(dashboardSection);
        }

        updateNavigationState();
    }

    private void updateNavigationState() {
        applyNavigationItemState(dashboardNavItem, activeSection == AdminSection.DASHBOARD);
        applyNavigationItemState(configurationNavItem, activeSection == AdminSection.CONFIGURATION);
        applyNavigationItemState(mailTestNavItem, activeSection == AdminSection.MAIL_TEST);
        applyNavigationItemState(userManagementNavItem, activeSection == AdminSection.USER_MANAGEMENT);

        // Der Elternpunkt klappt nur auf; die aktive Seite ist der Unterpunkt.
        if (configurationNavItem != null) {
            configurationNavItem.getElement().removeAttribute("aria-current");
        }

        configurationSubmenuItems.forEach((group, item) -> {
            boolean isActive = activeSection == AdminSection.CONFIGURATION
                    && Objects.equals(activeConfigurationGroup, group);
            applyNavigationItemState(item, isActive);
        });
    }

    private void applyNavigationItemState(SideNavItem item, boolean active) {
        if (item == null) {
            return;
        }

        if (active) {
            item.getElement().getClassList().add("cosmos-nav-item--active");
            item.getElement().setAttribute("aria-current", "page");
        } else {
            item.getElement().getClassList().remove("cosmos-nav-item--active");
            item.getElement().removeAttribute("aria-current");
        }
    }

    private void refreshTexts() {
        if (headerTitle != null) {
            headerTitle.setText(messageProperties.getAdminHeaderTitle());
        }

        dashboardNavItem.setLabel(messageProperties.getAdminNavDashboard());
        configurationNavItem.setLabel(messageProperties.getAdminNavConfiguration());
        mailTestNavItem.setLabel(messageProperties.getAdminNavMailTest());
        userManagementNavItem.setLabel(messageProperties.getAdminUsersTitle());

        configurationSubmenuItems.forEach((group, item) ->
                item.setLabel(messageProperties.getAdminConfigGroupLabel(group)));
        sideNavigation.getElement().setAttribute("aria-label", messageProperties.getAdminHeaderTitle());
        sideNavigation.setI18n(new SideNav.SideNavI18n().setToggle(messageProperties.getAdminNavToggleChildren()));

        if (dashboardSection != null) {
            dashboardSection.refreshTexts();
        }
        if (configurationSection != null) {
            configurationSection.refreshTexts();
        }
        if (mailTestSection != null) {
            mailTestSection.refreshTexts();
        }
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