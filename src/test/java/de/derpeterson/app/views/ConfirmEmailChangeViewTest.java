package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.Confirmation;
import de.derpeterson.app.service.ConfigService;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConfirmEmailChangeViewTest {
  AccountSecurityService service;
  MessageProperties messages;
  ConfirmEmailChangeView view;

  @BeforeEach
  void setup() {
    var ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    var config = mock(ConfigService.class);
    when(config.getString(any()))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    service = mock(AccountSecurityService.class);
    messages = new MessageProperties(new CustomI18NProvider());
    view = new ConfirmEmailChangeView(messages, service, config);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }

  Button confirm() {
    return (Button)
        tree(view)
            .filter(c -> c.getId().orElse("").equals("account-email-confirm-action"))
            .findFirst()
            .orElseThrow();
  }

  @Test
  void openingTheLinkNeverChangesTheAccountAndConfirmationNeedsAnExplicitClick() {
    when(service.inspect("own-token")).thenReturn(Confirmation.READY);
    when(service.confirm("own-token")).thenReturn(Confirmation.SUCCESS);
    view.setParameter(mock(BeforeEvent.class), "own-token");
    verify(service, never()).confirm(any());
    assertTrue(confirm().isVisible());
    confirm().click();
    verify(service).confirm("own-token");
    assertFalse(confirm().isVisible());
    assertTrue(
        view.getElement()
            .getTextRecursively()
            .contains(messages.getAccountEmailConfirmationSuccess()));
  }

  @ParameterizedTest
  @EnumSource(
      value = Confirmation.class,
      names = {"INVALID", "EXPIRED", "CONFLICT"})
  void unavailableLinksNeverOfferAnAction(Confirmation state) {
    when(service.inspect(any())).thenReturn(state);
    view.setParameter(mock(BeforeEvent.class), "own-token");
    assertFalse(confirm().isVisible());
    verify(service, never()).confirm(any());
  }

  @Test
  void technicalFailureIsLocalizedAndDoesNotExposeBackendDetails() {
    when(service.inspect(any())).thenThrow(new IllegalStateException("secret SQL details"));
    view.setParameter(mock(BeforeEvent.class), "own-token");
    assertFalse(confirm().isVisible());
    var text = view.getElement().getTextRecursively();
    assertTrue(text.contains(messages.getAccountEmailConfirmationFailed()));
    assertFalse(text.contains("secret SQL"));
  }
}
