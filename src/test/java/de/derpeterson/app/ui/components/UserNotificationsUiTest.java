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
import org.junit.jupiter.api.Tag;
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
    when(service.retentionDays()).thenReturn(180);
    when(service.deliveryState()).thenReturn(MaintenanceService.State.OPEN);
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

  Button rowTitle(UserNotificationsPanel panel) {
    return tree(panel)
        .filter(c -> c.hasClassName("cosmos-notification-title"))
        .map(Button.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @Tag("notification-ui-pending-redesign")
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
  void openingOneItemMarksOnlyThatItemAndUsesExistingDialogTheme() {
    var panel = new UserNotificationsPanel(texts, service, http, false);
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
    assertTrue(tree(detail).noneMatch(c -> c.getElement().getTag().equals("img")));
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
  @Tag("notification-ui-pending-redesign")
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
    when(service.sent(http, 0)).thenReturn(Page.empty());
    var section = new AdminUserNotificationsSection(texts, service, request);
    section.activate();
    return section;
  }

  @Tag("notification-ui-pending-redesign")
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceKeepsSentHistoryPaginationAvailable(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = admin();
    when(service.sent(http, 0))
        .thenReturn(
            new PageImpl<>(
                List.of(new Sent(1L, "Previous", "Body", Instant.now(), 1)),
                PageRequest.of(0, 20),
                21));
    section.activate();
    when(service.deliveryState()).thenReturn(MaintenanceService.State.ACTIVE);
    section.refreshDeliveryState();
    assertTrue(((Button) field(section, "next")).isEnabled());
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
    when(service.sent(http, 0)).thenReturn(Page.empty());
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
    when(service.sent(http, 0)).thenReturn(Page.empty());
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
    when(service.sent(http, 0)).thenReturn(Page.empty());
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
