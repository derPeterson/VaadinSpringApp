package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

/** Echte UIDL-Ausgabe des Frameworks; kein Browser und kein eigener JSON-Nachbau. */
class RoleUiGuardTest {
  UI testUi;
  VaadinSession vaadinSession;
  VaadinServletService servlet;
  MockHttpServletRequest mockHttpRequest;
  MockHttpSession mockHttpSession;
  VaadinServletRequest request;
  SecurityService securityService;
  UserRoleBroadcaster events;
  RoleUiGuard guard;
  AtomicReference<Authentication> current = new AtomicReference<>();

  @BeforeEach
  void setup() {
    servlet = mock(VaadinServletService.class);
    DeploymentConfiguration config = mock(DeploymentConfiguration.class);
    when(servlet.getDeploymentConfiguration()).thenReturn(config);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getService()).thenReturn(servlet);
    when(vaadinSession.getLocale()).thenReturn(Locale.ENGLISH);
    when(vaadinSession.access(any()))
        .thenAnswer(c -> {
          ((Command) c.getArgument(0)).execute();
          return CompletableFuture.completedFuture(null);
        });
    testUi = new UI();
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    when(servlet.findUI(any())).thenReturn(testUi);
    mockHttpSession = new MockHttpSession();
    mockHttpRequest = spy(new MockHttpServletRequest("POST", "/VAADIN/uidl"));
    mockHttpRequest.setSession(mockHttpSession);
    mockHttpRequest.setParameter("v-r", "uidl");
    request = new VaadinServletRequest(mockHttpRequest, servlet);
    com.vaadin.flow.internal.CurrentInstance.set(VaadinRequest.class, request);
    securityService = mock(SecurityService.class);
    events = new UserRoleBroadcaster();
    guard = new RoleUiGuard(securityService, events);
    current.set(auth("ADMIN"));
    when(securityService.refreshSessionAuthentication(mockHttpSession))
        .thenAnswer(c -> Optional.ofNullable(current.get()));
    when(securityService.getSessionUser(mockHttpSession))
        .thenReturn(Optional.of(UserEntity.builder().id(5L).build()));
    location("admin?section=user_management");
  }

  @AfterEach
  void cleanup() {
    ComponentUtil.fireEvent(testUi, new DetachEvent(testUi));
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    com.vaadin.flow.internal.CurrentInstance.set(VaadinRequest.class, null);
  }

  Authentication auth(String... roles) {
    UserDetails p = User.withUsername("own@example.test").password("unused").roles(roles).build();
    return UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities());
  }

  void location(String path) {
    ReflectionTestUtils.setField(testUi.getInternals(), "viewLocation", new Location(path));
  }

  SynchronizedRequestHandler handler() {
    ServiceInitEvent event = new ServiceInitEvent(servlet);
    guard.serviceInit(event);
    return (SynchronizedRequestHandler) event.getAddedRequestHandlers().findFirst().orElseThrow();
  }

  @Test
  void changedRoleRequestWritesFrameworkRedirectWithoutDispatchOrLogout() throws Exception {
    guard.bind(testUi);
    current.set(auth("USER"));
    MockHttpServletResponse raw = new MockHttpServletResponse();
    VaadinServletResponse response = new VaadinServletResponse(raw, servlet);
    AtomicInteger dispatch = new AtomicInteger();
    if (!handler().synchronizedHandleRequest(vaadinSession, request, response)) {
      dispatch.incrementAndGet();
    }
    assertEquals(0, dispatch.get());
    assertTrue(raw.getContentAsString().contains("\"async\":true"));
    assertTrue(raw.getContentAsString().contains("/home"));
    assertTrue(raw.getContentAsString().contains("execute"));
    assertEquals("application/json; charset=UTF-8", raw.getContentType());
    assertFalse(mockHttpSession.isInvalid());
    verify(mockHttpRequest, never()).getReader();
    assertTrue(RoleUiState.hasUiRoleStateChanged(testUi, current.get()));
  }

  @Test
  void unchangedRoleRequestPassesThroughWithoutConsumingBody() throws Exception {
    guard.bind(testUi);
    VaadinServletResponse response =
        new VaadinServletResponse(new MockHttpServletResponse(), servlet);
    assertFalse(handler().synchronizedHandleRequest(vaadinSession, request, response));
    verify(mockHttpRequest, never()).getReader();
  }

  @ParameterizedTest
  @CsvSource({
    "admin?section=notifications,/ctx/home",
    "account?tab=security,/ctx/account?tab=security",
    "notifications,/ctx/notifications",
    "home,/ctx/home",
    "login,/ctx/home"
  })
  void permittedLocationAndAccountTabSurviveButLostAdminAccessGoesHome(
      String path, String expected) {
    location(path);
    assertEquals(expected, RoleUiState.resolveRoleRefreshLocation(testUi, auth("USER"), "/ctx"));
  }

  @Test
  void roleAdditionRebuildsCurrentLocationWithoutDiscardingAuthentication() {
    current.set(auth("USER"));
    location("home");
    guard.bind(testUi);
    current.set(auth("USER", "ADMIN"));
    assertTrue(RoleUiState.refreshUiAfterRoleChange(testUi, current.get(), ""));
    assertFalse(mockHttpSession.isInvalid());
    assertTrue(
        testUi.getInternals().dumpPendingJavaScriptInvocations().stream()
            .anyMatch(js -> js.getInvocation().getParameters().toString().contains("/home")));
  }

  @Test
  void liveEventsRefreshOnlyTheTargetAndDetachRemovesSubscription() {
    guard.bind(testUi);
    guard.bind(testUi);
    clearInvocations(securityService);
    current.set(auth("USER"));
    events.publish(999L);
    verifyNoInteractions(securityService);
    events.publish(5L);
    verify(securityService).refreshSessionAuthentication(mockHttpSession);
    assertTrue(
        testUi.getInternals().dumpPendingJavaScriptInvocations().stream()
            .anyMatch(js -> js.getInvocation().getParameters().toString().contains("/home")));
    ComponentUtil.fireEvent(testUi, new DetachEvent(testUi));
    clearInvocations(securityService);
    events.publish(5L);
    verifyNoInteractions(securityService);
  }

  @Test
  void anonymousUiIsBoundOnlyAfterAuthenticationBecomesAvailable() {
    current.set(null);
    guard.bind(testUi);
    assertNull(ComponentUtil.getData(testUi, RoleUiState.class));
    current.set(auth("USER"));
    guard.bind(testUi);
    assertNotNull(ComponentUtil.getData(testUi, RoleUiState.class));
  }
}
