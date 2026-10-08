package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

class UserLockRepositoryImpl implements UserLockRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserEntity> lockVerificationUser(Long id) {
        // Scalar projection avoids trusting a previously loaded, stale entity.
        var ids = entityManager.createQuery("select u.id from UserEntity u where u.id = :id", Long.class)
                .setParameter("id", id).setFlushMode(FlushModeType.COMMIT)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        UserEntity user = entityManager.find(UserEntity.class, id);
        // Acquire the database lock before flushing. Preserve this transaction's
        // own pending changes before refresh; flush is not a commit, and stale
        // dirty entities must still pass their optimistic version checks.
        entityManager.flush();
        entityManager.refresh(user);
        return Optional.of(user);
    }
}
