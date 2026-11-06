package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportReplyEntity.Kind;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.SupportTicketEntity.Type;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.LiveView;
import de.derpeterson.app.service.SupportTicketService.Reply;
import de.derpeterson.app.service.SupportTicketService.ReplyAuthor;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import de.derpeterson.app.service.SupportTicketService.UserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

/**
 * Native gemeinsame Vorgangsansicht; die Dienstgrenzen unterscheiden Eigenzugang und Bearbeitung.
 */
@StyleSheet("custom-theme/cosmos-mfa.css")
@StyleSheet("custom-theme/cosmos-mfa-confirmation.css")
@StyleSheet("custom-theme/cosmos-provider-settings.css")
@Slf4j
public final class SupportTicketsComponent extends VerticalLayout {
  public enum Category {
    ALL,
    RECOVERY
  }

  public record NavigationState(Category category, String ticketNumber) implements Serializable {}

  private record DraftTarget(Long ticketId) implements Serializable {}

  private record Draft(String text, boolean internal) implements Serializable {}

  private static final DraftTarget NEW_REQUEST_DRAFT = new DraftTarget(null);

  private final transient MessageProperties texts;
  private final transient SupportTicketService service;
  private final transient HttpServletRequest request;
  private final boolean administration;
  private final Grid<Ticket> grid = new Grid<>();
  private final Scroller adminTicketScroll = new Scroller();
  private final Span allTicketsLabel = new Span();
  private final Span recoveryTicketsLabel = new Span();
  private final Tab allTickets = new Tab(AppIcon.MESSAGES.create(), allTicketsLabel);
  private final Tab recoveryTickets = new Tab(AppIcon.LOCK_QUESTION.create(), recoveryTicketsLabel);
  private final Tabs ticketCategories = new Tabs(allTickets, recoveryTickets);
  private final H3 queueHeading = new H3();
  private final Span adminBreadcrumb = new Span();
  private final Span adminPageTitle = new Span();
  private final Span adminPageDescription = new Span();
  private final H3 noSelectionTitle = new H3();
  private final Span selectTicket = new Span();
  private final VerticalLayout noSelection = new VerticalLayout();
  private final CosmosCardComponent adminDetail = new CosmosCardComponent();
  private final Div adminWorkspace = new Div();
  private final VerticalLayout adminQueue = new VerticalLayout();
  private final Button focusToggle = new Button();
  private final HorizontalLayout adminTicketTools = new HorizontalLayout();
  private final HorizontalLayout adminActionFooter = new HorizontalLayout();
  private final VerticalLayout adminEditor = new VerticalLayout();
  private final Badge adminStatus = new Badge();
  private final Span adminTicketNumber = new Span();
  private final Span adminUpdated = new Span();
  private final H3 adminSubject = new H3();
  private final VerticalLayout adminRequester = new VerticalLayout();
  private final VerticalLayout adminAssignment = new VerticalLayout();
  private final Span adminRecoveryHint = new Span();
  private final Span adminPolicyLabel = new Span();
  private final HorizontalLayout adminRecoveryInfo = new HorizontalLayout();
  private final Span adminStatusNote = new Span();
  private final VerticalLayout conversation = new VerticalLayout();
  private final TextArea body = new TextArea();
  private final Switch internal = new Switch();
  private final Switch subscription = new Switch();
  private final Button create = new Button();
  private final Button claim = new Button();
  private final Button reply = new Button();
  private final Button close = new Button();
  private final Button instructions = new Button();
  private final Button reload = new Button();
  private final Button filterButton = new Button();
  private final Badge activeFilterCount = new Badge();
  private SupportTicketFilterPanel filters;
  private final PortalConfirmationDialog instructionDialog;
  private final AccountPanelComponent panel =
      new AccountPanelComponent("support-panel", AppIcon.INFO_CIRCLE);
  private final Span warning = new Span();
  private final Span userWarningLabel = new Span();
  private final Span emptyText = new Span();
  private final Span userExplanation = new Span();
  private final H3 userRequestsHeading = new H3();
  private final Badge userRequestCount = new Badge();
  private final HorizontalLayout userRequestsHeader =
      new HorizontalLayout(userRequestsHeading, userRequestCount);
  private final H3 userConversationHeading = new H3();
  private final Badge userConversationCount = new Badge();
  private final HorizontalLayout userConversationHeader =
      new HorizontalLayout(userConversationHeading, userConversationCount);
  private final VerticalLayout userMessageThread = new VerticalLayout();
  private final Scroller userMessageScroll =
      new Scroller(userMessageThread, Scroller.ScrollDirection.VERTICAL);
  private final HorizontalLayout emptyState = new HorizontalLayout();
  private final HorizontalLayout backActions = new HorizontalLayout();
  private final VerticalLayout userEditor = new VerticalLayout();
  private final Div messageField = new Div();
  private Ticket selected;
  private List<Ticket> visibleTickets = List.of();
  private final Map<DraftTarget, Draft> drafts = new HashMap<>();
  private final Set<Long> expandedInstructions = new HashSet<>();
  private DraftTarget draftTarget;
  private boolean openRequest;
  private Long administratorId;
  private SerializableConsumer<NavigationState> navigationListener = state -> {};
  private NavigationState reportedNavigation;
  private boolean restoringNavigation;
  private boolean ticketFocused;
  private transient Registration liveRegistration;
  private transient HttpSession liveSession;
  private transient volatile Long liveViewerId;

