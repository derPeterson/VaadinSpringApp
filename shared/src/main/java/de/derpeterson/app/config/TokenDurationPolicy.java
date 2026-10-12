package de.derpeterson.app.config;

import java.time.Duration;
import java.util.Objects;

/** Shared minimums for configured token lifetimes, independent of UI and persistence. */
public final class TokenDurationPolicy {
    public static final Duration MIN_VALIDITY = Duration.ofMinutes(1);
    public static final Duration MIN_RETENTION = Duration.ofMinutes(2);

    private TokenDurationPolicy() {
    }

    public static boolean isAtLeast(Duration duration, Duration minimum) {
        Objects.requireNonNull(minimum, "Minimum duration required");
        return duration != null && duration.compareTo(minimum) >= 0;
    }
}
