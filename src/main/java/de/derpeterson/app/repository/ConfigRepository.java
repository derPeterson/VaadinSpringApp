package de.derpeterson.app.repository;

import de.derpeterson.app.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config, Long> {

    boolean existsByKey(String key);

    Optional<Config> findByKey(String key);
}
