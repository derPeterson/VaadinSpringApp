package de.derpeterson.app.ui.components;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;

/** Gemeinsame Konto-Schnelleinstiege neben dem Avatar; anonym vollständig ausgeblendet. */
public class AccountHeaderActions extends HorizontalLayout {
  private final transient MessageProperties messageProperties;
  private final Button openNotificationsButton = new Button(AppIcon.PAPERPLANE.create());
  private final Button openAccountSettingsButton = new Button(AppIcon.COG.create());

  public AccountHeaderActions(MessageProperties messageProperties, boolean authenticated) {
    this.messageProperties = messageProperties;
    setSpacing(false);
    setPadding(false);
    addClassName("cosmos-topbar-shortcuts");
    setVisible(authenticated);
    openNotificationsButton.setId("account-header-notifications");
    openAccountSettingsButton.setId("account-header-settings");
    for (Button accountActionButton :
        new Button[] {openNotificationsButton, openAccountSettingsButton}) {
      accountActionButton.addThemeVariants(LUMO_TERTIARY_INLINE);
      accountActionButton.addClassNames("cosmos-topbar-shortcut", LumoUtility.TextColor.PRIMARY);
    }

    if (authenticated) {
      openAccountSettingsButton.addClickListener(
          event -> UI.getCurrent().navigate(AccountView.class));
    }
    add(openNotificationsButton, openAccountSettingsButton);
    refreshTexts();
  }

  private UserNotificationBell notificationBell;
  private boolean referenceStyle;
  private SerializableConsumer<Long> unreadCountListener = value -> {};

  public void setUnreadCountListener(SerializableConsumer<Long> unreadCountConsumer) {
    unreadCountListener = unreadCountConsumer;

    if (notificationBell != null) {
      notificationBell.setUnreadCountListener(unreadCountConsumer);
    }
  }

  public void useReferenceIcons() {
    referenceStyle = true;

    if (notificationBell != null) {
      notificationBell.useReferenceStyle();
    }
    openAccountSettingsButton.setIcon(AppIcon.COG.create());
    openNotificationsButton.setIcon(AppIcon.BELL.create());
  }

  public void configureNotifications(
      de.derpeterson.app.service.UserNotificationService service,
      jakarta.servlet.http.HttpServletRequest request) {
    if (!isVisible() || notificationBell != null) {
      return;
    }
    remove(openNotificationsButton);
    notificationBell =
        new UserNotificationBell(messageProperties, service, request, openNotificationsButton);
    notificationBell.setUnreadCountListener(unreadCountListener);

    if (referenceStyle) {
      notificationBell.useReferenceStyle();
    }
    addComponentAtIndex(0, notificationBell);
  }

  public void refreshTexts() {
    openNotificationsButton.setAriaLabel(messageProperties.getBaseNotifications());
    openNotificationsButton.setTooltipText(messageProperties.getBaseNotifications());
    openAccountSettingsButton.setAriaLabel(messageProperties.getBaseManageAccountButton());
    openAccountSettingsButton.setTooltipText(messageProperties.getBaseManageAccountButton());

    if (notificationBell != null) {
      notificationBell.refreshTexts();
    }
  }
}
