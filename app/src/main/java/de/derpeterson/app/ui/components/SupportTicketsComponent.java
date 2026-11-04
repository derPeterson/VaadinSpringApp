package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.badge.Badge;
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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Supplier;

/**
 * Native gemeinsame Vorgangsansicht; die Dienstgrenzen unterscheiden Eigenzugang und Bearbeitung.
 */
@StyleSheet("custom-theme/cosmos-mfa.css")
public final class SupportTicketsComponent extends VerticalLayout {
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
  private final HorizontalLayout emptyState = new HorizontalLayout();
  private final HorizontalLayout backActions = new HorizontalLayout();
  private final VerticalLayout userEditor = new VerticalLayout();
  private Ticket selected;
  private boolean openRequest;

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
      grid.addColumn(ticket -> "#" + ticket.id()).setHeader("#").setAutoWidth(true).setFlexGrow(0);
      subjectColumn = grid.addColumn(Ticket::subject).setAutoWidth(true);
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
              refreshConversation();
            });
    body.setId("support-reply");
    body.setWidthFull();
    body.setMinHeight("120px");
    body.setMaxLength(2000);
    body.setManualValidation(true);
    internal.setVisible(administration);
    create.setId("support-create");
    create.setPrefixComponent(AppIcon.PLUS.create());
    create.addClickListener(
        event -> {
          if (ValidationHelper.validateRequiredInputs(List.of(body), texts)) {
            perform(() -> service.createRecovery(request, body.getValue()));
          }
        });
    claim.setId("support-claim");
    claim.setPrefixComponent(AppIcon.CHECK.create());
    claim.addClickListener(
        event -> perform(() -> service.claim(request, selected.id(), selected.version())));
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
                        administration));
          }
        });
    close.setId("support-close");
    close.setPrefixComponent(AppIcon.CHECK_CHECK.create());
    close.addClickListener(
        event -> perform(() -> service.close(request, selected.id(), selected.version())));
    instructions.setId("support-instructions");
    instructions.setPrefixComponent(AppIcon.ENVELOPE.create());
    instructions.addClickListener(event -> previewInstructions());
    reload.setPrefixComponent(AppIcon.REFRESH.create());
    reload.setId("support-reload");
    reload.addClickListener(event -> refresh());
    if (administration) {
      warning.addClassName("cosmos-panel-description");
      Div actions = new Div(create, claim, reply, instructions, close, reload);
      actions.addClassName("cosmos-dialog-actions");
      EqualButtonWidths.configure(actions);
      panel.getBody().add(grid, conversation, warning, body, internal);
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
      userWarningLabel.addClassName("support-sensitive-notice-label");
      HorizontalLayout sensitiveNotice =
          new HorizontalLayout(AppIcon.ERROR.create(), new Span(userWarningLabel, warning));
      sensitiveNotice.setPadding(false);
      sensitiveNotice.setSpacing(false);
      sensitiveNotice.setAlignItems(Alignment.CENTER);
      sensitiveNotice.addClassName("support-sensitive-notice");
      userEditor.setPadding(false);
      userEditor.setSpacing(false);
      userEditor.setWidthFull();
      userEditor.addClassName("support-user-editor");
      userEditor.add(sensitiveNotice, body);
      userRequestsHeading.addClassName("support-conversation-heading");
      VerticalLayout requests = new VerticalLayout(userRequestsHeading, emptyState, grid);
      requests.setPadding(false);
      requests.setSpacing(false);
      requests.addClassName("support-user-requests");
      VerticalLayout form = new VerticalLayout(helpNotice, requests, conversation, userEditor);
      form.setPadding(false);
      form.setSpacing(false);
      form.setWidthFull();
      form.addClassName("support-user-form");
      panel.getBody().add(form);
      body.setRequiredIndicatorVisible(true);
      create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      reply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      HorizontalLayout actions = new HorizontalLayout(reload, reply, create);
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
    body.setPlaceholder(administration ? "" : texts.getSupportRequestPlaceholder());
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
    body.setLabel(texts.getSupportDescription());
    internal.setLabel(texts.getSupportInternal());
    create.setText(texts.getSupportCreate());
    claim.setText(texts.getSupportClaim());
    reply.setText(texts.getSupportReply());
    close.setText(texts.getSupportClose());
    instructions.setText(texts.getSupportInstructions());
    reload.setText(texts.getSessionRefresh());
    instructionDialog.refreshTexts();
    refresh();
  }

  public void refresh() {
    try {
      Long id = selected == null ? null : selected.id();
      List<Ticket> values = service.list(request, administration);
      grid.setVisible(administration || !values.isEmpty());
      emptyState.setVisible(!administration && values.isEmpty());
      openRequest = values.stream().anyMatch(ticket -> ticket.status() != Status.CLOSED);
      grid.setItems(values);
      selected = values.stream().filter(ticket -> ticket.id().equals(id)).findFirst().orElse(null);
      if (selected == null && !administration && !values.isEmpty()) {
        selected =
            values.stream()
                .filter(ticket -> ticket.status() != Status.CLOSED)
                .findFirst()
                .orElse(values.getFirst());
      }
      if (selected != null) {
        grid.asSingleSelect().setValue(selected);
      }
      refreshConversation();
    } catch (RuntimeException failure) {
      failed();
    }
  }

  public void addBackAction(Button action) {
    if (administration) {
      throw new IllegalStateException("Recovery navigation belongs to the user's request form");
    }
    backActions.add(action);
  }

  private HorizontalLayout userTicketRow(Ticket ticket) {
    Span number = new Span("#" + ticket.id());
    number.addClassName("support-ticket-number");
    HorizontalLayout title = new HorizontalLayout(number, new Span(ticket.subject()));
    title.setSpacing(false);
    title.addClassName("support-ticket-title");
    Span updated = new Span(formatTime(ticket.updatedAt()));
    updated.addClassName("support-ticket-updated");
    VerticalLayout summary = new VerticalLayout(title, updated);
    summary.setPadding(false);
    summary.setSpacing(false);
    Badge badge = new Badge(status(ticket.status()));
    badge.addClassName("support-ticket-status");
    badge
        .getElement()
        .setAttribute("data-closed", Boolean.toString(ticket.status() == Status.CLOSED));
    HorizontalLayout row = new HorizontalLayout(summary, badge);
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
    conversation.removeAll();
    conversation.setVisible(administration);
    boolean canReply = selected != null && selected.status() != Status.CLOSED;
    if (!administration) {
      body.setLabel(openRequest ? texts.getSupportReply() : texts.getSupportDescription());
      userEditor.setVisible(!openRequest || canReply);
    }
    create.setVisible(!administration && !openRequest);
    claim.setVisible(administration && selected != null && selected.status() != Status.CLOSED);
    reply.setVisible(canReply);
    close.setVisible(administration && selected != null && selected.status() != Status.CLOSED);
    instructions.setVisible(
        administration && selected != null && selected.status() != Status.CLOSED);
    instructions.setEnabled(selected != null && !selected.instructionSent());
    if (selected != null) {
      try {
        List<Reply> messages = service.conversation(request, selected.id(), administration);
        if (!administration) {
          if (!messages.isEmpty()) {
            showUserConversation(messages);
          }
          return;
        }
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
      } catch (RuntimeException failure) {
        failed();
      }
    }
  }

  private void showUserConversation(List<Reply> messages) {
    H3 heading = new H3(texts.getSupportConversation() + " #" + selected.id());
    heading.addClassName("support-conversation-heading");
    Span count = new Span(texts.getSupportMessageCount(messages.size()));
    count.addClassName("support-conversation-count");
    HorizontalLayout header = new HorizontalLayout(heading, count);
    header.setAlignItems(Alignment.CENTER);
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    VerticalLayout thread = new VerticalLayout();
    thread.setSpacing(false);
    thread.addClassName("support-message-thread");
    RequesterIdentity requester = service.requesterIdentity(request);
    for (Reply message : messages) {
      Span timestamp = new Span(formatTime(message.createdAt()));
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
        case SUPPORT ->
            metadata.add(AppIcon.SHIELD.create(), new Span(texts.getSupportMessageSupport()));
        case UNKNOWN -> {
          // Der Zeitstempel bleibt sichtbar, wenn das frühere Autorenkonto gelöscht ist.
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
      thread.add(entry);
    }
    Scroller scroll = new Scroller(thread, Scroller.ScrollDirection.VERTICAL);
    scroll.addClassName("support-message-scroll");
    scroll.getElement().setAttribute("aria-label", heading.getText());
    conversation.add(header, scroll);
    conversation.setVisible(true);
  }

  private String formatTime(Instant time) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(getLocale())
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
              failed();
            }
          });
    } catch (RuntimeException failure) {
      failed();
    }
  }

  private void perform(Supplier<Ticket> action) {
    try {
      selected = action.get();
      body.clear();
      body.setInvalid(false);
      refresh();
      NotificationHelper.getInstance()
          .showNotification(texts::getSupportSaved, NotificationHelper.NotificationType.SUCCESS);
    } catch (RuntimeException failure) {
      body.setInvalid(true);
      failed();
    }
  }

  private void failed() {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle,
            texts::getSupportFailed,
            NotificationHelper.NotificationType.ERROR);
  }

  private String status(Status value) {
    return switch (value) {
      case OPEN -> texts.getSupportOpen();
      case IN_PROGRESS -> texts.getSupportInProgress();
      case CLOSED -> texts.getSupportClosed();
    };
  }
}
