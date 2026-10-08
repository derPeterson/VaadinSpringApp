package de.derpeterson.app.repository;

import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import java.util.Optional;

/** Fresh reads after the account lock, including with an existing persistence context. */
public interface PasswordResetTokenLookup {
    Optional<PasswordResetTokenEntity> findByToken(String token);

    Optional<PasswordResetTokenEntity> findByTokenAndStatus(String token, TokenStatus status);
}
