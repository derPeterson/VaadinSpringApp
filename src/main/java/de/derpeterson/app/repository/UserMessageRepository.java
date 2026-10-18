package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserMessageEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserMessageRepository extends JpaRepository<UserMessageEntity, Long> {
  Optional<UserMessageEntity> findByRequestKey(String requestKey);

  Page<UserMessageEntity> findBySenderIdOrderByIdDesc(Long sender, Pageable page);

  @Modifying
  @Query(
      "delete from UserMessageEntity m where m.createdAt < :cutoff or not exists (select r.id from UserMessageRecipientEntity r where r.message = m)")
  int deleteRetired(@Param("cutoff") Instant cutoff);
}
