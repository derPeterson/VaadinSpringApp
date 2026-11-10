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
        .addUIInitListener(init -> {
          UI ui = init.getUI();
          registerActivity(ui);
          ui.addBeforeEnterListener(this::guardNavigation);
        });
  }

  private void registerActivity(UI ui) {
    // Echte Eingaben zählen. Heartbeat, Push und reine Serverupdates erzeugen diese Ereignisse
    // nicht.
    for (String type : List.of("pointerdown", "keydown", "wheel")) {
      ui.getElement()
          .addEventListener(type, event -> recordActivity())
          .setFilter("event.isTrusted")
          .throttle(30_000);
    }
  }

  private void recordActivity() {
    if (VaadinRequest.getCurrent() instanceof VaadinServletRequest servlet) {
      HttpServletRequest request = servlet.getHttpServletRequest();
      if (securityService.get().getAuthenticatedUser(request).isPresent()) {
        twoFactorAuthorization.get().recordActivity(request);
      }
    }
  }

  private void guardNavigation(BeforeEnterEvent navigation) {
    Class<?> target = navigation.getNavigationTarget();
    if (!(VaadinRequest.getCurrent() instanceof VaadinServletRequest servlet)) {
      return;
    }
    HttpServletRequest request = servlet.getHttpServletRequest();
    TwoFactorFlow flow = TwoFactorFlow.current(request);
    if (flow != null && securityService.get().getAuthenticatedUser(request).isPresent()) {
      forwardToActiveFlow(navigation, target, flow);
      return;
    }
    if (target == TwoFactorView.class || target == RecoveryHelpView.class) {
      return;
    }
    boolean administration = target == AdminView.class;
    if (securityService.get().getAuthenticatedUser(request).isPresent()
        && (!twoFactorAuthorization.get().sessionConfirmed(request)
            || administration && !twoFactorAuthorization.get().administrationConfirmed(request))) {
      navigation.forwardTo(
          TwoFactorView.class,
          administration ? QueryParameters.of("administration", "true") : QueryParameters.empty());
    }
  }

  private void forwardToActiveFlow(
      BeforeEnterEvent navigation, Class<?> target, TwoFactorFlow flow) {
    if (flow.help() && target != RecoveryHelpView.class) {
      navigation.forwardTo(RecoveryHelpView.class);
    } else if (!flow.help() && target != TwoFactorView.class) {
      navigation.forwardTo(
          TwoFactorView.class, new Location(flow.location().substring(1)).getQueryParameters());
    }
  }

  @Override
  public void requestStart(VaadinRequest request, VaadinResponse response) {
    if (response == null && request instanceof VaadinServletRequest servlet) {
      HttpServletRequest http = servlet.getHttpServletRequest();
      if (securityService.get().getAuthenticatedUser(http).isPresent()
          && (TwoFactorFlow.current(http) != null
              || !twoFactorAuthorization.get().sessionConfirmed(http)
              || TwoFactorTransport.administrativeUi(http)
                  && !twoFactorAuthorization.get().administrationConfirmed(http))
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
      VaadinRequest request, VaadinResponse response, VaadinSession session, Exception exception) {
    // Die Anwendung verarbeitet Ausnahmen über die reguläre Vaadin-Fehlerbehandlung.
  }

  @Override
  public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
    // Der Wächter hält keinen Zustand, der am Ende des Requests freigegeben werden muss.
  }
}
