package de.derpeterson.app.events;

import de.derpeterson.app.model.enums.RoleType;
import java.util.Set;

/** Synchrones Fachereignis innerhalb der Änderungstransaktion. */
public record UserRolesChanged(Long userId, Set<RoleType> before, Set<RoleType> after) {
  public UserRolesChanged {
    before = Set.copyOf(before);
    after = Set.copyOf(after);
  }
}
