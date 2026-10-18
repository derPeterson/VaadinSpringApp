package de.derpeterson.app.service;

import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.security.*;
import de.derpeterson.app.websocket.UserMessageBroadcaster;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

@Service
@RequiredArgsConstructor
public class UserMessageService {
  public static final int TITLE_LIMIT = 120;
  public static final int BODY_LIMIT = 4000;
  private final UserMessagePolicy policy;

  public int retentionDays() {
    return policy.retentionDays();
  }

  private final UserMessageRepository messages;
  private final UserMessageRecipientRepository recipients;
  private final UserRepository users;
  private final SecurityService security;
  private final ConfigService config;
  private final UserMessageBroadcaster events;

  public enum Problem {
    INVALID_DRAFT,
    NO_RECIPIENTS,
    RECIPIENTS_CHANGED,
    REQUEST_CONFLICT
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

  public record Item(
      Long id, UserMessageType type, String title, String body, Instant createdAt, boolean read) {}

  public record Sent(Long id, String title, String body, Instant createdAt, int recipientCount) {}

  private UserEntity owner(HttpSession session) {
    var user =
        security
            .getSessionUser(session)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    if (!user.isEnabled() || user.isVerificationPending())
      throw new AccessDeniedException("Account unavailable");
    var state = new MaintenanceService(config).state();
    if (state != MaintenanceService.State.OPEN
        && (state != MaintenanceService.State.ACTIVE || !user.hasAdministrativeRole()))
      throw new AccessDeniedException("Messages paused for maintenance");
    return user;
  }

  private UserEntity superadmin(HttpSession session) {
    var user = owner(session);
    if (!user.hasRole(RoleType.ROLE_SUPERADMIN))
      throw new AccessDeniedException("Superadmin required");
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

  public UserMessageBroadcaster events() {
    return events;
  }

  @Transactional(readOnly = true)
  public Page<Item> inbox(HttpSession session, boolean unread, int page, int size) {
    return recipients
        .inbox(owner(session).getId(), unread, page(page, size))
        .map(
            r ->
                new Item(
                    r.getId(),
                    r.getType(),
                    r.getTitle(),
                    r.getBody(),
                    r.getCreatedAt(),
                    r.getReadAt() != null));
  }

  @Transactional(readOnly = true)
  public long unreadCount(HttpSession session) {
    return recipients.countByUserIdAndReadAtIsNull(owner(session).getId());
  }

  @Transactional(readOnly = true)
  public List<Long> unreadSnapshot(HttpSession session) {
    return List.copyOf(recipients.unreadIds(owner(session).getId()));
  }

  @Transactional
  public void markRead(HttpSession session, Collection<Long> ids) {
    var owner = owner(session).getId();
    var snapshot = List.copyOf(ids);
    // Kleine IN-Blöcke statt einer datenbankabhängigen Obergrenze bei langen Historien.
    int changed = 0;
    for (int i = 0; i < snapshot.size(); i += 500)
      changed +=
          recipients.markRead(
              owner, snapshot.subList(i, Math.min(i + 500, snapshot.size())), Instant.now());
    if (changed > 0) afterCommit(Set.of(owner));
  }

  @Transactional(readOnly = true)
  public List<Target> targets(HttpSession session) {
    superadmin(session);
    return eligible();
  }

  private List<Target> eligible() {
    return users.findMessageTargets().stream()
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
    var selected = selected(Objects.requireNonNull(selection));
    if (selected.isEmpty()) throw new Rejected(Problem.NO_RECIPIENTS);
    if (!selection.everyone()
        && !new HashSet<>(selected.stream().map(Target::id).toList()).equals(selection.ids()))
      throw new Rejected(Problem.RECIPIENTS_CHANGED);
    return new Preview(selection, selected.stream().map(Target::id).toList());
  }

  @Transactional
  public Sent send(
      HttpSession session, UUID requestId, String title, String body, Preview preview) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(preview);
    // Dieselbe Sperrreihenfolge wie bei Rollen-/Kontomutationen; serialisiert auch Wiederholungen.
    users
        .lockRole(RoleType.ROLE_ADMIN)
        .orElseThrow(() -> new IllegalStateException("Admin role missing"));
    var sender = superadmin(session);
    title = clean(title, TITLE_LIMIT);
    body = clean(body, BODY_LIMIT);
    var key = "admin:" + requestId;
    var existing = messages.findByRequestKey(key);
    if (existing.isPresent()) {
      var m = existing.get();
      if (m.getSender() == null
          || !m.getSender().getId().equals(sender.getId())
          || !Objects.equals(title, m.getTitle())
          || !Objects.equals(body, m.getBody())
          || m.isBroadcast() != preview.selection().everyone()
          || !recipients.recipientIds(m.getId()).equals(preview.ids()))
        throw new Rejected(Problem.REQUEST_CONFLICT);
      return sent(m);
    }
    var selected = selected(preview.selection());
    var ids = selected.stream().map(Target::id).toList();
    if (!ids.equals(preview.ids()) || ids.isEmpty()) throw new Rejected(Problem.RECIPIENTS_CHANGED);
    var m = new UserMessageEntity();
    m.setRequestKey(key);
    m.setType(UserMessageType.ADMIN_MESSAGE);
    m.setTitle(title);
    m.setBody(body);
    m.setSender(sender);
    m.setBroadcast(preview.selection().everyone());
    create(m, selected.stream().map(t -> users.getReferenceById(t.id())).toList());
    return sent(m);
  }

  @Transactional(readOnly = true)
  public Page<Sent> sent(HttpSession session, int page) {
    return messages
        .findBySenderIdOrderByIdDesc(superadmin(session).getId(), page(page, 20))
        .map(this::sent);
  }

  private Sent sent(UserMessageEntity m) {
    return new Sent(m.getId(), m.getTitle(), m.getBody(), m.getCreatedAt(), m.getRecipientCount());
  }

  private static String clean(String value, int max) {
    if (value == null || value.isBlank() || value.length() > max)
      throw new Rejected(Problem.INVALID_DRAFT);
    return value.strip();
  }

  private Pageable page(int number, int size) {
    if (number < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page");
    return PageRequest.of(number, size);
  }

  /**
   * Pflichtbestandteil der bereits gesperrten Kontoaktion, ohne eigenen Commit oder Mailversand.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordSecurityEvent(UserEntity user, UserMessageType type) {
    if (type == null || type == UserMessageType.ADMIN_MESSAGE || user.getId() == null)
      throw new IllegalArgumentException("Persisted security event required");
    var m = new UserMessageEntity();
    m.setRequestKey("security:" + UUID.randomUUID());
    m.setType(type);
    create(m, List.of(user));
  }

  private void create(UserMessageEntity m, List<UserEntity> selected) {
    m.setCreatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    m.setRecipientCount(selected.size());
    messages.saveAndFlush(m);
    recipients.saveAllAndFlush(
        selected.stream()
            .map(
                u -> {
                  var r = new UserMessageRecipientEntity();
                  r.setMessage(m);
                  r.setUser(u);
                  return r;
                })
            .toList());
    afterCommit(new HashSet<>(selected.stream().map(UserEntity::getId).toList()));
  }

  private void afterCommit(Set<Long> ids) {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Message transaction required");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              events.publish(ids);
            } catch (RuntimeException failure) {
              org.slf4j.LoggerFactory.getLogger(UserMessageService.class)
                  .warn("Committed messages await refresh", failure);
            }
          }
        });
  }
}