  public SupportTicketsComponent(
      MessageProperties texts,
      SupportTicketService service,
      HttpServletRequest request,
      boolean administration) {
    this.texts = texts;
    this.service = service;
    this.request = request;
    this.administration = administration;
    instructionDialog = new PortalConfirmationDialog(texts, "support-instruction-preview");
    instructionDialog.addClassName("support-instructions");
    setId(administration ? "admin-support" : "account-support");
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-dialog-form", "support-tickets");
    conversation.setPadding(false);
    conversation.setSpacing(false);
    conversation.setAlignItems(Alignment.STRETCH);
    grid.setId("support-ticket-grid");
    grid.setWidthFull();
    grid.setAllRowsVisible(true);
    grid.addComponentColumn(this::ticketRow);
    grid.asSingleSelect()
        .addValueChangeListener(
            event -> {
              selected = event.getValue();
              if (event.isFromClient()) {
                try {
                  refreshConversation();
                } catch (RuntimeException failure) {
                  failed(texts::getSupportRefreshFailed);
                }
                publishNavigation();
              }
            });
    body.setId("support-reply");
    body.setWidthFull();
    body.setMinRows(4);
    body.addClassName("support-message-input");
    body.setMaxLength(SupportTicketService.MESSAGE_LIMIT);
    body.setRequiredIndicatorVisible(true);
    body.setManualValidation(true);
    body.setValueChangeMode(ValueChangeMode.EAGER);
    Span messageCount = new Span("0 / " + body.getMaxLength());
    messageCount.addClassName("cosmos-notification-compose-counter");
    messageCount.getElement().setAttribute("aria-hidden", "true");
    body.addValueChangeListener(
        event -> {
          body.setInvalid(false);
          messageCount.setText(event.getValue().length() + " / " + body.getMaxLength());
        });
    messageField.add(body, messageCount);
    messageField.addClassName("cosmos-notification-compose-field");
    internal.setId("support-internal-note");
    internal.setVisible(administration);
    if (administration) {
      internal.addValueChangeListener(event -> updateAdminComposerLabels());
    }
    subscription.setId("support-subscription");
    subscription.setVisible(false);
    subscription.addValueChangeListener(
        event -> {
          if (event.isFromClient() && selected != null) {
            try {
              service.setSubscription(request, selected.id(), event.getValue());
              refresh();
            } catch (RuntimeException failure) {
              subscription.setValue(selected.subscribed());
              handleActionFailure(failure);
            }
          }
        });
    create.setId("support-create");
    create.setPrefixComponent(AppIcon.PLUS.create());
    create.addClickListener(
        event -> {
          if (ValidationHelper.validateRequiredInputs(List.of(body), texts)) {
            perform(
                () -> service.createRecovery(request, body.getValue()),
                texts::getSupportCreated,
                true);
          }
        });
    claim.setId("support-claim");
    claim.setPrefixComponent(AppIcon.CHECK.create());
    claim.addClickListener(
        event ->
            perform(
                () -> service.claim(request, selected.id(), selected.version()),
                texts::getSupportSaved,
                false));
    reply.setId("support-add-reply");
    reply.setPrefixComponent(AppIcon.PAPERPLANE.create());
    reply.addClickListener(
        event -> {
          if (ValidationHelper.validateRequiredInputs(List.of(body), texts)) {
            boolean privateNote = administration && internal.getValue();
            perform(
                () ->
                    service.reply(
                        request,
                        selected.id(),
                        selected.version(),
                        body.getValue(),
                        privateNote,
                        administration),
                privateNote ? texts::getSupportInternalSaved : texts::getSupportReplySaved,
                true);
          }
        });
    close.setId("support-close");
    close.setPrefixComponent(
        (administration ? AppIcon.CHECK_CHECK : AppIcon.CHECK_CIRCLE).create());
    close.addClickListener(
        event ->
            perform(
                () ->
                    administration
                        ? service.close(request, selected.id(), selected.version())
                        : service.closeOwn(request, selected.id(), selected.version()),
                texts::getSupportSaved,
                true));
    instructions.setId("support-instructions");
    instructions.setPrefixComponent(AppIcon.ENVELOPE.create());
    instructions.addClickListener(event -> previewInstructions());
    reload.setPrefixComponent(AppIcon.REFRESH.create());
    reload.addThemeVariants(ButtonVariant.LUMO_ICON);
    reload.setId("support-reload");
    reload.addClickListener(event -> refresh(true));
    HorizontalLayout sensitiveNotice = sensitiveNotice();
    if (administration) {
      buildAdminWorkspace(sensitiveNotice);
    } else {
      addClassName("support-recovery-help");
      panel.setIcon(AppIcon.LOCK_QUESTION.create());
      conversation.addClassName("support-user-conversation");
      userExplanation.setId("support-no-unlock");
      HorizontalLayout helpNotice =
          new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), userExplanation);
      helpNotice.setPadding(false);
      helpNotice.setSpacing(false);
      helpNotice.setAlignItems(Alignment.CENTER);
      helpNotice.addClassName("support-help-notice");
      emptyState.add(AppIcon.ARCHIVE.create(), emptyText);
      emptyState.setPadding(false);
      emptyState.setSpacing(false);
      emptyState.setWidthFull();
      emptyState.setAlignItems(Alignment.CENTER);
      emptyState.setJustifyContentMode(JustifyContentMode.CENTER);
      emptyState.addClassName("support-empty-state");
      userEditor.setPadding(false);
      userEditor.setSpacing(false);
      userEditor.setWidthFull();
      userEditor.addClassName("support-user-editor");
      userEditor.add(sensitiveNotice, messageField);
      userRequestsHeading.addClassName("support-conversation-heading");
      userConversationHeading.addClassName("support-conversation-heading");
      userRequestCount.setId("support-request-count");
      userRequestCount.addClassNames("support-section-count", "support-request-count");
      userConversationCount.addClassName("support-section-count");
      for (HorizontalLayout header : List.of(userRequestsHeader, userConversationHeader)) {
        header.setWidthFull();
        header.setSpacing(false);
        header.setAlignItems(Alignment.CENTER);
        header.addClassName("support-section-header");
      }
      userRequestsHeader.add(reload);
      userMessageThread.setSpacing(false);
      userMessageThread.addClassName("support-message-thread");
      userMessageScroll.addClassName("support-message-scroll");
      conversation.add(userConversationHeader, userMessageScroll);
      VerticalLayout requests = new VerticalLayout(userRequestsHeader, emptyState, grid);
      requests.setPadding(false);
      requests.setSpacing(false);
      requests.addClassName("support-user-requests");
      VerticalLayout form = new VerticalLayout(helpNotice, requests, conversation, userEditor);
      form.setPadding(false);
      form.setSpacing(false);
      form.setWidthFull();
      form.addClassName("support-user-form");
      panel.getBody().add(form);
      create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      reply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      HorizontalLayout actions = new HorizontalLayout(close, reply, create);
      actions.setPadding(false);
      actions.setSpacing(false);
      actions.addClassName("cosmos-dialog-actions");
      backActions.setPadding(false);
      backActions.setSpacing(false);
      backActions.addClassName("support-back-actions");
      HorizontalLayout footer = new HorizontalLayout(backActions, actions);
      footer.setPadding(false);
      footer.setSpacing(false);
      footer.setWidthFull();
      footer.setAlignItems(Alignment.CENTER);
      footer.addClassName("support-user-footer");
      panel.addActionFooter(footer);
    }
    add(panel, instructionDialog);
    refreshTexts();
  }

  private HorizontalLayout sensitiveNotice() {
    userWarningLabel.addClassName("cosmos-security-warning-label");
    HorizontalLayout notice =
        new HorizontalLayout(AppIcon.ERROR.create(), new Span(userWarningLabel, warning));
    notice.setPadding(false);
    notice.setSpacing(false);
    notice.setAlignItems(Alignment.START);
    notice.addClassName("cosmos-security-warning");
    return notice;
  }

  private void buildAdminWorkspace(HorizontalLayout sensitiveNotice) {
    addClassName("support-admin-workspace");
    panel.getBody().setWidthFull();
    CosmosSectionHeaderComponent heading =
        new CosmosSectionHeaderComponent(adminBreadcrumb, adminPageTitle, adminPageDescription);
    heading.setId("support-page-header");
    add(heading);
    panel.setIcon(AppIcon.MESSAGES.create());
    ticketCategories.setId("support-ticket-categories");
    ticketCategories.addClassName(CosmosClasses.PANEL_TABS);
    ticketCategories.setWidthFull();
    ticketCategories.addSelectedChangeListener(event -> refresh());
    queueHeading.addClassName(CosmosClasses.PANEL_TITLE);
    HorizontalLayout queueIcon = new HorizontalLayout(AppIcon.LIST.create());
    queueIcon.setPadding(false);
    queueIcon.setSpacing(false);
    queueIcon.addClassName(CosmosClasses.PANEL_ICON);
    HorizontalLayout queueHeader = new HorizontalLayout(queueIcon, queueHeading);
    queueHeader.setId("support-queue-header");
    queueHeader.setPadding(false);
    queueHeader.setSpacing(false);
    queueHeader.setWidthFull();
    queueHeader.setAlignItems(Alignment.CENTER);
    queueHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
    queueHeader.setFlexGrow(1, queueHeading);
    queueHeader.addClassName(CosmosClasses.PANEL_HEADER);
    filters = new SupportTicketFilterPanel(texts, this::status, this::refresh);
    add(filters);
    filterButton.setId("support-filter");
    filterButton.getElement().setAttribute("aria-controls", "support-filter-panel");
    filterButton.getElement().setAttribute("aria-haspopup", "dialog");
    filterButton.getElement().setAttribute("aria-expanded", "false");
    filters.addOpenedChangeListener(
        event ->
            filterButton
                .getElement()
                .setAttribute("aria-expanded", Boolean.toString(event.isOpened())));
    filterButton.setPrefixComponent(AppIcon.FILTER.create());
    activeFilterCount.addClassName("support-section-count");
    activeFilterCount.setId("support-active-filter-count");
    activeFilterCount.setVisible(false);
    filterButton.setSuffixComponent(activeFilterCount);
    filterButton.addClickListener(event -> filters.openWithAppliedFilters());
    HorizontalLayout queueTools = new HorizontalLayout(filterButton, reload);
    queueTools.setId("support-queue-tools");
    queueTools.setPadding(false);
    queueTools.setSpacing(false);
    queueTools.setWidthFull();
    queueTools.setAlignItems(Alignment.CENTER);
    queueTools.setJustifyContentMode(JustifyContentMode.END);
    queueTools.addClassNames("support-section-header", "support-admin-list-tools");
    emptyState.add(AppIcon.ARCHIVE.create(), emptyText);
    emptyState.setAlignItems(Alignment.CENTER);
    emptyState.addClassName("support-empty-state");
    grid.addClassName("support-admin-ticket-list");
    adminTicketScroll.setContent(grid);
    adminTicketScroll.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
    adminTicketScroll.setWidthFull();
    adminTicketScroll.addClassName("support-admin-ticket-scroll");
    CosmosCardComponent queuePanel =
        new CosmosCardComponent(queueHeader, queueTools, emptyState, adminTicketScroll);
    queuePanel.setId("support-queue-panel");
    queuePanel.addClassNames(CosmosClasses.PANEL_CARD, "support-admin-list-panel");
    adminQueue.add(queuePanel);
    adminQueue.setPadding(false);
    adminQueue.addClassName("support-admin-queue");

    adminSubject.addClassNames(CosmosClasses.PANEL_TITLE, "support-admin-subject");
    adminTicketNumber.addClassName("support-ticket-number");
    HorizontalLayout metadata = new HorizontalLayout(adminTicketNumber, adminStatus);
    metadata.setPadding(false);
    metadata.setSpacing(false);
    metadata.setAlignItems(Alignment.CENTER);
    metadata.addClassName("support-admin-ticket-metadata");
    adminAssignment.setId("support-detail-assignee");
    adminAssignment.setPadding(false);
    adminAssignment.setSpacing(false);
    adminAssignment.setWidth(null);
    adminAssignment.setAlignItems(Alignment.END);
    adminAssignment.addClassName("support-admin-assignment");
    adminRequester.setId("support-detail-requester");
    adminRequester.setPadding(false);
    adminRequester.setSpacing(false);
    adminRequester.setWidth(null);
    adminRequester.addClassName("support-admin-requester");
    adminUpdated.addClassName("support-ticket-updated");
    HorizontalLayout ticketIcon = new HorizontalLayout(AppIcon.LOCK_QUESTION.create());
    ticketIcon.setPadding(false);
    ticketIcon.setSpacing(false);
    ticketIcon.addClassName(CosmosClasses.PANEL_ICON);
    focusToggle.setId("support-focus-toggle");
    focusToggle.addThemeVariants(ButtonVariant.LUMO_ICON);
    focusToggle
        .getElement()
        .setAttribute("aria-controls", "support-queue-panel support-ticket-panel");
    focusToggle.addClickListener(event -> setTicketFocused(!ticketFocused));
    HorizontalLayout orientation =
        new HorizontalLayout(ticketIcon, metadata, adminUpdated, focusToggle);
    orientation.setPadding(false);
    orientation.setSpacing(false);
    orientation.setWidthFull();
    orientation.setAlignItems(Alignment.CENTER);
    orientation.setFlexGrow(1, metadata);
    orientation.addClassName("support-admin-ticket-orientation");
    HorizontalLayout people = new HorizontalLayout(adminRequester, adminAssignment);
    people.setPadding(false);
    people.setSpacing(false);
    people.setWidthFull();
    people.setAlignItems(Alignment.STRETCH);
    people.setFlexGrow(1, adminRequester, adminAssignment);
    people.addClassName("support-admin-ticket-people");
    VerticalLayout ticketHeader = new VerticalLayout(orientation, adminSubject, people);
    ticketHeader.setId("support-ticket-header");
    ticketHeader.setPadding(false);
    ticketHeader.setSpacing(false);
    ticketHeader.setWidthFull();
    ticketHeader.setAlignItems(Alignment.STRETCH);
    ticketHeader.addClassNames(CosmosClasses.PANEL_HEADER, "support-admin-ticket-header");
    adminTicketTools.setId("support-ticket-tools");
    adminTicketTools.setPadding(false);
    adminTicketTools.setSpacing(false);
    adminTicketTools.setWidthFull();
    adminTicketTools.setAlignItems(Alignment.CENTER);
    adminTicketTools.addClassName("support-admin-ticket-tools");
    subscription.addClassName("cosmos-config-switch");
    adminTicketTools.add(subscription, claim, instructions);
    adminPolicyLabel.addClassName("support-admin-policy-label");
    Span policy = new Span(adminPolicyLabel, new Text(" "), adminRecoveryHint);
    adminRecoveryInfo.add(AppIcon.INFO_CIRCLE.create(), policy);
    adminRecoveryInfo.setId("support-ticket-policy");
    adminRecoveryInfo.setPadding(false);
    adminRecoveryInfo.setSpacing(false);
    adminRecoveryInfo.setAlignItems(Alignment.CENTER);
    adminRecoveryInfo.addClassName("support-help-notice");
    adminStatusNote.setId("support-ticket-status-note");
    adminStatusNote.addClassName("support-admin-status-note");

    userMessageThread.setSpacing(false);
    userMessageThread.addClassName("support-message-thread");
    userMessageScroll.addClassName("support-message-scroll");
    userConversationHeading.addClassName("support-conversation-heading");
    userConversationCount.addClassName("support-section-count");
    userConversationHeader.setAlignItems(Alignment.CENTER);
    userConversationHeader.setSpacing(false);
    userConversationHeader.addClassName("support-section-header");
    conversation.add(userConversationHeader, userMessageScroll, adminStatusNote);
    reply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    HorizontalLayout actions = new HorizontalLayout(close, reply);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.setWidthFull();
    actions.addClassNames("cosmos-panel-actions", "support-admin-actions");
    adminActionFooter.setId("support-ticket-footer");
    adminActionFooter.setPadding(false);
    adminActionFooter.setSpacing(false);
    adminActionFooter.setWidthFull();
    adminActionFooter.addClassNames("cosmos-panel-footer", "support-admin-detail-footer");
    adminActionFooter.add(actions);
    adminEditor.setPadding(false);
    adminEditor.addClassName("support-admin-editor");
    adminEditor.add(internal, sensitiveNotice, messageField);
    VerticalLayout content = new VerticalLayout(adminRecoveryInfo, conversation, adminEditor);
    content.setPadding(false);
    content.setWidthFull();
    content.addClassName("support-admin-detail-content");
    adminDetail.setId("support-ticket-panel");
    adminDetail.addClassNames(CosmosClasses.PANEL_CARD, "support-admin-detail");
    adminDetail.add(ticketHeader, adminTicketTools, content, adminActionFooter);
    HorizontalLayout selectionIcon = new HorizontalLayout(AppIcon.LIST.create());
    selectionIcon.setPadding(false);
    selectionIcon.setSpacing(false);
    selectionIcon.addClassName(CosmosClasses.PANEL_ICON);
    noSelectionTitle.addClassName("cosmos-notification-empty-title");
    selectTicket.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    noSelection.add(selectionIcon, noSelectionTitle, selectTicket);
    noSelection.setAlignItems(Alignment.CENTER);
    noSelection.setJustifyContentMode(JustifyContentMode.CENTER);
    noSelection.setSpacing(false);
    noSelection.setPadding(false);
    noSelection.setWidthFull();
    noSelection.setId("support-select-ticket");
    noSelection.addClassNames("support-admin-no-selection", "cosmos-notification-empty");
    selectTicket.getElement().setAttribute("role", "status");
    VerticalLayout detail = new VerticalLayout(noSelection, adminDetail);
    detail.setPadding(false);
    detail.setSpacing(false);
    detail.setFlexGrow(1, noSelection);
    detail.addClassName("support-admin-detail-column");
    adminWorkspace.add(adminQueue, detail);
    adminWorkspace.addClassName("support-admin-columns");
    panel.getBody().add(ticketCategories, adminWorkspace);
  }

  private void setTicketFocused(boolean focused) {
    ticketFocused = focused && selected != null;
    adminQueue.setVisible(!ticketFocused);
    adminWorkspace.setClassName("support-focused", ticketFocused);
    updateFocusControl();
  }

  private void updateFocusControl() {
    focusToggle.setPrefixComponent((ticketFocused ? AppIcon.MINIMIZE : AppIcon.EXPAND).create());
    focusToggle.getElement().setAttribute("aria-pressed", Boolean.toString(ticketFocused));
    String label =
        ticketFocused ? texts.getSupportRestoreSplitView() : texts.getSupportFocusTicket();
    focusToggle.setAriaLabel(label);
    focusToggle.setTooltipText(label);
    if (ticketFocused) {
      focusToggle.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      focusToggle.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
  }

  public void refreshTexts() {
    panel.setHeading(
        administration ? texts.getSupportTitle() : texts.getSupportHelpTitle(),
        administration ? texts.getSupportInboxDescription() : texts.getSupportHelpSubtitle(),
        "");
    emptyText.setText(texts.getSupportEmpty());
    userExplanation.setText(texts.getSupportNoUnlock());
    body.setPlaceholder(texts.getSupportRequestPlaceholder());
    if (administration) {
      adminBreadcrumb.setText(texts.getAdminHeaderTitle() + " / " + texts.getSupportTitle());
      adminPageTitle.setText(texts.getSupportTitle());
      adminPageDescription.setText(texts.getSupportPageDescription());
      queueHeading.setText(texts.getSupportCases());
      filterButton.setText(texts.getAdminUsersAddFilter());
      filters.refreshTexts();
      allTicketsLabel.setText(texts.getSupportAllTickets());
      recoveryTicketsLabel.setText(texts.getSupportRecoveryCategory());
      ticketCategories.getElement().setAttribute("aria-label", texts.getSupportTitle());
      noSelectionTitle.setText(texts.getSupportNoTicketSelected());
      selectTicket.setText(texts.getSupportSelectTicket());
      grid.getElement().setAttribute("aria-label", texts.getSupportCases());
    } else {
      userRequestsHeading.setText(texts.getSupportExistingRequests());
      grid.getElement().setAttribute("aria-label", texts.getSupportExistingRequests());
    }
    userWarningLabel.setText(texts.getSupportSecurityNoticeLabel() + " ");
    warning.setText(texts.getSupportSecurityNotice());
    body.setLabel(administration ? texts.getSupportReply() : texts.getSupportDescription());
    internal.setLabel(texts.getSupportInternal());
    subscription.setLabel(texts.getSupportSubscription());
    subscription.setTooltipText(texts.getSupportSubscriptionHint());
    create.setText(texts.getSupportCreate());
    claim.setText(texts.getSupportClaim());
    reply.setText(texts.getSupportReply());
    close.setText(administration ? texts.getSupportClose() : texts.getSupportCloseOwn());
    instructions.setText(texts.getSupportInstructions());
    reload.setAriaLabel(texts.getSessionRefresh());
    reload.setTooltipText(texts.getSessionRefresh());
    if (administration) {
      updateAdminComposerLabels();
      updateFocusControl();
      adminPolicyLabel.setText(texts.getSupportSecurityPolicy());
    }
    instructionDialog.refreshTexts();
    refresh();
    grid.getDataProvider().refreshAll();
  }

  private void updateAdminComposerLabels() {
    boolean privateNote = internal.getValue();
    body.setLabel(privateNote ? texts.getSupportInternal() : texts.getSupportReply());
    body.setPlaceholder(
        privateNote ? texts.getSupportInternalPlaceholder() : texts.getSupportReplyPlaceholder());
    reply.setText(privateNote ? texts.getSupportSaveInternal() : texts.getSupportReply());
  }

  public void refresh() {
    refresh(false);
  }

  public void openTicket(String number) {
    List<Ticket> values = service.list(request, administration);
    Ticket matching =
        values.stream()
            .filter(ticket -> ticketNumber(ticket).equals("#" + number))
            .findFirst()
            .orElse(null);
    if (administration && matching != null && !filters.matches(matching, administratorId)) {
      filters.resetApplied();
    }
    applyTickets(values, matching);
    refreshConversation();
    if (matching == null) {
      failed(texts::getSupportTicketUnavailable);
    }
  }

  public void setNavigationListener(SerializableConsumer<NavigationState> listener) {
    navigationListener = Objects.requireNonNull(listener);
  }

  /** URL-Zustand einlesen, ohne beim Wiederherstellen neue Browserhistorie zu schreiben. */
  public void restoreNavigation(Category category, String number) {
    restoringNavigation = true;
    try {
      ticketCategories.setSelectedTab(category == Category.RECOVERY ? recoveryTickets : allTickets);
      selected = null;
      if (number == null) {
        refresh();
      } else {
        openTicket(number);
      }
    } finally {
      restoringNavigation = false;
      reportedNavigation = navigationState();
    }
  }

  private NavigationState navigationState() {
    Category category =
        ticketCategories.getSelectedTab() == recoveryTickets ? Category.RECOVERY : Category.ALL;
    return new NavigationState(
        category, selected == null ? null : ticketNumber(selected).substring(1));
  }

  private void publishNavigation() {
    if (!administration || restoringNavigation) {
      return;
    }
    NavigationState state = navigationState();
    if (!state.equals(reportedNavigation)) {
      reportedNavigation = state;
      navigationListener.accept(state);
    }
  }

  @Override
  protected void onAttach(AttachEvent event) {
    super.onAttach(event);
    UI ui = event.getUI();
    if (!(event.getSession().getSession() instanceof WrappedHttpSession wrapped)) {
      return;
    }
    HttpSession session = wrapped.getHttpSession();
    liveSession = session;
    liveRegistration =
        service
            .events()
            .register(
                change -> {
                  Long viewer = liveViewerId;
                  if (change.affects(viewer, administration)) {
                    ui.access(
                        () -> {
                          if (isAttached() && liveSession == session) {
                            refreshLive(session);
                          }
                        });
                  }
                });
    refreshLive(session);
  }

  @Override
  protected void onDetach(DetachEvent event) {
    if (liveRegistration != null) {
      liveRegistration.remove();
      liveRegistration = null;
    }
    liveSession = null;
    liveViewerId = null;
    super.onDetach(event);
  }

  private void refreshLive(HttpSession session) {
    try {
      LiveView state = service.liveView(session, administration, selectedId());
      liveViewerId = state.viewerId();
      if (administration) {
        administratorId = state.viewerId();
      }
      applyTickets(state.tickets(), state.selected());
      showConversation(state.messages(), state.requester());
    } catch (AccessDeniedException _) {
      // Die Sitzungs-/Rollenprüfung hat Vorrang vor dem Nachladen per Push.
    } catch (RuntimeException failure) {
      log.warn("Ticket refresh failed", safeFailure(failure));
    }
  }

  private void refresh(boolean notifySuccess) {
    try {
      List<Ticket> values = service.list(request, administration);
      if (administration) {
        administratorId = service.administratorId(request);
      }
      applyTickets(values, SupportTicketService.selectTicket(values, selectedId(), administration));
      refreshConversation();
      if (notifySuccess) {
        succeeded(texts::getSupportRefreshed);
      }
    } catch (RuntimeException failure) {
      failed(texts::getSupportRefreshFailed);
    }
  }

  private Long selectedId() {
    return selected == null ? null : selected.id();
  }

  private void applyTickets(List<Ticket> values, Ticket selection) {
    if (administration && ticketCategories.getSelectedTab() == recoveryTickets) {
      values = values.stream().filter(ticket -> ticket.type() == Type.RECOVERY).toList();
      if (selection != null && selection.type() != Type.RECOVERY) {
        selection = null;
      }
    } else if (!administration) {
      userRequestCount.setText(texts.getSupportRequestCount(values.size()));
    }
    if (administration) {
      values = values.stream().filter(ticket -> filters.matches(ticket, administratorId)).toList();
      if (selection != null) {
        Long selectionId = selection.id();
        if (values.stream().noneMatch(ticket -> ticket.id().equals(selectionId))) {
          selection = null;
        }
      }
      int count = filters.activeCount();
      activeFilterCount.setText(Integer.toString(count));
      activeFilterCount.setVisible(count > 0);
      filterButton.setAriaLabel(
          count == 0
              ? texts.getAdminUsersAddFilter()
              : texts.getAdminUsersAddFilter()
                  + ", "
                  + texts.getAdminUsersActiveFilterCount(count));
      emptyText.setText(count > 0 ? texts.getSupportFilterEmpty() : texts.getSupportEmpty());
    }
    grid.setVisible(!values.isEmpty());
    adminTicketScroll.setVisible(!values.isEmpty());
    emptyState.setVisible(values.isEmpty());
    openRequest = values.stream().anyMatch(ticket -> ticket.status().acceptsMessages());
    // Gleiche Daten beim Kategorienwechsel sollen Fokus und Scrollposition im Grid behalten.
    if (!visibleTickets.equals(values)) {
      visibleTickets = List.copyOf(values);
      grid.setItems(visibleTickets).setIdentifierProvider(Ticket::id);
    }
    grid.asSingleSelect().setValue(selection);
    selected = selection;
    publishNavigation();
  }

  public void addBackAction(Button action) {
    if (administration) {
      throw new IllegalStateException("Recovery navigation belongs to the user's request form");
    }
    backActions.add(action);
  }

  private Component ticketRow(Ticket ticket) {
    Span title = new Span(ticketSubject(ticket));
    title.addClassName("support-ticket-title");
    Span updated = new Span(formatTime(ticket.updatedAt()));
    updated.addClassName("support-ticket-updated");
    Badge badge = new Badge(status(ticket.status()));
    setStatusBadge(badge, ticket.status());
    if (administration) {
      return adminTicketRow(ticket, title, updated, badge);
    }
    VerticalLayout summary = new VerticalLayout(title, updated);
    summary.setPadding(false);
    summary.setSpacing(false);
    HorizontalLayout details = new HorizontalLayout(ticketNumberLabel(ticket), summary);
    details.setSpacing(false);
    details.setAlignItems(Alignment.CENTER);
    details.addClassName("support-ticket-details");
    HorizontalLayout row = new HorizontalLayout(details, ticketIndicators(ticket, badge));
    row.setSpacing(false);
    row.setWidthFull();
    row.setAlignItems(Alignment.CENTER);
    row.addClassName("support-ticket-row");
    return row;
  }

  private VerticalLayout adminTicketRow(Ticket ticket, Span title, Span updated, Badge badge) {
    HorizontalLayout metadata =
        new HorizontalLayout(ticketNumberLabel(ticket), ticketIndicators(ticket, badge));
    metadata.setSpacing(false);
    metadata.setWidthFull();
    metadata.setAlignItems(Alignment.CENTER);
    metadata.setJustifyContentMode(JustifyContentMode.BETWEEN);
    metadata.addClassName("support-admin-ticket-metadata");

    HorizontalLayout footer =
        new HorizontalLayout(assigneeAvatar(ticket, IconSize.PIXEL_24), updated);
    footer.setSpacing(false);
    footer.setWidthFull();
    footer.setAlignItems(Alignment.CENTER);
    footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
    footer.addClassName("support-admin-ticket-footer");

    VerticalLayout row = new VerticalLayout(metadata, title, footer);
    row.setPadding(false);
    row.setSpacing(false);
    row.setWidthFull();
    row.addClassNames("support-ticket-row", "support-admin-ticket-card");
    return row;
  }

  private Component assigneeAvatar(Ticket ticket, IconSize iconSize) {
    if (ticket.assigneeIdentity() == null) {
      Span unassigned = new Span(texts.getSupportUnassigned());
      unassigned.addClassNames("support-ticket-assignment", "support-ticket-unassigned");
      return unassigned;
    }
    HorizontalLayout identity =
        new HorizontalLayout(identityAvatar(ticket.assigneeIdentity(), iconSize));
    identity.setPadding(false);
    identity.setSpacing(false);
    identity.setAlignItems(Alignment.CENTER);
    identity.addClassNames("support-ticket-assignment", "support-ticket-assignee");
    if (ticket.assignee().equals(administratorId)) {
      Badge you = new Badge(texts.getSupportYou());
      you.addThemeVariants(BadgeVariant.SUCCESS);
      you.addClassName("support-assignee-self");
      identity.add(you);
    }
    return identity;
  }

  private OverlayUserIcon identityAvatar(UserIdentity identity, IconSize iconSize) {
    OverlayUserIcon avatar = new OverlayUserIcon(texts, null, null, iconSize);
    avatar.setName(identity.firstName(), identity.lastName());
    avatar.showIdentityTooltip(identity.email());
    return avatar;
  }

  private String ticketSubject(Ticket ticket) {
    return switch (ticket.type()) {
      case RECOVERY -> texts.getSupportRecoverySubject();
    };
  }

  private void setStatusBadge(Badge badge, Status ticketStatus) {
    badge.setText(status(ticketStatus));
    badge.addClassName("support-ticket-status");
    badge.getElement().setAttribute("data-status", ticketStatus.name());
    if (ticketStatus == Status.CLOSED_BY_REQUESTER) {
      badge.addThemeVariants(BadgeVariant.SUCCESS);
    } else {
      badge.removeThemeVariants(BadgeVariant.SUCCESS);
    }
  }

  private HorizontalLayout ticketIndicators(Ticket ticket, Badge statusBadge) {
    HorizontalLayout indicators = new HorizontalLayout(statusBadge);
    indicators.setSpacing(false);
    indicators.setAlignItems(Alignment.CENTER);
    indicators.addClassName("support-ticket-indicators");
    if (ticket.unreadMessages() > 0) {
      Badge unread = new Badge(Long.toString(ticket.unreadMessages()));
      unread.addThemeVariants(BadgeVariant.ERROR);
      unread.addClassName("support-ticket-unread");
      unread
          .getElement()
          .setAttribute("aria-label", texts.getSupportUnreadMessages(ticket.unreadMessages()));
      indicators.add(unread);
    }
    return indicators;
  }

  private void refreshConversation() {
    List<Reply> messages =
        selected == null ? List.of() : service.conversation(request, selected.id(), administration);
    UserIdentity requester =
        !administration && !messages.isEmpty() ? service.requesterIdentity(request) : null;
    showConversation(messages, requester);
  }

  private void showConversation(List<Reply> messages, UserIdentity requester) {
    boolean active = selected != null && selected.status().acceptsMessages();
    boolean unassigned = selected != null && selected.assignee() == null;
    boolean assignedToMe =
        selected != null && administratorId != null && administratorId.equals(selected.assignee());
    boolean canReply = active && (!administration || assignedToMe);
    boolean requesterClosed = selected != null && selected.status() == Status.CLOSED_BY_REQUESTER;
    DraftTarget target = null;
    if (canReply) {
      target = new DraftTarget(selected.id());
    } else if (!administration && !openRequest) {
      target = NEW_REQUEST_DRAFT;
    }
    switchDraft(target);
    if (!administration) {
      body.setLabel(openRequest ? texts.getSupportReply() : texts.getSupportDescription());
      userEditor.setVisible(!openRequest || canReply);
    } else {
      adminEditor.setVisible(canReply);
      subscription.setVisible(selected != null && selected.subscriptionAllowed());
      subscription.setValue(selected != null && selected.subscribed());
    }
    create.setVisible(!administration && !openRequest);
    claim.setVisible(administration && active && unassigned);
    if (selected != null && selected.status() == Status.OPEN) {
      claim.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      claim.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    reply.setVisible(canReply);
    close.setVisible(
        administration ? canReply || requesterClosed && (unassigned || assignedToMe) : active);
    instructions.setVisible(administration && canReply);
    instructions.setEnabled(selected != null && !selected.instructionSent());
    if (administration) {
      adminTicketTools.setVisible(
          subscription.isVisible() || claim.isVisible() || instructions.isVisible());
      adminActionFooter.setVisible(close.isVisible() || reply.isVisible());
    }
    if (!administration) {
      refreshUserConversation(messages, requester);
    } else {
      refreshAdminConversation(messages);
    }
    acknowledgeVisibleMessages(messages);
  }

  private void refreshAdminConversation(List<Reply> messages) {
    adminDetail.setVisible(selected != null);
    noSelection.setVisible(selected == null);
    if (selected == null) {
      setTicketFocused(false);
      userMessageThread.removeAll();
      return;
    }
    adminTicketNumber.setText(texts.getSupportTicketNumber(ticketNumber(selected)));
    adminSubject.setText(ticketSubject(selected));
    setStatusBadge(adminStatus, selected.status());
    adminUpdated.setText(formatTime(selected.updatedAt()));
    adminAssignment.removeAll();
    Span assignmentLabel = new Span(texts.getSupportAssigneeColumn());
    assignmentLabel.addClassName("support-admin-person-label");
    Component assignee;
    if (selected.assigneeIdentity() != null
        && !Objects.equals(selected.assignee(), administratorId)) {
      HorizontalLayout identity = adminPersonIdentity(selected.assigneeIdentity());
      identity.setWidth(null);
      assignee = identity;
    } else {
      assignee = assigneeAvatar(selected, IconSize.PIXEL_32);
    }
    assignee.addClassName("support-admin-assignee-identity");
    adminAssignment.add(assignmentLabel, assignee);
    updateAdminRequester(selected.requesterIdentity());
    adminRecoveryHint.setText(texts.getSupportNoUnlock());
    adminRecoveryInfo.setVisible(selected.type() == Type.RECOVERY);
    String statusHint =
        switch (selected.status()) {
          case OPEN -> selected.assignee() == null ? texts.getSupportUnassignedHint() : "";
          case IN_PROGRESS -> "";
          case CLOSED -> texts.getSupportClosedHint();
          case CLOSED_BY_REQUESTER -> texts.getSupportRequesterClosedHint();
        };
    adminStatusNote.setText(statusHint);
    adminStatusNote.setVisible(!statusHint.isEmpty());
    String heading = texts.getSupportConversation();
    userConversationHeading.setText(heading);
    userConversationCount.setText(texts.getSupportMessageCount(messages.size()));
    userMessageScroll
        .getElement()
        .setAttribute("aria-label", heading + " " + ticketNumber(selected));
    renderMessages(messages, selected.requesterIdentity());
  }

  private void updateAdminRequester(UserIdentity requester) {
    adminRequester.removeAll();
    adminRequester.setVisible(requester != null);
    if (requester == null) {
      return;
    }
    Span label = new Span(texts.getSupportRequester());
    label.addClassName("support-admin-person-label");
    adminRequester.add(label, adminPersonIdentity(requester));
  }

  private HorizontalLayout adminPersonIdentity(UserIdentity person) {
    Span name = new Span(identityName(person));
    name.addClassName("support-admin-person-name");
    Span email = new Span(person.email());
    email.addClassName("support-admin-person-email");
    VerticalLayout copy = new VerticalLayout(name, email);
    copy.setPadding(false);
    copy.setSpacing(false);
    copy.setWidth(null);
    HorizontalLayout identity =
        new HorizontalLayout(identityAvatar(person, IconSize.PIXEL_32), copy);
    identity.setPadding(false);
    identity.setSpacing(false);
    identity.setWidthFull();
    identity.setFlexGrow(1, copy);
    identity.setAlignItems(Alignment.CENTER);
    identity.addClassName("support-admin-person-identity");
    return identity;
  }

  private static String identityName(UserIdentity identity) {
    String name =
        (Objects.toString(identity.firstName(), "")
                + " "
                + Objects.toString(identity.lastName(), ""))
            .strip();
    return name.isEmpty() ? identity.email() : name;
  }

  private void acknowledgeVisibleMessages(List<Reply> messages) {
    HttpSession session = liveSession;
    Ticket ticket = selected;
    if (session == null
        || ticket == null
        || !administration && ticket.unreadMessages() == 0
        || messages.isEmpty()) {
      return;
    }
    Long lastReplyId = messages.stream().map(Reply::id).max(Long::compareTo).orElseThrow();
    // Im Hintergrund geöffnete Tabs quittieren erst, sobald der Verlauf sichtbar wird.
    conversation
        .getElement()
        .executeJs(
            """
        this.__finishSupportRead?.(false);
        const displayed = () => this.isConnected && this.getClientRects().length > 0;
        if (document.visibilityState === 'visible') return displayed();
        return new Promise(resolve => {
          const shown = () => finish(displayed() && document.visibilityState === 'visible');
          const finish = value => {
            document.removeEventListener('visibilitychange', shown);
            delete this.__finishSupportRead;
            resolve(value);
          };
          this.__finishSupportRead = finish;
          document.addEventListener('visibilitychange', shown, {once: true});
        });
        """)
        .then(
            Boolean.class,
            visible -> {
              if (!Boolean.TRUE.equals(visible)
                  || liveSession != session
                  || !Objects.equals(selectedId(), ticket.id())) {
                return;
              }
              try {
                service.markSeen(
                    session, administration, ticket.id(), lastReplyId, ticket.version());
              } catch (AccessDeniedException _) {
                // Eine inzwischen abgelaufene Sitzung darf keine Lesegrenze schreiben.
              } catch (RuntimeException failure) {
                log.warn("Ticket read status could not be saved", safeFailure(failure));
              }
            });
  }

  private void switchDraft(DraftTarget target) {
    if (Objects.equals(draftTarget, target)) {
      return;
    }
    if (draftTarget != null) {
      if (body.isEmpty()) {
        drafts.remove(draftTarget);
      } else {
        drafts.put(draftTarget, new Draft(body.getValue(), internal.getValue()));
      }
    }
    draftTarget = target;
    Draft draft = drafts.remove(target);
    body.setValue(draft == null ? "" : draft.text());
    body.setInvalid(false);
    internal.setValue(draft != null && draft.internal());
  }

  private void refreshUserConversation(List<Reply> messages, UserIdentity requester) {
    boolean showConversation = !messages.isEmpty();
    HorizontalLayout refreshHeader = showConversation ? userConversationHeader : userRequestsHeader;
    if (reload.getParent().filter(refreshHeader::equals).isEmpty()) {
      refreshHeader.add(reload);
    }
    conversation.setVisible(showConversation);
    if (!showConversation) {
      userMessageThread.removeAll();
      return;
    }
    String heading = texts.getSupportConversation() + " ";
    userConversationHeading.removeAll();
    userConversationHeading.add(new Text(heading), ticketNumberLabel(selected));
    userConversationCount.setText(texts.getSupportMessageCount(messages.size()));
    userMessageScroll.getElement().setAttribute("aria-label", heading + ticketNumber(selected));
    renderMessages(messages, requester);
  }

  private void renderMessages(List<Reply> messages, UserIdentity requester) {
    userMessageThread.removeAll();
    for (Reply message : messages) {
      Span timestamp = new Span("· " + formatTime(message.createdAt()));
      timestamp.addClassName("support-message-time");
      HorizontalLayout metadata = new HorizontalLayout();
      metadata.setAlignItems(Alignment.CENTER);
      metadata.setSpacing(false);
      metadata.addClassName("support-message-metadata");
      switch (message.author()) {
        case REQUESTER -> {
          if (requester == null) {
            metadata.add(AppIcon.USER.create(), new Span(texts.getSupportRequester()));
          } else {
            metadata.add(identityAvatar(requester, IconSize.PIXEL_24));
            if (administration) {
              metadata.add(new Span(identityName(requester)));
            }
          }
        }
        case SUPPORT -> {
          Span sender = new Span(texts.getSupportMessageSupport());
          sender.addClassName("support-message-sender");
          metadata.add(AppIcon.SHIELD_COG.create(), sender);
        }
      }
      metadata.add(timestamp);
      if (message.internal()) {
        Badge note = new Badge(texts.getSupportInternal());
        note.addClassName("support-admin-internal");
        metadata.add(note);
      }
      Component copy;
      if (message.kind() == Kind.RECOVERY_INSTRUCTIONS) {
        copy = instructionMessage(message);
      } else {
        copy = new Span(message.body());
      }
      copy.getElement().getClassList().add("support-message-copy");
      VerticalLayout entry = new VerticalLayout(metadata, copy);
      entry.setSpacing(false);
      entry.setPadding(false);
      entry.addClassName("support-message");
      entry.getElement().setAttribute("data-author", message.author().name());
      boolean own = (message.author() == ReplyAuthor.SUPPORT) == administration;
      entry.getElement().setAttribute("data-side", own ? "own" : "other");
      if (message.internal()) {
        entry.addClassName("support-internal-message");
      }
      userMessageThread.add(entry);
    }
  }

  private Component instructionMessage(Reply message) {
    RecoveryInstructionsComponent guide = new RecoveryInstructionsComponent(message.body());
    Details details = new Details("", guide);
    details.setId("support-instruction-" + message.id());
    details.setOpened(expandedInstructions.contains(message.id()));
    details.addClassName("support-instruction-details");
    details.addOpenedChangeListener(
        event -> {
          if (event.isOpened()) {
            expandedInstructions.add(message.id());
          } else {
            expandedInstructions.remove(message.id());
          }
        });
    Span title = new Span(texts.getSupportInstructionMessageTitle());
    title.addClassName("support-instruction-title");
    HorizontalLayout heading = new HorizontalLayout(AppIcon.ENVELOPE.create(), title);
    heading.setSpacing(false);
    heading.setPadding(false);
    heading.setAlignItems(Alignment.CENTER);
    heading.addClassName("support-instruction-heading");
    Button toggle = new DetailsToggleButton(texts, details, title.getText());
    toggle.addClassName("support-instruction-toggle");
    HorizontalLayout header = new HorizontalLayout(heading, toggle);
    header.setWidthFull();
    header.setSpacing(false);
    header.setPadding(false);
    header.setAlignItems(Alignment.CENTER);
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    header.addClassName("support-instruction-header");
    VerticalLayout bubble = new VerticalLayout(header, new Span(guide.getDescription()), details);
    bubble.setSpacing(false);
    bubble.setPadding(false);
    bubble.addClassName("support-instruction-message");
    return bubble;
  }

  private static Span ticketNumberLabel(Ticket ticket) {
    Span number = new Span(ticketNumber(ticket));
    number.addClassName("support-ticket-number");
    return number;
  }

  private static String ticketNumber(Ticket ticket) {
    return SupportTicketService.ticketNumber(ticket.id());
  }

  private String formatTime(Instant time) {
    return new DateTimeFormatterBuilder()
        .appendLocalized(FormatStyle.SHORT, null)
        .appendLiteral(" · ")
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter(getLocale())
        .withZone(ZoneId.systemDefault())
        .format(time);
  }

  private void previewInstructions() {
    try {
      String preview = service.instructionPreview(request, selected.id());
      RecoveryInstructionsComponent guide = new RecoveryInstructionsComponent(preview);
      Ticket target = selected;
      instructionDialog.open(
          new PortalConfirmationDialog.Prompt(
              texts::getSupportInstructions,
              () -> "",
              guide::getHeading,
              guide::getDescription,
              () -> guide,
              () -> "",
              texts::getSupportInstructions,
              false,
              AppIcon.ENVELOPE::create),
          () -> {
            try {
              service.sendInstructions(request, target.id(), target.version());
              refresh();
              NotificationHelper.getInstance()
                  .showNotification(
                      texts::getSupportQueued, NotificationHelper.NotificationType.SUCCESS);
            } catch (RuntimeException failure) {
              handleActionFailure(failure);
            }
          });
    } catch (RuntimeException failure) {
      handleActionFailure(failure);
    }
  }

  private void perform(Supplier<Ticket> action, Supplier<String> success, boolean clearDraft) {
    try {
      selected = action.get();
      if (clearDraft) {
        body.clear();
        body.setInvalid(false);
        internal.setValue(false);
      }
      succeeded(success);
      refresh();
    } catch (RuntimeException failure) {
      handleActionFailure(failure);
    }
  }

  private void handleActionFailure(RuntimeException failure) {
    if (failure instanceof OptimisticLockingFailureException) {
      refresh();
      failed(texts::getSupportConflict);
      return;
    }
    failed(texts::getSupportFailed);
  }

  private void succeeded(Supplier<String> message) {
    NotificationHelper.getInstance()
        .showNotification(message, NotificationHelper.NotificationType.SUCCESS);
  }

  private void failed(Supplier<String> message) {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle, message, NotificationHelper.NotificationType.ERROR);
  }

  private String status(Status value) {
    return switch (value) {
      case OPEN -> texts.getSupportOpen();
      case IN_PROGRESS -> texts.getSupportInProgress();
      case CLOSED -> texts.getSupportClosed();
      case CLOSED_BY_REQUESTER -> texts.getSupportClosedByRequester();
    };
  }
}
