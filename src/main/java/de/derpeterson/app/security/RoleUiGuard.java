package de.derpeterson.app.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import com.vaadin.flow.server.communication.*;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Aktualisiert alte Ansichten mit erhaltenem HTTP-Login und stoppt vorher ihre alten RPCs. */
@Component
@RequiredArgsConstructor
public class RoleUiGuard implements VaadinServiceInitListener {
  private final SecurityService security;
  private final UserRoleBroadcaster events;

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
            if (!(request instanceof VaadinServletRequest servlet)) return false;
            var auth =
                security.refreshSessionAuthentication(
                    servlet.getHttpServletRequest().getSession(false));
            var ui = session.getService().findUI(request);
            if (ui == null
                || auth.isEmpty()
                || !RoleUiState.refresh(ui, auth.get(), servlet.getContextPath())) return false;
            // Framework-Writer statt selbstgebautem UIDL. Der alte Request wird nicht an RPC
            // weitergegeben.
            // Asynchron: der verworfene Client-Aufruf wird bewusst nicht als verarbeitet bestätigt.
            var payload = new UidlWriter().createUidl(ui, true).toString();
            ui.getInternals().setLastRequestResponse(payload);
            UidlRequestHandler.commitJsonResponse(response, payload);
            return true;
          }
        });
    event
        .getSource()
        .addUIInitListener(
            init -> {
              var ui = init.getUI();
              ui.addBeforeEnterListener(navigation -> bind(ui));
              bind(ui);
            });
  }

  void bind(UI ui) {
    if (!(VaadinRequest.getCurrent() instanceof VaadinServletRequest request)) return;
    var http = request.getHttpServletRequest().getSession(false);
    var auth = security.refreshSessionAuthentication(http);
    if (auth.isEmpty()) return;
    RoleUiState.changed(ui, auth.get());
    String marker = RoleUiGuard.class.getName() + ".subscribed";
    if (com.vaadin.flow.component.ComponentUtil.getData(ui, marker) != null) return;
    var owner = security.getSessionUser(http).orElse(null);
    if (owner == null) return;
    com.vaadin.flow.component.ComponentUtil.setData(ui, marker, Boolean.TRUE);
    var contextPath = request.getContextPath();
    var registration =
        events.register(
            id -> {
              if (!Objects.equals(owner.getId(), id)) return;
              ui.access(
                  () -> {
                    if (!ui.isAttached()) return;
                    security
                        .refreshSessionAuthentication(http)
                        .ifPresent(fresh -> RoleUiState.refresh(ui, fresh, contextPath));
                  });
            });
    ui.addDetachListener(detach -> registration.remove());
  }
}
