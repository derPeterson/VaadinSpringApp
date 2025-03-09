package de.derpeterson.app.repository;

import de.derpeterson.app.model.ConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

    boolean existsByKey(String key);

    Optional<ConfigEntity> findByKey(String key);
}
