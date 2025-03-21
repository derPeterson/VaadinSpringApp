package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentPrefixHelper;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.UserActivityAwareView;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

@Route("home")
@UIScope
@PageTitle("Home")
@AnonymousAllowed
public class HomeView extends UserActivityAwareView {

    private final transient MessageProperties messageProperties;
    private final transient UserStatusBroadcaster userStatusBroadcaster;

    private Registration userStatusBroadcasterRegistration;

    private transient Optional<UserEntity> currentUser = Optional.empty();

    // Text
    private Span onlineText = null;
    private Span offlineText = null;
    private Button manageAccountButton = null;
    private Button logoutButton = null;
    private Button registrationButton = null;
    private Button loginButton = null;
    private Image deFlagIcon = null;
    private Image enFlagIcon = null;
    private Select<UserStatus> userStatusSelect = null;

    // UserStatus
    private Button userIconButton = null;
    private OverlayUserIcon overlayIcon = null;


    private boolean suppressUpdate = false;

    @PostConstruct
    private void setupBroadcastListener() {
        // WebSocket-Updates abonnieren
        userStatusBroadcasterRegistration = userStatusBroadcaster.register(message -> {
            if (currentUser.isPresent() && message.userId().equals(currentUser.get().getId())) {
                getUI().ifPresent(ui -> ui.access(() -> {
                    this.suppressUpdate = true;

                    UserStatus oldStatus = UserStatus.valueOf(message.oldStatus());
                    UserStatus newStatus = UserStatus.valueOf(message.newStatus());

                    if (!oldStatus.equals(newStatus)) {
                        if (userIconButton != null) {
                            userIconButton.setIcon(new OverlayUserIcon(
                                    messageProperties,
                                    new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                                    newStatus,
                                    IconSize.PIXEL_48
                            ));
                        }

                        if (overlayIcon != null) {
                            overlayIcon.removeAll();
                            overlayIcon.add(new OverlayUserIcon(
                                    messageProperties,
                                    new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                                    newStatus,
                                    IconSize.PIXEL_96
                            ));
                        }

                        Optional.ofNullable(this.userStatusSelect).ifPresent(select -> {
                            select.setItemLabelGenerator(status -> getTranslation(status.getTextKey()));
                            select.setItems(UserStatus.values());
                            select.getDataProvider().refreshAll();
                            select.setValue(newStatus);
                        });
                    }

                    this.suppressUpdate = false;
                }));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // UI aus Broadcaster entfernen, um Speicherlecks zu vermeiden
        userStatusBroadcasterRegistration.remove();
    }

    @Autowired
    public HomeView(MessageProperties messageProperties, SecurityService securityService, HttpServletRequest request, ConfigService configService, UserService userService, UserStatusBroadcaster userStatusBroadcaster) {
        super(securityService, userService);

        this.messageProperties = messageProperties;
        this.userStatusBroadcaster = userStatusBroadcaster;

        this.currentUser = securityService.getCurrentUser(request);

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(onlineText)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseOnlineText));
            Optional.ofNullable(offlineText)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseOfflineText));
            Optional.ofNullable(manageAccountButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseManageAccountButton));
            Optional.ofNullable(logoutButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseLogoutButton));
            Optional.ofNullable(registrationButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseRegistrationButton));
            Optional.ofNullable(loginButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseLoginButton));

            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);

            NotificationHelper.getInstance().updateText();

            Optional.ofNullable(this.userStatusSelect).ifPresent(select -> {
                select.setItemLabelGenerator(status -> getTranslation(status.getTextKey()));
                select.setItems(UserStatus.values());
                select.getDataProvider().refreshAll();
                select.setValue(currentUser.isPresent() ? currentUser.get().getStatus() : UserStatus.getDefaultStatus());
            });

            var currentLocale = VaadinSession.getCurrent().getLocale();

            if (!currentLocale.equals(Locale.GERMAN)) {
                deFlagIcon.getStyle().set("filter", "grayscale(100%)");
            } else {
                deFlagIcon.getStyle().remove("filter");
            }
            if (!currentLocale.equals(Locale.ENGLISH)) {
                enFlagIcon.getStyle().set("filter", "grayscale(100%)");
            } else {
                enFlagIcon.getStyle().remove("filter");
            }
        });

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

        this.userIconButton = new Button(new OverlayUserIcon(messageProperties, new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                currentUser.isPresent() ? currentUser.get().getStatus() : UserStatus.getDefaultStatus(), IconSize.PIXEL_48));

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

        this.overlayIcon = new OverlayUserIcon(messageProperties, new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                currentUser.isPresent() ? currentUser.get().getStatus() : UserStatus.getDefaultStatus(), IconSize.PIXEL_96);

        contentLayout.add(overlayIcon);

        if (currentUser.isPresent()) {
            this.onlineText = new Span(messageProperties.getBaseOnlineText());
            onlineText.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
            contentLayout.add(onlineText);

            if (StringUtils.isNotEmpty(currentUser.get().getFirstName()) || StringUtils.isNotEmpty(currentUser.get().getLastName())) {
                Span nameText = new Span(currentUser.get().getFirstName() + " " + currentUser.get().getLastName());
                nameText.addClassNames(LumoUtility.FontWeight.BOLD);
                contentLayout.add(nameText);
            }

            Span emailText = new Span(currentUser.get().getEmail());
            emailText.addClassNames(LumoUtility.TextColor.SECONDARY);
            contentLayout.add(emailText);
        } else {
            this.offlineText = new Span(messageProperties.getBaseOfflineText());
            offlineText.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.SEMIBOLD);
            contentLayout.add(offlineText);
        }

        this.manageAccountButton = new Button(messageProperties.getBaseManageAccountButton());
        manageAccountButton.setPrefixComponent(VaadinIcon.COG.create());
        manageAccountButton.setEnabled(currentUser.isPresent());
        if (currentUser.isPresent()) {
            manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        manageAccountButton.setWidthFull();

        contentLayout.add(manageAccountButton);

        this.userStatusSelect = new Select<>();
        userStatusSelect.setEnabled(currentUser.isPresent());
        userStatusSelect.setItems(UserStatus.values());
        userStatusSelect.setItemLabelGenerator(UserStatus::getTextKey);
        userStatusSelect.setItemLabelGenerator(userStatus -> messageProperties.getTranslation(userStatus.getTextKey()));
        userStatusSelect.addValueChangeListener(event -> {
            UserStatus userStatus = event.getValue();
            if (userStatus == null) {
                ComponentPrefixHelper.clearSlot(userStatusSelect, "prefix");
                return;
            } else {
                if (userStatus.getComponent(IconSize.PIXEL_16) instanceof Icon statusIcon) {
                    HorizontalLayout iconLayout = new HorizontalLayout();
                    iconLayout.addClassNames(LumoUtility.Padding.Left.SMALL, LumoUtility.Padding.Right.SMALL);

                    iconLayout.add(statusIcon);

                    ComponentPrefixHelper.setPrefixComponent(userStatusSelect, iconLayout);
                }

                if (userStatus.getComponent(IconSize.PIXEL_16) instanceof Div statusDiv) {
                    HorizontalLayout divLayout = new HorizontalLayout();
                    divLayout.addClassNames(LumoUtility.Padding.Left.SMALL, LumoUtility.Padding.Right.SMALL);

                    divLayout.add(statusDiv);

                    ComponentPrefixHelper.setPrefixComponent(userStatusSelect, divLayout);
                }

                if (!suppressUpdate) {
                    currentUser.ifPresent(entity -> userService.updateUserStatus(entity, event.getValue(), true));
                }
            }
        });
        userStatusSelect.setRenderer(new ComponentRenderer<>(userStatus -> {
            HorizontalLayout contentComboBox = new HorizontalLayout();
            contentComboBox.setWidthFull();

            if (userStatus.getComponent() instanceof Icon statusIcon) {
                VerticalLayout iconLayout = new VerticalLayout();
                iconLayout.setPadding(false);
                iconLayout.setSpacing(false);
                iconLayout.setWidth(null);
                iconLayout.setSizeUndefined();
                iconLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                iconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

                iconLayout.add(statusIcon);

                contentComboBox.add(iconLayout);
            }

            if (userStatus.getComponent() instanceof Div statusDiv) {
                VerticalLayout divLayout = new VerticalLayout();
                divLayout.setPadding(false);
                divLayout.setSpacing(false);
                divLayout.setWidth(null);
                divLayout.setSizeUndefined();
                divLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                divLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

                divLayout.add(statusDiv);

                contentComboBox.add(divLayout);
            }

            VerticalLayout textLayout = new VerticalLayout();
            textLayout.setPadding(false);
            textLayout.setSpacing(false);
            textLayout.setWidthFull();

            Span userStatusText = new Span(messageProperties.getTranslation(userStatus.getTextKey()));

            Span userStatusSecondaryText = new Span(messageProperties.getTranslation(userStatus.getTextDescriptionKey()));
            userStatusSecondaryText.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);

            textLayout.add(userStatusText);
            textLayout.add(new Hr());
            textLayout.add(userStatusSecondaryText);

            contentComboBox.add(textLayout);

            return contentComboBox;
        }));
        userStatusSelect.setValue(currentUser.isPresent() ? currentUser.get().getStatus() : UserStatus.getDefaultStatus());
        userStatusSelect.setWidthFull();

        contentLayout.add(userStatusSelect);

        Locale currentLocale = VaadinSession.getCurrent().getLocale();

        this.deFlagIcon = new Image("themes/custom-theme/flag_de.png", "German Flag Icon");
        deFlagIcon.setWidth("32px");
        deFlagIcon.setHeight("32px");
        if (!currentLocale.equals(Locale.GERMAN)) {
            deFlagIcon.getStyle().set("filter", "grayscale(100%)");
        } else {
            deFlagIcon.getStyle().remove("filter");
        }
        Button deFlagIconButton = new Button(deFlagIcon, buttonClickEvent -> {
            currentUser.ifPresent(user -> userService.updateUserLocale(user.getEmail(), Locale.GERMAN));
            VaadinSession.getCurrent().setLocale(Locale.GERMAN);
            LanguageChangeEvent.fire(UI.getCurrent(), Locale.GERMAN);
        });
        deFlagIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        this.enFlagIcon = new Image("themes/custom-theme/flag_en.png", "Englisch Flag Icon");
        enFlagIcon.setWidth("32px");
        enFlagIcon.setHeight("32px");
        if (!currentLocale.equals(Locale.ENGLISH)) {
            enFlagIcon.getStyle().set("filter", "grayscale(100%)");
        } else {
            enFlagIcon.getStyle().remove("filter");
        }
        Button enFlagIconButton = new Button(enFlagIcon, buttonClickEvent -> {
            currentUser.ifPresent(user -> userService.updateUserLocale(user.getEmail(), Locale.ENGLISH));
            VaadinSession.getCurrent().setLocale(Locale.ENGLISH);
            LanguageChangeEvent.fire(UI.getCurrent(), Locale.ENGLISH);
        });
        enFlagIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        HorizontalLayout flagLayout = new HorizontalLayout();
        flagLayout.add(deFlagIconButton, enFlagIconButton);

        contentLayout.add(flagLayout);

        if (currentUser.isPresent()) {
            this.logoutButton = new Button(messageProperties.getBaseLogoutButton());
            logoutButton.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
            logoutButton.addClickListener(buttonClickEvent -> securityService.logout());
            logoutButton.setWidthFull();

            contentLayout.add(logoutButton);
        } else {
            this.registrationButton = new Button(messageProperties.getBaseRegistrationButton());
            registrationButton.setPrefixComponent(VaadinIcon.EDIT.create());
            registrationButton.addClickListener(buttonClickEvent -> UI.getCurrent().navigate(RegistrationView.class));
            registrationButton.setWidthFull();

            this.loginButton = new Button(messageProperties.getBaseLoginButton());
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
