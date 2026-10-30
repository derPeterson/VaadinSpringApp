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
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(session);
    UI ui = new UI();
    ui.getInternals().setSession(session);
    UI.setCurrent(ui);
    MessageProperties texts = new MessageProperties(new CustomI18NProvider());
    dialog = new PortalConfirmationDialog(texts, "test-confirmation");
    ui.add(dialog);
    prompt =
        new PortalConfirmationDialog.Prompt(
            () -> "Titel",
            () -> "Beschreibung",
            () -> "Frage?",
            () -> "Erklärung",
            () -> new Span("Betroffener Eintrag"),
            () -> "Hinweis",
            texts::getPortalDeleteAction,
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
    AtomicInteger calls = new AtomicInteger();
    dialog.open(prompt, calls::incrementAndGet);
    button("cancel").click();
    button("confirm").click();
    dialog.open(prompt, calls::incrementAndGet);
    button("close").click();
    button("confirm").click();
    assertEquals(0, calls.get());
    assertFalse(dialog.isOpened());
  }

  @Test
  void confirmingConsumesOnlyTheCurrentActionOnce() {
    AtomicInteger calls = new AtomicInteger();
    dialog.open(prompt, () -> calls.addAndGet(100));
    dialog.close();
    dialog.open(prompt, calls::incrementAndGet);
    button("confirm").click();
    button("confirm").click();
    assertEquals(1, calls.get());
    assertFalse(dialog.isOpened());
  }

  @Test
  void aFailedActionCannotBeReplayed() {
    AtomicInteger calls = new AtomicInteger();
    dialog.open(
        prompt,
        () -> {
          calls.incrementAndGet();
          throw new IllegalStateException("Test failure");
        });
    assertThrows(IllegalStateException.class, () -> button("confirm").click());
    button("confirm").click();
    assertEquals(1, calls.get());
    assertFalse(dialog.isOpened());
  }
}
