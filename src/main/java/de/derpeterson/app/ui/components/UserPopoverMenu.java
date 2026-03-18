package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentPrefixHelper;
import de.derpeterson.app.helper.ui.StyleHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import io.micrometer.common.util.StringUtils;

import java.util.Locale;
import java.util.function.Consumer;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

public class UserPopoverMenu {

    public record Actions(
            Runnable manageApplicationAction,
            Runnable homeAction,
            Runnable registrationAction,
            Runnable loginAction
    ) {
    }

    private final MessageProperties messageProperties;
    private final SecurityService securityService;
    private final UserService userService;
    private final UserEntity currentUser;
    private final Actions actions;
    private final Consumer<UserStatus> statusIconConsumer;
    private final Popover userPopover;

    private Span onlineText = null;
    private Span offlineText = null;
    private Button manageAccountButton = null;
    private Button manageApplicationButton = null;
    private Button logoutButton = null;
    private Button registrationButton = null;
    private Button loginButton = null;
    private Button homeButton = null;
    private Image deFlagIcon = null;
    private Image enFlagIcon = null;
    private OverlayUserIcon overlayIcon = null;
    private Select<UserStatus> userStatusSelect = null;

    private UserStatus currentStatus;

    public UserPopoverMenu(MessageProperties messageProperties,
                           SecurityService securityService,
                           UserService userService,
                           UserEntity currentUser,
                           Button targetButton,
                           Actions actions,
                           Consumer<UserStatus> statusIconConsumer) {
        this.messageProperties = messageProperties;
        this.securityService = securityService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.actions = actions;
        this.statusIconConsumer = statusIconConsumer;
        this.currentStatus = currentUser != null
                ? currentUser.getStatus()
                : UserStatus.getDefaultStatus();
        this.userPopover = createUserPopover(targetButton);
    }

    public Popover getPopover() {
        return userPopover;
    }

    public void refreshForLanguageChange() {
        refreshTexts();
        refreshUserStatusSelect();
        refreshLanguageFlags();
    }

    public void applyExternalStatus(UserStatus newStatus) {
        if (newStatus == null) {
            return;
        }

        currentStatus = newStatus;

        if (hasCurrentUser()) {
            currentUser.setStatus(newStatus);
        }

        updateStatusIcons(newStatus);

        if (userStatusSelect != null) {
            userStatusSelect.setValue(newStatus);
            updateStatusSelectPrefix(newStatus);
        }
    }

    private Popover createUserPopover(Button targetButton) {
        Popover popover = createConfiguredPopover(targetButton);

        VerticalLayout popoverLayout = createPopoverLayout();
        HorizontalLayout headerLayout = createHeaderLayout(popover);
        VerticalLayout contentLayout = createContentLayout();

        addUserIcon(contentLayout);
        addUserInfoSection(contentLayout);
        addManageAccountButton(contentLayout);
        addManageApplicationButton(contentLayout);
        addUserStatusSelect(contentLayout);
        addLanguageButtons(contentLayout);
        addHomeButton(contentLayout);
        addAuthButtons(contentLayout);

        popoverLayout.add(headerLayout, contentLayout);
        popover.add(popoverLayout);

        return popover;
    }

    private Popover createConfiguredPopover(Button targetButton) {
        Popover popover = new Popover();
        popover.setTarget(targetButton);
        popover.addThemeVariants(PopoverVariant.ARROW);
        popover.setPosition(PopoverPosition.BOTTOM);
        popover.setModal(true);
        popover.setCloseOnEsc(true);
        popover.setCloseOnOutsideClick(true);
        return popover;
    }

    private VerticalLayout createPopoverLayout() {
        VerticalLayout popoverLayout = new VerticalLayout();
        popoverLayout.setSpacing(false);
        popoverLayout.setPadding(false);
        popoverLayout.addClassNames(LumoUtility.Padding.SMALL);
        popoverLayout.setMinWidth("300px");
        return popoverLayout;
    }

