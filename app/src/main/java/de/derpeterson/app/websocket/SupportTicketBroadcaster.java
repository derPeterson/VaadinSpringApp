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
  public record SupportTicketChangeMessage(Long requesterId, Long viewerId) {
    public boolean affects(Long currentViewerId, boolean hasAdministrativeAccess) {
      if (viewerId != null) {
        return viewerId.equals(currentViewerId);
      }

      return hasAdministrativeAccess || requesterId != null && requesterId.equals(currentViewerId);
    }
  }

  private final CopyOnWriteArrayList<Consumer<SupportTicketChangeMessage>> ticketChangeListeners =
      new CopyOnWriteArrayList<>();

  public Registration register(Consumer<SupportTicketChangeMessage> ticketChangeListener) {
    ticketChangeListeners.add(ticketChangeListener);

    return () -> ticketChangeListeners.remove(ticketChangeListener);
  }

  public void ticketChanged(Long requesterId) {
    afterCommit(new SupportTicketChangeMessage(requesterId, null));
  }

  public void viewerChanged(Long viewerId) {
    afterCommit(new SupportTicketChangeMessage(null, viewerId));
  }

  private void afterCommit(SupportTicketChangeMessage ticketChangeMessage) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            for (Consumer<SupportTicketChangeMessage> ticketChangeListener :
                ticketChangeListeners) {
              try {
                ticketChangeListener.accept(ticketChangeMessage);
              } catch (RuntimeException listenerFailure) {
                log.warn(
                    "Ticket refresh deferred until next request", safeFailure(listenerFailure));
              }
            }
          }
        });
  }
}
