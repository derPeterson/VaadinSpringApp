package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.server.*;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.*;
import de.derpeterson.app.views.admin.AdminUserNotificationsSection;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Java-Komponentenprüfung ohne Browser; sichtbare Geometrie/Frontend bleiben getrennt zu prüfen.
 */
class UserNotificationsUiTest {
  UI ui;
  MessageProperties texts;
  UserNotificationService service;
  MockHttpSession http;
  MockHttpServletRequest request;
  AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  UserNotificationBroadcaster events;
  Item item =
      new Item(
          7L,
          UserNotificationType.PASSWORD_CHANGED,
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
    service = mock(UserNotificationService.class);
    http = new MockHttpSession();
    request = new MockHttpServletRequest();
    request.setSession(http);
    events = new UserNotificationBroadcaster();
    when(service.events()).thenReturn(events);
    when(service.retentionDays()).thenReturn(OptionalInt.of(180));
    when(service.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
    when(service.ownerId(http)).thenReturn(5L);
    when(service.unreadCount(http)).thenReturn(1L);
    when(service.unreadSnapshot(http)).thenReturn(List.of(7L));
    when(service.inbox(eq(http), anyBoolean(), anyInt(), anyInt()))
        .thenAnswer(
            c ->
                new PageImpl<>(
                    List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 1));
    when(service.inbox(eq(http), any(ReadFilter.class), anyInt(), anyInt()))
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
    var children = component.getChildren();
    // Vaadin verwaltet Header und Footer als separate Dialogbereiche.
    if (component instanceof Dialog dialog) {
      children =
          Stream.concat(
              children,
              Stream.concat(
                      dialog.getHeader().getElement().getChildren(),
                      dialog.getFooter().getElement().getChildren())
                  .flatMap(element -> element.getComponent().stream()));
    }
    return Stream.concat(Stream.of(component), children.distinct().flatMap(this::tree));
  }

  Button rowTitle(UserNotificationsPanel panel) {
    return tree(panel)
        .filter(c -> c.hasClassName("cosmos-notification-title"))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void refreshOpeningAndLanguageNeverMarkNotificationsRead(String language) {
    locale.set(Locale.forLanguageTag(language));
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    assertEquals(texts.getNotificationsPasswordChangedTitle(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(s -> s.getText().equals(texts.getNotificationsRetention(180))));
    panel.refreshTexts();
    verify(service, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void headerPreviewUsesAccountHeaderAndClosesExplicitlyWithoutReading(String language) {
    locale.set(Locale.forLanguageTag(language));
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    assertTrue(popover.getThemeNames().contains("cosmos-notifications"));
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    assertEquals(
        texts.getNotificationsDescription(), ((Span) field(panel, "description")).getText());
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    assertEquals(ReadFilter.ALL, filter.getValue());
    assertEquals(texts.getNotificationsAll(), filter.getItemLabelGenerator().apply(ReadFilter.ALL));
    assertEquals(
        texts.getNotificationsRead(), filter.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        texts.getNotificationsUnread(), filter.getItemLabelGenerator().apply(ReadFilter.UNREAD));
    assertEquals(texts.getNotificationsFilter(), filter.getAriaLabel().orElseThrow());
    assertTrue(tree(panel).noneMatch(Checkbox.class::isInstance));
    Span previewHint = field(panel, "previewHint");
    assertEquals(texts.getNotificationsLatest(5), previewHint.getText());
    filter.setValue(ReadFilter.READ);
    assertEquals(texts.getNotificationsLatestRead(5), previewHint.getText());
    filter.setValue(ReadFilter.UNREAD);
    assertEquals(texts.getNotificationsLatestUnread(5), previewHint.getText());
    filter.setValue(ReadFilter.ALL);
    assertEquals(texts.getNotificationsLatest(5), previewHint.getText());
    Button close = field(panel, "close");
    assertEquals(texts.getNotificationsClose(), close.getAriaLabel().orElseThrow());
    assertTrue(close.hasClassName("cosmos-overlay-close"));
    close.click();
    assertFalse(popover.isOpened());
    verify(service, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void segmentedReadFilterQueriesEachStateResetsPageAndSurvivesLanguageChange(String language) {
    locale.set(Locale.forLanguageTag(language));
    var readItem = new Item(8L, item.type(), item.title(), item.body(), item.createdAt(), true);
    when(service.inbox(eq(http), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(
            c -> {
              ReadFilter filter = c.getArgument(1);
              var items =
                  switch (filter) {
                    case ALL -> List.of(readItem, item);
                    case READ -> List.of(readItem);
                    case UNREAD -> List.of(item);
                  };
              return new PageImpl<>(
                  items, PageRequest.of(c.getArgument(2), c.getArgument(3)), items.size());
            });
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    assertEquals(
        2, tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).count());
    ReflectionTestUtils.setField(panel, "page", 2);
    filter.setValue(ReadFilter.READ);
    verify(service).inbox(http, ReadFilter.READ, 0, 5);
    assertEquals(0, (int) field(panel, "page"));
    var rows = tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).toList();
    assertEquals(1, rows.size());
    assertEquals("8", rows.getFirst().getElement().getAttribute("data-notification-id"));
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals(ReadFilter.READ, filter.getValue());
    assertEquals(
        texts.getNotificationsRead(), filter.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        texts.getNotificationsLatestRead(5), ((Span) field(panel, "previewHint")).getText());
    filter.setValue(ReadFilter.UNREAD);
    verify(service).inbox(http, ReadFilter.UNREAD, 0, 5);
    rows = tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).toList();
    assertEquals(1, rows.size());
    assertEquals("7", rows.getFirst().getElement().getAttribute("data-notification-id"));
    assertTrue(rows.getFirst().hasClassName("cosmos-notification-row--unread"));
    filter.setValue(ReadFilter.ALL);
    assertEquals(
        2, tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).count());
    verify(service, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void compactPreviewSupportsSafeTextAndIndividualReadWithoutOpening(String language) {
    locale.set(Locale.forLanguageTag(language));
    item =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "<b>Title</b>",
            "<img src=x onerror=alert(1)>\nSecond line",
            Instant.now(),
            false);
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    assertEquals(item.title(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(
                div ->
                    div.hasClassName("cosmos-notification-preview-message")
                        && div.getText().equals(item.body())));
    assertTrue(tree(panel).noneMatch(Html.class::isInstance));
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span -> span.getText().equals(texts.getNotificationsSourceAdministration())));
    Button read =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.hasClassName("cosmos-notification-preview-mark-read"))
            .findFirst()
            .orElseThrow();
    assertTrue(read.isVisible());
    read.click();
    verify(service).markRead(http, List.of(7L));
    assertNull(field(panel, "detail"));
    ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).setValue(ReadFilter.UNREAD);
    verify(service).inbox(http, ReadFilter.UNREAD, 0, 5);
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
    panel.refresh();
    assertTrue(
        tree(panel)
            .noneMatch(component -> component.hasClassName("cosmos-notification-row--unread")));
    assertTrue(
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.hasClassName("cosmos-notification-preview-mark-read"))
            .noneMatch(Button::isVisible));
  }

  @Test
  void openingCompactItemKeepsPopoverWithoutMarkingRead() {
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    rowTitle(panel).click();
    assertTrue(popover.isOpened());
    Dialog detail = field(panel, "detail");
    assertTrue(detail.isOpened());
    assertFalse(popover.isCloseOnOutsideClick());
    assertFalse(popover.isCloseOnEsc());
    detail.close();
    assertTrue(popover.isOpened());
    assertTrue(popover.isCloseOnOutsideClick());
    assertTrue(popover.isCloseOnEsc());
    verify(service, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void doubleClickOnPreviewCardOpensOnlyItsMessageAndIgnoresButtonEvents(
      String language, boolean read) {
    locale.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    var row =
        tree(panel)
            .filter(c -> c.hasClassName("cosmos-notification-preview-row"))
            .findFirst()
            .orElseThrow();
    var element = row.getElement();
    var listeners =
        element.getNode().getFeature(com.vaadin.flow.internal.nodefeature.ElementListenerMap.class);
    var data = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "click", data));
    assertNull(field(panel, "detail"));
    verify(service, never()).markRead(any(), any());
    var expressions = listeners.getExpressions("dblclick");
    assertFalse(expressions.isEmpty());
    expressions.forEach(expression -> data.put(expression, false));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "dblclick", data));
    assertNull(field(panel, "detail"));
    verify(service, never()).markRead(any(), any());
    expressions.forEach(expression -> data.put(expression, true));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "dblclick", data));
    assertTrue(popover.isOpened());
    assertTrue(((Dialog) field(panel, "detail")).isOpened());
    verify(service, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,false,title", "en,false,title", "de,true,title", "en,true,title",
    "de,false,open", "en,false,open", "de,true,open", "en,true,open"
  })
  void titleAndOpenButtonUseSingleClickForBothReadStates(
      String language, boolean read, String target) {
    locale.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    Button button =
        target.equals("title")
            ? rowTitle(panel)
            : tree(panel)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(b -> b.getText().equals(texts.getNotificationsOpen()))
                .findFirst()
                .orElseThrow();
    button.click();
    Dialog dialog = field(panel, "detail");
    assertTrue(dialog.isOpened());
    assertTrue(popover.isOpened());
    assertEquals(
        read ? texts.getNotificationsRead() : texts.getNotificationsUnread(),
        detailState(dialog).getText());
    verify(service, never()).markRead(any(), any());
    dialog.close();
    assertTrue(popover.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void dismissPreviewItemUsesItsOwnRecipientIdWithoutReadingOrClosingPopup(String language) {
    locale.set(Locale.forLanguageTag(language));
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    Button history = field(panel, "history");
    assertEquals("vaadin:envelopes", history.getIcon().getElement().getAttribute("icon"));
    Button dismiss =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.hasClassName("cosmos-notification-preview-dismiss"))
            .findFirst()
            .orElseThrow();
    assertTrue(dismiss.getAriaLabel().orElseThrow().startsWith(texts.getNotificationsDismiss()));
    when(service.inbox(eq(http), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    dismiss.click();
    verify(service).dismiss(http, 7L);
    verify(service, never()).markRead(any(), any());
    assertTrue(popover.isOpened());
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-preview-row")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void unlimitedRetentionUsesLocalizedFooterWithoutDayCount(String language) {
    locale.set(Locale.forLanguageTag(language));
    when(service.retentionDays()).thenReturn(OptionalInt.empty());
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    assertEquals(
        texts.getNotificationsRetentionUnlimited(), ((Span) field(panel, "retention")).getText());
    when(service.retentionDays()).thenReturn(OptionalInt.of(14));
    panel.refresh();
    assertEquals(texts.getNotificationsRetention(14), ((Span) field(panel, "retention")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxRefreshAndLanguageNeverMarkNotificationsRead(String language) {
    locale.set(Locale.forLanguageTag(language));
    var panel = new UserNotificationsPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    assertEquals(texts.getNotificationsPasswordChangedTitle(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(s -> s.getText().equals(texts.getNotificationsRetention(180))));
    panel.refreshTexts();
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void openingOneItemKeepsReadStatusAndUsesExistingDialogTheme() {
    var panel = new UserNotificationsPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    verify(service, never()).markRead(any(), any());
    Dialog detail = field(panel, "detail");
    assertTrue(detail.isOpened());
    assertTrue(detail.getThemeNames().contains("cosmos"));
    assertTrue(detail.isCloseOnOutsideClick());
    assertTrue(detail.isCloseOnEsc());
    assertTrue(tree(detail).noneMatch(Html.class::isInstance));
  }

  Button detailButton(Dialog dialog, String css) {
    return tree(dialog)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> button.hasClassName(css))
        .findFirst()
        .orElseThrow();
  }

  Span detailState(Dialog dialog) {
    return tree(dialog)
        .filter(Span.class::isInstance)
        .map(Span.class::cast)
        .filter(span -> span.hasClassName("cosmos-notification-detail-state"))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailUsesPaperplaneBadgeMetadataAndExplicitReadAction(
      String language, boolean compact, boolean read) {
    locale.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    when(service.unreadSnapshot(http)).thenReturn(read ? List.of() : List.of(7L));
    var panel = new UserNotificationsPanel(texts, service, http, compact);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "detail");
    assertTrue(dialog.isOpened());
    verify(service, never()).markRead(any(), any());
    var state = detailState(dialog);
    assertEquals(
        read ? texts.getNotificationsRead() : texts.getNotificationsUnread(), state.getText());
    assertEquals("badge", state.getElement().getAttribute("theme"));
    var dot =
        tree(dialog)
            .filter(c -> c.hasClassName("cosmos-notification-detail-dot"))
            .findFirst()
            .orElseThrow();
    assertEquals(!read, dot.isVisible());
    assertEquals(
        !read,
        tree(dialog).anyMatch(c -> c.hasClassName("cosmos-notification-detail-message--unread")));
    assertTrue(dot.hasClassName("cosmos-notification-preview-dot"));
    assertEquals("true", dot.getElement().getAttribute("aria-hidden"));
    var header =
        tree(dialog)
            .filter(c -> c.hasClassName("cosmos-notification-detail-header"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        tree(header)
            .anyMatch(c -> "vaadin:paperplane".equals(c.getElement().getAttribute("icon"))));
    var headerTitles = tree(header).filter(H2.class::isInstance).map(H2.class::cast).toList();
    assertEquals(1, headerTitles.size());
    assertEquals(texts.getNotificationsDetailTitle(), headerTitles.getFirst().getText());
    assertTrue(
        tree(header)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span ->
                    span.getText().equals(texts.getNotificationsDetailDescription())
                        && span.hasClassName("cosmos-panel-description")));
    assertTrue(tree(header).noneMatch(c -> c.hasClassName("cosmos-notification-detail-state")));
    assertEquals(
        1,
        tree(dialog)
            .filter(H3.class::isInstance)
            .map(H3.class::cast)
            .filter(h -> h.getText().equals(UserNotificationTexts.title(texts, item)))
            .count());
    assertTrue(
        tree(dialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span ->
                    span.getText().equals(texts.getNotificationsSourceSystem())
                        && span.hasClassName("cosmos-notification-detail-source")));
    assertTrue(
        tree(dialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span ->
                    span.getText()
                        .equals(UserNotificationTexts.time(item.createdAt(), ui.getLocale()))));
    Button mark = detailButton(dialog, "cosmos-notification-detail-mark-read");
    assertEquals(!read, mark.isVisible());
    assertEquals("vaadin:envelope-open", mark.getIcon().getElement().getAttribute("icon"));
    Button history = detailButton(dialog, "cosmos-notification-detail-history");
    assertEquals(texts.getNotificationsHistory(), history.getText());
    assertEquals("vaadin:envelopes", history.getIcon().getElement().getAttribute("icon"));
    assertTrue(
        tree(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(button -> button.getText().equals(texts.getNotificationsOpenAccount())));
    assertTrue(
        tree(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(
                button ->
                    button.getText().equals(texts.getNotificationsClose())
                        && button.getThemeNames().contains("primary")
                        && "vaadin:close-circle"
                            .equals(button.getIcon().getElement().getAttribute("icon"))));
    if (!read) {
      doAnswer(
              invocation -> {
                item =
                    new Item(
                        item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
                when(service.unreadSnapshot(http)).thenReturn(List.of());
                return null;
              })
          .when(service)
          .markRead(http, List.of(7L));
      mark.click();
      verify(service).markRead(http, List.of(7L));
      assertTrue(dialog.isOpened());
      assertEquals(texts.getNotificationsRead(), detailState(dialog).getText());
      assertTrue(
          tree(dialog)
              .noneMatch(c -> c.hasClassName("cosmos-notification-detail-message--unread")));
      assertFalse(
          tree(dialog)
              .filter(c -> c.hasClassName("cosmos-notification-detail-dot"))
              .findFirst()
              .orElseThrow()
              .isVisible());
      assertFalse(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
      assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-row--unread")));
    }
    dialog.close();
    rowTitle(panel).click();
    assertEquals(texts.getNotificationsRead(), detailState(field(panel, "detail")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void closingAndReopeningDetailDoesNotReadAndLanguageRefreshPreservesManualContent(
      String language) {
    item =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "Original title",
            "Original body\nSecond line",
            Instant.now(),
            false);
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "detail");
    dialog.close();
    rowTitle(panel).click();
    dialog = field(panel, "detail");
    locale.set(Locale.forLanguageTag(language));
    panel.refreshTexts();
    assertEquals(texts.getNotificationsUnread(), detailState(dialog).getText());
    assertEquals(
        texts.getNotificationsMarkRead(),
        detailButton(dialog, "cosmos-notification-detail-mark-read").getText());
    assertTrue(
        tree(dialog)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(item.body())));
    assertTrue(
        tree(dialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span -> span.getText().contains(texts.getNotificationsSourceAdministration())));
    assertFalse(
        tree(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(button -> button.getText().equals(texts.getNotificationsOpenAccount())));
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void failedExplicitReadDoesNotShowFalseSuccess() {
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "detail");
    doThrow(new IllegalStateException("isolated failure"))
        .when(service)
        .markRead(http, List.of(7L));
    detailButton(dialog, "cosmos-notification-detail-mark-read").click();
    assertTrue(dialog.isOpened());
    assertEquals(texts.getNotificationsUnread(), detailState(dialog).getText());
    assertTrue(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
  }

  @Test
  void pushReadRefreshUpdatesOpenDetailAndHeaderCount() {
    var bell = new UserNotificationBell(texts, service, request, new Button());
    ui.add(bell);
    Popover popover = field(bell, "popover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "panel");
    rowTitle(panel).click();
    Dialog dialog = field(panel, "detail");
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
    when(service.unreadSnapshot(http)).thenReturn(List.of());
    when(service.unreadCount(http)).thenReturn(0L);
    events.publish(Set.of(5L));
    assertTrue(dialog.isOpened());
    assertTrue(popover.isOpened());
    assertEquals(texts.getNotificationsRead(), detailState(dialog).getText());
    assertFalse(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
    assertFalse(((Span) field(bell, "count")).isVisible());
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void openingDetailRechecksAccessWithoutMutatingReadStatus() {
    var panel = new UserNotificationsPanel(texts, service, http, true);
    ui.add(panel);
    panel.refresh();
    when(service.ownerId(http)).thenThrow(new AccessDeniedException("expired"));
    rowTitle(panel).click();
    assertNull(field(panel, "detail"));
    verify(service, never()).markRead(any(), any());
  }

  @Test
  void manualTextIsNotTranslatedOrParsedAsHtml() {
    item =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "<b>Titel</b>",
            "<img src=x onerror=alert(1)>\nText",
            Instant.now(),
            false);
    var panel = new UserNotificationsPanel(texts, service, http, false);
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
    assertTrue(
        tree(detail)
            .noneMatch(c -> !c.getElement().isTextNode() && c.getElement().getTag().equals("img")));
  }

  @Test
  void markAllSubmitsDisplayedSnapshotAndUnreadFilterResetsPage() {
    var panel = new UserNotificationsPanel(texts, service, http, false);
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
    var panel = new UserNotificationsPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    ((Button) field(panel, "next")).click();
    verify(service).inbox(http, false, 1, 20);
    ((Button) field(panel, "previous")).click();
    verify(service, times(2)).inbox(http, false, 0, 20);
  }

  @Test
  void revokedSessionClearsContentsAndOpenDetail() {
    var panel = new UserNotificationsPanel(texts, service, http, false);
    ui.add(panel);
    panel.refresh();
    rowTitle(panel).click();
    Dialog detail = field(panel, "detail");
    when(service.inbox(eq(http), anyBoolean(), anyInt(), anyInt()))
        .thenThrow(new AccessDeniedException("expired"));
    panel.refresh();
    assertFalse(detail.isOpened());
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-title")));
    assertFalse(((Button) field(panel, "markAll")).isEnabled());
  }

  @Test
  void liveBellCapsBadgeAndRefreshesOnlyItsOwnUserAndDetachesCleanly() {
    when(service.unreadCount(http)).thenReturn(125L);
    var button = new Button();
    var bell = new UserNotificationBell(texts, service, request, button);
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
    var bell = new UserNotificationBell(texts, service, request, new Button());
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
    var bell = new UserNotificationBell(texts, service, request, button);
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
    for (var type : UserNotificationType.values()) {
      var message = new Item(1L, type, "Original title", "Original body", Instant.now(), false);
      assertFalse(UserNotificationTexts.title(texts, message).isBlank());
      assertFalse(UserNotificationTexts.body(texts, message).isBlank());
      if (type == UserNotificationType.ADMIN_ANNOUNCEMENT)
        assertEquals("Original body", UserNotificationTexts.body(texts, message));
    }
    assertEquals(
        language.equals("de") ? "Passwort geändert" : "Password changed",
        UserNotificationTexts.title(texts, item));
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
        super(texts, security, users, status, UserNotificationsUiTest.this.request, testAvatars);
      }
    }
    try (var context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.registerBean(UserNotificationService.class, () -> service);
      context.refresh();
      var authenticated = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(authenticated);
      AccountHeaderActions actions = field(authenticated, "accountHeaderActions");
      assertTrue(tree(actions).anyMatch(UserNotificationBell.class::isInstance));
      ui.add(actions);
      verify(service).ownerId(http);
      when(security.getCurrentUser(request)).thenReturn(Optional.empty());
      var anonymous = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(anonymous);
      AccountHeaderActions hidden = field(anonymous, "accountHeaderActions");
      assertFalse(hidden.isVisible());
      assertTrue(tree(hidden).noneMatch(UserNotificationBell.class::isInstance));
    }
  }

  AdminUserNotificationsSection admin() {
    when(service.targets(http)).thenReturn(List.of(new Target(5L, "Own", "own@example.test")));
    when(service.sent(http, 0, 5)).thenReturn(Page.empty());
    var section = new AdminUserNotificationsSection(texts, service, request);
    section.activate();
    return section;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryExpandsMessagesIndependentlyAndPaginates(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();
    var message = "<b>Plain text</b>\nSecond line";
    when(service.sent(http, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new Sent(1L, "First", message, Instant.now(), 2),
                    new Sent(2L, "Second", "Other body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    when(service.sent(http, 4, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(3L, "Last", "Last body", Instant.now(), 1)),
                PageRequest.of(4, 5),
                21));
    section.activate();
    assertEquals(
        texts.getNotificationsHistoryCount(21), ((Span) field(section, "historyCount")).getText());
    assertEquals(
        texts.getAdminUsersPaginationTotal(21),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    Div history = field(section, "history");
    var details =
        tree(history)
            .filter(com.vaadin.flow.component.details.Details.class::isInstance)
            .map(com.vaadin.flow.component.details.Details.class::cast)
            .toList();
    var toggles = tree(history).filter(Button.class::isInstance).map(Button.class::cast).toList();
    assertEquals(2, details.size());
    assertTrue(details.stream().noneMatch(com.vaadin.flow.component.details.Details::isOpened));
    toggles.getFirst().click();
    assertTrue(details.getFirst().isOpened());
    assertFalse(details.getLast().isOpened());
    assertEquals("true", toggles.getFirst().getElement().getAttribute("aria-expanded"));
    assertEquals(texts.getNotificationsClose(), toggles.getFirst().getText());
    assertTrue(
        tree(details.getFirst())
            .filter(H4.class::isInstance)
            .map(H4.class::cast)
            .anyMatch(title -> title.getText().equals("First")));
    assertTrue(
        tree(details.getFirst())
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                meta ->
                    meta.getText().startsWith(texts.getNotificationsHistoryRecipients(2) + " · ")));
    assertTrue(
        tree(details.getFirst())
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(message)));
    assertTrue(tree(history).noneMatch(Html.class::isInstance));
    toggles.getFirst().click();
    assertFalse(details.getFirst().isOpened());
    assertEquals("false", toggles.getFirst().getElement().getAttribute("aria-expanded"));
    ((Button) field(field(section, "historyPagination"), "last")).click();
    assertEquals(
        texts.getAdminUsersPaginationTotal(21),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    assertFalse(((Button) field(field(section, "historyPagination"), "next")).isEnabled());
    assertTrue(((Button) field(field(section, "historyPagination"), "previous")).isEnabled());
    ((Button) field(field(section, "historyPagination"), "first")).click();
    assertEquals(0, ((Integer) field(section, "page")).intValue());
    when(service.sent(http, 0, 5)).thenReturn(Page.empty());
    section.activate();
    assertEquals(
        texts.getAdminUsersPaginationTotal(0),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    assertFalse(((Button) field(field(section, "historyPagination"), "next")).isEnabled());
    assertFalse(((Button) field(field(section, "historyPagination"), "previous")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryPageSizeSelectionResetsThePageAndLoadsTheSelectedSize(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();
    var pagination = field(section, "historyPagination");
    com.vaadin.flow.component.combobox.ComboBox<Integer> sizes = field(pagination, "pageSize");
    assertEquals(List.of(5, 10, 15), sizes.getListDataView().getItems().toList());
    assertEquals(5, sizes.getValue());
    assertEquals(texts.getAdminUsersPaginationPerPage(), sizes.getAriaLabel().orElseThrow());
    for (int size : List.of(10, 15, 5)) {
      when(service.sent(http, 0, size)).thenReturn(Page.empty());
      ReflectionTestUtils.setField(section, "page", 3);
      sizes.setValue(size);
      assertEquals(0, ((Integer) field(section, "page")).intValue());
      verify(service, atLeastOnce()).sent(http, 0, size);
      assertEquals(size, ((Integer) field(section, "historyPageSize")).intValue());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceKeepsSentHistoryPaginationAvailable(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();
    when(service.sent(http, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(1L, "Previous", "Body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    section.activate();
    when(service.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    section.refreshDeliveryState();
    assertTrue(((Button) field(field(section, "historyPagination"), "next")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceDisablesComposeAndRequiresFreshReviewWhenReopened(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();

    ((TextField) field(section, "title")).setValue("Subject");
    ((TextArea) field(section, "body")).setValue("Message");
    ((Checkbox) field(section, "everyone")).setValue(true);
    var checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(service.preview(http, checked.selection())).thenReturn(checked);
    Button review = field(section, "previewButton");
    Button send = field(section, "send");
    review.click();
    assertTrue(send.isEnabled());
    Div meta = field(section, "deliveryMeta");
    assertEquals(
        texts.getNotificationsInternal(), ((Span) field(section, "deliveryLabel")).getText());
    assertEquals("open", meta.getElement().getAttribute("data-state"));
    when(service.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    section.refreshDeliveryState();
    assertEquals("active", meta.getElement().getAttribute("data-state"));
    assertTrue(
        meta.getElement().getAttribute("aria-label").contains(texts.getMaintenanceStatusActive()));
    com.vaadin.flow.component.orderedlayout.VerticalLayout form = field(section, "composeForm");
    assertFalse(form.isEnabled());
    assertEquals("true", form.getElement().getAttribute("aria-disabled"));
    assertFalse(((TextField) field(section, "title")).isEnabled());
    assertFalse(((TextArea) field(section, "body")).isEnabled());
    assertFalse(((Checkbox) field(section, "everyone")).isEnabled());
    assertFalse(review.isEnabled());
    assertFalse(send.isEnabled());
    assertNull(field(section, "preview"));
    when(service.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
    section.refreshDeliveryState();
    assertTrue(form.isEnabled());
    assertEquals("open", meta.getElement().getAttribute("data-state"));
    assertEquals("Subject", ((TextField) field(section, "title")).getValue());
    assertFalse(send.isEnabled());
    review.click();
    assertTrue(send.isEnabled());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void deliveryActionRechecksMaintenanceBeforeNextPoll(MaintenanceService.State state) {
    var section = admin();
    ((TextField) field(section, "title")).setValue("Subject");
    ((TextArea) field(section, "body")).setValue("Message");
    ((Checkbox) field(section, "everyone")).setValue(true);
    var checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(service.preview(http, checked.selection())).thenReturn(checked);
    Button review = field(section, "previewButton");
    review.click();
    when(service.deliveryState()).thenReturn(state);
    ((Button) field(section, "send")).click();
    assertFalse(
        ((com.vaadin.flow.component.orderedlayout.VerticalLayout) field(section, "composeForm"))
            .isEnabled());
    assertNull(field(section, "preview"));
    review.click();
    verify(service, times(1)).preview(any(), any());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1000, 12000})
  void deliveryPollingUpdatesStateAndRestoresPreviousIntervalOnDetach(int previousInterval) {
    ui.setPollInterval(previousInterval);
    int[] unrelatedPolls = {0};
    var unrelated = ui.addPollListener(e -> unrelatedPolls[0]++);
    try {
      var section = admin();
      ui.add(section);
      assertEquals(
          previousInterval > 0 ? Math.min(previousInterval, 5000) : 5000, ui.getPollInterval());
      when(service.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
      ComponentUtil.fireEvent(ui, new PollEvent(ui, true));
      assertFalse(
          ((com.vaadin.flow.component.orderedlayout.VerticalLayout) field(section, "composeForm"))
              .isEnabled());
      assertEquals(1, unrelatedPolls[0]);
      ui.remove(section);
      assertEquals(previousInterval, ui.getPollInterval());
      clearInvocations(service);
      ComponentUtil.fireEvent(ui, new PollEvent(ui, true));
      assertEquals(2, unrelatedPolls[0]);
      verify(service, never()).deliveryState();
    } finally {
      unrelated.remove();
    }
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

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void draftErrorsOnlyAppearAfterReviewAndEditingClearsThem(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();
    TextField subject = field(section, "title");
    TextArea message = field(section, "body");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "targets");
    Button review = field(section, "previewButton");
    Button send = field(section, "send");
    assertEquals(texts.getNotificationsSend(), send.getText());
    subject.setValue("Draft");
    message.setValue("Draft");
    recipients.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    subject.clear();
    message.clear();
    recipients.clear();
    ReflectionTestUtils.invokeMethod(subject, "validate");
    ReflectionTestUtils.invokeMethod(message, "validate");
    ReflectionTestUtils.invokeMethod(recipients, "validate");
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    assertFalse(recipients.isInvalid());
    assertFalse(send.isEnabled());
    review.click();
    assertTrue(subject.isInvalid());
    assertTrue(message.isInvalid());
    assertTrue(recipients.isInvalid());
    assertEquals(texts.getNotificationsInvalidSubject(), subject.getErrorMessage());
    assertEquals(texts.getNotificationsInvalidBody(), message.getErrorMessage());
    assertNotEquals(subject.getErrorMessage(), message.getErrorMessage());
    assertEquals(texts.getNotificationsNoRecipients(), recipients.getErrorMessage());
    assertPreviewValidationHint();
    ReflectionTestUtils.invokeMethod(recipients, "validate");
    assertTrue(recipients.isInvalid());
    subject.setValue("Changed");
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    assertFalse(recipients.isInvalid());
    subject.clear();
    ReflectionTestUtils.invokeMethod(subject, "validate");
    assertFalse(subject.isInvalid());
    subject.setValue("Valid subject");
    message.setValue("Valid message");
    review.click();
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    assertTrue(recipients.isInvalid());
    assertPreviewValidationHint();
    assertFalse(send.isEnabled());
    verify(service, never()).preview(any(), any());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  private void assertPreviewValidationHint() {
    var helper = NotificationHelper.getInstance();
    Span hintTitle = field(helper, "titleText");
    Html hintMessage = field(helper, "messageText");
    assertEquals(texts.getNotificationsPreviewFailedTitle(), hintTitle.getText());
    assertEquals(
        new Html(
                de.derpeterson.app.helper.ui.HtmlHelper.ensureParagraphTags(
                    texts.getNotificationsPreviewFailedMessage()))
            .getElement()
            .getProperty("innerHTML"),
        hintMessage.getElement().getProperty("innerHTML"));
  }

  @ParameterizedTest
  @EnumSource(
      value = Problem.class,
      names = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED"})
  void rejectedPreviewKeepsRecipientDetailsAtFieldAndUsesGeneralHint(Problem problem) {
    var section = admin();
    ((TextField) field(section, "title")).setValue("Subject");
    ((TextArea) field(section, "body")).setValue("Message");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "targets");
    recipients.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    when(service.preview(any(), any())).thenThrow(new Rejected(problem));
    ((Button) field(section, "previewButton")).click();
    assertTrue(recipients.isInvalid());
    assertEquals(
        problem == Problem.NO_RECIPIENTS
            ? texts.getNotificationsNoRecipients()
            : texts.getNotificationsRecipientsChanged(),
        recipients.getErrorMessage());
    assertPreviewValidationHint();
    assertFalse(((Button) field(section, "send")).isEnabled());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body", "recipients", "everyone"})
  void changingAnyDraftInputRequiresAnotherSuccessfulReview(String input) {
    var section = admin();
    TextField subject = field(section, "title");
    TextArea message = field(section, "body");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "targets");
    Checkbox all = field(section, "everyone");
    Button review = field(section, "previewButton");
    Button send = field(section, "send");
    subject.setValue("Subject");
    message.setValue("Message");
    recipients.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    var checked = new Preview(new Selection(false, Set.of(5L)), List.of(5L));
    when(service.preview(http, checked.selection())).thenReturn(checked);
    assertFalse(send.isEnabled());
    review.click();
    assertTrue(send.isEnabled());
    switch (input) {
      case "title" -> subject.setValue("Changed subject");
      case "body" -> message.clear();
      case "recipients" -> recipients.clear();
      case "everyone" -> all.setValue(true);
      default -> fail("Unknown draft input");
    }
    assertFalse(send.isEnabled());
    assertNull(field(section, "preview"));
    assertTrue(((Div) field(section, "previewArea")).isVisible());
    assertEquals(
        texts.getNotificationsPreviewNeedsReview(),
        ((Span) field(section, "previewStatus")).getText());
    assertEquals(
        "warning",
        ((Span) field(section, "previewStatus")).getElement().getAttribute("data-state"));
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    assertFalse(recipients.isInvalid());
    send.click();
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED", "UNAVAILABLE"})
  void failedRepeatReviewRevokesEarlierApproval(String problem) {
    var section = admin();
    ((TextField) field(section, "title")).setValue("Subject");
    ((TextArea) field(section, "body")).setValue("Message");
    ((Checkbox) field(section, "everyone")).setValue(true);
    var checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    RuntimeException rejection =
        problem.equals("UNAVAILABLE")
            ? new IllegalStateException("Isolated review failure")
            : new Rejected(Problem.valueOf(problem));
    when(service.preview(http, checked.selection())).thenReturn(checked).thenThrow(rejection);
    Button review = field(section, "previewButton");
    Button send = field(section, "send");
    review.click();
    assertTrue(send.isEnabled());
    review.click();
    assertFalse(send.isEnabled());
    assertNull(field(section, "preview"));
    assertTrue(((Div) field(section, "previewArea")).isVisible());
    assertEquals(
        texts.getNotificationsPreviewNeedsReview(),
        ((Span) field(section, "previewStatus")).getText());
    assertEquals(
        "warning",
        ((Span) field(section, "previewStatus")).getElement().getAttribute("data-state"));
    assertEquals(
        !problem.equals("UNAVAILABLE"),
        ((TextField) field(section, "everyoneRecipients")).isInvalid());
    send.click();
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body"})
  void overlongDraftIsMarkedOnlyWhenReviewed(String input) {
    var section = admin();
    TextField subject = field(section, "title");
    TextArea message = field(section, "body");
    subject.setValue("Subject");
    message.setValue("Message");
    ((Checkbox) field(section, "everyone")).setValue(true);
    if (input.equals("title"))
      subject.setValue("x".repeat(UserNotificationService.TITLE_LIMIT + 1));
    else message.setValue("x".repeat(UserNotificationService.BODY_LIMIT + 1));
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    ((Button) field(section, "previewButton")).click();
    assertEquals(input.equals("title"), subject.isInvalid());
    assertEquals(input.equals("body"), message.isInvalid());
    assertFalse(((Button) field(section, "send")).isEnabled());
    verify(service, never()).preview(any(), any());
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
    assertFalse(((Div) field(section, "previewArea")).isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void recipientCountTracksSelectionRemovalAndLanguage(String language) {
    locale.set(Locale.forLanguageTag(language));
    var accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(service.targets(http)).thenReturn(accounts);
    when(service.sent(http, 0, 5)).thenReturn(Page.empty());
    var section = new AdminUserNotificationsSection(texts, service, request);
    section.activate();
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "targets");
    Span summary = field(section, "recipientSummary");
    assertEquals(texts.getNotificationsSelectedUsers(0), summary.getText());
    recipients.setValue(Set.of(accounts.getFirst()));
    assertEquals(texts.getNotificationsSelectedUsers(1), summary.getText());
    recipients.setValue(Set.copyOf(accounts));
    assertEquals(texts.getNotificationsSelectedUsers(2), summary.getText());
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(texts.getNotificationsSelectedUsers(2), summary.getText());
    recipients.setValue(Set.of(accounts.getLast()));
    assertEquals(texts.getNotificationsSelectedUsers(1), summary.getText());
    recipients.clear();
    assertEquals(texts.getNotificationsSelectedUsers(0), summary.getText());
    assertFalse(((Button) field(section, "send")).isEnabled());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void allRecipientsShowsDisabledSummaryAndRestoresIndividualSelection(String language) {
    locale.set(Locale.forLanguageTag(language));
    var accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(service.targets(http)).thenReturn(accounts);
    when(service.sent(http, 0, 5)).thenReturn(Page.empty());
    var section = new AdminUserNotificationsSection(texts, service, request);
    section.activate();
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "targets");
    TextField summary = field(section, "everyoneRecipients");
    Checkbox all = field(section, "everyone");
    assertTrue(recipients.isVisible());
    assertTrue(recipients.isEnabled());
    assertFalse(summary.isVisible());
    recipients.setValue(Set.of(accounts.getFirst()));
    all.setValue(true);
    assertFalse(recipients.isVisible());
    assertFalse(recipients.isEnabled());
    assertTrue(summary.isVisible());
    assertFalse(summary.isEnabled());
    assertTrue(summary.isReadOnly());
    assertEquals(texts.getNotificationsEveryonePlaceholder(2), summary.getValue());
    assertEquals(texts.getNotificationsRecipients(), summary.getLabel());
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(texts.getNotificationsEveryonePlaceholder(2), summary.getValue());
    assertEquals(texts.getNotificationsRecipients(), summary.getLabel());
    all.setValue(false);
    assertFalse(summary.isVisible());
    assertTrue(recipients.isVisible());
    assertTrue(recipients.isEnabled());
    assertEquals(Set.of(accounts.getFirst()), recipients.getSelectedItems());
    verify(service, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reviewedPreviewUsesNativeRecipientsAndInvalidatesAfterEditing(String language) {
    locale.set(Locale.forLanguageTag(language));
    var accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"),
            new Target(7L, "Third User", "third@example.test"),
            new Target(8L, "Fourth User", "fourth@example.test"));
    when(service.targets(http)).thenReturn(accounts);
    when(service.sent(http, 0, 5)).thenReturn(Page.empty());
    var section = new AdminUserNotificationsSection(texts, service, request);
    section.activate();
    TextField titleField = field(section, "title");
    TextArea bodyField = field(section, "body");
    Checkbox all = field(section, "everyone");
    var preview = new Preview(new Selection(true, Set.of()), List.of(5L, 6L, 7L, 8L));
    when(service.preview(http, preview.selection())).thenReturn(preview);
    titleField.setValue("<b>Title</b>");
    bodyField.setValue("<img src=x onerror=alert(1)>\nPlain text");
    all.setValue(true);
    assertEquals(
        titleField.getValue().length() + " / " + UserNotificationService.TITLE_LIMIT,
        ((Span) field(section, "titleCount")).getText());
    assertEquals(
        bodyField.getValue().length() + " / " + UserNotificationService.BODY_LIMIT,
        ((Span) field(section, "bodyCount")).getText());
    assertEquals(UserNotificationService.TITLE_LIMIT, titleField.getMaxLength());
    assertEquals(UserNotificationService.BODY_LIMIT, bodyField.getMaxLength());
    com.vaadin.flow.component.combobox.MultiSelectComboBox<?> recipients =
        field(section, "targets");
    assertFalse(recipients.isEnabled());
    assertEquals(texts.getNotificationsEveryonePlaceholder(4), recipients.getPlaceholder());
    ((Button) field(section, "previewButton")).click();
    Div area = field(section, "previewArea");
    assertTrue(area.isVisible());
    Span status = field(section, "previewStatus");
    assertTrue(status.isVisible());
    assertEquals(texts.getNotificationsPreviewUpdated(), status.getText());
    assertEquals("success", status.getElement().getAttribute("data-state"));
    assertTrue(
        status
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-compose-preview-header"));
    assertEquals(
        3, tree(area).filter(com.vaadin.flow.component.avatar.Avatar.class::isInstance).count());
    var details =
        tree(area)
            .filter(com.vaadin.flow.component.details.Details.class::isInstance)
            .map(com.vaadin.flow.component.details.Details.class::cast)
            .findFirst()
            .orElseThrow();
    assertFalse(details.isOpened());
    var detailsButton =
        tree(area)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(
                button -> button.getId().orElse("").equals("admin-notifications-preview-details"))
            .findFirst()
            .orElseThrow();
    assertEquals(texts.getNotificationsRecipientDetails(), detailsButton.getText());
    assertEquals("false", detailsButton.getElement().getAttribute("aria-expanded"));
    assertEquals(
        details.getId().orElseThrow(), detailsButton.getElement().getAttribute("aria-controls"));
    assertTrue(
        detailsButton
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-compose-recipients"));
    detailsButton.click();
    assertTrue(details.isOpened());
    assertEquals(texts.getNotificationsHideRecipientDetails(), detailsButton.getText());
    assertEquals("true", detailsButton.getElement().getAttribute("aria-expanded"));
    detailsButton.click();
    assertFalse(details.isOpened());
    assertEquals(texts.getNotificationsRecipientDetails(), detailsButton.getText());
    detailsButton.click();
    for (var account : accounts)
      assertTrue(
          tree(details)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(span -> span.getText().equals(account.email())));
    assertTrue(tree(area).noneMatch(Html.class::isInstance));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(bodyField.getValue())));
    assertTrue(((Button) field(section, "send")).isEnabled());
    verify(service, never()).send(any(), any(), any(), any(), any());
    String checkedBody = bodyField.getValue();
    bodyField.setValue("Changed");
    assertTrue(area.isVisible());
    assertNull(field(section, "preview"));
    assertTrue(status.isVisible());
    assertEquals(texts.getNotificationsPreviewNeedsReview(), status.getText());
    assertEquals("warning", status.getElement().getAttribute("data-state"));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(checkedBody)));
    assertFalse(((Button) field(section, "send")).isEnabled());
    assertEquals(
        "7 / " + UserNotificationService.BODY_LIMIT,
        ((Span) field(section, "bodyCount")).getText());
    ((Button) field(section, "previewButton")).click();
    assertTrue(((Button) field(section, "send")).isEnabled());
    assertEquals(texts.getNotificationsPreviewUpdated(), status.getText());
    assertEquals("success", status.getElement().getAttribute("data-state"));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals("Changed")));
  }
}
