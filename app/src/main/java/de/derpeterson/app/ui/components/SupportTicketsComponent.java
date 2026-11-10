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
import com.vaadin.flow.component.icon.SvgIcon;
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
import de.derpeterson.app.service.SupportTicketService.InboxQuery;
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
@StyleSheet("custom-theme/cosmos-mfa-methods.css")
@StyleSheet("custom-theme/cosmos-provider-settings.css")
@Slf4j
public final class SupportTicketsComponent extends VerticalLayout {
  private static final String CONVERSATION_HEADING_CLASS = "support-conversation-heading";
  private static final String SECTION_COUNT_CLASS = "support-section-count";
  private static final String SECTION_HEADER_CLASS = "support-section-header";
  private static final String ARIA_LABEL = "aria-label";

  public enum Category {
    ALL,
    RECOVERY
  }

  public record NavigationState(Category category, String ticketNumber) implements Serializable {}

  private record DraftTarget(Long ticketId) implements Serializable {}

  private record Draft(String text, boolean internal) implements Serializable {}

  private static final DraftTarget NEW_REQUEST_DRAFT = new DraftTarget(null);

  private final transient MessageProperties messageProperties;
  private final transient SupportTicketService supportTicketService;
  private final transient HttpServletRequest httpRequest;
  private final boolean administration;
  private final Grid<Ticket> ticketGrid = new Grid<>();
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
  private final Span selectTicketHint = new Span();
  private final VerticalLayout ticketSelectionPlaceholder = new VerticalLayout();
  private final CosmosCardComponent adminDetail = new CosmosCardComponent();
  private final Div adminWorkspace = new Div();
  private final VerticalLayout adminQueue = new VerticalLayout();
  private final Button toggleTicketFocusButton = new Button();
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
  private final VerticalLayout ticketConversationLayout = new VerticalLayout();
  private final TextArea ticketMessageInput = new TextArea();
  private final Span publicReplyLabel = new Span();
  private final Span internalNoteLabel = new Span();
  private final Tab publicReplyMode = new Tab(AppIcon.MESSAGE.create(), publicReplyLabel);
  private final Tab internalNoteMode = new Tab(AppIcon.LOCK.create(), internalNoteLabel);
  private final Tabs replyMode = new Tabs(publicReplyMode, internalNoteMode);
  private final Switch watchTicketSwitch = new Switch();
  private final Button createRequestButton = new Button();
  private final Button claimTicketButton = new Button();
  private final Button sendReplyButton = new Button();
  private final Button closeTicketButton = new Button();
  private final Button sendInstructionsButton = new Button();
  private final Button reloadTicketsButton = new Button();
  private final Button filterButton = new Button();
  private final Badge activeFilterCount = new Badge();
  private SupportTicketFilterPanel ticketFilterPanel;
  private CosmosPagination ticketPagination;
  private int currentPage;
  private int pageSize = 10;
  private final PortalConfirmationDialog instructionDialog;
  private RecoveryInstructionsComponent instructionPreview;
  private final AccountPanelComponent ticketWorkspacePanel =
      new AccountPanelComponent("support-panel", AppIcon.INFO_CIRCLE);
  private final Span securityWarningSpan = new Span();
  private final Span userWarningLabel = new Span();
  private final Span emptyText = new Span();
  private final Span queueEmptyTitle = new Span();
  private final Span queueEmptyHint = new Span();
  private final Span userExplanation = new Span();
  private final H3 userRequestsHeading = new H3();
  private final Badge userRequestCount = new Badge();
  private final HorizontalLayout userRequestsHeader =
      new HorizontalLayout(userRequestsHeading, userRequestCount);
  private final H3 userConversationHeading = new H3();
  private final Badge userConversationCount = new Badge();
  private final HorizontalLayout userConversationHeader =
      new HorizontalLayout(userConversationHeading, userConversationCount);
  private final HorizontalLayout userConversationActions = new HorizontalLayout();
  private final VerticalLayout userMessageThread = new VerticalLayout();
  private final Scroller userMessageScroll =
      new Scroller(userMessageThread, Scroller.ScrollDirection.VERTICAL);
  private final HorizontalLayout emptyState = new HorizontalLayout();
  private final Div queueEmptyState = new Div();
  private final HorizontalLayout backActions = new HorizontalLayout();
  private final HorizontalLayout userFooterActions = new HorizontalLayout();
  private final VerticalLayout userEditor = new VerticalLayout();
  private final Div messageField = new Div();
  private Ticket selectedTicket;
  private List<Ticket> visibleTickets = List.of();
  private final Map<DraftTarget, Draft> drafts = new HashMap<>();
  private final Set<Long> expandedInstructions = new HashSet<>();
  private DraftTarget draftTarget;
  private boolean hasOpenRequest;
  private Long administratorId;
  private SerializableConsumer<NavigationState> navigationListener = state -> {};
  private NavigationState reportedNavigation;
  private boolean restoringNavigation;
  private boolean ticketFocused;
  private transient Registration liveRegistration;
  private transient HttpSession liveSession;
  private transient volatile Long liveViewerId;

