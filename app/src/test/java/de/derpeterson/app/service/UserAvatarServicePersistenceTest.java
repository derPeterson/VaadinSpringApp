package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserAvatarRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AvatarImageProcessor.Crop;
import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.UserAvatarService.Change;
import de.derpeterson.app.service.UserAvatarService.Open;
import de.derpeterson.app.service.UserAvatarService.State;
import de.derpeterson.app.ui.components.AccountAvatarDialog;
import de.derpeterson.app.ui.components.AccountAvatarEditor;
import de.derpeterson.app.ui.components.AvatarImageResource;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserProfileBroadcaster.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringJUnitConfig(UserAvatarServicePersistenceTest.Config.class)
class UserAvatarServicePersistenceTest {
  @Configuration
  @Import({
    UserServicePersistenceTest.Config.class,
    UserAvatarService.class,
    AvatarImageProcessor.class,
    AvatarDraftStore.class
  })
  static class Config {
    @Bean
    ConfigService configService() {
      return mock(ConfigService.class);
    }
  }

  @Autowired UserAvatarService service;
  @Autowired SecurityService security;
  @Autowired UserAvatarRepository avatars;
  @Autowired UserRepository users;
  @Autowired ConfigService config;
  @Autowired UserProfileBroadcaster events;
  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;
  UserEntity user, other;
  final MockHttpServletRequest request = new MockHttpServletRequest();

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(config);
    tx.executeWithoutResult(
        status -> {
          avatars.deleteAll();
          users.deleteAll();
          users.flush();
          em.createQuery("delete from RoleEntity").executeUpdate();
          RoleEntity role = new RoleEntity();
          role.setName(RoleType.ROLE_USER);
          em.persist(role);
          user = newUser("avatar@example.test", role);
          other = newUser("other-avatar@example.test", role);
          em.persist(user);
          em.persist(other);
        });
    authenticate(user);
  }

  UserEntity newUser(String email, RoleEntity role) {
    return UserEntity.builder()
        .firstName("Avatar")
        .lastName("User")
        .email(email)
        .password("encoded-password")
        .gender(Gender.MALE)
        .birthDate(LocalDate.of(1990, 1, 1))
        .enabled(true)
        .preferredLocale(Locale.GERMAN)
        .roleEntities(new ArrayList<>(List.of(role)))
        .build();
  }

  void authenticate(UserEntity account) {
    UserDetails principal =
        new CustomUserDetailsService(users).loadUserByUsername(account.getEmail());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  State uploadAndSave() throws Exception {
    Open opened = service.open(request);
    UUID id =
        service.upload(
            request,
            opened.draft(),
            new ByteArrayInputStream(AvatarImageProcessorTest.image("png", 200, 100)));
    return service.save(
        request,
        new Change(
            opened.state().version(),
            AvatarMode.CUSTOM,
            false,
            opened.draft(),
            id,
            Crop.centered()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void rawUploadWithoutTheUiLockCanBeSavedAndDownloaded(boolean threadAuthentication)
      throws Exception {
    VaadinServletService servlet = mock(VaadinServletService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(servlet.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    VaadinSession session = new VaadinSession(servlet);
    ReflectionTestUtils.setField(session, "lock", new ReentrantLock());
    ReflectionTestUtils.setField(
        session, "sessionLockCheckStrategy", SessionLockCheckStrategy.THROW);
    UI ui = new UI();
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    AccountAvatarDialog dialog = null;
    try {
      session.lock();
      Upload uploader;
      UploadHandler handler;
      UUID token;
      try {
        session.setLocale(Locale.GERMAN);
        ui.getInternals().setSession(session);
        UserDetails principal =
            (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        request.getSession().setAttribute("authenticatedUser", principal);
        request
            .getSession()
            .setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        session.setAttribute(UserDetails.class, principal);
        dialog = new AccountAvatarDialog(new MessageProperties(new CustomI18NProvider()), user);
        AccountAvatarEditor editor =
            spy((AccountAvatarEditor) ReflectionTestUtils.getField(dialog, "editor"));
        ReflectionTestUtils.setField(dialog, "editor", editor);
        dialog.configure(service, request);
        ArgumentCaptor<UploadHandler> captured = ArgumentCaptor.forClass(UploadHandler.class);
        verify(editor).setUploadHandler(captured.capture());
        handler = captured.getValue();
        uploader = (Upload) ReflectionTestUtils.getField(editor, "uploader");
        ui.add(dialog);
        dialog.open();
        assertTrue(dialog.isOpened());
        token = (UUID) ReflectionTestUtils.getField(dialog, "draft");
      } finally {
        session.unlock();
      }
      if (!threadAuthentication) {
        SecurityContextHolder.clearContext();
      }
      assertFalse(session.hasLock());
      request.setMethod("POST");
      request.setContentType("image/jpeg");
      request.addHeader("X-Avatar-Draft", token.toString());
      request.setContent(AvatarImageProcessorTest.image("jpg", 1367, 1823));
      MockHttpServletResponse response = new MockHttpServletResponse();
      handler.handleRequest(
          new VaadinServletRequest(request, servlet),
          new VaadinServletResponse(response, servlet),
          session,
          uploader.getElement());
      assertEquals(200, response.getStatus(), response.getContentAsString());
      UUID imageId = UUID.fromString(response.getHeader("X-Avatar-Image"));
      session.lock();
      try {
        ReflectionTestUtils.invokeMethod(
            dialog,
            "save",
            new ObjectMapper()
                .readTree(
                    "{\"local\":true,\"loading\":false,\"imageId\":\""
                        + imageId
                        + "\",\"zoom\":1,\"x\":0,\"y\":0}"));
        assertFalse(dialog.isOpened());
      } finally {
        session.unlock();
      }
      State saved = service.state(user.getId());
      assertTrue(saved.hasImage());
      assertEquals(Locale.GERMAN, users.findById(user.getId()).orElseThrow().getPreferredLocale());
      DownloadHandler download =
          AvatarImageResource.handler(service, user.getId(), saved.imageRevision(), false);
      MockHttpServletResponse imageResponse = new MockHttpServletResponse();
      download.handleRequest(
          new VaadinServletRequest(request, servlet),
          new VaadinServletResponse(imageResponse, servlet),
          session,
          new Element("img"));
      assertEquals(200, imageResponse.getStatus());
      assertEquals("image/png", imageResponse.getContentType());
      try (ByteArrayInputStream downloadedImage =
          new ByteArrayInputStream(imageResponse.getContentAsByteArray())) {
        assertEquals(512, javax.imageio.ImageIO.read(downloadedImage).getWidth());
      }
      tx.executeWithoutResult(
          status -> users.findById(user.getId()).orElseThrow().setEnabled(false));
      SecurityContextHolder.clearContext();
      assertTrue(security.getAuthenticatedUser(request).isEmpty());
      MockHttpServletResponse denied = new MockHttpServletResponse();
      download.handleRequest(
          new VaadinServletRequest(request, servlet),
          new VaadinServletResponse(denied, servlet),
          session,
          new Element("img"));
      assertEquals(401, denied.getStatus());
      assertEquals(0, denied.getContentAsByteArray().length);
    } finally {
      session.lock();
      try {
        if (dialog != null) {
          dialog.close();
        }
        NotificationHelper.getInstance().closeAndClearAllNotifications();
        ui.removeAll();
      } finally {
        session.unlock();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
      }
    }
  }

  @Test
  void saveSwitchKeepReplaceDeleteAndDeleteAccountHaveNoImageHistory() throws Exception {
    Long profileVersion = users.findById(user.getId()).orElseThrow().getVersion();
    State saved = uploadAndSave();
    assertTrue(saved.hasImage());
    assertEquals(AvatarMode.CUSTOM, saved.mode());
    assertEquals(1, avatars.count());
    byte[] first = service.image(request, user.getId(), saved.imageRevision(), false).orElseThrow();
    assertEquals(512, javax.imageio.ImageIO.read(new ByteArrayInputStream(first)).getWidth());
    State initials =
        service.save(
            request, new Change(saved.version(), AvatarMode.INITIALS, false, null, null, null));
    assertTrue(initials.hasImage());
    assertEquals(saved.imageRevision(), initials.imageRevision());
    assertTrue(service.image(request, user.getId(), saved.imageRevision(), false).isEmpty());
    assertArrayEquals(
        first, service.image(request, user.getId(), saved.imageRevision(), true).orElseThrow());
    State custom =
        service.save(
            request, new Change(initials.version(), AvatarMode.CUSTOM, false, null, null, null));
    assertArrayEquals(
        first, service.image(request, user.getId(), custom.imageRevision(), false).orElseThrow());
    State replaced = uploadAndSave();
    assertNotEquals(saved.imageRevision(), replaced.imageRevision());
    assertEquals(1, avatars.count());
    assertTrue(service.image(request, user.getId(), saved.imageRevision(), false).isEmpty());
    State deleted =
        service.save(
            request, new Change(replaced.version(), AvatarMode.INITIALS, true, null, null, null));
    assertFalse(deleted.hasImage());
    assertNull(deleted.imageRevision());
    assertNull(avatars.findById(user.getId()).orElseThrow().getImage());
    assertEquals(profileVersion, users.findById(user.getId()).orElseThrow().getVersion());
    uploadAndSave();
    tx.executeWithoutResult(
        status -> {
          users.deleteById(user.getId());
          users.flush();
        });
    assertEquals(0, avatars.count());
  }

  @Test
  void staleDialogAndRollbackCannotReplaceANewerImageOrPublishPrematureEvents() throws Exception {
    ArrayList<Message> messages = new ArrayList<UserProfileBroadcaster.Message>();
    Registration registration = events.register(messages::add);
    try {
      Open old = service.open(request);
      State saved = uploadAndSave();
      assertEquals(1, messages.size());
      Change draftId =
          new Change(old.state().version(), AvatarMode.INITIALS, true, old.draft(), null, null);
      assertThrows(OptimisticLockingFailureException.class, () -> service.save(request, draftId));
      assertEquals(saved, service.state(user.getId()));
      assertEquals(1, messages.size());
      tx.executeWithoutResult(
          status -> {
            events.afterCommit(user);
            status.setRollbackOnly();
          });
      assertEquals(1, messages.size());
    } finally {
      registration.remove();
    }
  }

  @Test
  void cancelInvalidUploadAndMissingImageNeverWrite() throws Exception {
    Open opened = service.open(request);
    service.upload(
        request,
        opened.draft(),
        new ByteArrayInputStream(AvatarImageProcessorTest.image("png", 10, 10)));
    service.release(opened.draft());
    assertEquals(0, avatars.count());
    UUID draftId = opened.draft();
    ByteArrayInputStream imageInput = new ByteArrayInputStream(new byte[] {1});
    assertThrows(InvalidImage.class, () -> service.upload(request, draftId, imageInput));
    Change change = new Change(null, AvatarMode.CUSTOM, false, null, null, null);
    assertThrows(InvalidImage.class, () -> service.save(request, change));
    assertEquals(0, avatars.count());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void everyRoleCanSaveOwnImageAndOnlyAdministratorsCanReadAnotherImage(RoleType role)
      throws Exception {
    State target = uploadAndSave();
    tx.executeWithoutResult(
        status -> {
          RoleEntity r = new RoleEntity();
          r.setName(role);
          if (role == RoleType.ROLE_USER) {
            r = em.find(RoleEntity.class, user.getRoleEntities().iterator().next().getId());
          } else {
            em.persist(r);
          }
          UserEntity account = users.findById(other.getId()).orElseThrow();
          account.setRoleEntities(new ArrayList<>(List.of(r)));
        });
    authenticate(other);
    State own = uploadAndSave();
    assertTrue(own.hasImage());
    if (role == RoleType.ROLE_ADMIN || role == RoleType.ROLE_SUPERADMIN) {
      assertTrue(service.image(request, user.getId(), target.imageRevision(), false).isPresent());
    } else {
      Long targetUserId = user.getId();
      UUID targetImageRevision = target.imageRevision();
      assertThrows(
          AccessDeniedException.class,
          () -> service.image(request, targetUserId, targetImageRevision, false));
    }
  }

  @Test
  void anonymousDisabledAndMaintenanceRequestsCannotReadOrSave() throws Exception {
    State saved = uploadAndSave();
    SecurityContextHolder.clearContext();
    Long anonymousUserId = user.getId();
    UUID anonymousImageRevision = saved.imageRevision();
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> service.image(request, anonymousUserId, anonymousImageRevision, false));
    authenticate(user);
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(MaintenanceAccessException.class, () -> service.open(request));
    reset(config);
    tx.executeWithoutResult(status -> users.findById(user.getId()).orElseThrow().setEnabled(false));
    Long disabledUserId = user.getId();
    UUID disabledImageRevision = saved.imageRevision();
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> service.image(request, disabledUserId, disabledImageRevision, false));
  }

  @Test
  void repositoryDefaultMethodRetainsTheIndependentSecurityRead() {
    tx.executeWithoutResult(
        status -> {
          UserEntity pending = users.findById(user.getId()).orElseThrow();
          pending.setEnabled(false);
          em.flush();

          assertTrue(users.findSecurityUser(user.getEmail()).getFirst().isEnabled());

          status.setRollbackOnly();
        });
    assertTrue(users.findById(user.getId()).orElseThrow().isEnabled());
  }

  @Test
  void singleMetadataReadIsIndependentOfPendingOuterChanges() throws Exception {
    State committed = uploadAndSave();
    tx.executeWithoutResult(
        status -> {
          de.derpeterson.app.model.UserAvatarEntity pending =
              avatars.findById(user.getId()).orElseThrow();
          pending.setMode(AvatarMode.INITIALS);
          em.flush();

          assertEquals(committed, service.state(user.getId()));
          assertEquals(committed, service.states(List.of(user.getId())).get(user.getId()));

          status.setRollbackOnly();
        });
    assertEquals(committed, service.state(user.getId()));
  }

  @Test
  void simultaneousFirstSavesSerializeAndOneStaleDialogLosesWithoutOverwriting() throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Boolean> save =
          () -> {
            authenticate(user);
            ready.countDown();
            try {
              if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test synchronization timeout");
              }
              service.save(
                  new MockHttpServletRequest(),
                  new Change(null, AvatarMode.INITIALS, false, null, null, null));
              return true;
            } catch (OptimisticLockingFailureException _) {
              return false;
            } finally {
              SecurityContextHolder.clearContext();
            }
          };
      Future<Boolean> first = executor.submit(save);
      Future<Boolean> second = executor.submit(save);
      try {
        assertTrue(ready.await(5, TimeUnit.SECONDS));
      } finally {
        start.countDown();
      }
      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
      assertEquals(1, avatars.count());
    }
  }
}
