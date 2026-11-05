package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.RecoveryCodeDownload;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.User;

class TwoFactorFlowViewTest {
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final SecurityService security = mock(SecurityService.class);
  private final TwoFactorAuthorization authorization = mock(TwoFactorAuthorization.class);
  private final TwoFactorEnrollment enrollment = mock(TwoFactorEnrollment.class);
  private final ConfigService config = mock(ConfigService.class);
  private MessageProperties texts;
  private UI ui;
  private VaadinSession session;
  private VaadinService service;

  private TwoFactorView open(String language, boolean recovering) {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    service = mock(VaadinService.class);
    when(service.getDeploymentConfiguration()).thenReturn(mock(DeploymentConfiguration.class));
    when(session.getService()).thenReturn(service);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.forLanguageTag(language));
    VaadinSession.setCurrent(session);
    VaadinService.setCurrent(service);
    ui.getInternals().setSession(session);
    texts = MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    when(config.getString(any(ConfigEntry.class))).thenReturn("Testportal");
    when(request.getContextPath()).thenReturn("");
    when(security.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(
                User.withUsername("flow@example.test").password("unused").roles("USER").build()));
    TwoFactorAuthorization.Owner owner =
        new TwoFactorAuthorization.Owner(
            1L, "flow@example.test", "current", true, true, List.of(RoleType.ROLE_USER));
    when(authorization.owner(request)).thenReturn(owner);
    when(authorization.state(owner))
        .thenReturn(
            new TwoFactorAuthorization.State(true, false, false, UUID.randomUUID(), Instant.now()));
    when(enrollment.available()).thenReturn(true);
    TwoFactorView view =
        new TwoFactorView(
            config,
            texts,
            security,
            authorization,
            enrollment,
            mock(ExternalLoginFlow.class),
            mock(ExternalIdentityService.class),
            mock(RecoveryCodeDownload.class),
            request);
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getLocation())
        .thenReturn(new Location("two-factor" + (recovering ? "?recover" : "")));
    view.beforeEnter(event);
    return view;
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    VaadinService.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void confirmationUsesNativeOtpAndKeepsTheRecoveryCodeWhole(String language) {
    TwoFactorView view = open(language, false);
    assertInstanceOf(OtpField.class, find(view, "two-factor-code"));
    Details alternatives = assertInstanceOf(Details.class, find(view, "two-factor-login-help"));
    assertFalse(alternatives.isOpened());
    assertEquals(2, alternatives.getContent().findFirst().orElseThrow().getChildren().count());
    Button toggle = assertInstanceOf(Button.class, find(view, "two-factor-login-help-toggle"));
    assertEquals(AppIcon.ANGLE_RIGHT.getSource(), ((SvgIcon) toggle.getIcon()).getSrc());
    toggle.click();
    assertTrue(alternatives.isOpened());
    assertEquals("true", toggle.getElement().getAttribute("aria-expanded"));
    assertEquals(AppIcon.ANGLE_DOWN.getSource(), ((SvgIcon) toggle.getIcon()).getSrc());
    toggle.click();
    assertFalse(alternatives.isOpened());
    assertEquals("false", toggle.getElement().getAttribute("aria-expanded"));
    assertEquals(AppIcon.ANGLE_RIGHT.getSource(), ((SvgIcon) toggle.getIcon()).getSrc());
    Tabs methods =
        find(view, "two-factor-authenticator-tab").getParent().map(Tabs.class::cast).orElseThrow();
    methods.setSelectedIndex(1);
    TextField recovery = (TextField) find(view, "two-factor-code");
    String value = "12345678-12345678-12345678-12345678";
    recovery.setValue(value);
    ((Button) find(view, "two-factor-submit")).click();
    verify(authorization).confirmSession(request, value);
    verify(enrollment, never()).beginRecovery(eq(request), anyString());
    assertEquals("", recovery.getValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void authenticatorMethodConfirmsTheSessionWithTheOtp(String language) {
    TwoFactorView view = open(language, false);
    ((OtpField) find(view, "two-factor-code")).setValue("123456");
    ((Button) find(view, "two-factor-submit")).click();
    verify(authorization).confirmSession(request, "123456");
    verify(enrollment, never()).beginRecovery(eq(request), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void replacementUsesItsOwnProofAndKeepsTheBackActionInTheFooter(String language) {
    TwoFactorView view = open(language, true);
    String value = "12345678-12345678-12345678-12345678";
    ((TextField) find(view, "two-factor-code")).setValue(value);
    assertInstanceOf(
        HorizontalLayout.class, find(view, "two-factor-recovery-back").getParent().orElseThrow());
    ((Button) find(view, "two-factor-submit")).click();
    verify(enrollment).beginRecovery(request, value);
    verify(authorization, never()).confirmSession(eq(request), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void helpKeepsTheExistingTicketListAndUsesNativeFooterActions(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    when(tickets.list(request, false)).thenReturn(List.of());
    RecoveryHelpView help = new RecoveryHelpView(config, texts, tickets, request);
    assertTrue(find(help, "support-no-unlock").isVisible());
    assertInstanceOf(HorizontalLayout.class, find(help, "support-back").getParent().orElseThrow());
    assertTrue(find(help, "support-create") instanceof Button);
    assertTrue(find(help, "support-reply").getElement().getProperty("required", false));
    assertEquals(
        language.equals("de") ? "1 Nachricht" : "1 message", texts.getSupportMessageCount(1));
    assertEquals(
        language.equals("de") ? "2 Nachrichten" : "2 messages", texts.getSupportMessageCount(2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void helpSelectsTheOpenRequestWhenAnOlderRequestIsListedFirst(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket closed =
        new Ticket(1L, 0L, "Earlier request", Status.CLOSED, Instant.EPOCH, null, false);
    Ticket active = new Ticket(2L, 0L, "Current request", Status.OPEN, Instant.EPOCH, null, false);
    when(tickets.list(request, false)).thenReturn(List.of(closed, active));

    RecoveryHelpView help = new RecoveryHelpView(config, texts, tickets, request);

    Grid<?> grid = (Grid<?>) find(help, "support-ticket-grid");
    assertEquals(List.of(active), List.copyOf(grid.getSelectedItems()));
    @SuppressWarnings("unchecked")
    ComponentRenderer<Component, Ticket> renderer =
        (ComponentRenderer<Component, Ticket>) grid.getColumns().getFirst().getRenderer();
    assertTrue(
        renderer
            .createComponent(active)
            .getElement()
            .getTextRecursively()
            .contains(
                language.equals("de")
                    ? "Hilfe beim Authenticator-Zugriff"
                    : "Help accessing your authenticator"));
    assertTrue(find(help, "support-add-reply").isVisible());
    assertFalse(find(help, "support-create").isVisible());
    verify(tickets, never()).conversation(request, closed.id(), false);
  }

  private Component find(Component root, String id) {
    if (root.getId().filter(id::equals).isPresent()) {
      return root;
    }
    return root.getChildren()
        .map(child -> findOrNull(child, id))
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElseThrow();
  }

  private Component findOrNull(Component root, String id) {
    if (root.getId().filter(id::equals).isPresent()) {
      return root;
    }
    return root.getChildren()
        .map(child -> findOrNull(child, id))
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
