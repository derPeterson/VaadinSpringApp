package de.derpeterson.app.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class UserActivityTracker implements VaadinServiceInitListener {

    private final transient SecurityService securityService;
    private final transient UserService userService;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {

            uiEvent.getUI().addBeforeEnterListener(beforeEnterEvent -> updateLastActivity());
            uiEvent.getUI().addBeforeLeaveListener(beforeLeaveEvent -> updateLastActivity());
        });
    }

    private void updateLastActivity() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Optional<UserEntity> currentUserOpt = securityService.getCurrentUser();
            currentUserOpt.ifPresent(user -> {
                userService.updateLastActivity(user.getId());
            });
        }
    }
}
