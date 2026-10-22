package de.derpeterson.app.security;

import org.springframework.security.core.AuthenticationException;

public final class MaintenanceAuthenticationException extends AuthenticationException {
  public MaintenanceAuthenticationException() {
    super("Sign-in paused for maintenance");
  }
}
