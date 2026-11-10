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
  public record UserProfileChangeMessage(Long userId, String firstName, String lastName) {}

  private final CopyOnWriteArrayList<Consumer<UserProfileChangeMessage>> profileChangeListeners =
      new CopyOnWriteArrayList<>();

  public Registration register(Consumer<UserProfileChangeMessage> profileChangeListener) {
    profileChangeListeners.add(profileChangeListener);
    return () -> profileChangeListeners.remove(profileChangeListener);
  }

  public void afterCommit(UserEntity updatedUserEntity) {
    UserProfileChangeMessage profileChangeMessage =
        new UserProfileChangeMessage(
            updatedUserEntity.getId(),
            updatedUserEntity.getFirstName(),
            updatedUserEntity.getLastName());
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            broadcast(profileChangeMessage);
          }
        });
  }

  public void broadcast(UserProfileChangeMessage profileChangeMessage) {
    for (Consumer<UserProfileChangeMessage> profileChangeListener : profileChangeListeners) {
      try {
        profileChangeListener.accept(profileChangeMessage);
      } catch (RuntimeException listenerFailure) {
        log.warn(
            "Profilanzeige konnte nicht aktualisiert werden: {}",
            profileChangeMessage.userId(),
            safeFailure(listenerFailure));
      }
    }
  }
}
