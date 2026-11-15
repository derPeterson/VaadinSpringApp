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
  @Query(
      """
      delete from EmailQueueCleanupRunEntity r
      where r.finishedAt < :cutoff and r.status in ('COMPLETED', 'FAILED', 'PARTIAL')
        and r.id not in (select s.currentRunId from EmailQueueCleanupStateEntity s
          where s.currentRunId is not null)
      """)
  int deleteFinishedBefore(@Param("cutoff") LocalDateTime cutoffDateTime);
}
