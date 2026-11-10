package de.derpeterson.app.helper.logging;

import java.util.Arrays;
import java.util.IdentityHashMap;

/** Exceptiontexte können SQL-Werte, Mailinhalte oder Tokens enthalten; Typen/Frames bleiben. */
public final class LogSafety {
  private LogSafety() {}

  public static Throwable safeFailure(Throwable originalFailure) {
    return copy(originalFailure, new IdentityHashMap<>(), 0);
  }

  private static Throwable copy(
      Throwable originalFailure,
      IdentityHashMap<Throwable, Throwable> logSafeFailuresByOriginal,
      int causeDepth) {
    if (originalFailure == null) {
      return null;
    }
    Throwable cachedSafeFailure = logSafeFailuresByOriginal.get(originalFailure);
    if (cachedSafeFailure != null) {
      return cachedSafeFailure;
    }
    RuntimeException logSafeFailure = new RuntimeException(originalFailure.getClass().getName());
    StackTraceElement[] stackTraceFrames = originalFailure.getStackTrace();
    logSafeFailure.setStackTrace(
        Arrays.copyOf(stackTraceFrames, Math.min(stackTraceFrames.length, 128)));
    if (causeDepth >= 8 || logSafeFailuresByOriginal.size() >= 32) {
      return logSafeFailure;
    }
    logSafeFailuresByOriginal.put(originalFailure, logSafeFailure);
    if (originalFailure.getCause() != null && originalFailure.getCause() != originalFailure) {
      logSafeFailure.initCause(
          copy(originalFailure.getCause(), logSafeFailuresByOriginal, causeDepth + 1));
    }
    Throwable[] suppressedFailures = originalFailure.getSuppressed();
    for (int i = 0; i < Math.min(suppressedFailures.length, 8); i++) {
      if (suppressedFailures[i] != originalFailure) {
        logSafeFailure.addSuppressed(
            copy(suppressedFailures[i], logSafeFailuresByOriginal, causeDepth + 1));
      }
    }
    return logSafeFailure;
  }
}
