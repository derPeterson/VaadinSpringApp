package de.derpeterson.app.websocket;

import com.vaadin.flow.shared.Registration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Nur lokale Invalidierung nach Commit; die Datenbank bleibt die Nachrichtenquelle. */
@Component
public class UserNotificationBroadcaster {
  private final CopyOnWriteArrayList<Consumer<Set<Long>>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Set<Long>> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void publish(Set<Long> userIds) {
    var immutable = Set.copyOf(userIds);
    for (var listener : listeners) {
      try {
        listener.accept(immutable);
      } catch (RuntimeException failure) {
        LoggerFactory.getLogger(getClass())
            .warn("Notification refresh deferred until next request", failure);
      }
    }
  }
}
