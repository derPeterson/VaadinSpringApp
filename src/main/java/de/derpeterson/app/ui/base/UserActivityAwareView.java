package de.derpeterson.app.ui.base;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;

import java.time.LocalDateTime;
import java.util.Optional;

public abstract class UserActivityAwareView extends AppLayout {

    protected final transient SecurityService securityService;
    protected final transient UserService userService;

    protected UserActivityAwareView(SecurityService securityService, UserService userService) {
        this.securityService = securityService;
        this.userService = userService;

        // Globaler Listener für Maus + Tastatur-Aktivität
        UI.getCurrent().getPage().executeJs("""
                    if (!window.__userActivityListenerAttached) {
                        let lastSent = 0;
                        const debounceMs = 5000;
                
                        const sendActivity = () => {
                            const now = Date.now();
                            if (now - lastSent > debounceMs) {
                                $0.$server.userActivityDetected();
                                lastSent = now;
                            }
                        };
                
                        document.body.addEventListener('mousemove', sendActivity);
                        document.body.addEventListener('keydown', sendActivity);
                
                        window.__userActivityListenerAttached = true;
                    }
                """, getElement());
    }

    @ClientCallable
    public void userActivityDetected() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Optional<UserEntity> currentUserOpt = securityService.getCurrentUser();
            currentUserOpt.ifPresent(user -> {
                user.setLastActivity(LocalDateTime.now());
                userService.save(user);
            });
        }
    }
}
