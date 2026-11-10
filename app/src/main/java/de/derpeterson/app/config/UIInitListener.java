package de.derpeterson.app.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.PublicLanguagePreference;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UIInitListener implements VaadinServiceInitListener {

  private final transient SecurityService securityService;
  private final transient UserService userService;

  private final transient HttpServletRequest httpRequest;

  @Override
  public void serviceInit(ServiceInitEvent serviceInitEvent) {
    serviceInitEvent
        .getSource()
        .addSessionInitListener(sessionInitEvent ->
            PublicLanguagePreference.restore(
                sessionInitEvent.getRequest(), sessionInitEvent.getSession()));

    serviceInitEvent
        .getSource()
        .addUIInitListener(uiInitEvent -> {
          UI initializedUi = uiInitEvent.getUI();

          if (VaadinSession.getCurrent() != null
              && VaadinSession.getCurrent().getAttribute("locale-set") == null) {
            Optional<UserDetails> authenticatedUserDetailsOptional =
                securityService.getAuthenticatedUser(httpRequest);
            authenticatedUserDetailsOptional.ifPresent(authenticatedUserDetails -> {
              Optional<UserEntity> accountUserOptional =
                  userService.findByEmail(authenticatedUserDetails.getUsername());
              if (accountUserOptional.isPresent()) {
                Locale preferredUserLocale = accountUserOptional.get().getPreferredLocale();
                VaadinSession.getCurrent().setLocale(preferredUserLocale);
                VaadinSession.getCurrent().setAttribute("locale-set", true);
              }
            });
          }

          initializedUi.addBeforeEnterListener(
              beforeEnterEvent -> NotificationHelper.getInstance().closeAndClearAllNotifications());
        });
  }
}
