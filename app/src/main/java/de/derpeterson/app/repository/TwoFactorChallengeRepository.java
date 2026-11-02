package de.derpeterson.app.repository;

import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TwoFactorChallengeRepository
    extends JpaRepository<TwoFactorChallengeEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from TwoFactorChallengeEntity c where c.id = :id")
  Optional<TwoFactorChallengeEntity> lock(UUID id);

  @Query(
      "select c.id from TwoFactorChallengeEntity c where c.user.id = :userId and c.sessionDigest = :session and c.kind = :kind and c.action = :action and c.consumed = false and c.expiresAt > :now order by c.createdAt desc")
  List<UUID> active(Long userId, String session, Kind kind, TwoFactorAction action, Instant now);

  @Query(
      "select (count(c) > 0) from TwoFactorChallengeEntity c where c.user.id = :userId and c.sessionDigest = :session and c.kind = :kind and c.setupProof = :proof and c.consumed = false and c.expiresAt > :now")
  boolean setupInProgress(Long userId, String session, Kind kind, SetupProof proof, Instant now);

  @Modifying
  @Query(
      "update TwoFactorChallengeEntity c set c.consumed = true where c.user.id = :userId and c.sessionDigest = :session and c.kind = :kind and c.action = :action and c.consumed = false")
  void invalidate(Long userId, String session, Kind kind, TwoFactorAction action);

  @Modifying
  @Query("delete from TwoFactorChallengeEntity c where c.expiresAt < :before")
  int deleteExpired(Instant before);

  @Query(
      "select count(c) from TwoFactorChallengeEntity c where c.user.id = :userId and c.contactAddress is not null and c.createdAt > :after")
  long emailRequests(Long userId, Instant after);

  @Modifying
  @Query(
      "update TwoFactorChallengeEntity c set c.consumed = true, c.encryptedPayload = null, c.verificationDigest = null where c.user.id = :userId")
  void invalidateOwner(Long userId);

  @Modifying
  @Query(
      "update TwoFactorChallengeEntity c set c.encryptedPayload = null, c.verificationDigest = null, c.consumed = true where c.expiresAt <= :now and (c.encryptedPayload is not null or c.verificationDigest is not null)")
  int clearExpiredSecrets(Instant now);
}
