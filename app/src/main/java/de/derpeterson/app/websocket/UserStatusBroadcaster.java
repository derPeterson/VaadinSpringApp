package de.derpeterson.app.websocket;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserStatusBroadcaster {

  private final List<Consumer<UserStatusMessage>> userStatusListeners = new ArrayList<>();

  public record UserStatusMessage(Long userId, String oldStatus, String newStatus) {}

  // UI-Komponenten registrieren, die den Status empfangen sollen
  public synchronized Registration register(Consumer<UserStatusMessage> userStatusListener) {
    userStatusListeners.add(userStatusListener);

    return () -> {
      synchronized (UserStatusBroadcaster.this) {
        userStatusListeners.remove(userStatusListener);
      }
    };
  }

  // Nachricht an alle registrierten Listener senden
  public synchronized void broadcast(UserStatusMessage userStatusMessage) {
    // A snapshot also permits listeners to unregister themselves during delivery.
    for (Consumer<UserStatusMessage> userStatusListener : new ArrayList<>(userStatusListeners)) {
      try {
        userStatusListener.accept(userStatusMessage);
      } catch (RuntimeException listenerFailure) {
        log.error(
            "Status-Listener fehlgeschlagen für Benutzer {}",
            userStatusMessage.userId(),
            safeFailure(listenerFailure));
      }
    }
  }
}
