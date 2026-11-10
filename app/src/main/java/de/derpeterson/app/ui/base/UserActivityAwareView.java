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

  protected UserActivityAwareView(
      SecurityService securityService,
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

  protected void onViewAttached(AttachEvent attachEvent) {}

  protected void onViewDetached(DetachEvent detachEvent) {}

  private void registerUserActivityListener() {
    getElement()
        .executeJs(
            """
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
            """,
            ACTIVITY_DEBOUNCE_MS);
  }

  private void unregisterUserActivityListener() {
    getElement()
        .executeJs(
            """
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
    userStatusBroadcasterRegistration =
        userStatusBroadcaster.register(userStatusMessage -> {
          Optional<UserEntity> trackedUserOptional = getTrackedCurrentUser();
          if (trackedUserOptional.isEmpty()) {
            return;
          }

          UserEntity currentUser = trackedUserOptional.get();
          if (!userStatusMessage.userId().equals(currentUser.getId())) {
            return;
          }

          getUI()
              .ifPresent(activeUi ->
                  activeUi.access(() -> {
                    UserStatus oldStatus = UserStatus.valueOf(userStatusMessage.oldStatus());
                    UserStatus newStatus = UserStatus.valueOf(userStatusMessage.newStatus());

                    if (!oldStatus.equals(newStatus)) {
                      currentUser.setStatus(newStatus);
                      onTrackedUserStatusChanged(currentUser, oldStatus, newStatus);
                    }
                  }));
        });
  }

  protected abstract Optional<UserEntity> getTrackedCurrentUser();

  protected abstract void onTrackedUserStatusChanged(
      UserEntity trackedUserEntity, UserStatus oldStatus, UserStatus newStatus);

  @ClientCallable
  public void userActivityDetected() {
    VaadinSession vaadinSession = VaadinSession.getCurrent();
    if (vaadinSession == null) {
      return;
    }

    Optional<UserEntity> trackedUserOptional = securityService.getCurrentUser();
    trackedUserOptional.ifPresent(trackedUserEntity -> {
      userService.updateLastActivity(trackedUserEntity.getId());
    });
  }
}
