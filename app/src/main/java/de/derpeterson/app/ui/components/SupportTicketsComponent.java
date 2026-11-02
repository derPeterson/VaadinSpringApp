package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.service.SupportTicketService;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import jakarta.servlet.http.HttpServletRequest;
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
  private final Div conversation = new Div();
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
    grid.setId("support-ticket-grid");
    grid.setWidthFull();
    grid.setAllRowsVisible(true);
    grid.addColumn(ticket -> "#" + ticket.id()).setHeader("#").setAutoWidth(true).setFlexGrow(0);
    grid.addColumn(Ticket::subject).setAutoWidth(true);
    grid.addComponentColumn(ticket -> new Badge(status(ticket.status()))).setAutoWidth(true);
    grid.addColumn(
            ticket ->
                ticket.assignee() == null
                    ? texts.getSupportUnassigned()
                    : texts.getSupportAssigned() + " #" + ticket.assignee())
        .setAutoWidth(true);
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
        event -> perform(() -> service.createRecovery(request, body.getValue())));
    claim.setId("support-claim");
    claim.setPrefixComponent(AppIcon.CHECK.create());
    claim.addClickListener(
        event -> perform(() -> service.claim(request, selected.id(), selected.version())));
    reply.setId("support-add-reply");
    reply.setPrefixComponent(AppIcon.PAPERPLANE.create());
    reply.addClickListener(
        event ->
            perform(
                () ->
                    service.reply(
                        request,
                        selected.id(),
                        selected.version(),
                        body.getValue(),
                        administration && internal.getValue(),
                        administration)));
    close.setId("support-close");
    close.setPrefixComponent(AppIcon.CHECK_CHECK.create());
    close.addClickListener(
        event -> perform(() -> service.close(request, selected.id(), selected.version())));
    instructions.setId("support-instructions");
    instructions.setPrefixComponent(AppIcon.ENVELOPE.create());
    instructions.addClickListener(event -> previewInstructions());
    reload.setPrefixComponent(AppIcon.REFRESH.create());
    reload.addClickListener(event -> refresh());
    Div actions = new Div(create, claim, reply, instructions, close, reload);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    warning.addClassName("cosmos-panel-description");
    panel.getBody().add(grid, conversation, warning, body, internal);
    panel.addActionFooter(actions);
    add(panel, instructionDialog);
    refreshTexts();
  }

  public void refreshTexts() {
    panel.setHeading(texts.getSupportTitle(), texts.getSupportNoUnlock(), "");
    grid.getColumns().get(1).setHeader(texts.getSupportSubjectColumn());
    grid.getColumns().get(2).setHeader(texts.getSupportStatusColumn());
    grid.getColumns().get(3).setHeader(texts.getSupportAssigneeColumn());
    warning.setText(texts.getSupportSensitiveWarning());
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
      openRequest = values.stream().anyMatch(ticket -> ticket.status() != Status.CLOSED);
      grid.setItems(values);
      selected = values.stream().filter(ticket -> ticket.id().equals(id)).findFirst().orElse(null);
      if (selected == null && !administration && values.size() == 1) {
        selected = values.getFirst();
      }
      if (selected != null) {
        grid.asSingleSelect().setValue(selected);
      }
      refreshConversation();
    } catch (RuntimeException failure) {
      failed();
    }
  }

  private void refreshConversation() {
    conversation.removeAll();
    create.setVisible(!administration && !openRequest);
    claim.setVisible(administration && selected != null && selected.status() != Status.CLOSED);
    reply.setVisible(selected != null && selected.status() != Status.CLOSED);
    close.setVisible(administration && selected != null && selected.status() != Status.CLOSED);
    instructions.setVisible(
        administration && selected != null && selected.status() != Status.CLOSED);
    instructions.setEnabled(selected != null && !selected.instructionSent());
    if (selected != null) {
      try {
        for (SupportTicketService.Reply message :
            service.conversation(request, selected.id(), administration)) {
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
