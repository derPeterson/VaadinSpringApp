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
  UI ui;
  VaadinSession session;
  VaadinServletService servlet;
  MockHttpServletRequest http;
  MockHttpSession login;
  VaadinServletRequest request;
  SecurityService security;
  UserRoleBroadcaster events;
  RoleUiGuard guard;
  AtomicReference<Authentication> current = new AtomicReference<>();

  @BeforeEach
  void setup() {
    servlet = mock(VaadinServletService.class);
    DeploymentConfiguration config = mock(DeploymentConfiguration.class);
    when(servlet.getDeploymentConfiguration()).thenReturn(config);
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getService()).thenReturn(servlet);
    when(session.getLocale()).thenReturn(Locale.ENGLISH);
    when(session.access(any()))
        .thenAnswer(
            c -> {
              ((Command) c.getArgument(0)).execute();
              return CompletableFuture.completedFuture(null);
            });
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    when(servlet.findUI(any())).thenReturn(ui);
    login = new MockHttpSession();
    http = spy(new MockHttpServletRequest("POST", "/VAADIN/uidl"));
    http.setSession(login);
    http.setParameter("v-r", "uidl");
    request = new VaadinServletRequest(http, servlet);
    com.vaadin.flow.internal.CurrentInstance.set(VaadinRequest.class, request);
    security = mock(SecurityService.class);
    events = new UserRoleBroadcaster();
    guard = new RoleUiGuard(security, events);
    current.set(auth("ADMIN"));
    when(security.refreshSessionAuthentication(login))
        .thenAnswer(c -> Optional.ofNullable(current.get()));
    when(security.getSessionUser(login))
        .thenReturn(Optional.of(UserEntity.builder().id(5L).build()));
    location("admin?section=user_management");
  }

  @AfterEach
  void cleanup() {
    ComponentUtil.fireEvent(ui, new DetachEvent(ui));
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    com.vaadin.flow.internal.CurrentInstance.set(VaadinRequest.class, null);
  }

  Authentication auth(String... roles) {
    UserDetails p = User.withUsername("own@example.test").password("unused").roles(roles).build();
    return UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities());
  }

  void location(String path) {
    ReflectionTestUtils.setField(ui.getInternals(), "viewLocation", new Location(path));
  }

  SynchronizedRequestHandler handler() {
    ServiceInitEvent event = new ServiceInitEvent(servlet);
    guard.serviceInit(event);
    return (SynchronizedRequestHandler) event.getAddedRequestHandlers().findFirst().orElseThrow();
  }

  @Test
  void changedRoleRequestWritesFrameworkRedirectWithoutDispatchOrLogout() throws Exception {
    guard.bind(ui);
    current.set(auth("USER"));
    MockHttpServletResponse raw = new MockHttpServletResponse();
    VaadinServletResponse response = new VaadinServletResponse(raw, servlet);
    AtomicInteger dispatch = new AtomicInteger();
    if (!handler().synchronizedHandleRequest(session, request, response)) {
      dispatch.incrementAndGet();
    }
    assertEquals(0, dispatch.get());
    assertTrue(raw.getContentAsString().contains("\"async\":true"));
    assertTrue(raw.getContentAsString().contains("/home"));
    assertTrue(raw.getContentAsString().contains("execute"));
    assertEquals("application/json; charset=UTF-8", raw.getContentType());
    assertFalse(login.isInvalid());
    verify(http, never()).getReader();
    assertTrue(RoleUiState.changed(ui, current.get()));
  }

  @Test
  void unchangedRoleRequestPassesThroughWithoutConsumingBody() throws Exception {
    guard.bind(ui);
    VaadinServletResponse response =
        new VaadinServletResponse(new MockHttpServletResponse(), servlet);
    assertFalse(handler().synchronizedHandleRequest(session, request, response));
    verify(http, never()).getReader();
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
    assertEquals(expected, RoleUiState.target(ui, auth("USER"), "/ctx"));
  }

  @Test
  void roleAdditionRebuildsCurrentLocationWithoutDiscardingAuthentication() {
    current.set(auth("USER"));
    location("home");
    guard.bind(ui);
    current.set(auth("USER", "ADMIN"));
    assertTrue(RoleUiState.refresh(ui, current.get(), ""));
    assertFalse(login.isInvalid());
    assertTrue(
        ui.getInternals().dumpPendingJavaScriptInvocations().stream()
            .anyMatch(js -> js.getInvocation().getParameters().toString().contains("/home")));
  }

  @Test
  void liveEventsRefreshOnlyTheTargetAndDetachRemovesSubscription() {
    guard.bind(ui);
    guard.bind(ui);
    clearInvocations(security);
    current.set(auth("USER"));
    events.publish(999L);
    verifyNoInteractions(security);
    events.publish(5L);
    verify(security).refreshSessionAuthentication(login);
    assertTrue(
        ui.getInternals().dumpPendingJavaScriptInvocations().stream()
            .anyMatch(js -> js.getInvocation().getParameters().toString().contains("/home")));
    ComponentUtil.fireEvent(ui, new DetachEvent(ui));
    clearInvocations(security);
    events.publish(5L);
    verifyNoInteractions(security);
  }

  @Test
  void anonymousUiIsBoundOnlyAfterAuthenticationBecomesAvailable() {
    current.set(null);
    guard.bind(ui);
    assertNull(ComponentUtil.getData(ui, RoleUiState.class));
    current.set(auth("USER"));
    guard.bind(ui);
    assertNotNull(ComponentUtil.getData(ui, RoleUiState.class));
  }
}
