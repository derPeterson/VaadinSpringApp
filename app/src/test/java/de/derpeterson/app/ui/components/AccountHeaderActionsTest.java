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

  private AccountHeaderActions createAccountHeaderActions(boolean isAuthenticated) {
    UI.setCurrent(testUi);

    when(messageProperties.getBaseNotifications()).thenReturn("Benachrichtigungen");
    when(messageProperties.getBaseManageAccountButton()).thenReturn("Konto verwalten");

    return new AccountHeaderActions(messageProperties, isAuthenticated);
  }

  @AfterEach
  void clearCurrentUi() {
    UI.setCurrent(null);
  }

  private Button findHeaderActionButton(
      AccountHeaderActions accountHeaderActions, String buttonId) {
    return accountHeaderActions
        .getChildren()
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(buttonId))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shortcutsAreOnlyVisibleForAuthenticatedAccounts(boolean isAuthenticated) {
    assertEquals(isAuthenticated, createAccountHeaderActions(isAuthenticated).isVisible());
  }

  @Test
  void settingsOpensTheAccountDirectlyWithoutUsingTheBell() {
    AccountHeaderActions accountHeaderActions = createAccountHeaderActions(true);
    doReturn(Optional.empty()).when(testUi).navigate(AccountView.class);

    findHeaderActionButton(accountHeaderActions, "account-header-notifications").click();

    verify(testUi, never()).navigate(AccountView.class);

    findHeaderActionButton(accountHeaderActions, "account-header-settings").click();

    verify(testUi).navigate(AccountView.class);
  }

  @Test
  void accessibleLabelsFollowLanguageChanges() {
    AccountHeaderActions accountHeaderActions = createAccountHeaderActions(true);

    assertEquals(
        "Benachrichtigungen",
        findHeaderActionButton(accountHeaderActions, "account-header-notifications")
            .getAriaLabel()
            .orElseThrow());

    when(messageProperties.getBaseNotifications()).thenReturn("Notifications");
    when(messageProperties.getBaseManageAccountButton()).thenReturn("Manage account");
    accountHeaderActions.refreshTexts();

    assertEquals(
        "Notifications",
        findHeaderActionButton(accountHeaderActions, "account-header-notifications")
            .getAriaLabel()
            .orElseThrow());
    assertEquals(
        "Manage account",
        findHeaderActionButton(accountHeaderActions, "account-header-settings")
            .getAriaLabel()
            .orElseThrow());
  }
}
