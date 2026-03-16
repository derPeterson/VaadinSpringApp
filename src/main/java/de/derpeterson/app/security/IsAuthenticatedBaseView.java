package de.derpeterson.app.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.views.HomeView;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class IsAuthenticatedBaseView extends TrackedUserAppLayout implements BeforeEnterObserver {

    protected IsAuthenticatedBaseView(MessageProperties messageProperties,
                                      SecurityService securityService,
                                      UserService userService,
                                      UserStatusBroadcaster userStatusBroadcaster,
                                      HttpServletRequest request) {
        super(messageProperties, securityService, userService, userStatusBroadcaster, request);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UserDetails authenticatedUser = securityService.getAuthenticatedUser(request).orElse(null);

        if (authenticatedUser == null) {
            event.forwardTo(LoginView.class);
            return;
        }

        if (!hasRequiredAccess(authenticatedUser)) {
            event.forwardTo(getUnauthorizedForwardTarget());
        }
    }

    protected boolean hasRequiredAccess(UserDetails authenticatedUser) {
        return true;
    }

    protected Class<? extends Component> getUnauthorizedForwardTarget() {
        return HomeView.class;
    }

    protected boolean hasRole(UserDetails authenticatedUser, String requiredRole) {
        if (authenticatedUser == null || requiredRole == null || requiredRole.isBlank()) {
            return false;
        }

        String normalizedRequiredRole = normalizeRole(requiredRole);

        return authenticatedUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .anyMatch(normalizedRequiredRole::equals);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }
}