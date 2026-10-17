package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class AccountSettingsComponentTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider provider = new CustomI18NProvider();
  private UI ui;
  private UserEntity user;
  private SerializableFunction<Changes, Profile> saver;
  private AccountSettingsComponent content;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    saver = mock(SerializableFunction.class);
    user =
        UserEntity.builder()
            .version(3L)
            .firstName("Alex")
            .lastName("Weber")
            .email("own-account@example.test")
            .birthDate(LocalDate.of(1997, 11, 5))
            .enabled(true)
            .preferredLocale(Locale.GERMAN)
            .build();
    content = new AccountSettingsComponent(new MessageProperties(provider), user, saver, 7);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  private Component byId(String id) {
    return tree(content)
        .filter(component -> component.getId().orElse("").equals(id))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void sessionsShowConfiguredLimitWithoutPresentingSampleCountAsReal(String language) {
    locale.set(Locale.forLanguageTag(language));
    content.refreshTexts();
    var texts = MessageProperties.forLocale(provider, locale.get());
    assertEquals(
        texts.getAccountSettingsSessionsLimit(7),
        ((Span) byId("account-sessions-limit")).getText());
    var panel = byId("account-panel-sessions").getElement().getTextRecursively();
    assertTrue(panel.contains(texts.getAccountSettingsSessionsActiveTitle()));
    assertTrue(panel.contains(texts.getAccountSettingsSessionsActivity()));
    verifyNoInteractions(saver);
  }

  @Test
  void tabChangesPreserveDraftsAndNeverChangeTheAccount() {
    var first = (TextField) byId("account-first-name");
    first.setValue("Draft");
    var tabs = (Tabs) byId("account-tabs");
    for (int index : new int[] {1, 2, 0}) {
      tabs.setSelectedIndex(index);
      assertEquals(index == 0, byId("account-panel-profile").isVisible());
      assertEquals(index == 1, byId("account-panel-security").isVisible());
      assertEquals(index == 2, byId("account-panel-sessions").isVisible());
    }
    ((ComboBox<?>) byId("account-language")).clear();
    assertEquals("Draft", first.getValue());
    assertEquals("Alex", user.getFirstName());
    assertEquals(Locale.GERMAN, user.getPreferredLocale());
    assertEquals("own-account@example.test", user.getEmail());
    assertEquals(LocalDate.of(1997, 11, 5), user.getBirthDate());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageRefreshKeepsSelectedTabAndInputsAndTranslatesSessions(String language) {
    ((TextField) byId("account-first-name")).setValue("Unsaved");
    var tabs = (Tabs) byId("account-tabs");
    tabs.setSelectedIndex(2);
    locale.set(Locale.forLanguageTag(language));
    content.refreshTexts();
    var texts = MessageProperties.forLocale(provider, locale.get());
    assertTrue(content.getElement().getTextRecursively().contains(texts.getAccountSettingsTitle()));
    assertTrue(
        byId("account-tab-sessions")
            .getElement()
            .getTextRecursively()
            .contains(texts.getAccountSettingsSessionsTab()));
    assertTrue(
        byId("account-panel-sessions")
            .getElement()
            .getTextRecursively()
            .contains(texts.getAccountSettingsSessionsActivity()));
    assertEquals(2, tabs.getSelectedIndex());
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
  }

  @Test
  void regionalSavedLanguageMapsToASupportedOptionWithoutChangingTheUser() {
    user.setPreferredLocale(Locale.GERMANY);
    content = new AccountSettingsComponent(new MessageProperties(provider), user, saver, 7);
    assertEquals(Locale.GERMAN, ((ComboBox<?>) byId("account-language")).getValue());
    assertEquals(Locale.GERMANY, user.getPreferredLocale());
  }

  @Test
  void profileImagePreviewDoesNotSaveOrChangeProfileDrafts() {
    var picture =
        tree(content)
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(
                button ->
                    button
                        .getText()
                        .equals(
                            MessageProperties.forLocale(provider, locale.get())
                                .getAccountSettingsEditPicture()))
            .findFirst()
            .orElseThrow();
    assertTrue(picture.isEnabled());
    ((TextField) byId("account-first-name")).setValue("Unsaved draft");
    picture.click();
    verifyNoInteractions(saver);
    assertEquals("Unsaved draft", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
    assertEquals("own-account@example.test", user.getEmail());
    assertEquals(Locale.GERMAN, user.getPreferredLocale());
  }

  private void save() {
    ((Button) byId("account-save")).click();
  }

  private void assertNotification(String expected, boolean error) {
    var helper = NotificationHelper.getInstance();
    var message = (Supplier<String>) ReflectionTestUtils.getField(helper, "messageSupplier");
    assertEquals(expected, message.get());
    var notification = (Notification) ReflectionTestUtils.getField(helper, "currentNotification");
    assertTrue(notification.isOpened());
    var title = (Span) ReflectionTestUtils.getField(helper, "titleText");
    assertTrue(
        title.hasClassName(error ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.SUCCESS));
    assertTrue(
        tree(notification)
            .anyMatch(
                component ->
                    component.getElement().getAttribute("icon") != null
                        && component
                            .getElement()
                            .getAttribute("icon")
                            .equals(error ? "vaadin:warning" : "vaadin:check-circle")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void requiredFieldsAreMarkedOnlyOnSaveAndNothingIsWritten(String language) {
    locale.set(Locale.forLanguageTag(language));
    content.refreshTexts();
    var first = (TextField) byId("account-first-name");
    var last = (TextField) byId("account-last-name");
    var birth = (DatePicker) byId("account-birth-date");
    var choice = (ComboBox<?>) byId("account-language");
    first.setValue("   ");
    last.clear();
    birth.clear();
    choice.clear();
    assertFalse(first.isInvalid());
    save();
    assertTrue(first.isInvalid());
    assertTrue(last.isInvalid());
    assertTrue(birth.isInvalid());
    assertTrue(choice.isInvalid());
    verifyNoInteractions(saver);
    assertNotification(new MessageProperties(provider).getBaseValidationRequiredMessage(), true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void todayAndFutureBirthDatesAreRejectedAndCorrectionCanBeSaved(String language) {
    locale.set(Locale.forLanguageTag(language));
    var birth = (DatePicker) byId("account-birth-date");
    for (var date : new LocalDate[] {LocalDate.now(), LocalDate.now().plusDays(1)}) {
      birth.setValue(date);
      save();
      assertTrue(birth.isInvalid());
      assertNotification(
          new MessageProperties(provider).getBaseValidationBirthDatePastMessage(), true);
    }
    verifyNoInteractions(saver);
    var date = LocalDate.of(1990, 2, 1);
    birth.setValue(date);
    when(saver.apply(any())).thenReturn(new Profile(4L, "Alex", "Weber", date, Locale.GERMAN));
    save();
    assertFalse(birth.isInvalid());
    verify(saver).apply(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void successUsesReturnedDataAndNewVersionForTheNextSave(String language) {
    locale.set(Locale.forLanguageTag(language));
    ((TextField) byId("account-first-name")).setValue(" New ");
    var date = LocalDate.of(1992, 3, 4);
    ((DatePicker) byId("account-birth-date")).setValue(date);
    when(saver.apply(any())).thenReturn(new Profile(4L, "New", "Weber", date, Locale.ENGLISH));
    save();
    verify(saver).apply(new Changes(3L, " New ", "Weber", date, Locale.GERMAN));
    assertTrue(content.getElement().getTextRecursively().contains("New Weber"));
    assertEquals("New", ((TextField) byId("account-first-name")).getValue());
    assertEquals(Locale.ENGLISH, ((ComboBox<?>) byId("account-language")).getValue());
    assertNotification(new MessageProperties(provider).getAccountSettingsSaveSuccess(), false);
    save();
    verify(saver).apply(new Changes(4L, "New", "Weber", date, Locale.ENGLISH));
    assertEquals("Alex", user.getFirstName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void failedAndConflictingSavesKeepDraftsAndShowTranslatedErrors(String language) {
    locale.set(Locale.forLanguageTag(language));
    ((TextField) byId("account-first-name")).setValue("Keep draft");
    when(saver.apply(any())).thenThrow(new OptimisticLockingFailureException("changed"));
    save();
    assertNotification(new MessageProperties(provider).getBaseUserUpdateConflict(), true);
    doThrow(new IllegalStateException("Unavailable")).when(saver).apply(any());
    save();
    assertNotification(new MessageProperties(provider).getAccountSettingsSaveFailed(), true);
    assertEquals("Keep draft", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
  }

  @Test
  void emailIsVisibleButDisabledAndReadOnly() {
    var field = (EmailField) byId("account-email");
    assertFalse(field.isEnabled());
    assertTrue(field.isReadOnly());
    assertEquals(user.getEmail(), field.getValue());
    assertTrue(field.getHelperText() == null || field.getHelperText().isBlank());
    assertFalse(
        tree(content)
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
    var passwordDialog = (AccountPasswordChangeDialog) byId("account-password-change-dialog");
    var emailDialog = (AccountEmailChangeDialog) byId("account-email-change-dialog");
    ((Button) byId("account-password-edit-security")).click();
    assertTrue(passwordDialog.isOpened());
    assertFalse(emailDialog.isOpened());
    passwordDialog.close();
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
    verifyNoInteractions(saver);
  }

  @Test
  void securityEmailButtonOpensTheDraftDialogWithoutChangingProfileFields() {
    ((TextField) byId("account-first-name")).setValue("Unsaved");
    var dialog = (AccountEmailChangeDialog) byId("account-email-change-dialog");
    assertFalse(dialog.isOpened());
    ((Button) byId("account-email-edit-security")).click();
    assertTrue(dialog.isOpened());
    dialog.close();
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("own-account@example.test", user.getEmail());
    verifyNoInteractions(saver);
  }
}
