package de.derpeterson.app.repository;

import de.derpeterson.app.model.LoginAttemptGuardEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LoginAttemptGuardRepository
    extends JpaRepository<LoginAttemptGuardEntity, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from LoginAttemptGuardEntity g where g.id = 1")
  Optional<LoginAttemptGuardEntity> lockCreation();
}
