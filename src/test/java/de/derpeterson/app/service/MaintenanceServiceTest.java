package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.MaintenanceAuthenticationException;
import de.derpeterson.app.security.MaintenanceHttpFilter;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceTransport;
import de.derpeterson.app.security.MaintenanceUiGuard;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.RegistrationView;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

class MaintenanceServiceTest {
  private final ConfigService config = mock(ConfigService.class);
  private final MaintenanceService maintenance = new MaintenanceService(config);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private Authentication actor(RoleType role) {
    UserDetails user =
        User.withUsername("maintenance@example.test")
            .password("unused")
            .authorities(role.getGrantedRoles().stream().map(Enum::name).toArray(String[]::new))
            .build();
    return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void liveSwitchRespectsAllRolesWithoutEvictingExistingSession(RoleType role) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(actor(role));
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    request.setSession(session);
    assertFalse(maintenance.blocks(request));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertEquals(!role.isAdministrative(), maintenance.blocks(request));
    assertFalse(session.isInvalid());
    assertSame(
        context,
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    assertFalse(maintenance.blocks(request));
  }

  @Test
  void requestSessionIdentityWinsOverAnUnrelatedPushWorkerContext() {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    SecurityContextHolder.getContext().setAuthentication(actor(RoleType.ROLE_ADMIN));
    assertNull(MaintenanceService.authentication(request));
    assertTrue(maintenance.blocks(request));
    assertTrue(maintenance.blocks(new MockHttpServletRequest()));
    SecurityContext stored = SecurityContextHolder.createEmptyContext();
    Authentication user = actor(RoleType.ROLE_USER);
    stored.setAuthentication(user);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, stored);
    SecurityContextHolder.getContext().setAuthentication(actor(RoleType.ROLE_ADMIN));
    assertSame(user, MaintenanceService.authentication(request));
    assertTrue(maintenance.blocks(request));
    stored.setAuthentication(null);
    assertNull(MaintenanceService.authentication(request));
    assertTrue(maintenance.blocks(request));
    Authentication admin = actor(RoleType.ROLE_SUPERADMIN);
    stored.setAuthentication(admin);
    SecurityContextHolder.getContext().setAuthentication(user);
    assertSame(admin, MaintenanceService.authentication(request));
    assertFalse(maintenance.blocks(request));
  }

