package de.derpeterson.app.repository;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByLastActivityBeforeAndStatusNot(LocalDateTime lastActivity, UserStatus status);

}
