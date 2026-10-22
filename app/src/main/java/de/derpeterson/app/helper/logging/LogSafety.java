package de.derpeterson.app.helper.logging;

import java.util.Arrays;
import java.util.IdentityHashMap;

/** Exceptiontexte können SQL-Werte, Mailinhalte oder Tokens enthalten; Typen/Frames bleiben. */
public final class LogSafety {
  private LogSafety() {}

  public static Throwable safeFailure(Throwable original) {
    return copy(original, new IdentityHashMap<>(), 0);
  }

  private static Throwable copy(
      Throwable original, IdentityHashMap<Throwable, Throwable> seen, int depth) {
    if (original == null) {
      return null;
    }
    Throwable known = seen.get(original);
    if (known != null) {
      return known;
    }
    RuntimeException safe = new RuntimeException(original.getClass().getName());
    StackTraceElement[] frames = original.getStackTrace();
    safe.setStackTrace(Arrays.copyOf(frames, Math.min(frames.length, 128)));
    if (depth >= 8 || seen.size() >= 32) {
      return safe;
    }
    seen.put(original, safe);
    if (original.getCause() != null && original.getCause() != original) {
      safe.initCause(copy(original.getCause(), seen, depth + 1));
    }
    Throwable[] suppressed = original.getSuppressed();
    for (int i = 0; i < Math.min(suppressed.length, 8); i++) {
      if (suppressed[i] != original) {
        safe.addSuppressed(copy(suppressed[i], seen, depth + 1));
      }
    }
    return safe;
  }
}
