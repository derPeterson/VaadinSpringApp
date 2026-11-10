package de.derpeterson.app.service;

import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.SupportTicketEntity.Type;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/** Fachliche Auswahl für Vorgangsliste, Gesamtzahl und direkten Ticketsprung. */
public record SupportTicketFilter(
    Set<Status> statuses, Assignment assignment, Reading reading, Type type)
    implements Serializable {
  public enum Assignment {
    ALL,
    MINE,
    UNASSIGNED
  }

  public enum Reading {
    ALL,
    UNREAD
  }

  public static final SupportTicketFilter ALL =
      new SupportTicketFilter(Set.of(), Assignment.ALL, Reading.ALL, null);

  public SupportTicketFilter {
    statuses = Set.copyOf(statuses);
    Objects.requireNonNull(assignment);
    Objects.requireNonNull(reading);
  }

  public SupportTicketFilter withType(Type requestedTicketType) {
    return new SupportTicketFilter(statuses, assignment, reading, requestedTicketType);
  }

  /** Der Lesestand darf eine bereits geöffnete Detailansicht nicht schließen. */
  public boolean matchesMetadata(Ticket ticket, Long viewerUserId) {
    return (statuses.isEmpty() || statuses.contains(ticket.status()))
        && (type == null || ticket.type() == type)
        && switch (assignment) {
          case ALL -> true;
          case MINE -> viewerUserId != null && viewerUserId.equals(ticket.assignee());
          case UNASSIGNED -> ticket.assignee() == null;
        };
  }
}
