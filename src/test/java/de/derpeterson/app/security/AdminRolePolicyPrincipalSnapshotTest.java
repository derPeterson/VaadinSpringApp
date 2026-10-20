package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class AdminRolePolicyPrincipalSnapshotTest {
  private final UserRepository users = mock(UserRepository.class);
  private final AdminRolePolicy policy = new AdminRolePolicy(users);

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authorizationUsesThePrincipalThatWasActuallyChecked() {
    Authentication authentication = mock(Authentication.class);
    UserDetails principal =
        User.withUsername("policy@example.test").password("fixture").roles("SUPERADMIN").build();
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(principal, null);
    SecurityUserRow row = mock(SecurityUserRow.class);
    when(row.isEnabled()).thenReturn(true);
    when(row.getRole()).thenReturn(RoleType.ROLE_SUPERADMIN);
    when(users.findSecurityUser(principal.getUsername())).thenReturn(List.of(row));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertDoesNotThrow(() -> policy.authorizeMutation(null, Set.of(RoleType.ROLE_USER)));

    verify(authentication, times(1)).getPrincipal();
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "unauthenticated", "null-principal", "wrong-principal"})
  void invalidAuthenticationIsDeniedBeforeTheRoleLookup(String state) {
    SecurityContextHolder.clearContext();
    if (!state.equals("missing")) {
      Authentication authentication = mock(Authentication.class);
      when(authentication.isAuthenticated()).thenReturn(!state.equals("unauthenticated"));
      when(authentication.getPrincipal())
          .thenReturn(state.equals("wrong-principal") ? "anonymous" : null);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    Set<RoleType> desiredRoles = Set.of(RoleType.ROLE_USER);

    assertThrows(AccessDeniedException.class, () -> policy.authorizeMutation(null, desiredRoles));

    verifyNoInteractions(users);
  }
}
