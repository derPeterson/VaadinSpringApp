package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailChangeTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.TokenStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeTokenEntity, Long> {
  Optional<EmailChangeTokenEntity> findByDigest(String digest);

  @Query("select t.userEntity.id from EmailChangeTokenEntity t where t.digest = :digest")
  Optional<Long> findUserIdByDigest(String digest);

  List<EmailChangeTokenEntity> findAllByUserEntityAndStatus(UserEntity user, TokenStatus status);

  int deleteByExpiryDateBefore(LocalDateTime cutoff);
}
