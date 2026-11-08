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
  private final MessageProperties texts;
  private final OverlayUserIcon avatar;
  private final PageNavigationComponent accountNavigation;
  private final Span name = new Span();
  private final Span presence = new Span();
  private final Span unread = new Span();
  private final List<Runnable> textUpdates = new ArrayList<>();
  private UserStatus status;

  public PortalSidebarComponent(
      MessageProperties texts,
      UserEntity user,
      AppRoute selectedRoute,
      PageNavigationComponent accountNavigation,
      Runnable editAvatar) {
    this.texts = texts;
    this.accountNavigation = accountNavigation;
    status =
        user == null
            ? UserStatus.getDefaultStatus()
            : Objects.requireNonNullElse(user.getStatus(), UserStatus.getDefaultStatus());
    setId("account-sidebar");
    addClassName("account-sidebar");
    avatar = new OverlayUserIcon(texts, user, status, IconSize.PIXEL_48, true);
    avatar.addClassName("account-summary-avatar");
    name.addClassName("account-summary-name");
    refreshName(user == null ? "" : user.getFirstName(), user == null ? "" : user.getLastName());
    Span address = new Span(user == null ? "" : Objects.toString(user.getEmail(), ""));
    address.addClassName("account-summary-email");
    address.getElement().setAttribute("title", address.getText());
    Span active =
        label(
            user != null && user.isEnabled()
                ? texts::getAccountSettingsEnabled
                : texts::getAccountSettingsDisabled);
    active.addClassName("account-summary-status");
    active
        .getElement()
        .getThemeList()
        .addAll(List.of("badge", user != null && user.isEnabled() ? "success" : "error"));
    presence.addClassName("account-summary-presence");
    Div badges = new Div(active, presence);
    badges.addClassName("account-summary-badges");
    Div identity = new Div(name, address, badges);
    identity.addClassName("account-summary-details");
    Div row = new Div(avatar, identity);
    row.addClassName("account-summary-row");
    Button edit = button(AppIcon.EDIT, texts::getAccountSettingsEditPicture, editAvatar);
    edit.setId("account-avatar-edit");
    CosmosCardComponent summary = new CosmosCardComponent(row, edit);
    summary.addClassName("account-summary");
    Span portalGroup = label(texts::getAccountNavigationPortal);
    portalGroup.addClassName("account-nav-group");
    PageNavigationComponent portalNavigation = new PageNavigationComponent();
    SideNavItem home = new SideNavItem("", AppRoute.HOME.getSecurityRoute(), AppIcon.HOME.create());
    home.setId("account-nav-home");
    textUpdates.add(() -> home.setLabel(texts.getBaseHomeButton()));
    portalNavigation.registerAction(home, () -> UI.getCurrent().navigate(HomeView.class));
    PageNavigationComponent.setItemActive(home, selectedRoute == AppRoute.HOME);
    SideNavItem notifications =
        new SideNavItem("", AppRoute.NOTIFICATIONS.getSecurityRoute(), AppIcon.BELL.create());
    notifications.setId("account-nav-notifications");
    textUpdates.add(() -> notifications.setLabel(texts.getNotificationsTitle()));
    portalNavigation.registerAction(
        notifications, () -> UI.getCurrent().navigate(UserNotificationsView.class));
    PageNavigationComponent.setItemActive(notifications, selectedRoute == AppRoute.NOTIFICATIONS);
    unread.addClassName("account-nav-count");
    unread.setVisible(false);
    unread.getElement().setAttribute("aria-hidden", "true");
    notifications.setSuffixComponent(unread);
    portalNavigation.addItem(home, notifications);
    Span accountGroup = label(texts::getAccountSettingsOverview);
    accountGroup.addClassName("account-nav-group");
    add(summary, portalGroup, portalNavigation, accountGroup, accountNavigation);
    refreshTexts();
  }

  public static PageNavigationComponent accountLinks(MessageProperties texts) {
    PageNavigationComponent navigation = new PageNavigationComponent();
    navigation.setId("account-navigation");
    navigation.getElement().setAttribute("aria-label", texts.getAccountSettingsOverview());
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
    textUpdates.add(() -> label.setText(value.get()));
    return label;
  }

  private Button button(AppIcon icon, Supplier<String> value, Runnable action) {
    Button button = new Button();
    button.setPrefixComponent(icon.create());
    button.addClickListener(event -> action.run());
    textUpdates.add(() -> button.setText(value.get()));
    return button;
  }

  public void configureAvatars(UserAvatarService avatars) {
    avatar.configureAvatar(avatars, null);
  }

  public void configureStatusSelection(Select<UserStatus> selection) {
    selection.setId("account-avatar-status");
    selection.getThemeNames().add("cosmos-reference");
    Runnable heading =
        () ->
            selection
                .getStyle()
                .set("--portal-status-heading", "\"" + texts.getPortalPresenceHeading() + "\"");
    textUpdates.add(heading);
    heading.run();
    avatar.setStatusSelection(selection);
  }

  public void refreshName(String first, String last) {
    name.setText((Objects.toString(first, "") + " " + Objects.toString(last, "")).trim());
    avatar.setName(first, last);
  }

  public void updateStatus(UserStatus newStatus) {
    status = Objects.requireNonNullElse(newStatus, UserStatus.getDefaultStatus());
    avatar.setUserStatus(status);
    refreshPresence();
  }

  private void refreshPresence() {
    presence.setText(texts.getUserStatusLabel(status));
    presence.getStyle().set("--account-presence-color", status.getColor());
  }

  public void updateUnreadCount(long count) {
    unread.setVisible(count > 0);
    unread.setText(count > 99 ? "99+" : Long.toString(Math.max(0, count)));
  }

  public void refreshTexts() {
    textUpdates.forEach(Runnable::run);
    List<String> labels =
        List.of(
            texts.getAccountSettingsProfileTab(),
            texts.getAccountSettingsSecurityTab(),
            texts.getAccountSettingsSessionsTab());
    List<SideNavItem> items = accountNavigation.getItems();
    for (int index = 0; index < Math.min(labels.size(), items.size()); index++) {
      items.get(index).setLabel(labels.get(index));
    }
    avatar.refreshTexts();
    refreshPresence();
  }
}
