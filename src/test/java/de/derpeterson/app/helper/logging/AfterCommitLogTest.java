package de.derpeterson.app.helper.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class AfterCommitLogTest {
  private LoggerContext context;
  private Logger logger;
  private ListAppender<ILoggingEvent> logs;
  private StubTransactionManager manager;
  private TransactionTemplate transaction;

  @BeforeEach
  void setup() {
    context = new LoggerContext();
    logger = context.getLogger("commit-test");
    logger.setLevel(Level.DEBUG);
    logs = new ListAppender<>();
    logs.start();
    logger.addAppender(logs);
    manager = new StubTransactionManager();
    transaction = new TransactionTemplate(manager);
  }

  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.clear();
    context.stop();
  }

  @ParameterizedTest
  @ValueSource(strings = {"INFO", "DEBUG", "WARN"})
  void successAppearsOnlyAfterCommit(String level) {
    transaction.executeWithoutResult(
        status -> {
          emit(level);
          assertTrue(logs.list.isEmpty());
        });
    assertEquals(1, logs.list.size());
    assertEquals(Level.toLevel(level), logs.list.getFirst().getLevel());
    assertEquals("Committed userId=17", logs.list.getFirst().getFormattedMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"INFO", "DEBUG", "WARN"})
  void rollbackDoesNotReportCommittedEvents(String level) {
    transaction.executeWithoutResult(
        status -> {
          emit(level);
          status.setRollbackOnly();
        });
    assertTrue(logs.list.isEmpty());
  }

  @Test
  void failingCommitDoesNotReportSuccess() {
    manager.failCommit = true;
    assertThrows(
        TransactionSystemException.class,
        () -> transaction.executeWithoutResult(status -> emit("INFO")));
    assertTrue(logs.list.isEmpty());
  }

  @Test
  void completedIndependentOperationCanLogImmediately() {
    emit("INFO");
    assertEquals(1, logs.list.size());
  }

  @Test
  void pendingArgumentsAreAnArraySnapshot() {
    transaction.executeWithoutResult(
        status -> {
          Object[] fields = {17L};
          AfterCommitLog.info(logger, "Committed userId={}", fields);
          fields[0] = 99L;
        });
    assertEquals("Committed userId=17", logs.list.getFirst().getFormattedMessage());
  }

  @Test
  void unobservableCommitDoesNotClaimSuccess() {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    emit("INFO");
    assertTrue(logs.list.isEmpty());
  }

  private void emit(String level) {
    switch (level) {
      case "INFO" -> AfterCommitLog.info(logger, "Committed userId={}", 17L);
      case "DEBUG" -> AfterCommitLog.debug(logger, "Committed userId={}", 17L);
      case "WARN" -> AfterCommitLog.warn(logger, "Committed userId={}", 17L);
      default -> throw new IllegalArgumentException("Unknown fixture level");
    }
  }

  private static final class StubTransactionManager extends AbstractPlatformTransactionManager {
    private boolean failCommit;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      if (failCommit) {
        throw new TransactionSystemException("Fixture commit rejected");
      }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
