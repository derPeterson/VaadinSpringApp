package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.scheduler.UserNotificationCleanup;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.UserNotificationService.*;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
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
          for (var role : RoleType.values()) em.persist(RoleEntity.builder().name(role).build());
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
    var role =
        em.createQuery("select r from RoleEntity r where r.name = :type", RoleEntity.class)
            .setParameter("type", type)
            .getSingleResult();
    var u =
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
    var principal = new CustomUserDetailsService(users).loadUserByUsername(user.getEmail());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
    var session = new MockHttpSession();
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
  void broadcastSnapshotsEnabledAccountsIncludingOfflineGuestAndSender() {
    var sent = broadcast();
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
    var selected = selected(guest.getId(), ordinary.getId());
    var sent = service.send(adminSession, UUID.randomUUID(), "Note", "Body", selected);
    assertEquals(2, sent.recipientCount());
    assertEquals(0, service.unreadCount(adminSession));
    assertEquals(selected.ids(), recipients.recipientIds(sent.id()));
  }

  @Test
  void targetsDoNotExposeCredentialsAndExcludePendingAndDisabled() {
    var targets = service.targets(adminSession);
    assertEquals(3, targets.size());
    assertEquals(
        List.of("id", "name", "email"),
        Arrays.stream(Target.class.getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName)
            .toList());
  }

  @Test
  void emptySelectionRejected() {
    assertProblem(Problem.NO_RECIPIENTS, () -> selected());
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
    var user = tx.execute(s -> create("role" + role.name(), role, true));
    var session = session(user);
    assertFalse(service.canSend(session));
    assertThrows(AccessDeniedException.class, () -> service.targets(session));
    assertThrows(
        AccessDeniedException.class, () -> service.preview(session, new Selection(true, Set.of())));
    assertThrows(AccessDeniedException.class, () -> service.sent(session, 0, 5));
    assertThrows(
        AccessDeniedException.class,
        () -> service.send(session, UUID.randomUUID(), "X", "Y", everyone()));
    assertEquals(0, notificationCount());
  }

  @Test
  void threadAuthenticationDoesNotAuthorizeAnonymousOrRestrictedSession() {
    var context =
        (org.springframework.security.core.context.SecurityContext)
            adminSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    SecurityContextHolder.setContext(context);
    var restricted = new MockHttpSession();
    restricted.setAttribute("sessionRecoveryUserId", sender.getId());
    assertThrows(AccessDeniedException.class, () -> service.inbox(null, false, 0, 20));
    assertThrows(AccessDeniedException.class, () -> service.inbox(restricted, false, 0, 20));
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
    assertEquals(0, service.inbox(adminSession, false, 0, 20).getTotalElements());
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
    var checked = everyone();
    for (int index = 0; index < 17; index++) {
      service.send(adminSession, UUID.randomUUID(), "Update " + index, "Body", checked);
    }
    var first = service.sent(adminSession, 0, size);
    var second = service.sent(adminSession, 1, size);
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
    var checked = everyone();
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
    var checked = everyone();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("Isolated configuration failure"));
    assertEquals(MaintenanceService.State.UNAVAILABLE, service.deliveryState());
    assertThrows(
        AccessDeniedException.class,
        () -> service.preview(adminSession, new Selection(true, Set.of())));
    assertThrows(
        AccessDeniedException.class,
        () -> service.send(adminSession, UUID.randomUUID(), "Subject", "Body", checked));
    assertEquals(0, notificationCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void recipientMutationRequiresNewPreviewAndCreatesNothing() {
    var preview = everyone();
    mutate(ordinary, u -> u.setEnabled(false));
    assertProblem(
        Problem.RECIPIENTS_CHANGED,
        () -> service.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
    assertEquals(0, notificationCount());
  }

  @Test
  void recipientPreviewRechecksDatabaseEvenWithAnOldManagedAccount() throws Exception {
    var preview = everyone();
    try (var executor = Executors.newSingleThreadExecutor()) {
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
    var preview = everyone();
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
    var sent =
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
    var key = UUID.randomUUID();
    var preview = everyone();
    var first = service.send(adminSession, key, "title", "body", preview);
    assertEquals(first, service.send(adminSession, key, "title", "body", preview));
    assertEquals(1, notificationCount());
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> service.send(adminSession, key, "changed", "body", preview));
    assertEquals(3, recipients.count());
  }

  @Test
  void concurrentReplayCommitsExactlyOneContentAndOneRecipientSet() throws Exception {
    var key = UUID.randomUUID();
    var preview = everyone();
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Sent> send =
          () -> {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return service.send(adminSession, key, "title", "body", preview);
          };
      var a = executor.submit(send);
      var b = executor.submit(send);
      start.countDown();
      assertEquals(a.get(15, TimeUnit.SECONDS).id(), b.get(15, TimeUnit.SECONDS).id());
    }
    assertEquals(1, notificationCount());
    assertEquals(3, recipients.count());
  }

  @Test
  void senderCannotReplayOtherSendersRequest() {
    var key = UUID.randomUUID();
    var preview = everyone();
    service.send(adminSession, key, "title", "body", preview);
    var other = tx.execute(s -> create("super2", RoleType.ROLE_SUPERADMIN, true));
    assertProblem(
        Problem.REQUEST_CONFLICT,
        () -> service.send(session(other), key, "title", "body", preview));
  }

  @Test
  void readStatusIsAccountWideIdempotentAndDoesNotLeakOtherInbox() {
    broadcast();
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    var foreign = service.inbox(adminSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(foreign.id()));
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(1, service.unreadCount(userSession));
    service.markRead(userSession, List.of(own.id()));
    service.markRead(userSession, List.of(own.id()));
    assertEquals(0, service.unreadCount(session(ordinary)));
    assertEquals(0, service.inbox(userSession, true, 0, 20).getTotalElements());
    assertTrue(service.inbox(userSession, false, 0, 20).getContent().getFirst().read());
  }

  @Test
  void dismissAffectsOnlyOwnInboxAndUnreadCountAndSurvivesNewSessions() {
    var sent = broadcast();
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    var foreign = service.inbox(adminSession, false, 0, 20).getContent().getFirst();
    var pushes = new ArrayList<Set<Long>>();
    subscriptions.add(service.events().register(pushes::add));
    service.dismiss(userSession, foreign.id());
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(1, service.unreadCount(userSession));
    assertTrue(pushes.isEmpty());
    service.dismiss(userSession, own.id());
    service.dismiss(userSession, own.id());
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertTrue(service.inbox(session(ordinary), false, 0, 20).isEmpty());
    assertTrue(service.inbox(userSession, true, 0, 20).isEmpty());
    assertEquals(0, service.unreadCount(userSession));
    assertTrue(service.unreadSnapshot(userSession).isEmpty());
    service.markRead(userSession, List.of(own.id()));
    assertNotNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
    assertNull(recipients.findById(own.id()).orElseThrow().getReadAt());
    assertEquals(1, service.inbox(adminSession, false, 0, 20).getTotalElements());
    assertEquals(1, service.unreadCount(adminSession));
    assertEquals(3, service.sent(adminSession, 0, 5).getContent().getFirst().recipientCount());
    assertEquals(3, recipients.recipientIds(sent.id()).size());
  }

  @Test
  void dismissReadPreservesUnreadOtherOwnersHistoryAndIdempotency() {
    var key = UUID.randomUUID();
    var preview = everyone();
    var sent = service.send(adminSession, key, "Read notice", "Body", preview);
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    var other = service.inbox(adminSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(own.id()));
    service.markRead(adminSession, List.of(other.id()));
    broadcast();
    var unread = service.inbox(userSession, true, 0, 20).getContent().getFirst();
    var expired = broadcast();
    var expiredReceipt = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(expiredReceipt.id()));
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, service.readCount(userSession));
    var pushes = new ArrayList<Set<Long>>();
    subscriptions.add(events.register(pushes::add));
    service.dismissRead(userSession);
    service.dismissRead(userSession);
    assertEquals(List.of(Set.of(ordinary.getId())), pushes);
    assertEquals(0, service.readCount(session(ordinary)));
    assertEquals(
        List.of(unread.id()),
        service.inbox(userSession, false, 0, 20).stream().map(Item::id).toList());
    assertEquals(1, service.unreadCount(userSession));
    assertEquals(1, service.readCount(adminSession));
    assertNull(recipients.findById(other.id()).orElseThrow().getDismissedAt());
    assertNull(recipients.findById(expiredReceipt.id()).orElseThrow().getDismissedAt());
    var removed = recipients.findById(own.id()).orElseThrow();
    assertNotNull(removed.getDismissedAt());
    assertNotNull(removed.getReadAt());
    assertEquals(3, service.sent(adminSession, 0, 20).getTotalElements());
    assertEquals(3, recipients.recipientIds(sent.id()).size());
    assertEquals(sent.id(), service.send(adminSession, key, "Read notice", "Body", preview).id());
    assertEquals(0, service.readCount(userSession));
    assertEquals(1, service.inbox(userSession, false, 0, 20).getTotalElements());
  }

  @Test
  void dismissReadRollbackAndAuthorizationDoNotRemoveEntriesOrPublish() {
    broadcast();
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(own.id()));
    var pushes = new ArrayList<Set<Long>>();
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
    var sent = broadcast();
    var manual = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(manual.id()));
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    tx.executeWithoutResult(
        status ->
            service.recordSecurityEvent(
                users.findById(ordinary.getId()).orElseThrow(),
                UserNotificationType.PASSWORD_CHANGED));
    var automatic = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    service.markRead(userSession, List.of(automatic.id()));
    assertEquals(1, service.readCount(userSession));
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("");
    assertEquals(2, service.readCount(userSession));
    service.dismissRead(userSession);
    assertTrue(service.inbox(userSession, false, 0, 20).isEmpty());
    assertEquals(0, service.readCount(userSession));
    assertNotNull(recipients.findById(manual.id()).orElseThrow().getDismissedAt());
    assertNotNull(recipients.findById(automatic.id()).orElseThrow().getDismissedAt());
    assertEquals(2, notificationCount());
  }

  @Test
  void dismissedNotificationDoesNotReappearOnIdempotentSendRetry() {
    var preview = everyone();
    var key = UUID.randomUUID();
    var first = service.send(adminSession, key, "Title", "Body", preview);
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    service.dismiss(userSession, own.id());
    assertEquals(first.id(), service.send(adminSession, key, "Title", "Body", preview).id());
    assertEquals(1, notificationCount());
    assertTrue(service.inbox(userSession, false, 0, 20).isEmpty());
  }

  @Test
  void dismissRollbackPublishesNothingAndRequiresValidOwnerSession() {
    broadcast();
    var own = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    var pushes = new ArrayList<Set<Long>>();
    subscriptions.add(service.events().register(pushes::add));
    tx.executeWithoutResult(
        status -> {
          service.dismiss(userSession, own.id());
          status.setRollbackOnly();
        });
    assertTrue(pushes.isEmpty());
    assertEquals(1, service.unreadCount(userSession));
    assertThrows(AccessDeniedException.class, () -> service.dismiss(null, own.id()));
    mutate(ordinary, user -> user.setEnabled(false));
    assertThrows(AccessDeniedException.class, () -> service.dismiss(userSession, own.id()));
    assertNull(recipients.findById(own.id()).orElseThrow().getDismissedAt());
  }

  @Test
  void readUpdateIsVisibleInsideSamePersistenceContextWithoutClearingUnrelatedChanges() {
    broadcast();
    tx.executeWithoutResult(
        s -> {
          var first = service.inbox(userSession, false, 0, 20).getContent().getFirst();
          var managed = recipients.findById(first.id()).orElseThrow();
          assertNull(managed.getReadAt());
          var user = users.findById(ordinary.getId()).orElseThrow();
          user.setFirstName("Pending");
          service.markRead(userSession, List.of(first.id()));
          assertTrue(service.inbox(userSession, false, 0, 20).getContent().getFirst().read());
          assertTrue(em.contains(user));
          assertEquals("Pending", user.getFirstName());
        });
  }

  @Test
  void markAllUsesSnapshotAndDoesNotMarkLaterArrivals() {
    broadcast();
    var snapshot = service.unreadSnapshot(userSession);
    broadcast();
    service.markRead(userSession, snapshot);
    assertEquals(1, service.unreadCount(userSession));
    service.markRead(userSession, List.of());
    assertEquals(1, service.unreadCount(userSession));
  }

  @Test
  void persistedNotificationsAreVisibleInNewSessionAndPagedNewestFirst() {
    broadcast();
    var newer = broadcast();
    var fresh = session(ordinary);
    var page = service.inbox(fresh, false, 0, 1);
    assertEquals(2, page.getTotalElements());
    assertTrue(page.hasNext());
    assertEquals(newer.createdAt(), page.getContent().getFirst().createdAt());
    assertEquals(1, service.inbox(fresh, false, 1, 1).getNumberOfElements());
  }

  @ParameterizedTest
  @EnumSource(ReadFilter.class)
  void readFilterKeepsOwnershipDismissalRetentionAndPagination(ReadFilter filter) {
    broadcast();
    var unreadId = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    broadcast();
    var readId = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(readId));
    broadcast();
    var dismissed = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(dismissed));
    service.dismiss(userSession, dismissed);
    var expired = broadcast();
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    var page = service.inbox(session(ordinary), filter, 0, 1);
    assertEquals(filter == ReadFilter.ALL ? 2 : 1, page.getTotalElements());
    assertEquals(
        filter == ReadFilter.UNREAD ? unreadId : readId, page.getContent().getFirst().id());
    if (filter == ReadFilter.ALL) {
      assertTrue(page.hasNext());
      assertEquals(unreadId, service.inbox(userSession, filter, 1, 1).getContent().getFirst().id());
    } else assertFalse(page.hasNext());
    assertTrue(service.inbox(adminSession, ReadFilter.READ, 0, 5).isEmpty());
    assertEquals(List.of(unreadId), service.unreadSnapshot(userSession));
    service.markRead(userSession, List.of(unreadId));
    assertEquals(2, service.inbox(userSession, ReadFilter.READ, 0, 5).getTotalElements());
    assertTrue(service.inbox(userSession, ReadFilter.UNREAD, 0, 5).isEmpty());
  }

  @Test
  void readFilterSelectsBeforeApplyingFiveItemPreviewLimit() {
    for (int i = 0; i < 7; i++) broadcast();
    var all = service.inbox(userSession, false, 0, 20).getContent();
    service.markRead(userSession, all.subList(1, 7).stream().map(Item::id).toList());
    var first = service.inbox(userSession, ReadFilter.READ, 0, 5);
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
    var older = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(older));
    service.send(adminSession, UUID.randomUUID(), "Second report", "ALPHA in the body", everyone());
    var newer = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.send(
        adminSession, UUID.randomUUID(), "Alpha private", "Foreign body", selected(sender.getId()));
    service.send(adminSession, UUID.randomUUID(), "Alpha removed", "Removed body", everyone());
    service.dismiss(
        userSession, service.inbox(userSession, false, 0, 20).getContent().getFirst().id());
    var expired =
        service.send(adminSession, UUID.randomUUID(), "Alpha old", "Expired body", everyone());
    createdAt(expired.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(2, service.inboxCount(userSession));
    var first = service.searchInbox(session(ordinary), false, "  aLpHa  ", Set.of(), 0, 1);
    assertEquals(2, first.getTotalElements());
    assertEquals(newer, first.getContent().getFirst().id());
    assertTrue(first.hasNext());
    assertEquals(
        older,
        service
            .searchInbox(userSession, false, "alpha", Set.of(), 1, 1)
            .getContent()
            .getFirst()
            .id());
    var unread = service.searchInbox(userSession, true, "alpha", Set.of(), 0, 20);
    assertEquals(List.of(newer), unread.stream().map(Item::id).toList());
    assertEquals(
        List.of(older),
        service.searchInbox(userSession, ReadFilter.READ, "alpha", Set.of(), 0, 20).stream()
            .map(Item::id)
            .toList());
    assertEquals(
        2, service.searchInbox(userSession, false, "  ", Set.of(), 0, 20).getTotalElements());
    assertThrows(
        AccessDeniedException.class,
        () -> service.searchInbox(null, false, "alpha", Set.of(), 0, 20));
    assertThrows(AccessDeniedException.class, () -> service.inboxCount(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.searchInbox(userSession, false, "x".repeat(121), Set.of(), 0, 20));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.searchInbox(userSession, false, "alpha", Set.of(), -1, 20));
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("");
    assertEquals(3, service.inboxCount(userSession));
    assertEquals(
        3, service.searchInbox(userSession, false, "alpha", Set.of(), 0, 20).getTotalElements());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(ReadFilter.class)
  void inboxSearchReadStatesAreAppliedBeforePagingAndForEmptyQueries(ReadFilter filter) {
    service.send(adminSession, UUID.randomUUID(), "Alpha first", "Body", everyone());
    var first = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.send(adminSession, UUID.randomUUID(), "Alpha second", "Body", everyone());
    var second = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    service.markRead(userSession, List.of(first, second));
    service.send(adminSession, UUID.randomUUID(), "Alpha third", "Body", everyone());
    var third = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    var expected =
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
    var firstPage = service.searchInbox(userSession, filter, "alpha", Set.of(), 0, 1);
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
    var expected = service.inbox(userSession, false, 0, 20).getContent().getFirst().id();
    broadcast();
    var found = service.searchInbox(userSession, false, literal, Set.of(), 0, 20);
    assertEquals(List.of(expected), found.stream().map(Item::id).toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxSearchMatchesLocalizedSystemTitlesBodiesAndSources(String language) {
    var provider = new de.derpeterson.app.i18n.CustomI18NProvider();
    var texts =
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
    var types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, title);
    assertTrue(types.contains(UserNotificationType.PASSWORD_CHANGED));
    assertEquals(
        1, service.searchInbox(userSession, false, title, types, 0, 20).getTotalElements());
    String body = texts.getNotificationsPasswordChangedBody().substring(0, 25);
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, body);
    assertEquals(1, service.searchInbox(userSession, false, body, types, 0, 20).getTotalElements());
    String system = texts.getNotificationsSourceSystem();
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, system);
    assertEquals(
        1, service.searchInbox(userSession, false, system, types, 0, 20).getTotalElements());
    assertTrue(service.searchInbox(adminSession, false, system, types, 0, 20).isEmpty());
    String administration = texts.getNotificationsSourceAdministration();
    types = de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, administration);
    assertEquals(
        UserNotificationType.ADMIN_ANNOUNCEMENT,
        service
            .searchInbox(userSession, false, administration, types, 0, 20)
            .getContent()
            .getFirst()
            .type());
    assertTrue(de.derpeterson.app.i18n.UserNotificationTexts.matchingTypes(texts, "   ").isEmpty());
  }

  @Test
  void paginationBoundsAreChecked() {
    assertThrows(IllegalArgumentException.class, () -> service.inbox(userSession, false, -1, 20));
    assertThrows(IllegalArgumentException.class, () -> service.inbox(userSession, false, 0, 101));
    assertThrows(IllegalArgumentException.class, () -> service.inbox(userSession, false, 0, 0));
  }

  @Test
  void eventsPublishOnlyAfterCommitAndNotOnRollback() {
    var count = new AtomicInteger();
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
    var count = new AtomicInteger();
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
    var item = service.inbox(userSession, false, 0, 20).getContent().getFirst();
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
    var sent = broadcast();
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
    assertEquals(1, service.inbox(session(guest), false, 0, 20).getTotalElements());
  }

  @Test
  void cleanupRemovesReadAndUnreadExpiredNotificationsAndOrphanContents() {
    var old = broadcast();
    var fresh = broadcast();
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
    var sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertTrue(service.inbox(userSession, false, 0, 20).isEmpty());
    assertTrue(service.inbox(userSession, true, 0, 20).isEmpty());
    assertEquals(0, service.unreadCount(userSession));
    assertTrue(service.unreadSnapshot(userSession).isEmpty());
    assertEquals(1, service.sent(adminSession, 0, 5).getTotalElements());
    assertEquals(0, cleanup.cleanup());
    assertEquals(3, recipients.count());
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("365");
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("90");
    assertEquals(1, service.inbox(userSession, false, 0, 20).getTotalElements());
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
    var sent = broadcast();
    createdAt(sent.id(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(inboxLimited ? 0 : 1, service.inbox(userSession, false, 0, 20).getTotalElements());
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
    var automatic = notifications.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, service.inbox(userSession, false, 0, 20).getTotalElements());
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
    var automatic = notifications.findAll().getFirst();
    createdAt(automatic.getId(), Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS));
    assertEquals(1, cleanup.cleanup());
    assertEquals(0, notificationCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void exactCutoffIsVisibleAndCleanupDeletesOnlyStrictlyOlderMessages() {
    var sent = broadcast();
    var cutoff = Instant.parse("2026-01-01T00:00:00Z");
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
    var policy = new UserNotificationPolicy(config);
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("0");
    assertThrows(IllegalArgumentException.class, policy::historyRetentionDays);
    when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS)).thenReturn("365");
    assertEquals(OptionalInt.of(180), service.retentionDays());
  }

  @Test
  void notificationSqlSchemaHasConsistentNamesAndPreservesStoredReadState() {
    tx.executeWithoutResult(
        s -> {
          var columns =
              em.createNativeQuery(
                      "select column_name from information_schema.columns where table_name='USER_NOTIFICATION_RECIPIENTS'")
                  .getResultList();
          assertTrue(columns.contains("NOTIFICATION_ID"));
          var constraints =
              em.createNativeQuery(
                      "select constraint_name from information_schema.table_constraints where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
                  .getResultList();
          assertTrue(constraints.contains("UK_USER_NOTIFICATION_REQUEST"));
          assertTrue(constraints.contains("UK_USER_NOTIFICATION_RECIPIENT"));
          var indexes =
              em.createNativeQuery(
                      "select index_name from information_schema.indexes where table_name in ('USER_NOTIFICATIONS','USER_NOTIFICATION_RECIPIENTS')")
                  .getResultList();
          assertTrue(indexes.contains("IDX_USER_NOTIFICATION_CREATED"));
          assertTrue(indexes.contains("IDX_USER_NOTIFICATION_INBOX"));
        });
    var created = Instant.parse("2026-10-18T10:00:00Z");
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
    var item = service.inbox(userSession, false, 0, 20).getContent().getFirst();
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
