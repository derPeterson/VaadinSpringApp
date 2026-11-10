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

  int activeGroupCount() {
    return countActiveGroups(search, enabled, statuses, roles, activeSince != null);
  }

  static int countActiveGroups(
      String search,
      Boolean enabled,
      Set<UserStatus> statuses,
      Set<RoleType> roles,
      boolean hasActivity) {
    return (normalizeSearchText(search).isEmpty() ? 0 : 1)
        + (enabled == null ? 0 : 1)
        + (statuses.isEmpty() ? 0 : 1)
        + (roles.isEmpty() ? 0 : 1)
        + (hasActivity ? 1 : 0);
  }

  boolean matches(UserEntity userEntity) {
    String normalizedSearchText = normalizeSearchText(search);
    String normalizedFullName =
        normalizeSearchText(userEntity.getFirstName())
            + " "
            + normalizeSearchText(userEntity.getLastName());
    return (normalizedSearchText.isEmpty()
            || normalizedFullName.contains(normalizedSearchText)
            || normalizeSearchText(userEntity.getEmail()).contains(normalizedSearchText))
        && (enabled == null || userEntity.isEnabled() == enabled)
        && (statuses.isEmpty() || statuses.contains(userEntity.getStatus()))
        && (roles.isEmpty() || roles.stream().anyMatch(userEntity::hasRole))
        && (activeSince == null
            || userEntity.getLastActivity() != null
                && !userEntity.getLastActivity().isBefore(activeSince));
  }

  private static String normalizeSearchText(String searchValue) {
    return searchValue == null ? "" : searchValue.trim().toLowerCase(Locale.ROOT);
  }
}
