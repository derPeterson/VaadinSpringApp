package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.*;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.ui.components.AccountHeaderActions;
import de.derpeterson.app.ui.components.AccountNavigationTabs;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/** Gemeinsame Kopfzeile und echte Menüaktionen ohne Browser oder Anwendungsdaten. */
class ApplicationHeaderTest {
  private UI ui;

  static Stream<Arguments> pagesRolesAndLanguages() {
    var authenticated =
        Stream.of("home", "account", "notifications")
            .flatMap(
                page ->
                    Stream.of(RoleType.ROLE_USER, RoleType.ROLE_ADMIN, RoleType.ROLE_SUPERADMIN)
                        .flatMap(
                            role ->
                                Stream.of(Locale.GERMAN, Locale.ENGLISH)
                                    .map(locale -> Arguments.of(page, role, locale))));
    return Stream.concat(
        authenticated,
        Stream.of(Locale.GERMAN, Locale.ENGLISH).map(locale -> Arguments.of("home", null, locale)));
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @MethodSource("pagesRolesAndLanguages")
  void sharedHeaderKeepsLogoShortcutsAvatarAndAuthorizedNavigation(
      String page, RoleType role, Locale initialLocale) {
    ui = spy(new UI());
    UI.setCurrent(ui);
    var locale = new AtomicReference<>(initialLocale);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    doAnswer(
            call -> {
              locale.set(call.getArgument(0));
              return null;
            })
        .when(session)
        .setLocale(any());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    var request = new MockHttpServletRequest();
    request.setSession(new MockHttpSession());
    var security = mock(SecurityService.class);
    UserEntity user =
        role == null
            ? null
            : UserEntity.builder()
                .id(17L)
                .firstName("Header")
                .lastName("User")
                .email("header@example.test")
                .enabled(true)
                .roleEntities(List.of(RoleEntity.builder().name(role).build()))
                .build();
    when(security.getCurrentUser(request)).thenReturn(Optional.ofNullable(user));
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Header Test Service");
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(5);
    var texts = new MessageProperties(new CustomI18NProvider());
    var users = mock(UserService.class);
    var avatars = mock(UserAvatarService.class);
    var status = new UserStatusBroadcaster();
    var notifications = mock(UserNotificationService.class);
    when(notifications.inbox(
            eq(request.getSession()),
            any(UserNotificationService.ReadFilter.class),
            anyInt(),
            anyInt()))
        .thenReturn(Page.empty());
    when(notifications.retentionDays()).thenReturn(OptionalInt.of(180));
    TrackedUserAppLayout view =
        switch (page) {
          case "home" -> new HomeView(texts, security, request, config, users, status, avatars);
          case "account" ->
              new AccountView(
                  texts,
                  security,
                  request,
                  config,
                  users,
                  mock(AccountProfileService.class),
                  mock(AccountSecurityService.class),
                  mock(SessionManagementService.class),
                  status,
                  avatars);
          case "notifications" ->
              new UserNotificationsView(
                  texts, security, users, status, request, avatars, notifications, config);
          default -> throw new IllegalArgumentException(page);
        };
    var headers = tree(view).filter(c -> c.hasClassName("cosmos-topbar")).toList();
    assertEquals(1, headers.size());
    var header = (HorizontalLayout) headers.getFirst();
    assertFalse(header.isSpacing());
    var parts = header.getChildren().toList();
    assertEquals(2, parts.size());
    var brand = parts.getFirst();
    assertTrue(brand.hasClassName("cosmos-topbar-brand"));
    var brandParts = brand.getChildren().toList();
    assertEquals(2, brandParts.size());
    var logo = assertInstanceOf(Image.class, brandParts.getFirst());
    assertEquals("custom-theme/service_logo.png", logo.getSrc());
    assertTrue(logo.hasClassName("cosmos-topbar-logo"));
    assertFalse(logo.getAlt().orElseThrow().isBlank());
    assertNotNull(getClass().getResource("/META-INF/resources/" + logo.getSrc()));
    assertEquals("Header Test Service", brandParts.getLast().getElement().getText());
    assertTrue(brandParts.getLast().hasClassName("cosmos-topbar-service-name"));
    var right = parts.getLast();
    assertTrue(right.hasClassName("cosmos-topbar-account-menu"));
    var rightParts = right.getChildren().toList();
    assertEquals(2, rightParts.size());
    var shortcuts = assertInstanceOf(AccountHeaderActions.class, rightParts.getFirst());
    assertEquals(user != null, shortcuts.isVisible());
    var avatar = assertInstanceOf(Button.class, rightParts.getLast());
    assertTrue(avatar.hasClassName("tracked-user-button"));
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertNotNull(menu);
    assertTrue(menu.getPopover().getThemeNames().contains("cosmos"));
    avatar.click();
    assertTrue(menu.getPopover().isOpened());
    var settings =
        tree(shortcuts)
            .filter(c -> c.getId().orElse("").equals("account-header-settings"))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow();
    LanguageChangeEvent.fire(
        ui, initialLocale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    assertEquals(texts.getBaseManageAccountButton(), settings.getAriaLabel().orElseThrow());
    var admin = menuButton(menu, "manageApplicationButton");
    assertEquals(role != null && role.isAdministrative(), admin != null);
    if (admin != null) {
      assertEquals(texts.getBaseManageApplicationButton(), admin.getText());
      doReturn(Optional.empty()).when(ui).navigate(AdminView.class);
      admin.click();
      verify(ui).navigate(AdminView.class);
    }
    var home = menuButton(menu, "homeButton");
    assertEquals(!page.equals("home"), home != null);
    if (home != null) {
      doReturn(Optional.empty()).when(ui).navigate(HomeView.class);
      home.click();
      verify(ui).navigate(HomeView.class);
    }
    if (user != null) {
      doReturn(Optional.empty()).when(ui).navigate(AccountView.class);
      settings.click();
      verify(ui).navigate(AccountView.class);
      assertNotNull(menuButton(menu, "logoutButton"));
      assertNull(menuButton(menu, "registrationButton"));
      assertNull(menuButton(menu, "loginButton"));
    } else {
      doReturn(Optional.empty()).when(ui).navigate(RegistrationView.class);
      doReturn(Optional.empty()).when(ui).navigate(LoginView.class);
      menuButton(menu, "registrationButton").click();
      menuButton(menu, "loginButton").click();
      verify(ui).navigate(RegistrationView.class);
      verify(ui).navigate(LoginView.class);
      assertNull(menuButton(menu, "logoutButton"));
    }
    var accountNavigation = tree(view).filter(AccountNavigationTabs.class::isInstance).toList();
    if (page.equals("home")) {
      assertTrue(accountNavigation.isEmpty());
    } else {
      assertEquals(1, accountNavigation.size());
      var navigation = (AccountNavigationTabs) accountNavigation.getFirst();
      assertTrue(navigation.hasClassName("account-navigation-tabs"));
      assertEquals(
          texts.getAccountSettingsTitle(), navigation.getElement().getAttribute("aria-label"));
      var tabs = navigation.getChildren().map(Tab.class::cast).toList();
      assertEquals(2, tabs.size());
      assertEquals(texts.getAccountNavigationSettings(), tabLabel(tabs.getFirst()));
      assertEquals(texts.getNotificationsTitle(), tabLabel(tabs.getLast()));
      assertSame(
          page.equals("account") ? tabs.getFirst() : tabs.getLast(), navigation.getSelectedTab());
      var parentChildren = navigation.getParent().orElseThrow().getChildren().toList();
      assertEquals(1, parentChildren.indexOf(navigation));
      assertTrue(parentChildren.get(0).hasClassName("cosmos-dashboard-introduction"));
      assertTrue(
          parentChildren
              .get(2)
              .hasClassName(
                  page.equals("account") ? "account-summary" : "cosmos-notification-inbox-card"));
      var routeRequests = new ArrayList<Class<? extends Component>>();
      var tabUi =
          new UI() {
            @Override
            public <T extends Component> Optional<T> navigate(Class<T> target) {
              routeRequests.add(target);
              return Optional.empty();
            }
          };
      tabUi.getInternals().setSession(session);
      tabUi.add(navigation);
      navigation.setSelectedTab(page.equals("account") ? tabs.getLast() : tabs.getFirst());
      assertEquals(
          List.of(page.equals("account") ? UserNotificationsView.class : AccountView.class),
          routeRequests);
      tabUi.remove(navigation);
      routeRequests.clear();
      tabUi.add(navigation);
      assertSame(
          page.equals("account") ? tabs.getFirst() : tabs.getLast(), navigation.getSelectedTab());
      assertTrue(routeRequests.isEmpty());
      tabUi.remove(navigation);
    }
  }

  private String tabLabel(Tab tab) {
    return tab.getChildren()
        .filter(Span.class::isInstance)
        .map(Span.class::cast)
        .findFirst()
        .orElseThrow()
        .getText();
  }

  private Button menuButton(UserPopoverMenu menu, String name) {
    return (Button) ReflectionTestUtils.getField(menu, name);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }
}
