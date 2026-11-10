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
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.HtmlHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.UserNotificationTexts;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserNotificationRelevance;
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
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.UserNotificationsView;
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
import org.junit.jupiter.params.provider.CsvSource;
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
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Java-Komponentenprüfung ohne Browser; sichtbare Geometrie/Frontend bleiben getrennt zu prüfen.
 */
class UserNotificationsUiTest {
  // Aktuelle UI, Sprache und HTTP-Sitzung
  private UI testUi;
  private MockHttpSession mockHttpSession;
  private MockHttpServletRequest mockHttpRequest;
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  // Textkatalog und Benachrichtigungsdienst
  private MessageProperties messageProperties;
  private UserNotificationService userNotificationService;
  private UserNotificationBroadcaster userNotificationBroadcaster;

  // Benachrichtigung, die der simulierte Posteingang zurückgibt
  private Item inboxNotificationItem =
      new Item(
          7L,
          UserNotificationType.PASSWORD_CHANGED,
          null,
          null,
          Instant.parse("2026-10-18T10:00:00Z"),
          false);

  // Testumgebung und Aufräumen
  @BeforeEach
  void createNotificationTestEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    when(vaadinSession.access(any()))
        .thenAnswer(mockInvocation -> {
          ((Command) mockInvocation.getArgument(0)).execute();
          return CompletableFuture.completedFuture(null);
        });
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

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
        .thenAnswer(mockInvocation ->
            new PageImpl<>(
                List.of(inboxNotificationItem),
                PageRequest.of(mockInvocation.getArgument(2), mockInvocation.getArgument(3)),
                1));
  }

  @AfterEach
  void clearNotificationTestEnvironment() {
    testUi.getChildren().toList().forEach(testUi::remove);
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  // Aktivität in der Ticketnavigation
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ticketNavigationActivityFollowsUnreadChangesAndLanguageWithoutOpeningTickets(
      String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    SupportTicketService mockSupportTicketService = mock(SupportTicketService.class);
    SupportTicketBroadcaster supportTicketBroadcaster = mock(SupportTicketBroadcaster.class);
    when(mockSupportTicketService.events()).thenReturn(supportTicketBroadcaster);
    AtomicReference<Consumer<SupportTicketBroadcaster.SupportTicketChangeMessage>>
        ticketChangeListenerReference = new AtomicReference<>();
    when(supportTicketBroadcaster.register(any()))
        .thenAnswer(mockInvocation -> {
          ticketChangeListenerReference.set(mockInvocation.getArgument(0));
          return (Registration) () -> ticketChangeListenerReference.set(null);
        });
    Ticket supportTicket = mock(Ticket.class);
    when(supportTicket.unreadMessages()).thenReturn(3L);
    when(mockSupportTicketService.loadTicketActivity(mockHttpSession))
        .thenAnswer(mockInvocation ->
            new SupportTicketService.Activity(5L, supportTicket.unreadMessages() > 0));
    SideNavItem ticketSideNavItem = new SideNavItem("Tickets");
    SupportTicketActivityIndicator ticketActivityIndicator =
        new SupportTicketActivityIndicator(
            messageProperties, mockSupportTicketService, mockHttpRequest, ticketSideNavItem);
    testUi.add(ticketSideNavItem);

    assertTrue(ticketActivityIndicator.isVisible());
    assertNull(ticketActivityIndicator.getText());
    assertEquals(
        messageProperties.getSupportNewActivity(),
        Tooltip.forComponent(ticketSideNavItem).getText());

    ticketChangeListenerReference
        .get()
        .accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 77L));

    verify(mockSupportTicketService, times(1)).loadTicketActivity(mockHttpSession);

    when(supportTicket.unreadMessages()).thenReturn(0L);
    ticketChangeListenerReference
        .get()
        .accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 5L));

    assertFalse(ticketActivityIndicator.isVisible());
    assertEquals("", Tooltip.forComponent(ticketSideNavItem).getText());

    when(supportTicket.unreadMessages()).thenReturn(1L);
    ticketChangeListenerReference
        .get()
        .accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(8L, null));

    assertTrue(ticketActivityIndicator.isVisible());

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    ticketActivityIndicator.refreshTexts();

    assertEquals(
        messageProperties.getSupportNewActivity(),
        Tooltip.forComponent(ticketSideNavItem).getText());

    when(mockSupportTicketService.loadTicketActivity(mockHttpSession))
        .thenThrow(new AccessDeniedException("Access revoked"));
    ticketChangeListenerReference
        .get()
        .accept(new SupportTicketBroadcaster.SupportTicketChangeMessage(null, 5L));

    assertFalse(ticketActivityIndicator.isVisible());
    assertEquals("", Tooltip.forComponent(ticketSideNavItem).getText());

    testUi.remove(ticketSideNavItem);

    assertNull(ticketChangeListenerReference.get());
  }

  // Benachrichtigungsseite, gemeinsame Listen und Sprachtexte
  @ParameterizedTest
  @CsvSource({"de,true", "en,true", "de,false", "en,false"})
  void emptyListsUseTheSameIconAndTitleForEveryReadFilter(
      String languageTag, boolean isCompactPreview) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    UserNotificationsPanel notificationsPanel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, isCompactPreview);
    testUi.add(notificationsPanel);

    RadioButtonGroup<ReadFilter> readFilterRadioButtonGroup =
        readComponentField(notificationsPanel, "readFilter");
    Div notificationRowsDiv = readComponentField(notificationsPanel, "notificationRows");

    for (ReadFilter notificationReadFilter : ReadFilter.values()) {
      readFilterRadioButtonGroup.setValue(notificationReadFilter);
      notificationsPanel.refreshInbox();

      assertEquals(1, notificationRowsDiv.getComponentCount());
      Div emptyNotificationDiv = (Div) notificationRowsDiv.getComponentAt(0);

      assertTrue(emptyNotificationDiv.hasClassName("cosmos-notification-empty"));
      assertEquals(2, emptyNotificationDiv.getComponentCount());
      assertEquals(
          "icons/lucide/bell.svg", ((SvgIcon) emptyNotificationDiv.getComponentAt(0)).getSrc());
      assertEquals(
          messageProperties.getNotificationsEmpty(),
          ((Span) emptyNotificationDiv.getComponentAt(1)).getText());
    }

    if (!isCompactPreview) {
      when(userNotificationService.searchInbox(
              eq(mockHttpSession),
              any(ReadFilter.class),
              eq("no-hit"),
              anySet(),
              anyInt(),
              anyInt()))
          .thenReturn(Page.empty());
      ((TextField) readComponentField(notificationsPanel, "notificationSearchField"))
          .setValue("no-hit");

      assertTrue(
          notificationRowsDiv
              .getElement()
              .getTextRecursively()
              .contains(messageProperties.getNotificationsSearchEmpty()));
    }

    notificationsPanel.unavailable();

    assertFalse(
        streamComponentTree(notificationRowsDiv)
            .anyMatch(
                targetComponent -> targetComponent.hasClassName("cosmos-notification-empty")));
    assertTrue(
        notificationRowsDiv
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getNotificationsUnavailable()));
    verify(userNotificationService, never()).dismiss(any(), any());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @CsvSource({"de,true", "en,true", "de,false", "en,false"})
  void onlyManualRefreshReportsTheActualOutcome(String languageTag, boolean isCompactPreview) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    UserNotificationsPanel notificationsPanel =
        createAndRefreshNotificationsPanel(isCompactPreview);
    NotificationHelper notificationHelper = NotificationHelper.getInstance();

    assertNull(readComponentField(notificationHelper, "messageText"));
    Button refreshInboxButton = readComponentField(notificationsPanel, "refreshInboxButton");

    refreshInboxButton.click();
    Span successTitleSpan = readComponentField(notificationHelper, "titleText");

    assertEquals(messageProperties.getNotificationsRefreshed(), successTitleSpan.getText());
    assertNull(readComponentField(notificationHelper, "messageText"));

    notificationHelper.closeAndClearAllNotifications();
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenThrow(new IllegalStateException("Unavailable test fixture"));
    refreshInboxButton.click();
    Html failureMessageHtml = readComponentField(notificationHelper, "messageText");

    assertTrue(
        failureMessageHtml
            .getInnerHtml()
            .contains(messageProperties.getNotificationsUnavailable()));
    assertFalse(
        failureMessageHtml.getInnerHtml().contains(messageProperties.getNotificationsRefreshed()));
    assertEquals("–", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void personalNotificationsPageUsesSharedAccountSectionHeaderAndUpdatesLanguage(
      String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    SecurityService mockSecurityService = mock(SecurityService.class);
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(mockInvocation ->
            ((ConfigEntry) mockInvocation.getArgument(0)).getDefaultValueString());
    UserEntity testUserEntity =
        UserEntity.builder().id(5L).firstName("Own").lastName("User").enabled(true).build();
    when(mockSecurityService.getCurrentUser(mockHttpRequest))
        .thenReturn(Optional.of(testUserEntity));
    UserService mockUserService = mock(UserService.class);
    UserAvatarService mockUserAvatarService = mock(UserAvatarService.class);
    class LanguageRefreshNotificationsView extends UserNotificationsView {
      LanguageRefreshNotificationsView() {
        super(
            UserNotificationsUiTest.this.messageProperties,
            mockSecurityService,
            mockUserService,
            new UserStatusBroadcaster(),
            UserNotificationsUiTest.this.mockHttpRequest,
            mockUserAvatarService,
            UserNotificationsUiTest.this.userNotificationService,
            configService);
      }

      void refreshTrackedUserLanguage() {
        onTrackedUserLanguageChanged();
      }
    }
    LanguageRefreshNotificationsView notificationsView = new LanguageRefreshNotificationsView();
    Component notificationHeaderComponent =
        streamComponentTree(notificationsView.getContent())
            .filter(CosmosSectionHeaderComponent.class::isInstance)
            .findFirst()
            .orElseThrow();

    assertTrue(notificationHeaderComponent.hasClassName("cosmos-dashboard-introduction"));
    assertTrue(
        streamComponentTree(notificationHeaderComponent)
            .filter(H1.class::isInstance)
            .anyMatch(headingComponent -> headingComponent.hasClassName("cosmos-section-title")));
    Span portalBreadcrumbSpan = readComponentField(notificationsView, "portalBreadcrumbSpan");
    Span notificationsTitleSpan = readComponentField(notificationsView, "notificationsTitleSpan");
    Span notificationsDescriptionSpan =
        readComponentField(notificationsView, "notificationsDescriptionSpan");

    assertTrue(portalBreadcrumbSpan.hasClassName("cosmos-dashboard-kicker"));
    assertTrue(notificationsDescriptionSpan.hasClassName("cosmos-dashboard-description"));
    assertEquals(messageProperties.getAccountNavigationPortal(), portalBreadcrumbSpan.getText());
    assertEquals(messageProperties.getNotificationsTitle(), notificationsTitleSpan.getText());
    assertEquals(
        messageProperties.getNotificationsPageDescription(),
        notificationsDescriptionSpan.getText());

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsView.refreshTrackedUserLanguage();

    assertEquals(messageProperties.getAccountNavigationPortal(), portalBreadcrumbSpan.getText());
    assertEquals(messageProperties.getNotificationsTitle(), notificationsTitleSpan.getText());
    assertEquals(
        messageProperties.getNotificationsPageDescription(),
        notificationsDescriptionSpan.getText());
    assertEquals(
        1,
        streamComponentTree(notificationsView.getContent())
            .filter(candidateComponent -> candidateComponent == notificationsDescriptionSpan)
            .count());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @CsvSource({"de,true", "en,true", "de,false", "en,false"})
  void refreshingInboxAndTextsDoesNotMarkNotificationsRead(
      String languageTag, boolean isCompactPreview) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    UserNotificationsPanel notificationsPanel =
        createAndRefreshNotificationsPanel(isCompactPreview);

    assertEquals(
        messageProperties.getNotificationsPasswordChangedTitle(),
        findNotificationTitleButton(notificationsPanel).getText());
    assertTrue(
        streamComponentTree(notificationsPanel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan.getText().equals(messageProperties.getNotificationsRetention(180))));

    notificationsPanel.refreshTexts();

    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void unlimitedRetentionUsesLocalizedFooterWithoutDayCount(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.retentionDays()).thenReturn(OptionalInt.empty());
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);

    assertEquals(
        messageProperties.getNotificationsRetentionUnlimited(),
        ((Span) readComponentField(notificationsPanel, "notificationRetentionSpan")).getText());

    when(userNotificationService.retentionDays()).thenReturn(OptionalInt.of(14));
    notificationsPanel.refreshInbox();

    assertEquals(
        messageProperties.getNotificationsRetention(14),
        ((Span) readComponentField(notificationsPanel, "notificationRetentionSpan")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void notificationTextCatalogLocalizesSystemMessagesAndPreservesManualBody(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    for (UserNotificationType notificationType : UserNotificationType.values()) {
      Item notificationItem =
          new Item(1L, notificationType, "Original title", "Original body", Instant.now(), false);

      assertFalse(
          UserNotificationTexts.getNotificationTitleText(messageProperties, notificationItem)
              .isBlank());
      assertFalse(
          UserNotificationTexts.getNotificationBodyText(messageProperties, notificationItem)
              .isBlank());
      if (notificationType == UserNotificationType.ADMIN_ANNOUNCEMENT) {
        assertEquals(
            "Original body",
            UserNotificationTexts.getNotificationBodyText(messageProperties, notificationItem));
      }
    }

    assertEquals(
        languageTag.equals("de") ? "Passwort geändert" : "Password changed",
        UserNotificationTexts.getNotificationTitleText(messageProperties, inboxNotificationItem));
  }

  // Kompakte Vorschau und ihre Einzelaktionen
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void bellPreviewLabelsFiltersAndClosesWithoutMarkingRead(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");

    assertTrue(notificationPopover.getThemeNames().contains("cosmos-notifications"));
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    assertMarkAllReadButtonIcon(
        (Button) readComponentField(notificationsPanel, "markAllReadButton"));

    assertEquals(
        messageProperties.getNotificationsLatest(5),
        ((Span) readComponentField(notificationsPanel, "inboxDescription")).getText());
    RadioButtonGroup<ReadFilter> readFilterRadioButtonGroup =
        readComponentField(notificationsPanel, "readFilter");

    assertEquals(ReadFilter.ALL, readFilterRadioButtonGroup.getValue());
    assertEquals(
        messageProperties.getNotificationsAll(),
        readFilterRadioButtonGroup.getItemLabelGenerator().apply(ReadFilter.ALL));
    assertEquals(
        messageProperties.getNotificationsRead(),
        readFilterRadioButtonGroup.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        messageProperties.getNotificationsUnread(),
        readFilterRadioButtonGroup.getItemLabelGenerator().apply(ReadFilter.UNREAD));
    assertEquals(
        messageProperties.getNotificationsFilter(),
        readFilterRadioButtonGroup.getAriaLabel().orElseThrow());
    assertTrue(streamComponentTree(notificationsPanel).noneMatch(Checkbox.class::isInstance));
    Span previewHintSpan = readComponentField(notificationsPanel, "previewHint");

    assertEquals(messageProperties.getNotificationsLatest(5), previewHintSpan.getText());

    readFilterRadioButtonGroup.setValue(ReadFilter.READ);

    assertEquals(messageProperties.getNotificationsLatestRead(5), previewHintSpan.getText());

    readFilterRadioButtonGroup.setValue(ReadFilter.UNREAD);

    assertEquals(messageProperties.getNotificationsLatestUnread(5), previewHintSpan.getText());

    readFilterRadioButtonGroup.setValue(ReadFilter.ALL);

    assertEquals(messageProperties.getNotificationsLatest(5), previewHintSpan.getText());
    Button closePreviewButton = readComponentField(notificationsPanel, "closeInboxButton");

    assertEquals(
        messageProperties.getNotificationsClose(), closePreviewButton.getAriaLabel().orElseThrow());
    assertTrue(closePreviewButton.hasClassName("cosmos-overlay-close"));

    closePreviewButton.click();

    assertFalse(notificationPopover.isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void segmentedReadFilterQueriesEachStateResetsPageAndSurvivesLanguageChange(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    Item readNotificationItem =
        new Item(
            8L,
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            true);
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(mockInvocation -> {
          ReadFilter notificationReadFilter = mockInvocation.getArgument(1);
          List<Item> notificationItems =
              switch (notificationReadFilter) {
                case ALL -> List.of(readNotificationItem, inboxNotificationItem);
                case READ -> List.of(readNotificationItem);
                case UNREAD -> List.of(inboxNotificationItem);
              };

          return new PageImpl<>(
              notificationItems,
              PageRequest.of(mockInvocation.getArgument(2), mockInvocation.getArgument(3)),
              notificationItems.size());
        });
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);
    RadioButtonGroup<ReadFilter> readFilterRadioButtonGroup =
        readComponentField(notificationsPanel, "readFilter");

    assertEquals(
        2,
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row"))
            .count());

    ReflectionTestUtils.setField(notificationsPanel, "page", 2);
    readFilterRadioButtonGroup.setValue(ReadFilter.READ);

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.READ, 0, 5);
    assertEquals(0, (int) readComponentField(notificationsPanel, "page"));
    List<Component> notificationRowComponents =
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row"))
            .toList();

    assertEquals(1, notificationRowComponents.size());
    assertEquals(
        "8",
        notificationRowComponents.getFirst().getElement().getAttribute("data-notification-id"));

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsPanel.refreshTexts();

    assertEquals(ReadFilter.READ, readFilterRadioButtonGroup.getValue());
    assertEquals(
        messageProperties.getNotificationsRead(),
        readFilterRadioButtonGroup.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        messageProperties.getNotificationsLatestRead(5),
        ((Span) readComponentField(notificationsPanel, "previewHint")).getText());

    readFilterRadioButtonGroup.setValue(ReadFilter.UNREAD);

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 5);
    notificationRowComponents =
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row"))
            .toList();

    assertEquals(1, notificationRowComponents.size());
    assertEquals(
        "7",
        notificationRowComponents.getFirst().getElement().getAttribute("data-notification-id"));
    assertTrue(
        notificationRowComponents.getFirst().hasClassName("cosmos-notification-row--unread"));

    readFilterRadioButtonGroup.setValue(ReadFilter.ALL);

    assertEquals(
        2,
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row"))
            .count());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void compactPreviewSupportsSafeTextAndIndividualReadWithoutOpening(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "<b>Title</b>",
            "<img src=x onerror=alert(1)>\nSecond line",
            Instant.now(),
            false);
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);

    assertEquals(
        inboxNotificationItem.title(), findNotificationTitleButton(notificationsPanel).getText());
    assertTrue(
        streamComponentTree(notificationsPanel)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv ->
                candidateDiv.hasClassName("cosmos-notification-preview-message")
                    && candidateDiv.getText().equals(inboxNotificationItem.body())));
    assertTrue(streamComponentTree(notificationsPanel).noneMatch(Html.class::isInstance));
    assertTrue(
        streamComponentTree(notificationsPanel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan
                    .getText()
                    .equals(messageProperties.getNotificationsSourceAdministration())));
    Button markPreviewReadButton =
        streamComponentTree(notificationsPanel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-preview-mark-read"))
            .findFirst()
            .orElseThrow();

    assertTrue(markPreviewReadButton.isVisible());

    markPreviewReadButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
        .setValue(ReadFilter.UNREAD);

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 5);
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            true);

    notificationsPanel.refreshInbox();

    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(targetComponent ->
                targetComponent.hasClassName("cosmos-notification-row--unread")));
    assertTrue(
        streamComponentTree(notificationsPanel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-preview-mark-read"))
            .noneMatch(Button::isVisible));
  }

  @Test
  void openingCompactItemKeepsPopoverWithoutMarkingRead() {
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    findNotificationTitleButton(notificationsPanel).click();

    assertTrue(notificationPopover.isOpened());
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");

    assertTrue(notificationDetailDialog.isOpened());
    assertFalse(notificationPopover.isCloseOnOutsideClick());
    assertFalse(notificationPopover.isCloseOnEsc());

    notificationDetailDialog.close();

    assertTrue(notificationPopover.isOpened());
    assertTrue(notificationPopover.isCloseOnOutsideClick());
    assertTrue(notificationPopover.isCloseOnEsc());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void singleClickOnPreviewCardOpensOnlyItsMessageAndIgnoresButtonEvents(
      String languageTag, boolean isNotificationRead) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            isNotificationRead);
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    Component notificationRowComponent =
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row"))
            .findFirst()
            .orElseThrow();
    Element notificationRowElement = notificationRowComponent.getElement();
    ElementListenerMap elementListenerMap =
        notificationRowElement.getNode().getFeature(ElementListenerMap.class);
    ObjectNode clickEventData = JsonNodeFactory.instance.objectNode();

    assertTrue(elementListenerMap.getExpressions("dblclick").isEmpty());
    Set<String> clickFilterExpressions = elementListenerMap.getExpressions("click");

    assertFalse(clickFilterExpressions.isEmpty());
    clickFilterExpressions.forEach(
        clickFilterExpression -> clickEventData.put(clickFilterExpression, false));
    elementListenerMap.fireEvent(new DomEvent(notificationRowElement, "click", clickEventData));

    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    verify(userNotificationService, never()).markRead(any(), any());
    clickFilterExpressions.forEach(
        clickFilterExpression -> clickEventData.put(clickFilterExpression, true));
    elementListenerMap.fireEvent(new DomEvent(notificationRowElement, "click", clickEventData));

    assertTrue(notificationPopover.isOpened());
    assertTrue(
        ((Dialog) readComponentField(notificationsPanel, "notificationDetailDialog")).isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @CsvSource({
    "de,false,title", "en,false,title", "de,true,title", "en,true,title",
    "de,false,open", "en,false,open", "de,true,open", "en,true,open"
  })
  void titleAndOpenButtonUseSingleClickForBothReadStates(
      String languageTag, boolean isNotificationRead, String actionTargetName) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            isNotificationRead);
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    Button openNotificationButton =
        actionTargetName.equals("title")
            ? findNotificationTitleButton(notificationsPanel)
            : streamComponentTree(notificationsPanel)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(
                    candidateButton -> candidateButton.hasClassName("cosmos-notification-row-open"))
                .findFirst()
                .orElseThrow();
    if (actionTargetName.equals("open")) {
      assertEquals("", openNotificationButton.getText());
      assertEquals(
          messageProperties.getNotificationsOpen(), openNotificationButton.getTooltip().getText());
      assertEquals(
          messageProperties.getNotificationsOpen()
              + ": "
              + UserNotificationTexts.getNotificationTitleText(
                  messageProperties, inboxNotificationItem),
          openNotificationButton.getAriaLabel().orElseThrow());
      assertFalse(openNotificationButton.getThemeNames().contains("tertiary-inline"));
    }

    openNotificationButton.click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");

    assertTrue(notificationDetailDialog.isOpened());
    assertTrue(notificationPopover.isOpened());
    assertEquals(
        isNotificationRead
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        findDetailReadStateSpan(notificationDetailDialog).getText());
    verify(userNotificationService, never()).markRead(any(), any());

    notificationDetailDialog.close();

    assertTrue(notificationPopover.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void dismissPreviewItemUsesItsOwnRecipientIdWithoutReadingOrClosingPopup(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    Button openHistoryButton = readComponentField(notificationsPanel, "openHistoryButton");

    assertEquals("icons/lucide/eye.svg", ((SvgIcon) openHistoryButton.getIcon()).getSrc());
    Button dismissNotificationButton =
        streamComponentTree(notificationsPanel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-preview-dismiss"))
            .findFirst()
            .orElseThrow();

    assertTrue(
        dismissNotificationButton
            .getAriaLabel()
            .orElseThrow()
            .startsWith(messageProperties.getNotificationsDismiss()));
    assertTrue(dismissNotificationButton.hasClassName("cosmos-delete-button"));

    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    dismissNotificationButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertTrue(notificationPopover.isOpened());
    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-preview-row")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void bulkDismissReadIsAbsentFromOverlay(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.readCount(mockHttpSession)).thenReturn(3L);
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);
    Button dismissReadNotificationsButton =
        readComponentField(notificationsPanel, "dismissReadButton");

    assertTrue(dismissReadNotificationsButton.getParent().isEmpty());
    assertFalse(dismissReadNotificationsButton.isEnabled());
    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(targetComponent -> targetComponent == dismissReadNotificationsButton));
    verify(userNotificationService, never()).readCount(any());
    verify(userNotificationService, never()).dismissRead(any());
  }

  // Nachrichtendetails, Bestätigungen und Zugriffsprüfung
  @Test
  void openingOneItemKeepsReadStatusAndUsesExistingDialogTheme() {
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    findNotificationTitleButton(notificationsPanel).click();

    verify(userNotificationService, never()).markRead(any(), any());
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");

    assertTrue(notificationDetailDialog.isOpened());
    assertTrue(notificationDetailDialog.getThemeNames().contains("cosmos"));
    assertTrue(notificationDetailDialog.isCloseOnOutsideClick());
    assertTrue(notificationDetailDialog.isCloseOnEsc());
    assertTrue(streamComponentTree(notificationDetailDialog).noneMatch(Html.class::isInstance));
  }

  @ParameterizedTest
  @CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailShowsLocalizedHeaderMetadataAndReadState(
      String languageTag, boolean isCompactPreview, boolean isNotificationRead) {
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail(languageTag, isCompactPreview, isNotificationRead);
    Dialog notificationDetailDialog = notificationDetailFixture.notificationDetailDialog();

    assertTrue(notificationDetailDialog.isOpened());
    verify(userNotificationService, never()).markRead(any(), any());
    Span readStateSpan = findDetailReadStateSpan(notificationDetailDialog);

    assertEquals(
        isNotificationRead
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        readStateSpan.getText());
    assertEquals("badge", readStateSpan.getElement().getAttribute("theme"));
    assertEquals(
        Boolean.toString(isNotificationRead), readStateSpan.getElement().getAttribute("data-read"));
    assertEquals(
        !isNotificationRead,
        streamComponentTree(notificationDetailDialog)
            .anyMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-detail-message--unread")));
    Component notificationHeaderComponent =
        streamComponentTree(notificationDetailDialog)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-detail-header"))
            .findFirst()
            .orElseThrow();

    assertTrue(
        streamComponentTree(notificationHeaderComponent)
            .anyMatch(candidateComponent ->
                candidateComponent instanceof SvgIcon notificationSvgIcon
                    && "icons/lucide/bell.svg".equals(notificationSvgIcon.getSrc())));
    List<H2> headerTitleH2Components =
        streamComponentTree(notificationHeaderComponent)
            .filter(H2.class::isInstance)
            .map(H2.class::cast)
            .toList();

    assertEquals(1, headerTitleH2Components.size());
    assertEquals(
        messageProperties.getNotificationsDetailTitle(),
        headerTitleH2Components.getFirst().getText());
    assertTrue(
        streamComponentTree(notificationHeaderComponent)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan
                        .getText()
                        .equals(messageProperties.getNotificationsDetailDescription())
                    && candidateSpan.hasClassName("cosmos-panel-description")));
    assertTrue(
        streamComponentTree(notificationHeaderComponent)
            .noneMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-detail-state")));
    assertEquals(
        1,
        streamComponentTree(notificationDetailDialog)
            .filter(H3.class::isInstance)
            .map(H3.class::cast)
            .filter(headingComponent ->
                headingComponent
                    .getText()
                    .equals(
                        UserNotificationTexts.getNotificationTitleText(
                            messageProperties, inboxNotificationItem)))
            .count());
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan.getText().equals(messageProperties.getNotificationsSourceSystem())
                    && candidateSpan.hasClassName("portal-notification-source-system")));
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan
                    .getText()
                    .equals(
                        UserNotificationTexts.formatNotificationTimestamp(
                            inboxNotificationItem.createdAt(), testUi.getLocale()))));
  }

  @ParameterizedTest
  @CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailShowsExpectedActionsAndRetainsReadStateAfterReopen(
      String languageTag, boolean isCompactPreview, boolean isNotificationRead) {
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail(languageTag, isCompactPreview, isNotificationRead);
    UserNotificationsPanel notificationsPanel = notificationDetailFixture.notificationsPanel();
    Dialog notificationDetailDialog = notificationDetailFixture.notificationDetailDialog();
    Button markNotificationReadButton =
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read");

    assertEquals(!isNotificationRead, markNotificationReadButton.isVisible());
    assertEquals(
        "icons/lucide/check.svg", ((SvgIcon) markNotificationReadButton.getIcon()).getSrc());
    assertDetailNavigationAndReadActionPresentation(
        notificationDetailDialog, markNotificationReadButton, isCompactPreview);

    if (!isNotificationRead) {
      doAnswer(
              mockInvocation -> {
                inboxNotificationItem =
                    new Item(
                        inboxNotificationItem.id(),
                        inboxNotificationItem.type(),
                        inboxNotificationItem.title(),
                        inboxNotificationItem.body(),
                        inboxNotificationItem.createdAt(),
                        true);
                when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
                return null;
              })
          .when(userNotificationService)
          .markRead(mockHttpSession, List.of(7L));
      markNotificationReadButton.click();
      confirmPendingNotificationAction(notificationsPanel);

      verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
      assertTrue(notificationDetailDialog.isOpened());
      assertEquals(
          messageProperties.getNotificationsRead(),
          findDetailReadStateSpan(notificationDetailDialog).getText());
      assertTrue(
          streamComponentTree(notificationDetailDialog)
              .noneMatch(candidateComponent ->
                  candidateComponent.hasClassName("cosmos-notification-detail-message--unread")));
      assertEquals(
          "true",
          findDetailReadStateSpan(notificationDetailDialog).getElement().getAttribute("data-read"));
      assertFalse(
          findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read")
              .isVisible());
      assertTrue(
          streamComponentTree(notificationsPanel)
              .noneMatch(candidateComponent ->
                  candidateComponent.hasClassName("cosmos-notification-row--unread")));
    }

    notificationDetailDialog.close();
    findNotificationTitleButton(notificationsPanel).click();

    assertEquals(
        messageProperties.getNotificationsRead(),
        findDetailReadStateSpan(readComponentField(notificationsPanel, "notificationDetailDialog"))
            .getText());
  }

  @ParameterizedTest
  @CsvSource({
    "de,true,false", "en,true,false", "de,false,false", "en,false,false",
    "de,true,true", "en,true,true", "de,false,true", "en,false,true"
  })
  void detailDeleteConfirmsOnlyTheOpenedNotificationAndCancelKeepsIt(
      String languageTag, boolean isCompactPreview, boolean isNotificationRead) {
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail(languageTag, isCompactPreview, isNotificationRead);
    UserNotificationsPanel notificationsPanel = notificationDetailFixture.notificationsPanel();
    Dialog notificationDetailDialog = notificationDetailFixture.notificationDetailDialog();
    Button deleteNotificationButton =
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-delete");

    assertEquals(messageProperties.getNotificationsDismiss(), deleteNotificationButton.getText());
    assertEquals(
        messageProperties.getNotificationsDismiss(),
        deleteNotificationButton.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getNotificationsDismiss(),
        deleteNotificationButton.getTooltip().getText());
    assertEquals(
        "icons/lucide/trash-2.svg", ((SvgIcon) deleteNotificationButton.getIcon()).getSrc());
    assertTrue(deleteNotificationButton.hasClassName("cosmos-delete-button"));

    deleteNotificationButton.click();
    PortalConfirmationDialog notificationActionConfirmationDialog =
        readComponentField(notificationsPanel, "notificationActionConfirmation");

    assertTrue(notificationActionConfirmationDialog.isOpened());
    assertTrue(notificationDetailDialog.isOpened());
    verify(userNotificationService, never()).dismiss(any(), any());
    Button cancelConfirmationButton =
        readComponentField(notificationActionConfirmationDialog, "cancelButton");

    cancelConfirmationButton.click();

    assertFalse(notificationActionConfirmationDialog.isOpened());
    assertTrue(notificationDetailDialog.isOpened());
    verify(userNotificationService, never()).dismiss(any(), any());

    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    deleteNotificationButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertFalse(notificationDetailDialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void retiredTicketStatusUpdatesOpenDetailWithoutReadingOrLosingItsHistory(String languageTag) {
    inboxNotificationItem =
        new Item(
            7L,
            UserNotificationType.RECOVERY_REQUEST,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            UserNotificationRelevance.ACTIVE);
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail(languageTag, true, false);
    inboxNotificationItem =
        new Item(
            7L,
            inboxNotificationItem.type(),
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            UserNotificationRelevance.TICKET_ASSIGNED);
    notificationDetailFixture.notificationsPanel().refreshInbox();

    assertTrue(notificationDetailFixture.notificationDetailDialog().isOpened());
    assertEquals(
        1,
        streamComponentTree(notificationDetailFixture.notificationDetailDialog())
            .filter(Badge.class::isInstance)
            .filter(badgeComponent ->
                badgeComponent
                    .getElement()
                    .getTextRecursively()
                    .equals(messageProperties.getNotificationsTicketAssigned()))
            .count());
    Item openedNotificationItem =
        readComponentField(notificationDetailFixture.notificationsPanel(), "openedNotification");

    assertFalse(openedNotificationItem.read());
    assertFalse(openedNotificationItem.unread());
    assertFalse(
        findDetailActionButton(
                notificationDetailFixture.notificationDetailDialog(),
                "cosmos-notification-detail-mark-read")
            .isVisible());
    verify(userNotificationService, never()).markRead(any(), any());

    inboxNotificationItem =
        new Item(
            7L,
            inboxNotificationItem.type(),
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            UserNotificationRelevance.TICKET_CLOSED);

    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.inboxItem(mockHttpSession, 7L))
        .thenReturn(Optional.of(inboxNotificationItem));
    notificationDetailFixture.notificationsPanel().refreshInbox();

    assertTrue(notificationDetailFixture.notificationDetailDialog().isOpened());
    assertTrue(
        streamComponentTree(notificationDetailFixture.notificationDetailDialog())
            .filter(Badge.class::isInstance)
            .anyMatch(badgeComponent ->
                badgeComponent
                    .getElement()
                    .getTextRecursively()
                    .equals(messageProperties.getSupportClosed())));
  }

  @ParameterizedTest
  @EnumSource(
      value = UserNotificationType.class,
      names = {"RECOVERY_REQUEST", "SUPPORT_REPLY", "SUPPORT_REQUESTER_CLOSED"})
  void ticketNotificationsDoNotOfferUnrelatedAccountSecurityNavigation(
      UserNotificationType notificationType) {
    inboxNotificationItem = new Item(7L, notificationType, null, null, Instant.EPOCH, false);
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail("de", true, false);

    assertFalse(
        streamComponentTree(notificationDetailFixture.notificationDetailDialog())
            .filter(Button.class::isInstance)
            .anyMatch(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-detail-account")));
    assertTrue(
        findDetailActionButton(
                notificationDetailFixture.notificationDetailDialog(),
                "cosmos-notification-detail-mark-read")
            .isVisible());
    assertTrue(
        findDetailActionButton(
                notificationDetailFixture.notificationDetailDialog(),
                "cosmos-notification-detail-delete")
            .isVisible());
    assertTrue(
        findDetailActionButton(
                notificationDetailFixture.notificationDetailDialog(),
                "cosmos-notification-detail-history")
            .isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ticketDetailsOfferADestinationOnlyForAnAuthorizedAdministrator(String languageTag) {
    inboxNotificationItem =
        new Item(
            7L,
            UserNotificationType.SUPPORT_REPLY,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10001",
            UserNotificationRelevance.ACTIVE);
    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(true);
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail(languageTag, true, false);
    Button openTicketButton =
        findDetailActionButton(
            notificationDetailFixture.notificationDetailDialog(),
            "cosmos-notification-detail-ticket");

    assertEquals(messageProperties.getNotificationsOpenTicket(), openTicketButton.getText());
    assertTrue(openTicketButton.getThemeNames().contains("primary"));
    Button moreActionsButton =
        findDetailActionButton(
            notificationDetailFixture.notificationDetailDialog(),
            "cosmos-notification-detail-more");
    Popover detailActionsPopover =
        streamComponentTree(notificationDetailFixture.notificationDetailDialog())
            .filter(Popover.class::isInstance)
            .map(Popover.class::cast)
            .findFirst()
            .orElseThrow();

    assertEquals(moreActionsButton, detailActionsPopover.getTarget());
    detailActionsPopover.open();
    findDetailActionButton(
            notificationDetailFixture.notificationDetailDialog(),
            "cosmos-notification-detail-delete")
        .click();

    assertFalse(detailActionsPopover.isOpened());
    assertTrue(
        ((PortalConfirmationDialog)
                readComponentField(
                    notificationDetailFixture.notificationsPanel(),
                    "notificationActionConfirmation"))
            .isOpened());

    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(false);
    notificationDetailFixture.notificationsPanel().refreshTexts();

    assertTrue(
        streamComponentTree(notificationDetailFixture.notificationDetailDialog())
            .noneMatch(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-detail-ticket")));
    assertFalse(
        findDetailActionButton(
                notificationDetailFixture.notificationDetailDialog(),
                "cosmos-notification-detail-close")
            .getThemeNames()
            .contains("primary"));
  }

  @ParameterizedTest
  @EnumSource(
      value = UserNotificationType.class,
      names = {"RECOVERY_REQUEST", "SUPPORT_REPLY", "SUPPORT_REQUESTER_CLOSED"})
  void ticketDestinationUsesThePublicNumberWithoutHash(UserNotificationType notificationType) {
    inboxNotificationItem =
        new Item(
            7L,
            notificationType,
            null,
            null,
            Instant.EPOCH,
            false,
            "#10003",
            UserNotificationRelevance.ACTIVE);
    when(userNotificationService.canOpenTickets(mockHttpSession)).thenReturn(true);
    NotificationDetailFixture notificationDetailFixture =
        createPanelAndOpenNotificationDetail("de", true, false);
    UI navigationUi = mock(UI.class);
    UI.setCurrent(navigationUi);
    try {
      findDetailActionButton(
              notificationDetailFixture.notificationDetailDialog(),
              "cosmos-notification-detail-ticket")
          .click();
    } finally {
      UI.setCurrent(testUi);
    }

    ArgumentCaptor<QueryParameters> queryParametersCaptor =
        ArgumentCaptor.forClass(QueryParameters.class);

    verify(navigationUi).navigate(eq(AdminView.class), queryParametersCaptor.capture());
    assertEquals(
        List.of("support"), queryParametersCaptor.getValue().getParameters().get("section"));
    assertEquals(List.of("10003"), queryParametersCaptor.getValue().getParameters().get("ticket"));
    assertFalse(notificationDetailFixture.notificationDetailDialog().isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void closingAndReopeningDetailDoesNotReadAndLanguageRefreshPreservesManualContent(
      String languageTag) {
    inboxNotificationItem =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "Original title",
            "Original body\nSecond line",
            Instant.now(),
            false);
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");
    notificationDetailDialog.close();
    findNotificationTitleButton(notificationsPanel).click();
    notificationDetailDialog = readComponentField(notificationsPanel, "notificationDetailDialog");
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    notificationsPanel.refreshTexts();

    assertEquals(
        messageProperties.getNotificationsUnread(),
        findDetailReadStateSpan(notificationDetailDialog).getText());
    assertEquals(
        messageProperties.getNotificationsMarkRead(),
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read")
            .getText());
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv -> candidateDiv.getText().equals(inboxNotificationItem.body())));
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan
                    .getText()
                    .contains(messageProperties.getNotificationsSourceAdministration())));
    assertFalse(
        streamComponentTree(notificationDetailDialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(notificationButton ->
                notificationButton.hasClassName("cosmos-notification-detail-account")));
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void failedReadKeepsDetailOpenAndUnread() {
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");
    doThrow(new IllegalStateException("isolated failure"))
        .when(userNotificationService)
        .markRead(mockHttpSession, List.of(7L));
    findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read")
        .click();
    confirmPendingNotificationAction(notificationsPanel);

    assertTrue(notificationDetailDialog.isOpened());
    assertEquals(
        messageProperties.getNotificationsUnread(),
        findDetailReadStateSpan(notificationDetailDialog).getText());
    assertTrue(
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read")
            .isVisible());
  }

  @Test
  void pushReadRefreshUpdatesOpenDetailAndHeaderCount() {
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");
    notificationPopover.open();
    UserNotificationsPanel notificationsPanel =
        readComponentField(notificationBell, "notificationPreviewPanel");
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            true);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    when(userNotificationService.unreadCount(mockHttpSession)).thenReturn(0L);
    userNotificationBroadcaster.publish(Set.of(5L));

    assertTrue(notificationDetailDialog.isOpened());
    assertTrue(notificationPopover.isOpened());
    assertEquals(
        messageProperties.getNotificationsRead(),
        findDetailReadStateSpan(notificationDetailDialog).getText());
    assertFalse(
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-mark-read")
            .isVisible());
    Component unreadCountComponent = readComponentField(notificationBell, "unreadCountBadge");

    assertFalse(unreadCountComponent.isVisible());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void openingDetailRechecksAccessWithoutMutatingReadStatus() {
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(true);
    when(userNotificationService.ownerId(mockHttpSession))
        .thenThrow(new AccessDeniedException("expired"));
    findNotificationTitleButton(notificationsPanel).click();

    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void manualTextIsNotTranslatedOrParsedAsHtml() {
    inboxNotificationItem =
        new Item(
            7L,
            UserNotificationType.ADMIN_ANNOUNCEMENT,
            "<b>Titel</b>",
            "<img src=x onerror=alert(1)>\nText",
            Instant.now(),
            false);
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);

    assertEquals(
        inboxNotificationItem.title(), findNotificationTitleButton(notificationsPanel).getText());
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");

    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv -> candidateDiv.getText().equals(inboxNotificationItem.body())));
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .noneMatch(candidateComponent ->
                !candidateComponent.getElement().isTextNode()
                    && candidateComponent.getElement().getTag().equals("img")));
  }

  private void confirmPendingNotificationAction(UserNotificationsPanel notificationsPanel) {
    PortalConfirmationDialog notificationActionConfirmationDialog =
        readComponentField(notificationsPanel, "notificationActionConfirmation");

    assertTrue(notificationActionConfirmationDialog.isOpened());
    ((Button) readComponentField(notificationActionConfirmationDialog, "confirmButton")).click();

    assertFalse(notificationActionConfirmationDialog.isOpened());
  }

  private Button findDetailActionButton(Dialog notificationDetailDialog, String actionCssClass) {
    return streamComponentTree(notificationDetailDialog)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(notificationButton -> notificationButton.hasClassName(actionCssClass))
        .findFirst()
        .orElseThrow();
  }

  private Span findDetailReadStateSpan(Dialog notificationDetailDialog) {
    return streamComponentTree(notificationDetailDialog)
        .filter(Span.class::isInstance)
        .map(Span.class::cast)
        .filter(candidateSpan -> candidateSpan.hasClassName("cosmos-notification-detail-state"))
        .findFirst()
        .orElseThrow();
  }

  private NotificationDetailFixture createPanelAndOpenNotificationDetail(
      String languageTag, boolean isCompactPreview, boolean isNotificationRead) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            isNotificationRead,
            inboxNotificationItem.supportTicketNumber(),
            inboxNotificationItem.relevance());
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(isNotificationRead ? List.of() : List.of(7L));
    UserNotificationsPanel notificationsPanel =
        createAndRefreshNotificationsPanel(isCompactPreview);
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");
    return new NotificationDetailFixture(notificationsPanel, notificationDetailDialog);
  }

  private void assertDetailNavigationAndReadActionPresentation(
      Dialog notificationDetailDialog,
      Button markNotificationReadButton,
      boolean isCompactPreview) {
    Button openHistoryButton =
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-history");

    assertEquals(isCompactPreview, openHistoryButton.isVisible());
    assertEquals(messageProperties.getNotificationsHistory(), openHistoryButton.getText());
    assertEquals(
        messageProperties.getNotificationsHistory(),
        openHistoryButton.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getNotificationsHistory(), openHistoryButton.getTooltip().getText());
    assertEquals("icons/lucide/bell.svg", ((SvgIcon) openHistoryButton.getIcon()).getSrc());
    Button openAccountButton =
        findDetailActionButton(notificationDetailDialog, "cosmos-notification-detail-account");

    assertEquals(messageProperties.getNotificationsOpenAccount(), openAccountButton.getText());
    assertEquals(
        messageProperties.getNotificationsOpenAccount(),
        openAccountButton.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getNotificationsOpenAccount(), openAccountButton.getTooltip().getText());
    assertEquals("icons/lucide/lock-keyhole.svg", ((SvgIcon) openAccountButton.getIcon()).getSrc());
    assertTrue(
        openAccountButton
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-detail-primary"));
    assertTrue(
        markNotificationReadButton
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-detail-read-actions"));
    assertTrue(
        streamComponentTree(notificationDetailDialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .anyMatch(notificationButton ->
                notificationButton.getText().equals(messageProperties.getNotificationsClose())
                    && !notificationButton.getThemeNames().contains("primary")
                    && notificationButton.getIcon() instanceof SvgIcon notificationSvgIcon
                    && "icons/lucide/x.svg".equals(notificationSvgIcon.getSrc())));
  }

  // Posteingang, Suche und Seitennavigation
  @ParameterizedTest
  @CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void dismissInboxItemRefreshesOwnListAndCountersWithoutOpeningOrReading(
      String languageTag, boolean isNotificationRead) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            isNotificationRead);
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(isNotificationRead ? List.of() : List.of(7L));
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    Button dismissNotificationButton =
        streamComponentTree(notificationsPanel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(candidateButton ->
                candidateButton.hasClassName("cosmos-notification-preview-dismiss"))
            .findFirst()
            .orElseThrow();

    assertTrue(
        dismissNotificationButton
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-inbox-actions"));
    assertEquals(
        "icons/lucide/trash-2.svg", ((SvgIcon) dismissNotificationButton.getIcon()).getSrc());
    assertEquals(
        messageProperties.getNotificationsDismiss(),
        dismissNotificationButton.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsDismiss()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(
                messageProperties, inboxNotificationItem),
        dismissNotificationButton.getAriaLabel().orElseThrow());

    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(0L);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
    dismissNotificationButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).dismiss(mockHttpSession, 7L);
    verify(userNotificationService, never()).markRead(any(), any());
    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-inbox-row")));
    assertEquals("0", ((Span) readComponentField(notificationsPanel, "totalCount")).getText());
    assertEquals("0", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
    assertFalse(((Button) readComponentField(notificationsPanel, "markAllReadButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void bulkDismissReadIsAvailableAcrossFiltersAndSearchAndKeepsUnread(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    Button dismissReadNotificationsButton =
        readComponentField(notificationsPanel, "dismissReadButton");
    Button markAllReadButton = readComponentField(notificationsPanel, "markAllReadButton");

    assertFalse(dismissReadNotificationsButton.isEnabled());
    assertEquals("", dismissReadNotificationsButton.getText());
    assertTrue(dismissReadNotificationsButton.hasClassName("cosmos-delete-button"));
    assertEquals(
        "icons/lucide/trash-2.svg", ((SvgIcon) dismissReadNotificationsButton.getIcon()).getSrc());
    assertEquals(
        messageProperties.getNotificationsDismissRead(),
        dismissReadNotificationsButton.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsDismissRead(),
        dismissReadNotificationsButton.getAriaLabel().orElseThrow());
    List<Component> toolbarComponents =
        dismissReadNotificationsButton.getParent().orElseThrow().getChildren().toList();

    assertEquals(
        toolbarComponents.indexOf(markAllReadButton) + 1,
        toolbarComponents.indexOf(dismissReadNotificationsButton));

    when(userNotificationService.readCount(mockHttpSession)).thenReturn(3L);
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(4L);
    notificationsPanel.refreshInbox();
    RadioButtonGroup<ReadFilter> readFilterRadioButtonGroup =
        readComponentField(notificationsPanel, "readFilter");
    readFilterRadioButtonGroup.setValue(ReadFilter.UNREAD);
    TextField notificationSearchTextField =
        readComponentField(notificationsPanel, "notificationSearchField");
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    notificationSearchTextField.setValue("no visible match");

    assertTrue(dismissReadNotificationsButton.isEnabled());

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsPanel.refreshTexts();

    assertEquals(
        messageProperties.getNotificationsDismissRead(),
        dismissReadNotificationsButton.getTooltip().getText());

    doAnswer(
            mockInvocation -> {
              when(userNotificationService.readCount(mockHttpSession)).thenReturn(0L);
              when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
              return null;
            })
        .when(userNotificationService)
        .dismissRead(mockHttpSession);
    dismissReadNotificationsButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).dismissRead(mockHttpSession);
    verify(userNotificationService, never()).dismiss(any(), any());
    verify(userNotificationService, never()).markRead(any(), any());
    assertEquals(List.of(7L), readComponentField(notificationsPanel, "unreadIds"));
    assertFalse(dismissReadNotificationsButton.isEnabled());
    assertTrue(markAllReadButton.isEnabled());
    assertEquals(ReadFilter.UNREAD, readFilterRadioButtonGroup.getValue());
    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    assertEquals("no visible match", notificationSearchTextField.getValue());
    assertEquals("1", ((Span) readComponentField(notificationsPanel, "totalCount")).getText());
    assertEquals("1", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
    notificationsPanel.unavailable();

    assertFalse(dismissReadNotificationsButton.isEnabled());
  }

  @Test
  void markAllSubmitsDisplayedSnapshotAndUnreadFilterResetsPage() {
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    ((Button) readComponentField(notificationsPanel, "markAllReadButton")).click();

    verify(userNotificationService, never()).markRead(any(), any());

    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of(7L, 9L));
    notificationsPanel.refreshInbox();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
    verify(userNotificationService, never()).markRead(mockHttpSession, List.of(7L, 9L));
    ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
        .setValue(ReadFilter.UNREAD);

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 10);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, Integer.MAX_VALUE})
  void numberedInboxNavigationClampsExtremeIndicesWithoutIntegerOverflow(int pageIndex) {
    UserNotificationsPanel notificationsPanel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, false);
    ReflectionTestUtils.setField(notificationsPanel, "page", pageIndex);
    ReflectionTestUtils.setField(notificationsPanel, "totalPages", 20);
    ReflectionTestUtils.invokeMethod(notificationsPanel, "renderPageNumbers");
    Div pageNumbersDiv = readComponentField(notificationsPanel, "pageNumbers");
    List<String> pageNumberLabels =
        streamComponentTree(pageNumbersDiv)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .map(Button::getText)
            .toList();

    assertEquals(
        pageIndex < 0 ? List.of("1", "2", "3", "4", "20") : List.of("1", "17", "18", "19", "20"),
        pageNumberLabels);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxArchiveHeaderCountsSearchPagingAndLanguageWorkTogether(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(6L);
    when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of(7L, 9L));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(mockInvocation ->
            new PageImpl<>(
                List.of(inboxNotificationItem),
                PageRequest.of(mockInvocation.getArgument(2), mockInvocation.getArgument(3)),
                45));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenAnswer(mockInvocation ->
            new PageImpl<>(
                List.of(inboxNotificationItem),
                PageRequest.of(mockInvocation.getArgument(4), mockInvocation.getArgument(5)),
                1));
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);

    assertInboxHeaderAndCounters(notificationsPanel);

    ((Button) readComponentField(notificationsPanel, "lastPageButton")).click();

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 4, 10);
    assertFalse(((Button) readComponentField(notificationsPanel, "nextPageButton")).isEnabled());
    ((Button) readComponentField(notificationsPanel, "firstPageButton")).click();

    assertEquals(0, (int) readComponentField(notificationsPanel, "page"));
    Div pageNumbersDiv = (Div) readComponentField(notificationsPanel, "pageNumbers");
    Button thirdPageButton =
        streamComponentTree(pageNumbersDiv)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(candidateButton -> candidateButton.getText().equals("3"))
            .findFirst()
            .orElseThrow();

    thirdPageButton.click();
    TextField notificationSearchTextField =
        readComponentField(notificationsPanel, "notificationSearchField");
    notificationSearchTextField.setValue("alpha");

    assertEquals(0, (int) readComponentField(notificationsPanel, "page"));
    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.ALL, "alpha", Set.of(), 0, 10);
    assertEquals("6", ((Span) readComponentField(notificationsPanel, "totalCount")).getText());
    assertEquals("2", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
    ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
        .setValue(ReadFilter.UNREAD);

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 10);

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsPanel.refreshTexts();

    assertEquals("alpha", notificationSearchTextField.getValue());
    assertEquals(
        messageProperties.getNotificationsSearch(), notificationSearchTextField.getPlaceholder());
    assertEquals(
        ReadFilter.UNREAD,
        ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
            .getValue());

    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), eq("no-hit"), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    notificationSearchTextField.setValue("no-hit");

    assertTrue(
        streamComponentTree(notificationsPanel)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan.getText().equals(messageProperties.getNotificationsSearchEmpty())));
    assertFalse(((Button) readComponentField(notificationsPanel, "firstPageButton")).isEnabled());
    assertFalse(((Button) readComponentField(notificationsPanel, "lastPageButton")).isEnabled());

    notificationSearchTextField.clear();

    verify(userNotificationService, atLeastOnce()).inbox(mockHttpSession, ReadFilter.UNREAD, 0, 10);
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxPageSizeControlsAndChangesResetThePage(String languageTag) {
    UserNotificationsPanel notificationsPanel =
        createAndRefreshInboxWithFortyFiveItems(languageTag);
    ComboBox<Integer> pageSizeComboBox = readComponentField(notificationsPanel, "pageSize");
    Span pageTotalSpan = readComponentField(notificationsPanel, "pageTotal");

    assertEquals(List.of(10, 20, 50), pageSizeComboBox.getListDataView().getItems().toList());
    assertEquals(10, pageSizeComboBox.getValue());
    assertFalse(pageSizeComboBox.isAllowCustomValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(),
        pageSizeComboBox.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(),
        ((Span) readComponentField(notificationsPanel, "pageLabel")).getText());
    assertEquals(messageProperties.getPortalPaginationRange(1, 10, 45), pageTotalSpan.getText());
    ((Button) readComponentField(notificationsPanel, "lastPageButton")).click();

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 4, 10);

    pageSizeComboBox.setValue(20);

    assertEquals(0, (int) readComponentField(notificationsPanel, "page"));
    verify(userNotificationService, atLeastOnce()).inbox(mockHttpSession, ReadFilter.ALL, 0, 20);
    ((Button) readComponentField(notificationsPanel, "nextPageButton")).click();

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 1, 20);

    pageSizeComboBox.setValue(50);

    assertEquals(0, (int) readComponentField(notificationsPanel, "page"));
    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 0, 50);
    assertFalse(((Button) readComponentField(notificationsPanel, "nextPageButton")).isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxPagingKeepsSearchUnreadLanguageAndEmptyFailureStates(String languageTag) {
    UserNotificationsPanel notificationsPanel =
        createAndRefreshInboxWithFortyFiveItems(languageTag);
    ComboBox<Integer> pageSizeComboBox = readComponentField(notificationsPanel, "pageSize");
    Span pageTotalSpan = readComponentField(notificationsPanel, "pageTotal");
    pageSizeComboBox.setValue(50);
    ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
        .setValue(ReadFilter.UNREAD);
    TextField notificationSearchTextField =
        readComponentField(notificationsPanel, "notificationSearchField");
    notificationSearchTextField.setValue("alpha");

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 50);

    pageSizeComboBox.setValue(20);

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 20);
    ((Button) readComponentField(notificationsPanel, "lastPageButton")).click();

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 2, 20);

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsPanel.refreshTexts();

    assertEquals(20, pageSizeComboBox.getValue());
    assertEquals("alpha", notificationSearchTextField.getValue());
    assertEquals(
        ReadFilter.UNREAD,
        ((RadioButtonGroup<ReadFilter>) readComponentField(notificationsPanel, "readFilter"))
            .getValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(),
        pageSizeComboBox.getAriaLabel().orElseThrow());
    assertEquals(messageProperties.getPortalPaginationRange(41, 45, 45), pageTotalSpan.getText());

    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), eq("no-hit"), anySet(), anyInt(), anyInt()))
        .thenReturn(Page.empty());
    notificationSearchTextField.setValue("no-hit");

    assertEquals(messageProperties.getPortalPaginationRange(0, 0, 0), pageTotalSpan.getText());
    assertFalse(((Button) readComponentField(notificationsPanel, "firstPageButton")).isEnabled());
    assertFalse(((Button) readComponentField(notificationsPanel, "lastPageButton")).isEnabled());
    notificationsPanel.unavailable();

    assertEquals("–", pageTotalSpan.getText());
    verify(userNotificationService, never()).markRead(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void inboxToolbarUsesSharedFilterAndIconToolsAndSearchesReadItems(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenReturn(new PageImpl<>(List.of(inboxNotificationItem)));
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    RadioButtonGroup<ReadFilter> readFilterRadioButtonGroup =
        readComponentField(notificationsPanel, "readFilter");

    assertEquals("notifications-inbox-filter", readFilterRadioButtonGroup.getId().orElseThrow());
    assertEquals(ReadFilter.ALL, readFilterRadioButtonGroup.getValue());
    assertEquals(
        List.of(ReadFilter.ALL, ReadFilter.UNREAD, ReadFilter.READ),
        readFilterRadioButtonGroup.getListDataView().getItems().toList());
    assertTrue(streamComponentTree(notificationsPanel).noneMatch(Checkbox.class::isInstance));
    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-inbox-hint")));
    Button refreshInboxButton = readComponentField(notificationsPanel, "refreshInboxButton");
    Button markAllReadButton = readComponentField(notificationsPanel, "markAllReadButton");

    assertEquals("", refreshInboxButton.getText());
    assertEquals("", markAllReadButton.getText());
    assertEquals(
        messageProperties.getNotificationsRefresh(),
        refreshInboxButton.getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getNotificationsRefresh(), refreshInboxButton.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsMarkAll(), markAllReadButton.getTooltip().getText());

    readFilterRadioButtonGroup.setValue(ReadFilter.READ);

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.READ, 0, 10);
    TextField notificationSearchTextField =
        readComponentField(notificationsPanel, "notificationSearchField");

    notificationSearchTextField.setValue("alpha");

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.READ, "alpha", Set.of(), 0, 10);

    ReflectionTestUtils.setField(notificationsPanel, "page", 4);
    readFilterRadioButtonGroup.setValue(ReadFilter.UNREAD);

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.UNREAD, "alpha", Set.of(), 0, 10);

    readFilterRadioButtonGroup.setValue(ReadFilter.ALL);

    verify(userNotificationService)
        .searchInbox(mockHttpSession, ReadFilter.ALL, "alpha", Set.of(), 0, 10);

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    notificationsPanel.refreshTexts();

    assertEquals(ReadFilter.ALL, readFilterRadioButtonGroup.getValue());
    assertEquals(
        messageProperties.getNotificationsRead(),
        readFilterRadioButtonGroup.getItemLabelGenerator().apply(ReadFilter.READ));
    assertEquals(
        messageProperties.getNotificationsRefresh(), refreshInboxButton.getTooltip().getText());
    assertEquals(
        messageProperties.getNotificationsMarkAll(), markAllReadButton.getTooltip().getText());
    assertEquals("alpha", notificationSearchTextField.getValue());

    refreshInboxButton.click();
    markAllReadButton.click();
    confirmPendingNotificationAction(notificationsPanel);

    verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
  }

  @ParameterizedTest
  @CsvSource({"de,false", "en,false", "de,true", "en,true"})
  void inboxCardSingleClickAndExplicitReadRemainSeparate(
      String languageTag, boolean isNotificationRead) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    inboxNotificationItem =
        new Item(
            inboxNotificationItem.id(),
            inboxNotificationItem.type(),
            inboxNotificationItem.title(),
            inboxNotificationItem.body(),
            inboxNotificationItem.createdAt(),
            isNotificationRead);
    when(userNotificationService.unreadSnapshot(mockHttpSession))
        .thenReturn(isNotificationRead ? List.of() : List.of(7L));
    when(userNotificationService.inboxCount(mockHttpSession)).thenReturn(1L);
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    Component notificationRowComponent =
        streamComponentTree(notificationsPanel)
            .filter(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-inbox-row"))
            .findFirst()
            .orElseThrow();

    assertEquals(
        !isNotificationRead,
        notificationRowComponent.hasClassName("cosmos-notification-row--unread"));
    assertTrue(
        streamComponentTree(notificationRowComponent)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv ->
                candidateDiv
                    .getText()
                    .equals(
                        UserNotificationTexts.getNotificationBodyText(
                            messageProperties, inboxNotificationItem))));
    Element notificationRowElement = notificationRowComponent.getElement();
    ElementListenerMap elementListenerMap =
        notificationRowElement.getNode().getFeature(ElementListenerMap.class);
    ObjectNode clickEventData = JsonNodeFactory.instance.objectNode();
    Set<String> clickFilterExpressions = elementListenerMap.getExpressions("click");

    assertFalse(clickFilterExpressions.isEmpty());
    clickFilterExpressions.forEach(
        clickFilterExpression -> clickEventData.put(clickFilterExpression, false));
    elementListenerMap.fireEvent(new DomEvent(notificationRowElement, "click", clickEventData));

    assertNull(readComponentField(notificationsPanel, "notificationDetailDialog"));
    clickFilterExpressions.forEach(
        clickFilterExpression -> clickEventData.put(clickFilterExpression, true));
    elementListenerMap.fireEvent(new DomEvent(notificationRowElement, "click", clickEventData));
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");

    assertTrue(notificationDetailDialog.isOpened());
    assertEquals(
        isNotificationRead
            ? messageProperties.getNotificationsRead()
            : messageProperties.getNotificationsUnread(),
        findDetailReadStateSpan(notificationDetailDialog).getText());
    verify(userNotificationService, never()).markRead(any(), any());

    notificationDetailDialog.close();
    Button markNotificationReadButton =
        streamComponentTree(notificationsPanel)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(candidateButton ->
                candidateButton.hasClassName("cosmos-notification-inbox-mark-read"))
            .findFirst()
            .orElseThrow();

    assertEquals(!isNotificationRead, markNotificationReadButton.isVisible());
    assertEquals("", markNotificationReadButton.getText());
    assertEquals(
        messageProperties.getNotificationsMarkRead(),
        markNotificationReadButton.getTooltip().getText());
    if (!isNotificationRead) {
      doAnswer(
              mockInvocation -> {
                inboxNotificationItem =
                    new Item(
                        inboxNotificationItem.id(),
                        inboxNotificationItem.type(),
                        inboxNotificationItem.title(),
                        inboxNotificationItem.body(),
                        inboxNotificationItem.createdAt(),
                        true);
                when(userNotificationService.unreadSnapshot(mockHttpSession)).thenReturn(List.of());
                return null;
              })
          .when(userNotificationService)
          .markRead(mockHttpSession, List.of(7L));
      markNotificationReadButton.click();
      confirmPendingNotificationAction(notificationsPanel);

      verify(userNotificationService).markRead(mockHttpSession, List.of(7L));
      assertEquals("0", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
      assertEquals("1", ((Span) readComponentField(notificationsPanel, "totalCount")).getText());
      assertTrue(
          streamComponentTree(notificationsPanel)
              .noneMatch(candidateComponent ->
                  candidateComponent.hasClassName("cosmos-notification-row--unread")));
    }
  }

  @Test
  void paginatedInboxRequestsNextPageAndBack() {
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(mockInvocation ->
            new PageImpl<>(
                List.of(inboxNotificationItem),
                PageRequest.of(mockInvocation.getArgument(2), mockInvocation.getArgument(3)),
                30));
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    ((Button) readComponentField(notificationsPanel, "nextPageButton")).click();

    verify(userNotificationService).inbox(mockHttpSession, ReadFilter.ALL, 1, 10);
    ((Button) readComponentField(notificationsPanel, "previousPageButton")).click();

    verify(userNotificationService, times(2)).inbox(mockHttpSession, ReadFilter.ALL, 0, 10);
  }

  @Test
  void revokedSessionClearsContentsAndOpenDetail() {
    UserNotificationsPanel notificationsPanel = createAndRefreshNotificationsPanel(false);
    findNotificationTitleButton(notificationsPanel).click();
    Dialog notificationDetailDialog =
        readComponentField(notificationsPanel, "notificationDetailDialog");
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenThrow(new AccessDeniedException("expired"));
    notificationsPanel.refreshInbox();

    assertFalse(notificationDetailDialog.isOpened());
    assertTrue(
        streamComponentTree(notificationsPanel)
            .noneMatch(candidateComponent ->
                candidateComponent.hasClassName("cosmos-notification-title")));
    assertFalse(((Button) readComponentField(notificationsPanel, "markAllReadButton")).isEnabled());
  }

  private UserNotificationsPanel createAndRefreshInboxWithFortyFiveItems(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(userNotificationService.inbox(
            eq(mockHttpSession), any(ReadFilter.class), anyInt(), anyInt()))
        .thenAnswer(mockInvocation ->
            createInboxPage(mockInvocation.getArgument(2), mockInvocation.getArgument(3)));
    when(userNotificationService.searchInbox(
            eq(mockHttpSession), any(ReadFilter.class), anyString(), anySet(), anyInt(), anyInt()))
        .thenAnswer(mockInvocation ->
            createInboxPage(mockInvocation.getArgument(4), mockInvocation.getArgument(5)));
    return createAndRefreshNotificationsPanel(false);
  }

  private Page<Item> createInboxPage(int pageIndex, int pageSize) {
    int remainingNotificationCount = 45 - pageIndex * pageSize;
    int visibleNotificationCount = Math.min(pageSize, remainingNotificationCount);

    return new PageImpl<>(
        Collections.nCopies(visibleNotificationCount, inboxNotificationItem),
        PageRequest.of(pageIndex, pageSize),
        45);
  }

  private void assertInboxHeaderAndCounters(UserNotificationsPanel notificationsPanel) {
    assertTrue(
        streamComponentTree(notificationsPanel)
            .anyMatch(candidateComponent ->
                candidateComponent instanceof SvgIcon notificationSvgIcon
                    && "icons/lucide/bell.svg".equals(notificationSvgIcon.getSrc())));
    assertEquals(
        messageProperties.getNotificationsInboxDescription(),
        ((Span) readComponentField(notificationsPanel, "inboxDescription")).getText());
    assertEquals("6", ((Span) readComponentField(notificationsPanel, "totalCount")).getText());
    assertEquals("2", ((Span) readComponentField(notificationsPanel, "unreadCount")).getText());
    assertEquals(
        "icons/lucide/refresh-cw.svg",
        ((SvgIcon)
                ((Button) readComponentField(notificationsPanel, "refreshInboxButton")).getIcon())
            .getSrc());
    assertMarkAllReadButtonIcon(
        (Button) readComponentField(notificationsPanel, "markAllReadButton"));
  }

  // Glockenzähler, Live-Ereignisse und Einbindung in den Header
  @Test
  void liveBellCapsBadgeAndRefreshesOnlyItsOwnUserAndDetachesCleanly() {
    when(userNotificationService.unreadCount(mockHttpSession)).thenReturn(125L);
    Button notificationBellButton = new Button();
    UserNotificationBell notificationBell = createAndAttachNotificationBell(notificationBellButton);
    Component unreadCountComponent = readComponentField(notificationBell, "unreadCountBadge");

    assertEquals("99+", assertInstanceOf(HasText.class, unreadCountComponent).getText());
    assertTrue(unreadCountComponent.isVisible());
    assertTrue(notificationBellButton.getAriaLabel().orElseThrow().contains("125"));

    clearInvocations(userNotificationService);

    userNotificationBroadcaster.publish(Set.of(999L));

    verify(userNotificationService, never()).unreadCount(any());

    userNotificationBroadcaster.publish(Set.of(5L));

    verify(userNotificationService).unreadCount(mockHttpSession);

    testUi.remove(notificationBell);
    clearInvocations(userNotificationService);
    userNotificationBroadcaster.publish(Set.of(5L));

    verify(userNotificationService, never()).unreadCount(any());
  }

  @Test
  void bellConfiguresClickOpeningAndEscapeOrOutsideClosingWithoutReading() {
    UserNotificationBell notificationBell = createAndAttachNotificationBell(new Button());
    Popover notificationPopover = readComponentField(notificationBell, "notificationPopover");

    assertTrue(notificationPopover.isOpenOnClick());
    assertFalse(notificationPopover.isOpenOnFocus());
    assertFalse(notificationPopover.isOpenOnHover());
    assertTrue(notificationPopover.isCloseOnEsc());
    assertTrue(notificationPopover.isCloseOnOutsideClick());

    notificationPopover.open();

    verify(userNotificationService, never()).markRead(any(), any());
  }

  @Test
  void revokedSessionDisablesBellAndHidesUnreadBadge() {
    Button notificationBellButton = new Button();
    UserNotificationBell notificationBell = createAndAttachNotificationBell(notificationBellButton);
    when(userNotificationService.unreadCount(mockHttpSession))
        .thenThrow(new AccessDeniedException("expired"));
    userNotificationBroadcaster.publish(Set.of(5L));

    Component unreadCountComponent = readComponentField(notificationBell, "unreadCountBadge");

    assertFalse(notificationBellButton.isEnabled());
    assertFalse(unreadCountComponent.isVisible());
  }

  @Test
  void springWiresTheSharedBellForAuthenticatedViewsOnly() {
    SecurityService mockSecurityService = mock(SecurityService.class);
    UserService mockUserService = mock(UserService.class);
    UserAvatarService mockUserAvatarService = mock(UserAvatarService.class);
    UserStatusBroadcaster testUserStatusBroadcaster = new UserStatusBroadcaster();
    UserEntity authenticatedUserEntity =
        UserEntity.builder().id(5L).firstName("Own").lastName("User").enabled(true).build();
    when(mockSecurityService.getCurrentUser(mockHttpRequest))
        .thenReturn(Optional.of(authenticatedUserEntity));
    class TestUserHeaderLayout extends TrackedUserAppLayout {
      TestUserHeaderLayout() {
        super(
            UserNotificationsUiTest.this.messageProperties,
            mockSecurityService,
            mockUserService,
            testUserStatusBroadcaster,
            UserNotificationsUiTest.this.mockHttpRequest,
            mockUserAvatarService);
      }
    }
    try (AnnotationConfigApplicationContext applicationContext =
        new AnnotationConfigApplicationContext()) {
      applicationContext.registerBean(UserNotificationService.class, () -> userNotificationService);
      applicationContext.refresh();
      TestUserHeaderLayout authenticatedHeaderLayout = new TestUserHeaderLayout();
      applicationContext.getAutowireCapableBeanFactory().autowireBean(authenticatedHeaderLayout);
      AccountHeaderActions accountHeaderActions =
          readComponentField(authenticatedHeaderLayout, "accountHeaderActions");

      assertTrue(
          streamComponentTree(accountHeaderActions)
              .anyMatch(UserNotificationBell.class::isInstance));
      testUi.add(accountHeaderActions);

      verify(userNotificationService).ownerId(mockHttpSession);
      when(mockSecurityService.getCurrentUser(mockHttpRequest)).thenReturn(Optional.empty());
      TestUserHeaderLayout anonymousHeaderLayout = new TestUserHeaderLayout();
      applicationContext.getAutowireCapableBeanFactory().autowireBean(anonymousHeaderLayout);
      AccountHeaderActions anonymousHeaderActions =
          readComponentField(anonymousHeaderLayout, "accountHeaderActions");

      assertFalse(anonymousHeaderActions.isVisible());
      assertTrue(
          streamComponentTree(anonymousHeaderActions)
              .noneMatch(UserNotificationBell.class::isInstance));
    }
  }

  // Administratives Versandarchiv
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryExpandsMessagesIndependentlyAndPaginates(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    String notificationBodyText = "<b>Plain text</b>\nSecond line";
    when(userNotificationService.sent(mockHttpSession, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new Sent(1L, "First", notificationBodyText, Instant.now(), 2),
                    new Sent(2L, "Second", "Other body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    when(userNotificationService.sent(mockHttpSession, 4, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(3L, "Last", "Last body", Instant.now(), 1)),
                PageRequest.of(4, 5),
                21));
    adminNotificationsSection.activate();

    CosmosPagination historyPagination =
        readComponentField(adminNotificationsSection, "historyPagination");
    Span totalItemsSpan = readComponentField(historyPagination, "totalItemsSpan");
    Button lastPageButton = readComponentField(historyPagination, "lastPageButton");
    Button nextPageButton = readComponentField(historyPagination, "nextPageButton");
    Button previousPageButton = readComponentField(historyPagination, "previousPageButton");
    Button firstPageButton = readComponentField(historyPagination, "firstPageButton");

    assertEquals(
        messageProperties.getNotificationsHistoryCount(21),
        ((Span) readComponentField(adminNotificationsSection, "historyCountSpan")).getText());
    assertEquals(messageProperties.getPortalPaginationRange(1, 5, 21), totalItemsSpan.getText());
    Div sentNotificationHistoryDiv =
        readComponentField(adminNotificationsSection, "sentNotificationHistoryDiv");
    List<Details> sentMessageDetails =
        streamComponentTree(sentNotificationHistoryDiv)
            .filter(Details.class::isInstance)
            .map(Details.class::cast)
            .toList();
    List<Button> historyToggleButtons =
        streamComponentTree(sentNotificationHistoryDiv)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .toList();

    assertEquals(2, sentMessageDetails.size());
    assertTrue(sentMessageDetails.stream().noneMatch(Details::isOpened));
    historyToggleButtons.getFirst().click();

    assertTrue(sentMessageDetails.getFirst().isOpened());
    assertFalse(sentMessageDetails.getLast().isOpened());
    assertEquals(
        "true", historyToggleButtons.getFirst().getElement().getAttribute("aria-expanded"));
    assertEquals(
        messageProperties.getNotificationsHistoryDetails(),
        historyToggleButtons.getFirst().getText());
    assertExpandedSentMessageContent(
        sentMessageDetails.getFirst(), sentNotificationHistoryDiv, notificationBodyText);

    historyToggleButtons.getFirst().click();

    assertFalse(sentMessageDetails.getFirst().isOpened());
    assertEquals(
        "false", historyToggleButtons.getFirst().getElement().getAttribute("aria-expanded"));

    lastPageButton.click();

    assertEquals(messageProperties.getPortalPaginationRange(21, 21, 21), totalItemsSpan.getText());
    assertFalse(nextPageButton.isEnabled());
    assertTrue(previousPageButton.isEnabled());

    firstPageButton.click();

    assertEquals(
        0,
        ((Integer) readComponentField(adminNotificationsSection, "historyPageIndex")).intValue());

    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    adminNotificationsSection.activate();

    assertEquals(messageProperties.getPortalPaginationRange(0, 0, 0), totalItemsSpan.getText());
    assertFalse(nextPageButton.isEnabled());
    assertFalse(previousPageButton.isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sentHistoryPageSizeSelectionResetsThePageAndLoadsTheSelectedSize(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    CosmosPagination historyPagination =
        readComponentField(adminNotificationsSection, "historyPagination");
    ComboBox<Integer> pageSizeComboBox = readComponentField(historyPagination, "pageSizeSelector");

    assertEquals(List.of(5, 10, 15), pageSizeComboBox.getListDataView().getItems().toList());
    assertEquals(5, pageSizeComboBox.getValue());
    assertEquals(
        messageProperties.getAdminUsersPaginationPerPage(),
        pageSizeComboBox.getAriaLabel().orElseThrow());
    for (int selectedPageSize : List.of(10, 15, 5)) {
      when(userNotificationService.sent(mockHttpSession, 0, selectedPageSize))
          .thenReturn(Page.empty());
      ReflectionTestUtils.setField(adminNotificationsSection, "historyPageIndex", 3);
      pageSizeComboBox.setValue(selectedPageSize);

      assertEquals(
          0,
          ((Integer) readComponentField(adminNotificationsSection, "historyPageIndex")).intValue());
      verify(userNotificationService, atLeastOnce()).sent(mockHttpSession, 0, selectedPageSize);
      assertEquals(
          selectedPageSize,
          ((Integer) readComponentField(adminNotificationsSection, "historyPageSize")).intValue());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceKeepsSentHistoryPaginationAvailable(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    when(userNotificationService.sent(mockHttpSession, 0, 5))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(1L, "Previous", "Body", Instant.now(), 1)),
                PageRequest.of(0, 5),
                21));
    adminNotificationsSection.activate();
    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    adminNotificationsSection.refreshDeliveryState();

    CosmosPagination historyPagination =
        readComponentField(adminNotificationsSection, "historyPagination");
    Button nextPageButton = readComponentField(historyPagination, "nextPageButton");

    assertTrue(nextPageButton.isEnabled());
  }

  private void assertExpandedSentMessageContent(
      Details expandedMessageDetails, Div sentNotificationHistoryDiv, String notificationBodyText) {
    assertFalse(
        streamComponentTree(expandedMessageDetails)
            .filter(H4.class::isInstance)
            .map(H4.class::cast)
            .anyMatch(messageTitleH4 -> messageTitleH4.getText().equals("First")));
    assertTrue(
        streamComponentTree(expandedMessageDetails)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(metadataSpan ->
                metadataSpan.getText().equals(messageProperties.getNotificationsMessageContent())));
    assertFalse(
        streamComponentTree(expandedMessageDetails)
            .anyMatch(deliveryStatusDiv ->
                deliveryStatusDiv.hasClassName("cosmos-notification-sent-detail-meta")));
    assertTrue(
        streamComponentTree(expandedMessageDetails)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv -> candidateDiv.getText().equals(notificationBodyText)));
    assertTrue(streamComponentTree(sentNotificationHistoryDiv).noneMatch(Html.class::isInstance));
    assertTrue(
        streamComponentTree(expandedMessageDetails)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(metadataSpan ->
                metadataSpan
                    .getText()
                    .equals(messageProperties.getNotificationsHistoryDelivery())));
    assertTrue(
        streamComponentTree(expandedMessageDetails)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(metadataSpan ->
                metadataSpan
                    .getText()
                    .equals(messageProperties.getNotificationsHistoryImmutableLog())));
  }

  // Wartungszustand und Polling des Nachrichtenversands
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceDisablesComposeAndRequiresFreshReviewWhenReopened(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();

    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Subject");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Message");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    Preview reviewedRecipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, reviewedRecipientPreview.selection()))
        .thenReturn(reviewedRecipientPreview);
    Button reviewDraftButton = readComponentField(adminNotificationsSection, "previewButton");
    Button sendNotificationButton =
        readComponentField(adminNotificationsSection, "sendNotificationButton");
    reviewDraftButton.click();

    assertTrue(sendNotificationButton.isEnabled());
    Div deliveryStatusDiv = readComponentField(adminNotificationsSection, "deliveryStatusDiv");

    assertEquals(
        messageProperties.getNotificationsInternal(),
        ((Span) readComponentField(adminNotificationsSection, "deliveryLabelSpan")).getText());
    assertEquals("open", deliveryStatusDiv.getElement().getAttribute("data-state"));
    Tooltip deliveryTooltip = readComponentField(adminNotificationsSection, "deliveryTooltip");

    assertEquals(
        messageProperties.getNotificationsInternal()
            + ": "
            + messageProperties.getMaintenanceStatusAvailable(),
        deliveryTooltip.getText());

    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    adminNotificationsSection.refreshDeliveryState();

    assertEquals("active", deliveryStatusDiv.getElement().getAttribute("data-state"));
    assertTrue(
        deliveryStatusDiv
            .getElement()
            .getAttribute("aria-label")
            .contains(messageProperties.getMaintenanceStatusActive()));
    assertEquals(
        messageProperties.getNotificationsInternal()
            + ": "
            + messageProperties.getMaintenanceStatusActive(),
        deliveryTooltip.getText());
    VerticalLayout notificationComposeLayout =
        readComponentField(adminNotificationsSection, "notificationComposeLayout");

    assertFalse(notificationComposeLayout.isEnabled());
    assertEquals("true", notificationComposeLayout.getElement().getAttribute("aria-disabled"));
    assertFalse(
        ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
            .isEnabled());
    assertFalse(
        ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
            .isEnabled());
    assertFalse(
        ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
            .isEnabled());
    assertFalse(reviewDraftButton.isEnabled());
    assertFalse(sendNotificationButton.isEnabled());
    assertNull(readComponentField(adminNotificationsSection, "reviewedRecipientPreview"));

    when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
    adminNotificationsSection.refreshDeliveryState();

    assertTrue(notificationComposeLayout.isEnabled());
    assertEquals("open", deliveryStatusDiv.getElement().getAttribute("data-state"));
    assertEquals(
        "Subject",
        ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
            .getValue());
    assertFalse(sendNotificationButton.isEnabled());

    reviewDraftButton.click();

    assertTrue(sendNotificationButton.isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void deliveryActionRechecksMaintenanceBeforeNextPoll(MaintenanceService.State deliveryState) {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Subject");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Message");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    Preview reviewedRecipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, reviewedRecipientPreview.selection()))
        .thenReturn(reviewedRecipientPreview);
    Button reviewDraftButton = readComponentField(adminNotificationsSection, "previewButton");
    reviewDraftButton.click();
    when(userNotificationService.deliveryState()).thenReturn(deliveryState);
    ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton")).click();

    assertFalse(
        ((VerticalLayout)
                readComponentField(adminNotificationsSection, "notificationComposeLayout"))
            .isEnabled());
    assertNull(readComponentField(adminNotificationsSection, "reviewedRecipientPreview"));

    reviewDraftButton.click();

    verify(userNotificationService, times(1)).preview(any(), any());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1000, 12000})
  void deliveryPollingUpdatesStateAndRestoresPreviousIntervalOnDetach(int previousPollInterval) {
    testUi.setPollInterval(previousPollInterval);
    int[] unrelatedPollCounts = {0};
    Registration unrelatedPollRegistration =
        testUi.addPollListener(pollEvent -> unrelatedPollCounts[0]++);
    try {
      AdminUserNotificationsSection adminNotificationsSection =
          createAndActivateAdminNotificationsSection();
      testUi.add(adminNotificationsSection);

      assertEquals(
          previousPollInterval > 0 ? Math.min(previousPollInterval, 5000) : 5000,
          testUi.getPollInterval());
      when(userNotificationService.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));

      assertFalse(
          ((VerticalLayout)
                  readComponentField(adminNotificationsSection, "notificationComposeLayout"))
              .isEnabled());
      assertEquals(1, unrelatedPollCounts[0]);
      testUi.remove(adminNotificationsSection);

      assertEquals(previousPollInterval, testUi.getPollInterval());
      clearInvocations(userNotificationService);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));

      assertEquals(2, unrelatedPollCounts[0]);
      verify(userNotificationService, never()).deliveryState();
    } finally {
      unrelatedPollRegistration.remove();
    }
  }

  // Entwurfsvalidierung und verlässlicher Versand
  @Test
  void adminRequiresValidFieldsAndFreshPreviewBeforeSend() {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();

    assertTrue(
        ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
            .isInvalid());
    assertTrue(
        ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
            .isInvalid());
    verify(userNotificationService, never()).preview(any(), any());
    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    Preview recipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L));

    when(userNotificationService.preview(mockHttpSession, recipientPreview.selection()))
        .thenReturn(recipientPreview);
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Title");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Body");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();

    assertTrue(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Changed");

    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void draftErrorsOnlyAppearAfterReviewAndEditingClearsThem(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    TextField notificationTitleTextField =
        readComponentField(adminNotificationsSection, "notificationTitleField");
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");
    MultiSelectComboBox<Target> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");
    Button reviewDraftButton = readComponentField(adminNotificationsSection, "previewButton");
    Button sendNotificationButton =
        readComponentField(adminNotificationsSection, "sendNotificationButton");

    assertEquals(messageProperties.getNotificationsSend(), sendNotificationButton.getText());

    notificationTitleTextField.setValue("Draft");
    notificationBodyTextArea.setValue("Draft");
    recipientMultiSelectComboBox.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    notificationTitleTextField.clear();
    notificationBodyTextArea.clear();
    recipientMultiSelectComboBox.clear();
    ReflectionTestUtils.invokeMethod(notificationTitleTextField, "validate");
    ReflectionTestUtils.invokeMethod(notificationBodyTextArea, "validate");
    ReflectionTestUtils.invokeMethod(recipientMultiSelectComboBox, "validate");

    assertFalse(notificationTitleTextField.isInvalid());
    assertFalse(notificationBodyTextArea.isInvalid());
    assertFalse(recipientMultiSelectComboBox.isInvalid());
    assertFalse(sendNotificationButton.isEnabled());

    reviewDraftButton.click();

    assertMissingDraftFieldErrors(
        notificationTitleTextField, notificationBodyTextArea, recipientMultiSelectComboBox);
    assertPreviewValidationHint();

    ReflectionTestUtils.invokeMethod(recipientMultiSelectComboBox, "validate");

    assertTrue(recipientMultiSelectComboBox.isInvalid());

    notificationTitleTextField.setValue("Changed");

    assertFalse(notificationTitleTextField.isInvalid());
    assertFalse(notificationBodyTextArea.isInvalid());
    assertFalse(recipientMultiSelectComboBox.isInvalid());

    notificationTitleTextField.clear();
    ReflectionTestUtils.invokeMethod(notificationTitleTextField, "validate");

    assertFalse(notificationTitleTextField.isInvalid());

    notificationTitleTextField.setValue("Valid subject");
    notificationBodyTextArea.setValue("Valid message");
    reviewDraftButton.click();

    assertFalse(notificationTitleTextField.isInvalid());
    assertFalse(notificationBodyTextArea.isInvalid());
    assertTrue(recipientMultiSelectComboBox.isInvalid());
    assertPreviewValidationHint();

    assertFalse(sendNotificationButton.isEnabled());
    verify(userNotificationService, never()).preview(any(), any());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(
      value = Problem.class,
      names = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED"})
  void rejectedPreviewKeepsRecipientDetailsAtFieldAndUsesGeneralHint(Problem previewProblem) {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Subject");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Message");
    MultiSelectComboBox<Target> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");
    recipientMultiSelectComboBox.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    when(userNotificationService.preview(any(), any())).thenThrow(new Rejected(previewProblem));
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();

    assertTrue(recipientMultiSelectComboBox.isInvalid());
    assertEquals(
        previewProblem == Problem.NO_RECIPIENTS
            ? messageProperties.getNotificationsNoRecipients()
            : messageProperties.getNotificationsRecipientsChanged(),
        recipientMultiSelectComboBox.getErrorMessage());
    assertPreviewValidationHint();

    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body", "recipients", "everyone"})
  void changingAnyDraftInputRequiresAnotherSuccessfulReview(String draftInputName) {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    TextField notificationTitleTextField =
        readComponentField(adminNotificationsSection, "notificationTitleField");
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");
    MultiSelectComboBox<Target> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");
    Checkbox sendToEveryoneCheckbox =
        readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox");
    Button reviewDraftButton = readComponentField(adminNotificationsSection, "previewButton");
    Button sendNotificationButton =
        readComponentField(adminNotificationsSection, "sendNotificationButton");
    notificationTitleTextField.setValue("Subject");
    notificationBodyTextArea.setValue("Message");
    recipientMultiSelectComboBox.setValue(Set.of(new Target(5L, "Own", "own@example.test")));
    Preview reviewedRecipientPreview = new Preview(new Selection(false, Set.of(5L)), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, reviewedRecipientPreview.selection()))
        .thenReturn(reviewedRecipientPreview);

    assertFalse(sendNotificationButton.isEnabled());

    reviewDraftButton.click();

    assertTrue(sendNotificationButton.isEnabled());
    switch (draftInputName) {
      case "title" -> notificationTitleTextField.setValue("Changed subject");
      case "body" -> notificationBodyTextArea.clear();
      case "recipients" -> recipientMultiSelectComboBox.clear();
      case "everyone" -> sendToEveryoneCheckbox.setValue(true);
      default -> fail("Unknown draft input");
    }

    assertFalse(sendNotificationButton.isEnabled());
    assertNull(readComponentField(adminNotificationsSection, "reviewedRecipientPreview"));
    assertTrue(
        ((Div) readComponentField(adminNotificationsSection, "notificationPreviewDiv"))
            .isVisible());
    assertEquals(
        messageProperties.getNotificationsPreviewNeedsReview(),
        ((Span) readComponentField(adminNotificationsSection, "previewStatusSpan")).getText());
    assertEquals(
        "warning",
        ((Span) readComponentField(adminNotificationsSection, "previewStatusSpan"))
            .getElement()
            .getAttribute("data-state"));
    assertFalse(notificationTitleTextField.isInvalid());
    assertFalse(notificationBodyTextArea.isInvalid());
    assertFalse(recipientMultiSelectComboBox.isInvalid());

    sendNotificationButton.click();

    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"NO_RECIPIENTS", "RECIPIENTS_CHANGED", "UNAVAILABLE"})
  void failedRepeatReviewRevokesEarlierApproval(String previewProblem) {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Subject");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Message");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    Preview reviewedRecipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L));
    RuntimeException previewRejection =
        previewProblem.equals("UNAVAILABLE")
            ? new IllegalStateException("Isolated review failure")
            : new Rejected(Problem.valueOf(previewProblem));
    when(userNotificationService.preview(mockHttpSession, reviewedRecipientPreview.selection()))
        .thenReturn(reviewedRecipientPreview)
        .thenThrow(previewRejection);
    Button reviewDraftButton = readComponentField(adminNotificationsSection, "previewButton");
    Button sendNotificationButton =
        readComponentField(adminNotificationsSection, "sendNotificationButton");
    reviewDraftButton.click();

    assertTrue(sendNotificationButton.isEnabled());

    reviewDraftButton.click();

    assertFalse(sendNotificationButton.isEnabled());
    assertNull(readComponentField(adminNotificationsSection, "reviewedRecipientPreview"));
    assertTrue(
        ((Div) readComponentField(adminNotificationsSection, "notificationPreviewDiv"))
            .isVisible());
    assertEquals(
        messageProperties.getNotificationsPreviewNeedsReview(),
        ((Span) readComponentField(adminNotificationsSection, "previewStatusSpan")).getText());
    assertEquals(
        "warning",
        ((Span) readComponentField(adminNotificationsSection, "previewStatusSpan"))
            .getElement()
            .getAttribute("data-state"));
    assertEquals(
        !previewProblem.equals("UNAVAILABLE"),
        ((TextField) readComponentField(adminNotificationsSection, "everyoneRecipientsTextField"))
            .isInvalid());

    sendNotificationButton.click();

    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "body"})
  void overlongDraftIsMarkedOnlyWhenReviewed(String draftInputName) {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    TextField notificationTitleTextField =
        readComponentField(adminNotificationsSection, "notificationTitleField");
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");
    notificationTitleTextField.setValue("Subject");
    notificationBodyTextArea.setValue("Message");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    if (draftInputName.equals("title")) {
      notificationTitleTextField.setValue("x".repeat(UserNotificationService.TITLE_LIMIT + 1));
    } else {
      notificationBodyTextArea.setValue("x".repeat(UserNotificationService.BODY_LIMIT + 1));
    }

    assertFalse(notificationTitleTextField.isInvalid());
    assertFalse(notificationBodyTextArea.isInvalid());
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();

    assertEquals(draftInputName.equals("title"), notificationTitleTextField.isInvalid());
    assertEquals(draftInputName.equals("body"), notificationBodyTextArea.isInvalid());
    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    verify(userNotificationService, never()).preview(any(), any());
  }

  @Test
  void adminSendUsesSameRequestAfterAmbiguousFailureAndClearsOnlyAfterSuccess() {
    AdminUserNotificationsSection adminNotificationsSection =
        createAndActivateAdminNotificationsSection();
    Preview recipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L));
    when(userNotificationService.preview(mockHttpSession, recipientPreview.selection()))
        .thenReturn(recipientPreview);
    ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
        .setValue("Title");
    ((TextArea) readComponentField(adminNotificationsSection, "notificationBodyField"))
        .setValue("Body");
    ((Checkbox) readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox"))
        .setValue(true);
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();
    UUID sendRequestUuid = readComponentField(adminNotificationsSection, "sendRequestUuid");
    when(userNotificationService.send(
            mockHttpSession, sendRequestUuid, "Title", "Body", recipientPreview))
        .thenThrow(new IllegalStateException("response interrupted"))
        .thenReturn(new Sent(1L, "Title", "Body", Instant.now(), 1));
    ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton")).click();

    assertEquals(
        sendRequestUuid, (UUID) readComponentField(adminNotificationsSection, "sendRequestUuid"));
    assertEquals(
        "Title",
        ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
            .getValue());
    ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton")).click();

    verify(userNotificationService, times(2))
        .send(mockHttpSession, sendRequestUuid, "Title", "Body", recipientPreview);
    assertTrue(
        ((TextField) readComponentField(adminNotificationsSection, "notificationTitleField"))
            .isEmpty());
    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    assertFalse(
        ((Div) readComponentField(adminNotificationsSection, "notificationPreviewDiv"))
            .isVisible());
  }

  private void assertMissingDraftFieldErrors(
      TextField notificationTitleTextField,
      TextArea notificationBodyTextArea,
      MultiSelectComboBox<Target> recipientMultiSelectComboBox) {
    assertTrue(notificationTitleTextField.isInvalid());
    assertTrue(notificationBodyTextArea.isInvalid());
    assertTrue(recipientMultiSelectComboBox.isInvalid());
    assertEquals(
        messageProperties.getNotificationsInvalidSubject(),
        notificationTitleTextField.getErrorMessage());
    assertEquals(
        messageProperties.getNotificationsInvalidBody(),
        notificationBodyTextArea.getErrorMessage());
    assertNotEquals(
        notificationTitleTextField.getErrorMessage(), notificationBodyTextArea.getErrorMessage());
    assertEquals(
        messageProperties.getNotificationsNoRecipients(),
        recipientMultiSelectComboBox.getErrorMessage());
  }

  private void assertPreviewValidationHint() {
    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    Span validationHintTitleSpan = readComponentField(notificationHelper, "titleText");
    Html validationHintMessageHtml = readComponentField(notificationHelper, "messageText");

    assertEquals(
        messageProperties.getNotificationsPreviewFailedTitle(), validationHintTitleSpan.getText());
    assertEquals(
        new Html(
                HtmlHelper.ensureParagraphTags(
                    messageProperties.getNotificationsPreviewFailedMessage()))
            .getElement()
            .getProperty("innerHTML"),
        validationHintMessageHtml.getElement().getProperty("innerHTML"));
  }

  // Empfängerauswahl und geprüfte Versandvorschau
  private AdminUserNotificationsSection createAndActivateAdminNotificationsSection() {
    when(userNotificationService.targets(mockHttpSession))
        .thenReturn(List.of(new Target(5L, "Own", "own@example.test")));
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection adminNotificationsSection =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    adminNotificationsSection.activate();

    return adminNotificationsSection;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void recipientCountTracksSelectionRemovalAndLanguage(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    List<Target> recipientTargets =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(recipientTargets);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection adminNotificationsSection =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    adminNotificationsSection.activate();
    MultiSelectComboBox<Target> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");
    Span recipientSummarySpan =
        readComponentField(adminNotificationsSection, "recipientSummarySpan");

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(0), recipientSummarySpan.getText());

    recipientMultiSelectComboBox.setValue(Set.of(recipientTargets.getFirst()));

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(1), recipientSummarySpan.getText());

    recipientMultiSelectComboBox.setValue(Set.copyOf(recipientTargets));

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(2), recipientSummarySpan.getText());

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    adminNotificationsSection.refreshTexts();

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(2), recipientSummarySpan.getText());

    recipientMultiSelectComboBox.setValue(Set.of(recipientTargets.getLast()));

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(1), recipientSummarySpan.getText());

    recipientMultiSelectComboBox.clear();

    assertEquals(
        messageProperties.getNotificationsSelectedUsers(0), recipientSummarySpan.getText());
    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void allRecipientsShowsDisabledSummaryAndRestoresIndividualSelection(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    List<Target> recipientTargets =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(recipientTargets);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection adminNotificationsSection =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    adminNotificationsSection.activate();
    MultiSelectComboBox<Target> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");
    TextField everyoneRecipientsTextField =
        readComponentField(adminNotificationsSection, "everyoneRecipientsTextField");
    Checkbox sendToEveryoneCheckbox =
        readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox");

    assertTrue(recipientMultiSelectComboBox.isVisible());
    assertTrue(recipientMultiSelectComboBox.isEnabled());
    assertFalse(everyoneRecipientsTextField.isVisible());

    recipientMultiSelectComboBox.setValue(Set.of(recipientTargets.getFirst()));
    sendToEveryoneCheckbox.setValue(true);

    assertFalse(recipientMultiSelectComboBox.isVisible());
    assertFalse(recipientMultiSelectComboBox.isEnabled());
    assertTrue(everyoneRecipientsTextField.isVisible());
    assertFalse(everyoneRecipientsTextField.isEnabled());
    assertTrue(everyoneRecipientsTextField.isReadOnly());
    assertEquals(
        messageProperties.getNotificationsEveryonePlaceholder(2),
        everyoneRecipientsTextField.getValue());
    assertEquals(
        messageProperties.getNotificationsRecipients(), everyoneRecipientsTextField.getLabel());

    activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    adminNotificationsSection.refreshTexts();

    assertEquals(
        messageProperties.getNotificationsEveryonePlaceholder(2),
        everyoneRecipientsTextField.getValue());
    assertEquals(
        messageProperties.getNotificationsRecipients(), everyoneRecipientsTextField.getLabel());

    sendToEveryoneCheckbox.setValue(false);

    assertFalse(everyoneRecipientsTextField.isVisible());
    assertTrue(recipientMultiSelectComboBox.isVisible());
    assertTrue(recipientMultiSelectComboBox.isEnabled());
    assertEquals(
        Set.of(recipientTargets.getFirst()), recipientMultiSelectComboBox.getSelectedItems());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reviewedPreviewShowsPlainTextCountersRecipientsAndReviewStatus(String languageTag) {
    AdminDraftFixture adminDraftFixture = createAdminDraftFixture(languageTag);
    AdminUserNotificationsSection adminNotificationsSection =
        adminDraftFixture.adminNotificationsSection();
    TextField notificationTitleTextField =
        readComponentField(adminNotificationsSection, "notificationTitleField");
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");

    assertEquals(
        notificationTitleTextField.getValue().length()
            + " / "
            + UserNotificationService.TITLE_LIMIT,
        ((Span) readComponentField(adminNotificationsSection, "titleCountSpan")).getText());
    assertEquals(
        notificationBodyTextArea.getValue().length() + " / " + UserNotificationService.BODY_LIMIT,
        ((Span) readComponentField(adminNotificationsSection, "bodyCountSpan")).getText());
    assertEquals(UserNotificationService.TITLE_LIMIT, notificationTitleTextField.getMaxLength());
    assertEquals(UserNotificationService.BODY_LIMIT, notificationBodyTextArea.getMaxLength());
    MultiSelectComboBox<?> recipientMultiSelectComboBox =
        readComponentField(adminNotificationsSection, "recipientComboBox");

    assertFalse(recipientMultiSelectComboBox.isEnabled());
    assertEquals(
        messageProperties.getNotificationsEveryonePlaceholder(4),
        recipientMultiSelectComboBox.getPlaceholder());
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();
    Div notificationPreviewDiv =
        readComponentField(adminNotificationsSection, "notificationPreviewDiv");

    assertTrue(notificationPreviewDiv.isVisible());
    Span previewStatusSpan = readComponentField(adminNotificationsSection, "previewStatusSpan");

    assertTrue(previewStatusSpan.isVisible());
    assertEquals(messageProperties.getNotificationsPreviewUpdated(), previewStatusSpan.getText());
    assertEquals("success", previewStatusSpan.getElement().getAttribute("data-state"));
    assertTrue(
        previewStatusSpan
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-compose-preview-header"));
    assertEquals(
        3, streamComponentTree(notificationPreviewDiv).filter(Avatar.class::isInstance).count());
    assertTrue(streamComponentTree(notificationPreviewDiv).noneMatch(Html.class::isInstance));
    assertTrue(
        streamComponentTree(notificationPreviewDiv)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv ->
                candidateDiv.getText().equals(notificationBodyTextArea.getValue())));
    assertTrue(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    verify(userNotificationService, never()).send(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void reviewedPreviewRecipientDetailsKeepLabelsAriaAndAllRecipients(String languageTag) {
    AdminDraftFixture adminDraftFixture = createAdminDraftFixture(languageTag);
    AdminUserNotificationsSection adminNotificationsSection =
        adminDraftFixture.adminNotificationsSection();
    List<Target> recipientTargets = adminDraftFixture.recipientTargets();
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();
    Div notificationPreviewDiv =
        readComponentField(adminNotificationsSection, "notificationPreviewDiv");
    Details recipientDetails =
        streamComponentTree(notificationPreviewDiv)
            .filter(Details.class::isInstance)
            .map(Details.class::cast)
            .findFirst()
            .orElseThrow();

    assertFalse(recipientDetails.isOpened());
    Button recipientDetailsToggleButton =
        streamComponentTree(notificationPreviewDiv)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(notificationButton ->
                notificationButton.getId().orElse("").equals("admin-notifications-preview-details"))
            .findFirst()
            .orElseThrow();

    assertEquals(
        messageProperties.getNotificationsRecipientDetails(),
        recipientDetailsToggleButton.getText());
    assertEquals("false", recipientDetailsToggleButton.getElement().getAttribute("aria-expanded"));
    assertEquals(
        recipientDetails.getId().orElseThrow(),
        recipientDetailsToggleButton.getElement().getAttribute("aria-controls"));
    assertTrue(
        recipientDetailsToggleButton
            .getParent()
            .orElseThrow()
            .hasClassName("cosmos-notification-compose-recipients"));

    recipientDetailsToggleButton.click();

    assertTrue(recipientDetails.isOpened());
    assertEquals(
        messageProperties.getNotificationsRecipientDetails(),
        recipientDetailsToggleButton.getText());
    assertEquals("true", recipientDetailsToggleButton.getElement().getAttribute("aria-expanded"));

    recipientDetailsToggleButton.click();

    assertFalse(recipientDetails.isOpened());
    assertEquals(
        messageProperties.getNotificationsRecipientDetails(),
        recipientDetailsToggleButton.getText());

    recipientDetailsToggleButton.click();
    for (Target recipientTarget : recipientTargets) {
      assertTrue(
          streamComponentTree(recipientDetails)
              .filter(Span.class::isInstance)
              .map(Span.class::cast)
              .anyMatch(candidateSpan -> candidateSpan.getText().equals(recipientTarget.email())));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void editingReviewedPreviewKeepsOldContentAndRequiresAnotherReview(String languageTag) {
    AdminDraftFixture adminDraftFixture = createAdminDraftFixture(languageTag);
    AdminUserNotificationsSection adminNotificationsSection =
        adminDraftFixture.adminNotificationsSection();
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();
    Div notificationPreviewDiv =
        readComponentField(adminNotificationsSection, "notificationPreviewDiv");
    Span previewStatusSpan = readComponentField(adminNotificationsSection, "previewStatusSpan");
    String reviewedBodyText = notificationBodyTextArea.getValue();
    notificationBodyTextArea.setValue("Changed");

    assertTrue(notificationPreviewDiv.isVisible());
    assertNull(readComponentField(adminNotificationsSection, "reviewedRecipientPreview"));
    assertTrue(previewStatusSpan.isVisible());
    assertEquals(
        messageProperties.getNotificationsPreviewNeedsReview(), previewStatusSpan.getText());
    assertEquals("warning", previewStatusSpan.getElement().getAttribute("data-state"));
    assertTrue(
        streamComponentTree(notificationPreviewDiv)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv -> candidateDiv.getText().equals(reviewedBodyText)));
    assertFalse(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    assertEquals(
        "7 / " + UserNotificationService.BODY_LIMIT,
        ((Span) readComponentField(adminNotificationsSection, "bodyCountSpan")).getText());
    ((Button) readComponentField(adminNotificationsSection, "previewButton")).click();

    assertTrue(
        ((Button) readComponentField(adminNotificationsSection, "sendNotificationButton"))
            .isEnabled());
    assertEquals(messageProperties.getNotificationsPreviewUpdated(), previewStatusSpan.getText());
    assertEquals("success", previewStatusSpan.getElement().getAttribute("data-state"));
    assertTrue(
        streamComponentTree(notificationPreviewDiv)
            .filter(Div.class::isInstance)
            .map(Div.class::cast)
            .anyMatch(candidateDiv -> candidateDiv.getText().equals("Changed")));
  }

  private AdminDraftFixture createAdminDraftFixture(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    List<Target> recipientTargets =
        List.of(
            new Target(5L, "Own User", "own@example.test"),
            new Target(6L, "Second User", "second@example.test"),
            new Target(7L, "Third User", "third@example.test"),
            new Target(8L, "Fourth User", "fourth@example.test"));
    when(userNotificationService.targets(mockHttpSession)).thenReturn(recipientTargets);
    when(userNotificationService.sent(mockHttpSession, 0, 5)).thenReturn(Page.empty());
    AdminUserNotificationsSection adminNotificationsSection =
        new AdminUserNotificationsSection(
            messageProperties, userNotificationService, mockHttpRequest);
    adminNotificationsSection.activate();
    TextField notificationTitleTextField =
        readComponentField(adminNotificationsSection, "notificationTitleField");
    TextArea notificationBodyTextArea =
        readComponentField(adminNotificationsSection, "notificationBodyField");
    Checkbox sendToEveryoneCheckbox =
        readComponentField(adminNotificationsSection, "sendToEveryoneCheckbox");
    Preview recipientPreview = new Preview(new Selection(true, Set.of()), List.of(5L, 6L, 7L, 8L));
    when(userNotificationService.preview(mockHttpSession, recipientPreview.selection()))
        .thenReturn(recipientPreview);
    notificationTitleTextField.setValue("<b>Title</b>");
    notificationBodyTextArea.setValue("<img src=x onerror=alert(1)>\nPlain text");
    sendToEveryoneCheckbox.setValue(true);

    return new AdminDraftFixture(adminNotificationsSection, recipientTargets);
  }

  // Aufbau gemeinsamer Testkomponenten
  private UserNotificationsPanel createAndRefreshNotificationsPanel(boolean isCompactPreview) {
    UserNotificationsPanel notificationsPanel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, mockHttpSession, isCompactPreview);
    testUi.add(notificationsPanel);
    notificationsPanel.refreshInbox();

    return notificationsPanel;
  }

  private UserNotificationBell createAndAttachNotificationBell(Button notificationBellButton) {
    UserNotificationBell notificationBell =
        new UserNotificationBell(
            messageProperties, userNotificationService, mockHttpRequest, notificationBellButton);
    testUi.add(notificationBell);

    return notificationBell;
  }

  // Gemeinsame Komponentenabfragen und Assertions
  @SuppressWarnings("unchecked")
  private <T> T readComponentField(Object targetComponent, String fieldName) {
    return (T) ReflectionTestUtils.getField(targetComponent, fieldName);
  }

  private Stream<Component> streamComponentTree(Component targetComponent) {
    Stream<Component> childComponents = targetComponent.getChildren();
    // Vaadin verwaltet Header und Footer als separate Dialogbereiche.
    if (targetComponent instanceof Dialog targetDialog) {
      childComponents =
          Stream.concat(
              childComponents,
              Stream.concat(
                      targetDialog.getHeader().getElement().getChildren(),
                      targetDialog.getFooter().getElement().getChildren())
                  .flatMap(dialogSlotElement -> dialogSlotElement.getComponent().stream()));
    }

    return Stream.concat(
        Stream.of(targetComponent), childComponents.distinct().flatMap(this::streamComponentTree));
  }

  private void assertMarkAllReadButtonIcon(Button markAllReadButton) {
    SvgIcon notificationSvgIcon = assertInstanceOf(SvgIcon.class, markAllReadButton.getIcon());

    assertEquals("icons/lucide/check-check.svg", notificationSvgIcon.getSrc());
    assertEquals("vaadin-icon", notificationSvgIcon.getElement().getTag());
    assertEquals("true", notificationSvgIcon.getElement().getAttribute("aria-hidden"));
    assertNotNull(getClass().getResource("/META-INF/resources/" + notificationSvgIcon.getSrc()));
  }

  private Button findNotificationTitleButton(UserNotificationsPanel notificationsPanel) {
    return streamComponentTree(notificationsPanel)
        .filter(candidateComponent -> candidateComponent.hasClassName("cosmos-notification-title"))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  // Zusammengehörige Komponenten und Testdaten
  private record NotificationDetailFixture(
      UserNotificationsPanel notificationsPanel, Dialog notificationDetailDialog) {}

  private record AdminDraftFixture(
      AdminUserNotificationsSection adminNotificationsSection, List<Target> recipientTargets) {}
}
