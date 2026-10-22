package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/** Real translations, notification components and existing language event; no browser. */
class UserStatusNotificationTest {
  private UI ui;
  private VaadinSession session;
  private TrackedUserAppLayout layout;
  private final CustomI18NProvider provider = new CustomI18NProvider();
  private final MessageProperties texts = new MessageProperties(provider);

  @BeforeEach
  void setup() {
    ui = new UI();
    session = mock(VaadinSession.class);
    AtomicReference<Locale> locale = new AtomicReference<>(Locale.ENGLISH);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    when(session.hasLock()).thenReturn(true);
    doAnswer(
            call -> {
              locale.set(call.getArgument(0));
              return null;
            })
        .when(session)
        .setLocale(any());
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    SecurityService security = mock(SecurityService.class);
    when(security.getCurrentUser(any(HttpServletRequest.class))).thenReturn(Optional.empty());
    layout =
        new TrackedUserAppLayout(
            texts,
            security,
            mock(UserService.class),
            new UserStatusBroadcaster(),
            mock(HttpServletRequest.class),
            mock(UserAvatarService.class)) {};
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    layout = null;
    ui = null;
    session = null;
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void visiblePopoverFailureAndNewFailureTranslateImmediatelyIncludingTitle(boolean conflict) {
    UserEntity user = UserEntity.builder().id(7L).status(UserStatus.OFFLINE).build();
    UserService service = mock(UserService.class);
    when(service.updateUserStatus(user, UserStatus.EMPLOYED, true))
        .thenThrow(
            conflict
                ? new OptimisticLockingFailureException("conflict")
                : new IllegalStateException("failed"));
    UserPopoverMenu menu =
        new UserPopoverMenu(
            texts,
            mock(SecurityService.class),
            service,
            user,
            new Button(),
            new UserPopoverMenu.Actions(null, null, null, null),
            status -> {});
    ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED, true);
    Notification original = currentNotification();
    assertText(Locale.ENGLISH, conflict);
    LanguageChangeEvent.fire(ui, Locale.GERMAN);
    assertSame(original, currentNotification());
    assertText(Locale.GERMAN, conflict);
    ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED, true);
    assertNotSame(original, currentNotification());
    assertText(Locale.GERMAN, conflict);
    LanguageChangeEvent.fire(ui, Locale.ENGLISH);
    assertText(Locale.ENGLISH, conflict);
    assertEquals(UserStatus.OFFLINE, user.getStatus());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void adminConflictSupplierUsesCurrentSessionLanguageAndUpdatesVisibleTitle(boolean startGerman) {
    LanguageChangeEvent.fire(ui, startGerman ? Locale.GERMAN : Locale.ENGLISH);
    AdminUserManagementSection admin =
        new AdminUserManagementSection(
            texts, mock(UserService.class), mock(RoleService.class), mock(SecurityService.class));
    ReflectionTestUtils.invokeMethod(
        admin, "showError", (java.util.function.Supplier<String>) texts::getBaseUserUpdateConflict);
    assertText(startGerman ? Locale.GERMAN : Locale.ENGLISH, true);
    LanguageChangeEvent.fire(ui, startGerman ? Locale.ENGLISH : Locale.GERMAN);
    assertText(startGerman ? Locale.ENGLISH : Locale.GERMAN, true);
  }

  private Notification currentNotification() {
    return (Notification)
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
  }

  private void assertText(Locale locale, boolean conflict) {
    NotificationHelper helper = NotificationHelper.getInstance();
    assertTrue(currentNotification().isOpened());
    Span title = (Span) ReflectionTestUtils.getField(helper, "titleText");
    Html message = (Html) ReflectionTestUtils.getField(helper, "messageText");
    MessageProperties expectedTexts = MessageProperties.forLocale(provider, locale);
    MessageProperties otherTexts =
        MessageProperties.forLocale(
            provider, locale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    String expectedTitle = expectedTexts.getBaseFailedTitle();
    String expectedMessage =
        conflict
            ? expectedTexts.getBaseUserUpdateConflict()
            : expectedTexts.getBaseUserStatusFailed();
    assertNotEquals(otherTexts.getBaseFailedTitle(), expectedTitle);
    assertNotEquals(
        conflict ? otherTexts.getBaseUserUpdateConflict() : otherTexts.getBaseUserStatusFailed(),
        expectedMessage);
    assertEquals(expectedTitle, title.getText());
    assertEquals("p", message.getElement().getTag());
    assertEquals(expectedMessage, message.getElement().getProperty("innerHTML"));
  }
}
