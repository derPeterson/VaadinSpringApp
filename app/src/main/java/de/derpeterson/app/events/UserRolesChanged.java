package de.derpeterson.app.events;

import de.derpeterson.app.model.enums.RoleType;
import java.util.Set;

/** Synchrones Fachereignis innerhalb der Änderungstransaktion. */
public record UserRolesChanged(
    Long userId, Set<RoleType> rolesBeforeChange, Set<RoleType> rolesAfterChange) {
  public UserRolesChanged {
    rolesBeforeChange = Set.copyOf(rolesBeforeChange);
    rolesAfterChange = Set.copyOf(rolesAfterChange);
  }
}
