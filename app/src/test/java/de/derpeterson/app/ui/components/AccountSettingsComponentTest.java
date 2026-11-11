package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;

class AccountSettingsComponentTest {
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider customI18NProvider = new CustomI18NProvider();
  private UI testUi;
  private UserEntity testUserEntity;
  private SerializableFunction<Changes, Profile> saveProfileFunction;
  private AccountSettingsComponent accountSettingsComponent;

  @BeforeEach
  void createAccountSettingsEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    saveProfileFunction = mock(SerializableFunction.class);
    testUserEntity =
        UserEntity.builder()
            .version(3L)
            .gender(Gender.OTHER)
            .firstName("Alex")
            .lastName("Weber")
            .email("own-account@example.test")
            .birthDate(LocalDate.of(1997, 11, 5))
            .enabled(true)
            .preferredLocale(Locale.GERMAN)
            .build();
    accountSettingsComponent =
        new AccountSettingsComponent(
            new MessageProperties(customI18NProvider), testUserEntity, saveProfileFunction, 7);
  }

  @AfterEach
  void clearAccountSettingsEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Stream<Component> streamComponentTree(Component candidateComponent) {
    if (candidateComponent instanceof TabSheet securityTabSheet) {
      return Stream.concat(
          Stream.of(securityTabSheet),
          IntStream.range(0, securityTabSheet.getTabCount())
              .mapToObj(securityTabSheet::getTabAt)
              .flatMap(securityTab ->
                  Stream.concat(
                      streamComponentTree(securityTab),
                      streamComponentTree(securityTabSheet.getComponent(securityTab)))));
    }

    Stream<Component> childComponents = candidateComponent.getChildren();
    if (candidateComponent instanceof Dialog dialog) {
      // Native Dialog-Footer gehören nicht zu Dialog.getChildren().
      childComponents =
          Stream.concat(
              childComponents,
              dialog
                  .getFooter()
                  .getElement()
                  .getChildren()
                  .flatMap(element -> element.getComponent().stream()));
    }

    return Stream.concat(
        Stream.of(candidateComponent), childComponents.flatMap(this::streamComponentTree));
  }

  private void selectAccountSection(AccountSection accountSection) {
    PageNavigationComponent accountPageNavigationComponent =
        (PageNavigationComponent) findAccountComponentById("account-navigation");
    accountPageNavigationComponent.activate(
        (SideNavItem)
            findAccountComponentById(
                "account-tab-" + accountSection.name().toLowerCase(Locale.ROOT)));
  }

  private Component findAccountComponentById(String componentId) {
    return streamComponentTree(accountSettingsComponent)
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(componentId))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @CsvSource({
    "de,de,Deutsch,Englisch",
    "en,de,German,English",
    "de,en,Deutsch,Englisch",
    "en,en,German,English"
  })
  @SuppressWarnings("unchecked")
  void languageNamesFollowTheInterfaceWithoutChangingThePreference(
      String languageTag,
      String preferredLanguageTag,
      String germanLanguageLabel,
      String englishLanguageLabel) {
    ComboBox<Locale> languageComboBox =
        (ComboBox<Locale>) findAccountComponentById("account-language");
    Locale preferredLocale = Locale.forLanguageTag(preferredLanguageTag);
    languageComboBox.setValue(preferredLocale);
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSettingsComponent.refreshTexts();

    assertEquals(
        germanLanguageLabel, languageComboBox.getItemLabelGenerator().apply(Locale.GERMAN));
    assertEquals(
        englishLanguageLabel, languageComboBox.getItemLabelGenerator().apply(Locale.ENGLISH));
    JsonNode selectedLanguageJsonNode =
        assertInstanceOf(
            JsonNode.class, languageComboBox.getElement().getPropertyRaw("selectedItem"));

    assertEquals(
        preferredLanguageTag.equals("de") ? germanLanguageLabel : englishLanguageLabel,
        selectedLanguageJsonNode.path("label").asText());
    assertEquals(preferredLocale, languageComboBox.getValue());
    verifyNoInteractions(saveProfileFunction);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sessionsShowConfiguredLimitWithoutPresentingSampleCountAsReal(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get());

    assertEquals(
        messageProperties.getAccountSettingsSessionsLimit(7),
        ((Badge) findAccountComponentById("account-sessions-limit")).getText());
    String sessionPanelText =
        findAccountComponentById("account-panel-sessions").getElement().getTextRecursively();

    assertTrue(sessionPanelText.contains(messageProperties.getPortalSessionActiveCount(0)));
    assertTrue(sessionPanelText.contains(messageProperties.getAccountSettingsSessionsActivity()));
    verifyNoInteractions(saveProfileFunction);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void securityHeadingAndActionNamesRefreshWithoutChangingTheDraft(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    selectAccountSection(AccountSection.SECURITY);
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get());
    String securityHeadingText =
        streamComponentTree(accountSettingsComponent)
            .filter(CosmosSectionHeaderComponent.class::isInstance)
            .findFirst()
            .orElseThrow()
            .getElement()
            .getTextRecursively();

    assertTrue(
        securityHeadingText.contains(messageProperties.getAccountSettingsSecurityIntroduction()));
    assertFalse(
        securityHeadingText.contains(messageProperties.getAccountSettingsSecurityDescription()));
    Button changeEmailButton = (Button) findAccountComponentById("account-email-edit-security");

    assertEquals(messageProperties.getAccountSettingsChangeAction(), changeEmailButton.getText());
    assertEquals(
        messageProperties.getAccountSettingsChangeEmail(),
        changeEmailButton.getAriaLabel().orElseThrow());
    TabSheet securityTabSheet = (TabSheet) findAccountComponentById("account-security-tabs");

    assertEquals(
        findAccountComponentById("account-security-tab-portal"), securityTabSheet.getSelectedTab());
    assertEquals(
        messageProperties.getAccountSecurityPortalAccess(),
        findAccountComponentById("account-security-tab-portal").getElement().getTextRecursively());

    securityTabSheet.setSelectedTab(
        (Tab) findAccountComponentById("account-security-tab-providers"));
    accountSettingsComponent.refreshTexts();

    assertEquals(
        findAccountComponentById("account-security-tab-providers"),
        securityTabSheet.getSelectedTab());
    assertEquals(
        messageProperties.getProviderSettingsPanelTitle(),
        findAccountComponentById("account-security-tab-providers")
            .getElement()
            .getTextRecursively());
    assertEquals("Alex", ((TextField) findAccountComponentById("account-first-name")).getValue());
    verifyNoInteractions(saveProfileFunction);
  }

  @Test
  void sideNavigationPreservesProfileDraftAndSavedAccount() {
    TextField firstNameTextField = (TextField) findAccountComponentById("account-first-name");
    firstNameTextField.setValue("Draft");

    for (AccountSection accountSection :
        List.of(AccountSection.SECURITY, AccountSection.SESSIONS, AccountSection.PROFILE)) {
      selectAccountSection(accountSection);

      assertEquals(
          accountSection == AccountSection.PROFILE,
          findAccountComponentById("account-panel-profile").isVisible());
      assertEquals(
          accountSection == AccountSection.SECURITY,
          findAccountComponentById("account-panel-security").isVisible());
      assertEquals(
          accountSection == AccountSection.SESSIONS,
          findAccountComponentById("account-panel-sessions").isVisible());
    }

    ((ComboBox<?>) findAccountComponentById("account-language")).clear();

    assertEquals("Draft", firstNameTextField.getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    assertEquals(Locale.GERMAN, testUserEntity.getPreferredLocale());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    assertEquals(LocalDate.of(1997, 11, 5), testUserEntity.getBirthDate());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageRefreshKeepsSelectedTabAndInputsAndTranslatesSessions(String languageTag) {
    ((TextField) findAccountComponentById("account-first-name")).setValue("Unsaved");
    selectAccountSection(AccountSection.SESSIONS);
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get());

    assertTrue(
        accountSettingsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsTitle()));
    assertTrue(
        findAccountComponentById("account-tab-sessions")
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsTab()));
    assertTrue(
        findAccountComponentById("account-panel-sessions")
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsActivity()));
    assertEquals(AccountSection.SESSIONS, accountSettingsComponent.getSelectedSection());
    assertEquals(
        "Unsaved", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  @Test
  void regionalSavedLanguageMapsToASupportedOptionWithoutChangingTheUser() {
    testUserEntity.setPreferredLocale(Locale.GERMANY);
    accountSettingsComponent =
        new AccountSettingsComponent(
            new MessageProperties(customI18NProvider), testUserEntity, saveProfileFunction, 7);

    assertEquals(
        Locale.GERMAN, ((ComboBox<?>) findAccountComponentById("account-language")).getValue());
    assertEquals(Locale.GERMANY, testUserEntity.getPreferredLocale());
  }

  @Test
  void profileImagePreviewDoesNotSaveOrChangeProfileDrafts() {
    Button editProfilePictureButton =
        streamComponentTree(accountSettingsComponent)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(candidateButton ->
                candidateButton
                    .getText()
                    .equals(
                        MessageProperties.forLocale(customI18NProvider, activeLocaleReference.get())
                            .getAccountSettingsEditPicture()))
            .findFirst()
            .orElseThrow();

    assertTrue(editProfilePictureButton.isEnabled());
    ((TextField) findAccountComponentById("account-first-name")).setValue("Unsaved draft");
    editProfilePictureButton.click();

    verifyNoInteractions(saveProfileFunction);
    assertEquals(
        "Unsaved draft", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    assertEquals(Locale.GERMAN, testUserEntity.getPreferredLocale());
  }

  private void clickSaveProfileButton() {
    ((Button) findAccountComponentById("account-save")).click();
  }

  private void assertProfileSaveNotification(
      String expectedMessageText, boolean isErrorNotification) {
    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    Supplier<String> notificationMessageSupplier =
        (Supplier<String>) ReflectionTestUtils.getField(notificationHelper, "messageSupplier");

    assertEquals(expectedMessageText, notificationMessageSupplier.get());
    Notification notification =
        (Notification) ReflectionTestUtils.getField(notificationHelper, "currentNotification");

    assertTrue(notification.isOpened());
    Span titleSpan = (Span) ReflectionTestUtils.getField(notificationHelper, "titleText");

    assertTrue(
        titleSpan.hasClassName(
            isErrorNotification ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.SUCCESS));
    assertTrue(
        streamComponentTree(notification)
            .anyMatch(candidateComponent ->
                candidateComponent.getElement().getAttribute("src") != null
                    && candidateComponent
                        .getElement()
                        .getAttribute("src")
                        .equals(
                            isErrorNotification
                                ? "icons/lucide/triangle-alert.svg"
                                : "icons/lucide/circle-check.svg")));
  }

  // Profilvalidierung und Speichern
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requiredFieldsAreMarkedOnlyOnSaveAndNothingIsWritten(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountSettingsComponent.refreshTexts();
    TextField firstNameTextField = (TextField) findAccountComponentById("account-first-name");
    TextField lastNameTextField = (TextField) findAccountComponentById("account-last-name");
    DatePicker birthDatePicker = (DatePicker) findAccountComponentById("account-birth-date");
    ComboBox<?> languageComboBox = (ComboBox<?>) findAccountComponentById("account-language");
    ComboBox<?> genderComboBox = (ComboBox<?>) findAccountComponentById("account-gender");

    firstNameTextField.setValue("   ");
    lastNameTextField.clear();
    birthDatePicker.clear();
    languageComboBox.clear();
    genderComboBox.clear();

    assertFalse(firstNameTextField.isInvalid());

    clickSaveProfileButton();

    assertTrue(firstNameTextField.isInvalid());
    assertTrue(lastNameTextField.isInvalid());
    assertFalse(birthDatePicker.isInvalid());
    assertTrue(languageComboBox.isInvalid());
    assertFalse(genderComboBox.isInvalid());
    verifyNoInteractions(saveProfileFunction);
    assertProfileSaveNotification(
        new MessageProperties(customI18NProvider).getBaseValidationRequiredMessage(), true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void todayAndFutureBirthDatesAreRejectedAndCorrectionCanBeSaved(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    DatePicker birthDatePicker = (DatePicker) findAccountComponentById("account-birth-date");
    for (LocalDate birthDate : new LocalDate[] {LocalDate.now(), LocalDate.now().plusDays(1)}) {
      birthDatePicker.setValue(birthDate);
      clickSaveProfileButton();

      assertTrue(birthDatePicker.isInvalid());
      assertProfileSaveNotification(
          new MessageProperties(customI18NProvider).getBaseValidationBirthDatePastMessage(), true);
    }
    verifyNoInteractions(saveProfileFunction);

    LocalDate birthDate = LocalDate.of(1990, 2, 1);
    birthDatePicker.setValue(birthDate);
    when(saveProfileFunction.apply(any()))
        .thenReturn(new Profile(4L, "Alex", "Weber", birthDate, Locale.GERMAN, Gender.OTHER));
    clickSaveProfileButton();

    assertFalse(birthDatePicker.isInvalid());
    verify(saveProfileFunction).apply(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void successUsesReturnedDataAndNewVersionForTheNextSave(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    ((TextField) findAccountComponentById("account-first-name")).setValue(" New ");
    LocalDate birthDate = LocalDate.of(1992, 3, 4);
    ((DatePicker) findAccountComponentById("account-birth-date")).setValue(birthDate);
    when(saveProfileFunction.apply(any()))
        .thenReturn(new Profile(4L, "New", "Weber", birthDate, Locale.ENGLISH, Gender.OTHER));
    clickSaveProfileButton();

    verify(saveProfileFunction)
        .apply(new Changes(3L, " New ", "Weber", birthDate, Locale.GERMAN, Gender.OTHER));
    assertTrue(accountSettingsComponent.getElement().getTextRecursively().contains("New Weber"));
    assertEquals("New", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals(
        Locale.ENGLISH, ((ComboBox<?>) findAccountComponentById("account-language")).getValue());
    assertProfileSaveNotification(
        new MessageProperties(customI18NProvider).getAccountSettingsSaveSuccess(), false);
    clickSaveProfileButton();

    verify(saveProfileFunction)
        .apply(new Changes(4L, "New", "Weber", birthDate, Locale.ENGLISH, Gender.OTHER));
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void failedAndConflictingSavesKeepDraftsAndShowTranslatedErrors(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    ((TextField) findAccountComponentById("account-first-name")).setValue("Keep draft");
    when(saveProfileFunction.apply(any()))
        .thenThrow(new OptimisticLockingFailureException("changed"));
    clickSaveProfileButton();
    assertProfileSaveNotification(
        new MessageProperties(customI18NProvider).getBaseUserUpdateConflict(), true);

    doThrow(new IllegalStateException("Unavailable")).when(saveProfileFunction).apply(any());
    clickSaveProfileButton();
    assertProfileSaveNotification(
        new MessageProperties(customI18NProvider).getAccountSettingsSaveFailed(), true);

    assertEquals(
        "Keep draft", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  // E-Mail-Anzeige und Sicherheitsdialoge
  @ParameterizedTest
  @CsvSource({"de,true", "de,false", "en,true", "en,false"})
  void passwordStatusReflectsTheLocalPasswordAndUsesTheCurrentLanguage(
      String languageTag, boolean hasPortalPassword) {
    testUserEntity.setPassword(hasPortalPassword ? "encoded-password" : null);
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    MessageProperties messageProperties = new MessageProperties(customI18NProvider);
    accountSettingsComponent =
        new AccountSettingsComponent(messageProperties, testUserEntity, saveProfileFunction, 7);

    Badge passwordStatusBadge = (Badge) findAccountComponentById("account-password-status");
    Button passwordActionButton =
        (Button) findAccountComponentById("account-password-edit-security");

    assertEquals(
        hasPortalPassword
            ? messageProperties.getAccountPasswordConfigured()
            : messageProperties.getAccountPasswordNotConfigured(),
        passwordStatusBadge.getText());
    assertEquals(hasPortalPassword, passwordStatusBadge.getThemeNames().contains("success"));
    assertEquals(
        hasPortalPassword
            ? messageProperties.getAccountSettingsChangeAction()
            : messageProperties.getAccountPasswordSetupAction(),
        passwordActionButton.getText());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void passwordStatusChangesOnlyAfterSuccessfulPasswordSetup(boolean setupSucceeds) {
    accountSettingsComponent.configureSecurityActions(
        passwordChange -> {
          if (!setupSucceeds) {
            throw new IllegalStateException("Isolated password save failure");
          }
          return new PasswordChanged(3L, 4L);
        },
        emailChange -> {});
    AccountPasswordChangeDialog passwordChangeDialog =
        (AccountPasswordChangeDialog) findAccountComponentById("account-password-change-dialog");
    passwordChangeDialog.setCurrentPasswordRequired(() -> false);
    ((Button) findAccountComponentById("account-password-edit-security")).click();
    ((PasswordField) findAccountComponentById("account-password-new")).setValue("NewPassword!");
    ((PasswordField) findAccountComponentById("account-password-confirm")).setValue("NewPassword!");

    ((Button) findAccountComponentById("account-password-save")).click();

    MessageProperties messageProperties = new MessageProperties(customI18NProvider);
    assertEquals(
        setupSucceeds
            ? messageProperties.getAccountPasswordConfigured()
            : messageProperties.getAccountPasswordNotConfigured(),
        ((Badge) findAccountComponentById("account-password-status")).getText());
    assertEquals(
        setupSucceeds
            ? messageProperties.getAccountSettingsChangeAction()
            : messageProperties.getAccountPasswordSetupAction(),
        ((Button) findAccountComponentById("account-password-edit-security")).getText());
  }

  @Test
  void profileEmailIsReadOnlyAndHasNoEditAction() {
    EmailField accountEmailField = (EmailField) findAccountComponentById("account-email");

    assertTrue(accountEmailField.isEnabled());
    assertTrue(accountEmailField.isReadOnly());
    assertEquals(testUserEntity.getEmail(), accountEmailField.getValue());
    assertTrue(
        accountEmailField.getHelperText() == null || accountEmailField.getHelperText().isBlank());
    assertFalse(
        streamComponentTree(accountSettingsComponent)
            .anyMatch(candidateComponent ->
                candidateComponent.getId().orElse("").equals("account-email-edit-profile")));
    assertFalse(
        streamComponentTree(findAccountComponentById("account-panel-security"))
            .anyMatch(candidateComponent ->
                candidateComponent.getElement().getClassList().contains("account-info-text")));
  }

  @Test
  void securityPasswordButtonOpensOnlyItsDialogAndPreservesProfileDraft() {
    ((TextField) findAccountComponentById("account-first-name")).setValue("Unsaved");
    AccountPasswordChangeDialog passwordChangeDialog =
        (AccountPasswordChangeDialog) findAccountComponentById("account-password-change-dialog");
    AccountEmailChangeDialog emailChangeDialog =
        (AccountEmailChangeDialog) findAccountComponentById("account-email-change-dialog");
    ((Button) findAccountComponentById("account-password-edit-security")).click();

    assertTrue(passwordChangeDialog.isOpened());
    assertFalse(emailChangeDialog.isOpened());

    passwordChangeDialog.close();

    assertEquals(
        "Unsaved", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    verifyNoInteractions(saveProfileFunction);
  }

  @Test
  void securityEmailButtonOpensTheDraftDialogWithoutChangingProfileFields() {
    ((TextField) findAccountComponentById("account-first-name")).setValue("Unsaved");
    AccountEmailChangeDialog emailChangeDialog =
        (AccountEmailChangeDialog) findAccountComponentById("account-email-change-dialog");

    assertFalse(emailChangeDialog.isOpened());
    ((Button) findAccountComponentById("account-email-edit-security")).click();

    assertTrue(emailChangeDialog.isOpened());

    emailChangeDialog.close();

    assertEquals(
        "Unsaved", ((TextField) findAccountComponentById("account-first-name")).getValue());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    verifyNoInteractions(saveProfileFunction);
  }
}
