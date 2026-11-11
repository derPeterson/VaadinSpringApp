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
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
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

/** Prüft Passwortbestätigung und Validierungszeitpunkte über Dialogaktionen ohne DB oder SMTP. */
class AdminUserDialogValidationTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private UserService userService;
  private SecurityService securityService;
  private AdminUserManagementSection userManagementSection;
  private MessageProperties messageProperties;
  private Dialog userDialog;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.ENGLISH);

  @BeforeEach
  void createUserDialogEnvironment() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(invocation -> localeReference.get());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    userService = mock(UserService.class);
    securityService = mock(SecurityService.class);
    RoleService roleService = mock(RoleService.class);
    when(roleService.findByName(any()))
        .thenAnswer(invocation ->
            Optional.of(RoleEntity.builder().name(invocation.getArgument(0)).build()));

    messageProperties = new MessageProperties(new CustomI18NProvider());
    userManagementSection =
        new AdminUserManagementSection(
            messageProperties, userService, roleService, securityService);
  }

  @AfterEach
  void clearUserDialogEnvironment() {
    if (userDialog != null) {
      userDialog.close();
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
  void accountDetailsDistinguishRegistrationFromTheAvailableMethods(String languageTag) {
    ExternalIdentityService externalIdentityService = mock(ExternalIdentityService.class);
    when(externalIdentityService.adminSignInSummary(2L))
        .thenReturn(
            Optional.of(
                new SignInSummary(
                    RegistrationChannel.GOOGLE,
                    Set.of(SignInMethod.PORTAL, SignInMethod.MICROSOFT))));
    userManagementSection.configureSignInDetails(externalIdentityService);
    openUserDialog(languageTag, false);

    Badge registrationChannelBadge = (Badge) findSignInDetailComponent("admin-user-registered-via");
    Component availableSignInMethodsComponent =
        findSignInDetailComponent("admin-user-sign-in-methods");

    assertEquals(messageProperties.getExternalGoogle(), registrationChannelBadge.getText());
    assertEquals(
        List.of(
            messageProperties.getSignInPortalPassword(), messageProperties.getExternalMicrosoft()),
        availableSignInMethodsComponent
            .getChildren()
            .map(Badge.class::cast)
            .map(Badge::getText)
            .toList());
  }

  private Component findSignInDetailComponent(String componentId) {
    return streamDescendantComponents(userDialog)
        .filter(candidateComponent ->
            candidateComponent.getId().filter(componentId::equals).isPresent())
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ownAccountStartsWithEmptyNewPasswordFieldsAndClosesWithoutDiscardPrompt(String languageTag) {
    when(securityService.getCurrentUser())
        .thenReturn(Optional.of(UserEntity.builder().id(2L).build()));
    openUserDialog(languageTag, false);

    assertEquals(Autocomplete.NEW_PASSWORD, findNewPasswordField().getAutocomplete());
    assertEquals(Autocomplete.NEW_PASSWORD, findPasswordConfirmationField().getAutocomplete());
    assertTrue(findNewPasswordField().isEmpty());
    assertTrue(findPasswordConfirmationField().isEmpty());

    findCancelUserEditButton().click();

    assertFalse(userDialog.isOpened());
    assertTrue(findOpenUnsavedChangesDialog().isEmpty());
    verify(userService, never()).updateAdminUser(any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ownAccountStillRequiresDiscardingRealChangesAndAllowsRevertingThem(String languageTag) {
    when(securityService.getCurrentUser())
        .thenReturn(Optional.of(UserEntity.builder().id(2L).build()));
    openUserDialog(languageTag, false);
    TextField firstNameTextField =
        streamDescendantComponents(userDialog)
            .filter(TextField.class::isInstance)
            .map(TextField.class::cast)
            .findFirst()
            .orElseThrow();
    String savedFirstName = firstNameTextField.getValue();
    firstNameTextField.setValue("Changed first name");

    findCancelUserEditButton().click();

    Dialog unsavedChangesDialog = findOpenUnsavedChangesDialog().orElseThrow();
    assertTrue(userDialog.isOpened());
    findDialogFooterButton(unsavedChangesDialog, messageProperties.getUnsavedChangesKeepEditing())
        .click();
    assertEquals("Changed first name", firstNameTextField.getValue());

    firstNameTextField.setValue(savedFirstName);
    findCancelUserEditButton().click();

    assertFalse(userDialog.isOpened());
    assertTrue(findOpenUnsavedChangesDialog().isEmpty());
    verify(userService, never()).updateAdminUser(any(), any(), any());
  }

  private Button findCancelUserEditButton() {
    return findDialogFooterButton(userDialog, messageProperties.getAdminUsersCancel());
  }

  private Button findDialogFooterButton(Dialog sourceDialog, String buttonText) {
    return sourceDialog
        .getFooter()
        .getElement()
        .getChildren()
        .flatMap(childElement -> childElement.getComponent().stream())
        .flatMap(this::streamDescendantComponents)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(candidateButton -> candidateButton.getText().equals(buttonText))
        .findFirst()
        .orElseThrow();
  }

  private Optional<Dialog> findOpenUnsavedChangesDialog() {
    testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();

    return streamDescendantComponents(testUi)
        .filter(Dialog.class::isInstance)
        .map(Dialog.class::cast)
        .filter(Dialog::isOpened)
        .filter(candidateDialog ->
            candidateDialog.getId().filter("unsaved-changes-confirmation"::equals).isPresent())
        .findFirst();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void blankEditPasswordDoesNotRequireConfirmationAndPreservesTheExistingPassword(
      String languageTag) {
    openUserDialog(languageTag, false);

    assertFalse(findPasswordConfirmationField().isEnabled());
    assertFalse(findPasswordConfirmationField().isRequiredIndicatorVisible());
    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq(""));
    assertFalse(userDialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void typingDoesNotShowErrorsButBlurShowsRulesAndMismatchUntilCorrected(String languageTag) {
    openUserDialog(languageTag, false);
    findNewPasswordField().setValue("w");

    assertTrue(findPasswordConfirmationField().isEnabled());
    assertTrue(findPasswordConfirmationField().isRequiredIndicatorVisible());
    assertFalse(findNewPasswordField().isInvalid());
    assertFalse(findPasswordConfirmationField().isInvalid());
    assertFalse(findSaveUserButton().isEnabled());
    firePasswordBlurEvent(findNewPasswordField());

    assertTrue(findNewPasswordField().isInvalid());
    assertEquals(
        messageProperties.getAdminUsersPasswordValidationEdit(),
        findNewPasswordField().getErrorMessage());
    firePasswordFocusEvent(findNewPasswordField());

    assertFalse(findNewPasswordField().isInvalid());
    findNewPasswordField().setValue("ChangedPassword!");
    firePasswordBlurEvent(findNewPasswordField());
    findPasswordConfirmationField().setValue("DifferentPassword!");

    assertFalse(findPasswordConfirmationField().isInvalid());
    assertFalse(findSaveUserButton().isEnabled());
    firePasswordBlurEvent(findPasswordConfirmationField());

    assertTrue(findPasswordConfirmationField().isInvalid());
    assertEquals(
        messageProperties.getBaseValidationPasswordConfirmMessage(),
        findPasswordConfirmationField().getErrorMessage());
    firePasswordFocusEvent(findPasswordConfirmationField());

    assertFalse(findPasswordConfirmationField().isInvalid());
    findPasswordConfirmationField().setValue("ChangedPassword!");
    firePasswordBlurEvent(findPasswordConfirmationField());

    assertFalse(findPasswordConfirmationField().isInvalid());
    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq("ChangedPassword!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void clearingTheOptionalPasswordDiscardsConfirmationAndItsError(String languageTag) {
    openUserDialog(languageTag, false);
    findNewPasswordField().setValue("ChangedPassword!");
    findPasswordConfirmationField().setValue("Wrong!");
    firePasswordBlurEvent(findPasswordConfirmationField());

    assertTrue(findPasswordConfirmationField().isInvalid());
    findNewPasswordField().clear();

    assertEquals("", findPasswordConfirmationField().getValue());
    assertFalse(findPasswordConfirmationField().isEnabled());
    assertFalse(findPasswordConfirmationField().isInvalid());
    assertFalse(findPasswordConfirmationField().isRequiredIndicatorVisible());
    assertTrue(findSaveUserButton().isEnabled());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void createRequiresAValidPasswordAndMatchingConfirmation(String languageTag) {
    openUserDialog(languageTag, true);

    assertFalse(findSaveUserButton().isEnabled());
    findNewPasswordField().setValue("ValidPassword!");

    assertFalse(findSaveUserButton().isEnabled());
    findPasswordConfirmationField().setValue("validPassword!");

    assertFalse(findSaveUserButton().isEnabled());
    findPasswordConfirmationField().setValue("ValidPassword!");

    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();

    verify(userService)
        .createAdminUser(
            argThat(user -> Locale.forLanguageTag(languageTag).equals(user.getPreferredLocale())),
            eq("ValidPassword!"));
  }

  @Test
  void superadminRoleIsOnlyOfferedToASuperadmin() {
    openUserDialog("de", false);
    MultiSelectComboBox<RoleType> roleMultiSelectComboBox = findRoleMultiSelectComboBox();

    assertFalse(
        roleMultiSelectComboBox
            .getListDataView()
            .getItems()
            .toList()
            .contains(RoleType.ROLE_SUPERADMIN));
    assertTrue(
        roleMultiSelectComboBox
            .getListDataView()
            .getItems()
            .toList()
            .contains(RoleType.ROLE_ADMIN));
    when(userService.canManageSuperadmins()).thenReturn(true);
    roleMultiSelectComboBox.getDataProvider().refreshAll();

    assertTrue(
        roleMultiSelectComboBox
            .getListDataView()
            .getItems()
            .toList()
            .contains(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void roleSelectionRequiresOneRoleAndSurvivesLanguageChanges() {
    openUserDialog("de", false);
    MultiSelectComboBox<RoleType> roleMultiSelectComboBox = findRoleMultiSelectComboBox();
    roleMultiSelectComboBox.setValue(Set.of(RoleType.ROLE_USER, RoleType.ROLE_GUEST));

    assertEquals(
        messageProperties.getAdminUsersSelectedRoles(2), roleMultiSelectComboBox.getPlaceholder());
    localeReference.set(Locale.ENGLISH);
    userManagementSection.refreshTexts();

    assertEquals(
        Set.of(RoleType.ROLE_USER, RoleType.ROLE_GUEST), roleMultiSelectComboBox.getValue());
    assertEquals(
        messageProperties.getAdminUsersSelectedRoles(2), roleMultiSelectComboBox.getPlaceholder());
    assertEquals(
        messageProperties.getRoleGuest(),
        roleMultiSelectComboBox.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    roleMultiSelectComboBox.clear();

    assertFalse(findSaveUserButton().isEnabled());
    roleMultiSelectComboBox.setValue(Set.of(RoleType.ROLE_USER));

    assertTrue(findSaveUserButton().isEnabled());
  }

  @SuppressWarnings("unchecked")
  private MultiSelectComboBox<RoleType> findRoleMultiSelectComboBox() {
    return (MultiSelectComboBox<RoleType>)
        streamDescendantComponents(userDialog)
            .filter(MultiSelectComboBox.class::isInstance)
            .findFirst()
            .orElseThrow();
  }

  @Test
  void languageChangesPreserveValuesAndTranslateAnAlreadyDisplayedConfirmationError() {
    openUserDialog("de", false);
    findNewPasswordField().setValue("ValidPassword!");
    findPasswordConfirmationField().setValue("Different!");
    firePasswordBlurEvent(findPasswordConfirmationField());
    localeReference.set(Locale.ENGLISH);
    userManagementSection.refreshTexts();

    assertEquals("ValidPassword!", findNewPasswordField().getValue());
    assertEquals("Different!", findPasswordConfirmationField().getValue());
    assertEquals(
        messageProperties.getBaseValidationPasswordConfirmMessage(),
        findPasswordConfirmationField().getErrorMessage());
    assertEquals(
        messageProperties.getAccountConfirmPasswordPlaceholder(),
        findPasswordConfirmationField().getPlaceholder());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newRolesCanBeAssignedAndHaveTranslatedLabels(String languageTag) {
    when(userService.canManageSuperadmins()).thenReturn(true);
    openUserDialog(languageTag, false);
    @SuppressWarnings("unchecked")
    MultiSelectComboBox<RoleType> roleMultiSelectComboBox =
        (MultiSelectComboBox<RoleType>)
            streamDescendantComponents(userDialog)
                .filter(MultiSelectComboBox.class::isInstance)
                .findFirst()
                .orElseThrow();

    assertEquals(
        List.of(RoleType.values()), roleMultiSelectComboBox.getListDataView().getItems().toList());
    assertEquals(
        messageProperties.getRoleSuperadmin(),
        roleMultiSelectComboBox.getItemLabelGenerator().apply(RoleType.ROLE_SUPERADMIN));
    assertEquals(
        messageProperties.getRoleGuest(),
        roleMultiSelectComboBox.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    roleMultiSelectComboBox.setValue(Set.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_GUEST));
    findSaveUserButton().click();
    ArgumentCaptor<UserEntity> editedUserCaptor =
        org.mockito.ArgumentCaptor.forClass(UserEntity.class);

    verify(userService).updateAdminUser(editedUserCaptor.capture(), eq(3L), eq(""));
    assertEquals(
        Set.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_GUEST),
        editedUserCaptor.getValue().getRoleEntities().stream()
            .map(RoleEntity::getName)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "edit", "delete"})
  void nativeDismissalIsEnabledAndClosingDoesNotPersistOrDelete(String dialogActionName) {
    if (dialogActionName.equals("delete")) {
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
      userDialog =
          streamDescendantComponents(testUi)
              .filter(Dialog.class::isInstance)
              .map(Dialog.class::cast)
              .filter(Dialog::isOpened)
              .findFirst()
              .orElseThrow();
    } else {
      openUserDialog("de", dialogActionName.equals("create"));
    }
    assertTrue(userDialog.isCloseOnOutsideClick());
    assertTrue(userDialog.isCloseOnEsc());
    userDialog.setOpened(false);

    assertFalse(userDialog.isOpened());
    verify(userService, never()).save(any());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
    verify(userService, never()).deleteUser(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void simultaneousEditorsKeepTheirValidationAndSaveStateSeparate(String languageTag) {
    openUserDialog(languageTag, false);
    Dialog editingUserDialog = userDialog;
    findNewPasswordField().setValue("EditedPassword!");
    findPasswordConfirmationField().setValue("EditedPassword!");

    assertTrue(findSaveUserButton().isEnabled());

    openUserDialog(languageTag, true);
    Dialog creatingUserDialog = userDialog;

    assertNotSame(editingUserDialog, creatingUserDialog);
    findNewPasswordField().setValue("CreatedPassword!");
    findPasswordConfirmationField().setValue("DifferentPassword!");
    firePasswordBlurEvent(findPasswordConfirmationField());

    assertFalse(findSaveUserButton().isEnabled());

    userDialog = editingUserDialog;

    assertTrue(findSaveUserButton().isEnabled());
    assertFalse(findPasswordConfirmationField().isInvalid());
    findSaveUserButton().click();

    verify(userService).updateAdminUser(any(), eq(3L), eq("EditedPassword!"));
    assertFalse(editingUserDialog.isOpened());
    assertTrue(creatingUserDialog.isOpened());
    verify(userService, never()).createAdminUser(any(), any());

    userDialog = creatingUserDialog;

    assertEquals("CreatedPassword!", findNewPasswordField().getValue());
    assertTrue(findPasswordConfirmationField().isInvalid());
    findPasswordConfirmationField().setValue("CreatedPassword!");
    findSaveUserButton().click();

    verify(userService).createAdminUser(any(), eq("CreatedPassword!"));
    assertFalse(creatingUserDialog.isOpened());
  }

  private void openUserDialog(String languageTag, boolean createNewUser) {
    Dialog previouslyOpenedDialog = userDialog;
    localeReference.set(Locale.forLanguageTag(languageTag));
    UserEntity testUserEntity =
        UserEntity.builder()
            .id(createNewUser ? null : 2L)
            .version(createNewUser ? null : 3L)
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
    userDialog =
        streamDescendantComponents(testUi)
            .filter(Dialog.class::isInstance)
            .map(Dialog.class::cast)
            .filter(Dialog::isOpened)
            .filter(candidateDialog -> candidateDialog != previouslyOpenedDialog)
            .findFirst()
            .orElseThrow();
  }

  private PasswordField findNewPasswordField() {
    return findPasswordFields().get(0);
  }

  private PasswordField findPasswordConfirmationField() {
    return findPasswordFields().get(1);
  }

  private List<PasswordField> findPasswordFields() {
    return streamDescendantComponents(userDialog)
        .filter(PasswordField.class::isInstance)
        .map(PasswordField.class::cast)
        .toList();
  }

  private Button findSaveUserButton() {
    return findDialogFooterButton(userDialog, messageProperties.getAdminUsersSave());
  }

  private void firePasswordBlurEvent(PasswordField passwordField) {
    ComponentUtil.fireEvent(passwordField, new BlurNotifier.BlurEvent<>(passwordField, true));
  }

  private void firePasswordFocusEvent(PasswordField passwordField) {
    ComponentUtil.fireEvent(passwordField, new FocusNotifier.FocusEvent<>(passwordField, true));
  }

  private Stream<Component> streamDescendantComponents(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent),
        rootComponent.getChildren().flatMap(this::streamDescendantComponents));
  }
}
