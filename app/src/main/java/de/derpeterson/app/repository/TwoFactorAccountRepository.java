package de.derpeterson.app.repository;

import de.derpeterson.app.model.TwoFactorAccountEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TwoFactorAccountRepository extends JpaRepository<TwoFactorAccountEntity, Long> {
  interface State {
    UUID getRevision();

    boolean getEnrolled();

    boolean getPreviouslyEnrolled();

    boolean getOperatorEnrollment();

    Instant getEnrolledAt();
  }

  // Skalare Projektion statt eines möglicherweise veralteten OSIV-Entities für Zugriffskontrollen.
  @Query(
      "select a.revision as revision, (a.encryptedSecret is not null) as enrolled, a.previouslyEnrolled as previouslyEnrolled, a.operatorEnrollment as operatorEnrollment, a.enrolledAt as enrolledAt from TwoFactorAccountEntity a where a.userId = :userId")
  Optional<State> state(Long userId);

  @Query("select a.blockedUntil from TwoFactorAccountEntity a where a.userId = :userId")
  Optional<Instant> blockedUntil(Long userId);
}
