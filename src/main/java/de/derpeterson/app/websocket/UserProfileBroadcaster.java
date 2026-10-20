package de.derpeterson.app.websocket;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.model.UserEntity;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Eigener Profilkanal; Avatar-/Namensänderungen sind keine Statuswechsel. */
@Component
@Slf4j
public class UserProfileBroadcaster {
  public record Message(Long userId, String firstName, String lastName) {}

  private final CopyOnWriteArrayList<Consumer<Message>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Message> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void afterCommit(UserEntity user) {
    Message message = new Message(user.getId(), user.getFirstName(), user.getLastName());
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            broadcast(message);
          }
        });
  }

  public void broadcast(Message message) {
    for (Consumer<Message> listener : listeners) {
      try {
        listener.accept(message);
      } catch (RuntimeException failure) {
        log.warn(
            "Profilanzeige konnte nicht aktualisiert werden: {}",
            message.userId(),
            safeFailure(failure));
      }
    }
  }
}