  @Test
  void anonymousAndUnreadableConfigurationFailClosed() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertTrue(maintenance.blocks(request));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalArgumentException("invalid boolean"));
    assertEquals(MaintenanceService.State.UNAVAILABLE, maintenance.state());
    assertTrue(maintenance.blocks(request));
    assertThrows(
        AuthenticationServiceException.class,
        () -> maintenance.requireLogin(actor(RoleType.ROLE_SUPERADMIN)));
    assertThrows(
        MaintenanceAccessException.class, () -> MaintenanceService.requirePublicAccess(config));
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void newAdmissionIsCheckedBeforeSessionAndRegistryAllocation(RoleType role) {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    SessionRegistryImpl registry = new SessionRegistryImpl();
    SessionAdmissionService admission = new SessionAdmissionService(config, registry);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    if (role.isAdministrative()) {
      admission.onAuthentication(actor(role), request, response);
      assertEquals(1, registry.getAllSessions("maintenance@example.test", true).size());
    } else {
      assertThrows(
          MaintenanceAuthenticationException.class,
          () -> admission.onAuthentication(actor(role), request, response));
      assertNull(request.getSession(false));
      assertTrue(registry.getAllPrincipals().isEmpty());
      assertEquals("maintenance", request.getAttribute(SessionAdmissionService.FAILURE_REASON));
    }
  }

  @Test
  void existingSlotCanBeRestoredDuringMaintenanceAndReusedAfterwards() {
    when(config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    SessionRegistryImpl registry = new SessionRegistryImpl();
    SessionAdmissionService admission = new SessionAdmissionService(config, registry);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    Authentication user = actor(RoleType.ROLE_USER);
    admission.onAuthentication(user, request, response);
    HttpSession session = request.getSession(false);
    String id = session.getId();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    admission.complete(user, request, response, () -> {});
    assertEquals(id, session.getId());
    assertEquals(1, registry.getAllSessions(user.getName(), true).size());
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    admission.onAuthentication(user, request, response);
    assertSame(session, request.getSession(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "/home",
        "/admin",
        "/registration",
        "/verification/secret-token",
        "/reset-password/secret-token",
        "/h2/"
      })
  void initialDocumentUses503AndMaintenanceViewWithoutTokenEcho(String path) throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    assertEquals(503, response.getStatus());
    assertEquals("/maintenance", response.getForwardedUrl());
    assertFalse(response.getContentAsString().contains("secret-token"));
    assertNull(response.getHeader("Retry-After"));
    verifyNoInteractions(chain);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/custom-theme/cosmos-theme.css",
        "/VAADIN/build/app.js",
        "/icons/icon.png",
        "/login",
        "/maintenance",
        "/logout"
      })
  void requiredResourcesAndRecoveryStayReachable(String path) throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/home", "/login", "/VAADIN/uidl"})
  void oldOrSpoofedUiCannotDispatchCallbacksThroughPublicLoginPath(String path) throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    request.setParameter("v-r", "uidl");
    request.setParameter("v-uiId", "123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    verifyNoInteractions(chain);
    assertTrue(response.getContentType().startsWith("application/json"));
    assertTrue(response.getContentAsString().contains("/maintenance"));
    assertFalse(response.getContentAsString().contains("<html"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"uidl", "push"})
  void deserializedSessionBeforeVaadinRestoresItsLockRejectsOldRpcWithoutServerErrors(String type)
      throws Exception {
    VaadinSession original = new VaadinSession(null);
    ReflectionTestUtils.setField(original, "lock", new ReentrantLock());
    assertNotNull(original.getLockInstance());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(original);
    }
    VaadinSession restored;
    try (ObjectInputStream input =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      restored = (VaadinSession) input.readObject();
    }
    assertNull(restored.getLockInstance());
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
    request.setServletPath("/");
    request.setParameter("v-r", type);
    request.setParameter("v-uiId", "17");
    MockHttpSession http = new MockHttpSession();
    request.setSession(http);
    http.setAttribute("com.vaadin.flow.server.VaadinSession.restored", restored);
    for (int attempt = 0; attempt < 3; attempt++) {
      assertFalse(MaintenanceTransport.allowedUi(request));
      assertDoesNotThrow(() -> MaintenanceTransport.redirectUi(request));
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);
      assertDoesNotThrow(
          () -> new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain));
      verifyNoInteractions(chain);
      assertTrue(response.getContentType().startsWith("application/json"));
      assertTrue(response.getContentAsString().contains("/maintenance"));
      assertNotEquals(500, response.getStatus());
      assertSame(restored, http.getAttribute("com.vaadin.flow.server.VaadinSession.restored"));
      assertFalse(http.isInvalid());
    }
  }

  @Test
  void uninitializedSessionDoesNotPreventFindingAnAlreadyInitializedLoginUi() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
    request.setParameter("v-uiId", "17");
    MockHttpSession http = new MockHttpSession();
    request.setSession(http);
    http.setAttribute("com.vaadin.flow.server.VaadinSession.restored", new VaadinSession(null));
    VaadinSession initialized = mock(VaadinSession.class);
    when(initialized.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(initialized.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain())
        .thenReturn(List.of(mock(LoginView.class)));
    http.setAttribute("com.vaadin.flow.server.VaadinSession.initialized", initialized);
    assertTrue(MaintenanceTransport.allowedUi(request));
    verify(initialized).lock();
    verify(initialized).unlock();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void onlyTheActualLoginUiMayUseLoginRpc(boolean loginUi) throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
    request.setServletPath("/");
    request.setParameter("v-r", "uidl");
    request.setParameter("v-uiId", "17");
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    session.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    if (loginUi) {
      when(ui.getInternals().getActiveRouterTargetsChain())
          .thenReturn(List.of(mock(LoginView.class)));
    } else {
      when(ui.getInternals().getActiveRouterTargetsChain())
          .thenReturn(List.of(mock(RegistrationView.class)));
    }
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    if (loginUi) {
      verify(chain).doFilter(request, response);
    } else {
      verifyNoInteractions(chain);
    }
    verify(vaadin).lock();
    verify(vaadin).unlock();
  }

  @ParameterizedTest
  @CsvSource({
    "/h2/,init",
    "/h2/,heartbeat",
    "/h2/,uidl",
    "/h2/,push",
    "/HILLA/action,init",
    "/HILLA/action,heartbeat",
    "/HILLA/action,uidl",
    "/login,uidl",
    "/admin,init"
  })
  void protocolParametersDoNotReleaseOtherServletsEvenWithARealLoginUi(String path, String type)
      throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    request.setParameter("v-r", type);
    request.setParameter("v-uiId", "17");
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain())
        .thenReturn(List.of(mock(LoginView.class)));
    session.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    // Binding the fixture invokes HttpSessionBindingListener before the request under test.
    clearInvocations(vaadin);
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    verifyNoInteractions(chain, vaadin);
    if (type.equals("init") || type.equals("heartbeat")) {
      assertEquals(503, response.getStatus());
    } else {
      assertTrue(response.getContentAsString().contains("/maintenance"));
    }
  }

  @Test
  void realServerCreatedBlankUiCanCompleteItsRouteGuardedBootstrap() throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
    request.setServletPath("/");
    request.setParameter("v-r", "uidl");
    request.setParameter("v-uiId", "17");
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain()).thenReturn(List.of());
    session.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new MaintenanceHttpFilter(maintenance).doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
    verify(vaadin).lock();
    verify(vaadin).unlock();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/application"})
  void forwardedTokenDocumentKeepsBootstrapResourcesAtTheApplicationRoot(String contextPath) {
    VaadinService service = mock(VaadinService.class);
    ServiceInitEvent event = new ServiceInitEvent(service);
    new MaintenanceUiGuard(maintenance).serviceInit(event);
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", contextPath + "/maintenance");
    request.setContextPath(contextPath);
    VaadinServletRequest vaadinRequest = new VaadinServletRequest(request, null);
    Document document = Jsoup.parse("<html><head><base href='.'></head></html>");
    IndexHtmlResponse response = new IndexHtmlResponse(vaadinRequest, null, document);
    IndexHtmlRequestListener listener =
        event.getAddedIndexHtmlRequestListeners().findFirst().orElseThrow();
    listener.modifyIndexHtmlResponse(response);
    assertEquals(".", document.head().selectFirst("base").attr("href"));
    request.setAttribute(MaintenanceHttpFilter.FORWARDED_DOCUMENT, Boolean.TRUE);
    listener.modifyIndexHtmlResponse(response);
    assertEquals(contextPath + "/", document.head().selectFirst("base").attr("href"));
  }

  @Test
  void publicServiceGuardRunsBeforeAnyAccountOrQueueInteraction() throws Exception {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    UserService users = mock(UserService.class);
    VerificationService verification = mock(VerificationService.class);
    RegistrationService registration = new RegistrationService(users, verification, config);
    assertThrows(MaintenanceAccessException.class, () -> registration.register(new UserEntity()));
    verifyNoInteractions(users, verification);
  }
}
