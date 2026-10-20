package de.derpeterson.app.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.session.ServiceReference;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.MaintenanceView;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceUiGuard implements VaadinServiceInitListener {
  private final ServiceReference<MaintenanceService> maintenance;

  public MaintenanceUiGuard(MaintenanceService maintenance) {
    this.maintenance = new ServiceReference<>(MaintenanceService.class, maintenance);
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addIndexHtmlRequestListener(
        response -> adjustMaintenanceBase(response.getVaadinRequest(), response.getDocument()));
    event.getSource().addUIInitListener(init -> initializeUi(init.getUI()));
  }

  private void adjustMaintenanceBase(VaadinRequest vaadinRequest, Document document) {
    if (Boolean.TRUE.equals(vaadinRequest.getAttribute(MaintenanceHttpFilter.FORWARDED_DOCUMENT))
        && vaadinRequest instanceof VaadinServletRequest request) {
      // A forward changes the server path, but the browser still has e.g. /verification/token.
      // Its bootstrap resources must resolve from the application root, not /verification/.
      Element base = document.head().selectFirst("base");
      if (base != null) {
        base.attr("href", request.getContextPath() + "/");
      }
    }
  }

  private void initializeUi(UI ui) {
    installMaintenanceErrorHandler(ui.getSession());
    ui.addBeforeEnterListener(
        navigation -> {
          if (VaadinRequest.getCurrent() instanceof VaadinServletRequest request
              && maintenance.get().blocks(request.getHttpServletRequest())
              && navigation.getNavigationTarget() != LoginView.class
              && navigation.getNavigationTarget() != MaintenanceView.class) {
            navigation.rerouteTo(MaintenanceView.class);
          }
        });
  }

  private void installMaintenanceErrorHandler(VaadinSession session) {
    String marker = MaintenanceUiGuard.class.getName() + ".errorHandler";
    if (session.getAttribute(marker) != null) {
      return;
    }
    ErrorHandler previous = session.getErrorHandler();
    session.setErrorHandler(
        error -> {
          if (redirectMaintenanceFailure(error.getThrowable())) {
            return;
          }
          if (previous != null) {
            previous.error(error);
          } else {
            new DefaultErrorHandler().error(error);
          }
        });
    session.setAttribute(marker, Boolean.TRUE);
  }

  private boolean redirectMaintenanceFailure(Throwable failure) {
    while (failure != null) {
      if (failure instanceof MaintenanceAccessException && UI.getCurrent() != null) {
        String contextPath =
            VaadinService.getCurrentRequest() instanceof VaadinServletRequest current
                ? current.getContextPath()
                : "";
        UI.getCurrent()
            .getPage()
            .setLocation(contextPath + AppRoute.MAINTENANCE.getSecurityRoute());
        return true;
      }
      if (failure == failure.getCause()) {
        break;
      }
      failure = failure.getCause();
    }
    return false;
  }
}
