package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.UserNotificationEntity;
import de.derpeterson.app.model.UserNotificationRecipientEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.scheduler.UserNotificationCleanup;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SessionAdmissionService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.service.UserNotificationService.Preview;
import de.derpeterson.app.service.UserNotificationService.Problem;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.service.UserNotificationService.Rejected;
import de.derpeterson.app.service.UserNotificationService.Selection;
import de.derpeterson.app.service.UserNotificationService.Sent;
import de.derpeterson.app.service.UserNotificationService.Target;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** Eigene H2-Datenbank, echte Rollenprüfung/Transaktionen/FKs, kein Server und kein SMTP. */
@SpringJUnitConfig(UserNotificationServicePersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserNotificationServicePersistenceTest {
  @Configuration
  @Import({
    UserServicePersistenceTest.Config.class,
    UserNotificationService.class,
    UserNotificationBroadcaster.class,
    UserNotificationCleanup.class
  })
  static class Config {
    @Bean
    TwoFactorAuthorization twoFactorAuthorization() {
      return mock(TwoFactorAuthorization.class);
    }

    @Bean
    ConfigService config() {
      return mock(ConfigService.class);
    }

    @Bean
    UserNotificationPolicy policy(ConfigService configService) {
      return new UserNotificationPolicy(configService);
    }

    @Bean
    SessionRegistryImpl registry() {
      return new SessionRegistryImpl();
    }

    @Bean
    SessionAdmissionService admissions(
        ConfigService configService, SessionRegistryImpl sessionRegistry) {
      return new SessionAdmissionService(configService, sessionRegistry);
    }
  }

  @Autowired UserNotificationService userNotificationService;
  @Autowired UserNotificationRepository userNotificationRepository;
  @Autowired UserNotificationRecipientRepository userNotificationRecipientRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserNotificationBroadcaster userNotificationBroadcaster;
  @Autowired UserNotificationCleanup cleanup;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired SessionRegistryImpl sessionRegistry;
  @Autowired ConfigService configService;
  @Autowired TwoFactorAuthorization twoFactor;
  @PersistenceContext EntityManager entityManager;
  UserEntity sender, ordinary, guest, disabled, pending;
  MockHttpSession adminSession, userSession;
  final List<com.vaadin.flow.shared.Registration> subscriptions = new ArrayList<>();

  @BeforeEach
  void seed() {
    reset(configService, twoFactor);
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("180");
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn("365");
    transactionTemplate.executeWithoutResult(s -> {
      userNotificationRecipientRepository.deleteAll();
      userNotificationRepository.deleteAll();
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      for (RoleType role : RoleType.values()) {
        entityManager.persist(RoleEntity.builder().name(role).build());
      }
      sender = create("sender", RoleType.ROLE_SUPERADMIN, true);
      ordinary = create("ordinary", RoleType.ROLE_USER, true);
      guest = create("guest", RoleType.ROLE_GUEST, true);
      disabled = create("disabled", RoleType.ROLE_USER, false);
      disabled.setEnabled(false);
      userRepository.save(disabled);
      pending = create("pending", RoleType.ROLE_USER, false);
    });
    adminSession = session(sender);
    userSession = session(ordinary);
  }

  @AfterEach
  void release() {
    subscriptions.forEach(com.vaadin.flow.shared.Registration::remove);
    subscriptions.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void notificationReadsAndChangesStopWhenTheSessionFactorExpires() {
    assertDoesNotThrow(() -> userNotificationService.unreadCount(userSession));
    doThrow(new AccessDeniedException("Second factor expired"))
        .when(twoFactor)
        .requireSession(userSession);
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.unreadCount(userSession));
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 5));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.ownerId(userSession));
    List<Long> notificationIds = List.of(1L);
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.markRead(userSession, notificationIds));
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.dismiss(userSession, 1L));
    doNothing().when(twoFactor).requireSession(userSession);
    assertDoesNotThrow(() -> userNotificationService.unreadCount(userSession));
  }

  UserEntity create(String name, RoleType type, boolean enabled) {
    RoleEntity role =
        entityManager
            .createQuery("select r from RoleEntity r where r.name = :type", RoleEntity.class)
            .setParameter("type", type)
            .getSingleResult();
    UserEntity u =
        UserEntity.builder()
            .firstName(name)
            .lastName("Test")
            .email(name + "@example.test")
            .password("hash")
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .enabled(enabled)
            .status(UserStatus.OFFLINE)
            .roleEntities(new ArrayList<>(List.of(role)))
            .build();
    return userRepository.saveAndFlush(u);
  }

  MockHttpSession session(UserEntity user) {
    UserDetails principal =
        new CustomUserDetailsService(userRepository).loadUserByUsername(user.getEmail());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  Preview everyone() {
    return userNotificationService.preview(adminSession, new Selection(true, Set.of()));
  }

  Preview selected(Long... ids) {
    return userNotificationService.preview(adminSession, new Selection(false, Set.of(ids)));
  }

  Sent broadcast() {
    return userNotificationService.send(
        adminSession, UUID.randomUUID(), "Update", "Plain text", everyone());
  }

  long notificationCount() {
    return userNotificationRepository.count();
  }

  void mutate(UserEntity user, java.util.function.Consumer<UserEntity> change) {
    transactionTemplate.executeWithoutResult(
        s -> change.accept(userRepository.findById(user.getId()).orElseThrow()));
  }

  void assertProblem(Problem problem, Runnable work) {
    assertEquals(problem, assertThrows(Rejected.class, work::run).problem());
  }

  @Test
  void notificationLogFollowsCommitAndOmitsPrivateDraftAndIdentity() {
    Logger logger = (Logger) LoggerFactory.getLogger(UserNotificationService.class);
    ListAppender<ILoggingEvent> logs = new ListAppender<>();
    logs.start();
    logger.addAppender(logs);
    try {
      Preview preview = selected(ordinary.getId());
      UUID request = UUID.randomUUID();
      transactionTemplate.executeWithoutResult(status -> {
        userNotificationService.send(
            adminSession, request, "private title", "private body token=secret", preview);
        assertTrue(logs.list.isEmpty());
        status.setRollbackOnly();
      });
      assertTrue(logs.list.isEmpty());
      assertEquals(0, userNotificationRepository.count());
      Sent sent =
          userNotificationService.send(
              adminSession, request, "private title", "private body token=secret", preview);
      assertEquals(
          1,
          logs.list.stream()
              .filter(e -> e.getFormattedMessage().startsWith("Admin notification created:"))
              .count());
      userNotificationService.send(
          adminSession, request, "private title", "private body token=secret", preview);
      assertEquals(
          1,
          logs.list.stream()
              .filter(e -> e.getFormattedMessage().startsWith("Admin notification created:"))
              .count());
      assertTrue(
          logs.list.getFirst().getFormattedMessage().contains("notificationId=" + sent.id()));
      for (ILoggingEvent event : logs.list) {
        String text = event.getFormattedMessage();
        assertFalse(text.contains("private"));
        assertFalse(text.contains("secret"));
        assertFalse(text.contains(sender.getEmail()));
        assertFalse(text.contains(ordinary.getEmail()));
        assertFalse(text.contains(request.toString()));
      }
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  @Test
  void broadcastSnapshotsEnabledAccountsIncludingOfflineGuestAndSender() {
    Sent sent = broadcast();
    assertEquals(3, sent.recipientCount());
    assertEquals(3, userNotificationRecipientRepository.count());
    assertEquals(1, userNotificationService.unreadCount(adminSession));
    assertEquals(1, userNotificationService.unreadCount(userSession));
    assertEquals(
        0, userNotificationRecipientRepository.countUnread(disabled.getId(), Instant.EPOCH));
    assertEquals(
        0, userNotificationRecipientRepository.countUnread(pending.getId(), Instant.EPOCH));
    transactionTemplate.executeWithoutResult(s -> create("later", RoleType.ROLE_USER, true));
    assertEquals(3, userNotificationRecipientRepository.count());
  }

  @Test
  void individualSelectionIsDistinctAndStable() {
    Preview selected = selected(guest.getId(), ordinary.getId());
    Sent sent =
        userNotificationService.send(adminSession, UUID.randomUUID(), "Note", "Body", selected);
    assertEquals(2, sent.recipientCount());
    assertEquals(0, userNotificationService.unreadCount(adminSession));
    assertEquals(selected.ids(), userNotificationRecipientRepository.recipientIds(sent.id()));
  }

  @Test
  void targetsDoNotExposeCredentialsAndExcludePendingAndDisabled() {
    List<Target> targets = userNotificationService.targets(adminSession);
    assertEquals(3, targets.size());
    assertEquals(
        List.of("id", "name", "email"),
        Arrays.stream(Target.class.getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName)
            .toList());
  }

  @Test
  void emptySelectionRejected() {
    assertProblem(Problem.NO_RECIPIENTS, this::selected);
  }

  @Test
  void unavailableSelectedAccountRejected() {
    assertProblem(Problem.RECIPIENTS_CHANGED, () -> selected(ordinary.getId(), disabled.getId()));
  }

  @Test
  void everyoneIgnoresResidualManualSelection() {
    assertEquals(Set.of(), new Selection(true, Set.of(123L)).ids());
  }

  @ParameterizedTest
  @EnumSource(
      value = RoleType.class,
      names = {"ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST"})
  void nonSuperadminsCannotListTargetsPreviewSendOrViewSent(RoleType role) {
    UserEntity user = transactionTemplate.execute(s -> create("role" + role.name(), role, true));
    MockHttpSession session = session(user);
    assertFalse(userNotificationService.canSend(session));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.targets(session));
    Selection selection = new Selection(true, Set.of());
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.preview(session, selection));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.sent(session, 0, 5));
    UUID operationId = UUID.randomUUID();
    Preview preview2 = everyone();
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.send(session, operationId, "X", "Y", preview2));
    assertEquals(0, notificationCount());
  }

  @Test
  void threadAuthenticationDoesNotAuthorizeAnonymousOrRestrictedSession() {
    SecurityContext context =
        (org.springframework.security.core.context.SecurityContext)
            adminSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    SecurityContextHolder.setContext(context);
    MockHttpSession restricted = new MockHttpSession();
    restricted.setAttribute("sessionRecoveryUserId", sender.getId());
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.inbox(null, ReadFilter.ALL, 0, 20));
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.inbox(restricted, ReadFilter.ALL, 0, 20));
  }

  @Test
  void revokedOrInvalidatedSessionCannotRead() {
    sessionRegistry.registerNewSession(userSession.getId(), ordinary.getEmail());
    sessionRegistry.getSessionInformation(userSession.getId()).expireNow();
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.unreadCount(userSession));
    adminSession.invalidate();
    assertFalse(userNotificationService.canSend(adminSession));
  }

  @Test
  void freshRoleCheckRejectsOldSuperadminSessionAfterDemotion() {
    mutate(sender, u -> u.setRoleEntities(new ArrayList<>()));
    assertFalse(userNotificationService.canSend(adminSession));
    assertEquals(
        0, userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertFalse(adminSession.isInvalid());
  }

  @Test
  void disabledSessionCannotReadOrSend() {
    mutate(sender, u -> u.setEnabled(false));
    assertFalse(userNotificationService.canSend(adminSession));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.targets(adminSession));
  }

  @Test
  void maintenancePausesOrdinaryInboxButAllowsSuperadmin() {
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.unreadCount(userSession));
    assertTrue(userNotificationService.canSend(adminSession));
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("unavailable"));
    assertFalse(userNotificationService.canSend(adminSession));
  }

  @ParameterizedTest
  @ValueSource(ints = {5, 10, 15})
  void sentHistoryUsesSelectedPageSizeAndKeepsSenderOwnership(int size) {
    Preview checked = everyone();
    for (int index = 0; index < 17; index++) {
      userNotificationService.send(
          adminSession, UUID.randomUUID(), "Update " + index, "Body", checked);
    }
    Page<Sent> first = userNotificationService.sent(adminSession, 0, size);
    Page<Sent> second = userNotificationService.sent(adminSession, 1, size);
    assertEquals(17, first.getTotalElements());
    assertEquals(size, first.getNumberOfElements());
    assertEquals(Math.min(size, 17 - size), second.getNumberOfElements());
    assertEquals(size, first.getSize());
    assertTrue(first.getContent().stream().noneMatch(second.getContent()::contains));
    assertEquals("Update 16", first.getContent().getFirst().title());
    UserEntity other =
        transactionTemplate.execute(
            status -> create("other-superadmin", RoleType.ROLE_SUPERADMIN, true));
    assertTrue(userNotificationService.sent(session(other), 0, size).isEmpty());
  }

  @Test
  void maintenanceBlocksManualReviewAndSendButKeepsSentHistoryReadable() {
    broadcast();
    Preview checked = everyone();
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertEquals(MaintenanceService.State.ACTIVE, userNotificationService.deliveryState());
    assertTrue(userNotificationService.canSend(adminSession));
    assertProblem(
        Problem.DELIVERY_PAUSED,
        () -> userNotificationService.preview(adminSession, new Selection(true, Set.of())));
    assertProblem(
        Problem.DELIVERY_PAUSED,
        () ->
            userNotificationService.send(adminSession, UUID.randomUUID(), "New", "Body", checked));
    assertEquals(1, userNotificationService.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(1, notificationCount());
    assertEquals(3, userNotificationRecipientRepository.count());
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    userNotificationService.send(adminSession, UUID.randomUUID(), "Resumed", "Body", everyone());
    assertEquals(2, notificationCount());
  }

  @Test
  void unreadableMaintenanceSettingBlocksPreviouslyReviewedManualSend() {
    Preview checked = everyone();
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("Isolated configuration failure"));
    assertEquals(MaintenanceService.State.UNAVAILABLE, userNotificationService.deliveryState());
    Selection selection = new Selection(true, Set.of());
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.preview(adminSession, selection));
    UUID operationId = UUID.randomUUID();
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.send(adminSession, operationId, "Subject", "Body", checked));
    assertEquals(0, notificationCount());
    assertEquals(0, userNotificationRecipientRepository.count());
  }

  @Test
  void recipientMutationRequiresNewPreviewAndCreatesNothing() {
    Preview preview = everyone();
    mutate(ordinary, u -> u.setEnabled(false));
    assertProblem(
        Problem.RECIPIENTS_CHANGED,
        () -> userNotificationService.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
    assertEquals(0, notificationCount());
  }

  @Test
  void recipientPreviewRechecksDatabaseEvenWithAnOldManagedAccount() throws Exception {
    Preview preview = everyone();
    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      transactionTemplate.executeWithoutResult(s -> {
        assertTrue(userRepository.findById(ordinary.getId()).orElseThrow().isEnabled());
        try {
          executor
              .submit(() -> mutate(ordinary, u -> u.setEnabled(false)))
              .get(5, TimeUnit.SECONDS);
        } catch (Exception failure) {
          throw new IllegalStateException(failure);
        }
        assertProblem(
            Problem.RECIPIENTS_CHANGED,
            () -> userNotificationService.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
        s.setRollbackOnly();
      });
    }
  }

  @Test
  void newlyActivatedAccountRequiresNewBroadcastPreview() {
    Preview preview = everyone();
    mutate(pending, u -> u.setEnabled(true));
    assertProblem(
        Problem.RECIPIENTS_CHANGED,
        () -> userNotificationService.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "1234567890"})
  void invalidDraftNeverCreatesRecipients(String value) {
    String title = value.isBlank() ? value : value.repeat(13);
    assertProblem(
        Problem.INVALID_DRAFT,
        () ->
            userNotificationService.send(
                adminSession, UUID.randomUUID(), title, "body", everyone()));
    assertEquals(0, userNotificationRecipientRepository.count());
  }

  @Test
  void oversizedBodyRejectedAndPlainTextStoredExactly() {
    assertProblem(
        Problem.INVALID_DRAFT,
        () ->
            userNotificationService.send(
                adminSession, UUID.randomUUID(), "title", "x".repeat(4001), everyone()));
    Sent sent =
        userNotificationService.send(
            adminSession,
            UUID.randomUUID(),
            "<script>title</script>",
            "<img src=x onerror=alert(1)>\n&",
            everyone());
    assertEquals("<img src=x onerror=alert(1)>\n&", sent.body());
  }

  @Test
  void sameRequestIsIdempotentAndChangedContentRejected() {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    Sent first = userNotificationService.send(adminSession, key, "title", "body", preview);
    assertEquals(first, userNotificationService.send(adminSession, key, "title", "body", preview));
    assertEquals(1, notificationCount());
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> userNotificationService.send(adminSession, key, "changed", "body", preview));
    assertEquals(3, userNotificationRecipientRepository.count());
  }

  @Test
  void concurrentReplayCommitsExactlyOneContentAndOneRecipientSet() throws Exception {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Callable<Sent> send =
          () -> {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return userNotificationService.send(adminSession, key, "title", "body", preview);
          };
      Future<Sent> a = executor.submit(send);
      Future<Sent> b = executor.submit(send);
      start.countDown();
      assertEquals(a.get(15, TimeUnit.SECONDS).id(), b.get(15, TimeUnit.SECONDS).id());
    }
    assertEquals(1, notificationCount());
    assertEquals(3, userNotificationRecipientRepository.count());
  }

  @Test
  void senderCannotReplayOtherSendersRequest() {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    userNotificationService.send(adminSession, key, "title", "body", preview);
    UserEntity other =
        transactionTemplate.execute(s -> create("super2", RoleType.ROLE_SUPERADMIN, true));
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> userNotificationService.send(session(other), key, "title", "body", preview));
  }

  @Test
  void readStatusIsAccountWideIdempotentAndDoesNotLeakOtherInbox() {
    broadcast();
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item foreign =
        userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(foreign.id()));
    assertEquals(1, userNotificationService.unreadCount(adminSession));
    assertEquals(1, userNotificationService.unreadCount(userSession));
    userNotificationService.markRead(userSession, List.of(own.id()));
    userNotificationService.markRead(userSession, List.of(own.id()));
    assertEquals(0, userNotificationService.unreadCount(session(ordinary)));
    assertEquals(
        0, userNotificationService.inbox(userSession, ReadFilter.UNREAD, 0, 20).getTotalElements());
    assertTrue(
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .read());
  }

  @Test
  void dismissAffectsOnlyOwnInboxAndUnreadCountAndSurvivesNewSessions() {
    Sent sent = broadcast();
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item foreign =
        userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(userNotificationService.events().register(pushes::add));
    userNotificationService.dismiss(userSession, foreign.id());
    assertEquals(1, userNotificationService.unreadCount(adminSession));
    assertEquals(1, userNotificationService.unreadCount(userSession));
    assertTrue(pushes.isEmpty());
    userNotificationService.dismiss(userSession, own.id());
    userNotificationService.dismiss(userSession, own.id());
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertTrue(userNotificationService.inbox(session(ordinary), ReadFilter.ALL, 0, 20).isEmpty());
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.UNREAD, 0, 20).isEmpty());
    assertEquals(0, userNotificationService.unreadCount(userSession));
    assertTrue(userNotificationService.unreadSnapshot(userSession).isEmpty());
    userNotificationService.markRead(userSession, List.of(own.id()));
    assertNotNull(
        userNotificationRecipientRepository.findById(own.id()).orElseThrow().getDismissedAt());
    assertNull(userNotificationRecipientRepository.findById(own.id()).orElseThrow().getReadAt());
    assertEquals(
        1, userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(1, userNotificationService.unreadCount(adminSession));
    assertEquals(
        3,
        userNotificationService.sent(adminSession, 0, 5).getContent().getFirst().recipientCount());
    assertEquals(3, userNotificationRecipientRepository.recipientIds(sent.id()).size());
  }

  @Test
  void dismissReadPreservesUnreadOtherOwnersHistoryAndIdempotency() {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    Sent sent = userNotificationService.send(adminSession, key, "Read notice", "Body", preview);
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item other =
        userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(own.id()));
    userNotificationService.markRead(adminSession, List.of(other.id()));
    broadcast();
    Item unread =
        userNotificationService
            .inbox(userSession, ReadFilter.UNREAD, 0, 20)
            .getContent()
            .getFirst();
    Sent expired = broadcast();
    Item expiredReceipt =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(expiredReceipt.id()));
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, userNotificationService.readCount(userSession));
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(userNotificationBroadcaster.register(pushes::add));
    userNotificationService.dismissRead(userSession);
    userNotificationService.dismissRead(userSession);
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertEquals(0, userNotificationService.readCount(session(ordinary)));
    assertEquals(
        List.of(unread.id()),
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).stream()
            .map(Item::id)
            .toList());
    assertEquals(1, userNotificationService.unreadCount(userSession));
    assertEquals(1, userNotificationService.readCount(adminSession));
    assertNull(
        userNotificationRecipientRepository.findById(other.id()).orElseThrow().getDismissedAt());
    assertNull(
        userNotificationRecipientRepository
            .findById(expiredReceipt.id())
            .orElseThrow()
            .getDismissedAt());
    UserNotificationRecipientEntity removed =
        userNotificationRecipientRepository.findById(own.id()).orElseThrow();
    assertNotNull(removed.getDismissedAt());
    assertNotNull(removed.getReadAt());
    assertEquals(3, userNotificationService.sent(adminSession, 0, 20).getTotalElements());
    assertEquals(3, userNotificationRecipientRepository.recipientIds(sent.id()).size());
    assertEquals(
        sent.id(),
        userNotificationService.send(adminSession, key, "Read notice", "Body", preview).id());
    assertEquals(0, userNotificationService.readCount(userSession));
    assertEquals(
        1, userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
  }

  @Test
  void dismissReadRollbackAndAuthorizationDoNotRemoveEntriesOrPublish() {
    broadcast();
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(own.id()));
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(userNotificationBroadcaster.register(pushes::add));
    transactionTemplate.executeWithoutResult(status -> {
      userNotificationService.dismissRead(userSession);
      assertTrue(pushes.isEmpty());
      status.setRollbackOnly();
    });
    assertTrue(pushes.isEmpty());
    assertEquals(1, userNotificationService.readCount(userSession));
    assertNull(
        userNotificationRecipientRepository.findById(own.id()).orElseThrow().getDismissedAt());
    assertThrows(AccessDeniedException.class, () -> userNotificationService.dismissRead(null));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.readCount(null));
    mutate(ordinary, user -> user.setEnabled(false));
    assertThrows(
        AccessDeniedException.class, () -> userNotificationService.dismissRead(userSession));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.readCount(userSession));
    assertTrue(pushes.isEmpty());
    assertNull(
        userNotificationRecipientRepository.findById(own.id()).orElseThrow().getDismissedAt());
  }

  @Test
  void dismissReadHonorsUnlimitedRetentionAndHandlesAutomaticNotifications() {
    Sent sent = broadcast();
    Item manual =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(manual.id()));
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.recordSecurityEvent(
            userRepository.findById(ordinary.getId()).orElseThrow(),
            UserNotificationType.PASSWORD_CHANGED));
    Item automatic =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.markRead(userSession, List.of(automatic.id()));
    assertEquals(1, userNotificationService.readCount(userSession));
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("");
    assertEquals(2, userNotificationService.readCount(userSession));
    userNotificationService.dismissRead(userSession);
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
    assertEquals(0, userNotificationService.readCount(userSession));
    assertNotNull(
        userNotificationRecipientRepository.findById(manual.id()).orElseThrow().getDismissedAt());
    assertNotNull(
        userNotificationRecipientRepository
            .findById(automatic.id())
            .orElseThrow()
            .getDismissedAt());
    assertEquals(2, notificationCount());
  }

  @Test
  void dismissedNotificationDoesNotReappearOnIdempotentSendRetry() {
    Preview preview = everyone();
    UUID key = UUID.randomUUID();
    Sent first = userNotificationService.send(adminSession, key, "Title", "Body", preview);
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    userNotificationService.dismiss(userSession, own.id());
    assertEquals(
        first.id(), userNotificationService.send(adminSession, key, "Title", "Body", preview).id());
    assertEquals(1, notificationCount());
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
  }

  @Test
  void dismissRollbackPublishesNothingAndRequiresValidOwnerSession() {
    broadcast();
    Item own =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(userNotificationService.events().register(pushes::add));
    transactionTemplate.executeWithoutResult(status -> {
      userNotificationService.dismiss(userSession, own.id());
      status.setRollbackOnly();
    });
    assertTrue(pushes.isEmpty());
    assertEquals(1, userNotificationService.unreadCount(userSession));
    Long ownNotificationId = own.id();
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.dismiss(null, ownNotificationId));
    mutate(ordinary, user -> user.setEnabled(false));
    Long disabledNotificationId = own.id();
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.dismiss(userSession, disabledNotificationId));
    assertNull(
        userNotificationRecipientRepository.findById(own.id()).orElseThrow().getDismissedAt());
  }

  @Test
  void readUpdateIsVisibleInsideSamePersistenceContextWithoutClearingUnrelatedChanges() {
    broadcast();
    transactionTemplate.executeWithoutResult(s -> {
      Item first =
          userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
      UserNotificationRecipientEntity managed =
          userNotificationRecipientRepository.findById(first.id()).orElseThrow();
      assertNull(managed.getReadAt());
      UserEntity user = userRepository.findById(ordinary.getId()).orElseThrow();
      user.setFirstName("Pending");
      userNotificationService.markRead(userSession, List.of(first.id()));
      assertTrue(
          userNotificationService
              .inbox(userSession, ReadFilter.ALL, 0, 20)
              .getContent()
              .getFirst()
              .read());
      assertTrue(entityManager.contains(user));
      assertEquals("Pending", user.getFirstName());
    });
  }

  @Test
  void markAllUsesSnapshotAndDoesNotMarkLaterArrivals() {
    broadcast();
    List<Long> snapshot = userNotificationService.unreadSnapshot(userSession);
    broadcast();
    userNotificationService.markRead(userSession, snapshot);
    assertEquals(1, userNotificationService.unreadCount(userSession));
    userNotificationService.markRead(userSession, List.of());
    assertEquals(1, userNotificationService.unreadCount(userSession));
  }

  @Test
  void persistedNotificationsAreVisibleInNewSessionAndPagedNewestFirst() {
    broadcast();
    Sent newer = broadcast();
    MockHttpSession fresh = session(ordinary);
    Page<Item> page = userNotificationService.inbox(fresh, ReadFilter.ALL, 0, 1);
    assertEquals(2, page.getTotalElements());
    assertTrue(page.hasNext());
    assertEquals(newer.createdAt(), page.getContent().getFirst().createdAt());
    assertEquals(
        1, userNotificationService.inbox(fresh, ReadFilter.ALL, 1, 1).getNumberOfElements());
  }

  @ParameterizedTest
  @EnumSource(ReadFilter.class)
  void readFilterKeepsOwnershipDismissalRetentionAndPagination(ReadFilter filter) {
    broadcast();
    Long unreadId =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    broadcast();
    Long readId =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.markRead(userSession, List.of(readId));
    broadcast();
    Long dismissed =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.markRead(userSession, List.of(dismissed));
    userNotificationService.dismiss(userSession, dismissed);
    Sent expired = broadcast();
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    Page<Item> page = userNotificationService.inbox(session(ordinary), filter, 0, 1);
    assertEquals(filter == ReadFilter.ALL ? 2 : 1, page.getTotalElements());
    assertEquals(
        filter == ReadFilter.UNREAD ? unreadId : readId, page.getContent().getFirst().id());
    if (filter == ReadFilter.ALL) {
      assertTrue(page.hasNext());
      assertEquals(
          unreadId,
          userNotificationService.inbox(userSession, filter, 1, 1).getContent().getFirst().id());
    } else {
      assertFalse(page.hasNext());
    }
    assertTrue(userNotificationService.inbox(adminSession, ReadFilter.READ, 0, 5).isEmpty());
    assertEquals(List.of(unreadId), userNotificationService.unreadSnapshot(userSession));
    userNotificationService.markRead(userSession, List.of(unreadId));
    assertEquals(
        2, userNotificationService.inbox(userSession, ReadFilter.READ, 0, 5).getTotalElements());
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.UNREAD, 0, 5).isEmpty());
  }

  @Test
  void readFilterSelectsBeforeApplyingFiveItemPreviewLimit() {
    for (int notificationIndex = 0; notificationIndex < 7; notificationIndex++) {
      broadcast();
    }
    List<Item> all = userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent();
    userNotificationService.markRead(
        userSession, all.subList(1, 7).stream().map(Item::id).toList());
    Page<Item> first = userNotificationService.inbox(userSession, ReadFilter.READ, 0, 5);
    assertEquals(6, first.getTotalElements());
    assertEquals(5, first.getNumberOfElements());
    assertTrue(first.hasNext());
    assertTrue(first.stream().allMatch(Item::read));
    assertEquals(
        1, userNotificationService.inbox(userSession, ReadFilter.READ, 1, 5).getNumberOfElements());
    assertEquals(
        all.getFirst().id(),
        userNotificationService
            .inbox(userSession, ReadFilter.UNREAD, 0, 5)
            .getContent()
            .getFirst()
            .id());
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.inbox(null, ReadFilter.READ, 0, 5));
  }

  @Test
  void inboxSearchFiltersBeforePagingAndExcludesForeignDismissedExpiredEntries() {
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha report", "First body", everyone());
    Long older =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.markRead(userSession, List.of(older));
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Second report", "ALPHA in the body", everyone());
    Long newer =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha private", "Foreign body", selected(sender.getId()));
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha removed", "Removed body", everyone());
    userNotificationService.dismiss(
        userSession,
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id());
    Sent expired =
        userNotificationService.send(
            adminSession, UUID.randomUUID(), "Alpha old", "Expired body", everyone());
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(2, userNotificationService.inboxCount(userSession));
    Page<Item> first =
        userNotificationService.searchInbox(
            session(ordinary), ReadFilter.ALL, "  aLpHa  ", Set.of(), 0, 1);
    assertEquals(2, first.getTotalElements());
    assertEquals(newer, first.getContent().getFirst().id());
    assertTrue(first.hasNext());
    assertEquals(
        older,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, "alpha", Set.of(), 1, 1)
            .getContent()
            .getFirst()
            .id());
    Page<Item> unread =
        userNotificationService.searchInbox(
            userSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 20);
    assertEquals(List.of(newer), unread.stream().map(Item::id).toList());
    assertEquals(
        List.of(older),
        userNotificationService
            .searchInbox(userSession, ReadFilter.READ, "alpha", Set.of(), 0, 20)
            .stream()
            .map(Item::id)
            .toList());
    assertEquals(
        2,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, "  ", Set.of(), 0, 20)
            .getTotalElements());
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.searchInbox(null, ReadFilter.ALL, "alpha", Set.of(), 0, 20));
    assertThrows(AccessDeniedException.class, () -> userNotificationService.inboxCount(null));
    String longSearchText = "x".repeat(121);
    Set<UserNotificationType> notificationTypes = Set.of();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            userNotificationService.searchInbox(
                userSession, ReadFilter.ALL, longSearchText, notificationTypes, 0, 20));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            userNotificationService.searchInbox(
                userSession, ReadFilter.ALL, "alpha", Set.of(), -1, 20));
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("");
    assertEquals(3, userNotificationService.inboxCount(userSession));
    assertEquals(
        3,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, "alpha", Set.of(), 0, 20)
            .getTotalElements());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(ReadFilter.class)
  void inboxSearchReadStatesAreAppliedBeforePagingAndForEmptyQueries(ReadFilter filter) {
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha first", "Body", everyone());
    Long first =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha second", "Body", everyone());
    Long second =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    userNotificationService.markRead(userSession, List.of(first, second));
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Alpha third", "Body", everyone());
    Long third =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    List<Long> expected =
        switch (filter) {
          case ALL -> List.of(third, second, first);
          case READ -> List.of(second, first);
          case UNREAD -> List.of(third);
        };
    assertEquals(
        expected,
        userNotificationService.searchInbox(userSession, filter, "alpha", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    assertEquals(
        expected,
        userNotificationService.searchInbox(userSession, filter, "  ", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    Page<Item> firstPage =
        userNotificationService.searchInbox(userSession, filter, "alpha", Set.of(), 0, 1);
    assertEquals(expected.size(), firstPage.getTotalElements());
    assertEquals(expected.getFirst(), firstPage.getContent().getFirst().id());
    if (expected.size() > 1) {
      assertEquals(
          expected.get(1),
          userNotificationService
              .searchInbox(userSession, filter, "alpha", Set.of(), 1, 1)
              .getContent()
              .getFirst()
              .id());
    }
    assertThrows(
        AccessDeniedException.class,
        () -> userNotificationService.searchInbox(null, filter, "alpha", Set.of(), 0, 20));
  }

  @ParameterizedTest
  @ValueSource(strings = {"%", "_", "!"})
  void inboxSearchTreatsLikeMetacharactersLiterally(String literal) {
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Needle " + literal, "Exact match", everyone());
    Long expected =
        userNotificationService
            .inbox(userSession, ReadFilter.ALL, 0, 20)
            .getContent()
            .getFirst()
            .id();
    broadcast();
    Page<Item> found =
        userNotificationService.searchInbox(userSession, ReadFilter.ALL, literal, Set.of(), 0, 20);
    assertEquals(List.of(expected), found.stream().map(Item::id).toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxSearchMatchesLocalizedSystemTitlesBodiesAndSources(String language) {
    CustomI18NProvider provider = new de.derpeterson.app.i18n.CustomI18NProvider();
    MessageProperties messageProperties =
        de.derpeterson.app.i18n.MessageProperties.forLocale(
            provider, Locale.forLanguageTag(language));
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.recordSecurityEvent(
            userRepository.findById(ordinary.getId()).orElseThrow(),
            UserNotificationType.PASSWORD_CHANGED));
    userNotificationService.send(
        adminSession, UUID.randomUUID(), "Original notice", "Original manual body", everyone());
    String title = messageProperties.getNotificationsPasswordChangedTitle();
    Set<UserNotificationType> types =
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, title);
    assertTrue(types.contains(UserNotificationType.PASSWORD_CHANGED));
    assertEquals(
        1,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, title, types, 0, 20)
            .getTotalElements());
    String body = messageProperties.getNotificationsPasswordChangedBody().substring(0, 25);
    types =
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, body);
    assertEquals(
        1,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, body, types, 0, 20)
            .getTotalElements());
    String system = messageProperties.getNotificationsSourceSystem();
    types =
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, system);
    assertEquals(
        1,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, system, types, 0, 20)
            .getTotalElements());
    assertTrue(
        userNotificationService
            .searchInbox(adminSession, ReadFilter.ALL, system, types, 0, 20)
            .isEmpty());
    String administration = messageProperties.getNotificationsSourceAdministration();
    types =
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, administration);
    assertEquals(
        UserNotificationType.ADMIN_ANNOUNCEMENT,
        userNotificationService
            .searchInbox(userSession, ReadFilter.ALL, administration, types, 0, 20)
            .getContent()
            .getFirst()
            .type());
    assertTrue(
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
                messageProperties, "   ")
            .isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requesterClosureCanBeFoundByItsVisibleTicketTitlePrefix(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    transactionTemplate.executeWithoutResult(
        status -> userNotificationService.recordRequesterClosed("#10001", 1L, List.of(sender)));
    String query = messageProperties.getSupportTicketNumber("").strip();
    Set<UserNotificationType> types =
        de.derpeterson.app.i18n.UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, query);
    assertTrue(types.contains(UserNotificationType.SUPPORT_REQUESTER_CLOSED));
    assertEquals(
        1,
        userNotificationService
            .searchInbox(adminSession, ReadFilter.ALL, query, types, 0, 20)
            .getTotalElements());
  }

  @Test
  void paginationBoundsAreChecked() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userNotificationService.inbox(userSession, ReadFilter.ALL, -1, 20));
    assertThrows(
        IllegalArgumentException.class,
        () -> userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 101));
    assertThrows(
        IllegalArgumentException.class,
        () -> userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 0));
  }

  @Test
  void eventsPublishOnlyAfterCommitAndNotOnRollback() {
    AtomicInteger count = new AtomicInteger();
    subscriptions.add(
        userNotificationBroadcaster.register(ids -> {
          assertTrue(ids.contains(ordinary.getId()));
          count.incrementAndGet();
        }));
    transactionTemplate.executeWithoutResult(s -> {
      broadcast();
      assertEquals(0, count.get());
      s.setRollbackOnly();
    });
    assertEquals(0, count.get());
    assertEquals(0, notificationCount());
    broadcast();
    assertEquals(1, count.get());
  }

  @Test
  void brokenPushSubscriberCannotTurnCommittedSendIntoFailure() {
    subscriptions.add(
        userNotificationBroadcaster.register(ids -> {
          throw new IllegalStateException("detached UI");
        }));
    AtomicInteger count = new AtomicInteger();
    subscriptions.add(userNotificationBroadcaster.register(ids -> count.incrementAndGet()));
    assertDoesNotThrow(this::broadcast);
    assertEquals(1, notificationCount());
    assertEquals(1, count.get());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserNotificationType.class,
      names = {"PASSWORD_CHANGED", "PASSWORD_RESET", "EMAIL_CHANGED"})
  void securityEventsAreMinimalAndRequireTheDomainTransaction(UserNotificationType type) {
    assertThrows(
        IllegalTransactionStateException.class,
        () -> userNotificationService.recordSecurityEvent(ordinary, type));
    transactionTemplate.executeWithoutResult(s ->
        userNotificationService.recordSecurityEvent(
            userRepository.findById(ordinary.getId()).orElseThrow(), type));
    Item item =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertEquals(type, item.type());
    assertNull(item.title());
    assertNull(item.body());
    assertEquals(0, userNotificationService.sent(adminSession, 0, 5).getTotalElements());
  }

  @Test
  void recoveryNotificationKeepsTheTicketDestinationRestrictedToAdministrators() {
    transactionTemplate.executeWithoutResult(
        status -> userNotificationService.recordRecoveryRequest("#10001", 1L));
    Item item =
        userNotificationService
            .inbox(adminSession, ReadFilter.UNREAD, 0, 20)
            .getContent()
            .getFirst();
    assertEquals(UserNotificationType.RECOVERY_REQUEST, item.type());
    assertEquals("#10001", item.supportTicketNumber());
    assertTrue(userNotificationService.canOpenTickets(adminSession));
    assertFalse(userNotificationService.canOpenTickets(userSession));
    assertFalse(userNotificationService.canOpenTickets(new MockHttpSession()));
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
  }

  @Test
  void supportReplyNotificationShowsOnlyTheTicketNumberToItsRecipient() {
    String ticketNumber = "#10003";
    transactionTemplate.executeWithoutResult(
        status -> userNotificationService.recordSupportReply(ticketNumber, 1L, List.of(sender)));
    Item item =
        userNotificationService
            .searchInbox(adminSession, ReadFilter.UNREAD, ticketNumber, Set.of(), 0, 20)
            .getContent()
            .getFirst();
    assertEquals(UserNotificationType.SUPPORT_REPLY, item.type());
    assertEquals(ticketNumber, item.supportTicketNumber());
    assertNull(item.title());
    assertNull(item.body());
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
  }

  @Test
  void retiredTicketHintsStayInHistoryWithoutChangingReadStateOrCountingAsUnread() {
    transactionTemplate.executeWithoutResult(
        status -> userNotificationService.recordRecoveryRequest("#10001", 1L));
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.retireTicketNotifications(
            "#10001", de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED));
    assertEquals(0, userNotificationService.unreadCount(adminSession));
    assertTrue(userNotificationService.unreadSnapshot(adminSession).isEmpty());
    assertTrue(userNotificationService.inbox(adminSession, ReadFilter.UNREAD, 0, 20).isEmpty());
    Item assigned =
        userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertFalse(assigned.read());
    assertFalse(assigned.unread());
    assertEquals(
        de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED,
        assigned.relevance());
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.retireTicketNotifications(
            "#10001", de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_CLOSED));
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.retireTicketNotifications(
            "#10001", de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_CLOSED));
    Item closed =
        userNotificationService.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertFalse(closed.read());
    assertEquals(
        de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_CLOSED, closed.relevance());
    assertEquals(0, userNotificationService.unreadCount(adminSession));
    assertEquals(1, userNotificationService.inboxCount(adminSession));
  }

  @Test
  void rolledBackTicketRetirementKeepsTheHintActionableAndDoesNotPublish() {
    transactionTemplate.executeWithoutResult(
        status -> userNotificationService.recordRecoveryRequest("#10001", 1L));
    List<Set<Long>> changes = new ArrayList<>();
    subscriptions.add(userNotificationBroadcaster.register(changes::add));
    transactionTemplate.executeWithoutResult(status -> {
      userNotificationService.retireTicketNotifications(
          "#10001", de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED);
      status.setRollbackOnly();
    });
    assertTrue(changes.isEmpty());
    assertEquals(1, userNotificationService.unreadCount(adminSession));
  }

  @Test
  void rolledBackSecurityEventDoesNotClaimSuccess() {
    transactionTemplate.executeWithoutResult(s -> {
      userNotificationService.recordSecurityEvent(ordinary, UserNotificationType.PASSWORD_CHANGED);
      s.setRollbackOnly();
    });
    assertEquals(0, notificationCount());
    assertEquals(0, userNotificationRecipientRepository.count());
  }

  @Test
  void deletingRecipientKeepsSharedNotificationForOthersAndDeletingSenderSetsNull() {
    Sent sent = broadcast();
    transactionTemplate.executeWithoutResult(s -> {
      userRepository.deleteById(ordinary.getId());
      userRepository.flush();
    });
    assertEquals(2, userNotificationRecipientRepository.count());
    assertTrue(userNotificationRepository.existsById(sent.id()));
    transactionTemplate.executeWithoutResult(s -> {
      userRepository.deleteById(sender.getId());
      userRepository.flush();
    });
    assertEquals(1, userNotificationRecipientRepository.count());
    transactionTemplate.executeWithoutResult(
        s -> assertNull(userNotificationRepository.findById(sent.id()).orElseThrow().getSender()));
    assertEquals(
        1, userNotificationService.inbox(session(guest), ReadFilter.ALL, 0, 20).getTotalElements());
  }

  @Test
  void cleanupRemovesReadAndUnreadExpiredNotificationsAndOrphanContents() {
    Sent old = broadcast();
    Sent fresh = broadcast();
    // Unveränderliche Fachspalten nur als künstliche Altstruktur per SQL altern lassen.
    transactionTemplate.executeWithoutResult(s ->
        entityManager
            .createNativeQuery("update user_notifications set created_at = :old where id = :id")
            .setParameter("old", Instant.now().minus(366, java.time.temporal.ChronoUnit.DAYS))
            .setParameter("id", old.id())
            .executeUpdate());
    assertEquals(1, cleanup.purgeRetiredNotifications());
    assertFalse(userNotificationRepository.existsById(old.id()));
    assertTrue(userNotificationRepository.existsById(fresh.id()));
    assertEquals(3, userNotificationRecipientRepository.count());
    transactionTemplate.executeWithoutResult(s -> userNotificationRecipientRepository.deleteAll());
    assertEquals(0, cleanup.purgeRetiredNotifications());
    assertTrue(userNotificationRepository.existsById(fresh.id()));
  }

  private void createdAt(Long id, Instant created) {
    transactionTemplate.executeWithoutResult(status ->
        entityManager
            .createNativeQuery("update user_notifications set created_at = :created where id = :id")
            .setParameter("created", created)
            .setParameter("id", id)
            .executeUpdate());
  }

  @Test
  void inboxAndAdminHistoryExpireIndependentlyAndReadLiveConfiguration() {
    Sent sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
    assertTrue(userNotificationService.inbox(userSession, ReadFilter.UNREAD, 0, 20).isEmpty());
    assertEquals(0, userNotificationService.unreadCount(userSession));
    assertTrue(userNotificationService.unreadSnapshot(userSession).isEmpty());
    assertEquals(1, userNotificationService.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(0, cleanup.purgeRetiredNotifications());
    assertEquals(3, userNotificationRecipientRepository.count());
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("365");
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn("90");
    assertEquals(
        1, userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(1, userNotificationService.unreadCount(userSession));
    assertTrue(userNotificationService.sent(adminSession, 0, 5).isEmpty());
    assertEquals(0, cleanup.purgeRetiredNotifications());
    assertTrue(userNotificationRepository.existsById(sent.id()));
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("90");
    assertEquals(1, cleanup.purgeRetiredNotifications());
    assertFalse(userNotificationRepository.existsById(sent.id()));
    assertEquals(0, userNotificationRecipientRepository.count());
  }

  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void unlimitedAndLimitedPeriodsRemainIndependent(boolean inboxLimited, boolean historyLimited) {
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn(inboxLimited ? "180" : "");
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn(historyLimited ? "365" : "");
    Sent sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(
        inboxLimited ? 0 : 1,
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(inboxLimited ? 0 : 1, userNotificationService.unreadCount(userSession));
    assertEquals(inboxLimited ? 0 : 1, userNotificationService.unreadSnapshot(userSession).size());
    assertEquals(
        historyLimited ? 0 : 1,
        userNotificationService.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(inboxLimited && historyLimited ? 1 : 0, cleanup.purgeRetiredNotifications());
    assertEquals(
        !(inboxLimited && historyLimited), userNotificationRepository.existsById(sent.id()));
  }

  @Test
  void unlimitedInboxAlsoProtectsOldAutomaticSystemNotifications() {
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("");
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.recordSecurityEvent(
            userRepository.findById(ordinary.getId()).orElseThrow(),
            UserNotificationType.PASSWORD_CHANGED));
    UserNotificationEntity automatic = userNotificationRepository.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(
        1, userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(0, cleanup.purgeRetiredNotifications());
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn("180");
    assertEquals(1, cleanup.purgeRetiredNotifications());
  }

  @Test
  void automaticNotificationsUseOnlyTheInboxRetentionPeriod() {
    transactionTemplate.executeWithoutResult(status ->
        userNotificationService.recordSecurityEvent(
            userRepository.findById(ordinary.getId()).orElseThrow(),
            UserNotificationType.PASSWORD_CHANGED));
    UserNotificationEntity automatic = userNotificationRepository.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, cleanup.purgeRetiredNotifications());
    assertEquals(0, notificationCount());
    assertEquals(0, userNotificationRecipientRepository.count());
  }

  @Test
  void exactCutoffIsVisibleAndCleanupDeletesOnlyStrictlyOlderMessages() {
    Sent sent = broadcast();
    Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
    createdAt(sent.id(), cutoff);
    assertEquals(
        1,
        userNotificationRecipientRepository
            .inbox(
                ordinary.getId(),
                null,
                cutoff,
                org.springframework.data.domain.PageRequest.of(0, 5))
            .getTotalElements());
    assertEquals(1, userNotificationRecipientRepository.countUnread(ordinary.getId(), cutoff));
    assertEquals(1, userNotificationRecipientRepository.unreadIds(ordinary.getId(), cutoff).size());
    assertEquals(
        1,
        userNotificationRepository
            .sent(sender.getId(), cutoff, org.springframework.data.domain.PageRequest.of(0, 5))
            .getTotalElements());
    assertTrue(
        userNotificationRecipientRepository
            .inbox(
                ordinary.getId(),
                null,
                cutoff.plusSeconds(1),
                org.springframework.data.domain.PageRequest.of(0, 5))
            .isEmpty());
    assertEquals(
        0,
        transactionTemplate
            .<Integer>execute(status -> userNotificationRepository.deleteRetired(cutoff, cutoff))
            .intValue());
    assertEquals(
        1,
        transactionTemplate
            .<Integer>execute(status ->
                userNotificationRepository.deleteRetired(
                    cutoff.plusSeconds(1), cutoff.plusSeconds(1)))
            .intValue());
  }

  @Test
  void retentionMustBePositiveAndVisiblePolicyUsesSameValue() {
    UserNotificationPolicy policy = new UserNotificationPolicy(configService);
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn("0");
    assertThrows(IllegalArgumentException.class, policy::historyRetentionDays);
    when(configService.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn("365");
    assertEquals(OptionalInt.of(180), userNotificationService.retentionDays());
  }

  @Test
  void notificationSqlSchemaHasConsistentNamesAndPreservesStoredReadState() {
    transactionTemplate.executeWithoutResult(s -> {
      List columns =
          entityManager
              .createNativeQuery(
                  "select column_name from information_schema.columns where table_name='USER_NOTIFICATION_RECIPIENTS'")
              .getResultList();
      assertTrue(columns.contains("NOTIFICATION_ID"));
      List constraints =
          entityManager
              .createNativeQuery(
                  "select constraint_name from information_schema.table_constraints where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
              .getResultList();
      assertTrue(constraints.contains("UK_USER_NOTIFICATION_REQUEST"));
      assertTrue(constraints.contains("UK_USER_NOTIFICATION_RECIPIENT"));
      List indexes =
          entityManager
              .createNativeQuery(
                  "select index_name from information_schema.indexes where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
              .getResultList();
      assertTrue(indexes.contains("IDX_USER_NOTIFICATION_CREATED"));
      assertTrue(indexes.contains("IDX_USER_NOTIFICATION_INBOX"));
    });
    Instant created = Instant.parse("2026-10-18T10:00:00Z");
    transactionTemplate.executeWithoutResult(s -> {
      entityManager
          .createNativeQuery(
              "insert into user_notifications (id, request_key, type, title, body, created_at, broadcast, recipient_count, sender_id) values (900001, 'schema:notification', 'ADMIN_ANNOUNCEMENT', 'Existing title', 'Existing body', :created, false, 1, :sender)")
          .setParameter("created", created)
          .setParameter("sender", sender.getId())
          .executeUpdate();
      entityManager
          .createNativeQuery(
              "insert into user_notification_recipients (id, notification_id, user_id, read_at, relevance) values (900002, 900001, :owner, :read, 'ACTIVE')")
          .setParameter("owner", ordinary.getId())
          .setParameter("read", created)
          .executeUpdate();
    });
    Item item =
        userNotificationService.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertEquals(900002L, item.id());
    assertEquals(UserNotificationType.ADMIN_ANNOUNCEMENT, item.type());
    assertEquals("Existing title", item.title());
    assertEquals("Existing body", item.body());
    assertTrue(item.read());
    assertEquals(0, userNotificationService.unreadCount(userSession));
    Long notificationId =
        transactionTemplate.execute(s ->
            userNotificationRecipientRepository
                .findById(item.id())
                .orElseThrow()
                .getNotification()
                .getId());
    assertEquals(Long.valueOf(900001L), notificationId);
  }
}
