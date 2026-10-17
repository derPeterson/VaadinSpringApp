package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.ConfigService;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class LoginSessionRecoveryTest {
  private TrackingUI ui;
  private LoginView view;
  private final ConfigService config = mock(ConfigService.class);
  private final SecurityService security = mock(SecurityService.class);
  private final AuthenticationManager authentication = mock(AuthenticationManager.class);
  private final SessionManagementService sessions = mock(SessionManagementService.class);
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private VaadinServletResponse vaadinResponse;
  private MessageProperties messages;

  @BeforeEach
  void setup() {
    ui = new TrackingUI();
    UI.setCurrent(ui);
    var vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(vaadinSession);
    ui.getInternals().setSession(vaadinSession);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
    registry.setRoute("registration", RegistrationView.class, List.of());
    registry.setRoute("forgot-password", ForgotPasswordView.class, List.of());
    var service = mock(TestService.class);
    when(service.getRouter()).thenReturn(new Router(registry));
    when(service.getRouteRegistry()).thenReturn(registry);
    when(service.getDeploymentConfiguration()).thenReturn(mock(DeploymentConfiguration.class));
    when(vaadinSession.getService()).thenReturn(service);
    VaadinService.setCurrent(service);
    CurrentInstance.set(VaadinRequest.class, new VaadinServletRequest(request, service));
    vaadinResponse = new VaadinServletResponse(response, service);
    CurrentInstance.set(VaadinResponse.class, vaadinResponse);
    when(config.getString(any()))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    messages = new MessageProperties(new CustomI18NProvider());
    view = new LoginView(messages, authentication, security, request, config, sessions);
    ui.add(view);
    var principal = User.withUsername("own@example.test").password("").roles("USER").build();
    when(authentication.authenticate(any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    doThrow(new SessionLimitException())
        .when(security)
        .storeAuthenticatedUser(any(), any(), any(), anyBoolean());
    var when = Instant.parse("2026-10-17T03:00:00Z");
    when(sessions.recoveryList(request))
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
    field("emailField", EmailField.class).setValue("own@example.test");
    field("passwordField", PasswordField.class).setValue("Secret!Password8");
    field("rememberMeCheckBox", Checkbox.class).setValue(true);
    field("loginButton", Button.class).click();
  }

  @Test
  void correctPasswordAtLimitOpensOnlyManagementAndClearsPassword() {
    reachedLimit();
    verify(sessions).startRecovery(request, "own@example.test", "Secret!Password8", true);
    verify(sessions).recoveryList(request);
    assertTrue(field("recoveryArea", Div.class).isVisible());
    assertFalse(field("loginCard", Component.class).isVisible());
    assertTrue(field("passwordField", PasswordField.class).isEmpty());
    assertNull(ui.target);
  }

  @Test
  void invalidCredentialsNeverOpenRestrictedManagement() {
    when(authentication.authenticate(any()))
        .thenThrow(new BadCredentialsException("secret diagnostic"));
    reachedLimit();
    verifyNoInteractions(sessions);
    assertFalse(field("recoveryArea", Div.class).isVisible());
    assertTrue(field("loginCard", Component.class).isVisible());
    verify(security, never()).storeAuthenticatedUser(any(), any(), any(), anyBoolean());
    var text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messages.getLoginFailedMessage(), text.get());
  }

  @Test
  void cancelDropsProofAndReturnsToNormalLogin() {
    reachedLimit();
    button("session-recovery-cancel").click();
    verify(sessions).cancelRecovery(request);
    assertFalse(field("recoveryArea", Div.class).isVisible());
    assertTrue(field("loginCard", Component.class).isVisible());
    assertTrue(field("passwordField", PasswordField.class).isEmpty());
  }

  @Test
  void successfulCompletionNavigatesOnlyAfterNormalAdmission() {
    reachedLimit();
    button("session-recovery-continue").click();
    verify(sessions).finishRecovery(request, vaadinResponse);
    assertEquals(HomeView.class, ui.target);
    verify(authentication, times(1)).authenticate(any());
  }

  @Test
  void fullLimitDoesNotNavigateOrDiscardManagement() {
    reachedLimit();
    when(sessions.finishRecovery(request, vaadinResponse)).thenThrow(new SessionLimitException());
    button("session-recovery-continue").click();
    assertTrue(field("recoveryArea", Div.class).isVisible());
    assertNull(ui.target);
    var text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messages.getLoginSessionLimitMessage(), text.get());
  }

  @Test
  void expiredProofReturnsToLoginWithUnderstandableNotification() {
    reachedLimit();
    when(sessions.finishRecovery(request, vaadinResponse))
        .thenThrow(new AuthenticationCredentialsNotFoundException("expired"));
    button("session-recovery-continue").click();
    assertTrue(field("loginCard", Component.class).isVisible());
    assertFalse(field("recoveryArea", Div.class).isVisible());
    assertNull(ui.target);
    var text =
        (Supplier<String>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messages.getSessionRecoveryExpired(), text.get());
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
