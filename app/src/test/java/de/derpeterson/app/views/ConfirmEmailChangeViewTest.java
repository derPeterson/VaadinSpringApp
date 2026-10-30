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
import com.vaadin.flow.component.icon.SvgIcon;
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

  SvgIcon statusIcon() {
    return (SvgIcon)
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
    Component hint =
        tree(view)
            .filter(c -> c.getId().orElse("").equals("account-email-confirm-hint"))
            .findFirst()
            .orElseThrow();
    assertTrue(hint.isVisible());
    assertTrue(
        hint.getElement().getTextRecursively().contains(messages.getPublicEmailConfirmationHint()));
    assertTrue(view.getElement().getTextRecursively().contains(messages.getPublicEmailFormTitle()));
    assertEquals("icons/lucide/mail.svg", statusIcon().getElement().getAttribute("src"));
    confirm().click();
    verify(service).confirm("own-token");
    assertFalse(confirm().isVisible());
    assertFalse(hint.isVisible());
    assertEquals("icons/lucide/circle-check.svg", statusIcon().getElement().getAttribute("src"));
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
    assertEquals("icons/lucide/circle-alert.svg", statusIcon().getElement().getAttribute("src"));
    assertTrue(
        statusIcon().hasClassName(state == Confirmation.EXPIRED ? "text-warning" : "text-error"));
    assertEquals(
        state == Confirmation.EXPIRED, statusIcon().hasClassName("cosmos-status-icon--warning"));
    verify(service, never()).confirm(any());
  }

  @Test
  void technicalFailureIsLocalizedAndDoesNotExposeBackendDetails() {
    when(service.inspect(any())).thenThrow(new IllegalStateException("secret SQL details"));
    view.setParameter(mock(BeforeEvent.class), "own-token");
    assertFalse(confirm().isVisible());
    assertEquals("icons/lucide/circle-alert.svg", statusIcon().getElement().getAttribute("src"));
    assertTrue(statusIcon().hasClassName("text-error"));
    String text = view.getElement().getTextRecursively();
    assertTrue(text.contains(messages.getAccountEmailConfirmationFailed()));
    assertFalse(text.contains("secret SQL"));
  }
}
