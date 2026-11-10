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

  // Vorgangsliste und Admin-Detailbereich
  private final Grid<Ticket> ticketGrid = new Grid<>();
  private final Scroller adminTicketScroll = new Scroller();
  private final Span allTicketsLabelSpan = new Span();
  private final Span recoveryTicketsLabelSpan = new Span();
  private final Tab allTickets = new Tab(AppIcon.MESSAGES.create(), allTicketsLabelSpan);
  private final Tab recoveryTickets =
      new Tab(AppIcon.LOCK_QUESTION.create(), recoveryTicketsLabelSpan);
  private final Tabs ticketCategories = new Tabs(allTickets, recoveryTickets);
  private final H3 queueHeading = new H3();
  private final Span adminBreadcrumbSpan = new Span();
  private final Span adminPageTitleSpan = new Span();
  private final Span adminPageDescriptionSpan = new Span();
  private final H3 noSelectionTitle = new H3();
  private final Span selectTicketHintSpan = new Span();
  private final VerticalLayout ticketSelectionPlaceholder = new VerticalLayout();
  private final CosmosCardComponent adminDetail = new CosmosCardComponent();
  private final Div adminWorkspaceDiv = new Div();
  private final VerticalLayout adminQueueLayout = new VerticalLayout();
  private final Button toggleTicketFocusButton = new Button();
  private final HorizontalLayout adminTicketTools = new HorizontalLayout();
  private final HorizontalLayout adminActionFooter = new HorizontalLayout();
  private final VerticalLayout adminEditorLayout = new VerticalLayout();
  private final Badge adminStatus = new Badge();
  private final Span adminTicketNumber = new Span();
  private final Span adminUpdated = new Span();
  private final H3 adminSubject = new H3();
  private final VerticalLayout adminRequesterLayout = new VerticalLayout();
  private final VerticalLayout adminAssignmentLayout = new VerticalLayout();
  private final Span adminRecoveryHintSpan = new Span();
  private final Span adminPolicyLabelSpan = new Span();
  private final HorizontalLayout adminRecoveryInfoLayout = new HorizontalLayout();
  private final Span adminStatusNoteSpan = new Span();

  // Verlauf und Eingabe für Antworten
  private final VerticalLayout ticketConversationLayout = new VerticalLayout();
  private final TextArea ticketMessageInput = new TextArea();
  private final Span publicReplyLabelSpan = new Span();
  private final Span internalNoteLabelSpan = new Span();
  private final Tab publicReplyMode = new Tab(AppIcon.MESSAGE.create(), publicReplyLabelSpan);
  private final Tab internalNoteMode = new Tab(AppIcon.LOCK.create(), internalNoteLabelSpan);
  private final Tabs replyMode = new Tabs(publicReplyMode, internalNoteMode);
  private final Switch watchTicketSwitch = new Switch();
  private final Button createRequestButton = new Button();
  private final Button claimTicketButton = new Button();
  private final Button sendReplyButton = new Button();
  private final Button closeTicketButton = new Button();
  private final Button sendInstructionsButton = new Button();

  // Vorgangsliste und Filter
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

  // Darstellung in der Wiederherstellungshilfe
  private final Span securityWarningSpan = new Span();
  private final Span userWarningLabelSpan = new Span();
  private final Span emptyText = new Span();
  private final Span queueEmptyTitle = new Span();
  private final Span queueEmptyHintSpan = new Span();
  private final Span userExplanationSpan = new Span();
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
  private final VerticalLayout userEditorLayout = new VerticalLayout();
  private final Div messageField = new Div();

  // Auswahl, Entwürfe und URL-Zustand
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

  // Registrierung für Aktualisierungen während der Sitzung
  private transient Registration liveTicketRegistration;
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
      buildUserWorkspace(sensitiveNotice);
    }

    add(ticketWorkspacePanel, instructionDialog);
    refreshTexts();
  }

  private void buildUserWorkspace(HorizontalLayout sensitiveNoticeLayout) {
    addClassName("support-recovery-help");
    ticketWorkspacePanel.setIcon(AppIcon.LOCK_QUESTION.create());
    ticketConversationLayout.addClassName("support-user-conversation");
    userExplanationSpan.setId("support-no-unlock");

    HorizontalLayout recoveryHelpNoticeLayout =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), userExplanationSpan);
    recoveryHelpNoticeLayout.setPadding(false);
    recoveryHelpNoticeLayout.setSpacing(false);
    recoveryHelpNoticeLayout.setAlignItems(Alignment.CENTER);
    recoveryHelpNoticeLayout.addClassName("support-help-notice");

    emptyState.add(AppIcon.ARCHIVE.create(), emptyText);
    emptyState.setPadding(false);
    emptyState.setSpacing(false);
    emptyState.setWidthFull();
    emptyState.setAlignItems(Alignment.CENTER);
    emptyState.setJustifyContentMode(JustifyContentMode.CENTER);
    emptyState.addClassName("support-empty-state");

    userEditorLayout.setPadding(false);
    userEditorLayout.setSpacing(false);
    userEditorLayout.setWidthFull();
    userEditorLayout.addClassName("support-user-editor");
    userEditorLayout.add(sensitiveNoticeLayout, messageField);

    userRequestsHeading.addClassName(CONVERSATION_HEADING_CLASS);
    userConversationHeading.addClassName(CONVERSATION_HEADING_CLASS);
    userRequestCount.setId("support-request-count");
    userRequestCount.addClassNames(SECTION_COUNT_CLASS, "support-request-count");
    userConversationCount.addClassName(SECTION_COUNT_CLASS);

    for (HorizontalLayout conversationSectionHeaderLayout :
        List.of(userRequestsHeader, userConversationHeader)) {
      conversationSectionHeaderLayout.setWidthFull();
      conversationSectionHeaderLayout.setSpacing(false);
      conversationSectionHeaderLayout.setAlignItems(Alignment.CENTER);
      conversationSectionHeaderLayout.addClassName(SECTION_HEADER_CLASS);
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

    VerticalLayout userRequestsLayout =
        new VerticalLayout(userRequestsHeader, emptyState, ticketGrid);
    userRequestsLayout.setPadding(false);
    userRequestsLayout.setSpacing(false);
    userRequestsLayout.addClassName("support-user-requests");

    VerticalLayout userTicketFormLayout =
        new VerticalLayout(
            recoveryHelpNoticeLayout,
            userRequestsLayout,
            ticketConversationLayout,
            userEditorLayout);
    userTicketFormLayout.setPadding(false);
    userTicketFormLayout.setSpacing(false);
    userTicketFormLayout.setWidthFull();
    userTicketFormLayout.addClassName("support-user-form");
    ticketWorkspacePanel.getBody().add(userTicketFormLayout);

    createRequestButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendReplyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    userFooterActions.add(sendReplyButton, createRequestButton);
    userFooterActions.setPadding(false);
    userFooterActions.setSpacing(false);
    userFooterActions.addClassName("cosmos-dialog-actions");
    backActions.setPadding(false);
    backActions.setSpacing(false);
    backActions.addClassName("support-back-actions");

    HorizontalLayout userTicketFooterLayout = new HorizontalLayout(backActions, userFooterActions);
    userTicketFooterLayout.setPadding(false);
    userTicketFooterLayout.setSpacing(false);
    userTicketFooterLayout.setWidthFull();
    userTicketFooterLayout.setAlignItems(Alignment.CENTER);
    userTicketFooterLayout.addClassName("support-user-footer");

    ticketWorkspacePanel.addActionFooter(userTicketFooterLayout);
  }

  private HorizontalLayout sensitiveNotice() {
    userWarningLabelSpan.addClassName("cosmos-security-warning-label");
    HorizontalLayout sensitiveNoticeLayout =
        new HorizontalLayout(
            AppIcon.ERROR.create(), new Span(userWarningLabelSpan, securityWarningSpan));
    sensitiveNoticeLayout.setPadding(false);
    sensitiveNoticeLayout.setSpacing(false);
    sensitiveNoticeLayout.setAlignItems(Alignment.START);
    sensitiveNoticeLayout.addClassName("cosmos-security-warning");
    return sensitiveNoticeLayout;
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
        runTicketActionAndRefreshWorkspace(
            () -> supportTicketService.createRecovery(httpRequest, ticketMessageInput.getValue()),
            messageProperties::getSupportCreated,
            true);
      }
    });
    claimTicketButton.setId("support-claim");
    claimTicketButton.setPrefixComponent(AppIcon.CHECK.create());
    claimTicketButton.addClickListener(event ->
        runTicketActionAndRefreshWorkspace(
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
        runTicketActionAndRefreshWorkspace(
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
        runTicketActionAndRefreshWorkspace(
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
        new CosmosSectionHeaderComponent(
            adminBreadcrumbSpan, adminPageTitleSpan, adminPageDescriptionSpan);
    heading.setId("support-page-header");
    add(heading);
    ticketWorkspacePanel.setIcon(AppIcon.MESSAGES.create());
    ticketCategories.setId("support-ticket-categories");
    ticketCategories.addClassName(CosmosCssClasses.PANEL_TABS);
    ticketCategories.setWidthFull();
    ticketCategories.addSelectedChangeListener(event -> {
      currentPage = 0;
      refreshTicketWorkspace();
    });
    buildAdminTicketQueue();
    VerticalLayout ticketHeader = buildAdminTicketHeader();
    buildAdminTicketDetail(ticketHeader, sensitiveNotice);
    VerticalLayout ticketDetailColumn = buildAdminDetailColumn();
    adminWorkspaceDiv.add(adminQueueLayout, ticketDetailColumn);
    adminWorkspaceDiv.addClassName("support-admin-columns");
    ticketWorkspacePanel.getBody().add(ticketCategories, adminWorkspaceDiv);
  }

  private void buildAdminTicketQueue() {
    queueHeading.addClassName(CosmosCssClasses.PANEL_TITLE);
    HorizontalLayout queueIconLayout = new HorizontalLayout(AppIcon.LIST.create());
    queueIconLayout.setPadding(false);
    queueIconLayout.setSpacing(false);
    queueIconLayout.addClassName(CosmosCssClasses.PANEL_ICON);
    HorizontalLayout queueHeaderLayout = new HorizontalLayout(queueIconLayout, queueHeading);
    queueHeaderLayout.setId("support-queue-header");
    queueHeaderLayout.setPadding(false);
    queueHeaderLayout.setSpacing(false);
    queueHeaderLayout.setWidthFull();
    queueHeaderLayout.setAlignItems(Alignment.CENTER);
    queueHeaderLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    queueHeaderLayout.setFlexGrow(1, queueHeading);
    queueHeaderLayout.addClassName(CosmosCssClasses.PANEL_HEADER);
    configureAdminQueueFilter();
    HorizontalLayout queueToolsLayout = new HorizontalLayout(filterButton, reloadTicketsButton);
    queueToolsLayout.setId("support-queue-tools");
    queueToolsLayout.setPadding(false);
    queueToolsLayout.setSpacing(false);
    queueToolsLayout.setWidthFull();
    queueToolsLayout.setAlignItems(Alignment.CENTER);
    queueToolsLayout.setJustifyContentMode(JustifyContentMode.END);
    queueToolsLayout.addClassNames(SECTION_HEADER_CLASS, "support-admin-list-tools");
    queueEmptyTitle.addClassName("admin-grid-empty-title");
    queueEmptyState.add(AppIcon.LIST.create(), queueEmptyTitle, queueEmptyHintSpan);
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
    HorizontalLayout queueFooterLayout = new HorizontalLayout(ticketPagination);
    queueFooterLayout.setId("support-queue-footer");
    queueFooterLayout.setPadding(false);
    queueFooterLayout.setSpacing(false);
    queueFooterLayout.setWidthFull();
    queueFooterLayout.addClassName("cosmos-panel-footer");
    CosmosCardComponent queuePanel =
        new CosmosCardComponent(
            queueHeaderLayout,
            queueToolsLayout,
            queueEmptyState,
            adminTicketScroll,
            queueFooterLayout);
    queuePanel.setId("support-queue-panel");
    queuePanel.addClassNames(CosmosCssClasses.PANEL_CARD, "support-admin-list-panel");
    adminQueueLayout.add(queuePanel);
    adminQueueLayout.setPadding(false);
    adminQueueLayout.addClassName("support-admin-queue");
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
    adminSubject.addClassNames(CosmosCssClasses.PANEL_TITLE, "support-admin-subject");
    adminTicketNumber.addClassName("support-ticket-number");
    HorizontalLayout ticketMetadataLayout = new HorizontalLayout(adminTicketNumber, adminStatus);
    ticketMetadataLayout.setPadding(false);
    ticketMetadataLayout.setSpacing(false);
    ticketMetadataLayout.setAlignItems(Alignment.CENTER);
    ticketMetadataLayout.addClassName("support-admin-ticket-metadata");
    adminAssignmentLayout.setId("support-detail-assignee");
    adminAssignmentLayout.setPadding(false);
    adminAssignmentLayout.setSpacing(false);
    adminAssignmentLayout.setWidth(null);
    adminAssignmentLayout.setAlignItems(Alignment.END);
    adminAssignmentLayout.addClassName("support-admin-assignment");
    adminRequesterLayout.setId("support-detail-requester");
    adminRequesterLayout.setPadding(false);
    adminRequesterLayout.setSpacing(false);
    adminRequesterLayout.setWidth(null);
    adminRequesterLayout.addClassName("support-admin-requester");
    adminUpdated.addClassName("support-ticket-updated");
    HorizontalLayout ticketIconLayout = new HorizontalLayout(AppIcon.LOCK_QUESTION.create());
    ticketIconLayout.setPadding(false);
    ticketIconLayout.setSpacing(false);
    ticketIconLayout.addClassName(CosmosCssClasses.PANEL_ICON);
    toggleTicketFocusButton.setId("support-focus-toggle");
    toggleTicketFocusButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    toggleTicketFocusButton
        .getElement()
        .setAttribute("aria-controls", "support-queue-panel support-ticket-panel");
    toggleTicketFocusButton.addClickListener(event -> setTicketFocused(!ticketFocused));
    HorizontalLayout ticketOrientationLayout =
        new HorizontalLayout(
            ticketIconLayout, ticketMetadataLayout, adminUpdated, toggleTicketFocusButton);
    ticketOrientationLayout.setPadding(false);
    ticketOrientationLayout.setSpacing(false);
    ticketOrientationLayout.setWidthFull();
    ticketOrientationLayout.setAlignItems(Alignment.CENTER);
    ticketOrientationLayout.setFlexGrow(1, ticketMetadataLayout);
    ticketOrientationLayout.addClassName("support-admin-ticket-orientation");
    HorizontalLayout ticketPeopleLayout =
        new HorizontalLayout(adminRequesterLayout, adminAssignmentLayout);
    ticketPeopleLayout.setPadding(false);
    ticketPeopleLayout.setSpacing(false);
    ticketPeopleLayout.setWidthFull();
    ticketPeopleLayout.setAlignItems(Alignment.STRETCH);
    ticketPeopleLayout.setFlexGrow(1, adminRequesterLayout, adminAssignmentLayout);
    ticketPeopleLayout.addClassName("support-admin-ticket-people");
    VerticalLayout ticketHeaderLayout =
        new VerticalLayout(ticketOrientationLayout, adminSubject, ticketPeopleLayout);
    ticketHeaderLayout.setId("support-ticket-header");
    ticketHeaderLayout.setPadding(false);
    ticketHeaderLayout.setSpacing(false);
    ticketHeaderLayout.setWidthFull();
    ticketHeaderLayout.setAlignItems(Alignment.STRETCH);
    ticketHeaderLayout.addClassNames(CosmosCssClasses.PANEL_HEADER, "support-admin-ticket-header");
    return ticketHeaderLayout;
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
    adminPolicyLabelSpan.addClassName("support-admin-policy-label");
    Span ticketPolicySpan = new Span(adminPolicyLabelSpan, new Text(" "), adminRecoveryHintSpan);
    adminRecoveryInfoLayout.add(AppIcon.INFO_CIRCLE.create(), ticketPolicySpan);
    adminRecoveryInfoLayout.setId("support-ticket-policy");
    adminRecoveryInfoLayout.setPadding(false);
    adminRecoveryInfoLayout.setSpacing(false);
    adminRecoveryInfoLayout.setAlignItems(Alignment.START);
    adminRecoveryInfoLayout.addClassName("support-help-notice");
    adminStatusNoteSpan.setId("support-ticket-status-note");
    adminStatusNoteSpan.addClassName("support-admin-status-note");

    userMessageThread.setSpacing(false);
    userMessageThread.addClassName("support-message-thread");
    userMessageScroll.addClassName("support-message-scroll");
    userConversationHeading.addClassName(CONVERSATION_HEADING_CLASS);
    userConversationCount.addClassName(SECTION_COUNT_CLASS);
    userConversationHeader.setAlignItems(Alignment.CENTER);
    userConversationHeader.setSpacing(false);
    userConversationHeader.addClassName(SECTION_HEADER_CLASS);
    ticketConversationLayout.add(userConversationHeader, userMessageScroll, adminStatusNoteSpan);
    sendReplyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    HorizontalLayout ticketActionsLayout = new HorizontalLayout(closeTicketButton, sendReplyButton);
    ticketActionsLayout.setPadding(false);
    ticketActionsLayout.setSpacing(false);
    ticketActionsLayout.setWidthFull();
    ticketActionsLayout.addClassNames("cosmos-panel-actions", "support-admin-actions");
    adminActionFooter.setId("support-ticket-footer");
    adminActionFooter.setPadding(false);
    adminActionFooter.setSpacing(false);
    adminActionFooter.setWidthFull();
    adminActionFooter.addClassNames("cosmos-panel-footer", "support-admin-detail-footer");
    adminActionFooter.add(ticketActionsLayout);
    adminEditorLayout.setPadding(false);
    adminEditorLayout.addClassName("support-admin-editor");
    adminEditorLayout.add(replyMode, sensitiveNotice, messageField);
    VerticalLayout ticketDetailContentLayout =
        new VerticalLayout(adminRecoveryInfoLayout, ticketConversationLayout, adminEditorLayout);
    ticketDetailContentLayout.setPadding(false);
    ticketDetailContentLayout.setWidthFull();
    ticketDetailContentLayout.addClassName("support-admin-detail-content");
    adminDetail.setId("support-ticket-panel");
    adminDetail.addClassNames(CosmosCssClasses.PANEL_CARD, "support-admin-detail");
    adminDetail.add(ticketHeader, adminTicketTools, ticketDetailContentLayout, adminActionFooter);
  }

  private VerticalLayout buildAdminDetailColumn() {
    noSelectionTitle.addClassName("cosmos-notification-empty-title");
    selectTicketHintSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    ticketSelectionPlaceholder.add(
        AppIcon.MOUSE_POINTER_CLICK.create(), noSelectionTitle, selectTicketHintSpan);
    ticketSelectionPlaceholder.setAlignItems(Alignment.CENTER);
    ticketSelectionPlaceholder.setJustifyContentMode(JustifyContentMode.CENTER);
    ticketSelectionPlaceholder.setSpacing(false);
    ticketSelectionPlaceholder.setPadding(false);
    ticketSelectionPlaceholder.setWidthFull();
    ticketSelectionPlaceholder.setId("support-select-ticket");
    ticketSelectionPlaceholder.addClassNames(
        "support-admin-no-selection", "cosmos-notification-empty");
    selectTicketHintSpan.getElement().setAttribute("role", "status");
    VerticalLayout ticketDetailColumnLayout =
        new VerticalLayout(ticketSelectionPlaceholder, adminDetail);
    ticketDetailColumnLayout.setPadding(false);
    ticketDetailColumnLayout.setSpacing(false);
    ticketDetailColumnLayout.setFlexGrow(1, ticketSelectionPlaceholder);
    ticketDetailColumnLayout.addClassName("support-admin-detail-column");
    return ticketDetailColumnLayout;
  }

  private void setTicketFocused(boolean focused) {
    ticketFocused = focused && selectedTicket != null;
    adminQueueLayout.setVisible(!ticketFocused);
    adminWorkspaceDiv.setClassName("support-focused", ticketFocused);
    updateFocusControl();
  }

  private void updateFocusControl() {
    toggleTicketFocusButton.setPrefixComponent(
        (ticketFocused ? AppIcon.MINIMIZE : AppIcon.EXPAND).create());
    toggleTicketFocusButton
        .getElement()
        .setAttribute("aria-pressed", Boolean.toString(ticketFocused));
    String ticketFocusLabelText =
        ticketFocused
            ? messageProperties.getSupportRestoreSplitView()
            : messageProperties.getSupportFocusTicket();
    toggleTicketFocusButton.setAriaLabel(ticketFocusLabelText);
    toggleTicketFocusButton.setTooltipText(ticketFocusLabelText);
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
    userExplanationSpan.setText(messageProperties.getSupportNoUnlock());
    ticketMessageInput.setPlaceholder(messageProperties.getSupportRequestPlaceholder());
    if (administration) {
      adminBreadcrumbSpan.setText(
          messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getSupportTitle());
      adminPageTitleSpan.setText(messageProperties.getSupportTitle());
      adminPageDescriptionSpan.setText(messageProperties.getSupportPageDescription());
      queueHeading.setText(messageProperties.getSupportCases());
      queueEmptyTitle.setText(messageProperties.getSupportQueueEmptyTitle());
      filterButton.setText(messageProperties.getAdminUsersAddFilter());
      ticketFilterPanel.refreshTexts();
      ticketPagination.refreshTexts();
      allTicketsLabelSpan.setText(messageProperties.getSupportAllTickets());
      recoveryTicketsLabelSpan.setText(messageProperties.getSupportRecoveryCategory());
      ticketCategories.getElement().setAttribute(ARIA_LABEL, messageProperties.getSupportTitle());
      noSelectionTitle.setText(messageProperties.getSupportNoTicketSelected());
      selectTicketHintSpan.setText(messageProperties.getSupportSelectTicket());
      ticketGrid.getElement().setAttribute(ARIA_LABEL, messageProperties.getSupportCases());
    } else {
      userRequestsHeading.setText(messageProperties.getSupportExistingRequests());
      ticketGrid
          .getElement()
          .setAttribute(ARIA_LABEL, messageProperties.getSupportExistingRequests());
    }
    userWarningLabelSpan.setText(messageProperties.getSupportSecurityNoticeLabel() + " ");
    securityWarningSpan.setText(messageProperties.getSupportSecurityNotice());
    ticketMessageInput.setLabel(
        administration
            ? messageProperties.getSupportReply()
            : messageProperties.getSupportDescription());
    publicReplyLabelSpan.setText(messageProperties.getSupportPublicReply());
    internalNoteLabelSpan.setText(messageProperties.getSupportInternal());
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
      adminPolicyLabelSpan.setText(messageProperties.getSupportSecurityPolicy());
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
    adminEditorLayout.getElement().setAttribute("data-internal", Boolean.toString(privateNote));
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

  public void openTicket(String requestedTicketNumber) {
    openTicket(requestedTicketNumber, false);
  }

  private boolean jumpToTicket(String requestedTicketNumber) {
    return openTicket(
        requestedTicketNumber.startsWith("#")
            ? requestedTicketNumber.substring(1)
            : requestedTicketNumber,
        true);
  }

  private boolean openTicket(String requestedTicketNumber, boolean directJump) {
    List<Ticket> ticketsToDisplay =
        administration ? List.of() : supportTicketService.listTicketsForViewer(httpRequest, false);
    Ticket matchingTicket =
        administration
            ? supportTicketService.findForAdministration(httpRequest, requestedTicketNumber)
            : ticketsToDisplay.stream()
                .filter(ticket -> ticketNumber(ticket).equals("#" + requestedTicketNumber))
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
  public void restoreNavigation(Category category, String requestedTicketNumber) {
    restoringNavigation = true;
    try {
      ticketCategories.setSelectedTab(category == Category.RECOVERY ? recoveryTickets : allTickets);
      selectedTicket = null;
      if (requestedTicketNumber == null) {
        refreshTicketWorkspace();
      } else {
        openTicket(requestedTicketNumber);
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
    liveTicketRegistration =
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
    if (liveTicketRegistration != null) {
      liveTicketRegistration.remove();
      liveTicketRegistration = null;
    }
    liveSession = null;
    liveViewerId = null;
    super.onDetach(event);
  }

  private void refreshLive(HttpSession viewerHttpSession) {
    try {
      LiveView liveTicketView =
          supportTicketService.liveView(
              viewerHttpSession,
              administration,
              selectedTicketId(),
              administration ? inboxQuery() : InboxQuery.all());
      liveViewerId = liveTicketView.viewerId();
      if (administration) {
        applyInbox(liveTicketView);
        return;
      }
      applyTickets(liveTicketView.visibleTickets(), liveTicketView.selected());
      showConversation(liveTicketView.messages(), liveTicketView.requester());
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
      int activeTicketFilterCount = ticketFilterPanel.activeCount();
      activeFilterCount.setText(Integer.toString(activeTicketFilterCount));
      activeFilterCount.setVisible(activeTicketFilterCount > 0);
      filterButton.setAriaLabel(
          activeTicketFilterCount == 0
              ? messageProperties.getAdminUsersAddFilter()
              : messageProperties.getAdminUsersAddFilter()
                  + ", "
                  + messageProperties.getAdminUsersActiveFilterCount(activeTicketFilterCount));
      queueEmptyHintSpan.setText(
          activeTicketFilterCount > 0
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

    Ticket detailTicketSelection = ticketToSelect;
    Ticket gridTicketSelection =
        ticketsToDisplay.stream()
            .filter(ticket ->
                detailTicketSelection != null && ticket.id().equals(detailTicketSelection.id()))
            .findFirst()
            .orElse(null);

    ticketGrid.asSingleSelect().setValue(gridTicketSelection);
    selectedTicket = ticketToSelect;
    publishNavigation();
  }

  public void addBackAction(Button backNavigationButton) {
    if (administration) {
      throw new IllegalStateException("Recovery navigation belongs to the user's request form");
    }
    backActions.add(backNavigationButton);
  }

  public void addUserFooterAction(Button userFooterActionButton) {
    if (administration) {
      throw new IllegalStateException("Recovery actions belong to the user's request form");
    }
    userFooterActions.addComponentAtIndex(0, userFooterActionButton);
  }

  private Component ticketRow(Ticket ticket) {
    Span ticketTitleSpan = new Span(ticketSubject(ticket));
    ticketTitleSpan.addClassName("support-ticket-title");
    Span ticketTimestampSpan = new Span(formatTime(ticket.createdAt()));
    ticketTimestampSpan.addClassName("support-ticket-updated");
    Badge ticketStatusBadge = new Badge(status(ticket.status()));
    setStatusBadge(ticketStatusBadge, ticket.status());
    if (administration) {
      return adminTicketRow(ticket, ticketTitleSpan, ticketTimestampSpan, ticketStatusBadge);
    }
    VerticalLayout ticketSummaryLayout = new VerticalLayout(ticketTitleSpan, ticketTimestampSpan);
    ticketSummaryLayout.setPadding(false);
    ticketSummaryLayout.setSpacing(false);
    HorizontalLayout ticketDetailsLayout =
        new HorizontalLayout(ticketNumberLabel(ticket), ticketSummaryLayout);
    ticketDetailsLayout.setSpacing(false);
    ticketDetailsLayout.setAlignItems(Alignment.CENTER);
    ticketDetailsLayout.addClassName("support-ticket-details");
    HorizontalLayout ticketRowLayout =
        new HorizontalLayout(ticketDetailsLayout, ticketIndicators(ticket, ticketStatusBadge));
    ticketRowLayout.setSpacing(false);
    ticketRowLayout.setWidthFull();
    ticketRowLayout.setAlignItems(Alignment.CENTER);
    ticketRowLayout.addClassName("support-ticket-row");
    return ticketRowLayout;
  }

  private VerticalLayout adminTicketRow(
      Ticket ticket, Span ticketTitleSpan, Span ticketTimestampSpan, Badge ticketStatusBadge) {
    HorizontalLayout metadata =
        new HorizontalLayout(
            ticketNumberLabel(ticket), ticketIndicators(ticket, ticketStatusBadge));
    metadata.setSpacing(false);
    metadata.setWidthFull();
    metadata.setAlignItems(Alignment.CENTER);
    metadata.setJustifyContentMode(JustifyContentMode.BETWEEN);
    metadata.addClassName("support-admin-ticket-metadata");

    HorizontalLayout ticketRowFooterLayout =
        new HorizontalLayout(assigneeAvatar(ticket, IconSize.PIXEL_24), ticketTimestampSpan);
    ticketRowFooterLayout.setSpacing(false);
    ticketRowFooterLayout.setWidthFull();
    ticketRowFooterLayout.setAlignItems(Alignment.CENTER);
    ticketRowFooterLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    ticketRowFooterLayout.addClassName("support-admin-ticket-footer");

    VerticalLayout adminTicketRowLayout =
        new VerticalLayout(metadata, ticketTitleSpan, ticketRowFooterLayout);
    adminTicketRowLayout.setPadding(false);
    adminTicketRowLayout.setSpacing(false);
    adminTicketRowLayout.setWidthFull();
    adminTicketRowLayout.addClassNames("support-ticket-row", "support-admin-ticket-card");
    return adminTicketRowLayout;
  }

  private Component assigneeAvatar(Ticket ticket, IconSize iconSize) {
    if (ticket.assigneeIdentity() == null) {
      Span unassignedLabelSpan = new Span(messageProperties.getSupportUnassigned());
      unassignedLabelSpan.addClassNames("support-ticket-assignment", "support-ticket-unassigned");
      return unassignedLabelSpan;
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
      userEditorLayout.setVisible(!hasOpenRequest || canReply);
    } else {
      adminEditorLayout.setVisible(canReply);
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

  private void refreshAdminConversation(List<Reply> ticketReplies) {
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

    adminAssignmentLayout.removeAll();
    Span assignmentLabelSpan = new Span(messageProperties.getSupportAssigneeColumn());
    assignmentLabelSpan.addClassName("support-admin-person-label");

    Component assigneeComponent;
    if (selectedTicket.assigneeIdentity() != null
        && !Objects.equals(selectedTicket.assignee(), administratorId)) {
      HorizontalLayout assigneeIdentityLayout =
          adminPersonIdentity(selectedTicket.assigneeIdentity());
      assigneeIdentityLayout.setWidth(null);
      assigneeComponent = assigneeIdentityLayout;
    } else {
      assigneeComponent = assigneeAvatar(selectedTicket, IconSize.PIXEL_32);
    }

    assigneeComponent.addClassName("support-admin-assignee-identity");
    adminAssignmentLayout.add(assignmentLabelSpan, assigneeComponent);
    updateAdminRequester(selectedTicket.requesterIdentity());
    adminRecoveryHintSpan.setText(messageProperties.getSupportNoUnlock());
    adminRecoveryInfoLayout.setVisible(selectedTicket.type() == Type.RECOVERY);

    String ticketStatusHintText =
        switch (selectedTicket.status()) {
          case OPEN ->
              selectedTicket.assignee() == null ? messageProperties.getSupportUnassignedHint() : "";
          case IN_PROGRESS -> "";
          case CLOSED -> messageProperties.getSupportClosedHint();
          case CLOSED_BY_REQUESTER -> messageProperties.getSupportRequesterClosedHint();
        };
    adminStatusNoteSpan.setText(ticketStatusHintText);
    adminStatusNoteSpan.setVisible(!ticketStatusHintText.isEmpty());

    String conversationHeadingText = messageProperties.getSupportConversation();
    userConversationHeading.setText(conversationHeadingText);
    userConversationCount.setText(
        messageProperties.getSupportMessageCount(
            (int) ticketReplies.stream().filter(message -> !message.kind().isHistory()).count()));
    userMessageScroll
        .getElement()
        .setAttribute(ARIA_LABEL, conversationHeadingText + " " + ticketNumber(selectedTicket));

    renderMessages(ticketReplies, selectedTicket.requesterIdentity());
  }

  private void updateAdminRequester(UserIdentity requester) {
    adminRequesterLayout.removeAll();
    adminRequesterLayout.setVisible(requester != null);
    if (requester == null) {
      return;
    }
    Span requesterLabelSpan = new Span(messageProperties.getSupportRequester());
    requesterLabelSpan.addClassName("support-admin-person-label");
    adminRequesterLayout.add(requesterLabelSpan, adminPersonIdentity(requester));
  }

  private HorizontalLayout adminPersonIdentity(UserIdentity person) {
    Span personNameSpan = new Span(identityName(person));
    personNameSpan.addClassName("support-admin-person-name");
    Span personEmailSpan = new Span(person.email());
    personEmailSpan.addClassName("support-admin-person-email");
    VerticalLayout personDetailsLayout = new VerticalLayout(personNameSpan, personEmailSpan);
    personDetailsLayout.setPadding(false);
    personDetailsLayout.setSpacing(false);
    personDetailsLayout.setWidth(null);
    HorizontalLayout identity =
        new HorizontalLayout(identityAvatar(person, IconSize.PIXEL_32), personDetailsLayout);
    identity.setPadding(false);
    identity.setSpacing(false);
    identity.setWidthFull();
    identity.setFlexGrow(1, personDetailsLayout);
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

  private void renderMessages(List<Reply> ticketReplies, UserIdentity requester) {
    userMessageThread.removeAll();
    for (Reply ticketReply : ticketReplies) {
      if (ticketReply.kind().isHistory()) {
        userMessageThread.add(historyEntry(ticketReply));
        continue;
      }

      HorizontalLayout replyMetadataLayout = messageMetadata(ticketReply, requester);

      Component replyBodyComponent;
      if (ticketReply.kind() == Kind.RECOVERY_INSTRUCTIONS) {
        replyBodyComponent = instructionMessage(ticketReply);
      } else {
        replyBodyComponent = new Span(ticketReply.body());
      }
      replyBodyComponent.getElement().getClassList().add("support-message-copy");

      VerticalLayout replyEntryLayout = new VerticalLayout(replyMetadataLayout, replyBodyComponent);
      replyEntryLayout.setSpacing(false);
      replyEntryLayout.setPadding(false);
      replyEntryLayout.addClassName("support-message");
      replyEntryLayout.getElement().setAttribute("data-author", ticketReply.author().name());
      boolean ownReply = (ticketReply.author() == ReplyAuthor.SUPPORT) == administration;
      replyEntryLayout.getElement().setAttribute("data-side", ownReply ? "own" : "other");

      if (ticketReply.internal()) {
        replyEntryLayout.addClassName("support-internal-message");
      }
      userMessageThread.add(replyEntryLayout);
    }
  }

  private HorizontalLayout messageMetadata(Reply message, UserIdentity requester) {
    Span replyTimestampSpan = new Span("· " + formatTime(message.createdAt()));
    replyTimestampSpan.addClassName("support-message-time");

    HorizontalLayout replyMetadataLayout = new HorizontalLayout();
    replyMetadataLayout.setAlignItems(Alignment.CENTER);
    replyMetadataLayout.setSpacing(false);
    replyMetadataLayout.addClassName("support-message-metadata");
    addMessageSender(replyMetadataLayout, message, requester);
    replyMetadataLayout.add(replyTimestampSpan);
    if (message.internal()) {
      Badge internalNoteBadge = new Badge(messageProperties.getSupportInternal());
      internalNoteBadge.setIcon(AppIcon.LOCK.create());
      internalNoteBadge.addClassName("support-admin-internal");
      replyMetadataLayout.add(internalNoteBadge);
    }
    return replyMetadataLayout;
  }

  private void addMessageSender(
      HorizontalLayout replyMetadataLayout, Reply message, UserIdentity requester) {
    switch (message.author()) {
      case REQUESTER -> {
        if (requester == null) {
          replyMetadataLayout.add(
              AppIcon.USER.create(), new Span(messageProperties.getSupportRequester()));
        } else {
          replyMetadataLayout.add(identityAvatar(requester, IconSize.PIXEL_24));
          if (administration) {
            replyMetadataLayout.add(new Span(identityName(requester)));
          }
        }
      }
      case SUPPORT -> {
        UserIdentity administrator = administration ? message.actor() : null;
        Span senderNameSpan =
            new Span(
                administrator == null
                    ? messageProperties.getSupportMessageSupport()
                    : identityName(administrator));
        senderNameSpan.addClassName("support-message-sender");
        Component senderIconComponent =
            administrator == null
                ? AppIcon.SHIELD_COG.create()
                : identityAvatar(administrator, IconSize.PIXEL_24);
        replyMetadataLayout.add(senderIconComponent, senderNameSpan);
      }
    }
  }

  private Component historyEntry(Reply ticketHistoryEvent) {
    HorizontalLayout historySummaryLayout = new HorizontalLayout();
    historySummaryLayout.setPadding(false);
    historySummaryLayout.setSpacing(false);
    historySummaryLayout.setAlignItems(Alignment.CENTER);
    SvgIcon historyEventSvgIcon =
        (ticketHistoryEvent.kind() == Kind.TICKET_CREATED
                ? AppIcon.MESSAGE_SQUARE_PLUS
                : AppIcon.STATUS_CHANGE)
            .create();
    historyEventSvgIcon.addClassName("support-history-event-icon");
    historySummaryLayout.add(historyEventSvgIcon);
    if (ticketHistoryEvent.kind() == Kind.TICKET_CREATED) {
      historySummaryLayout.add(new Span(messageProperties.getSupportTicketCreated()));
    } else {
      historySummaryLayout.add(new Span(messageProperties.getSupportStatusChanged()));
      Badge previousStatusBadge = new Badge();
      setStatusBadge(previousStatusBadge, ticketHistoryEvent.previousStatus());
      Badge nextStatusBadge = new Badge();
      setStatusBadge(nextStatusBadge, ticketHistoryEvent.nextStatus());
      historySummaryLayout.add(
          previousStatusBadge, AppIcon.ARROW_FORWARD.create(), nextStatusBadge);
    }

    Span eventTimeSpan = new Span(formatTime(ticketHistoryEvent.createdAt()));
    eventTimeSpan.addClassName("support-message-time");
    String actorNameText =
        ticketHistoryEvent.actor() == null
            ? messageProperties.getSupportMessageSupport()
            : identityName(ticketHistoryEvent.actor());

    HorizontalLayout historyMetadataLayout =
        new HorizontalLayout(eventTimeSpan, new Span("·"), new Span(actorNameText));
    historyMetadataLayout.setPadding(false);
    historyMetadataLayout.setSpacing(false);
    historyMetadataLayout.addClassName("support-history-metadata");

    VerticalLayout historyEntryLayout =
        new VerticalLayout(historySummaryLayout, historyMetadataLayout);
    historyEntryLayout.setPadding(false);
    historyEntryLayout.setSpacing(false);
    historyEntryLayout.addClassName("support-history-event");
    historyEntryLayout.getElement().setAttribute("data-event", ticketHistoryEvent.kind().name());
    return historyEntryLayout;
  }

  private Component instructionMessage(Reply message) {
    RecoveryInstructionsComponent instructionsComponent =
        new RecoveryInstructionsComponent(message.body());
    Details instructionsDetails = new Details("", instructionsComponent);
    instructionsDetails.setId("support-instruction-" + message.id());
    instructionsDetails.setOpened(expandedInstructions.contains(message.id()));
    instructionsDetails.addClassName("support-instruction-details");
    instructionsDetails.addOpenedChangeListener(event -> {
      if (event.isOpened()) {
        expandedInstructions.add(message.id());
      } else {
        expandedInstructions.remove(message.id());
      }
    });

    Span instructionsTitleSpan = new Span(messageProperties.getSupportInstructionMessageTitle());
    instructionsTitleSpan.addClassName("support-instruction-title");
    HorizontalLayout instructionsHeadingLayout =
        new HorizontalLayout(AppIcon.ENVELOPE.create(), instructionsTitleSpan);
    instructionsHeadingLayout.setSpacing(false);
    instructionsHeadingLayout.setPadding(false);
    instructionsHeadingLayout.setAlignItems(Alignment.CENTER);
    instructionsHeadingLayout.addClassName("support-instruction-heading");

    Button toggleInstructionsButton =
        new DetailsToggleButton(
            messageProperties, instructionsDetails, instructionsTitleSpan.getText());
    toggleInstructionsButton.addClassName("support-instruction-toggle");

    HorizontalLayout instructionsHeaderLayout =
        new HorizontalLayout(instructionsHeadingLayout, toggleInstructionsButton);
    instructionsHeaderLayout.setWidthFull();
    instructionsHeaderLayout.setSpacing(false);
    instructionsHeaderLayout.setPadding(false);
    instructionsHeaderLayout.setAlignItems(Alignment.CENTER);
    instructionsHeaderLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    instructionsHeaderLayout.addClassName("support-instruction-header");

    VerticalLayout instructionsBubbleLayout =
        new VerticalLayout(
            instructionsHeaderLayout,
            new Span(instructionsComponent.getDescription()),
            instructionsDetails);
    instructionsBubbleLayout.setSpacing(false);
    instructionsBubbleLayout.setPadding(false);
    instructionsBubbleLayout.addClassName("support-instruction-message");

    return instructionsBubbleLayout;
  }

  private static Span ticketNumberLabel(Ticket ticket) {
    Span ticketNumberSpan = new Span(ticketNumber(ticket));
    ticketNumberSpan.addClassName("support-ticket-number");
    return ticketNumberSpan;
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

  private void runTicketActionAndRefreshWorkspace(
      Supplier<Ticket> ticketActionSupplier,
      Supplier<String> successMessageSupplier,
      boolean clearDraft) {
    try {
      selectedTicket = ticketActionSupplier.get();

      if (clearDraft) {
        ticketMessageInput.clear();
        ticketMessageInput.setInvalid(false);
        replyMode.setSelectedTab(publicReplyMode);
      }

      succeeded(successMessageSupplier);
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
