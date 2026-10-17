package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class AccountViewTest {
  private UI ui;
  private SecurityService security;
  private HttpServletRequest request;
  private AccountView view;
  private AccountProfileService profiles;
  private UserService users;
  private UserEntity user;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = spy(new UI());
    UI.setCurrent(ui);
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
    request = mock(HttpServletRequest.class);
    security = mock(SecurityService.class);
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(11);
    profiles = mock(AccountProfileService.class);
    users = mock(UserService.class);
    user =
        UserEntity.builder()
            .id(12L)
            .version(3L)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.GERMAN)
            .firstName("Own")
            .lastName("Account")
            .email("own@example.test")
            .enabled(true)
            .build();
    when(security.getCurrentUser(request)).thenReturn(Optional.of(user));
    when(security.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(
                User.withUsername("own@example.test").password("unused").roles("USER").build()));
    var avatars = mock(UserAvatarService.class);
    when(avatars.events()).thenReturn(new UserProfileBroadcaster());
    view =
        new AccountView(
            new MessageProperties(new CustomI18NProvider()),
            security,
            request,
            config,
            users,
            profiles,
            mock(AccountSecurityService.class),
            sessionService(),
            new UserStatusBroadcaster(),
            avatars);
  }

  @SuppressWarnings("unchecked")
  private Select<UserStatus> avatarStatus() {
    return (Select<UserStatus>)
        tree(settings())
            .filter(component -> component.getId().orElse("").equals("account-avatar-status"))
            .findFirst()
            .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void avatarStatusUsesTheExistingSaveActionAndTracksOtherStatusChanges(String language) {
    LanguageChangeEvent.fire(ui, Locale.forLanguageTag(language));
    var select = avatarStatus();
    assertEquals(UserStatus.getDefaultStatus(), select.getValue());
    assertTrue(select.isEnabled());
    assertTrue(select.getElement().getThemeList().contains("cosmos-status"));
    assertEquals(List.of(UserStatus.values()), select.getListDataView().getItems().toList());
    assertEquals("Status", select.getAriaLabel().orElseThrow());
    when(users.updateUserStatus(user, UserStatus.ABSENT, true))
        .thenReturn(new UserService.StatusUpdate(UserStatus.ABSENT, true));
    ReflectionTestUtils.invokeMethod(select, "setModelValue", UserStatus.ABSENT, true);
    verify(users).updateUserStatus(user, UserStatus.ABSENT, true);
    var popupAvatar =
        (OverlayUserIcon)
            ReflectionTestUtils.getField(
                ReflectionTestUtils.getField(view, "userPopoverMenu"), "overlayIcon");
    assertEquals(UserStatus.ABSENT, ReflectionTestUtils.getField(popupAvatar, "userStatus"));
    assertTrue(((Div) ReflectionTestUtils.getField(popupAvatar, "status")).isVisible());
    assertEquals(UserStatus.ABSENT, user.getStatus());
    assertTrue(user.isStatusManuallySet());
    assertEquals(UserStatus.ABSENT, select.getValue());
    var summary = ReflectionTestUtils.getField(settings(), "summaryAvatar");
    var tooltip = (Tooltip) ReflectionTestUtils.getField(summary, "tooltip");
    assertTrue(tooltip.isManual());
    assertFalse(tooltip.isOpened());
    assertEquals("", tooltip.getText());
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertEquals(
        UserStatus.ABSENT,
        ((Select<?>) ReflectionTestUtils.getField(menu, "userStatusSelect")).getValue());
    var header = (Button) ReflectionTestUtils.getField(view, "userIconButton");
    assertEquals(UserStatus.ABSENT, ReflectionTestUtils.getField(header.getIcon(), "userStatus"));
    menu.applyExternalStatus(UserStatus.EMPLOYED);
    assertEquals(UserStatus.EMPLOYED, select.getValue());
    LanguageChangeEvent.fire(ui, Locale.forLanguageTag(language.equals("de") ? "en" : "de"));
    assertEquals(UserStatus.EMPLOYED, select.getValue());
    assertEquals("", tooltip.getText());
    assertEquals(
        new MessageProperties(new CustomI18NProvider()).getUserStatusEmployed(),
        select.getItemLabelGenerator().apply(UserStatus.EMPLOYED));
    verifyNoMoreInteractions(users);
    verifyNoInteractions(profiles);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void failedAvatarStatusChangeRestoresBothSelectorsAndShowsTheExistingError(boolean conflict) {
    var select = avatarStatus();
    when(users.updateUserStatus(user, UserStatus.ABSENT, true))
        .thenThrow(
            conflict
                ? new OptimisticLockingFailureException("conflict")
                : new IllegalStateException("failed"));
    ReflectionTestUtils.invokeMethod(select, "setModelValue", UserStatus.ABSENT, true);
    assertEquals(UserStatus.getDefaultStatus(), select.getValue());
    assertEquals(UserStatus.getDefaultStatus(), user.getStatus());
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertEquals(
        UserStatus.getDefaultStatus(),
        ((Select<?>) ReflectionTestUtils.getField(menu, "userStatusSelect")).getValue());
    var notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    assertTrue(notification.isOpened());
    var texts = new MessageProperties(new CustomI18NProvider());
    var message =
        (Html) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText");
    assertEquals(
        conflict ? texts.getBaseUserUpdateConflict() : texts.getBaseUserStatusFailed(),
        message.getElement().getProperty("innerHTML"));
    verify(users).updateUserStatus(user, UserStatus.ABSENT, true);
    verifyNoMoreInteractions(users);
  }

  private SessionManagementService sessionService() {
    var sessions = mock(SessionManagementService.class);
    when(sessions.list(any()))
        .thenReturn(new SessionManagementService.Snapshot(java.util.List.of(), 0, 11));
    return sessions;
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private AccountSettingsComponent settings() {
    return (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
  }

  private void enter(String query) {
    var event = mock(BeforeEnterEvent.class);
    when(event.getLocation()).thenReturn(new Location(AppRouteConstants.ACCOUNT_ROUTE + query));
    view.beforeEnter(event);
  }

  @ParameterizedTest
  @CsvSource({
    "'', PROFILE", "?tab=profile, PROFILE", "?tab=security, SECURITY",
    "?tab=sessions, SESSIONS", "?tab=SECURITY, SECURITY", "?tab=unknown, PROFILE",
    "?tab=, PROFILE", "?tab=sessions&tab=security, PROFILE", "?tab=%3Cscript%3E, PROFILE"
  })
  void accountUrlsRestoreTheTabAndExactlyOneVisiblePanel(String query, AccountSection expected) {
    enter(query);
    assertEquals(expected, settings().getSelectedSection());
    for (var section : AccountSection.values()) {
      var id = "account-panel-" + section.name().toLowerCase(Locale.ROOT);
      var panel =
          tree(settings()).filter(c -> c.getId().orElse("").equals(id)).findFirst().orElseThrow();
      assertEquals(section == expected, panel.isVisible());
    }
    verify(ui, never()).navigate(eq(AccountView.class), any(QueryParameters.class));
  }

  @Test
  void tabChangesNavigateWithFlowAndHistoryRestorationDoesNotNavigateAgainOrLoseDrafts() {
    doReturn(Optional.of(view))
        .when(ui)
        .navigate(eq(AccountView.class), any(QueryParameters.class));
    var first =
        (TextField)
            tree(settings())
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow();
    first.setValue("Unsaved draft");
    var tabs =
        (Tabs)
            tree(settings())
                .filter(c -> c.getId().orElse("").equals("account-tabs"))
                .findFirst()
                .orElseThrow();
    tabs.setSelectedIndex(2);
    verify(ui).navigate(AccountView.class, QueryParameters.simple(Map.of("tab", "sessions")));
    enter("?tab=security");
    assertEquals(AccountSection.SECURITY, settings().getSelectedSection());
    enter("?tab=sessions");
    assertEquals(AccountSection.SESSIONS, settings().getSelectedSection());
    assertEquals("Unsaved draft", first.getValue());
    verify(ui, times(1)).navigate(eq(AccountView.class), any(QueryParameters.class));
    verifyNoInteractions(profiles);
  }

  @Test
  void invalidOrMissingTabResetsEarlierSelectionAndLanguageChangesKeepTheTab() {
    enter("?tab=security");
    LanguageChangeEvent.fire(ui, Locale.ENGLISH);
    assertEquals(AccountSection.SECURITY, settings().getSelectedSection());
    enter("?tab=unknown");
    assertEquals(AccountSection.PROFILE, settings().getSelectedSection());
    enter("?tab=sessions");
    enter("");
    assertEquals(AccountSection.PROFILE, settings().getSelectedSection());
    verify(ui, never()).navigate(eq(AccountView.class), any(QueryParameters.class));
  }

  @Test
  void sessionsLimitComesFromConfigurationRatherThanTheThreeSampleDevices() {
    var limit =
        tree(view)
            .filter(c -> c.getId().orElse("").equals("account-sessions-limit"))
            .findFirst()
            .orElseThrow();
    var texts = MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN);
    assertEquals(texts.getAccountSettingsSessionsLimit(11), limit.getElement().getText());
  }

  @Test
  void viewForwardsAnUnauthenticatedVisitorToLogin() {
    when(security.getAuthenticatedUser(request)).thenReturn(Optional.empty());
    var event = mock(BeforeEnterEvent.class);
    when(event.hasForwardTarget()).thenReturn(true);
    view.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
    verify(event, never()).getLocation();
  }

  @Test
  void existingAccountMenuOpensTheNewAccountRouteAndClosesThePopover() {
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    var button = (Button) ReflectionTestUtils.getField(menu, "manageAccountButton");
    assertTrue(button.isEnabled());
    doReturn(Optional.empty()).when(ui).navigate(AccountView.class);
    menu.getPopover().setOpened(true);
    button.click();
    assertFalse(menu.getPopover().isOpened());
    verify(ui).navigate(AccountView.class);
    verify(security).getCurrentUser(request);
    verifyNoMoreInteractions(security);
  }

  @Test
  void trackedLanguageEventRefreshesTheAccountSurface() {
    LanguageChangeEvent.fire(ui, Locale.ENGLISH);
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    var texts = MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH);
    assertTrue(content.getElement().getTextRecursively().contains(texts.getAccountSettingsTitle()));
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  @Test
  void saveUsesAuthenticatedRequestRefreshesNameAndAppliesLanguageOnlyAfterSuccess() {
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    var date = LocalDate.of(1990, 1, 1);
    when(profiles.save(eq(request), any()))
        .thenReturn(new Profile(4L, "Changed", "Name", date, Locale.ENGLISH));
    ((TextField)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow())
        .setValue("Changed");
    ((ComboBox<Locale>)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-language"))
                .findFirst()
                .orElseThrow())
        .setValue(Locale.ENGLISH);
    ((Button)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-save"))
                .findFirst()
                .orElseThrow())
        .click();
    verify(profiles).save(request, new Changes(3L, "Changed", "Account", date, Locale.ENGLISH));
    assertEquals(Locale.ENGLISH, locale.get());
    assertEquals("Changed", user.getFirstName());
    var avatars =
        Stream.concat(
                tree(view),
                tree(
                    ((UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu"))
                        .getPopover()))
            .filter(component -> component instanceof OverlayUserIcon)
            .distinct()
            .toList();
    assertEquals(3, avatars.size());
    for (var avatar : avatars) {
      String expected =
          "Changed Name – "
              + MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                  .getUserStatusLabel(UserStatus.getDefaultStatus());
      assertEquals(expected, avatar.getElement().getAttribute("aria-label"));
    }

    assertEquals(4L, user.getVersion());
    var menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertTrue(menu.getPopover().getElement().getTextRecursively().contains("Changed Name"));
    assertTrue(
        content
            .getElement()
            .getTextRecursively()
            .contains(
                MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                    .getAccountSettingsTitle()));
  }

  @Test
  void failedSaveKeepsDraftAndDoesNotChangeLanguageOrTheUserSnapshot() {
    var content = (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettings");
    when(profiles.save(eq(request), any())).thenThrow(new IllegalStateException("Unavailable"));
    ((TextField)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow())
        .setValue("Draft");
    ((ComboBox<Locale>)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-language"))
                .findFirst()
                .orElseThrow())
        .setValue(Locale.ENGLISH);
    ((Button)
            tree(content)
                .filter(c -> c.getId().orElse("").equals("account-save"))
                .findFirst()
                .orElseThrow())
        .click();
    assertEquals(Locale.GERMAN, locale.get());
    assertEquals("Own", user.getFirstName());
    assertEquals(Locale.GERMAN, user.getPreferredLocale());
    assertEquals(
        "Draft",
        ((TextField)
                tree(content)
                    .filter(c -> c.getId().orElse("").equals("account-first-name"))
                    .findFirst()
                    .orElseThrow())
            .getValue());
  }
}
