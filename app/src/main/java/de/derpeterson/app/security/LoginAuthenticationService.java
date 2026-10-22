package de.derpeterson.app.security;

import de.derpeterson.app.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;

/** One password entry point shared by the Vaadin button and HTTP form filter. */
@RequiredArgsConstructor
public class LoginAuthenticationService implements AuthenticationProvider {
  private final AuthenticationProvider provider;
  private final LoginAttemptService attempts;

  @Override
  public boolean supports(Class<?> type) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
  }

  @Override
  public Authentication authenticate(Authentication request) {
    if (!(request instanceof UsernamePasswordAuthenticationToken)) {
      throw new AuthenticationServiceException("Unsupported password sign-in request");
    }
    Authentication result =
        attempts.authenticate(request.getName(), () -> provider.authenticate(request));
    if (result instanceof CredentialsContainer credentials) {
      credentials.eraseCredentials();
    }
    return result;
  }
}
