package de.derpeterson.app.repository;

import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    Optional<PasswordResetTokenEntity> findByTokenAndStatus(String token, TokenStatus status);

    int deleteByToken(String token);

    int deleteByExpiryDateBefore(LocalDateTime now);
}
