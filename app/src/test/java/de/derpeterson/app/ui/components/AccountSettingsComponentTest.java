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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
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
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import java.time.LocalDate;
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
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();
  private UI testUi;
  private UserEntity testUserEntity;
  private SerializableFunction<Changes, Profile> saver;
  private AccountSettingsComponent accountSettingsComponent;

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> localeReference.get());
    VaadinSession.setCurrent(session);
    testUi.getInternals().setSession(session);
    saver = mock(SerializableFunction.class);
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
        new AccountSettingsComponent(new MessageProperties(i18nProvider), testUserEntity, saver, 7);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Stream<Component> tree(Component component) {
    if (component instanceof TabSheet tabs) {
      return Stream.concat(
          Stream.of(tabs),
          IntStream.range(0, tabs.getTabCount())
              .mapToObj(tabs::getTabAt)
              .flatMap(tab -> Stream.concat(tree(tab), tree(tabs.getComponent(tab)))));
    }
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  private void navigateTo(AccountSection section) {
    PageNavigationComponent navigation = (PageNavigationComponent) byId("account-navigation");
    navigation.activate(
        (SideNavItem) byId("account-tab-" + section.name().toLowerCase(Locale.ROOT)));
  }

  private Component byId(String id) {
    return tree(accountSettingsComponent)
        .filter(component -> component.getId().orElse("").equals(id))
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
      String language, String preference, String german, String english) {
    ComboBox<Locale> choice = (ComboBox<Locale>) byId("account-language");
    Locale selected = Locale.forLanguageTag(preference);
    choice.setValue(selected);
    localeReference.set(Locale.forLanguageTag(language));
    accountSettingsComponent.refreshTexts();
    assertEquals(german, choice.getItemLabelGenerator().apply(Locale.GERMAN));
    assertEquals(english, choice.getItemLabelGenerator().apply(Locale.ENGLISH));
    JsonNode presentation =
        assertInstanceOf(JsonNode.class, choice.getElement().getPropertyRaw("selectedItem"));
    assertEquals(preference.equals("de") ? german : english, presentation.path("label").asText());
    assertEquals(selected, choice.getValue());
    verifyNoInteractions(saver);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sessionsShowConfiguredLimitWithoutPresentingSampleCountAsReal(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(i18nProvider, localeReference.get());
    assertEquals(
        messageProperties.getAccountSettingsSessionsLimit(7),
        ((Badge) byId("account-sessions-limit")).getText());
    String panel = byId("account-panel-sessions").getElement().getTextRecursively();
    assertTrue(panel.contains(messageProperties.getPortalSessionActiveCount(0)));
    assertTrue(panel.contains(messageProperties.getAccountSettingsSessionsActivity()));
    verifyNoInteractions(saver);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void securityHeadingAndActionNamesRefreshWithoutChangingTheDraft(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    navigateTo(AccountSection.SECURITY);
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(i18nProvider, localeReference.get());
    String heading =
        tree(accountSettingsComponent)
            .filter(CosmosSectionHeaderComponent.class::isInstance)
            .findFirst()
            .orElseThrow()
            .getElement()
            .getTextRecursively();
    assertTrue(heading.contains(messageProperties.getAccountSettingsSecurityIntroduction()));
    assertFalse(heading.contains(messageProperties.getAccountSettingsSecurityDescription()));
    Button emailAction = (Button) byId("account-email-edit-security");
    assertEquals(messageProperties.getAccountSettingsChangeAction(), emailAction.getText());
    assertEquals(
        messageProperties.getAccountSettingsChangeEmail(),
        emailAction.getAriaLabel().orElseThrow());
    TabSheet tabs = (TabSheet) byId("account-security-tabs");
    assertEquals(byId("account-security-tab-portal"), tabs.getSelectedTab());
    assertEquals(
        messageProperties.getAccountSecurityPortalAccess(),
        byId("account-security-tab-portal").getElement().getTextRecursively());
    tabs.setSelectedTab((Tab) byId("account-security-tab-providers"));
    accountSettingsComponent.refreshTexts();
    assertEquals(byId("account-security-tab-providers"), tabs.getSelectedTab());
    assertEquals(
        messageProperties.getProviderSettingsPanelTitle(),
        byId("account-security-tab-providers").getElement().getTextRecursively());
    assertEquals("Alex", ((TextField) byId("account-first-name")).getValue());
    verifyNoInteractions(saver);
  }

  @Test
  void tabChangesPreserveDraftsAndNeverChangeTheAccount() {
    TextField first = (TextField) byId("account-first-name");
    first.setValue("Draft");
    for (int index : new int[] {1, 2, 0}) {
      navigateTo(AccountSection.values()[index]);
      assertEquals(index == 0, byId("account-panel-profile").isVisible());
      assertEquals(index == 1, byId("account-panel-security").isVisible());
      assertEquals(index == 2, byId("account-panel-sessions").isVisible());
    }
    ((ComboBox<?>) byId("account-language")).clear();
    assertEquals("Draft", first.getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    assertEquals(Locale.GERMAN, testUserEntity.getPreferredLocale());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    assertEquals(LocalDate.of(1997, 11, 5), testUserEntity.getBirthDate());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageRefreshKeepsSelectedTabAndInputsAndTranslatesSessions(String language) {
    ((TextField) byId("account-first-name")).setValue("Unsaved");
    navigateTo(AccountSection.SESSIONS);
    localeReference.set(Locale.forLanguageTag(language));
    accountSettingsComponent.refreshTexts();
    MessageProperties messageProperties =
        MessageProperties.forLocale(i18nProvider, localeReference.get());
    assertTrue(
        accountSettingsComponent
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsTitle()));
    assertTrue(
        byId("account-tab-sessions")
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsTab()));
    assertTrue(
        byId("account-panel-sessions")
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountSettingsSessionsActivity()));
    assertEquals(AccountSection.SESSIONS, accountSettingsComponent.getSelectedSection());
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  @Test
  void regionalSavedLanguageMapsToASupportedOptionWithoutChangingTheUser() {
    testUserEntity.setPreferredLocale(Locale.GERMANY);
    accountSettingsComponent =
        new AccountSettingsComponent(new MessageProperties(i18nProvider), testUserEntity, saver, 7);
    assertEquals(Locale.GERMAN, ((ComboBox<?>) byId("account-language")).getValue());
    assertEquals(Locale.GERMANY, testUserEntity.getPreferredLocale());
  }

  @Test
  void profileImagePreviewDoesNotSaveOrChangeProfileDrafts() {
    Button picture =
        tree(accountSettingsComponent)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button ->
                button
                    .getText()
                    .equals(
                        MessageProperties.forLocale(i18nProvider, localeReference.get())
                            .getAccountSettingsEditPicture()))
            .findFirst()
            .orElseThrow();
    assertTrue(picture.isEnabled());
    ((TextField) byId("account-first-name")).setValue("Unsaved draft");
    picture.click();
    verifyNoInteractions(saver);
    assertEquals("Unsaved draft", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    assertEquals(Locale.GERMAN, testUserEntity.getPreferredLocale());
  }

  private void save() {
    ((Button) byId("account-save")).click();
  }

  private void assertNotification(String expected, boolean error) {
    NotificationHelper helper = NotificationHelper.getInstance();
    Supplier<String> message =
        (Supplier<String>) ReflectionTestUtils.getField(helper, "messageSupplier");
    assertEquals(expected, message.get());
    Notification notification =
        (Notification) ReflectionTestUtils.getField(helper, "currentNotification");
    assertTrue(notification.isOpened());
    Span title = (Span) ReflectionTestUtils.getField(helper, "titleText");
    assertTrue(
        title.hasClassName(error ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.SUCCESS));
    assertTrue(
        tree(notification)
            .anyMatch(component ->
                component.getElement().getAttribute("src") != null
                    && component
                        .getElement()
                        .getAttribute("src")
                        .equals(
                            error
                                ? "icons/lucide/triangle-alert.svg"
                                : "icons/lucide/circle-check.svg")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requiredFieldsAreMarkedOnlyOnSaveAndNothingIsWritten(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    accountSettingsComponent.refreshTexts();
    TextField first = (TextField) byId("account-first-name");
    TextField last = (TextField) byId("account-last-name");
    DatePicker birth = (DatePicker) byId("account-birth-date");
    ComboBox<?> choice = (ComboBox<?>) byId("account-language");
    ComboBox<?> gender = (ComboBox<?>) byId("account-gender");
    first.setValue("   ");
    last.clear();
    birth.clear();
    choice.clear();
    gender.clear();
    assertFalse(first.isInvalid());
    save();
    assertTrue(first.isInvalid());
    assertTrue(last.isInvalid());
    assertFalse(birth.isInvalid());
    assertTrue(choice.isInvalid());
    assertFalse(gender.isInvalid());
    verifyNoInteractions(saver);
    assertNotification(
        new MessageProperties(i18nProvider).getBaseValidationRequiredMessage(), true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void todayAndFutureBirthDatesAreRejectedAndCorrectionCanBeSaved(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    DatePicker birth = (DatePicker) byId("account-birth-date");
    for (LocalDate date : new LocalDate[] {LocalDate.now(), LocalDate.now().plusDays(1)}) {
      birth.setValue(date);
      save();
      assertTrue(birth.isInvalid());
      assertNotification(
          new MessageProperties(i18nProvider).getBaseValidationBirthDatePastMessage(), true);
    }
    verifyNoInteractions(saver);
    LocalDate date = LocalDate.of(1990, 2, 1);
    birth.setValue(date);
    when(saver.apply(any()))
        .thenReturn(new Profile(4L, "Alex", "Weber", date, Locale.GERMAN, Gender.OTHER));
    save();
    assertFalse(birth.isInvalid());
    verify(saver).apply(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void successUsesReturnedDataAndNewVersionForTheNextSave(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    ((TextField) byId("account-first-name")).setValue(" New ");
    LocalDate date = LocalDate.of(1992, 3, 4);
    ((DatePicker) byId("account-birth-date")).setValue(date);
    when(saver.apply(any()))
        .thenReturn(new Profile(4L, "New", "Weber", date, Locale.ENGLISH, Gender.OTHER));
    save();
    verify(saver).apply(new Changes(3L, " New ", "Weber", date, Locale.GERMAN, Gender.OTHER));
    assertTrue(accountSettingsComponent.getElement().getTextRecursively().contains("New Weber"));
    assertEquals("New", ((TextField) byId("account-first-name")).getValue());
    assertEquals(Locale.ENGLISH, ((ComboBox<?>) byId("account-language")).getValue());
    assertNotification(new MessageProperties(i18nProvider).getAccountSettingsSaveSuccess(), false);
    save();
    verify(saver).apply(new Changes(4L, "New", "Weber", date, Locale.ENGLISH, Gender.OTHER));
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void failedAndConflictingSavesKeepDraftsAndShowTranslatedErrors(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    ((TextField) byId("account-first-name")).setValue("Keep draft");
    when(saver.apply(any())).thenThrow(new OptimisticLockingFailureException("changed"));
    save();
    assertNotification(new MessageProperties(i18nProvider).getBaseUserUpdateConflict(), true);
    doThrow(new IllegalStateException("Unavailable")).when(saver).apply(any());
    save();
    assertNotification(new MessageProperties(i18nProvider).getAccountSettingsSaveFailed(), true);
    assertEquals("Keep draft", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
  }

  @Test
  void emailIsVisibleAndReadOnlyWithoutBeingDisabled() {
    EmailField field = (EmailField) byId("account-email");
    assertTrue(field.isEnabled());
    assertTrue(field.isReadOnly());
    assertEquals(testUserEntity.getEmail(), field.getValue());
    assertTrue(field.getHelperText() == null || field.getHelperText().isBlank());
    assertFalse(
        tree(accountSettingsComponent)
            .anyMatch(
                component -> component.getId().orElse("").equals("account-email-edit-profile")));
    assertFalse(
        tree(byId("account-panel-security"))
            .anyMatch(
                component -> component.getElement().getClassList().contains("account-info-text")));
  }

  @Test
  void securityPasswordButtonOpensOnlyItsDialogAndPreservesProfileDraft() {
    ((TextField) byId("account-first-name")).setValue("Unsaved");
    AccountPasswordChangeDialog passwordDialog =
        (AccountPasswordChangeDialog) byId("account-password-change-dialog");
    AccountEmailChangeDialog emailDialog =
        (AccountEmailChangeDialog) byId("account-email-change-dialog");
    ((Button) byId("account-password-edit-security")).click();
    assertTrue(passwordDialog.isOpened());
    assertFalse(emailDialog.isOpened());
    passwordDialog.close();
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", testUserEntity.getFirstName());
    verifyNoInteractions(saver);
  }

  @Test
  void securityEmailButtonOpensTheDraftDialogWithoutChangingProfileFields() {
    ((TextField) byId("account-first-name")).setValue("Unsaved");
    AccountEmailChangeDialog dialog =
        (AccountEmailChangeDialog) byId("account-email-change-dialog");
    assertFalse(dialog.isOpened());
    ((Button) byId("account-email-edit-security")).click();
    assertTrue(dialog.isOpened());
    dialog.close();
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("own-account@example.test", testUserEntity.getEmail());
    verifyNoInteractions(saver);
  }
}
