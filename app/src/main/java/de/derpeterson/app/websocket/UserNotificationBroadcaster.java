package de.derpeterson.app.websocket;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.shared.Registration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Nur lokale Invalidierung nach Commit; die Datenbank bleibt die Nachrichtenquelle. */
@Component
public class UserNotificationBroadcaster {
  private final CopyOnWriteArrayList<Consumer<Set<Long>>> notificationChangeListeners =
      new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Set<Long>> notificationChangeListener) {
    notificationChangeListeners.add(notificationChangeListener);
    return () -> notificationChangeListeners.remove(notificationChangeListener);
  }

  public void publish(Set<Long> userIds) {
    Set<Long> affectedUserIdsSnapshot = Set.copyOf(userIds);
    for (Consumer<Set<Long>> notificationChangeListener : notificationChangeListeners) {
      try {
        notificationChangeListener.accept(affectedUserIdsSnapshot);
      } catch (RuntimeException listenerFailure) {
        LoggerFactory.getLogger(getClass())
            .warn("Notification refresh deferred until next request", safeFailure(listenerFailure));
      }
    }
  }
}
