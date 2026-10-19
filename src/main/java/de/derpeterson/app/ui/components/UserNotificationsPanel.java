package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.views.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.slf4j.LoggerFactory;

/** Gemeinsame kompakte Vorschau und paginierter eigener Posteingang. */
public class UserNotificationsPanel extends Div {
  private static final int PREVIEW_LIMIT = 5;
  private final transient MessageProperties texts;
  private final transient UserNotificationService service;
  private final transient HttpSession session;
  private final boolean compact;
  private final Div rows = new Div();
  private final H2 heading = new H2();
  private final Span description = new Span();
  private final Button close = new Button(VaadinIcon.CLOSE_CIRCLE.create());
  private transient Runnable closeAction = () -> {};
  private transient java.util.function.Consumer<Boolean> detailOpenListener = open -> {};
  private final Checkbox unread = new Checkbox();
  private final Button markAll = new Button();
  private final Button previous = new Button();
  private final Button next = new Button();
  private final Button refresh = new Button();
  private final Button history = new Button();
  private final Span pageLabel = new Span();
  private final Span retention = new Span();
  private final Span previewHint = new Span();
  private List<Long> unreadIds = List.of();
  private int page;
  private Dialog detail;
  private Item openedItem;

  public UserNotificationsPanel(
      MessageProperties texts,
      UserNotificationService service,
      HttpSession session,
      boolean compact) {
    this.texts = texts;
    this.service = service;
    this.session = session;
    this.compact = compact;
    addClassNames("cosmos-theme", "cosmos-notifications-panel");
    setId(compact ? "notifications-preview" : "notifications-inbox");
    heading.addClassName("cosmos-panel-title");
    var toolbar = new Div(unread, refresh);
    toolbar.addClassName("cosmos-notifications-actions");
    refresh.addClickListener(e -> refresh());
    markAll.addClickListener(e -> action(() -> service.markRead(session, unreadIds)));
    unread.addValueChangeListener(
        e -> {
          page = 0;
          refresh();
        });
    previous.addClickListener(
        e -> {
          if (page > 0) page--;
          refresh();
        });
    next.addClickListener(
        e -> {
          page++;
          refresh();
        });
    history.addClickListener(
        e -> {
          closeAction.run();
          UI.getCurrent().navigate(UserNotificationsView.class);
        });
    var paging = new Div(previous, pageLabel, next);
    paging.addClassName("cosmos-notifications-actions");
    paging.setVisible(!compact);
    history.setVisible(compact);
    retention.addClassName("cosmos-notifications-secondary");
    if (compact) {
      var icon = VaadinIcon.PAPERPLANE.create();
      icon.getElement().setAttribute("aria-hidden", "true");
      var badge = new Div(icon);
      badge.addClassName("cosmos-panel-icon");
      description.addClassName("cosmos-panel-description");
      var titles = new Div(heading, description);
      titles.addClassName("cosmos-panel-heading");
      close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      close.addClassName("cosmos-overlay-close");
      close.addClickListener(e -> closeAction.run());
      var header = new Div(badge, titles, close);
      header.addClassName("cosmos-notification-preview-header");
      toolbar.addClassName("cosmos-notification-preview-toolbar");
      refresh.setIcon(VaadinIcon.REFRESH.create());
      refresh.addClassNames(
          "cosmos-notification-preview-refresh", "cosmos-notification-preview-tool");
      markAll.setIcon(VaadinIcon.ENVELOPE_OPEN.create());
      markAll.addClassNames(
          "cosmos-notification-preview-mark-all", "cosmos-notification-preview-tool");
      toolbar.add(markAll);
      rows.addClassName("cosmos-notification-preview-rows");
      previewHint.addClassName("cosmos-notification-preview-hint");
      var content = new Div(toolbar, previewHint, rows);
      content.addClassName("cosmos-notification-preview-content");
      history.setIcon(VaadinIcon.ENVELOPES.create());
      var footer = new Div(history, retention);
      footer.addClassName("cosmos-notification-preview-footer");
      add(header, content, footer);
    } else {
      toolbar.add(markAll);
      add(heading, toolbar, rows, paging, history, retention);
    }
    refreshTexts();
    addDetachListener(
        e -> {
          if (detail != null) detail.close();
        });
  }

