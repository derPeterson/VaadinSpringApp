package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.SupportReplyEntity.Kind;
import de.derpeterson.app.model.SupportTicketEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.SupportReplyRepository;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.SupportTicketViewerRepository;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AccountSecurityMail;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailQueueService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Reply;
import de.derpeterson.app.service.SupportTicketService.ReplyAuthor;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.service.SupportTicketService.UserIdentity;
import de.derpeterson.app.service.UserNotificationPolicy;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.websocket.SupportTicketBroadcaster;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig({
  TwoFactorStoreTest.Config.class,
  TwoFactorAuthorizationTest.Config.class,
  SupportTicketServiceTest.Config.class
})
class SupportTicketServiceTest {
  @Autowired SupportTicketService service;
  @Autowired SupportReplyRepository replies;
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorStore factors;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transaction;
  @Autowired EmailQueueService queue;
  @Autowired UserNotificationService notifications;
  @Autowired UserNotificationRecipientRepository notificationRecipients;
  @Autowired SecurityService security;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @PersistenceContext EntityManager entityManager;
  private UserEntity user;
  private MockHttpServletRequest owner;
  private MockHttpServletRequest administrator;
  private MockHttpServletRequest otherAdministrator;

  @BeforeEach
  void setup() {
    clock.now.set(java.time.Instant.parse("2026-11-02T12:00:00Z"));
    notifications =
        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(notifications);
    queue = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(queue);
    reset(queue, notifications);
    user = create(RoleType.ROLE_USER);
    owner = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    administrator = confirmedAdmin();
    otherAdministrator = confirmedAdmin();
  }

  @Test
  void directTicketNumberFindsOlderTicketsAndKeepsThemInLiveUpdates() {
    Ticket older = service.createRecovery(owner, "Older request");
    List<Long> fixtureIds = new ArrayList<>();
    transaction.executeWithoutResult(
        status -> {
          entityManager.find(SupportTicketEntity.class, older.id()).setStatus(Status.CLOSED);
          UserEntity requester = entityManager.find(UserEntity.class, user.getId());
          for (int index = 0; index < 101; index++) {
            SupportTicketEntity ticket = new SupportTicketEntity();
            ticket.setOwner(requester);
            ticket.setStatus(Status.CLOSED);
            ticket.setSubject("Synthetic archived request");
            ticket.setCreatedAt(older.createdAt().plusSeconds(index + 1));
            ticket.setUpdatedAt(older.updatedAt().plusSeconds(index + 1));
            ticket.setPublicUpdatedAt(older.updatedAt().plusSeconds(index + 1));
            entityManager.persist(ticket);
            fixtureIds.add(ticket.getId());
          }
        });
    try {
      assertFalse(
          service.list(administrator, true).stream()
              .anyMatch(ticket -> ticket.id().equals(older.id())));
      String number = SupportTicketService.ticketNumber(older.id()).substring(1);
      assertEquals(older.id(), service.findForAdministration(administrator, number).id());
      assertEquals(
          older.id(),
          service.liveView(bindLiveSession(administrator), true, older.id()).selected().id());
      assertNull(service.findForAdministration(administrator, "#" + number));
      assertNull(service.findForAdministration(administrator, "9999999999999999999"));
      assertThrows(AccessDeniedException.class, () -> service.findForAdministration(owner, number));
    } finally {
      transaction.executeWithoutResult(
          status -> {
            for (Long id : fixtureIds) {
              entityManager.remove(entityManager.find(SupportTicketEntity.class, id));
            }
          });
    }
  }

