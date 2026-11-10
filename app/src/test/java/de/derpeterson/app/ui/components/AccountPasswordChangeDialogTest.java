package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.PasswordChange;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;

class AccountPasswordChangeDialogTest {
  private static final String[] PASSWORD_FIELD_ID_SUFFIXES = {"current", "new", "confirm"};

  private UI testUi;
  private AccountPasswordChangeDialog passwordChangeDialog;
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void createPasswordChangeDialogEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    passwordChangeDialog =
        new AccountPasswordChangeDialog(new MessageProperties(new CustomI18NProvider()));
    testUi.add(passwordChangeDialog);
  }

  @AfterEach
  void clearPasswordChangeDialogEnvironment() {
    passwordChangeDialog.close();
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Stream<Component> streamComponentTree(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::streamComponentTree));
  }

  private Stream<Component> streamSlottedComponents(Element slotRootElement) {
    return slotRootElement
        .getChildren()
        .flatMap(childElement -> childElement.getComponent().stream())
        .flatMap(this::streamComponentTree);
  }

  private Component findDialogComponentById(String componentId) {
    return Stream.of(
            streamComponentTree(passwordChangeDialog),
            streamSlottedComponents(passwordChangeDialog.getHeader().getElement()),
            streamSlottedComponents(passwordChangeDialog.getFooter().getElement()))
        .flatMap(childComponentStream -> childComponentStream)
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(componentId))
        .findFirst()
        .orElseThrow();
  }

  private PasswordField findPasswordField(String passwordFieldIdSuffix) {
    return (PasswordField) findDialogComponentById("account-password-" + passwordFieldIdSuffix);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void localizedFieldsUsePasswordManagerSemanticsAndNativeDismissal(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    passwordChangeDialog.refreshTexts();
    passwordChangeDialog.open();
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), activeLocaleReference.get());

    assertEquals(
        messageProperties.getAccountSettingsChangePassword(),
        passwordChangeDialog.getElement().getProperty("ariaLabel"));
    assertEquals(
        messageProperties.getAccountPasswordDialogCurrentPassword(),
        findPasswordField("current").getLabel());
    assertEquals(
        messageProperties.getResetPasswordNewPasswordField(), findPasswordField("new").getLabel());
    assertEquals(
        messageProperties.getAccountPasswordDialogConfirmPassword(),
        findPasswordField("confirm").getLabel());
    assertEquals(
        messageProperties.getAccountPasswordPlaceholder(),
        findPasswordField("current").getPlaceholder());
    assertEquals(
        messageProperties.getAccountNewPasswordPlaceholder(),
        findPasswordField("new").getPlaceholder());
    assertEquals(
        messageProperties.getAccountConfirmPasswordPlaceholder(),
        findPasswordField("confirm").getPlaceholder());

    assertEquals(Autocomplete.CURRENT_PASSWORD, findPasswordField("current").getAutocomplete());
    assertEquals(Autocomplete.NEW_PASSWORD, findPasswordField("new").getAutocomplete());
    assertEquals(Autocomplete.NEW_PASSWORD, findPasswordField("confirm").getAutocomplete());
    assertEquals(
        messageProperties.getAdminUsersCloseDialog(),
        ((Button) findDialogComponentById("account-password-close")).getAriaLabel().orElseThrow());
    assertTrue(
        streamComponentTree(passwordChangeDialog)
            .anyMatch(candidateComponent ->
                candidateComponent
                    .getElement()
                    .getTextRecursively()
                    .contains(messageProperties.getAccountPasswordDialogHint())));
    assertTrue(
        streamSlottedComponents(passwordChangeDialog.getHeader().getElement())
            .anyMatch(candidateComponent ->
                candidateComponent instanceof SvgIcon headerSvgIcon
                    && "icons/lucide/key-round.svg".equals(headerSvgIcon.getSrc())));
    assertEquals(ModalityMode.STRICT, passwordChangeDialog.getModality());
    assertTrue(passwordChangeDialog.isCloseOnEsc());
    assertTrue(passwordChangeDialog.isCloseOnOutsideClick());
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      assertTrue(findPasswordField(passwordFieldIdSuffix).isRequiredIndicatorVisible());
      assertTrue(findPasswordField(passwordFieldIdSuffix).isRevealButtonVisible());
      assertEquals(
          ValueChangeMode.EAGER, findPasswordField(passwordFieldIdSuffix).getValueChangeMode());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"account-password-close", "account-password-cancel", "native-close"})
  void closingDiscardsAllSecretsAndInvalidStatesImmediately(String closeActionName) {
    passwordChangeDialog.open();
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      findPasswordField(passwordFieldIdSuffix).setValue("Local-test-secret");
      findPasswordField(passwordFieldIdSuffix).setInvalid(true);
    }

    if (closeActionName.equals("native-close")) {
      passwordChangeDialog.setOpened(false);
    } else {
      ((Button) findDialogComponentById(closeActionName)).click();
    }

    assertFalse(passwordChangeDialog.isOpened());
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      assertTrue(findPasswordField(passwordFieldIdSuffix).isEmpty());
      assertFalse(findPasswordField(passwordFieldIdSuffix).isInvalid());
    }
  }

  @Test
  void languageRefreshPreservesDraftsAndDoesNotValidateWhileTyping() {
    passwordChangeDialog.open();
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      findPasswordField(passwordFieldIdSuffix).setValue("Local-" + passwordFieldIdSuffix);
    }

    activeLocaleReference.set(Locale.ENGLISH);
    passwordChangeDialog.refreshTexts();
    Button savePasswordButton = (Button) findDialogComponentById("account-password-save");

    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
            .getAccountSettingsChangePassword(),
        savePasswordButton.getText());
    assertTrue(savePasswordButton.isEnabled());
    assertTrue(passwordChangeDialog.isOpened());
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      assertEquals(
          "Local-" + passwordFieldIdSuffix, findPasswordField(passwordFieldIdSuffix).getValue());
      assertFalse(findPasswordField(passwordFieldIdSuffix).isInvalid());
    }
  }

  @Test
  void reopeningAlsoDiscardsLateClientSynchronization() {
    passwordChangeDialog.open();
    passwordChangeDialog.close();
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      findPasswordField(passwordFieldIdSuffix).setValue("Late-draft");
    }

    passwordChangeDialog.open();
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      assertTrue(findPasswordField(passwordFieldIdSuffix).isEmpty());
    }
  }

  // Absenden und serverseitige Fehlerfälle
  @Test
  void successfulSubmissionCallsActionClosesAndClearsAllSecrets() {
    ArrayList<PasswordChange> requestedPasswordChanges = new ArrayList<PasswordChange>();
    passwordChangeDialog.setChangeAction(passwordChange -> {
      requestedPasswordChanges.add(passwordChange);
      return new PasswordChanged(1L, 2L);
    });
    passwordChangeDialog.open();
    findPasswordField("current").setValue("Old-secret!");
    findPasswordField("new").setValue("New-secret!");
    findPasswordField("confirm").setValue("New-secret!");

    ((Button) findDialogComponentById("account-password-save")).click();

    assertEquals(1, requestedPasswordChanges.size());
    assertEquals("Old-secret!", requestedPasswordChanges.getFirst().currentPassword());
    assertEquals("New-secret!", requestedPasswordChanges.getFirst().newPassword());
    assertFalse(passwordChangeDialog.isOpened());
    for (String passwordFieldIdSuffix : PASSWORD_FIELD_ID_SUFFIXES) {
      assertTrue(findPasswordField(passwordFieldIdSuffix).isEmpty());
    }
  }

  @Test
  void invalidAndMismatchedPasswordsNeverInvokeTheAction() {
    AtomicInteger passwordChangeCallCounter = new AtomicInteger();
    passwordChangeDialog.setChangeAction(passwordChange -> {
      passwordChangeCallCounter.incrementAndGet();
      return null;
    });
    passwordChangeDialog.open();

    ((Button) findDialogComponentById("account-password-save")).click();

    assertTrue(findPasswordField("current").isInvalid());

    findPasswordField("current").setValue("Old-secret!");
    findPasswordField("new").setValue("a");
    findPasswordField("confirm").setValue("a");

    ((Button) findDialogComponentById("account-password-save")).click();

    assertTrue(findPasswordField("new").isInvalid());

    findPasswordField("new").setValue("New-secret!");
    findPasswordField("confirm").setValue("Other-secret!");

    ((Button) findDialogComponentById("account-password-save")).click();

    assertTrue(findPasswordField("confirm").isInvalid());
    assertEquals(0, passwordChangeCallCounter.get());
  }

  @Test
  void wrongCurrentPasswordMarksOnlyThatFieldAndKeepsDialogOpen() {
    passwordChangeDialog.setChangeAction(passwordChange -> {
      throw new BadCredentialsException("invalid");
    });
    passwordChangeDialog.open();
    findPasswordField("current").setValue("Wrong-secret!");
    findPasswordField("new").setValue("New-secret!");
    findPasswordField("confirm").setValue("New-secret!");

    ((Button) findDialogComponentById("account-password-save")).click();

    assertTrue(passwordChangeDialog.isOpened());
    assertTrue(findPasswordField("current").isInvalid());
    assertFalse(findPasswordField("new").isInvalid());
    assertFalse(findPasswordField("confirm").isInvalid());
    assertTrue(((Button) findDialogComponentById("account-password-save")).isEnabled());
  }
}
