package de.derpeterson.app.security.session;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.web.context.support.WebApplicationContextUtils;

/** Nur der Beantyp überlebt Serialisierung; Auflösung erfolgt im aktuellen Servletkontext. */
public final class ServiceReference<T> implements Serializable {
  private final Class<T> serviceType;
  // Veröffentlicht nur die Beanreferenz; Zustand und Synchronisierung gehören zum Spring-Service.
  private transient volatile T liveService;

  public ServiceReference(Class<T> serviceType, T liveService) {
    this.serviceType = Objects.requireNonNull(serviceType);
    this.liveService = Objects.requireNonNull(liveService);
  }

  public T get() {
    T available = liveService;
    if (available != null) {
      return available;
    }
    if (!(VaadinService.getCurrent() instanceof VaadinServletService service)) {
      throw new IllegalStateException("Current Vaadin Spring context required");
    }
    available =
        WebApplicationContextUtils.getRequiredWebApplicationContext(
                service.getServlet().getServletContext())
            .getBean(serviceType);
    liveService = available;
    return available;
  }
}
