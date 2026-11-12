package de.derpeterson.app.security;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String emailAddress) throws UsernameNotFoundException {
    return toUserDetails(userRepository.findSecurityUser(emailAddress));
  }

  public UserDetails loadPasswordUserByUsername(String emailAddress) {
    List<SecurityUserRow> securityUserRows =
        userRepository.findPasswordSecurityUser(EmailIdentity.canonicalize(emailAddress));

    if (securityUserRows.isEmpty()
        || securityUserRows.getFirst().getPassword() == null
        || securityUserRows.getFirst().getPassword().isBlank()) {
      throw new UsernameNotFoundException("Password sign-in unavailable");
    }

    return toUserDetails(securityUserRows, true);
  }

  private UserDetails toUserDetails(List<UserRepository.SecurityUserRow> securityUserRows) {
    return toUserDetails(securityUserRows, false);
  }

  private UserDetails toUserDetails(
      List<UserRepository.SecurityUserRow> securityUserRows, boolean passwordLogin) {
    if (securityUserRows.isEmpty()) {
      throw new UsernameNotFoundException("User not found");
    }

    SecurityUserRow accountSecurityRow = securityUserRows.getFirst();
    List<GrantedAuthority> grantedAuthorities =
        securityUserRows.stream()
            .map(UserRepository.SecurityUserRow::getRole)
            .filter(Objects::nonNull)
            .flatMap(role -> role.getGrantedRoles().stream())
            .map(RoleType::name)
            .distinct()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    boolean pendingVerification = passwordLogin && accountSecurityRow.getPendingEmailVerification();
    boolean pendingReactivation = passwordLogin && accountSecurityRow.getSelfDeactivated();
    PortalUserDetails portalUserDetails =
        new PortalUserDetails(
            accountSecurityRow.getEmail(),
            accountSecurityRow.getPassword(),
            accountSecurityRow.getAccessVersion(),
            accountSecurityRow.isEnabled() || pendingVerification || pendingReactivation,
            grantedAuthorities);
    if (pendingVerification) {
      portalUserDetails.requireEmailVerification(accountSecurityRow.getId());
    }
    if (pendingReactivation) {
      portalUserDetails.requireAccountReactivation(accountSecurityRow.getId());
    }
    return portalUserDetails;
  }
}
