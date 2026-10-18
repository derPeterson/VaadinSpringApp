package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserMessageRecipientEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserMessageRecipientRepository
    extends JpaRepository<UserMessageRecipientEntity, Long> {
  interface InboxRow {
    Long getId();

    de.derpeterson.app.model.enums.UserMessageType getType();

    String getTitle();

    String getBody();

    Instant getCreatedAt();

    Instant getReadAt();
  }

  // Skalare Projektionen sehen Bulk-Leseupdates auch bei bereits verwalteten Empfangseinträgen.
  @Query(
      "select r.id as id, m.type as type, m.title as title, m.body as body, m.createdAt as createdAt, r.readAt as readAt from UserMessageRecipientEntity r join r.message m where r.user.id = :owner and (:unread = false or r.readAt is null) order by r.id desc")
  Page<InboxRow> inbox(@Param("owner") Long owner, @Param("unread") boolean unread, Pageable page);

  long countByUserIdAndReadAtIsNull(Long owner);

  @Query(
      "select r.id from UserMessageRecipientEntity r where r.user.id = :owner and r.readAt is null")
  List<Long> unreadIds(@Param("owner") Long owner);

  @Modifying
  @Query(
      "update UserMessageRecipientEntity r set r.readAt = :now where r.user.id = :owner and r.id in :ids and r.readAt is null")
  int markRead(
      @Param("owner") Long owner, @Param("ids") Collection<Long> ids, @Param("now") Instant now);

  @Query(
      "select r.user.id from UserMessageRecipientEntity r where r.message.id = :message order by r.user.id")
  List<Long> recipientIds(@Param("message") Long message);
}
