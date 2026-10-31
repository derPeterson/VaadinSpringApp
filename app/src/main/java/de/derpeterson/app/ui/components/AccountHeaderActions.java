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
  private final transient MessageProperties messages;
  private final Button notifications = new Button(AppIcon.PAPERPLANE.create());
  private final Button settings = new Button(AppIcon.COG.create());

  public AccountHeaderActions(MessageProperties messages, boolean authenticated) {
    this.messages = messages;
    setSpacing(false);
    setPadding(false);
    addClassName("cosmos-topbar-shortcuts");
    setVisible(authenticated);
    notifications.setId("account-header-notifications");
    settings.setId("account-header-settings");
    for (Button button : new Button[] {notifications, settings}) {
      button.addThemeVariants(LUMO_TERTIARY_INLINE);
      button.addClassNames("cosmos-topbar-shortcut", LumoUtility.TextColor.PRIMARY);
    }
    if (authenticated) {
      settings.addClickListener(event -> UI.getCurrent().navigate(AccountView.class));
    }
    add(notifications, settings);
    refreshTexts();
  }

  private UserNotificationBell notificationBell;
  private boolean referenceStyle;
  private SerializableConsumer<Long> unreadCountListener = value -> {};

  public void setUnreadCountListener(SerializableConsumer<Long> listener) {
    unreadCountListener = listener;
    if (notificationBell != null) {
      notificationBell.setUnreadCountListener(listener);
    }
  }

  public void useReferenceIcons() {
    referenceStyle = true;
    if (notificationBell != null) {
      notificationBell.useReferenceStyle();
    }
    settings.setIcon(AppIcon.COG.create());
    notifications.setIcon(AppIcon.BELL.create());
  }

  public void configureNotifications(
      de.derpeterson.app.service.UserNotificationService service,
      jakarta.servlet.http.HttpServletRequest request) {
    if (!isVisible() || notificationBell != null) {
      return;
    }
    remove(notifications);
    notificationBell = new UserNotificationBell(messages, service, request, notifications);
    notificationBell.setUnreadCountListener(unreadCountListener);
    if (referenceStyle) {
      notificationBell.useReferenceStyle();
    }
    addComponentAtIndex(0, notificationBell);
  }

  public void refreshTexts() {
    notifications.setAriaLabel(messages.getBaseNotifications());
    notifications.setTooltipText(messages.getBaseNotifications());
    settings.setAriaLabel(messages.getBaseManageAccountButton());
    settings.setTooltipText(messages.getBaseManageAccountButton());
    if (notificationBell != null) {
      notificationBell.refreshTexts();
    }
  }
}
