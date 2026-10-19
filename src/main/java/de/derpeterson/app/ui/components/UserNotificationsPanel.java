package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
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
  private final RadioButtonGroup<ReadFilter> readFilter = new RadioButtonGroup<>();
  private boolean updatingFilterLabels;
  private final Button markAll = new Button();
  private final Button previous = new Button();
  private final Button next = new Button();
  private final Button refresh = new Button();
  private final Button history = new Button();
  private final Span pageLabel = new Span();
  private final ComboBox<Integer> pageSize = new ComboBox<>();
  private final Span pageTotal = new Span();
  private final Button first = new Button(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
  private final Button last = new Button(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
  private final Div pageNumbers = new Div();
  private final TextField search = new TextField();
  private final Span totalLabel = new Span();
  private final Span totalCount = new Span("0");
  private final Span unreadCount = new Span("0");
  private final Span unreadLabel = new Span();
  private int totalPages = 1;
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
    readFilter.setId(compact ? "notifications-preview-filter" : "notifications-inbox-filter");
    readFilter.setItems(ReadFilter.values());
    readFilter.setItemLabelGenerator(this::filterLabel);
    readFilter.setValue(ReadFilter.ALL);
    readFilter.addValueChangeListener(
        e -> {
          if (!updatingFilterLabels) {
            page = 0;
            refresh();
          }
        });
    var toolbar = new Div(readFilter, refresh);
    toolbar.addClassName("cosmos-notifications-actions");
    refresh.addClickListener(e -> refresh());
    markAll.addClickListener(
        e ->
            action(
                () -> {
                  var snapshot = unreadIds;
                  boolean includesOpened = openedItem != null && snapshot.contains(openedItem.id());
                  service.markRead(session, snapshot);
                  if (includesOpened) updateOpenedRead();
                }));
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
    first.addClickListener(
        e -> {
          page = 0;
          refresh();
        });
    last.addClickListener(
        e -> {
          page = Math.max(0, totalPages - 1);
          refresh();
        });
    var paging = new Div(first, previous, pageNumbers, next, last);
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
      var archive = VaadinIcon.ARCHIVE.create();
      archive.getElement().setAttribute("aria-hidden", "true");
      var icon = new Div(archive);
      icon.addClassName("cosmos-panel-icon");
      description.addClassName("cosmos-panel-description");
      var titles = new Div(heading, description);
      titles.addClassName("cosmos-panel-heading");
      var counts = new Div(stat(totalLabel, totalCount), stat(unreadLabel, unreadCount));
      counts.addClassName("cosmos-notification-inbox-counts");
      totalCount.setId("notifications-inbox-total");
      unreadCount.setId("notifications-inbox-unread-total");
      var header = new Div(icon, titles, counts);
      header.addClassNames("cosmos-panel-header", "cosmos-notification-inbox-header");
      refresh.setIcon(VaadinIcon.REFRESH.create());
      markAll.setIcon(VaadinIcon.ENVELOPE_OPEN.create());
      search.setId("notifications-inbox-search");
      search.setPrefixComponent(VaadinIcon.SEARCH.create());
      search.setClearButtonVisible(true);
      search.setMaxLength(UserNotificationService.TITLE_LIMIT);
      search.setValueChangeMode(ValueChangeMode.EAGER);
      search.addValueChangeListener(
          e -> {
            page = 0;
            refresh();
          });
      var tools = new Div(refresh, markAll);
      tools.addClassName("cosmos-notification-inbox-tools");
      refresh.addClassName("cosmos-notification-inbox-tool");
      markAll.addClassName("cosmos-notification-inbox-tool");
      toolbar.removeAll();
      toolbar.add(search, readFilter, tools);
      toolbar.addClassName("cosmos-notification-inbox-toolbar");
      rows.addClassName("cosmos-notification-inbox-rows");
      previous.setIcon(VaadinIcon.ANGLE_LEFT.create());
      next.setIcon(VaadinIcon.ANGLE_RIGHT.create());
      pageNumbers.addClassName("cosmos-notification-inbox-page-numbers");
      paging.addClassName("cosmos-notification-inbox-pagination");
      pageSize.setId("notifications-inbox-page-size");
      pageSize.setItems(10, 20, 50);
      pageSize.setValue(10);
      pageSize.setAllowCustomValue(false);
      pageSize.setWidth("56px");
      pageSize.addValueChangeListener(
          e -> {
            if (e.getValue() != null) {
              page = 0;
              refresh();
            }
          });
      var summary = new Div(pageLabel, pageSize, pageTotal);
      summary.addClassName("cosmos-notification-inbox-page-summary");
      var paginationRow = new Div(summary, paging);
      paginationRow.addClassName("cosmos-notification-inbox-pagination-row");
      var footer = new Div(paginationRow, retention);
      footer.addClassName("cosmos-notification-inbox-footer");
      add(header, toolbar, rows, footer);
    }
    refreshTexts();
    addDetachListener(
        e -> {
          if (detail != null) detail.close();
        });
  }

  private Div stat(Span label, Span value) {
    label.addClassName("cosmos-notification-inbox-stat-label");
    value.addClassName("cosmos-notification-inbox-stat-value");
    var stat = new Div(label, value);
    stat.addClassName("cosmos-notification-inbox-stat");
    return stat;
  }

  public void setCloseAction(Runnable closeAction) {
    this.closeAction = Objects.requireNonNull(closeAction);
  }

  public void setDetailOpenListener(java.util.function.Consumer<Boolean> listener) {
    detailOpenListener = Objects.requireNonNull(listener);
  }

  public void refreshTexts() {
    heading.setText(texts.getNotificationsTitle());
    description.setText(
        compact ? texts.getNotificationsDescription() : texts.getNotificationsInboxDescription());
    totalLabel.setText(texts.getNotificationsTotal());
    unreadLabel.setText(texts.getNotificationsUnread());
    search.setPlaceholder(texts.getNotificationsSearch());
    search.setAriaLabel(texts.getNotificationsSearch());
    close.setAriaLabel(texts.getNotificationsClose());
    readFilter.setAriaLabel(texts.getNotificationsFilter());
    updatingFilterLabels = true;
    try {
      var selected = selectedFilter();
      readFilter.getDataProvider().refreshAll();
      readFilter.setValue(selected);
    } finally {
      updatingFilterLabels = false;
    }
    refresh.setAriaLabel(texts.getNotificationsRefresh());
    refresh.setTooltipText(texts.getNotificationsRefresh());
    markAll.setAriaLabel(texts.getNotificationsMarkAll());
    markAll.setTooltipText(texts.getNotificationsMarkAll());
    pageLabel.setText(texts.getAdminUsersPaginationPerPage());
    pageSize.setAriaLabel(texts.getAdminUsersPaginationPerPage());
    previous.setAriaLabel(texts.getNotificationsPrevious());
    previous.setTooltipText(texts.getNotificationsPrevious());
    next.setAriaLabel(texts.getNotificationsNext());
    next.setTooltipText(texts.getNotificationsNext());
    first.setAriaLabel(texts.getAdminUsersPaginationFirstPage());
    first.setTooltipText(texts.getAdminUsersPaginationFirstPage());
    last.setAriaLabel(texts.getAdminUsersPaginationLastPage());
    last.setTooltipText(texts.getAdminUsersPaginationLastPage());
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

  private ReadFilter selectedFilter() {
    return Objects.requireNonNullElse(readFilter.getValue(), ReadFilter.ALL);
  }

  private String filterLabel(ReadFilter filter) {
    return switch (filter) {
      case ALL -> texts.getNotificationsAll();
      case READ -> texts.getNotificationsRead();
      case UNREAD -> texts.getNotificationsUnread();
    };
  }

  private void refreshPreviewHint() {
    previewHint.setText(
        switch (selectedFilter()) {
          case ALL -> texts.getNotificationsLatest(PREVIEW_LIMIT);
          case READ -> texts.getNotificationsLatestRead(PREVIEW_LIMIT);
          case UNREAD -> texts.getNotificationsLatestUnread(PREVIEW_LIMIT);
        });
  }

  public void refresh() {
    refreshRetention();
    refreshPreviewHint();
    try {
      var result =
          compact
              ? service.inbox(session, selectedFilter(), page, PREVIEW_LIMIT)
              : search.isEmpty()
                  ? service.inbox(session, selectedFilter(), page, selectedPageSize())
                  : service.searchInbox(
                      session,
                      selectedFilter(),
                      search.getValue(),
                      UserNotificationTexts.matchingTypes(texts, search.getValue()),
                      page,
                      selectedPageSize());
      if (page > 0 && page >= result.getTotalPages()) {
        page = Math.max(0, result.getTotalPages() - 1);
        refresh();
        return;
      }
      unreadIds = service.unreadSnapshot(session);
      if (!compact) {
        totalCount.setText(Long.toString(service.inboxCount(session)));
        unreadCount.setText(Integer.toString(unreadIds.size()));
      }
      if (detail != null && detail.isOpened() && openedItem != null) {
        result.stream()
            .filter(item -> item.id().equals(openedItem.id()))
            .findFirst()
            .ifPresent(
                item -> {
                  if (item.read() != openedItem.read()) {
                    openedItem = item;
                    renderDetail();
                  }
                });
      }
      markAll.setEnabled(!unreadIds.isEmpty());
      rows.removeAll();
      if (result.isEmpty()) {
        var empty =
            new Span(
                !compact && !search.isEmpty()
                    ? texts.getNotificationsSearchEmpty()
                    : texts.getNotificationsEmpty());
        if (compact) {
          empty.addClassName("cosmos-notifications-secondary");
          rows.add(empty);
        } else {
          var icon = VaadinIcon.INFO_CIRCLE_O.create();
          icon.getElement().setAttribute("aria-hidden", "true");
          var notice = new Div(icon, empty);
          notice.addClassName("cosmos-notification-inbox-empty");
          rows.add(notice);
        }
      }
      for (var item : result) rows.add(row(item));
      totalPages = Math.max(1, result.getTotalPages());
      first.setEnabled(page > 0);
      previous.setEnabled(page > 0);
      next.setEnabled(result.hasNext());
      last.setEnabled(result.hasNext());
      if (!compact) renderPageNumbers();
      pageTotal.setText(
          texts.getAdminUsersPaginationTotal(Math.toIntExact(result.getTotalElements())));
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
    first.setEnabled(false);
    last.setEnabled(false);
    pageNumbers.removeAll();
    pageTotal.setText("–");
    totalCount.setText("–");
    unreadCount.setText("–");
    if (detail != null) detail.close();
  }

  private int selectedPageSize() {
    return Objects.requireNonNullElse(pageSize.getValue(), 10);
  }

  private void renderPageNumbers() {
    pageNumbers.removeAll();
    var pages = new TreeSet<Integer>();
    pages.add(0);
    pages.add(totalPages - 1);
    int from = totalPages <= 7 ? 0 : Math.max(1, Math.min(page - 1, totalPages - 4));
    int to = totalPages <= 7 ? totalPages - 1 : Math.min(totalPages - 2, Math.max(page + 1, 3));
    for (int index = from; index <= to; index++) pages.add(index);
    int preceding = -1;
    for (int index : pages) {
      if (preceding >= 0 && index - preceding > 1) pageNumbers.add(new Span("…"));
      var button =
          new Button(
              Integer.toString(index + 1),
              e -> {
                page = index;
                refresh();
              });
      button.setAriaLabel(texts.getAdminUsersPaginationPage(index + 1));
      if (index == page) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.getElement().setAttribute("aria-current", "page");
      }
      pageNumbers.add(button);
      preceding = index;
    }
  }

  private Div row(Item item) {
    var title = new Button(UserNotificationTexts.title(texts, item), e -> action(() -> open(item)));
    title.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    title.addClassName("cosmos-notification-title");
    var time = new Span(UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale()));
    var state =
        new Span(item.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread());
    if (compact) return previewRow(item, title, time, state);
    var dot = new Span();
    dot.addClassName("cosmos-notification-preview-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!item.read());
    var titleRow = new Div(dot, title);
    titleRow.addClassName("cosmos-notification-inbox-title");
    var source = new Span(source(item));
    source.addClassName("cosmos-notification-preview-source");
    state.getElement().setAttribute("theme", "badge");
    state.addClassName("cosmos-notification-preview-state");
    var meta = new Div(time, source, state);
    meta.addClassName("cosmos-notification-inbox-meta");
    var body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-inbox-message");
    var content = new Div(titleRow, body);
    content.addClassName("cosmos-notification-inbox-content");
    var open =
        new Button(
            texts.getNotificationsOpen(), VaadinIcon.EYE.create(), e -> action(() -> open(item)));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    var mark =
        new Button(
            texts.getNotificationsMarkRead(),
            VaadinIcon.ENVELOPE_OPEN.create(),
            e -> action(() -> markRead(item)));
    mark.setVisible(!item.read());
    mark.addClassName("cosmos-notification-inbox-mark-read");
    mark.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    var actions = new Div(open, mark);
    actions.addClassName("cosmos-notification-inbox-actions");
    var footer = new Div(meta, actions);
    footer.addClassName("cosmos-notification-inbox-row-footer");
    var row = new Div(content, footer);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-inbox-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) row.addClassName("cosmos-notification-row--unread");
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    row.getElement()
        .addEventListener("click", e -> action(() -> open(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
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
    open.setIcon(VaadinIcon.EYE.create());
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    var markRead = new Button(texts.getNotificationsMarkRead(), e -> action(() -> markRead(item)));
    markRead.setIcon(VaadinIcon.ENVELOPE_OPEN.create());
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
    service.ownerId(session);
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

  private void markRead(Item item) {
    service.markRead(session, List.of(item.id()));
    if (openedItem != null && openedItem.id().equals(item.id())) updateOpenedRead();
  }

  private void updateOpenedRead() {
    openedItem =
        new Item(
            openedItem.id(),
            openedItem.type(),
            openedItem.title(),
            openedItem.body(),
            openedItem.createdAt(),
            true);
    if (detail != null && detail.isOpened()) renderDetail();
  }

  private String source(Item item) {
    return item.type() == UserNotificationType.ADMIN_ANNOUNCEMENT
        ? texts.getNotificationsSourceAdministration()
        : texts.getNotificationsSourceSystem();
  }

  private void renderDetail() {
    detail.removeAll();
    detail.getHeader().removeAll();
    detail.getFooter().removeAll();
    var subject = UserNotificationTexts.title(texts, openedItem);
    var timestamp = UserNotificationTexts.time(openedItem.createdAt(), UI.getCurrent().getLocale());
    var stateText =
        openedItem.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread();
    var title = new H2(texts.getNotificationsDetailTitle());
    title.addClassName("cosmos-panel-title");
    var description = new Span(texts.getNotificationsDetailDescription());
    description.addClassName("cosmos-panel-description");
    var titles = new Div(title, description);
    titles.addClassName("cosmos-panel-heading");
    var icon = VaadinIcon.PAPERPLANE.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    var badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    var closeIcon = new Button(VaadinIcon.CLOSE_CIRCLE.create(), e -> detail.close());
    closeIcon.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeIcon.addClassName("cosmos-overlay-close");
    closeIcon.setAriaLabel(texts.getNotificationsClose());
    closeIcon.setTooltipText(texts.getNotificationsClose());
    var header = new Div(badge, titles, closeIcon);
    header.addClassName("cosmos-notification-detail-header");
    detail.getHeader().add(header);
    detail.getElement().setProperty("ariaLabel", subject);

    var contentTitle = new H3(subject);
    contentTitle.addClassName("cosmos-notification-detail-title");
    var dot = new Span();
    dot.addClassNames("cosmos-notification-preview-dot", "cosmos-notification-detail-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!openedItem.read());
    var titleRow = new Div(dot, contentTitle);
    titleRow.addClassName("cosmos-notification-detail-title-row");
    var state = new Span(stateText);
    state.getElement().setAttribute("theme", "badge");
    state.addClassName("cosmos-notification-detail-state");
    var source = new Span(source(openedItem));
    source.addClassName("cosmos-notification-detail-source");
    var contentMeta = new Div(new Span(timestamp), source, state);
    contentMeta.addClassName("cosmos-notification-detail-meta");
    var body = new Div();
    body.setText(UserNotificationTexts.body(texts, openedItem));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-detail-body");
    var message = new Div(titleRow, contentMeta, body);
    message.addClassName("cosmos-notification-detail-message");
    if (!openedItem.read()) message.addClassName("cosmos-notification-detail-message--unread");
    detail.add(message);

    var history =
        new Button(
            texts.getNotificationsHistory(),
            VaadinIcon.ENVELOPES.create(),
            e -> {
              detail.close();
              closeAction.run();
              UI.getCurrent().navigate(UserNotificationsView.class);
            });
    history.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    history.addClassName("cosmos-notification-detail-history");
    history.setVisible(compact);
    var markRead =
        new Button(
            texts.getNotificationsMarkRead(),
            VaadinIcon.ENVELOPE_OPEN.create(),
            e -> action(() -> markRead(openedItem)));
    markRead.addClassName("cosmos-notification-detail-mark-read");
    markRead.setVisible(!openedItem.read());
    var close =
        new Button(
            texts.getNotificationsClose(), VaadinIcon.CLOSE_CIRCLE.create(), e -> detail.close());
    close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    var actions = new Div(history);
    actions.addClassNames("cosmos-dialog-actions", "cosmos-notification-detail-actions");
    if (openedItem.type() != UserNotificationType.ADMIN_ANNOUNCEMENT) {
      var account =
          new Button(
              texts.getNotificationsOpenAccount(),
              VaadinIcon.COG.create(),
              e -> {
                detail.close();
                closeAction.run();
                UI.getCurrent()
                    .navigate(AccountView.class, QueryParameters.simple(Map.of("tab", "security")));
              });
      account.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      actions.add(account);
    }
    actions.add(markRead, close);
    detail.getFooter().add(actions);
  }
}
