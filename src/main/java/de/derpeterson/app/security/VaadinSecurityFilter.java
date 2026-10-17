package de.derpeterson.app.security;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.enums.AppRoute;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    HttpSession session = httpRequest.getSession(false);
    boolean hadPrincipal =
        authentication != null && authentication.getPrincipal() instanceof UserDetails
            || VaadinSession.getCurrent() != null
                && VaadinSession.getCurrent().getAttribute(UserDetails.class) != null
            || session != null && session.getAttribute("authenticatedUser") instanceof UserDetails;
    boolean loginDocument =
        "GET".equals(httpRequest.getMethod())
            && AppRoute.LOGIN.getSecurityRoute().equals(httpRequest.getServletPath());
    if (securityService.getObject().getAuthenticatedUser(httpRequest).isEmpty()
        && hadPrincipal
        && !loginDocument) {
      // Stop old UI callbacks. Only the login document may continue after the security
      // service cleared the stale context and invalidated its old HTTP/Vaadin session.
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    if ("uidl".equals(httpRequest.getParameter("v-r"))
        || httpRequest.getParameter("v-r") == null && "GET".equals(httpRequest.getMethod()))
      securityService.getObject().touchSession(httpRequest);
    chain.doFilter(request, response);
  }
}
