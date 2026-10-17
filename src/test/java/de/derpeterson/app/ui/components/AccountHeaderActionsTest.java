package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountHeaderActionsTest {
  private final UI ui = spy(new UI());
  private final MessageProperties messages = mock(MessageProperties.class);

  private AccountHeaderActions create(boolean authenticated) {
    UI.setCurrent(ui);
    when(messages.getBaseNotifications()).thenReturn("Benachrichtigungen");
    when(messages.getBaseManageAccountButton()).thenReturn("Konto verwalten");
    return new AccountHeaderActions(messages, authenticated);
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
  }

  private Button button(AccountHeaderActions actions, String id) {
    return actions
        .getChildren()
        .filter(component -> component.getId().orElse("").equals(id))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shortcutsAreOnlyVisibleForAuthenticatedAccounts(boolean authenticated) {
    assertEquals(authenticated, create(authenticated).isVisible());
  }

  @Test
  void settingsOpensTheAccountDirectlyWhileTheBellHasNoAction() {
    var actions = create(true);
    doReturn(Optional.empty()).when(ui).navigate(AccountView.class);
    button(actions, "account-header-notifications").click();
    verify(ui, never()).navigate(AccountView.class);
    button(actions, "account-header-settings").click();
    verify(ui).navigate(AccountView.class);
  }

  @Test
  void accessibleLabelsFollowLanguageChanges() {
    var actions = create(true);
    assertEquals(
        "Benachrichtigungen",
        button(actions, "account-header-notifications").getAriaLabel().orElseThrow());
    when(messages.getBaseNotifications()).thenReturn("Notifications");
    when(messages.getBaseManageAccountButton()).thenReturn("Manage account");
    actions.refreshTexts();
    assertEquals(
        "Notifications",
        button(actions, "account-header-notifications").getAriaLabel().orElseThrow());
    assertEquals(
        "Manage account", button(actions, "account-header-settings").getAriaLabel().orElseThrow());
  }
}
