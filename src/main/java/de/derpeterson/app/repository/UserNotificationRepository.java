package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserNotificationEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, Long> {
  Optional<UserNotificationEntity> findByRequestKey(String requestKey);

  Page<UserNotificationEntity> findBySenderIdOrderByIdDesc(Long sender, Pageable page);

  @Modifying
  @Query(
      "delete from UserNotificationEntity m where m.createdAt < :cutoff or not exists (select r.id from UserNotificationRecipientEntity r where r.notification = m)")
  int deleteRetired(@Param("cutoff") Instant cutoff);
}
