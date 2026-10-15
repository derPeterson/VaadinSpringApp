package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Last persistence gate, shared by HTTP, Vaadin and Remember-me. */
public class AdmittedSecurityContextRepository extends HttpSessionSecurityContextRepository {
  private final SessionAdmissionService admissions;

  public AdmittedSecurityContextRepository(SessionAdmissionService admissions) {
    this.admissions = admissions;
  }

  @Override
  public void saveContext(
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    var authentication = context.getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      super.saveContext(context, request, response);
      return;
    }
    admissions.complete(
        authentication, request, response, () -> super.saveContext(context, request, response));
  }
}
