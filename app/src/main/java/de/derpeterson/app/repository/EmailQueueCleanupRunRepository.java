package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailQueueCleanupRunEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailQueueCleanupRunRepository
    extends JpaRepository<EmailQueueCleanupRunEntity, Long> {
  @Modifying
  @Query("delete from EmailQueueCleanupRunEntity r where r.attemptedAt < :cutoff")
  int deleteAttemptsBefore(@Param("cutoff") LocalDateTime cutoffDateTime);
}
