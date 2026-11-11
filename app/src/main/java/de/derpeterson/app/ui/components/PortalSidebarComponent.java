package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
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
  private final Badge presenceBadge = new Badge();
  private final Badge unreadNotificationCountBadge = new Badge();
  private final List<Runnable> localizedTextRefreshActions = new ArrayList<>();
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

    Span accountEmailSpan = new Span(user == null ? "" : Objects.toString(user.getEmail(), ""));
    accountEmailSpan.addClassName("account-summary-email");
    accountEmailSpan.getElement().setAttribute("title", accountEmailSpan.getText());

    presenceBadge.addClassName("account-summary-presence");

    Div accountBadgesDiv = new Div(presenceBadge);
    accountBadgesDiv.addClassName("account-summary-badges");
    Div accountIdentityDiv = new Div(accountNameLabel, accountEmailSpan, accountBadgesDiv);
    accountIdentityDiv.addClassName("account-summary-details");
    Div accountIdentityRowDiv = new Div(accountAvatar, accountIdentityDiv);
    accountIdentityRowDiv.addClassName("account-summary-row");

    Button editAvatarButton =
        createLocalizedActionButton(
            AppIcon.EDIT, messageProperties::getAccountSettingsEditPicture, editAvatar);
    editAvatarButton.setId("account-avatar-edit");

    CosmosCardComponent accountSummaryCard =
        new CosmosCardComponent(accountIdentityRowDiv, editAvatarButton);
    accountSummaryCard.addClassName("account-summary");

    Span portalGroupSpan = createLocalizedLabelSpan(messageProperties::getAccountNavigationPortal);
    portalGroupSpan.addClassName("account-nav-group");
    PageNavigationComponent portalNavigation = new PageNavigationComponent();
    SideNavItem home = new SideNavItem("", AppRoute.HOME.getSecurityRoute(), AppIcon.HOME.create());
    home.setId("account-nav-home");
    localizedTextRefreshActions.add(() -> home.setLabel(messageProperties.getBaseHomeButton()));
    portalNavigation.registerAction(home, () -> UI.getCurrent().navigate(HomeView.class));
    PageNavigationComponent.setItemActive(home, selectedRoute == AppRoute.HOME);
    SideNavItem notifications =
        new SideNavItem("", AppRoute.NOTIFICATIONS.getSecurityRoute(), AppIcon.BELL.create());
    notifications.setId("account-nav-notifications");
    localizedTextRefreshActions.add(
        () -> notifications.setLabel(messageProperties.getNotificationsTitle()));
    portalNavigation.registerAction(
        notifications, () -> UI.getCurrent().navigate(UserNotificationsView.class));
    PageNavigationComponent.setItemActive(notifications, selectedRoute == AppRoute.NOTIFICATIONS);
    unreadNotificationCountBadge.addClassName("account-nav-count");
    unreadNotificationCountBadge.setVisible(false);
    unreadNotificationCountBadge.getElement().setAttribute("aria-hidden", "true");
    notifications.setSuffixComponent(unreadNotificationCountBadge);
    portalNavigation.addItem(home, notifications);

    Span accountGroupSpan = createLocalizedLabelSpan(messageProperties::getAccountSettingsOverview);
    accountGroupSpan.addClassName("account-nav-group");
    add(accountSummaryCard, portalGroupSpan, portalNavigation, accountGroupSpan, accountNavigation);
    refreshTexts();
  }

  public static PageNavigationComponent accountLinks(MessageProperties messageProperties) {
    PageNavigationComponent accountNavigationComponent = new PageNavigationComponent();
    accountNavigationComponent.setId("account-navigation");
    accountNavigationComponent
        .getElement()
        .setAttribute("aria-label", messageProperties.getAccountSettingsOverview());
    List<AppIcon> sectionIcons = List.of(AppIcon.USER, AppIcon.LOCK, AppIcon.DEVICES);
    List<AccountSection> accountSections =
        List.of(AccountSection.PROFILE, AccountSection.SECURITY, AccountSection.SESSIONS);

    for (int sectionIndex = 0; sectionIndex < accountSections.size(); sectionIndex++) {
      AccountSection accountSection = accountSections.get(sectionIndex);
      SideNavItem accountSectionItem =
          new SideNavItem(
              "", AppRoute.ACCOUNT.getSecurityRoute(), sectionIcons.get(sectionIndex).create());
      accountSectionItem.setId("account-nav-" + accountSection.urlValue());
      QueryParameters accountSectionQuery = accountSection.queryParameters();
      accountSectionItem.setQueryParameters(accountSectionQuery);
      accountNavigationComponent.registerAction(
          accountSectionItem,
          () -> UI.getCurrent().navigate(AccountView.class, accountSectionQuery));
      accountNavigationComponent.addItem(accountSectionItem);
    }
    return accountNavigationComponent;
  }

  private Span createLocalizedLabelSpan(Supplier<String> labelTextSupplier) {
    Span localizedLabelSpan = new Span();
    localizedTextRefreshActions.add(() -> localizedLabelSpan.setText(labelTextSupplier.get()));
    return localizedLabelSpan;
  }

  private Button createLocalizedActionButton(
      AppIcon actionAppIcon, Supplier<String> buttonTextSupplier, Runnable buttonAction) {
    Button localizedActionButton = new Button();
    localizedActionButton.setPrefixComponent(actionAppIcon.create());
    localizedActionButton.addClickListener(event -> buttonAction.run());
    localizedTextRefreshActions.add(() -> localizedActionButton.setText(buttonTextSupplier.get()));
    return localizedActionButton;
  }

  public void configureAvatars(UserAvatarService userAvatarService) {
    accountAvatar.configureAvatar(userAvatarService, null);
  }

  public void configureStatusSelection(Select<UserStatus> userStatusSelect) {
    userStatusSelect.setId("account-avatar-status");
    userStatusSelect.getThemeNames().add("cosmos-reference");
    Runnable refreshStatusSelectHeadingAction =
        () ->
            userStatusSelect
                .getStyle()
                .set(
                    "--portal-status-heading",
                    "\"" + messageProperties.getPortalPresenceHeading() + "\"");
    localizedTextRefreshActions.add(refreshStatusSelectHeadingAction);
    refreshStatusSelectHeadingAction.run();
    accountAvatar.setStatusSelection(userStatusSelect);
  }

  public void refreshName(String firstName, String lastName) {
    accountNameLabel.setText(
        (Objects.toString(firstName, "") + " " + Objects.toString(lastName, "")).trim());
    accountAvatar.setName(firstName, lastName);
  }

  public void updateStatus(UserStatus newStatus) {
    status = Objects.requireNonNullElse(newStatus, UserStatus.getDefaultStatus());
    accountAvatar.setUserStatus(status);
    refreshPresence();
  }

  private void refreshPresence() {
    presenceBadge.setText(messageProperties.getUserStatusLabel(status));
    presenceBadge.getStyle().set("--account-presence-color", status.getColor());
  }

  public void updateUnreadCount(long count) {
    unreadNotificationCountBadge.setVisible(count > 0);
    unreadNotificationCountBadge.setText(count > 99 ? "99+" : Long.toString(Math.max(0, count)));
  }

  public void refreshTexts() {
    localizedTextRefreshActions.forEach(Runnable::run);

    List<String> sectionLabels =
        List.of(
            messageProperties.getAccountSettingsProfileTab(),
            messageProperties.getAccountSettingsSecurityTab(),
            messageProperties.getAccountSettingsSessionsTab());
    List<SideNavItem> accountNavigationItems = accountNavigation.getItems();

    for (int sectionIndex = 0;
        sectionIndex < Math.min(sectionLabels.size(), accountNavigationItems.size());
        sectionIndex++) {
      accountNavigationItems.get(sectionIndex).setLabel(sectionLabels.get(sectionIndex));
    }

    accountAvatar.refreshTexts();
    refreshPresence();
  }
}
