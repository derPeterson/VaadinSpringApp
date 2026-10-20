package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushHandler;
import com.vaadin.flow.server.communication.ServerRpcHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.communication.PushConstants;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.ForwardingRequestWrapper;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.PushSecurityInterceptor;
import de.derpeterson.app.security.SecurityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/** Actual PushHandler -> ServerRpcHandler -> DOM callback, without a socket/server. */
@SpringJUnitConfig(SecurityChainRegressionTest.Config.class)
@WebAppConfiguration
class PushRpcSecurityPersistenceTest {
  @Autowired SecurityFilterChain httpChain;
  @Autowired UserService users;
  @Autowired UserRepository repository;
  @Autowired SecurityService security;
  @Autowired TransactionTemplate transaction;
  @PersistenceContext EntityManager entityManager;
  private Long actorId;
  private Long targetId;
  private RoleEntity ordinaryRole;
  private UI ui;
  private Button action;
  private VaadinSession vaadin;
  private VaadinServletService service;
  private AtmosphereRequest request;
  private AtmosphereResource resource;
  private MockHttpSession httpSession;
  private PushHandler handler;
  private Authentication authentication;
  private final ConfigService maintenanceConfig = mock(ConfigService.class);
  private final AtomicInteger calls = new AtomicInteger();

  @BeforeEach
  void setup() throws Exception {
    transaction.executeWithoutResult(
        tx -> {
          repository.deleteAll();
          repository.flush();
          entityManager.createQuery("delete from RoleEntity").executeUpdate();
          RoleEntity admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
          ordinaryRole = RoleEntity.builder().name(RoleType.ROLE_USER).build();
          entityManager.persist(admin);
          entityManager.persist(ordinaryRole);
          actorId = create("push-admin@example.com", admin);
          create("surviving-admin@example.com", admin);
          targetId = create("push-target@example.com", ordinaryRole);
        });
    service = mock(VaadinServletService.class);
    DeploymentConfiguration deployment = mock(DeploymentConfiguration.class);
    when(deployment.isXsrfProtectionEnabled()).thenReturn(true);
    when(deployment.isSyncIdCheckEnabled()).thenReturn(true);
    when(deployment.getMaxRequestBodySize()).thenReturn(1024L * 1024);
    when(service.getDeploymentConfiguration()).thenReturn(deployment);
    when(service.ensurePushAvailable()).thenReturn(true);
    when(service.getEventBus()).thenReturn(new VaadinServiceEventBus(service));
    // Use the actual registered interceptor collection and real requestStart
    // method. Only servlet/session lookup and outgoing network I/O are mocked.
    ServiceInitEvent init = new ServiceInitEvent(service);
    new PushSecurityInterceptor(security, new MaintenanceService(maintenanceConfig))
        .serviceInit(init);
    assertEquals(1, init.getAddedVaadinRequestInterceptor().count());
    ReflectionTestUtils.setField(service, "initialized", true);
    ReflectionTestUtils.setField(
        service, "vaadinRequestInterceptors", init.getAddedVaadinRequestInterceptor().toList());
    doCallRealMethod().when(service).requestStart(any(), any());
    doCallRealMethod().when(service).setCurrentInstances(any(), any());
    vaadin = mock(VaadinSession.class);
    when(vaadin.getService()).thenReturn(service);
    when(vaadin.hasLock()).thenReturn(true);
    when(vaadin.getLockInstance()).thenReturn(new java.util.concurrent.locks.ReentrantLock());
    when(vaadin.access(any()))
        .thenAnswer(
            call -> {
              ((com.vaadin.flow.server.Command) call.getArgument(0)).execute();
              return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
    when(vaadin.getErrorHandler())
        .thenReturn(
            event -> {
              throw new AssertionError(event.getThrowable());
            });
    when(service.findVaadinSession(any()))
        .thenAnswer(
            call -> {
              VaadinSession.setCurrent(vaadin);
              return vaadin;
            });
    ui = new UI();
    ui.getInternals()
        .setRouterTargetChain(java.util.List.of(mock(de.derpeterson.app.views.HomeView.class)));
    ui.getInternals().setSession(vaadin);
    when(service.findUI(any()))
        .thenAnswer(
            call -> {
              UI.setCurrent(ui);
              return ui;
            });
    action =
        new Button(
            "Protected mutation",
            event -> {
              int count = calls.incrementAndGet();
              UserEntity target = form(targetId);
              target.setFirstName("Mutation " + count);
              users.updateAdminUser(target, target.getVersion(), "");
            });
    ui.add(action);
    ui.getInternals().getStateTree().collectChanges(change -> {});
    AtmospherePushConnection connection = mock(AtmospherePushConnection.class);
    when(connection.getOrCreateFragmentedMessage(any()))
        .thenAnswer(call -> new AtmospherePushConnection.FragmentedMessage());
    ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
    ui.getInternals().setPushConnection(connection);
    request = mock(AtmosphereRequest.class);
    httpSession = new MockHttpSession();
    httpSession.setAttribute("test.vaadin", vaadin);
    when(vaadin.getUIById(0)).thenReturn(ui);
    when(request.getParameter("v-uiId")).thenReturn("0");
    when(request.getSession(false)).thenReturn(httpSession);
    resource = mock(AtmosphereResource.class);
    when(resource.getRequest()).thenReturn(request);
    when(resource.transport()).thenReturn(AtmosphereResource.TRANSPORT.WEBSOCKET);
    when(resource.uuid()).thenReturn("existing-connection");
    handler = new PushHandler(service);
    authenticate(actorId, false, true);
    SecurityContextHolder.clearContext(); // WebSocket worker has no HTTP filter context.
  }

  private Long create(String email, RoleEntity role) {
    UserEntity user =
        UserEntity.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("hash")
            .enabled(true)
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>(List.of(role)))
            .build();
    entityManager.persist(user);
    return user.getId();
  }

  private UserEntity form(Long id) {
    return repository.findAll().stream()
        .filter(user -> user.getId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private void authenticate(Long id, boolean remember, boolean customPrincipal) {
    UserDetails principal =
        new CustomUserDetailsService(repository).loadUserByUsername(form(id).getEmail());
    authentication =
        remember
            ? new RememberMeAuthenticationToken("test", principal, principal.getAuthorities())
            : UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    httpSession.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    if (customPrincipal) {
      httpSession.setAttribute("authenticatedUser", principal);
    } else {
      httpSession.removeAttribute("authenticatedUser");
    }
  }

  private String rpcMessage(String token) {
    return "{\""
        + ApplicationConstants.CSRF_TOKEN
        + "\":\""
        + token
        + "\",\""
        + ApplicationConstants.SERVER_SYNC_ID
        + "\":0,\""
        + ApplicationConstants.CLIENT_TO_SERVER_ID
        + "\":"
        + (ui.getInternals().getLastProcessedClientToServerId() + 1)
        + ",\"rpc\":[{\"type\":\"event\",\"node\":"
        + action.getElement().getNode().getId()
        + ",\"event\":\"click\",\"data\":{}}]}";
  }

  private void receive(String token) throws Exception {
    String message = rpcMessage(token);
    when(request.getReader())
        .thenReturn(
            new BufferedReader(
                new StringReader(
                    message.length() + "" + PushConstants.MESSAGE_DELIMITER + message)));
    Method onMessage = PushHandler.class.getDeclaredMethod("onMessage", AtmosphereResource.class);
    onMessage.setAccessible(true);
    try {
      onMessage.invoke(handler, resource);
    } catch (InvocationTargetException exception) {
      if (exception.getCause() instanceof Exception cause) {
        throw cause;
      }
      throw (Error) exception.getCause();
    }
  }

  private void change(String kind) {
    UserEntity actor = form(actorId);
    if (kind.equals("delete")) {
      users.deleteUser(actor);
    } else if (kind.equals("disable")) {
      actor.setEnabled(false);
      users.save(actor);
    } else {
      actor.setRoleEntities(new ArrayList<>(List.of(ordinaryRole)));
      users.updateAdminUser(actor, actor.getVersion(), "");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "role",
        "disable",
        "delete",
        "remember",
        "standard-context-only",
        "custom-principal-only"
      })
  void existingSocketCannotExecuteValidRpcAfterCommittedRevocation(String kind) throws Exception {
    authenticate(actorId, kind.equals("remember"), !kind.equals("standard-context-only"));
    if (kind.equals("custom-principal-only")) {
      httpSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }
    receive(ui.getCsrfToken()); // Same resource/UI/connection is usable before revocation.
    assertEquals(1, calls.get());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    change(kind);
    assertTrue(VaadinService.isCsrfTokenValid(ui, ui.getCsrfToken()));
    assertThrows(AccessDeniedException.class, () -> receive(ui.getCsrfToken()));
    assertEquals(1, calls.get());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    assertEquals(kind.equals("disable") || kind.equals("delete"), httpSession.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertNull(VaadinService.getCurrentRequest());
    assertNull(UI.getCurrent());
  }

  @ParameterizedTest
  @ValueSource(strings = {"role", "disable", "delete"})
  void rollbackRetainsExistingPushCallbacks(String kind) throws Exception {
    transaction.executeWithoutResult(
        tx -> {
          change(kind);
          tx.setRollbackOnly();
        });
    receive(ui.getCsrfToken());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    assertFalse(httpSession.isInvalid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unchangedAndRememberMePrincipalsContinueThroughRepeatedMessages(boolean remember)
      throws Exception {
    authenticate(actorId, remember, true);
    receive(ui.getCsrfToken());
    users.updateLastActivity(actorId);
    UserEntity actor = form(actorId);
    actor.setFirstName("Profile edit");
    users.updateAdminUser(actor, actor.getVersion(), "");
    receive(ui.getCsrfToken());
    assertEquals("Mutation 2", form(targetId).getFirstName());
    assertSame(
        authentication,
        ((SecurityContext)
                httpSession.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
            .getAuthentication());
    assertFalse(httpSession.isInvalid());
  }

  @Test
  void ordinaryUserRpcStillRunsAndBackgroundMutationNeedsNoRequest() throws Exception {
    authenticate(targetId, false, true);
    receive(ui.getCsrfToken());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    CurrentInstance.clearAll();
    SecurityContextHolder.clearContext();
    users.updateLastActivity(targetId);
    assertNotNull(form(targetId).getLastActivity());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void maintenanceStopsActualExistingSocketBeforeRpcAndPreservesItsSession(boolean remember)
      throws Exception {
    authenticate(targetId, remember, true);
    receive(ui.getCsrfToken());
    when(maintenanceConfig.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(MaintenanceAccessException.class, () -> receive(ui.getCsrfToken()));
    assertEquals(1, calls.get());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    assertFalse(httpSession.isInvalid());
    assertNull(VaadinService.getCurrentRequest());
    when(maintenanceConfig.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    receive(ui.getCsrfToken());
    assertEquals(2, calls.get());
  }

  @Test
  void maintenanceKeepsAdministrativeSocketAndBackgroundJobsUsable() throws Exception {
    when(maintenanceConfig.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    receive(ui.getCsrfToken());
    assertEquals(1, calls.get());
    CurrentInstance.clearAll();
    SecurityContextHolder.clearContext();
    users.updateLastActivity(targetId);
    assertNotNull(form(targetId).getLastActivity());
  }

  @Test
  void maintenanceDoesNotRejectOrdinaryHttpBootstrapBeforeItsRouteGuard() {
    authenticate(targetId, false, true);
    when(maintenanceConfig.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    PushSecurityInterceptor interceptor =
        new PushSecurityInterceptor(security, new MaintenanceService(maintenanceConfig));
    assertDoesNotThrow(
        () ->
            interceptor.requestStart(
                new VaadinServletRequest(request, service), mock(VaadinResponse.class)));
    assertFalse(httpSession.isInvalid());
    assertEquals(0, calls.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void actualRootUidlPassesHttpFiltersButStillRequiresFlowToken(boolean valid) throws Exception {
    MockHttpServletRequest http = new MockHttpServletRequest("POST", "/");
    http.setServletPath("/");
    http.setParameter("v-r", "uidl");
    http.setSession(httpSession);
    MockHttpServletResponse response = new MockHttpServletResponse();
    jakarta.servlet.FilterChain servlet =
        (incoming, outgoing) -> {
          VaadinServletRequest wrapped =
              new VaadinServletRequest((jakarta.servlet.http.HttpServletRequest) incoming, service);
          service.setCurrentInstances(wrapped, mock(VaadinResponse.class));
          VaadinSession.setCurrent(vaadin);
          UI.setCurrent(ui);
          try {
            new ServerRpcHandler()
                .handleRpc(ui, rpcMessage(valid ? ui.getCsrfToken() : "invalid"), wrapped);
          } catch (Exception failure) {
            throw new ServletException(failure);
          } finally {
            CurrentInstance.clearAll();
          }
        };
    if (valid) {
      new FilterChainProxy(httpChain).doFilter(http, response, servlet);
      assertEquals(200, response.getStatus());
      assertEquals(1, calls.get());
      assertEquals("Mutation 1", form(targetId).getFirstName());
    } else {
      ServletException failure =
          assertThrows(
              ServletException.class,
              () -> new FilterChainProxy(httpChain).doFilter(http, response, servlet));
      assertTrue(failure.getCause() instanceof ServerRpcHandler.InvalidUIDLSecurityKeyException);
      assertEquals(0, calls.get());
      assertEquals("Test", form(targetId).getFirstName());
    }
  }

  @Test
  void actualPushUrlPassesHttpFiltersAndExecutesProtectedRpc() throws Exception {
    MockHttpServletRequest http = new MockHttpServletRequest("POST", "/VAADIN/push");
    http.setServletPath("/VAADIN/push");
    http.setParameter("v-r", "push");
    http.setSession(httpSession);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new FilterChainProxy(httpChain)
        .doFilter(
            http,
            response,
            (incoming, outgoing) -> {
              try {
                receive(ui.getCsrfToken());
              } catch (Exception failure) {
                throw new ServletException(failure);
              }
            });
    assertEquals(200, response.getStatus());
    assertEquals(1, calls.get());
    assertEquals("Mutation 1", form(targetId).getFirstName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"valid", "unknown-key", "disabled", "hidden"})
  void actualDynamicUploadTransportRetainsResourceKeyAndElementGuards(String mode)
      throws Exception {
    StreamResourceRegistry resources = new StreamResourceRegistry(vaadin);
    when(vaadin.getResourceRegistry()).thenReturn(resources);
    UI.setCurrent(ui);
    UploadHandler upload =
        new UploadHandler() {
          @Override
          public void handleUploadRequest(UploadEvent event) {
            throw new AssertionError("Multipart processing is outside this transport-guard test");
          }

          @Override
          public void handleRequest(
              com.vaadin.flow.server.VaadinRequest request,
              VaadinResponse response,
              VaadinSession session,
              Element owner) {
            // Only the file-I/O boundary is replaced; StreamRequestHandler performs its real
            // checks.
            calls.incrementAndGet();
          }
        };
    StreamRegistration registration = resources.registerResource(upload, action.getElement());
    String path = "/" + registration.getResourceUri();
    if (mode.equals("unknown-key")) {
      String[] parts = path.split("/", 7);
      path = path.replace(parts[5], "unknown-key");
    } else if (mode.equals("disabled")) {
      action.setEnabled(false);
    } else if (mode.equals("hidden")) {
      action.setVisible(false);
    }
    MockHttpServletRequest http = new MockHttpServletRequest("POST", path);
    http.setServletPath(path);
    http.setSession(httpSession);
    MockHttpServletResponse response = new MockHttpServletResponse();
    try {
      new FilterChainProxy(httpChain)
          .doFilter(
              http,
              response,
              (incoming, outgoing) -> {
                VaadinServletRequest wrapped =
                    new VaadinServletRequest(
                        new ForwardingRequestWrapper(
                            (jakarta.servlet.http.HttpServletRequest) incoming),
                        service);
                assertTrue(
                    new StreamRequestHandler()
                        .handleRequest(
                            vaadin,
                            wrapped,
                            new VaadinServletResponse(
                                (jakarta.servlet.http.HttpServletResponse) outgoing, service)));
              });
      int expectedStatus = 403;
      if (mode.equals("valid")) {
        expectedStatus = 200;
      } else if (mode.equals("unknown-key")) {
        expectedStatus = 404;
      }
      assertEquals(expectedStatus, response.getStatus());
      assertEquals(mode.equals("valid") ? 1 : 0, calls.get());
    } finally {
      registration.unregister();
      CurrentInstance.clearAll();
    }
  }

  @Test
  void csrfProtectionIsActuallyEnabled() {
    assertFalse(VaadinService.isCsrfTokenValid(ui, "invalid"));
    assertThrows(
        ServerRpcHandler.InvalidUIDLSecurityKeyException.class,
        () ->
            new ServerRpcHandler()
                .handleRpc(
                    ui,
                    "{\"csrfToken\":\"invalid\",\"syncId\":0,\"clientId\":0,\"rpc\":[]}",
                    new VaadinServletRequest(request, service)));
    assertEquals(0, calls.get());
  }

  @Test
  void pendingRoleChangesDoNotRevokeCommittedSessionAndRollbackRetainsRpc() throws Exception {
    transaction.executeWithoutResult(
        tx -> {
          change("role");
          assertTrue(security.hasCurrentSessionPrivileges(request));
          tx.setRollbackOnly();
        });
    receive(ui.getCsrfToken());
    assertEquals("Mutation 1", form(targetId).getFirstName());
    assertFalse(httpSession.isInvalid());
  }

  @Test
  void publicRequestDoesNotRequireOrCreateAnAuthenticatedSession() {
    MockHttpServletRequest publicRequest = new MockHttpServletRequest("GET", "/home");
    assertDoesNotThrow(
        () -> service.requestStart(new VaadinServletRequest(publicRequest, service), null));
    assertNull(publicRequest.getSession(false));
  }

  @AfterEach
  void cleanup() {
    CurrentInstance.clearAll();
    SecurityContextHolder.clearContext();
    ui = null;
    vaadin = null;
  }
}
