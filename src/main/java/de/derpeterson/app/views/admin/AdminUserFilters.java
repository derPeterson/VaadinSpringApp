package de.derpeterson.app.views.admin;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.Locale;

/** UI filter values; enum identity stays stable across translation changes. */
record AdminUserFilters(
    String search, Boolean enabled, UserStatus status, RoleType role, LocalDateTime activeSince) {
  boolean matches(UserEntity user) {
    String query = normalize(search);
    String fullName = normalize(user.getFirstName()) + " " + normalize(user.getLastName());
    return (query.isEmpty()
            || fullName.contains(query)
            || normalize(user.getEmail()).contains(query))
        && (enabled == null || user.isEnabled() == enabled)
        && (status == null || user.getStatus() == status)
        && (role == null || user.hasRole(role))
        && (activeSince == null
            || user.getLastActivity() != null && !user.getLastActivity().isBefore(activeSince));
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
