package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Frische explizite Rollen; bestehende Superadminkonten auch gegen indirekte Übernahme schützen.
 */
@Component
@RequiredArgsConstructor
public class AdminRolePolicy {
  private final UserRepository users;

  public void authorizeMutation(Long targetId, Set<RoleType> desired) {
    var current = currentRoles();
    if (!current.contains(RoleType.ROLE_ADMIN) && !current.contains(RoleType.ROLE_SUPERADMIN))
      denied();
    if (protectedTarget(targetId, desired) && !current.contains(RoleType.ROLE_SUPERADMIN)) denied();
  }

  public void authorizeProtectedMutation(Long targetId, Set<RoleType> desired) {
    if (protectedTarget(targetId, desired) && !currentRoles().contains(RoleType.ROLE_SUPERADMIN))
      denied();
  }

  public boolean canManageSuperadmins() {
    try {
      return currentRoles().contains(RoleType.ROLE_SUPERADMIN);
    } catch (AccessDeniedException denied) {
      return false;
    }
  }

  private boolean protectedTarget(Long targetId, Set<RoleType> desired) {
    return desired.contains(RoleType.ROLE_SUPERADMIN)
        || targetId != null && users.findExplicitRoles(targetId).contains(RoleType.ROLE_SUPERADMIN);
  }

  private Set<RoleType> currentRoles() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails)) {
      denied();
    }
    try {
      var rows = users.findSecurityUser(((UserDetails) auth.getPrincipal()).getUsername());
      if (rows.isEmpty() || !rows.getFirst().isEnabled()) denied();
      var result = EnumSet.noneOf(RoleType.class);
      rows.stream()
          .map(UserRepository.SecurityUserRow::getRole)
          .filter(Objects::nonNull)
          .forEach(result::add);
      return result;
    } catch (UsernameNotFoundException missing) {
      throw new AccessDeniedException("Account unavailable");
    }
  }

  private static void denied() {
    throw new AccessDeniedException("Administrative role assignment denied");
  }
}
