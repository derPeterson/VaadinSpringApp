package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.HomeView;
import de.derpeterson.app.views.UserNotificationsView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Eine gemeinsame Identitätskarte und Navigation für Startseite, Postfach und Konto. */
public class PortalSidebarComponent extends Div {
  private final MessageProperties messageProperties;
  private final OverlayUserIcon accountAvatar;
  private final PageNavigationComponent accountNavigation;
  private final Span accountNameLabel = new Span();
  private final Span presenceLabel = new Span();
  private final Span unreadNotificationCount = new Span();
  private final List<Runnable> localizedTextUpdates = new ArrayList<>();
  private UserStatus status;

  public PortalSidebarComponent(
      MessageProperties messageProperties,
      UserEntity user,
      AppRoute selectedRoute,
      PageNavigationComponent accountNavigation,
      Runnable editAvatar) {
    this.messageProperties = messageProperties;
    this.accountNavigation = accountNavigation;
    status =
        user == null
            ? UserStatus.getDefaultStatus()
            : Objects.requireNonNullElse(user.getStatus(), UserStatus.getDefaultStatus());
    setId("account-sidebar");
    addClassName("account-sidebar");
    accountAvatar = new OverlayUserIcon(messageProperties, user, status, IconSize.PIXEL_48, true);
    accountAvatar.addClassName("account-summary-avatar");
    accountNameLabel.addClassName("account-summary-name");
    refreshName(user == null ? "" : user.getFirstName(), user == null ? "" : user.getLastName());
    Span address = new Span(user == null ? "" : Objects.toString(user.getEmail(), ""));
    address.addClassName("account-summary-email");
    address.getElement().setAttribute("title", address.getText());
    Span active =
        label(
            user != null && user.isEnabled()
                ? messageProperties::getAccountSettingsEnabled
                : messageProperties::getAccountSettingsDisabled);
    active.addClassName("account-summary-status");
    active
        .getElement()
        .getThemeList()
        .addAll(List.of("badge", user != null && user.isEnabled() ? "success" : "error"));
    presenceLabel.addClassName("account-summary-presence");
    Div badges = new Div(active, presenceLabel);
    badges.addClassName("account-summary-badges");
    Div identity = new Div(accountNameLabel, address, badges);
    identity.addClassName("account-summary-details");
    Div row = new Div(accountAvatar, identity);
    row.addClassName("account-summary-row");
    Button edit =
        button(AppIcon.EDIT, messageProperties::getAccountSettingsEditPicture, editAvatar);
    edit.setId("account-avatar-edit");
    CosmosCardComponent summary = new CosmosCardComponent(row, edit);
    summary.addClassName("account-summary");
    Span portalGroup = label(messageProperties::getAccountNavigationPortal);
    portalGroup.addClassName("account-nav-group");
    PageNavigationComponent portalNavigation = new PageNavigationComponent();
    SideNavItem home = new SideNavItem("", AppRoute.HOME.getSecurityRoute(), AppIcon.HOME.create());
    home.setId("account-nav-home");
    localizedTextUpdates.add(() -> home.setLabel(messageProperties.getBaseHomeButton()));
    portalNavigation.registerAction(home, () -> UI.getCurrent().navigate(HomeView.class));
    PageNavigationComponent.setItemActive(home, selectedRoute == AppRoute.HOME);
    SideNavItem notifications =
        new SideNavItem("", AppRoute.NOTIFICATIONS.getSecurityRoute(), AppIcon.BELL.create());
    notifications.setId("account-nav-notifications");
    localizedTextUpdates.add(
        () -> notifications.setLabel(messageProperties.getNotificationsTitle()));
    portalNavigation.registerAction(
        notifications, () -> UI.getCurrent().navigate(UserNotificationsView.class));
    PageNavigationComponent.setItemActive(notifications, selectedRoute == AppRoute.NOTIFICATIONS);
    unreadNotificationCount.addClassName("account-nav-count");
    unreadNotificationCount.setVisible(false);
    unreadNotificationCount.getElement().setAttribute("aria-hidden", "true");
    notifications.setSuffixComponent(unreadNotificationCount);
    portalNavigation.addItem(home, notifications);
    Span accountGroup = label(messageProperties::getAccountSettingsOverview);
    accountGroup.addClassName("account-nav-group");
    add(summary, portalGroup, portalNavigation, accountGroup, accountNavigation);
    refreshTexts();
  }

