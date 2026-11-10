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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.AccountSecurityService.EmailChange;
import de.derpeterson.app.service.AccountSecurityService.Problem;
import de.derpeterson.app.service.AccountSecurityService.Rejected;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountEmailChangeDialogTest {
  private UI testUi;
  private AccountEmailChangeDialog emailChangeDialog;
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void createEmailChangeDialogEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    emailChangeDialog =
        new AccountEmailChangeDialog(new MessageProperties(new CustomI18NProvider()));
    testUi.add(emailChangeDialog);
  }

  @AfterEach
  void clearEmailChangeDialogEnvironment() {
    emailChangeDialog.close();
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
            streamComponentTree(emailChangeDialog),
            streamSlottedComponents(emailChangeDialog.getHeader().getElement()),
            streamSlottedComponents(emailChangeDialog.getFooter().getElement()))
        .flatMap(childComponentStream -> childComponentStream)
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(componentId))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void fieldsAndHeaderUseTheSelectedLanguageAndNativeModalDismissal(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    emailChangeDialog.refreshTexts();
    emailChangeDialog.open();
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), activeLocaleReference.get());

    assertTrue(emailChangeDialog.isOpened());
    assertEquals(ModalityMode.STRICT, emailChangeDialog.getModality());
    assertTrue(emailChangeDialog.isCloseOnEsc());
    assertTrue(emailChangeDialog.isCloseOnOutsideClick());
    assertEquals(
        messageProperties.getAccountSettingsChangeEmail(),
        emailChangeDialog.getElement().getProperty("ariaLabel"));
    assertEquals(messageProperties.getAccountEmailDialogNewEmail(), findNewEmailField().getLabel());
    assertEquals(
        messageProperties.getAccountEmailDialogConfirmEmail(), findConfirmEmailField().getLabel());
    PasswordField currentPasswordField = findCurrentPasswordField();

    assertEquals(
        messageProperties.getAccountEmailDialogPassword(), currentPasswordField.getLabel());
    assertEquals(Autocomplete.CURRENT_PASSWORD, currentPasswordField.getAutocomplete());
    assertEquals(
        messageProperties.getAdminUsersCloseDialog(),
        ((Button) findDialogComponentById("account-email-close")).getAriaLabel().orElseThrow());
    assertTrue(
        streamSlottedComponents(emailChangeDialog.getHeader().getElement())
            .anyMatch(candidateComponent ->
                candidateComponent instanceof SvgIcon headerSvgIcon
                    && "icons/lucide/mail.svg".equals(headerSvgIcon.getSrc())));
    assertTrue(
        streamComponentTree(emailChangeDialog)
            .anyMatch(candidateComponent ->
                candidateComponent
                    .getElement()
                    .getTextRecursively()
                    .contains(messageProperties.getAccountEmailDialogHint())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"account-email-close", "account-email-cancel", "native-close"})
  void allClosePathsDiscardEmailDraftAndPassword(String closeActionName) {
    emailChangeDialog.open();
    findNewEmailField().setValue("new@example.test");
    findConfirmEmailField().setValue("new@example.test");
    findCurrentPasswordField().setValue("Only-a-local-draft");

    if (closeActionName.equals("native-close")) {
      emailChangeDialog.setOpened(false);
    } else {
      ((Button) findDialogComponentById(closeActionName)).click();
    }

    assertFalse(emailChangeDialog.isOpened());

    emailChangeDialog.open();

    assertTrue(findNewEmailField().isEmpty());
    assertTrue(findConfirmEmailField().isEmpty());
    assertTrue(findCurrentPasswordField().isEmpty());
  }

  @Test
  void languageRefreshPreservesDraftsAndIncompleteSubmissionKeepsTheDialogOpen() {
    emailChangeDialog.open();
    findNewEmailField().setValue("draft@example.test");

    activeLocaleReference.set(Locale.ENGLISH);
    emailChangeDialog.refreshTexts();

    assertEquals("draft@example.test", findNewEmailField().getValue());
    Button requestEmailChangeButton = (Button) findDialogComponentById("account-email-request");

    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
            .getAccountEmailDialogRequest(),
        requestEmailChangeButton.getText());
    assertTrue(requestEmailChangeButton.isEnabled());

    requestEmailChangeButton.click();

    assertTrue(emailChangeDialog.isOpened());
    assertEquals("draft@example.test", findNewEmailField().getValue());
  }

  @Test
  void unfinishedClientDraftsAreSynchronizedAndOpeningStartsWithEmptyFields() {
    emailChangeDialog.open();
    EmailField newEmailField = findNewEmailField();
    EmailField confirmEmailField = findConfirmEmailField();
    PasswordField currentPasswordField = findCurrentPasswordField();

    assertEquals(ValueChangeMode.EAGER, newEmailField.getValueChangeMode());
    assertEquals(ValueChangeMode.EAGER, confirmEmailField.getValueChangeMode());
    assertEquals(ValueChangeMode.EAGER, currentPasswordField.getValueChangeMode());

    emailChangeDialog.close();

    // Nachlaufende Feldsynchronisierung darf beim nächsten Öffnen nichts wiederherstellen.
    newEmailField.setValue("late@example.test");
    confirmEmailField.setValue("late@example.test");
    currentPasswordField.setValue("late-draft");
    emailChangeDialog.open();

    assertTrue(newEmailField.isEmpty());
    assertTrue(confirmEmailField.isEmpty());
    assertTrue(currentPasswordField.isEmpty());
  }

  @Test
  void successfulRequestPassesConfirmedAddressAndClearsTheDraft() {
    ArrayList<EmailChange> requestedEmailChanges = new ArrayList<EmailChange>();
    emailChangeDialog.setRequestAction(requestedEmailChanges::add);
    emailChangeDialog.open();
    findNewEmailField().setValue("new@example.test");
    findConfirmEmailField().setValue("new@example.test");
    findCurrentPasswordField().setValue("Old-secret!");

    ((Button) findDialogComponentById("account-email-request")).click();

    assertEquals(1, requestedEmailChanges.size());
    assertEquals("new@example.test", requestedEmailChanges.getFirst().newEmail());
    assertFalse(emailChangeDialog.isOpened());
    assertTrue(findNewEmailField().isEmpty());
    assertTrue(findCurrentPasswordField().isEmpty());
  }

  @Test
  void conflictingAddressMarksBothEmailFieldsAndKeepsTheCurrentPasswordUnmarked() {
    emailChangeDialog.setRequestAction(emailChange -> {
      throw new Rejected(Problem.EMAIL_UNAVAILABLE);
    });
    emailChangeDialog.open();
    findNewEmailField().setValue("taken@example.test");
    findConfirmEmailField().setValue("taken@example.test");
    findCurrentPasswordField().setValue("Old-secret!");

    ((Button) findDialogComponentById("account-email-request")).click();

    assertTrue(emailChangeDialog.isOpened());
    assertTrue(findNewEmailField().isInvalid());
    assertTrue(findConfirmEmailField().isInvalid());
    assertFalse(findCurrentPasswordField().isInvalid());
  }

  private EmailField findNewEmailField() {
    return (EmailField) findDialogComponentById("account-email-new");
  }

  private EmailField findConfirmEmailField() {
    return (EmailField) findDialogComponentById("account-email-confirm");
  }

  private PasswordField findCurrentPasswordField() {
    return (PasswordField) findDialogComponentById("account-email-password");
  }
}
