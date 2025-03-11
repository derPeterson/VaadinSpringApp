package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Locale;

@Route("home")
@PageTitle("Home")
@AnonymousAllowed
public class HomeView extends AppLayout {

    @Autowired
    public HomeView(CustomI18NProvider i18nProvider, SecurityService securityService, HttpServletRequest request, ConfigService configService) {
        // Language
        ComboBox<Locale> languageSelector = new ComboBox<>();
        languageSelector.setItems(Locale.ENGLISH, Locale.GERMAN);
        languageSelector.setItemLabelGenerator(locale -> locale.getDisplayLanguage(locale));
        languageSelector.setValue(VaadinSession.getCurrent().getLocale());

        Button switchLanguage = new Button(i18nProvider.getTranslation("base.apply_button"), event -> {
            Locale selectedLocale = languageSelector.getValue();
            VaadinSession.getCurrent().setLocale(selectedLocale);
            UI.getCurrent().getPage().reload();
        });

        HorizontalLayout switchLanguageLayout = new HorizontalLayout(switchLanguage);
        switchLanguageLayout.add(languageSelector);
        switchLanguageLayout.add(switchLanguage);
        switchLanguageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // Service
        Image serviceLogoIcon = new Image("themes/custom-theme/service_logo.png", "Custom Icon");
        serviceLogoIcon.setWidth("48px"); // Icon-Größe setzen
        serviceLogoIcon.setHeight("48px");

        Span serviceNameSpan = new Span(configService.getString(ConfigEntry.SERVICE_NAME));
        serviceNameSpan.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FlexWrap.NOWRAP);

        HorizontalLayout serviceLayout = new HorizontalLayout(serviceLogoIcon, serviceNameSpan);
        serviceLayout.addClassNames(LumoUtility.Gap.SMALL);
        serviceLayout.setAlignItems(FlexComponent.Alignment.CENTER);


        HorizontalLayout header;
        if (securityService.getAuthenticatedUser(request).isPresent()) {
            Button logout = new Button(i18nProvider.getTranslation("base.logout_button"), click ->
                    securityService.logout());
            HorizontalLayout logoutLayout = new HorizontalLayout(logout);
            logoutLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            logoutLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            UserDetails userDetails = securityService.getAuthenticatedUser(request).orElse(null);
            if (userDetails != null) {
                logoutLayout.add(new Span(userDetails.getUsername()));
                logoutLayout.add(new Span(String.valueOf(userDetails.getAuthorities())));
            }
            logoutLayout.add(logout);
            header = new HorizontalLayout(serviceLayout, switchLanguageLayout, logoutLayout);
        } else {
            Button login = new Button(i18nProvider.getTranslation("base.login_button"), event -> UI.getCurrent().navigate("login"));
            header = new HorizontalLayout(serviceLayout, switchLanguageLayout, login);
        }

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(switchLanguageLayout);
        header.addClassNames(
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }
}
