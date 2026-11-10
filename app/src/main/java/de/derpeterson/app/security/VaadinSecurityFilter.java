package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.session.DurableSessionRepository;
import de.derpeterson.app.security.session.DurableSessionRepository.MissingSessionException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaadinSecurityFilter implements Filter {

  // Deferred lookup avoids the SecurityConfig/remember-me construction cycle.
  private final ObjectProvider<SecurityService> securityService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (httpRequest.getAttribute(SessionAdmissionService.FAILURE_REASON) instanceof String reason) {
      ((HttpServletResponse) response)
          .sendRedirect(AppRoute.LOGIN.getSecurityRoute() + "?error=" + reason);
      return;
    }
    boolean loginDocument =
        "GET".equals(httpRequest.getMethod())
            && AppRoute.LOGIN.getSecurityRoute().equals(httpRequest.getServletPath());
    SecurityService security = securityService.getObject();
    try {
      boolean hadPrincipal = hasPrincipal(httpRequest);
      if (security.getAuthenticatedUser(httpRequest).isEmpty() && hadPrincipal && !loginDocument) {
        // Stop old UI callbacks. Only the login document may continue after the security
        // service cleared the stale context and invalidated its old HTTP/Vaadin session.
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      if ("uidl".equals(httpRequest.getParameter("v-r"))
          || httpRequest.getParameter("v-r") == null && "GET".equals(httpRequest.getMethod())) {
        security.touchSession(httpRequest);
      }
    } catch (MissingSessionException _) {
      security.clearStaleAuthentication(httpRequest);
      if (!loginDocument) {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private boolean hasPrincipal(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    HttpSession session = request.getSession(false);
    VaadinSession vaadin = VaadinSession.getCurrent();
    return authentication != null && authentication.getPrincipal() instanceof UserDetails
        || vaadin != null && vaadin.getAttribute(UserDetails.class) != null
        || session != null && session.getAttribute("authenticatedUser") instanceof UserDetails
        || session != null && session.getAttribute(DurableSessionRepository.ACTIVE) != null;
  }
}
