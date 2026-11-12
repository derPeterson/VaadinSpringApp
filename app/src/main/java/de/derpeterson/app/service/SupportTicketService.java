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
import de.derpeterson.app.repository.SupportTicketSpecifications;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Kontakt und Bearbeitung ohne jede Methode zum Entsperren oder Ändern eines Faktors. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {
  private static final String UPDATED_AT = "updatedAt";
  private static final String TICKET_UNAVAILABLE = "Ticket unavailable";
  public static final int MESSAGE_LIMIT = 2000;
  private static final long TICKET_NUMBER_OFFSET = 10_000L;

  private final TwoFactorAuthorization twoFactorAuthorization;
  private final SupportTicketRepository supportTicketRepository;
  private final SupportReplyRepository supportReplyRepository;
  private final SupportTicketViewerRepository supportTicketViewerRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;
  private final UserNotificationService userNotificationService;
  private final AccountSecurityMail accountSecurityMail;
  private final EmailQueueService emailQueueService;
  private final MaintenanceService maintenanceService;
  private final SupportTicketBroadcaster supportTicketBroadcaster;

  public record Ticket(
      Long id,
      Long version,
      String subject,
      Type type,
      Status status,
      Instant updatedAt,
      Instant createdAt,
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
      Long id,
      String body,
      boolean internal,
      Instant createdAt,
      ReplyAuthor author,
      Kind kind,
      Status previousStatus,
      Status nextStatus,
      UserIdentity actor)
      implements java.io.Serializable {}

  public record UserIdentity(String firstName, String lastName, String email)
      implements java.io.Serializable {}

  public record LiveView(
      Long viewerId,
      List<Ticket> visibleTickets,
      Ticket selected,
      List<Reply> messages,
      UserIdentity requester,
      long totalTickets,
      int page,
      int pageSize) {}

  public record InboxQuery(SupportTicketFilter filter, int page, int size) {
    public InboxQuery {
      Objects.requireNonNull(filter);
      if (page < 0 || !List.of(10, 20, 50).contains(size)) {
        throw new IllegalArgumentException("Unsupported ticket page");
      }
    }

    public static InboxQuery all() {
      return new InboxQuery(SupportTicketFilter.ALL, 0, 10);
    }
  }

  public record Activity(Long viewerId, boolean unread) {}

  public SupportTicketBroadcaster events() {
    return supportTicketBroadcaster;
  }

  public static String ticketNumber(Long id) {
    return "#" + (TICKET_NUMBER_OFFSET + id);
  }

  @Transactional(readOnly = true)
  public UserIdentity requesterIdentity(HttpServletRequest httpRequest) {
    return requesterIdentity(twoFactorAuthorization.owner(httpRequest).id());
  }

  private UserIdentity requesterIdentity(Long id) {
    UserEntity user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    return new UserIdentity(user.getFirstName(), user.getLastName(), user.getEmail());
  }

  @Transactional(readOnly = true)
  public List<Ticket> listTicketsForViewer(HttpServletRequest httpRequest, boolean administration) {
    Owner owner =
        administration
            ? requireAdministrator(httpRequest)
            : twoFactorAuthorization.owner(httpRequest);
    return listTicketsForViewer(owner, administration);
  }

  private List<Ticket> listTicketsForViewer(Owner owner, boolean administration) {
    return administration
        ? inboxView(owner, null, InboxQuery.all()).visibleTickets()
        : buildTicketViews(
            owner, supportTicketRepository.own(owner.id(), PageRequest.of(0, 20)), false);
  }

  /** Direkter Zugriff bleibt auf berechtigte Admins beschränkt, auch außerhalb der letzten 100. */
  @Transactional(readOnly = true)
  public Ticket findForAdministration(
      HttpServletRequest httpRequest, String requestedTicketNumber) {
    Owner owner = requireAdministrator(httpRequest);
    // Nur die sichtbare Dezimalnummer ohne Raute, Leerzeichen oder weitere URL-Anteile zulassen.
    if (requestedTicketNumber == null || !requestedTicketNumber.matches("\\d{1,19}")) {
      return null;
    }
    long ticketId;
    try {
      long publicTicketNumber = Long.parseLong(requestedTicketNumber);
      if (publicTicketNumber <= TICKET_NUMBER_OFFSET) {
        return null;
      }
      ticketId = publicTicketNumber - TICKET_NUMBER_OFFSET;
    } catch (NumberFormatException _) {
      return null;
    }

    return findTicket(owner, ticketId, true);
  }

  private Ticket findTicket(Owner owner, Long ticketId, boolean administration) {
    if (ticketId == null) {
      return null;
    }

    return supportTicketRepository
        .findById(ticketId)
        .map(ticketEntity ->
            buildTicketViews(owner, List.of(ticketEntity), administration).getFirst())
        .orElse(null);
  }

  private List<Ticket> buildTicketViews(
      Owner owner, List<SupportTicketEntity> visibleTicketEntities, boolean administration) {
    if (visibleTicketEntities.isEmpty()) {
      return List.of();
    }

    List<Long> visibleTicketIds =
        visibleTicketEntities.stream().map(SupportTicketEntity::getId).toList();
    Map<Long, Long> unreadReplyCountsByTicketId =
        supportReplyRepository.unreadCounts(visibleTicketIds, owner.id(), administration).stream()
            .collect(Collectors.toMap(UnreadCount::getTicketId, UnreadCount::getAmount));
    Set<Long> subscribedTicketIds =
        administration
            ? Set.copyOf(
                supportTicketViewerRepository.subscribedTickets(owner.id(), visibleTicketIds))
            : Set.of();

    return visibleTicketEntities.stream()
        .map(ticket ->
            buildTicketView(
                ticket,
                administration ? ticket.getUpdatedAt() : ticket.getPublicUpdatedAt(),
                unreadVisible(owner, ticket, administration, subscribedTicketIds)
                    ? unreadReplyCountsByTicketId.getOrDefault(ticket.getId(), 0L)
                    : 0L,
                subscribedTicketIds.contains(ticket.getId()),
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

  public Long administratorId(HttpServletRequest httpRequest) {
    return requireAdministrator(httpRequest).id();
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public LiveView liveView(HttpSession httpSession, boolean administration, Long selectedId) {
    return liveView(
        liveViewer(httpSession, administration), administration, selectedId, InboxQuery.all());
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public LiveView liveView(
      HttpSession httpSession, boolean administration, Long selectedId, InboxQuery query) {
    return liveView(liveViewer(httpSession, administration), administration, selectedId, query);
  }

  private LiveView liveView(
      Owner owner, boolean administration, Long selectedId, InboxQuery query) {
    if (administration) {
      return inboxView(owner, selectedId, query);
    }
    List<Ticket> values = listTicketsForViewer(owner, false);
    Ticket selected = selectTicket(values, selectedId, false);
    List<Reply> messages =
        selected == null ? List.of() : loadTicketConversation(selected.id(), false);
    UserIdentity requester = messages.isEmpty() ? null : requesterIdentity(owner.id());
    return new LiveView(owner.id(), values, selected, messages, requester, values.size(), 0, 20);
  }

  @Transactional(readOnly = true)
  public LiveView loadTicketInbox(
      HttpServletRequest httpRequest, Long selectedId, InboxQuery query) {
    return inboxView(requireAdministrator(httpRequest), selectedId, query);
  }

  private Specification<SupportTicketEntity> buildViewerTicketSpecification(
      Owner owner, SupportTicketFilter filter) {
    return SupportTicketSpecifications.buildTicketFilterSpecification(
        filter, owner.id(), owner.roles().contains(RoleType.ROLE_SUPERADMIN));
  }

  private LiveView inboxView(Owner owner, Long selectedId, InboxQuery query) {
    Specification<SupportTicketEntity> filter =
        buildViewerTicketSpecification(owner, query.filter());
    Sort order = Sort.by(Sort.Direction.DESC, UPDATED_AT, "id");
    long total = supportTicketRepository.count(filter);
    int page = (int) Math.clamp(query.page(), 0, Math.max(0, (total - 1) / query.size()));
    Page<SupportTicketEntity> result =
        supportTicketRepository.findAll(filter, PageRequest.of(page, query.size(), order));
    List<Ticket> values = buildTicketViews(owner, result.getContent(), true);
    Ticket selected =
        values.stream().filter(ticket -> ticket.id().equals(selectedId)).findFirst().orElse(null);
    if (selected == null) {
      selected = findTicket(owner, selectedId, true);
    }
    if (selected != null && !query.filter().matchesMetadata(selected, owner.id())) {
      selected = null;
    }
    List<Reply> messages =
        selected == null ? List.of() : loadTicketConversation(selected.id(), true);
    return new LiveView(
        owner.id(),
        values,
        selected,
        messages,
        null,
        result.getTotalElements(),
        page,
        query.size());
  }

  /**
   * Ermittelt die Listenseite eines berechtigten Vorgangs unter denselben Filter-/Sortierregeln.
   */
  @Transactional(readOnly = true)
  public int findTicketPageIndex(
      HttpServletRequest httpRequest, Long id, SupportTicketFilter filter, int size) {
    Owner owner = requireAdministrator(httpRequest);
    new InboxQuery(filter, 0, size);
    SupportTicketEntity selected =
        supportTicketRepository
            .findById(id)
            .orElseThrow(() -> new AccessDeniedException(TICKET_UNAVAILABLE));
    Specification<SupportTicketEntity> preceding =
        (ticket, query, cb) ->
            cb.or(
                cb.greaterThan(ticket.get(UPDATED_AT), selected.getUpdatedAt()),
                cb.and(
                    cb.equal(ticket.get(UPDATED_AT), selected.getUpdatedAt()),
                    cb.greaterThan(ticket.get("id"), id)));
    return Math.toIntExact(
        supportTicketRepository.count(buildViewerTicketSpecification(owner, filter).and(preceding))
            / size);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Activity loadTicketActivity(HttpSession httpSession) {
    Owner owner = liveViewer(httpSession, true);
    SupportTicketFilter unread =
        new SupportTicketFilter(
            Set.of(), SupportTicketFilter.Assignment.ALL, SupportTicketFilter.Reading.UNREAD, null);
    return new Activity(
        owner.id(), supportTicketRepository.exists(buildViewerTicketSpecification(owner, unread)));
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
  public void setSubscription(HttpServletRequest httpRequest, Long id, boolean subscribed) {
    Owner owner = requireAdministrator(httpRequest);
    SupportTicketEntity ticket = lockTicketEntity(id);
    if (!canSubscribe(owner, ticket)) {
      throw new AccessDeniedException("Subscription unavailable for this ticket");
    }
    SupportTicketViewerEntity ticketViewerEntity = findOrBuildTicketViewerEntity(id, owner.id());
    if (ticketViewerEntity.isSubscribed() != subscribed) {
      ticketViewerEntity.setSubscribed(subscribed);
      supportTicketViewerRepository.save(ticketViewerEntity);
      supportTicketBroadcaster.viewerChanged(owner.id());
    }
  }

  @Transactional
  public boolean markSeen(
      HttpSession httpSession,
      boolean administration,
      Long id,
      Long replyId,
      Long displayedVersion) {
    Owner owner = liveViewer(httpSession, administration);
    SupportTicketEntity ticket = lockTicketEntity(id);
    requireTicketAccess(owner, ticket, administration);
    SupportReplyEntity reply = supportReplyRepository.findById(replyId).orElse(null);
    if (reply == null
        || !id.equals(reply.getTicket().getId())
        || !administration && reply.isInternal()) {
      return false;
    }
    SupportTicketViewerEntity ticketViewerEntity = findOrBuildTicketViewerEntity(id, owner.id());
    long previous =
        administration
            ? ticketViewerEntity.getAdminReadThrough()
            : ticketViewerEntity.getRequesterReadThrough();
    boolean changed = replyId > previous;
    if (changed) {
      if (administration) {
        ticketViewerEntity.setAdminReadThrough(replyId);
      } else {
        ticketViewerEntity.setRequesterReadThrough(replyId);
      }
      supportTicketViewerRepository.save(ticketViewerEntity);
      supportTicketBroadcaster.viewerChanged(owner.id());
    }
    boolean notificationRead =
        administration
            && userNotificationService.markTicketSeen(
                owner.id(),
                ticketNumber(id),
                replyId,
                Math.min(ticket.getVersion(), displayedVersion));
    return changed || notificationRead;
  }

  private SupportTicketViewerEntity findOrBuildTicketViewerEntity(Long ticketId, Long userId) {
    return supportTicketViewerRepository
        .findByTicketIdAndUserId(ticketId, userId)
        .orElseGet(() -> {
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

  private Owner liveViewer(HttpSession httpSession, boolean administration) {
    Owner owner = twoFactorAuthorization.owner(httpSession);
    boolean administrator = owner.roles().stream().anyMatch(RoleType::isAdministrative);
    MaintenanceService.State state = maintenanceService.state();
    if (!owner.verifiedContact()
        || state == MaintenanceService.State.UNAVAILABLE
        || state == MaintenanceService.State.ACTIVE && !administrator
        || administration
            && (!administrator
                || !twoFactorAuthorization.administrationConfirmed(owner, httpSession))) {
      throw new AccessDeniedException("Ticket view unavailable");
    }
    return owner;
  }

  @Transactional
  public Ticket createRecovery(HttpServletRequest httpRequest, String description) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    UserEntity user = userRepository.findByIdForUpdate(owner.id()).orElseThrow();
    entityManager.refresh(user);
    if (!user.isEnabled()
        || !Objects.equals(user.getAccessVersion(), owner.accessVersion())
        || !owner.verifiedContact()) {
      throw new AccessDeniedException("Verified current account required");
    }
    if (supportTicketRepository.openFor(owner.id(), List.of(Status.OPEN, Status.IN_PROGRESS)) != 0
        || supportTicketRepository.createdSince(owner.id(), Instant.now().minusSeconds(86400))
            >= 3) {
      throw new IllegalStateException("Recovery request already open or rate limited");
    }
    String body = validateTicketMessageText(description, MESSAGE_LIMIT);
    SupportTicketEntity ticket = new SupportTicketEntity();
    ticket.setOwner(user);
    ticket.setSubject(
        accountSecurityMail.messagePropertiesForRecipient(user).getSupportRecoverySubject());
    ticket.setCreatedAt(Instant.now());
    ticket.setUpdatedAt(ticket.getCreatedAt());
    ticket.setPublicUpdatedAt(ticket.getCreatedAt());
    supportTicketRepository.saveAndFlush(ticket);
    appendHistory(ticket, user, Kind.TICKET_CREATED, null, Status.OPEN);
    SupportReplyEntity firstReply =
        appendTicketReply(ticket, user, body, ReplyAuthor.REQUESTER, false, Kind.MESSAGE);
    userNotificationService.recordRecoveryRequest(ticketNumber(ticket.getId()), firstReply.getId());
    AfterCommitLog.info(
        log,
        "Recovery support request created: ticketId={}, accountId={}",
        ticket.getId(),
        owner.id());
    return buildTicketView(ticket, false);
  }

  @Transactional(readOnly = true)
  public List<Reply> loadTicketConversation(
      HttpServletRequest httpRequest, Long id, boolean administration) {
    requireAccessibleTicketEntity(httpRequest, id, administration);
    return loadTicketConversation(id, administration);
  }

  private List<Reply> loadTicketConversation(Long id, boolean administration) {
    return supportReplyRepository.visible(id, administration).stream()
        .map(reply ->
            new Reply(
                reply.getId(),
                reply.getBody(),
                reply.isInternal(),
                reply.getCreatedAt(),
                reply.isSupportAuthor() ? ReplyAuthor.SUPPORT : ReplyAuthor.REQUESTER,
                reply.getKind(),
                reply.getPreviousStatus(),
                reply.getNextStatus(),
                administration && reply.getAuthor() != null
                    ? new UserIdentity(
                        reply.getAuthor().getFirstName(),
                        reply.getAuthor().getLastName(),
                        reply.getAuthor().getEmail())
                    : null))
        .toList();
  }

  @Transactional
  public Ticket claimTicket(HttpServletRequest httpRequest, Long id, Long version) {
    TwoFactorAuthorization.Owner administrator = requireAdministrator(httpRequest);
    SupportTicketEntity ticket = lockTicketEntityAtVersion(id, version);
    if (!ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Closed ticket cannot be claimed");
    }
    if (ticket.getAssignee() != null && !ticket.getAssignee().getId().equals(administrator.id())) {
      throw new IllegalStateException("Ticket already assigned");
    }
    Status previousStatus = ticket.getStatus();
    ticket.setAssignee(userRepository.getReferenceById(administrator.id()));
    ticket.setStatus(Status.IN_PROGRESS);
    ticket.setUpdatedAt(Instant.now());
    ticket.setPublicUpdatedAt(ticket.getUpdatedAt());
    supportTicketRepository.saveAndFlush(ticket);
    if (previousStatus != ticket.getStatus()) {
      appendHistory(
          ticket, ticket.getAssignee(), Kind.STATUS_CHANGED, previousStatus, ticket.getStatus());
    }
    userNotificationService.retireTicketNotifications(
        ticketNumber(ticket.getId()), UserNotificationRelevance.TICKET_ASSIGNED);
    supportTicketBroadcaster.ticketChanged(ticket.getOwner().getId());
    return buildTicketView(ticket, true);
  }

  @Transactional
  public Ticket replyToTicket(
      HttpServletRequest httpRequest,
      Long id,
      Long version,
      String text,
      boolean internal,
      boolean administration) {
    TwoFactorAuthorization.Owner actor = twoFactorAuthorization.owner(httpRequest);
    requireAccessibleTicketEntity(httpRequest, id, administration);
    SupportTicketEntity ticket = lockTicketEntityAtVersion(id, version);
    if (!ticket.getStatus().acceptsMessages() || !administration && internal) {
      throw new AccessDeniedException("Reply is not permitted");
    }
    if (administration) {
      requireAssignee(ticket, actor);
    }
    appendTicketReply(
        ticket,
        userRepository.getReferenceById(actor.id()),
        validateTicketMessageText(text, MESSAGE_LIMIT),
        administration ? ReplyAuthor.SUPPORT : ReplyAuthor.REQUESTER,
        internal,
        Kind.MESSAGE);
    return buildTicketView(ticket, administration);
  }

  @Transactional
  public Ticket closeTicketAsAdministrator(HttpServletRequest httpRequest, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(httpRequest);
    SupportTicketEntity ticket = lockTicketEntityAtVersion(id, version);
    if (ticket.getStatus() == Status.CLOSED) {
      throw new AccessDeniedException("Ticket already closed");
    }
    if (ticket.getStatus() == Status.CLOSED_BY_REQUESTER && ticket.getAssignee() == null) {
      ticket.setAssignee(userRepository.getReferenceById(actor.id()));
    } else {
      requireAssignee(ticket, actor);
    }
    return updateStatus(ticket, Status.CLOSED, true, actor.id());
  }

  @Transactional
  public Ticket closeOwnTicket(HttpServletRequest httpRequest, Long id, Long version) {
    requireAccessibleTicketEntity(httpRequest, id, false);
    SupportTicketEntity ticket = lockTicketEntityAtVersion(id, version);
    if (!ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Ticket already closed");
    }
    Ticket result =
        updateStatus(ticket, Status.CLOSED_BY_REQUESTER, false, ticket.getOwner().getId());
    Long assignee = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
    userNotificationService.recordRequesterClosed(
        ticketNumber(id),
        ticket.getVersion(),
        supportTicketViewerRepository.requesterClosedRecipients(
            id, assignee, ticket.getOwner().getId()));
    return result;
  }

  private Ticket updateStatus(
      SupportTicketEntity ticket, Status status, boolean administration, Long actorId) {
    Status previousStatus = ticket.getStatus();
    ticket.setStatus(status);
    ticket.setUpdatedAt(Instant.now());
    ticket.setPublicUpdatedAt(ticket.getUpdatedAt());
    supportTicketRepository.saveAndFlush(ticket);
    appendHistory(
        ticket,
        userRepository.getReferenceById(actorId),
        Kind.STATUS_CHANGED,
        previousStatus,
        status);
    userNotificationService.retireTicketNotifications(
        ticketNumber(ticket.getId()),
        status == Status.CLOSED
            ? UserNotificationRelevance.TICKET_CLOSED
            : UserNotificationRelevance.REQUESTER_CLOSED);
    supportTicketBroadcaster.ticketChanged(ticket.getOwner().getId());
    return buildTicketView(ticket, administration);
  }

  @Transactional(readOnly = true)
  public void validateInstructionPreview(HttpServletRequest httpRequest, Long id) {
    SupportTicketEntity ticket = requireAccessibleTicketEntity(httpRequest, id, true);
    if (ticket.getOwner().isDevelopmentAccount() || !ticket.getStatus().acceptsMessages()) {
      throw new AccessDeniedException("Instruction delivery unavailable");
    }
  }

  @Transactional(readOnly = true)
  public boolean belongsToDevelopmentAccount(HttpServletRequest httpRequest, Long ticketId) {
    return requireAccessibleTicketEntity(httpRequest, ticketId, true)
        .getOwner()
        .isDevelopmentAccount();
  }

  @Transactional
  public Ticket sendTicketRecoveryInstructions(
      HttpServletRequest httpRequest, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(httpRequest);
    SupportTicketEntity ticket = lockTicketEntityAtVersion(id, version);
    requireAssignee(ticket, actor);
    UserEntity recipient = ticket.getOwner();
    entityManager.refresh(recipient);
    if (ticket.getInstructionSentAt() != null
        || recipient.isDevelopmentAccount()
        || !ticket.getStatus().acceptsMessages()
        || !recipient.isEnabled()
        || recipient.isVerificationPending()) {
      throw new AccessDeniedException("Instruction delivery unavailable or already queued");
    }
    MessageProperties messageProperties =
        accountSecurityMail.messagePropertiesForRecipient(recipient);
    String body = messageProperties.getSupportRecoveryInstructions();
    emailQueueService.addAddressedNotification(
        recipient,
        recipient.getEmail(),
        messageProperties.getSupportRecoverySubject(),
        accountSecurityMail.render(
            "two_factor",
            recipient,
            Map.of(
                "TITLE",
                messageProperties.getSupportRecoverySubject(),
                "MESSAGE",
                body,
                "CODE",
                "",
                "NOTICE",
                messageProperties.getSupportNoUnlock())));
    appendTicketReply(
        ticket,
        userRepository.getReferenceById(actor.id()),
        body,
        ReplyAuthor.SUPPORT,
        false,
        Kind.RECOVERY_INSTRUCTIONS);
    ticket.setInstructionSentAt(Instant.now());
    supportTicketRepository.saveAndFlush(ticket);
    AfterCommitLog.info(log, "Recovery instructions queued: ticketId={}", id);
    return buildTicketView(ticket, true);
  }

  private SupportTicketEntity requireAccessibleTicketEntity(
      HttpServletRequest httpRequest, Long id, boolean administration) {
    TwoFactorAuthorization.Owner actor = twoFactorAuthorization.owner(httpRequest);
    if (administration) {
      requireAdministrator(httpRequest);
    }
    SupportTicketEntity ticket =
        supportTicketRepository
            .findById(id)
            .orElseThrow(() -> new AccessDeniedException(TICKET_UNAVAILABLE));
    requireTicketAccess(actor, ticket, administration);
    return ticket;
  }

  private void requireTicketAccess(
      Owner actor, SupportTicketEntity ticket, boolean administration) {
    if (!administration && !ticket.getOwner().getId().equals(actor.id())) {
      throw new AccessDeniedException("Own ticket required");
    }
  }

  private TwoFactorAuthorization.Owner requireAdministrator(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    if (owner.roles().stream().noneMatch(de.derpeterson.app.model.enums.RoleType::isAdministrative)
        || !twoFactorAuthorization.administrationConfirmed(httpRequest)) {
      throw new AccessDeniedException("Fully authenticated administrator required");
    }
    return owner;
  }

  private SupportTicketEntity lockTicketEntityAtVersion(Long id, Long version) {
    SupportTicketEntity ticket = lockTicketEntity(id);
    if (!Objects.equals(ticket.getVersion(), version)) {
      throw new org.springframework.dao.OptimisticLockingFailureException("Ticket changed");
    }
    return ticket;
  }

  private SupportTicketEntity lockTicketEntity(Long id) {
    SupportTicketEntity ticket =
        supportTicketRepository
            .lock(id)
            .orElseThrow(() -> new AccessDeniedException(TICKET_UNAVAILABLE));
    entityManager.refresh(ticket);
    return ticket;
  }

  private void requireAssignee(SupportTicketEntity ticket, TwoFactorAuthorization.Owner actor) {
    if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(actor.id())) {
      throw new AccessDeniedException("Claim the ticket first");
    }
  }

  private SupportReplyEntity appendTicketReply(
      SupportTicketEntity ticket,
      UserEntity author,
      String body,
      ReplyAuthor role,
      boolean internal,
      Kind kind) {
    if (supportReplyRepository.countFor(ticket.getId()) >= 50) {
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
    supportReplyRepository.saveAndFlush(reply);
    ticket.setUpdatedAt(reply.getCreatedAt());
    if (!internal) {
      ticket.setPublicUpdatedAt(reply.getCreatedAt());
    }
    supportTicketRepository.saveAndFlush(ticket);
    supportTicketBroadcaster.ticketChanged(ticket.getOwner().getId());
    if (!internal) {
      Long assignee = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
      List<UserEntity> recipients =
          supportTicketViewerRepository.recipients(ticket.getId(), assignee, author.getId());
      if (!recipients.isEmpty()) {
        userNotificationService.recordSupportReply(
            ticketNumber(ticket.getId()), reply.getId(), recipients);
      }
    }
    return reply;
  }

  /** Statusereignisse bleiben administrativ und lösen keine Nachrichtenzähler oder Hinweise aus. */
  private void appendHistory(
      SupportTicketEntity ticket,
      UserEntity actor,
      Kind kind,
      Status previousStatus,
      Status nextStatus) {
    SupportReplyEntity event = new SupportReplyEntity();
    event.setTicket(ticket);
    event.setAuthor(actor);
    event.setSupportAuthor(!actor.getId().equals(ticket.getOwner().getId()));
    event.setKind(kind);
    event.setPreviousStatus(previousStatus);
    event.setNextStatus(nextStatus);
    // Der sichtbare Text entsteht beim Lesen aus den typisierten Zuständen in der aktuellen
    // Sprache.
    event.setBody("");
    event.setInternal(true);
    event.setCreatedAt(kind == Kind.TICKET_CREATED ? ticket.getCreatedAt() : ticket.getUpdatedAt());
    supportReplyRepository.saveAndFlush(event);
  }

  private Ticket buildTicketView(SupportTicketEntity ticket, boolean administration) {
    return buildTicketView(ticket, ticket.getUpdatedAt(), 0L, false, false, administration);
  }

  private Ticket buildTicketView(
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
        ticket.getCreatedAt(),
        ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
        assigneeIdentity,
        ticket.getInstructionSentAt() != null,
        unread,
        subscribed && allowed,
        allowed,
        requesterIdentity);
  }

  private static String validateTicketMessageText(String text, int maximum) {
    if (text == null || text.isBlank() || text.length() > maximum) {
      throw new IllegalArgumentException("Support text length invalid");
    }
    return text.strip();
  }
}
