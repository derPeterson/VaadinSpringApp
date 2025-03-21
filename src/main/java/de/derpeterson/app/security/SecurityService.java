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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private static final String AUTH_USER_SESSION_KEY = "authenticatedUser";

    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    private final UserService userService;
    private final UserRepository userRepository;

    public Optional<UserDetails> getAuthenticatedUser(HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
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

    public void handleLogin(UserDetails user) throws AuthenticationException {
        Optional<UserEntity> userEntity = userRepository.findByEmail(user.getUsername());

        if (userEntity.isPresent()) {
            if (!userEntity.get().isStatusManuallySet()) {
                userEntity.get().setAutomaticStatus(UserStatus.AVAILABLE);
            }
            userEntity.get().updateLastActivity();
            userRepository.save(userEntity.get());
        }
    }
}
