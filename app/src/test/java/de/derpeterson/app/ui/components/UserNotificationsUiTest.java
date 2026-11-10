package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.UserNotificationTexts;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.service.UserNotificationService.Preview;
import de.derpeterson.app.service.UserNotificationService.Problem;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.service.UserNotificationService.Rejected;
import de.derpeterson.app.service.UserNotificationService.Selection;
import de.derpeterson.app.service.UserNotificationService.Sent;
import de.derpeterson.app.service.UserNotificationService.Target;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.admin.AdminUserNotificationsSection;
import de.derpeterson.app.websocket.SupportTicketBroadcaster;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.ObjectNode;

/**
 * Java-Komponentenprüfung ohne Browser; sichtbare Geometrie/Frontend bleiben getrennt zu prüfen.
 */
class UserNotificationsUiTest {
  UI testUi;
  MessageProperties messageProperties;
  UserNotificationService userNotificationService;
  MockHttpSession mockHttpSession;
  MockHttpServletRequest mockHttpRequest;
  AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  UserNotificationBroadcaster userNotificationBroadcaster;
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
    testUi = new UI();
    UI.setCurrent(testUi);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(c -> localeReference.get());
    when(session.access(any()))
        .thenAnswer(c -> {
          ((Command) c.getArgument(0)).execute();
          return CompletableFuture.completedFuture(null);
        });
    VaadinSession.setCurrent(session);
    testUi.getInternals().setSession(session);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    userNotificationService = mock(UserNotificationService.class);
    mockHttpSession = new MockHttpSession();
    mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.setSession(mockHttpSession);
    userNotificationBroadcaster = new UserNotificationBroadcaster();
    when(userNotificationService.events()).thenReturn(userNotificationBroadcaster);
    when(userNotificationService.retentionDays()).thenReturn(OptionalInt.of(180));
    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
    when(userNotificationService.ownerId(mockHttpSession)).thenReturn(5L);
    when(userNotificationService.unreadCount(mockHttpSession)).thenReturn(1L);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of(7L));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 1));
  }

  @AfterEach
  void cleanup() {
    testUi.getChildren().toList().forEach(testUi::remove);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ticketNavigationActivityFollowsUnreadChangesAndLanguageWithoutOpeningTickets(
      String language) {
    localeReference.set(Locale.forLanguageTag(language));
    SupportTicketService mockSupportTicketService = mock(SupportTicketService.class);
    SupportTicketBroadcaster changes = mock(SupportTicketBroadcaster.class);
    when(mockSupportTicketService.events()).thenReturn(changes);
    AtomicReference<Consumer<SupportTicketBroadcaster.SupportTicketChangeMessage>> listener =
        new AtomicReference<>();
    when(changes.register(any()))
        .thenAnswer(call -> {
          listener.set(call.getArgument(0));
          return (Registration) () -> listener.set(null);
        });
    Ticket ticket = mock(Ticket.class);
    when(ticket.unreadMessages()).thenReturn(3L);
    when(mockSupportTicketService.loadTicketActivity(mockHttpSession))
        .thenAnswer(call -> new SupportTicketService.Activity(5L, ticket.unreadMessages() > 0));
    SideNavItem navigation = new SideNavItem("Tickets");
    SupportTicketActivityIndicator indicator =
        new SupportTicketActivityIndicator(
            messageProperties, mockSupportTicketService, mockHttpRequest, navigation);
    testUi.add(navigation);
    assertTrue(indicator.isVisible());
    assertNull(indicator.getText());
    assertEquals(
        messageProperties.getSupportNewActivity(), Tooltip.forComponent(navigation).getText());

    listener.get().accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 77L));
    verify(mockSupportTicketService, times(1)).loadTicketActivity(mockHttpSession);

    when(ticket.unreadMessages()).thenReturn(0L);
    listener.get().accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 5L));
    assertFalse(indicator.isVisible());
    assertEquals("", Tooltip.forComponent(navigation).getText());

    when(ticket.unreadMessages()).thenReturn(1L);
    listener.get().accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(8L, null));
    assertTrue(indicator.isVisible());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    indicator.refreshTexts();
    assertEquals(
        messageProperties.getSupportNewActivity(), Tooltip.forComponent(navigation).getText());

    when(mockSupportTicketService.loadTicketActivity(mockHttpSession))
        .thenThrow(new AccessDeniedException("Access revoked"));
    listener.get().accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 5L));
    assertFalse(indicator.isVisible());
    assertEquals("", Tooltip.forComponent(navigation).getText());
    testUi.remove(navigation);
    assertNull(listener.get());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,true", "en,true", "de,false", "en,false"})
  void emptyListsUseTheSameIconAndTitleForEveryReadFilter(String language, boolean compact) {
    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, compact);
    testUi.add(panel);
    RadioButtonGroup<ReadFilter> filters = field(panel, "readFilter");
    Div rows = field(panel, "notificationRows");
    for (ReadFilter filter : ReadFilter.values()) {
      filters.setValue(filter);
      panel.refreshInbox();
      assertEquals(1, rows.getComponentCount());
      Div empty = (Div) rows.getComponentAt(0);
      assertTrue(empty.hasClassName("cosmos-notification-empty"));
      assertEquals(2, empty.getComponentCount());
      assertEquals("icons/lucide/bell.svg", ((SvgIcon) empty.getComponentAt(0)).getSrc());
      assertEquals(
          messageProperties.getNotificationsEmpty(), ((Span) empty.getComponentAt(1)).getText());
    }
    if (!compact) {
      when(userNotificationService.searchInbox(
              eq(mockHttpSession),
              any(ReadFilter.class),
              eq("no-hit"),
              anySet(),
              anyInt(),
              anyInt()))
          .thenReturn(Page.empty());
      ((TextField) field(panel, "notificationSearchField")).setValue("no-hit");
      assertTrue(
          rows.getElement()
              .getTextRecursively()
              .contains(messageProperties.getNotificationsSearchEmpty()));
    }
    panel.unavailable();
    assertFalse(
        tree(rows).anyMatch(component -> component.hasClassName("cosmos-notification-empty")));
    assertTrue(
        rows.getElement()
            .getTextRecursively()
            .contains(messageProperties.getNotificationsUnavailable()));
    verify(userNotificationService, never()).dismiss(any(), any());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,true", "en,true", "de,false", "en,false"})
  void onlyManualRefreshReportsTheActualOutcome(String language, boolean compact) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, compact);
    testUi.add(panel);
    panel.refreshInbox();
    NotificationHelper helper = NotificationHelper.getInstance();
    assertNull(field(helper, "messageText"));
    Button refreshButton = field(panel, "refreshInboxButton");
    refreshButton.click();
    Span success = field(helper, "titleText");
    assertEquals(messageProperties.getNotificationsRefreshed(), success.getText());
    assertNull(field(helper, "messageText"));
    helper.closeAndClearAllNotifications();
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenThrow(new IllegalStateException("Unavailable test fixture"));
    refreshButton.click();
    Html failure = field(helper, "messageText");
    assertTrue(failure.getInnerHtml().contains(messageProperties.getNotificationsUnavailable()));
    assertFalse(failure.getInnerHtml().contains(messageProperties.getNotificationsRefreshed()));
    assertEquals("–", ((Span) field(panel, "unreadCount")).getText());
  }

  private void confirmPending(UserNotificationsPanel panel) {
    PortalConfirmationDialog confirmation = field(panel, "notificationActionConfirmation");
    assertTrue(confirmation.isOpened());
    ((Button) field(confirmation, "confirmButton")).click();
    assertFalse(confirmation.isOpened());
  }

  @SuppressWarnings("unchecked")
  <T> T field(Object component, String name) {
    return (T) ReflectionTestUtils.getField(component, name);
  }

  Stream<Component> tree(Component component) {
    Stream<Component> children = component.getChildren();
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

  private void assertReadAllIcon(Button button) {
    SvgIcon icon = assertInstanceOf(SvgIcon.class, button.getIcon());
    assertEquals("icons/lucide/check-check.svg", icon.getSrc());
    assertEquals("vaadin-icon", icon.getElement().getTag());
    assertEquals("true", icon.getElement().getAttribute("aria-hidden"));
    assertNotNull(getClass().getResource("/META-INF/resources/" + icon.getSrc()));
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
  void personalNotificationsPageUsesSharedAccountSectionHeaderAndUpdatesLanguage(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    SecurityService security = mock(de.derpeterson.app.security.SecurityService.class);
    ConfigService config = mock(de.derpeterson.app.service.ConfigService.class);
    when(config.getString(any(de.derpeterson.app.model.enums.ConfigEntry.class)))
        .thenAnswer(c ->
            ((de.derpeterson.app.model.enums.ConfigEntry) c.getArgument(0))
                .getDefaultValueString());
    UserEntity user =
        de.derpeterson.app.model.UserEntity.builder()
            .id(5L)
            .firstName("Own")
            .lastName("User")
            .enabled(true)
            .build();
    when(security.getCurrentUser(mockHttpRequest)).thenReturn(Optional.of(user));
    UserService users = mock(de.derpeterson.app.service.UserService.class);
    UserAvatarService testAvatars = mock(de.derpeterson.app.service.UserAvatarService.class);
    class NotificationsView extends de.derpeterson.app.views.UserNotificationsView {
      NotificationsView() {
        super(
            UserNotificationsUiTest.this.messageProperties,
            security,
            users,
            new de.derpeterson.app.websocket.UserStatusBroadcaster(),
            UserNotificationsUiTest.this.mockHttpRequest,
            testAvatars,
            UserNotificationsUiTest.this.userNotificationService,
            config);
      }

      void refreshLanguage() {
        onTrackedUserLanguageChanged();
      }
    }
    NotificationsView view = new NotificationsView();
    Component header =
        tree(view.getContent())
            .filter(CosmosSectionHeaderComponent.class::isInstance)
            .findFirst()
            .orElseThrow();
    assertTrue(header.hasClassName("cosmos-dashboard-introduction"));
    assertTrue(
        tree(header)
            .filter(H1.class::isInstance)
            .anyMatch(h -> h.hasClassName("cosmos-section-title")));
    Span overview = field(view, "overview");
    Span title = field(view, "title");
    Span description = field(view, "description");
    assertTrue(overview.hasClassName("cosmos-dashboard-kicker"));
    assertTrue(description.hasClassName("cosmos-dashboard-description"));
    assertEquals(messageProperties.getAccountNavigationPortal(), overview.getText());
    assertEquals(messageProperties.getNotificationsTitle(), title.getText());
    assertEquals(messageProperties.getNotificationsPageDescription(), description.getText());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    view.refreshLanguage();
    assertEquals(messageProperties.getAccountNavigationPortal(), overview.getText());
    assertEquals(messageProperties.getNotificationsTitle(), title.getText());
    assertEquals(messageProperties.getNotificationsPageDescription(), description.getText());
    assertEquals(1, tree(view.getContent()).filter(c -> c == description).count());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void refreshOpeningAndLanguageNeverMarkNotificationsRead(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    assertEquals(
        messageProperties.getNotificationsPasswordChangedTitle(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(s -> s.getText().equals(messageProperties.getNotificationsRetention(180))));
    panel.refreshTexts();
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void headerPreviewUsesAccountHeaderAndClosesExplicitlyWithoutReading(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    assertTrue(popover.getThemeNames().contains("cosmos-notifications"));
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    assertReadAllIcon((Button) field(panel, "markAllReadButton"));
    assertEquals(
        messageProperties.getNotificationsLatest(5),
        ((Span) field(panel, "inboxDescription")).getText());
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    assertEquals(ReadFilter.ALL, filter.getValue());
    assertEquals(
        messageProperties.getNotificationsAll(),
        filter.getItemLabelGenerator().apply(ReadFilter.ALL));
    assertEquals(
        messageProperties.getNotificationsRead(),
        filter.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        messageProperties.getNotificationsUnread(),
        filter.getItemLabelGenerator().apply(ReadFilter.UNREAD));
    assertEquals(messageProperties.getNotificationsFilter(), filter.getAriaLabel().orElseThrow());
    assertTrue(tree(panel).noneMatch(Checkbox.class::isInstance));
    Span previewHint = field(panel, "previewHint");
    assertEquals(messageProperties.getNotificationsLatest(5), previewHint.getText());
    filter.setValue(ReadFilter.READ);
    assertEquals(messageProperties.getNotificationsLatestRead(5), previewHint.getText());
    filter.setValue(ReadFilter.UNREAD);
    assertEquals(messageProperties.getNotificationsLatestUnread(5), previewHint.getText());
    filter.setValue(ReadFilter.ALL);
    assertEquals(messageProperties.getNotificationsLatest(5), previewHint.getText());
    Button close = field(panel, "closeInboxButton");
    assertEquals(messageProperties.getNotificationsClose(), close.getAriaLabel().orElseThrow());
    assertTrue(close.hasClassName("cosmos-overlay-close"));
    close.click();
    assertFalse(popover.isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void segmentedReadFilterQueriesEachStateResetsPageAndSurvivesLanguageChange(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    Item readItem = new Item(8L, item.type(), item.title(), item.body(), item.createdAt(), true);
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(c -> {
          ReadFilter filter = c.getArgument(1);
          List<Item> items =
              switch (filter) {
                case ALL -> List.of(readItem, item);
                case READ -> List.of(readItem);
                case UNREAD -> List.of(item);
              };
          return new PageImpl<>(
              items, PageRequest.of(c.getArgument(2), c.getArgument(3)), items.size());
        });
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    assertEquals(
        2, tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).count());
    ReflectionTestUtils.setField(panel, "page", 2);
    filter.setValue(ReadFilter.READ);
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.READ, 0, 5);
    assertEquals(0, (int) field(panel, "page"));
    List<Component> rows =
        tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).toList();
    assertEquals(1, rows.size());
    assertEquals("8", rows.getFirst().getElement().getAttribute("data-notification-id"));
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals(ReadFilter.READ, filter.getValue());
    assertEquals(
        messageProperties.getNotificationsRead(),
        filter.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        messageProperties.getNotificationsLatestRead(5),
        ((Span) field(panel, "previewHint")).getText());
    filter.setValue(ReadFilter.UNREAD);
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 5);
    rows = tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).toList();
    assertEquals(1, rows.size());
    assertEquals("7", rows.getFirst().getElement().getAttribute("data-notification-id"));
    assertTrue(rows.getFirst().hasClassName("cosmos-notification-row--unread"));
    filter.setValue(ReadFilter.ALL);
    assertEquals(
        2, tree(panel).filter(c -> c.hasClassName("cosmos-notification-preview-row")).count());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void compactPreviewSupportsSafeTextAndIndividualReadWithoutOpening(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    item =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "<b>Title</b>",
            "<img src=x onerror=alert(1)>\nSecond line",
            Instant.now(),
            false);
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    assertEquals(item.title(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div ->
                div.hasClassName("cosmos-notification-preview-message")
                    && div.getText().equals(item.body())));
    assertTrue(tree(panel).noneMatch(Html.class::isInstance));
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getText().equals(messageProperties.getNotificationsSourceAdministration())));
    Button read =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.hasClassName("cosmos-notification-preview-mark-read"))
            .findFirst()
            .orElseThrow();
    assertTrue(read.isVisible());
    read.click();
    confirmPending(panel);
    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
    assertNull(field(panel, "notificationDetailDialog"));
    ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).setValue(ReadFilter.UNREAD);
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 5);
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
    panel.refreshInbox();
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
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    rowTitle(panel).click();
    assertTrue(popover.isOpened());
    Dialog detail = field(panel, "notificationDetailDialog");
    assertTrue(detail.isOpened());
    assertFalse(popover.isCloseOnOutsideClick());
    assertFalse(popover.isCloseOnEsc());
    detail.close();
    assertTrue(popover.isOpened());
    assertTrue(popover.isCloseOnOutsideClick());
    assertTrue(popover.isCloseOnEsc());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void singleClickOnPreviewCardOpensOnlyItsMessageAndIgnoresButtonEvents(
      String language, boolean read) {
    localeReference.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    Component row =
        tree(panel)
            .filter(c -> c.hasClassName("cosmos-notification-preview-row"))
            .findFirst()
            .orElseThrow();
    Element element = row.getElement();
    ElementListenerMap listeners =
        element.getNode().getFeature(com.vaadin.flow.internal.nodefeature.ElementListenerMap.class);
    ObjectNode data = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    assertTrue(listeners.getExpressions("dblclick").isEmpty());
    Set<String> expressions = listeners.getExpressions("click");
    assertFalse(expressions.isEmpty());
    expressions.forEach(expression -> data.put(expression, false));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "click", data));
    assertNull(field(panel, "notificationDetailDialog"));
    verify(userNotificationService, never()).markRead(any(), any());
    expressions.forEach(expression -> data.put(expression, true));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "click", data));
    assertTrue(popover.isOpened());
    assertTrue(((Dialog) field(panel, "notificationDetailDialog")).isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,false,title", "en,false,title", "de,true,title", "en,true,title",
    "de,false,open", "en,false,open", "de,true,open", "en,true,open"
  })
  void titleAndOpenButtonUseSingleClickForBothReadStates(
      String language, boolean read, String target) {
    localeReference.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    Button button =
        target.equals("title")
            ? rowTitle(panel)
            : tree(panel)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(b -> b.hasClassName("cosmos-notification-row-open"))
                .findFirst()
                .orElseThrow();
    if (target.equals("open")) {
      assertEquals("", button.getText());
      assertEquals(messageProperties.getNotificationsOpen(), button.getTooltip().getText());
      assertEquals(
          messageProperties.getNotificationsOpen()
              + ": "
              + UserNotificationTexts.getNotificationTitleText(messageProperties, item),
          button.getAriaLabel().orElseThrow());
      assertFalse(button.getThemeNames().contains("tertiary-inline"));
    }
    button.click();
    Dialog dialog = field(panel, "notificationDetailDialog");
    assertTrue(dialog.isOpened());
    assertTrue(popover.isOpened());
    assertEquals(
        read
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        detailState(dialog).getText());
    verify(userNotificationService, never()).markRead(any(), any());
    dialog.close();
    assertTrue(popover.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void dismissPreviewItemUsesItsOwnRecipientIdWithoutReadingOrClosingPopup(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    Button history = field(panel, "openHistoryButton");
    assertEquals("icons/lucide/eye.svg", ((SvgIcon) history.getIcon()).getSrc());
    Button dismiss =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.hasClassName("cosmos-notification-preview-dismiss"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        dismiss
            .getAriaLabel()
            .orElseThrow()
            .startsWith(messageProperties.getNotificationsDismiss()));
    assertTrue(dismiss.hasClassName("cosmos-delete-button"));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    dismiss.click();
    confirmPending(panel);
    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertTrue(popover.isOpened());
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-preview-row")));
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void dismissInboxItemRefreshesOwnListAndCountersWithoutOpeningOrReading(
      String language, boolean read) {
    localeReference.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(read ? List.of() : List.of(7L));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    Button dismiss =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(b -> b.hasClassName("cosmos-notification-preview-dismiss"))
            .findFirst()
            .orElseThrow();
    assertTrue(dismiss.getParent().orElseThrow().hasClassName("cosmos-notification-inbox-actions"));
    assertEquals("icons/lucide/trash-2.svg", ((SvgIcon) dismiss.getIcon()).getSrc());
    assertEquals(messageProperties.getNotificationsDismiss(), dismiss.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsDismiss()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item),
        dismiss.getAriaLabel().orElseThrow());
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(0L);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    dismiss.click();
    confirmPending(panel);
    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertNull(field(panel, "notificationDetailDialog"));
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-inbox-row")));
    assertEquals("0", ((Span) field(panel, "totalCount")).getText());
    assertEquals("0", ((Span) field(panel, "unreadCount")).getText());
    assertFalse(((Button) field(panel, "markAllReadButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void bulkDismissReadIsAvailableAcrossFiltersAndSearchAndKeepsUnread(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    Button remove = field(panel, "dismissReadButton");
    Button markAll = field(panel, "markAllReadButton");
    assertFalse(remove.isEnabled());
    assertEquals("", remove.getText());
    assertTrue(remove.hasClassName("cosmos-delete-button"));
    assertEquals("icons/lucide/trash-2.svg", ((SvgIcon) remove.getIcon()).getSrc());
    assertEquals(messageProperties.getNotificationsDismissRead(), remove.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsDismissRead(), remove.getAriaLabel().orElseThrow());
    List<Component> controls = remove.getParent().orElseThrow().getChildren().toList();
    assertEquals(controls.indexOf(markAll) + 1, controls.indexOf(remove));
    when(userNotificationService.readCount(mockHttpSession)).thenReturn(3L);
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(4L);
    panel.refreshInbox();
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    filter.setValue(ReadFilter.UNREAD);
    TextField search = field(panel, "notificationSearchField");
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    search.setValue("no visible match");
    assertTrue(remove.isEnabled());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals(messageProperties.getNotificationsDismissRead(), remove.getTooltip().getText());
    doAnswer(
            call -> {
              when(userNotificationService.readCount(mockHttpSession)).thenReturn(0L);
              when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
              return null;
            })
        .when(userNotificationService)
        .dismissRead(mockHttpSession);
    remove.click();
    confirmPending(panel);
    verify(userNotificationService).dismissRead(mockHttpSession);
    verify(userNotificationService, never()).dismiss(any(), any());
    verify(userNotificationService, never()).markRead(any(), any());
    assertEquals(List.of(7L), field(panel, "unreadIds"));
    assertFalse(remove.isEnabled());
    assertTrue(markAll.isEnabled());
    assertEquals(ReadFilter.UNREAD, filter.getValue());
    assertNull(field(panel, "notificationDetailDialog"));
    assertEquals("no visible match", search.getValue());
    assertEquals("1", ((Span) field(panel, "totalCount")).getText());
    assertEquals("1", ((Span) field(panel, "unreadCount")).getText());
    panel.unavailable();
    assertFalse(remove.isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void bulkDismissReadIsAbsentFromOverlay(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.readCount(mockHttpSession)).thenReturn(3L);
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    Button remove = field(panel, "dismissReadButton");
    assertTrue(remove.getParent().isEmpty());
    assertFalse(remove.isEnabled());
    assertTrue(tree(panel).noneMatch(component -> component == remove));
    verify(userNotificationService, never()).readCount(any());
    verify(userNotificationService, never()).dismissRead(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void unlimitedRetentionUsesLocalizedFooterWithoutDayCount(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.retentionDays()).thenReturn(OptionalInt.empty());
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    assertEquals(
        messageProperties.getNotificationsRetentionUnlimited(),
        ((Span) field(panel, "retention")).getText());
    when(userNotificationService.retentionDays()).thenReturn(OptionalInt.of(14));
    panel.refreshInbox();
    assertEquals(
        messageProperties.getNotificationsRetention(14),
        ((Span) field(panel, "retention")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxRefreshAndLanguageNeverMarkNotificationsRead(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    assertEquals(
        messageProperties.getNotificationsPasswordChangedTitle(), rowTitle(panel).getText());
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(s -> s.getText().equals(messageProperties.getNotificationsRetention(180))));
    panel.refreshTexts();
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void openingOneItemKeepsReadStatusAndUsesExistingDialogTheme() {
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    rowTitle(panel).click();
    verify(userNotificationService, never()).markRead(any(), any());
    Dialog detail = field(panel, "notificationDetailDialog");
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
  void detailShowsLocalizedHeaderMetadataAndReadState(
      String language, boolean compact, boolean read) {
    NotificationDetailFixture fixture = openNotificationDetail(language, compact, read);
    Dialog dialog = fixture.dialog();
    assertTrue(dialog.isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
    Span state = detailState(dialog);
    assertEquals(
        read
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        state.getText());
    assertEquals("badge", state.getElement().getAttribute("theme"));
    assertEquals(Boolean.toString(read), state.getElement().getAttribute("data-read"));
    assertEquals(
        !read,
        tree(dialog).anyMatch(c -> c.hasClassName("cosmos-notification-detail-message--unread")));
    Component header =
        tree(dialog)
            .filter(c -> c.hasClassName("cosmos-notification-detail-header"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        tree(header)
            .anyMatch(
                c -> c instanceof SvgIcon icon && "icons/lucide/bell.svg".equals(icon.getSrc())));
    List<H2> headerTitles = tree(header).filter(H2.class::isInstance).map(H2.class::cast).toList();
    assertEquals(1, headerTitles.size());
    assertEquals(
        messageProperties.getNotificationsDetailTitle(), headerTitles.getFirst().getText());
    assertTrue(
        tree(header)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getText().equals(messageProperties.getNotificationsDetailDescription())
                    && span.hasClassName("cosmos-panel-description")));
    assertTrue(tree(header).noneMatch(c -> c.hasClassName("cosmos-notification-detail-state")));
    assertEquals(
        1,
        tree(dialog)
            .filter(H3.class::isInstance)
            .map(H3.class::cast)
            .filter(h ->
                h.getText()
                    .equals(
                        UserNotificationTexts.getNotificationTitleText(messageProperties, item)))
            .count());
    assertTrue(
        tree(dialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getText().equals(messageProperties.getNotificationsSourceSystem())
                    && span.hasClassName("portal-notification-source-system")));
    assertTrue(
        tree(dialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(span ->
                span.getText()
                    .equals(
                        UserNotificationTexts.formatNotificationTimestamp(
                            item.createdAt(), testUi.getLocale()))));
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailReadAndHistoryActionsPreserveTheirContracts(
      String language, boolean compact, boolean read) {
    NotificationDetailFixture fixture = openNotificationDetail(language, compact, read);
    UserNotificationsPanel panel = fixture.panel();
    Dialog dialog = fixture.dialog();
    Button mark = detailButton(dialog, "cosmos-notification-detail-mark-read");
    assertEquals(!read, mark.isVisible());
    assertEquals("icons/lucide/check.svg", ((SvgIcon) mark.getIcon()).getSrc());
    Button history = detailButton(dialog, "cosmos-notification-detail-history");
    assertEquals(compact, history.isVisible());
    assertEquals(messageProperties.getNotificationsHistory(), history.getText());
    assertEquals(messageProperties.getNotificationsHistory(), history.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getNotificationsHistory(), history.getTooltip().getText());
    assertEquals("icons/lucide/bell.svg", ((SvgIcon) history.getIcon()).getSrc());
    Button account = detailButton(dialog, "cosmos-notification-detail-account");
    assertEquals(messageProperties.getNotificationsOpenAccount(), account.getText());
    assertEquals(
        messageProperties.getNotificationsOpenAccount(), account.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getNotificationsOpenAccount(), account.getTooltip().getText());
    assertEquals("icons/lucide/lock-keyhole.svg", ((SvgIcon) account.getIcon()).getSrc());
    assertTrue(
        account.getParent().orElseThrow().hasClassName("cosmos-notification-detail-primary"));
    assertTrue(
        mark.getParent().orElseThrow().hasClassName("cosmos-notification-detail-read-actions"));
    assertTrue(
        tree(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(button ->
                button.getText().equals(messageProperties.getNotificationsClose())
                    && !button.getThemeNames().contains("primary")
                    && button.getIcon() instanceof SvgIcon icon
                    && "icons/lucide/x.svg".equals(icon.getSrc())));
    if (!read) {
      doAnswer(
              invocation -> {
                item =
                    new Item(
                        item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
                when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
                return null;
              })
          .when(userNotificationService)
          .markRead(mockHttpSession, List.of(7L));
      mark.click();
      confirmPending(panel);
      verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
      assertTrue(dialog.isOpened());
      assertEquals(messageProperties.getNotificationsRead(), detailState(dialog).getText());
      assertTrue(
          tree(dialog)
              .noneMatch(c -> c.hasClassName("cosmos-notification-detail-message--unread")));
      assertEquals("true", detailState(dialog).getElement().getAttribute("data-read"));
      assertFalse(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
      assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-row--unread")));
    }
    dialog.close();
    rowTitle(panel).click();
    assertEquals(
        messageProperties.getNotificationsRead(),
        detailState(field(panel, "notificationDetailDialog")).getText());
  }

  private record NotificationDetailFixture(UserNotificationsPanel panel, Dialog dialog) {}

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailDeleteConfirmsOnlyTheOpenedNotificationAndCancelKeepsIt(
      String language, boolean compact, boolean read) {
    NotificationDetailFixture fixture = openNotificationDetail(language, compact, read);
    UserNotificationsPanel panel = fixture.panel();
    Dialog dialog = fixture.dialog();
    Button delete = detailButton(dialog, "cosmos-notification-detail-delete");
    assertEquals(messageProperties.getNotificationsDismiss(), delete.getText());
    assertEquals(messageProperties.getNotificationsDismiss(), delete.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getNotificationsDismiss(), delete.getTooltip().getText());
    assertEquals("icons/lucide/trash-2.svg", ((SvgIcon) delete.getIcon()).getSrc());
    assertTrue(delete.hasClassName("cosmos-delete-button"));
    delete.click();
    PortalConfirmationDialog confirmation = field(panel, "notificationActionConfirmation");
    assertTrue(confirmation.isOpened());
    assertTrue(dialog.isOpened());
    verify(userNotificationService, never()).dismiss(any(), any());
    Button cancel = field(confirmation, "cancelButton");
    cancel.click();
    assertFalse(confirmation.isOpened());
    assertTrue(dialog.isOpened());
    verify(userNotificationService, never()).dismiss(any(), any());
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    delete.click();
    confirmPending(panel);
    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertFalse(dialog.isOpened());
  }

  private NotificationDetailFixture openNotificationDetail(
      String language, boolean compact, boolean read) {

    localeReference.set(Locale.forLanguageTag(language));
    item =
        new Item(
            item.id(),
            item.type(),
            item.title(),
            item.body(),
            item.createdAt(),
            read,
            item.supportTicketNumber(),
            item.relevance());
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(read ? List.of() : List.of(7L));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, compact);
    testUi.add(panel);
    panel.refreshInbox();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "notificationDetailDialog");
    return new NotificationDetailFixture(panel, dialog);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void retiredTicketStatusUpdatesOpenDetailWithoutReadingOrLosingItsHistory(String language) {
    item =
        new Item(
            7L,
            UserNotificationType.RECOVERY_REQUEST,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE);
    NotificationDetailFixture fixture = openNotificationDetail(language, true, false);
    item =
        new Item(
            7L,
            item.type(),
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_ASSIGNED);
    fixture.panel().refreshInbox();
    assertTrue(fixture.dialog().isOpened());
    assertEquals(
        1,
        tree(fixture.dialog())
            .filter(com.vaadin.flow.component.badge.Badge.class::isInstance)
            .filter(part ->
                part.getElement()
                    .getTextRecursively()
                    .equals(messageProperties.getNotificationsTicketAssigned()))
            .count());
    Item shown = field(fixture.panel(), "openedNotification");
    assertFalse(shown.read());
    assertFalse(shown.unread());
    assertFalse(detailButton(fixture.dialog(), "cosmos-notification-detail-mark-read").isVisible());
    verify(userNotificationService, never()).markRead(any(), any());

    item =
        new Item(
            7L,
            item.type(),
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            de.derpeterson.app.model.enums.UserNotificationRelevance.TICKET_CLOSED);
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.inboxItem(mockHttpSession, 7L)).thenReturn(Optional.of(item));
    fixture.panel().refreshInbox();
    assertTrue(fixture.dialog().isOpened());
    assertTrue(
        tree(fixture.dialog())
            .filter(com.vaadin.flow.component.badge.Badge.class::isInstance)
            .anyMatch(part ->
                part.getElement()
                    .getTextRecursively()
                    .equals(messageProperties.getSupportClosed())));
  }

  @ParameterizedTest
  @EnumSource(
      value = UserNotificationType.class,
      names = {"RECOVERY_REQUEST", "SUPPORT_REPLY", "SUPPORT_REQUESTER_CLOSED"})
  void ticketNotificationsDoNotOfferUnrelatedAccountSecurityNavigation(UserNotificationType type) {
    item = new Item(7L, type, null, null, Instant.EPOCH, false);
    NotificationDetailFixture fixture = openNotificationDetail("de", true, false);
    assertFalse(
        tree(fixture.dialog())
            .filter(Button.class::isInstance)
            .anyMatch(button -> button.hasClassName("cosmos-notification-detail-account")));
    assertTrue(detailButton(fixture.dialog(), "cosmos-notification-detail-mark-read").isVisible());
    assertTrue(detailButton(fixture.dialog(), "cosmos-notification-detail-delete").isVisible());
    assertTrue(detailButton(fixture.dialog(), "cosmos-notification-detail-history").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ticketDetailsOfferADestinationOnlyForAnAuthorizedAdministrator(String language) {
    item =
        new Item(
            7L,
            UserNotificationType.SUPPORT_REPLY,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE);
    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(true);
    NotificationDetailFixture fixture = openNotificationDetail(language, true, false);
    Button ticket = detailButton(fixture.dialog(), "cosmos-notification-detail-ticket");
    assertEquals(messageProperties.getNotificationsOpenTicket(), ticket.getText());
    assertTrue(ticket.getThemeNames().contains("primary"));
    Button more = detailButton(fixture.dialog(), "cosmos-notification-detail-more");
    Popover menu =
        tree(fixture.dialog())
            .filter(Popover.class::isInstance)
            .map(Popover.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(more, menu.getTarget());
    menu.open();
    detailButton(fixture.dialog(), "cosmos-notification-detail-delete").click();
    assertFalse(menu.isOpened());
    assertTrue(
        ((PortalConfirmationDialog) field(fixture.panel(), "notificationActionConfirmation"))
            .isOpened());
    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(false);
    fixture.panel().refreshTexts();
    assertTrue(
        tree(fixture.dialog())
            .noneMatch(button -> button.hasClassName("cosmos-notification-detail-ticket")));
    assertFalse(
        detailButton(fixture.dialog(), "cosmos-notification-detail-close")
            .getThemeNames()
            .contains("primary"));
  }

  @ParameterizedTest
  @EnumSource(
      value = UserNotificationType.class,
      names = {"RECOVERY_REQUEST", "SUPPORT_REPLY", "SUPPORT_REQUESTER_CLOSED"})
  void ticketDestinationUsesThePublicNumberWithoutHash(UserNotificationType type) {
    item =
        new Item(
            7L,
            type,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10003",
            de.derpeterson.app.model.enums.UserNotificationRelevance.ACTIVE);
    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(true);
    NotificationDetailFixture fixture = openNotificationDetail("de", true, false);
    UI navigator = mock(UI.class);
    UI.setCurrent(navigator);
    try {
      detailButton(fixture.dialog(), "cosmos-notification-detail-ticket").click();
    } finally {
      UI.setCurrent(testUi);
    }
    ArgumentCaptor<QueryParameters> parameters = ArgumentCaptor.forClass(QueryParameters.class);
    verify(navigator).navigate(eq(AdminView.class), parameters.capture());
    assertEquals(List.of("support"), parameters.getValue().getParameters().get("section"));
    assertEquals(List.of("10003"), parameters.getValue().getParameters().get("ticket"));
    assertFalse(fixture.dialog().isOpened());
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
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "notificationDetailDialog");
    dialog.close();
    rowTitle(panel).click();
    dialog = field(panel, "notificationDetailDialog");
    localeReference.set(Locale.forLanguageTag(language));
    panel.refreshTexts();
    assertEquals(messageProperties.getNotificationsUnread(), detailState(dialog).getText());
    assertEquals(
        messageProperties.getNotificationsMarkRead(),
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
            .anyMatch(span ->
                span.getText().contains(messageProperties.getNotificationsSourceAdministration())));
    assertFalse(
        tree(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(button -> button.hasClassName("cosmos-notification-detail-account")));
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void failedExplicitReadDoesNotShowFalseSuccess() {
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    rowTitle(panel).click();
    Dialog dialog = field(panel, "notificationDetailDialog");
    doThrow(new IllegalStateException("isolated failure"))
        .when(userNotificationService)
        .markRead(mockHttpSession, List.of(7L));
    detailButton(dialog, "cosmos-notification-detail-mark-read").click();
    confirmPending(panel);
    assertTrue(dialog.isOpened());
    assertEquals(messageProperties.getNotificationsUnread(), detailState(dialog).getText());
    assertTrue(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
  }

  @Test
  void pushReadRefreshUpdatesOpenDetailAndHeaderCount() {
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    popover.open();
    UserNotificationsPanel panel = field(bell, "notificationPreviewPanel");
    rowTitle(panel).click();
    Dialog dialog = field(panel, "notificationDetailDialog");
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    when(userNotificationService.unreadCount(mockHttpSession)).thenReturn(0L);
    userNotificationBroadcaster.publish(Set.of(5L));
    assertTrue(dialog.isOpened());
    assertTrue(popover.isOpened());
    assertEquals(messageProperties.getNotificationsRead(), detailState(dialog).getText());
    assertFalse(detailButton(dialog, "cosmos-notification-detail-mark-read").isVisible());
    assertFalse(((Span) field(bell, "unreadCountBadge")).isVisible());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void openingDetailRechecksAccessWithoutMutatingReadStatus() {
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, true);
    testUi.add(panel);
    panel.refreshInbox();
    when(userNotificationService.ownerId(mockHttpSession))
        .thenThrow(new AccessDeniedException("expired"));
    rowTitle(panel).click();
    assertNull(field(panel, "notificationDetailDialog"));
    verify(userNotificationService, never()).markRead(any(), any());
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
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    assertEquals(item.title(), rowTitle(panel).getText());
    rowTitle(panel).click();
    Dialog detail = field(panel, "notificationDetailDialog");
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
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    ((Button) field(panel, "markAllReadButton")).click();
    verify(userNotificationService, never()).markRead(any(), any());
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of(7L, 9L));
    panel.refreshInbox();
    confirmPending(panel);
    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
    verify(userNotificationService, never()).markRead(mockHttpSession, List.of(7L, 9L));
    ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).setValue(ReadFilter.UNREAD);
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 10);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, Integer.MAX_VALUE})
  void numberedInboxNavigationClampsExtremeIndicesWithoutIntegerOverflow(int page) {
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    ReflectionTestUtils.setField(panel, "page", page);
    ReflectionTestUtils.setField(panel, "totalPages", 20);
    ReflectionTestUtils.invokeMethod(panel, "renderPageNumbers");
    Div numbers = field(panel, "pageNumbers");
    List<String> labels =
        tree(numbers)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .map(Button::getText)
            .toList();
    assertEquals(
        page < 0 ? List.of("1", "2", "3", "4", "20") : List.of("1", "17", "18", "19", "20"),
        labels);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxArchiveHeaderCountsSearchPagingAndLanguageWorkTogether(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(6L);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of(7L, 9L));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 45));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(List.of(item), PageRequest.of(c.getArgument(4), c.getArgument(5)), 1));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    assertTrue(
        tree(panel)
            .anyMatch(
                c -> c instanceof SvgIcon icon && "icons/lucide/bell.svg".equals(icon.getSrc())));
    assertEquals(
        messageProperties.getNotificationsInboxDescription(),
        ((Span) field(panel, "inboxDescription")).getText());
    assertEquals("6", ((Span) field(panel, "totalCount")).getText());
    assertEquals("2", ((Span) field(panel, "unreadCount")).getText());
    assertEquals(
        "icons/lucide/refresh-cw.svg",
        ((SvgIcon) ((Button) field(panel, "refreshInboxButton")).getIcon()).getSrc());
    assertReadAllIcon((Button) field(panel, "markAllReadButton"));
    ((Button) field(panel, "lastPageButton")).click();
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 4, 10);
    assertFalse(((Button) field(panel, "nextPageButton")).isEnabled());
    ((Button) field(panel, "firstPageButton")).click();
    assertEquals(0, (int) field(panel, "page"));
    Div numbers = (Div) field(panel, "pageNumbers");
    Button third =
        tree(numbers)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(b -> b.getText().equals("3"))
            .findFirst()
            .orElseThrow();
    third.click();
    TextField search = field(panel, "notificationSearchField");
    search.setValue("alpha");
    assertEquals(0, (int) field(panel, "page"));
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.ALL, "alpha", Set.of(), 0, 10);
    assertEquals("6", ((Span) field(panel, "totalCount")).getText());
    assertEquals("2", ((Span) field(panel, "unreadCount")).getText());
    ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).setValue(ReadFilter.UNREAD);
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 10);
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals("alpha", search.getValue());
    assertEquals(messageProperties.getNotificationsSearch(), search.getPlaceholder());
    assertEquals(
        ReadFilter.UNREAD, ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).getValue());
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), eq("no-hit"), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    search.setValue("no-hit");
    assertTrue(
        tree(panel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span -> span.getText().equals(messageProperties.getNotificationsSearchEmpty())));
    assertFalse(((Button) field(panel, "firstPageButton")).isEnabled());
    assertFalse(((Button) field(panel, "lastPageButton")).isEnabled());
    search.clear();
    verify(userNotificationService, atLeastOnce()).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 10);
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxPageSizeControlsAndChangesResetThePage(String language) {
    UserNotificationsPanel panel = inboxWithFortyFiveItems(language);
    com.vaadin.flow.component.combobox.ComboBox<Integer> sizes = field(panel, "pageSize");
    Span pageTotal = field(panel, "pageTotal");
    assertEquals(List.of(10, 20, 50), sizes.getListDataView().getItems().toList());
    assertEquals(10, sizes.getValue());
    assertFalse(sizes.isAllowCustomValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(), sizes.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(),
        ((Span) field(panel, "pageLabel")).getText());
    assertEquals(messageProperties.getPortalPaginationRange(1, 10, 45), pageTotal.getText());
    ((Button) field(panel, "lastPageButton")).click();
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 4, 10);
    sizes.setValue(20);
    assertEquals(0, (int) field(panel, "page"));
    verify(userNotificationService, atLeastOnce()).inbox(mockHttpSession, ReadFilter.ALL, 0, 20);
    ((Button) field(panel, "nextPageButton")).click();
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 1, 20);
    sizes.setValue(50);
    assertEquals(0, (int) field(panel, "page"));
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 0, 50);
    assertFalse(((Button) field(panel, "nextPageButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxPagingKeepsSearchUnreadLanguageAndEmptyFailureStates(String language) {
    UserNotificationsPanel panel = inboxWithFortyFiveItems(language);
    com.vaadin.flow.component.combobox.ComboBox<Integer> sizes = field(panel, "pageSize");
    Span pageTotal = field(panel, "pageTotal");
    sizes.setValue(50);
    ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).setValue(ReadFilter.UNREAD);
    TextField search = field(panel, "notificationSearchField");
    search.setValue("alpha");
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 50);
    sizes.setValue(20);
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 20);
    ((Button) field(panel, "lastPageButton")).click();
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 2, 20);
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals(20, sizes.getValue());
    assertEquals("alpha", search.getValue());
    assertEquals(
        ReadFilter.UNREAD, ((RadioButtonGroup<ReadFilter>) field(panel, "readFilter")).getValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(), sizes.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getPortalPaginationRange(41, 45, 45), pageTotal.getText());
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), eq("no-hit"), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    search.setValue("no-hit");
    assertEquals(messageProperties.getPortalPaginationRange(0, 0, 0), pageTotal.getText());
    assertFalse(((Button) field(panel, "firstPageButton")).isEnabled());
    assertFalse(((Button) field(panel, "lastPageButton")).isEnabled());
    panel.unavailable();
    assertEquals("–", pageTotal.getText());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  private UserNotificationsPanel inboxWithFortyFiveItems(String language) {

    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(
                Collections.nCopies(
                    Math.min(
                        (int) c.getArgument(3),
                        45 - (int) c.getArgument(2) * (int) c.getArgument(3)),
                    item),
                PageRequest.of(c.getArgument(2), c.getArgument(3)),
                45));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(
                Collections.nCopies(
                    Math.min(
                        (int) c.getArgument(5),
                        45 - (int) c.getArgument(4) * (int) c.getArgument(5)),
                    item),
                PageRequest.of(c.getArgument(4), c.getArgument(5)),
                45));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    return panel;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxToolbarUsesSharedFilterAndIconToolsAndSearchesReadItems(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenReturn(new PageImpl<>(List.of(item)));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    RadioButtonGroup<ReadFilter> filter = field(panel, "readFilter");
    assertEquals("notifications-inbox-filter", filter.getId().orElseThrow());
    assertEquals(ReadFilter.ALL, filter.getValue());
    assertEquals(
        List.of(ReadFilter.ALL, ReadFilter.UNREAD, ReadFilter.READ),
        filter.getListDataView().getItems().toList());
    assertTrue(tree(panel).noneMatch(Checkbox.class::isInstance));
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-inbox-hint")));
    Button reload = field(panel, "refreshInboxButton");
    Button markAll = field(panel, "markAllReadButton");
    assertEquals("", reload.getText());
    assertEquals("", markAll.getText());
    assertEquals(messageProperties.getNotificationsRefresh(), reload.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getNotificationsRefresh(), reload.getTooltip().getText());
    assertEquals(messageProperties.getNotificationsMarkAll(), markAll.getTooltip().getText());
    filter.setValue(ReadFilter.READ);
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.READ, 0, 10);
    TextField search = field(panel, "notificationSearchField");
    search.setValue("alpha");
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.READ, "alpha", Set.of(), 0, 10);
    ReflectionTestUtils.setField(panel, "page", 4);
    filter.setValue(ReadFilter.UNREAD);
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 10);
    filter.setValue(ReadFilter.ALL);
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.ALL, "alpha", Set.of(), 0, 10);
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    panel.refreshTexts();
    assertEquals(ReadFilter.ALL, filter.getValue());
    assertEquals(
        messageProperties.getNotificationsRead(),
        filter.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(messageProperties.getNotificationsRefresh(), reload.getTooltip().getText());
    assertEquals(messageProperties.getNotificationsMarkAll(), markAll.getTooltip().getText());
    assertEquals("alpha", search.getValue());
    reload.click();
    markAll.click();
    confirmPending(panel);
    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void inboxCardSingleClickAndExplicitReadRemainSeparate(String language, boolean read) {
    localeReference.set(Locale.forLanguageTag(language));
    item = new Item(item.id(), item.type(), item.title(), item.body(), item.createdAt(), read);
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(read ? List.of() : List.of(7L));
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    Component row =
        tree(panel)
            .filter(c -> c.hasClassName("cosmos-notification-inbox-row"))
            .findFirst()
            .orElseThrow();
    assertEquals(!read, row.hasClassName("cosmos-notification-row--unread"));
    assertTrue(
        tree(row)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div ->
                div.getText()
                    .equals(
                        UserNotificationTexts.getNotificationBodyText(messageProperties, item))));
    Element element = row.getElement();
    ElementListenerMap listeners =
        element.getNode().getFeature(com.vaadin.flow.internal.nodefeature.ElementListenerMap.class);
    ObjectNode data = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    Set<String> filters = listeners.getExpressions("click");
    assertFalse(filters.isEmpty());
    filters.forEach(expression -> data.put(expression, false));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "click", data));
    assertNull(field(panel, "notificationDetailDialog"));
    filters.forEach(expression -> data.put(expression, true));
    listeners.fireEvent(new com.vaadin.flow.dom.DomEvent(element, "click", data));
    Dialog detail = field(panel, "notificationDetailDialog");
    assertTrue(detail.isOpened());
    assertEquals(
        read
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        detailState(detail).getText());
    verify(userNotificationService, never()).markRead(any(), any());
    detail.close();
    Button mark =
        tree(panel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(b -> b.hasClassName("cosmos-notification-inbox-mark-read"))
            .findFirst()
            .orElseThrow();
    assertEquals(!read, mark.isVisible());
    assertEquals("", mark.getText());
    assertEquals(messageProperties.getNotificationsMarkRead(), mark.getTooltip().getText());
    if (!read) {
      doAnswer(
              c -> {
                item =
                    new Item(
                        item.id(), item.type(), item.title(), item.body(), item.createdAt(), true);
                when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
                return null;
              })
          .when(userNotificationService)
          .markRead(mockHttpSession, List.of(7L));
      mark.click();
      confirmPending(panel);
      verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
      assertEquals("0", ((Span) field(panel, "unreadCount")).getText());
      assertEquals("1", ((Span) field(panel, "totalCount")).getText());
      assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-row--unread")));
    }
  }

  @Test
  void paginatedInboxRequestsNextPageAndBack() {
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(c ->
            new PageImpl<>(List.of(item), PageRequest.of(c.getArgument(2), c.getArgument(3)), 30));
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    ((Button) field(panel, "nextPageButton")).click();
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 1, 10);
    ((Button) field(panel, "previousPageButton")).click();
    verify(userNotificationService, times(2)).inbox(mockHttpSession, ReadFilter.ALL, 0, 10);
  }

  @Test
  void revokedSessionClearsContentsAndOpenDetail() {
    UserNotificationsPanel panel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    testUi.add(panel);
    panel.refreshInbox();
    rowTitle(panel).click();
    Dialog detail = field(panel, "notificationDetailDialog");
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenThrow(new AccessDeniedException("expired"));
    panel.refreshInbox();
    assertFalse(detail.isOpened());
    assertTrue(tree(panel).noneMatch(c -> c.hasClassName("cosmos-notification-title")));
    assertFalse(((Button) field(panel, "markAllReadButton")).isEnabled());
  }

  @Test
  void liveBellCapsBadgeAndRefreshesOnlyItsOwnUserAndDetachesCleanly() {
    when(userNotificationService.unreadCount(mockHttpSession)).thenReturn(125L);
    Button button = new Button();
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, button);
    testUi.add(bell);
    Span count = field(bell, "unreadCountBadge");
    assertEquals("99+", count.getText());
    assertTrue(count.isVisible());
    assertTrue(button.getAriaLabel().orElseThrow().contains("125"));
    clearInvocations(userNotificationService);
    userNotificationBroadcaster.publish(Set.of(999L));
    verify(userNotificationService, never()).unreadCount(any());
    userNotificationBroadcaster.publish(Set.of(5L));
    verify(userNotificationService).unreadCount(mockHttpSession);
    testUi.remove(bell);
    clearInvocations(userNotificationService);
    userNotificationBroadcaster.publish(Set.of(5L));
    verify(userNotificationService, never()).unreadCount(any());
  }

  @Test
  void bellOpensOnClickAndClosesWithEscapeOrOutsideWithoutMarkingAllRead() {
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, new Button());
    testUi.add(bell);
    Popover popover = field(bell, "notificationPopover");
    assertTrue(popover.isOpenOnClick());
    assertFalse(popover.isOpenOnFocus());
    assertFalse(popover.isOpenOnHover());
    assertTrue(popover.isCloseOnEsc());
    assertTrue(popover.isCloseOnOutsideClick());
    popover.open();
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void bellDoesNotReusePreviousCountOrContentsWhenSessionRevoked() {
    Button button = new Button();
    UserNotificationBell bell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, button);
    testUi.add(bell);
    when(userNotificationService.unreadCount(mockHttpSession))
        .thenThrow(new AccessDeniedException("expired"));
    userNotificationBroadcaster.publish(Set.of(5L));
    assertFalse(button.isEnabled());
    assertFalse(((Span) field(bell, "unreadCountBadge")).isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void systemTypesRenderInCurrentLanguageAndManualBodyStaysOriginal(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    for (UserNotificationType type : UserNotificationType.values()) {
      Item message = new Item(1L, type, "Original title", "Original body", Instant.now(), false);
      assertFalse(
          UserNotificationTexts.getNotificationTitleText(messageProperties, message).isBlank());
      assertFalse(
          UserNotificationTexts.getNotificationBodyText(messageProperties, message).isBlank());
      if (type == UserNotificationType.ADMIN_ANNOUNCEMENT) {
        assertEquals(
            "Original body",
            UserNotificationTexts.getNotificationBodyText(messageProperties, message));
      }
    }
    assertEquals(
        language.equals("de") ? "Passwort geändert" : "Password changed",
        UserNotificationTexts.getNotificationTitleText(messageProperties, item));
  }

  @Test
  void springWiresTheSharedBellForAuthenticatedViewsOnly() {
    SecurityService security = mock(de.derpeterson.app.security.SecurityService.class);
    UserService users = mock(de.derpeterson.app.service.UserService.class);
    UserAvatarService testAvatars = mock(de.derpeterson.app.service.UserAvatarService.class);
    UserStatusBroadcaster status = new de.derpeterson.app.websocket.UserStatusBroadcaster();
    UserEntity identity =
        de.derpeterson.app.model.UserEntity.builder()
            .id(5L)
            .firstName("Own")
            .lastName("User")
            .enabled(true)
            .build();
    when(security.getCurrentUser(mockHttpRequest)).thenReturn(Optional.of(identity));
    class HeaderView extends de.derpeterson.app.ui.base.TrackedUserAppLayout {
      HeaderView() {
        super(
            UserNotificationsUiTest.this.messageProperties,
            security,
            users,
            status,
            UserNotificationsUiTest.this.mockHttpRequest,
            testAvatars);
      }
    }
    try (AnnotationConfigApplicationContext context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.registerBean(UserNotificationService.class, () -> userNotificationService);
      context.refresh();
      HeaderView authenticated = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(authenticated);
      AccountHeaderActions actions = field(authenticated, "accountHeaderActions");
      assertTrue(tree(actions).anyMatch(UserNotificationBell.class::isInstance));
      testUi.add(actions);
      verify(userNotificationService).ownerId(mockHttpSession);
      when(security.getCurrentUser(mockHttpRequest)).thenReturn(Optional.empty());
      HeaderView anonymous = new HeaderView();
      context.getAutowireCapableBeanFactory().autowireBean(anonymous);
      AccountHeaderActions hidden = field(anonymous, "accountHeaderActions");
      assertFalse(hidden.isVisible());
      assertTrue(tree(hidden).noneMatch(UserNotificationBell.class::isInstance));
    }
  }

  AdminUserNotificationsSection admin() {
    when(userNotificationService.targets(mockHttpSession))
        .thenReturn(List.of(new Target(5L, "Own", "own@example.test")));
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection section =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    section.activate();
    return section;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryExpandsMessagesIndependentlyAndPaginates(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminUserNotificationsSection section = admin();
    String message = "<b>Plain text</b>\nSecond line";
    when(userNotificationService.sent(mockHttpSession, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new Sent(1L, "First", message, Instant.now(), 2),
                    new Sent(2L, "Second", "Other body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    when(userNotificationService.sent(mockHttpSession, 4, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(3L, "Last", "Last body", Instant.now(), 1)),
                PageRequest.of(4, 5),
                21));
    section.activate();
    assertEquals(
        messageProperties.getNotificationsHistoryCount(21),
        ((Span) field(section, "historyCount")).getText());
    assertEquals(
        messageProperties.getPortalPaginationRange(1, 5, 21),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    Div history = field(section, "sentNotificationHistory");
    List<Details> details =
        tree(history)
            .filter(com.vaadin.flow.component.details.Details.class::isInstance)
            .map(com.vaadin.flow.component.details.Details.class::cast)
            .toList();
    List<Button> toggles =
        tree(history).filter(Button.class::isInstance).map(Button.class::cast).toList();
    assertEquals(2, details.size());
    assertTrue(details.stream().noneMatch(com.vaadin.flow.component.details.Details::isOpened));
    toggles.getFirst().click();
    assertTrue(details.getFirst().isOpened());
    assertFalse(details.getLast().isOpened());
    assertEquals("true", toggles.getFirst().getElement().getAttribute("aria-expanded"));
    assertEquals(messageProperties.getNotificationsHistoryDetails(), toggles.getFirst().getText());
    assertFalse(
        tree(details.getFirst())
            .filter(H4.class::isInstance)
            .map(H4.class::cast)
            .anyMatch(title -> title.getText().equals("First")));
    assertTrue(
        tree(details.getFirst())
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(label ->
                label.getText().equals(messageProperties.getNotificationsMessageContent())));
    assertFalse(
        tree(details.getFirst())
            .anyMatch(meta -> meta.hasClassName("cosmos-notification-sent-detail-meta")));
    assertTrue(
        tree(details.getFirst())
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(message)));
    assertTrue(tree(history).noneMatch(Html.class::isInstance));
    assertTrue(
        tree(details.getFirst())
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(label ->
                label.getText().equals(messageProperties.getNotificationsHistoryDelivery())));
    assertTrue(
        tree(details.getFirst())
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(label ->
                label.getText().equals(messageProperties.getNotificationsHistoryImmutableLog())));
    toggles.getFirst().click();
    assertFalse(details.getFirst().isOpened());
    assertEquals("false", toggles.getFirst().getElement().getAttribute("aria-expanded"));
    ((Button) field(field(section, "historyPagination"), "lastPageButton")).click();
    assertEquals(
        messageProperties.getPortalPaginationRange(21, 21, 21),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    assertFalse(
        ((Button) field(field(section, "historyPagination"), "nextPageButton")).isEnabled());
    assertTrue(
        ((Button) field(field(section, "historyPagination"), "previousPageButton")).isEnabled());
    ((Button) field(field(section, "historyPagination"), "firstPageButton")).click();
    assertEquals(0, ((Integer) field(section, "page")).intValue());
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    section.activate();
    assertEquals(
        messageProperties.getPortalPaginationRange(0, 0, 0),
        ((Span) field(field(section, "historyPagination"), "totalLabel")).getText());
    assertFalse(
        ((Button) field(field(section, "historyPagination"), "nextPageButton")).isEnabled());
    assertFalse(
        ((Button) field(field(section, "historyPagination"), "previousPageButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryPageSizeSelectionResetsThePageAndLoadsTheSelectedSize(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminUserNotificationsSection section = admin();
    Object pagination = field(section, "historyPagination");
    com.vaadin.flow.component.combobox.ComboBox<Integer> sizes =
        field(pagination, "pageSizeSelector");
    assertEquals(List.of(5, 10, 15), sizes.getListDataView().getItems().toList());
    assertEquals(5, sizes.getValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(), sizes.getAriaLabel().orElseThrow());
    for (int size : List.of(10, 15, 5)) {
      when(userNotificationService.sent(mockHttpSession, 0, size)).thenReturn(Page.empty());
      ReflectionTestUtils.setField(section, "page", 3);
      sizes.setValue(size);
      assertEquals(0, ((Integer) field(section, "page")).intValue());
      verify(userNotificationService, atLeastOnce()).sent(mockHttpSession, 0, size);
      assertEquals(size, ((Integer) field(section, "historyPageSize")).intValue());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceKeepsSentHistoryPaginationAvailable(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminUserNotificationsSection section = admin();
    when(userNotificationService.sent(mockHttpSession, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(1L, "Previous", "Body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    section.activate();
    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    section.refreshDeliveryState();
    assertTrue(((Button) field(field(section, "historyPagination"), "nextPageButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceDisablesComposeAndRequiresFreshReviewWhenReopened(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminUserNotificationsSection section = admin();

    ((TextField) field(section, "notificationTitleField")).setValue("Subject");
    ((TextArea) field(section, "notificationBodyField")).setValue("Message");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    Preview checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, checked.selection())).thenReturn(checked);
    Button review = field(section, "previewButton");
    Button send = field(section, "sendNotificationButton");
    review.click();
    assertTrue(send.isEnabled());
    Div meta = field(section, "deliveryMeta");
    assertEquals(
        messageProperties.getNotificationsInternal(),
        ((Span) field(section, "deliveryLabel")).getText());
    assertEquals("open", meta.getElement().getAttribute("data-state"));
    com.vaadin.flow.component.shared.Tooltip statusHint = field(section, "deliveryTooltip");
    assertEquals(
        messageProperties.getNotificationsInternal()
            + ": "
            + messageProperties.getMaintenanceStatusAvailable(),
        statusHint.getText());
    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    section.refreshDeliveryState();
    assertEquals("active", meta.getElement().getAttribute("data-state"));
    assertTrue(
        meta.getElement()
            .getAttribute("aria-label")
            .contains(messageProperties.getMaintenanceStatusActive()));
    assertEquals(
        messageProperties.getNotificationsInternal()
            + ": "
            + messageProperties.getMaintenanceStatusActive(),
        statusHint.getText());
    com.vaadin.flow.component.orderedlayout.VerticalLayout form = field(section, "composeForm");
    assertFalse(form.isEnabled());
    assertEquals("true", form.getElement().getAttribute("aria-disabled"));
    assertFalse(((TextField) field(section, "notificationTitleField")).isEnabled());
    assertFalse(((TextArea) field(section, "notificationBodyField")).isEnabled());
    assertFalse(((Checkbox) field(section, "sendToEveryoneCheckbox")).isEnabled());
    assertFalse(review.isEnabled());
    assertFalse(send.isEnabled());
    assertNull(field(section, "preview"));
    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
    section.refreshDeliveryState();
    assertTrue(form.isEnabled());
    assertEquals("open", meta.getElement().getAttribute("data-state"));
    assertEquals("Subject", ((TextField) field(section, "notificationTitleField")).getValue());
    assertFalse(send.isEnabled());
    review.click();
    assertTrue(send.isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void deliveryActionRechecksMaintenanceBeforeNextPoll(MaintenanceService.State state) {
    AdminUserNotificationsSection section = admin();
    ((TextField) field(section, "notificationTitleField")).setValue("Subject");
    ((TextArea) field(section, "notificationBodyField")).setValue("Message");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    Preview checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, checked.selection())).thenReturn(checked);
    Button review = field(section, "previewButton");
    review.click();
    when(userNotificationService.deliveryState()).thenReturn(state);
    ((Button) field(section, "sendNotificationButton")).click();
    assertFalse(
        ((com.vaadin.flow.component.orderedlayout.VerticalLayout) field(section, "composeForm"))
            .isEnabled());
    assertNull(field(section, "preview"));
    review.click();
    verify(userNotificationService, times(1)).preview(any(), any());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1000, 12000})
  void deliveryPollingUpdatesStateAndRestoresPreviousIntervalOnDetach(int previousInterval) {
    testUi.setPollInterval(previousInterval);
    int[] unrelatedPolls = {0};
    Registration unrelated = testUi.addPollListener(e -> unrelatedPolls[0]++);
    try {
      AdminUserNotificationsSection section = admin();
      testUi.add(section);
      assertEquals(
          previousInterval > 0 ? Math.min(previousInterval, 5000) : 5000, testUi.getPollInterval());
      when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
      assertFalse(
          ((com.vaadin.flow.component.orderedlayout.VerticalLayout) field(section, "composeForm"))
              .isEnabled());
      assertEquals(1, unrelatedPolls[0]);
      testUi.remove(section);
      assertEquals(previousInterval, testUi.getPollInterval());
      clearInvocations(userNotificationService);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
      assertEquals(2, unrelatedPolls[0]);
      verify(userNotificationService, never()).deliveryState();
    } finally {
      unrelated.remove();
    }
  }

  @Test
  void adminRequiresValidFieldsAndFreshPreviewBeforeSend() {
    AdminUserNotificationsSection section = admin();
    ((Button) field(section, "previewButton")).click();
    assertTrue(((TextField) field(section, "notificationTitleField")).isInvalid());
    assertTrue(((TextArea) field(section, "notificationBodyField")).isInvalid());
    verify(userNotificationService, never()).preview(any(), any());
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    Preview preview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, preview.selection())).thenReturn(preview);
    ((TextField) field(section, "notificationTitleField")).setValue("Title");
    ((TextArea) field(section, "notificationBodyField")).setValue("Body");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    ((Button) field(section, "previewButton")).click();
    assertTrue(((Button) field(section, "sendNotificationButton")).isEnabled());
    ((TextField) field(section, "notificationTitleField")).setValue("Changed");
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void draftErrorsOnlyAppearAfterReviewAndEditingClearsThem(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminUserNotificationsSection section = admin();
    TextField subject = field(section, "notificationTitleField");
    TextArea message = field(section, "notificationBodyField");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "recipientSelector");
    Button review = field(section, "previewButton");
    Button send = field(section, "sendNotificationButton");
    assertEquals(messageProperties.getNotificationsSend(), send.getText());
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
    assertEquals(messageProperties.getNotificationsInvalidSubject(), subject.getErrorMessage());
    assertEquals(messageProperties.getNotificationsInvalidBody(), message.getErrorMessage());
    assertNotEquals(subject.getErrorMessage(), message.getErrorMessage());
    assertEquals(messageProperties.getNotificationsNoRecipients(), recipients.getErrorMessage());
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
    verify(userNotificationService, never()).preview(any(), any());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  private void assertPreviewValidationHint() {
    NotificationHelper helper = NotificationHelper.getInstance();
    Span hintTitle = field(helper, "titleText");
    Html hintMessage = field(helper, "messageText");
    assertEquals(messageProperties.getNotificationsPreviewFailedTitle(), hintTitle.getText());
    assertEquals(
        new Html(
                de.derpeterson.app.helper.ui.HtmlHelper.ensureParagraphTags(
                    messageProperties.getNotificationsPreviewFailedMessage()))
            .getElement()
            .getProperty("innerHTML"),
        hintMessage.getElement().getProperty("innerHTML"));
  }

  @ParameterizedTest
  @EnumSource(
      value = Problem.class,
      names = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED"})
  void rejectedPreviewKeepsRecipientDetailsAtFieldAndUsesGeneralHint(Problem problem) {
    AdminUserNotificationsSection section = admin();
    ((TextField) field(section, "notificationTitleField")).setValue("Subject");
    ((TextArea) field(section, "notificationBodyField")).setValue("Message");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "recipientSelector");
    recipients.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    when(userNotificationService.preview(any(), any())).thenThrow(new Rejected(problem));
    ((Button) field(section, "previewButton")).click();
    assertTrue(recipients.isInvalid());
    assertEquals(
        problem == Problem.NO_RECIPIENTS
            ? messageProperties.getNotificationsNoRecipients()
            : messageProperties.getNotificationsRecipientsChanged(),
        recipients.getErrorMessage());
    assertPreviewValidationHint();
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body", "recipients", "everyone"})
  void changingAnyDraftInputRequiresAnotherSuccessfulReview(String input) {
    AdminUserNotificationsSection section = admin();
    TextField subject = field(section, "notificationTitleField");
    TextArea message = field(section, "notificationBodyField");
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "recipientSelector");
    Checkbox all = field(section, "sendToEveryoneCheckbox");
    Button review = field(section, "previewButton");
    Button send = field(section, "sendNotificationButton");
    subject.setValue("Subject");
    message.setValue("Message");
    recipients.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    Preview checked = new Preview(new Selection(false, Set.of(5L)), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, checked.selection())).thenReturn(checked);
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
        messageProperties.getNotificationsPreviewNeedsReview(),
        ((Span) field(section, "previewStatus")).getText());
    assertEquals(
        "warning",
        ((Span) field(section, "previewStatus")).getElement().getAttribute("data-state"));
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    assertFalse(recipients.isInvalid());
    send.click();
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED", "UNAVAILABLE"})
  void failedRepeatReviewRevokesEarlierApproval(String problem) {
    AdminUserNotificationsSection section = admin();
    ((TextField) field(section, "notificationTitleField")).setValue("Subject");
    ((TextArea) field(section, "notificationBodyField")).setValue("Message");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    Preview checked = new Preview(new Selection(true, Set.of()), List.of(5L));
    RuntimeException rejection =
        problem.equals("UNAVAILABLE")
            ? new IllegalStateException("Isolated review failure")
            : new Rejected(Problem.valueOf(problem));
    when(userNotificationService.preview(mockHttpSession, checked.selection()))
        .thenReturn(checked)
        .thenThrow(rejection);
    Button review = field(section, "previewButton");
    Button send = field(section, "sendNotificationButton");
    review.click();
    assertTrue(send.isEnabled());
    review.click();
    assertFalse(send.isEnabled());
    assertNull(field(section, "preview"));
    assertTrue(((Div) field(section, "previewArea")).isVisible());
    assertEquals(
        messageProperties.getNotificationsPreviewNeedsReview(),
        ((Span) field(section, "previewStatus")).getText());
    assertEquals(
        "warning",
        ((Span) field(section, "previewStatus")).getElement().getAttribute("data-state"));
    assertEquals(
        !problem.equals("UNAVAILABLE"),
        ((TextField) field(section, "everyoneRecipients")).isInvalid());
    send.click();
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body"})
  void overlongDraftIsMarkedOnlyWhenReviewed(String input) {
    AdminUserNotificationsSection section = admin();
    TextField subject = field(section, "notificationTitleField");
    TextArea message = field(section, "notificationBodyField");
    subject.setValue("Subject");
    message.setValue("Message");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    if (input.equals("title")) {
      subject.setValue("x".repeat(UserNotificationService.TITLE_LIMIT + 1));
    } else {
      message.setValue("x".repeat(UserNotificationService.BODY_LIMIT + 1));
    }
    assertFalse(subject.isInvalid());
    assertFalse(message.isInvalid());
    ((Button) field(section, "previewButton")).click();
    assertEquals(input.equals("title"), subject.isInvalid());
    assertEquals(input.equals("body"), message.isInvalid());
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    verify(userNotificationService, never()).preview(any(), any());
  }

  @Test
  void adminSendUsesSameRequestAfterAmbiguousFailureAndClearsOnlyAfterSuccess() {
    AdminUserNotificationsSection section = admin();
    Preview preview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, preview.selection())).thenReturn(preview);
    ((TextField) field(section, "notificationTitleField")).setValue("Title");
    ((TextArea) field(section, "notificationBodyField")).setValue("Body");
    ((Checkbox) field(section, "sendToEveryoneCheckbox")).setValue(true);
    ((Button) field(section, "previewButton")).click();
    UUID key = field(section, "requestId");
    when(userNotificationService.send(mockHttpSession, key, "Title", "Body", preview))
        .thenThrow(new IllegalStateException("response interrupted"))
        .thenReturn(new Sent(1L, "Title", "Body", Instant.now(), 1));
    ((Button) field(section, "sendNotificationButton")).click();
    assertEquals(key, (UUID) field(section, "requestId"));
    assertEquals("Title", ((TextField) field(section, "notificationTitleField")).getValue());
    ((Button) field(section, "sendNotificationButton")).click();
    verify(userNotificationService, times(2)).send(mockHttpSession, key, "Title", "Body", preview);
    assertTrue(((TextField) field(section, "notificationTitleField")).isEmpty());
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    assertFalse(((Div) field(section, "previewArea")).isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void recipientCountTracksSelectionRemovalAndLanguage(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    List<Target> accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(accounts);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection section =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    section.activate();
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "recipientSelector");
    Span summary = field(section, "recipientSummary");
    assertEquals(messageProperties.getNotificationsSelectedUsers(0), summary.getText());
    recipients.setValue(Set.of(accounts.getFirst()));
    assertEquals(messageProperties.getNotificationsSelectedUsers(1), summary.getText());
    recipients.setValue(Set.copyOf(accounts));
    assertEquals(messageProperties.getNotificationsSelectedUsers(2), summary.getText());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(messageProperties.getNotificationsSelectedUsers(2), summary.getText());
    recipients.setValue(Set.of(accounts.getLast()));
    assertEquals(messageProperties.getNotificationsSelectedUsers(1), summary.getText());
    recipients.clear();
    assertEquals(messageProperties.getNotificationsSelectedUsers(0), summary.getText());
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void allRecipientsShowsDisabledSummaryAndRestoresIndividualSelection(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    List<Target> accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(accounts);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection section =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    section.activate();
    com.vaadin.flow.component.combobox.MultiSelectComboBox<Target> recipients =
        field(section, "recipientSelector");
    TextField summary = field(section, "everyoneRecipients");
    Checkbox all = field(section, "sendToEveryoneCheckbox");
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
    assertEquals(messageProperties.getNotificationsEveryonePlaceholder(2), summary.getValue());
    assertEquals(messageProperties.getNotificationsRecipients(), summary.getLabel());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(messageProperties.getNotificationsEveryonePlaceholder(2), summary.getValue());
    assertEquals(messageProperties.getNotificationsRecipients(), summary.getLabel());
    all.setValue(false);
    assertFalse(summary.isVisible());
    assertTrue(recipients.isVisible());
    assertTrue(recipients.isEnabled());
    assertEquals(Set.of(accounts.getFirst()), recipients.getSelectedItems());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reviewedPreviewShowsPlainTextCountersRecipientsAndReviewStatus(String language) {
    ComposePreviewFixture fixture = composePreviewFixture(language);
    AdminUserNotificationsSection section = fixture.section();
    TextField titleField = field(section, "notificationTitleField");
    TextArea bodyField = field(section, "notificationBodyField");
    assertEquals(
        titleField.getValue().length() + " / " + UserNotificationService.TITLE_LIMIT,
        ((Span) field(section, "titleCount")).getText());
    assertEquals(
        bodyField.getValue().length() + " / " + UserNotificationService.BODY_LIMIT,
        ((Span) field(section, "bodyCount")).getText());
    assertEquals(UserNotificationService.TITLE_LIMIT, titleField.getMaxLength());
    assertEquals(UserNotificationService.BODY_LIMIT, bodyField.getMaxLength());
    com.vaadin.flow.component.combobox.MultiSelectComboBox<?> recipients =
        field(section, "recipientSelector");
    assertFalse(recipients.isEnabled());
    assertEquals(
        messageProperties.getNotificationsEveryonePlaceholder(4), recipients.getPlaceholder());
    ((Button) field(section, "previewButton")).click();
    Div area = field(section, "previewArea");
    assertTrue(area.isVisible());
    Span status = field(section, "previewStatus");
    assertTrue(status.isVisible());
    assertEquals(messageProperties.getNotificationsPreviewUpdated(), status.getText());
    assertEquals("success", status.getElement().getAttribute("data-state"));
    assertTrue(
        status
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-compose-preview-header"));
    assertEquals(
        3, tree(area).filter(com.vaadin.flow.component.avatar.Avatar.class::isInstance).count());
    assertTrue(tree(area).noneMatch(Html.class::isInstance));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(bodyField.getValue())));
    assertTrue(((Button) field(section, "sendNotificationButton")).isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reviewedPreviewRecipientDetailsKeepLabelsAriaAndAllRecipients(String language) {
    ComposePreviewFixture fixture = composePreviewFixture(language);
    AdminUserNotificationsSection section = fixture.section();
    List<Target> accounts = fixture.accounts();
    ((Button) field(section, "previewButton")).click();
    Div area = field(section, "previewArea");
    Details details =
        tree(area)
            .filter(com.vaadin.flow.component.details.Details.class::isInstance)
            .map(com.vaadin.flow.component.details.Details.class::cast)
            .findFirst()
            .orElseThrow();
    assertFalse(details.isOpened());
    Button detailsButton =
        tree(area)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(
                button -> button.getId().orElse("").equals("admin-notifications-preview-details"))
            .findFirst()
            .orElseThrow();
    assertEquals(messageProperties.getNotificationsRecipientDetails(), detailsButton.getText());
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
    assertEquals(messageProperties.getNotificationsRecipientDetails(), detailsButton.getText());
    assertEquals("true", detailsButton.getElement().getAttribute("aria-expanded"));
    detailsButton.click();
    assertFalse(details.isOpened());
    assertEquals(messageProperties.getNotificationsRecipientDetails(), detailsButton.getText());
    detailsButton.click();
    for (Target account : accounts) {
      assertTrue(
          tree(details)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(span -> span.getText().equals(account.email())));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void editingReviewedPreviewKeepsOldContentAndRequiresAnotherReview(String language) {
    ComposePreviewFixture fixture = composePreviewFixture(language);
    AdminUserNotificationsSection section = fixture.section();
    TextArea bodyField = field(section, "notificationBodyField");
    ((Button) field(section, "previewButton")).click();
    Div area = field(section, "previewArea");
    Span status = field(section, "previewStatus");
    String checkedBody = bodyField.getValue();
    bodyField.setValue("Changed");
    assertTrue(area.isVisible());
    assertNull(field(section, "preview"));
    assertTrue(status.isVisible());
    assertEquals(messageProperties.getNotificationsPreviewNeedsReview(), status.getText());
    assertEquals("warning", status.getElement().getAttribute("data-state"));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals(checkedBody)));
    assertFalse(((Button) field(section, "sendNotificationButton")).isEnabled());
    assertEquals(
        "7 / " + UserNotificationService.BODY_LIMIT,
        ((Span) field(section, "bodyCount")).getText());
    ((Button) field(section, "previewButton")).click();
    assertTrue(((Button) field(section, "sendNotificationButton")).isEnabled());
    assertEquals(messageProperties.getNotificationsPreviewUpdated(), status.getText());
    assertEquals("success", status.getElement().getAttribute("data-state"));
    assertTrue(
        tree(area)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(div -> div.getText().equals("Changed")));
  }

  private record ComposePreviewFixture(
      AdminUserNotificationsSection section, List<Target> accounts) {}

  private ComposePreviewFixture composePreviewFixture(String language) {

    localeReference.set(Locale.forLanguageTag(language));
    List<Target> accounts =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"),
            new Target(7L, "Third User", "third@example.test"),
            new Target(8L, "Fourth User", "fourth@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(accounts);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection section =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    section.activate();
    TextField titleField = field(section, "notificationTitleField");
    TextArea bodyField = field(section, "notificationBodyField");
    Checkbox all = field(section, "sendToEveryoneCheckbox");
    Preview preview = new Preview(new Selection(true, Set.of()), List.of(5L, 6L, 7L, 8L));
    when(userNotificationService.preview(mockHttpSession, preview.selection())).thenReturn(preview);
    titleField.setValue("<b>Title</b>");
    bodyField.setValue("<img src=x onerror=alert(1)>\nPlain text");
    all.setValue(true);
    return new ComposePreviewFixture(section, accounts);
  }
}
