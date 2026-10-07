package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private static final String AUTH_USER_SESSION_KEY = "authenticatedUser";

    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    private final UserService userService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Runs before Vaadin resolves/locks a UI, including for each WebSocket
     * message. Use the connection's HTTP session, not a worker-thread context
     * or VaadinSession attributes (which require the session lock).
     */
    public boolean hasCurrentSessionPrivileges(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true; // Public requests have no authenticated UI to revoke.
        }
        Object storedContext = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        Authentication authentication = storedContext instanceof SecurityContext context ? context.getAuthentication() : null;
        Object storedUser = session.getAttribute(AUTH_USER_SESSION_KEY);
        boolean valid = (authentication == null || !(authentication.getPrincipal() instanceof UserDetails principal)
                || hasCurrentPrivileges(principal, authentication))
                && (!(storedUser instanceof UserDetails storedPrincipal) || hasCurrentPrivileges(storedPrincipal, null));
        if (!valid) {
            session.removeAttribute(AUTH_USER_SESSION_KEY);
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            session.invalidate(); // Discard every old UI associated with this connection.
            SecurityContextHolder.clearContext();
        }
        return valid;
    }

    public Optional<UserDetails> getAuthenticatedUser(HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            if (hasCurrentPrivileges(userDetails, authentication)) {
                return Optional.of(userDetails);
            }
            clearStaleAuthentication(request);
            return Optional.empty();
        }

        UserDetails userDetails = null;
        if (VaadinSession.getCurrent() != null) {
            userDetails = VaadinSession.getCurrent().getAttribute(UserDetails.class);
        }

        if (userDetails == null) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                userDetails = (UserDetails) httpSession.getAttribute(AUTH_USER_SESSION_KEY);
            }
        }

        if (userDetails != null) {
            if (!hasCurrentPrivileges(userDetails, null)) {
                clearStaleAuthentication(request);
                return Optional.empty();
            }
            restoreSecurityContext(userDetails);
            return Optional.of(userDetails);
        }

        return Optional.empty();
    }

    public Optional<UserEntity> getCurrentUser() {
        HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
        return getCurrentUser(request);
    }

    public Optional<UserEntity> getCurrentUser(HttpServletRequest request) {
        Optional<UserDetails> authenticatedUser = getAuthenticatedUser(request);
        if (authenticatedUser.isPresent()) {
            return userRepository.findByEmail(authenticatedUser.get().getUsername());
        }
        return Optional.empty();
    }

    public void storeAuthenticatedUser(HttpServletRequest request, UserDetails user, Boolean rememberMe) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(UserDetails.class, user);
        }

        HttpSession httpSession = request.getSession();
        httpSession.setAttribute(AUTH_USER_SESSION_KEY, user);

        if (VaadinSession.getCurrent() != null) {
            Optional<UserEntity> userEntityOptional = userService.findByEmail(user.getUsername());
            userEntityOptional.ifPresent(userEntity -> VaadinSession.getCurrent().setLocale(userEntity.getPreferredLocale()));
        }

        var auth = restoreSecurityContext(user);

        if (Boolean.TRUE.equals(rememberMe)) {
            HttpServletRequest req = (HttpServletRequest) VaadinRequest.getCurrent();
            HttpServletResponse resp = (HttpServletResponse) VaadinResponse.getCurrent();

            rememberMeServices.setAlwaysRemember(true);

            rememberMeServices.loginSuccess(req, resp, auth);
        } else {
            rememberMeServices.setAlwaysRemember(false);
        }
    }

    private Authentication restoreSecurityContext(UserDetails user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    public void logout() {
        HttpServletRequest request = (HttpServletRequest) VaadinRequest.getCurrent();
        HttpServletResponse response = (HttpServletResponse) VaadinResponse.getCurrent();

        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().getSession().invalidate();
        }
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }

        if (rememberMeServices != null && SecurityContextHolder.getContext().getAuthentication() != null) {
            rememberMeServices.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        }

        SecurityContextHolder.clearContext();
    }

    private boolean hasCurrentPrivileges(UserDetails principal, Authentication authentication) {
        try {
            UserDetails current = userDetailsService.loadUserByUsername(principal.getUsername());
            var authorities = current.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            return current.isEnabled()
                    && authorities.equals(principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet()))
                    && (authentication == null || authorities.equals(authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority).collect(Collectors.toSet())));
        } catch (UsernameNotFoundException exception) {
            return false;
        }
    }

    private void clearStaleAuthentication(HttpServletRequest request) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(UserDetails.class, null);
        }

        HttpSession httpSession = request == null ? null : request.getSession(false);
        if (httpSession != null) {
            httpSession.removeAttribute(AUTH_USER_SESSION_KEY);
            httpSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            // Discard the old Vaadin UIs too: a later remember-me authentication
            // with reduced roles must not resume callbacks on an old admin view.
            httpSession.invalidate();
        }

        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void handleLogin(UserDetails user) throws AuthenticationException {
        Optional<UserEntity> userEntity = userRepository.findByEmailForUpdate(user.getUsername());

        if (userEntity.isPresent()) {
            if (!userEntity.get().isStatusManuallySet()) {
                userService.updateUserStatus(userEntity.get(), UserStatus.AVAILABLE, false);
            }
            userService.updateLastActivity(userEntity.get().getId());
        }
    }
}
