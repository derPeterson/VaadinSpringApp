package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import java.util.List;
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
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return toUserDetails(userRepository.findSecurityUser(email));
  }

  public UserDetails loadPasswordUserByUsername(String email) {
    return toUserDetails(
        userRepository.findPasswordSecurityUser(
            de.derpeterson.app.model.EmailIdentity.canonicalize(email)));
  }

  private UserDetails toUserDetails(List<UserRepository.SecurityUserRow> rows) {
    if (rows.isEmpty()) {
      throw new UsernameNotFoundException("User not found");
    }
    SecurityUserRow user = rows.getFirst();
    List<GrantedAuthority> authorities =
        rows.stream()
            .map(UserRepository.SecurityUserRow::getRole)
            .filter(java.util.Objects::nonNull)
            .flatMap(role -> role.getGrantedRoles().stream())
            .map(RoleType::name)
            .distinct()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(), user.getPassword(), user.isEnabled(), true, true, true, authorities);
  }
}
