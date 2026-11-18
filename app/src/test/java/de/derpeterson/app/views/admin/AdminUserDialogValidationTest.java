package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ConfirmDialogTestActions.click;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
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
import de.derpeterson.app.service.AdminAccountMailService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.ExternalIdentityService.SignInSummary;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import java.io.IOException;
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
import org.junit.jupiter.params.provider.CsvSource;
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
  @CsvSource({"de,true", "de,false", "en,true", "en,false"})
  void verificationChoiceDefaultsToRequiredAndPassesTheCorrectExemption(
      String languageTag, boolean requireVerification) throws Exception {
    when(userService.canManageSuperadmins()).thenReturn(true);
    AdminAccountMailService adminAccountMailService = mock(AdminAccountMailService.class);
    userManagementSection.configureAccountMailActions(adminAccountMailService);
    openUserDialog(languageTag, true);
    Switch verificationSwitch =
        streamDescendantComponents(userDialog)
            .filter(component ->
                component.getId().orElse("").equals("admin-email-verification-required"))
            .map(Switch.class::cast)
            .findFirst()
            .orElseThrow();

    assertTrue(verificationSwitch.getValue());
    assertTrue(
        streamDescendantComponents(verificationSwitch)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span -> span.getText().equals(messageProperties.getAdminEmailVerificationHint())));
    verificationSwitch.setValue(requireVerification);
    findNewPasswordField().setValue("CreatedPassword!");
    findPasswordConfirmationField().setValue("CreatedPassword!");

    findSaveUserButton().click();

    verify(adminAccountMailService)
        .createUser(any(), eq("CreatedPassword!"), eq(!requireVerification));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void developmentAccountOptionShowsTranslatedCaptionAndHintWhenOpened(String languageTag) {
    when(userService.canManageSuperadmins()).thenReturn(true);
    openUserDialog(languageTag, true);

    Switch developmentAccountSwitch = (Switch) findUserDialogComponent("admin-development-account");
    List<Span> developmentAccountLabelSpans =
        streamDescendantComponents(developmentAccountSwitch)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .toList();

    assertFalse(messageProperties.getDevelopmentAccountLabel().isBlank());
    assertFalse(messageProperties.getDevelopmentAccountHint().isBlank());
    assertEquals(
        List.of(
            messageProperties.getDevelopmentAccountLabel(),
            messageProperties.getDevelopmentAccountHint()),
        developmentAccountLabelSpans.stream().map(Span::getText).toList());
    assertTrue(developmentAccountSwitch.hasClassName("admin-user-option-switch"));
    assertTrue(developmentAccountLabelSpans.get(1).hasClassName("admin-user-option-hint"));
  }

  @ParameterizedTest
  @CsvSource({"de,false", "de,true", "en,false", "en,true"})
  void accountTypeIsLabeledForBothStandardAndDevelopmentAccounts(
      String languageTag, boolean developmentAccount) {
    ExternalIdentityService externalIdentityService = mock(ExternalIdentityService.class);
    when(externalIdentityService.adminSignInSummary(2L))
        .thenReturn(
            Optional.of(
                new SignInSummary(
                    RegistrationChannel.APPLICATION_START, Set.of(SignInMethod.PORTAL))));
    userManagementSection.configureSignInDetails(externalIdentityService);
    openUserDialog(languageTag, false, developmentAccount);

    Badge accountTypeBadge = (Badge) findUserDialogComponent("admin-user-account-type");
    Component accountTypeDetailComponent = accountTypeBadge.getParent().orElseThrow();

    assertTrue(accountTypeBadge.isVisible());
    assertEquals(
        developmentAccount
            ? messageProperties.getDevelopmentAccountLabel()
            : messageProperties.getStandardAccountLabel(),
        accountTypeBadge.getText());
    assertEquals(
        Boolean.toString(developmentAccount),
        accountTypeBadge.getElement().getAttribute("data-development-account"));
    assertEquals(
        messageProperties.getAccountTypeLabel(),
        ((Span) accountTypeDetailComponent.getChildren().findFirst().orElseThrow()).getText());

    Component statusRowComponent = accountTypeDetailComponent.getParent().orElseThrow();
    assertEquals(2, statusRowComponent.getChildren().count());
    assertEquals(accountTypeDetailComponent, statusRowComponent.getChildren().toList().get(1));
    assertTrue(
        streamDescendantComponents(statusRowComponent.getChildren().findFirst().orElseThrow())
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span -> span.getText().equals(messageProperties.getEmailVerificationLabel())));
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

    Badge registrationChannelBadge = (Badge) findUserDialogComponent("admin-user-registered-via");
    Component availableSignInMethodsComponent =
        findUserDialogComponent("admin-user-sign-in-methods");

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

  private Component findUserDialogComponent(String componentId) {
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

    ConfirmDialog unsavedChangesDialog = findOpenUnsavedChangesDialog().orElseThrow();
    assertTrue(userDialog.isOpened());
    click(
        findDialogFooterButton(
            unsavedChangesDialog, messageProperties.getUnsavedChangesKeepEditing()));
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

  private Button findDialogFooterButton(Component sourceDialog, String buttonText) {
    Stream<Component> footerComponents =
        sourceDialog instanceof Dialog dialog
            ? dialog
                .getFooter()
                .getElement()
                .getChildren()
                .flatMap(childElement -> childElement.getComponent().stream())
            : sourceDialog
                .getChildren()
                .filter(component ->
                    "cancel-button".equals(component.getElement().getAttribute("slot"))
                        || "confirm-button".equals(component.getElement().getAttribute("slot")));
    return footerComponents
        .flatMap(this::streamDescendantComponents)
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(candidateButton -> candidateButton.getText().equals(buttonText))
        .findFirst()
        .orElseThrow();
  }

  private Optional<ConfirmDialog> findOpenUnsavedChangesDialog() {
    testUi.getInternals().getStateTree().runExecutionsBeforeClientResponse();

    return streamDescendantComponents(testUi)
        .filter(ConfirmDialog.class::isInstance)
        .map(ConfirmDialog.class::cast)
        .filter(ConfirmDialog::isOpened)
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
  void savingAnEditUsesTheTransactionalMailWorkflow(String languageTag) throws IOException {
    AdminAccountMailService adminAccountMailService = mock(AdminAccountMailService.class);
    userManagementSection.configureAccountMailActions(adminAccountMailService);
    openUserDialog(languageTag, false);

    findSaveUserButton().click();

    verify(adminAccountMailService).updateUser(any(), eq(3L), eq(""));
    verify(userService, never()).updateAdminUser(any(), any(), any());
    assertFalse(userDialog.isOpened());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void passwordRulesAndMismatchAreReportedOnlyOnSaveWithoutInlineErrors(String languageTag) {
    openUserDialog(languageTag, false);
    findNewPasswordField().setValue("w");
    findPasswordConfirmationField().setValue("w");

    assertTrue(findPasswordConfirmationField().isEnabled());
    assertTrue(findPasswordConfirmationField().isRequiredIndicatorVisible());
    assertFalse(findNewPasswordField().isInvalid());
    assertFalse(findPasswordConfirmationField().isInvalid());
    assertTrue(findSaveUserButton().isEnabled());
    firePasswordBlurEvent(findNewPasswordField());

    assertFalse(findNewPasswordField().isInvalid());
    findSaveUserButton().click();

    assertTrue(findNewPasswordField().isInvalid());
    assertNull(findNewPasswordField().getErrorMessage());
    assertNotification(messageProperties.getAdminUsersPasswordValidationCreate());
    assertTrue(messageProperties.getAdminUsersPasswordValidationCreate().contains("\n"));
    findNewPasswordField().setValue("ChangedPassword!");

    assertFalse(findNewPasswordField().isInvalid());
    firePasswordBlurEvent(findNewPasswordField());
    findPasswordConfirmationField().setValue("DifferentPassword!");

    assertFalse(findPasswordConfirmationField().isInvalid());
    assertTrue(findSaveUserButton().isEnabled());
    firePasswordBlurEvent(findPasswordConfirmationField());

    assertFalse(findPasswordConfirmationField().isInvalid());
    findSaveUserButton().click();

    assertTrue(findPasswordConfirmationField().isInvalid());
    assertNull(findPasswordConfirmationField().getErrorMessage());
    assertNotification(messageProperties.getBaseValidationPasswordConfirmMessage());
    findPasswordConfirmationField().setValue("ChangedPassword!");

    assertFalse(findPasswordConfirmationField().isInvalid());
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
    findSaveUserButton().click();

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

    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();
    assertTrue(findNewPasswordField().isInvalid());
    assertNotification(messageProperties.getBaseValidationRequiredMessage());
    findNewPasswordField().setValue("ValidPassword!");

    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();
    assertTrue(findPasswordConfirmationField().isInvalid());
    findPasswordConfirmationField().setValue("validPassword!");

    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();
    assertNotification(messageProperties.getBaseValidationPasswordConfirmMessage());
    verify(userService, never()).createAdminUser(any(), any());
    findPasswordConfirmationField().setValue("ValidPassword!");

    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();

    verify(userService)
        .createAdminUser(
            argThat(user -> Locale.forLanguageTag(languageTag).equals(user.getPreferredLocale())),
            eq("ValidPassword!"));
  }

  @ParameterizedTest
  @CsvSource({"de,true", "de,false", "en,true", "en,false"})
  void requiredFieldsAndEmailFormatAreCheckedOnSaveAndClearTheirOwnMarkers(
      String languageTag, boolean create) {
    openUserDialog(languageTag, create);
    if (create) {
      findNewPasswordField().setValue("ValidPassword!");
      findPasswordConfirmationField().setValue("ValidPassword!");
    }
    TextField firstNameField = findTextField(messageProperties.getAdminUsersFieldFirstName());
    TextField lastNameField = findTextField(messageProperties.getAdminUsersFieldLastName());
    EmailField emailField =
        streamDescendantComponents(userDialog)
            .filter(EmailField.class::isInstance)
            .map(EmailField.class::cast)
            .findFirst()
            .orElseThrow();
    firstNameField.clear();
    lastNameField.clear();
    emailField.clear();

    assertFalse(firstNameField.isInvalid());
    assertFalse(lastNameField.isInvalid());
    assertFalse(emailField.isInvalid());
    assertTrue(findSaveUserButton().isEnabled());
    findSaveUserButton().click();

    assertTrue(firstNameField.isInvalid());
    assertTrue(lastNameField.isInvalid());
    assertTrue(emailField.isInvalid());
    assertNull(firstNameField.getErrorMessage());
    assertNull(lastNameField.getErrorMessage());
    assertNull(emailField.getErrorMessage());
    assertNotification(messageProperties.getBaseValidationRequiredMessage());
    firstNameField.setValue("Corrected");
    assertFalse(firstNameField.isInvalid());
    assertTrue(lastNameField.isInvalid());
    lastNameField.setValue("User");
    emailField.setValue("invalid");
    assertFalse(emailField.isInvalid());
    findSaveUserButton().click();

    assertTrue(emailField.isInvalid());
    assertNotification(messageProperties.getBaseValidationEmailInvalidMessage());
    verify(userService, never()).emailExistsForOtherUser(any(), any());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
  }

  @ParameterizedTest
  @CsvSource({"de,true", "de,false", "en,true", "en,false"})
  void unparseableBirthdayBlocksSavingAndStillProtectsUnsavedInput(
      String languageTag, boolean create) {
    openUserDialog(languageTag, create, false, null);
    if (create) {
      findNewPasswordField().setValue("ValidPassword!");
      findPasswordConfirmationField().setValue("ValidPassword!");
    }
    DatePicker birthDatePicker =
        streamDescendantComponents(userDialog)
            .filter(DatePicker.class::isInstance)
            .map(DatePicker.class::cast)
            .findFirst()
            .orElseThrow();
    birthDatePicker.clear();
    birthDatePicker.getElement().setProperty("_inputElementValue", "31022026");
    ReflectionTestUtils.invokeMethod(birthDatePicker, "setModelValue", null, true);

    assertFalse(birthDatePicker.isInvalid());
    findSaveUserButton().click();
    assertTrue(birthDatePicker.isInvalid());
    assertNull(birthDatePicker.getErrorMessage());
    assertNotification(messageProperties.getBaseValidationBirthDatePastMessage());
    verify(userService, never()).createAdminUser(any(), any());
    verify(userService, never()).updateAdminUser(any(), any(), any());
    findCancelUserEditButton().click();
    assertTrue(findOpenUnsavedChangesDialog().isPresent());
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

    assertTrue(findSaveUserButton().isEnabled());
    assertFalse(roleMultiSelectComboBox.isInvalid());
    findSaveUserButton().click();
    assertTrue(roleMultiSelectComboBox.isInvalid());
    assertNull(roleMultiSelectComboBox.getErrorMessage());
    assertNotification(messageProperties.getAdminUsersRolesHelper());
    roleMultiSelectComboBox.setValue(Set.of(RoleType.ROLE_USER));

    assertFalse(roleMultiSelectComboBox.isInvalid());
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
  void languageChangesPreserveValuesAndTranslateTheNotificationWithoutInlineErrors() {
    openUserDialog("de", false);
    findNewPasswordField().setValue("ValidPassword!");
    findPasswordConfirmationField().setValue("Different!");
    findSaveUserButton().click();
    localeReference.set(Locale.ENGLISH);
    userManagementSection.refreshTexts();
    NotificationHelper.getInstance().updateText();

    assertEquals("ValidPassword!", findNewPasswordField().getValue());
    assertEquals("Different!", findPasswordConfirmationField().getValue());
    assertNotification(messageProperties.getBaseValidationPasswordConfirmMessage());
    assertNull(findPasswordConfirmationField().getErrorMessage());
    assertTrue(findPasswordConfirmationField().isInvalid());
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
      ConfirmDialog deletionDialog =
          streamDescendantComponents(testUi)
              .filter(ConfirmDialog.class::isInstance)
              .map(ConfirmDialog.class::cast)
              .filter(ConfirmDialog::isOpened)
              .findFirst()
              .orElseThrow();
      assertTrue(deletionDialog.isCloseOnEsc());
      ComponentUtil.fireEvent(deletionDialog, new ConfirmDialog.CancelEvent(deletionDialog, true));
      assertFalse(deletionDialog.isOpened());
      ComponentUtil.fireEvent(deletionDialog, new ConfirmDialog.ConfirmEvent(deletionDialog, true));
    } else {
      openUserDialog("de", dialogActionName.equals("create"));
      assertTrue(userDialog.isCloseOnOutsideClick());
      assertTrue(userDialog.isCloseOnEsc());
      userDialog.setOpened(false);
      assertFalse(userDialog.isOpened());
    }
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
    findSaveUserButton().click();

    assertTrue(findSaveUserButton().isEnabled());
    assertTrue(findPasswordConfirmationField().isInvalid());

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
    openUserDialog(languageTag, createNewUser, false);
  }

  private void openUserDialog(
      String languageTag, boolean createNewUser, boolean developmentAccount) {
    openUserDialog(languageTag, createNewUser, developmentAccount, LocalDate.of(1990, 1, 1));
  }

  private void openUserDialog(
      String languageTag, boolean createNewUser, boolean developmentAccount, LocalDate birthDate) {
    Dialog previouslyOpenedDialog = userDialog;
    localeReference.set(Locale.forLanguageTag(languageTag));
    UserEntity testUserEntity =
        UserEntity.builder()
            .id(createNewUser ? null : 2L)
            .version(createNewUser ? null : 3L)
            .firstName("Test")
            .lastName("User")
            .email("test@example.test")
            .birthDate(birthDate)
            .gender(Gender.OTHER)
            .enabled(true)
            .developmentAccount(developmentAccount)
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

  private TextField findTextField(String label) {
    return streamDescendantComponents(userDialog)
        .filter(TextField.class::isInstance)
        .map(TextField.class::cast)
        .filter(field -> field.getLabel().equals(label))
        .findFirst()
        .orElseThrow();
  }

  private void firePasswordBlurEvent(PasswordField passwordField) {
    ComponentUtil.fireEvent(passwordField, new BlurNotifier.BlurEvent<>(passwordField, true));
  }

  private void assertNotification(String expectedMessage) {
    assertEquals(
        expectedMessage,
        ((Paragraph) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText"))
            .getText());
    assertEquals(
        messageProperties.getBaseFailedTitle(),
        ((Span) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "titleText"))
            .getText());
  }

  private Stream<Component> streamDescendantComponents(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent),
        rootComponent.getChildren().flatMap(this::streamDescendantComponents));
  }
}
