package de.derpeterson.app.repository;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, UserLockRepository {

    default Optional<UserEntity> findByEmail(String email) {
        return findByCanonicalEmail(EmailIdentity.canonicalize(email));
    }

    @Query("select u from UserEntity u where u.email = :email")
    Optional<UserEntity> findByCanonicalEmail(@Param("email") String email);

    default boolean emailExistsForOtherUser(String email, Long id) {
        return canonicalEmailExistsForOtherUser(EmailIdentity.canonicalize(email), id);
    }

    @Query("select count(u) > 0 from UserEntity u where u.email = :email and (:id is null or u.id <> :id)")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    boolean canonicalEmailExistsForOtherUser(@Param("email") String email, @Param("id") Long id);

    // A single existing, unique role row serializes privilege mutations across
    // application instances. Do not flush already edited managed users first.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoleEntity r where r.name = :role")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    Optional<RoleEntity> lockRole(@Param("role") RoleType role);

    interface SecurityUserRow {
        String getEmail();
        String getPassword();
        boolean isEnabled();
        RoleType getRole();
    }

    // Scalar projections bypass stale managed entities/collections (including
    // OSIV). An independent read sees committed privileges, not pending edits.
    default List<SecurityUserRow> findSecurityUser(String email) {
        return findCanonicalSecurityUser(EmailIdentity.canonicalize(email));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Query("select u.email as email, u.password as password, u.enabled as enabled, r.name as role "
            + "from UserEntity u left join u.roleEntities r where u.email = :email")
    List<SecurityUserRow> findCanonicalSecurityUser(@Param("email") String email);

    default Optional<UserEntity> findByEmailForUpdate(String email) {
        return findByCanonicalEmailForUpdate(EmailIdentity.canonicalize(email));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.email = :email")
    Optional<UserEntity> findByCanonicalEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    // Bulk update deliberately bypasses entity version checks/increments. Flush any
    // pending login status first; do not clear unrelated managed entities.
    @Modifying(flushAutomatically = true)
    @Query("update UserEntity u set u.lastActivity = :activity where u.id = :id")
    int updateLastActivity(@Param("id") Long id, @Param("activity") LocalDateTime activity);

    @Override
    @EntityGraph(attributePaths = "roleEntities")
    List<UserEntity> findAll();

    // Read stored IDs, not possibly already edited entities from the persistence context.
    // Do not auto-flush a pending demotion before the service has checked it.
    @Query("select distinct u.id from UserEntity u join u.roleEntities r where u.enabled = true and r.name = :role")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    List<Long> findEnabledUserIdsByRole(@Param("role") RoleType role);
    
    List<UserEntity> findByLastActivityBeforeAndStatus(LocalDateTime lastActivity, UserStatus status);

    List<UserEntity> findByLastActivityAfterAndStatusAndStatusManuallySetFalse(LocalDateTime lastActivity, UserStatus status);

}
