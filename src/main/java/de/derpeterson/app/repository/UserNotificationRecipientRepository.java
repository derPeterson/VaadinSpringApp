package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserNotificationRecipientEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRecipientRepository
    extends JpaRepository<UserNotificationRecipientEntity, Long> {
  interface InboxRow {
    Long getId();

    de.derpeterson.app.model.enums.UserNotificationType getType();

    String getTitle();

    String getBody();

    Instant getCreatedAt();

    Instant getReadAt();
  }

  // Skalare Projektionen sehen Bulk-Leseupdates auch bei bereits verwalteten Empfangseinträgen.
  @Query(
      "select r.id as id, m.type as type, m.title as title, m.body as body, m.createdAt as createdAt, r.readAt as readAt from UserNotificationRecipientEntity r join r.notification m where r.user.id = :owner and r.dismissedAt is null and (:cutoff is null or m.createdAt >= :cutoff) and (:unread = false or r.readAt is null) order by r.id desc")
  Page<InboxRow> inbox(
      @Param("owner") Long owner,
      @Param("unread") boolean unread,
      @Param("cutoff") Instant cutoff,
      Pageable page);

  @Query(
      "select count(r) from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and r.readAt is null and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  long countUnread(@Param("owner") Long owner, @Param("cutoff") Instant cutoff);

  @Query(
      "select r.id from UserNotificationRecipientEntity r where r.user.id = :owner and r.dismissedAt is null and r.readAt is null and (:cutoff is null or r.notification.createdAt >= :cutoff)")
  List<Long> unreadIds(@Param("owner") Long owner, @Param("cutoff") Instant cutoff);

  @Modifying
  @Query(
      "update UserNotificationRecipientEntity r set r.readAt = :now where r.user.id = :owner and r.id in :ids and r.dismissedAt is null and r.readAt is null")
  int markRead(
      @Param("owner") Long owner, @Param("ids") Collection<Long> ids, @Param("now") Instant now);

  @Modifying
  @Query(
      "update UserNotificationRecipientEntity r set r.dismissedAt = :now where r.user.id = :owner and r.id = :id and r.dismissedAt is null")
  int dismiss(@Param("owner") Long owner, @Param("id") Long id, @Param("now") Instant now);

  @Query(
      "select r.user.id from UserNotificationRecipientEntity r where r.notification.id = :notification order by r.user.id")
  List<Long> recipientIds(@Param("notification") Long notification);
}
