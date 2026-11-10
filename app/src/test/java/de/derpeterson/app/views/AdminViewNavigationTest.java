package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.SupportTicketEntity.Type;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.PageFrameComponent;
import de.derpeterson.app.ui.components.PageNavigationComponent;
import de.derpeterson.app.ui.components.PortalPageComponent;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class AdminViewNavigationTest {
  private UI testUi;
  private SecurityService securityService;
  private HttpServletRequest mockHttpRequest;
  private AdminView view;
  private ConfigService configService;
  private UserAvatarService userAvatarService;
  private ExternalProviderSettings externalProviderSettings;

  @BeforeEach
  void setup() {
    testUi = spy(new UI());
    UI.setCurrent(testUi);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(session);
    testUi.getInternals().setSession(session);
    var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
    registry.setRoute(AppRouteConstants.ADMIN_ROUTE, AdminView.class, List.of());
    TestService service = mock(TestService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(service.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(service.getRouter()).thenReturn(new Router(registry));
    when(service.getRouteRegistry()).thenReturn(registry);
    when(session.getService()).thenReturn(service);
    VaadinService.setCurrent(service);
    securityService = mock(SecurityService.class);
    mockHttpRequest = mock(HttpServletRequest.class);
    when(securityService.getAuthenticatedUser(mockHttpRequest))
        .thenReturn(
            Optional.of(
                User.withUsername("admin@example.test")
                    .password("unused")
                    .roles("SUPERADMIN", "ADMIN")
                    .build()));
    when(securityService.getCurrentUser(mockHttpRequest))
        .thenReturn(
            Optional.of(
                UserEntity.builder()
                    .id(1L)
                    .firstName("Admin")
                    .lastName("Test")
                    .email("admin@example.test")
                    .enabled(true)
                    .build()));
    configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    userAvatarService = mock(UserAvatarService.class);
    when(userAvatarService.events()).thenReturn(new UserProfileBroadcaster());
    view = createView();
  }

  private AdminView createView() {
    AdminView created =
        new AdminView(
            new MessageProperties(new CustomI18NProvider()),
            securityService,
            configService,
            new MaintenanceService(configService),
            mock(EmailService.class),
            mock(UserService.class),
            mock(RoleService.class),
            mockHttpRequest,
            new UserStatusBroadcaster(),
            userAvatarService,
            mock(ExternalIdentityService.class));
    externalProviderSettings = mock(ExternalProviderSettings.class);
    when(externalProviderSettings.load(any()))
        .thenAnswer(call ->
            new ExternalProviderSettings.Settings(
                call.getArgument(0), "", false, false, "own-version"));
    de.derpeterson.app.security.mfa.TwoFactorAuthorization factor =
        mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class);
    created.configureExternalProviders(externalProviderSettings, factor);
    de.derpeterson.app.security.mfa.TwoFactorPolicies policies =
        mock(de.derpeterson.app.security.mfa.TwoFactorPolicies.class);
    when(policies.load())
        .thenReturn(
            new de.derpeterson.app.security.mfa.TwoFactorPolicies.Snapshot(
                -1, de.derpeterson.app.security.mfa.TwoFactorPolicy.defaults()));
    created.configureTwoFactor(policies, factor);
    return created;
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    VaadinService.setCurrent(null);
  }

  private BeforeEnterEvent enter(String query) {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getLocation()).thenReturn(new Location(AppRouteConstants.ADMIN_ROUTE + query));
    view.beforeEnter(event);
    return event;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "?section=support&ticket=10001",
        "?ticket=10001&section=support",
        "?section=support&tab=recovery&ticket=10001",
        "?ticket=10001&tab=recovery&section=support"
      })
  void ticketLinkSelectsOnlyItsMatchingVisibleTicketRegardlessOfParameterOrder(String query) {
    SupportTicketService mockSupportTicketService = mock(SupportTicketService.class);
    Ticket ticket =
        new Ticket(
            1L,
            0L,
            "Request",
            Type.RECOVERY,
            Status.OPEN,
            Instant.EPOCH,
            Instant.EPOCH,
            null,
            null,
            false,
            0L,
            false,
            false,
            null);
    when(mockSupportTicketService.findForAdministration(eq(mockHttpRequest), any()))
        .thenAnswer(call -> "10001".equals(call.getArgument(1)) ? ticket : null);
    when(mockSupportTicketService.loadTicketInbox(eq(mockHttpRequest), any(), any()))
        .thenAnswer(call -> {
          Long selected = call.getArgument(1);
          return new SupportTicketService.LiveView(
              1L,
              List.of(ticket),
              ticket.id().equals(selected) ? ticket : null,
              List.of(),
              null,
              1,
              0,
              10);
        });
    view.configureSupport(mockSupportTicketService);
    enter(query);
    Component support = (Component) ReflectionTestUtils.getField(view, "supportTicketsComponent");
    Grid<?> grid = (Grid<?>) ReflectionTestUtils.getField(support, "ticketGrid");
    assertEquals(List.of(ticket), List.copyOf(grid.getSelectedItems()));
    Tabs categories = (Tabs) ReflectionTestUtils.getField(support, "ticketCategories");
    assertEquals(query.contains("tab=recovery") ? 1 : 0, categories.getSelectedIndex());

    enter("?section=support&tab=recovery");
    assertTrue(grid.getSelectedItems().isEmpty());
    assertEquals(1, categories.getSelectedIndex());

    enter("?section=support&tab=unknown&ticket=10001");
    assertEquals(List.of(ticket), List.copyOf(grid.getSelectedItems()));
    assertEquals(0, categories.getSelectedIndex());

    enter("?section=support&ticket=99999");
    assertTrue(grid.getSelectedItems().isEmpty());
    assertEquals("SUPPORT", ReflectionTestUtils.getField(view, "activeAdminSection").toString());
  }

  @Test
  void sharedFrameKeepsHeadingAndNavigationInContentAndClosesMobileNavigationOnSelection() {
    assertInstanceOf(PageFrameComponent.class, view.getContent());
    PortalPageComponent page =
        (PortalPageComponent) ReflectionTestUtils.getField(view, "adminPortalPageComponent");
    Component heading = (Component) ReflectionTestUtils.getField(view, "adminHeadingDiv");
    assertSame(page, heading.getParent().orElseThrow());
    assertFalse(heading.getElement().hasAttribute("slot"));
    Button toggle = (Button) ReflectionTestUtils.getField(view, "accountNavigationToggle");
    assertEquals("admin-sidebar", toggle.getElement().getAttribute("aria-controls"));
    toggle.click();
    assertTrue(page.hasClassName("account-navigation-open"));
    assertEquals("true", toggle.getElement().getAttribute("aria-expanded"));
    enter("?section=mail_test");
    assertFalse(page.hasClassName("account-navigation-open"));
    assertEquals("false", toggle.getElement().getAttribute("aria-expanded"));
  }

  @Test
  void notificationsNavigationIsHiddenAndDirectUrlFallsBackForOrdinaryAdmin() {
    UserNotificationService notifications =
        mock(de.derpeterson.app.service.UserNotificationService.class);
    view.configureAdminNotifications(notifications);
    enter("?section=notifications");
    assertFalse(
        ((SideNavItem) ReflectionTestUtils.getField(view, "notificationsNavItem")).isVisible());
    assertEquals("DASHBOARD", ReflectionTestUtils.getField(view, "activeAdminSection").toString());
    verify(notifications, never()).targets(any());
  }

  @Test
  void superadminCanOpenNotificationsAndRefreshUsesTheSameSection() {
    UserNotificationService notifications =
        mock(de.derpeterson.app.service.UserNotificationService.class);
    when(notifications.canSend(any())).thenReturn(true);
    when(notifications.targets(any())).thenReturn(List.of());
    when(notifications.sent(any(), eq(0), eq(5)))
        .thenReturn(org.springframework.data.domain.Page.empty());
    try (AnnotationConfigApplicationContext context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.registerBean(
          de.derpeterson.app.service.UserNotificationService.class, () -> notifications);
      context.registerBean(ExternalProviderSettings.class, () -> externalProviderSettings);
      context.registerBean(
          de.derpeterson.app.security.mfa.TwoFactorAuthorization.class,
          () -> mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class));
      context.registerBean(
          de.derpeterson.app.security.mfa.TwoFactorPolicies.class,
          () -> mock(de.derpeterson.app.security.mfa.TwoFactorPolicies.class));
      context.registerBean(
          de.derpeterson.app.service.SupportTicketService.class,
          () -> mock(de.derpeterson.app.service.SupportTicketService.class));
      context.refresh();
      context.getAutowireCapableBeanFactory().autowireBean(view);
    }
    enter("?section=notifications");
    assertEquals(
        "NOTIFICATIONS", ReflectionTestUtils.getField(view, "activeAdminSection").toString());
    assertTrue(
        ((SideNavItem) ReflectionTestUtils.getField(view, "notificationsNavItem")).isVisible());
    when(notifications.canSend(any())).thenReturn(false);
    enter("?section=notifications");
    assertEquals("DASHBOARD", ReflectionTestUtils.getField(view, "activeAdminSection").toString());
  }

  @ParameterizedTest
  @CsvSource({
    "'', dashboardSection, dashboardNavItem",
    "?section=dashboard, dashboardSection, dashboardNavItem",
    "?section=user_management, userManagementSection, userManagementNavItem",
    "?section=mail_test, mailTestSection, mailTestNavItem",
    "?section=unknown, dashboardSection, dashboardNavItem",
    "?section=, dashboardSection, dashboardNavItem",
    "?section=mail_test&section=user_management, dashboardSection, dashboardNavItem"
  })
  void urlsSelectExactlyOnePanelAndItsNavigationItem(String query, String section, String item) {
    enter(query);
    VerticalLayout layout =
        (VerticalLayout) ReflectionTestUtils.getField(view, "adminSectionContentLayout");
    assertEquals(1, layout.getComponentCount());
    assertSame(ReflectionTestUtils.getField(view, section), layout.getComponentAt(0));
    SideNavItem nav = (SideNavItem) ReflectionTestUtils.getField(view, item);
    assertEquals("page", nav.getElement().getAttribute("aria-current"));
  }

  @ParameterizedTest
  @EnumSource(AdminConfigGroup.class)
  void everyConfigurationGroupCanBeOpenedByDirectLink(AdminConfigGroup group) {
    enter("?section=configuration&group=" + group.name().toLowerCase(Locale.ROOT));
    assertEquals(group, ReflectionTestUtils.getField(view, "activeConfigurationGroup"));
    Object section = ReflectionTestUtils.getField(view, "configurationSection");
    if (group == AdminConfigGroup.EXTERNAL_LOGIN) {
      VerticalLayout layout =
          (VerticalLayout) ReflectionTestUtils.getField(view, "adminSectionContentLayout");
      assertSame(
          ReflectionTestUtils.getField(view, "externalLoginSection"), layout.getComponentAt(0));
      verify(externalProviderSettings).load("google");
      verify(externalProviderSettings).load("microsoft");
    } else if (group == AdminConfigGroup.TWO_FACTOR) {
      VerticalLayout layout =
          (VerticalLayout) ReflectionTestUtils.getField(view, "adminSectionContentLayout");
      assertSame(ReflectionTestUtils.getField(view, "twoFactorSection"), layout.getComponentAt(0));
    } else {
      assertEquals(group, ReflectionTestUtils.getField(section, "selectedGroup"));
    }
    SideNavItem parent = (SideNavItem) ReflectionTestUtils.getField(view, "configurationNavItem");
    assertTrue(parent.isExpanded());
    Map<?, ?> items =
        (Map<?, ?>) ReflectionTestUtils.getField(view, "configurationSubmenuNavItems");
    assertEquals(
        "page", ((SideNavItem) items.get(group)).getElement().getAttribute("aria-current"));
  }

  @Test
  void providerConfigurationAndGeneralConfigurationKeepTheirOwnHeadingsWhenSwitchingBack() {
    enter("?section=configuration&group=general");
    Component heading = (Component) ReflectionTestUtils.getField(view, "adminHeadingDiv");
    Component general = heading.getChildren().findFirst().orElseThrow();
    enter("?section=configuration&group=external_login");
    Component external = heading.getChildren().findFirst().orElseThrow();
    assertNotSame(general, external);
    enter("?section=configuration&group=general");
    assertSame(general, heading.getChildren().findFirst().orElseThrow());
    enter("?section=configuration&group=external_login");
    assertSame(external, heading.getChildren().findFirst().orElseThrow());
  }

  @ParameterizedTest
  @CsvSource({"''", "&group=unknown", "&group=", "&group=security&group=smtp"})
  void missingOrInvalidGroupDoesNotReuseThePreviouslyOpenedGroup(String suffix) {
    enter("?section=configuration&group=security");
    enter("?section=configuration" + suffix);
    assertEquals(
        AdminConfigGroup.GENERAL, ReflectionTestUtils.getField(view, "activeConfigurationGroup"));
  }

  @Test
  void menuSelectionUsesFlowNavigationWithTheSameParametersAsTheLink() {
    doReturn(Optional.of(view))
        .when(testUi)
        .navigate(eq(AdminView.class), any(QueryParameters.class));
    PageNavigationComponent navigation =
        (PageNavigationComponent)
            ReflectionTestUtils.getField(view, "adminPageNavigationComponent");
    Map<?, ?> items =
        (Map<?, ?>) ReflectionTestUtils.getField(view, "configurationSubmenuNavItems");
    SideNavItem item = (SideNavItem) items.get(AdminConfigGroup.SECURITY);
    navigation.activate(item);
    verify(testUi)
        .navigate(
            AdminView.class,
            QueryParameters.simple(Map.of("section", "configuration", "group", "security")));
  }

  @Test
  void restoringTheUrlDoesNotBypassTheExistingAccessCheck() {
    when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.empty());
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.hasForwardTarget()).thenReturn(true);
    view.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
    verify(event, never()).getLocation();
  }

  @Test
  void requestWithoutCurrentUserIsForwardedWithoutBuildingAnAnonymousUserMenu() {
    when(securityService.getCurrentUser(mockHttpRequest)).thenReturn(Optional.empty());
    when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.empty());
    AdminView anonymous = createView();
    assertNull(ReflectionTestUtils.getField(anonymous, "userPopoverMenu"));
    assertNull(ReflectionTestUtils.getField(anonymous, "userIconButton"));
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.hasForwardTarget()).thenReturn(true);
    anonymous.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
  }

  public static class TestService extends VaadinServletService {
    @Override
    public RouteRegistry getRouteRegistry() {
      return super.getRouteRegistry();
    }
  }
}
