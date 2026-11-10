package de.derpeterson.app.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
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
  private final ServiceReference<SecurityService> securityService;
  private final ServiceReference<TwoFactorAuthorization> twoFactorAuthorization;

  public TwoFactorUiGuard(
      SecurityService securityService, TwoFactorAuthorization twoFactorAuthorization) {
    this.securityService = new ServiceReference<>(SecurityService.class, securityService);
    this.twoFactorAuthorization =
        new ServiceReference<>(TwoFactorAuthorization.class, twoFactorAuthorization);
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addVaadinRequestInterceptor(this);
    event
        .getSource()
        .addUIInitListener(uiInitEvent -> {
          UI ui = uiInitEvent.getUI();

          registerActivity(ui);
          ui.addBeforeEnterListener(this::guardNavigation);
        });
  }

  private void registerActivity(UI ui) {
    // Echte Eingaben zählen. Heartbeat, Push und reine Serverupdates erzeugen diese Ereignisse
    // nicht.
    for (String eventType : List.of("pointerdown", "keydown", "wheel")) {
      ui.getElement()
          .addEventListener(eventType, event -> recordActivity())
          .setFilter("event.isTrusted")
          .throttle(30_000);
    }
  }

  private void recordActivity() {
    if (VaadinRequest.getCurrent() instanceof VaadinServletRequest vaadinServletRequest) {
      HttpServletRequest httpRequest = vaadinServletRequest.getHttpServletRequest();
      if (securityService.get().getAuthenticatedUser(httpRequest).isPresent()) {
        twoFactorAuthorization.get().recordActivity(httpRequest);
      }
    }
  }

  private void guardNavigation(BeforeEnterEvent beforeEnterEvent) {
    Class<?> navigationTarget = beforeEnterEvent.getNavigationTarget();
    if (!(VaadinRequest.getCurrent() instanceof VaadinServletRequest vaadinServletRequest)) {
      return;
    }

    HttpServletRequest httpRequest = vaadinServletRequest.getHttpServletRequest();
    TwoFactorFlow activeTwoFactorFlow = TwoFactorFlow.current(httpRequest);

    if (activeTwoFactorFlow != null
        && securityService.get().getAuthenticatedUser(httpRequest).isPresent()) {
      forwardToActiveFlow(beforeEnterEvent, navigationTarget, activeTwoFactorFlow);
      return;
    }

    if (navigationTarget == TwoFactorView.class || navigationTarget == RecoveryHelpView.class) {
      return;
    }

    boolean administrationRequested = navigationTarget == AdminView.class;
    if (securityService.get().getAuthenticatedUser(httpRequest).isPresent()
        && (!twoFactorAuthorization.get().sessionConfirmed(httpRequest)
            || administrationRequested
                && !twoFactorAuthorization.get().administrationConfirmed(httpRequest))) {
      beforeEnterEvent.forwardTo(
          TwoFactorView.class,
          administrationRequested
              ? QueryParameters.of("administration", "true")
              : QueryParameters.empty());
    }
  }

  private void forwardToActiveFlow(
      BeforeEnterEvent beforeEnterEvent,
      Class<?> navigationTarget,
      TwoFactorFlow activeTwoFactorFlow) {
    if (activeTwoFactorFlow.help() && navigationTarget != RecoveryHelpView.class) {
      beforeEnterEvent.forwardTo(RecoveryHelpView.class);
    } else if (!activeTwoFactorFlow.help() && navigationTarget != TwoFactorView.class) {
      beforeEnterEvent.forwardTo(
          TwoFactorView.class,
          new Location(activeTwoFactorFlow.location().substring(1)).getQueryParameters());
    }
  }

  @Override
  public void requestStart(VaadinRequest request, VaadinResponse response) {
    if (response == null && request instanceof VaadinServletRequest vaadinServletRequest) {
      HttpServletRequest httpRequest = vaadinServletRequest.getHttpServletRequest();
      if (securityService.get().getAuthenticatedUser(httpRequest).isPresent()
          && (TwoFactorFlow.current(httpRequest) != null
              || !twoFactorAuthorization.get().sessionConfirmed(httpRequest)
              || TwoFactorTransport.administrativeUi(httpRequest)
                  && !twoFactorAuthorization.get().administrationConfirmed(httpRequest))
          && !TwoFactorTransport.allowedUi(httpRequest)) {
        TwoFactorTransport.redirectUi(httpRequest);
        CurrentInstance.clearAll();
        SecurityContextHolder.clearContext();
        throw new AccessDeniedException("Second factor required before UI action");
      }
    }
  }

  @Override
  public void handleException(
      VaadinRequest request, VaadinResponse response, VaadinSession session, Exception exception) {
    // Die Anwendung verarbeitet Ausnahmen über die reguläre Vaadin-Fehlerbehandlung.
  }

  @Override
  public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
    // Der Wächter hält keinen Zustand, der am Ende des Requests freigegeben werden muss.
  }
}
