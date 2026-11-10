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
  private final ConfigService configService = mock(ConfigService.class);
  private final MaintenanceService maintenance = new MaintenanceService(configService);

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
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpSession mockHttpSession = new MockHttpSession();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(actor(role));
    mockHttpSession.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    mockHttpRequest.setSession(mockHttpSession);

    assertFalse(maintenance.blocks(mockHttpRequest));
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);

    assertEquals(!role.isAdministrative(), maintenance.blocks(mockHttpRequest));
    assertFalse(mockHttpSession.isInvalid());
    assertSame(
        context,
        mockHttpSession.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);

    assertFalse(maintenance.blocks(mockHttpRequest));
  }

  @Test
  void requestSessionIdentityWinsOverAnUnrelatedPushWorkerContext() {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpRequest.setSession(mockHttpSession);
    SecurityContextHolder.getContext().setAuthentication(actor(RoleType.ROLE_ADMIN));

    assertNull(MaintenanceService.authentication(mockHttpRequest));
    assertTrue(maintenance.blocks(mockHttpRequest));
    assertTrue(maintenance.blocks(new MockHttpServletRequest()));
    SecurityContext stored = SecurityContextHolder.createEmptyContext();
    Authentication user = actor(RoleType.ROLE_USER);
    stored.setAuthentication(user);
    mockHttpSession.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, stored);
    SecurityContextHolder.getContext().setAuthentication(actor(RoleType.ROLE_ADMIN));

    assertSame(user, MaintenanceService.authentication(mockHttpRequest));
    assertTrue(maintenance.blocks(mockHttpRequest));
    stored.setAuthentication(null);

    assertNull(MaintenanceService.authentication(mockHttpRequest));
    assertTrue(maintenance.blocks(mockHttpRequest));
    Authentication admin = actor(RoleType.ROLE_SUPERADMIN);
    stored.setAuthentication(admin);
    SecurityContextHolder.getContext().setAuthentication(user);

    assertSame(admin, MaintenanceService.authentication(mockHttpRequest));
    assertFalse(maintenance.blocks(mockHttpRequest));
  }

  @Test
  void anonymousAndUnreadableConfigurationFailClosed() {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);

    assertTrue(maintenance.blocks(mockHttpRequest));
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalArgumentException("invalid boolean"));

    assertEquals(MaintenanceService.State.UNAVAILABLE, maintenance.state());
    assertTrue(maintenance.blocks(mockHttpRequest));
    Authentication attemptAuthentication = actor(RoleType.ROLE_SUPERADMIN);

    assertThrows(
        AuthenticationServiceException.class,
        () -> maintenance.requireLogin(attemptAuthentication));
    assertThrows(
        MaintenanceAccessException.class,
        () -> MaintenanceService.requirePublicAccess(configService));
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void newAdmissionIsCheckedBeforeSessionAndRegistryAllocation(RoleType role) {
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(3);
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    SessionRegistryImpl registry = new SessionRegistryImpl();
    SessionAdmissionService admission = new SessionAdmissionService(configService, registry);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    if (role.isAdministrative()) {
      admission.onAuthentication(actor(role), mockHttpRequest, mockHttpResponse);

      assertEquals(1, registry.getAllSessions("maintenance@example.test", true).size());
    } else {
      Authentication attemptAuthentication = actor(role);

      assertThrows(
          MaintenanceAuthenticationException.class,
          () ->
              admission.onAuthentication(attemptAuthentication, mockHttpRequest, mockHttpResponse));
      assertNull(mockHttpRequest.getSession(false));
      assertTrue(registry.getAllPrincipals().isEmpty());
      assertEquals(
          "maintenance", mockHttpRequest.getAttribute(SessionAdmissionService.FAILURE_REASON));
    }
  }

  @Test
  void existingSlotCanBeRestoredDuringMaintenanceAndReusedAfterwards() {
    when(configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER)).thenReturn(1);
    SessionRegistryImpl registry = new SessionRegistryImpl();
    SessionAdmissionService admission = new SessionAdmissionService(configService, registry);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    Authentication user = actor(RoleType.ROLE_USER);
    admission.onAuthentication(user, mockHttpRequest, mockHttpResponse);
    HttpSession httpSession = mockHttpRequest.getSession(false);
    String id = httpSession.getId();
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    admission.complete(user, mockHttpRequest, mockHttpResponse, () -> {});

    assertEquals(id, httpSession.getId());
    assertEquals(1, registry.getAllSessions(user.getName(), true).size());
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    admission.onAuthentication(user, mockHttpRequest, mockHttpResponse);

    assertSame(httpSession, mockHttpRequest.getSession(false));
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
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("GET", path);
    mockHttpRequest.setServletPath(path);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);

    assertEquals(503, mockHttpResponse.getStatus());
    assertEquals("/maintenance", mockHttpResponse.getForwardedUrl());
    assertFalse(mockHttpResponse.getContentAsString().contains("secret-token"));
    assertNull(mockHttpResponse.getHeader("Retry-After"));
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
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("GET", path);
    mockHttpRequest.setServletPath(path);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);

    verify(chain).doFilter(mockHttpRequest, mockHttpResponse);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/home", "/login", "/VAADIN/uidl"})
  void oldOrSpoofedUiCannotDispatchCallbacksThroughPublicLoginPath(String path) throws Exception {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", path);
    mockHttpRequest.setServletPath(path);
    mockHttpRequest.setParameter("v-r", "uidl");
    mockHttpRequest.setParameter("v-uiId", "123");
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);

    verifyNoInteractions(chain);
    assertTrue(mockHttpResponse.getContentType().startsWith("application/json"));
    assertTrue(mockHttpResponse.getContentAsString().contains("/maintenance"));
    assertFalse(mockHttpResponse.getContentAsString().contains("<html"));
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
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/");
    mockHttpRequest.setServletPath("/");
    mockHttpRequest.setParameter("v-r", type);
    mockHttpRequest.setParameter("v-uiId", "17");
    MockHttpSession http = new MockHttpSession();
    mockHttpRequest.setSession(http);
    http.setAttribute("com.vaadin.flow.server.VaadinSession.restored", restored);
    for (int attempt = 0; attempt < 3; attempt++) {
      assertFalse(MaintenanceTransport.allowedUi(mockHttpRequest));
      assertDoesNotThrow(() -> MaintenanceTransport.redirectUi(mockHttpRequest));
      MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);

      assertDoesNotThrow(() ->
          new MaintenanceHttpFilter(maintenance)
              .doFilter(mockHttpRequest, mockHttpResponse, chain));
      verifyNoInteractions(chain);
      assertTrue(mockHttpResponse.getContentType().startsWith("application/json"));
      assertTrue(mockHttpResponse.getContentAsString().contains("/maintenance"));
      assertNotEquals(500, mockHttpResponse.getStatus());
      assertSame(restored, http.getAttribute("com.vaadin.flow.server.VaadinSession.restored"));
      assertFalse(http.isInvalid());
    }
  }

  @Test
  void uninitializedSessionDoesNotPreventFindingAnAlreadyInitializedLoginUi() {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/");
    mockHttpRequest.setParameter("v-uiId", "17");
    MockHttpSession http = new MockHttpSession();
    mockHttpRequest.setSession(http);
    http.setAttribute("com.vaadin.flow.server.VaadinSession.restored", new VaadinSession(null));
    VaadinSession initialized = mock(VaadinSession.class);
    when(initialized.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(initialized.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain())
        .thenReturn(List.of(mock(LoginView.class)));
    http.setAttribute("com.vaadin.flow.server.VaadinSession.initialized", initialized);

    assertTrue(MaintenanceTransport.allowedUi(mockHttpRequest));
    verify(initialized).lock();
    verify(initialized).unlock();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void onlyTheActualLoginUiMayUseLoginRpc(boolean loginUi) throws Exception {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/");
    mockHttpRequest.setServletPath("/");
    mockHttpRequest.setParameter("v-r", "uidl");
    mockHttpRequest.setParameter("v-uiId", "17");
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpRequest.setSession(mockHttpSession);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    mockHttpSession.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    if (loginUi) {
      when(ui.getInternals().getActiveRouterTargetsChain())
          .thenReturn(List.of(mock(LoginView.class)));
    } else {
      when(ui.getInternals().getActiveRouterTargetsChain())
          .thenReturn(List.of(mock(RegistrationView.class)));
    }
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);
    if (loginUi) {
      verify(chain).doFilter(mockHttpRequest, mockHttpResponse);
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
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", path);
    mockHttpRequest.setServletPath(path);
    mockHttpRequest.setParameter("v-r", type);
    mockHttpRequest.setParameter("v-uiId", "17");
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpRequest.setSession(mockHttpSession);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain())
        .thenReturn(List.of(mock(LoginView.class)));
    mockHttpSession.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    // Binding the fixture invokes HttpSessionBindingListener before the request under test.
    clearInvocations(vaadin);
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);

    verifyNoInteractions(chain, vaadin);
    if (type.equals("init") || type.equals("heartbeat")) {
      assertEquals(503, mockHttpResponse.getStatus());
    } else {
      assertTrue(mockHttpResponse.getContentAsString().contains("/maintenance"));
    }
  }

  @Test
  void realServerCreatedBlankUiCanCompleteItsRouteGuardedBootstrap() throws Exception {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/");
    mockHttpRequest.setServletPath("/");
    mockHttpRequest.setParameter("v-r", "uidl");
    mockHttpRequest.setParameter("v-uiId", "17");
    MockHttpSession mockHttpSession = new MockHttpSession();
    mockHttpRequest.setSession(mockHttpSession);
    VaadinSession vaadin = mock(VaadinSession.class);
    when(vaadin.getLockInstance()).thenReturn(new ReentrantLock());
    UI ui = mock(UI.class, RETURNS_DEEP_STUBS);
    when(vaadin.getUIById(17)).thenReturn(ui);
    when(ui.getInternals().getActiveRouterTargetsChain()).thenReturn(List.of());
    mockHttpSession.setAttribute("com.vaadin.flow.server.VaadinSession.test", vaadin);
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    new MaintenanceHttpFilter(maintenance).doFilter(mockHttpRequest, mockHttpResponse, chain);

    verify(chain).doFilter(mockHttpRequest, mockHttpResponse);
    verify(vaadin).lock();
    verify(vaadin).unlock();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/application"})
  void forwardedTokenDocumentKeepsBootstrapResourcesAtTheApplicationRoot(String contextPath) {
    VaadinService vaadinService = mock(VaadinService.class);
    ServiceInitEvent event = new ServiceInitEvent(vaadinService);
    new MaintenanceUiGuard(maintenance).serviceInit(event);
    MockHttpServletRequest mockHttpRequest =
        new MockHttpServletRequest("GET", contextPath + "/maintenance");
    mockHttpRequest.setContextPath(contextPath);
    VaadinServletRequest vaadinRequest = new VaadinServletRequest(mockHttpRequest, null);
    Document document = Jsoup.parse("<html><head><base href='.'></head></html>");
    IndexHtmlResponse response = new IndexHtmlResponse(vaadinRequest, null, document);
    IndexHtmlRequestListener listener =
        event.getAddedIndexHtmlRequestListeners().findFirst().orElseThrow();
    listener.modifyIndexHtmlResponse(response);

    assertEquals(".", document.head().selectFirst("base").attr("href"));
    mockHttpRequest.setAttribute(MaintenanceHttpFilter.FORWARDED_DOCUMENT, Boolean.TRUE);
    listener.modifyIndexHtmlResponse(response);

    assertEquals(contextPath + "/", document.head().selectFirst("base").attr("href"));
  }

  @Test
  void publicServiceGuardRunsBeforeAnyAccountOrQueueInteraction() {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    UserService users = mock(UserService.class);
    VerificationService verification = mock(VerificationService.class);
    RegistrationService registration = new RegistrationService(users, verification, configService);
    UserEntity targetUser = new UserEntity();

    assertThrows(MaintenanceAccessException.class, () -> registration.register(targetUser));
    verifyNoInteractions(users, verification);
  }
}
