package de.derpeterson.app.ui.base;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.websocket.UserStatusBroadcaster;

import java.util.Optional;

public abstract class UserActivityAwareView extends AppLayout {

    private static final int ACTIVITY_DEBOUNCE_MS = 5000;

    protected final transient SecurityService securityService;
    protected final transient UserService userService;
    protected final transient UserStatusBroadcaster userStatusBroadcaster;

    private Registration userStatusBroadcasterRegistration;

    protected UserActivityAwareView(SecurityService securityService,
                                    UserService userService,
                                    UserStatusBroadcaster userStatusBroadcaster) {
        this.securityService = securityService;
        this.userService = userService;
        this.userStatusBroadcaster = userStatusBroadcaster;
    }

    @Override
    protected final void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registerUserActivityListener();
        registerUserStatusBroadcaster();

        onViewAttached(attachEvent);
    }

    @Override
    protected final void onDetach(DetachEvent detachEvent) {
        unregisterUserActivityListener();

        if (userStatusBroadcasterRegistration != null) {
            userStatusBroadcasterRegistration.remove();
            userStatusBroadcasterRegistration = null;
        }

        onViewDetached(detachEvent);
        super.onDetach(detachEvent);
    }

    protected void onViewAttached(AttachEvent attachEvent) {
    }

    protected void onViewDetached(DetachEvent detachEvent) {
    }

    private void registerUserActivityListener() {
        getElement().executeJs("""
                    const element = this;
                
                    if (element.__userActivityHandler) {
                        return;
                    }
                
                    let lastSent = 0;
                    const debounceMs = $0;
                
                    const handler = () => {
                        const now = Date.now();
                        if (now - lastSent < debounceMs) {
                            return;
                        }
                
                        lastSent = now;
                
                        if (element.isConnected) {
                            element.$server.userActivityDetected();
                        }
                    };
                
                    element.__userActivityHandler = handler;
                
                    document.addEventListener('click', handler, true);
                    document.addEventListener('keydown', handler, true);
                    document.addEventListener('mousemove', handler, true);
                    document.addEventListener('touchstart', handler, true);
                    document.addEventListener('scroll', handler, true);
                """, ACTIVITY_DEBOUNCE_MS);
    }

    private void unregisterUserActivityListener() {
        getElement().executeJs("""
                    const element = this;
                    const handler = element.__userActivityHandler;
                
                    if (!handler) {
                        return;
                    }
                
                    document.removeEventListener('click', handler, true);
                    document.removeEventListener('keydown', handler, true);
                    document.removeEventListener('mousemove', handler, true);
                    document.removeEventListener('touchstart', handler, true);
                    document.removeEventListener('scroll', handler, true);
                
                    delete element.__userActivityHandler;
                """);
    }

    private void registerUserStatusBroadcaster() {
        userStatusBroadcasterRegistration = userStatusBroadcaster.register(message -> {
            Optional<UserEntity> currentUserOpt = getTrackedCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return;
            }

            UserEntity currentUser = currentUserOpt.get();
            if (!message.userId().equals(currentUser.getId())) {
                return;
            }

            getUI().ifPresent(ui -> ui.access(() -> {
                UserStatus oldStatus = UserStatus.valueOf(message.oldStatus());
                UserStatus newStatus = UserStatus.valueOf(message.newStatus());

                if (!oldStatus.equals(newStatus)) {
                    currentUser.setStatus(newStatus);
                    onTrackedUserStatusChanged(currentUser, oldStatus, newStatus);
                }
            }));
        });
    }

    protected abstract Optional<UserEntity> getTrackedCurrentUser();

    protected abstract void onTrackedUserStatusChanged(UserEntity user,
                                                       UserStatus oldStatus,
                                                       UserStatus newStatus);

    @ClientCallable
    public void userActivityDetected() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }

        Optional<UserEntity> currentUserOpt = securityService.getCurrentUser();
        currentUserOpt.ifPresent(user -> {
            user.updateLastActivity();
            userService.save(user);
        });
    }
}