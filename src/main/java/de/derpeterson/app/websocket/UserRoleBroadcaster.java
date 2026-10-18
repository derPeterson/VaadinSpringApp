package de.derpeterson.app.websocket;

import com.vaadin.flow.shared.Registration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserRoleBroadcaster {
  private final CopyOnWriteArrayList<Consumer<Long>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(Consumer<Long> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void publish(Long userId) {
    for (var listener : listeners)
      try {
        listener.accept(userId);
      } catch (RuntimeException failure) {
        LoggerFactory.getLogger(getClass()).warn("Role UI refresh deferred", failure);
      }
  }
}
