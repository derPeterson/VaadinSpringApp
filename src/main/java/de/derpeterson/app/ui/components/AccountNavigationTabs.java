package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.UserNotificationsView;

/** Gemeinsame Seitenauswahl für Einstellungen und persönliche Benachrichtigungen. */
public class AccountNavigationTabs extends Tabs {
  private final MessageProperties messages;
  private final Tab currentPage;
  private boolean restoringSelection;
  private final Span settingsLabel = new Span();
  private final Span notificationsLabel = new Span();
  private final Tab settings = new Tab(VaadinIcon.COG.create(), settingsLabel);
  private final Tab notifications = new Tab(VaadinIcon.PAPERPLANE.create(), notificationsLabel);

  public AccountNavigationTabs(MessageProperties messages, boolean notificationsSelected) {
    this.messages = messages;
    setId("account-navigation-tabs");
    addClassName("account-navigation-tabs");
    settings.setId("account-navigation-settings");
    notifications.setId("account-navigation-notifications");
    add(settings, notifications);
    currentPage = notificationsSelected ? notifications : settings;
    setSelectedTab(currentPage);
    addAttachListener(event -> restoreSelection());
    addSelectedChangeListener(
        event -> {
          if (restoringSelection) return;
          if (event.getSelectedTab() == settings)
            getUI().ifPresent(ui -> ui.navigate(AccountView.class));
          else getUI().ifPresent(ui -> ui.navigate(UserNotificationsView.class));
        });
    refreshTexts();
  }

  private void restoreSelection() {
    // UIScope behält die Komponente beim späteren Zurückkehren auf diese Seite.
    restoringSelection = true;
    try {
      setSelectedTab(currentPage);
    } finally {
      restoringSelection = false;
    }
  }

  public void refreshTexts() {
    getElement().setAttribute("aria-label", messages.getAccountSettingsTitle());
    settingsLabel.setText(messages.getAccountNavigationSettings());
    notificationsLabel.setText(messages.getNotificationsTitle());
  }
}
