package de.derpeterson.app.helper.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.ui.components.PageFrameComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
        .showTextNotification(() -> text, () -> text, 0, NotificationHelper.NotificationType.ERROR);
  }

  private Notification notification(NotificationHelper helper) {
    return (Notification) ReflectionTestUtils.getField(helper, "currentNotification");
  }

  private Paragraph message(NotificationHelper helper) {
    return (Paragraph) ReflectionTestUtils.getField(helper, "messageText");
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
    firstHelper.showTextNotification(
        null, firstText::get, 0, NotificationHelper.NotificationType.ERROR);
    window(session());
    AtomicReference<String> secondText = new AtomicReference<>("Second notice EN");
    NotificationHelper secondHelper = NotificationHelper.getInstance();
    secondHelper.showTextNotification(
        null, secondText::get, 0, NotificationHelper.NotificationType.ERROR);
    Paragraph secondMessage = message(secondHelper);
    select(first);
    firstText.set("First notice EN");
    NotificationHelper.getInstance().updateText();

    assertTrue(message(NotificationHelper.getInstance()).getText().contains("First notice EN"));
    assertTrue(secondMessage.getText().contains("Second notice EN"));
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
    helper.showTextNotification(() -> "Title", () -> "Message", 4200, type);
    Notification referenceNotice = notification(helper);

    assertTrue(referenceNotice.getElement().getThemeList().contains("cosmos-reference"));
    assertEquals(4200, referenceNotice.getDuration());
    assertEquals(Notification.Position.TOP_CENTER, referenceNotice.getPosition());
    assertTrue(message(helper).getText().contains("Message"));
    window(session());
    NotificationHelper.getInstance().showTextNotification(() -> "Other window", type);

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
    helper.showTextNotification(
        title::get, null, 4200, NotificationHelper.NotificationType.SUCCESS);
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
    helper.showTextNotification(
        () -> "Title", body::get, 4200, NotificationHelper.NotificationType.SUCCESS);
    Notification notice = notification(helper);

    assertNull(message(helper));
    body.set("Details");
    helper.updateText();

    assertTrue(message(helper).getText().contains("Details"));
    body.set("");
    helper.updateText();

    assertNull(message(helper));
    assertSame(notice, notification(helper));
  }

  @Test
  void nativeTextKeepsMarkupLiteralAndRefreshesWithoutChangingLifetimeOrOtherWindows() {
    UI textWindow = window(session());
    NotificationHelper textHelper = NotificationHelper.getInstance();
    AtomicReference<String> messageText = new AtomicReference<>("Gespeichert: <b>Alex & Chris</b>");
    textHelper.showTextNotification(
        () -> "Erfolgreich", messageText::get, NotificationHelper.NotificationType.SUCCESS);
    Notification textNotification = notification(textHelper);
    Paragraph messageParagraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(textHelper, "messageText"));

    assertEquals(messageText.get(), messageParagraph.getText());
    assertEquals(0, messageParagraph.getChildren().count());
    assertEquals(3000, textNotification.getDuration());
    assertEquals(Notification.Position.TOP_CENTER, textNotification.getPosition());
    window(session());
    show("Hinweis im anderen Fenster");
    Notification otherNotification = notification(NotificationHelper.getInstance());
    select(textWindow);
    messageText.set("Saved: <b>Alex & Chris</b>");
    textHelper.updateText();
    messageParagraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(textHelper, "messageText"));

    assertEquals(messageText.get(), messageParagraph.getText());
    assertSame(textNotification, notification(textHelper));
    assertEquals(3000, textNotification.getDuration());
    Button closeButton = (Button) ReflectionTestUtils.getField(textHelper, "closeButton");
    closeButton.click();

    assertFalse(textNotification.isOpened());
    assertTrue(otherNotification.isOpened());
    textHelper.showTextNotification(() -> "<strong>Formatierte Meldung</strong>");

    assertEquals("<strong>Formatierte Meldung</strong>", message(textHelper).getText());
    assertEquals(0, message(textHelper).getChildren().count());
    assertEquals(3000, notification(textHelper).getDuration());
    assertNull(ReflectionTestUtils.getField(textHelper, "titleText"));
  }

  @Test
  void nativeTextCanAppearAndDisappearWhileTheNoticeStaysOpen() {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    AtomicReference<String> messageText = new AtomicReference<>(" ");
    helper.showTextNotification(
        () -> "Title", messageText::get, NotificationHelper.NotificationType.SUCCESS);
    Notification notice = notification(helper);

    assertNull(ReflectionTestUtils.getField(helper, "messageText"));
    messageText.set("Saved");
    helper.updateText();
    Paragraph messageParagraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));

    assertEquals("Saved", messageParagraph.getText());
    messageText.set("");
    helper.updateText();

    assertNull(ReflectionTestUtils.getField(helper, "messageText"));
    assertSame(notice, notification(helper));
    assertTrue(notice.isOpened());
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

  @ParameterizedTest
  @EnumSource(NotificationHelper.NotificationType.class)
  void nativeTextUsesEverySeverityAndPreservesExplicitLifetime(
      NotificationHelper.NotificationType type) {
    window(session());
    Div frame = new Div();
    frame.addClassNames("cosmos-theme", "cosmos-reference-portal");
    UI.getCurrent().add(frame);
    NotificationHelper helper = NotificationHelper.getInstance();
    helper.showTextNotification(() -> "Title", () -> "Text <>&", -1, type);

    Paragraph paragraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    assertEquals("Text <>&", paragraph.getText());
    assertEquals(-1, notification(helper).getDuration());
    assertTrue(
        notification(helper)
            .getElement()
            .getThemeList()
            .contains("cosmos-reference-" + type.name().toLowerCase(Locale.ROOT)));
  }

  @Test
  void nativeDetailsRefreshTheirTextAndKeepTheSamePermanentNotice() {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    AtomicReference<String> detail = new AtomicReference<>("Feld DE <>&");
    helper.showTextNotificationWithDetails(
        () -> "Saved",
        () -> "Restart required",
        () -> List.of(detail.get()),
        0,
        NotificationHelper.NotificationType.WARNING);
    Notification notice = notification(helper);
    assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    VerticalLayout layout = (VerticalLayout) ReflectionTestUtils.getField(helper, "textLayout");
    assertTrue(layout.getElement().getTextRecursively().contains(detail.get()));
    detail.set("Field EN <>&");
    helper.updateText();
    assertTrue(layout.getElement().getTextRecursively().contains(detail.get()));
    assertFalse(layout.getElement().getTextRecursively().contains("Feld DE"));
    assertSame(notice, notification(helper));
    assertEquals(0, notice.getDuration());
  }

  @Test
  void highlightedTextStaysLiteralAndFollowsLanguageAndArgumentUpdates() {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    AtomicReference<String> message = new AtomicReference<>("E-Mail an <Chris & Alex> gesendet.");
    AtomicReference<String> highlighted = new AtomicReference<>("<Chris & Alex>");
    helper.showHighlightedTextNotification(
        () -> "Success",
        message::get,
        highlighted::get,
        -1,
        NotificationHelper.NotificationType.SUCCESS);
    Notification notice = notification(helper);
    Paragraph paragraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    assertEquals(message.get(), paragraph.getElement().getTextRecursively());
    Span emphasis =
        paragraph
            .getChildren()
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(highlighted.get(), emphasis.getText());
    assertTrue(emphasis.hasClassName("text-primary"));
    assertTrue(emphasis.hasClassName(LumoUtility.FontWeight.SEMIBOLD));
    highlighted.set("<Updated & Chris>");
    message.set("Sent to <Updated & Chris>.");
    helper.updateText();
    paragraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    assertEquals(message.get(), paragraph.getElement().getTextRecursively());
    assertSame(notice, notification(helper));
    assertEquals(-1, notice.getDuration());
    helper.showTextNotification(
        () -> "Next", () -> "Plain", NotificationHelper.NotificationType.NORMAL);
    paragraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    assertEquals("Plain", paragraph.getText());
    assertEquals(0, paragraph.getChildren().count());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"missing"})
  void refreshedHighlightCanDisappearAndReturnWithoutReplacingTheNotice(
      String absentHighlightText) {
    window(session());
    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    String completeMessageText = "E-Mail an <Chris & Alex> gesendet.";
    AtomicReference<String> highlightedTextReference = new AtomicReference<>("<Chris & Alex>");
    notificationHelper.showHighlightedTextNotification(
        () -> "Success",
        () -> completeMessageText,
        highlightedTextReference::get,
        -1,
        NotificationHelper.NotificationType.SUCCESS);
    Notification originalNotification = notification(notificationHelper);
    assertEquals(
        1, message(notificationHelper).getChildren().filter(Span.class::isInstance).count());

    highlightedTextReference.set(absentHighlightText);
    notificationHelper.updateText();

    Paragraph messageParagraph = message(notificationHelper);
    assertEquals(completeMessageText, messageParagraph.getText());
    assertEquals(0, messageParagraph.getChildren().count());
    assertSame(originalNotification, notification(notificationHelper));
    assertTrue(originalNotification.isOpened());
    assertEquals(-1, originalNotification.getDuration());

    highlightedTextReference.set("<Chris & Alex>");
    notificationHelper.updateText();

    messageParagraph = message(notificationHelper);
    assertEquals(completeMessageText, messageParagraph.getElement().getTextRecursively());
    Span highlightedSpan =
        messageParagraph
            .getChildren()
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(highlightedTextReference.get(), highlightedSpan.getText());
    assertSame(originalNotification, notification(notificationHelper));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"missing"})
  void absentHighlightKeepsTheCompleteLiteralMessage(String highlightedText) {
    window(session());
    NotificationHelper helper = NotificationHelper.getInstance();
    helper.showHighlightedTextNotification(
        () -> "Title",
        () -> "Literal <>&",
        () -> highlightedText,
        -1,
        NotificationHelper.NotificationType.SUCCESS);

    Paragraph messageParagraph =
        assertInstanceOf(Paragraph.class, ReflectionTestUtils.getField(helper, "messageText"));
    assertEquals("Literal <>&", messageParagraph.getText());
    assertEquals(0, messageParagraph.getChildren().count());
  }
}
