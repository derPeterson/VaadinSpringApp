package de.derpeterson.app.repository;

import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

class PasswordResetTokenLookupImpl implements PasswordResetTokenLookup {
  @PersistenceContext private EntityManager entityManager;

  @Override
  @Transactional
  public Optional<PasswordResetTokenEntity> findByToken(String passwordResetToken) {
    List<PasswordResetTokenEntity> matchingTokenEntities =
        entityManager
            .createQuery(
                "select t from PasswordResetTokenEntity t where t.token = :token",
                PasswordResetTokenEntity.class)
            .setParameter("token", passwordResetToken)
            .getResultList();
    if (matchingTokenEntities.isEmpty()) {
      return Optional.empty();
    }
    PasswordResetTokenEntity resetTokenEntity = matchingTokenEntities.getFirst();
    // The caller may have loaded ACTIVE before another transaction consumed it.
    // Flush our own pending changes before refreshing, never undo them.
    entityManager.flush();
    entityManager.refresh(resetTokenEntity);
    return Optional.of(resetTokenEntity);
  }

  @Override
  @Transactional
  public Optional<PasswordResetTokenEntity> findByTokenAndStatus(
      String passwordResetToken, TokenStatus tokenStatus) {
    return findByToken(passwordResetToken)
        .filter(resetTokenEntity -> resetTokenEntity.getStatus() == tokenStatus);
  }
}
