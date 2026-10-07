package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Looks up an existing role without creating or modifying roles. Delegates
     * directly to the repository and propagates its failures unchanged.
     *
     * @param name role name to look up; may be {@code null}
     * @return the repository result, empty if the role is missing or the name is
     *         {@code null} (persisted role names cannot be null)
     */
    public Optional<RoleEntity> findByName(RoleType name) {
        return roleRepository.findByName(name);
    }
}
