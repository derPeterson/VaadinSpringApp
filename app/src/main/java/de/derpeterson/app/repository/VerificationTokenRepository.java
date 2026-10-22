package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

  @Query("select t.userEntity.id from VerificationTokenEntity t where t.token = :token")
  Optional<Long> findUserIdByToken(@Param("token") String token);

  Optional<VerificationTokenEntity> findByToken(String token);

  Optional<VerificationTokenEntity> findByTokenAndStatus(String token, TokenStatus status);

  List<VerificationTokenEntity> findAllByUserEntityAndStatus(UserEntity user, TokenStatus status);

  int deleteByExpiryDateBefore(LocalDateTime now);
}