  public SupportTicketsComponent(
      MessageProperties messageProperties,
      SupportTicketService supportTicketService,
      HttpServletRequest httpRequest,
      boolean administration) {
    this.messageProperties = messageProperties;
    this.supportTicketService = supportTicketService;
    this.httpRequest = httpRequest;
    this.administration = administration;
    instructionDialog =
        new PortalConfirmationDialog(messageProperties, "support-instruction-preview");
    instructionDialog.addClassName("support-instructions");
    setId(administration ? "admin-support" : "account-support");
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    addClassNames("cosmos-dialog-form", "support-tickets");
    ticketConversationLayout.setPadding(false);
    ticketConversationLayout.setSpacing(false);
    ticketConversationLayout.setAlignItems(Alignment.STRETCH);
    configureGrid();
    configureEditor();
    configureActions();
    HorizontalLayout sensitiveNotice = sensitiveNotice();
    if (administration) {
      buildAdminWorkspace(sensitiveNotice);
    } else {
      addClassName("support-recovery-help");
      ticketWorkspacePanel.setIcon(AppIcon.LOCK_QUESTION.create());
      ticketConversationLayout.addClassName("support-user-conversation");
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
      userRequestsHeading.addClassName(CONVERSATION_HEADING_CLASS);
      userConversationHeading.addClassName(CONVERSATION_HEADING_CLASS);
      userRequestCount.setId("support-request-count");
      userRequestCount.addClassNames(SECTION_COUNT_CLASS, "support-request-count");
      userConversationCount.addClassName(SECTION_COUNT_CLASS);
      for (HorizontalLayout header : List.of(userRequestsHeader, userConversationHeader)) {
        header.setWidthFull();
        header.setSpacing(false);
        header.setAlignItems(Alignment.CENTER);
        header.addClassName(SECTION_HEADER_CLASS);
      }
      userRequestsHeader.add(reloadTicketsButton);
      userConversationActions.setSpacing(false);
      userConversationActions.setAlignItems(Alignment.CENTER);
      userConversationActions.addClassName("support-conversation-actions");
      userConversationActions.add(closeTicketButton);
      userConversationHeader.add(userConversationActions);
      userMessageThread.setSpacing(false);
      userMessageThread.addClassName("support-message-thread");
      userMessageScroll.addClassName("support-message-scroll");
      ticketConversationLayout.add(userConversationHeader, userMessageScroll);
      VerticalLayout requests = new VerticalLayout(userRequestsHeader, emptyState, ticketGrid);
      requests.setPadding(false);
      requests.setSpacing(false);
      requests.addClassName("support-user-requests");
      VerticalLayout form =
          new VerticalLayout(helpNotice, requests, ticketConversationLayout, userEditor);
      form.setPadding(false);
      form.setSpacing(false);
      form.setWidthFull();
      form.addClassName("support-user-form");
      ticketWorkspacePanel.getBody().add(form);
      createRequestButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      sendReplyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      userFooterActions.add(sendReplyButton, createRequestButton);
      userFooterActions.setPadding(false);
      userFooterActions.setSpacing(false);
      userFooterActions.addClassName("cosmos-dialog-actions");
      backActions.setPadding(false);
      backActions.setSpacing(false);
      backActions.addClassName("support-back-actions");
      HorizontalLayout footer = new HorizontalLayout(backActions, userFooterActions);
      footer.setPadding(false);
      footer.setSpacing(false);
      footer.setWidthFull();
      footer.setAlignItems(Alignment.CENTER);
      footer.addClassName("support-user-footer");
      ticketWorkspacePanel.addActionFooter(footer);
    }
    add(ticketWorkspacePanel, instructionDialog);
    refreshTexts();
  }

  private HorizontalLayout sensitiveNotice() {
    userWarningLabel.addClassName("cosmos-security-warning-label");
    HorizontalLayout notice =
        new HorizontalLayout(
            AppIcon.ERROR.create(), new Span(userWarningLabel, securityWarningSpan));
    notice.setPadding(false);
    notice.setSpacing(false);
    notice.setAlignItems(Alignment.START);
    notice.addClassName("cosmos-security-warning");
    return notice;
  }

  private void configureGrid() {
    ticketGrid.setId("support-ticket-grid");
    ticketGrid.setWidthFull();
    ticketGrid.setAllRowsVisible(true);
    ticketGrid.addComponentColumn(this::ticketRow);
    ticketGrid
        .asSingleSelect()
        .addValueChangeListener(event -> {
          selectedTicket = event.getValue();
          if (event.isFromClient()) {
            try {
              refreshConversation();
            } catch (RuntimeException _) {
              failed(messageProperties::getSupportRefreshFailed);
            }
            publishNavigation();
          }
        });
  }

  private void configureEditor() {
    ticketMessageInput.setId("support-reply");
    ticketMessageInput.setWidthFull();
    ticketMessageInput.setMinRows(4);
    ticketMessageInput.addClassName("support-message-input");
    ticketMessageInput.setMaxLength(SupportTicketService.MESSAGE_LIMIT);
    ticketMessageInput.setRequiredIndicatorVisible(true);
    ticketMessageInput.setManualValidation(true);
    ticketMessageInput.setValueChangeMode(ValueChangeMode.EAGER);
    Span messageCount = new Span("0 / " + ticketMessageInput.getMaxLength());
    messageCount.addClassName("cosmos-notification-compose-counter");
    messageCount.getElement().setAttribute("aria-hidden", "true");
    ticketMessageInput.addValueChangeListener(event -> {
      ticketMessageInput.setInvalid(false);
      messageCount.setText(event.getValue().length() + " / " + ticketMessageInput.getMaxLength());
    });
    messageField.add(ticketMessageInput, messageCount);
    messageField.addClassName("cosmos-notification-compose-field");
    internalNoteMode.setId("support-internal-note");
    publicReplyMode.setId("support-public-reply");
    replyMode.setId("support-reply-mode");
    replyMode.addClassName("two-factor-method-tabs");
    replyMode.setVisible(administration);
    if (administration) {
      replyMode.addSelectedChangeListener(event -> updateAdminComposerLabels());
    }
    watchTicketSwitch.setId("support-subscription");
    watchTicketSwitch.setVisible(false);
    watchTicketSwitch.addValueChangeListener(event -> {
      if (event.isFromClient() && selectedTicket != null) {
        try {
          supportTicketService.setSubscription(httpRequest, selectedTicket.id(), event.getValue());
          refreshTicketWorkspace();
        } catch (RuntimeException failure) {
          watchTicketSwitch.setValue(selectedTicket.subscribed());
          handleActionFailure(failure);
        }
      }
    });
  }

  private void configureActions() {
    createRequestButton.setId("support-create");
    createRequestButton.setPrefixComponent(AppIcon.PLUS.create());
    createRequestButton.addClickListener(event -> {
      if (ValidationHelper.validateRequiredInputs(List.of(ticketMessageInput), messageProperties)) {
        perform(
            () -> supportTicketService.createRecovery(httpRequest, ticketMessageInput.getValue()),
            messageProperties::getSupportCreated,
            true);
      }
    });
    claimTicketButton.setId("support-claim");
    claimTicketButton.setPrefixComponent(AppIcon.CHECK.create());
    claimTicketButton.addClickListener(event ->
        perform(
            () ->
                supportTicketService.claimTicket(
                    httpRequest, selectedTicket.id(), selectedTicket.version()),
            messageProperties::getSupportSaved,
            false));
    sendReplyButton.setId("support-add-reply");
    sendReplyButton.setPrefixComponent(AppIcon.PAPERPLANE.create());
    sendReplyButton.addClickListener(event -> {
      if (ValidationHelper.validateRequiredInputs(List.of(ticketMessageInput), messageProperties)) {
        boolean privateNote = administration && isInternalNote();
        perform(
            () ->
                supportTicketService.replyToTicket(
                    httpRequest,
                    selectedTicket.id(),
                    selectedTicket.version(),
                    ticketMessageInput.getValue(),
                    privateNote,
                    administration),
            privateNote
                ? messageProperties::getSupportInternalSaved
                : messageProperties::getSupportReplySaved,
            true);
      }
    });
    closeTicketButton.setId("support-close");
    closeTicketButton.setPrefixComponent(AppIcon.CHECK_CIRCLE.create());
    closeTicketButton.addClickListener(event ->
        perform(
            () ->
                administration
                    ? supportTicketService.closeTicketAsAdministrator(
                        httpRequest, selectedTicket.id(), selectedTicket.version())
                    : supportTicketService.closeOwnTicket(
                        httpRequest, selectedTicket.id(), selectedTicket.version()),
            messageProperties::getSupportSaved,
            true));
    sendInstructionsButton.setId("support-instructions");
    sendInstructionsButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    sendInstructionsButton.addClickListener(event -> previewInstructions());
    reloadTicketsButton.setPrefixComponent(AppIcon.REFRESH.create());
    reloadTicketsButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    reloadTicketsButton.setId("support-reload");
    reloadTicketsButton.addClickListener(event -> refreshTicketWorkspace(true));
  }

