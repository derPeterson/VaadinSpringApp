package de.derpeterson.app.repository;

import de.derpeterson.app.model.ExternalIdentityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, Long> {
  Optional<ExternalIdentityEntity> findByProviderAndIssuerAndSubject(
      String provider, String issuer, String subject);

  Optional<ExternalIdentityEntity> findByUserIdAndProvider(Long userId, String provider);

  List<ExternalIdentityEntity> findByUserIdOrderByProvider(Long userId);

  long countByUserId(Long userId);
}
