package de.derpeterson.app.websocket;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
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
        listeners.forEach(listener -> listener.accept(message));
    }
}
