package de.derpeterson.app.repository;

import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

class PasswordResetTokenLookupImpl implements PasswordResetTokenLookup {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Optional<PasswordResetTokenEntity> findByToken(String token) {
        var values = entityManager.createQuery("select t from PasswordResetTokenEntity t where t.token = :token",
                PasswordResetTokenEntity.class).setParameter("token", token).getResultList();
        if (values.isEmpty()) {
            return Optional.empty();
        }
        var value = values.getFirst();
        // The caller may have loaded ACTIVE before another transaction consumed it.
        // Flush our own pending changes before refreshing, never undo them.
        entityManager.flush();
        entityManager.refresh(value);
        return Optional.of(value);
    }

    @Override
    @Transactional
    public Optional<PasswordResetTokenEntity> findByTokenAndStatus(String token, TokenStatus status) {
        return findByToken(token).filter(value -> value.getStatus() == status);
    }
}