  public void setCloseAction(Runnable closeAction) {
    this.closeAction = Objects.requireNonNull(closeAction);
  }

  public void setDetailOpenListener(java.util.function.Consumer<Boolean> listener) {
    detailOpenListener = Objects.requireNonNull(listener);
  }

  public void refreshTexts() {
    heading.setText(texts.getNotificationsTitle());
    description.setText(texts.getNotificationsDescription());
    close.setAriaLabel(texts.getNotificationsClose());
    unread.setLabel(compact ? texts.getNotificationsOnlyUnread() : texts.getNotificationsUnread());
    refresh.setText(compact ? "" : texts.getNotificationsRefresh());
    if (compact) {
      refresh.setAriaLabel(texts.getNotificationsRefresh());
      refresh.setTooltipText(texts.getNotificationsRefresh());
      markAll.setAriaLabel(texts.getNotificationsMarkAll());
      markAll.setTooltipText(texts.getNotificationsMarkAll());
    }
    markAll.setText(compact ? "" : texts.getNotificationsMarkAll());
    previous.setText(texts.getNotificationsPrevious());
    next.setText(texts.getNotificationsNext());
    history.setText(texts.getNotificationsHistory());
    refreshRetention();
    refreshPreviewHint();
    if (isAttached()) refresh();
    if (detail != null && detail.isOpened() && openedItem != null) renderDetail();
  }

  private void refreshRetention() {
    var days = service.retentionDays();
    retention.setText(
        days.isPresent()
            ? texts.getNotificationsRetention(days.getAsInt())
            : texts.getNotificationsRetentionUnlimited());
  }

  private void refreshPreviewHint() {
    previewHint.setText(
        unread.getValue()
            ? texts.getNotificationsLatestUnread(PREVIEW_LIMIT)
            : texts.getNotificationsLatest(PREVIEW_LIMIT));
  }

  public void refresh() {
    refreshRetention();
    refreshPreviewHint();
    try {
      var result = service.inbox(session, unread.getValue(), page, compact ? PREVIEW_LIMIT : 20);
      if (page > 0 && page >= result.getTotalPages()) {
        page = Math.max(0, result.getTotalPages() - 1);
        refresh();
        return;
      }
      unreadIds = service.unreadSnapshot(session);
      markAll.setEnabled(!unreadIds.isEmpty());
      rows.removeAll();
      if (result.isEmpty()) rows.add(new Span(texts.getNotificationsEmpty()));
      for (var item : result) rows.add(row(item));
      previous.setEnabled(page > 0);
      next.setEnabled(result.hasNext());
      pageLabel.setText(
          texts.getNotificationsPage()
              + " "
              + (page + 1)
              + " / "
              + Math.max(1, result.getTotalPages()));
    } catch (RuntimeException failure) {
      unavailable();
    }
  }

  public void unavailable() {
    unreadIds = List.of();
    rows.removeAll();
    rows.add(new Span(texts.getNotificationsUnavailable()));
    markAll.setEnabled(false);
    previous.setEnabled(false);
    next.setEnabled(false);
    if (detail != null) detail.close();
  }

  private Div row(Item item) {
    var title = new Button(UserNotificationTexts.title(texts, item), e -> action(() -> open(item)));
    title.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    title.addClassName("cosmos-notification-title");
    var time = new Span(UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale()));
    var state =
        new Span(item.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread());
    if (compact) return previewRow(item, title, time, state);
    var meta = new Div(time, state);
    meta.addClassNames("cosmos-notifications-actions", "cosmos-notifications-secondary");
    var row = new Div(title, meta);
    row.addClassName("cosmos-notification-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) row.addClassName("cosmos-notification-row--unread");
    return row;
  }

