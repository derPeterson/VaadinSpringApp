package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.service.ConfigService;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AdminExternalLoginSectionTest {
  @ParameterizedTest
  @CsvSource({
    "de,true,true", "de,true,false", "de,false,false",
    "en,true,true", "en,true,false", "en,false,false"
  })
  void statusExplainsStoredSettingsAndDoesNotApplyAnUnsavedCheckbox(
      String language, boolean configured, boolean enabled) {
    MessageProperties texts =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    ExternalProviderSettings settings = mock(ExternalProviderSettings.class);
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://portal.example.test/");
    when(settings.load("google"))
        .thenReturn(
            new ExternalProviderSettings.Settings(
                "google", configured ? "example-client" : "", configured, enabled, "revision"));
    when(settings.load("microsoft"))
        .thenReturn(
            new ExternalProviderSettings.Settings("microsoft", "", false, false, "revision"));
    AdminExternalLoginSection section = new AdminExternalLoginSection(texts, settings, config);
    section.activate();
    Badge badge = (Badge) find(section, "provider-google-status");
    String expected =
        enabled ? texts.getProviderSettingsActive() : texts.getProviderSettingsInactive();
    assertEquals(expected, badge.getText());
    assertEquals("Google: " + expected, badge.getElement().getAttribute("aria-label"));
    assertTrue(badge.getThemeNames().contains(enabled ? "success" : "warning"));
    String expectedHint =
        !configured
            ? texts.getProviderSettingsUnconfiguredHint("Google")
            : enabled
                ? texts.getProviderSettingsActiveHint("Google")
                : texts.getProviderSettingsInactiveHint("Google");
    assertEquals(expectedHint, Tooltip.forComponent(badge).getText());
    TabSheet tabs = (TabSheet) find(section, "admin-provider-tabs");
    Checkbox checkbox =
        (Checkbox) find(tabs.getComponent(tabs.getTabAt(0)), "provider-google-enabled");
    checkbox.setValue(!enabled);
    section.refreshTexts();
    assertEquals(expected, badge.getText(), "Ein Entwurf ist keine gespeicherte Freigabe");
    verify(settings, never())
        .save(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.anyString());
  }

  @ParameterizedTest
  @CsvSource({"de,false", "de,true", "en,false", "en,true"})
  void enablingRequiresCredentialsButDisablingRemainsPossible(
      String language, boolean secretStored) {
    MessageProperties texts =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    ExternalProviderSettings settings = mock(ExternalProviderSettings.class);
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://portal.example.test/");
    when(settings.load("google"))
        .thenReturn(
            new ExternalProviderSettings.Settings(
                "google", "saved-client", secretStored, false, "revision"));
    when(settings.load("microsoft"))
        .thenReturn(
            new ExternalProviderSettings.Settings("microsoft", "", false, false, "revision"));
    AdminExternalLoginSection section = new AdminExternalLoginSection(texts, settings, config);
    section.activate();
    TabSheet tabs = (TabSheet) find(section, "admin-provider-tabs");
    Component form = tabs.getComponent(tabs.getTabAt(0));
    TextField client = (TextField) find(form, "provider-google-client-id");
    PasswordField secret = (PasswordField) find(form, "provider-google-secret");
    Checkbox checkbox = (Checkbox) find(form, "provider-google-enabled");
    assertEquals(secretStored, checkbox.isEnabled());
    client.setValue(" ");
    assertFalse(checkbox.isEnabled());
    secret.setValue("new-example-secret");
    assertFalse(checkbox.isEnabled());
    client.setValue("new-client");
    assertTrue(checkbox.isEnabled());
    secret.clear();
    assertEquals(secretStored, checkbox.isEnabled(), "Ein gespeichertes Secret zählt weiter");
    secret.setValue("new-example-secret");
    checkbox.setValue(true);
    client.clear();
    assertTrue(
        checkbox.isEnabled(), "Ausschalten muss auch bei unvollständigem Entwurf möglich bleiben");
    checkbox.setValue(false);
    assertFalse(checkbox.isEnabled());
    assertEquals(
        texts.getProviderSettingsInactive(),
        ((Badge) find(section, "provider-google-status")).getText());
  }

  private Component find(Component root, String id) {
    return descendants(root)
        .filter(component -> component.getId().filter(id::equals).isPresent())
        .findFirst()
        .orElseThrow();
  }

  private Stream<Component> descendants(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::descendants));
  }
}
