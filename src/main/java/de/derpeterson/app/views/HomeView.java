package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Locale;
import java.util.Optional;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@Route("home")
@PageTitle("Home")
@AnonymousAllowed
public class HomeView extends AppLayout {

    private final CustomI18NProvider i18nProvider;
    private final transient UserService userService;
    private final transient SecurityService securityService;

    private Optional<UserDetails> authenticatedUser;

    @Autowired
    public HomeView(CustomI18NProvider i18nProvider, SecurityService securityService, HttpServletRequest request, ConfigService configService, UserService userService) {
        this.i18nProvider = i18nProvider;
        this.securityService = securityService;
        this.userService = userService;

        this.authenticatedUser = securityService.getAuthenticatedUser(request);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();

        HorizontalLayout serviceLayout = new HorizontalLayout();
        serviceLayout.addClassNames(LumoUtility.Gap.SMALL);
        serviceLayout.setWidthFull();
        serviceLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        serviceLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        // Service
        Image serviceLogoIcon = new Image("themes/custom-theme/service_logo.png", "Service Icon");
        serviceLogoIcon.setWidth("48px");
        serviceLogoIcon.setHeight("48px");

        Span serviceNameSpan = new Span(configService.getString(ConfigEntry.SERVICE_NAME));
        serviceNameSpan.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FlexWrap.NOWRAP);

        serviceLayout.add(serviceLogoIcon, serviceNameSpan);

        HorizontalLayout userLayout = new HorizontalLayout();
        userLayout.setWidthFull();
        userLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button userIconButton = new Button(new OverlayUserIcon(i18nProvider, new Image("themes/custom-theme/user_icon.png", "User Icon"),
                securityService.getAuthenticatedUser(request).isPresent() ? UserStatus.AVAILABLE : UserStatus.OFFLINE, IconSize.PIXEL_48));

        Popover popover = createUserPopover(userIconButton);

        userIconButton.addClickListener(buttonClickEvent -> popover.setOpened(true));
        userIconButton.addClassNames(LumoUtility.TextColor.PRIMARY);
        userIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        userLayout.add(userIconButton);

        headerLayout.add(serviceLayout, userLayout);

        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setWidthFull();
        headerLayout.addClassNames(
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(headerLayout);
    }

    private Popover createUserPopover(Button userIconButton) {
        Popover userPopover = new Popover();
        userPopover.setTarget(userIconButton);
        userPopover.addThemeVariants(PopoverVariant.ARROW);
        userPopover.setPosition(PopoverPosition.BOTTOM);
        userPopover.setModal(true);
        userPopover.setCloseOnEsc(true);
        userPopover.setCloseOnOutsideClick(true);

        VerticalLayout popoverLayout = new VerticalLayout();
        popoverLayout.setSpacing(false);
        popoverLayout.setPadding(false);
        popoverLayout.addClassNames(LumoUtility.Padding.SMALL);
        popoverLayout.setMinWidth("300px");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button(VaadinIcon.CLOSE_CIRCLE.create(),
                clickEvent -> userPopover.close());
        closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        headerLayout.add(closeButton);

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setWidthFull();
        contentLayout.setSpacing(false);
        contentLayout.setPadding(false);
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        contentLayout.addClassNames(LumoUtility.Padding.Top.SMALL, LumoUtility.Padding.Bottom.SMALL, LumoUtility.Gap.SMALL);

        contentLayout.add(new OverlayUserIcon(i18nProvider, new Image("themes/custom-theme/user_icon.png", "User Icon"),
                authenticatedUser.isPresent() ? UserStatus.AVAILABLE : UserStatus.OFFLINE, IconSize.PIXEL_96));

        if (authenticatedUser.isPresent()) {
            Span onlineText = new Span(i18nProvider.getTranslation("base.online_text"));
            onlineText.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
            contentLayout.add(onlineText);

            Optional<UserEntity> user = userService.findByEmail(authenticatedUser.get().getUsername());
            if (user.isPresent()) {
                if (StringUtils.isNotEmpty(user.get().getFirstName()) || StringUtils.isNotEmpty(user.get().getLastName())) {
                    Span nameText = new Span(user.get().getFirstName() + " " + user.get().getLastName());
                    nameText.addClassNames(LumoUtility.FontWeight.BOLD);
                    contentLayout.add(nameText);
                }

                Span emailText = new Span(user.get().getEmail());
                emailText.addClassNames(LumoUtility.TextColor.SECONDARY);
                contentLayout.add(emailText);
            }
        } else {
            Span offlineText = new Span(i18nProvider.getTranslation("base.offline_text"));
            offlineText.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.SEMIBOLD);
            contentLayout.add(offlineText);
        }

        Button manageAccountButton = new Button(i18nProvider.getTranslation("base.manage_account_button"));
        manageAccountButton.setPrefixComponent(VaadinIcon.COG.create());
        manageAccountButton.setEnabled(authenticatedUser.isPresent());
        if (authenticatedUser.isPresent()) {
            manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        manageAccountButton.setWidthFull();

        contentLayout.add(manageAccountButton);

        Locale curentLocale = VaadinSession.getCurrent().getLocale();

        Image deFlagIcon = new Image("themes/custom-theme/flag_de.png", "German Flag Icon");
        deFlagIcon.setWidth("32px");
        deFlagIcon.setHeight("32px");
        if (!curentLocale.equals(Locale.GERMAN)) {
            deFlagIcon.getStyle().set("filter", "grayscale(100%)");
        } else {
            deFlagIcon.getStyle().remove("filter");
        }
        Button deFlagIconButton = new Button(deFlagIcon, buttonClickEvent -> {
            VaadinSession.getCurrent().setLocale(Locale.GERMAN);
            UI.getCurrent().getPage().reload();
        });
        deFlagIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        Image enFlagIcon = new Image("themes/custom-theme/flag_en.png", "Englisch Flag Icon");
        enFlagIcon.setWidth("32px");
        enFlagIcon.setHeight("32px");
        if (!curentLocale.equals(Locale.ENGLISH)) {
            enFlagIcon.getStyle().set("filter", "grayscale(100%)");
        } else {
            enFlagIcon.getStyle().remove("filter");
        }
        Button enFlagIconButton = new Button(enFlagIcon, buttonClickEvent -> {
            VaadinSession.getCurrent().setLocale(Locale.ENGLISH);
            UI.getCurrent().getPage().reload();
        });
        enFlagIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        HorizontalLayout flagLayout = new HorizontalLayout();
        flagLayout.add(deFlagIconButton, enFlagIconButton);

        contentLayout.add(flagLayout);

        if (authenticatedUser.isPresent()) {
            Button logoutButton = new Button(i18nProvider.getTranslation("base.logout_button"));
            logoutButton.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
            logoutButton.addClickListener(buttonClickEvent -> securityService.logout());
            logoutButton.setWidthFull();

            contentLayout.add(logoutButton);
        } else {
            Button registrationButton = new Button(i18nProvider.getTranslation("base.registration_button"));
            registrationButton.setPrefixComponent(VaadinIcon.EDIT.create());
            registrationButton.addClickListener(buttonClickEvent -> UI.getCurrent().navigate(RegistrationView.class));
            registrationButton.setWidthFull();

            Button loginButton = new Button(i18nProvider.getTranslation("base.login_button"));
            loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
            loginButton.addClickListener(buttonClickEvent -> UI.getCurrent().navigate(LoginView.class));
            loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            loginButton.setWidthFull();

            contentLayout.add(registrationButton, loginButton);
        }

        popoverLayout.add(headerLayout, contentLayout);

        userPopover.add(popoverLayout);

        return userPopover;
    }
}
