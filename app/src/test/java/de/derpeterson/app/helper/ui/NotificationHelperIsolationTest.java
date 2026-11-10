package de.derpeterson.app.helper.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.ui.components.PageFrameComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Echte Vaadin-Notifications in getrennten Fenstern, ohne Anwendung oder Datenbank. */
class NotificationHelperIsolationTest {
  private final List<UI> windows = new ArrayList<>();
  private final List<VaadinSession> sessions = new ArrayList<>();

  private VaadinSession session() {
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    sessions.add(vaadinSession);
    return vaadinSession;
  }

  private UI window(VaadinSession vaadinSession) {
    UI ui = new UI();
    ui.getInternals().setSession(vaadinSession);
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
    VaadinSession firstSession = session();
    UI first = window(firstSession);
    show("Hinweis des ersten Fensters");
    Notification firstNotice = notification(NotificationHelper.getInstance());
    UI second = window(sameSession ? firstSession : session());
    show("Hinweis des zweiten Fensters");
    Notification secondNotice = notification(NotificationHelper.getInstance());

    assertTrue(
        firstNotice.isOpened(), "Das andere Fenster darf den ersten Hinweis nicht schließen");
    assertTrue(secondNotice.isOpened());
    select(first);
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    assertFalse(firstNotice.isOpened());
    assertTrue(secondNotice.isOpened(), "Nur der eigene Hinweis wird geschlossen");
    select(second);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void notificationThemeFollowsTheActuallyAttachedPageFrame(boolean themeOnPortalRoot) {
    UI ui = window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    show("Ohne Seitenrahmen");

    assertFalse(notification(helper).getElement().getThemeList().contains("cosmos"));
    PageFrameComponent frame = new PageFrameComponent(new Div(), new Div(), new Div());
    Div wrapper = new Div(frame);
    // Public markiert den Rahmen selbst, Portal/Admin ihren übergeordneten View.
    (themeOnPortalRoot ? wrapper : frame).addClassName("cosmos-theme");
    ui.add(wrapper);
    show("Mit verschachteltem Seitenrahmen");

    assertTrue(notification(helper).getElement().getThemeList().contains("cosmos"));
    ui.remove(wrapper);
    show("Nach Entfernen des Seitenrahmens");

    assertFalse(notification(helper).getElement().getThemeList().contains("cosmos"));
  }

  @Test
  void languageRefreshKeepsOtherWindowsTextAndSuppliers() {
    UI first = window(session());
    AtomicReference<String> firstText = new AtomicReference<>("Erste Meldung DE");
    NotificationHelper firstHelper = NotificationHelper.getInstance();
    firstHelper.showNotification(
        null, firstText::get, 0, NotificationHelper.NotificationType.ERROR);
    window(session());
    AtomicReference<String> secondText = new AtomicReference<>("Second notice EN");
    NotificationHelper secondHelper = NotificationHelper.getInstance();
    secondHelper.showNotification(
        null, secondText::get, 0, NotificationHelper.NotificationType.ERROR);
    Html secondMessage = message(secondHelper);
    select(first);
    firstText.set("First notice EN");
    NotificationHelper.getInstance().updateText();

    assertTrue(
        message(NotificationHelper.getInstance()).getInnerHtml().contains("First notice EN"));
    assertTrue(secondMessage.getInnerHtml().contains("Second notice EN"));
    assertTrue(notification(secondHelper).isOpened());
  }

  @ParameterizedTest
  @EnumSource(NotificationHelper.NotificationType.class)
  void referencePresentationKeepsNativeLifetimeAndDoesNotLeakToAnotherWindow(
      NotificationHelper.NotificationType type) {
    window(session());
    Div referenceAccount = new Div();
    referenceAccount.addClassNames("cosmos-theme", "cosmos-reference-portal");
    UI.getCurrent().add(referenceAccount);
    NotificationHelper helper = NotificationHelper.getInstance();
    helper.showNotification(() -> "Title", () -> "Message", 4200, type);
    Notification referenceNotice = notification(helper);

    assertTrue(referenceNotice.getElement().getThemeList().contains("cosmos-reference"));
    assertEquals(4200, referenceNotice.getDuration());
    assertEquals(Notification.Position.TOP_CENTER, referenceNotice.getPosition());
    assertTrue(message(helper).getInnerHtml().contains("Message"));
    window(session());
    NotificationHelper.getInstance().showNotification(() -> "Other window", type);

    assertFalse(
        notification(NotificationHelper.getInstance())
            .getElement()
            .getThemeList()
            .contains("cosmos-reference"));
    assertTrue(referenceNotice.isOpened());
  }

  @Test
  void titleOnlySuccessKeepsItsTitleAndDoesNotCreateAnEmptyBody() {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    AtomicReference<String> title = new AtomicReference<>("Notifications refreshed.");
    helper.showNotification(title::get, null, 4200, NotificationHelper.NotificationType.SUCCESS);
    Notification notice = notification(helper);
    Span heading = (Span) ReflectionTestUtils.getField(helper, "titleText");

    assertEquals(title.get(), heading.getText());
    assertNull(message(helper));
    assertTrue(notice.isOpened());
    title.set("Benachrichtigungen aktualisiert.");
    helper.updateText();

    assertEquals(title.get(), heading.getText());
    assertNull(message(helper));
    assertSame(notice, notification(helper));
    assertEquals(4200, notice.getDuration());
  }

  @Test
  void optionalBodyCanAppearAndDisappearDuringSupplierUpdates() {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    AtomicReference<String> body = new AtomicReference<>(" ");
    helper.showNotification(
        () -> "Title", body::get, 4200, NotificationHelper.NotificationType.SUCCESS);
    Notification notice = notification(helper);

    assertNull(message(helper));
    body.set("Details");
    helper.updateText();

    assertTrue(message(helper).getInnerHtml().contains("Details"));
    body.set("");
    helper.updateText();

    assertNull(message(helper));
    assertSame(notice, notification(helper));
  }

  @Test
  void missingCurrentWindowCannotReuseAnExistingUsersHelper() {
    window(session());
    show("Privater Hinweis");
    Notification current = notification(NotificationHelper.getInstance());
    UI.setCurrent(null);

    assertThrows(IllegalStateException.class, NotificationHelper::getInstance);
    assertTrue(current.isOpened());
  }
}
