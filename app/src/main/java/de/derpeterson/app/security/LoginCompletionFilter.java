package de.derpeterson.app.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Covers errors after reservation, including context storage, cookie issuance and success handlers.
 */
public class LoginCompletionFilter implements Filter {
  private final SessionAdmissionService sessionAdmissionService;
  private final org.springframework.security.web.authentication.RememberMeServices
      rememberMeServices;

  public LoginCompletionFilter(
      SessionAdmissionService sessionAdmissionService,
      org.springframework.security.web.authentication.RememberMeServices rememberMeServices) {
    this.sessionAdmissionService = sessionAdmissionService;
    this.rememberMeServices = rememberMeServices;
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } catch (IOException | ServletException | RuntimeException loginCompletionFailure) {
      HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
      boolean rolledBackNewLogin = sessionAdmissionService.rollback(httpRequest);
      HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

      if (rolledBackNewLogin && !httpResponse.isCommitted()) {
        try {
          rememberMeServices.loginFail(httpRequest, httpResponse);
        } catch (RuntimeException cleanupFailure) {
          loginCompletionFailure.addSuppressed(cleanupFailure);
        }
      }

      throw loginCompletionFailure;
    }
  }
}
