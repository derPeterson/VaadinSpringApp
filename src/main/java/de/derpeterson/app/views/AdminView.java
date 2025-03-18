package de.derpeterson.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

@Route("admin")
@PageTitle("Admin")
@RolesAllowed("ADMIN")
public class AdminView extends IsAuthentificatedBaseView<VerticalLayout> {

    public AdminView(MessageProperties messageProperties, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new VerticalLayout());

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        UserDetails user = securityService.getAuthenticatedUser(request).orElse(null);

        if (user != null) {
            add(new H1("Willkommen, " + user.getUsername() + "!"));
            add(new H1("Deine Rollen: " + user.getAuthorities()));
        } else {
            add(new H1("Nicht eingeloggt!"));
            Notification.show("Fehler: Benutzer ist nicht authentifiziert.");
        }

        // Logout-Button
        Button logoutButton = new Button(messageProperties.getBaseLogoutButton(), event -> securityService.logout());

        add(logoutButton);
    }
}
