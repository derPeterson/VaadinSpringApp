package de.derpeterson.app.security;

import de.derpeterson.app.model.enums.AppRoute;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;

/** Token bootstrap for classic HTTP clients; runs after Remember-me and its token rotation. */
final class LoginCsrfTokenFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) request;
    if ("GET".equals(http.getMethod())
        && (http.getContextPath() + AppRoute.LOGIN.getSecurityRoute()).equals(http.getRequestURI())
        && request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
      HttpServletResponse outgoing = (HttpServletResponse) response;
      outgoing.setHeader(token.getHeaderName(), token.getToken());
      outgoing.setHeader("Cache-Control", "no-store");
    }
    chain.doFilter(request, response);
  }
}