  private void buildAdminWorkspace(HorizontalLayout sensitiveNotice) {
    addClassName("support-admin-workspace");
    ticketWorkspacePanel.getBody().setWidthFull();
    CosmosSectionHeaderComponent heading =
        new CosmosSectionHeaderComponent(adminBreadcrumb, adminPageTitle, adminPageDescription);
    heading.setId("support-page-header");
    add(heading);
    ticketWorkspacePanel.setIcon(AppIcon.MESSAGES.create());
    ticketCategories.setId("support-ticket-categories");
    ticketCategories.addClassName(CosmosClasses.PANEL_TABS);
    ticketCategories.setWidthFull();
    ticketCategories.addSelectedChangeListener(event -> {
      currentPage = 0;
      refreshTicketWorkspace();
    });
    buildAdminTicketQueue();
    VerticalLayout ticketHeader = buildAdminTicketHeader();
    buildAdminTicketDetail(ticketHeader, sensitiveNotice);
    VerticalLayout ticketDetailColumn = buildAdminDetailColumn();
    adminWorkspace.add(adminQueue, ticketDetailColumn);
    adminWorkspace.addClassName("support-admin-columns");
    ticketWorkspacePanel.getBody().add(ticketCategories, adminWorkspace);
  }

  private void buildAdminTicketQueue() {
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
    configureAdminQueueFilter();
    HorizontalLayout queueTools = new HorizontalLayout(filterButton, reloadTicketsButton);
    queueTools.setId("support-queue-tools");
    queueTools.setPadding(false);
    queueTools.setSpacing(false);
    queueTools.setWidthFull();
    queueTools.setAlignItems(Alignment.CENTER);
    queueTools.setJustifyContentMode(JustifyContentMode.END);
    queueTools.addClassNames(SECTION_HEADER_CLASS, "support-admin-list-tools");
    queueEmptyTitle.addClassName("admin-grid-empty-title");
    queueEmptyState.add(AppIcon.LIST.create(), queueEmptyTitle, queueEmptyHint);
    queueEmptyState.addClassName("admin-grid-empty");
    queueEmptyState.setId("support-queue-empty");
    ticketGrid.addClassName("support-admin-ticket-list");
    adminTicketScroll.setContent(ticketGrid);
    adminTicketScroll.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
    adminTicketScroll.setWidthFull();
    adminTicketScroll.addClassName("support-admin-ticket-scroll");
    ticketPagination =
        new CosmosPagination(
            messageProperties,
            page -> {
              currentPage = page;
              refreshTicketWorkspace();
            },
            size -> {
              pageSize = size;
              currentPage = 0;
              refreshTicketWorkspace();
            },
            List.of(10, 20, 50),
            CosmosPagination.Mode.PREVIOUS_NEXT);
    ticketPagination.setId("support-pagination");
    HorizontalLayout queueFooter = new HorizontalLayout(ticketPagination);
    queueFooter.setId("support-queue-footer");
    queueFooter.setPadding(false);
    queueFooter.setSpacing(false);
    queueFooter.setWidthFull();
    queueFooter.addClassName("cosmos-panel-footer");
    CosmosCardComponent queuePanel =
        new CosmosCardComponent(
            queueHeader, queueTools, queueEmptyState, adminTicketScroll, queueFooter);
    queuePanel.setId("support-queue-panel");
    queuePanel.addClassNames(CosmosClasses.PANEL_CARD, "support-admin-list-panel");
    adminQueue.add(queuePanel);
    adminQueue.setPadding(false);
    adminQueue.addClassName("support-admin-queue");
  }

  private void configureAdminQueueFilter() {
    ticketFilterPanel =
        new SupportTicketFilterPanel(
            messageProperties,
            this::status,
            () -> {
              currentPage = 0;
              refreshTicketWorkspace();
            },
            this::jumpToTicket);
    add(ticketFilterPanel);
    filterButton.setId("support-filter");
    filterButton.getElement().setAttribute("aria-controls", "support-filter-panel");
    filterButton.getElement().setAttribute("aria-haspopup", "dialog");
    filterButton.getElement().setAttribute("aria-expanded", "false");
    ticketFilterPanel.addOpenedChangeListener(event ->
        filterButton
            .getElement()
            .setAttribute("aria-expanded", Boolean.toString(event.isOpened())));
    filterButton.setPrefixComponent(AppIcon.FILTER.create());
    activeFilterCount.addClassName(SECTION_COUNT_CLASS);
    activeFilterCount.setId("support-active-filter-count");
    activeFilterCount.setVisible(false);
    filterButton.setSuffixComponent(activeFilterCount);
    filterButton.addClickListener(event -> ticketFilterPanel.openWithAppliedFilters());
  }

  private VerticalLayout buildAdminTicketHeader() {
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
    toggleTicketFocusButton.setId("support-focus-toggle");
    toggleTicketFocusButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    toggleTicketFocusButton
        .getElement()
        .setAttribute("aria-controls", "support-queue-panel support-ticket-panel");
    toggleTicketFocusButton.addClickListener(event -> setTicketFocused(!ticketFocused));
    HorizontalLayout orientation =
        new HorizontalLayout(ticketIcon, metadata, adminUpdated, toggleTicketFocusButton);
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
    return ticketHeader;
  }

