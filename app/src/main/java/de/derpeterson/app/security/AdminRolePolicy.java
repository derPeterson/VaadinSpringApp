package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
  private final UserRepository userRepository;

  public void requireSuperadmin() {
    if (!loadCurrentRoleTypes().contains(RoleType.ROLE_SUPERADMIN)) {
      throw roleAssignmentDeniedException();
    }
  }

  public void authorizeMutation(Long targetUserId, Set<RoleType> requestedRoleTypes) {
    Set<RoleType> currentRoleTypes = loadCurrentRoleTypes();
    if (!currentRoleTypes.contains(RoleType.ROLE_ADMIN)
        && !currentRoleTypes.contains(RoleType.ROLE_SUPERADMIN)) {
      throw roleAssignmentDeniedException();
    }

    if (targetsSuperadmin(targetUserId, requestedRoleTypes)
        && !currentRoleTypes.contains(RoleType.ROLE_SUPERADMIN)) {
      throw roleAssignmentDeniedException();
    }
  }

  public void authorizeProtectedMutation(Long targetUserId, Set<RoleType> requestedRoleTypes) {
    if (targetsSuperadmin(targetUserId, requestedRoleTypes)
        && !loadCurrentRoleTypes().contains(RoleType.ROLE_SUPERADMIN)) {
      throw roleAssignmentDeniedException();
    }
  }

  public boolean canManageSuperadmins() {

    try {
      return loadCurrentRoleTypes().contains(RoleType.ROLE_SUPERADMIN);
    } catch (AccessDeniedException _) {
      return false;
    }
  }

  private boolean targetsSuperadmin(Long targetUserId, Set<RoleType> requestedRoleTypes) {
    return requestedRoleTypes.contains(RoleType.ROLE_SUPERADMIN)
        || targetUserId != null
            && userRepository.findExplicitRoles(targetUserId).contains(RoleType.ROLE_SUPERADMIN);
  }

  private Set<RoleType> loadCurrentRoleTypes() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails authenticatedUserDetails)) {
      throw roleAssignmentDeniedException();
    }
    try {
      List<SecurityUserRow> securityUserRows =
          userRepository.findSecurityUser(authenticatedUserDetails.getUsername());
      if (securityUserRows.isEmpty() || !securityUserRows.getFirst().isEnabled()) {
        throw roleAssignmentDeniedException();
      }

      EnumSet<RoleType> currentRoleTypes = EnumSet.noneOf(RoleType.class);
      securityUserRows.stream()
          .map(UserRepository.SecurityUserRow::getRole)
          .filter(Objects::nonNull)
          .forEach(currentRoleTypes::add);

      return currentRoleTypes;
    } catch (UsernameNotFoundException _) {
      throw new AccessDeniedException("Account unavailable");
    }
  }

  private static AccessDeniedException roleAssignmentDeniedException() {
    return new AccessDeniedException("Administrative role assignment denied");
  }
}
