package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailQueueCleanupStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EmailQueueCleanupStateRepository
    extends JpaRepository<EmailQueueCleanupStateEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from EmailQueueCleanupStateEntity s where s.id = 1")
  EmailQueueCleanupStateEntity lockState();
}
