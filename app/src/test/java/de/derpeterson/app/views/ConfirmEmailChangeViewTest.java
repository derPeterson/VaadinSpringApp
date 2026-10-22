package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConfirmEmailChangeViewTest {
  private UI ui;
  AccountSecurityService service;
  MessageProperties messages;
  ConfirmEmailChangeView view;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    ConfigService config = mock(ConfigService.class);
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

  Icon statusIcon() {
    return (Icon)
        tree(view)
            .filter(c -> c.getClassNames().contains("cosmos-status-icon"))
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
    assertEquals("vaadin:envelope", statusIcon().getElement().getAttribute("icon"));
    confirm().click();
    verify(service).confirm("own-token");
    assertFalse(confirm().isVisible());
    assertEquals("vaadin:check-circle", statusIcon().getElement().getAttribute("icon"));
    assertTrue(statusIcon().hasClassName("text-success"));
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
    assertEquals(
        state == Confirmation.EXPIRED ? "vaadin:bell" : "vaadin:warning",
        statusIcon().getElement().getAttribute("icon"));
    assertTrue(
        statusIcon().hasClassName(state == Confirmation.EXPIRED ? "text-warning" : "text-error"));
    verify(service, never()).confirm(any());
  }

  @Test
  void technicalFailureIsLocalizedAndDoesNotExposeBackendDetails() {
    when(service.inspect(any())).thenThrow(new IllegalStateException("secret SQL details"));
    view.setParameter(mock(BeforeEvent.class), "own-token");
    assertFalse(confirm().isVisible());
    assertEquals("vaadin:warning", statusIcon().getElement().getAttribute("icon"));
    assertTrue(statusIcon().hasClassName("text-error"));
    String text = view.getElement().getTextRecursively();
    assertTrue(text.contains(messages.getAccountEmailConfirmationFailed()));
    assertFalse(text.contains("secret SQL"));
  }
}