    private HorizontalLayout createHeaderLayout(Popover popover) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button(VaadinIcon.CLOSE_CIRCLE.create(), clickEvent -> popover.close());
        closeButton.addClassNames(LumoUtility.TextColor.PRIMARY);
        closeButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        headerLayout.add(closeButton);
        return headerLayout;
    }

    private VerticalLayout createContentLayout() {
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setWidthFull();
        contentLayout.setSpacing(false);
        contentLayout.setPadding(false);
        contentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        contentLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        contentLayout.addClassNames(
                LumoUtility.Padding.Top.SMALL,
                LumoUtility.Padding.Bottom.SMALL,
                LumoUtility.Gap.SMALL
        );
        return contentLayout;
    }

    private void addUserIcon(VerticalLayout contentLayout) {
        overlayIcon = new OverlayUserIcon(
                messageProperties,
                new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                currentStatus,
                IconSize.PIXEL_96
        );
        contentLayout.add(overlayIcon);
    }

    private void addUserInfoSection(VerticalLayout contentLayout) {
        if (hasCurrentUser()) {
            addOnlineUserInfo(contentLayout);
            return;
        }

        addOfflineUserInfo(contentLayout);
    }

    private void addOnlineUserInfo(VerticalLayout contentLayout) {
        onlineText = new Span(messageProperties.getBaseOnlineText());
        onlineText.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
        contentLayout.add(onlineText);

        if (hasUserName()) {
            Span nameText = new Span(buildFullName());
            nameText.addClassNames(LumoUtility.FontWeight.BOLD);
            contentLayout.add(nameText);
        }

        Span emailText = new Span(currentUser.getEmail());
        emailText.addClassNames(LumoUtility.TextColor.SECONDARY);
        contentLayout.add(emailText);
    }

    private void addOfflineUserInfo(VerticalLayout contentLayout) {
        offlineText = new Span(messageProperties.getBaseOfflineText());
        offlineText.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.SEMIBOLD);
        contentLayout.add(offlineText);
    }

    private void addManageAccountButton(VerticalLayout contentLayout) {
        manageAccountButton = new Button(messageProperties.getBaseManageAccountButton());
        manageAccountButton.setPrefixComponent(VaadinIcon.COG.create());
        manageAccountButton.setEnabled(hasCurrentUser());

        if (hasCurrentUser()) {
            manageAccountButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        manageAccountButton.setWidthFull();
        contentLayout.add(manageAccountButton);
    }

    private void addManageApplicationButton(VerticalLayout contentLayout) {
        if (!hasCurrentUser() || actions.manageApplicationAction() == null) {
            return;
        }

        manageApplicationButton = new Button(
                messageProperties.getBaseManageApplicationButton(),
                clickEvent -> actions.manageApplicationAction().run()
        );
        manageApplicationButton.setPrefixComponent(VaadinIcon.COGS.create());
        manageApplicationButton.setWidthFull();
        contentLayout.add(manageApplicationButton);
    }

    private void addUserStatusSelect(VerticalLayout contentLayout) {
        userStatusSelect = new Select<>();
        userStatusSelect.setEnabled(hasCurrentUser());
        userStatusSelect.setItems(UserStatus.values());
        userStatusSelect.setItemLabelGenerator(
                userStatus -> messageProperties.getTranslation(userStatus.getTextKey())
        );
        userStatusSelect.setRenderer(new ComponentRenderer<>(this::createUserStatusRenderer));

        userStatusSelect.addValueChangeListener(event ->
                handleUserStatusChange(event.getValue(), event.isFromClient())
        );

        userStatusSelect.setValue(currentStatus);
        userStatusSelect.setWidthFull();

        updateStatusSelectPrefix(currentStatus);
        contentLayout.add(userStatusSelect);
    }

    private Component createUserStatusRenderer(UserStatus userStatus) {
        HorizontalLayout contentComboBox = new HorizontalLayout();
        contentComboBox.setWidthFull();

        addStatusVisualComponent(contentComboBox, userStatus);
        contentComboBox.add(createStatusTextLayout(userStatus));

        return contentComboBox;
    }

    private void addStatusVisualComponent(HorizontalLayout parent, UserStatus userStatus) {
        Object statusComponent = userStatus.getComponent();

        if (statusComponent instanceof Icon statusIcon) {
            parent.add(createCenteredWrapper(statusIcon));
            return;
        }

        if (statusComponent instanceof Div statusDiv) {
            parent.add(createCenteredWrapper(statusDiv));
        }
    }

    private VerticalLayout createCenteredWrapper(Component component) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidth(null);
        wrapper.setSizeUndefined();
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        wrapper.add(component);
        return wrapper;
    }

    private VerticalLayout createStatusTextLayout(UserStatus userStatus) {
        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setWidthFull();

        Span userStatusText = new Span(messageProperties.getTranslation(userStatus.getTextKey()));
        Span userStatusSecondaryText =
                new Span(messageProperties.getTranslation(userStatus.getTextDescriptionKey()));
        userStatusSecondaryText.addClassNames(
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.TextColor.SECONDARY
        );

        textLayout.add(userStatusText);
        textLayout.add(new Hr());
        textLayout.add(userStatusSecondaryText);

        return textLayout;
    }

    private void handleUserStatusChange(UserStatus userStatus, boolean isFromClient) {
        updateStatusSelectPrefix(userStatus);

        if (userStatus == null) {
            return;
        }

        currentStatus = userStatus;

        if (hasCurrentUser()) {
            currentUser.setStatus(userStatus);
        }

        updateStatusIcons(userStatus);

        if (isFromClient && hasCurrentUser()) {
            userService.updateUserStatus(currentUser, userStatus, true);
        }
    }

    private void addLanguageButtons(VerticalLayout contentLayout) {
        Locale locale = VaadinSession.getCurrent().getLocale();

        Button deFlagButton = createLanguageButton(
                "themes/custom-theme/flag_de.png",
                "German Flag Icon",
                Locale.GERMAN,
                locale
        );
        deFlagIcon = (Image) deFlagButton.getChildren()
                .filter(Image.class::isInstance)
                .findFirst()
                .orElse(null);

        Button enFlagButton = createLanguageButton(
                "themes/custom-theme/flag_en.png",
                "English Flag Icon",
                Locale.ENGLISH,
                locale
        );
        enFlagIcon = (Image) enFlagButton.getChildren()
                .filter(Image.class::isInstance)
                .findFirst()
                .orElse(null);

        HorizontalLayout flagLayout = new HorizontalLayout(deFlagButton, enFlagButton);
        contentLayout.add(flagLayout);
    }

    private Button createLanguageButton(String imagePath,
                                        String altText,
                                        Locale targetLocale,
                                        Locale currentLocale) {
        Image flagIcon = new Image(imagePath, altText);
        flagIcon.setWidth("32px");
        flagIcon.setHeight("32px");
        updateFlagStyle(flagIcon, currentLocale.equals(targetLocale));

        Button flagButton = new Button(flagIcon, clickEvent -> switchLanguage(targetLocale));
        flagButton.addThemeVariants(LUMO_TERTIARY_INLINE);
        return flagButton;
    }

    private void switchLanguage(Locale locale) {
        if (hasCurrentUser()) {
            userService.updateUserLocale(currentUser.getEmail(), locale);
        }

        VaadinSession.getCurrent().setLocale(locale);
        LanguageChangeEvent.fire(UI.getCurrent(), locale);
    }

    private void addHomeButton(VerticalLayout contentLayout) {
        if (actions.homeAction() == null) {
            return;
        }

        homeButton = new Button(messageProperties.getBaseHomeButton(), clickEvent -> actions.homeAction().run());
        homeButton.setPrefixComponent(VaadinIcon.HOME.create());
        homeButton.setWidthFull();
        contentLayout.add(homeButton);
    }

    private void addAuthButtons(VerticalLayout contentLayout) {
        if (hasCurrentUser()) {
            addLogoutButton(contentLayout);
            return;
        }

        addRegistrationButton(contentLayout);
        addLoginButton(contentLayout);
    }

    private void addLogoutButton(VerticalLayout contentLayout) {
        logoutButton = new Button(messageProperties.getBaseLogoutButton(), clickEvent -> securityService.logout());
        logoutButton.setPrefixComponent(VaadinIcon.SIGN_OUT.create());
        logoutButton.setWidthFull();
        contentLayout.add(logoutButton);
    }

    private void addRegistrationButton(VerticalLayout contentLayout) {
        if (actions.registrationAction() == null) {
            return;
        }

        registrationButton = new Button(
                messageProperties.getBaseRegistrationButton(),
                clickEvent -> actions.registrationAction().run()
        );
        registrationButton.setPrefixComponent(VaadinIcon.EDIT.create());
        registrationButton.setWidthFull();
        contentLayout.add(registrationButton);
    }

    private void addLoginButton(VerticalLayout contentLayout) {
        if (actions.loginAction() == null) {
            return;
        }

        loginButton = new Button(
                messageProperties.getBaseLoginButton(),
                clickEvent -> actions.loginAction().run()
        );
        loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        contentLayout.add(loginButton);
    }

    private void refreshTexts() {
        if (onlineText != null) {
            onlineText.setText(messageProperties.getBaseOnlineText());
        }
        if (offlineText != null) {
            offlineText.setText(messageProperties.getBaseOfflineText());
        }
        if (manageAccountButton != null) {
            manageAccountButton.setText(messageProperties.getBaseManageAccountButton());
        }
        if (manageApplicationButton != null) {
            manageApplicationButton.setText(messageProperties.getBaseManageApplicationButton());
        }
        if (homeButton != null) {
            homeButton.setText(messageProperties.getBaseHomeButton());
        }
        if (logoutButton != null) {
            logoutButton.setText(messageProperties.getBaseLogoutButton());
        }
        if (registrationButton != null) {
            registrationButton.setText(messageProperties.getBaseRegistrationButton());
        }
        if (loginButton != null) {
            loginButton.setText(messageProperties.getBaseLoginButton());
        }
    }

    private void refreshUserStatusSelect() {
        if (userStatusSelect == null) {
            return;
        }

        userStatusSelect.setItemLabelGenerator(
                userStatus -> messageProperties.getTranslation(userStatus.getTextKey())
        );
        userStatusSelect.setItems(UserStatus.values());
        userStatusSelect.getDataProvider().refreshAll();
        userStatusSelect.setValue(currentStatus);
        updateStatusSelectPrefix(currentStatus);
    }

    private void refreshLanguageFlags() {
        Locale locale = VaadinSession.getCurrent().getLocale();

        if (deFlagIcon != null) {
            updateFlagStyle(deFlagIcon, locale.equals(Locale.GERMAN));
        }

        if (enFlagIcon != null) {
            updateFlagStyle(enFlagIcon, locale.equals(Locale.ENGLISH));
        }
    }

    private void updateFlagStyle(Image flagIcon, boolean active) {
        if (active) {
            StyleHelper.removeFilter(flagIcon);
        } else {
            StyleHelper.addFilterWithGrayscale100Percent(flagIcon);
        }
    }

    private void updateStatusIcons(UserStatus userStatus) {
        if (userStatus == null) {
            return;
        }

        if (overlayIcon != null) {
            overlayIcon.removeAll();
            overlayIcon.add(new OverlayUserIcon(
                    messageProperties,
                    new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                    userStatus,
                    IconSize.PIXEL_96
            ));
        }

        if (statusIconConsumer != null) {
            statusIconConsumer.accept(userStatus);
        }
    }

    private void updateStatusSelectPrefix(UserStatus userStatus) {
        if (userStatus == null) {
            ComponentPrefixHelper.clearSlot(userStatusSelect, "prefix");
            return;
        }

        Object prefixComponent = userStatus.getComponent(IconSize.PIXEL_16);

        if (prefixComponent instanceof Icon statusIcon) {
            ComponentPrefixHelper.setPrefixComponent(userStatusSelect, createPrefixLayout(statusIcon));
            return;
        }

        if (prefixComponent instanceof Div statusDiv) {
            ComponentPrefixHelper.setPrefixComponent(userStatusSelect, createPrefixLayout(statusDiv));
        }
    }

    private HorizontalLayout createPrefixLayout(Component component) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.addClassNames(LumoUtility.Padding.Left.SMALL, LumoUtility.Padding.Right.SMALL);
        layout.add(component);
        return layout;
    }

    private boolean hasCurrentUser() {
        return currentUser != null;
    }

    private boolean hasUserName() {
        return hasCurrentUser()
                && (StringUtils.isNotEmpty(currentUser.getFirstName())
                || StringUtils.isNotEmpty(currentUser.getLastName()));
    }

    private String buildFullName() {
        String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
        String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}