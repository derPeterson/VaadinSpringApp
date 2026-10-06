package de.derpeterson.app.websocket;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@Slf4j
public class UserStatusBroadcaster {

    private final List<Consumer<UserStatusMessage>> listeners = new ArrayList<>();

    public record UserStatusMessage(Long userId, String oldStatus, String newStatus) {
    }

    // UI-Komponenten registrieren, die den Status empfangen sollen
    public synchronized Registration register(Consumer<UserStatusMessage> listener) {
        listeners.add(listener);
        return () -> {
            synchronized (UserStatusBroadcaster.this) {
                listeners.remove(listener);
            }
        };
    }

    // Nachricht an alle registrierten Listener senden
    public synchronized void broadcast(UserStatusMessage message) {
        // A snapshot also permits listeners to unregister themselves during delivery.
        for (Consumer<UserStatusMessage> listener : new ArrayList<>(listeners)) {
            try {
                listener.accept(message);
            } catch (RuntimeException exception) {
                log.error("Status-Listener fehlgeschlagen für Benutzer {}", message.userId(), exception);
            }
        }
    }
}
