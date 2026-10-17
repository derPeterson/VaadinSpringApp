package de.derpeterson.app.helper.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Echte Vaadin-Notifications in getrennten Fenstern, ohne Anwendung oder Datenbank. */
class NotificationHelperIsolationTest {
  private final List<UI> windows = new ArrayList<>();
  private final List<VaadinSession> sessions = new ArrayList<>();

  private VaadinSession session() {
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    sessions.add(session);
    return session;
  }

  private UI window(VaadinSession session) {
    var ui = new UI();
    ui.getInternals().setSession(session);
    windows.add(ui);
    select(ui);
    return ui;
  }

  private void select(UI ui) {
    UI.setCurrent(ui);
    VaadinSession.setCurrent(ui.getSession());
  }

  private void show(String text) {
    NotificationHelper.getInstance()
        .showNotification(() -> text, () -> text, 0, NotificationHelper.NotificationType.ERROR);
  }

  private Notification notification(NotificationHelper helper) {
    return (Notification) ReflectionTestUtils.getField(helper, "currentNotification");
  }

  private Html message(NotificationHelper helper) {
    return (Html) ReflectionTestUtils.getField(helper, "messageText");
  }

  @AfterEach
  void cleanup() {
    for (UI window : windows) {
      select(window);
      NotificationHelper.getInstance().closeAndClearAllNotifications();
    }
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void anotherWindowCannotCloseTheExistingNotice(boolean sameSession) {
    var firstSession = session();
    var first = window(firstSession);
    show("Hinweis des ersten Fensters");
    var firstNotice = notification(NotificationHelper.getInstance());
    var second = window(sameSession ? firstSession : session());
    show("Hinweis des zweiten Fensters");
    var secondNotice = notification(NotificationHelper.getInstance());
    assertTrue(
        firstNotice.isOpened(), "Das andere Fenster darf den ersten Hinweis nicht schließen");
    assertTrue(secondNotice.isOpened());
    select(first);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    assertFalse(firstNotice.isOpened());
    assertTrue(secondNotice.isOpened(), "Nur der eigene Hinweis wird geschlossen");
    select(second);
  }

  @Test
  void languageRefreshKeepsOtherWindowsTextAndSuppliers() {
    var first = window(session());
    var firstText = new AtomicReference<>("Erste Meldung DE");
    var firstHelper = NotificationHelper.getInstance();
    firstHelper.showNotification(
        null, firstText::get, 0, NotificationHelper.NotificationType.ERROR);
    window(session());
    var secondText = new AtomicReference<>("Second notice EN");
    var secondHelper = NotificationHelper.getInstance();
    secondHelper.showNotification(
        null, secondText::get, 0, NotificationHelper.NotificationType.ERROR);
    var secondMessage = message(secondHelper);
    select(first);
    firstText.set("First notice EN");
    NotificationHelper.getInstance().updateText();
    assertTrue(
        message(NotificationHelper.getInstance()).getInnerHtml().contains("First notice EN"));
    assertTrue(secondMessage.getInnerHtml().contains("Second notice EN"));
    assertTrue(notification(secondHelper).isOpened());
  }

  @Test
  void missingCurrentWindowCannotReuseAnExistingUsersHelper() {
    window(session());
    show("Privater Hinweis");
    var current = notification(NotificationHelper.getInstance());
    UI.setCurrent(null);
    assertThrows(IllegalStateException.class, NotificationHelper::getInstance);
    assertTrue(current.isOpened());
  }
}
