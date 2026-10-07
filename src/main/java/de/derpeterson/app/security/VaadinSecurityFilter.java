package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class VaadinSecurityFilter implements Filter {

    // Deferred lookup avoids the SecurityConfig/remember-me construction cycle.
    private final ObjectProvider<SecurityService> securityService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = httpRequest.getSession(false);
        boolean hadPrincipal = authentication != null && authentication.getPrincipal() instanceof UserDetails
                || VaadinSession.getCurrent() != null && VaadinSession.getCurrent().getAttribute(UserDetails.class) != null
                || session != null && session.getAttribute("authenticatedUser") instanceof UserDetails;
        if (securityService.getObject().getAuthenticatedUser(httpRequest).isEmpty() && hadPrincipal) {
            // Also stop UIDL/VAADIN callbacks to an already open privileged view.
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
