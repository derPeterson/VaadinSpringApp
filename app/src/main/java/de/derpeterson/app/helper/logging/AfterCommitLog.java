package de.derpeterson.app.helper.logging;

import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Erfolg erst nach Commit; Argumente müssen unveränderliche, freigegebene Metadaten sein. */
public final class AfterCommitLog {
  private AfterCommitLog() {}

  public static void info(Logger logger, String message, Object... arguments) {
    Object[] snapshot = arguments.clone();
    afterCommit(() -> logger.info(message, snapshot));
  }

  public static void debug(Logger logger, String message, Object... arguments) {
    Object[] snapshot = arguments.clone();
    afterCommit(() -> logger.debug(message, snapshot));
  }

  public static void warn(Logger logger, String message, Object... arguments) {
    Object[] snapshot = arguments.clone();
    afterCommit(() -> logger.warn(message, snapshot));
  }

  private static void afterCommit(Runnable event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      // Bereits unabhängig abgeschlossene Repository-/Template-Operationen.
      event.run();
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
            event.run();
          }
        });
  }
}
