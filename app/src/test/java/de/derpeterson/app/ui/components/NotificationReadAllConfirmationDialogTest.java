package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationReadAllConfirmationDialogTest {
  private VaadinSession testVaadinSession;
  private UI testUi;
  private MessageProperties messageProperties;
  private NotificationReadAllConfirmationDialog readAllConfirmationDialog;

  @BeforeEach
  void configureConfirmationEnvironment() {
    testVaadinSession = mock(VaadinSession.class);
    when(testVaadinSession.hasLock()).thenReturn(true);
    when(testVaadinSession.getLocale()).thenReturn(Locale.GERMAN);
    VaadinSession.setCurrent(testVaadinSession);

    testUi = new UI();
    testUi.getInternals().setSession(testVaadinSession);
    UI.setCurrent(testUi);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    readAllConfirmationDialog =
        new NotificationReadAllConfirmationDialog(messageProperties, "read-all-test");
    testUi.add(readAllConfirmationDialog);
  }

  @AfterEach
  void clearConfirmationEnvironment() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @Test
  void nativeConfirmationExecutesPendingActionOnlyOnce() {
    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, confirmationCallCounter::incrementAndGet);
    assertEquals(0, confirmationCallCounter.get());

    fireReadAllConfirmationEvent();
    fireReadAllConfirmationEvent();

    assertEquals(1, confirmationCallCounter.get());
    assertFalse(readAllConfirmationDialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"close", "cancel", "header"})
  void dismissalDiscardsThePendingAction(String dismissalKind) {
    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, confirmationCallCounter::incrementAndGet);

    switch (dismissalKind) {
      case "cancel" ->
          ComponentUtil.fireEvent(
              readAllConfirmationDialog,
              new ConfirmDialog.CancelEvent(readAllConfirmationDialog, true));
      case "header" ->
          ((Button) ReflectionTestUtils.getField(readAllConfirmationDialog, "closeDialogButton"))
              .click();
      default -> readAllConfirmationDialog.close();
    }
    fireReadAllConfirmationEvent();

    assertEquals(0, confirmationCallCounter.get());
    assertFalse(readAllConfirmationDialog.isOpened());
  }

  @Test
  void nativeOpenedSynchronizationKeepsTheFollowingConfirmEventValid() {
    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, confirmationCallCounter::incrementAndGet);

    // Der Browser synchronisiert den geschlossenen Zustand vor dem Bestätigungsereignis.
    readAllConfirmationDialog.setOpened(false);
    fireReadAllConfirmationEvent();

    assertEquals(1, confirmationCallCounter.get());
  }

  @Test
  void completedClientClosureDiscardsAnUnconfirmedAction() {
    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, confirmationCallCounter::incrementAndGet);

    readAllConfirmationDialog.setOpened(false);
    ComponentUtil.fireEvent(
        readAllConfirmationDialog, new ConfirmDialog.ClosedEvent(readAllConfirmationDialog, true));
    fireReadAllConfirmationEvent();

    assertEquals(0, confirmationCallCounter.get());
  }

  @Test
  void failedActionClosesAndAllowsAnotherExplicitConfirmation() {
    readAllConfirmationDialog.open(
        2,
        () -> {
          throw new IllegalStateException("test failure");
        });
    assertThrows(IllegalStateException.class, this::fireReadAllConfirmationEvent);
    assertFalse(readAllConfirmationDialog.isOpened());

    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(1, confirmationCallCounter::incrementAndGet);
    assertTrue(
        ((Button) ReflectionTestUtils.getField(readAllConfirmationDialog, "confirmReadAllButton"))
            .isEnabled());
    fireReadAllConfirmationEvent();
    assertEquals(1, confirmationCallCounter.get());
  }

  @Test
  void lateClosedEventDoesNotDiscardAReopenedConfirmation() {
    AtomicInteger originalConfirmationCallCounter = new AtomicInteger();
    AtomicInteger reopenedConfirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, originalConfirmationCallCounter::incrementAndGet);
    readAllConfirmationDialog.close();
    readAllConfirmationDialog.open(1, reopenedConfirmationCallCounter::incrementAndGet);

    // Das ClosedEvent der vorherigen Schließanimation kann nach dem erneuten Öffnen ankommen.
    ComponentUtil.fireEvent(
        readAllConfirmationDialog, new ConfirmDialog.ClosedEvent(readAllConfirmationDialog, true));
    fireReadAllConfirmationEvent();

    assertEquals(0, originalConfirmationCallCounter.get());
    assertEquals(1, reopenedConfirmationCallCounter.get());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageRefreshPreservesThePendingConfirmation(String languageTag) {
    AtomicInteger confirmationCallCounter = new AtomicInteger();
    readAllConfirmationDialog.open(2, confirmationCallCounter::incrementAndGet);
    when(testVaadinSession.getLocale()).thenReturn(Locale.forLanguageTag(languageTag));
    readAllConfirmationDialog.refreshTexts();

    assertEquals(
        messageProperties.getNotificationsMarkAll(),
        ((H2) ReflectionTestUtils.getField(readAllConfirmationDialog, "dialogTitleHeading"))
            .getText());
    assertEquals(
        messageProperties.getPortalMarkAllExplanation(),
        ((Span)
                ReflectionTestUtils.getField(
                    readAllConfirmationDialog, "confirmationExplanationSpan"))
            .getText());
    assertEquals(
        messageProperties.getAdminUsersCancel(),
        ((Button)
                ReflectionTestUtils.getField(readAllConfirmationDialog, "cancelConfirmationButton"))
            .getText());
    assertTrue(readAllConfirmationDialog.isOpened());
    assertEquals(0, confirmationCallCounter.get());

    fireReadAllConfirmationEvent();
    assertEquals(1, confirmationCallCounter.get());
  }

  private void fireReadAllConfirmationEvent() {
    ComponentUtil.fireEvent(
        readAllConfirmationDialog, new ConfirmDialog.ConfirmEvent(readAllConfirmationDialog, true));
  }
}
