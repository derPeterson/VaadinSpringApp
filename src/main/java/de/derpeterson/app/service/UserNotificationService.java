package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.UserNotificationEntity;
import de.derpeterson.app.model.UserNotificationRecipientEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRecipientRepository.InboxRow;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceService.State;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import jakarta.servlet.http.HttpSession;
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
  private final UserNotificationPolicy policy;

  public OptionalInt retentionDays() {
    return policy.retentionDays();
  }

  private final UserNotificationRepository notifications;
  private final UserNotificationRecipientRepository recipients;
  private final UserRepository users;
  private final SecurityService security;
  private final ConfigService config;
  private final UserNotificationBroadcaster events;

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

  public record Target(Long id, String name, String email) {}

  public record Selection(boolean everyone, Set<Long> ids) {
    public Selection {
      ids = everyone || ids == null ? Set.of() : Set.copyOf(ids);
    }
  }

  public record Preview(Selection selection, List<Long> ids) {
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
      boolean read) {}

  public record Sent(Long id, String title, String body, Instant createdAt, int recipientCount) {}

  public MaintenanceService.State deliveryState() {
    return new MaintenanceService(config).state();
  }

  private void requireDeliveryOpen() {
    if (deliveryState() != MaintenanceService.State.OPEN) {
      throw new Rejected(Problem.DELIVERY_PAUSED);
    }
  }

  private UserEntity owner(HttpSession session) {
    UserEntity user =
        security
            .getSessionUser(session)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    if (!user.isEnabled() || user.isVerificationPending()) {
      throw new AccessDeniedException("Account unavailable");
    }
    State state = new MaintenanceService(config).state();
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
    } catch (AccessDeniedException denied) {
      return false;
    }
  }

  @Transactional(readOnly = true)
  public Long ownerId(HttpSession session) {
    return owner(session).getId();
  }

  public UserNotificationBroadcaster events() {
    return events;
  }

  @Transactional(readOnly = true)
  public Page<Item> inbox(HttpSession session, ReadFilter filter, int page, int size) {
    Boolean read =
        switch (Objects.requireNonNull(filter)) {
          case ALL -> null;
          case READ -> true;
          case UNREAD -> false;
        };
    return recipients
        .inbox(owner(session).getId(), read, policy.inboxCutoff(Instant.now()), page(page, size))
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
    return recipients
        .searchInbox(
            owner(session).getId(),
            read,
            policy.inboxCutoff(Instant.now()),
            pattern,
            Set.copyOf(matchingTypes),
            page(page, size))
        .map(UserNotificationService::toInboxItem);
  }

  private static Item toInboxItem(InboxRow recipient) {
    return new Item(
        recipient.getId(),
        recipient.getType(),
        recipient.getTitle(),
        recipient.getBody(),
        recipient.getCreatedAt(),
        recipient.getReadAt() != null);
  }

  @Transactional(readOnly = true)
  public long inboxCount(HttpSession session) {
    return recipients.countVisible(owner(session).getId(), policy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public long unreadCount(HttpSession session) {
    return recipients.countUnread(owner(session).getId(), policy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public long readCount(HttpSession session) {
    return recipients.countRead(owner(session).getId(), policy.inboxCutoff(Instant.now()));
  }

  @Transactional(readOnly = true)
  public List<Long> unreadSnapshot(HttpSession session) {
    return List.copyOf(
        recipients.unreadIds(owner(session).getId(), policy.inboxCutoff(Instant.now())));
  }

  @Transactional
  public void markRead(HttpSession session, Collection<Long> ids) {
    Long owner = owner(session).getId();
    List<Long> snapshot = List.copyOf(ids);
    // Kleine IN-Blöcke statt einer datenbankabhängigen Obergrenze bei langen Historien.
    int changed = 0;
    for (int i = 0; i < snapshot.size(); i += 500) {
      changed +=
          recipients.markRead(
              owner, snapshot.subList(i, Math.min(i + 500, snapshot.size())), Instant.now());
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
    if (recipients.dismiss(owner, Objects.requireNonNull(id), Instant.now()) > 0) {
      AfterCommitLog.debug(log, "Notification dismissed: userId={}, recipientId={}", owner, id);
      afterCommit(Set.of(owner));
    }
  }

  /** Entfernt alle sichtbaren gelesenen Empfangseinträge des Kontos, unabhängig von UI-Filtern. */
  @Transactional
  public void dismissRead(HttpSession session) {
    Long owner = owner(session).getId();
    Instant now = Instant.now();
    int changed = recipients.dismissRead(owner, policy.inboxCutoff(now), now);
    if (changed > 0) {
      AfterCommitLog.info(log, "Read notifications dismissed: userId={}, count={}", owner, changed);
      afterCommit(Set.of(owner));
    }
  }

  @Transactional(readOnly = true)
  public List<Target> targets(HttpSession session) {
    superadmin(session);
    return eligible();
  }

  private List<Target> eligible() {
    return users.findNotificationTargets().stream()
        .map(u -> new Target(u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()))
        .toList();
  }

  private List<Target> selected(Selection selection) {
    return eligible().stream()
        .filter(u -> selection.everyone() || selection.ids().contains(u.id()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Preview preview(HttpSession session, Selection selection) {
    superadmin(session);
    requireDeliveryOpen();
    List<Target> selected = selected(Objects.requireNonNull(selection));
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
      HttpSession session, UUID requestId, String title, String body, Preview preview) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(preview);
    // Dieselbe Sperrreihenfolge wie bei Rollen-/Kontomutationen; serialisiert auch Wiederholungen.
    users
        .lockRole(RoleType.ROLE_SUPERADMIN)
        .orElseThrow(() -> new IllegalStateException("Superadmin role missing"));
    UserEntity sender = superadmin(session);
    requireDeliveryOpen();
    title = clean(title, TITLE_LIMIT);
    body = clean(body, BODY_LIMIT);
    String key = "admin:" + requestId;
    Optional<UserNotificationEntity> existing = notifications.findByRequestKey(key);
    if (existing.isPresent()) {
      UserNotificationEntity m = existing.get();
      if (m.getSender() == null
          || !m.getSender().getId().equals(sender.getId())
          || !Objects.equals(title, m.getTitle())
          || !Objects.equals(body, m.getBody())
          || m.isBroadcast() != preview.selection().everyone()
          || !recipients.recipientIds(m.getId()).equals(preview.ids())) {
        throw new Rejected(Problem.REQUEST_CONFLICT);
      }
      log.debug("Notification send reused existing result: notificationId={}", m.getId());
      return sent(m);
    }
    List<Target> selected = selected(preview.selection());
    List<Long> ids = selected.stream().map(Target::id).toList();
    if (!ids.equals(preview.ids()) || ids.isEmpty()) {
      throw new Rejected(Problem.RECIPIENTS_CHANGED);
    }
    UserNotificationEntity m = new UserNotificationEntity();
    m.setRequestKey(key);
    m.setType(UserNotificationType.ADMIN_ANNOUNCEMENT);
    m.setTitle(title);
    m.setBody(body);
    m.setSender(sender);
    m.setBroadcast(preview.selection().everyone());
    create(m, selected.stream().map(t -> users.getReferenceById(t.id())).toList());
    return sent(m);
  }

  @Transactional(readOnly = true)
  public Page<Sent> sent(HttpSession session, int page, int size) {
    return notifications
        .sent(superadmin(session).getId(), policy.historyCutoff(Instant.now()), page(page, size))
        .map(this::sent);
  }

  private Sent sent(UserNotificationEntity m) {
    return new Sent(m.getId(), m.getTitle(), m.getBody(), m.getCreatedAt(), m.getRecipientCount());
  }

  private static String clean(String value, int max) {
    if (value == null || value.isBlank() || value.length() > max) {
      throw new Rejected(Problem.INVALID_DRAFT);
    }
    return value.strip();
  }

  private Pageable page(int number, int size) {
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
    create(m, List.of(user));
  }

  private void create(UserNotificationEntity m, List<UserEntity> selected) {
    m.setCreatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    m.setRecipientCount(selected.size());
    notifications.saveAndFlush(m);
    recipients.saveAllAndFlush(
        selected.stream()
            .map(
                u -> {
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
    } else {
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
              events.publish(ids);
            } catch (RuntimeException failure) {
              org.slf4j.LoggerFactory.getLogger(UserNotificationService.class)
                  .warn("Committed notifications await refresh", safeFailure(failure));
            }
          }
        });
  }
}
