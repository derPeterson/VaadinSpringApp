package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.UserNotificationTexts;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.UserNotificationsView;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

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
  private transient Consumer<Boolean> detailOpenListener = open -> {};
  private final RadioButtonGroup<ReadFilter> readFilter = new RadioButtonGroup<>();
  private boolean updatingFilterLabels;
  private final Button markAll = new Button();
  private final Button dismissRead = new Button(VaadinIcon.TRASH.create());
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
    configureReadFilter();
    Div toolbar = new Div(readFilter, refresh);
    toolbar.addClassName("cosmos-notifications-actions");
    configureCommonActions();
    Div paging = new Div(first, previous, pageNumbers, next, last);
    paging.addClassName("cosmos-notifications-actions");
    paging.setVisible(!compact);
    history.setVisible(compact);
    retention.addClassName("cosmos-notifications-secondary");
    if (compact) {
      buildPreview(toolbar);
    } else {
      buildInbox(toolbar, paging);
    }
    refreshTexts();
    addDetachListener(
        e -> {
          if (detail != null) {
            detail.close();
          }
        });
  }

  private void configureReadFilter() {
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
  }

  private void configureCommonActions() {
    SvgIcon readAllIcon = new SvgIcon("icons/envelopes-check.svg");
    readAllIcon.getElement().setAttribute("aria-hidden", "true");
    markAll.setIcon(readAllIcon);
    dismissRead.addClassName("cosmos-delete-button");
    dismissRead.setEnabled(false);
    dismissRead.addClickListener(
        e ->
            action(
                () -> {
                  service.dismissRead(session);
                  if (detail != null && openedItem != null && openedItem.read()) {
                    detail.close();
                  }
                }));
    refresh.addClickListener(e -> refresh());
    markAll.addClickListener(
        e ->
            action(
                () -> {
                  List<Long> snapshot = unreadIds;
                  boolean includesOpened = openedItem != null && snapshot.contains(openedItem.id());
                  service.markRead(session, snapshot);
                  if (includesOpened) {
                    updateOpenedRead();
                  }
                }));
    previous.addClickListener(
        e -> {
          if (page > 0) {
            page--;
          }
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
  }

  private void buildPreview(Div toolbar) {
    Icon icon = VaadinIcon.PAPERPLANE.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    Div badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    description.addClassName("cosmos-panel-description");
    Div titles = new Div(heading, description);
    titles.addClassName("cosmos-panel-heading");
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(e -> closeAction.run());
    Div header = new Div(badge, titles, close);
    header.addClassName("cosmos-notification-preview-header");
    toolbar.addClassName("cosmos-notification-preview-toolbar");
    refresh.setIcon(VaadinIcon.REFRESH.create());
    refresh.addClassNames(
        "cosmos-notification-preview-refresh", "cosmos-notification-preview-tool");
    markAll.addClassNames(
        "cosmos-notification-preview-mark-all", "cosmos-notification-preview-tool");
    toolbar.add(markAll);
    rows.addClassName("cosmos-notification-preview-rows");
    previewHint.addClassName("cosmos-notification-preview-hint");
    Div content = new Div(toolbar, previewHint, rows);
    content.addClassName("cosmos-notification-preview-content");
    history.setIcon(VaadinIcon.ENVELOPES.create());
    Div footer = new Div(history, retention);
    footer.addClassName("cosmos-notification-preview-footer");
    add(header, content, footer);
  }

  private void buildInbox(Div toolbar, Div paging) {
    Icon archive = VaadinIcon.ARCHIVE.create();
    archive.getElement().setAttribute("aria-hidden", "true");
    Div icon = new Div(archive);
    icon.addClassName("cosmos-panel-icon");
    description.addClassName("cosmos-panel-description");
    Div titles = new Div(heading, description);
    titles.addClassName("cosmos-panel-heading");
    Div counts = new Div(stat(totalLabel, totalCount), stat(unreadLabel, unreadCount));
    counts.addClassName("cosmos-notification-inbox-counts");
    totalCount.setId("notifications-inbox-total");
    unreadCount.setId("notifications-inbox-unread-total");
    Div header = new Div(icon, titles, counts);
    header.addClassNames("cosmos-panel-header", "cosmos-notification-inbox-header");
    refresh.setIcon(VaadinIcon.REFRESH.create());
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
    Div tools = new Div(refresh, markAll, dismissRead);
    tools.addClassName("cosmos-notification-inbox-tools");
    refresh.addClassName("cosmos-notification-inbox-tool");
    markAll.addClassName("cosmos-notification-inbox-tool");
    dismissRead.addClassName("cosmos-notification-inbox-tool");
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
    Div summary = new Div(pageLabel, pageSize, pageTotal);
    summary.addClassName("cosmos-notification-inbox-page-summary");
    Div paginationRow = new Div(summary, paging);
    paginationRow.addClassName("cosmos-notification-inbox-pagination-row");
    Div footer = new Div(paginationRow, retention);
    footer.addClassName("cosmos-notification-inbox-footer");
    add(header, toolbar, rows, footer);
  }

  private Div stat(Span label, Span value) {
    label.addClassName("cosmos-notification-inbox-stat-label");
    value.addClassName("cosmos-notification-inbox-stat-value");
    Div stat = new Div(label, value);
    stat.addClassName("cosmos-notification-inbox-stat");
    return stat;
  }

  public void setCloseAction(Runnable closeAction) {
    this.closeAction = Objects.requireNonNull(closeAction);
  }

  public void setDetailOpenListener(Consumer<Boolean> listener) {
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
      ReadFilter selected = selectedFilter();
      readFilter.getDataProvider().refreshAll();
      readFilter.setValue(selected);
    } finally {
      updatingFilterLabels = false;
    }
    refresh.setAriaLabel(texts.getNotificationsRefresh());
    refresh.setTooltipText(texts.getNotificationsRefresh());
    markAll.setAriaLabel(texts.getNotificationsMarkAll());
    markAll.setTooltipText(texts.getNotificationsMarkAll());
    dismissRead.setTooltipText(texts.getNotificationsDismissRead());
    dismissRead.setAriaLabel(texts.getNotificationsDismissRead());
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
    if (isAttached()) {
      refresh();
    }
    if (detail != null && detail.isOpened() && openedItem != null) {
      renderDetail();
    }
  }

  private void refreshRetention() {
    OptionalInt days = service.retentionDays();
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

  private Page<Item> loadPage() {
    if (compact) {
      return service.inbox(session, selectedFilter(), page, PREVIEW_LIMIT);
    }
    if (search.isEmpty()) {
      return service.inbox(session, selectedFilter(), page, selectedPageSize());
    }
    return service.searchInbox(
        session,
        selectedFilter(),
        search.getValue(),
        UserNotificationTexts.matchingTypes(texts, search.getValue()),
        page,
        selectedPageSize());
  }

  public void refresh() {
    refreshRetention();
    refreshPreviewHint();
    try {
      Page<Item> result = loadPage();
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
      dismissRead.setEnabled(!compact && service.readCount(session) > 0);
      rows.removeAll();
      if (result.isEmpty()) {
        Span empty =
            new Span(
                !compact && !search.isEmpty()
                    ? texts.getNotificationsSearchEmpty()
                    : texts.getNotificationsEmpty());
        if (compact) {
          empty.addClassName("cosmos-notifications-secondary");
          rows.add(empty);
        } else {
          Icon icon = VaadinIcon.INFO_CIRCLE.create();
          icon.getElement().setAttribute("aria-hidden", "true");
          Div notice = new Div(icon, empty);
          notice.addClassName("cosmos-notification-inbox-empty");
          rows.add(notice);
        }
      }
      for (Item item : result) {
        rows.add(row(item));
      }
      totalPages = Math.max(1, result.getTotalPages());
      first.setEnabled(page > 0);
      previous.setEnabled(page > 0);
      next.setEnabled(result.hasNext());
      last.setEnabled(result.hasNext());
      if (!compact) {
        renderPageNumbers();
      }
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
    dismissRead.setEnabled(false);
    previous.setEnabled(false);
    next.setEnabled(false);
    first.setEnabled(false);
    last.setEnabled(false);
    pageNumbers.removeAll();
    pageTotal.setText("–");
    totalCount.setText("–");
    unreadCount.setText("–");
    if (detail != null) {
      detail.close();
    }
  }

  private int selectedPageSize() {
    return Objects.requireNonNullElse(pageSize.getValue(), 10);
  }

  private void renderPageNumbers() {
    pageNumbers.removeAll();
    TreeSet<Integer> pages = new TreeSet<Integer>();
    pages.add(0);
    pages.add(totalPages - 1);
    int from = totalPages <= 7 ? 0 : Math.max(1, Math.min(page - 1, totalPages - 4));
    int to = totalPages <= 7 ? totalPages - 1 : Math.min(totalPages - 2, Math.max(page + 1, 3));
    for (int index = from; index <= to; index++) {
      pages.add(index);
    }
    int preceding = -1;
    for (int index : pages) {
      if (preceding >= 0 && index - preceding > 1) {
        pageNumbers.add(new Span("…"));
      }
      Button button =
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
    Button title =
        new Button(UserNotificationTexts.title(texts, item), e -> action(() -> open(item)));
    title.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    title.addClassName("cosmos-notification-title");
    Span time = new Span(UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale()));
    Span state =
        new Span(item.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread());
    if (compact) {
      return previewRow(item, title, time, state);
    }
    Span dot = new Span();
    dot.addClassName("cosmos-notification-preview-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!item.read());
    Div titleRow = new Div(dot, title, dismissButton(item));
    titleRow.addClassName("cosmos-notification-inbox-title");
    Span source = new Span(source(item));
    source.addClassName("cosmos-notification-preview-source");
    state.getElement().setAttribute("theme", "badge");
    state.addClassName("cosmos-notification-preview-state");
    Div meta = new Div(time, source, state);
    meta.addClassName("cosmos-notification-inbox-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-inbox-message");
    Div content = new Div(titleRow, body);
    content.addClassName("cosmos-notification-inbox-content");
    Button open = new Button(VaadinIcon.EYE.create(), e -> action(() -> open(item)));
    open.addClassNames("cosmos-notification-row-action", "cosmos-notification-row-open");
    open.setTooltipText(texts.getNotificationsOpen());
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    Button mark = new Button(VaadinIcon.ENVELOPE_OPEN.create(), e -> action(() -> markRead(item)));
    mark.setVisible(!item.read());
    mark.addClassNames("cosmos-notification-inbox-mark-read", "cosmos-notification-row-action");
    mark.setTooltipText(texts.getNotificationsMarkRead());
    mark.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    Div actions = new Div(open, mark);
    actions.addClassName("cosmos-notification-inbox-actions");
    Div footer = new Div(meta, actions);
    footer.addClassName("cosmos-notification-inbox-row-footer");
    Div row = new Div(content, footer);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-inbox-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) {
      row.addClassName("cosmos-notification-row--unread");
    }
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    row.getElement()
        .addEventListener("click", e -> action(() -> open(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return row;
  }

  private Button dismissButton(Item item) {
    Button dismiss =
        new Button(
            VaadinIcon.CLOSE_CIRCLE.create(),
            e ->
                action(
                    () -> {
                      service.dismiss(session, item.id());
                      if (detail != null
                          && openedItem != null
                          && openedItem.id().equals(item.id())) {
                        detail.close();
                      }
                    }));
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    dismiss.addClassName("cosmos-notification-preview-dismiss");
    dismiss.setTooltipText(texts.getNotificationsDismiss());
    dismiss.setAriaLabel(
        texts.getNotificationsDismiss() + ": " + UserNotificationTexts.title(texts, item));
    return dismiss;
  }

  private Div previewRow(Item item, Button title, Span time, Span state) {
    Span dot = new Span();
    dot.addClassName("cosmos-notification-preview-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!item.read());
    Div titleRow = new Div(dot, title);
    titleRow.addClassName("cosmos-notification-preview-title");
    titleRow.add(dismissButton(item));
    Span source = new Span(source(item));
    source.addClassName("cosmos-notification-preview-source");
    state.addClassName("cosmos-notification-preview-state");
    state.getElement().setAttribute("theme", "badge");
    Div meta = new Div(time, source, state);
    meta.addClassName("cosmos-notification-preview-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-preview-message");
    Button open = new Button(VaadinIcon.EYE.create(), e -> action(() -> open(item)));
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    open.addClassNames("cosmos-notification-row-action", "cosmos-notification-row-open");
    open.setTooltipText(texts.getNotificationsOpen());
    Button markRead =
        new Button(VaadinIcon.ENVELOPE_OPEN.create(), e -> action(() -> markRead(item)));
    markRead.addClassNames(
        "cosmos-notification-preview-mark-read", "cosmos-notification-row-action");
    markRead.setTooltipText(texts.getNotificationsMarkRead());
    markRead.setVisible(!item.read());
    markRead.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    Div actions = new Div(open, markRead);
    actions.addClassName("cosmos-notification-preview-actions");
    Div footer = new Div(meta, actions);
    footer.addClassName("cosmos-notification-preview-row-footer");
    Div row = new Div(titleRow, body, footer);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-preview-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (!item.read()) {
      row.addClassName("cosmos-notification-row--unread");
    }
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
      if (failure instanceof org.springframework.security.access.AccessDeniedException) {
        unavailable();
      }
      LoggerFactory.getLogger(getClass()).warn("Notification action failed", safeFailure(failure));
      NotificationHelper.getInstance()
          .showNotification(
              texts::getNotificationsUnavailable, NotificationHelper.NotificationType.ERROR);
    }
  }

  private void open(Item item) {
    service.ownerId(session);
    if (detail != null) {
      detail.close();
    }
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
    if (openedItem != null && openedItem.id().equals(item.id())) {
      updateOpenedRead();
    }
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
    if (detail != null && detail.isOpened()) {
      renderDetail();
    }
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
    String subject = UserNotificationTexts.title(texts, openedItem);
    String timestamp =
        UserNotificationTexts.time(openedItem.createdAt(), UI.getCurrent().getLocale());
    String stateText =
        openedItem.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread();
    H2 title = new H2(texts.getNotificationsDetailTitle());
    title.addClassName("cosmos-panel-title");
    Span description = new Span(texts.getNotificationsDetailDescription());
    description.addClassName("cosmos-panel-description");
    Div titles = new Div(title, description);
    titles.addClassName("cosmos-panel-heading");
    Icon icon = VaadinIcon.PAPERPLANE.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    Div badge = new Div(icon);
    badge.addClassName("cosmos-panel-icon");
    Button closeIcon = new Button(VaadinIcon.CLOSE_CIRCLE.create(), e -> detail.close());
    closeIcon.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeIcon.addClassName("cosmos-overlay-close");
    closeIcon.setAriaLabel(texts.getNotificationsClose());
    closeIcon.setTooltipText(texts.getNotificationsClose());
    Div header = new Div(badge, titles, closeIcon);
    header.addClassName("cosmos-notification-detail-header");
    detail.getHeader().add(header);
    detail.getElement().setProperty("ariaLabel", subject);

    H3 contentTitle = new H3(subject);
    contentTitle.addClassName("cosmos-notification-detail-title");
    Span dot = new Span();
    dot.addClassNames("cosmos-notification-preview-dot", "cosmos-notification-detail-dot");
    dot.getElement().setAttribute("aria-hidden", "true");
    dot.setVisible(!openedItem.read());
    Div titleRow = new Div(dot, contentTitle);
    titleRow.addClassName("cosmos-notification-detail-title-row");
    Span state = new Span(stateText);
    state.getElement().setAttribute("theme", "badge");
    state.addClassName("cosmos-notification-detail-state");
    Span source = new Span(source(openedItem));
    source.addClassName("cosmos-notification-detail-source");
    Div contentMeta = new Div(new Span(timestamp), source, state);
    contentMeta.addClassName("cosmos-notification-detail-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, openedItem));
    body.addClassNames("cosmos-notification-body", "cosmos-notification-detail-body");
    Div message = new Div(titleRow, contentMeta, body);
    message.addClassName("cosmos-notification-detail-message");
    if (!openedItem.read()) {
      message.addClassName("cosmos-notification-detail-message--unread");
    }
    detail.add(message);

    Button history =
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
    Button markRead =
        new Button(
            texts.getNotificationsMarkRead(),
            VaadinIcon.ENVELOPE_OPEN.create(),
            e -> action(() -> markRead(openedItem)));
    markRead.addClassName("cosmos-notification-detail-mark-read");
    markRead.setVisible(!openedItem.read());
    Button close =
        new Button(
            texts.getNotificationsClose(), VaadinIcon.CLOSE_CIRCLE.create(), e -> detail.close());
    close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div actions = new Div(history);
    actions.addClassNames("cosmos-dialog-actions", "cosmos-notification-detail-actions");
    if (openedItem.type() != UserNotificationType.ADMIN_ANNOUNCEMENT) {
      Button account =
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
