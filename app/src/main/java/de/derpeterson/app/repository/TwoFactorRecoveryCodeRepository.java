package de.derpeterson.app.repository;

import de.derpeterson.app.model.TwoFactorRecoveryCodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TwoFactorRecoveryCodeRepository
    extends JpaRepository<TwoFactorRecoveryCodeEntity, Long> {
  @Query(
      "select c from TwoFactorRecoveryCodeEntity c where c.user.id = :userId and c.digest = :digest and c.usedAt is null")
  Optional<TwoFactorRecoveryCodeEntity> unused(Long userId, String digest);

  @Query(
      "select count(c) from TwoFactorRecoveryCodeEntity c where c.user.id = :userId and c.usedAt is null")
  long remaining(Long userId);

  @Modifying
  @Query("delete from TwoFactorRecoveryCodeEntity c where c.user.id = :userId")
  void deleteForUser(Long userId);
}
