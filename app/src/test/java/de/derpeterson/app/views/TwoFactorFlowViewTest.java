package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
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
import de.derpeterson.app.model.SupportTicketEntity.Type;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
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
import de.derpeterson.app.service.SupportTicketService.UserIdentity;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.OverlayUserIcon;
import de.derpeterson.app.ui.components.RecoveryCodeDownload;
import de.derpeterson.app.ui.components.SupportTicketFilterPanel;
import de.derpeterson.app.ui.components.SupportTicketFilterPanel.Assignment;
import de.derpeterson.app.ui.components.SupportTicketsComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
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
    DeploymentConfiguration deployment = mock(DeploymentConfiguration.class);
    when(deployment.getUrlSafeSchemes()).thenCallRealMethod();
    when(service.getDeploymentConfiguration()).thenReturn(deployment);
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
    OtpField otp = assertInstanceOf(OtpField.class, find(view, "two-factor-code"));
    assertTrue(otp.isAutofocus());
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
  void completedAuthenticatorCodeConfirmsOnceWithoutClickingTheButton(String language) {
    TwoFactorView view = open(language, false);
    ui.add(view);
    OtpField otp = (OtpField) find(view, "two-factor-code");
    for (String partial : List.of("1", "12", "123", "1234", "12345")) {
      otp.setValue(partial);
    }
    verify(authorization, never()).confirmSession(eq(request), anyString());
    otp.setValue("123456");
    verify(authorization, never()).confirmSession(eq(request), anyString());
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    verify(authorization).confirmSession(request, "123456");
    assertTrue(otp.isEmpty());
    assertTrue(find(view, "two-factor-submit").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void completedEnrollmentCodeActivatesOnceWithoutClickingTheButton(String language) {
    UUID enrollmentId = UUID.randomUUID();
    when(enrollment.pending(request))
        .thenReturn(
            new TwoFactorEnrollment.Pending(enrollmentId, Kind.ENROLLMENT, SetupProof.PASSWORD));
    TwoFactorView view = open(language, false);
    ui.add(view);
    OtpField otp = (OtpField) find(view, "two-factor-enrollment-code");
    otp.setValue("12345");
    verify(enrollment, never()).complete(eq(request), eq(enrollmentId), anyString());
    otp.setValue("123456");
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    verify(enrollment).complete(request, enrollmentId, "123456");
    assertTrue(otp.isEmpty());
    assertTrue(find(view, "two-factor-submit").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void incorrectCodeCanBeReenteredAndConfirmedAutomatically(String language) {
    TwoFactorView view = open(language, false);
    ui.add(view);
    OtpField otp = (OtpField) find(view, "two-factor-code");
    doThrow(new BadCredentialsException("Invalid test code"))
        .when(authorization)
        .confirmSession(request, "123456");
    otp.setValue("123456");
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    assertTrue(otp.isInvalid());
    assertTrue(otp.isEmpty());
    otp.setValue("654321");
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "654321"));
    verify(authorization).confirmSession(request, "123456");
    verify(authorization).confirmSession(request, "654321");
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void automaticConfirmationIgnoresDisabledActionsAndStaleCodes(String language) {
    TwoFactorView view = open(language, false);
    ui.add(view);
    OtpField otp = (OtpField) find(view, "two-factor-code");
    Button button = (Button) find(view, "two-factor-submit");
    button.setEnabled(false);
    otp.setValue("123456");
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "123456"));
    button.setEnabled(true);
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, false, "123456"));
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "654321"));
    otp.setValue("12345");
    ComponentUtil.fireEvent(otp, new OtpCompleteEvent(otp, true, "12345"));
    verify(authorization, never()).confirmSession(eq(request), anyString());
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
    assertEquals(
        language.equals("de") ? "1 Anfrage" : "1 request", texts.getSupportRequestCount(1));
    assertEquals(
        language.equals("de") ? "2 Anfragen" : "2 requests", texts.getSupportRequestCount(2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void helpSelectsTheOpenRequestWhenAnOlderRequestIsListedFirst(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket closed =
        new Ticket(
            1L,
            0L,
            "Earlier request",
            Type.RECOVERY,
            Status.CLOSED,
            Instant.EPOCH,
            null,
            null,
            false,
            0,
            false,
            false);
    Ticket active =
        new Ticket(
            2L,
            0L,
            "Current request",
            Type.RECOVERY,
            Status.OPEN,
            Instant.EPOCH,
            null,
            null,
            false,
            0,
            false,
            false);
    when(tickets.list(request, false)).thenReturn(List.of(closed, active));

    RecoveryHelpView help = new RecoveryHelpView(config, texts, tickets, request);

    Grid<?> grid = (Grid<?>) find(help, "support-ticket-grid");
    assertEquals(
        texts.getSupportRequestCount(2),
        find(help, "support-request-count").getElement().getText());
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

  @Test
  void closingAnOpenRequestElsewhereDoesNotTurnItsReplyDraftIntoANewRequest() {
    open("de", false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket active =
        new Ticket(
            1L,
            0L,
            "Request",
            Type.RECOVERY,
            Status.OPEN,
            Instant.EPOCH,
            null,
            null,
            false,
            0,
            false,
            false);
    Ticket closed =
        new Ticket(
            1L,
            1L,
            "Request",
            Type.RECOVERY,
            Status.CLOSED,
            Instant.EPOCH,
            null,
            null,
            false,
            0,
            false,
            false);
    when(tickets.list(request, false)).thenReturn(List.of(active));
    RecoveryHelpView help = new RecoveryHelpView(config, texts, tickets, request);
    TextArea draft = (TextArea) find(help, "support-reply");
    draft.setValue("Entwurf für die offene Anfrage.");

    Button refresh = (Button) find(help, "support-reload");
    refresh.click();
    assertEquals("Entwurf für die offene Anfrage.", draft.getValue());

    when(tickets.list(request, false)).thenReturn(List.of(closed));
    refresh.click();
    assertTrue(find(help, "support-create").isVisible());
    assertTrue(draft.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void ticketSelectionHintUsesTheListIconAndTheSharedEmptyStyle(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    when(tickets.list(request, true)).thenReturn(List.of());
    SupportTicketsComponent component = new SupportTicketsComponent(texts, tickets, request, true);
    VerticalLayout hint =
        assertInstanceOf(VerticalLayout.class, find(component, "support-select-ticket"));
    assertTrue(hint.hasClassName("cosmos-notification-empty"));
    assertTrue(hint.isVisible());
    assertEquals(
        texts.getSupportNoTicketSelected() + texts.getSupportSelectTicket(),
        hint.getElement().getTextRecursively());
    HorizontalLayout iconFrame = assertInstanceOf(HorizontalLayout.class, hint.getComponentAt(0));
    SvgIcon icon = assertInstanceOf(SvgIcon.class, iconFrame.getComponentAt(0));
    assertEquals(AppIcon.LIST.getSource(), icon.getSrc());
    assertEquals(
        texts.getSupportCases(),
        find(component, "support-ticket-grid").getElement().getAttribute("aria-label"));
    Tabs categories = (Tabs) find(component, "support-ticket-categories");
    categories.setSelectedIndex(1);
    icon = assertInstanceOf(SvgIcon.class, iconFrame.getComponentAt(0));
    assertEquals(AppIcon.LIST.getSource(), icon.getSrc());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  @SuppressWarnings("unchecked")
  void ticketFiltersApplyTogetherAndDiscardCancelledDrafts(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    List<Ticket> values =
        List.of(
            filterTicket(1L, Status.IN_PROGRESS, 9L),
            filterTicket(2L, Status.OPEN, null),
            filterTicket(3L, Status.CLOSED, 10L));
    when(tickets.list(request, true)).thenReturn(values);
    when(tickets.administratorId(request)).thenReturn(9L);
    SupportTicketsComponent component = new SupportTicketsComponent(texts, tickets, request, true);
    Grid<Ticket> grid = (Grid<Ticket>) find(component, "support-ticket-grid");
    SupportTicketFilterPanel filters =
        (SupportTicketFilterPanel) find(component, "support-filter-panel");
    CheckboxGroup<Status> statuses = (CheckboxGroup<Status>) find(filters, "support-filter-status");
    RadioButtonGroup<Assignment> assignment =
        (RadioButtonGroup<Assignment>) find(filters, "support-filter-assignment");
    Button button = (Button) find(component, "support-filter");
    button.click();
    statuses.setValue(Set.of(Status.IN_PROGRESS, Status.CLOSED));
    assignment.setValue(Assignment.MINE);
    assertEquals(3, grid.getListDataView().getItemCount());
    filterAction(filters, "support-filter-cancel").click();
    button.click();
    assertTrue(statuses.isEmpty());
    assertEquals(Assignment.ALL, assignment.getValue());
    statuses.setValue(Set.of(Status.IN_PROGRESS, Status.CLOSED));
    assignment.setValue(Assignment.MINE);
    filterAction(filters, "support-filter-apply").click();
    assertEquals(List.of(1L), grid.getListDataView().getItems().map(Ticket::id).toList());
    assertEquals(
        "2", find(component, "support-active-filter-count").getElement().getTextRecursively());
    ((Button) find(component, "support-reload")).click();
    assertEquals(List.of(1L), grid.getListDataView().getItems().map(Ticket::id).toList());
    button.click();
    ((Button) find(filters, "support-filter-reset")).click();
    assertEquals(1, grid.getListDataView().getItemCount());
    filterAction(filters, "support-filter-apply").click();
    assertEquals(3, grid.getListDataView().getItemCount());
    assertFalse(find(component, "support-active-filter-count").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  @SuppressWarnings("unchecked")
  void unassignedFilterUpdatesOnRefreshAndTicketLinksCanOpenHiddenCases(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket mine = filterTicket(1L, Status.IN_PROGRESS, 9L);
    Ticket unassigned = filterTicket(2L, Status.OPEN, null);
    when(tickets.list(request, true)).thenReturn(List.of(mine, unassigned));
    when(tickets.administratorId(request)).thenReturn(9L);
    SupportTicketsComponent component = new SupportTicketsComponent(texts, tickets, request, true);
    Grid<Ticket> grid = (Grid<Ticket>) find(component, "support-ticket-grid");
    SupportTicketFilterPanel filters =
        (SupportTicketFilterPanel) find(component, "support-filter-panel");
    ((Button) find(component, "support-filter")).click();
    ((CheckboxGroup<Status>) find(filters, "support-filter-status")).setValue(Set.of(Status.OPEN));
    ((RadioButtonGroup<Assignment>) find(filters, "support-filter-assignment"))
        .setValue(Assignment.UNASSIGNED);
    filterAction(filters, "support-filter-apply").click();
    assertEquals(List.of(2L), grid.getListDataView().getItems().map(Ticket::id).toList());
    when(tickets.list(request, true))
        .thenReturn(List.of(mine, filterTicket(2L, Status.IN_PROGRESS, 10L)));
    component.refresh();
    assertEquals(0, grid.getListDataView().getItemCount());
    assertTrue(component.getElement().getTextRecursively().contains(texts.getSupportFilterEmpty()));
    component.openTicket(SupportTicketService.ticketNumber(mine.id()));
    assertEquals(mine, grid.asSingleSelect().getValue());
    assertEquals(2, grid.getListDataView().getItemCount());
    assertFalse(find(component, "support-active-filter-count").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  @SuppressWarnings("unchecked")
  void filteringAnOpenTicketPreservesItsReplyDraft(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket mine = filterTicket(1L, Status.IN_PROGRESS, 9L);
    when(tickets.list(request, true))
        .thenReturn(List.of(mine, filterTicket(2L, Status.OPEN, null)));
    when(tickets.administratorId(request)).thenReturn(9L);
    SupportTicketsComponent component = new SupportTicketsComponent(texts, tickets, request, true);
    Grid<Ticket> grid = (Grid<Ticket>) find(component, "support-ticket-grid");
    grid.asSingleSelect().setValue(mine);
    component.refresh();
    TextArea draft = (TextArea) find(component, "support-reply");
    draft.setValue("Diese Antwort ist noch ein Entwurf.");
    SupportTicketFilterPanel filters =
        (SupportTicketFilterPanel) find(component, "support-filter-panel");
    ((Button) find(component, "support-filter")).click();
    ((CheckboxGroup<Status>) find(filters, "support-filter-status")).setValue(Set.of(Status.OPEN));
    filterAction(filters, "support-filter-apply").click();
    assertTrue(find(component, "support-select-ticket").isVisible());
    component.openTicket(SupportTicketService.ticketNumber(mine.id()));
    assertEquals("Diese Antwort ist noch ein Entwurf.", draft.getValue());
  }

  private Ticket filterTicket(Long id, Status status, Long assignee) {
    UserIdentity identity =
        assignee == null ? null : new UserIdentity("Alex", "Beispiel", "alex@example.test");
    return new Ticket(
        id,
        0L,
        "Request",
        Type.RECOVERY,
        status,
        Instant.EPOCH,
        assignee,
        identity,
        false,
        0,
        false,
        false);
  }

  private Button filterAction(SupportTicketFilterPanel panel, String id) {
    Component actions = panel.getFooter().getElement().getChild(0).getComponent().orElseThrow();
    return (Button) find(actions, id);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void administrationShowsOwnAssignmentOrTheAssignedAdministratorsName(String language) {
    open(language, false);
    SupportTicketService tickets = mock(SupportTicketService.class);
    Ticket assigned =
        new Ticket(
            1L,
            0L,
            "Request",
            Type.RECOVERY,
            Status.IN_PROGRESS,
            Instant.EPOCH,
            9L,
            new UserIdentity("Alex", "Beispiel", "alex@example.test"),
            false,
            0,
            false,
            false);
    when(tickets.list(request, true)).thenReturn(List.of(assigned));
    when(tickets.administratorId(request)).thenReturn(9L);
    SupportTicketsComponent component = new SupportTicketsComponent(texts, tickets, request, true);
    Component pageHeader = find(component, "support-page-header");
    assertTrue(pageHeader.hasClassName("cosmos-section-header"));
    String headingText = pageHeader.getElement().getTextRecursively();
    assertTrue(headingText.contains(texts.getAdminHeaderTitle() + " / " + texts.getSupportTitle()));
    assertTrue(headingText.contains(texts.getSupportPageDescription()));
    assertFalse(headingText.contains(texts.getSupportInboxDescription()));
    @SuppressWarnings("unchecked")
    Grid<Ticket> grid = (Grid<Ticket>) find(component, "support-ticket-grid");
    grid.asSingleSelect().setValue(assigned);
    component.refresh();
    @SuppressWarnings("unchecked")
    ComponentRenderer<Component, Ticket> renderer =
        (ComponentRenderer<Component, Ticket>) grid.getColumns().getFirst().getRenderer();
    assertTrue(
        renderer
            .createComponent(assigned)
            .getElement()
            .getTextRecursively()
            .contains(texts.getSupportYou()));
    Component detailAssignee = find(component, "support-detail-assignee");
    assertTrue(detailAssignee.getElement().getTextRecursively().contains(texts.getSupportYou()));

    when(tickets.administratorId(request)).thenReturn(10L);
    component.refresh();
    Component otherAssignee = renderer.createComponent(assigned);
    assertFalse(otherAssignee.getElement().getTextRecursively().contains(texts.getSupportYou()));
    OverlayUserIcon avatar =
        tree(otherAssignee)
            .filter(OverlayUserIcon.class::isInstance)
            .map(OverlayUserIcon.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals("Alex Beispiel", avatar.getElement().getAttribute("aria-label"));
    assertFalse(detailAssignee.getElement().getTextRecursively().contains(texts.getSupportYou()));
    assertEquals(
        1,
        tree(detailAssignee)
            .filter(OverlayUserIcon.class::isInstance)
            .filter(part -> "Alex Beispiel".equals(part.getElement().getAttribute("aria-label")))
            .count());
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

  private Stream<Component> tree(Component root) {
    return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::tree));
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
