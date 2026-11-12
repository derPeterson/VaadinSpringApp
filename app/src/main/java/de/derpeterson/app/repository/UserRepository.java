package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, UserLockRepository {

  @Query(
      """
      select distinct u
      from UserEntity u join u.roleEntities r
      where u.enabled = true
      and u.administrativelyBlocked = false
      and u.accountDeactivation.deletionAt is null
      and u.verificationPending = false
      and u.emailVerificationStatus <> de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING
      and r.name in (de.derpeterson.app.model.enums.RoleType.ROLE_ADMIN, de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN)
      """)
  List<UserEntity> findEnabledAdministrators();

  default Optional<UserEntity> findByEmail(String email) {
    return findByCanonicalEmail(EmailIdentity.canonicalize(email));
  }

  @Query("select u from UserEntity u where u.email = :email")
  Optional<UserEntity> findByCanonicalEmail(@Param("email") String canonicalEmailAddress);

  default boolean emailExistsForOtherUser(String email, Long id) {
    return canonicalEmailExistsForOtherUser(EmailIdentity.canonicalize(email), id);
  }

  @Query(
      "select count(u) > 0 from UserEntity u where u.email = :email and (:id is null or u.id <> :id)")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
  boolean canonicalEmailExistsForOtherUser(
      @Param("email") String canonicalEmailAddress, @Param("id") Long userId);

  // A single existing, unique role row serializes privilege mutations across
  // application instances. Do not flush already edited managed users first.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RoleEntity r where r.name = :role")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
  Optional<RoleEntity> lockRole(@Param("role") RoleType roleType);

  @Query("select r.name from UserEntity u join u.roleEntities r where u.id = :id")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
  List<RoleType> findExplicitRoles(@Param("id") Long userId);

  interface NotificationTargetRow {
    Long getId();

    String getFirstName();

    String getLastName();

    String getEmail();
  }

  // Keine veralteten OSIV-Entities für die aktuelle Versandberechtigung der Empfänger auswerten.
  @Query(
      """
      select u.id as id, u.firstName as firstName, u.lastName as lastName, u.email as email
      from UserEntity u
      where u.enabled = true
      and u.administrativelyBlocked = false
      and u.accountDeactivation.deletionAt is null
      and u.verificationPending = false
      and u.emailVerificationStatus <> de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING
      order by u.id
      """)
  List<NotificationTargetRow> findNotificationTargets();

  interface SecurityUserRow {
    Long getId();

    boolean getPendingEmailVerification();

    boolean getSelfDeactivated();

    String getEmail();

    String getPassword();

    String getAccessVersion();

    boolean isEnabled();

    RoleType getRole();
  }

  @Modifying
  @Query(
      "update VerificationTokenEntity t set t.status = de.derpeterson.app.model.enums.TokenStatus.INACTIVE where t.userEntity.id = :userId and t.status = de.derpeterson.app.model.enums.TokenStatus.ACTIVE")
  void invalidateVerificationTokensForAddressChange(Long userId);

  @Modifying
  @Query(
      "update PasswordResetTokenEntity t set t.status = de.derpeterson.app.model.enums.TokenStatus.INACTIVE where t.userEntity.id = :userId and t.status = de.derpeterson.app.model.enums.TokenStatus.ACTIVE")
  void invalidatePasswordResetTokensForAddressChange(Long userId);

  @Modifying
  @Query(
      "delete from EmailQueueEntity e where e.userEntity.id = :userId and e.status = de.derpeterson.app.model.enums.EmailStatus.PENDING and e.emailType in (de.derpeterson.app.model.enums.EmailType.VERIFICATION, de.derpeterson.app.model.enums.EmailType.PASSWORD_RESET)")
  void deletePendingAddressMails(Long userId);

  @Modifying
  @Query(
      "update EmailQueueEntity e set e.recipient = :previousEmailAddress where e.userEntity.id = :userId and e.recipient is null and e.status = de.derpeterson.app.model.enums.EmailStatus.IN_PROGRESS and e.emailType in (de.derpeterson.app.model.enums.EmailType.VERIFICATION, de.derpeterson.app.model.enums.EmailType.PASSWORD_RESET)")
  void preserveClaimedAddressMailRecipients(Long userId, String previousEmailAddress);

  interface TwoFactorAccessRow {
    Long getId();

    String getEmail();

    String getAccessVersion();

    boolean getEnabled();

    boolean getVerificationPending();

    boolean getHasPassword();

    RoleType getRole();
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  @Query(
      """
      select u.id as id, u.email as email, u.accessVersion as accessVersion,
        (u.enabled = true and u.administrativelyBlocked = false
          and u.accountDeactivation.deletionAt is null
          and u.emailVerificationStatus <> de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING) as enabled,
        (u.administrativelyBlocked = false and u.accountDeactivation.deletionAt is null
          and (u.verificationPending = true or (u.enabled = true
            and u.emailVerificationStatus = de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING))) as verificationPending,
        (u.password is not null) as hasPassword, r.name as role
      from UserEntity u left join u.roleEntities r
      where u.email = :email
      """)
  List<TwoFactorAccessRow> findTwoFactorAccess(String email);

  // Scalar projections bypass stale managed entities/collections (including
  // OSIV). An independent read sees committed privileges, not pending edits.
  default List<SecurityUserRow> findSecurityUser(String email) {
    return findCanonicalSecurityUser(EmailIdentity.canonicalize(email));
  }

  String SECURITY_USER_QUERY =
      """
      select u.id as id, u.email as email, u.password as password,
        u.accessVersion as accessVersion,
        (u.enabled = true and u.administrativelyBlocked = false
          and u.accountDeactivation.deletionAt is null
          and u.emailVerificationStatus <> de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING) as enabled,
        case when u.administrativelyBlocked = false
          and u.accountDeactivation.deletionAt > CURRENT_TIMESTAMP then true else false end as selfDeactivated,
        (u.administrativelyBlocked = false and u.accountDeactivation.deletionAt is null
          and (u.verificationPending = true or (u.enabled = true
            and u.emailVerificationStatus = de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING))) as pendingEmailVerification,
        r.name as role
      from UserEntity u left join u.roleEntities r where u.email = :email
      """;

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  @Query(SECURITY_USER_QUERY)
  List<SecurityUserRow> findCanonicalSecurityUser(@Param("email") String canonicalEmailAddress);

  // The password guard already owns a fresh independent transaction. Reuse its connection
  // instead of exhausting the pool with a second connection for each locked login row.
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  @Query(SECURITY_USER_QUERY)
  List<SecurityUserRow> findPasswordSecurityUser(@Param("email") String canonicalEmailAddress);

  @Query("select count(i) > 0 from ExternalIdentityEntity i where i.user.id = :id")
  boolean hasExternalSignIn(@Param("id") Long userId);

  default Optional<UserEntity> findByEmailForUpdate(String email) {
    return findByCanonicalEmailForUpdate(EmailIdentity.canonicalize(email));
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserEntity u where u.email = :email")
  Optional<UserEntity> findByCanonicalEmailForUpdate(@Param("email") String canonicalEmailAddress);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserEntity u where u.id = :id")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
  Optional<UserEntity> findByIdForUpdate(@Param("id") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT u FROM UserEntity u
      WHERE u.administrativelyBlocked = false AND (
        u.accountDeactivation.initialTokenDigest = :tokenDigest
        OR u.accountDeactivation.reminderTokenDigest = :tokenDigest
        OR u.accountDeactivation.requestedTokenDigest = :tokenDigest
      )
      """)
  Optional<UserEntity> lockByAccountReactivationToken(String tokenDigest);

  @Query(
      """
      SELECT u.id FROM UserEntity u WHERE u.accountDeactivation.deletionAt IS NOT NULL
        AND (u.accountDeactivation.deletionAt <= :now
          OR (u.administrativelyBlocked = false AND u.accountDeactivation.reminderAt <= :now))
      ORDER BY u.accountDeactivation.deletionAt, u.id
      """)
  List<Long> findAccountLifecycleDueIds(Instant now, Pageable page);

  // Bulk update deliberately bypasses entity version checks/increments. Flush any
  // pending login status first; do not clear unrelated managed entities.
  @Modifying(flushAutomatically = true)
  @Query("update UserEntity u set u.lastActivity = :activity where u.id = :id")
  int updateLastActivity(@Param("id") Long userId, @Param("activity") Instant lastActivityTime);

  @Override
  @EntityGraph(attributePaths = "roleEntities")
  List<UserEntity> findAll();

  // Read stored IDs, not possibly already edited entities from the persistence context.
  // Do not auto-flush a pending demotion before the service has checked it.
  @Query(
      """
      select distinct u.id
      from UserEntity u join u.roleEntities r
      where u.enabled = true
      and u.administrativelyBlocked = false
      and u.accountDeactivation.deletionAt is null
      and u.emailVerificationStatus <> de.derpeterson.app.model.enums.EmailVerificationStatus.PENDING
      and r.name = :role
      """)
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
  List<Long> findEnabledUserIdsByRole(@Param("role") RoleType roleType);

  List<UserEntity> findByLastActivityBeforeAndStatus(Instant lastActivity, UserStatus status);

  List<UserEntity> findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
      Instant lastActivity, UserStatus status);
}