  public static PageNavigationComponent accountLinks(MessageProperties messageProperties) {
    PageNavigationComponent navigation = new PageNavigationComponent();
    navigation.setId("account-navigation");
    navigation
        .getElement()
        .setAttribute("aria-label", messageProperties.getAccountSettingsOverview());
    List<AppIcon> icons = List.of(AppIcon.USER, AppIcon.LOCK, AppIcon.DEVICES);
    List<AccountSection> sections =
        List.of(AccountSection.PROFILE, AccountSection.SECURITY, AccountSection.SESSIONS);
    for (int index = 0; index < sections.size(); index++) {
      AccountSection section = sections.get(index);
      SideNavItem item =
          new SideNavItem("", AppRoute.ACCOUNT.getSecurityRoute(), icons.get(index).create());
      item.setId("account-nav-" + section.urlValue());
      QueryParameters query = section.queryParameters();
      item.setQueryParameters(query);
      navigation.registerAction(item, () -> UI.getCurrent().navigate(AccountView.class, query));
      navigation.addItem(item);
    }
    return navigation;
  }

  private Span label(Supplier<String> value) {
    Span label = new Span();
    localizedTextUpdates.add(() -> label.setText(value.get()));
    return label;
  }

  private Button button(AppIcon icon, Supplier<String> value, Runnable action) {
    Button button = new Button();
    button.setPrefixComponent(icon.create());
    button.addClickListener(event -> action.run());
    localizedTextUpdates.add(() -> button.setText(value.get()));
    return button;
  }

  public void configureAvatars(UserAvatarService avatars) {
    accountAvatar.configureAvatar(avatars, null);
  }

  public void configureStatusSelection(Select<UserStatus> selection) {
    selection.setId("account-avatar-status");
    selection.getThemeNames().add("cosmos-reference");
    Runnable heading =
        () ->
            selection
                .getStyle()
                .set(
                    "--portal-status-heading",
                    "\"" + messageProperties.getPortalPresenceHeading() + "\"");
    localizedTextUpdates.add(heading);
    heading.run();
    accountAvatar.setStatusSelection(selection);
  }

  public void refreshName(String first, String last) {
    accountNameLabel.setText(
        (Objects.toString(first, "") + " " + Objects.toString(last, "")).trim());
    accountAvatar.setName(first, last);
  }

  public void updateStatus(UserStatus newStatus) {
    status = Objects.requireNonNullElse(newStatus, UserStatus.getDefaultStatus());
    accountAvatar.setUserStatus(status);
    refreshPresence();
  }

  private void refreshPresence() {
    presenceLabel.setText(messageProperties.getUserStatusLabel(status));
    presenceLabel.getStyle().set("--account-presence-color", status.getColor());
  }

  public void updateUnreadCount(long count) {
    unreadNotificationCount.setVisible(count > 0);
    unreadNotificationCount.setText(count > 99 ? "99+" : Long.toString(Math.max(0, count)));
  }

  public void refreshTexts() {
    localizedTextUpdates.forEach(Runnable::run);
    List<String> labels =
        List.of(
            messageProperties.getAccountSettingsProfileTab(),
            messageProperties.getAccountSettingsSecurityTab(),
            messageProperties.getAccountSettingsSessionsTab());
    List<SideNavItem> items = accountNavigation.getItems();
    for (int index = 0; index < Math.min(labels.size(), items.size()); index++) {
      items.get(index).setLabel(labels.get(index));
    }
    accountAvatar.refreshTexts();
    refreshPresence();
  }
}
