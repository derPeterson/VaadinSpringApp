package de.derpeterson.app.websocket;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.shared.Registration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserRoleBroadcaster {
  private final CopyOnWriteArrayList<Consumer<Long>> roleChangeListeners =
      new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Long> roleChangeListener) {
    roleChangeListeners.add(roleChangeListener);
    return () -> roleChangeListeners.remove(roleChangeListener);
  }

  public void publish(Long userId) {
    for (Consumer<Long> roleChangeListener : roleChangeListeners) {
      try {
        roleChangeListener.accept(userId);
      } catch (RuntimeException listenerFailure) {
        LoggerFactory.getLogger(getClass())
            .warn("Role UI refresh deferred", safeFailure(listenerFailure));
      }
    }
  }
}
