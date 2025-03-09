package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueueEntity, Long> {

    @Query("SELECT e FROM EmailQueueEntity e WHERE e.status = 'PENDING'")
    List<EmailQueueEntity> findPendingEmails(Pageable pageable);

    List<EmailQueueEntity> findByStatus(EmailStatus status);

    Optional<EmailQueueEntity> findByUserEntityAndEmailType(UserEntity userEntity, EmailType emailType);

    int deleteByStatus(EmailStatus status);
}
