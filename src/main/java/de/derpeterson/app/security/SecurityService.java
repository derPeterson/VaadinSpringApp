package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityService {

    private static final String AUTH_USER_SESSION_KEY = "authenticatedUser";

    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    public SecurityService(PersistentTokenBasedRememberMeServices rememberMeServices) {
        this.rememberMeServices = rememberMeServices;
    }

    public Optional<UserDetails> getAuthenticatedUser(HttpServletRequest request) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
        }

        UserDetails user = null;
        if (VaadinSession.getCurrent() != null) {
            user = VaadinSession.getCurrent().getAttribute(UserDetails.class);
        }

        if (user == null) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                user = (UserDetails) httpSession.getAttribute(AUTH_USER_SESSION_KEY);
            }
        }

        if (user != null) {
            restoreSecurityContext(user);
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public void storeAuthenticatedUser(HttpServletRequest request, UserDetails user, Boolean rememberMe) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(UserDetails.class, user);
        }

        HttpSession httpSession = request.getSession();
        httpSession.setAttribute(AUTH_USER_SESSION_KEY, user);

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
}
