package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Last persistence gate, shared by HTTP, Vaadin and Remember-me. */
public class AdmittedSecurityContextRepository extends HttpSessionSecurityContextRepository {
  private final SessionAdmissionService sessionAdmissionService;

  public AdmittedSecurityContextRepository(SessionAdmissionService sessionAdmissionService) {
    this.sessionAdmissionService = sessionAdmissionService;
  }

  @Override
  public void saveContext(
      SecurityContext securityContext,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authentication = securityContext.getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      super.saveContext(securityContext, httpRequest, httpResponse);
      return;
    }

    sessionAdmissionService.complete(
        authentication,
        httpRequest,
        httpResponse,
        () -> super.saveContext(securityContext, httpRequest, httpResponse));
  }
}
