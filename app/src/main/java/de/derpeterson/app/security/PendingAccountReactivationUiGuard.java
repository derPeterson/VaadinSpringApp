package de.derpeterson.app.security;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import de.derpeterson.app.security.session.ServiceReference;
import de.derpeterson.app.views.PendingAccountReactivationView;
import de.derpeterson.app.views.ReactivateAccountView;
import org.springframework.stereotype.Component;

/** Der erste Nachweis bleibt bis Abmelden oder Reaktivierung auf den Hinweis beschränkt. */
@Component
public final class PendingAccountReactivationUiGuard implements VaadinServiceInitListener {
  private final ServiceReference<PendingAccountReactivationFlow> pendingAccountReactivationFlow;

  public PendingAccountReactivationUiGuard(
      PendingAccountReactivationFlow pendingAccountReactivationFlow) {
    this.pendingAccountReactivationFlow =
        new ServiceReference<>(
            PendingAccountReactivationFlow.class, pendingAccountReactivationFlow);
  }

  @Override
  public void serviceInit(ServiceInitEvent serviceInitEvent) {
    serviceInitEvent
        .getSource()
        .addUIInitListener(uiInitEvent ->
            uiInitEvent.getUI().addBeforeEnterListener(this::guardPendingReactivation));
  }

  private void guardPendingReactivation(BeforeEnterEvent beforeEnterEvent) {
    Class<?> targetView = beforeEnterEvent.getNavigationTarget();
    if (targetView == PendingAccountReactivationView.class
        || targetView == ReactivateAccountView.class) {
      return;
    }
    if (VaadinRequest.getCurrent() instanceof VaadinServletRequest servletRequest
        && pendingAccountReactivationFlow
            .get()
            .currentAccount(servletRequest.getHttpServletRequest())
            .isPresent()) {
      beforeEnterEvent.forwardTo(PendingAccountReactivationView.class);
    }
  }
}
