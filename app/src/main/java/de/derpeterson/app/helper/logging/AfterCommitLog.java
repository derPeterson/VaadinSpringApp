package de.derpeterson.app.helper.logging;

import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Erfolg erst nach Commit; Argumente müssen unveränderliche, freigegebene Metadaten sein. */
public final class AfterCommitLog {
  private AfterCommitLog() {}

  public static void info(Logger eventLogger, String logMessage, Object... logArguments) {
    Object[] logArgumentSnapshot = logArguments.clone();
    runLogActionAfterCommit(() -> eventLogger.info(logMessage, logArgumentSnapshot));
  }

  public static void debug(Logger eventLogger, String logMessage, Object... logArguments) {
    Object[] logArgumentSnapshot = logArguments.clone();
    runLogActionAfterCommit(() -> eventLogger.debug(logMessage, logArgumentSnapshot));
  }

  public static void warn(Logger eventLogger, String logMessage, Object... logArguments) {
    Object[] logArgumentSnapshot = logArguments.clone();
    runLogActionAfterCommit(() -> eventLogger.warn(logMessage, logArgumentSnapshot));
  }

  private static void runLogActionAfterCommit(Runnable logAction) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      // Bereits unabhängig abgeschlossene Repository-/Template-Operationen.
      logAction.run();
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      // Ohne beobachtbaren Commit keinen möglicherweise falschen Erfolg behaupten.
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            logAction.run();
          }
        });
  }
}
