package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.UserNotificationEntity;
import de.derpeterson.app.model.UserNotificationRecipientEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationRelevance;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRecipientRepository.InboxRow;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceService.State;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationService {
  public static final int TITLE_LIMIT = 120;
  public static final int BODY_LIMIT = 4000;
  private final UserNotificationPolicy userNotificationPolicy;

  public OptionalInt retentionDays() {
    return userNotificationPolicy.retentionDays();
  }

  private final UserNotificationRepository userNotificationRepository;
  private final UserNotificationRecipientRepository userNotificationRecipientRepository;
  private final UserRepository userRepository;
  private final SecurityService securityService;
  private final ConfigService configService;
  private final UserNotificationBroadcaster userNotificationBroadcaster;
  private final TwoFactorAuthorization twoFactor;

  public enum Problem {
    INVALID_DRAFT,
    NO_RECIPIENTS,
    RECIPIENTS_CHANGED,
    REQUEST_CONFLICT,
    DELIVERY_PAUSED
  }

  public static class Rejected extends RuntimeException {
    private final Problem problem;

    public Rejected(Problem problem) {
      super(problem.name());
      this.problem = problem;
    }

    public Problem problem() {
      return problem;
    }
  }

  public record Target(Long id, String name, String email) implements Serializable {}

  public record Selection(boolean everyone, Set<Long> ids) implements Serializable {
    public Selection {
      ids = everyone || ids == null ? Set.of() : Set.copyOf(ids);
    }
  }

  public record Preview(Selection selection, List<Long> ids) implements Serializable {
    public Preview {
      Objects.requireNonNull(selection);
      ids = List.copyOf(ids);
    }
  }

  public enum ReadFilter {
    ALL,
    READ,
    UNREAD
  }

  public record Item(
      Long id,
      UserNotificationType type,
      String title,
      String body,
      Instant createdAt,
      boolean read,
      String supportTicketNumber,
      UserNotificationRelevance relevance)
      implements Serializable {
    public Item(
        Long id,
        UserNotificationType type,
        String title,
        String body,
        Instant createdAt,
        boolean read) {
      this(id, type, title, body, createdAt, read, null, UserNotificationRelevance.ACTIVE);
    }

    public boolean unread() {
      return !read && relevance == UserNotificationRelevance.ACTIVE;
    }
  }

  public record Sent(Long id, String title, String body, Instant createdAt, int recipientCount) {}

  public MaintenanceService.State deliveryState() {
    return new MaintenanceService(configService).state();
  }

  private void requireDeliveryOpen() {
    if (deliveryState() != MaintenanceService.State.OPEN) {
      throw new Rejected(Problem.DELIVERY_PAUSED);
    }
  }

  private UserEntity owner(HttpSession session) {
    UserEntity user =
        securityService
            .getSessionUser(session)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    if (!user.isEnabled() || user.isVerificationPending()) {
      throw new AccessDeniedException("Account unavailable");
    }
    twoFactor.requireSession(session);
    State state = new MaintenanceService(configService).state();
    if (state != MaintenanceService.State.OPEN
        && (state != MaintenanceService.State.ACTIVE || !user.hasAdministrativeRole())) {
      throw new AccessDeniedException("Notifications paused for maintenance");
    }
    return user;
  }

  private UserEntity superadmin(HttpSession session) {
    UserEntity user = owner(session);
    if (!user.hasRole(RoleType.ROLE_SUPERADMIN)) {
      throw new AccessDeniedException("Superadmin required");
    }
    return user;
  }

  @Transactional(readOnly = true)
  public boolean canSend(HttpSession session) {
    try {
      superadmin(session);
      return true;
    } catch (AccessDeniedException _) {
      return false;
    }
  }

  @Transactional(readOnly = true)
  public boolean canOpenTickets(HttpSession session) {
    try {
      return owner(session).hasAdministrativeRole();
    } catch (AccessDeniedException _) {
      return false;
    }
  }

  @Transactional(readOnly = true)
  public Long ownerId(HttpSession session) {
    return owner(session).getId();
  }

  public UserNotificationBroadcaster events() {
    return userNotificationBroadcaster;
  }

  @Transactional(readOnly = true)
  public Page<Item> inbox(HttpSession session, ReadFilter filter, int page, int size) {
    Boolean read =
        switch (Objects.requireNonNull(filter)) {
          case ALL -> null;
          case READ -> true;
          case UNREAD -> false;
        };
    return userNotificationRecipientRepository
        .inbox(
            owner(session).getId(),
            read,
            userNotificationPolicy.inboxCutoff(Instant.now()),
            buildNotificationPageRequest(page, size))
        .map(UserNotificationService::toInboxItem);
  }

  @Transactional(readOnly = true)
  public Page<Item> searchInbox(
      HttpSession session,
      ReadFilter filter,
      String query,
      Set<UserNotificationType> matchingTypes,
      int page,
      int size) {
    Boolean read =
        switch (Objects.requireNonNull(filter)) {
          case ALL -> null;
          case READ -> true;
          case UNREAD -> false;
        };
    String term = Objects.requireNonNull(query).strip().toLowerCase(Locale.ROOT);
    if (term.length() > TITLE_LIMIT) {
      throw new IllegalArgumentException("Search is too long");
    }
    if (term.isEmpty()) {
      return inbox(session, filter, page, size);
    }
    // LIKE-Sonderzeichen bleiben wörtlicher Suchtext; ! ist das explizite Escapezeichen.
    String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    return userNotificationRecipientRepository
        .searchInbox(
            owner(session).getId(),
            read,
            userNotificationPolicy.inboxCutoff(Instant.now()),
            pattern,
            Set.copyOf(matchingTypes),
            buildNotificationPageRequest(page, size))
        .map(UserNotificationService::toInboxItem);
  }

  private static Item toInboxItem(InboxRow recipient) {
    return new Item(
        recipient.getId(),
        recipient.getType(),
        recipient.getTitle(),
        recipient.getBody(),
        recipient.getCreatedAt(),
        recipient.getReadAt() != null,
        recipient.getSupportTicketNumber(),
        recipient.getRelevance());
  }

  @Transactional(readOnly = true)
  public Optional<Item> inboxItem(HttpSession session, Long id) {
    return userNotificationRecipientRepository
        .visibleItem(owner(session).getId(), id, userNotificationPolicy.inboxCutoff(Instant.now()))
        .map(UserNotificationService::toInboxItem);
  }

  @Transactional(readOnly = true)
  public long inboxCount(HttpSession session) {
    return userNotificationRecipientRepository.countVisible(
        owner(session).getId(), userNotificationPolicy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public long unreadCount(HttpSession session) {
    return userNotificationRecipientRepository.countUnread(
        owner(session).getId(), userNotificationPolicy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public long readCount(HttpSession session) {
    return userNotificationRecipientRepository.countRead(
        owner(session).getId(), userNotificationPolicy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public List<Long> unreadSnapshot(HttpSession session) {
    return List.copyOf(
        userNotificationRecipientRepository.unreadIds(
            owner(session).getId(), userNotificationPolicy.inboxCutoff(Instant.now())));
  }

  @Transactional
  public void markRead(HttpSession session, Collection<Long> ids) {
    Long owner = owner(session).getId();
    List<Long> snapshot = List.copyOf(ids);
    // Kleine IN-Blöcke statt einer datenbankabhängigen Obergrenze bei langen Historien.
    int changed = 0;
    for (int recipientBatchStartIndex = 0;
        recipientBatchStartIndex < snapshot.size();
        recipientBatchStartIndex += 500) {
      changed +=
          userNotificationRecipientRepository.markRead(
              owner,
              snapshot.subList(
                  recipientBatchStartIndex,
                  Math.min(recipientBatchStartIndex + 500, snapshot.size())),
              Instant.now());
    }
    if (changed > 0) {
      AfterCommitLog.debug(log, "Notifications marked read: userId={}, count={}", owner, changed);
      afterCommit(Set.of(owner));
    }
  }

  /** Persönlich entfernen; der Versandnachweis bleibt für idempotente Wiederholungen erhalten. */
  @Transactional
  public void dismiss(HttpSession session, Long id) {
    Long owner = owner(session).getId();
    if (userNotificationRecipientRepository.dismiss(
            owner, Objects.requireNonNull(id), Instant.now())
        > 0) {
      AfterCommitLog.debug(log, "Notification dismissed: userId={}, recipientId={}", owner, id);
      afterCommit(Set.of(owner));
    }
  }

  /** Entfernt alle sichtbaren gelesenen Empfangseinträge des Kontos, unabhängig von UI-Filtern. */
  @Transactional
  public void dismissRead(HttpSession session) {
    Long owner = owner(session).getId();
    Instant now = Instant.now();
    int changed =
        userNotificationRecipientRepository.dismissRead(
            owner, userNotificationPolicy.inboxCutoff(now), now);
    if (changed > 0) {
      AfterCommitLog.info(log, "Read notifications dismissed: userId={}, count={}", owner, changed);
      afterCommit(Set.of(owner));
    }
  }

  @Transactional(readOnly = true)
  public List<Target> targets(HttpSession session) {
    superadmin(session);
    return findEligibleRecipientTargets();
  }

  private List<Target> findEligibleRecipientTargets() {
    return userRepository.findNotificationTargets().stream()
        .map(u -> new Target(u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()))
        .toList();
  }

  private List<Target> findSelectedRecipientTargets(Selection selection) {
    return findEligibleRecipientTargets().stream()
        .filter(u -> selection.everyone() || selection.ids().contains(u.id()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Preview preview(HttpSession session, Selection selection) {
    superadmin(session);
    requireDeliveryOpen();
    List<Target> selected = findSelectedRecipientTargets(Objects.requireNonNull(selection));
    if (selected.isEmpty()) {
      throw new Rejected(Problem.NO_RECIPIENTS);
    }
    if (!selection.everyone()
        && !new HashSet<>(selected.stream().map(Target::id).toList()).equals(selection.ids())) {
      throw new Rejected(Problem.RECIPIENTS_CHANGED);
    }
    return new Preview(selection, selected.stream().map(Target::id).toList());
  }

  @Transactional
  public Sent send(
      HttpSession httpSession,
      UUID requestId,
      String notificationTitle,
      String notificationBody,
      Preview preview) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(preview);
    // Dieselbe Sperrreihenfolge wie bei Rollen-/Kontomutationen; serialisiert auch Wiederholungen.
    userRepository
        .lockRole(RoleType.ROLE_SUPERADMIN)
        .orElseThrow(() -> new IllegalStateException("Superadmin role missing"));
    UserEntity senderUser = superadmin(httpSession);
    requireDeliveryOpen();
    notificationTitle = normalizeDraftText(notificationTitle, TITLE_LIMIT);
    notificationBody = normalizeDraftText(notificationBody, BODY_LIMIT);
    String requestKey = "admin:" + requestId;
    Optional<UserNotificationEntity> existingNotification =
        userNotificationRepository.findByRequestKey(requestKey);
    if (existingNotification.isPresent()) {
      UserNotificationEntity notificationEntity = existingNotification.get();
      if (notificationEntity.getSender() == null
          || !notificationEntity.getSender().getId().equals(senderUser.getId())
          || !Objects.equals(notificationTitle, notificationEntity.getTitle())
          || !Objects.equals(notificationBody, notificationEntity.getBody())
          || notificationEntity.isBroadcast() != preview.selection().everyone()
          || !userNotificationRecipientRepository
              .recipientIds(notificationEntity.getId())
              .equals(preview.ids())) {
        throw new Rejected(Problem.REQUEST_CONFLICT);
      }
      log.debug(
          "Notification send reused existing result: notificationId={}",
          notificationEntity.getId());
      return sent(notificationEntity);
    }
    List<Target> selectedRecipientTargets = findSelectedRecipientTargets(preview.selection());
    List<Long> recipientIds = selectedRecipientTargets.stream().map(Target::id).toList();
    if (!recipientIds.equals(preview.ids()) || recipientIds.isEmpty()) {
      throw new Rejected(Problem.RECIPIENTS_CHANGED);
    }
    UserNotificationEntity notificationEntity = new UserNotificationEntity();
    notificationEntity.setRequestKey(requestKey);
    notificationEntity.setType(UserNotificationType.ADMIN_ANNOUNCEMENT);
    notificationEntity.setTitle(notificationTitle);
    notificationEntity.setBody(notificationBody);
    notificationEntity.setSender(senderUser);
    notificationEntity.setBroadcast(preview.selection().everyone());
    persistNotificationAndRecipients(
        notificationEntity,
        selectedRecipientTargets.stream()
            .map(recipientTarget -> userRepository.getReferenceById(recipientTarget.id()))
            .toList());
    return sent(notificationEntity);
  }

  @Transactional(readOnly = true)
  public Page<Sent> sent(HttpSession session, int page, int size) {
    return userNotificationRepository
        .sent(
            superadmin(session).getId(),
            userNotificationPolicy.historyCutoff(Instant.now()),
            buildNotificationPageRequest(page, size))
        .map(this::sent);
  }

  private Sent sent(UserNotificationEntity m) {
    return new Sent(m.getId(), m.getTitle(), m.getBody(), m.getCreatedAt(), m.getRecipientCount());
  }

  private static String normalizeDraftText(String value, int max) {
    if (value == null || value.isBlank() || value.length() > max) {
      throw new Rejected(Problem.INVALID_DRAFT);
    }
    return value.strip();
  }

  private Pageable buildNotificationPageRequest(int number, int size) {
    if (number < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("Invalid page");
    }
    return PageRequest.of(number, size);
  }

  /**
   * Pflichtbestandteil der bereits gesperrten Kontoaktion, ohne eigenen Commit oder Mailversand.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordSecurityEvent(UserEntity user, UserNotificationType type) {
    if (type == null || type == UserNotificationType.ADMIN_ANNOUNCEMENT || user.getId() == null) {
      throw new IllegalArgumentException("Persisted security event required");
    }
    UserNotificationEntity m = new UserNotificationEntity();
    m.setRequestKey("security:" + UUID.randomUUID());
    m.setType(type);
    persistNotificationAndRecipients(m, List.of(user));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordRecoveryRequest(String ticketNumber, Long replyId) {
    List<UserEntity> administrators = userRepository.findEnabledAdministrators();
    if (administrators.isEmpty()) {
      return;
    }
    UserNotificationEntity event = new UserNotificationEntity();
    event.setRequestKey("recovery:" + ticketNumber);
    event.setType(UserNotificationType.RECOVERY_REQUEST);
    event.setSupportTicketNumber(ticketNumber);
    event.setSupportReplyId(replyId);
    persistNotificationAndRecipients(event, administrators);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordSupportReply(String ticketNumber, Long replyId, List<UserEntity> viewers) {
    UserNotificationEntity event = new UserNotificationEntity();
    event.setRequestKey("support-reply:" + replyId);
    event.setType(UserNotificationType.SUPPORT_REPLY);
    event.setSupportTicketNumber(ticketNumber);
    event.setSupportReplyId(replyId);
    persistNotificationAndRecipients(event, viewers);
  }

  /** Der Ticketdienst hält die Vorgangssperre; Zustandswechsel und Hinweise committen gemeinsam. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void retireTicketNotifications(String number, UserNotificationRelevance relevance) {
    Set<UserNotificationType> types =
        relevance == UserNotificationRelevance.TICKET_ASSIGNED
            ? Set.of(UserNotificationType.RECOVERY_REQUEST)
            : Set.of(
                UserNotificationType.RECOVERY_REQUEST,
                UserNotificationType.SUPPORT_REPLY,
                UserNotificationType.SUPPORT_REQUESTER_CLOSED);
    List<Long> owners =
        userNotificationRecipientRepository.ticketRecipients(number, types, relevance);
    if (!owners.isEmpty()
        && userNotificationRecipientRepository.retireTicket(number, types, relevance) > 0) {
      afterCommit(Set.copyOf(owners));
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void recordRequesterClosed(String number, Long version, List<UserEntity> viewers) {
    if (viewers.isEmpty()) {
      return;
    }
    UserNotificationEntity event = new UserNotificationEntity();
    event.setRequestKey("support-closed:" + number);
    event.setType(UserNotificationType.SUPPORT_REQUESTER_CLOSED);
    event.setSupportTicketNumber(number);
    event.setSupportTicketVersion(version);
    persistNotificationAndRecipients(event, viewers);
  }

  /**
   * Quittiert nur Nachrichten und Status, die in genau dieser Ticketansicht bereits sichtbar waren.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean markTicketSeen(Long owner, String number, Long replyId, Long version) {
    if (userNotificationRecipientRepository.markTicketRead(
            owner, number, replyId, version, Instant.now())
        == 0) {
      return false;
    }
    afterCommit(Set.of(owner));
    return true;
  }

  private void persistNotificationAndRecipients(
      UserNotificationEntity m, List<UserEntity> selected) {
    m.setCreatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    m.setRecipientCount(selected.size());
    userNotificationRepository.saveAndFlush(m);
    userNotificationRecipientRepository.saveAllAndFlush(
        selected.stream()
            .map(u -> {
              UserNotificationRecipientEntity r = new UserNotificationRecipientEntity();
              r.setNotification(m);
              r.setUser(u);
              return r;
            })
            .toList());
    if (m.getType() == UserNotificationType.ADMIN_ANNOUNCEMENT) {
      AfterCommitLog.info(
          log,
          "Admin notification created: notificationId={}, senderId={}, recipients={}",
          m.getId(),
          m.getSender().getId(),
          selected.size());
    } else if (m.getType() != UserNotificationType.SUPPORT_REPLY
        && m.getType() != UserNotificationType.SUPPORT_REQUESTER_CLOSED) {
      AfterCommitLog.info(
          log,
          "Account security event recorded: userId={}, type={}",
          selected.getFirst().getId(),
          m.getType());
    }
    afterCommit(new HashSet<>(selected.stream().map(UserEntity::getId).toList()));
  }

  private void afterCommit(Set<Long> ids) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Notification transaction required");
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              userNotificationBroadcaster.publish(ids);
            } catch (RuntimeException failure) {
              org.slf4j.LoggerFactory.getLogger(UserNotificationService.class)
                  .warn("Committed notifications await refresh", safeFailure(failure));
            }
          }
        });
  }
}
