package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    ConfigService config() {
      return mock(ConfigService.class);
    }

    @Bean
    UserNotificationPolicy policy(ConfigService config) {
      return new UserNotificationPolicy(config);
    }

    @Bean
    SessionRegistryImpl registry() {
      return new SessionRegistryImpl();
    }

    @Bean
    SessionAdmissionService admissions(ConfigService config, SessionRegistryImpl registry) {
      return new SessionAdmissionService(config, registry);
    }
  }

  @Autowired UserNotificationService service;
  @Autowired UserNotificationRepository notifications;
  @Autowired UserNotificationRecipientRepository recipients;
  @Autowired UserRepository users;
  @Autowired UserNotificationBroadcaster events;
  @Autowired UserNotificationCleanup cleanup;
  @Autowired TransactionTemplate tx;
  @Autowired SessionRegistryImpl registry;
  @Autowired ConfigService config;
  @PersistenceContext EntityManager em;
  UserEntity sender, ordinary, guest, disabled, pending;
  MockHttpSession adminSession, userSession;
  final List<com.vaadin.flow.shared.Registration> subscriptions = new ArrayList<>();

  @BeforeEach
  void seed() {
    reset(config);
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("180");
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("365");
    tx.executeWithoutResult(
        s -> {
          recipients.deleteAll();
          notifications.deleteAll();
          users.deleteAll();
          users.flush();
          em.createQuery("delete from RoleEntity").executeUpdate();
          for (RoleType role : RoleType.values()) {
            em.persist(RoleEntity.builder().name(role).build());
          }
          sender = create("sender", RoleType.ROLE_SUPERADMIN, true);
          ordinary = create("ordinary", RoleType.ROLE_USER, true);
          guest = create("guest", RoleType.ROLE_GUEST, true);
          disabled = create("disabled", RoleType.ROLE_USER, false);
          disabled.setEnabled(false);
          users.save(disabled);
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

  UserEntity create(String name, RoleType type, boolean enabled) {
    RoleEntity role =
        em.createQuery("select r from RoleEntity r where r.name = :type", RoleEntity.class)
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
    return users.saveAndFlush(u);
  }

  MockHttpSession session(UserEntity user) {
    UserDetails principal = new CustomUserDetailsService(users).loadUserByUsername(user.getEmail());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  Preview everyone() {
    return service.preview(adminSession, new Selection(true, Set.of()));
  }

  Preview selected(Long... ids) {
    return service.preview(adminSession, new Selection(false, Set.of(ids)));
  }

  Sent broadcast() {
    return service.send(adminSession, UUID.randomUUID(), "Update", "Plain text", everyone());
  }

  long notificationCount() {
    return notifications.count();
  }

  void mutate(UserEntity user, java.util.function.Consumer<UserEntity> change) {
    tx.executeWithoutResult(s -> change.accept(users.findById(user.getId()).orElseThrow()));
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
      tx.executeWithoutResult(
          status -> {
            service.send(
                adminSession, request, "private title", "private body token=secret", preview);
            assertTrue(logs.list.isEmpty());
            status.setRollbackOnly();
          });
      assertTrue(logs.list.isEmpty());
      assertEquals(0, notifications.count());
      Sent sent =
          service.send(
              adminSession, request, "private title", "private body token=secret", preview);
      assertEquals(
          1,
          logs.list.stream()
              .filter(e -> e.getFormattedMessage().startsWith("Admin notification created:"))
              .count());
      service.send(adminSession, request, "private title", "private body token=secret", preview);
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
    assertEquals(3, recipients.count());
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(1, service.unreadCount(userSession));
    assertEquals(0, recipients.countUnread(disabled.getId(), Instant.EPOCH));
    assertEquals(0, recipients.countUnread(pending.getId(), Instant.EPOCH));
    tx.executeWithoutResult(s -> create("later", RoleType.ROLE_USER, true));
    assertEquals(3, recipients.count());
  }

  @Test
  void individualSelectionIsDistinctAndStable() {
    Preview selected = selected(guest.getId(), ordinary.getId());
    Sent sent = service.send(adminSession, UUID.randomUUID(), "Note", "Body", selected);
    assertEquals(2, sent.recipientCount());
    assertEquals(0, service.unreadCount(adminSession));
    assertEquals(selected.ids(), recipients.recipientIds(sent.id()));
  }

  @Test
  void targetsDoNotExposeCredentialsAndExcludePendingAndDisabled() {
    List<Target> targets = service.targets(adminSession);
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
    UserEntity user = tx.execute(s -> create("role" + role.name(), role, true));
    MockHttpSession session = session(user);
    assertFalse(service.canSend(session));
    assertThrows(AccessDeniedException.class, () -> service.targets(session));
    Selection selection = new Selection(true, Set.of());
    assertThrows(AccessDeniedException.class, () -> service.preview(session, selection));
    assertThrows(AccessDeniedException.class, () -> service.sent(session, 0, 5));
    UUID operationId = UUID.randomUUID();
    Preview preview2 = everyone();
    assertThrows(
        AccessDeniedException.class, () -> service.send(session, operationId, "X", "Y", preview2));
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
    assertThrows(AccessDeniedException.class, () -> service.inbox(null, ReadFilter.ALL, 0, 20));
    assertThrows(
        AccessDeniedException.class, () -> service.inbox(restricted, ReadFilter.ALL, 0, 20));
  }

  @Test
  void revokedOrInvalidatedSessionCannotRead() {
    registry.registerNewSession(userSession.getId(), ordinary.getEmail());
    registry.getSessionInformation(userSession.getId()).expireNow();
    assertThrows(AccessDeniedException.class, () -> service.unreadCount(userSession));
    adminSession.invalidate();
    assertFalse(service.canSend(adminSession));
  }

  @Test
  void freshRoleCheckRejectsOldSuperadminSessionAfterDemotion() {
    mutate(sender, u -> u.setRoleEntities(new ArrayList<>()));
    assertFalse(service.canSend(adminSession));
    assertEquals(0, service.inbox(adminSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertFalse(adminSession.isInvalid());
  }

  @Test
  void disabledSessionCannotReadOrSend() {
    mutate(sender, u -> u.setEnabled(false));
    assertFalse(service.canSend(adminSession));
    assertThrows(AccessDeniedException.class, () -> service.targets(adminSession));
  }

  @Test
  void maintenancePausesOrdinaryInboxButAllowsSuperadmin() {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertThrows(AccessDeniedException.class, () -> service.unreadCount(userSession));
    assertTrue(service.canSend(adminSession));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("unavailable"));
    assertFalse(service.canSend(adminSession));
  }

  @ParameterizedTest
  @ValueSource(ints = {5, 10, 15})
  void sentHistoryUsesSelectedPageSizeAndKeepsSenderOwnership(int size) {
    Preview checked = everyone();
    for (int index = 0; index < 17; index++) {
      service.send(adminSession, UUID.randomUUID(), "Update " + index, "Body", checked);
    }
    Page<Sent> first = service.sent(adminSession, 0, size);
    Page<Sent> second = service.sent(adminSession, 1, size);
    assertEquals(17, first.getTotalElements());
    assertEquals(size, first.getNumberOfElements());
    assertEquals(Math.min(size, 17 - size), second.getNumberOfElements());
    assertEquals(size, first.getSize());
    assertTrue(first.getContent().stream().noneMatch(second.getContent()::contains));
    assertEquals("Update 16", first.getContent().getFirst().title());
    UserEntity other =
        tx.execute(status -> create("other-superadmin", RoleType.ROLE_SUPERADMIN, true));
    assertTrue(service.sent(session(other), 0, size).isEmpty());
  }

  @Test
  void maintenanceBlocksManualReviewAndSendButKeepsSentHistoryReadable() {
    broadcast();
    Preview checked = everyone();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    assertEquals(MaintenanceService.State.ACTIVE, service.deliveryState());
    assertTrue(service.canSend(adminSession));
    assertProblem(
        Problem.DELIVERY_PAUSED,
        () -> service.preview(adminSession, new Selection(true, Set.of())));
    assertProblem(
        Problem.DELIVERY_PAUSED,
        () -> service.send(adminSession, UUID.randomUUID(), "New", "Body", checked));
    assertEquals(1, service.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(1, notificationCount());
    assertEquals(3, recipients.count());
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    service.send(adminSession, UUID.randomUUID(), "Resumed", "Body", everyone());
    assertEquals(2, notificationCount());
  }

  @Test
  void unreadableMaintenanceSettingBlocksPreviouslyReviewedManualSend() {
    Preview checked = everyone();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("Isolated configuration failure"));
    assertEquals(MaintenanceService.State.UNAVAILABLE, service.deliveryState());
    Selection selection = new Selection(true, Set.of());
    assertThrows(AccessDeniedException.class, () -> service.preview(adminSession, selection));
    UUID operationId = UUID.randomUUID();
    assertThrows(
        AccessDeniedException.class,
        () -> service.send(adminSession, operationId, "Subject", "Body", checked));
    assertEquals(0, notificationCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void recipientMutationRequiresNewPreviewAndCreatesNothing() {
    Preview preview = everyone();
    mutate(ordinary, u -> u.setEnabled(false));
    assertProblem(
        Problem.RECIPIENTS_CHANGED,
        () -> service.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
    assertEquals(0, notificationCount());
  }

  @Test
  void recipientPreviewRechecksDatabaseEvenWithAnOldManagedAccount() throws Exception {
    Preview preview = everyone();
    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      tx.executeWithoutResult(
          s -> {
            assertTrue(users.findById(ordinary.getId()).orElseThrow().isEnabled());
            try {
              executor
                  .submit(() -> mutate(ordinary, u -> u.setEnabled(false)))
                  .get(5, TimeUnit.SECONDS);
            } catch (Exception failure) {
              throw new IllegalStateException(failure);
            }
            assertProblem(
                Problem.RECIPIENTS_CHANGED,
                () -> service.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
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
        () -> service.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "1234567890"})
  void invalidDraftNeverCreatesRecipients(String value) {
    String title = value.isBlank() ? value : value.repeat(13);
    assertProblem(
        Problem.INVALID_DRAFT,
        () -> service.send(adminSession, UUID.randomUUID(), title, "body", everyone()));
    assertEquals(0, recipients.count());
  }

  @Test
  void oversizedBodyRejectedAndPlainTextStoredExactly() {
    assertProblem(
        Problem.INVALID_DRAFT,
        () -> service.send(adminSession, UUID.randomUUID(), "title", "x".repeat(4001), everyone()));
    Sent sent =
        service.send(
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
    Sent first = service.send(adminSession, key, "title", "body", preview);
    assertEquals(first, service.send(adminSession, key, "title", "body", preview));
    assertEquals(1, notificationCount());
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> service.send(adminSession, key, "changed", "body", preview));
    assertEquals(3, recipients.count());
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
            return service.send(adminSession, key, "title", "body", preview);
          };
      Future<Sent> a = executor.submit(send);
      Future<Sent> b = executor.submit(send);
      start.countDown();
      assertEquals(a.get(15, TimeUnit.SECONDS).id(), b.get(15, TimeUnit.SECONDS).id());
    }
    assertEquals(1, notificationCount());
    assertEquals(3, recipients.count());
  }

  @Test
  void senderCannotReplayOtherSendersRequest() {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    service.send(adminSession, key, "title", "body", preview);
    UserEntity other = tx.execute(s -> create("super2", RoleType.ROLE_SUPERADMIN, true));
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> service.send(session(other), key, "title", "body", preview));
  }

  @Test
  void readStatusIsAccountWideIdempotentAndDoesNotLeakOtherInbox() {
    broadcast();
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item foreign = service.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(foreign.id()));
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(1, service.unreadCount(userSession));
    service.markRead(userSession, List.of(own.id()));
    service.markRead(userSession, List.of(own.id()));
    assertEquals(0, service.unreadCount(session(ordinary)));
    assertEquals(0, service.inbox(userSession, ReadFilter.UNREAD, 0, 20).getTotalElements());
    assertTrue(service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().read());
  }

  @Test
  void dismissAffectsOnlyOwnInboxAndUnreadCountAndSurvivesNewSessions() {
    Sent sent = broadcast();
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item foreign = service.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(service.events().register(pushes::add));
    service.dismiss(userSession, foreign.id());
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(1, service.unreadCount(userSession));
    assertTrue(pushes.isEmpty());
    service.dismiss(userSession, own.id());
    service.dismiss(userSession, own.id());
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertTrue(service.inbox(session(ordinary), ReadFilter.ALL, 0, 20).isEmpty());
    assertTrue(service.inbox(userSession, ReadFilter.UNREAD, 0, 20).isEmpty());
    assertEquals(0, service.unreadCount(userSession));
    assertTrue(service.unreadSnapshot(userSession).isEmpty());
    service.markRead(userSession, List.of(own.id()));
    assertNotNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
    assertNull(recipients.findById(own.id()).orElseThrow().getReadAt());
    assertEquals(1, service.inbox(adminSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(3, service.sent(adminSession, 0, 5).getContent().getFirst().recipientCount());
    assertEquals(3, recipients.recipientIds(sent.id()).size());
  }

  @Test
  void dismissReadPreservesUnreadOtherOwnersHistoryAndIdempotency() {
    UUID key = UUID.randomUUID();
    Preview preview = everyone();
    Sent sent = service.send(adminSession, key, "Read notice", "Body", preview);
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    Item other = service.inbox(adminSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(own.id()));
    service.markRead(adminSession, List.of(other.id()));
    broadcast();
    Item unread = service.inbox(userSession, ReadFilter.UNREAD, 0, 20).getContent().getFirst();
    Sent expired = broadcast();
    Item expiredReceipt = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(expiredReceipt.id()));
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, service.readCount(userSession));
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(events.register(pushes::add));
    service.dismissRead(userSession);
    service.dismissRead(userSession);
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertEquals(0, service.readCount(session(ordinary)));
    assertEquals(
        List.of(unread.id()),
        service.inbox(userSession, ReadFilter.ALL, 0, 20).stream().map(Item::id).toList());
    assertEquals(1, service.unreadCount(userSession));
    assertEquals(1, service.readCount(adminSession));
    assertNull(recipients.findById(other.id()).orElseThrow().getDismissedAt());
    assertNull(recipients.findById(expiredReceipt.id()).orElseThrow().getDismissedAt());
    UserNotificationRecipientEntity removed = recipients.findById(own.id()).orElseThrow();
    assertNotNull(removed.getDismissedAt());
    assertNotNull(removed.getReadAt());
    assertEquals(3, service.sent(adminSession, 0, 20).getTotalElements());
    assertEquals(3, recipients.recipientIds(sent.id()).size());
    assertEquals(sent.id(), service.send(adminSession, key, "Read notice", "Body", preview).id());
    assertEquals(0, service.readCount(userSession));
    assertEquals(1, service.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
  }

  @Test
  void dismissReadRollbackAndAuthorizationDoNotRemoveEntriesOrPublish() {
    broadcast();
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(own.id()));
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(events.register(pushes::add));
    tx.executeWithoutResult(
        status -> {
          service.dismissRead(userSession);
          assertTrue(pushes.isEmpty());
          status.setRollbackOnly();
        });
    assertTrue(pushes.isEmpty());
    assertEquals(1, service.readCount(userSession));
    assertNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
    assertThrows(AccessDeniedException.class, () -> service.dismissRead(null));
    assertThrows(AccessDeniedException.class, () -> service.readCount(null));
    mutate(ordinary, user -> user.setEnabled(false));
    assertThrows(AccessDeniedException.class, () -> service.dismissRead(userSession));
    assertThrows(AccessDeniedException.class, () -> service.readCount(userSession));
    assertTrue(pushes.isEmpty());
    assertNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
  }

  @Test
  void dismissReadHonorsUnlimitedRetentionAndHandlesAutomaticNotifications() {
    Sent sent = broadcast();
    Item manual = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(manual.id()));
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    tx.executeWithoutResult(
        status ->
            service.recordSecurityEvent(
                users.findById(ordinary.getId()).orElseThrow(),
                UserNotificationType.PASSWORD_CHANGED));
    Item automatic = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(automatic.id()));
    assertEquals(1, service.readCount(userSession));
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("");
    assertEquals(2, service.readCount(userSession));
    service.dismissRead(userSession);
    assertTrue(service.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
    assertEquals(0, service.readCount(userSession));
    assertNotNull(recipients.findById(manual.id()).orElseThrow().getDismissedAt());
    assertNotNull(recipients.findById(automatic.id()).orElseThrow().getDismissedAt());
    assertEquals(2, notificationCount());
  }

  @Test
  void dismissedNotificationDoesNotReappearOnIdempotentSendRetry() {
    Preview preview = everyone();
    UUID key = UUID.randomUUID();
    Sent first = service.send(adminSession, key, "Title", "Body", preview);
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    service.dismiss(userSession, own.id());
    assertEquals(first.id(), service.send(adminSession, key, "Title", "Body", preview).id());
    assertEquals(1, notificationCount());
    assertTrue(service.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
  }

  @Test
  void dismissRollbackPublishesNothingAndRequiresValidOwnerSession() {
    broadcast();
    Item own = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    ArrayList<Set<Long>> pushes = new ArrayList<Set<Long>>();
    subscriptions.add(service.events().register(pushes::add));
    tx.executeWithoutResult(
        status -> {
          service.dismiss(userSession, own.id());
          status.setRollbackOnly();
        });
    assertTrue(pushes.isEmpty());
    assertEquals(1, service.unreadCount(userSession));
    Long ownNotificationId = own.id();
    assertThrows(AccessDeniedException.class, () -> service.dismiss(null, ownNotificationId));
    mutate(ordinary, user -> user.setEnabled(false));
    Long disabledNotificationId = own.id();
    assertThrows(
        AccessDeniedException.class, () -> service.dismiss(userSession, disabledNotificationId));
    assertNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
  }

  @Test
  void readUpdateIsVisibleInsideSamePersistenceContextWithoutClearingUnrelatedChanges() {
    broadcast();
    tx.executeWithoutResult(
        s -> {
          Item first = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
          UserNotificationRecipientEntity managed = recipients.findById(first.id()).orElseThrow();
          assertNull(managed.getReadAt());
          UserEntity user = users.findById(ordinary.getId()).orElseThrow();
          user.setFirstName("Pending");
          service.markRead(userSession, List.of(first.id()));
          assertTrue(
              service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().read());
          assertTrue(em.contains(user));
          assertEquals("Pending", user.getFirstName());
        });
  }

  @Test
  void markAllUsesSnapshotAndDoesNotMarkLaterArrivals() {
    broadcast();
    List<Long> snapshot = service.unreadSnapshot(userSession);
    broadcast();
    service.markRead(userSession, snapshot);
    assertEquals(1, service.unreadCount(userSession));
    service.markRead(userSession, List.of());
    assertEquals(1, service.unreadCount(userSession));
  }

  @Test
  void persistedNotificationsAreVisibleInNewSessionAndPagedNewestFirst() {
    broadcast();
    Sent newer = broadcast();
    MockHttpSession fresh = session(ordinary);
    Page<Item> page = service.inbox(fresh, ReadFilter.ALL, 0, 1);
    assertEquals(2, page.getTotalElements());
    assertTrue(page.hasNext());
    assertEquals(newer.createdAt(), page.getContent().getFirst().createdAt());
    assertEquals(1, service.inbox(fresh, ReadFilter.ALL, 1, 1).getNumberOfElements());
  }

  @ParameterizedTest
  @EnumSource(ReadFilter.class)
  void readFilterKeepsOwnershipDismissalRetentionAndPagination(ReadFilter filter) {
    broadcast();
    Long unreadId = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    broadcast();
    Long readId = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(readId));
    broadcast();
    Long dismissed = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(dismissed));
    service.dismiss(userSession, dismissed);
    Sent expired = broadcast();
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    Page<Item> page = service.inbox(session(ordinary), filter, 0, 1);
    assertEquals(filter == ReadFilter.ALL ? 2 : 1, page.getTotalElements());
    assertEquals(
        filter == ReadFilter.UNREAD ? unreadId : readId, page.getContent().getFirst().id());
    if (filter == ReadFilter.ALL) {
      assertTrue(page.hasNext());
      assertEquals(unreadId, service.inbox(userSession, filter, 1, 1).getContent().getFirst().id());
    } else {
      assertFalse(page.hasNext());
    }
    assertTrue(service.inbox(adminSession, ReadFilter.READ, 0, 5).isEmpty());
    assertEquals(List.of(unreadId), service.unreadSnapshot(userSession));
    service.markRead(userSession, List.of(unreadId));
    assertEquals(2, service.inbox(userSession, ReadFilter.READ, 0, 5).getTotalElements());
    assertTrue(service.inbox(userSession, ReadFilter.UNREAD, 0, 5).isEmpty());
  }

  @Test
  void readFilterSelectsBeforeApplyingFiveItemPreviewLimit() {
    for (int i = 0; i < 7; i++) {
      broadcast();
    }
    List<Item> all = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent();
    service.markRead(userSession, all.subList(1, 7).stream().map(Item::id).toList());
    Page<Item> first = service.inbox(userSession, ReadFilter.READ, 0, 5);
    assertEquals(6, first.getTotalElements());
    assertEquals(5, first.getNumberOfElements());
    assertTrue(first.hasNext());
    assertTrue(first.stream().allMatch(Item::read));
    assertEquals(1, service.inbox(userSession, ReadFilter.READ, 1, 5).getNumberOfElements());
    assertEquals(
        all.getFirst().id(),
        service.inbox(userSession, ReadFilter.UNREAD, 0, 5).getContent().getFirst().id());
    assertThrows(AccessDeniedException.class, () -> service.inbox(null, ReadFilter.READ, 0, 5));
  }

  @Test
  void inboxSearchFiltersBeforePagingAndExcludesForeignDismissedExpiredEntries() {
    service.send(adminSession, UUID.randomUUID(), "Alpha report", "First body", everyone());
    Long older = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(older));
    service.send(adminSession, UUID.randomUUID(), "Second report", "ALPHA in the body", everyone());
    Long newer = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.send(
        adminSession, UUID.randomUUID(), "Alpha private", "Foreign body", selected(sender.getId()));
    service.send(adminSession, UUID.randomUUID(), "Alpha removed", "Removed body", everyone());
    service.dismiss(
        userSession,
        service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id());
    Sent expired =
        service.send(adminSession, UUID.randomUUID(), "Alpha old", "Expired body", everyone());
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(2, service.inboxCount(userSession));
    Page<Item> first =
        service.searchInbox(session(ordinary), ReadFilter.ALL, "  aLpHa  ", Set.of(), 0, 1);
    assertEquals(2, first.getTotalElements());
    assertEquals(newer, first.getContent().getFirst().id());
    assertTrue(first.hasNext());
    assertEquals(
        older,
        service
            .searchInbox(userSession, ReadFilter.ALL, "alpha", Set.of(), 1, 1)
            .getContent()
            .getFirst()
            .id());
    Page<Item> unread =
        service.searchInbox(userSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 20);
    assertEquals(List.of(newer), unread.stream().map(Item::id).toList());
    assertEquals(
        List.of(older),
        service.searchInbox(userSession, ReadFilter.READ, "alpha", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    assertEquals(
        2,
        service.searchInbox(userSession, ReadFilter.ALL, "  ", Set.of(), 0, 20).getTotalElements());
    assertThrows(
        AccessDeniedException.class,
        () -> service.searchInbox(null, ReadFilter.ALL, "alpha", Set.of(), 0, 20));
    assertThrows(AccessDeniedException.class, () -> service.inboxCount(null));
    String longSearchText = "x".repeat(121);
    Set<UserNotificationType> notificationTypes = Set.of();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.searchInbox(
                userSession, ReadFilter.ALL, longSearchText, notificationTypes, 0, 20));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.searchInbox(userSession, ReadFilter.ALL, "alpha", Set.of(), -1, 20));
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("");
    assertEquals(3, service.inboxCount(userSession));
    assertEquals(
        3,
        service
            .searchInbox(userSession, ReadFilter.ALL, "alpha", Set.of(), 0, 20)
            .getTotalElements());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(ReadFilter.class)
  void inboxSearchReadStatesAreAppliedBeforePagingAndForEmptyQueries(ReadFilter filter) {
    service.send(adminSession, UUID.randomUUID(), "Alpha first", "Body", everyone());
    Long first = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.send(adminSession, UUID.randomUUID(), "Alpha second", "Body", everyone());
    Long second = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(first, second));
    service.send(adminSession, UUID.randomUUID(), "Alpha third", "Body", everyone());
    Long third = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    List<Long> expected =
        switch (filter) {
          case ALL -> List.of(third, second, first);
          case READ -> List.of(second, first);
          case UNREAD -> List.of(third);
        };
    assertEquals(
        expected,
        service.searchInbox(userSession, filter, "alpha", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    assertEquals(
        expected,
        service.searchInbox(userSession, filter, "  ", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    Page<Item> firstPage = service.searchInbox(userSession, filter, "alpha", Set.of(), 0, 1);
    assertEquals(expected.size(), firstPage.getTotalElements());
    assertEquals(expected.getFirst(), firstPage.getContent().getFirst().id());
    if (expected.size() > 1) {
      assertEquals(
          expected.get(1),
          service
              .searchInbox(userSession, filter, "alpha", Set.of(), 1, 1)
              .getContent()
              .getFirst()
              .id());
    }
    assertThrows(
        AccessDeniedException.class,
        () -> service.searchInbox(null, filter, "alpha", Set.of(), 0, 20));
  }

  @ParameterizedTest
  @ValueSource(strings = {"%", "_", "!"})
  void inboxSearchTreatsLikeMetacharactersLiterally(String literal) {
    service.send(adminSession, UUID.randomUUID(), "Needle " + literal, "Exact match", everyone());
    Long expected = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst().id();
    broadcast();
    Page<Item> found = service.searchInbox(userSession, ReadFilter.ALL, literal, Set.of(), 0, 20);
    assertEquals(List.of(expected), found.stream().map(Item::id).toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxSearchMatchesLocalizedSystemTitlesBodiesAndSources(String language) {
    CustomI18NProvider provider = new de.derpeterson.app.i18n.CustomI18NProvider();
    MessageProperties texts =
        de.derpeterson.app.i18n.MessageProperties.forLocale(
            provider, Locale.forLanguageTag(language));
    tx.executeWithoutResult(
        status ->
            service.recordSecurityEvent(
                users.findById(ordinary.getId()).orElseThrow(),
                UserNotificationType.PASSWORD_CHANGED));
    service.send(
        adminSession, UUID.randomUUID(), "Original notice", "Original manual body", everyone());
    String title = texts.getNotificationsPasswordChangedTitle();
    Set<UserNotificationType> types =
        de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, title);
    assertTrue(types.contains(UserNotificationType.PASSWORD_CHANGED));
    assertEquals(
        1,
        service.searchInbox(userSession, ReadFilter.ALL, title, types, 0, 20).getTotalElements());
    String body = texts.getNotificationsPasswordChangedBody().substring(0, 25);
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, body);
    assertEquals(
        1, service.searchInbox(userSession, ReadFilter.ALL, body, types, 0, 20).getTotalElements());
    String system = texts.getNotificationsSourceSystem();
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, system);
    assertEquals(
        1,
        service.searchInbox(userSession, ReadFilter.ALL, system, types, 0, 20).getTotalElements());
    assertTrue(service.searchInbox(adminSession, ReadFilter.ALL, system, types, 0, 20).isEmpty());
    String administration = texts.getNotificationsSourceAdministration();
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, administration);
    assertEquals(
        UserNotificationType.ADMIN_ANNOUNCEMENT,
        service
            .searchInbox(userSession, ReadFilter.ALL, administration, types, 0, 20)
            .getContent()
            .getFirst()
            .type());
    assertTrue(de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, "   ").isEmpty());
  }

  @Test
  void paginationBoundsAreChecked() {
    assertThrows(
        IllegalArgumentException.class, () -> service.inbox(userSession, ReadFilter.ALL, -1, 20));
    assertThrows(
        IllegalArgumentException.class, () -> service.inbox(userSession, ReadFilter.ALL, 0, 101));
    assertThrows(
        IllegalArgumentException.class, () -> service.inbox(userSession, ReadFilter.ALL, 0, 0));
  }

  @Test
  void eventsPublishOnlyAfterCommitAndNotOnRollback() {
    AtomicInteger count = new AtomicInteger();
    subscriptions.add(
        events.register(
            ids -> {
              assertTrue(ids.contains(ordinary.getId()));
              count.incrementAndGet();
            }));
    tx.executeWithoutResult(
        s -> {
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
        events.register(
            ids -> {
              throw new IllegalStateException("detached UI");
            }));
    AtomicInteger count = new AtomicInteger();
    subscriptions.add(events.register(ids -> count.incrementAndGet()));
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
        IllegalTransactionStateException.class, () -> service.recordSecurityEvent(ordinary, type));
    tx.executeWithoutResult(
        s -> service.recordSecurityEvent(users.findById(ordinary.getId()).orElseThrow(), type));
    Item item = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertEquals(type, item.type());
    assertNull(item.title());
    assertNull(item.body());
    assertEquals(0, service.sent(adminSession, 0, 5).getTotalElements());
  }

  @Test
  void rolledBackSecurityEventDoesNotClaimSuccess() {
    tx.executeWithoutResult(
        s -> {
          service.recordSecurityEvent(ordinary, UserNotificationType.PASSWORD_CHANGED);
          s.setRollbackOnly();
        });
    assertEquals(0, notificationCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void deletingRecipientKeepsSharedNotificationForOthersAndDeletingSenderSetsNull() {
    Sent sent = broadcast();
    tx.executeWithoutResult(
        s -> {
          users.deleteById(ordinary.getId());
          users.flush();
        });
    assertEquals(2, recipients.count());
    assertTrue(notifications.existsById(sent.id()));
    tx.executeWithoutResult(
        s -> {
          users.deleteById(sender.getId());
          users.flush();
        });
    assertEquals(1, recipients.count());
    tx.executeWithoutResult(
        s -> assertNull(notifications.findById(sent.id()).orElseThrow().getSender()));
    assertEquals(1, service.inbox(session(guest), ReadFilter.ALL, 0, 20).getTotalElements());
  }

  @Test
  void cleanupRemovesReadAndUnreadExpiredNotificationsAndOrphanContents() {
    Sent old = broadcast();
    Sent fresh = broadcast();
    // Unveränderliche Fachspalten nur als künstliche Altstruktur per SQL altern lassen.
    tx.executeWithoutResult(
        s ->
            em.createNativeQuery("update user_notifications set created_at = :old where id = :id")
                .setParameter("old", Instant.now().minus(366, java.time.temporal.ChronoUnit.DAYS))
                .setParameter("id", old.id())
                .executeUpdate());
    assertEquals(1, cleanup.cleanup());
    assertFalse(notifications.existsById(old.id()));
    assertTrue(notifications.existsById(fresh.id()));
    assertEquals(3, recipients.count());
    tx.executeWithoutResult(s -> recipients.deleteAll());
    assertEquals(0, cleanup.cleanup());
    assertTrue(notifications.existsById(fresh.id()));
  }

  private void createdAt(Long id, Instant created) {
    tx.executeWithoutResult(
        status ->
            em.createNativeQuery(
                    "update user_notifications set created_at = :created where id = :id")
                .setParameter("created", created)
                .setParameter("id", id)
                .executeUpdate());
  }

  @Test
  void inboxAndAdminHistoryExpireIndependentlyAndReadLiveConfiguration() {
    Sent sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertTrue(service.inbox(userSession, ReadFilter.ALL, 0, 20).isEmpty());
    assertTrue(service.inbox(userSession, ReadFilter.UNREAD, 0, 20).isEmpty());
    assertEquals(0, service.unreadCount(userSession));
    assertTrue(service.unreadSnapshot(userSession).isEmpty());
    assertEquals(1, service.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(0, cleanup.cleanup());
    assertEquals(3, recipients.count());
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("365");
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("90");
    assertEquals(1, service.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(1, service.unreadCount(userSession));
    assertTrue(service.sent(adminSession, 0, 5).isEmpty());
    assertEquals(0, cleanup.cleanup());
    assertTrue(notifications.existsById(sent.id()));
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("90");
    assertEquals(1, cleanup.cleanup());
    assertFalse(notifications.existsById(sent.id()));
    assertEquals(0, recipients.count());
  }

  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void unlimitedAndLimitedPeriodsRemainIndependent(boolean inboxLimited, boolean historyLimited) {
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS))
        .thenReturn(inboxLimited ? "180" : "");
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
        .thenReturn(historyLimited ? "365" : "");
    Sent sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(
        inboxLimited ? 0 : 1, service.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(inboxLimited ? 0 : 1, service.unreadCount(userSession));
    assertEquals(inboxLimited ? 0 : 1, service.unreadSnapshot(userSession).size());
    assertEquals(historyLimited ? 0 : 1, service.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(inboxLimited && historyLimited ? 1 : 0, cleanup.cleanup());
    assertEquals(!(inboxLimited && historyLimited), notifications.existsById(sent.id()));
  }

  @Test
  void unlimitedInboxAlsoProtectsOldAutomaticSystemNotifications() {
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("");
    tx.executeWithoutResult(
        status ->
            service.recordSecurityEvent(
                users.findById(ordinary.getId()).orElseThrow(),
                UserNotificationType.PASSWORD_CHANGED));
    UserNotificationEntity automatic = notifications.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, service.inbox(userSession, ReadFilter.ALL, 0, 20).getTotalElements());
    assertEquals(0, cleanup.cleanup());
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("180");
    assertEquals(1, cleanup.cleanup());
  }

  @Test
  void automaticNotificationsUseOnlyTheInboxRetentionPeriod() {
    tx.executeWithoutResult(
        status ->
            service.recordSecurityEvent(
                users.findById(ordinary.getId()).orElseThrow(),
                UserNotificationType.PASSWORD_CHANGED));
    UserNotificationEntity automatic = notifications.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, cleanup.cleanup());
    assertEquals(0, notificationCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void exactCutoffIsVisibleAndCleanupDeletesOnlyStrictlyOlderMessages() {
    Sent sent = broadcast();
    Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
    createdAt(sent.id(), cutoff);
    assertEquals(
        1,
        recipients
            .inbox(
                ordinary.getId(),
                null,
                cutoff,
                org.springframework.data.domain.PageRequest.of(0, 5))
            .getTotalElements());
    assertEquals(1, recipients.countUnread(ordinary.getId(), cutoff));
    assertEquals(1, recipients.unreadIds(ordinary.getId(), cutoff).size());
    assertEquals(
        1,
        notifications
            .sent(sender.getId(), cutoff, org.springframework.data.domain.PageRequest.of(0, 5))
            .getTotalElements());
    assertTrue(
        recipients
            .inbox(
                ordinary.getId(),
                null,
                cutoff.plusSeconds(1),
                org.springframework.data.domain.PageRequest.of(0, 5))
            .isEmpty());
    assertEquals(
        0, tx.<Integer>execute(status -> notifications.deleteRetired(cutoff, cutoff)).intValue());
    assertEquals(
        1,
        tx.<Integer>execute(
                status -> notifications.deleteRetired(cutoff.plusSeconds(1), cutoff.plusSeconds(1)))
            .intValue());
  }

  @Test
  void retentionMustBePositiveAndVisiblePolicyUsesSameValue() {
    UserNotificationPolicy policy = new UserNotificationPolicy(config);
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("0");
    assertThrows(IllegalArgumentException.class, policy::historyRetentionDays);
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("365");
    assertEquals(OptionalInt.of(180), service.retentionDays());
  }

  @Test
  void notificationSqlSchemaHasConsistentNamesAndPreservesStoredReadState() {
    tx.executeWithoutResult(
        s -> {
          List columns =
              em.createNativeQuery(
                      "select column_name from information_schema.columns where table_name='USER_NOTIFICATION_RECIPIENTS'")
                  .getResultList();
          assertTrue(columns.contains("NOTIFICATION_ID"));
          List constraints =
              em.createNativeQuery(
                      "select constraint_name from information_schema.table_constraints where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
                  .getResultList();
          assertTrue(constraints.contains("UK_USER_NOTIFICATION_REQUEST"));
          assertTrue(constraints.contains("UK_USER_NOTIFICATION_RECIPIENT"));
          List indexes =
              em.createNativeQuery(
                      "select index_name from information_schema.indexes where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
                  .getResultList();
          assertTrue(indexes.contains("IDX_USER_NOTIFICATION_CREATED"));
          assertTrue(indexes.contains("IDX_USER_NOTIFICATION_INBOX"));
        });
    Instant created = Instant.parse("2026-10-18T10:00:00Z");
    tx.executeWithoutResult(
        s -> {
          em.createNativeQuery(
                  "insert into user_notifications (id, request_key, type, title, body, created_at, broadcast, recipient_count, sender_id) values (900001, 'schema:notification', 'ADMIN_ANNOUNCEMENT', 'Existing title', 'Existing body', :created, false, 1, :sender)")
              .setParameter("created", created)
              .setParameter("sender", sender.getId())
              .executeUpdate();
          em.createNativeQuery(
                  "insert into user_notification_recipients (id, notification_id, user_id, read_at) values (900002, 900001, :owner, :read)")
              .setParameter("owner", ordinary.getId())
              .setParameter("read", created)
              .executeUpdate();
        });
    Item item = service.inbox(userSession, ReadFilter.ALL, 0, 20).getContent().getFirst();
    assertEquals(900002L, item.id());
    assertEquals(UserNotificationType.ADMIN_ANNOUNCEMENT, item.type());
    assertEquals("Existing title", item.title());
    assertEquals("Existing body", item.body());
    assertTrue(item.read());
    assertEquals(0, service.unreadCount(userSession));
    Long notificationId =
        tx.execute(s -> recipients.findById(item.id()).orElseThrow().getNotification().getId());
    assertEquals(Long.valueOf(900001L), notificationId);
  }
}