  private Div previewRow(Item item, Button title, Span time, Span state) {
    var dot = new Span();
    dot.addClassName("cosmos-notification-preview-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!item.read());
    var titleRow = new Div(dot, title);
    titleRow.addClassName("cosmos-notification-preview-title");
    var dismiss =
        new Button(
            VaadinIcon.CLOSE_CIRCLE.create(),
            e ->
                action(
                    () -> {
                      service.dismiss(session, item.id());
                      if (detail != null && openedItem != null && openedItem.id().equals(item.id()))
                        detail.close();
                    }));
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    dismiss.addClassName("cosmos-notification-preview-dismiss");
    dismiss.setTooltipText(texts.getNotificationsDismiss());
    dismiss.setAriaLabel(
        texts.getNotificationsDismiss() + ": " + UserNotificationTexts.title(texts, item));
    titleRow.add(dismiss);
    var source =
        new Span(
            item.type() == UserNotificationType.ADMIN_ANNOUNCEMENT
                ? texts.getNotificationsSourceAdministration()
                : texts.getNotificationsSourceSystem());
    source.addClassName("cosmos-notification-preview-source");
    state.addClassName("cosmos-notification-preview-state");
    state.getElement().setAttribute("theme", "badge");
    var meta = new Div(time, source, state);
    meta.addClassName("cosmos-notification-preview-meta");
    var body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-preview-message");
    var open = new Button(texts.getNotificationsOpen(), e -> action(() -> open(item)));
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    var markRead =
        new Button(
            texts.getNotificationsMarkRead(),
            e -> action(() -> service.markRead(session, List.of(item.id()))));
    markRead.addClassName("cosmos-notification-preview-mark-read");
    markRead.setVisible(!item.read());
    markRead.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    var actions = new Div(open, markRead);
    actions.addClassName("cosmos-notification-preview-actions");
    var row = new Div(titleRow, meta, body, actions);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-preview-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) row.addClassName("cosmos-notification-row--unread");
    // Doppelklicks auf Schaltflächen bleiben deren eigener Aktion vorbehalten.
    row.getElement()
        .addEventListener("dblclick", e -> action(() -> open(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return row;
  }

  private void action(Runnable action) {
    try {
      action.run();
      refresh();
    } catch (RuntimeException failure) {
      if (failure instanceof org.springframework.security.access.AccessDeniedException)
        unavailable();
      LoggerFactory.getLogger(getClass()).warn("Notification action failed", failure);
      NotificationHelper.getInstance()
          .showNotification(
              texts::getNotificationsUnavailable, NotificationHelper.NotificationType.ERROR);
    }
  }

  private void open(Item item) {
    service.markRead(session, List.of(item.id()));
    if (detail != null) detail.close();
    openedItem = item;
    detail = new Dialog();
    detail.addOpenedChangeListener(e -> detailOpenListener.accept(e.isOpened()));
    detail.addClassNames("cosmos-theme", "cosmos-notification-dialog");
    detail.setModality(com.vaadin.flow.component.ModalityMode.STRICT);
    detail.getThemeNames().add("cosmos");
    detail.setCloseOnOutsideClick(true);
    detail.setCloseOnEsc(true);
    renderDetail();
    detail.open();
  }

  private void renderDetail() {
    detail.removeAll();
    detail.getHeader().removeAll();
    detail.getFooter().removeAll();
    var title = new H2(UserNotificationTexts.title(texts, openedItem));
    title.addClassName("cosmos-panel-title");
    var icon = com.vaadin.flow.component.icon.VaadinIcon.BELL.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    var badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    var closeIcon =
        new Button(
            com.vaadin.flow.component.icon.VaadinIcon.CLOSE_CIRCLE.create(), e -> detail.close());
    closeIcon.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeIcon.addClassName("cosmos-overlay-close");
    closeIcon.setAriaLabel(texts.getNotificationsClose());
    var header = new Div(badge, title, closeIcon);
    header.addClassName("cosmos-notification-detail-header");
    detail.getHeader().add(header);
    detail.getElement().setProperty("ariaLabel", title.getText());
    var body = new Div();
    body.setText(UserNotificationTexts.body(texts, openedItem));
    body.addClassName("cosmos-notification-body");
    detail.add(body);
    var close = new Button(texts.getNotificationsClose(), e -> detail.close());
    var actions = new Div(close);
    actions.addClassName("cosmos-dialog-actions");
    if (openedItem.type() != UserNotificationType.ADMIN_ANNOUNCEMENT) {
      var account =
          new Button(
              texts.getNotificationsOpenAccount(),
              e -> {
                detail.close();
                UI.getCurrent()
                    .navigate(AccountView.class, QueryParameters.simple(Map.of("tab", "security")));
              });
      account.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      actions.add(account);
    }
    detail.getFooter().add(actions);
  }
}
