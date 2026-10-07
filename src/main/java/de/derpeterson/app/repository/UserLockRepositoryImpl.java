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
        entityManager.refresh(user);
        return Optional.of(user);
    }
}
