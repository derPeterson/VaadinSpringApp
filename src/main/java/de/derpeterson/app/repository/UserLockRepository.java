package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import java.util.Optional;

public interface UserLockRepository {
    /** Locks the stored account and refreshes it, never merges a caller's snapshot. */
    Optional<UserEntity> lockVerificationUser(Long id);
}
