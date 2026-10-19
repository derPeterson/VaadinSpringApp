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
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.MaintenanceView;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaintenanceUiGuard implements VaadinServiceInitListener {
  private final MaintenanceService maintenance;

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addIndexHtmlRequestListener(
        response -> {
          if (Boolean.TRUE.equals(
                  response
                      .getVaadinRequest()
                      .getAttribute(MaintenanceHttpFilter.FORWARDED_DOCUMENT))
              && response.getVaadinRequest() instanceof VaadinServletRequest request) {
            // A forward changes the server path, but the browser still has e.g.
            // /verification/token.
            // Its bootstrap resources must resolve from the application root, not /verification/.
            Element base = response.getDocument().head().selectFirst("base");
            if (base != null) {
              base.attr("href", request.getContextPath() + "/");
            }
          }
        });
    event
        .getSource()
        .addUIInitListener(
            init -> {
              UI ui = init.getUI();
              VaadinSession session = ui.getSession();
              String marker = MaintenanceUiGuard.class.getName() + ".errorHandler";
              if (session.getAttribute(marker) == null) {
                ErrorHandler previous = session.getErrorHandler();
                session.setErrorHandler(
                    error -> {
                      Throwable failure = error.getThrowable();
                      while (failure != null) {
                        if (failure instanceof MaintenanceAccessException
                            && UI.getCurrent() != null) {
                          String contextPath =
                              VaadinService.getCurrentRequest()
                                      instanceof VaadinServletRequest current
                                  ? current.getContextPath()
                                  : "";
                          UI.getCurrent()
                              .getPage()
                              .setLocation(contextPath + AppRoute.MAINTENANCE.getSecurityRoute());
                          return;
                        }
                        if (failure == failure.getCause()) {
                          break;
                        }
                        failure = failure.getCause();
                      }
                      if (previous != null) {
                        previous.error(error);
                      } else {
                        new DefaultErrorHandler().error(error);
                      }
                    });
                session.setAttribute(marker, Boolean.TRUE);
              }
              ui.addBeforeEnterListener(
                  navigation -> {
                    if (VaadinRequest.getCurrent() instanceof VaadinServletRequest request
                        && maintenance.blocks(request.getHttpServletRequest())
                        && navigation.getNavigationTarget() != LoginView.class
                        && navigation.getNavigationTarget() != MaintenanceView.class) {
                      navigation.rerouteTo(MaintenanceView.class);
                    }
                  });
            });
  }
}
