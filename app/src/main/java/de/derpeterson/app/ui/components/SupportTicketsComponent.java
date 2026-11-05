package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Reply;
import de.derpeterson.app.service.SupportTicketService.RequesterIdentity;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Native gemeinsame Vorgangsansicht; die Dienstgrenzen unterscheiden Eigenzugang und Bearbeitung.
 */
@StyleSheet("custom-theme/cosmos-mfa.css")
public final class SupportTicketsComponent extends VerticalLayout {
  private static final long TICKET_NUMBER_OFFSET = 10_000L;

  private final transient MessageProperties texts;
  private final transient SupportTicketService service;
  private final transient HttpServletRequest request;
  private final boolean administration;
  private final Grid<Ticket> grid = new Grid<>();
  private final Column<Ticket> subjectColumn;
  private final Column<Ticket> statusColumn;
  private final Column<Ticket> assigneeColumn;
  private final VerticalLayout conversation = new VerticalLayout();
  private final TextArea body = new TextArea();
  private final Switch internal = new Switch();
  private final Button create = new Button();
  private final Button claim = new Button();
  private final Button reply = new Button();
  private final Button close = new Button();
  private final Button instructions = new Button();
  private final Button reload = new Button();
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
  private boolean openRequest;
  private Long administratorId;

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
    if (administration) {
      grid.addColumn(SupportTicketsComponent::ticketNumber)
          .setHeader("#")
          .setAutoWidth(true)
          .setFlexGrow(0);
      subjectColumn =
          grid.addColumn(ticket -> texts.getSupportRecoverySubject()).setAutoWidth(true);
      statusColumn = grid.addComponentColumn(ticket -> new Badge(status(ticket.status())));
      statusColumn.setAutoWidth(true);
      assigneeColumn = grid.addColumn(this::assignee).setAutoWidth(true);
    } else {
      grid.addComponentColumn(this::userTicketRow);
      subjectColumn = null;
      statusColumn = null;
      assigneeColumn = null;
    }
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
    internal.setVisible(administration);
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
            perform(
                () ->
                    service.reply(
                        request,
                        selected.id(),
                        selected.version(),
                        body.getValue(),
                        administration && internal.getValue(),
                        administration),
                texts::getSupportReplySaved,
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
    reload.setId("support-reload");
    reload.addClickListener(event -> refresh(true));
    if (administration) {
      warning.addClassName("cosmos-panel-description");
      Div actions = new Div(create, claim, reply, instructions, close, reload);
      actions.addClassName("cosmos-dialog-actions");
      EqualButtonWidths.configure(actions);
      panel.getBody().add(grid, conversation, warning, messageField, internal);
      panel.addActionFooter(actions);
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
      userWarningLabel.addClassName("cosmos-security-warning-label");
      HorizontalLayout sensitiveNotice =
          new HorizontalLayout(AppIcon.ERROR.create(), new Span(userWarningLabel, warning));
      sensitiveNotice.setPadding(false);
      sensitiveNotice.setSpacing(false);
      sensitiveNotice.setAlignItems(Alignment.START);
      sensitiveNotice.addClassName("cosmos-security-warning");
      userEditor.setPadding(false);
      userEditor.setSpacing(false);
      userEditor.setWidthFull();
      userEditor.addClassName("support-user-editor");
      userEditor.add(sensitiveNotice, messageField);
      userRequestsHeading.addClassName("support-conversation-heading");
      userConversationHeading.addClassName("support-conversation-heading");
      userRequestCount.addClassNames("support-section-count", "support-request-count");
      userConversationCount.addClassName("support-section-count");
      for (HorizontalLayout header : List.of(userRequestsHeader, userConversationHeader)) {
        header.setWidthFull();
        header.setSpacing(false);
        header.setAlignItems(Alignment.CENTER);
        header.addClassName("support-section-header");
      }
      reload.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
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

  public void refreshTexts() {
    panel.setHeading(
        administration ? texts.getSupportTitle() : texts.getSupportHelpTitle(),
        administration ? texts.getSupportNoUnlock() : texts.getSupportHelpSubtitle(),
        "");
    emptyText.setText(texts.getSupportEmpty());
    userExplanation.setText(texts.getSupportNoUnlock());
    body.setPlaceholder(texts.getSupportRequestPlaceholder());
    if (administration) {
      subjectColumn.setHeader(texts.getSupportSubjectColumn());
      statusColumn.setHeader(texts.getSupportStatusColumn());
      assigneeColumn.setHeader(texts.getSupportAssigneeColumn());
    } else {
      userRequestsHeading.setText(texts.getSupportExistingRequests());
      grid.getElement().setAttribute("aria-label", texts.getSupportExistingRequests());
    }
    if (administration) {
      warning.setText(texts.getSupportSensitiveWarning());
    } else {
      userWarningLabel.setText(texts.getSupportSecurityNoticeLabel() + " ");
      warning.setText(texts.getSupportSecurityNotice());
    }
    body.setLabel(administration ? texts.getSupportReply() : texts.getSupportDescription());
    internal.setLabel(texts.getSupportInternal());
    create.setText(texts.getSupportCreate());
    claim.setText(texts.getSupportClaim());
    reply.setText(texts.getSupportReply());
    close.setText(administration ? texts.getSupportClose() : texts.getSupportCloseOwn());
    instructions.setText(texts.getSupportInstructions());
    reload.setText(texts.getSessionRefresh());
    instructionDialog.refreshTexts();
    refresh();
  }

  public void refresh() {
    refresh(false);
  }

  private void refresh(boolean notifySuccess) {
    try {
      Long id = selected == null ? null : selected.id();
      List<Ticket> values = service.list(request, administration);
      if (administration) {
        administratorId = service.administratorId(request);
      } else {
        userRequestCount.setText(texts.getSupportMessageCount(values.size()));
      }
      grid.setVisible(administration || !values.isEmpty());
      emptyState.setVisible(!administration && values.isEmpty());
      openRequest = values.stream().anyMatch(ticket -> ticket.status().acceptsMessages());
      grid.setItems(values);
      selected = values.stream().filter(ticket -> ticket.id().equals(id)).findFirst().orElse(null);
      if (selected == null && !administration && !values.isEmpty()) {
        selected =
            values.stream()
                .filter(ticket -> ticket.status().acceptsMessages())
                .findFirst()
                .orElse(values.getFirst());
      }
      if (selected != null) {
        grid.asSingleSelect().setValue(selected);
      }
      refreshConversation();
      if (notifySuccess) {
        succeeded(texts::getSupportRefreshed);
      }
    } catch (RuntimeException failure) {
      failed(texts::getSupportRefreshFailed);
    }
  }

  public void addBackAction(Button action) {
    if (administration) {
      throw new IllegalStateException("Recovery navigation belongs to the user's request form");
    }
    backActions.add(action);
  }

  private HorizontalLayout userTicketRow(Ticket ticket) {
    Span title = new Span(texts.getSupportRecoverySubject());
    title.addClassName("support-ticket-title");
    Span updated = new Span(formatTime(ticket.updatedAt()));
    updated.addClassName("support-ticket-updated");
    VerticalLayout summary = new VerticalLayout(title, updated);
    summary.setPadding(false);
    summary.setSpacing(false);
    HorizontalLayout details = new HorizontalLayout(ticketNumberLabel(ticket), summary);
    details.setSpacing(false);
    details.setAlignItems(Alignment.CENTER);
    details.addClassName("support-ticket-details");
    Badge badge = new Badge(status(ticket.status()));
    badge.addClassName("support-ticket-status");
    badge.getElement().setAttribute("data-status", ticket.status().name());
    if (ticket.status() == Status.CLOSED_BY_REQUESTER) {
      badge.addThemeVariants(BadgeVariant.SUCCESS);
    }
    HorizontalLayout row = new HorizontalLayout(details, badge);
    row.setSpacing(false);
    row.setWidthFull();
    row.setAlignItems(Alignment.CENTER);
    row.addClassName("support-ticket-row");
    return row;
  }

  private String assignee(Ticket ticket) {
    return ticket.assignee() == null
        ? texts.getSupportUnassigned()
        : texts.getSupportAssigned() + " #" + ticket.assignee();
  }

  private void refreshConversation() {
    if (administration) {
      conversation.removeAll();
    }
    boolean active = selected != null && selected.status().acceptsMessages();
    boolean unassigned = selected != null && selected.assignee() == null;
    boolean assignedToMe =
        selected != null && administratorId != null && administratorId.equals(selected.assignee());
    boolean canReply = active && (!administration || assignedToMe);
    boolean requesterClosed = selected != null && selected.status() == Status.CLOSED_BY_REQUESTER;
    if (!administration) {
      body.setLabel(openRequest ? texts.getSupportReply() : texts.getSupportDescription());
      userEditor.setVisible(!openRequest || canReply);
    } else {
      messageField.setVisible(canReply);
      internal.setVisible(canReply);
    }
    create.setVisible(!administration && !openRequest);
    claim.setVisible(administration && active && unassigned);
    reply.setVisible(canReply);
    close.setVisible(
        administration ? canReply || requesterClosed && (unassigned || assignedToMe) : active);
    instructions.setVisible(administration && canReply);
    instructions.setEnabled(selected != null && !selected.instructionSent());
    List<Reply> messages =
        selected == null ? List.of() : service.conversation(request, selected.id(), administration);
    if (!administration) {
      refreshUserConversation(messages);
    } else {
      for (Reply message : messages) {
        Div card = new Div();
        card.addClassName("account-entry");
        if (message.internal()) {
          card.add(new Badge(texts.getSupportInternal()));
        }
        Span copy = new Span(message.body());
        copy.getStyle().set("white-space", "pre-wrap");
        card.add(copy);
        conversation.add(card);
      }
    }
  }

  private void refreshUserConversation(List<Reply> messages) {
    boolean showConversation = !messages.isEmpty();
    HorizontalLayout refreshHeader = showConversation ? userConversationHeader : userRequestsHeader;
    if (reload.getParent().filter(refreshHeader::equals).isEmpty()) {
      refreshHeader.add(reload);
    }
    userMessageThread.removeAll();
    conversation.setVisible(showConversation);
    if (!showConversation) {
      return;
    }
    String heading = texts.getSupportConversation() + " ";
    userConversationHeading.removeAll();
    userConversationHeading.add(new Text(heading), ticketNumberLabel(selected));
    userConversationCount.setText(texts.getSupportMessageCount(messages.size()));
    userMessageScroll.getElement().setAttribute("aria-label", heading + ticketNumber(selected));
    RequesterIdentity requester = service.requesterIdentity(request);
    for (Reply message : messages) {
      Span timestamp = new Span("· " + formatTime(message.createdAt()));
      timestamp.addClassName("support-message-time");
      HorizontalLayout metadata = new HorizontalLayout();
      metadata.setAlignItems(Alignment.CENTER);
      metadata.setSpacing(false);
      metadata.addClassName("support-message-metadata");
      switch (message.author()) {
        case REQUESTER -> {
          OverlayUserIcon avatar = new OverlayUserIcon(texts, null, null, IconSize.PIXEL_24);
          avatar.setName(requester.firstName(), requester.lastName());
          avatar.showIdentityTooltip(requester.email());
          metadata.add(avatar);
        }
        case SUPPORT -> {
          Span sender = new Span(texts.getSupportMessageSupport());
          sender.addClassName("support-message-sender");
          metadata.add(AppIcon.SHIELD_COG.create(), sender);
        }
      }
      metadata.add(timestamp);
      Span copy = new Span(message.body());
      copy.addClassName("support-message-copy");
      VerticalLayout entry = new VerticalLayout(metadata, copy);
      entry.setSpacing(false);
      entry.setPadding(false);
      entry.addClassName("support-message");
      entry.getElement().setAttribute("data-author", message.author().name());
      userMessageThread.add(entry);
    }
  }

  private static Span ticketNumberLabel(Ticket ticket) {
    Span number = new Span(ticketNumber(ticket));
    number.addClassName("support-ticket-number");
    return number;
  }

  private static String ticketNumber(Ticket ticket) {
    return "#" + (TICKET_NUMBER_OFFSET + ticket.id());
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
      Ticket target = selected;
      instructionDialog.open(
          new PortalConfirmationDialog.Prompt(
              texts::getSupportInstructions,
              () -> "",
              () -> "",
              () -> preview,
              Div::new,
              texts::getSupportNoUnlock,
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
