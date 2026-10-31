package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.ui.components.AccountHeaderActions;
import de.derpeterson.app.ui.components.PageNavigationComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
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
    Stream<Arguments> authenticated =
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
  void sharedHeaderKeepsBrandShortcutsAndAvatar(String page, RoleType role, Locale initialLocale) {
    HeaderFixture fixture = createHeader(page, role, initialLocale);
    TrackedUserAppLayout view = fixture.view();
    UserEntity user = fixture.user();
    if (user == null) {
      assertPublicHeader(view, fixture.texts());
      return;
    }
    List<Component> headers = tree(view).filter(c -> c.hasClassName("cosmos-topbar")).toList();
    assertEquals(1, headers.size());
    HorizontalLayout header = (HorizontalLayout) headers.getFirst();
    assertFalse(header.isSpacing());
    List<Component> parts = header.getChildren().toList();
    assertEquals(2, parts.size());
    Component brand = parts.getFirst();
    assertTrue(brand.hasClassName("cosmos-topbar-brand"));
    List<Component> brandParts = brand.getChildren().toList();
    assertEquals(2, brandParts.size());
    if (user != null) {
      assertEquals("account-navigation-toggle", brandParts.getFirst().getId().orElseThrow());
      Anchor home = assertInstanceOf(Anchor.class, brandParts.getLast());
      assertEquals("/home", home.getHref());
      brandParts = home.getChildren().toList();
    }
    Image logo = assertInstanceOf(Image.class, brandParts.getFirst());
    assertEquals("icons/service-logo.svg", logo.getSrc());
    assertTrue(logo.hasClassName("cosmos-topbar-logo"));
    assertEquals(user != null, logo.getAlt().orElseThrow().isBlank());
    assertNotNull(getClass().getResource("/META-INF/resources/" + logo.getSrc()));
    assertEquals("Header Test Service", brandParts.getLast().getElement().getText());
    assertTrue(brandParts.getLast().hasClassName("cosmos-topbar-service-name"));
    Component right = parts.getLast();
    assertTrue(right.hasClassName("cosmos-topbar-account-menu"));
    List<Component> rightParts = right.getChildren().toList();
    assertEquals(2, rightParts.size());
    AccountHeaderActions shortcuts =
        assertInstanceOf(AccountHeaderActions.class, rightParts.getFirst());
    assertEquals(user != null, shortcuts.isVisible());
    Button avatar = assertInstanceOf(Button.class, rightParts.getLast());
    assertTrue(avatar.hasClassName("tracked-user-button"));
  }

  @ParameterizedTest
  @MethodSource("pagesRolesAndLanguages")
  void headerMenuKeepsAuthorizedNavigationAndLanguageChanges(
      String page, RoleType role, Locale initialLocale) {
    HeaderFixture fixture = createHeader(page, role, initialLocale);
    TrackedUserAppLayout view = fixture.view();
    UserEntity user = fixture.user();
    MessageProperties texts = fixture.texts();
    if (user == null) {
      assertPublicHeader(view, texts);
      doReturn(Optional.empty()).when(ui).navigate(RegistrationView.class);
      doReturn(Optional.empty()).when(ui).navigate(LoginView.class);
      buttonById(view, "public-nav-register").click();
      buttonById(view, "public-nav-login").click();
      verify(ui).navigate(RegistrationView.class);
      verify(ui).navigate(LoginView.class);
      LanguageChangeEvent.fire(
          ui, initialLocale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
      assertPublicHeader(view, texts);
      assertTrue(tree(view).noneMatch(c -> c.hasClassName("tracked-user-button")));
      return;
    }
    HorizontalLayout header =
        (HorizontalLayout)
            tree(view).filter(c -> c.hasClassName("cosmos-topbar")).findFirst().orElseThrow();
    List<Component> rightParts = header.getChildren().toList().getLast().getChildren().toList();
    AccountHeaderActions shortcuts = (AccountHeaderActions) rightParts.getFirst();
    Button avatar = (Button) rightParts.getLast();
    UserPopoverMenu menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertNotNull(menu);
    assertTrue(menu.getPopover().getThemeNames().contains("cosmos"));
    assertTrue(
        tree(menu.getPopover())
            .noneMatch(
                component -> component.getId().orElse("").equals("portal-menu-avatar-edit")));
    avatar.click();
    assertTrue(menu.getPopover().isOpened());
    Button settings =
        tree(shortcuts)
            .filter(c -> c.getId().orElse("").equals("account-header-settings"))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow();
    LanguageChangeEvent.fire(
        ui, initialLocale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    assertEquals(texts.getBaseManageAccountButton(), settings.getAriaLabel().orElseThrow());
    Button admin = menuButton(menu, "manageApplicationButton");
    assertEquals(role != null && role.isAdministrative(), admin != null);
    if (admin != null) {
      assertTrue(tree(menu.getPopover()).anyMatch(component -> component == admin));
      assertTrue(admin.isVisible());
      assertEquals(texts.getBaseManageApplicationButton(), admin.getText());
      assertEquals(
          "icons/lucide/shield-cog.svg",
          admin.getPrefixComponent().getElement().getAttribute("src"));
      doReturn(Optional.empty()).when(ui).navigate(AdminView.class);
      admin.click();
      verify(ui).navigate(AdminView.class);
    }
    Button home = menuButton(menu, "homeButton");
    assertEquals(!page.equals("home"), home != null);
    if (home != null) {
      doReturn(Optional.empty()).when(ui).navigate(HomeView.class);
      home.click();
      verify(ui).navigate(HomeView.class);
    }
    doReturn(Optional.empty()).when(ui).navigate(AccountView.class);
    settings.click();
    verify(ui).navigate(AccountView.class);
    assertNotNull(menuButton(menu, "logoutButton"));
    assertTrue(
        tree(menu.getPopover())
            .noneMatch(
                component -> component instanceof com.vaadin.flow.component.select.Select<?>));
    assertTrue(
        tree(menu.getPopover())
            .noneMatch(component -> component.hasClassName("user-popover-header")));
    assertEquals(
        4,
        tree(menu.getPopover())
            .filter(component -> component.hasClassName("portal-status-option"))
            .count());
  }

  @ParameterizedTest
  @MethodSource("pagesRolesAndLanguages")
  void sideNavigationKeepsSelectedRoutePlacementAndReattachment(
      String page, RoleType role, Locale initialLocale) {
    HeaderFixture fixture = createHeader(page, role, initialLocale);
    TrackedUserAppLayout view = fixture.view();
    MessageProperties texts = fixture.texts();
    List<Component> sidebars =
        tree(view).filter(c -> c.getId().orElse("").equals("account-sidebar")).toList();
    if (fixture.user() == null) {
      assertTrue(sidebars.isEmpty());
      return;
    }
    assertTrue(view.hasClassName("cosmos-reference-portal"));
    assertEquals(1, sidebars.size());
    Component sidebar = sidebars.getFirst();
    PageNavigationComponent navigation =
        tree(sidebar)
            .filter(PageNavigationComponent.class::isInstance)
            .map(PageNavigationComponent.class::cast)
            .filter(component -> component.getId().orElse("").equals("account-navigation"))
            .findFirst()
            .orElseThrow();
    List<SideNavItem> entries = navigation.getItems();
    assertEquals(3, entries.size());
    assertEquals(texts.getAccountSettingsProfileTab(), entries.get(0).getLabel());
    assertEquals(texts.getAccountSettingsSecurityTab(), entries.get(1).getLabel());
    assertEquals(texts.getAccountSettingsSessionsTab(), entries.get(2).getLabel());
    assertTrue(sidebar.getElement().getTextRecursively().contains(texts.getNotificationsTitle()));
    Component columns = sidebar.getParent().orElseThrow();
    assertTrue(columns.hasClassName("account-columns"));
    assertSame(sidebar, columns.getChildren().findFirst().orElseThrow());
    assertTrue(
        columns
            .getParent()
            .orElseThrow()
            .getChildren()
            .findFirst()
            .orElseThrow()
            .hasClassName("cosmos-section-header"));
    SideNavItem home =
        tree(sidebar)
            .filter(c -> c.getId().orElse("").equals("account-nav-home"))
            .map(SideNavItem.class::cast)
            .findFirst()
            .orElseThrow();
    SideNavItem inbox =
        tree(sidebar)
            .filter(c -> c.getId().orElse("").equals("account-nav-notifications"))
            .map(SideNavItem.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(
        page.equals("home"), home.getElement().getClassList().contains("cosmos-nav-item--active"));
    assertEquals(
        page.equals("notifications"),
        inbox.getElement().getClassList().contains("cosmos-nav-item--active"));
    if (page.equals("account")) {
      assertEquals(
          "account-tab-profile",
          entries.stream()
              .filter(item -> item.getElement().getClassList().contains("cosmos-nav-item--active"))
              .findFirst()
              .orElseThrow()
              .getId()
              .orElseThrow());
    } else {
      assertTrue(
          entries.stream()
              .noneMatch(
                  item -> item.getElement().getClassList().contains("cosmos-nav-item--active")));
      doReturn(Optional.empty())
          .when(ui)
          .navigate(eq(AccountView.class), any(com.vaadin.flow.router.QueryParameters.class));
      ui.add(navigation);
      navigation.activate(entries.get(1));
      org.mockito.ArgumentCaptor<com.vaadin.flow.router.QueryParameters> query =
          org.mockito.ArgumentCaptor.forClass(com.vaadin.flow.router.QueryParameters.class);
      verify(ui).navigate(eq(AccountView.class), query.capture());
      assertEquals(java.util.Map.of("tab", List.of("security")), query.getValue().getParameters());
      ui.remove(navigation);
      org.mockito.Mockito.clearInvocations(ui);
      ui.add(navigation);
      assertTrue(
          entries.stream()
              .noneMatch(
                  item -> item.getElement().getClassList().contains("cosmos-nav-item--active")));
      verify(ui, never())
          .navigate(eq(AccountView.class), any(com.vaadin.flow.router.QueryParameters.class));
      ui.remove(navigation);
    }
  }

  private record HeaderFixture(
      TrackedUserAppLayout view, UserEntity user, MessageProperties texts, VaadinSession session) {}

  private void assertPublicHeader(Component view, MessageProperties texts) {
    assertEquals(1, tree(view).filter(c -> c.hasClassName("public-header")).count());
    Anchor brand =
        (Anchor) tree(view).filter(c -> c.hasClassName("public-brand")).findFirst().orElseThrow();
    assertEquals("/home", brand.getHref());
    assertEquals(
        "icons/service-logo.svg", ((Image) brand.getChildren().findFirst().orElseThrow()).getSrc());
    assertEquals(texts.getHomeTitle(), buttonById(view, "public-nav-home").getText());
    assertEquals(texts.getBaseLoginButton(), buttonById(view, "public-nav-login").getText());
    assertEquals(
        texts.getBaseRegistrationButton(), buttonById(view, "public-nav-register").getText());
  }

  private Button buttonById(Component view, String id) {
    return tree(view)
        .filter(c -> c.getId().orElse("").equals(id))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private HeaderFixture createHeader(String page, RoleType role, Locale initialLocale) {
    ui = spy(new UI());
    UI.setCurrent(ui);
    AtomicReference<Locale> locale = new AtomicReference<>(initialLocale);
    VaadinSession session = mock(VaadinSession.class);
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
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(new MockHttpSession());
    SecurityService security = mock(SecurityService.class);
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
    ConfigService config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Header Test Service");
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(5);
    MessageProperties texts = new MessageProperties(new CustomI18NProvider());
    UserService users = mock(UserService.class);
    UserAvatarService avatars = mock(UserAvatarService.class);
    UserStatusBroadcaster status = new UserStatusBroadcaster();
    UserNotificationService notifications = mock(UserNotificationService.class);
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
                  avatars,
                  mock(de.derpeterson.app.security.ExternalLoginFlow.class),
                  mock(de.derpeterson.app.service.ExternalIdentityService.class));
          case "notifications" ->
              new UserNotificationsView(
                  texts, security, users, status, request, avatars, notifications, config);
          default -> throw new IllegalArgumentException(page);
        };
    return new HeaderFixture(view, user, texts, session);
  }

  private Button menuButton(UserPopoverMenu menu, String name) {
    return (Button) ReflectionTestUtils.getField(menu, name);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }
}
