package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.service.UserMessageService;
import de.derpeterson.app.service.UserMessageService.*;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.LoggerFactory;

/** Superadmin-Entwurf mit serverseitiger Empfängervorschau und dauerhaftem Request-Schlüssel. */
public class AdminUserMessagesSection extends Div {
  private final transient MessageProperties texts;
  private final transient UserMessageService service;
  private final transient HttpServletRequest request;
  private final H2 heading = new H2();
  private final Span description = new Span();
  private final Checkbox everyone = new Checkbox();
  private final MultiSelectComboBox<Target> targets = new MultiSelectComboBox<>();
  private final TextField title = new TextField();
  private final TextArea body = new TextArea();
  private final Button previewButton = new Button();
  private final Button send = new Button();
  private final Div previewArea = new Div();
  private final Span hint = new Span();
  private final H2 historyTitle = new H2();
  private final Div history = new Div();
  private final Button previous = new Button();
  private final Button next = new Button();
  private final Span pageLabel = new Span();
  private Preview preview;
  private UUID requestId = UUID.randomUUID();
  private int page;

  public AdminUserMessagesSection(
      MessageProperties texts, UserMessageService service, HttpServletRequest request) {
    this.texts = texts;
    this.service = service;
    this.request = request;
    addClassNames("cosmos-admin-section", "cosmos-theme");
    setWidthFull();
    heading.addClassName("cosmos-panel-title");
    historyTitle.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    hint.addClassName("cosmos-messages-secondary");
    targets.setItemLabelGenerator(t -> t.name() + " · " + t.email());
    targets.setWidthFull();
    targets.setId("admin-messages-recipients");
    everyone.setId("admin-messages-everyone");
    title.setId("admin-messages-title");
    body.setId("admin-messages-body");
    title.setMaxLength(UserMessageService.TITLE_LIMIT);
    body.setMaxLength(UserMessageService.BODY_LIMIT);
    for (var field : List.of(title, body)) {
      field.setWidthFull();
      field.setRequiredIndicatorVisible(true);
      field.setValueChangeMode(ValueChangeMode.EAGER);
      field.addValueChangeListener(e -> invalidatePreview());
    }
    everyone.addValueChangeListener(
        e -> {
          targets.setEnabled(!e.getValue());
          invalidatePreview();
        });
    targets.addValueChangeListener(e -> invalidatePreview());
    previewButton.setId("admin-messages-preview");
    send.setId("admin-messages-send");
    previewButton.addClickListener(e -> preview());
    send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    send.setEnabled(false);
    send.addClickListener(e -> send());
    var actions = new Div(previewButton, send);
    actions.addClassName("cosmos-dialog-actions");
    var form =
        new Div(heading, description, everyone, targets, title, body, hint, previewArea, actions);
    form.addClassName("cosmos-messages-panel");
    previous.addClickListener(
        e -> {
          if (page > 0) page--;
          loadHistory();
        });
    next.addClickListener(
        e -> {
          page++;
          loadHistory();
        });
    var paging = new Div(previous, pageLabel, next);
    paging.addClassName("cosmos-messages-actions");
    var sent = new Div(historyTitle, history, paging);
    sent.addClassName("cosmos-messages-panel");
    add(new CosmosCardComponent(form), new CosmosCardComponent(sent));
    refreshTexts();
  }

  public void activate() {
    // Auch erneute Navigation und jede Serviceaktion prüfen die aktuellen Berechtigungen.
    targets.setItems(service.targets(request.getSession(false)));
    invalidatePreview();
    page = 0;
    loadHistory();
  }

  private void invalidatePreview() {
    preview = null;
    previewArea.removeAll();
    send.setEnabled(false);
    requestId = UUID.randomUUID();
  }

  private boolean valid() {
    title.setInvalid(title.getValue().isBlank());
    body.setInvalid(body.getValue().isBlank());
    if (title.isInvalid() || body.isInvalid()) {
      feedback(texts::getMessagesInvalidDraft);
      return false;
    }
    return true;
  }

