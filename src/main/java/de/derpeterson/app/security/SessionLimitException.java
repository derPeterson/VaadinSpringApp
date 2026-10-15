package de.derpeterson.app.security;

import org.springframework.security.web.authentication.session.SessionAuthenticationException;

/** Valid credentials, but no free authenticated session slot. */
public class SessionLimitException extends SessionAuthenticationException {
    public SessionLimitException() {
        super("Maximum concurrent sessions reached");
    }
}
