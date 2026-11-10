package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import java.util.Map;
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
  private UI testUi;
  private VaadinSession vaadinSession;
  private TrackedUserAppLayout layout;
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();
  private final MessageProperties messageProperties = new MessageProperties(i18nProvider);

  @BeforeEach
  void setup() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.ENGLISH);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    doAnswer(
            call -> {
              localeReference.set(call.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    SecurityService security = mock(SecurityService.class);
    when(security.getCurrentUser(any(HttpServletRequest.class))).thenReturn(Optional.empty());
    layout =
        new TrackedUserAppLayout(
            messageProperties,
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
    testUi = null;
    vaadinSession = null;
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void visiblePopoverFailureAndNewFailureTranslateImmediatelyIncludingTitle(boolean conflict) {
    UserEntity testUserEntity = UserEntity.builder().id(7L).status(UserStatus.OFFLINE).build();
    UserService userService = mock(UserService.class);
    when(userService.updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true))
        .thenThrow(
            conflict
                ? new OptimisticLockingFailureException("conflict")
                : new IllegalStateException("failed"));
    UserPopoverMenu userPopoverMenu =
        new UserPopoverMenu(
            messageProperties,
            mock(SecurityService.class),
            userService,
            testUserEntity,
            new Button(),
            new UserPopoverMenu.UserPopoverActions(null, null),
            status -> {});
    ReflectionTestUtils.invokeMethod(
        userPopoverMenu, "handleUserStatusChange", UserStatus.EMPLOYED);
    Notification original = currentNotification();
    assertText(Locale.ENGLISH, conflict);
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(original, currentNotification());
    assertText(Locale.GERMAN, conflict);
    ReflectionTestUtils.invokeMethod(
        userPopoverMenu, "handleUserStatusChange", UserStatus.EMPLOYED);

    assertNotSame(original, currentNotification());
    assertText(Locale.GERMAN, conflict);
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    assertText(Locale.ENGLISH, conflict);

    assertEquals(UserStatus.OFFLINE, testUserEntity.getStatus());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void adminConflictSupplierUsesCurrentSessionLanguageAndUpdatesVisibleTitle(boolean startGerman) {
    LanguageChangeEvent.fire(testUi, startGerman ? Locale.GERMAN : Locale.ENGLISH);
    AdminUserManagementSection admin =
        new AdminUserManagementSection(
            messageProperties,
            mock(UserService.class),
            mock(RoleService.class),
            mock(SecurityService.class));
    ReflectionTestUtils.invokeMethod(
        admin,
        "showError",
        (java.util.function.Supplier<String>) messageProperties::getBaseUserUpdateConflict);
    assertText(startGerman ? Locale.GERMAN : Locale.ENGLISH, true);
    LanguageChangeEvent.fire(testUi, startGerman ? Locale.ENGLISH : Locale.GERMAN);
    assertText(startGerman ? Locale.ENGLISH : Locale.GERMAN, true);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @SuppressWarnings("unchecked")
  void statusButtonsPersistThroughTheExistingServiceAndKeepFailuresUnselected(boolean fail) {
    UserEntity testUserEntity = UserEntity.builder().id(7L).status(UserStatus.OFFLINE).build();
    UserService userService = mock(UserService.class);
    if (fail) {
      when(userService.updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true))
          .thenThrow(new OptimisticLockingFailureException("conflict"));
    } else {
      when(userService.updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true))
          .thenReturn(new UserService.StatusUpdate(UserStatus.EMPLOYED, true));
    }
    UserPopoverMenu userPopoverMenu =
        new UserPopoverMenu(
            messageProperties,
            mock(SecurityService.class),
            userService,
            testUserEntity,
            new Button(),
            new UserPopoverMenu.UserPopoverActions(null, null),
            status -> {});
    userPopoverMenu.addThemeName("cosmos-reference");
    Map<UserStatus, Button> buttons =
        (Map<UserStatus, Button>) ReflectionTestUtils.getField(userPopoverMenu, "statusButtons");

    assertEquals(UserStatus.values().length, buttons.size());
    buttons.get(UserStatus.EMPLOYED).click();
    UserStatus expected = fail ? UserStatus.OFFLINE : UserStatus.EMPLOYED;

    assertEquals(expected, testUserEntity.getStatus());
    for (UserStatus status : UserStatus.values()) {
      assertEquals(
          Boolean.toString(status == expected),
          buttons.get(status).getElement().getAttribute("aria-pressed"));
    }
    vaadinSession.setLocale(Locale.GERMAN);
    userPopoverMenu.refreshForLanguageChange();

    assertEquals(
        messageProperties.getUserStatusLabel(UserStatus.EMPLOYED),
        buttons.get(UserStatus.EMPLOYED).getAriaLabel().orElseThrow());
    verify(userService).updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true);
  }

  private Notification currentNotification() {
    return (Notification)
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
  }

  private void assertText(Locale locale, boolean conflict) {
    NotificationHelper helper = NotificationHelper.getInstance();

    assertTrue(currentNotification().isOpened());
    Span titleSpan = (Span) ReflectionTestUtils.getField(helper, "titleText");
    Html messageHtml = (Html) ReflectionTestUtils.getField(helper, "messageText");
    MessageProperties expectedTexts = MessageProperties.forLocale(i18nProvider, locale);
    MessageProperties otherTexts =
        MessageProperties.forLocale(
            i18nProvider, locale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    String expectedTitle = expectedTexts.getBaseFailedTitle();
    String expectedMessage =
        conflict
            ? expectedTexts.getBaseUserUpdateConflict()
            : expectedTexts.getBaseUserStatusFailed();

    assertNotEquals(otherTexts.getBaseFailedTitle(), expectedTitle);
    assertNotEquals(
        conflict ? otherTexts.getBaseUserUpdateConflict() : otherTexts.getBaseUserStatusFailed(),
        expectedMessage);
    assertEquals(expectedTitle, titleSpan.getText());
    assertEquals("p", messageHtml.getElement().getTag());
    assertEquals(expectedMessage, messageHtml.getElement().getProperty("innerHTML"));
  }
}
