package de.derpeterson.app.repository;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.enums.RoleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

  Optional<RoleEntity> findByName(RoleType name);
}
