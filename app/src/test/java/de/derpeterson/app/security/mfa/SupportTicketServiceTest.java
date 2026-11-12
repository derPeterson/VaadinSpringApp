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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import de.derpeterson.app.service.SupportTicketFilter;
import de.derpeterson.app.service.SupportTicketFilter.Assignment;
import de.derpeterson.app.service.SupportTicketFilter.Reading;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.InboxQuery;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  @Autowired SupportTicketService supportTicketService;
  @Autowired SupportReplyRepository replies;
  @Autowired TwoFactorAuthorization twoFactorAuthorization;
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired UserRepository userRepository;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired EmailQueueService emailQueueService;
  @Autowired UserNotificationService userNotificationService;
  @Autowired UserNotificationRecipientRepository notificationRecipients;
  @Autowired SecurityService securityService;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @PersistenceContext EntityManager entityManager;
  private UserEntity testUserEntity;
  private MockHttpServletRequest owner;
  private MockHttpServletRequest administrator;
  private MockHttpServletRequest otherAdministrator;

  @BeforeEach
  void setup() {
    clock.now.set(java.time.Instant.parse("2026-11-02T12:00:00Z"));
    userNotificationService =
        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(userNotificationService);
    emailQueueService =
        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(emailQueueService);
    reset(emailQueueService, userNotificationService);
    testUserEntity = create(RoleType.ROLE_USER);
    owner = TwoFactorAuthorizationTest.requestFor(testUserEntity.getEmail());
    administrator = confirmedAdmin();
    otherAdministrator = confirmedAdmin();
  }

  @Test
  void databasePagingFiltersAndDirectJumpIncludeOlderTickets() {
    Ticket older = supportTicketService.createRecovery(owner, "Older request");
    Long administratorId = supportTicketService.administratorId(administrator);
    SupportTicketFilter filter =
        new SupportTicketFilter(Set.of(Status.CLOSED), Assignment.MINE, Reading.ALL, null);
    List<Long> fixtureIds = new ArrayList<>();
    transactionTemplate.executeWithoutResult(status -> {
      entityManager.find(SupportTicketEntity.class, older.id()).setStatus(Status.CLOSED);
      UserEntity assignee = entityManager.find(UserEntity.class, administratorId);
      entityManager.find(SupportTicketEntity.class, older.id()).setAssignee(assignee);
      UserEntity requesterUserEntity = entityManager.find(UserEntity.class, testUserEntity.getId());
      for (int index = 0; index < 101; index++) {
        SupportTicketEntity ticket = new SupportTicketEntity();
        ticket.setOwner(requesterUserEntity);
        ticket.setAssignee(assignee);
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
          supportTicketService.listTicketsForViewer(administrator, true).stream()
              .anyMatch(ticket -> ticket.id().equals(older.id())));
      String number = SupportTicketService.ticketNumber(older.id()).substring(1);
      SupportTicketService.LiveView first =
          supportTicketService.loadTicketInbox(administrator, null, new InboxQuery(filter, 0, 10));

      assertEquals(102, first.totalTickets());
      assertEquals(10, first.visibleTickets().size());
      assertEquals(
          10, supportTicketService.findTicketPageIndex(administrator, older.id(), filter, 10));
      for (int size : List.of(10, 20, 50)) {
        int lastPage = 101 / size;
        SupportTicketService.LiveView last =
            supportTicketService.loadTicketInbox(
                administrator, older.id(), new InboxQuery(filter, Integer.MAX_VALUE, size));

        assertEquals(lastPage, last.page());
        assertEquals(102 % size, last.visibleTickets().size());
        assertEquals(older.id(), last.selected().id());
        assertEquals(older.id(), last.visibleTickets().getLast().id());
      }
      SupportTicketService.LiveView detail =
          supportTicketService.loadTicketInbox(
              administrator, older.id(), new InboxQuery(filter, 0, 10));

      assertEquals(older.id(), detail.selected().id());
      assertFalse(
          detail.visibleTickets().stream().anyMatch(ticket -> ticket.id().equals(older.id())));
      SupportTicketFilter open =
          new SupportTicketFilter(Set.of(Status.OPEN), Assignment.MINE, Reading.ALL, null);

      assertEquals(
          0,
          supportTicketService
              .loadTicketInbox(administrator, older.id(), new InboxQuery(open, 0, 10))
              .totalTickets());
      assertNull(
          supportTicketService
              .loadTicketInbox(administrator, older.id(), new InboxQuery(open, 0, 10))
              .selected());
      assertEquals(
          older.id(), supportTicketService.findForAdministration(administrator, number).id());
      assertEquals(
          older.id(),
          supportTicketService
              .liveView(bindLiveSession(administrator), true, older.id())
              .selected()
              .id());
      assertNull(supportTicketService.findForAdministration(administrator, "#" + number));
      assertNull(supportTicketService.findForAdministration(administrator, "9999999999999999999"));
      assertThrows(
          AccessDeniedException.class,
          () -> supportTicketService.findForAdministration(owner, number));
      Long olderId = older.id();
      InboxQuery allTickets = InboxQuery.all();

      assertThrows(
          AccessDeniedException.class,
          () -> supportTicketService.loadTicketInbox(owner, olderId, allTickets));
      assertThrows(IllegalArgumentException.class, () -> new InboxQuery(filter, 0, 7));
    } finally {
      transactionTemplate.executeWithoutResult(status -> {
        for (Long id : fixtureIds) {
          entityManager.remove(entityManager.find(SupportTicketEntity.class, id));
        }
      });
    }
  }

  @Test
  void databaseUnreadFilterRespectsAssignmentSubscriptionAndReadDetails() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Filtered unread request");
    supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    SupportTicketFilter mine =
        new SupportTicketFilter(Set.of(Status.IN_PROGRESS), Assignment.MINE, Reading.UNREAD, null);
    InboxQuery query = new InboxQuery(mine, 0, 10);
    SupportTicketService.LiveView unread =
        supportTicketService.loadTicketInbox(administrator, ticket.id(), query);

    assertEquals(1, unread.totalTickets());
    assertEquals(ticket.id(), unread.visibleTickets().getFirst().id());
    SupportTicketFilter any =
        new SupportTicketFilter(Set.of(Status.IN_PROGRESS), Assignment.ALL, Reading.UNREAD, null);

    assertEquals(
        0,
        supportTicketService
            .loadTicketInbox(otherAdministrator, null, new InboxQuery(any, 0, 10))
            .totalTickets());
    MockHttpServletRequest watcher = confirmedAdmin(RoleType.ROLE_SUPERADMIN);

    assertEquals(
        0,
        supportTicketService
            .loadTicketInbox(watcher, null, new InboxQuery(any, 0, 10))
            .totalTickets());
    supportTicketService.setSubscription(watcher, ticket.id(), true);

    assertEquals(
        1,
        supportTicketService
            .loadTicketInbox(watcher, null, new InboxQuery(any, 0, 10))
            .totalTickets());
    Reply latest = unread.messages().getLast();
    supportTicketService.markSeen(
        bindLiveSession(administrator),
        true,
        ticket.id(),
        latest.id(),
        unread.selected().version());
    SupportTicketService.LiveView read =
        supportTicketService.loadTicketInbox(administrator, ticket.id(), query);

    assertEquals(0, read.totalTickets());
    assertTrue(read.visibleTickets().isEmpty());
    assertEquals(ticket.id(), read.selected().id());
  }

  @Test
  void unreadDatabasePredicateMatchesEstablishedRelevanceForEveryStatusAndAssignee() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Unread predicate comparison");
    MockHttpServletRequest watcher = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    supportTicketService.setSubscription(watcher, ticket.id(), true);
    for (Status status : Status.values()) {
      for (Long assignee :
          new Long[] {
            null,
            supportTicketService.administratorId(administrator),
            supportTicketService.administratorId(otherAdministrator)
          }) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
          SupportTicketEntity entity = entityManager.find(SupportTicketEntity.class, ticket.id());
          entity.setStatus(status);
          entity.setAssignee(
              assignee == null ? null : entityManager.find(UserEntity.class, assignee));
        });
        SupportTicketFilter unread =
            new SupportTicketFilter(Set.of(status), Assignment.ALL, Reading.UNREAD, null);
        for (MockHttpServletRequest viewer : List.of(administrator, otherAdministrator, watcher)) {
          Ticket expected =
              supportTicketService.findForAdministration(
                  viewer, SupportTicketService.ticketNumber(ticket.id()).substring(1));
          int page = supportTicketService.findTicketPageIndex(viewer, ticket.id(), unread, 10);
          boolean listed =
              supportTicketService
                  .loadTicketInbox(viewer, null, new InboxQuery(unread, page, 10))
                  .visibleTickets()
                  .stream()
                  .anyMatch(value -> value.id().equals(ticket.id()));

          assertEquals(expected.unreadMessages() > 0, listed, status + " / " + assignee);
        }
      }
    }
  }

  @Test
  void ownRequestDoesNotGrantPortalAccessAndNeverExposesInternalNotes() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Ich benötige Hilfe.");

    verify(userNotificationService)
        .recordRecoveryRequest(eq(SupportTicketService.ticketNumber(ticket.id())), any());
    assertThrows(
        IllegalStateException.class, () -> supportTicketService.createRecovery(owner, "Doppelt"));
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());

    assertTrue(supportTicketService.listTicketsForViewer(foreign, false).isEmpty());
    Long ticketId = ticket.id();

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.loadTicketConversation(foreign, ticketId, false));
    assertThrows(
        AccessDeniedException.class, () -> supportTicketService.listTicketsForViewer(owner, true));
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    UserIdentity expectedAssignee =
        new UserIdentity("Support", "Fixture", twoFactorAuthorization.owner(administrator).email());

    assertEquals(expectedAssignee, claimed.assigneeIdentity());
    UserIdentity expectedRequester =
        new UserIdentity(
            testUserEntity.getFirstName(), testUserEntity.getLastName(), testUserEntity.getEmail());

    assertEquals(expectedRequester, claimed.requesterIdentity());
    assertEquals(
        expectedRequester, ticketFor(otherAdministrator, ticket.id(), true).requesterIdentity());
    assertEquals(
        expectedRequester,
        supportTicketService
            .liveView(bindLiveSession(administrator), true, ticket.id())
            .selected()
            .requesterIdentity());
    assertNull(ticketFor(owner, ticket.id(), false).requesterIdentity());
    assertNull(
        supportTicketService
            .liveView(bindLiveSession(owner), false, ticket.id())
            .selected()
            .requesterIdentity());
    assertEquals(
        expectedAssignee, ticketFor(otherAdministrator, ticket.id(), true).assigneeIdentity());
    assertNull(ticketFor(owner, ticket.id(), false).assigneeIdentity());
    assertNull(
        supportTicketService
            .liveView(bindLiveSession(owner), false, ticket.id())
            .selected()
            .assigneeIdentity());
    Ticket changed =
        supportTicketService.replyToTicket(
            administrator, claimed.id(), claimed.version(), "Interne Prüfung", true, true);

    assertEquals(1, supportTicketService.loadTicketConversation(owner, ticket.id(), false).size());
    assertEquals(
        4, supportTicketService.loadTicketConversation(administrator, ticket.id(), true).size());
    Long changedId = changed.id();
    Long changedVersion = changed.version();

    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.replyToTicket(
                owner, changedId, changedVersion, "Verbergen", true, false));
    supportTicketService.replyToTicket(
        administrator, changed.id(), changed.version(), "Öffentliche Antwort", false, true);
    List<Reply> visible = supportTicketService.loadTicketConversation(owner, ticket.id(), false);

    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT),
        visible.stream().map(Reply::author).toList());
    assertTrue(visible.stream().allMatch(reply -> reply.actor() == null));
    List<Reply> administrativeMessages =
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true);

    assertEquals(expectedAssignee, administrativeMessages.getLast().actor());
    assertEquals(
        expectedAssignee,
        administrativeMessages.stream()
            .filter(Reply::internal)
            .filter(reply -> reply.kind() == Kind.MESSAGE)
            .findFirst()
            .orElseThrow()
            .actor());
    assertFalse(twoFactorAuthorization.state(twoFactorAuthorization.owner(owner)).enrolled());
  }

  @Test
  void historyRecordsActualTransitionsWithoutCountingMessagesOrExposingAdministrativeEvents() {
    Ticket created = supportTicketService.createRecovery(owner, "Bitte helfen.");
    List<Reply> first =
        supportTicketService.loadTicketConversation(administrator, created.id(), true);

    assertEquals(
        List.of(Kind.TICKET_CREATED, Kind.MESSAGE), first.stream().map(Reply::kind).toList());
    Instant createdAt = ticketFor(owner, created.id(), false).createdAt();

    assertEquals(createdAt, first.getFirst().createdAt());
    assertEquals(testUserEntity.getEmail(), first.getFirst().actor().email());
    assertNull(first.getFirst().previousStatus());
    assertEquals(Status.OPEN, first.getFirst().nextStatus());
    assertEquals(1L, ticketFor(otherAdministrator, created.id(), true).unreadMessages());

    Ticket claimed =
        supportTicketService.claimTicket(administrator, created.id(), created.version());
    Ticket repeated =
        supportTicketService.claimTicket(administrator, claimed.id(), claimed.version());

    assertEquals(
        3, supportTicketService.loadTicketConversation(administrator, created.id(), true).size());
    assertEquals(createdAt, repeated.createdAt());
    assertEquals(createdAt, ticketFor(owner, created.id(), false).createdAt());
    assertEquals(0L, ticketFor(owner, created.id(), false).unreadMessages());
    assertEquals(1, supportTicketService.loadTicketConversation(owner, created.id(), false).size());
    assertEquals(1L, replies.countFor(created.id()));

    Ticket requesterClosed =
        supportTicketService.closeOwnTicket(owner, repeated.id(), repeated.version());
    Ticket closed =
        supportTicketService.closeTicketAsAdministrator(
            administrator, requesterClosed.id(), requesterClosed.version());
    List<Reply> history =
        supportTicketService.loadTicketConversation(administrator, created.id(), true).stream()
            .filter(reply -> reply.kind().isHistory())
            .toList();

    assertEquals(
        List.of(Status.OPEN, Status.IN_PROGRESS, Status.CLOSED_BY_REQUESTER, Status.CLOSED),
        history.stream().map(Reply::nextStatus).toList());
    assertEquals(
        List.of(Status.OPEN, Status.IN_PROGRESS, Status.CLOSED_BY_REQUESTER),
        history.stream().skip(1).map(Reply::previousStatus).toList());
    assertEquals(testUserEntity.getEmail(), history.get(2).actor().email());
    assertEquals(
        twoFactorAuthorization.owner(administrator).email(), history.getLast().actor().email());
    assertEquals(createdAt, closed.createdAt());
    assertEquals(1, supportTicketService.loadTicketConversation(owner, created.id(), false).size());
    assertEquals(0L, ticketFor(otherAdministrator, created.id(), true).unreadMessages());
    Long closedId = closed.id();
    Long closedVersion = closed.version();

    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.closeTicketAsAdministrator(
                administrator, closedId, closedVersion));
    assertEquals(
        4,
        supportTicketService.loadTicketConversation(administrator, created.id(), true).stream()
            .filter(reply -> reply.kind().isHistory())
            .count());
  }

  @Test
  void adminReplyToOwnTicketRetainsSupportRole() {
    Ticket ticket = supportTicketService.createRecovery(administrator, "Ich benötige Hilfe.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    Ticket answered =
        supportTicketService.replyToTicket(
            administrator, claimed.id(), claimed.version(), "Ich prüfe die Anfrage.", false, true);
    supportTicketService.replyToTicket(
        administrator, answered.id(), answered.version(), "Danke für die Antwort.", false, false);

    assertEquals(
        List.of(ReplyAuthor.REQUESTER, ReplyAuthor.SUPPORT, ReplyAuthor.REQUESTER),
        supportTicketService.loadTicketConversation(administrator, ticket.id(), false).stream()
            .map(Reply::author)
            .toList());
  }

  @Test
  void assignedAdminsAndWatchingSuperadminsReceiveOnlyTheirSubscribedPublicMessages() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long assignedId = supportTicketService.administratorId(administrator);
    Long watcherId = supportTicketService.administratorId(superadmin);
    Long ticketId = ticket.id();

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.setSubscription(otherAdministrator, ticketId, true));
    supportTicketService.setSubscription(administrator, ticket.id(), true);
    supportTicketService.setSubscription(superadmin, ticket.id(), true);
    Ticket noted =
        supportTicketService.replyToTicket(
            administrator, claimed.id(), claimed.version(), "Intern.", true, true);

    verify(userNotificationService, never()).recordSupportReply(any(), any(), any());

    Ticket updated =
        supportTicketService.replyToTicket(
            owner, noted.id(), noted.version(), "Noch eine Frage.", false, false);

    verify(userNotificationService)
        .recordSupportReply(
            eq(SupportTicketService.ticketNumber(ticket.id())),
            any(),
            argThat(recipients ->
                recipients.size() == 2
                    && recipients.stream()
                        .map(UserEntity::getId)
                        .toList()
                        .containsAll(List.of(assignedId, watcherId))));
    supportTicketService.setSubscription(superadmin, ticket.id(), false);

    assertFalse(ticketFor(superadmin, ticket.id(), true).subscribed());
    supportTicketService.closeOwnTicket(owner, updated.id(), updated.version());

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.setSubscription(superadmin, ticketId, true));
  }

  @Test
  void unreadCursorOnlyAcknowledgesDisplayedRepliesInTheCurrentPerspective() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    Ticket answered =
        supportTicketService.replyToTicket(
            administrator, claimed.id(), claimed.version(), "Erste Antwort.", false, true);
    Reply first = supportTicketService.loadTicketConversation(owner, ticket.id(), false).getLast();
    Instant publicUpdate = ticketFor(owner, ticket.id(), false).updatedAt();
    Ticket noted =
        supportTicketService.replyToTicket(
            administrator, answered.id(), answered.version(), "Interner Vermerk.", true, true);
    Reply internal =
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true).getLast();

    assertEquals(publicUpdate, ticketFor(owner, ticket.id(), false).updatedAt());
    supportTicketService.replyToTicket(
        administrator, noted.id(), noted.version(), "Zweite Antwort.", false, true);
    HttpSession requesterSession = bindLiveSession(owner);
    HttpSession administratorSession = bindLiveSession(administrator);

    assertEquals(2, ticketFor(owner, ticket.id(), false).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertFalse(
        supportTicketService.markSeen(
            requesterSession, false, ticket.id(), internal.id(), ticket.version()));
    assertTrue(
        supportTicketService.markSeen(
            requesterSession, false, ticket.id(), first.id(), ticket.version()));
    assertFalse(
        supportTicketService.markSeen(
            requesterSession, false, ticket.id(), first.id(), ticket.version()));
    assertEquals(
        1,
        supportTicketService
            .liveView(requesterSession, false, ticket.id())
            .selected()
            .unreadMessages());
    Reply latest =
        supportTicketService.liveView(requesterSession, false, ticket.id()).messages().getLast();

    assertTrue(
        supportTicketService.markSeen(
            requesterSession,
            false,
            ticket.id(),
            latest.id(),
            ticketFor(administrator, ticket.id(), true).version()));
    assertEquals(0, ticketFor(owner, ticket.id(), false).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertTrue(
        supportTicketService.markSeen(
            administratorSession,
            true,
            ticket.id(),
            latest.id(),
            ticketFor(administrator, ticket.id(), true).version()));
    assertEquals(0, ticketFor(administrator, ticket.id(), true).unreadMessages());
    Long ticketId = ticket.id();

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.liveView(requesterSession, true, ticketId));
  }

  @Test
  void countersFollowStatusAssignmentAndWatchWithoutDiscardingUnreadMessages() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);

    assertEquals(1, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());

    assertEquals(0, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    supportTicketService.setSubscription(superadmin, ticket.id(), true);

    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    supportTicketService.setSubscription(superadmin, ticket.id(), false);

    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    supportTicketService.setSubscription(superadmin, ticket.id(), true);

    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    Ticket resolved = supportTicketService.closeOwnTicket(owner, claimed.id(), claimed.version());

    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(1, ticketFor(superadmin, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    supportTicketService.closeTicketAsAdministrator(
        administrator, resolved.id(), resolved.version());

    assertEquals(0, ticketFor(administrator, ticket.id(), true).unreadMessages());
    assertEquals(0, ticketFor(superadmin, ticket.id(), true).unreadMessages());
  }

  @Test
  void claimAndClosingRetireNotificationsAndRequesterClosureReachesAssigneeWithoutWatch() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Long adminId = supportTicketService.administratorId(administrator);
    Long otherId = supportTicketService.administratorId(otherAdministrator);

    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());

    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertEquals(0, notificationRecipients.countUnread(otherId, null));
    MockHttpServletRequest superadmin = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long watcherId = supportTicketService.administratorId(superadmin);
    supportTicketService.setSubscription(superadmin, ticket.id(), true);
    Ticket resolved = supportTicketService.closeOwnTicket(owner, ticket.id(), claimed.version());

    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertEquals(1, notificationRecipients.countUnread(watcherId, null));
    assertEquals(0, notificationRecipients.countUnread(otherId, null));
    Reply last =
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true).getLast();
    HttpSession adminSession = bindLiveSession(administrator);
    supportTicketService.markSeen(adminSession, true, ticket.id(), last.id(), claimed.version());

    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertTrue(
        supportTicketService.markSeen(
            adminSession, true, ticket.id(), last.id(), resolved.version()));
    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertFalse(
        supportTicketService.markSeen(
            adminSession, true, ticket.id(), last.id(), resolved.version()));
    assertEquals(1, notificationRecipients.countUnread(watcherId, null));
    supportTicketService.closeTicketAsAdministrator(
        administrator, resolved.id(), resolved.version());

    assertEquals(0, notificationRecipients.countUnread(watcherId, null));
    assertEquals(
        2,
        notificationRecipients
            .inbox(adminId, null, null, org.springframework.data.domain.PageRequest.of(0, 20))
            .getTotalElements());
  }

  @Test
  void requesterClosureBeforeClaimRemainsActionableForAllActiveAdministrators() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket resolved = supportTicketService.closeOwnTicket(owner, ticket.id(), ticket.version());

    assertEquals(
        1,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(administrator), null));
    assertEquals(
        1,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(otherAdministrator), null));
    assertEquals(1, ticketFor(otherAdministrator, ticket.id(), true).unreadMessages());
    Long ticketId = ticket.id();
    Long resolvedVersion = resolved.version();

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.closeOwnTicket(owner, ticketId, resolvedVersion));
    assertEquals(
        1,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(administrator), null));
    supportTicketService.closeTicketAsAdministrator(administrator, ticket.id(), resolved.version());

    assertEquals(
        0,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(otherAdministrator), null));
  }

  @Test
  void readingTheDisplayedMessageDoesNotAcknowledgeAnArrivingMessageAndUnwatchStopsNotifications() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    Long adminId = supportTicketService.administratorId(administrator);
    supportTicketService.setSubscription(administrator, ticket.id(), true);
    Ticket first =
        supportTicketService.replyToTicket(
            owner, ticket.id(), claimed.version(), "Erste Frage.", false, false);
    Reply displayed =
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true).getLast();
    Ticket second =
        supportTicketService.replyToTicket(
            owner, ticket.id(), first.version(), "Später eingetroffen.", false, false);

    assertEquals(2, notificationRecipients.countUnread(adminId, null));
    HttpSession httpSession = bindLiveSession(administrator);

    assertTrue(
        supportTicketService.markSeen(
            httpSession, true, ticket.id(), displayed.id(), first.version()));
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    assertFalse(
        supportTicketService.markSeen(
            httpSession, true, ticket.id(), displayed.id(), first.version()));
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    Reply latest =
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true).getLast();
    supportTicketService.markSeen(httpSession, true, ticket.id(), latest.id(), second.version());
    supportTicketService.setSubscription(administrator, ticket.id(), false);
    Ticket third =
        supportTicketService.replyToTicket(
            owner, ticket.id(), second.version(), "Ohne Beobachtung.", false, false);

    assertEquals(0, notificationRecipients.countUnread(adminId, null));
    assertEquals(1, ticketFor(administrator, ticket.id(), true).unreadMessages());
    supportTicketService.closeOwnTicket(owner, ticket.id(), third.version());

    assertEquals(1, notificationRecipients.countUnread(adminId, null));
  }

  @Test
  void revokedAndDisabledWatchersReceiveNeitherMessagesNorRequesterClosure() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    MockHttpServletRequest revoked = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    MockHttpServletRequest disabled = confirmedAdmin(RoleType.ROLE_SUPERADMIN);
    Long revokedId = supportTicketService.administratorId(revoked);
    Long disabledId = supportTicketService.administratorId(disabled);
    supportTicketService.setSubscription(revoked, ticket.id(), true);
    supportTicketService.setSubscription(disabled, ticket.id(), true);
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity formerAdmin = userRepository.findById(revokedId).orElseThrow();
      formerAdmin.setRoleEntities(List.of());
      UserEntity disabledAdmin = userRepository.findById(disabledId).orElseThrow();
      disabledAdmin.setEnabled(false);
    });
    Ticket updated =
        supportTicketService.replyToTicket(
            owner, ticket.id(), claimed.version(), "Neue Frage.", false, false);
    supportTicketService.closeOwnTicket(owner, ticket.id(), updated.version());

    assertEquals(0, notificationRecipients.countUnread(revokedId, null));
    assertEquals(0, notificationRecipients.countUnread(disabledId, null));
  }

  @Test
  void liveRefreshOnlyPublishesCommittedChanges() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    List<SupportTicketBroadcaster.SupportTicketChangeMessage> changes = new ArrayList<>();
    Registration listener = supportTicketService.events().register(changes::add);
    try {
      transactionTemplate.executeWithoutResult(status -> {
        supportTicketService.replyToTicket(
            owner, ticket.id(), ticket.version(), "Zurückgerollt.", false, false);

        assertTrue(changes.isEmpty());
        status.setRollbackOnly();
      });

      assertTrue(changes.isEmpty());
      supportTicketService.replyToTicket(
          owner, ticket.id(), ticket.version(), "Gespeichert.", false, false);

      assertEquals(
          List.of(
              new SupportTicketBroadcaster.SupportTicketChangeMessage(
                  testUserEntity.getId(), null)),
          changes);

      Reply latest =
          supportTicketService.loadTicketConversation(owner, ticket.id(), false).getLast();

      assertTrue(
          supportTicketService.markSeen(
              bindLiveSession(owner),
              false,
              ticket.id(),
              latest.id(),
              ticketFor(administrator, ticket.id(), true).version()));
      assertEquals(2, changes.size());
      SupportTicketBroadcaster.SupportTicketChangeMessage read = changes.getLast();

      assertTrue(read.affects(testUserEntity.getId(), false));
      assertFalse(read.affects(supportTicketService.administratorId(administrator), true));
    } finally {
      listener.remove();
    }
  }

  private Ticket ticketFor(
      MockHttpServletRequest mockHttpRequest, Long id, boolean administration) {
    if (administration) {
      return supportTicketService.findForAdministration(
          mockHttpRequest, SupportTicketService.ticketNumber(id).substring(1));
    }
    return supportTicketService.listTicketsForViewer(mockHttpRequest, administration).stream()
        .filter(ticket -> ticket.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private HttpSession bindLiveSession(MockHttpServletRequest mockHttpRequest) {
    HttpSession httpSession = mockHttpRequest.getSession();
    String email = (String) mockHttpRequest.getAttribute("fixture.identity");
    when(securityService.refreshSessionAuthentication(httpSession))
        .thenReturn(
            Optional.of(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of())));
    return httpSession;
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void liveTicketDataKeepsDevelopmentMailRestrictionWithoutAnotherRequest(
      boolean developmentAccount) {
    UserEntity requesterUserEntity = create(RoleType.ROLE_USER, developmentAccount);
    MockHttpServletRequest requesterHttpRequest =
        TwoFactorAuthorizationTest.requestFor(requesterUserEntity.getEmail());
    Ticket createdTicket =
        supportTicketService.createRecovery(requesterHttpRequest, "Hilfe bitte.");
    Ticket claimedTicket =
        supportTicketService.claimTicket(
            administrator, createdTicket.id(), createdTicket.version());
    HttpSession administratorHttpSession = bindLiveSession(administrator);

    supportTicketService.replyToTicket(
        requesterHttpRequest,
        claimedTicket.id(),
        claimedTicket.version(),
        "Neue Information",
        false,
        false);
    SupportTicketService.LiveView liveTicketView =
        supportTicketService.liveView(administratorHttpSession, true, createdTicket.id());

    assertEquals(developmentAccount, liveTicketView.selected().developmentAccount());
    assertTrue(
        liveTicketView.messages().stream()
            .anyMatch(reply -> reply.body().equals("Neue Information")));
    if (developmentAccount) {
      assertThrows(
          AccessDeniedException.class,
          () -> supportTicketService.validateInstructionPreview(administrator, createdTicket.id()));
      assertThrows(
          AccessDeniedException.class,
          () ->
              supportTicketService.sendTicketRecoveryInstructions(
                  administrator,
                  liveTicketView.selected().id(),
                  liveTicketView.selected().version()));
      verify(emailQueueService, never()).addAddressedNotification(any(), any(), any(), any());
    } else {
      supportTicketService.validateInstructionPreview(administrator, createdTicket.id());
    }
  }

  @Test
  void blankRequestAndBlankReplyDoNotChangeTheTicketVersion() {
    assertThrows(
        IllegalArgumentException.class, () -> supportTicketService.createRecovery(owner, " "));
    assertTrue(supportTicketService.listTicketsForViewer(owner, false).isEmpty());

    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    Long claimedId = claimed.id();
    Long claimedVersion = claimed.version();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            supportTicketService.replyToTicket(
                administrator, claimedId, claimedVersion, " ", false, true));
    supportTicketService.replyToTicket(
        administrator, claimed.id(), claimed.version(), "Ich prüfe das.", false, true);

    assertEquals(2, supportTicketService.loadTicketConversation(owner, ticket.id(), false).size());
  }

  @Test
  void requesterCanCloseOwnTicketAndAdministratorCanFinalizeItWithoutReopening() {
    Ticket open = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    MockHttpServletRequest foreign =
        TwoFactorAuthorizationTest.requestFor(create(RoleType.ROLE_USER).getEmail());
    Long openId = open.id();
    Long openVersion = open.version();

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.closeOwnTicket(foreign, openId, openVersion));
    Ticket resolved = supportTicketService.closeOwnTicket(owner, open.id(), open.version());

    assertEquals(Status.CLOSED_BY_REQUESTER, resolved.status());
    Long resolvedId = resolved.id();
    Long resolvedVersion = resolved.version();

    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.replyToTicket(
                owner, resolvedId, resolvedVersion, "Nachtrag", false, false));
    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.replyToTicket(
                administrator, resolvedId, resolvedVersion, "Nachtrag", false, true));
    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.sendTicketRecoveryInstructions(
                administrator, resolvedId, resolvedVersion));
    assertEquals(
        Status.OPEN, supportTicketService.createRecovery(owner, "Ein neues Anliegen.").status());
    Ticket closed =
        supportTicketService.closeTicketAsAdministrator(
            administrator, resolved.id(), resolved.version());

    assertEquals(Status.CLOSED, closed.status());
    Long closedId = closed.id();
    Long closedVersion = closed.version();

    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.replyToTicket(
                administrator, closedId, closedVersion, "Nachtrag", false, true));
    assertEquals(1, supportTicketService.loadTicketConversation(owner, closed.id(), false).size());
  }

  @Test
  void competingClaimAndStaleUpdatesCannotOverwriteAnAssignee() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    Long ticketId = ticket.id();
    Long ticketVersion = ticket.version();

    assertThrows(
        OptimisticLockingFailureException.class,
        () -> supportTicketService.claimTicket(otherAdministrator, ticketId, ticketVersion));
    Long claimedId = claimed.id();
    Long claimedVersion = claimed.version();

    assertThrows(
        IllegalStateException.class,
        () -> supportTicketService.claimTicket(otherAdministrator, claimedId, claimedVersion));
    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.closeTicketAsAdministrator(
                otherAdministrator, claimedId, claimedVersion));
  }

  @Test
  void rolledBackClaimKeepsOriginalNotificationAndTicketOpen() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe bitte.");
    Long adminId = supportTicketService.administratorId(administrator);
    transactionTemplate.executeWithoutResult(status -> {
      supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
      status.setRollbackOnly();
    });

    assertEquals(Status.OPEN, ticketFor(administrator, ticket.id(), true).status());
    assertEquals(
        List.of(Kind.TICKET_CREATED, Kind.MESSAGE),
        supportTicketService.loadTicketConversation(administrator, ticket.id(), true).stream()
            .map(Reply::kind)
            .toList());
    assertEquals(1, notificationRecipients.countUnread(adminId, null));
    supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());

    assertEquals(0, notificationRecipients.countUnread(adminId, null));
  }

  @Test
  void parallelClaimSucceedsForExactlyOneAdministrator() throws Exception {
    Ticket ticket = supportTicketService.createRecovery(owner, "Gemeinsame Anfrage.");
    java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    try (java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.function.Function<MockHttpServletRequest, Boolean> claim =
          actor -> {
            ready.countDown();
            try {
              assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
              supportTicketService.claimTicket(actor, ticket.id(), ticket.version());
              return true;
            } catch (OptimisticLockingFailureException | IllegalStateException _) {
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
    verify(userNotificationService, times(1))
        .retireTicketNotifications(
            SupportTicketService.ticketNumber(ticket.id()),
            de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED);
    assertEquals(
        0,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(administrator), null));
    assertEquals(
        0,
        notificationRecipients.countUnread(
            supportTicketService.administratorId(otherAdministrator), null));
  }

  @Test
  void restrictedFirstFactorCanAskForHelpButCannotReadAdministrationOrExceedLimits() {
    UserEntity required = create(RoleType.ROLE_ADMIN);
    MockHttpServletRequest firstFactor = TwoFactorAuthorizationTest.requestFor(required.getEmail());

    assertFalse(twoFactorAuthorization.sessionConfirmed(firstFactor));
    assertThrows(
        IllegalArgumentException.class,
        () -> supportTicketService.createRecovery(firstFactor, " "));
    String overlongMessage = "x".repeat(2001);

    assertThrows(
        IllegalArgumentException.class,
        () -> supportTicketService.createRecovery(firstFactor, overlongMessage));
    for (int attempt = 0; attempt < 3; attempt++) {
      Ticket ticket = supportTicketService.createRecovery(firstFactor, "Verlorener Authenticator.");

      assertFalse(twoFactorAuthorization.sessionConfirmed(firstFactor));
      assertThrows(
          AccessDeniedException.class,
          () -> supportTicketService.listTicketsForViewer(firstFactor, true));
      Ticket claimed =
          supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
      Ticket closed =
          supportTicketService.closeTicketAsAdministrator(
              administrator, claimed.id(), claimed.version());
      Long closedId = closed.id();
      Long closedVersion = closed.version();

      assertThrows(
          AccessDeniedException.class,
          () -> supportTicketService.claimTicket(administrator, closedId, closedVersion));
    }
    assertThrows(
        IllegalStateException.class,
        () -> supportTicketService.createRecovery(firstFactor, "Zu oft"));
  }

  @Test
  void preparedInstructionsUseVerifiedStoredContactAndNeverUnlockAnything() {
    Ticket ticket =
        supportTicketService.createRecovery(
            owner, "Bitte schicken Sie Hilfe an fremd@example.test.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    supportTicketService.validateInstructionPreview(administrator, claimed.id());
    String preview =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN)
            .getSupportRecoveryInstructions();

    assertTrue(preview.contains("Ersatzcode"));
    Ticket sent =
        supportTicketService.sendTicketRecoveryInstructions(
            administrator, claimed.id(), claimed.version());
    List<Reply> messages = supportTicketService.loadTicketConversation(owner, claimed.id(), false);

    assertEquals(Kind.RECOVERY_INSTRUCTIONS, messages.getLast().kind());
    assertEquals(preview, messages.getLast().body());
    assertTrue(preview.length() <= SupportTicketService.MESSAGE_LIMIT);
    Ticket copiedText =
        supportTicketService.replyToTicket(
            administrator, sent.id(), sent.version(), preview, false, true);

    assertEquals(
        Kind.MESSAGE,
        supportTicketService.loadTicketConversation(owner, claimed.id(), false).getLast().kind());
    verify(emailQueueService)
        .addAddressedNotification(any(), eq(testUserEntity.getEmail()), any(), any());
    assertTrue(sent.instructionSent());
    Long copiedTextId = copiedText.id();
    Long copiedTextVersion = copiedText.version();

    assertThrows(
        AccessDeniedException.class,
        () ->
            supportTicketService.sendTicketRecoveryInstructions(
                administrator, copiedTextId, copiedTextVersion));
    supportTicketService.closeTicketAsAdministrator(
        administrator, copiedText.id(), copiedText.version());

    assertThrows(
        AccessDeniedException.class,
        () -> supportTicketService.validateInstructionPreview(administrator, copiedTextId));
    assertFalse(twoFactorAuthorization.state(twoFactorAuthorization.owner(owner)).enrolled());
  }

  @Test
  void queueFailureKeepsInstructionsUnsentAndLeavesConversationUnchanged() {
    Ticket ticket = supportTicketService.createRecovery(owner, "Hilfe.");
    Ticket claimed = supportTicketService.claimTicket(administrator, ticket.id(), ticket.version());
    doThrow(new IllegalStateException("test queue failure"))
        .when(emailQueueService)
        .addAddressedNotification(any(), any(), any(), any());
    Long claimedId = claimed.id();
    Long claimedVersion = claimed.version();

    assertThrows(
        IllegalStateException.class,
        () ->
            supportTicketService.sendTicketRecoveryInstructions(
                administrator, claimedId, claimedVersion));
    assertFalse(
        supportTicketService.listTicketsForViewer(owner, false).getFirst().instructionSent());
    assertEquals(1, supportTicketService.loadTicketConversation(owner, claimed.id(), false).size());
  }

  private MockHttpServletRequest confirmedAdmin() {
    return confirmedAdmin(RoleType.ROLE_ADMIN);
  }

  private MockHttpServletRequest confirmedAdmin(RoleType role) {
    UserEntity adminUserEntity = create(role);
    MockHttpServletRequest mockHttpRequest =
        TwoFactorAuthorizationTest.requestFor(adminUserEntity.getEmail());
    byte[] seed = Totp.newSecret();
    UUID revision =
        twoFactorStore.initialize(adminUserEntity.getId(), adminUserEntity.getAccessVersion());
    List<String> codes =
        twoFactorStore
            .enroll(
                adminUserEntity.getId(),
                adminUserEntity.getAccessVersion(),
                revision,
                seed,
                Totp.code(seed, clock.instant().getEpochSecond() / 30),
                false)
            .recoveryCodes();
    twoFactorAuthorization.confirmSession(mockHttpRequest, codes.getFirst());
    return mockHttpRequest;
  }

  private UserEntity create(RoleType roleType) {
    return create(roleType, false);
  }

  private UserEntity create(RoleType roleType, boolean developmentAccount) {
    return transactionTemplate.execute(transactionStatus -> {
      RoleEntity roleEntity =
          entityManager
              .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
              .setParameter("name", roleType)
              .getResultStream()
              .findFirst()
              .orElse(null);
      if (roleEntity == null) {
        roleEntity = RoleEntity.builder().name(roleType).build();
        entityManager.persist(roleEntity);
      }
      return userRepository.saveAndFlush(
          UserEntity.builder()
              .email(UUID.randomUUID() + "@example.test")
              .firstName("Support")
              .lastName("Fixture")
              .preferredLocale(Locale.GERMAN)
              .enabled(true)
              .developmentAccount(developmentAccount)
              .roleEntities(List.of(roleEntity))
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
      ConfigService configService = mock(ConfigService.class);
      when(configService.getString(any()))
          .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
      when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://fixture.invalid/");
      return configService;
    }

    @Bean
    EmailQueueService queue() {
      return mock(EmailQueueService.class);
    }

    @Bean
    UserNotificationService notifications(
        UserNotificationRepository notificationRepository,
        UserNotificationRecipientRepository recipients,
        UserRepository userRepository,
        SecurityService securityService,
        ConfigService config,
        TwoFactorAuthorization twoFactorAuthorization) {
      return spy(
          new UserNotificationService(
              new UserNotificationPolicy(config),
              notificationRepository,
              recipients,
              userRepository,
              securityService,
              config,
              new UserNotificationBroadcaster(),
              twoFactorAuthorization));
    }

    @Bean
    AccountSecurityMail templates(ConfigService config) {
      return new AccountSecurityMail(config, new CustomI18NProvider());
    }

    @Bean
    SupportTicketService support(
        TwoFactorAuthorization twoFactorAuthorization,
        SupportTicketRepository tickets,
        SupportReplyRepository replies,
        SupportTicketViewerRepository viewers,
        UserRepository userRepository,
        EntityManagerFactory factory,
        UserNotificationService userNotificationService,
        AccountSecurityMail templates,
        EmailQueueService emailQueueService,
        ConfigService config,
        SupportTicketBroadcaster events) {
      return new SupportTicketService(
          twoFactorAuthorization,
          tickets,
          replies,
          viewers,
          userRepository,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          userNotificationService,
          templates,
          emailQueueService,
          new MaintenanceService(config),
          events);
    }
  }
}
