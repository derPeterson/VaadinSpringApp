package de.derpeterson.app.repository;

import de.derpeterson.app.model.TwoFactorPolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TwoFactorPolicyRepository extends JpaRepository<TwoFactorPolicyEntity, Integer> {
  interface Snapshot {
    Long getVersion();

    String getRules();
  }

  @Query(
      "select p.version as version, p.rules as rules from TwoFactorPolicyEntity p where p.id = 1")
  Optional<Snapshot> snapshot();
}
