package de.derpeterson.app.repository;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(RoleType name);
}
