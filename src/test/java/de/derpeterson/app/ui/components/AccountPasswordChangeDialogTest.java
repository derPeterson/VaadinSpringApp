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
  private UI ui;
  private AccountPasswordChangeDialog dialog;
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
    dialog = new AccountPasswordChangeDialog(new MessageProperties(new CustomI18NProvider()));
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

  private Stream<Component> slot(Element root) {
    return root.getChildren()
        .flatMap(element -> element.getComponent().stream())
        .flatMap(this::tree);
  }

  private Component byId(String id) {
    return Stream.of(
            tree(dialog),
            slot(dialog.getHeader().getElement()),
            slot(dialog.getFooter().getElement()))
        .flatMap(stream -> stream)
        .filter(component -> component.getId().orElse("").equals(id))
        .findFirst()
        .orElseThrow();
  }

  private PasswordField field(String suffix) {
    return (PasswordField) byId("account-password-" + suffix);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void localizedFieldsUsePasswordManagerSemanticsAndNativeDismissal(String language) {
    locale.set(Locale.forLanguageTag(language));
    dialog.refreshTexts();
    dialog.open();
    MessageProperties texts = MessageProperties.forLocale(new CustomI18NProvider(), locale.get());
    assertEquals(
        texts.getAccountSettingsChangePassword(), dialog.getElement().getProperty("ariaLabel"));
    assertEquals(texts.getAccountPasswordDialogCurrentPassword(), field("current").getLabel());
    assertEquals(texts.getResetPasswordNewPasswordField(), field("new").getLabel());
    assertEquals(texts.getAccountPasswordDialogConfirmPassword(), field("confirm").getLabel());
    assertEquals(texts.getAccountPasswordPlaceholder(), field("current").getPlaceholder());
    assertEquals(texts.getAccountNewPasswordPlaceholder(), field("new").getPlaceholder());
    assertEquals(texts.getAccountConfirmPasswordPlaceholder(), field("confirm").getPlaceholder());
    assertEquals(Autocomplete.CURRENT_PASSWORD, field("current").getAutocomplete());
    assertEquals(Autocomplete.NEW_PASSWORD, field("new").getAutocomplete());
    assertEquals(Autocomplete.NEW_PASSWORD, field("confirm").getAutocomplete());
    assertEquals(
        texts.getAdminUsersCloseDialog(),
        ((Button) byId("account-password-close")).getAriaLabel().orElseThrow());
    assertTrue(
        tree(dialog)
            .anyMatch(
                component ->
                    component
                        .getElement()
                        .getTextRecursively()
                        .contains(texts.getAccountPasswordDialogHint())));
    assertTrue(
        slot(dialog.getHeader().getElement())
            .anyMatch(
                component -> "vaadin:lock".equals(component.getElement().getAttribute("icon"))));
    assertEquals(ModalityMode.STRICT, dialog.getModality());
    assertTrue(dialog.isCloseOnEsc());
    assertTrue(dialog.isCloseOnOutsideClick());
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isRequiredIndicatorVisible());
      assertTrue(field(suffix).isRevealButtonVisible());
      assertEquals(ValueChangeMode.EAGER, field(suffix).getValueChangeMode());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"account-password-close", "account-password-cancel", "native-close"})
  void closingDiscardsAllSecretsAndInvalidStatesImmediately(String action) {
    dialog.open();
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      field(suffix).setValue("Local-test-secret");
      field(suffix).setInvalid(true);
    }
    if (action.equals("native-close")) {
      dialog.setOpened(false);
    } else {
      ((Button) byId(action)).click();
    }
    assertFalse(dialog.isOpened());
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isEmpty());
      assertFalse(field(suffix).isInvalid());
    }
  }

  @Test
  void languageRefreshPreservesDraftsAndDoesNotValidateWhileTyping() {
    dialog.open();
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      field(suffix).setValue("Local-" + suffix);
    }
    locale.set(Locale.ENGLISH);
    dialog.refreshTexts();
    Button save = (Button) byId("account-password-save");
    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
            .getAccountSettingsChangePassword(),
        save.getText());
    assertTrue(save.isEnabled());
    assertTrue(dialog.isOpened());
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      assertEquals("Local-" + suffix, field(suffix).getValue());
      assertFalse(field(suffix).isInvalid());
    }
  }

  @Test
  void reopeningAlsoDiscardsLateClientSynchronization() {
    dialog.open();
    dialog.close();
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      field(suffix).setValue("Late-draft");
    }
    dialog.open();
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isEmpty());
    }
  }

  @Test
  void successfulSubmissionCallsActionClosesAndClearsAllSecrets() {
    ArrayList<PasswordChange> calls = new ArrayList<PasswordChange>();
    dialog.setChangeAction(
        change -> {
          calls.add(change);
          return new PasswordChanged(1L, 2L);
        });
    dialog.open();
    field("current").setValue("Old-secret!");
    field("new").setValue("New-secret!");
    field("confirm").setValue("New-secret!");
    ((Button) byId("account-password-save")).click();
    assertEquals(1, calls.size());
    assertEquals("Old-secret!", calls.getFirst().currentPassword());
    assertEquals("New-secret!", calls.getFirst().newPassword());
    assertFalse(dialog.isOpened());
    for (String suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isEmpty());
    }
  }

  @Test
  void invalidAndMismatchedPasswordsNeverInvokeTheAction() {
    AtomicInteger calls = new AtomicInteger();
    dialog.setChangeAction(
        change -> {
          calls.incrementAndGet();
          return null;
        });
    dialog.open();
    ((Button) byId("account-password-save")).click();
    assertTrue(field("current").isInvalid());
    field("current").setValue("Old-secret!");
    field("new").setValue("a");
    field("confirm").setValue("a");
    ((Button) byId("account-password-save")).click();
    assertTrue(field("new").isInvalid());
    field("new").setValue("New-secret!");
    field("confirm").setValue("Other-secret!");
    ((Button) byId("account-password-save")).click();
    assertTrue(field("confirm").isInvalid());
    assertEquals(0, calls.get());
  }

  @Test
  void wrongCurrentPasswordMarksOnlyThatFieldAndKeepsDialogOpen() {
    dialog.setChangeAction(
        change -> {
          throw new BadCredentialsException("invalid");
        });
    dialog.open();
    field("current").setValue("Wrong-secret!");
    field("new").setValue("New-secret!");
    field("confirm").setValue("New-secret!");
    ((Button) byId("account-password-save")).click();
    assertTrue(dialog.isOpened());
    assertTrue(field("current").isInvalid());
    assertFalse(field("new").isInvalid());
    assertFalse(field("confirm").isInvalid());
    assertTrue(((Button) byId("account-password-save")).isEnabled());
  }
}
