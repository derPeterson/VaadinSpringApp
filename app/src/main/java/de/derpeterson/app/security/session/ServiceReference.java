package de.derpeterson.app.security.session;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.web.context.support.WebApplicationContextUtils;

/** Nur der Beantyp überlebt Serialisierung; Auflösung erfolgt im aktuellen Servletkontext. */
public final class ServiceReference<T> implements Serializable {
  private final Class<T> type;
  // Veröffentlicht nur die Beanreferenz; Zustand und Synchronisierung gehören zum Spring-Service.
  private transient volatile T live;

  public ServiceReference(Class<T> type, T live) {
    this.type = Objects.requireNonNull(type);
    this.live = Objects.requireNonNull(live);
  }

  public T get() {
    T available = live;
    if (available != null) {
      return available;
    }
    if (!(VaadinService.getCurrent() instanceof VaadinServletService service)) {
      throw new IllegalStateException("Current Vaadin Spring context required");
    }
    available =
        WebApplicationContextUtils.getRequiredWebApplicationContext(
                service.getServlet().getServletContext())
            .getBean(type);
    live = available;
    return available;
  }
}
