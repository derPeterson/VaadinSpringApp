package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class AdminViewNavigationTest {
  private UI ui;
  private SecurityService security;
  private HttpServletRequest request;
  private AdminView view;

  @BeforeEach
  void setup() {
    ui = spy(new UI());
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
    registry.setRoute(AppRouteConstants.ADMIN_ROUTE, AdminView.class, List.of());
    var service = mock(TestService.class);
    when(service.getDeploymentConfiguration()).thenReturn(mock(DeploymentConfiguration.class));
    when(service.getRouter()).thenReturn(new Router(registry));
    when(service.getRouteRegistry()).thenReturn(registry);
    when(session.getService()).thenReturn(service);
    VaadinService.setCurrent(service);
    security = mock(SecurityService.class);
    request = mock(HttpServletRequest.class);
    when(security.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(
                User.withUsername("admin@example.test").password("unused").roles("ADMIN").build()));
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    view =
        new AdminView(
            new MessageProperties(new CustomI18NProvider()),
            security,
            config,
            new MaintenanceService(config),
            mock(EmailService.class),
            mock(UserService.class),
            mock(RoleService.class),
            request,
            new UserStatusBroadcaster());
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    VaadinService.setCurrent(null);
  }

  private BeforeEnterEvent enter(String query) {
    var event = mock(BeforeEnterEvent.class);
    when(event.getLocation()).thenReturn(new Location(AppRouteConstants.ADMIN_ROUTE + query));
    view.beforeEnter(event);
    return event;
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
    var layout = (VerticalLayout) ReflectionTestUtils.getField(view, "sectionLayout");
    assertEquals(1, layout.getComponentCount());
    assertSame(ReflectionTestUtils.getField(view, section), layout.getComponentAt(0));
    var nav = (SideNavItem) ReflectionTestUtils.getField(view, item);
    assertEquals("page", nav.getElement().getAttribute("aria-current"));
  }

  @ParameterizedTest
  @EnumSource(AdminConfigGroup.class)
  void everyConfigurationGroupCanBeOpenedByDirectLink(AdminConfigGroup group) {
    enter("?section=configuration&group=" + group.name().toLowerCase(Locale.ROOT));
    assertEquals(group, ReflectionTestUtils.getField(view, "activeConfigurationGroup"));
    var section = ReflectionTestUtils.getField(view, "configurationSection");
    assertEquals(group, ReflectionTestUtils.getField(section, "selectedGroup"));
    var parent = (SideNavItem) ReflectionTestUtils.getField(view, "configurationNavItem");
    assertTrue(parent.isExpanded());
    var items = (Map<?, ?>) ReflectionTestUtils.getField(view, "configurationSubmenuItems");
    assertEquals(
        "page", ((SideNavItem) items.get(group)).getElement().getAttribute("aria-current"));
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
    doReturn(Optional.of(view)).when(ui).navigate(eq(AdminView.class), any(QueryParameters.class));
    var actions = (Map<String, Runnable>) ReflectionTestUtils.getField(view, "navigationActions");
    var items = (Map<?, ?>) ReflectionTestUtils.getField(view, "configurationSubmenuItems");
    var item = (SideNavItem) items.get(AdminConfigGroup.SECURITY);
    actions.get(item.getPath()).run();
    verify(ui)
        .navigate(
            AdminView.class,
            QueryParameters.simple(Map.of("section", "configuration", "group", "security")));
  }

  @Test
  void restoringTheUrlDoesNotBypassTheExistingAccessCheck() {
    when(security.getAuthenticatedUser(request)).thenReturn(Optional.empty());
    var event = mock(BeforeEnterEvent.class);
    when(event.hasForwardTarget()).thenReturn(true);
    view.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
    verify(event, never()).getLocation();
  }

  public static class TestService extends VaadinServletService {
    @Override
    public RouteRegistry getRouteRegistry() {
      return super.getRouteRegistry();
    }
  }
}
