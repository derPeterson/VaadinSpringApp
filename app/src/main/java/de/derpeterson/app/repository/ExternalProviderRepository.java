package de.derpeterson.app.repository;

import de.derpeterson.app.model.ExternalProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalProviderRepository extends JpaRepository<ExternalProviderEntity, String> {}
