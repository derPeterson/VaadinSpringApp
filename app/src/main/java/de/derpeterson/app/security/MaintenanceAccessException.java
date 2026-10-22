package de.derpeterson.app.security;

import org.springframework.security.access.AccessDeniedException;

public final class MaintenanceAccessException extends AccessDeniedException {
  public MaintenanceAccessException(String message) {
    super(message);
  }
}
