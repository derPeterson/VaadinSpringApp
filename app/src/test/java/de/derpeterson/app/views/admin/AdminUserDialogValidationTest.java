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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
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
  private UI ui;
  private VaadinSession session;
  private UserService users;
  private AdminUserManagementSection section;
  private MessageProperties texts;
  private Dialog dialog;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.ENGLISH);

  @BeforeEach
  void setup() {
    ui = new UI();
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    users = mock(UserService.class);
    RoleService roles = mock(RoleService.class);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
    texts = new MessageProperties(new CustomI18NProvider());
    section = new AdminUserManagementSection(texts, users, roles, mock(SecurityService.class));
  }

  @AfterEach
  void cleanup() {
    if (dialog != null) {
      dialog.close();
    }
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    section = null;
    session = null;
    ui = null;
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void blankEditPasswordDoesNotRequireConfirmationAndPreservesTheExistingPassword(String language) {
    open(language, false);
    assertFalse(confirm().isEnabled());
    assertFalse(confirm().isRequiredIndicatorVisible());
    assertTrue(save().isEnabled());
    save().click();
    verify(users).updateAdminUser(any(), eq(3L), eq(""));
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
    assertEquals(texts.getAdminUsersPasswordValidationEdit(), password().getErrorMessage());
    focus(password());
    assertFalse(password().isInvalid());
    password().setValue("ChangedPassword!");
    blur(password());
    confirm().setValue("DifferentPassword!");
    assertFalse(confirm().isInvalid());
    assertFalse(save().isEnabled());
    blur(confirm());
    assertTrue(confirm().isInvalid());
    assertEquals(texts.getBaseValidationPasswordConfirmMessage(), confirm().getErrorMessage());
    focus(confirm());
    assertFalse(confirm().isInvalid());
    confirm().setValue("ChangedPassword!");
    blur(confirm());
    assertFalse(confirm().isInvalid());
    assertTrue(save().isEnabled());
    save().click();
    verify(users).updateAdminUser(any(), eq(3L), eq("ChangedPassword!"));
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
    verify(users)
        .createAdminUser(
            argThat(user -> Locale.forLanguageTag(language).equals(user.getPreferredLocale())),
            eq("ValidPassword!"));
  }

  @Test
  void superadminCheckboxIsOnlyEditableByASuperadmin() {
    open("de", false);
    @SuppressWarnings("unchecked")
    CheckboxGroup<RoleType> roles =
        (com.vaadin.flow.component.checkbox.CheckboxGroup<de.derpeterson.app.model.enums.RoleType>)
            descendants(dialog)
                .filter(com.vaadin.flow.component.checkbox.CheckboxGroup.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertFalse(
        roles
            .getItemEnabledProvider()
            .test(de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN));
    assertTrue(
        roles.getItemEnabledProvider().test(de.derpeterson.app.model.enums.RoleType.ROLE_ADMIN));
    when(users.canManageSuperadmins()).thenReturn(true);
    assertTrue(
        roles
            .getItemEnabledProvider()
            .test(de.derpeterson.app.model.enums.RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void languageChangesPreserveValuesAndTranslateAnAlreadyDisplayedConfirmationError() {
    open("de", false);
    password().setValue("ValidPassword!");
    confirm().setValue("Different!");
    blur(confirm());
    locale.set(Locale.ENGLISH);
    section.refreshTexts();
    assertEquals("ValidPassword!", password().getValue());
    assertEquals("Different!", confirm().getValue());
    assertEquals(texts.getBaseValidationPasswordConfirmMessage(), confirm().getErrorMessage());
    assertEquals(texts.getAccountConfirmPasswordPlaceholder(), confirm().getPlaceholder());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newRolesCanBeAssignedAndHaveTranslatedLabels(String language) {
    open(language, false);
    @SuppressWarnings("unchecked")
    CheckboxGroup<RoleType> roles =
        (CheckboxGroup<RoleType>)
            descendants(dialog).filter(CheckboxGroup.class::isInstance).findFirst().orElseThrow();
    assertEquals(List.of(RoleType.values()), roles.getListDataView().getItems().toList());
    assertEquals(
        texts.getRoleSuperadmin(), roles.getItemLabelGenerator().apply(RoleType.ROLE_SUPERADMIN));
    assertEquals(texts.getRoleGuest(), roles.getItemLabelGenerator().apply(RoleType.ROLE_GUEST));
    roles.setValue(Set.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_GUEST));
    save().click();
    ArgumentCaptor<UserEntity> edited = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
    verify(users).updateAdminUser(edited.capture(), eq(3L), eq(""));
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
      UserEntity target =
          UserEntity.builder()
              .id(2L)
              .version(3L)
              .firstName("Test")
              .lastName("User")
              .email("test@example.test")
              .build();
      when(users.canDeleteUser(target)).thenReturn(true);
      ReflectionTestUtils.invokeMethod(section, "deleteUser", target);
      ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
      dialog =
          descendants(ui)
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
    verify(users, never()).save(any());
    verify(users, never()).createAdminUser(any(), any());
    verify(users, never()).updateAdminUser(any(), any(), any());
    verify(users, never()).deleteUser(any());
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
    verify(users).updateAdminUser(any(), eq(3L), eq("EditedPassword!"));
    assertFalse(editing.isOpened());
    assertTrue(creating.isOpened());
    verify(users, never()).createAdminUser(any(), any());

    dialog = creating;
    assertEquals("CreatedPassword!", password().getValue());
    assertTrue(confirm().isInvalid());
    confirm().setValue("CreatedPassword!");
    save().click();
    verify(users).createAdminUser(any(), eq("CreatedPassword!"));
    assertFalse(creating.isOpened());
  }

  private void open(String language, boolean create) {
    Dialog previouslyOpened = dialog;
    locale.set(Locale.forLanguageTag(language));
    UserEntity user =
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
    ReflectionTestUtils.invokeMethod(section, "openDialog", user);
    ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    dialog =
        descendants(ui)
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
        .filter(button -> button.getText().equals(texts.getAdminUsersSave()))
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
