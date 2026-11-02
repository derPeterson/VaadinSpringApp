package de.derpeterson.app.security;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.session.ServiceReference;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.RecoveryHelpView;
import de.derpeterson.app.views.TwoFactorView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class TwoFactorUiGuard implements VaadinServiceInitListener, VaadinRequestInterceptor {
  private final ServiceReference<SecurityService> security;
  private final ServiceReference<TwoFactorAuthorization> authorization;

  public TwoFactorUiGuard(SecurityService security, TwoFactorAuthorization authorization) {
    this.security = new ServiceReference<>(SecurityService.class, security);
    this.authorization = new ServiceReference<>(TwoFactorAuthorization.class, authorization);
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addVaadinRequestInterceptor(this);
    event
        .getSource()
        .addUIInitListener(
            init -> {
              // Echte Eingaben zählen. Heartbeat, Push, Polling und reine Serverupdates erzeugen
              // diese nativen DOM-Ereignisse nicht. Der abgelaufene Zugriff wird bereits vor RPC
              // gestoppt.
              for (String type : List.of("pointerdown", "keydown", "wheel")) {
                init.getUI()
                    .getElement()
                    .addEventListener(
                        type,
                        interaction -> {
                          if (VaadinRequest.getCurrent() instanceof VaadinServletRequest servlet
                              && security
                                  .get()
                                  .getAuthenticatedUser(servlet.getHttpServletRequest())
                                  .isPresent()) {
                            authorization.get().recordActivity(servlet.getHttpServletRequest());
                          }
                        })
                    .setFilter("event.isTrusted")
                    .throttle(30_000);
              }
              init.getUI()
                  .addBeforeEnterListener(
                      navigation -> {
                        if (VaadinRequest.getCurrent() instanceof VaadinServletRequest servlet
                            && navigation.getNavigationTarget() != TwoFactorView.class
                            && navigation.getNavigationTarget() != RecoveryHelpView.class) {
                          HttpServletRequest request = servlet.getHttpServletRequest();
                          if (security.get().getAuthenticatedUser(request).isPresent()
                              && (!authorization.get().sessionConfirmed(request)
                                  || navigation.getNavigationTarget() == AdminView.class
                                      && !authorization.get().administrationConfirmed(request))) {
                            navigation.forwardTo(
                                TwoFactorView.class,
                                navigation.getNavigationTarget() == AdminView.class
                                    ? QueryParameters.of("administration", "true")
                                    : QueryParameters.empty());
                          }
                        }
                      });
            });
  }

  @Override
  public void requestStart(VaadinRequest request, VaadinResponse response) {
    if (response == null && request instanceof VaadinServletRequest servlet) {
      HttpServletRequest http = servlet.getHttpServletRequest();
      if (security.get().getAuthenticatedUser(http).isPresent()
          && (!authorization.get().sessionConfirmed(http)
              || TwoFactorTransport.administrativeUi(http)
                  && !authorization.get().administrationConfirmed(http))
          && !TwoFactorTransport.allowedUi(http)) {
        TwoFactorTransport.redirectUi(http);
        CurrentInstance.clearAll();
        SecurityContextHolder.clearContext();
        throw new AccessDeniedException("Second factor required before UI action");
      }
    }
  }

  @Override
  public void handleException(
      VaadinRequest request, VaadinResponse response, VaadinSession session, Exception exception) {}

  @Override
  public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {}
}
