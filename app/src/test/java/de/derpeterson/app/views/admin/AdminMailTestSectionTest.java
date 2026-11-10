package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import java.util.Locale;
import java.util.Set;
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
  private UI testUi;
  private ConfigService configService;
  private EmailService emailService;
  private MessageProperties messageProperties;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(c -> localeReference.get());
    VaadinService service = mock(VaadinService.class);
    DeploymentConfiguration deployment = mock(DeploymentConfiguration.class);
    when(deployment.getUrlSafeSchemes()).thenReturn(Set.of("http", "https"));
    when(service.getDeploymentConfiguration()).thenReturn(deployment);
    when(session.getService()).thenReturn(service);
    VaadinSession.setCurrent(session);
    testUi.getInternals().setSession(session);
    configService = mock(ConfigService.class);
    emailService = mock(EmailService.class);
    messageProperties = new MessageProperties(new CustomI18NProvider());
  }

  @AfterEach
  void cleanup() {
    testUi.getChildren().toList().forEach(testUi::remove);
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private AdminMailTestSection section() {
    AdminMailTestSection section =
        new AdminMailTestSection(
            messageProperties,
            emailService,
            new MaintenanceService(configService),
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
  void smtpSettingsUseANavigationLinkAndFollowLanguageChanges(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminMailTestSection section = section();
    Anchor link = field(section, "smtpLink");
    TextArea body = field(section, "testBodyField");
    Span bodyHint = field(section, "mailBodyHint");
    assertEquals("admin?section=configuration&group=smtp", link.getHref());
    assertEquals(
        messageProperties.getAdminConfigGroupSmtp(), link.getElement().getTextRecursively());
    assertEquals(messageProperties.getAdminMailBodyHint(), bodyHint.getText());
    assertEquals(body.getHelperText(), bodyHint.getText());
    assertEquals("true", bodyHint.getElement().getAttribute("aria-hidden"));
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(
        messageProperties.getAdminConfigGroupSmtp(), link.getElement().getTextRecursively());
    assertEquals(messageProperties.getAdminMailBodyHint(), bodyHint.getText());
    assertEquals(body.getHelperText(), bodyHint.getText());
    verifyNoInteractions(emailService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void openFieldHelpFollowsLanguageChangesWithoutSending(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminMailTestSection section = section();
    ConfigInfoButton info = field(section, "bodyInfoButton");
    AdminSidePanel help = field(section, "helpPanel");
    TextArea body = field(section, "testBodyField");
    body.setValue("Draft body");
    info.click();
    assertTrue(help.isOpened());
    assertTrue(
        help.getElement()
            .getTextRecursively()
            .contains(messageProperties.getAdminMailValidationBody()));
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertTrue(help.isOpened());
    assertTrue(
        help.getElement()
            .getTextRecursively()
            .contains(messageProperties.getAdminMailValidationBody()));
    assertEquals("Draft body", body.getValue());
    verifyNoInteractions(emailService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void maintenanceLocksEntirePanelAndReopeningPreservesDraft(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminMailTestSection section = section();
    TextField subject = field(section, "testSubjectField");
    TextArea body = field(section, "testBodyField");
    Button send = field(section, "sendTestMailButton");
    VerticalLayout panel = field(section, "mailLayout");
    Div badge = field(section, "deliveryMeta");
    subject.setValue("Draft subject");
    body.setValue("Draft body");
    assertTrue(panel.isEnabled());
    assertEquals("open", badge.getElement().getAttribute("data-state"));
    com.vaadin.flow.component.shared.Tooltip statusHint = field(section, "deliveryTooltip");
    assertEquals(
        messageProperties.getAdminMailCardBadge()
            + ": "
            + messageProperties.getMaintenanceStatusAvailable(),
        statusHint.getText());
    assertEquals(
        messageProperties.getAdminMailCardBadge(),
        ((Span) field(section, "mailCardBadge")).getText());
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
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
        badge
            .getElement()
            .getAttribute("aria-label")
            .contains(messageProperties.getMaintenanceStatusActive()));
    assertEquals(
        messageProperties.getAdminMailCardBadge()
            + ": "
            + messageProperties.getMaintenanceStatusActive(),
        statusHint.getText());
    assertTrue(section.isEnabled());
    when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(false);
    section.refreshDeliveryState();
    assertTrue(panel.isEnabled());
    assertTrue(send.isEnabled());
    assertEquals("false", panel.getElement().getAttribute("aria-disabled"));
    assertEquals("open", badge.getElement().getAttribute("data-state"));
    assertEquals("Draft subject", subject.getValue());
    assertEquals("Draft body", body.getValue());
    assertEquals(
        messageProperties.getAdminMailCardBadge()
            + ": "
            + messageProperties.getMaintenanceStatusAvailable(),
        statusHint.getText());
    send.click();
    verify(emailService).sendAdminEmail("admin@example.test", "Draft subject", "Draft body");
  }

  private void block(MaintenanceService.State state) {
    if (state == MaintenanceService.State.ACTIVE) {
      when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    } else {
      when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE))
          .thenThrow(new IllegalStateException("fixture unavailable"));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void sendingChecksFreshMaintenanceBeforeNextPoll(MaintenanceService.State state) {
    AdminMailTestSection section = section();
    Button send = field(section, "sendTestMailButton");
    assertTrue(send.isEnabled());
    block(state);
    send.click();
    assertFalse(send.isEnabled());
    assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
    assertEquals(
        state.name().toLowerCase(Locale.ROOT),
        ((Div) field(section, "deliveryMeta")).getElement().getAttribute("data-state"));
    verifyNoInteractions(emailService);
  }

  @ParameterizedTest
  @EnumSource(
      value = MaintenanceService.State.class,
      names = {"ACTIVE", "UNAVAILABLE"})
  void initiallyBlockedStateNeverEnablesPanel(MaintenanceService.State state) {
    block(state);
    AdminMailTestSection section = section();
    assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
    assertFalse(((Button) field(section, "sendTestMailButton")).isEnabled());
    verifyNoInteractions(emailService);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1000, 12000})
  void pollingRefreshesStateAndDetachingRestoresPreviousInterval(int previousInterval) {
    testUi.setPollInterval(previousInterval);
    int[] unrelatedPolls = {0};
    Registration unrelated = testUi.addPollListener(e -> unrelatedPolls[0]++);
    try {
      AdminMailTestSection section = section();
      testUi.add(section);
      assertEquals(
          previousInterval > 0 ? Math.min(previousInterval, 5000) : 5000, testUi.getPollInterval());
      when(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
      assertFalse(((VerticalLayout) field(section, "mailLayout")).isEnabled());
      assertEquals(1, unrelatedPolls[0]);
      testUi.remove(section);
      assertEquals(previousInterval, testUi.getPollInterval());
      clearInvocations(configService);
      ComponentUtil.fireEvent(testUi, new PollEvent(testUi, true));
      assertEquals(2, unrelatedPolls[0]);
      verifyNoInteractions(configService);
      verifyNoInteractions(emailService);
    } finally {
      unrelated.remove();
    }
  }
}
