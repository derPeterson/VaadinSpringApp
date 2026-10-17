package de.derpeterson.app.ui.base;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountHeaderActions;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Optional;

public abstract class TrackedUserAppLayout extends UserActivityAwareView {

  protected final transient MessageProperties messageProperties;
  protected final transient HttpServletRequest request;

  protected final transient UserEntity currentUser;

  protected final AccountHeaderActions accountHeaderActions;
  protected final transient UserAvatarService avatars;
  private transient Registration profileRegistration;
  protected Button userIconButton;
  protected transient UserPopoverMenu userPopoverMenu;

  protected TrackedUserAppLayout(
      MessageProperties messageProperties,
      SecurityService securityService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      HttpServletRequest request,
      UserAvatarService avatars) {
    super(securityService, userService, userStatusBroadcaster);

    this.messageProperties = messageProperties;
    this.request = request;
    this.avatars = avatars;
    this.currentUser = securityService.getCurrentUser(request).orElse(null);
    this.accountHeaderActions = new AccountHeaderActions(messageProperties, currentUser != null);
    addAttachListener(
        event -> {
          if (currentUser == null) return;
          profileRegistration =
              avatars
                  .events()
                  .register(
                      message -> {
                        if (!Objects.equals(currentUser.getId(), message.userId())) return;
                        event
                            .getUI()
                            .access(
                                () -> {
                                  if (!isAttached()) return;
                                  currentUser.setFirstName(message.firstName());
                                  currentUser.setLastName(message.lastName());
                                  if (userPopoverMenu != null) userPopoverMenu.refreshProfileName();
                                  refreshTrackedUserAvatar();
                                  onTrackedUserProfileChanged(
                                      message.firstName(), message.lastName());
                                });
                      });
        });
    addDetachListener(
        event -> {
          if (profileRegistration != null) {
            profileRegistration.remove();
            profileRegistration = null;
          }
        });

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          onTrackedUserLanguageChanged();
          accountHeaderActions.refreshTexts();

          if (userPopoverMenu != null) {
            userPopoverMenu.refreshForLanguageChange();
          }

          NotificationHelper.getInstance().updateText();
        });
  }

  protected Button createTrackedUserButton() {
    this.userIconButton =
        new Button(
            new OverlayUserIcon(
                messageProperties,
                currentUser,
                currentUser != null ? currentUser.getStatus() : UserStatus.getDefaultStatus(),
                IconSize.PIXEL_48,
                true));

    ((OverlayUserIcon) userIconButton.getIcon()).configureAvatar(avatars, null);
    userIconButton.setAriaLabel(AppConstants.USER_ICON_ALT);
    userIconButton.addClassNames("tracked-user-button");
    userIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

    return userIconButton;
  }

  protected void createTrackedUserPopover(UserPopoverMenu.Actions actions) {
    if (userIconButton == null) {
      createTrackedUserButton();
    }

    this.userPopoverMenu =
        new UserPopoverMenu(
            messageProperties,
            securityService,
            userService,
            currentUser,
            userIconButton,
            actions,
            this::updateHeaderStatusIcon);

    userPopoverMenu.configureAvatars(avatars);
    Popover userPopover = userPopoverMenu.getPopover();
    userIconButton.addClickListener(buttonClickEvent -> userPopover.setOpened(true));
  }

  protected void updateHeaderStatusIcon(UserStatus userStatus) {
    if (userIconButton == null || userStatus == null) {
      return;
    }

    if (userIconButton.getIcon() instanceof OverlayUserIcon avatar) {
      avatar.setIdentity(currentUser);
      avatar.setUserStatus(userStatus);
    }
  }

  protected void refreshTrackedUserAvatar() {
    if (userIconButton != null && userIconButton.getIcon() instanceof OverlayUserIcon avatar) {
      avatar.setIdentity(currentUser);
    }
  }

  @Override
  protected Optional<UserEntity> getTrackedCurrentUser() {
    return Optional.ofNullable(currentUser);
  }

  @Override
  protected void onTrackedUserStatusChanged(
      UserEntity user, UserStatus oldStatus, UserStatus newStatus) {
    if (userPopoverMenu != null) {
      userPopoverMenu.applyExternalStatus(newStatus);
    } else {
      updateHeaderStatusIcon(newStatus);
    }
  }

  protected void onTrackedUserProfileChanged(String first, String last) {}

  protected void onTrackedUserLanguageChanged() {
    // optional override in subclass
  }
}