  private void buildAdminTicketDetail(
      VerticalLayout ticketHeader, HorizontalLayout sensitiveNotice) {
    adminTicketTools.setId("support-ticket-tools");
    adminTicketTools.setPadding(false);
    adminTicketTools.setSpacing(false);
    adminTicketTools.setWidthFull();
    adminTicketTools.setAlignItems(Alignment.CENTER);
    adminTicketTools.addClassName("support-admin-ticket-tools");
    watchTicketSwitch.addClassName("cosmos-config-switch");
    adminTicketTools.add(watchTicketSwitch, claimTicketButton, sendInstructionsButton);
    adminPolicyLabel.addClassName("support-admin-policy-label");
    Span policy = new Span(adminPolicyLabel, new Text(" "), adminRecoveryHint);
    adminRecoveryInfo.add(AppIcon.INFO_CIRCLE.create(), policy);
    adminRecoveryInfo.setId("support-ticket-policy");
    adminRecoveryInfo.setPadding(false);
    adminRecoveryInfo.setSpacing(false);
    adminRecoveryInfo.setAlignItems(Alignment.START);
    adminRecoveryInfo.addClassName("support-help-notice");
    adminStatusNote.setId("support-ticket-status-note");
    adminStatusNote.addClassName("support-admin-status-note");

    userMessageThread.setSpacing(false);
    userMessageThread.addClassName("support-message-thread");
    userMessageScroll.addClassName("support-message-scroll");
    userConversationHeading.addClassName(CONVERSATION_HEADING_CLASS);
    userConversationCount.addClassName(SECTION_COUNT_CLASS);
    userConversationHeader.setAlignItems(Alignment.CENTER);
    userConversationHeader.setSpacing(false);
    userConversationHeader.addClassName(SECTION_HEADER_CLASS);
    ticketConversationLayout.add(userConversationHeader, userMessageScroll, adminStatusNote);
    sendReplyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    HorizontalLayout actions = new HorizontalLayout(closeTicketButton, sendReplyButton);
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
    adminEditor.add(replyMode, sensitiveNotice, messageField);
    VerticalLayout content =
        new VerticalLayout(adminRecoveryInfo, ticketConversationLayout, adminEditor);
    content.setPadding(false);
    content.setWidthFull();
    content.addClassName("support-admin-detail-content");
    adminDetail.setId("support-ticket-panel");
    adminDetail.addClassNames(CosmosClasses.PANEL_CARD, "support-admin-detail");
    adminDetail.add(ticketHeader, adminTicketTools, content, adminActionFooter);
  }

  private VerticalLayout buildAdminDetailColumn() {
    noSelectionTitle.addClassName("cosmos-notification-empty-title");
    selectTicketHint.addClassName(CosmosClasses.PANEL_DESCRIPTION);
    ticketSelectionPlaceholder.add(
        AppIcon.MOUSE_POINTER_CLICK.create(), noSelectionTitle, selectTicketHint);
    ticketSelectionPlaceholder.setAlignItems(Alignment.CENTER);
    ticketSelectionPlaceholder.setJustifyContentMode(JustifyContentMode.CENTER);
    ticketSelectionPlaceholder.setSpacing(false);
    ticketSelectionPlaceholder.setPadding(false);
    ticketSelectionPlaceholder.setWidthFull();
    ticketSelectionPlaceholder.setId("support-select-ticket");
    ticketSelectionPlaceholder.addClassNames(
        "support-admin-no-selection", "cosmos-notification-empty");
    selectTicketHint.getElement().setAttribute("role", "status");
    VerticalLayout ticketDetailColumn = new VerticalLayout(ticketSelectionPlaceholder, adminDetail);
    ticketDetailColumn.setPadding(false);
    ticketDetailColumn.setSpacing(false);
    ticketDetailColumn.setFlexGrow(1, ticketSelectionPlaceholder);
    ticketDetailColumn.addClassName("support-admin-detail-column");
    return ticketDetailColumn;
  }

  private void setTicketFocused(boolean focused) {
    ticketFocused = focused && selectedTicket != null;
    adminQueue.setVisible(!ticketFocused);
    adminWorkspace.setClassName("support-focused", ticketFocused);
    updateFocusControl();
  }

