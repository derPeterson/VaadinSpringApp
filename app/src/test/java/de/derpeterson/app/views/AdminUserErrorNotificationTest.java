package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/** Real dialog/button callbacks and language events; no application, DB or SMTP. */
class AdminUserErrorNotificationTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private TrackedUserAppLayout layout;
  private AdminUserManagementSection userManagementSection;
  private UserService userService;
  private SecurityService securityService;
  private RoleService roles;
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();
  private final MessageProperties messageProperties = new MessageProperties(i18nProvider);
  private ListAppender<ILoggingEvent> logs;
  private Logger logger;

  @BeforeEach
  void setup() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    AtomicReference<Locale> locale = new AtomicReference<>(Locale.ENGLISH);
    when(vaadinSession.getLocale()).thenAnswer(call -> locale.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    doAnswer(
            call -> {
              locale.set(call.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    userService = mock(UserService.class);
    securityService = mock(SecurityService.class);
    roles = mock(RoleService.class);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
    when(userService.canDeleteUser(any())).thenReturn(true);
    layout =
        new TrackedUserAppLayout(
            messageProperties,
            securityService,
            userService,
            new UserStatusBroadcaster(),
            mock(HttpServletRequest.class),
            mock(UserAvatarService.class)) {};
    userManagementSection =
        new AdminUserManagementSection(messageProperties, userService, roles, securityService);
    // Same section refresh path used by AdminView.onTrackedUserLanguageChanged.
    ComponentUtil.addListener(
        testUi, LanguageChangeEvent.class, event -> userManagementSection.refreshTexts());
    logger = (Logger) LoggerFactory.getLogger(AdminUserManagementSection.class);
    logs = new ListAppender<>();
    logs.start();
    logger.addAppender(logs);
  }

  @AfterEach
  void cleanup() {
    logger.detachAppender(logs);
    logs.stop();
    descendants(testUi)
        .filter(Dialog.class::isInstance)
        .map(Dialog.class::cast)
        .toList()
        .forEach(Dialog::close);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    userManagementSection = null;
    layout = null;
    testUi = null;
    vaadinSession = null;
  }

  private static Stream<Component> descendants(Component root) {
    Stream<Component> children = root.getChildren();
    if (root instanceof Dialog dialog) {
      // Dialog.getChildren() excludes the native footer's slotted wrapper.
      // Traverse it explicitly so error tests exercise the real save/delete button.
      children =
          Stream.concat(
              children,
              dialog
                  .getFooter()
                  .getElement()
                  .getChildren()
                  .flatMap(element -> element.getComponent().stream()));
    }
    return Stream.concat(
        Stream.of(root), children.flatMap(AdminUserErrorNotificationTest::descendants));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void birthdayInputInCreateAndEditFollowsTheLocaleWithoutLosingTheDate(boolean create) {
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(create));
    DatePicker picker =
        descendants(dialog())
            .filter(DatePicker.class::isInstance)
            .map(DatePicker.class::cast)
            .findFirst()
            .orElseThrow();
    assertFalse(picker.isAutoOpen());
    assertEquals(Locale.GERMAN, picker.getLocale());
    assertEquals(messageProperties.getRegistrationBirthDatePlaceholder(), picker.getPlaceholder());
    LocalDate selected = picker.getValue();
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    assertEquals(Locale.ENGLISH, picker.getLocale());
    assertEquals(messageProperties.getRegistrationBirthDatePlaceholder(), picker.getPlaceholder());
    assertEquals(selected, picker.getValue());
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    assertEquals(Locale.GERMAN, picker.getLocale());
    assertEquals(messageProperties.getRegistrationBirthDatePlaceholder(), picker.getPlaceholder());
    assertEquals(selected, picker.getValue());
  }

  private UserEntity user(boolean create) {
    return UserEntity.builder()
        .id(create ? null : 2L)
        .version(create ? null : 3L)
        .firstName("Test")
        .lastName("User")
        .email("test@example.com")
        .password("hash")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender(Gender.OTHER)
        .enabled(true)
        .roleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_ADMIN).build()))
        .build();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void passwordHelpFollowsFormStateAndLanguage(boolean create) {
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(create));
    Dialog editor = dialog();
    List<PasswordField> passwords =
        descendants(editor)
            .filter(PasswordField.class::isInstance)
            .map(PasswordField.class::cast)
            .toList();
    Span hint =
        descendants(editor)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .filter(span -> span.hasClassName("admin-user-password-hint"))
            .findFirst()
            .orElseThrow();
    assertEquals(
        create
            ? messageProperties.getAdminUsersPasswordHelperCreate()
            : messageProperties.getAdminUsersPasswordHelperEdit(),
        hint.getText());
    assertFalse(passwords.get(1).isEnabled());
    assertTrue(hint.getText().contains(messageProperties.getAdminUsersPasswordHelperCreate()));

    passwords.getFirst().setValue("Password!");
    assertEquals(messageProperties.getAdminUsersPasswordHelperCreate(), hint.getText());
    assertTrue(passwords.get(1).isEnabled());
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    assertEquals(messageProperties.getAdminUsersPasswordHelperCreate(), hint.getText());

    passwords.getFirst().clear();
    assertEquals(
        create
            ? messageProperties.getAdminUsersPasswordHelperCreate()
            : messageProperties.getAdminUsersPasswordHelperEdit(),
        hint.getText());
    assertFalse(passwords.get(1).isEnabled());
    assertTrue(hint.getText().contains(messageProperties.getAdminUsersPasswordHelperCreate()));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    assertEquals(
        create
            ? messageProperties.getAdminUsersPasswordHelperCreate()
            : messageProperties.getAdminUsersPasswordHelperEdit(),
        hint.getText());
  }

  private Dialog dialog() {
    testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    return descendants(testUi)
        .filter(Dialog.class::isInstance)
        .map(Dialog.class::cast)
        .filter(Dialog::isOpened)
        .findFirst()
        .orElseThrow();
  }

  private void submit(String operation, UserEntity user) {
    ReflectionTestUtils.invokeMethod(
        userManagementSection, operation.equals("delete") ? "deleteUser" : "openDialog", user);
    Dialog dialog = dialog();
    if (operation.equals("create")) {
      descendants(dialog)
          .filter(PasswordField.class::isInstance)
          .map(PasswordField.class::cast)
          .forEach(field -> field.setValue("Password!"));
    }
    String label =
        operation.equals("delete")
            ? messageProperties.getPortalDeleteAction()
            : messageProperties.getAdminUsersSave();
    descendants(dialog)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> button.getText().equals(label))
        .findFirst()
        .orElseThrow()
        .click();
  }

  static Stream<Arguments> failures() {
    return Stream.of(false, true)
        .flatMap(german ->
            Stream.of("create", "edit", "delete")
                .flatMap(operation ->
                    Stream.of(
                            "version",
                            "email",
                            "admin",
                            "argument",
                            "unknown-argument",
                            "unknown",
                            "integrity",
                            "null-message")
                        .map(kind -> Arguments.of(german, operation, kind))));
  }

  private RuntimeException failure(String kind) {
    return switch (kind) {
      case "version" -> new OptimisticLockingFailureException("secret SELECT password FROM users");
      case "email" ->
          new DataIntegrityViolationException(
              "secret SQL",
              new ConstraintViolationException(
                  "secret SQL",
                  new SQLException("secret SQL", "23505"),
                  "INSERT secret SQL",
                  "uk_users_email"));
      case "admin" ->
          new IllegalStateException("Der letzte aktive Superadmin muss erhalten bleiben.");
      case "argument" -> new IllegalArgumentException("Invalid password");
      case "unknown-argument" -> new IllegalArgumentException("secret configuration argument");
      case "integrity" ->
          new DataIntegrityViolationException(
              "secret FK SQL",
              new ConstraintViolationException(
                  "secret FK SQL",
                  new SQLException("secret FK SQL", "23503"),
                  "DELETE secret SQL",
                  "unknown_fk"));
      case "null-message" -> new RuntimeException();
      default -> new IllegalStateException("secret SQL configuration failure");
    };
  }

  @ParameterizedTest
  @MethodSource("failures")
  void mutationCallbacksMapErrorsLogDetailsAndTranslateTheSameVisibleNotification(
      boolean german, String operation, String kind) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    RuntimeException failure = failure(kind);
    if (operation.equals("delete")) {
      doThrow(failure).when(userService).deleteUser(any());
    } else if (operation.equals("create")) {
      doThrow(failure).when(userService).createAdminUser(any(), any());
    } else {
      doThrow(failure).when(userService).updateAdminUser(any(), any(), any());
    }
    submit(operation, user(operation.equals("create")));
    String expected =
        switch (kind) {
          case "version" -> messageProperties.getBaseUserUpdateConflict();
          case "email" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getBaseValidationEmailExistsMessage();
          case "admin" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteLastSuperadmin()
                  : messageProperties.getAdminUsersErrorLastActiveSuperadmin();
          case "argument" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getAdminUsersValidationCheckFields();
          default ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getAdminUsersErrorSaveFailed();
        };
    Object notification =
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    assertNotification(expected);
    assertTrue(
        logs.list.stream()
            .anyMatch(event ->
                event.getThrowableProxy() != null
                    && Objects.equals(
                        event.getThrowableProxy().getMessage(), failure.getClass().getName())));
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertSame(
        notification,
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification"));
    String translated =
        switch (kind) {
          case "version" -> messageProperties.getBaseUserUpdateConflict();
          case "email" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getBaseValidationEmailExistsMessage();
          case "admin" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteLastSuperadmin()
                  : messageProperties.getAdminUsersErrorLastActiveSuperadmin();
          case "argument" ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getAdminUsersValidationCheckFields();
          default ->
              operation.equals("delete")
                  ? messageProperties.getAdminUsersErrorDeleteFailed()
                  : messageProperties.getAdminUsersErrorSaveFailed();
        };
    assertNotEquals(expected, translated);
    assertNotification(translated);
    if (operation.equals("delete")) {
      verify(userService).deleteUser(any());
    } else if (operation.equals("create")) {
      verify(userService).createAdminUser(any(), any());
    } else {
      verify(userService).updateAdminUser(any(), eq(3L), any());
    }
  }

  private void assertNotification(String expected) {
    NotificationHelper helper = NotificationHelper.getInstance();
    assertEquals(
        messageProperties.getBaseFailedTitle(),
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
    String actual =
        ((Html) ReflectionTestUtils.getField(helper, "messageText"))
            .getElement()
            .getProperty("innerHTML");
    assertEquals(expected, actual);
    assertFalse(actual.contains("secret"));
    assertFalse(actual.contains("SELECT"));
    assertFalse(actual.contains("SQLException"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void manualRefreshReportsSuccessAndFollowsLanguage(boolean german) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    NotificationHelper helper = NotificationHelper.getInstance();
    assertNull(ReflectionTestUtils.getField(helper, "titleText"));
    clearInvocations(userService);

    Button refresh = (Button) ReflectionTestUtils.getField(userManagementSection, "refreshButton");
    refresh.click();

    verify(userService).findAllUsers();
    Span notificationTitle = (Span) ReflectionTestUtils.getField(helper, "titleText");
    assertEquals(messageProperties.getAdminUsersRefreshSuccess(), notificationTitle.getText());
    assertTrue(notificationTitle.hasClassName("text-success"));
    Object notification = ReflectionTestUtils.getField(helper, "currentNotification");
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertSame(notification, ReflectionTestUtils.getField(helper, "currentNotification"));
    assertEquals(messageProperties.getAdminUsersRefreshSuccess(), notificationTitle.getText());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void manualRefreshReportsFailureAndCanBeRetried(boolean german) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    Object previousUsers = ReflectionTestUtils.getField(userManagementSection, "users");
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("secret SELECT account data");
    when(userService.findAllUsers()).thenThrow(failure);

    Button refresh = (Button) ReflectionTestUtils.getField(userManagementSection, "refreshButton");
    refresh.click();

    assertNotification(messageProperties.getAdminUsersRefreshFailed());
    assertSame(previousUsers, ReflectionTestUtils.getField(userManagementSection, "users"));
    assertTrue(
        logs.list.stream()
            .anyMatch(event ->
                event.getThrowableProxy() != null
                    && event
                        .getThrowableProxy()
                        .getMessage()
                        .equals(failure.getClass().getName())));
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertNotification(messageProperties.getAdminUsersRefreshFailed());

    doReturn(List.of()).when(userService).findAllUsers();
    refresh.click();
    Span notificationTitle =
        (Span) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "titleText");
    assertEquals(messageProperties.getAdminUsersRefreshSuccess(), notificationTitle.getText());
  }

  static Stream<Arguments> guards() {
    return Stream.of(false, true)
        .flatMap(german ->
            Stream.of("own-delete", "last-delete", "role", "disable", "remove-admin", "last-save")
                .map(guard -> Arguments.of(german, guard)));
  }

  @ParameterizedTest
  @MethodSource("guards")
  void preflightHintsAreDynamicInBothDirections(boolean german, String guard) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    UserEntity user = user(false);
    if (guard.equals("own-delete") || guard.equals("disable") || guard.equals("remove-admin")) {
      when(securityService.getCurrentUser()).thenReturn(Optional.of(user));
    }
    if (guard.equals("last-delete")) {
      when(userService.canDeleteUser(any())).thenReturn(false);
    }
    if (guard.equals("role")) {
      when(roles.findByName(any())).thenReturn(Optional.empty());
    }
    if (guard.equals("disable")) {
      user.setEnabled(false);
    }
    if (guard.equals("remove-admin")) {
      user.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
    }
    if (guard.equals("last-save")) {
      when(userService.wouldRemoveLastEnabledSuperadmin(any(), anyBoolean(), any()))
          .thenReturn(true);
    }
    if (guard.endsWith("delete")) {
      ReflectionTestUtils.invokeMethod(userManagementSection, "deleteUser", user);
    } else {
      submit("edit", user);
    }
    String expected = guardText(guard);
    assertNotification(expected);
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertNotEquals(expected, guardText(guard));
    assertNotification(guardText(guard));
    verify(userService, never()).updateAdminUser(any(), any(), any());
    verify(userService, never()).deleteUser(any());
  }

  private String guardText(String guard) {
    return switch (guard) {
      case "own-delete" -> messageProperties.getAdminUsersErrorDeleteOwnUser();
      case "last-delete" -> messageProperties.getAdminUsersErrorDeleteLastSuperadmin();
      case "role" -> messageProperties.getAdminUsersErrorRoleMissing();
      case "disable" -> messageProperties.getAdminUsersErrorDisableOwnUser();
      case "remove-admin" -> messageProperties.getAdminUsersErrorRemoveOwnAdminRole();
      default -> messageProperties.getAdminUsersErrorLastActiveSuperadmin();
    };
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void alreadyVisibleFieldErrorsRefreshWithoutWritingAndClosedDialogsReleaseRefreshers(
      boolean german) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(false));
    Dialog dialog = dialog();
    TextField firstName =
        descendants(dialog)
            .filter(TextField.class::isInstance)
            .map(TextField.class::cast)
            .filter(
                field -> field.getLabel().equals(messageProperties.getAdminUsersFieldFirstName()))
            .findFirst()
            .orElseThrow();
    firstName.setValue(" ");
    assertTrue(firstName.isInvalid());
    assertEquals(messageProperties.getBaseValidationRequiredMessage(), firstName.getErrorMessage());
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertTrue(firstName.isInvalid());
    assertEquals(messageProperties.getBaseValidationRequiredMessage(), firstName.getErrorMessage());
    verify(userService, never()).updateAdminUser(any(), any(), any());
    dialog.close();
    assertTrue(
        ((List<?>) ReflectionTestUtils.getField(userManagementSection, "dialogTextRefreshActions"))
            .isEmpty());
  }

  static Stream<Arguments> preflightFailures() {
    return Stream.of(false, true)
        .flatMap(german -> Stream.of(false, true).map(deleting -> Arguments.of(german, deleting)));
  }

  @ParameterizedTest
  @MethodSource("preflightFailures")
  void unexpectedPreflightDatabaseFailuresAlsoUseGenericDynamicHints(
      boolean german, boolean deleting) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("secret SQL connection failure");
    if (deleting) {
      when(userService.canDeleteUser(any())).thenThrow(failure);
      ReflectionTestUtils.invokeMethod(userManagementSection, "deleteUser", user(false));
    } else {
      when(userService.wouldRemoveLastEnabledSuperadmin(any(), anyBoolean(), any()))
          .thenThrow(failure);
      submit("edit", user(false));
    }
    assertNotification(
        deleting
            ? messageProperties.getAdminUsersErrorDeleteFailed()
            : messageProperties.getAdminUsersErrorSaveFailed());
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertNotification(
        deleting
            ? messageProperties.getAdminUsersErrorDeleteFailed()
            : messageProperties.getAdminUsersErrorSaveFailed());
    assertTrue(
        logs.list.stream()
            .anyMatch(event ->
                event.getThrowableProxy() != null
                    && event
                        .getThrowableProxy()
                        .getMessage()
                        .equals(failure.getClass().getName())));
  }

  static Stream<Arguments> emailChecks() {
    return Stream.of(false, true)
        .flatMap(german ->
            Stream.of(false, true)
                .flatMap(create ->
                    Stream.of("duplicate", "connection", "integrity", "unexpected")
                        .map(kind -> Arguments.of(german, create, kind))));
  }

  @ParameterizedTest
  @MethodSource("emailChecks")
  void emailValidationSeparatesDuplicatesFromTechnicalFailuresAndBlocksSaveInBothLanguages(
      boolean german, boolean create, String kind) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(create));
    Dialog dialog = dialog();
    if (create) {
      descendants(dialog)
          .filter(PasswordField.class::isInstance)
          .map(PasswordField.class::cast)
          .forEach(field -> field.setValue("Password!"));
    }
    EmailField email =
        descendants(dialog)
            .filter(EmailField.class::isInstance)
            .map(EmailField.class::cast)
            .findFirst()
            .orElseThrow();
    Button save =
        descendants(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
            .findFirst()
            .orElseThrow();
    boolean technical = !kind.equals("duplicate");
    RuntimeException failure =
        switch (kind) {
          case "connection" ->
              new DataAccessResourceFailureException("secret SELECT connection failure");
          case "integrity" -> new DataIntegrityViolationException("secret SQL integrity failure");
          default -> new IllegalStateException("secret SQL unexpected failure");
        };
    if (technical) {
      when(userService.emailExistsForOtherUser(anyString(), any())).thenThrow(failure);
    } else {
      when(userService.emailExistsForOtherUser(anyString(), any())).thenReturn(true);
    }
    email.setValue("other@example.com");
    assertTrue(email.isInvalid());
    assertEquals(
        technical
            ? messageProperties.getAdminUsersErrorSaveFailed()
            : messageProperties.getBaseValidationEmailExistsMessage(),
        email.getErrorMessage());
    assertFalse(email.getErrorMessage().contains("secret"));
    assertFalse(save.isEnabled());
    verify(userService, atLeastOnce())
        .emailExistsForOtherUser("other@example.com", create ? null : 2L);
    // Exercise the server callback as well, not merely the disabled button.
    save.setEnabled(true);
    save.click();
    assertFalse(save.isEnabled());
    assertNotification(
        technical
            ? messageProperties.getAdminUsersErrorSaveFailed()
            : messageProperties.getAdminUsersValidationCheckFields());
    Object notification =
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertSame(
        notification,
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification"));
    assertNotification(
        technical
            ? messageProperties.getAdminUsersErrorSaveFailed()
            : messageProperties.getAdminUsersValidationCheckFields());
    assertEquals(
        technical
            ? messageProperties.getAdminUsersErrorSaveFailed()
            : messageProperties.getBaseValidationEmailExistsMessage(),
        email.getErrorMessage());
    assertTrue(email.isInvalid());
    assertFalse(save.isEnabled());
    verify(userService, never()).updatePassword(any(), any());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
    if (technical) {
      assertTrue(
          logs.list.stream()
              .anyMatch(event ->
                  event.getThrowableProxy() != null
                      && event
                          .getThrowableProxy()
                          .getMessage()
                          .equals(failure.getClass().getName())));
    } else {
      assertTrue(logs.list.isEmpty());
    }
  }

  @ParameterizedTest
  @MethodSource("preflightFailures")
  void successfulRetryClearsTechnicalFieldErrorAndAllowsCreateOrEdit(
      boolean german, boolean create) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(create));
    Dialog dialog = dialog();
    if (create) {
      descendants(dialog)
          .filter(PasswordField.class::isInstance)
          .map(PasswordField.class::cast)
          .forEach(field -> field.setValue("Password!"));
    }
    EmailField email =
        descendants(dialog)
            .filter(EmailField.class::isInstance)
            .map(EmailField.class::cast)
            .findFirst()
            .orElseThrow();
    Button save =
        descendants(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
            .findFirst()
            .orElseThrow();
    when(userService.emailExistsForOtherUser(anyString(), any()))
        .thenThrow(new DataAccessResourceFailureException("secret SQL"));
    email.setValue("unavailable@example.com");
    assertTrue(email.isInvalid());
    assertFalse(save.isEnabled());
    doReturn(false).when(userService).emailExistsForOtherUser(anyString(), any());
    email.setValue("available@example.com");
    assertFalse(email.isInvalid());
    assertTrue(save.isEnabled());
    save.click();
    if (create) {
      verify(userService)
          .createAdminUser(
              argThat(edited -> edited.getEmail().equals("available@example.com")), any());
    } else {
      verify(userService)
          .updateAdminUser(
              argThat(edited -> edited.getEmail().equals("available@example.com")), eq(3L), any());
    }
    assertFalse(dialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void adminCreatedDisabledAccountIsNotEligibleForSelfVerification(boolean german) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(true));
    Dialog dialog = dialog();
    descendants(dialog)
        .filter(PasswordField.class::isInstance)
        .map(PasswordField.class::cast)
        .forEach(field -> field.setValue("Password!"));
    descendants(dialog)
        .filter(Checkbox.class::isInstance)
        .map(Checkbox.class::cast)
        .filter(field -> field.hasClassName("admin-user-enabled-checkbox"))
        .findFirst()
        .orElseThrow()
        .setValue(false);
    Button save =
        descendants(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
            .findFirst()
            .orElseThrow();
    assertTrue(save.isEnabled());
    save.click();
    verify(userService)
        .createAdminUser(
            argThat(edited -> !edited.isEnabled() && !edited.isVerificationPending()), any());
    assertFalse(dialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void invalidEmailFormatStillSkipsTheTechnicalCheckAfterAnEarlierFailure(boolean german) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(false));
    Dialog dialog = dialog();
    EmailField email =
        descendants(dialog)
            .filter(EmailField.class::isInstance)
            .map(EmailField.class::cast)
            .findFirst()
            .orElseThrow();
    when(userService.emailExistsForOtherUser(anyString(), any()))
        .thenThrow(new DataAccessResourceFailureException("secret SQL"));
    email.setValue("unavailable@example.com");
    clearInvocations(userService);
    email.setValue("invalid");
    assertTrue(email.isInvalid());
    // Flow's pre-existing default EmailField validator rejects this value
    // before the custom validators, with its default empty error text.
    assertEquals("", email.getErrorMessage());
    verify(userService, never()).emailExistsForOtherUser(anyString(), any());
    Button save =
        descendants(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
            .findFirst()
            .orElseThrow();
    save.setEnabled(true);
    save.click();
    assertNotification(messageProperties.getAdminUsersValidationCheckFields());
    verify(userService, never()).updateAdminUser(any(), any(), any());
  }

  @ParameterizedTest
  @MethodSource("preflightFailures")
  void technicalFailureFirstEncounteredOnSaveBlocksAnInitiallyValidForm(
      boolean german, boolean create) {
    LanguageChangeEvent.fire(testUi, german ? Locale.GERMAN : Locale.ENGLISH);
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", user(create));
    Dialog dialog = dialog();
    if (create) {
      descendants(dialog)
          .filter(PasswordField.class::isInstance)
          .map(PasswordField.class::cast)
          .forEach(field -> field.setValue("Password!"));
    }
    Button save =
        descendants(dialog)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
            .findFirst()
            .orElseThrow();
    assertTrue(save.isEnabled());
    when(userService.emailExistsForOtherUser(anyString(), any()))
        .thenThrow(new DataAccessResourceFailureException("secret SQL on save"));
    save.click();
    assertNotification(messageProperties.getAdminUsersErrorSaveFailed());
    assertFalse(save.isEnabled());
    assertTrue(dialog.isOpened());
    LanguageChangeEvent.fire(testUi, german ? Locale.ENGLISH : Locale.GERMAN);
    assertNotification(messageProperties.getAdminUsersErrorSaveFailed());
    EmailField email =
        descendants(dialog)
            .filter(EmailField.class::isInstance)
            .map(EmailField.class::cast)
            .findFirst()
            .orElseThrow();
    assertTrue(email.isInvalid());
    assertEquals(messageProperties.getAdminUsersErrorSaveFailed(), email.getErrorMessage());
    verify(userService, never()).updatePassword(any(), any());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
  }
}