  private void preview() {
    if (!valid()) return;
    try {
      preview =
          service.preview(
              request.getSession(false),
              new Selection(
                  everyone.getValue(),
                  targets.getSelectedItems().stream()
                      .map(Target::id)
                      .collect(java.util.stream.Collectors.toSet())));
      previewArea.removeAll();
      var subject = new H3(title.getValue());
      var content = new Div();
      content.setText(body.getValue());
      content.addClassName("cosmos-message-body");
      var recipients = new Div();
      var selected = new HashSet<>(preview.ids());
      service.targets(request.getSession(false)).stream()
          .filter(t -> selected.contains(t.id()))
          .forEach(t -> recipients.add(new Div(new Span(t.name() + " · " + t.email()))));
      previewArea.add(
          new Span(texts.getMessagesRecipients() + " (" + preview.ids().size() + ")"),
          recipients,
          subject,
          content);
      send.setEnabled(true);
    } catch (RuntimeException failure) {
      handle(failure);
    }
  }

  private void send() {
    if (!valid() || preview == null) return;
    send.setEnabled(false);
    try {
      service.send(
          request.getSession(false), requestId, title.getValue(), body.getValue(), preview);
      title.clear();
      body.clear();
      title.setInvalid(false);
      body.setInvalid(false);
      invalidatePreview();
      page = 0;
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseSuccessTitle,
              texts::getMessagesSent,
              NotificationHelper.NotificationType.SUCCESS);
      loadHistory();
    } catch (RuntimeException failure) {
      handle(failure);
      if (preview != null) send.setEnabled(true);
    }
  }

  private void handle(RuntimeException failure) {
    if (failure instanceof Rejected rejected) {
      switch (rejected.problem()) {
        case INVALID_DRAFT -> feedback(texts::getMessagesInvalidDraft);
        case NO_RECIPIENTS -> feedback(texts::getMessagesNoRecipients);
        case RECIPIENTS_CHANGED -> {
          invalidatePreview();
          feedback(texts::getMessagesRecipientsChanged);
        }
        case REQUEST_CONFLICT -> {
          invalidatePreview();
          feedback(texts::getMessagesRequestConflict);
        }
      }
    } else {
      LoggerFactory.getLogger(getClass()).warn("Admin message action failed", failure);
      feedback(texts::getMessagesUnavailable);
    }
  }

  private void feedback(java.util.function.Supplier<String> text) {
    NotificationHelper.getInstance()
        .showNotification(text, NotificationHelper.NotificationType.ERROR);
  }

  private void loadHistory() {
    var result = service.sent(request.getSession(false), page);
    history.removeAll();
    if (result.isEmpty()) history.add(new Span(texts.getMessagesEmpty()));
    for (var item : result) {
      var text = new Div();
      text.setText(item.body());
      text.addClassName("cosmos-message-body");
      var meta =
          new Span(
              UserMessageTexts.time(
                      item.createdAt(), com.vaadin.flow.component.UI.getCurrent().getLocale())
                  + " · "
                  + texts.getMessagesRecipients()
                  + ": "
                  + item.recipientCount());
      meta.addClassName("cosmos-messages-secondary");
      var row = new Div(new H3(item.title()), meta, text);
      row.addClassName("cosmos-message-row");
      history.add(row);
    }
    previous.setEnabled(page > 0);
    next.setEnabled(result.hasNext());
    pageLabel.setText(
        texts.getMessagesPage() + " " + (page + 1) + " / " + Math.max(1, result.getTotalPages()));
  }

  public void refreshTexts() {
    heading.setText(texts.getMessagesTitle());
    description.setText(texts.getMessagesAdminDescription());
    everyone.setLabel(texts.getMessagesEveryone());
    targets.setLabel(texts.getMessagesRecipients());
    title.setLabel(texts.getMessagesSubject());
    body.setLabel(texts.getMessagesBody());
    title.setErrorMessage(texts.getMessagesInvalidDraft());
    body.setErrorMessage(texts.getMessagesInvalidDraft());
    previewButton.setText(texts.getMessagesPreview());
    send.setText(texts.getMessagesSend());
    hint.setText(texts.getMessagesPreviewHint());
    historyTitle.setText(texts.getMessagesSentHistory());
    previous.setText(texts.getMessagesPrevious());
    next.setText(texts.getMessagesNext());
    if (isAttached()) {
      invalidatePreview();
      loadHistory();
    }
  }
}
