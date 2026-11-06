package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportReplyEntity;
import de.derpeterson.app.model.SupportReplyEntity.Kind;
import de.derpeterson.app.model.SupportTicketEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.SupportTicketEntity.Type;
import de.derpeterson.app.model.SupportTicketViewerEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationRelevance;
import de.derpeterson.app.repository.SupportReplyRepository;
import de.derpeterson.app.repository.SupportReplyRepository.UnreadCount;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.SupportTicketViewerRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization.Owner;
import de.derpeterson.app.websocket.SupportTicketBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Kontakt und Bearbeitung ohne jede Methode zum Entsperren oder Ändern eines Faktors. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {
  public static final int MESSAGE_LIMIT = 2000;
  private static final long TICKET_NUMBER_OFFSET = 10_000L;

  private final TwoFactorAuthorization authorization;
  private final SupportTicketRepository tickets;
  private final SupportReplyRepository replies;
  private final SupportTicketViewerRepository viewers;
  private final UserRepository users;
  private final EntityManager entityManager;
  private final UserNotificationService notifications;
  private final AccountSecurityMail templates;
  private final EmailQueueService queue;
  private final MaintenanceService maintenance;
  private final SupportTicketBroadcaster events;

  public record Ticket(
      Long id,
      Long version,
      String subject,
      Type type,
      Status status,
      Instant updatedAt,
      Long assignee,
      UserIdentity assigneeIdentity,
      boolean instructionSent,
      long unreadMessages,
      boolean subscribed,
      boolean subscriptionAllowed,
      UserIdentity requesterIdentity)
      implements java.io.Serializable {}

  public enum ReplyAuthor {
    REQUESTER,
    SUPPORT
  }

  public record Reply(
      Long id, String body, boolean internal, Instant createdAt, ReplyAuthor author, Kind kind)
      implements java.io.Serializable {}

  public record UserIdentity(String firstName, String lastName, String email)
      implements java.io.Serializable {}

  public record LiveView(
      Long viewerId,
      List<Ticket> tickets,
      Ticket selected,
      List<Reply> messages,
      UserIdentity requester) {}

  public SupportTicketBroadcaster events() {
    return events;
  }

  public static String ticketNumber(Long id) {
    return "#" + (TICKET_NUMBER_OFFSET + id);
  }

  @Transactional(readOnly = true)
  public UserIdentity requesterIdentity(HttpServletRequest request) {
    return requesterIdentity(authorization.owner(request).id());
  }

  private UserIdentity requesterIdentity(Long id) {
    UserEntity user =
        users.findById(id).orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    return new UserIdentity(user.getFirstName(), user.getLastName(), user.getEmail());
  }

  @Transactional(readOnly = true)
  public List<Ticket> list(HttpServletRequest request, boolean administration) {
    Owner owner = administration ? requireAdministrator(request) : authorization.owner(request);
    return list(owner, administration);
  }

  private List<Ticket> list(Owner owner, boolean administration) {
    List<SupportTicketEntity> visible =
        administration
            ? tickets.inbox(PageRequest.of(0, 100))
            : tickets.own(owner.id(), PageRequest.of(0, 20));
    if (visible.isEmpty()) {
      return List.of();
    }
    List<Long> ids = visible.stream().map(SupportTicketEntity::getId).toList();
    Map<Long, Long> unread =
        replies.unreadCounts(ids, owner.id(), administration).stream()
            .collect(Collectors.toMap(UnreadCount::getTicketId, UnreadCount::getAmount));
    Set<Long> subscribed =
        administration ? Set.copyOf(viewers.subscribedTickets(owner.id(), ids)) : Set.of();
    return visible.stream()
        .map(
            ticket ->
                view(
                    ticket,
                    administration ? ticket.getUpdatedAt() : ticket.getPublicUpdatedAt(),
                    unreadVisible(owner, ticket, administration, subscribed)
                        ? unread.getOrDefault(ticket.getId(), 0L)
                        : 0L,
                    subscribed.contains(ticket.getId()),
                    administration && canSubscribe(owner, ticket),
                    administration))
        .toList();
  }

  private boolean unreadVisible(
      Owner owner, SupportTicketEntity ticket, boolean administration, Set<Long> subscribed) {
    if (ticket.getStatus() == Status.CLOSED) {
      return false;
    }
    if (!administration
        || ticket.getStatus() == Status.OPEN
        || ticket.getStatus() == Status.CLOSED_BY_REQUESTER && ticket.getAssignee() == null) {
      return true;
    }
    return ticket.getAssignee() != null && owner.id().equals(ticket.getAssignee().getId())
        || subscribed.contains(ticket.getId()) && owner.roles().contains(RoleType.ROLE_SUPERADMIN);
  }

  public Long administratorId(HttpServletRequest request) {
    return requireAdministrator(request).id();
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public LiveView liveView(HttpSession session, boolean administration, Long selectedId) {
    Owner owner = liveViewer(session, administration);
    List<Ticket> values = list(owner, administration);
    Ticket selected = selectTicket(values, selectedId, administration);
    List<Reply> messages =
        selected == null ? List.of() : conversation(selected.id(), administration);
    UserIdentity requester =
        !administration && !messages.isEmpty() ? requesterIdentity(owner.id()) : null;
    return new LiveView(owner.id(), values, selected, messages, requester);
  }

  public static Ticket selectTicket(List<Ticket> values, Long selectedId, boolean administration) {
    Ticket selected =
        values.stream().filter(ticket -> ticket.id().equals(selectedId)).findFirst().orElse(null);
    if (selected == null && !administration && !values.isEmpty()) {
      return values.stream()
          .filter(ticket -> ticket.status().acceptsMessages())
          .findFirst()
          .orElse(values.getFirst());
    }
    return selected;
  }

  @Transactional
  public void setSubscription(HttpServletRequest request, Long id, boolean subscribed) {
    Owner owner = requireAdministrator(request);
    SupportTicketEntity ticket = lock(id);
    if (!canSubscribe(owner, ticket)) {
      throw new AccessDeniedException("Subscription unavailable for this ticket");
    }
    SupportTicketViewerEntity viewer = viewer(id, owner.id());
    if (viewer.isSubscribed() != subscribed) {
      viewer.setSubscribed(subscribed);
      viewers.save(viewer);
      events.viewerChanged(owner.id());
    }
  }

  @Transactional
  public boolean markSeen(
      HttpSession session, boolean administration, Long id, Long replyId, Long displayedVersion) {
    Owner owner = liveViewer(session, administration);
    SupportTicketEntity ticket = lock(id);
    requireTicketAccess(owner, ticket, administration);
    SupportReplyEntity reply = replies.findById(replyId).orElse(null);
    if (reply == null
        || !id.equals(reply.getTicket().getId())
        || !administration && reply.isInternal()) {
      return false;
    }
    SupportTicketViewerEntity viewer = viewer(id, owner.id());
    long previous =
        administration ? viewer.getAdminReadThrough() : viewer.getRequesterReadThrough();
    boolean changed = replyId > previous;
    if (changed) {
      if (administration) {
        viewer.setAdminReadThrough(replyId);
      } else {
        viewer.setRequesterReadThrough(replyId);
      }
      viewers.save(viewer);
      events.viewerChanged(owner.id());
    }
    boolean notificationRead =
        administration
            && notifications.markTicketSeen(
                owner.id(),
                ticketNumber(id),
                replyId,
                Math.min(ticket.getVersion(), displayedVersion));
    return changed || notificationRead;
  }

  private SupportTicketViewerEntity viewer(Long ticketId, Long userId) {
    return viewers
        .findByTicketIdAndUserId(ticketId, userId)
        .orElseGet(
            () -> {
              SupportTicketViewerEntity row = new SupportTicketViewerEntity();
              row.setTicketId(ticketId);
              row.setUserId(userId);
              return row;
            });
  }

  private boolean canSubscribe(Owner owner, SupportTicketEntity ticket) {
    return ticket.getStatus().acceptsMessages()
        && (owner.roles().contains(RoleType.ROLE_SUPERADMIN)
            || ticket.getAssignee() != null && owner.id().equals(ticket.getAssignee().getId()));
  }

  private Owner liveViewer(HttpSession session, boolean administration) {
    Owner owner = authorization.owner(session);
    boolean administrator = owner.roles().stream().anyMatch(RoleType::isAdministrative);
    MaintenanceService.State state = maintenance.state();
    if (!owner.verifiedContact()
        || state == MaintenanceService.State.UNAVAILABLE
        || state == MaintenanceService.State.ACTIVE && !administrator
        || administration
            && (!administrator || !authorization.administrationConfirmed(owner, session))) {
      throw new AccessDeniedException("Ticket view unavailable");
    }
    return owner;
  }

  @Transactional
  public Ticket createRecovery(HttpServletRequest request, String description) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    UserEntity user = users.findByIdForUpdate(owner.id()).orElseThrow();
    entityManager.refresh(user);
    if (!user.isEnabled()
        || !Objects.equals(user.getAccessVersion(), owner.accessVersion())
        || !owner.verifiedContact()) {
      throw new AccessDeniedException("Verified current account required");
    }
    if (tickets.openFor(owner.id(), List.of(Status.OPEN, Status.IN_PROGRESS)) != 0
        || tickets.createdSince(owner.id(), Instant.now().minusSeconds(86400)) >= 3) {
      throw new IllegalStateException("Recovery request already open or rate limited");
    }
    String body = bounded(description, MESSAGE_LIMIT);
    SupportTicketEntity ticket = new SupportTicketEntity();
    ticket.setOwner(user);
    ticket.setSubject(templates.texts(user).getSupportRecoverySubject());
    ticket.setCreatedAt(Instant.now());
    ticket.setUpdatedAt(ticket.getCreatedAt());
    ticket.setPublicUpdatedAt(ticket.getCreatedAt());
    tickets.saveAndFlush(ticket);
    SupportReplyEntity firstReply =
        append(ticket, user, body, ReplyAuthor.REQUESTER, false, Kind.MESSAGE);
    notifications.recordRecoveryRequest(ticketNumber(ticket.getId()), firstReply.getId());
    AfterCommitLog.info(
        log,
        "Recovery support request created: ticketId={}, accountId={}",
        ticket.getId(),
        owner.id());
    return view(ticket, false);
  }

  @Transactional(readOnly = true)
  public List<Reply> conversation(HttpServletRequest request, Long id, boolean administration) {
    accessible(request, id, administration);
    return conversation(id, administration);
  }

  private List<Reply> conversation(Long id, boolean administration) {
    return replies.visible(id, administration).stream()
        .map(
            reply ->
                new Reply(
                    reply.getId(),
                    reply.getBody(),
                    reply.isInternal(),
                    reply.getCreatedAt(),
                    reply.isSupportAuthor() ? ReplyAuthor.SUPPORT : ReplyAuthor.REQUESTER,
                    reply.getKind()))
        .toList();
  }

  @Transactional
  public Ticket claim(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner administrator = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    if (!ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Closed ticket cannot be claimed");
    }
    if (ticket.getAssignee() != null && !ticket.getAssignee().getId().equals(administrator.id())) {
      throw new IllegalStateException("Ticket already assigned");
    }
    ticket.setAssignee(users.getReferenceById(administrator.id()));
    ticket.setStatus(Status.IN_PROGRESS);
    ticket.setUpdatedAt(Instant.now());
    ticket.setPublicUpdatedAt(ticket.getUpdatedAt());
    tickets.saveAndFlush(ticket);
    notifications.retireTicketNotifications(
        ticketNumber(ticket.getId()), UserNotificationRelevance.TICKET_ASSIGNED);
    events.ticketChanged(ticket.getOwner().getId());
    return view(ticket, true);
  }

  @Transactional
  public Ticket reply(
      HttpServletRequest request,
      Long id,
      Long version,
      String text,
      boolean internal,
      boolean administration) {
    TwoFactorAuthorization.Owner actor = authorization.owner(request);
    accessible(request, id, administration);
    SupportTicketEntity ticket = locked(id, version);
    if (!ticket.getStatus().acceptsMessages() || !administration && internal) {
      throw new AccessDeniedException("Reply is not permitted");
    }
    if (administration) {
      requireAssignee(ticket, actor);
    }
    append(
        ticket,
        users.getReferenceById(actor.id()),
        bounded(text, MESSAGE_LIMIT),
        administration ? ReplyAuthor.SUPPORT : ReplyAuthor.REQUESTER,
        internal,
        Kind.MESSAGE);
    return view(ticket, administration);
  }

  @Transactional
  public Ticket close(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    if (ticket.getStatus() == Status.CLOSED) {
      throw new AccessDeniedException("Ticket already closed");
    }
    if (ticket.getStatus() == Status.CLOSED_BY_REQUESTER && ticket.getAssignee() == null) {
      ticket.setAssignee(users.getReferenceById(actor.id()));
    } else {
      requireAssignee(ticket, actor);
    }
    return updateStatus(ticket, Status.CLOSED, true);
  }

  @Transactional
  public Ticket closeOwn(HttpServletRequest request, Long id, Long version) {
    accessible(request, id, false);
    SupportTicketEntity ticket = locked(id, version);
    if (!ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Ticket already closed");
    }
    Ticket result = updateStatus(ticket, Status.CLOSED_BY_REQUESTER, false);
    Long assignee = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
    notifications.recordRequesterClosed(
        ticketNumber(id),
        ticket.getVersion(),
        viewers.requesterClosedRecipients(id, assignee, ticket.getOwner().getId()));
    return result;
  }

  private Ticket updateStatus(SupportTicketEntity ticket, Status status, boolean administration) {
    ticket.setStatus(status);
    ticket.setUpdatedAt(Instant.now());
    ticket.setPublicUpdatedAt(ticket.getUpdatedAt());
    tickets.saveAndFlush(ticket);
    notifications.retireTicketNotifications(
        ticketNumber(ticket.getId()),
        status == Status.CLOSED
            ? UserNotificationRelevance.TICKET_CLOSED
            : UserNotificationRelevance.REQUESTER_CLOSED);
    events.ticketChanged(ticket.getOwner().getId());
    return view(ticket, administration);
  }

  @Transactional(readOnly = true)
  public String instructionPreview(HttpServletRequest request, Long id) {
    SupportTicketEntity ticket = accessible(request, id, true);
    if (!ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Instruction delivery unavailable");
    }
    return templates.texts(ticket.getOwner()).getSupportRecoveryInstructions();
  }

  @Transactional
  public Ticket sendInstructions(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    requireAssignee(ticket, actor);
    UserEntity recipient = ticket.getOwner();
    entityManager.refresh(recipient);
    if (ticket.getInstructionSentAt() != null
        || !ticket.getStatus().acceptsMessages()
        || !recipient.isEnabled()
        || recipient.isVerificationPending()) {
      throw new AccessDeniedException("Instruction delivery unavailable or already queued");
    }
    MessageProperties texts = templates.texts(recipient);
    String body = texts.getSupportRecoveryInstructions();
    queue.addAddressedNotification(
        recipient,
        recipient.getEmail(),
        texts.getSupportRecoverySubject(),
        templates.render(
            "two_factor",
            recipient,
            Map.of(
                "TITLE",
                texts.getSupportRecoverySubject(),
                "MESSAGE",
                body,
                "CODE",
                "",
                "NOTICE",
                texts.getSupportNoUnlock())));
    append(
        ticket,
        users.getReferenceById(actor.id()),
        body,
        ReplyAuthor.SUPPORT,
        false,
        Kind.RECOVERY_INSTRUCTIONS);
    ticket.setInstructionSentAt(Instant.now());
    tickets.saveAndFlush(ticket);
    AfterCommitLog.info(log, "Recovery instructions queued: ticketId={}", id);
    return view(ticket, true);
  }

  private SupportTicketEntity accessible(
      HttpServletRequest request, Long id, boolean administration) {
    TwoFactorAuthorization.Owner actor = authorization.owner(request);
    if (administration) {
      requireAdministrator(request);
    }
    SupportTicketEntity ticket =
        tickets.findById(id).orElseThrow(() -> new AccessDeniedException("Ticket unavailable"));
    requireTicketAccess(actor, ticket, administration);
    return ticket;
  }

  private void requireTicketAccess(
      Owner actor, SupportTicketEntity ticket, boolean administration) {
    if (!administration && !ticket.getOwner().getId().equals(actor.id())) {
      throw new AccessDeniedException("Own ticket required");
    }
  }

  private TwoFactorAuthorization.Owner requireAdministrator(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    if (owner.roles().stream().noneMatch(de.derpeterson.app.model.enums.RoleType::isAdministrative)
        || !authorization.administrationConfirmed(request)) {
      throw new AccessDeniedException("Fully authenticated administrator required");
    }
    return owner;
  }

  private SupportTicketEntity locked(Long id, Long version) {
    SupportTicketEntity ticket = lock(id);
    if (!Objects.equals(ticket.getVersion(), version)) {
      throw new org.springframework.dao.OptimisticLockingFailureException("Ticket changed");
    }
    return ticket;
  }

  private SupportTicketEntity lock(Long id) {
    SupportTicketEntity ticket =
        tickets.lock(id).orElseThrow(() -> new AccessDeniedException("Ticket unavailable"));
    entityManager.refresh(ticket);
    return ticket;
  }

  private void requireAssignee(SupportTicketEntity ticket, TwoFactorAuthorization.Owner actor) {
    if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(actor.id())) {
      throw new AccessDeniedException("Claim the ticket first");
    }
  }

  private SupportReplyEntity append(
      SupportTicketEntity ticket,
      UserEntity author,
      String body,
      ReplyAuthor role,
      boolean internal,
      Kind kind) {
    if (replies.countFor(ticket.getId()) >= 50) {
      throw new IllegalStateException("Ticket conversation limit reached");
    }
    SupportReplyEntity reply = new SupportReplyEntity();
    reply.setTicket(ticket);
    reply.setAuthor(author);
    reply.setSupportAuthor(role == ReplyAuthor.SUPPORT);
    reply.setKind(kind);
    reply.setBody(body);
    reply.setInternal(internal);
    reply.setCreatedAt(Instant.now());
    replies.saveAndFlush(reply);
    ticket.setUpdatedAt(reply.getCreatedAt());
    if (!internal) {
      ticket.setPublicUpdatedAt(reply.getCreatedAt());
    }
    tickets.saveAndFlush(ticket);
    events.ticketChanged(ticket.getOwner().getId());
    if (!internal) {
      Long assignee = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
      List<UserEntity> recipients = viewers.recipients(ticket.getId(), assignee, author.getId());
      if (!recipients.isEmpty()) {
        notifications.recordSupportReply(ticketNumber(ticket.getId()), reply.getId(), recipients);
      }
    }
    return reply;
  }

  private Ticket view(SupportTicketEntity ticket, boolean administration) {
    return view(ticket, ticket.getUpdatedAt(), 0L, false, false, administration);
  }

  private Ticket view(
      SupportTicketEntity ticket,
      Instant updatedAt,
      long unread,
      boolean subscribed,
      boolean allowed,
      boolean administration) {
    UserEntity assignee = ticket.getAssignee();
    UserIdentity assigneeIdentity = null;
    if (administration && assignee != null) {
      assigneeIdentity =
          new UserIdentity(assignee.getFirstName(), assignee.getLastName(), assignee.getEmail());
    }
    UserIdentity requesterIdentity = null;
    if (administration) {
      UserEntity requester = ticket.getOwner();
      requesterIdentity =
          new UserIdentity(requester.getFirstName(), requester.getLastName(), requester.getEmail());
    }
    return new Ticket(
        ticket.getId(),
        ticket.getVersion(),
        ticket.getSubject(),
        ticket.getType(),
        ticket.getStatus(),
        updatedAt,
        ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
        assigneeIdentity,
        ticket.getInstructionSentAt() != null,
        unread,
        subscribed && allowed,
        allowed,
        requesterIdentity);
  }

  private static String bounded(String text, int maximum) {
    if (text == null || text.isBlank() || text.length() > maximum) {
      throw new IllegalArgumentException("Support text length invalid");
    }
    return text.strip();
  }
}
