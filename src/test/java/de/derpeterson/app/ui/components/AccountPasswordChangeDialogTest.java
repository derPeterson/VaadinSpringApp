package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountPasswordChangeDialogTest {
  private AccountPasswordChangeDialog dialog;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    var ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
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
    var texts = MessageProperties.forLocale(new CustomI18NProvider(), locale.get());
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
    for (var suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isRequiredIndicatorVisible());
      assertTrue(field(suffix).isRevealButtonVisible());
      assertEquals(ValueChangeMode.EAGER, field(suffix).getValueChangeMode());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"account-password-close", "account-password-cancel", "native-close"})
  void closingDiscardsAllSecretsAndInvalidStatesImmediately(String action) {
    dialog.open();
    for (var suffix : new String[] {"current", "new", "confirm"}) {
      field(suffix).setValue("Local-test-secret");
      field(suffix).setInvalid(true);
    }
    if (action.equals("native-close")) dialog.setOpened(false);
    else ((Button) byId(action)).click();
    assertFalse(dialog.isOpened());
    for (var suffix : new String[] {"current", "new", "confirm"}) {
      assertTrue(field(suffix).isEmpty());
      assertFalse(field(suffix).isInvalid());
    }
  }

  @Test
  void languageRefreshPreservesDraftsAndSaveIsStillAPreview() {
    dialog.open();
    for (var suffix : new String[] {"current", "new", "confirm"})
      field(suffix).setValue("Local-" + suffix);
    locale.set(Locale.ENGLISH);
    dialog.refreshTexts();
    var save = (Button) byId("account-password-save");
    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH)
            .getAccountSettingsChangePassword(),
        save.getText());
    assertTrue(save.isEnabled());
    save.click();
    assertTrue(dialog.isOpened());
    for (var suffix : new String[] {"current", "new", "confirm"}) {
      assertEquals("Local-" + suffix, field(suffix).getValue());
      assertFalse(field(suffix).isInvalid());
    }
  }

  @Test
  void reopeningAlsoDiscardsLateClientSynchronization() {
    dialog.open();
    dialog.close();
    for (var suffix : new String[] {"current", "new", "confirm"})
      field(suffix).setValue("Late-draft");
    dialog.open();
    for (var suffix : new String[] {"current", "new", "confirm"})
      assertTrue(field(suffix).isEmpty());
  }
}
