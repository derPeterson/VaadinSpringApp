package de.derpeterson.app.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserActivityTracker implements VaadinServiceInitListener {

  private final transient SecurityService securityService;
  private final transient UserService userService;

  @Override
  public void serviceInit(ServiceInitEvent serviceInitEvent) {
    serviceInitEvent
        .getSource()
        .addUIInitListener(uiInitEvent -> {
          uiInitEvent.getUI().addBeforeEnterListener(beforeEnterEvent -> updateLastActivity());
          uiInitEvent.getUI().addBeforeLeaveListener(beforeLeaveEvent -> updateLastActivity());
        });
  }

  private void updateLastActivity() {
    VaadinSession vaadinSession = VaadinSession.getCurrent();

    if (vaadinSession != null) {
      Optional<UserEntity> currentUserOptional = securityService.getCurrentUser();
      currentUserOptional.ifPresent(currentUserEntity -> {
        userService.updateLastActivity(currentUserEntity.getId());
      });
    }
  }
}
