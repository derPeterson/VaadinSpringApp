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
  private AccountView accountView;
  private AccountProfileService accountProfileService;
  private UserService userService;
  private UserEntity testAccount;
  private ConfigService configService;
  private UserAvatarService userAvatarService;
  private ExternalLoginFlow externalLoginFlow;
  private ExternalIdentityService externalIdentityService;
  private final AtomicReference<Locale> sessionLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    testUi = spy(new UI());
    UI.setCurrent(testUi);

    VaadinSession mockVaadinSession = mock(VaadinSession.class);
    when(mockVaadinSession.hasLock()).thenReturn(true);
    VaadinService mockVaadinService = mock(VaadinService.class);
    DeploymentConfiguration mockDeploymentConfiguration = mock(DeploymentConfiguration.class);
    when(mockVaadinService.getDeploymentConfiguration()).thenReturn(mockDeploymentConfiguration);
    when(mockVaadinSession.getService()).thenReturn(mockVaadinService);
    when(mockVaadinSession.getLocale()).thenAnswer(mockInvocation -> sessionLocaleReference.get());
    doAnswer(
            mockInvocation -> {
              sessionLocaleReference.set(mockInvocation.getArgument(0));
              return null;
            })
        .when(mockVaadinSession)
        .setLocale(any());
    VaadinSession.setCurrent(mockVaadinSession);
    testUi.getInternals().setSession(mockVaadinSession);

    mockHttpRequest = mock(HttpServletRequest.class);
    securityService = mock(SecurityService.class);
    configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(mockInvocation ->
            ((ConfigEntry) mockInvocation.getArgument(0)).getDefaultValueString());
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
    externalLoginFlow = mock(ExternalLoginFlow.class);
    externalIdentityService = mock(ExternalIdentityService.class);

    accountView = createAccountView();
  }

  private AccountView createAccountView() {
    return new AccountView(
        new MessageProperties(new CustomI18NProvider()),
        securityService,
        mockHttpRequest,
        configService,
        userService,
        accountProfileService,
        mock(AccountSecurityService.class),
        mockSessionManagementService(),
        new UserStatusBroadcaster(),
        userAvatarService,
        externalLoginFlow,
        externalIdentityService);
  }

  @Test
  void directAccountRouteKeepsExistingConnectionsWhenAllProvidersAreDisabled() {
    when(externalIdentityService.connected(testAccount.getId())).thenReturn(List.of("google"));
    when(externalLoginFlow.owner(mockHttpRequest)).thenReturn(testAccount);
    accountView = createAccountView();

    TabSheet securityTabSheet =
        streamComponentTree(getAccountSettingsComponent())
            .filter(TabSheet.class::isInstance)
            .map(TabSheet.class::cast)
            .findFirst()
            .orElseThrow();
    Component providersTabContent = securityTabSheet.getComponent(securityTabSheet.getTabAt(1));
    ExternalConnectionsComponent externalConnectionsComponent =
        streamComponentTree(providersTabContent)
            .filter(ExternalConnectionsComponent.class::isInstance)
            .map(ExternalConnectionsComponent.class::cast)
            .findFirst()
            .orElseThrow();

    assertTrue(
        streamComponentTree(externalConnectionsComponent)
            .anyMatch(component -> component.getId().orElse("").equals("external-connect-google")));
    assertEquals(
        1,
        streamComponentTree(providersTabContent)
            .filter(ExternalConnectionsComponent.class::isInstance)
            .count());
  }

  @SuppressWarnings("unchecked")
  private Select<UserStatus> getAvatarStatusSelect() {
    return (Select<UserStatus>)
        findComponentById(getAccountSettingsComponent(), "account-avatar-status");
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void avatarStatusUsesTheExistingSaveActionAndTracksOtherStatusChanges(String languageTag) {
    LanguageChangeEvent.fire(testUi, Locale.forLanguageTag(languageTag));
    Select<UserStatus> avatarStatusSelect = getAvatarStatusSelect();

    assertEquals(UserStatus.getDefaultStatus(), avatarStatusSelect.getValue());
    assertTrue(avatarStatusSelect.isEnabled());
    assertTrue(avatarStatusSelect.getElement().getThemeList().contains("cosmos-status"));
    assertEquals(
        List.of(UserStatus.values()), avatarStatusSelect.getListDataView().getItems().toList());
    assertEquals("Status", avatarStatusSelect.getAriaLabel().orElseThrow());

    when(userService.updateUserStatus(testAccount, UserStatus.ABSENT, true))
        .thenReturn(new UserService.StatusUpdate(UserStatus.ABSENT, true));
    ReflectionTestUtils.invokeMethod(avatarStatusSelect, "setModelValue", UserStatus.ABSENT, true);

    verify(userService).updateUserStatus(testAccount, UserStatus.ABSENT, true);
    UserPopoverMenu userPopoverMenu = getUserPopoverMenu();
    OverlayUserIcon popoverAvatarIcon =
        (OverlayUserIcon) ReflectionTestUtils.getField(userPopoverMenu, "overlayIcon");

    assertEquals(UserStatus.ABSENT, ReflectionTestUtils.getField(popoverAvatarIcon, "userStatus"));
    assertTrue(
        ((Div) ReflectionTestUtils.getField(popoverAvatarIcon, "statusIndicatorContainer"))
            .isVisible());
    assertEquals(UserStatus.ABSENT, testAccount.getStatus());
    assertTrue(testAccount.isStatusManuallySet());
    assertEquals(UserStatus.ABSENT, avatarStatusSelect.getValue());

    Object accountAvatarSummary =
        ReflectionTestUtils.getField(
            ReflectionTestUtils.getField(getAccountSettingsComponent(), "accountSidebar"),
            "accountAvatar");
    Tooltip avatarIdentityTooltip =
        (Tooltip) ReflectionTestUtils.getField(accountAvatarSummary, "avatarIdentityTooltip");

    assertTrue(avatarIdentityTooltip.isManual());
    assertFalse(avatarIdentityTooltip.isOpened());
    assertEquals("", avatarIdentityTooltip.getText());
    assertMenuStatus(userPopoverMenu, UserStatus.ABSENT);
    Button userIconButton = (Button) ReflectionTestUtils.getField(accountView, "userIconButton");

    assertEquals(
        UserStatus.ABSENT, ReflectionTestUtils.getField(userIconButton.getIcon(), "userStatus"));

    userPopoverMenu.applyExternalStatus(UserStatus.EMPLOYED);

    assertMenuStatus(userPopoverMenu, UserStatus.EMPLOYED);
    assertEquals(UserStatus.EMPLOYED, avatarStatusSelect.getValue());

    LanguageChangeEvent.fire(testUi, Locale.forLanguageTag(languageTag.equals("de") ? "en" : "de"));

    assertEquals(UserStatus.EMPLOYED, avatarStatusSelect.getValue());
    assertEquals("", avatarIdentityTooltip.getText());
    assertEquals(
        new MessageProperties(new CustomI18NProvider()).getUserStatusEmployed(),
        avatarStatusSelect.getItemLabelGenerator().apply(UserStatus.EMPLOYED));
    verifyNoMoreInteractions(userService);
    verifyNoInteractions(accountProfileService);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void failedAvatarStatusChangeRestoresSelectionAndMenuAndShowsTheExistingError(
      boolean optimisticLockingConflict) {
    Select<UserStatus> avatarStatusSelect = getAvatarStatusSelect();
    when(userService.updateUserStatus(testAccount, UserStatus.ABSENT, true))
        .thenThrow(
            optimisticLockingConflict
                ? new OptimisticLockingFailureException("conflict")
                : new IllegalStateException("failed"));
    ReflectionTestUtils.invokeMethod(avatarStatusSelect, "setModelValue", UserStatus.ABSENT, true);

    assertEquals(UserStatus.getDefaultStatus(), avatarStatusSelect.getValue());
    assertEquals(UserStatus.getDefaultStatus(), testAccount.getStatus());
    UserPopoverMenu userPopoverMenu = getUserPopoverMenu();
    assertMenuStatus(userPopoverMenu, UserStatus.getDefaultStatus());
    Notification errorNotification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");

    assertTrue(errorNotification.isOpened());
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    Html notificationMessageHtml =
        (Html) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText");

    assertEquals(
        optimisticLockingConflict
            ? messageProperties.getBaseUserUpdateConflict()
            : messageProperties.getBaseUserStatusFailed(),
        notificationMessageHtml.getElement().getProperty("innerHTML"));
    verify(userService).updateUserStatus(testAccount, UserStatus.ABSENT, true);
    verifyNoMoreInteractions(userService);
  }

  private void assertMenuStatus(UserPopoverMenu userPopoverMenu, UserStatus expectedStatus) {
    List<Component> statusOptionComponents =
        streamComponentTree(userPopoverMenu.getPopover())
            .filter(component -> component.hasClassName("portal-status-option"))
            .toList();
    String expectedStatusAttribute = expectedStatus.name().toLowerCase(Locale.ROOT);

    assertEquals(UserStatus.values().length, statusOptionComponents.size());

    int matchingStatusOptionCount = 0;
    for (Component statusOptionComponent : statusOptionComponents) {
      boolean isExpectedStatusOption =
          expectedStatusAttribute.equals(
              statusOptionComponent.getElement().getAttribute("data-status"));
      if (isExpectedStatusOption) {
        matchingStatusOptionCount++;
      }

      assertEquals(
          Boolean.toString(isExpectedStatusOption),
          statusOptionComponent.getElement().getAttribute("aria-pressed"));
    }

    assertEquals(1, matchingStatusOptionCount, "Expected exactly one option for " + expectedStatus);
  }

  private SessionManagementService mockSessionManagementService() {
    SessionManagementService mockSessionManagementService = mock(SessionManagementService.class);
    when(mockSessionManagementService.list(any()))
        .thenReturn(new SessionManagementService.Snapshot(List.of(), 0, 11));

    return mockSessionManagementService;
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private AccountSettingsComponent getAccountSettingsComponent() {
    return (AccountSettingsComponent)
        ReflectionTestUtils.getField(accountView, "accountSettingsComponent");
  }

  private UserPopoverMenu getUserPopoverMenu() {
    return (UserPopoverMenu) ReflectionTestUtils.getField(accountView, "userPopoverMenu");
  }

  private void enterAccountRoute(String queryString) {
    BeforeEnterEvent beforeEnterEvent = mock(BeforeEnterEvent.class);
    when(beforeEnterEvent.getLocation())
        .thenReturn(new Location(AppRouteConstants.ACCOUNT_ROUTE + queryString));

    accountView.beforeEnter(beforeEnterEvent);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"FRESH_AUTHENTICATION_REQUIRED", "IDENTITY_MISMATCH", "UNAVAILABLE", "unknown"})
  void failedExternalConfirmationStaysInSecurityAndTranslatesItsNotification(
      String externalErrorCode) {
    enterAccountRoute("?tab=security&external_error=" + externalErrorCode);

    assertEquals(AccountSection.SECURITY, getAccountSettingsComponent().getSelectedSection());
    assertFalse(
        ((Notification)
                ReflectionTestUtils.getField(
                    NotificationHelper.getInstance(), "currentNotification"))
            .isOpened());
    testUi.add(accountView);
    AfterNavigationEvent afterNavigationEvent = mock(AfterNavigationEvent.class);
    when(afterNavigationEvent.getLocation())
        .thenReturn(new Location("account?tab=security&external_error=" + externalErrorCode));
    accountView.afterNavigation(afterNavigationEvent);
    Notification errorNotification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");

    assertTrue(errorNotification.isOpened());
    assertTrue(errorNotification.getElement().getThemeList().contains("cosmos"));
    assertTrue(errorNotification.getElement().getThemeList().contains("cosmos-reference"));
    assertTrue(errorNotification.getElement().getThemeList().contains("cosmos-reference-error"));
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    for (Locale displayLocale : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      LanguageChangeEvent.fire(testUi, displayLocale);
      Html notificationMessageHtml =
          (Html) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText");
      String expectedNotificationHtml =
          switch (externalErrorCode) {
            case "FRESH_AUTHENTICATION_REQUIRED" ->
                messageProperties.getExternalProofFreshRequired();
            case "IDENTITY_MISMATCH" -> messageProperties.getExternalProofIdentityMismatch();
            default -> messageProperties.getExternalError();
          };

      assertEquals(
          expectedNotificationHtml, notificationMessageHtml.getElement().getProperty("innerHTML"));
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
  void accountUrlsRestoreExactlyOneVisibleSection(
      String queryString, AccountSection expectedAccountSection) {
    enterAccountRoute(queryString);

    assertEquals(expectedAccountSection, getAccountSettingsComponent().getSelectedSection());
    for (AccountSection accountSection : AccountSection.values()) {
      String panelId = "account-panel-" + accountSection.name().toLowerCase(Locale.ROOT);
      Component sectionPanel =
          streamComponentTree(getAccountSettingsComponent())
              .filter(component -> component.getId().orElse("").equals(panelId))
              .findFirst()
              .orElseThrow();

      assertEquals(accountSection == expectedAccountSection, sectionPanel.isVisible());
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
  void securityUrlsRestoreTheRequestedTab(String queryString, String expectedTabIdSuffix) {
    enterAccountRoute("?section=security&tab=providers");
    enterAccountRoute(queryString);

    assertEquals(
        "account-security-tab-" + expectedTabIdSuffix,
        getSecurityTabSheet().getSelectedTab().getId().orElseThrow());
  }

  @Test
  void aClientTabChangeReplacesTheCurrentAccountLocation() {
    UI attachedAccountUi = new UI();
    attachedAccountUi.getInternals().setSession(testUi.getSession());
    UI.setCurrent(attachedAccountUi);
    attachedAccountUi.add(accountView);
    enterAccountRoute("?section=security&tab=portal");
    TabSheet securityTabSheet = getSecurityTabSheet();
    securityTabSheet.setSelectedIndex(1);
    Tab portalTab = securityTabSheet.getTabAt(0);
    Tabs securityTabBar = (Tabs) portalTab.getParent().orElseThrow();
    ComponentUtil.fireEvent(
        securityTabBar, new Tabs.SelectedChangeEvent(securityTabBar, portalTab, true));
    JavaScriptInvocation replaceHistoryInvocation =
        attachedAccountUi.getInternals().dumpPendingJavaScriptInvocations().stream()
            .map(PendingJavaScriptInvocation::getInvocation)
            .filter(invocation -> invocation.getExpression().contains("history.replaceState"))
            .findFirst()
            .orElseThrow();
    Location updatedAccountLocation =
        new Location((String) replaceHistoryInvocation.getParameters().get(1));

    assertEquals("account", updatedAccountLocation.getPath());
    assertEquals(
        Map.of("section", List.of("security"), "tab", List.of("providers")),
        updatedAccountLocation.getQueryParameters().getParameters());
  }

  private TabSheet getSecurityTabSheet() {
    return streamComponentTree(getAccountSettingsComponent())
        .filter(TabSheet.class::isInstance)
        .map(TabSheet.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @Test
  void tabChangesNavigateWithFlowAndHistoryRestorationDoesNotNavigateAgainOrLoseDrafts() {
    doReturn(Optional.of(accountView))
        .when(testUi)
        .navigate(eq(AccountView.class), any(QueryParameters.class));
    TextField firstNameField =
        (TextField)
            streamComponentTree(getAccountSettingsComponent())
                .filter(component -> component.getId().orElse("").equals("account-first-name"))
                .findFirst()
                .orElseThrow();
    firstNameField.setValue("Unsaved draft");
    PageNavigationComponent accountNavigation =
        (PageNavigationComponent)
            streamComponentTree(getAccountSettingsComponent())
                .filter(component -> component.getId().orElse("").equals("account-navigation"))
                .findFirst()
                .orElseThrow();
    accountNavigation.activate(accountNavigation.getItems().get(2));

    verify(testUi).navigate(AccountView.class, QueryParameters.of("section", "sessions"));
    enterAccountRoute("?section=security&tab=portal");

    assertEquals(AccountSection.SECURITY, getAccountSettingsComponent().getSelectedSection());
    enterAccountRoute("?section=sessions");

    assertEquals(AccountSection.SESSIONS, getAccountSettingsComponent().getSelectedSection());
    assertEquals("Unsaved draft", firstNameField.getValue());
    verify(testUi, times(1)).navigate(eq(AccountView.class), any(QueryParameters.class));
    verifyNoInteractions(accountProfileService);
  }

  @Test
  void invalidOrMissingTabResetsEarlierSelectionAndLanguageChangesKeepTheTab() {
    enterAccountRoute("?tab=security");
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);

    assertEquals(AccountSection.SECURITY, getAccountSettingsComponent().getSelectedSection());
    enterAccountRoute("?tab=unknown");

    assertEquals(AccountSection.PROFILE, getAccountSettingsComponent().getSelectedSection());
    enterAccountRoute("?tab=sessions");
    enterAccountRoute("");

    assertEquals(AccountSection.PROFILE, getAccountSettingsComponent().getSelectedSection());
    verify(testUi, never()).navigate(eq(AccountView.class), any(QueryParameters.class));
  }

  @Test
  void sessionsLimitComesFromConfigurationRatherThanTheThreeSampleDevices() {
    Component sessionLimitComponent =
        streamComponentTree(accountView)
            .filter(component -> component.getId().orElse("").equals("account-sessions-limit"))
            .findFirst()
            .orElseThrow();
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN);

    assertEquals(
        messageProperties.getAccountSettingsSessionsLimit(11),
        sessionLimitComponent.getElement().getText());
  }

  @Test
  void viewForwardsAnUnauthenticatedVisitorToLogin() {
    when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.empty());
    BeforeEnterEvent beforeEnterEvent = mock(BeforeEnterEvent.class);
    when(beforeEnterEvent.hasForwardTarget()).thenReturn(true);
    accountView.beforeEnter(beforeEnterEvent);

    verify(beforeEnterEvent).forwardTo(LoginView.class);
    verify(beforeEnterEvent, never()).getLocation();
  }

  @Test
  void existingAccountMenuOpensTheNewAccountRouteAndClosesThePopover() {
    UserPopoverMenu userPopoverMenu = getUserPopoverMenu();
    Button manageAccountButton =
        (Button) ReflectionTestUtils.getField(userPopoverMenu, "manageAccountButton");

    assertTrue(manageAccountButton.isEnabled());
    doReturn(Optional.empty()).when(testUi).navigate(AccountView.class);
    userPopoverMenu.getPopover().setOpened(true);
    manageAccountButton.click();

    assertFalse(userPopoverMenu.getPopover().isOpened());
    verify(testUi).navigate(AccountView.class);
    verify(securityService).getCurrentUser(mockHttpRequest);
    verifyNoMoreInteractions(securityService);
  }

  @Test
  void trackedLanguageEventRefreshesTheAccountSurface() {
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    AccountSettingsComponent accountSettingsComponent = getAccountSettingsComponent();
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH);

    assertTrue(
        accountSettingsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsTitle()));
  }

  private Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::streamComponentTree));
  }

  @Test
  void saveUsesAuthenticatedRequestRefreshesNameAndAppliesLanguageOnlyAfterSuccess() {
    AccountSettingsComponent accountSettingsComponent = getAccountSettingsComponent();
    LocalDate birthDate = LocalDate.of(1990, 1, 1);
    when(accountProfileService.save(eq(mockHttpRequest), any()))
        .thenReturn(new Profile(4L, "Changed", "Name", birthDate, Locale.ENGLISH, Gender.OTHER));

    findFirstNameField(accountSettingsComponent).setValue("Changed");
    findPreferredLanguageSelector(accountSettingsComponent).setValue(Locale.ENGLISH);
    findSaveProfileButton(accountSettingsComponent).click();

    verify(accountProfileService)
        .save(
            mockHttpRequest,
            new Changes(3L, "Changed", "Account", birthDate, Locale.ENGLISH, Gender.OTHER));
    assertEquals(Locale.ENGLISH, sessionLocaleReference.get());
    assertEquals("Changed", testAccount.getFirstName());
    List<Component> avatarComponents =
        Stream.concat(
                streamComponentTree(accountView),
                streamComponentTree(getUserPopoverMenu().getPopover()))
            .filter(OverlayUserIcon.class::isInstance)
            .distinct()
            .toList();

    assertEquals(3, avatarComponents.size());
    for (Component avatar : avatarComponents) {
      String expectedAvatarLabel =
          "Changed Name – "
              + MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                  .getUserStatusLabel(UserStatus.getDefaultStatus());

      assertEquals(expectedAvatarLabel, avatar.getElement().getAttribute("aria-label"));
    }

    assertEquals(4L, testAccount.getVersion());
    UserPopoverMenu userPopoverMenu = getUserPopoverMenu();

    assertTrue(
        userPopoverMenu.getPopover().getElement().getTextRecursively().contains("Changed Name"));
    assertTrue(
        accountSettingsComponent
            .getElement()
            .getTextRecursively()
            .contains(
                MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
                    .getAccountSettingsTitle()));
  }

  @Test
  void failedSaveKeepsDraftAndDoesNotChangeLanguageOrTheUserSnapshot() {
    AccountSettingsComponent accountSettingsComponent = getAccountSettingsComponent();
    when(accountProfileService.save(eq(mockHttpRequest), any()))
        .thenThrow(new IllegalStateException("Unavailable"));

    findFirstNameField(accountSettingsComponent).setValue("Draft");
    findPreferredLanguageSelector(accountSettingsComponent).setValue(Locale.ENGLISH);
    findSaveProfileButton(accountSettingsComponent).click();

    assertEquals(Locale.GERMAN, sessionLocaleReference.get());
    assertEquals("Own", testAccount.getFirstName());
    assertEquals(Locale.GERMAN, testAccount.getPreferredLocale());
    assertEquals("Draft", findFirstNameField(accountSettingsComponent).getValue());
  }

  private TextField findFirstNameField(AccountSettingsComponent accountSettingsComponent) {
    return (TextField) findComponentById(accountSettingsComponent, "account-first-name");
  }

  @SuppressWarnings("unchecked")
  private ComboBox<Locale> findPreferredLanguageSelector(
      AccountSettingsComponent accountSettingsComponent) {
    return (ComboBox<Locale>) findComponentById(accountSettingsComponent, "account-language");
  }

  private Button findSaveProfileButton(AccountSettingsComponent accountSettingsComponent) {
    return (Button) findComponentById(accountSettingsComponent, "account-save");
  }

  private Component findComponentById(Component rootComponent, String componentId) {
    return streamComponentTree(rootComponent)
        .filter(component -> component.getId().orElse("").equals(componentId))
        .findFirst()
        .orElseThrow();
  }
}
