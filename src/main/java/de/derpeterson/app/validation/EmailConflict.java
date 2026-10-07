package de.derpeterson.app.validation;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

/** Recognizes only the named email uniqueness constraint, never arbitrary integrity failures. */
public final class EmailConflict {
    private EmailConflict() {
    }

    public static boolean isDuplicate(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if ("23505".equals(violation.getSQLState()) && name != null) {
                    // H2 exposes the constraint plus backing index/table; other dialects
                    // may expose the exact constraint name. Do not match message text.
                    String normalized = name.toLowerCase(Locale.ROOT);
                    if (normalized.equals("uk_users_email")
                            || normalized.matches("(?:[a-z0-9_]+\\.)?uk_users_email index [a-z0-9_.]+(?: on .*)?")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
