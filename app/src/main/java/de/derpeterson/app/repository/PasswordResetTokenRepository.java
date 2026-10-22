package de.derpeterson.app.repository;

import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetTokenEntity, Long>, PasswordResetTokenLookup {

  Optional<PasswordResetTokenEntity> findByToken(String token);

  // Resolve the account without loading a token/user before acquiring its lock.
  @Query("select t.userEntity.id from PasswordResetTokenEntity t where t.token = :token")
  Optional<Long> findUserIdByToken(@Param("token") String token);

  Optional<PasswordResetTokenEntity> findByTokenAndStatus(String token, TokenStatus status);

  List<PasswordResetTokenEntity> findAllByUserEntityAndStatus(UserEntity user, TokenStatus status);

  int deleteByExpiryDateBefore(LocalDateTime now);
}
