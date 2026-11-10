package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PortalConfirmationDialogTest {
  private PortalConfirmationDialog dialog;
  private PortalConfirmationDialog.Prompt prompt;

  @BeforeEach
  void setup() {
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(vaadinSession);
    UI ui = new UI();
    ui.getInternals().setSession(vaadinSession);
    UI.setCurrent(ui);
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    dialog = new PortalConfirmationDialog(messageProperties, "test-confirmation");
    ui.add(dialog);
    prompt =
        new PortalConfirmationDialog.Prompt(
            () -> "Titel",
            () -> "Beschreibung",
            () -> "Frage?",
            () -> "Erklärung",
            () -> new Span("Betroffener Eintrag"),
            () -> "Hinweis",
            messageProperties::getPortalDeleteAction,
            true);
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Button button(String field) {
    return (Button) ReflectionTestUtils.getField(dialog, field);
  }

  @Test
  void cancellingAndClosingNeverExecuteOrRetainTheAction() {
    AtomicInteger callCount = new AtomicInteger();
    dialog.open(prompt, callCount::incrementAndGet);
    button("cancelButton").click();
    button("confirmButton").click();
    dialog.open(prompt, callCount::incrementAndGet);
    button("closeDialogButton").click();
    button("confirmButton").click();

    assertEquals(0, callCount.get());
    assertFalse(dialog.isOpened());
  }

  @Test
  void confirmingConsumesOnlyTheCurrentActionOnce() {
    AtomicInteger callCount = new AtomicInteger();
    dialog.open(prompt, () -> callCount.addAndGet(100));
    dialog.close();
    dialog.open(prompt, callCount::incrementAndGet);
    button("confirmButton").click();
    button("confirmButton").click();

    assertEquals(1, callCount.get());
    assertFalse(dialog.isOpened());
  }

  @Test
  void aFailedActionCannotBeReplayed() {
    AtomicInteger callCount = new AtomicInteger();
    dialog.open(
        prompt,
        () -> {
          callCount.incrementAndGet();
          throw new IllegalStateException("Test failure");
        });
    Button confirm = button("confirmButton");

    assertThrows(IllegalStateException.class, confirm::click);
    button("confirmButton").click();

    assertEquals(1, callCount.get());
    assertFalse(dialog.isOpened());
  }
}
