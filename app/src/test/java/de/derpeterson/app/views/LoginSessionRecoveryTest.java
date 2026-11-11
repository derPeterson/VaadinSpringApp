package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionLimitException;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class LoginSessionRecoveryTest {
  private TrackingUI ui;
  private LoginView view;
  private final ConfigService configService = mock(ConfigService.class);
  private final SecurityService securityService = mock(SecurityService.class);
  private final AuthenticationManager authentication = mock(AuthenticationManager.class);
  private final SessionManagementService sessions = mock(SessionManagementService.class);
  private final ExternalLoginFlow externalLogin = mock(ExternalLoginFlow.class);
  private MockHttpServletRequest mockHttpRequest;
  private MockHttpServletResponse mockHttpResponse;
  private VaadinServletResponse vaadinResponse;
  private MessageProperties messageProperties;

  @BeforeEach
  void setup() {
    ui = new TrackingUI();
    UI.setCurrent(ui);
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(vaadinSession);
    ui.getInternals().setSession(vaadinSession);
    mockHttpRequest = new MockHttpServletRequest();
    mockHttpResponse = new MockHttpServletResponse();
    var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
    registry.setRoute("registration", RegistrationView.class, List.of());
    registry.setRoute("forgot-password", ForgotPasswordView.class, List.of());
    registry.setRoute("reactivate-account", ReactivateAccountView.class, List.of());
    TestService service = mock(TestService.class);
    when(service.getRouter()).thenReturn(new Router(registry));
    when(service.getRouteRegistry()).thenReturn(registry);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(service.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(vaadinSession.getService()).thenReturn(service);
    VaadinService.setCurrent(service);
    CurrentInstance.set(VaadinRequest.class, new VaadinServletRequest(mockHttpRequest, service));
    vaadinResponse = new VaadinServletResponse(mockHttpResponse, service);
    CurrentInstance.set(VaadinResponse.class, vaadinResponse);
    when(configService.getString(any()))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    when(externalLogin.loginConfirmed(mockHttpRequest)).thenReturn(true);
    view = createView();
    ui.add(view);
    UserDetails principal =
        User.withUsername("own@example.test").password("").roles("USER").build();
    when(authentication.authenticate(any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    doThrow(new SessionLimitException())
        .when(securityService)
        .storeVerifiedAuthentication(any(), any(), any(), anyBoolean());
    Instant when = Instant.parse("2026-10-17T03:00:00Z");
    when(sessions.recoveryList(mockHttpRequest))
        .thenReturn(
            new SessionManagementService.Snapshot(
                List.of(
                    new SessionManagementService.Entry(
                        "opaque-device",
                        DeviceDescription.from("Windows Chrome/130"),
                        when,
                        when,
                        false,
                        true,
                        true)),
                1,
                1));
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    CurrentInstance.clearAll();
  }

  private <T> T field(String name, Class<T> type) {
    return type.cast(ReflectionTestUtils.getField(view, name));
  }

  private LoginView createView() {
    return new LoginView(
        messageProperties,
        authentication,
        securityService,
        mockHttpRequest,
        configService,
        sessions,
        externalLogin);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "maintenance", "sessions", "throttled", "unavailable"})
  void navigationFailureUsesTheAttachedPublicTheme(String reason) {
    ui.remove(view);
    Location location = new Location("login?error=" + reason);
    BeforeEnterEvent before = mock(BeforeEnterEvent.class);
    when(before.getLocation()).thenReturn(location);
    view.beforeEnter(before);
    NotificationHelper helper = NotificationHelper.getInstance();

    assertFalse(
        ((Notification) ReflectionTestUtils.getField(helper, "currentNotification")).isOpened());
    ui.add(view);
    AfterNavigationEvent after = mock(AfterNavigationEvent.class);
    when(after.getLocation()).thenReturn(location);
    view.afterNavigation(after);
    Notification notification =
        (Notification) ReflectionTestUtils.getField(helper, "currentNotification");

    assertTrue(notification.isOpened());
    assertTrue(notification.getElement().getThemeList().contains("cosmos-reference-error"));
    Supplier<String> message =
        (Supplier<String>) ReflectionTestUtils.getField(helper, "messageSupplier");
    String expected =
        switch (reason) {
          case "maintenance" -> messageProperties.getMaintenanceLoginMessage();
          case "sessions" -> messageProperties.getLoginSessionLimitMessage();
          case "throttled" -> messageProperties.getLoginThrottledMessage();
          case "unavailable" -> messageProperties.getLoginUnavailableMessage();
          default -> messageProperties.getLoginFailedMessage();
        };

    assertEquals(expected, message.get());
  }

  @Test
  void recoveryKeepsPriorityOverAnErrorInTheRedirectUrl() {
    when(sessions.hasRecovery(mockHttpRequest)).thenReturn(true);
    Location location = new Location("login?error=sessions");
    BeforeEnterEvent before = mock(BeforeEnterEvent.class);
    when(before.getLocation()).thenReturn(location);
    view.beforeEnter(before);
    AfterNavigationEvent after = mock(AfterNavigationEvent.class);
    when(after.getLocation()).thenReturn(location);
    view.afterNavigation(after);

    assertTrue(field("sessionRecoveryDiv", Div.class).isVisible());
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");

    assertFalse(notification.isOpened());
  }

  private Button button(String id) {
    return tree(view)
        .filter(item -> item.getId().orElse("").equals(id))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private java.util.stream.Stream<Component> tree(Component root) {
    return java.util.stream.Stream.concat(
        java.util.stream.Stream.of(root), root.getChildren().flatMap(this::tree));
  }

  private void reachedLimit() {
    field("loginEmailField", EmailField.class).setValue("own@example.test");
    field("loginPasswordField", PasswordField.class).setValue("Secret!Password8");
    field("rememberMeSwitch", Switch.class).setValue(true);
    field("loginButton", Button.class).click();
  }

  @Test
  void correctPasswordAtLimitOpensOnlyManagementAndClearsPassword() {
    reachedLimit();

    verify(sessions).startRecovery(mockHttpRequest, "own@example.test", "Secret!Password8", true);
    verify(sessions).recoveryList(mockHttpRequest);
    assertTrue(field("sessionRecoveryDiv", Div.class).isVisible());
    Div recovery = field("sessionRecoveryDiv", Div.class);

    assertEquals(1, recovery.getChildren().count());
    assertTrue(
        recovery
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getSessionRecoveryTitle()));
    assertTrue(
        recovery
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getSessionRecoveryHint()));
    assertTrue(
        recovery
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getSessionRecoveryListHint()));
    Component panel = recovery.getChildren().findFirst().orElseThrow();

    assertTrue(tree(panel).anyMatch(item -> item == button("session-recovery-cancel")));
    assertTrue(tree(panel).anyMatch(item -> item == button("session-recovery-continue")));
    assertFalse(field("loginAuthenticationPanels", Component.class).isVisible());
    assertTrue(field("loginPasswordField", PasswordField.class).isEmpty());
    assertNull(ui.target);
    assertFalse(button("session-recovery-continue").isEnabled());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void switchForwardsRememberChoiceToNormalLogin(boolean remember) {
    Switch rememberSwitch = field("rememberMeSwitch", Switch.class);

    assertFalse(rememberSwitch.getValue());
    rememberSwitch.setValue(remember);
    doNothing()
        .when(securityService)
        .storeVerifiedAuthentication(any(), any(), any(), anyBoolean());
    field("loginEmailField", EmailField.class).setValue("own@example.test");
    field("loginPasswordField", PasswordField.class).setValue("Secret!Password8");

    field("loginButton", Button.class).click();

    verify(securityService).storeVerifiedAuthentication(any(), any(), any(), eq(remember));
    verifyNoInteractions(sessions);
    assertEquals(HomeView.class, ui.target);
  }

  @ParameterizedTest
  @CsvSource({"true,true", "false,false", "invalid,false"})
  void savedSwitchChoiceDoesNotRestoreCredentialsOrAuthenticate(String saved, boolean expected) {
    mockHttpRequest.setCookies(new Cookie("login-remember-choice", saved));
    view = createView();

    assertEquals(expected, field("rememberMeSwitch", Switch.class).getValue());
    assertTrue(field("loginEmailField", EmailField.class).isEmpty());
    assertTrue(field("loginPasswordField", PasswordField.class).isEmpty());
    assertEquals(0, mockHttpResponse.getCookies().length);
    verifyNoInteractions(authentication, securityService, sessions);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void explicitSwitchChangeStoresOnlyPreferenceBeforeLogin(boolean remember) {
    mockHttpRequest.setSecure(true);
    Switch rememberSwitch = field("rememberMeSwitch", Switch.class);
    rememberSwitch.setValue(remember);

    assertEquals(
        0, mockHttpResponse.getCookies().length, "Programmatische Änderungen speichern nichts");

    ComponentUtil.fireEvent(
        rememberSwitch,
        new ComponentValueChangeEvent<>(rememberSwitch, rememberSwitch, !remember, true));

    Cookie choice = mockHttpResponse.getCookie("login-remember-choice");

    assertNotNull(choice);
    assertEquals(Boolean.toString(remember), choice.getValue());
    assertEquals(31536000, choice.getMaxAge());
    assertEquals("/", choice.getPath());
    assertTrue(choice.isHttpOnly());
    assertTrue(choice.getSecure());
    assertEquals("Lax", choice.getAttribute("SameSite"));
    assertNull(mockHttpResponse.getCookie("remember-me"));
    verifyNoInteractions(authentication, securityService, sessions);
  }

  @Test
  void invalidCredentialsNeverOpenRestrictedManagement() {
    when(authentication.authenticate(any()))
        .thenThrow(new BadCredentialsException("secret diagnostic"));
    reachedLimit();

    verifyNoInteractions(sessions);
    assertFalse(field("sessionRecoveryDiv", Div.class).isVisible());
    assertTrue(field("loginAuthenticationPanels", Component.class).isVisible());
    verify(securityService, never()).storeVerifiedAuthentication(any(), any(), any(), anyBoolean());
    Supplier<String> text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getLoginFailedMessage(), text.get());
  }

  @Test
  void cancelDropsProofAndReturnsToNormalLogin() {
    reachedLimit();
    button("session-recovery-cancel").click();

    verify(sessions).cancelRecovery(mockHttpRequest);
    assertFalse(field("sessionRecoveryDiv", Div.class).isVisible());
    assertTrue(field("loginAuthenticationPanels", Component.class).isVisible());
    assertTrue(field("loginPasswordField", PasswordField.class).isEmpty());
  }

  private void freeSessionSlot() {
    when(sessions.recoveryList(mockHttpRequest))
        .thenReturn(new SessionManagementService.Snapshot(List.of(), 0, 1));
    button("account-sessions-refresh").click();

    assertTrue(button("session-recovery-continue").isEnabled());
  }

  @Test
  void continueIsEnabledOnlyAfterAConfirmedRevocationFreesASlot() {
    reachedLimit();

    assertFalse(button("session-recovery-continue").isEnabled());
    button("session-revoke-opaque-device").click();

    assertFalse(button("session-recovery-continue").isEnabled());
    Object sessionsPanel = ReflectionTestUtils.getField(view, "sessionRecoveryComponent");
    Object dialog = ReflectionTestUtils.getField(sessionsPanel, "sessionConfirmationDialog");
    when(sessions.recoveryList(mockHttpRequest))
        .thenReturn(new SessionManagementService.Snapshot(List.of(), 0, 1));
    ((Button) ReflectionTestUtils.getField(dialog, "confirmButton")).click();

    verify(sessions).recoveryRevoke(mockHttpRequest, "opaque-device");
    assertTrue(button("session-recovery-continue").isEnabled());
    verify(sessions, never()).finishRecovery(mockHttpRequest, vaadinResponse);
  }

  @Test
  void successfulCompletionNavigatesOnlyAfterNormalAdmission() {
    reachedLimit();
    freeSessionSlot();
    button("session-recovery-continue").click();

    verify(sessions).finishRecovery(mockHttpRequest, vaadinResponse);
    assertEquals(HomeView.class, ui.target);
    verify(authentication, times(1)).authenticate(any());
  }

  @Test
  void admittedFirstFactorRequiresTwoFactorBeforePortalAndSuccessNotice() {
    when(externalLogin.loginConfirmed(mockHttpRequest)).thenReturn(false);
    reachedLimit();
    freeSessionSlot();
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    button("session-recovery-continue").click();

    assertEquals(TwoFactorView.class, ui.target);
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");

    assertFalse(notification.isOpened());
  }

  @Test
  void fullLimitDoesNotNavigateOrDiscardManagement() {
    reachedLimit();
    SessionManagementService.Snapshot full =
        (SessionManagementService.Snapshot)
            ReflectionTestUtils.getField(
                field("sessionRecoveryComponent", Component.class), "sessionSnapshot");
    freeSessionSlot();
    when(sessions.recoveryList(mockHttpRequest)).thenReturn(full);
    when(sessions.finishRecovery(mockHttpRequest, vaadinResponse))
        .thenThrow(new SessionLimitException());
    button("session-recovery-continue").click();

    assertTrue(field("sessionRecoveryDiv", Div.class).isVisible());
    assertFalse(button("session-recovery-continue").isEnabled());
    assertNull(ui.target);
    Supplier<String> text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getLoginSessionLimitMessage(), text.get());
  }

  @Test
  void expiredProofReturnsToLoginWithUnderstandableNotification() {
    reachedLimit();
    freeSessionSlot();
    when(sessions.finishRecovery(mockHttpRequest, vaadinResponse))
        .thenThrow(new AuthenticationCredentialsNotFoundException("expired"));
    button("session-recovery-continue").click();

    assertTrue(field("loginAuthenticationPanels", Component.class).isVisible());
    assertFalse(field("sessionRecoveryDiv", Div.class).isVisible());
    assertNull(ui.target);
    Supplier<String> text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(messageProperties.getSessionRecoveryExpired(), text.get());
  }

  private static class TrackingUI extends UI {
    private Class<? extends Component> target;

    @Override
    public <T extends Component> Optional<T> navigate(Class<T> target) {
      this.target = target;
      return Optional.empty();
    }
  }

  public static class TestService extends VaadinServletService {
    @Override
    public RouteRegistry getRouteRegistry() {
      return super.getRouteRegistry();
    }
  }
}
