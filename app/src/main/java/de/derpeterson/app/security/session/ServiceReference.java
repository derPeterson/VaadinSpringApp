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
    T resolvedService = liveService;
    if (resolvedService != null) {
      return resolvedService;
    }

    if (!(VaadinService.getCurrent() instanceof VaadinServletService vaadinServletService)) {
      throw new IllegalStateException("Current Vaadin Spring context required");
    }

    resolvedService =
        WebApplicationContextUtils.getRequiredWebApplicationContext(
                vaadinServletService.getServlet().getServletContext())
            .getBean(serviceType);
    liveService = resolvedService;

    return resolvedService;
  }
}
