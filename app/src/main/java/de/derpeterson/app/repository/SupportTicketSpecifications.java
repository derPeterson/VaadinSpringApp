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
  private static final String STATUS = "status";
  private static final String ASSIGNEE = "assignee";

  private SupportTicketSpecifications() {}

  public static Specification<SupportTicketEntity> buildTicketFilterSpecification(
      SupportTicketFilter ticketFilter, Long viewerUserId, boolean isSuperadmin) {
    return (ticketRoot, criteriaQuery, criteriaBuilder) -> {
      List<Predicate> ticketPredicates = new ArrayList<>();
      if (!ticketFilter.statuses().isEmpty()) {
        ticketPredicates.add(ticketRoot.get(STATUS).in(ticketFilter.statuses()));
      }
      if (ticketFilter.type() != null) {
        ticketPredicates.add(criteriaBuilder.equal(ticketRoot.get("type"), ticketFilter.type()));
      }

      switch (ticketFilter.assignment()) {
        case ALL -> {
          // Ohne Zuständigkeitsfilter bleibt die Auswahl unverändert.
        }
        case MINE ->
            ticketPredicates.add(
                criteriaBuilder.equal(ticketRoot.get(ASSIGNEE).get("id"), viewerUserId));
        case UNASSIGNED -> ticketPredicates.add(criteriaBuilder.isNull(ticketRoot.get(ASSIGNEE)));
      }

      if (ticketFilter.reading() == Reading.UNREAD) {
        ticketPredicates.add(
            buildUnreadTicketPredicate(
                ticketRoot, criteriaQuery, criteriaBuilder, viewerUserId, isSuperadmin));
      }

      return criteriaBuilder.and(ticketPredicates.toArray(Predicate[]::new));
    };
  }

  private static Predicate buildUnreadTicketPredicate(
      Root<SupportTicketEntity> ticketRoot,
      CriteriaQuery<?> criteriaQuery,
      CriteriaBuilder criteriaBuilder,
      Long viewerUserId,
      boolean isSuperadmin) {
    Subquery<Long> subscriptionSubquery = criteriaQuery.subquery(Long.class);
    Root<SupportTicketViewerEntity> subscriptionViewerRoot =
        subscriptionSubquery.from(SupportTicketViewerEntity.class);
    subscriptionSubquery
        .select(subscriptionViewerRoot.get("id"))
        .where(
            criteriaBuilder.equal(subscriptionViewerRoot.get("ticketId"), ticketRoot.get("id")),
            criteriaBuilder.equal(subscriptionViewerRoot.get("userId"), viewerUserId),
            criteriaBuilder.isTrue(subscriptionViewerRoot.get("subscribed")));

    Predicate relevantTicketPredicate =
        criteriaBuilder.or(
            criteriaBuilder.equal(ticketRoot.get(STATUS), Status.OPEN),
            criteriaBuilder.and(
                criteriaBuilder.equal(ticketRoot.get(STATUS), Status.CLOSED_BY_REQUESTER),
                criteriaBuilder.isNull(ticketRoot.get(ASSIGNEE))),
            criteriaBuilder.equal(ticketRoot.get(ASSIGNEE).get("id"), viewerUserId),
            isSuperadmin
                ? criteriaBuilder.exists(subscriptionSubquery)
                : criteriaBuilder.disjunction());

    Subquery<Long> readThroughSubquery = criteriaQuery.subquery(Long.class);
    Root<SupportTicketViewerEntity> viewerStateRoot =
        readThroughSubquery.from(SupportTicketViewerEntity.class);
    readThroughSubquery
        .select(viewerStateRoot.get("adminReadThrough"))
        .where(
            criteriaBuilder.equal(viewerStateRoot.get("ticketId"), ticketRoot.get("id")),
            criteriaBuilder.equal(viewerStateRoot.get("userId"), viewerUserId));

    Subquery<Long> unreadMessagesSubquery = criteriaQuery.subquery(Long.class);
    Root<SupportReplyEntity> unreadReplyRoot =
        unreadMessagesSubquery.from(SupportReplyEntity.class);
    unreadMessagesSubquery
        .select(unreadReplyRoot.get("id"))
        .where(
            criteriaBuilder.equal(unreadReplyRoot.get("ticket"), ticketRoot),
            unreadReplyRoot.get("kind").in(Kind.MESSAGE, Kind.RECOVERY_INSTRUCTIONS),
            criteriaBuilder.or(
                criteriaBuilder.isFalse(unreadReplyRoot.get("supportAuthor")),
                criteriaBuilder.isNull(unreadReplyRoot.get("author")),
                criteriaBuilder.notEqual(unreadReplyRoot.get("author").get("id"), viewerUserId)),
            criteriaBuilder.greaterThan(
                unreadReplyRoot.get("id"), criteriaBuilder.coalesce(readThroughSubquery, 0L)));

    return criteriaBuilder.and(
        criteriaBuilder.notEqual(ticketRoot.get(STATUS), Status.CLOSED),
        relevantTicketPredicate,
        criteriaBuilder.exists(unreadMessagesSubquery));
  }
}
