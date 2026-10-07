package de.derpeterson.app.model;

import java.util.Locale;

/** Shared identity contract for persistence, authentication and lookup. */
public final class EmailIdentity {
    private EmailIdentity() {
    }

    public static String canonicalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
