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
  private UI ui;
  private AccountEmailChangeDialog dialog;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    dialog = new AccountEmailChangeDialog(new MessageProperties(new CustomI18NProvider()));
    ui.add(dialog);
  }

  @AfterEach
  void cleanup() {
    dialog.close();
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
  }

  private Stream<Component> slottedTree(Element root) {
    return root.getChildren()
        .flatMap(element -> element.getComponent().stream())
        .flatMap(this::tree);
  }

  private Component byId(String id) {
    return Stream.of(
            tree(dialog),
            slottedTree(dialog.getHeader().getElement()),
            slottedTree(dialog.getFooter().getElement()))
        .flatMap(stream -> stream)
        .filter(component -> component.getId().orElse("").equals(id))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void fieldsAndHeaderUseTheSelectedLanguageAndNativeModalDismissal(String language) {
    locale.set(Locale.forLanguageTag(language));
    dialog.refreshTexts();
    dialog.open();
    MessageProperties texts = MessageProperties.forLocale(new CustomI18NProvider(), locale.get());
    assertTrue(dialog.isOpened());
    assertEquals(ModalityMode.STRICT, dialog.getModality());
    assertTrue(dialog.isCloseOnEsc());
    assertTrue(dialog.isCloseOnOutsideClick());
    assertEquals(
        texts.getAccountSettingsChangeEmail(), dialog.getElement().getProperty("ariaLabel"));
    assertEquals(
        texts.getAccountEmailDialogNewEmail(), ((EmailField) byId("account-email-new")).getLabel());
    assertEquals(
        texts.getAccountEmailDialogConfirmEmail(),
        ((EmailField) byId("account-email-confirm")).getLabel());
    PasswordField password = (PasswordField) byId("account-email-password");
    assertEquals(texts.getAccountEmailDialogPassword(), password.getLabel());
    assertEquals(Autocomplete.CURRENT_PASSWORD, password.getAutocomplete());
    assertEquals(
        texts.getAdminUsersCloseDialog(),
        ((Button) byId("account-email-close")).getAriaLabel().orElseThrow());
    assertTrue(
        slottedTree(dialog.getHeader().getElement())
            .anyMatch(
                component ->
                    "vaadin:envelope".equals(component.getElement().getAttribute("icon"))));
    assertTrue(
        tree(dialog)
            .anyMatch(
                component ->
                    component
                        .getElement()
                        .getTextRecursively()
                        .contains(texts.getAccountEmailDialogHint())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"account-email-close", "account-email-cancel", "native-close"})
  void allClosePathsDiscardEmailDraftAndPassword(String action) {
    dialog.open();
    ((EmailField) byId("account-email-new")).setValue("new@example.test");
    ((EmailField) byId("account-email-confirm")).setValue("new@example.test");
    ((PasswordField) byId("account-email-password")).setValue("Only-a-local-draft");
    if (action.equals("native-close")) {
      dialog.setOpened(false);
    } else {
      ((Button) byId(action)).click();
    }
    assertFalse(dialog.isOpened());
    dialog.open();
    assertTrue(((EmailField) byId("account-email-new")).isEmpty());
    assertTrue(((EmailField) byId("account-email-confirm")).isEmpty());
    assertTrue(((PasswordField) byId("account-email-password")).isEmpty());
  }

  @Test
  void languageRefreshPreservesDraftsAndIncompleteSubmissionKeepsTheDialogOpen() {
    dialog.open();
    ((EmailField) byId("account-email-new")).setValue("draft@example.test");
    locale.set(Locale.ENGLISH);
    dialog.refreshTexts();
    assertEquals("draft@example.test", ((EmailField) byId("account-email-new")).getValue());
    Button request = (Button) byId("account-email-request");
    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
            .getAccountEmailDialogRequest(),
        request.getText());
    assertTrue(request.isEnabled());
    request.click();
    assertTrue(dialog.isOpened());
    assertEquals("draft@example.test", ((EmailField) byId("account-email-new")).getValue());
  }

  @Test
  void unfinishedClientDraftsAreSynchronizedAndOpeningStartsWithEmptyFields() {
    dialog.open();
    EmailField email = (EmailField) byId("account-email-new");
    EmailField confirm = (EmailField) byId("account-email-confirm");
    PasswordField password = (PasswordField) byId("account-email-password");
    assertEquals(ValueChangeMode.EAGER, email.getValueChangeMode());
    assertEquals(ValueChangeMode.EAGER, confirm.getValueChangeMode());
    assertEquals(ValueChangeMode.EAGER, password.getValueChangeMode());
    dialog.close();
    // Nachlaufende Feldsynchronisierung darf beim nächsten Öffnen nichts wiederherstellen.
    email.setValue("late@example.test");
    confirm.setValue("late@example.test");
    password.setValue("late-draft");
    dialog.open();
    assertTrue(email.isEmpty());
    assertTrue(confirm.isEmpty());
    assertTrue(password.isEmpty());
  }

  @Test
  void successfulRequestPassesConfirmedAddressAndClearsTheDraft() {
    ArrayList<EmailChange> calls = new ArrayList<EmailChange>();
    dialog.setRequestAction(calls::add);
    dialog.open();
    ((EmailField) byId("account-email-new")).setValue("new@example.test");
    ((EmailField) byId("account-email-confirm")).setValue("new@example.test");
    ((PasswordField) byId("account-email-password")).setValue("Old-secret!");
    ((Button) byId("account-email-request")).click();
    assertEquals(1, calls.size());
    assertEquals("new@example.test", calls.getFirst().newEmail());
    assertFalse(dialog.isOpened());
    assertTrue(((EmailField) byId("account-email-new")).isEmpty());
    assertTrue(((PasswordField) byId("account-email-password")).isEmpty());
  }

  @Test
  void conflictingAddressMarksBothEmailFieldsAndKeepsTheCurrentPasswordUnmarked() {
    dialog.setRequestAction(
        change -> {
          throw new Rejected(Problem.EMAIL_UNAVAILABLE);
        });
    dialog.open();
    ((EmailField) byId("account-email-new")).setValue("taken@example.test");
    ((EmailField) byId("account-email-confirm")).setValue("taken@example.test");
    ((PasswordField) byId("account-email-password")).setValue("Old-secret!");
    ((Button) byId("account-email-request")).click();
    assertTrue(dialog.isOpened());
    assertTrue(((EmailField) byId("account-email-new")).isInvalid());
    assertTrue(((EmailField) byId("account-email-confirm")).isInvalid());
    assertFalse(((PasswordField) byId("account-email-password")).isInvalid());
  }
}
