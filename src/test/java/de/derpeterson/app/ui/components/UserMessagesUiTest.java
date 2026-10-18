package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.server.*;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.enums.UserMessageType;
import de.derpeterson.app.service.UserMessageService;
import de.derpeterson.app.service.UserMessageService.*;
import de.derpeterson.app.views.admin.AdminUserMessagesSection;
import de.derpeterson.app.websocket.UserMessageBroadcaster;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Java-Komponentenprüfung ohne Browser; sichtbare Geometrie/Frontend bleiben getrennt zu prüfen.
 */
class UserMessagesUiTest {
  UI ui;
  MessageProperties texts;
  UserMessageService service;
  MockHttpSession http;
  MockHttpServletRequest request;
  AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  UserMessageBroadcaster events;
  Item item =
      new Item(
          7L,
          UserMessageType.PASSWORD_CHANGED,
          null,
          null,
          Instant.parse("2026-10-18T10:00:00Z"),
          false);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(c -> locale.get());
    when(session.access(any()))
        .thenAnswer(
            c -> {
              ((Command) c.getArgument(0)).execute();
              return CompletableFuture.completedFuture(null);
            });
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    texts = new MessageProperties(new CustomI18NProvider());
    service = mock(UserMessageService.class);
    http = new MockHttpSession();
    request = new MockHttpServletRequest();
    request.setSession(http);
    events = new UserMessageBroadcaster();
    when(service.events()).thenReturn(events);
    when(service.retentionDays()).thenReturn(180);
    when(service.ownerId(http)).thenReturn(5L);
    when(service.unreadCount(http)).thenReturn(1L);
    when(service.unreadSnapshot(http)).thenReturn(List.of(7L));
    when(service.inbox(eq(http), anyBoolean(), anyInt(), anyInt()))
        .thenAnswer(
            c ->
                new PageImpl<>(
                    List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 1));
  }

  @AfterEach
  void cleanup() {
    ui.getChildren().toList().forEach(ui::remove);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @SuppressWarnings("unchecked")
  <T> T field(Object component, String name) {
    return (T) ReflectionTestUtils.getField(component, name);
  }

  Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  Button rowTitle(UserMessagesPanel panel) {
    return tree(panel)
        .filter(c -> c.hasClassName("cosmos-message-title"))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void refreshOpeningAndLanguageNeverMarkMessagesRead(String language) {
    locale.set(Locale.forLanguageTag(language));
    var panel = new UserMessagesPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    assertEquals(texts.getMessagesPasswordChangedTitle(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(s -> s.getText().equals(texts.getMessagesRetention(180))));
    panel.refreshTexts();
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void openingOneItemMarksOnlyThatItemAndUsesExistingDialogTheme() {
    var panel = new UserMessagesPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    verify(service).markRead(http, List.of(7L));
    Dialog detail = field(panel, "detail");
    assertTrue(detail.isOpened());
    assertTrue(detail.getThemeNames().contains("cosmos"));
    assertTrue(detail.isCloseOnOutsideClick());
    assertTrue(detail.isCloseOnEsc());
    assertTrue(tree(detail).noneMatch(Html.class::isInstance));
  }

  @Test
  void manualTextIsNotTranslatedOrParsedAsHtml() {
    item =
        new Item(
            7L,
            UserMessageType.ADMIN_MESSAGE,
            "<b>Titel</b>",
            "<img src=x onerror=alert(1)>\nText",
            Instant.now(),
            false);
    var panel = new UserMessagesPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    assertEquals(item.title(), rowTitle(panel).getText());
    rowTitle(panel).click();
    Dialog detail = field(panel, "detail");
    assertTrue(
        tree(detail)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(d -> d.getText().equals(item.body())));
    assertTrue(tree(detail).noneMatch(c -> c.getElement().getTag().equals("img")));
  }

  @Test
  void markAllSubmitsDisplayedSnapshotAndUnreadFilterResetsPage() {
    var panel = new UserMessagesPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    ((Button) field(panel, "markAll")).click();
    verify(service).markRead(http, List.of(7L));
    ((Checkbox) field(panel, "unread")).setValue(true);
    verify(service).inbox(http, true, 0, 20);
  }

  @Test
  void paginatedInboxRequestsNextPageAndBack() {
    when(service.inbox(eq(http), anyBoolean(), anyInt(), anyInt()))
        .thenAnswer(
            c ->
                new PageImpl<>(
                    List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 30));
    var panel = new UserMessagesPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    ((Button) field(panel, "next")).click();
    verify(service).inbox(http, false, 1, 20);
    ((Button) field(panel, "previous")).click();
    verify(service, times(2)).inbox(http, false, 0, 20);
  }

  @Test
  void revokedSessionClearsContentsAndOpenDetail() {
    var panel = new UserMessagesPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    Dialog detail = field(panel, "detail");
    when(service.inbox(eq(http), anyBoolean(), anyInt(), anyInt()))
        .thenThrow(new AccessDeniedException("expired"));
    panel.refresh();
    assertFalse(detail.isOpened());
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-message-title")));
    assertFalse(((Button) field(panel, "markAll")).isEnabled());
  }

  @Test
  void liveBellCapsBadgeAndRefreshesOnlyItsOwnUserAndDetachesCleanly() {
    when(service.unreadCount(http)).thenReturn(125L);
    var button = new Button();
    var bell = new UserMessageBell(texts, service, request, button);
    ui.add(bell);
    Span count = field(bell, "count");
    assertEquals("99+", count.getText());
    assertTrue(count.isVisible());
    assertTrue(button.getAriaLabel().orElseThrow().contains("125"));
    clearInvocations(service);
    events.publish(Set.of(999L));
    verify(service, never()).unreadCount(any());
    events.publish(Set.of(5L));
    verify(service).unreadCount(http);
    ui.remove(bell);
    clearInvocations(service);
    events.publish(Set.of(5L));
    verify(service, never()).unreadCount(any());
  }

  @Test
  void bellOpensOnClickAndClosesWithEscapeOrOutsideWithoutMarkingAllRead() {
    var bell = new UserMessageBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    assertTrue(popover.isOpenOnClick());
    assertFalse(popover.isOpenOnFocus());
    assertFalse(popover.isOpenOnHover());
    assertTrue(popover.isCloseOnEsc());
    assertTrue(popover.isCloseOnOutsideClick());
    popover.open();
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void bellDoesNotReusePreviousCountOrContentsWhenSessionRevoked() {
    var button = new Button();
    var bell = new UserMessageBell(texts, service, request, button);
    ui.add(bell);
    when(service.unreadCount(http)).thenThrow(new AccessDeniedException("expired"));
    events.publish(Set.of(5L));
    assertFalse(button.isEnabled());
    assertFalse(((Span) field(bell, "count")).isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void systemTypesRenderInCurrentLanguageAndManualBodyStaysOriginal(String language) {
    locale.set(Locale.forLanguageTag(language));
    for (var type : UserMessageType.values()) {
      var message = new Item(1L, type, "Original title", "Original body", Instant.now(), false);
      assertFalse(UserMessageTexts.title(texts, message).isBlank());
      assertFalse(UserMessageTexts.body(texts, message).isBlank());
      if (type == UserMessageType.ADMIN_MESSAGE)
        assertEquals("Original body", UserMessageTexts.body(texts, message));
    }
    assertEquals(
        language.equals("de") ? "Passwort geändert" : "Password changed",
        UserMessageTexts.title(texts, item));
  }

  @Test
  void springWiresTheSharedBellForAuthenticatedViewsOnly() {
    var security = mock(de.derpeterson.app.security.SecurityService.class);
    var users = mock(de.derpeterson.app.service.UserService.class);
    var testAvatars = mock(de.derpeterson.app.service.UserAvatarService.class);
    var status = new de.derpeterson.app.websocket.UserStatusBroadcaster();
    var identity =
        de.derpeterson.app.model.UserEntity.builder()
            .id(5L)
            .firstName("Own")
            .lastName("User")
            .enabled(true)
            .build();
    when(security.getCurrentUser(request)).thenReturn(Optional.of(identity));
    class HeaderView extends de.derpeterson.app.ui.base.TrackedUserAppLayout {
      HeaderView() {
        super(texts, security, users, status, UserMessagesUiTest.this.request, testAvatars);
      }
    }
    try (var context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.registerBean(UserMessageService.class, () -> service);
      context.refresh();
      var authenticated = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(authenticated);
      AccountHeaderActions actions = field(authenticated, "accountHeaderActions");
      assertTrue(tree(actions).anyMatch(UserMessageBell.class::isInstance));
      ui.add(actions);
      verify(service).ownerId(http);
      when(security.getCurrentUser(request)).thenReturn(Optional.empty());
      var anonymous = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(anonymous);
      AccountHeaderActions hidden = field(anonymous, "accountHeaderActions");
      assertFalse(hidden.isVisible());
      assertTrue(tree(hidden).noneMatch(UserMessageBell.class::isInstance));
    }
  }

  AdminUserMessagesSection admin() {
    when(service.targets(http)).thenReturn(List.of(new Target(5L, "Own", "own@example.test")));
    when(service.sent(http, 0)).thenReturn(Page.empty());
    var section = new AdminUserMessagesSection(texts, service, request);
    section.activate();
    return section;
  }

  @Test
  void adminRequiresValidFieldsAndFreshPreviewBeforeSend() {
    var section = admin();
    ((Button) field(section, "previewButton")).click();
    assertTrue(((TextField) field(section, "title")).isInvalid());
    assertTrue(((TextArea) field(section, "body")).isInvalid());
    verify(service, never()).preview(any(), any());
    assertFalse(((Button) field(section, "send")).isEnabled());
    var preview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(service.preview(http, preview.selection())).thenReturn(preview);
    ((TextField) field(section, "title")).setValue("Title");
    ((TextArea) field(section, "body")).setValue("Body");
    ((Checkbox) field(section, "everyone")).setValue(true);
    ((Button) field(section, "previewButton")).click();
    assertTrue(((Button) field(section, "send")).isEnabled());
    ((TextField) field(section, "title")).setValue("Changed");
    assertFalse(((Button) field(section, "send")).isEnabled());
  }

  @Test
  void adminSendUsesSameRequestAfterAmbiguousFailureAndClearsOnlyAfterSuccess() {
    var section = admin();
    var preview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(service.preview(http, preview.selection())).thenReturn(preview);
    ((TextField) field(section, "title")).setValue("Title");
    ((TextArea) field(section, "body")).setValue("Body");
    ((Checkbox) field(section, "everyone")).setValue(true);
    ((Button) field(section, "previewButton")).click();
    UUID key = field(section, "requestId");
    when(service.send(http, key, "Title", "Body", preview))
        .thenThrow(new IllegalStateException("response interrupted"))
        .thenReturn(new Sent(1L, "Title", "Body", Instant.now(), 1));
    ((Button) field(section, "send")).click();
    assertEquals(key, (UUID) field(section, "requestId"));
    assertEquals("Title", ((TextField) field(section, "title")).getValue());
    ((Button) field(section, "send")).click();
    verify(service, times(2)).send(http, key, "Title", "Body", preview);
    assertTrue(((TextField) field(section, "title")).isEmpty());
    assertFalse(((Button) field(section, "send")).isEnabled());
  }
}
