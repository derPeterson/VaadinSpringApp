package de.derpeterson.app.ui.base;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountHeaderActions;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.PageContentLayout;
import de.derpeterson.app.ui.components.PageFrameComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.HomeView;
import de.derpeterson.app.websocket.UserProfileBroadcaster.UserProfileChangeMessage;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public abstract class TrackedUserAppLayout extends UserActivityAwareView {

  protected final transient MessageProperties messageProperties;
  protected final transient HttpServletRequest request;

  protected final transient UserEntity currentUser;

  protected final AccountHeaderActions accountHeaderActions;
  protected final transient UserAvatarService userAvatarService;
  private transient Registration profileChangeRegistration;
  protected Button userIconButton;
  protected transient UserPopoverMenu userPopoverMenu;
  protected final Button accountNavigationToggle = new Button(AppIcon.MENU.create());
  private final Span applicationServiceNameSpan = new Span();
  private final Span headerAccountNameSpan = new Span();
  private final Span headerAccountEmailSpan = new Span();

  protected TrackedUserAppLayout(
      MessageProperties messageProperties,
      SecurityService securityService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      HttpServletRequest request,
      UserAvatarService userAvatarService) {
    super(securityService, userService, userStatusBroadcaster);

    this.messageProperties = messageProperties;
    this.request = request;
    this.userAvatarService = userAvatarService;
    this.currentUser = securityService.getCurrentUser(request).orElse(null);
    this.accountHeaderActions = new AccountHeaderActions(messageProperties, currentUser != null);
    addAttachListener(event -> registerProfileUpdates(event.getUI()));
    addDetachListener(event -> {
      if (profileChangeRegistration != null) {
        profileChangeRegistration.remove();
        profileChangeRegistration = null;
      }
    });

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          onTrackedUserLanguageChanged();
          accountHeaderActions.refreshTexts();
          refreshHeaderIdentity();

          if (userPopoverMenu != null) {
            userPopoverMenu.refreshForLanguageChange();
          }

          NotificationHelper.getInstance().updateText();
        });
  }

  private void registerProfileUpdates(UI ui) {
    if (currentUser == null) {
      return;
    }
    profileChangeRegistration =
        userAvatarService
            .events()
            .register(message -> {
              if (Objects.equals(currentUser.getId(), message.userId())) {
                ui.access(() -> applyProfileUpdate(message));
              }
            });
  }

  private void applyProfileUpdate(UserProfileChangeMessage profileChangeMessage) {
    if (!isAttached()) {
      return;
    }

    currentUser.setFirstName(profileChangeMessage.firstName());
    currentUser.setLastName(profileChangeMessage.lastName());

    if (userPopoverMenu != null) {
      userPopoverMenu.refreshProfileName();
    }
    refreshTrackedUserAvatar();

    onTrackedUserProfileChanged(profileChangeMessage.firstName(), profileChangeMessage.lastName());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public final void configureUserNotifications(
      de.derpeterson.app.service.UserNotificationService service) {
    accountHeaderActions.configureNotifications(service, request);
  }

  /** Gemeinsame Kopfzeile der persönlichen Seiten; die Startseite liefert eigene Menüziele. */
  protected final void refreshApplicationServiceName(String serviceNameText) {
    applicationServiceNameSpan.setText(serviceNameText);
  }

  protected final HorizontalLayout createApplicationHeader(ConfigService config) {
    Runnable openAdministrationAction =
        currentUser != null && currentUser.hasAdministrativeRole()
            ? () -> UI.getCurrent().navigate(AdminView.class)
            : null;
    return createApplicationHeader(
        config,
        new UserPopoverMenu.UserPopoverActions(
            openAdministrationAction, () -> UI.getCurrent().navigate(HomeView.class)));
  }

  protected final void setPortalPage(
      Component header, PageContentLayout content, ConfigService config) {
    setContent(
        new PageFrameComponent(
            header,
            content,
            new CosmosCopyrightComponent(
                config,
                Clock.systemDefaultZone(),
                messageProperties::getAccountCopyrightTemplate)));
  }

  protected final HorizontalLayout createApplicationHeader(
      ConfigService config, UserPopoverMenu.UserPopoverActions actions) {
    Image serviceLogoImage = new Image("icons/service-logo.svg", "Service Icon");
    boolean usesReferenceAccountStyle = hasClassName(REFERENCE_PORTAL);
    if (usesReferenceAccountStyle) {
      serviceLogoImage.setAlt("");
    }
    serviceLogoImage.addClassName("cosmos-topbar-logo");
    Span serviceNameSpan = applicationServiceNameSpan;
    refreshApplicationServiceName(config.getString(ConfigEntry.SERVICE_NAME));
    serviceNameSpan.addClassName("cosmos-topbar-service-name");
    Anchor homeLink =
        new Anchor(AppRoute.HOME.getSecurityRoute(), serviceLogoImage, serviceNameSpan);
    homeLink.addClassName("cosmos-topbar-home");
    HorizontalLayout brandLayout =
        usesReferenceAccountStyle
            ? new HorizontalLayout(homeLink)
            : new HorizontalLayout(serviceLogoImage, serviceNameSpan);
    accountNavigationToggle.setId("account-navigation-toggle");
    accountNavigationToggle.addClassName("account-navigation-toggle");
    accountNavigationToggle.addThemeVariants(LUMO_TERTIARY_INLINE);
    accountNavigationToggle.setVisible(usesReferenceAccountStyle);
    if (usesReferenceAccountStyle) {
      brandLayout.addComponentAsFirst(accountNavigationToggle);
      accountHeaderActions.useReferenceIcons();
    }
    brandLayout.setSpacing(false);
    brandLayout.addClassName("cosmos-topbar-brand");

    HorizontalLayout userMenuLayout = new HorizontalLayout();
    // Geschützte Views können vor ihrer Weiterleitung noch ohne aktuellen Benutzer entstehen.
    // Besucheraktionen gehören ausschließlich in den öffentlichen Header.
    if (currentUser != null) {
      Button headerUserButton = createTrackedUserButton();
      if (usesReferenceAccountStyle) {
        Div headerIdentityDiv = new Div(headerAccountNameSpan, headerAccountEmailSpan);
        headerIdentityDiv.addClassName("cosmos-header-identity");
        headerUserButton
            .getElement()
            .appendChild(headerIdentityDiv.getElement(), AppIcon.ANGLE_DOWN.create().getElement());
        refreshHeaderIdentity();
      }
      createTrackedUserPopover(actions);
      userPopoverMenu.addThemeName("cosmos");
      if (usesReferenceAccountStyle) {
        userPopoverMenu.addThemeName("cosmos-reference");
      }
      userMenuLayout.add(accountHeaderActions, headerUserButton);
    }
    userMenuLayout.setSpacing(false);
    userMenuLayout.addClassName("cosmos-topbar-account-menu");
    HorizontalLayout applicationHeaderLayout = new HorizontalLayout(brandLayout, userMenuLayout);
    applicationHeaderLayout.setSpacing(false);
    applicationHeaderLayout.addClassName("cosmos-topbar");
    return applicationHeaderLayout;
  }

  protected Button createTrackedUserButton() {
    this.userIconButton =
        new Button(
            new OverlayUserIcon(
                messageProperties,
                currentUser,
                currentUser != null ? currentUser.getStatus() : UserStatus.getDefaultStatus(),
                hasClassName(REFERENCE_PORTAL) ? IconSize.PIXEL_32 : IconSize.PIXEL_48,
                true));

    ((OverlayUserIcon) userIconButton.getIcon()).configureAvatar(userAvatarService, null);
    refreshHeaderIdentity();
    userIconButton.addClassNames("tracked-user-button");
    userIconButton.addThemeVariants(LUMO_TERTIARY_INLINE);

    return userIconButton;
  }

  protected void createTrackedUserPopover(UserPopoverMenu.UserPopoverActions actions) {
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

    userPopoverMenu.configureAvatars(userAvatarService);
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
    refreshHeaderIdentity();
  }

  private void refreshHeaderIdentity() {
    String accountDisplayName =
        currentUser == null
            ? ""
            : (Objects.toString(currentUser.getFirstName(), "")
                    + " "
                    + Objects.toString(currentUser.getLastName(), ""))
                .trim();
    headerAccountNameSpan.setText(accountDisplayName);
    headerAccountEmailSpan.setText(
        currentUser == null ? "" : Objects.toString(currentUser.getEmail(), ""));
    accountNavigationToggle.setAriaLabel(messageProperties.getAccountOpenNavigation());

    if (userIconButton != null) {
      userIconButton.setAriaLabel(
          hasClassName(REFERENCE_PORTAL)
              ? messageProperties.getAccountOpenUserMenu(accountDisplayName)
              : AppConstants.USER_ICON_ALT);
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

  protected void onTrackedUserProfileChanged(String firstName, String lastName) {}

  protected void onTrackedUserLanguageChanged() {
    // optional override in subclass
  }
}
