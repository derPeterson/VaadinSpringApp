package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ConfirmDialogTestActions.click;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
  private PortalConfirmationDialog confirmationDialog;
  private PortalConfirmationDialog.Prompt confirmationPrompt;

  @BeforeEach
  void createConfirmationDialogEnvironment() {
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(vaadinSession);

    UI testUi = new UI();
    testUi.getInternals().setSession(vaadinSession);
    UI.setCurrent(testUi);

    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    confirmationDialog = new PortalConfirmationDialog(messageProperties, "test-confirmation");
    testUi.add(confirmationDialog);

    confirmationPrompt =
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
  void clearConfirmationDialogEnvironment() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Button readConfirmationButton(String buttonFieldName) {
    return (Button) ReflectionTestUtils.getField(confirmationDialog, buttonFieldName);
  }

  @Test
  void nativeStateSynchronizationDoesNotConsumeTheFollowingConfirmation() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    confirmationDialog.setOpened(false);
    ComponentUtil.fireEvent(
        confirmationDialog, new ConfirmDialog.ConfirmEvent(confirmationDialog, true));
    ComponentUtil.fireEvent(
        confirmationDialog, new ConfirmDialog.ConfirmEvent(confirmationDialog, true));
    assertEquals(1, actionCallCounter.get());
  }

  @Test
  void completedClientClosureDiscardsTheUnconfirmedAction() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    confirmationDialog.setOpened(false);
    ComponentUtil.fireEvent(
        confirmationDialog, new ConfirmDialog.ClosedEvent(confirmationDialog, true));
    ComponentUtil.fireEvent(
        confirmationDialog, new ConfirmDialog.ConfirmEvent(confirmationDialog, true));
    assertEquals(0, actionCallCounter.get());
  }

  @Test
  void lateClosedEventDoesNotDiscardTheNewlyOpenedAction() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(confirmationPrompt, () -> actionCallCounter.addAndGet(100));
    confirmationDialog.close();
    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    ComponentUtil.fireEvent(
        confirmationDialog, new ConfirmDialog.ClosedEvent(confirmationDialog, true));
    click(readConfirmationButton("confirmButton"));
    assertEquals(1, actionCallCounter.get());
  }

  @Test
  void draftVariantKeepsTheDraftOnCloseAndOnlyDiscardsExplicitlyOnce() {
    MessageProperties messageProperties = new MessageProperties(new CustomI18NProvider());
    confirmationDialog =
        new PortalConfirmationDialog(
            messageProperties,
            "draft-confirmation",
            PortalConfirmationDialog.Variant.UNSAVED_CHANGES);
    UI.getCurrent().add(confirmationDialog);
    AtomicInteger discardCallCounter = new AtomicInteger();
    AtomicInteger keepEditingCallCounter = new AtomicInteger();

    for (String buttonFieldName : new String[] {"closeDialogButton", "confirmButton"}) {
      confirmationDialog.openDiscardChanges(
          discardCallCounter::incrementAndGet, keepEditingCallCounter::incrementAndGet);
      click(readConfirmationButton(buttonFieldName));
      click(readConfirmationButton("cancelButton"));
    }
    assertEquals(0, discardCallCounter.get());
    assertEquals(2, keepEditingCallCounter.get());

    confirmationDialog.openDiscardChanges(
        discardCallCounter::incrementAndGet, keepEditingCallCounter::incrementAndGet);
    click(readConfirmationButton("cancelButton"));
    click(readConfirmationButton("cancelButton"));
    click(readConfirmationButton("closeDialogButton"));

    assertEquals(1, discardCallCounter.get());
    assertEquals(2, keepEditingCallCounter.get());
    assertFalse(confirmationDialog.isOpened());
  }

  @Test
  void cancellingAndClosingNeverExecuteOrRetainTheAction() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    click(readConfirmationButton("cancelButton"));
    click(readConfirmationButton("confirmButton"));

    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    click(readConfirmationButton("closeDialogButton"));
    click(readConfirmationButton("confirmButton"));

    assertEquals(0, actionCallCounter.get());
    assertFalse(confirmationDialog.isOpened());
  }

  @Test
  void confirmingConsumesOnlyTheCurrentActionOnce() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(confirmationPrompt, () -> actionCallCounter.addAndGet(100));
    confirmationDialog.close();

    confirmationDialog.open(confirmationPrompt, actionCallCounter::incrementAndGet);
    click(readConfirmationButton("confirmButton"));
    click(readConfirmationButton("confirmButton"));

    assertEquals(1, actionCallCounter.get());
    assertFalse(confirmationDialog.isOpened());
  }

  @Test
  void aFailedActionCannotBeReplayed() {
    AtomicInteger actionCallCounter = new AtomicInteger();
    confirmationDialog.open(
        confirmationPrompt,
        () -> {
          actionCallCounter.incrementAndGet();
          throw new IllegalStateException("Test failure");
        });
    Button confirmActionButton = readConfirmationButton("confirmButton");

    assertThrows(IllegalStateException.class, () -> click(confirmActionButton));

    click(readConfirmationButton("confirmButton"));

    assertEquals(1, actionCallCounter.get());
    assertFalse(confirmationDialog.isOpened());
  }
}
