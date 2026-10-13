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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@StyleSheet("custom-theme/styles.css")
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

    private final Span dashboardNavText = new Span();
    private final Span configurationNavText = new Span();
    private final Span mailTestNavText = new Span();
    private final Span userManagementNavText = new Span();
    private final Span serviceNameSpan = new Span();

    private Div dashboardNavItem = null;
    private Div configurationNavItem = null;
    private Div mailTestNavItem = null;
    private Div userManagementNavItem = null;
    private VerticalLayout configurationSubmenuLayout = null;
    private final Map<AdminConfigGroup, Div> configurationSubmenuItems = new LinkedHashMap<>();
    private final Map<AdminConfigGroup, Span> configurationSubmenuTextSpans = new LinkedHashMap<>();

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
        dashboardNavItem = createNavigationItem(
                VaadinIcon.DASHBOARD,
                dashboardNavText,
                false,
                () -> selectSection(AdminSection.DASHBOARD, null)
        );
        configurationNavItem = createNavigationItem(
                VaadinIcon.COG,
                configurationNavText,
                false,
                () -> selectSection(AdminSection.CONFIGURATION, AdminConfigGroup.GENERAL)
        );
        mailTestNavItem = createNavigationItem(
                VaadinIcon.ENVELOPE,
                mailTestNavText,
                false,
                () -> selectSection(AdminSection.MAIL_TEST, null)
        );
        userManagementNavItem = createNavigationItem(
                VaadinIcon.USERS,
                userManagementNavText,
                false,
                () -> selectSection(AdminSection.USER_MANAGEMENT, null)
        );

        configurationSubmenuLayout = new VerticalLayout();
        configurationSubmenuLayout.setPadding(false);
        configurationSubmenuLayout.setSpacing(false);
        configurationSubmenuLayout.setWidthFull();
        configurationSubmenuLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        configurationSubmenuLayout.addClassName("cosmos-nav-submenu");

        addConfigurationSubmenuButton(AdminConfigGroup.GENERAL);
        addConfigurationSubmenuButton(AdminConfigGroup.SECURITY);
        addConfigurationSubmenuButton(AdminConfigGroup.TOKENS);
        addConfigurationSubmenuButton(AdminConfigGroup.USER_STATUS);
        addConfigurationSubmenuButton(AdminConfigGroup.EMAIL_SENDER);
        addConfigurationSubmenuButton(AdminConfigGroup.SMTP);
        addConfigurationSubmenuButton(AdminConfigGroup.QUEUE);

        VerticalLayout drawerContent = getDrawerLayout();
        updateNavigationState();

        return drawerContent;
    }

    private @NonNull VerticalLayout getDrawerLayout() {
        VerticalLayout drawerContent = new VerticalLayout(getNavLayout());
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);
        drawerContent.setWidthFull();
        drawerContent.setAlignItems(FlexComponent.Alignment.STRETCH);
        drawerContent.addClassName("cosmos-sidebar");
        return drawerContent;
    }

    private @NonNull VerticalLayout getNavLayout() {
        VerticalLayout navLayout = new VerticalLayout(
                dashboardNavItem,
                configurationNavItem,
                configurationSubmenuLayout,
                mailTestNavItem,
                userManagementNavItem
        );
        navLayout.setPadding(false);
        navLayout.setSpacing(false);
        navLayout.setWidthFull();
        navLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        navLayout.addClassName("cosmos-nav");
        return navLayout;
    }

    private void addConfigurationSubmenuButton(AdminConfigGroup group) {
        Span submenuText = new Span();
        Div submenuButton = createNavigationItem(
                null,
                submenuText,
                true,
                () -> selectSection(AdminSection.CONFIGURATION, group)
        );
        configurationSubmenuItems.put(group, submenuButton);
        configurationSubmenuTextSpans.put(group, submenuText);
        configurationSubmenuLayout.add(submenuButton);
    }

    private Div createNavigationItem(VaadinIcon iconType, Span textSpan, boolean submenu, Runnable clickAction) {
        Div item;
        if (iconType != null) {
            var icon = iconType.create();
            icon.addClassNames(
                    submenu ? LumoUtility.IconSize.SMALL : LumoUtility.IconSize.MEDIUM,
                    LumoUtility.Flex.SHRINK_NONE
            );
            item = new Div(icon, textSpan);
        } else {
            item = new Div(textSpan);
        }

        textSpan.addClassName("cosmos-nav-label");
        item.addClassNames("admin-nav-item", "cosmos-nav-item");
        if (submenu) item.addClassName("cosmos-nav-item--sub");

        item.addClickListener(event -> clickAction.run());

        return item;
    }

    private void selectSection(AdminSection section, AdminConfigGroup configurationGroup) {
        activeSection = section;

        if (section == AdminSection.CONFIGURATION) {
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

        if (configurationSubmenuLayout != null) {
            configurationSubmenuLayout.setVisible(true);
        }

        configurationSubmenuItems.forEach((group, item) -> {
            boolean isActive = activeSection == AdminSection.CONFIGURATION
                    && Objects.equals(activeConfigurationGroup, group);
            applyNavigationItemState(item, isActive);
        });
    }

    private void applyNavigationItemState(Div item, boolean active) {
        if (item == null) {
            return;
        }

        if (active) {
            item.addClassName("cosmos-nav-item--active");
            item.getElement().setAttribute("aria-current", "page");
        } else {
            item.removeClassName("cosmos-nav-item--active");
            item.getElement().removeAttribute("aria-current");
        }
    }

    private void refreshTexts() {
        if (headerTitle != null) {
            headerTitle.setText(messageProperties.getAdminHeaderTitle());
        }

        dashboardNavText.setText(messageProperties.getAdminNavDashboard());
        configurationNavText.setText(messageProperties.getAdminNavConfiguration());
        mailTestNavText.setText(messageProperties.getAdminNavMailTest());
        userManagementNavText.setText(messageProperties.getAdminUsersTitle());

        configurationSubmenuTextSpans.forEach((group, textSpan) ->
                textSpan.setText(messageProperties.getAdminConfigGroupLabel(group)));

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