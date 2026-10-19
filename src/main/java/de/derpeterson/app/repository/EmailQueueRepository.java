package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueueEntity, Long> {

  @Query(
      "SELECT e FROM EmailQueueEntity e WHERE e.status = 'PENDING' "
          + "AND (e.lastRetryAt IS NULL OR e.lastRetryAt <= :retryCutoff) ORDER BY e.createdAt, e.id")
  List<EmailQueueEntity> findPendingEmails(LocalDateTime retryCutoff, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM EmailQueueEntity e WHERE e.id = :id")
  Optional<EmailQueueEntity> lockById(Long id);

  boolean existsByUserEntityAndEmailTypeAndStatusIn(
      UserEntity userEntity, EmailType emailType, Collection<EmailStatus> statuses);

  int deleteByStatusAndCreatedAtBefore(EmailStatus status, LocalDateTime date);

  int deleteByStatusAndLastRetryAtBefore(EmailStatus status, LocalDateTime date);
}
