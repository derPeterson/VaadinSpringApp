package de.derpeterson.app.ui.components;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;

/** Gemeinsame Konto-Schnelleinstiege neben dem Avatar; anonym vollständig ausgeblendet. */
public class AccountHeaderActions extends HorizontalLayout {
  private final transient MessageProperties messages;
  private final Button notifications = new Button(VaadinIcon.BELL.create());
  private final Button settings = new Button(VaadinIcon.COG.create());

  public AccountHeaderActions(MessageProperties messages, boolean authenticated) {
    this.messages = messages;
    setSpacing(false);
    setPadding(false);
    addClassName("cosmos-topbar-shortcuts");
    setVisible(authenticated);
    notifications.setId("account-header-notifications");
    settings.setId("account-header-settings");
    for (var button : new Button[] {notifications, settings}) {
      button.addThemeVariants(LUMO_TERTIARY_INLINE);
      button.addClassNames("cosmos-topbar-shortcut", LumoUtility.TextColor.PRIMARY);
    }
    if (authenticated)
      settings.addClickListener(event -> UI.getCurrent().navigate(AccountView.class));
    add(notifications, settings);
    refreshTexts();
  }

  private UserMessageBell messageBell;

  public void configureMessages(
      de.derpeterson.app.service.UserMessageService service,
      jakarta.servlet.http.HttpServletRequest request) {
    if (!isVisible() || messageBell != null) return;
    remove(notifications);
    messageBell = new UserMessageBell(messages, service, request, notifications);
    addComponentAtIndex(0, messageBell);
  }

  public void refreshTexts() {
    notifications.setAriaLabel(messages.getBaseNotifications());
    notifications.setTooltipText(messages.getBaseNotifications());
    settings.setAriaLabel(messages.getBaseManageAccountButton());
    settings.setTooltipText(messages.getBaseManageAccountButton());
    if (messageBell != null) messageBell.refreshTexts();
  }
}
