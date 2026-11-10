package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserNotificationRecipientEntity;
import de.derpeterson.app.model.enums.UserNotificationRelevance;
import de.derpeterson.app.model.enums.UserNotificationType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRecipientRepository
    extends JpaRepository<UserNotificationRecipientEntity, Long> {
  @Query(
      """
      select distinct r.user.id from UserNotificationRecipientEntity r
      where r.notification.supportTicketNumber = :ticket and r.notification.type in :types
        and r.relevance <> :relevance
      """)
  List<Long> ticketRecipients(
      String ticket, Collection<UserNotificationType> types, UserNotificationRelevance relevance);

  @Modifying
  @Query(
      """
      update UserNotificationRecipientEntity r set r.relevance = :relevance
      where r.notification.supportTicketNumber = :ticket and r.notification.type in :types
        and r.relevance <> :relevance
      """)
  int retireTicket(
      String ticket, Collection<UserNotificationType> types, UserNotificationRelevance relevance);

  @Modifying
  @Query(
      """
      update UserNotificationRecipientEntity r set r.readAt = :now
      where r.user.id = :owner and r.notification.supportTicketNumber = :ticket
        and r.readAt is null and r.dismissedAt is null
        and r.relevance = de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE
        and ((r.notification.supportReplyId is not null and r.notification.supportReplyId <= :reply)
          or (r.notification.type = de.derpeterson.app.model.enums.UserNotificationType.SUPPORT_REQUESTER_CLOSED
            and r.notification.supportTicketVersion <= :version))
      """)
  int markTicketRead(Long owner, String ticket, Long reply, Long version, Instant now);

  interface InboxRow {
    Long getId();

    UserNotificationType getType();

    String getTitle();

    String getBody();

    String getSupportTicketNumber();

    Instant getCreatedAt();

    Instant getReadAt();

    UserNotificationRelevance getRelevance();
  }

  // Skalare Projektionen sehen Bulk-Leseupdates auch bei bereits verwalteten Empfangseinträgen.
  @Query(
      "select r.id as id, m.type as type, m.title as title, m.body as body, m.supportTicketNumber as supportTicketNumber, m.createdAt as createdAt, r.readAt as readAt, r.relevance as relevance from UserNotificationRecipientEntity r join r.notification m where r.user.id = :owner and r.dismissedAt is null and (:cutoff is null or m.createdAt >= :cutoff) and (:read is null or (:read = true and r.readAt is not null) or (:read = false and r.readAt is null and r.relevance = de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE)) order by r.id desc")
  Page<InboxRow> inbox(
      @Param("owner") Long ownerUserId,
      @Param("read") Boolean read,
      @Param("cutoff") Instant inboxCutoffTime,
      Pageable page);

  @Query(
      """
      select r.id as id, m.type as type, m.title as title, m.body as body,
        m.supportTicketNumber as supportTicketNumber, m.createdAt as createdAt,
        r.readAt as readAt, r.relevance as relevance
      from UserNotificationRecipientEntity r join r.notification m
      where r.user.id = :owner and r.id = :id and r.dismissedAt is null
        and (:cutoff is null or m.createdAt >= :cutoff)
      """)
  Optional<InboxRow> visibleItem(Long owner, Long id, Instant cutoff);

  @Query(
      "select r.id as id, m.type as type, m.title as title, m.body as body, m.supportTicketNumber as supportTicketNumber, m.createdAt as createdAt, r.readAt as readAt, r.relevance as relevance from UserNotificationRecipientEntity r join r.notification m where r.user.id = :owner and r.dismissedAt is null and (:cutoff is null or m.createdAt >= :cutoff) and (:read is null or (:read = true and r.readAt is not null) or (:read = false and r.readAt is null and r.relevance = de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE)) and (lower(coalesce(m.title, '')) like :pattern escape '!' or lower(coalesce(m.body, '')) like :pattern escape '!' or lower(coalesce(m.supportTicketNumber, '')) like :pattern escape '!' or m.type in :matchingTypes) order by r.id desc")
  Page<InboxRow> searchInbox(
      @Param("owner") Long ownerUserId,
      @Param("read") Boolean read,
      @Param("cutoff") Instant inboxCutoffTime,
      @Param("pattern") String pattern,
      @Param("matchingTypes") Collection<UserNotificationType> matchingTypes,
      Pageable page);

  @Query(
      "select count(r) from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  long countVisible(@Param("owner") Long ownerUserId, @Param("cutoff") Instant inboxCutoffTime);

  @Query(
      "select count(r) from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and r.readAt is null and r.relevance = de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  long countUnread(@Param("owner") Long ownerUserId, @Param("cutoff") Instant inboxCutoffTime);

  @Query(
      "select count(r) from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and r.readAt is not null and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  long countRead(@Param("owner") Long ownerUserId, @Param("cutoff") Instant inboxCutoffTime);

  @Query(
      "select r.id from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and r.readAt is null and r.relevance = de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  List<Long> unreadIds(@Param("owner") Long ownerUserId, @Param("cutoff") Instant inboxCutoffTime);

  @Modifying
  @Query(
      "update UserNotificationRecipientEntity r set r.readAt = :now where r.user.id = :owner and r.id in :ids and r.dismissedAt is null and r.readAt is null")
  int markRead(
      @Param("owner") Long ownerUserId,
      @Param("ids") Collection<Long> notificationIds,
      @Param("now") Instant currentTime);

  @Modifying
  @Query(
      "update UserNotificationRecipientEntity r set r.dismissedAt = :now where r.user.id = :owner and r.id = :id and r.dismissedAt is null")
  int dismiss(
      @Param("owner") Long ownerUserId,
      @Param("id") Long notificationId,
      @Param("now") Instant currentTime);

  @Modifying
  @Query(
      "update UserNotificationRecipientEntity r set r.dismissedAt = :now where r.user.id = :owner and r.dismissedAt is null and r.readAt is not null and (:cutoff is null or r.notification.id in (select n.id from UserNotificationEntity n where n.createdAt >= :cutoff))")
  int dismissRead(
      @Param("owner") Long ownerUserId,
      @Param("cutoff") Instant inboxCutoffTime,
      @Param("now") Instant currentTime);

  @Query(
      "select r.user.id from UserNotificationRecipientEntity r where r.notification.id = :notification order by r.user.id")
  List<Long> recipientIds(@Param("notification") Long notification);
}
