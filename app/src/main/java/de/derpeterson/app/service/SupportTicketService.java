package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportReplyEntity;
import de.derpeterson.app.model.SupportTicketEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.SupportReplyRepository;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kontakt und Bearbeitung ohne jede Methode zum Entsperren oder Ändern eines Faktors. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {
  private final TwoFactorAuthorization authorization;
  private final SupportTicketRepository tickets;
  private final SupportReplyRepository replies;
  private final UserRepository users;
  private final EntityManager entityManager;
  private final UserNotificationService notifications;
  private final AccountSecurityMail templates;
  private final EmailQueueService queue;

  public record Ticket(
      Long id,
      Long version,
      String subject,
      Status status,
      Instant updatedAt,
      Long assignee,
      boolean instructionSent)
      implements java.io.Serializable {}

  public record Reply(String body, boolean internal, Instant createdAt)
      implements java.io.Serializable {}

  @Transactional(readOnly = true)
  public List<Ticket> list(HttpServletRequest request, boolean administration) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    if (administration) {
      requireAdministrator(request);
    }
    return (administration
            ? tickets.inbox(PageRequest.of(0, 100))
            : tickets.own(owner.id(), PageRequest.of(0, 20)))
        .stream().map(this::view).toList();
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
    if (tickets.openFor(owner.id(), Status.CLOSED) != 0
        || tickets.createdSince(owner.id(), Instant.now().minusSeconds(86400)) >= 3) {
      throw new IllegalStateException("Recovery request already open or rate limited");
    }
    String body = bounded(description, 2000);
    SupportTicketEntity ticket = new SupportTicketEntity();
    ticket.setOwner(user);
    ticket.setSubject(templates.texts(user).getSupportRecoverySubject());
    ticket.setCreatedAt(Instant.now());
    ticket.setUpdatedAt(ticket.getCreatedAt());
    tickets.saveAndFlush(ticket);
    append(ticket, user, body, false);
    notifications.recordRecoveryRequest();
    AfterCommitLog.info(
        log,
        "Recovery support request created: ticketId={}, accountId={}",
        ticket.getId(),
        owner.id());
    return view(ticket);
  }

  @Transactional(readOnly = true)
  public List<Reply> conversation(HttpServletRequest request, Long id, boolean administration) {
    accessible(request, id, administration);
    return replies.visible(id, administration).stream()
        .map(reply -> new Reply(reply.getBody(), reply.isInternal(), reply.getCreatedAt()))
        .toList();
  }

  @Transactional
  public Ticket claim(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner administrator = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    if (ticket.getStatus() == Status.CLOSED) {
      throw new AccessDeniedException("Closed ticket cannot be claimed");
    }
    if (ticket.getAssignee() != null && !ticket.getAssignee().getId().equals(administrator.id())) {
      throw new IllegalStateException("Ticket already assigned");
    }
    ticket.setAssignee(users.getReferenceById(administrator.id()));
    ticket.setStatus(Status.IN_PROGRESS);
    ticket.setUpdatedAt(Instant.now());
    tickets.saveAndFlush(ticket);
    return view(ticket);
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
    if (ticket.getStatus() == Status.CLOSED || !administration && internal) {
      throw new AccessDeniedException("Reply is not permitted");
    }
    if (administration) {
      requireAssignee(ticket, actor);
    }
    append(ticket, users.getReferenceById(actor.id()), bounded(text, 2000), internal);
    return view(ticket);
  }

  @Transactional
  public Ticket close(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    requireAssignee(ticket, actor);
    ticket.setStatus(Status.CLOSED);
    ticket.setUpdatedAt(Instant.now());
    tickets.saveAndFlush(ticket);
    return view(ticket);
  }

  @Transactional(readOnly = true)
  public String instructionPreview(HttpServletRequest request, Long id) {
    return templates
        .texts(accessible(request, id, true).getOwner())
        .getSupportRecoveryInstructions();
  }

  @Transactional
  public Ticket sendInstructions(HttpServletRequest request, Long id, Long version) {
    TwoFactorAuthorization.Owner actor = requireAdministrator(request);
    SupportTicketEntity ticket = locked(id, version);
    requireAssignee(ticket, actor);
    UserEntity recipient = ticket.getOwner();
    entityManager.refresh(recipient);
    if (ticket.getInstructionSentAt() != null
        || ticket.getStatus() == Status.CLOSED
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
    append(ticket, users.getReferenceById(actor.id()), body, false);
    ticket.setInstructionSentAt(Instant.now());
    tickets.saveAndFlush(ticket);
    AfterCommitLog.info(log, "Recovery instructions queued: ticketId={}", id);
    return view(ticket);
  }

  private SupportTicketEntity accessible(
      HttpServletRequest request, Long id, boolean administration) {
    TwoFactorAuthorization.Owner actor = authorization.owner(request);
    if (administration) {
      requireAdministrator(request);
    }
    SupportTicketEntity ticket =
        tickets.findById(id).orElseThrow(() -> new AccessDeniedException("Ticket unavailable"));
    if (!administration && !ticket.getOwner().getId().equals(actor.id())) {
      throw new AccessDeniedException("Own ticket required");
    }
    return ticket;
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
    SupportTicketEntity ticket =
        tickets.lock(id).orElseThrow(() -> new AccessDeniedException("Ticket unavailable"));
    entityManager.refresh(ticket);
    if (!Objects.equals(ticket.getVersion(), version)) {
      throw new org.springframework.dao.OptimisticLockingFailureException("Ticket changed");
    }
    return ticket;
  }

  private void requireAssignee(SupportTicketEntity ticket, TwoFactorAuthorization.Owner actor) {
    if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(actor.id())) {
      throw new AccessDeniedException("Claim the ticket first");
    }
  }

  private void append(
      SupportTicketEntity ticket, UserEntity author, String body, boolean internal) {
    if (replies.countFor(ticket.getId()) >= 50) {
      throw new IllegalStateException("Ticket conversation limit reached");
    }
    SupportReplyEntity reply = new SupportReplyEntity();
    reply.setTicket(ticket);
    reply.setAuthor(author);
    reply.setBody(body);
    reply.setInternal(internal);
    reply.setCreatedAt(Instant.now());
    replies.saveAndFlush(reply);
    ticket.setUpdatedAt(reply.getCreatedAt());
    tickets.saveAndFlush(ticket);
  }

  private Ticket view(SupportTicketEntity ticket) {
    return new Ticket(
        ticket.getId(),
        ticket.getVersion(),
        ticket.getSubject(),
        ticket.getStatus(),
        ticket.getUpdatedAt(),
        ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
        ticket.getInstructionSentAt() != null);
  }

  private static String bounded(String text, int maximum) {
    if (text == null || text.isBlank() || text.length() > maximum) {
      throw new IllegalArgumentException("Support text length invalid");
    }
    return text.strip();
  }
}