  private void updateFocusControl() {
    toggleTicketFocusButton.setPrefixComponent(
        (ticketFocused ? AppIcon.MINIMIZE : AppIcon.EXPAND).create());
    toggleTicketFocusButton
        .getElement()
        .setAttribute("aria-pressed", Boolean.toString(ticketFocused));
    String label =
        ticketFocused
            ? messageProperties.getSupportRestoreSplitView()
            : messageProperties.getSupportFocusTicket();
    toggleTicketFocusButton.setAriaLabel(label);
    toggleTicketFocusButton.setTooltipText(label);
    if (ticketFocused) {
      toggleTicketFocusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      toggleTicketFocusButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
  }

  public void refreshTexts() {
    ticketWorkspacePanel.setHeading(
        administration
            ? messageProperties.getSupportTitle()
            : messageProperties.getSupportHelpTitle(),
        administration
            ? messageProperties.getSupportInboxDescription()
            : messageProperties.getSupportHelpSubtitle(),
        "");
    emptyText.setText(messageProperties.getSupportEmpty());
    userExplanation.setText(messageProperties.getSupportNoUnlock());
    ticketMessageInput.setPlaceholder(messageProperties.getSupportRequestPlaceholder());
    if (administration) {
      adminBreadcrumb.setText(
          messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getSupportTitle());
      adminPageTitle.setText(messageProperties.getSupportTitle());
      adminPageDescription.setText(messageProperties.getSupportPageDescription());
      queueHeading.setText(messageProperties.getSupportCases());
      queueEmptyTitle.setText(messageProperties.getSupportQueueEmptyTitle());
      filterButton.setText(messageProperties.getAdminUsersAddFilter());
      ticketFilterPanel.refreshTexts();
      ticketPagination.refreshTexts();
      allTicketsLabel.setText(messageProperties.getSupportAllTickets());
      recoveryTicketsLabel.setText(messageProperties.getSupportRecoveryCategory());
      ticketCategories.getElement().setAttribute(ARIA_LABEL, messageProperties.getSupportTitle());
      noSelectionTitle.setText(messageProperties.getSupportNoTicketSelected());
      selectTicketHint.setText(messageProperties.getSupportSelectTicket());
      ticketGrid.getElement().setAttribute(ARIA_LABEL, messageProperties.getSupportCases());
    } else {
      userRequestsHeading.setText(messageProperties.getSupportExistingRequests());
      ticketGrid
          .getElement()
          .setAttribute(ARIA_LABEL, messageProperties.getSupportExistingRequests());
    }
    userWarningLabel.setText(messageProperties.getSupportSecurityNoticeLabel() + " ");
    securityWarningSpan.setText(messageProperties.getSupportSecurityNotice());
    ticketMessageInput.setLabel(
        administration
            ? messageProperties.getSupportReply()
            : messageProperties.getSupportDescription());
    publicReplyLabel.setText(messageProperties.getSupportPublicReply());
    internalNoteLabel.setText(messageProperties.getSupportInternal());
    replyMode.getElement().setAttribute(ARIA_LABEL, messageProperties.getSupportEditorMode());
    watchTicketSwitch.setLabel(messageProperties.getSupportSubscription());
    watchTicketSwitch.setTooltipText(messageProperties.getSupportSubscriptionHint());
    createRequestButton.setText(messageProperties.getSupportCreate());
    claimTicketButton.setText(messageProperties.getSupportClaim());
    sendReplyButton.setText(
        administration
            ? messageProperties.getSupportReply()
            : messageProperties.getSupportSendReply());
    closeTicketButton.setText(
        administration
            ? messageProperties.getSupportClose()
            : messageProperties.getSupportCloseOwn());
    sendInstructionsButton.setText(messageProperties.getSupportInstructions());
    reloadTicketsButton.setAriaLabel(messageProperties.getSessionRefresh());
    reloadTicketsButton.setTooltipText(messageProperties.getSessionRefresh());
    if (administration) {
      updateAdminComposerLabels();
      updateFocusControl();
      adminPolicyLabel.setText(messageProperties.getSupportSecurityPolicy());
    }
    if (instructionDialog.isOpened() && instructionPreview != null) {
      instructionPreview.setInstructions(messageProperties.getSupportRecoveryInstructions());
    }
    instructionDialog.refreshTexts();
    refreshTicketWorkspace();
    ticketGrid.getDataProvider().refreshAll();
  }

  private boolean isInternalNote() {
    return replyMode.getSelectedTab() == internalNoteMode;
  }

  private void updateAdminComposerLabels() {
    boolean privateNote = isInternalNote();
    adminEditor.getElement().setAttribute("data-internal", Boolean.toString(privateNote));
    securityWarningSpan.setText(
        privateNote
            ? messageProperties.getSupportInternalSecurityNotice()
            : messageProperties.getSupportSecurityNotice());
    ticketMessageInput.setLabel(
        privateNote ? messageProperties.getSupportInternal() : messageProperties.getSupportReply());
    ticketMessageInput.setPlaceholder(
        privateNote
            ? messageProperties.getSupportInternalPlaceholder()
            : messageProperties.getSupportReplyPlaceholder());
    sendReplyButton.setText(
        privateNote
            ? messageProperties.getSupportSaveInternal()
            : messageProperties.getSupportReply());
  }

  public void refreshTicketWorkspace() {
    refreshTicketWorkspace(false);
  }

  public void openTicket(String number) {
    openTicket(number, false);
  }

  private boolean jumpToTicket(String number) {
    return openTicket(number.startsWith("#") ? number.substring(1) : number, true);
  }

  private boolean openTicket(String number, boolean directJump) {
    List<Ticket> ticketsToDisplay =
        administration ? List.of() : supportTicketService.listTicketsForViewer(httpRequest, false);
    Ticket matchingTicket =
        administration
            ? supportTicketService.findForAdministration(httpRequest, number)
            : ticketsToDisplay.stream()
                .filter(ticket -> ticketNumber(ticket).equals("#" + number))
                .findFirst()
                .orElse(null);
    if (matchingTicket == null && directJump) {
      failed(messageProperties::getSupportTicketUnavailable);
      return false;
    }
    if (administration
        && matchingTicket != null
        && (directJump || !ticketFilterPanel.matches(matchingTicket, administratorId))) {
      ticketFilterPanel.resetApplied();
    }
    if (administration && matchingTicket != null && matchingTicket.type() != Type.RECOVERY) {
      ticketCategories.setSelectedTab(allTickets);
    }
    if (administration) {
      if (matchingTicket != null) {
        currentPage =
            supportTicketService.findTicketPageIndex(
                httpRequest, matchingTicket.id(), inboxQuery().filter(), pageSize);
      }
      selectedTicket = matchingTicket;
      applyInbox(
          supportTicketService.loadTicketInbox(httpRequest, selectedTicketId(), inboxQuery()));
      if (matchingTicket == null) {
        failed(messageProperties::getSupportTicketUnavailable);
      }
      return matchingTicket != null;
    }
    applyTickets(ticketsToDisplay, matchingTicket);
    refreshConversation();
    if (matchingTicket == null) {
      failed(messageProperties::getSupportTicketUnavailable);
    }
    return matchingTicket != null;
  }

  private InboxQuery inboxQuery() {
    return new InboxQuery(
        ticketFilterPanel
            .appliedFilter()
            .withType(ticketCategories.getSelectedTab() == recoveryTickets ? Type.RECOVERY : null),
        currentPage,
        pageSize);
  }

  private void applyInbox(LiveView state) {
    administratorId = state.viewerId();
    liveViewerId = state.viewerId();
    currentPage = state.page();
    ticketPagination.showPage(state.page(), state.totalTickets(), state.pageSize());
    applyTickets(state.visibleTickets(), state.selected());
    showConversation(state.messages(), null);
  }

  public void setNavigationListener(SerializableConsumer<NavigationState> listener) {
    navigationListener = Objects.requireNonNull(listener);
  }

  /** URL-Zustand einlesen, ohne beim Wiederherstellen neue Browserhistorie zu schreiben. */
  public void restoreNavigation(Category category, String number) {
    restoringNavigation = true;
    try {
      ticketCategories.setSelectedTab(category == Category.RECOVERY ? recoveryTickets : allTickets);
      selectedTicket = null;
      if (number == null) {
        refreshTicketWorkspace();
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
        category, selectedTicket == null ? null : ticketNumber(selectedTicket).substring(1));
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
        supportTicketService
            .events()
            .register(change -> {
              Long viewer = liveViewerId;
              if (change.affects(viewer, administration)) {
                ui.access(() -> {
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
      LiveView state =
          supportTicketService.liveView(
              session,
              administration,
              selectedTicketId(),
              administration ? inboxQuery() : InboxQuery.all());
      liveViewerId = state.viewerId();
      if (administration) {
        applyInbox(state);
        return;
      }
      applyTickets(state.visibleTickets(), state.selected());
      showConversation(state.messages(), state.requester());
    } catch (AccessDeniedException _) {
      // Die Sitzungs-/Rollenprüfung hat Vorrang vor dem Nachladen per Push.
    } catch (RuntimeException failure) {
      log.warn("Ticket refresh failed", safeFailure(failure));
    }
  }

  private void refreshTicketWorkspace(boolean notifySuccess) {
    try {
      if (administration) {
        applyInbox(
            supportTicketService.loadTicketInbox(httpRequest, selectedTicketId(), inboxQuery()));
      } else {
        List<Ticket> ticketsToDisplay =
            supportTicketService.listTicketsForViewer(httpRequest, false);
        applyTickets(
            ticketsToDisplay,
            SupportTicketService.selectTicket(ticketsToDisplay, selectedTicketId(), false));
        refreshConversation();
      }
      if (notifySuccess) {
        succeeded(messageProperties::getSupportRefreshed);
      }
    } catch (RuntimeException _) {
      failed(messageProperties::getSupportRefreshFailed);
    }
  }

  private Long selectedTicketId() {
    return selectedTicket == null ? null : selectedTicket.id();
  }

  private void applyTickets(List<Ticket> ticketsToDisplay, Ticket ticketToSelect) {
    if (!administration) {
      userRequestCount.setText(messageProperties.getSupportRequestCount(ticketsToDisplay.size()));
      emptyState.setVisible(ticketsToDisplay.isEmpty());
      reloadTicketsButton.setVisible(!ticketsToDisplay.isEmpty());
    } else {
      int count = ticketFilterPanel.activeCount();
      activeFilterCount.setText(Integer.toString(count));
      activeFilterCount.setVisible(count > 0);
      filterButton.setAriaLabel(
          count == 0
              ? messageProperties.getAdminUsersAddFilter()
              : messageProperties.getAdminUsersAddFilter()
                  + ", "
                  + messageProperties.getAdminUsersActiveFilterCount(count));
      queueEmptyHint.setText(
          count > 0
              ? messageProperties.getSupportFilterEmpty()
              : messageProperties.getSupportQueueEmptyHint());
      queueEmptyState.setVisible(ticketsToDisplay.isEmpty());
    }
    ticketGrid.setVisible(!ticketsToDisplay.isEmpty());
    adminTicketScroll.setVisible(!ticketsToDisplay.isEmpty());
    hasOpenRequest =
        ticketsToDisplay.stream().anyMatch(ticket -> ticket.status().acceptsMessages());
    // Gleiche Daten beim Kategorienwechsel sollen Fokus und Scrollposition im Grid behalten.
    if (!visibleTickets.equals(ticketsToDisplay)) {
      visibleTickets = List.copyOf(ticketsToDisplay);
      ticketGrid.setItems(visibleTickets).setIdentifierProvider(Ticket::id);
    }
    Ticket detailSelection = ticketToSelect;
    Ticket rowSelection =
        ticketsToDisplay.stream()
            .filter(ticket -> detailSelection != null && ticket.id().equals(detailSelection.id()))
            .findFirst()
            .orElse(null);
    ticketGrid.asSingleSelect().setValue(rowSelection);
    selectedTicket = ticketToSelect;
    publishNavigation();
  }

  public void addBackAction(Button action) {
    if (administration) {
      throw new IllegalStateException("Recovery navigation belongs to the user's request form");
    }
    backActions.add(action);
  }

  public void addUserFooterAction(Button action) {
    if (administration) {
      throw new IllegalStateException("Recovery actions belong to the user's request form");
    }
    userFooterActions.addComponentAtIndex(0, action);
  }

  private Component ticketRow(Ticket ticket) {
    Span title = new Span(ticketSubject(ticket));
    title.addClassName("support-ticket-title");
    Span updated = new Span(formatTime(ticket.createdAt()));
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
      Span unassigned = new Span(messageProperties.getSupportUnassigned());
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
      Badge you = new Badge(messageProperties.getSupportYou());
      you.addThemeVariants(BadgeVariant.SUCCESS);
      you.addClassName("support-assignee-self");
      identity.add(you);
    }
    return identity;
  }

  private OverlayUserIcon identityAvatar(UserIdentity identity, IconSize iconSize) {
    OverlayUserIcon avatar = new OverlayUserIcon(messageProperties, null, null, iconSize);
    avatar.setName(identity.firstName(), identity.lastName());
    avatar.showIdentityTooltip(identity.email());
    return avatar;
  }

  private String ticketSubject(Ticket ticket) {
    return switch (ticket.type()) {
      case RECOVERY -> messageProperties.getSupportRecoverySubject();
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
          .setAttribute(
              ARIA_LABEL, messageProperties.getSupportUnreadMessages(ticket.unreadMessages()));
      indicators.add(unread);
    }
    return indicators;
  }

  private void refreshConversation() {
    List<Reply> messages =
        selectedTicket == null
            ? List.of()
            : supportTicketService.loadTicketConversation(
                httpRequest, selectedTicket.id(), administration);
    UserIdentity requester =
        !administration && !messages.isEmpty()
            ? supportTicketService.requesterIdentity(httpRequest)
            : null;
    showConversation(messages, requester);
  }

  private void showConversation(List<Reply> messages, UserIdentity requester) {
    boolean active = selectedTicket != null && selectedTicket.status().acceptsMessages();
    boolean unassigned = selectedTicket != null && selectedTicket.assignee() == null;
    boolean assignedToMe =
        selectedTicket != null
            && administratorId != null
            && administratorId.equals(selectedTicket.assignee());
    boolean canReply = active && (!administration || assignedToMe);
    boolean requesterClosed =
        selectedTicket != null && selectedTicket.status() == Status.CLOSED_BY_REQUESTER;
    updateEditor(canReply);
    updateConversationActions(active, unassigned, assignedToMe, canReply, requesterClosed);
    if (!administration) {
      refreshUserConversation(messages, requester);
    } else {
      refreshAdminConversation(messages);
    }
    acknowledgeVisibleMessages(messages);
  }

  private void updateEditor(boolean canReply) {
    if (canReply) {
      switchDraft(new DraftTarget(selectedTicket.id()));
    } else {
      switchDraft(!administration && !hasOpenRequest ? NEW_REQUEST_DRAFT : null);
    }
    if (!administration) {
      ticketMessageInput.setLabel(
          hasOpenRequest
              ? messageProperties.getSupportReply()
              : messageProperties.getSupportDescription());
      userEditor.setVisible(!hasOpenRequest || canReply);
    } else {
      adminEditor.setVisible(canReply);
      watchTicketSwitch.setVisible(selectedTicket != null && selectedTicket.subscriptionAllowed());
      watchTicketSwitch.setValue(selectedTicket != null && selectedTicket.subscribed());
    }
  }

  private void updateConversationActions(
      boolean active,
      boolean unassigned,
      boolean assignedToMe,
      boolean canReply,
      boolean requesterClosed) {
    createRequestButton.setVisible(!administration && !hasOpenRequest);
    claimTicketButton.setVisible(administration && active && unassigned);
    if (selectedTicket != null && selectedTicket.status() == Status.OPEN) {
      claimTicketButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      claimTicketButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    sendReplyButton.setVisible(canReply);
    closeTicketButton.setVisible(
        administration ? canReply || requesterClosed && (unassigned || assignedToMe) : active);
    sendInstructionsButton.setVisible(administration && canReply);
    sendInstructionsButton.setEnabled(selectedTicket != null && !selectedTicket.instructionSent());
    if (administration) {
      adminTicketTools.setVisible(
          watchTicketSwitch.isVisible()
              || claimTicketButton.isVisible()
              || sendInstructionsButton.isVisible());
      adminActionFooter.setVisible(closeTicketButton.isVisible() || sendReplyButton.isVisible());
    }
  }

  private void refreshAdminConversation(List<Reply> messages) {
    adminDetail.setVisible(selectedTicket != null);
    ticketSelectionPlaceholder.setVisible(selectedTicket == null);
    if (selectedTicket == null) {
      setTicketFocused(false);
      userMessageThread.removeAll();
      return;
    }
    adminTicketNumber.setText(
        messageProperties.getSupportTicketNumber(ticketNumber(selectedTicket)));
    adminSubject.setText(ticketSubject(selectedTicket));
    setStatusBadge(adminStatus, selectedTicket.status());
    adminUpdated.setText(formatTime(selectedTicket.createdAt()));
    adminAssignment.removeAll();
    Span assignmentLabel = new Span(messageProperties.getSupportAssigneeColumn());
    assignmentLabel.addClassName("support-admin-person-label");
    Component assignee;
    if (selectedTicket.assigneeIdentity() != null
        && !Objects.equals(selectedTicket.assignee(), administratorId)) {
      HorizontalLayout identity = adminPersonIdentity(selectedTicket.assigneeIdentity());
      identity.setWidth(null);
      assignee = identity;
    } else {
      assignee = assigneeAvatar(selectedTicket, IconSize.PIXEL_32);
    }
    assignee.addClassName("support-admin-assignee-identity");
    adminAssignment.add(assignmentLabel, assignee);
    updateAdminRequester(selectedTicket.requesterIdentity());
    adminRecoveryHint.setText(messageProperties.getSupportNoUnlock());
    adminRecoveryInfo.setVisible(selectedTicket.type() == Type.RECOVERY);
    String statusHint =
        switch (selectedTicket.status()) {
          case OPEN ->
              selectedTicket.assignee() == null ? messageProperties.getSupportUnassignedHint() : "";
          case IN_PROGRESS -> "";
          case CLOSED -> messageProperties.getSupportClosedHint();
          case CLOSED_BY_REQUESTER -> messageProperties.getSupportRequesterClosedHint();
        };
    adminStatusNote.setText(statusHint);
    adminStatusNote.setVisible(!statusHint.isEmpty());
    String heading = messageProperties.getSupportConversation();
    userConversationHeading.setText(heading);
    userConversationCount.setText(
        messageProperties.getSupportMessageCount(
            (int) messages.stream().filter(message -> !message.kind().isHistory()).count()));
    userMessageScroll
        .getElement()
        .setAttribute(ARIA_LABEL, heading + " " + ticketNumber(selectedTicket));
    renderMessages(messages, selectedTicket.requesterIdentity());
  }

  private void updateAdminRequester(UserIdentity requester) {
    adminRequester.removeAll();
    adminRequester.setVisible(requester != null);
    if (requester == null) {
      return;
    }
    Span label = new Span(messageProperties.getSupportRequester());
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
    Ticket ticket = selectedTicket;
    if (session == null
        || ticket == null
        || !administration && ticket.unreadMessages() == 0
        || messages.isEmpty()) {
      return;
    }
    Long lastReplyId = messages.stream().map(Reply::id).max(Long::compareTo).orElseThrow();
    // Im Hintergrund geöffnete Tabs quittieren erst, sobald der Verlauf sichtbar wird.
    ticketConversationLayout
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
                  || !Objects.equals(selectedTicketId(), ticket.id())) {
                return;
              }
              try {
                supportTicketService.markSeen(
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
      if (ticketMessageInput.isEmpty()) {
        drafts.remove(draftTarget);
      } else {
        drafts.put(draftTarget, new Draft(ticketMessageInput.getValue(), isInternalNote()));
      }
    }
    draftTarget = target;
    Draft draft = drafts.remove(target);
    ticketMessageInput.setValue(draft == null ? "" : draft.text());
    ticketMessageInput.setInvalid(false);
    replyMode.setSelectedTab(
        draft != null && draft.internal() ? internalNoteMode : publicReplyMode);
  }

  private void refreshUserConversation(List<Reply> messages, UserIdentity requester) {
    boolean showConversation = !messages.isEmpty();
    HorizontalLayout refreshActions =
        showConversation ? userConversationActions : userRequestsHeader;
    if (reloadTicketsButton.getParent().filter(refreshActions::equals).isEmpty()) {
      refreshActions.add(reloadTicketsButton);
    }
    ticketConversationLayout.setVisible(showConversation);
    if (!showConversation) {
      userMessageThread.removeAll();
      return;
    }
    String heading = messageProperties.getSupportConversation() + " ";
    userConversationHeading.removeAll();
    userConversationHeading.add(new Text(heading), ticketNumberLabel(selectedTicket));
    userConversationCount.setText(
        messageProperties.getSupportMessageCount(
            (int) messages.stream().filter(message -> !message.kind().isHistory()).count()));
    userMessageScroll.getElement().setAttribute(ARIA_LABEL, heading + ticketNumber(selectedTicket));
    renderMessages(messages, requester);
  }

  private void renderMessages(List<Reply> messages, UserIdentity requester) {
    userMessageThread.removeAll();
    for (Reply message : messages) {
      if (message.kind().isHistory()) {
        userMessageThread.add(historyEntry(message));
        continue;
      }
      HorizontalLayout metadata = messageMetadata(message, requester);
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

  private HorizontalLayout messageMetadata(Reply message, UserIdentity requester) {
    Span timestamp = new Span("· " + formatTime(message.createdAt()));
    timestamp.addClassName("support-message-time");
    HorizontalLayout metadata = new HorizontalLayout();
    metadata.setAlignItems(Alignment.CENTER);
    metadata.setSpacing(false);
    metadata.addClassName("support-message-metadata");
    addMessageSender(metadata, message, requester);
    metadata.add(timestamp);
    if (message.internal()) {
      Badge note = new Badge(messageProperties.getSupportInternal());
      note.setIcon(AppIcon.LOCK.create());
      note.addClassName("support-admin-internal");
      metadata.add(note);
    }
    return metadata;
  }

  private void addMessageSender(HorizontalLayout metadata, Reply message, UserIdentity requester) {
    switch (message.author()) {
      case REQUESTER -> {
        if (requester == null) {
          metadata.add(AppIcon.USER.create(), new Span(messageProperties.getSupportRequester()));
        } else {
          metadata.add(identityAvatar(requester, IconSize.PIXEL_24));
          if (administration) {
            metadata.add(new Span(identityName(requester)));
          }
        }
      }
      case SUPPORT -> {
        UserIdentity administrator = administration ? message.actor() : null;
        Span sender =
            new Span(
                administrator == null
                    ? messageProperties.getSupportMessageSupport()
                    : identityName(administrator));
        sender.addClassName("support-message-sender");
        Component symbol =
            administrator == null
                ? AppIcon.SHIELD_COG.create()
                : identityAvatar(administrator, IconSize.PIXEL_24);
        metadata.add(symbol, sender);
      }
    }
  }

  private Component historyEntry(Reply event) {
    HorizontalLayout summary = new HorizontalLayout();
    summary.setPadding(false);
    summary.setSpacing(false);
    summary.setAlignItems(Alignment.CENTER);
    SvgIcon eventIcon =
        (event.kind() == Kind.TICKET_CREATED ? AppIcon.MESSAGE_SQUARE_PLUS : AppIcon.STATUS_CHANGE)
            .create();
    eventIcon.addClassName("support-history-event-icon");
    summary.add(eventIcon);
    if (event.kind() == Kind.TICKET_CREATED) {
      summary.add(new Span(messageProperties.getSupportTicketCreated()));
    } else {
      summary.add(new Span(messageProperties.getSupportStatusChanged()));
      Badge previous = new Badge();
      setStatusBadge(previous, event.previousStatus());
      Badge next = new Badge();
      setStatusBadge(next, event.nextStatus());
      summary.add(previous, AppIcon.ARROW_FORWARD.create(), next);
    }
    Span time = new Span(formatTime(event.createdAt()));
    time.addClassName("support-message-time");
    String actor =
        event.actor() == null
            ? messageProperties.getSupportMessageSupport()
            : identityName(event.actor());
    HorizontalLayout metadata = new HorizontalLayout(time, new Span("·"), new Span(actor));
    metadata.setPadding(false);
    metadata.setSpacing(false);
    metadata.addClassName("support-history-metadata");
    VerticalLayout entry = new VerticalLayout(summary, metadata);
    entry.setPadding(false);
    entry.setSpacing(false);
    entry.addClassName("support-history-event");
    entry.getElement().setAttribute("data-event", event.kind().name());
    return entry;
  }

  private Component instructionMessage(Reply message) {
    RecoveryInstructionsComponent guide = new RecoveryInstructionsComponent(message.body());
    Details details = new Details("", guide);
    details.setId("support-instruction-" + message.id());
    details.setOpened(expandedInstructions.contains(message.id()));
    details.addClassName("support-instruction-details");
    details.addOpenedChangeListener(event -> {
      if (event.isOpened()) {
        expandedInstructions.add(message.id());
      } else {
        expandedInstructions.remove(message.id());
      }
    });
    Span title = new Span(messageProperties.getSupportInstructionMessageTitle());
    title.addClassName("support-instruction-title");
    HorizontalLayout heading = new HorizontalLayout(AppIcon.ENVELOPE.create(), title);
    heading.setSpacing(false);
    heading.setPadding(false);
    heading.setAlignItems(Alignment.CENTER);
    heading.addClassName("support-instruction-heading");
    Button toggle = new DetailsToggleButton(messageProperties, details, title.getText());
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
      supportTicketService.validateInstructionPreview(httpRequest, selectedTicket.id());
      RecoveryInstructionsComponent guide =
          new RecoveryInstructionsComponent(messageProperties.getSupportRecoveryInstructions());
      instructionPreview = guide;
      Ticket target = selectedTicket;
      instructionDialog.open(
          new PortalConfirmationDialog.Prompt(
              messageProperties::getSupportInstructions,
              () -> "",
              guide::getHeading,
              guide::getDescription,
              () -> guide,
              () -> "",
              messageProperties::getSupportInstructions,
              false,
              AppIcon.ENVELOPE::create),
          () -> {
            try {
              supportTicketService.sendTicketRecoveryInstructions(
                  httpRequest, target.id(), target.version());
              refreshTicketWorkspace();
              NotificationHelper.getInstance()
                  .showNotification(
                      messageProperties::getSupportQueued,
                      NotificationHelper.NotificationType.SUCCESS);
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
      selectedTicket = action.get();
      if (clearDraft) {
        ticketMessageInput.clear();
        ticketMessageInput.setInvalid(false);
        replyMode.setSelectedTab(publicReplyMode);
      }
      succeeded(success);
      refreshTicketWorkspace();
    } catch (RuntimeException failure) {
      handleActionFailure(failure);
    }
  }

  private void handleActionFailure(RuntimeException failure) {
    if (failure instanceof OptimisticLockingFailureException) {
      refreshTicketWorkspace();
      failed(messageProperties::getSupportConflict);
      return;
    }
    failed(messageProperties::getSupportFailed);
  }

  private void succeeded(Supplier<String> message) {
    NotificationHelper.getInstance()
        .showNotification(message, NotificationHelper.NotificationType.SUCCESS);
  }

  private void failed(Supplier<String> message) {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            NotificationHelper.NotificationType.ERROR);
  }

  private String status(Status value) {
    return switch (value) {
      case OPEN -> messageProperties.getSupportOpen();
      case IN_PROGRESS -> messageProperties.getSupportInProgress();
      case CLOSED -> messageProperties.getSupportClosed();
      case CLOSED_BY_REQUESTER -> messageProperties.getSupportClosedByRequester();
    };
  }
}
