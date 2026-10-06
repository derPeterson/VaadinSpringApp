package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

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
