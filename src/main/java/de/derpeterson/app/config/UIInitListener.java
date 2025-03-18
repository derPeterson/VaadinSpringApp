package de.derpeterson.app.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UIInitListener implements VaadinServiceInitListener {

    private final SecurityService securityService;
    private final UserService userService;

    private final HttpServletRequest request;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();

            if (VaadinSession.getCurrent() != null && VaadinSession.getCurrent().getAttribute("locale-set") == null) {
                Optional<UserDetails> userOpt = securityService.getAuthenticatedUser(request);
                userOpt.ifPresent(user -> {
                    Optional<UserEntity> userEntity = userService.findByEmail(user.getUsername());
                    if (userEntity.isPresent()) {
                        Locale userLocale = userEntity.get().getPreferredLocale();
                        VaadinSession.getCurrent().setLocale(userLocale);
                        VaadinSession.getCurrent().setAttribute("locale-set", true);
                    }

                });
            }

            ui.addBeforeEnterListener(e -> NotificationHelper.getInstance().closeAndClearAllNotifications());
        });
    }
}
