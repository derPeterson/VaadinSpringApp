package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SignInMethod;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.ExternalIdentityService.SignInSummary;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Password confirmation and validation timing through real dialog callbacks, without DB or SMTP.
 */
class AdminUserDialogValidationTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private UserService userService;
  private AdminUserManagementSection userManagementSection;
  private MessageProperties messageProperties;
  private Dialog dialog;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.ENGLISH);

  @BeforeEach
  void setup() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    userService = mock(UserService.class);
    RoleService roles = mock(RoleService.class);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
    messageProperties = new MessageProperties(new CustomI18NProvider());
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties, userService, roles, mock(SecurityService.class));
  }

  @AfterEach
  void cleanup() {
    if (dialog != null) {
      dialog.close();
    }
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    userManagementSection = null;
    vaadinSession = null;
    testUi = null;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void accountDetailsDistinguishRegistrationFromTheAvailableMethods(String language) {
    ExternalIdentityService identities = mock(ExternalIdentityService.class);
    when(identities.adminSignInSummary(2L))
        .thenReturn(
            Optional.of(
                new SignInSummary(
                    RegistrationChannel.GOOGLE,
                    Set.of(SignInMethod.PORTAL, SignInMethod.MICROSOFT))));
    userManagementSection.configureSignInDetails(identities);
    open(language, false);

    Badge registration = (Badge) signInDetail("admin-user-registered-via");
    Component available = signInDetail("admin-user-sign-in-methods");

    assertEquals(messageProperties.getExternalGoogle(), registration.getText());
    assertEquals(
        List.of(
            messageProperties.getSignInPortalPassword(), messageProperties.getExternalMicrosoft()),
        available.getChildren().map(Badge.class::cast).map(Badge::getText).toList());
  }

  private Component signInDetail(String id) {
    return descendants(dialog)
        .filter(item -> item.getId().filter(id::equals).isPresent())
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void blankEditPasswordDoesNotRequireConfirmationAndPreservesTheExistingPassword(String language) {
    open(language, false);

    assertFalse(confirm().isEnabled());
    assertFalse(confirm().isRequiredIndicatorVisible());
    assertTrue(save().isEnabled());
    save().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq(""));
    assertFalse(dialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void typingDoesNotShowErrorsButBlurShowsRulesAndMismatchUntilCorrected(String language) {
    open(language, false);
    password().setValue("w");

    assertTrue(confirm().isEnabled());
    assertTrue(confirm().isRequiredIndicatorVisible());
    assertFalse(password().isInvalid());
    assertFalse(confirm().isInvalid());
    assertFalse(save().isEnabled());
    blur(password());

    assertTrue(password().isInvalid());
    assertEquals(
        messageProperties.getAdminUsersPasswordValidationEdit(), password().getErrorMessage());
    focus(password());

    assertFalse(password().isInvalid());
    password().setValue("ChangedPassword!");
    blur(password());
    confirm().setValue("DifferentPassword!");

    assertFalse(confirm().isInvalid());
    assertFalse(save().isEnabled());
    blur(confirm());

    assertTrue(confirm().isInvalid());
    assertEquals(
        messageProperties.getBaseValidationPasswordConfirmMessage(), confirm().getErrorMessage());
    focus(confirm());

    assertFalse(confirm().isInvalid());
    confirm().setValue("ChangedPassword!");
    blur(confirm());

    assertFalse(confirm().isInvalid());
    assertTrue(save().isEnabled());
    save().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq("ChangedPassword!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void clearingTheOptionalPasswordDiscardsConfirmationAndItsError(String language) {
    open(language, false);
    password().setValue("ChangedPassword!");
    confirm().setValue("Wrong!");
    blur(confirm());

    assertTrue(confirm().isInvalid());
    password().clear();

    assertEquals("", confirm().getValue());
    assertFalse(confirm().isEnabled());
    assertFalse(confirm().isInvalid());
    assertFalse(confirm().isRequiredIndicatorVisible());
    assertTrue(save().isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void createRequiresAValidPasswordAndMatchingConfirmation(String language) {
    open(language, true);

    assertFalse(save().isEnabled());
    password().setValue("ValidPassword!");

    assertFalse(save().isEnabled());
    confirm().setValue("validPassword!");

    assertFalse(save().isEnabled());
    confirm().setValue("ValidPassword!");

    assertTrue(save().isEnabled());
    save().click();

    verify(userService)
        .createAdminUser(
            argThat(user -> Locale.forLanguageTag(language).equals(user.getPreferredLocale())),
            eq("ValidPassword!"));
  }

  @Test
  void superadminRoleIsOnlyOfferedToASuperadmin() {
    open("de", false);
    MultiSelectComboBox<RoleType> roles = roles();

    assertFalse(roles.getListDataView().getItems().toList().contains(RoleType.ROLE_SUPERADMIN));
    assertTrue(roles.getListDataView().getItems().toList().contains(RoleType.ROLE_ADMIN));
    when(userService.canManageSuperadmins()).thenReturn(true);
    roles.getDataProvider().refreshAll();

    assertTrue(roles.getListDataView().getItems().toList().contains(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void roleSelectionRequiresOneRoleAndSurvivesLanguageChanges() {
    open("de", false);
    MultiSelectComboBox<RoleType> roles = roles();
    roles.setValue(Set.of(RoleType.ROLE_USER, RoleType.ROLE_GUEST));

    assertEquals(messageProperties.getAdminUsersSelectedRoles(2), roles.getPlaceholder());
    localeReference.set(Locale.ENGLISH);
    userManagementSection.refreshTexts();

    assertEquals(Set.of(RoleType.ROLE_USER, RoleType.ROLE_GUEST), roles.getValue());
    assertEquals(messageProperties.getAdminUsersSelectedRoles(2), roles.getPlaceholder());
    assertEquals(
        messageProperties.getRoleGuest(), roles.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    roles.clear();

    assertFalse(save().isEnabled());
    roles.setValue(Set.of(RoleType.ROLE_USER));

    assertTrue(save().isEnabled());
  }

  @SuppressWarnings("unchecked")
  private MultiSelectComboBox<RoleType> roles() {
    return (MultiSelectComboBox<RoleType>)
        descendants(dialog).filter(MultiSelectComboBox.class::isInstance).findFirst().orElseThrow();
  }

  @Test
  void languageChangesPreserveValuesAndTranslateAnAlreadyDisplayedConfirmationError() {
    open("de", false);
    password().setValue("ValidPassword!");
    confirm().setValue("Different!");
    blur(confirm());
    localeReference.set(Locale.ENGLISH);
    userManagementSection.refreshTexts();

    assertEquals("ValidPassword!", password().getValue());
    assertEquals("Different!", confirm().getValue());
    assertEquals(
        messageProperties.getBaseValidationPasswordConfirmMessage(), confirm().getErrorMessage());
    assertEquals(
        messageProperties.getAccountConfirmPasswordPlaceholder(), confirm().getPlaceholder());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newRolesCanBeAssignedAndHaveTranslatedLabels(String language) {
    when(userService.canManageSuperadmins()).thenReturn(true);
    open(language, false);
    @SuppressWarnings("unchecked")
    MultiSelectComboBox<RoleType> roles =
        (MultiSelectComboBox<RoleType>)
            descendants(dialog)
                .filter(MultiSelectComboBox.class::isInstance)
                .findFirst()
                .orElseThrow();

    assertEquals(List.of(RoleType.values()), roles.getListDataView().getItems().toList());
    assertEquals(
        messageProperties.getRoleSuperadmin(),
        roles.getItemLabelGenerator().apply(RoleType.ROLE_SUPERADMIN));
    assertEquals(
        messageProperties.getRoleGuest(), roles.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    roles.setValue(Set.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_GUEST));
    save().click();
    ArgumentCaptor<UserEntity> edited = org.mockito.ArgumentCaptor.forClass(UserEntity.class);

    verify(userService).updateAdminUser(edited.capture(), eq(3L), eq(""));
    assertEquals(
        Set.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_GUEST),
        edited.getValue().getRoleEntities().stream()
            .map(RoleEntity::getName)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "edit", "delete"})
  void nativeDismissalIsEnabledAndClosingDoesNotPersistOrDelete(String action) {
    if (action.equals("delete")) {
      UserEntity targetUserEntity =
          UserEntity.builder()
              .id(2L)
              .version(3L)
              .firstName("Test")
              .lastName("User")
              .email("test@example.test")
              .build();
      when(userService.canDeleteUser(targetUserEntity)).thenReturn(true);
      ReflectionTestUtils.invokeMethod(userManagementSection, "deleteUser", targetUserEntity);
      testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();
      dialog =
          descendants(testUi)
              .filter(Dialog.class::isInstance)
              .map(Dialog.class::cast)
              .filter(Dialog::isOpened)
              .findFirst()
              .orElseThrow();
    } else {
      open("de", action.equals("create"));
    }
    assertTrue(dialog.isCloseOnOutsideClick());
    assertTrue(dialog.isCloseOnEsc());
    dialog.setOpened(false);

    assertFalse(dialog.isOpened());
    verify(userService, never()).save(any());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
    verify(userService, never()).deleteUser(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void simultaneousEditorsKeepTheirValidationAndSaveStateSeparate(String language) {
    open(language, false);
    Dialog editing = dialog;
    password().setValue("EditedPassword!");
    confirm().setValue("EditedPassword!");

    assertTrue(save().isEnabled());

    open(language, true);
    Dialog creating = dialog;

    assertNotSame(editing, creating);
    password().setValue("CreatedPassword!");
    confirm().setValue("DifferentPassword!");
    blur(confirm());

    assertFalse(save().isEnabled());

    dialog = editing;

    assertTrue(save().isEnabled());
    assertFalse(confirm().isInvalid());
    save().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq("EditedPassword!"));
    assertFalse(editing.isOpened());
    assertTrue(creating.isOpened());
    verify(userService, never()).createAdminUser(any(), any());

    dialog = creating;

    assertEquals("CreatedPassword!", password().getValue());
    assertTrue(confirm().isInvalid());
    confirm().setValue("CreatedPassword!");
    save().click();

    verify(userService).createAdminUser(any(), eq("CreatedPassword!"));
    assertFalse(creating.isOpened());
  }

  private void open(String language, boolean create) {
    Dialog previouslyOpened = dialog;
    localeReference.set(Locale.forLanguageTag(language));
    UserEntity testUserEntity =
        UserEntity.builder()
            .id(create ? null : 2L)
            .version(create ? null : 3L)
            .firstName("Test")
            .lastName("User")
            .email("test@example.test")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender(Gender.OTHER)
            .enabled(true)
            .roleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()))
            .build();
    ReflectionTestUtils.invokeMethod(userManagementSection, "openDialog", testUserEntity);
    testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    dialog =
        descendants(testUi)
            .filter(Dialog.class::isInstance)
            .map(Dialog.class::cast)
            .filter(Dialog::isOpened)
            .filter(candidate -> candidate != previouslyOpened)
            .findFirst()
            .orElseThrow();
  }

  private PasswordField password() {
    return passwordFields().get(0);
  }

  private PasswordField confirm() {
    return passwordFields().get(1);
  }

  private List<PasswordField> passwordFields() {
    return descendants(dialog)
        .filter(PasswordField.class::isInstance)
        .map(PasswordField.class::cast)
        .toList();
  }

  private Button save() {
    return dialog
        .getFooter()
        .getElement()
        .getChildren()
        .flatMap(element -> element.getComponent().stream())
        .flatMap(this::descendants)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(button -> button.getText().equals(messageProperties.getAdminUsersSave()))
        .findFirst()
        .orElseThrow();
  }

  private void blur(PasswordField field) {
    ComponentUtil.fireEvent(field, new BlurNotifier.BlurEvent<>(field, true));
  }

  private void focus(PasswordField field) {
    ComponentUtil.fireEvent(field, new FocusNotifier.FocusEvent<>(field, true));
  }

  private Stream<Component> descendants(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::descendants));
  }
}
