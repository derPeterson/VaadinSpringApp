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
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    Authentication authentication = context.getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      super.saveContext(context, request, response);
      return;
    }
    sessionAdmissionService.complete(
        authentication, request, response, () -> super.saveContext(context, request, response));
  }
}
