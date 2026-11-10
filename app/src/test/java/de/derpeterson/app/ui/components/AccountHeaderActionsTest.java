package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private final UI testUi = spy(new UI());
  private final MessageProperties messageProperties = mock(MessageProperties.class);

  private AccountHeaderActions create(boolean authenticated) {
    UI.setCurrent(testUi);
    when(messageProperties.getBaseNotifications()).thenReturn("Benachrichtigungen");
    when(messageProperties.getBaseManageAccountButton()).thenReturn("Konto verwalten");
    return new AccountHeaderActions(messageProperties, authenticated);
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
  void settingsOpensTheAccountDirectlyWithoutUsingTheBell() {
    AccountHeaderActions actions = create(true);
    doReturn(Optional.empty()).when(testUi).navigate(AccountView.class);
    button(actions, "account-header-notifications").click();

    verify(testUi, never()).navigate(AccountView.class);
    button(actions, "account-header-settings").click();

    verify(testUi).navigate(AccountView.class);
  }

  @Test
  void accessibleLabelsFollowLanguageChanges() {
    AccountHeaderActions actions = create(true);

    assertEquals(
        "Benachrichtigungen",
        button(actions, "account-header-notifications").getAriaLabel().orElseThrow());
    when(messageProperties.getBaseNotifications()).thenReturn("Notifications");
    when(messageProperties.getBaseManageAccountButton()).thenReturn("Manage account");
    actions.refreshTexts();

    assertEquals(
        "Notifications",
        button(actions, "account-header-notifications").getAriaLabel().orElseThrow());
    assertEquals(
        "Manage account", button(actions, "account-header-settings").getAriaLabel().orElseThrow());
  }
}
