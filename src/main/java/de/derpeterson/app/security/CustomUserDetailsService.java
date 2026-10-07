package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var rows = userRepository.findSecurityUser(email);
        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        var user = rows.getFirst();
        List<GrantedAuthority> authorities = rows.stream()
                .map(UserRepository.SecurityUserRow::getRole)
                .filter(java.util.Objects::nonNull)
                .map(RoleType::name)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), user.isEnabled(), true, true, true, authorities);
    }
}
