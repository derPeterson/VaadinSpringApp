package de.derpeterson.app.websocket;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.shared.Registration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Meldet nach Commit nur eine Änderung; die Ansichten laden mit eigenen Berechtigungen nach. */
@Component
@Slf4j
public class SupportTicketBroadcaster {
  public record Change(Long requesterId, Long viewerId) {
    public boolean affects(Long currentViewer, boolean administration) {
      if (viewerId != null) {
        return viewerId.equals(currentViewer);
      }
      return administration || requesterId != null && requesterId.equals(currentViewer);
    }
  }

  private final CopyOnWriteArrayList<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Change> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void ticketChanged(Long requesterId) {
    afterCommit(new Change(requesterId, null));
  }

  public void viewerChanged(Long viewerId) {
    afterCommit(new Change(null, viewerId));
  }

  private void afterCommit(Change change) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            for (Consumer<Change> listener : listeners) {
              try {
                listener.accept(change);
              } catch (RuntimeException failure) {
                log.warn("Ticket refresh deferred until next request", safeFailure(failure));
              }
            }
          }
        });
  }
}
