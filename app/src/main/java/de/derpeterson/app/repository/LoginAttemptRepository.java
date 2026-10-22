package de.derpeterson.app.repository;

import de.derpeterson.app.model.LoginAttemptEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from LoginAttemptEntity a where a.identityHash = :key")
  Optional<LoginAttemptEntity> lockIdentity(String key);

  @Query(
      "select a.identityHash from LoginAttemptEntity a where a.lastActivity < :cutoff "
          + "and (a.blockedUntil is null or a.blockedUntil <= :now) order by a.lastActivity, a.identityHash")
  List<String> staleIdentities(Instant cutoff, Instant now, Pageable page);
}
