package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.views.admin.AdminConfigurationSection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class CosmosServiceStatusComponentTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private final CustomI18NProvider provider = new CustomI18NProvider();
  private UI ui;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    when(session.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(session);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  static Stream<Arguments> statesAndLanguages() {
    return Stream.of(Locale.GERMAN, Locale.ENGLISH)
        .flatMap(
            language ->
                Stream.of(MaintenanceService.State.values())
                    .map(state -> Arguments.of(language, state)));
  }

  @ParameterizedTest
  @MethodSource("statesAndLanguages")
  void statusUsesTranslatedTextAndDoesNotClaimReadinessOnReadFailure(
      Locale language, MaintenanceService.State state) {
    locale.set(language);
    var maintenance = mock(MaintenanceService.class);
    when(maintenance.state()).thenReturn(state);
    var texts = new MessageProperties(provider);
    var status = new CosmosServiceStatusComponent(maintenance, texts);
    var expectedTitle =
        switch (state) {
          case OPEN -> texts.getMaintenanceStatusOnline();
          case ACTIVE -> texts.getMaintenanceStatusActive();
          case UNAVAILABLE -> texts.getMaintenanceStatusUnavailable();
        };
    assertTrue(status.getElement().getTextRecursively().contains(expectedTitle));
    assertTrue(
        status
            .getElement()
            .getTextRecursively()
            .contains(
                state == MaintenanceService.State.OPEN
                    ? texts.getMaintenanceServicesReady()
                    : texts.getMaintenanceServicesRestricted()));
    assertEquals(
        state.name().toLowerCase(Locale.ROOT), status.getElement().getAttribute("data-state"));
    assertEquals("status", status.getElement().getAttribute("role"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void languageRefreshPreservesTheCurrentMaintenanceState(String language) {
    var maintenance = mock(MaintenanceService.class);
    when(maintenance.state()).thenReturn(MaintenanceService.State.ACTIVE);
    var status = new CosmosServiceStatusComponent(maintenance, new MessageProperties(provider));
    locale.set(Locale.forLanguageTag(language));
    status.refresh();
    var texts = MessageProperties.forLocale(provider, locale.get());
    assertTrue(
        status.getElement().getTextRecursively().contains(texts.getMaintenanceStatusActive()));
    assertTrue(
        status
            .getElement()
            .getTextRecursively()
            .contains(texts.getMaintenanceServicesRestricted()));
    assertEquals("active", status.getElement().getAttribute("data-state"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void savingTheActualMaintenanceCheckboxRefreshesStatusInBothDirections(String language) {
    locale.set(Locale.forLanguageTag(language));
    var saved = new EnumMap<ConfigEntry, String>(ConfigEntry.class);
    for (var entry : ConfigEntry.values()) saved.put(entry, entry.getDefaultValueString());
    var config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> saved.get(call.getArgument(0)));
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenAnswer(call -> Boolean.parseBoolean(saved.get(ConfigEntry.MAINTENANCE_MODE)));
    doAnswer(
            call -> {
              saved.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(config)
        .set(any(ConfigEntry.class), anyString());
    var texts = new MessageProperties(provider);
    var status = new CosmosServiceStatusComponent(new MaintenanceService(config), texts);
    var form = new AdminConfigurationSection(texts, config, status::refresh);
    ui.add(form, status);
    form.showGroup(AdminConfigGroup.SECURITY);
    var fields = (Map<?, ?>) ReflectionTestUtils.getField(form, "configInputs");
    var checkbox = (Checkbox) fields.get(ConfigEntry.MAINTENANCE_MODE);
    var save = (Button) ReflectionTestUtils.getField(form, "saveConfigButton");
    for (boolean enabled : new boolean[] {true, false}) {
      checkbox.setValue(enabled);
      save.click();
      assertEquals(Boolean.toString(enabled), saved.get(ConfigEntry.MAINTENANCE_MODE));
      assertEquals(enabled ? "active" : "open", status.getElement().getAttribute("data-state"));
      assertTrue(
          status
              .getElement()
              .getTextRecursively()
              .contains(
                  enabled
                      ? texts.getMaintenanceServicesRestricted()
                      : texts.getMaintenanceServicesReady()));
    }
  }
}
