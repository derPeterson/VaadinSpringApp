package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
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
  private final transient MessageProperties texts;
  private final transient UserNotificationService service;
  private final transient HttpSession session;
  private final boolean compact;
  private final Div rows = new Div();
  private final H2 heading = new H2();
  private final Checkbox unread = new Checkbox();
  private final Button markAll = new Button();
  private final Button previous = new Button();
  private final Button next = new Button();
  private final Button refresh = new Button();
  private final Button history = new Button();
  private final Span pageLabel = new Span();
  private final Span retention = new Span();
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
    var toolbar = new Div(unread, refresh, markAll);
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
    history.addClickListener(e -> UI.getCurrent().navigate(UserNotificationsView.class));
    var paging = new Div(previous, pageLabel, next);
    paging.addClassName("cosmos-notifications-actions");
    paging.setVisible(!compact);
    history.setVisible(compact);
    retention.addClassName("cosmos-notifications-secondary");
    add(heading, toolbar, rows, paging, history, retention);
    refreshTexts();
    addDetachListener(
        e -> {
          if (detail != null) detail.close();
        });
  }

  public void refreshTexts() {
    heading.setText(texts.getNotificationsTitle());
    unread.setLabel(texts.getNotificationsUnread());
    refresh.setText(texts.getNotificationsRefresh());
    markAll.setText(texts.getNotificationsMarkAll());
    previous.setText(texts.getNotificationsPrevious());
    next.setText(texts.getNotificationsNext());
    history.setText(texts.getNotificationsHistory());
    retention.setText(texts.getNotificationsRetention(service.retentionDays()));
    if (isAttached()) refresh();
    if (detail != null && detail.isOpened() && openedItem != null) renderDetail();
  }

  public void refresh() {
    try {
      var result = service.inbox(session, unread.getValue(), page, compact ? 5 : 20);
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
    var meta = new Div(time, state);
    meta.addClassNames("cosmos-notifications-actions", "cosmos-notifications-secondary");
    var row = new Div(title, meta);
    row.addClassName("cosmos-notification-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) row.addClassName("cosmos-notification-row--unread");
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
