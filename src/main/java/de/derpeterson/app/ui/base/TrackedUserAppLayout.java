package de.derpeterson.app.ui.base;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

public abstract class TrackedUserAppLayout extends UserActivityAwareView {

    protected final transient MessageProperties messageProperties;
    protected final transient HttpServletRequest request;

    protected final transient UserEntity currentUser;

    protected Button userIconButton;
    protected transient UserPopoverMenu userPopoverMenu;

    protected TrackedUserAppLayout(MessageProperties messageProperties,
                                   SecurityService securityService,
                                   UserService userService,
                                   UserStatusBroadcaster userStatusBroadcaster,
                                   HttpServletRequest request) {
        super(securityService, userService, userStatusBroadcaster);

        this.messageProperties = messageProperties;
        this.request = request;
        this.currentUser = securityService.getCurrentUser(request).orElse(null);

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            onTrackedUserLanguageChanged();

            if (userPopoverMenu != null) {
                userPopoverMenu.refreshForLanguageChange();
            }

            NotificationHelper.getInstance().updateText();
        });
    }

    protected Button createTrackedUserButton() {
        this.userIconButton = new Button(new OverlayUserIcon(
                messageProperties,
                new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                currentUser != null ? currentUser.getStatus() : UserStatus.getDefaultStatus(),
                IconSize.PIXEL_48
        ));

        userIconButton.addClassNames("tracked-user-button");
        userIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

        return userIconButton;
    }

    protected void createTrackedUserPopover(UserPopoverMenu.Actions actions) {
        if (userIconButton == null) {
            createTrackedUserButton();
        }

        this.userPopoverMenu = new UserPopoverMenu(
                messageProperties,
                securityService,
                userService,
                currentUser,
                userIconButton,
                actions,
                this::updateHeaderStatusIcon
        );

        Popover userPopover = userPopoverMenu.getPopover();
        userIconButton.addClickListener(buttonClickEvent -> userPopover.setOpened(true));

    }

    protected void updateHeaderStatusIcon(UserStatus userStatus) {
        if (userIconButton == null || userStatus == null) {
            return;
        }

        userIconButton.setIcon(new OverlayUserIcon(
                messageProperties,
                new Image(AppConstants.USER_ICON_PATH, AppConstants.USER_ICON_ALT),
                userStatus,
                IconSize.PIXEL_48
        ));
    }

    @Override
    protected Optional<UserEntity> getTrackedCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    protected void onTrackedUserStatusChanged(UserEntity user, UserStatus oldStatus, UserStatus newStatus) {
        if (userPopoverMenu != null) {
            userPopoverMenu.applyExternalStatus(newStatus);
        } else {
            updateHeaderStatusIcon(newStatus);
        }
    }

    protected void onTrackedUserLanguageChanged() {
        // optional override in subclass
    }
}