package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Route("admin")
@PageTitle("Admin")
@RolesAllowed("ADMIN")
public class AdminView extends IsAuthentificatedBaseView<VerticalLayout> {

    private final MessageProperties messageProperties;

    private Button logoutButton = null;
    private Button homeButton = null;

    public AdminView(MessageProperties messageProperties, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new VerticalLayout());

        this.messageProperties = messageProperties;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(logoutButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseLogoutButton));
            Optional.ofNullable(homeButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseHomeButton));

            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
            NotificationHelper.getInstance().updateText();
        });

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

        // Home-Button
        this.homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        add(homeButton);

        // Logout-Button
        this.logoutButton = new Button(messageProperties.getBaseLogoutButton(), event -> securityService.logout());
        add(logoutButton);
    }
}
