package de.derpeterson.app.security;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushSecurityInterceptor
    implements VaadinServiceInitListener, VaadinRequestInterceptor {

  private final SecurityService securityService;
  private final MaintenanceService maintenance;

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.addVaadinRequestInterceptor(this);
  }

  @Override
  public void requestStart(VaadinRequest request, VaadinResponse response) {
    if (response == null) {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    if (request instanceof VaadinServletRequest servletRequest
        && !securityService.hasCurrentSessionPrivileges(servletRequest.getHttpServletRequest())) {
      // requestStart is also called by PushHandler for an existing socket,
      // before resolving the UI or executing any ServerRpcHandler callback.
      // PushHandler calls requestStart outside its requestEnd finally.
      CurrentInstance.clearAll();
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
      throw new AccessDeniedException("Session privileges have changed");
    }
    if (response == null && request instanceof VaadinServletRequest servletRequest) {
      HttpServletRequest http = servletRequest.getHttpServletRequest();
      Optional<Authentication> auth =
          securityService.refreshSessionAuthentication(http.getSession(false));
      if (auth.isPresent()
          && MaintenanceTransport.withUi(
              http,
              ui -> {
                if (!RoleUiState.changed(ui, auth.get())) {
                  return false;
                }
                ui.access(
                    () -> {
                      RoleUiState.refresh(ui, auth.get(), http.getContextPath());
                      if (ui.getPushConfiguration().getPushMode().isEnabled()) {
                        ui.push();
                      }
                    });
                return true;
              },
              false)) {
        CurrentInstance.clearAll();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        throw new AccessDeniedException("Old role UI requires refresh");
      }
    }
    // HTTP is handled by MaintenanceHttpFilter. Only existing WebSocket messages
    // enter here without a response; blocking document/bootstrap requests would loop login.
    if (response == null
        && request instanceof VaadinServletRequest servletRequest
        && maintenance.blocks(servletRequest.getHttpServletRequest())
        && !MaintenanceTransport.allowedUi(servletRequest.getHttpServletRequest())) {
      MaintenanceTransport.redirectUi(servletRequest.getHttpServletRequest());
      CurrentInstance.clearAll();
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
      throw new MaintenanceAccessException("UI actions paused for maintenance");
    }
  }

  @Override
  public void handleException(
      VaadinRequest request, VaadinResponse response, VaadinSession session, Exception exception) {
    // The framework owns transport error handling; never resume RPC dispatch.
  }

  @Override
  public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
    if (response == null) {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }
}
