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
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/** Prüft echte Übersetzungen und Statusmeldungen über das vorhandene Sprachereignis. */
class UserStatusNotificationTest {
  private UI testUi;
  private VaadinSession vaadinSession;

  // Seitenrahmen, der das Sprachereignis verarbeitet
  private TrackedUserAppLayout trackedUserAppLayout;

  private final CustomI18NProvider customI18NProvider = new CustomI18NProvider();
  private final MessageProperties messageProperties = new MessageProperties(customI18NProvider);

  @BeforeEach
  void createStatusNotificationEnvironment() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    AtomicReference<Locale> activeLocaleReference = new AtomicReference<>(Locale.ENGLISH);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    doAnswer(
            mockInvocation -> {
              activeLocaleReference.set(mockInvocation.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());

    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);

    SecurityService securityService = mock(SecurityService.class);
    when(securityService.getCurrentUser(any(HttpServletRequest.class)))
        .thenReturn(Optional.empty());
    trackedUserAppLayout =
        new TrackedUserAppLayout(
            messageProperties,
            securityService,
            mock(UserService.class),
            new UserStatusBroadcaster(),
            mock(HttpServletRequest.class),
            mock(UserAvatarService.class)) {};
  }

  @AfterEach
  void clearStatusNotificationEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);

    trackedUserAppLayout = null;
    testUi = null;
    vaadinSession = null;
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void visiblePopoverFailureAndNewFailureTranslateImmediatelyIncludingTitle(
      boolean hasVersionConflict) {
    UserEntity testUserEntity = UserEntity.builder().id(7L).status(UserStatus.OFFLINE).build();
    UserService userService = mock(UserService.class);
    when(userService.updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true))
        .thenThrow(
            hasVersionConflict
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
            userStatus -> {});
    ReflectionTestUtils.invokeMethod(
        userPopoverMenu, "handleUserStatusChange", UserStatus.EMPLOYED);
    Notification originalNotification = readCurrentNotification();
    assertLocalizedFailureNotification(Locale.ENGLISH, hasVersionConflict);

    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(originalNotification, readCurrentNotification());
    assertLocalizedFailureNotification(Locale.GERMAN, hasVersionConflict);

    ReflectionTestUtils.invokeMethod(
        userPopoverMenu, "handleUserStatusChange", UserStatus.EMPLOYED);

    assertNotSame(originalNotification, readCurrentNotification());
    assertLocalizedFailureNotification(Locale.GERMAN, hasVersionConflict);

    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    assertLocalizedFailureNotification(Locale.ENGLISH, hasVersionConflict);

    assertEquals(UserStatus.OFFLINE, testUserEntity.getStatus());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void adminConflictSupplierUsesCurrentSessionLanguageAndUpdatesVisibleTitle(
      boolean startsInGerman) {
    LanguageChangeEvent.fire(testUi, startsInGerman ? Locale.GERMAN : Locale.ENGLISH);
    AdminUserManagementSection adminUserManagementSection =
        new AdminUserManagementSection(
            messageProperties,
            mock(UserService.class),
            mock(RoleService.class),
            mock(SecurityService.class));
    ReflectionTestUtils.invokeMethod(
        adminUserManagementSection,
        "showError",
        (Supplier<String>) messageProperties::getBaseUserUpdateConflict);
    assertLocalizedFailureNotification(startsInGerman ? Locale.GERMAN : Locale.ENGLISH, true);

    LanguageChangeEvent.fire(testUi, startsInGerman ? Locale.ENGLISH : Locale.GERMAN);
    assertLocalizedFailureNotification(startsInGerman ? Locale.ENGLISH : Locale.GERMAN, true);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @SuppressWarnings("unchecked")
  void statusButtonsPersistThroughTheExistingServiceAndKeepFailuresUnselected(
      boolean shouldStatusUpdateFail) {
    UserEntity testUserEntity = UserEntity.builder().id(7L).status(UserStatus.OFFLINE).build();
    UserService userService = mock(UserService.class);
    if (shouldStatusUpdateFail) {
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
            userStatus -> {});
    userPopoverMenu.addThemeName("cosmos-reference");
    Map<UserStatus, Button> statusButtonsByUserStatus =
        (Map<UserStatus, Button>) ReflectionTestUtils.getField(userPopoverMenu, "statusButtons");

    assertEquals(UserStatus.values().length, statusButtonsByUserStatus.size());
    statusButtonsByUserStatus.get(UserStatus.EMPLOYED).click();
    UserStatus expectedUserStatus =
        shouldStatusUpdateFail ? UserStatus.OFFLINE : UserStatus.EMPLOYED;

    assertEquals(expectedUserStatus, testUserEntity.getStatus());
    for (UserStatus userStatus : UserStatus.values()) {
      assertEquals(
          Boolean.toString(userStatus == expectedUserStatus),
          statusButtonsByUserStatus.get(userStatus).getElement().getAttribute("aria-pressed"));
    }

    vaadinSession.setLocale(Locale.GERMAN);
    userPopoverMenu.refreshForLanguageChange();

    assertEquals(
        messageProperties.getUserStatusLabel(UserStatus.EMPLOYED),
        statusButtonsByUserStatus.get(UserStatus.EMPLOYED).getAriaLabel().orElseThrow());
    verify(userService).updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true);
  }

  private Notification readCurrentNotification() {
    return (Notification)
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
  }

  private void assertLocalizedFailureNotification(
      Locale expectedLocale, boolean hasVersionConflict) {
    NotificationHelper notificationHelper = NotificationHelper.getInstance();

    assertTrue(readCurrentNotification().isOpened());
    Span titleSpan = (Span) ReflectionTestUtils.getField(notificationHelper, "titleText");
    Html messageHtml = (Html) ReflectionTestUtils.getField(notificationHelper, "messageText");
    MessageProperties expectedMessageProperties =
        MessageProperties.forLocale(customI18NProvider, expectedLocale);
    MessageProperties alternateLocaleMessageProperties =
        MessageProperties.forLocale(
            customI18NProvider,
            expectedLocale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    String expectedTitleText = expectedMessageProperties.getBaseFailedTitle();
    String expectedMessageText =
        hasVersionConflict
            ? expectedMessageProperties.getBaseUserUpdateConflict()
            : expectedMessageProperties.getBaseUserStatusFailed();

    assertNotEquals(alternateLocaleMessageProperties.getBaseFailedTitle(), expectedTitleText);
    assertNotEquals(
        hasVersionConflict
            ? alternateLocaleMessageProperties.getBaseUserUpdateConflict()
            : alternateLocaleMessageProperties.getBaseUserStatusFailed(),
        expectedMessageText);
    assertEquals(expectedTitleText, titleSpan.getText());
    assertEquals("p", messageHtml.getElement().getTag());
    assertEquals(expectedMessageText, messageHtml.getElement().getProperty("innerHTML"));
  }
}