  @Test
  void ownRequestDoesNotGrantPortalAccessAndNeverExposesInternalNotes() {
    Ticket ticket = service.createRecovery(owner, "Ich benötige Hilfe.");
    verify(notifications)
        .recordRecoveryRequest(eq(SupportTicketService.ticketNumber(ticket.id())), any());
    assertThrows(IllegalStateException.class, () -> service.createRecovery(owner, "Doppelt"));
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());
    assertTrue(service.list(foreign, false).isEmpty());
    assertThrows(
        AccessDeniedException.class, () -> service.conversation(foreign, ticket.id(), false));
    assertThrows(AccessDeniedException.class, () -> service.list(owner, true));
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    UserIdentity expectedAssignee =
        new UserIdentity("Support", "Fixture", authorization.owner(administrator).email());
    assertEquals(expectedAssignee, claimed.assigneeIdentity());
    UserIdentity expectedRequester =
        new UserIdentity(user.getFirstName(), user.getLastName(), user.getEmail());
    assertEquals(expectedRequester, claimed.requesterIdentity());
    assertEquals(
        expectedRequester, ticketFor(otherAdministrator, ticket.id(), true).requesterIdentity());
    assertEquals(
        expectedRequester,
        service
            .liveView(bindLiveSession(administrator), true, ticket.id())
            .selected()
            .requesterIdentity());
    assertNull(ticketFor(owner, ticket.id(), false).requesterIdentity());
    assertNull(
        service
            .liveView(bindLiveSession(owner), false, ticket.id())
            .selected()
            .requesterIdentity());
    assertEquals(
        expectedAssignee, ticketFor(otherAdministrator, ticket.id(), true).assigneeIdentity());
    assertNull(ticketFor(owner, ticket.id(), false).assigneeIdentity());
    assertNull(
        service.liveView(bindLiveSession(owner), false, ticket.id()).selected().assigneeIdentity());
    Ticket changed =
        service.reply(
            administrator, claimed.id(), claimed.version(), "Interne Prüfung", true, true);
    assertEquals(1, service.conversation(owner, ticket.id(), false).size());
    assertEquals(4, service.conversation(administrator, ticket.id(), true).size());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(owner, changed.id(), changed.version(), "Verbergen", true, false));
    service.reply(
        administrator, changed.id(), changed.version(), "Öffentliche Antwort", false, true);
    List<Reply> visible = service.conversation(owner, ticket.id(), false);
    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT),
        visible.stream().map(Reply::author).toList());
    assertTrue(visible.stream().allMatch(reply -> reply.actor() == null));
    List<Reply> administrativeMessages = service.conversation(administrator, ticket.id(), true);
    assertEquals(expectedAssignee, administrativeMessages.getLast().actor());
    assertEquals(
        expectedAssignee,
        administrativeMessages.stream()
            .filter(Reply::internal)
            .filter(reply -> reply.kind() == Kind.MESSAGE)
            .findFirst()
            .orElseThrow()
            .actor());
    assertFalse(authorization.state(authorization.owner(owner)).enrolled());
  }

  @Test
  void historyRecordsActualTransitionsWithoutCountingMessagesOrExposingAdministrativeEvents() {
    Ticket created = service.createRecovery(owner, "Bitte helfen.");
    List<Reply> first = service.conversation(administrator, created.id(), true);
    assertEquals(
        List.of(Kind.TICKET_CREATED, Kind.MESSAGE), first.stream().map(Reply::kind).toList());
    Instant createdAt = ticketFor(owner, created.id(), false).createdAt();
    assertEquals(createdAt, first.getFirst().createdAt());
    assertEquals(user.getEmail(), first.getFirst().actor().email());
    assertNull(first.getFirst().previousStatus());
    assertEquals(Status.OPEN, first.getFirst().nextStatus());
    assertEquals(1L, ticketFor(otherAdministrator, created.id(), true).unreadMessages());

    Ticket claimed = service.claim(administrator, created.id(), created.version());
    Ticket repeated = service.claim(administrator, claimed.id(), claimed.version());
    assertEquals(3, service.conversation(administrator, created.id(), true).size());
    assertEquals(createdAt, repeated.createdAt());
    assertEquals(createdAt, ticketFor(owner, created.id(), false).createdAt());
    assertEquals(0L, ticketFor(owner, created.id(), false).unreadMessages());
    assertEquals(1, service.conversation(owner, created.id(), false).size());
    assertEquals(1L, replies.countFor(created.id()));

    Ticket requesterClosed = service.closeOwn(owner, repeated.id(), repeated.version());
    Ticket closed = service.close(administrator, requesterClosed.id(), requesterClosed.version());
    List<Reply> history =
        service.conversation(administrator, created.id(), true).stream()
            .filter(reply -> reply.kind().isHistory())
            .toList();
    assertEquals(
        List.of(Status.OPEN, Status.IN_PROGRESS, Status.CLOSED_BY_REQUESTER, Status.CLOSED),
        history.stream().map(Reply::nextStatus).toList());
    assertEquals(
        List.of(Status.OPEN, Status.IN_PROGRESS, Status.CLOSED_BY_REQUESTER),
        history.stream().skip(1).map(Reply::previousStatus).toList());
    assertEquals(user.getEmail(), history.get(2).actor().email());
    assertEquals(authorization.owner(administrator).email(), history.getLast().actor().email());
    assertEquals(createdAt, closed.createdAt());
    assertEquals(1, service.conversation(owner, created.id(), false).size());
    assertEquals(0L, ticketFor(otherAdministrator, created.id(), true).unreadMessages());
    assertThrows(
        AccessDeniedException.class,
        () -> service.close(administrator, closed.id(), closed.version()));
    assertEquals(
        4,
        service.conversation(administrator, created.id(), true).stream()
            .filter(reply -> reply.kind().isHistory())
            .count());
  }

  @Test
  void adminReplyToOwnTicketRetainsSupportRole() {
    Ticket ticket = service.createRecovery(administrator, "Ich benötige Hilfe.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    Ticket answered =
        service.reply(
            administrator, claimed.id(), claimed.version(), "Ich prüfe die Anfrage.", false, true);
    service.reply(
        administrator, answered.id(), answered.version(), "Danke für die Antwort.", false, false);

    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT, ReplyAuthor.REQUESTER),
        service.conversation(administrator, ticket.id(), false).stream()
            .map(Reply::author)
            .toList());
  }

  @Test
  void assignedAdminsAndWatchingSuperadminsReceiveOnlyTheirSubscribedPublicMessages() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long assignedId = service.administratorId(administrator);
    Long watcherId = service.administratorId(superadmin);
    assertThrows(
        AccessDeniedException.class,
        () -> service.setSubscription(otherAdministrator, ticket.id(), true));
    service.setSubscription(administrator, ticket.id(), true);
    service.setSubscription(superadmin, ticket.id(), true);
    Ticket noted =
        service.reply(administrator, claimed.id(), claimed.version(), "Intern.", true, true);
    verify(notifications, org.mockito.Mockito.never()).recordSupportReply(any(), any(), any());

    Ticket updated =
        service.reply(owner, noted.id(), noted.version(), "Noch eine Frage.", false, false);
    verify(notifications)
        .recordSupportReply(
            eq(SupportTicketService.ticketNumber(ticket.id())),
            any(),
            argThat(
                recipients ->
                    recipients.size() == 2
                        && recipients.stream()
                            .map(UserEntity::getId)
                            .toList()
                            .containsAll(List.of(assignedId, watcherId))));
    service.setSubscription(superadmin, ticket.id(), false);
    assertFalse(ticketFor(superadmin, ticket.id(), true).subscribed());
    service.closeOwn(owner, updated.id(), updated.version());
    assertThrows(
        AccessDeniedException.class, () -> service.setSubscription(superadmin, ticket.id(), true));
  }

  @Test
  void unreadCursorOnlyAcknowledgesDisplayedRepliesInTheCurrentPerspective() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    Ticket answered =
        service.reply(
            administrator, claimed.id(), claimed.version(), "Erste Antwort.", false, true);
    Reply first = service.conversation(owner, ticket.id(), false).getLast();
    Instant publicUpdate = ticketFor(owner, ticket.id(), false).updatedAt();
    Ticket noted =
        service.reply(
            administrator, answered.id(), answered.version(), "Interner Vermerk.", true, true);
    Reply internal = service.conversation(administrator, ticket.id(), true).getLast();
    assertEquals(publicUpdate, ticketFor(owner, ticket.id(), false).updatedAt());
    service.reply(administrator, noted.id(), noted.version(), "Zweite Antwort.", false, true);
    HttpSession requesterSession = bindLiveSession(owner);
    HttpSession administratorSession = bindLiveSession(administrator);
    assertEquals(2, ticketFor(owner, ticket.id(), false).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertFalse(
        service.markSeen(requesterSession, false, ticket.id(), internal.id(), ticket.version()));
    assertTrue(
        service.markSeen(requesterSession, false, ticket.id(), first.id(), ticket.version()));
    assertFalse(
        service.markSeen(requesterSession, false, ticket.id(), first.id(), ticket.version()));
    assertEquals(
        1, service.liveView(requesterSession, false, ticket.id()).selected().unreadMessages());
    Reply latest = service.liveView(requesterSession, false, ticket.id()).messages().getLast();
    assertTrue(
        service.markSeen(
            requesterSession,
            false,
            ticket.id(),
            latest.id(),
            ticketFor(administrator, ticket.id(), true).version()));
    assertEquals(0, ticketFor(owner, ticket.id(), false).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertTrue(
        service.markSeen(
            administratorSession,
            true,
            ticket.id(),
            latest.id(),
            ticketFor(administrator, ticket.id(), true).version()));
    assertEquals(0, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertThrows(
        AccessDeniedException.class, () -> service.liveView(requesterSession, true, ticket.id()));
  }

  @Test
  void countersFollowStatusAssignmentAndWatchWithoutDiscardingUnreadMessages() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    assertEquals(1, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertEquals(0, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    service.setSubscription(superadmin, ticket.id(), true);
    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    service.setSubscription(superadmin, ticket.id(), false);
    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    service.setSubscription(superadmin, ticket.id(), true);
    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    Ticket resolved = service.closeOwn(owner, claimed.id(), claimed.version());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    service.close(administrator, resolved.id(), resolved.version());
    assertEquals(0, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
  }

  @Test
  void claimAndClosingRetireNotificationsAndRequesterClosureReachesAssigneeWithoutWatch() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Long adminId = service.administratorId(administrator);
    Long otherId = service.administratorId(otherAdministrator);
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertEquals(0, notificationRecipients.countUnread(otherId, null));
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long watcherId = service.administratorId(superadmin);
    service.setSubscription(superadmin, ticket.id(), true);
    Ticket resolved = service.closeOwn(owner, ticket.id(), claimed.version());
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertEquals(1, notificationRecipients.countUnread(watcherId, null));
    assertEquals(0, notificationRecipients.countUnread(otherId, null));
    Reply last = service.conversation(administrator, ticket.id(), true).getLast();
    HttpSession adminSession = bindLiveSession(administrator);
    service.markSeen(adminSession, true, ticket.id(), last.id(), claimed.version());
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertTrue(service.markSeen(adminSession, true, ticket.id(), last.id(), resolved.version()));
    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertFalse(service.markSeen(adminSession, true, ticket.id(), last.id(), resolved.version()));
    assertEquals(1, notificationRecipients.countUnread(watcherId, null));
    service.close(administrator, resolved.id(), resolved.version());
    assertEquals(0, notificationRecipients.countUnread(watcherId, null));
    assertEquals(
        2,
        notificationRecipients
            .inbox(adminId, null, null, org.springframework.data.domain.PageRequest.of(0, 20))
            .getTotalElements());
  }

  @Test
  void requesterClosureBeforeClaimRemainsActionableForAllActiveAdministrators() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket resolved = service.closeOwn(owner, ticket.id(), ticket.version());
    assertEquals(
        1, notificationRecipients.countUnread(service.administratorId(administrator), null));
    assertEquals(
        1, notificationRecipients.countUnread(service.administratorId(otherAdministrator), null));
    assertEquals(1, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    assertThrows(
        AccessDeniedException.class,
        () -> service.closeOwn(owner, ticket.id(), resolved.version()));
    assertEquals(
        1, notificationRecipients.countUnread(service.administratorId(administrator), null));
    service.close(administrator, ticket.id(), resolved.version());
    assertEquals(
        0, notificationRecipients.countUnread(service.administratorId(otherAdministrator), null));
  }

  @Test
  void readingTheDisplayedMessageDoesNotAcknowledgeAnArrivingMessageAndUnwatchStopsNotifications() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    Long adminId = service.administratorId(administrator);
    service.setSubscription(administrator, ticket.id(), true);
    Ticket first =
        service.reply(owner, ticket.id(), claimed.version(), "Erste Frage.", false, false);
    Reply displayed = service.conversation(administrator, ticket.id(), true).getLast();
    Ticket second =
        service.reply(owner, ticket.id(), first.version(), "Später eingetroffen.", false, false);
    assertEquals(2, notificationRecipients.countUnread(adminId, null));
    HttpSession session = bindLiveSession(administrator);
    assertTrue(service.markSeen(session, true, ticket.id(), displayed.id(), first.version()));
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertFalse(service.markSeen(session, true, ticket.id(), displayed.id(), first.version()));
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    Reply latest = service.conversation(administrator, ticket.id(), true).getLast();
    service.markSeen(session, true, ticket.id(), latest.id(), second.version());
    service.setSubscription(administrator, ticket.id(), false);
    Ticket third =
        service.reply(owner, ticket.id(), second.version(), "Ohne Beobachtung.", false, false);
    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    service.closeOwn(owner, ticket.id(), third.version());
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
  }

  @Test
  void revokedAndDisabledWatchersReceiveNeitherMessagesNorRequesterClosure() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    MockHttpServletRequest revoked = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    MockHttpServletRequest disabled = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long revokedId = service.administratorId(revoked);
    Long disabledId = service.administratorId(disabled);
    service.setSubscription(revoked, ticket.id(), true);
    service.setSubscription(disabled, ticket.id(), true);
    transaction.executeWithoutResult(
        status -> {
          UserEntity formerAdmin = users.findById(revokedId).orElseThrow();
          formerAdmin.setRoleEntities(List.of());
          UserEntity disabledAdmin = users.findById(disabledId).orElseThrow();
          disabledAdmin.setEnabled(false);
        });
    Ticket updated =
        service.reply(owner, ticket.id(), claimed.version(), "Neue Frage.", false, false);
    service.closeOwn(owner, ticket.id(), updated.version());
    assertEquals(0, notificationRecipients.countUnread(revokedId, null));
    assertEquals(0, notificationRecipients.countUnread(disabledId, null));
  }

  @Test
  void liveRefreshOnlyPublishesCommittedChanges() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    List<SupportTicketBroadcaster.Change> changes = new ArrayList<>();
    Registration listener = service.events().register(changes::add);
    try {
      transaction.executeWithoutResult(
          status -> {
            service.reply(owner, ticket.id(), ticket.version(), "Zurückgerollt.", false, false);
            assertTrue(changes.isEmpty());
            status.setRollbackOnly();
          });
      assertTrue(changes.isEmpty());
      service.reply(owner, ticket.id(), ticket.version(), "Gespeichert.", false, false);
      assertEquals(List.of(new SupportTicketBroadcaster.Change(user.getId(), null)), changes);

      Reply latest = service.conversation(owner, ticket.id(), false).getLast();
      assertTrue(
          service.markSeen(
              bindLiveSession(owner),
              false,
              ticket.id(),
              latest.id(),
              ticketFor(administrator, ticket.id(), true).version()));
      assertEquals(2, changes.size());
      SupportTicketBroadcaster.Change read = changes.getLast();
      assertTrue(read.affects(user.getId(), false));
      assertFalse(read.affects(service.administratorId(administrator), true));
    } finally {
      listener.remove();
    }
  }

  private Ticket ticketFor(MockHttpServletRequest request, Long id, boolean administration) {
    return service.list(request, administration).stream()
        .filter(ticket -> ticket.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private HttpSession bindLiveSession(MockHttpServletRequest request) {
    HttpSession session = request.getSession();
    String email = (String) request.getAttribute("fixture.identity");
    when(security.refreshSessionAuthentication(session))
        .thenReturn(
            Optional.of(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of())));
    return session;
  }

  @Test
  void blankRequestAndBlankReplyDoNotChangeTheTicketVersion() {
    assertThrows(IllegalArgumentException.class, () -> service.createRecovery(owner, " "));
    assertTrue(service.list(owner, false).isEmpty());

    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.reply(administrator, claimed.id(), claimed.version(), " ", false, true));
    service.reply(administrator, claimed.id(), claimed.version(), "Ich prüfe das.", false, true);
    assertEquals(2, service.conversation(owner, ticket.id(), false).size());
  }

  @Test
  void requesterCanCloseOwnTicketAndAdministratorCanFinalizeItWithoutReopening() {
    Ticket open = service.createRecovery(owner, "Hilfe bitte.");
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());
    assertThrows(
        AccessDeniedException.class, () -> service.closeOwn(foreign, open.id(), open.version()));
    Ticket resolved = service.closeOwn(owner, open.id(), open.version());
    assertEquals(Status.CLOSED_BY_REQUESTER, resolved.status());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(owner, resolved.id(), resolved.version(), "Nachtrag", false, false));
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.reply(
                administrator, resolved.id(), resolved.version(), "Nachtrag", false, true));
    assertThrows(
        AccessDeniedException.class,
        () -> service.sendInstructions(administrator, resolved.id(), resolved.version()));
    assertEquals(Status.OPEN, service.createRecovery(owner, "Ein neues Anliegen.").status());
    Ticket closed = service.close(administrator, resolved.id(), resolved.version());
    assertEquals(Status.CLOSED, closed.status());
    assertThrows(
        AccessDeniedException.class,
        () -> service.reply(administrator, closed.id(), closed.version(), "Nachtrag", false, true));
    assertEquals(1, service.conversation(owner, closed.id(), false).size());
  }

  @Test
  void competingClaimAndStaleUpdatesCannotOverwriteAnAssignee() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> service.claim(otherAdministrator, ticket.id(), ticket.version()));
    assertThrows(
        IllegalStateException.class,
        () -> service.claim(otherAdministrator, claimed.id(), claimed.version()));
    assertThrows(
        AccessDeniedException.class,
        () -> service.close(otherAdministrator, claimed.id(), claimed.version()));
  }

  @Test
  void rolledBackClaimKeepsOriginalNotificationAndTicketOpen() {
    Ticket ticket = service.createRecovery(owner, "Hilfe bitte.");
    Long adminId = service.administratorId(administrator);
    transaction.executeWithoutResult(
        status -> {
          service.claim(administrator, ticket.id(), ticket.version());
          status.setRollbackOnly();
        });
    assertEquals(Status.OPEN, ticketFor(administrator, ticket.id(), true).status());
    assertEquals(
        List.of(Kind.TICKET_CREATED, Kind.MESSAGE),
        service.conversation(administrator, ticket.id(), true).stream().map(Reply::kind).toList());
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    service.claim(administrator, ticket.id(), ticket.version());
    assertEquals(0, notificationRecipients.countUnread(adminId, null));
  }

  @Test
  void parallelClaimSucceedsForExactlyOneAdministrator() throws Exception {
    Ticket ticket = service.createRecovery(owner, "Gemeinsame Anfrage.");
    java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    try (java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.function.Function<MockHttpServletRequest, Boolean> claim =
          actor -> {
            ready.countDown();
            try {
              assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
              service.claim(actor, ticket.id(), ticket.version());
              return true;
            } catch (OptimisticLockingFailureException | IllegalStateException expected) {
              return false;
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(interrupted);
            }
          };
      var first = executor.submit(() -> claim.apply(administrator));
      var second = executor.submit(() -> claim.apply(otherAdministrator));
      assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
      start.countDown();
      org.junit.jupiter.api.Assertions.assertNotEquals(
          first.get(10, java.util.concurrent.TimeUnit.SECONDS),
          second.get(10, java.util.concurrent.TimeUnit.SECONDS));
    }
    verify(notifications, org.mockito.Mockito.times(1))
        .retireTicketNotifications(
            SupportTicketService.ticketNumber(ticket.id()),
            de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED);
    assertEquals(
        0, notificationRecipients.countUnread(service.administratorId(administrator), null));
    assertEquals(
        0, notificationRecipients.countUnread(service.administratorId(otherAdministrator), null));
  }

  @Test
  void restrictedFirstFactorCanAskForHelpButCannotReadAdministrationOrExceedLimits() {
    UserEntity required = create(RoleType.ROLE_ADMIN);
    MockHttpServletRequest firstFactor = TwoFactorAuthorizationTest.requestFor(required.getEmail());
    assertFalse(authorization.sessionConfirmed(firstFactor));
    assertThrows(IllegalArgumentException.class, () -> service.createRecovery(firstFactor, " "));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createRecovery(firstFactor, "x".repeat(2001)));
    for (int attempt = 0; attempt < 3; attempt++) {
      Ticket ticket = service.createRecovery(firstFactor, "Verlorener Authenticator.");
      assertFalse(authorization.sessionConfirmed(firstFactor));
      assertThrows(AccessDeniedException.class, () -> service.list(firstFactor, true));
      Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
      Ticket closed = service.close(administrator, claimed.id(), claimed.version());
      assertThrows(
          AccessDeniedException.class,
          () -> service.claim(administrator, closed.id(), closed.version()));
    }
    assertThrows(IllegalStateException.class, () -> service.createRecovery(firstFactor, "Zu oft"));
  }

  @Test
  void preparedInstructionsUseVerifiedStoredContactAndNeverUnlockAnything() {
    Ticket ticket =
        service.createRecovery(owner, "Bitte schicken Sie Hilfe an fremd@example.test.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    service.validateInstructionPreview(administrator, claimed.id());
    String preview =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN)
            .getSupportRecoveryInstructions();
    assertTrue(preview.contains("Ersatzcode"));
    Ticket sent = service.sendInstructions(administrator, claimed.id(), claimed.version());
    List<Reply> messages = service.conversation(owner, claimed.id(), false);
    assertEquals(Kind.RECOVERY_INSTRUCTIONS, messages.getLast().kind());
    assertEquals(preview, messages.getLast().body());
    assertTrue(preview.length() <= SupportTicketService.MESSAGE_LIMIT);
    Ticket copiedText =
        service.reply(administrator, sent.id(), sent.version(), preview, false, true);
    assertEquals(Kind.MESSAGE, service.conversation(owner, claimed.id(), false).getLast().kind());
    verify(queue).addAddressedNotification(any(), eq(user.getEmail()), any(), any());
    assertTrue(sent.instructionSent());
    assertThrows(
        AccessDeniedException.class,
        () -> service.sendInstructions(administrator, copiedText.id(), copiedText.version()));
    service.close(administrator, copiedText.id(), copiedText.version());
    assertThrows(
        AccessDeniedException.class,
        () -> service.validateInstructionPreview(administrator, copiedText.id()));
    assertFalse(authorization.state(authorization.owner(owner)).enrolled());
  }

  @Test
  void queueFailureKeepsInstructionsUnsentAndLeavesConversationUnchanged() {
    Ticket ticket = service.createRecovery(owner, "Hilfe.");
    Ticket claimed = service.claim(administrator, ticket.id(), ticket.version());
    doThrow(new IllegalStateException("test queue failure"))
        .when(queue)
        .addAddressedNotification(any(), any(), any(), any());
    assertThrows(
        IllegalStateException.class,
        () -> service.sendInstructions(administrator, claimed.id(), claimed.version()));
    assertFalse(service.list(owner, false).getFirst().instructionSent());
    assertEquals(1, service.conversation(owner, claimed.id(), false).size());
  }

  private MockHttpServletRequest confirmedAdmin() {
    return confirmedAdmin(RoleType.ROLE_ADMIN);
  }

  private MockHttpServletRequest confirmedAdmin(RoleType role) {
    UserEntity admin = create(role);
    MockHttpServletRequest request = TwoFactorAuthorizationTest.requestFor(admin.getEmail());
    byte[] seed = Totp.newSecret();
    UUID revision = factors.initialize(admin.getId(), admin.getAccessVersion());
    List<String> codes =
        factors
            .enroll(
                admin.getId(),
                admin.getAccessVersion(),
                revision,
                seed,
                Totp.code(seed, clock.instant().getEpochSecond() / 30),
                false)
            .recoveryCodes();
    authorization.confirmSession(request, codes.getFirst());
    return request;
  }

  private UserEntity create(RoleType type) {
    return transaction.execute(
        status -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                  .setParameter("name", type)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(type).build();
            entityManager.persist(role);
          }
          return users.saveAndFlush(
              UserEntity.builder()
                  .email(UUID.randomUUID() + "@example.test")
                  .firstName("Support")
                  .lastName("Fixture")
                  .preferredLocale(Locale.GERMAN)
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
  }

  @Configuration
  static class Config {
    @Bean
    SupportTicketBroadcaster ticketEvents() {
      return new SupportTicketBroadcaster();
    }

    @Bean
    ConfigService config() {
      ConfigService config = mock(ConfigService.class);
      when(config.getString(any()))
          .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
      when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://fixture.invalid/");
      return config;
    }

    @Bean
    EmailQueueService queue() {
      return mock(EmailQueueService.class);
    }

    @Bean
    UserNotificationService notifications(
        UserNotificationRepository notificationRepository,
        UserNotificationRecipientRepository recipients,
        UserRepository users,
        SecurityService security,
        ConfigService config,
        TwoFactorAuthorization authorization) {
      return org.mockito.Mockito.spy(
          new UserNotificationService(
              new UserNotificationPolicy(config),
              notificationRepository,
              recipients,
              users,
              security,
              config,
              new UserNotificationBroadcaster(),
              authorization));
    }

    @Bean
    AccountSecurityMail templates(ConfigService config) {
      return new AccountSecurityMail(config, new CustomI18NProvider());
    }

    @Bean
    SupportTicketService support(
        TwoFactorAuthorization authorization,
        SupportTicketRepository tickets,
        SupportReplyRepository replies,
        SupportTicketViewerRepository viewers,
        UserRepository users,
        EntityManagerFactory factory,
        UserNotificationService notifications,
        AccountSecurityMail templates,
        EmailQueueService queue,
        ConfigService config,
        SupportTicketBroadcaster events) {
      return new SupportTicketService(
          authorization,
          tickets,
          replies,
          viewers,
          users,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          notifications,
          templates,
          queue,
          new MaintenanceService(config),
          events);
    }
  }
}
