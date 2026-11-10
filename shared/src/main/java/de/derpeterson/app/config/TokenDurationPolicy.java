package de.derpeterson.app.config;

import java.time.Duration;
import java.util.Objects;

/** Shared minimums for configured token lifetimes, independent of UI and persistence. */
public final class TokenDurationPolicy {
  public static final Duration MIN_VALIDITY = Duration.ofMinutes(1);
  public static final Duration MIN_RETENTION = Duration.ofMinutes(2);

  private TokenDurationPolicy() {}

  public static boolean meetsMinimumDuration(
      Duration configuredDuration, Duration minimumDuration) {
    Objects.requireNonNull(minimumDuration, "Minimum duration required");
    return configuredDuration != null && configuredDuration.compareTo(minimumDuration) >= 0;
  }
}
