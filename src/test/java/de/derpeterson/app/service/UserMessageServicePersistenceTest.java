package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.scheduler.UserMessageCleanup;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.UserMessageService.*;
import de.derpeterson.app.websocket.UserMessageBroadcaster;
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
@SpringJUnitConfig(UserMessageServicePersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserMessageServicePersistenceTest {
  @Configuration
  @Import({
    UserServicePersistenceTest.Config.class,
    UserMessageService.class,
    UserMessageBroadcaster.class,
    UserMessageCleanup.class
  })
  static class Config {
    @Bean
    ConfigService config() {
      return mock(ConfigService.class);
    }

    @Bean
    UserMessagePolicy policy() {
      return new UserMessagePolicy(180);
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

  @Autowired UserMessageService service;
  @Autowired UserMessageRepository messages;
  @Autowired UserMessageRecipientRepository recipients;
  @Autowired UserRepository users;
  @Autowired UserMessageBroadcaster events;
  @Autowired UserMessageCleanup cleanup;
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
    tx.executeWithoutResult(
        s -> {
          recipients.deleteAll();
          messages.deleteAll();
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

  long messageCount() {
    return messages.count();
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
    assertEquals(0, recipients.countByUserIdAndReadAtIsNull(disabled.getId()));
    assertEquals(0, recipients.countByUserIdAndReadAtIsNull(pending.getId()));
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
    assertThrows(AccessDeniedException.class, () -> service.sent(session, 0));
    assertThrows(
        AccessDeniedException.class,
        () -> service.send(session, UUID.randomUUID(), "X", "Y", everyone()));
    assertEquals(0, messageCount());
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

  @Test
  void recipientMutationRequiresNewPreviewAndCreatesNothing() {
    var preview = everyone();
    mutate(ordinary, u -> u.setEnabled(false));
    assertProblem(
        Problem.RECIPIENTS_CHANGED,
        () -> service.send(adminSession, UUID.randomUUID(), "X", "Y", preview));
    assertEquals(0, messageCount());
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
    assertEquals(1, messageCount());
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
    assertEquals(1, messageCount());
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
  void persistedMessagesAreVisibleInNewSessionAndPagedNewestFirst() {
    broadcast();
    var newer = broadcast();
    var fresh = session(ordinary);
    var page = service.inbox(fresh, false, 0, 1);
    assertEquals(2, page.getTotalElements());
    assertTrue(page.hasNext());
    assertEquals(newer.createdAt(), page.getContent().getFirst().createdAt());
    assertEquals(1, service.inbox(fresh, false, 1, 1).getNumberOfElements());
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
    assertEquals(0, messageCount());
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
    assertEquals(1, messageCount());
    assertEquals(1, count.get());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserMessageType.class,
      names = {"PASSWORD_CHANGED", "PASSWORD_RESET", "EMAIL_CHANGED"})
  void securityEventsAreMinimalAndRequireTheDomainTransaction(UserMessageType type) {
    assertThrows(
        IllegalTransactionStateException.class, () -> service.recordSecurityEvent(ordinary, type));
    tx.executeWithoutResult(
        s -> service.recordSecurityEvent(users.findById(ordinary.getId()).orElseThrow(), type));
    var item = service.inbox(userSession, false, 0, 20).getContent().getFirst();
    assertEquals(type, item.type());
    assertNull(item.title());
    assertNull(item.body());
    assertEquals(0, service.sent(adminSession, 0).getTotalElements());
  }

  @Test
  void rolledBackSecurityEventDoesNotClaimSuccess() {
    tx.executeWithoutResult(
        s -> {
          service.recordSecurityEvent(ordinary, UserMessageType.PASSWORD_CHANGED);
          s.setRollbackOnly();
        });
    assertEquals(0, messageCount());
    assertEquals(0, recipients.count());
  }

  @Test
  void deletingRecipientKeepsSharedMessageForOthersAndDeletingSenderSetsNull() {
    var sent = broadcast();
    tx.executeWithoutResult(
        s -> {
          users.deleteById(ordinary.getId());
          users.flush();
        });
    assertEquals(2, recipients.count());
    assertTrue(messages.existsById(sent.id()));
    tx.executeWithoutResult(
        s -> {
          users.deleteById(sender.getId());
          users.flush();
        });
    assertEquals(1, recipients.count());
    tx.executeWithoutResult(
        s -> assertNull(messages.findById(sent.id()).orElseThrow().getSender()));
    assertEquals(1, service.inbox(session(guest), false, 0, 20).getTotalElements());
  }

  @Test
  void cleanupRemovesReadAndUnreadExpiredMessagesAndOrphanContents() {
    var old = broadcast();
    var fresh = broadcast();
    // Unveränderliche Fachspalten nur als künstliche Altstruktur per SQL altern lassen.
    tx.executeWithoutResult(
        s ->
            em.createNativeQuery("update user_messages set created_at = :old where id = :id")
                .setParameter("old", Instant.now().minus(181, java.time.temporal.ChronoUnit.DAYS))
                .setParameter("id", old.id())
                .executeUpdate());
    assertEquals(1, cleanup.cleanup());
    assertFalse(messages.existsById(old.id()));
    assertTrue(messages.existsById(fresh.id()));
    assertEquals(3, recipients.count());
    tx.executeWithoutResult(s -> recipients.deleteAll());
    assertEquals(1, cleanup.cleanup());
  }

  @Test
  void retentionMustBePositiveAndVisiblePolicyUsesSameValue() {
    assertThrows(IllegalArgumentException.class, () -> new UserMessagePolicy(0));
    assertThrows(IllegalArgumentException.class, () -> new UserMessagePolicy(-1));
    assertEquals(180, service.retentionDays());
  }
}
