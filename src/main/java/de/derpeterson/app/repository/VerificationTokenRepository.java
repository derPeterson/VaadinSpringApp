package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

    Optional<VerificationTokenEntity> findByToken(String token);

    Optional<VerificationTokenEntity> findByTokenAndStatus(String token, TokenStatus status);

    List<VerificationTokenEntity> findAllByUserEntityAndStatus(UserEntity user, TokenStatus status);

    int deleteByToken(String token);

    int deleteByExpiryDateBefore(LocalDateTime now);
}
