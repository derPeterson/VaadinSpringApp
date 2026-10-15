package de.derpeterson.app.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Covers errors after reservation, including context storage, cookie issuance and success handlers.
 */
public class LoginCompletionFilter implements Filter {
  private final SessionAdmissionService admissions;
  private final org.springframework.security.web.authentication.RememberMeServices rememberMe;

  public LoginCompletionFilter(
      SessionAdmissionService admissions,
      org.springframework.security.web.authentication.RememberMeServices rememberMe) {
    this.admissions = admissions;
    this.rememberMe = rememberMe;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException failure) {
      boolean newLogin = admissions.rollback((HttpServletRequest) request);
      var httpResponse = (jakarta.servlet.http.HttpServletResponse) response;
      if (newLogin && !httpResponse.isCommitted()) {
        try {
          rememberMe.loginFail((HttpServletRequest) request, httpResponse);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
  }
}
