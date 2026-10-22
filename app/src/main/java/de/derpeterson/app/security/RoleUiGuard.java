package de.derpeterson.app.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlRequestHandler;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.session.ServiceReference;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Aktualisiert alte Ansichten mit erhaltenem HTTP-Login und stoppt vorher ihre alten RPCs. */
@Component
public class RoleUiGuard implements VaadinServiceInitListener {
  private final ServiceReference<SecurityService> security;
  private final ServiceReference<UserRoleBroadcaster> events;

  public RoleUiGuard(SecurityService security, UserRoleBroadcaster events) {
    this.security = new ServiceReference<>(SecurityService.class, security);
    this.events = new ServiceReference<>(UserRoleBroadcaster.class, events);
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addRequestHandler(
        new SynchronizedRequestHandler() {
          @Override
          protected boolean canHandleRequest(VaadinRequest request) {
            return HandlerHelper.isRequestType(request, HandlerHelper.RequestType.UIDL);
          }

          @Override
          public boolean synchronizedHandleRequest(
              VaadinSession session, VaadinRequest request, VaadinResponse response)
              throws IOException {
            if (!(request instanceof VaadinServletRequest servlet)) {
              return false;
            }
            Optional<Authentication> auth =
                security
                    .get()
                    .refreshSessionAuthentication(
                        servlet.getHttpServletRequest().getSession(false));
            UI ui = session.getService().findUI(request);
            if (ui == null
                || auth.isEmpty()
                || !RoleUiState.refresh(ui, auth.get(), servlet.getContextPath())) {
              return false;
            }
            // Framework-Writer statt selbstgebautem UIDL. Der alte Request wird nicht an RPC
            // weitergegeben.
            // Asynchron: der verworfene Client-Aufruf wird bewusst nicht als verarbeitet bestätigt.
            String payload = new UidlWriter().createUidl(ui, true).toString();
            ui.getInternals().setLastRequestResponse(payload);
            UidlRequestHandler.commitJsonResponse(response, payload);
            return true;
          }
        });
    event
        .getSource()
        .addUIInitListener(
            init -> {
              UI ui = init.getUI();
              ui.addBeforeEnterListener(navigation -> bind(ui));
              bind(ui);
            });
  }

  void bind(UI ui) {
    if (!(VaadinRequest.getCurrent() instanceof VaadinServletRequest request)) {
      return;
    }
    HttpSession http = request.getHttpServletRequest().getSession(false);
    Optional<Authentication> auth = security.get().refreshSessionAuthentication(http);
    if (auth.isEmpty()) {
      return;
    }
    RoleUiState.changed(ui, auth.get());
    String marker = RoleUiGuard.class.getName() + ".subscribed";
    if (com.vaadin.flow.component.ComponentUtil.getData(ui, marker) != null) {
      return;
    }
    UserEntity owner = security.get().getSessionUser(http).orElse(null);
    if (owner == null) {
      return;
    }
    com.vaadin.flow.component.ComponentUtil.setData(ui, marker, Boolean.TRUE);
    String contextPath = request.getContextPath();
    Registration registration =
        events
            .get()
            .register(
                id -> {
                  if (!Objects.equals(owner.getId(), id)) {
                    return;
                  }
                  ui.access(
                      () -> {
                        if (!ui.isAttached()) {
                          return;
                        }
                        security
                            .get()
                            .refreshSessionAuthentication(http)
                            .ifPresent(fresh -> RoleUiState.refresh(ui, fresh, contextPath));
                      });
                });
    ui.addDetachListener(detach -> registration.remove());
  }
}
