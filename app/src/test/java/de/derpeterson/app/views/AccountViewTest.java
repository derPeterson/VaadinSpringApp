package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.ui.components.ExternalConnectionsComponent;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.PageNavigationComponent;
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
  private UI testUi;
  private SecurityService securityService;
  private HttpServletRequest mockHttpRequest;
  private AccountView view;
  private AccountProfileService accountProfileService;
  private UserService userService;
  private UserEntity testAccount;
  private ConfigService configService;
  private UserAvatarService userAvatarService;
  private ExternalLoginFlow externalLogin;
  private ExternalIdentityService externalIdentityService;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    testUi = spy(new UI());
    UI.setCurrent(testUi);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    VaadinService service = mock(VaadinService.class);
    DeploymentConfiguration deployment = mock(DeploymentConfiguration.class);
    when(service.getDeploymentConfiguration()).thenReturn(deployment);
    when(session.getService()).thenReturn(service);
    when(session.getLocale()).thenAnswer(call -> localeReference.get());
    doAnswer(
            call -> {
              localeReference.set(call.getArgument(0));
              return null;
            })
        .when(session)
        .setLocale(any());
    VaadinSession.setCurrent(session);
    testUi.getInternals().setSession(session);
    mockHttpRequest = mock(HttpServletRequest.class);
    securityService = mock(SecurityService.class);
    configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(11);
    accountProfileService = mock(AccountProfileService.class);
    userService = mock(UserService.class);
    testAccount =
        UserEntity.builder()
            .id(12L)
            .version(3L)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.GERMAN)
            .gender(Gender.OTHER)
            .firstName("Own")
            .lastName("Account")
            .email("own@example.test")
            .enabled(true)
            .build();
    when(securityService.getCurrentUser(mockHttpRequest)).thenReturn(Optional.of(testAccount));
    when(securityService.getAuthenticatedUser(mockHttpRequest))
        .thenReturn(
            Optional.of(
                User.withUsername("own@example.test").password("unused").roles("USER").build()));
    userAvatarService = mock(UserAvatarService.class);
    when(userAvatarService.events()).thenReturn(new UserProfileBroadcaster());
    externalLogin = mock(ExternalLoginFlow.class);
    externalIdentityService = mock(ExternalIdentityService.class);
    view = createView();
  }

  private AccountView createView() {
    return new AccountView(
        new MessageProperties(new CustomI18NProvider()),
        securityService,
        mockHttpRequest,
        configService,
        userService,
        accountProfileService,
        mock(AccountSecurityService.class),
        sessionService(),
        new UserStatusBroadcaster(),
        userAvatarService,
        externalLogin,
        externalIdentityService);
  }

  @Test
  void directAccountRouteKeepsExistingConnectionsWhenAllProvidersAreDisabled() {
    when(externalIdentityService.connected(testAccount.getId())).thenReturn(List.of("google"));
    when(externalLogin.owner(mockHttpRequest)).thenReturn(testAccount);
    view = createView();
    TabSheet tabs =
        tree(settings())
            .filter(TabSheet.class::isInstance)
            .map(TabSheet.class::cast)
            .findFirst()
            .orElseThrow();
    Component providers = tabs.getComponent(tabs.getTabAt(1));
    ExternalConnectionsComponent connections =
        tree(providers)
            .filter(ExternalConnectionsComponent.class::isInstance)
            .map(ExternalConnectionsComponent.class::cast)
            .findFirst()
            .orElseThrow();
    assertTrue(
        tree(connections)
            .anyMatch(component -> component.getId().orElse("").equals("external-connect-google")));
    assertEquals(1, tree(providers).filter(ExternalConnectionsComponent.class::isInstance).count());
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
    LanguageChangeEvent.fire(testUi, Locale.forLanguageTag(language));
    Select<UserStatus> select = avatarStatus();
    assertEquals(UserStatus.getDefaultStatus(), select.getValue());
    assertTrue(select.isEnabled());
    assertTrue(select.getElement().getThemeList().contains("cosmos-status"));
    assertEquals(List.of(UserStatus.values()), select.getListDataView().getItems().toList());
    assertEquals("Status", select.getAriaLabel().orElseThrow());
    when(userService.updateUserStatus(testAccount, UserStatus.ABSENT, true))
        .thenReturn(new UserService.StatusUpdate(UserStatus.ABSENT, true));
    ReflectionTestUtils.invokeMethod(select, "setModelValue", UserStatus.ABSENT, true);
    verify(userService).updateUserStatus(testAccount, UserStatus.ABSENT, true);
    OverlayUserIcon popupAvatar =
        (OverlayUserIcon)
            ReflectionTestUtils.getField(
                ReflectionTestUtils.getField(view, "userPopoverMenu"), "overlayIcon");
    assertEquals(UserStatus.ABSENT, ReflectionTestUtils.getField(popupAvatar, "userStatus"));
    assertTrue(
        ((Div) ReflectionTestUtils.getField(popupAvatar, "statusIndicatorContainer")).isVisible());
    assertEquals(UserStatus.ABSENT, testAccount.getStatus());
    assertTrue(testAccount.isStatusManuallySet());
    assertEquals(UserStatus.ABSENT, select.getValue());
    Object summary =
        ReflectionTestUtils.getField(
            ReflectionTestUtils.getField(settings(), "accountSidebar"), "accountAvatar");
    Tooltip tooltip = (Tooltip) ReflectionTestUtils.getField(summary, "avatarIdentityTooltip");
    assertTrue(tooltip.isManual());
    assertFalse(tooltip.isOpened());
    assertEquals("", tooltip.getText());
    UserPopoverMenu menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertMenuStatus(menu, UserStatus.ABSENT);
    Button header = (Button) ReflectionTestUtils.getField(view, "userIconButton");
    assertEquals(UserStatus.ABSENT, ReflectionTestUtils.getField(header.getIcon(), "userStatus"));
    menu.applyExternalStatus(UserStatus.EMPLOYED);
    assertMenuStatus(menu, UserStatus.EMPLOYED);
    assertEquals(UserStatus.EMPLOYED, select.getValue());
    LanguageChangeEvent.fire(testUi, Locale.forLanguageTag(language.equals("de") ? "en" : "de"));
    assertEquals(UserStatus.EMPLOYED, select.getValue());
    assertEquals("", tooltip.getText());
    assertEquals(
        new MessageProperties(new CustomI18NProvider()).getUserStatusEmployed(),
        select.getItemLabelGenerator().apply(UserStatus.EMPLOYED));
    verifyNoMoreInteractions(userService);
    verifyNoInteractions(accountProfileService);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void failedAvatarStatusChangeRestoresSelectionAndMenuAndShowsTheExistingError(boolean conflict) {
    Select<UserStatus> select = avatarStatus();
    when(userService.updateUserStatus(testAccount, UserStatus.ABSENT, true))
        .thenThrow(
            conflict
                ? new OptimisticLockingFailureException("conflict")
                : new IllegalStateException("failed"));
    ReflectionTestUtils.invokeMethod(select, "setModelValue", UserStatus.ABSENT, true);
    assertEquals(UserStatus.getDefaultStatus(), select.getValue());
    assertEquals(UserStatus.getDefaultStatus(), testAccount.getStatus());
    UserPopoverMenu menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    assertMenuStatus(menu, UserStatus.getDefaultStatus());
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    assertTrue(notification.isOpened());
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    Html message =
        (Html) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText");
    assertEquals(
        conflict
            ? messageProperties.getBaseUserUpdateConflict()
            : messageProperties.getBaseUserStatusFailed(),
        message.getElement().getProperty("innerHTML"));
    verify(userService).updateUserStatus(testAccount, UserStatus.ABSENT, true);
    verifyNoMoreInteractions(userService);
  }

  private void assertMenuStatus(UserPopoverMenu menu, UserStatus expected) {
    List<Component> choices =
        tree(menu.getPopover())
            .filter(component -> component.hasClassName("portal-status-option"))
            .toList();
    assertEquals(UserStatus.values().length, choices.size());
    for (Component choice : choices) {
      assertEquals(
          Boolean.toString(
              expected
                  .name()
                  .toLowerCase(Locale.ROOT)
                  .equals(choice.getElement().getAttribute("data-status"))),
          choice.getElement().getAttribute("aria-pressed"));
    }
  }

  private SessionManagementService sessionService() {
    SessionManagementService sessions = mock(SessionManagementService.class);
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
    return (AccountSettingsComponent)
        ReflectionTestUtils.getField(view, "accountSettingsComponent");
  }

  private void enter(String query) {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getLocation()).thenReturn(new Location(AppRouteConstants.ACCOUNT_ROUTE + query));
    view.beforeEnter(event);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"FRESH_AUTHENTICATION_REQUIRED", "IDENTITY_MISMATCH", "UNAVAILABLE", "unknown"})
  void failedExternalConfirmationStaysInSecurityAndTranslatesItsNotification(String problem) {
    enter("?tab=security&external_error=" + problem);
    assertEquals(AccountSection.SECURITY, settings().getSelectedSection());
    assertFalse(
        ((Notification)
                ReflectionTestUtils.getField(
                    NotificationHelper.getInstance(), "currentNotification"))
            .isOpened());
    testUi.add(view);
    AfterNavigationEvent event = mock(AfterNavigationEvent.class);
    when(event.getLocation())
        .thenReturn(new Location("account?tab=security&external_error=" + problem));
    view.afterNavigation(event);
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    assertTrue(notification.isOpened());
    assertTrue(notification.getElement().getThemeList().contains("cosmos"));
    assertTrue(notification.getElement().getThemeList().contains("cosmos-reference"));
    assertTrue(notification.getElement().getThemeList().contains("cosmos-reference-error"));
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    for (Locale language : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      LanguageChangeEvent.fire(testUi, language);
      Html message =
          (Html) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText");
      String expected =
          switch (problem) {
            case "FRESH_AUTHENTICATION_REQUIRED" ->
                messageProperties.getExternalProofFreshRequired();
            case "IDENTITY_MISMATCH" -> messageProperties.getExternalProofIdentityMismatch();
            default -> messageProperties.getExternalError();
          };
      assertEquals(expected, message.getElement().getProperty("innerHTML"));
    }
    verifyNoInteractions(accountProfileService);
  }

  @ParameterizedTest
  @CsvSource({
    "'', PROFILE",
    "?section=profile, PROFILE",
    "?section=security, SECURITY",
    "?section=sessions, SESSIONS",
    "?section=unknown&tab=security, PROFILE",
    "?section=profile&section=security, PROFILE",
    "?tab=profile, PROFILE",
    "?tab=security, SECURITY",
    "?tab=sessions, SESSIONS",
    "?tab=SECURITY, SECURITY",
    "?tab=unknown, PROFILE",
    "?tab=, PROFILE",
    "?tab=sessions&tab=security, PROFILE",
    "?tab=%3Cscript%3E, PROFILE"
  })
  void accountUrlsRestoreExactlyOneVisibleSection(String query, AccountSection expected) {
    enter(query);
    assertEquals(expected, settings().getSelectedSection());
    for (AccountSection section : AccountSection.values()) {
      String id = "account-panel-" + section.name().toLowerCase(Locale.ROOT);
      Component panel =
          tree(settings()).filter(c -> c.getId().orElse("").equals(id)).findFirst().orElseThrow();
      assertEquals(section == expected, panel.isVisible());
    }
    verify(testUi, never()).navigate(eq(AccountView.class), any(QueryParameters.class));
  }

  @ParameterizedTest
  @CsvSource({
    "?section=security&tab=providers, providers",
    "?tab=providers&section=security, providers",
    "?section=security&tab=portal, portal",
    "?section=security&tab=unknown, portal",
    "?section=security&tab=providers&tab=portal, portal",
    "?section=security, portal",
    "?tab=security, portal"
  })
  void securityUrlsRestoreTheRequestedTab(String query, String expectedTab) {
    enter("?section=security&tab=providers");
    enter(query);
    assertEquals(
        "account-security-tab-" + expectedTab,
        securityTabs().getSelectedTab().getId().orElseThrow());
  }

  @Test
  void aClientTabChangeReplacesTheCurrentAccountLocation() {
    UI attachedUi = new UI();
    attachedUi.getInternals().setSession(testUi.getSession());
    UI.setCurrent(attachedUi);
    attachedUi.add(view);
    enter("?section=security&tab=portal");
    TabSheet tabs = securityTabs();
    tabs.setSelectedIndex(1);
    Tab portalTab = tabs.getTabAt(0);
    Tabs tabBar = (Tabs) portalTab.getParent().orElseThrow();
    ComponentUtil.fireEvent(tabBar, new Tabs.SelectedChangeEvent(tabBar, portalTab, true));
    JavaScriptInvocation update =
        attachedUi.getInternals().dumpPendingJavaScriptInvocations().stream()
            .map(PendingJavaScriptInvocation::getInvocation)
            .filter(invocation -> invocation.getExpression().contains("history.replaceState"))
            .findFirst()
            .orElseThrow();
    Location location = new Location((String) update.getParameters().get(1));
    assertEquals("account", location.getPath());
    assertEquals(
        Map.of("section", List.of("security"), "tab", List.of("providers")),
        location.getQueryParameters().getParameters());
  }

  private TabSheet securityTabs() {
    return tree(settings())
        .filter(TabSheet.class::isInstance)
        .map(TabSheet.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @Test
  void tabChangesNavigateWithFlowAndHistoryRestorationDoesNotNavigateAgainOrLoseDrafts() {
    doReturn(Optional.of(view))
        .when(testUi)
        .navigate(eq(AccountView.class), any(QueryParameters.class));
    TextField first =
        (TextField)
            tree(settings())
                .filter(c -> c.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow();
    first.setValue("Unsaved draft");
    PageNavigationComponent navigation =
        (PageNavigationComponent)
            tree(settings())
                .filter(c -> c.getId().orElse("").equals("account-navigation"))
                .findFirst()
                .orElseThrow();
    navigation.activate(navigation.getItems().get(2));
    verify(testUi).navigate(AccountView.class, QueryParameters.of("section", "sessions"));
    enter("?section=security&tab=portal");
    assertEquals(AccountSection.SECURITY, settings().getSelectedSection());
    enter("?section=sessions");
    assertEquals(AccountSection.SESSIONS, settings().getSelectedSection());
    assertEquals("Unsaved draft", first.getValue());
    verify(testUi, times(1)).navigate(eq(AccountView.class), any(QueryParameters.class));
    verifyNoInteractions(accountProfileService);
  }

  @Test
  void invalidOrMissingTabResetsEarlierSelectionAndLanguageChangesKeepTheTab() {
    enter("?tab=security");
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    assertEquals(AccountSection.SECURITY, settings().getSelectedSection());
    enter("?tab=unknown");
    assertEquals(AccountSection.PROFILE, settings().getSelectedSection());
    enter("?tab=sessions");
    enter("");
    assertEquals(AccountSection.PROFILE, settings().getSelectedSection());
    verify(testUi, never()).navigate(eq(AccountView.class), any(QueryParameters.class));
  }

  @Test
  void sessionsLimitComesFromConfigurationRatherThanTheThreeSampleDevices() {
    Component limit =
        tree(view)
            .filter(c -> c.getId().orElse("").equals("account-sessions-limit"))
            .findFirst()
            .orElseThrow();
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN);
    assertEquals(
        messageProperties.getAccountSettingsSessionsLimit(11), limit.getElement().getText());
  }

  @Test
  void viewForwardsAnUnauthenticatedVisitorToLogin() {
    when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.empty());
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.hasForwardTarget()).thenReturn(true);
    view.beforeEnter(event);
    verify(event).forwardTo(LoginView.class);
    verify(event, never()).getLocation();
  }

  @Test
  void existingAccountMenuOpensTheNewAccountRouteAndClosesThePopover() {
    UserPopoverMenu menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
    Button button = (Button) ReflectionTestUtils.getField(menu, "manageAccountButton");
    assertTrue(button.isEnabled());
    doReturn(Optional.empty()).when(testUi).navigate(AccountView.class);
    menu.getPopover().setOpened(true);
    button.click();
    assertFalse(menu.getPopover().isOpened());
    verify(testUi).navigate(AccountView.class);
    verify(securityService).getCurrentUser(mockHttpRequest);
    verifyNoMoreInteractions(securityService);
  }

  @Test
  void trackedLanguageEventRefreshesTheAccountSurface() {
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    AccountSettingsComponent content =
        (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettingsComponent");
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH);
    assertTrue(
        content
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsTitle()));
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  @Test
  void saveUsesAuthenticatedRequestRefreshesNameAndAppliesLanguageOnlyAfterSuccess() {
    AccountSettingsComponent content =
        (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettingsComponent");
    LocalDate date = LocalDate.of(1990, 1, 1);
    when(accountProfileService.save(eq(mockHttpRequest), any()))
        .thenReturn(new Profile(4L, "Changed", "Name", date, Locale.ENGLISH, Gender.OTHER));
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
    verify(accountProfileService)
        .save(
            mockHttpRequest,
            new Changes(3L, "Changed", "Account", date, Locale.ENGLISH, Gender.OTHER));
    assertEquals(Locale.ENGLISH, localeReference.get());
    assertEquals("Changed", testAccount.getFirstName());
    List<Component> avatarComponents =
        Stream.concat(
                tree(view),
                tree(
                    ((UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu"))
                        .getPopover()))
            .filter(OverlayUserIcon.class::isInstance)
            .distinct()
            .toList();
    assertEquals(3, avatarComponents.size());
    for (Component avatar : avatarComponents) {
      String expected =
          "Changed Name – "
              + MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                  .getUserStatusLabel(UserStatus.getDefaultStatus());
      assertEquals(expected, avatar.getElement().getAttribute("aria-label"));
    }

    assertEquals(4L, testAccount.getVersion());
    UserPopoverMenu menu = (UserPopoverMenu) ReflectionTestUtils.getField(view, "userPopoverMenu");
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
    AccountSettingsComponent content =
        (AccountSettingsComponent) ReflectionTestUtils.getField(view, "accountSettingsComponent");
    when(accountProfileService.save(eq(mockHttpRequest), any()))
        .thenThrow(new IllegalStateException("Unavailable"));
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
    assertEquals(Locale.GERMAN, localeReference.get());
    assertEquals("Own", testAccount.getFirstName());
    assertEquals(Locale.GERMAN, testAccount.getPreferredLocale());
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
