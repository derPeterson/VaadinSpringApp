package de.derpeterson.app.security;

import org.springframework.security.core.AuthenticationException;

/** No account existence, role or counter details may reach the client. */
public class LoginThrottledException extends AuthenticationException {
    public LoginThrottledException() {
        super("Password sign-in temporarily limited");
    }
}
