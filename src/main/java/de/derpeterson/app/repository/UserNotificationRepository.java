package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserNotificationEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, Long> {
  Optional<UserNotificationEntity> findByRequestKey(String requestKey);

  @Query(
      "select m from UserNotificationEntity m where m.sender.id = :sender and (:cutoff is null or m.createdAt >= :cutoff) order by m.id desc")
  Page<UserNotificationEntity> sent(
      @Param("sender") Long sender, @Param("cutoff") Instant cutoff, Pageable page);

  @Modifying
  @Query(
      "delete from UserNotificationEntity m where "
          + "(m.type <> de.derpeterson.app.model.enums.UserNotificationType.ADMIN_ANNOUNCEMENT and ((:inboxCutoff is not null and m.createdAt < :inboxCutoff) or not exists (select r.id from UserNotificationRecipientEntity r where r.notification = m))) "
          + "or (m.type = de.derpeterson.app.model.enums.UserNotificationType.ADMIN_ANNOUNCEMENT and :historyCutoff is not null and m.createdAt < :historyCutoff and ((:inboxCutoff is not null and m.createdAt < :inboxCutoff) or not exists (select r.id from UserNotificationRecipientEntity r where r.notification = m)))")
  int deleteRetired(
      @Param("inboxCutoff") Instant inboxCutoff, @Param("historyCutoff") Instant historyCutoff);
}
