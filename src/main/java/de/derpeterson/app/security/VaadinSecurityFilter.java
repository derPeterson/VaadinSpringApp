package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class VaadinSecurityFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(VaadinSecurityFilter.class);

    private static final String USER_SESSION_KEY = "authenticatedUser";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        logger.debug("🔥 VaadinSecurityFilter is called: {}", requestURI);

        // 🔄 If it is a Vaadin request, simply forward it
        if (requestURI.startsWith("/VAADIN/")) {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 1️⃣ Get user from VaadinSession first
            UserDetails user = null;
            if (VaadinSession.getCurrent() != null) {
                user = VaadinSession.getCurrent().getAttribute(UserDetails.class);
            }

            // 2️⃣ If not available, restore user from HttpSession
            if (user == null) {
                HttpSession httpSession = httpRequest.getSession(false);
                if (httpSession != null) {
                    user = (UserDetails) httpSession.getAttribute(USER_SESSION_KEY);
                }
            }

            // 3️⃣ If user exists, set in SecurityContext
            if (user != null) {
                logger.debug("✅ User found from VaadinSession or HttpSession: {}", user.getUsername());
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                logger.debug("⚠️ No user found in VaadinSession or HttpSession.");
            }
        }

        chain.doFilter(request, response);
    }
}
