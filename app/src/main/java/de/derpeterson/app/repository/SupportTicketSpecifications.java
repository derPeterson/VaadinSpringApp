package de.derpeterson.app.repository;

import de.derpeterson.app.model.SupportReplyEntity;
import de.derpeterson.app.model.SupportReplyEntity.Kind;
import de.derpeterson.app.model.SupportTicketEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.SupportTicketViewerEntity;
import de.derpeterson.app.service.SupportTicketFilter;
import de.derpeterson.app.service.SupportTicketFilter.Reading;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Gleiche Datenbankbedingungen für Seiten, Gesamtzahl und relevante ungelesene Aktivitäten. */
public final class SupportTicketSpecifications {
  private SupportTicketSpecifications() {}

  public static Specification<SupportTicketEntity> matching(
      SupportTicketFilter filter, Long viewer, boolean superadmin) {
    return (ticket, query, cb) -> {
      List<Predicate> conditions = new ArrayList<>();
      if (!filter.statuses().isEmpty()) {
        conditions.add(ticket.get("status").in(filter.statuses()));
      }
      if (filter.type() != null) {
        conditions.add(cb.equal(ticket.get("type"), filter.type()));
      }
      switch (filter.assignment()) {
        case ALL -> {}
        case MINE -> conditions.add(cb.equal(ticket.get("assignee").get("id"), viewer));
        case UNASSIGNED -> conditions.add(cb.isNull(ticket.get("assignee")));
      }
      if (filter.reading() == Reading.UNREAD) {
        conditions.add(unread(ticket, query, cb, viewer, superadmin));
      }
      return cb.and(conditions.toArray(Predicate[]::new));
    };
  }

  private static Predicate unread(
      Root<SupportTicketEntity> ticket,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      Long viewer,
      boolean superadmin) {
    Subquery<Long> subscription = query.subquery(Long.class);
    Root<SupportTicketViewerEntity> watcher = subscription.from(SupportTicketViewerEntity.class);
    subscription
        .select(watcher.get("id"))
        .where(
            cb.equal(watcher.get("ticketId"), ticket.get("id")),
            cb.equal(watcher.get("userId"), viewer),
            cb.isTrue(watcher.get("subscribed")));
    Predicate relevant =
        cb.or(
            cb.equal(ticket.get("status"), Status.OPEN),
            cb.and(
                cb.equal(ticket.get("status"), Status.CLOSED_BY_REQUESTER),
                cb.isNull(ticket.get("assignee"))),
            cb.equal(ticket.get("assignee").get("id"), viewer),
            superadmin ? cb.exists(subscription) : cb.disjunction());

    Subquery<Long> readThrough = query.subquery(Long.class);
    Root<SupportTicketViewerEntity> state = readThrough.from(SupportTicketViewerEntity.class);
    readThrough
        .select(state.get("adminReadThrough"))
        .where(
            cb.equal(state.get("ticketId"), ticket.get("id")),
            cb.equal(state.get("userId"), viewer));
    Subquery<Long> messages = query.subquery(Long.class);
    Root<SupportReplyEntity> message = messages.from(SupportReplyEntity.class);
    messages
        .select(message.get("id"))
        .where(
            cb.equal(message.get("ticket"), ticket),
            message.get("kind").in(Kind.MESSAGE, Kind.RECOVERY_INSTRUCTIONS),
            cb.or(
                cb.isFalse(message.get("supportAuthor")),
                cb.isNull(message.get("author")),
                cb.notEqual(message.get("author").get("id"), viewer)),
            cb.greaterThan(message.get("id"), cb.coalesce(readThrough, 0L)));
    return cb.and(cb.notEqual(ticket.get("status"), Status.CLOSED), relevant, cb.exists(messages));
  }
}
