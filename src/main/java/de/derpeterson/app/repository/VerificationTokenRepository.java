package de.derpeterson.app.repository;

import de.derpeterson.app.model.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

    Optional<VerificationTokenEntity> findByToken(String token);

    int deleteByToken(String token);

    int deleteByExpiryDateBefore(LocalDateTime now);
}
