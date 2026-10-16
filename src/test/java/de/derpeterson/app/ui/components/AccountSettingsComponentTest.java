package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountSettingsComponentTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider provider = new CustomI18NProvider();
  private UserEntity user;
  private AccountSettingsComponent content;

  @BeforeEach
  void setup() {
    UI.setCurrent(new UI());
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    user =
        UserEntity.builder()
            .firstName("Alex")
            .lastName("Weber")
            .email("own-account@example.test")
            .birthDate(LocalDate.of(1997, 11, 5))
            .enabled(true)
            .preferredLocale(Locale.GERMAN)
            .build();
    content = new AccountSettingsComponent(new MessageProperties(provider), user);
  }

  @AfterEach
  void cleanup() {
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
  void languageRefreshKeepsSelectedTabAndInputsAndClearlyLabelsSampleSessions(String language) {
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
            .contains(texts.getAccountSettingsSessionsDemo()));
    assertTrue(
        byId("account-panel-sessions")
            .getElement()
            .getTextRecursively()
            .contains(texts.getAccountSettingsSessionsHint()));
    assertEquals(2, tabs.getSelectedIndex());
    assertEquals("Unsaved", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
  }

  @Test
  void regionalSavedLanguageMapsToASupportedOptionWithoutChangingTheUser() {
    user.setPreferredLocale(Locale.GERMANY);
    content = new AccountSettingsComponent(new MessageProperties(provider), user);
    assertEquals(Locale.GERMAN, ((ComboBox<?>) byId("account-language")).getValue());
    assertEquals(Locale.GERMANY, user.getPreferredLocale());
  }

  @Test
  void allPreviewButtonsAreEnabledButClicksDoNotChangeTheAccount() {
    var buttons = tree(content).filter(Button.class::isInstance).map(Button.class::cast).toList();
    assertFalse(buttons.isEmpty());
    assertTrue(buttons.stream().allMatch(Button::isEnabled));
    ((TextField) byId("account-first-name")).setValue("Unsaved draft");
    buttons.forEach(Button::click);
    assertEquals("Unsaved draft", ((TextField) byId("account-first-name")).getValue());
    assertEquals("Alex", user.getFirstName());
    assertEquals("own-account@example.test", user.getEmail());
    assertEquals(Locale.GERMAN, user.getPreferredLocale());
  }
}
