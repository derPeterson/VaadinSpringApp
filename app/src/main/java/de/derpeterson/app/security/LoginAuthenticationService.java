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
  private final AuthenticationProvider authenticationProvider;
  private final LoginAttemptService loginAttemptService;

  @Override
  public boolean supports(Class<?> authenticationType) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authenticationType);
  }

  @Override
  public Authentication authenticate(Authentication passwordAuthenticationRequest) {
    if (!(passwordAuthenticationRequest instanceof UsernamePasswordAuthenticationToken)) {
      throw new AuthenticationServiceException("Unsupported password sign-in request");
    }

    Authentication authenticatedPasswordLogin =
        loginAttemptService.authenticate(
            passwordAuthenticationRequest.getName(),
            () -> authenticationProvider.authenticate(passwordAuthenticationRequest));

    if (authenticatedPasswordLogin instanceof CredentialsContainer credentials) {
      credentials.eraseCredentials();
    }

    return authenticatedPasswordLogin;
  }
}
