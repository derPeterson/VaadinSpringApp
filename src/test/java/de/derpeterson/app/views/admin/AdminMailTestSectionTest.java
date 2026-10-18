package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

/** Native Komponenten und Callback-Sperre mit Mocks; kein Browser, Server oder SMTP. */
class AdminMailTestSectionTest {
  private UI ui;
  private ConfigService config;
  private EmailService email;
  private MessageProperties texts;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    var session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(c -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    config = mock(ConfigService.class);
    email = mock(EmailService.class);
    texts = new MessageProperties(new CustomI18NProvider());
  }

  @AfterEach
  void cleanup() {
    ui.getChildren().toList().forEach(ui::remove);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private AdminMailTestSection section() {
    var section =
        new AdminMailTestSection(
            texts,
            email,
            new MaintenanceService(config),
            User.withUsername("admin@example.test").password("unused").roles("ADMIN").build());
    section.refreshTexts();
    return section;
  }

  @SuppressWarnings("unchecked")
  private <T> T field(Object component, String name) {
    return (T) ReflectionTestUtils.getField(component, name);
  }

  private Stream<Component> tree(Component component) {
    return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::tree));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceLocksEntirePanelAndReopeningPreservesDraft(String language) {
    locale.set(Locale.forLanguageTag(language));
    var section = section();
    TextField subject = field(section, "testSubjectField");
    TextArea body = field(section, "testBodyField");
    Button send = field(section, "sendTestMailButton");
    VerticalLayout panel = field(section, "mailLayout");
    Div badge = field(section, "deliveryMeta");
    subject.setValue("Draft subject");
    body.setValue("Draft body");
    assertTrue(panel.isEnabled());
    assertEquals("open", badge.getElement().getAttribute("data-state"));
    assertEquals(texts.getAdminMailCardBadge(), ((Span) field(section, "mailCardBadge")).getText());
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    section.refreshDeliveryState();
    assertFalse(panel.isEnabled());
    assertEquals("true", panel.getElement().getAttribute("aria-disabled"));
    assertFalse(((EmailField) field(section, "testRecipientField")).isEnabled());
    assertFalse(subject.isEnabled());
    assertFalse(body.isEnabled());
    assertFalse(send.isEnabled());
    assertTrue(
        tree(section)
            .filter(ConfigInfoButton.class::isInstance)
            .map(ConfigInfoButton.class::cast)
            .noneMatch(ConfigInfoButton::isEnabled));
    assertEquals("active", badge.getElement().getAttribute("data-state"));
    assertTrue(
        badge.getElement().getAttribute("aria-label").contains(texts.getMaintenanceStatusActive()));
    assertTrue(section.isEnabled());
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    section.refreshDeliveryState();
    assertTrue(panel.isEnabled());
    assertTrue(send.isEnabled());
    assertEquals("false", panel.getElement().getAttribute("aria-disabled"));
    assertEquals("open", badge.getElement().getAttribute("data-state"));
    assertEquals("Draft subject", subject.getValue());
    assertEquals("Draft body", body.getValue());
    send.click();
    verify(email).sendAdminEmail("admin@example.test", "Draft subject", "Draft body");
  }

  private void block(MaintenanceService.State state) {
    if (state == MaintenanceService.State.ACTIVE) {
      when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    } else {
      when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
          .thenThrow(new IllegalStateException("fixture unavailable"));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void sendingChecksFreshMaintenanceBeforeNextPoll(MaintenanceService.State state) {
    var section = section();
    Button send = field(section, "sendTestMailButton");
    assertTrue(send.isEnabled());
    block(state);
    send.click();
    assertFalse(send.isEnabled());
    assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
    assertEquals(
        state.name().toLowerCase(Locale.ROOT),
        ((Div) field(section, "deliveryMeta")).getElement().getAttribute("data-state"));
    verifyNoInteractions(email);
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void initiallyBlockedStateNeverEnablesPanel(MaintenanceService.State state) {
    block(state);
    var section = section();
    assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
    assertFalse(((Button) field(section, "sendTestMailButton")).isEnabled());
    verifyNoInteractions(email);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1000, 12000})
  void pollingRefreshesStateAndDetachingRestoresPreviousInterval(int previousInterval) {
    ui.setPollInterval(previousInterval);
    int[] unrelatedPolls = {0};
    var unrelated = ui.addPollListener(e -> unrelatedPolls[0]++);
    try {
      var section = section();
      ui.add(section);
      assertEquals(
          previousInterval > 0 ? Math.min(previousInterval, 5000) : 5000, ui.getPollInterval());
      when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
      ComponentUtil.fireEvent(ui, new PollEvent(ui, true));
      assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
      assertEquals(1, unrelatedPolls[0]);
      ui.remove(section);
      assertEquals(previousInterval, ui.getPollInterval());
      clearInvocations(config);
      ComponentUtil.fireEvent(ui, new PollEvent(ui, true));
      assertEquals(2, unrelatedPolls[0]);
      verifyNoInteractions(config);
      verifyNoInteractions(email);
    } finally {
      unrelated.remove();
    }
  }
}
