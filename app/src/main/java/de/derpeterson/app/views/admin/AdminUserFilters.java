package de.derpeterson.app.views.admin;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/** UI filter values; enum identity stays stable across translation changes. */
record AdminUserFilters(
    String search,
    Boolean enabled,
    Set<UserStatus> statuses,
    Set<RoleType> roles,
    Instant activeSince)
    implements Serializable {
  AdminUserFilters {
    statuses = Set.copyOf(statuses);
    roles = Set.copyOf(roles);
  }

  enum Enabled {
    ALL,
    YES,
    NO;

    Boolean value() {
      return this == ALL ? null : this == YES;
    }
  }

  boolean matches(UserEntity user) {
    String query = normalize(search);
    String fullName = normalize(user.getFirstName()) + " " + normalize(user.getLastName());
    return (query.isEmpty()
            || fullName.contains(query)
            || normalize(user.getEmail()).contains(query))
        && (enabled == null || user.isEnabled() == enabled)
        && (statuses.isEmpty() || statuses.contains(user.getStatus()))
        && (roles.isEmpty() || roles.stream().anyMatch(user::hasRole))
        && (activeSince == null
            || user.getLastActivity() != null && !user.getLastActivity().isBefore(activeSince));
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
