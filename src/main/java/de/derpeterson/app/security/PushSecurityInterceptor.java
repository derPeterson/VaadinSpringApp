package de.derpeterson.app.security;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushSecurityInterceptor implements VaadinServiceInitListener, VaadinRequestInterceptor {

    private final SecurityService securityService;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addVaadinRequestInterceptor(this);
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (request instanceof VaadinServletRequest servletRequest
                && !securityService.hasCurrentSessionPrivileges(servletRequest.getHttpServletRequest())) {
            // requestStart is also called by PushHandler for an existing socket,
            // before resolving the UI or executing any ServerRpcHandler callback.
            // PushHandler calls requestStart outside its requestEnd finally.
            CurrentInstance.clearAll();
            throw new AccessDeniedException("Session privileges have changed");
        }
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response, VaadinSession session, Exception exception) {
        // The framework owns transport error handling; never resume RPC dispatch.
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
    }
}
