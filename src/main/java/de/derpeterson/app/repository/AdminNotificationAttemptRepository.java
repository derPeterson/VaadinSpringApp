package de.derpeterson.app.repository;

import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AdminNotificationAttemptRepository extends JpaRepository<AdminNotificationAttemptEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminNotificationAttemptEntity a WHERE a.id = :id")
    java.util.Optional<AdminNotificationAttemptEntity> lockById(Long id);
}
