package de.derpeterson.app.helper.logging;

import java.util.Arrays;
import java.util.IdentityHashMap;

/** Exceptiontexte können SQL-Werte, Mailinhalte oder Tokens enthalten; Typen/Frames bleiben. */
public final class LogSafety {
  private LogSafety() {}

  public static Throwable safeFailure(Throwable originalFailure) {
    return copyFailureForLogging(originalFailure, new IdentityHashMap<>(), 0);
  }

  private static Throwable copyFailureForLogging(
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
          copyFailureForLogging(
              originalFailure.getCause(), logSafeFailuresByOriginal, causeDepth + 1));
    }

    Throwable[] suppressedFailures = originalFailure.getSuppressed();
    for (int suppressedFailureIndex = 0;
        suppressedFailureIndex < Math.min(suppressedFailures.length, 8);
        suppressedFailureIndex++) {
      if (suppressedFailures[suppressedFailureIndex] != originalFailure) {
        logSafeFailure.addSuppressed(
            copyFailureForLogging(
                suppressedFailures[suppressedFailureIndex],
                logSafeFailuresByOriginal,
                causeDepth + 1));
      }
    }

    return logSafeFailure;
  }
}
