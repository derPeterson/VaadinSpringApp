package de.derpeterson.app.repository;

import de.derpeterson.app.model.ConfigEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

  boolean existsByKey(String configKey);

  Optional<ConfigEntity> findByKey(String configKey);

  List<ConfigEntity> findByKeyIn(Collection<String> configKeys);
}
