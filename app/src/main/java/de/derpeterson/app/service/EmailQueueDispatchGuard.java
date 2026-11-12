package de.derpeterson.app.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Koordiniert lokale H2-Instanzen, ohne eine Datenbanktransaktion während SMTP offen zu halten. */
@Component
public final class EmailQueueDispatchGuard {
  private static final ConcurrentMap<Path, DispatchLockState> FILE_LOCK_STATES =
      new ConcurrentHashMap<>();
  private final DispatchLockState dispatchLockState;

  public EmailQueueDispatchGuard(@Value("${spring.datasource.url}") String databaseUrl) {
    if (databaseUrl == null || databaseUrl.startsWith("jdbc:h2:mem:")) {
      dispatchLockState = new DispatchLockState(null);
      return;
    }
    if (!databaseUrl.startsWith("jdbc:h2:file:")) {
      throw new IllegalArgumentException("Mail dispatch coordination requires local H2 storage");
    }

    String databasePathText = databaseUrl.substring("jdbc:h2:file:".length()).split(";", 2)[0];
    if (databasePathText.startsWith("~/")) {
      databasePathText = System.getProperty("user.home") + databasePathText.substring(1);
    }
    Path databasePath = Path.of(databasePathText).toAbsolutePath().normalize();
    Path dispatchLockPath =
        databasePath.resolveSibling(databasePath.getFileName() + ".email-dispatch.lock");
    dispatchLockState = FILE_LOCK_STATES.computeIfAbsent(dispatchLockPath, DispatchLockState::new);
  }

  /** Die Sperre bleibt bis zum tatsächlichen Ende des Workers bestehen, auch bei Future.cancel. */
  public void runDispatch(Runnable dispatchAction) {
    dispatchLockState.localLock.readLock().lock();
    try {
      dispatchLockState.enterDispatch();
      try {
        dispatchAction.run();
      } finally {
        dispatchLockState.leaveDispatch();
      }
    } finally {
      dispatchLockState.localLock.readLock().unlock();
    }
  }

  /** Der Aufrufer muss seine Ergebnistransaktion innerhalb dieser Aktion vollständig beenden. */
  public <T> T runWhenIdle(Supplier<T> resolutionAction) {
    if (!dispatchLockState.localLock.writeLock().tryLock()) {
      throw new DispatchStillRunningException();
    }
    try {
      if (dispatchLockState.dispatchLockPath == null) {
        return resolutionAction.get();
      }
      try (FileChannel lockChannel = dispatchLockState.openLockChannel();
          FileLock exclusiveLock = lockChannel.tryLock()) {
        if (exclusiveLock == null) {
          throw new DispatchStillRunningException();
        }
        return resolutionAction.get();
      } catch (IOException lockFailure) {
        throw new UncheckedIOException("Mail dispatch coordination failed", lockFailure);
      }
    } finally {
      dispatchLockState.localLock.writeLock().unlock();
    }
  }

  public static final class DispatchStillRunningException extends IllegalStateException {
    private DispatchStillRunningException() {
      super("Mail dispatch is still running");
    }
  }

  private static final class DispatchLockState {
    private final ReentrantReadWriteLock localLock = new ReentrantReadWriteLock(true);
    private final Path dispatchLockPath;
    private int activeDispatchCount;
    private FileChannel sharedLockChannel;
    private FileLock sharedDispatchLock;

    private DispatchLockState(Path dispatchLockPath) {
      this.dispatchLockPath = dispatchLockPath;
    }

    private FileChannel openLockChannel() throws IOException {
      return FileChannel.open(
          dispatchLockPath,
          StandardOpenOption.CREATE,
          StandardOpenOption.READ,
          StandardOpenOption.WRITE);
    }

    private synchronized void enterDispatch() {
      if (dispatchLockPath != null && activeDispatchCount == 0) {
        try {
          sharedLockChannel = openLockChannel();
          sharedDispatchLock = sharedLockChannel.lock(0, Long.MAX_VALUE, true);
        } catch (IOException lockFailure) {
          closeSharedLock();
          throw new UncheckedIOException("Mail dispatch coordination failed", lockFailure);
        }
      }
      activeDispatchCount++;
    }

    private synchronized void leaveDispatch() {
      activeDispatchCount--;
      if (activeDispatchCount == 0) {
        closeSharedLock();
      }
    }

    private void closeSharedLock() {
      FileChannel closingChannel = sharedLockChannel;
      FileLock closingLock = sharedDispatchLock;
      sharedLockChannel = null;
      sharedDispatchLock = null;
      try (closingChannel;
          closingLock) {
        // Lock zuerst freigeben, danach den Kanal schließen; Fehler bleiben verkettet.
      } catch (IOException lockFailure) {
        throw new UncheckedIOException("Mail dispatch lock could not be closed", lockFailure);
      }
    }
  }
}
