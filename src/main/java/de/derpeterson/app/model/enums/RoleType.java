package de.derpeterson.app.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public enum RoleType {
    ROLE_SUPERADMIN("SUPERADMIN", "ROLE_SUPERADMIN"),
    ROLE_ADMIN("ADMIN", "ROLE_ADMIN"),
    ROLE_USER("USER", "ROLE_USER"),
    ROLE_GUEST("GUEST", "ROLE_GUEST");

    private final String name;
    private final String longName;

    public boolean isAdministrative() {
        return this == ROLE_ADMIN || this == ROLE_SUPERADMIN;
    }

    /** Effective permissions, separate from explicitly assigned roles used by forms and filters. */
    public List<RoleType> getGrantedRoles() {
        return this == ROLE_SUPERADMIN ? List.of(ROLE_SUPERADMIN, ROLE_ADMIN) : List.of(this);
    }
}
