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
import com.vaadin.flow.component.icon.SvgIcon;
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
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String COSMOS_NOTIFICATION_INBOX_TOOL_CLASS =
      "cosmos-notification-inbox-tool";
  private static final String COSMOS_NOTIFICATION_PREVIEW_DOT_CLASS =
      "cosmos-notification-preview-dot";
  private static final String COSMOS_NOTIFICATION_ROW_ACTION_CLASS =
      "cosmos-notification-row-action";
  private static final String COSMOS_PANEL_ICON_CLASS = "cosmos-panel-icon";
  private static final String COSMOS_PANEL_DESCRIPTION_CLASS = "cosmos-panel-description";
  private static final String COSMOS_PANEL_HEADING_CLASS = "cosmos-panel-heading";
  private static final String BADGE_THEME = "badge";
  private static final String THEME_ATTRIBUTE = "theme";
  private static final String COSMOS_NOTIFICATION_BODY_CLASS = "cosmos-notification-body";

  private static final int PREVIEW_LIMIT = 5;
  private final transient MessageProperties texts;
  private final transient UserNotificationService service;
  private final transient HttpSession session;
  private final boolean compact;
  private final Div rows = new Div();
  private final H2 heading = new H2();
  private final Span description = new Span();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create());
  private transient Runnable closeAction = () -> {};
  private transient Consumer<Boolean> detailOpenListener = open -> {};
  private final RadioButtonGroup<ReadFilter> readFilter = new RadioButtonGroup<>();
  private boolean updatingFilterLabels;
  private final Button markAll = new Button();
  private final Button dismissRead = new Button(AppIcon.TRASH.create());
  private final Button previous = new Button();
  private final Button next = new Button();
  private final Button refresh = new Button();
  private final Button history = new Button();
  private final Span pageLabel = new Span();
  private final ComboBox<Integer> pageSize = new ComboBox<>();
  private final Span pageTotal = new Span();
  private long totalItems;
  private final Button first = new Button(AppIcon.ANGLE_DOUBLE_LEFT.create());
  private final Button last = new Button(AppIcon.ANGLE_DOUBLE_RIGHT.create());
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
  private final PortalConfirmationDialog confirmation;

  public UserNotificationsPanel(
      MessageProperties texts,
      UserNotificationService service,
      HttpSession session,
      boolean compact) {
    this.texts = texts;
    this.service = service;
    this.session = session;
    this.compact = compact;
    confirmation =
        new PortalConfirmationDialog(
            texts,
            compact ? "notifications-preview-confirmation" : "notifications-inbox-confirmation");
    confirmation.addClassName("portal-notification-confirmation");
    confirmation.addOpenedChangeListener(event -> notifyOverlayState());
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
    add(confirmation);
    refreshTexts();
    addDetachListener(
        e -> {
          confirmation.close();
          if (detail != null) {
            detail.close();
          }
        });
  }

  private void configureReadFilter() {
    readFilter.setId(compact ? "notifications-preview-filter" : "notifications-inbox-filter");
    readFilter.setItems(List.of(ReadFilter.ALL, ReadFilter.UNREAD, ReadFilter.READ));
    readFilter.setItemLabelGenerator(this::filterLabel);
    readFilter.setRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(
            filter -> {
              Span label = new Span(filterLabel(filter));
              Span count = new Span();
              count.addClassName("portal-filter-count");
              count.getElement().setAttribute(ARIA_HIDDEN, "true");
              long total = totalItems;
              long unread = unreadIds.size();
              count.setText(
                  Long.toString(
                      switch (filter) {
                        case ALL -> total;
                        case UNREAD -> unread;
                        case READ -> Math.max(0, total - unread);
                      }));
              Div option = new Div(label, count);
              option.addClassName("portal-read-filter-option");
              return option;
            }));
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
    SvgIcon readAllIcon = AppIcon.CHECK_CHECK.create();
    readAllIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    markAll.setIcon(readAllIcon);
    dismissRead.addClassName("cosmos-delete-button");
    dismissRead.setEnabled(false);
    dismissRead.addClickListener(e -> confirmDismissRead());
    refresh.addClickListener(e -> refresh(true));
    markAll.addClickListener(e -> confirmMarkAll());
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
    SvgIcon icon = AppIcon.BELL.create();
    icon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div badge = new Div(icon);
    badge.addClassName(COSMOS_PANEL_ICON_CLASS);
    description.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(heading, description);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(e -> closeAction.run());
    unreadCount.addClassName("portal-preview-unread-count");
    Div header = new Div(badge, titles, unreadCount, close);
    header.addClassName("cosmos-notification-preview-header");
    toolbar.addClassName("cosmos-notification-preview-toolbar");
    refresh.setIcon(AppIcon.REFRESH.create());
    refresh.addClassNames(
        "cosmos-notification-preview-refresh", "cosmos-notification-preview-tool");
    markAll.addClassNames(
        "cosmos-notification-preview-mark-all", "cosmos-notification-preview-tool");
    Div tools = new Div(refresh, markAll);
    tools.addClassName("cosmos-notification-preview-tools");
    toolbar.removeAll();
    toolbar.add(readFilter, tools);
    rows.addClassName("cosmos-notification-preview-rows");
    previewHint.addClassName("cosmos-notification-preview-hint");
    previewHint.setVisible(false);
    Div content = new Div(toolbar, previewHint, rows);
    content.addClassName("cosmos-notification-preview-content");
    history.setIcon(AppIcon.EYE.create());
    history.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div note = new Div(AppIcon.INFO_CIRCLE.create(), retention);
    note.addClassName("cosmos-notification-preview-retention");
    Div footer = new Div(history, note);
    footer.addClassName("cosmos-notification-preview-footer");
    add(header, content, footer);
  }

  private void buildInbox(Div toolbar, Div paging) {
    SvgIcon archive = AppIcon.BELL.create();
    archive.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div icon = new Div(archive);
    icon.addClassName(COSMOS_PANEL_ICON_CLASS);
    description.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(heading, description);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div unreadStat = stat(unreadLabel, unreadCount);
    unreadStat.addClassName("cosmos-notification-inbox-stat-unread");
    Div counts = new Div(stat(totalLabel, totalCount), unreadStat);
    counts.addClassName("cosmos-notification-inbox-counts");
    totalCount.setId("notifications-inbox-total");
    unreadCount.setId("notifications-inbox-unread-total");
    Div headingGroup = new Div(icon, titles);
    headingGroup.addClassName("cosmos-notification-inbox-heading");
    Div header = new Div(headingGroup, counts);
    header.addClassNames("cosmos-panel-header", "cosmos-notification-inbox-header");
    refresh.setIcon(AppIcon.REFRESH.create());
    search.setId("notifications-inbox-search");
    search.setPrefixComponent(AppIcon.SEARCH.create());
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
    refresh.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    markAll.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    dismissRead.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    toolbar.removeAll();
    Div controls = new Div(readFilter, tools);
    controls.addClassName("cosmos-notification-inbox-controls");
    toolbar.add(controls, search);
    toolbar.addClassName("cosmos-notification-inbox-toolbar");
    rows.addClassName("cosmos-notification-inbox-rows");
    previous.setIcon(AppIcon.ANGLE_LEFT.create());
    next.setIcon(AppIcon.ANGLE_RIGHT.create());
    pageNumbers.addClassName("cosmos-notification-inbox-page-numbers");
    paging.addClassName("cosmos-notification-inbox-pagination");
    pageSize.setId("notifications-inbox-page-size");
    pageSize.addClassName("portal-page-size");
    PortalFields.styleChoice(pageSize, Object::toString, false);
    pageSize.setItems(10, 20, 50);
    pageSize.setValue(10);
    pageSize.setAllowCustomValue(false);
    pageSize.setWidth("64px");
    pageSize.addValueChangeListener(
        e -> {
          if (e.getValue() != null) {
            page = 0;
            refresh();
          }
        });
    Div summary = new Div(pageLabel, pageSize);
    pageTotal.addClassName("cosmos-notification-inbox-range");
    summary.addClassName("cosmos-notification-inbox-page-summary");
    Div paginationRow = new Div(summary, pageTotal, paging);
    paginationRow.addClassName("cosmos-notification-inbox-pagination-row");
    Div retentionNote = new Div(AppIcon.INFO_CIRCLE.create(), retention);
    retentionNote.addClassName("cosmos-notification-inbox-retention");
    Div footer = new Div(paginationRow, retentionNote);
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
    confirmation.refreshTexts();
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
    if (compact) {
      description.setText(previewHint.getText());
    }
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
    refresh(false);
  }

  private void refresh(boolean notifyResult) {
    try {
      refreshRetention();
      refreshPreviewHint();
      Page<Item> result = loadPage();
      if (page > 0 && page >= result.getTotalPages()) {
        page = Math.max(0, result.getTotalPages() - 1);
        refresh(notifyResult);
        return;
      }
      unreadIds = service.unreadSnapshot(session);
      totalItems = service.inboxCount(session);
      totalCount.setText(Long.toString(totalItems));
      unreadCount.setText(Integer.toString(unreadIds.size()));
      updatingFilterLabels = true;
      try {
        ReadFilter selected = selectedFilter();
        readFilter.getDataProvider().refreshAll();
        readFilter.setValue(selected);
      } finally {
        updatingFilterLabels = false;
      }
      refreshOpenedDetail(result);
      markAll.setEnabled(!unreadIds.isEmpty());
      dismissRead.setEnabled(!compact && service.readCount(session) > 0);
      renderRows(result);
      totalPages = Math.max(1, result.getTotalPages());
      first.setEnabled(page > 0);
      previous.setEnabled(page > 0);
      next.setEnabled(result.hasNext());
      last.setEnabled(result.hasNext());
      if (!compact) {
        renderPageNumbers();
      }
      long firstEntry = result.isEmpty() ? 0 : (long) page * selectedPageSize() + 1;
      long lastEntry =
          result.isEmpty() ? 0 : (long) page * selectedPageSize() + result.getNumberOfElements();
      pageTotal.setText(
          texts.getPortalPaginationRange(firstEntry, lastEntry, result.getTotalElements()));
      if (notifyResult) {
        NotificationHelper.getInstance()
            .showNotification(
                texts::getNotificationsRefreshed,
                null,
                NotificationHelper.NotificationType.SUCCESS);
      }
    } catch (RuntimeException _) {
      unavailable();
      if (notifyResult) {
        NotificationHelper.getInstance()
            .showNotification(
                texts::getNotificationsUnavailable, NotificationHelper.NotificationType.ERROR);
      }
    }
  }

  private void refreshOpenedDetail(Page<Item> result) {
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
  }

  private void renderRows(Page<Item> result) {
    rows.removeAll();
    if (result.isEmpty()) {
      Span empty =
          new Span(
              !compact && !search.isEmpty()
                  ? texts.getNotificationsSearchEmpty()
                  : texts.getNotificationsEmpty());
      empty.addClassName("cosmos-notification-empty-title");
      empty.getElement().setAttribute("role", "heading");
      empty.getElement().setAttribute("aria-level", "3");
      SvgIcon icon = AppIcon.BELL.create();
      Div notice = new Div(icon, empty);
      notice.addClassName("cosmos-notification-empty");
      rows.add(notice);
    }
    for (Item item : result) {
      rows.add(row(item));
    }
  }

  public void unavailable() {
    confirmation.close();
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
    int from = totalPages <= 7 ? 0 : Math.clamp(page - 1L, 1, totalPages - 4);
    int to = totalPages <= 7 ? totalPages - 1 : Math.clamp(page + 1L, 3, totalPages - 2);
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
    if (compact) {
      return previewRow(item, title);
    }
    Div meta = messageMetadata(item);
    meta.addClassName("cosmos-notification-inbox-meta");
    Button open = new Button(AppIcon.EYE.create(), e -> action(() -> open(item)));
    open.addClassNames(COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    open.setTooltipText(texts.getNotificationsOpen());
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    Button mark = new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    mark.setVisible(!item.read());
    mark.addClassNames("cosmos-notification-inbox-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    mark.setTooltipText(texts.getNotificationsMarkRead());
    mark.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    Div actions = new Div(open, mark, dismissButton(item));
    actions.addClassName("cosmos-notification-inbox-actions");
    Div top = new Div(meta, actions);
    top.addClassName("cosmos-notification-inbox-row-header");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-inbox-message");
    Div content = new Div(title, body);
    content.addClassName("cosmos-notification-inbox-content");
    Div row = new Div(top, content);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-inbox-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    row.setClassName("cosmos-notification-row--unread", !item.read());
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    row.getElement()
        .addEventListener("click", e -> action(() -> open(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return row;
  }

  private Button dismissButton(Item item) {
    Button dismiss = new Button(AppIcon.TRASH.create(), e -> confirmDismiss(item));
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    dismiss.addClassNames("cosmos-notification-preview-dismiss", "cosmos-delete-button");
    dismiss.setTooltipText(texts.getNotificationsDismiss());
    dismiss.setAriaLabel(
        texts.getNotificationsDismiss() + ": " + UserNotificationTexts.title(texts, item));
    return dismiss;
  }

  private Div previewRow(Item item, Button title) {
    Div meta = messageMetadata(item);
    meta.addClassName("cosmos-notification-preview-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, item));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-preview-message");
    Button open = new Button(AppIcon.EYE.create(), e -> action(() -> open(item)));
    open.setAriaLabel(
        texts.getNotificationsOpen() + ": " + UserNotificationTexts.title(texts, item));
    open.addClassNames(COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    open.setTooltipText(texts.getNotificationsOpen());
    Button markRead = new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    markRead.addClassNames(
        "cosmos-notification-preview-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    markRead.setTooltipText(texts.getNotificationsMarkRead());
    markRead.setVisible(!item.read());
    markRead.setAriaLabel(
        texts.getNotificationsMarkRead() + ": " + UserNotificationTexts.title(texts, item));
    Div actions = new Div(open, markRead, dismissButton(item));
    actions.addClassName("cosmos-notification-preview-actions");
    Div row = new Div(title, body, meta, actions);
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
    detail.addOpenedChangeListener(e -> notifyOverlayState());
    detail.addClassNames("cosmos-theme", "cosmos-reference-overlay", "cosmos-notification-dialog");
    detail.setModality(com.vaadin.flow.component.ModalityMode.STRICT);
    detail.getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    detail.setCloseOnOutsideClick(true);
    detail.setCloseOnEsc(true);
    renderDetail();
    detail.open();
  }

  private void notifyOverlayState() {
    detailOpenListener.accept(confirmation.isOpened() || detail != null && detail.isOpened());
  }

  private PortalContextCard context(Item item) {
    PortalContextCard card =
        new PortalContextCard(AppIcon.BELL.create(), UserNotificationTexts.title(texts, item));
    card.addMetadata(messageMetadata(item));
    return card;
  }

  private void confirmMarkRead(Item item) {
    confirmation.open(
        new PortalConfirmationDialog.Prompt(
            texts::getNotificationsMarkRead,
            texts::getPortalNotificationSelectionHint,
            texts::getPortalMarkReadQuestion,
            texts::getPortalMarkReadExplanation,
            () -> context(item),
            texts::getPortalNotificationSyncHint,
            texts::getPortalConfirmAction,
            false),
        () -> action(() -> markRead(item)));
  }

  private void confirmMarkAll() {
    List<Long> selected = List.copyOf(unreadIds);
    if (selected.isEmpty()) {
      return;
    }
    confirmation.open(
        new PortalConfirmationDialog.Prompt(
            texts::getNotificationsMarkAll,
            texts::getPortalNotificationSelectionHint,
            texts::getPortalMarkAllQuestion,
            texts::getPortalMarkAllExplanation,
            () ->
                new PortalContextCard(
                    AppIcon.BELL.create(), texts.getPortalNotificationAffected(selected.size())),
            texts::getPortalNotificationSyncHint,
            texts::getPortalConfirmAction,
            false,
            AppIcon.CHECK_CHECK::create),
        () ->
            action(
                () -> {
                  boolean includesOpened = openedItem != null && selected.contains(openedItem.id());
                  service.markRead(session, selected);
                  if (includesOpened) {
                    updateOpenedRead();
                  }
                }));
  }

  private void confirmDismiss(Item item) {
    confirmation.open(
        new PortalConfirmationDialog.Prompt(
            texts::getNotificationsDismiss,
            texts::getPortalNotificationSelectionHint,
            texts::getPortalDeleteNotificationQuestion,
            texts::getPortalDeleteNotificationExplanation,
            () -> context(item),
            texts::getPortalDeleteNotificationHint,
            texts::getPortalDeleteAction,
            true),
        () ->
            action(
                () -> {
                  service.dismiss(session, item.id());
                  if (detail != null && openedItem != null && openedItem.id().equals(item.id())) {
                    detail.close();
                  }
                }));
  }

  private void confirmDismissRead() {
    action(
        () ->
            confirmation.open(
                new PortalConfirmationDialog.Prompt(
                    texts::getNotificationsDismissRead,
                    texts::getPortalNotificationSelectionHint,
                    texts::getPortalDeleteReadQuestion,
                    texts::getPortalDeleteReadExplanation,
                    () ->
                        new PortalContextCard(
                            AppIcon.BELL.create(),
                            texts.getPortalNotificationAffected(service.readCount(session))),
                    texts::getPortalDeleteNotificationHint,
                    texts::getPortalDeleteAction,
                    true),
                () ->
                    action(
                        () -> {
                          service.dismissRead(session);
                          if (detail != null && openedItem != null && openedItem.read()) {
                            detail.close();
                          }
                        })));
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

  private Div messageMetadata(Item item, String... stateClasses) {
    boolean administration = item.type() == UserNotificationType.ADMIN_ANNOUNCEMENT;
    Span origin = new Span(source(item));
    origin.addClassNames(
        "cosmos-notification-preview-source",
        administration ? "portal-notification-source-admin" : "portal-notification-source-system");
    Span time = new Span(UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale()));
    time.addClassName("cosmos-notification-message-time");
    Span state =
        new Span(item.read() ? texts.getNotificationsRead() : texts.getNotificationsUnread());
    state.getElement().setAttribute(THEME_ATTRIBUTE, BADGE_THEME);
    state.getElement().setAttribute("data-read", Boolean.toString(item.read()));
    state.addClassName("cosmos-notification-preview-state");
    state.addClassNames(stateClasses);
    Div metadata = new Div(origin, time, state);
    metadata.addClassName("cosmos-notification-message-meta");
    return metadata;
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
    H2 title = new H2(texts.getNotificationsDetailTitle());
    title.addClassName("cosmos-panel-title");
    Span detailDescription = new Span(texts.getNotificationsDetailDescription());
    detailDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(title, detailDescription);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    SvgIcon icon = AppIcon.BELL.create();
    icon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div badge = new Div(icon);
    badge.addClassName(COSMOS_PANEL_ICON_CLASS);
    Button closeIcon = new Button(AppIcon.CLOSE_CIRCLE.create(), e -> detail.close());
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
    Div titleRow = new Div(contentTitle);
    titleRow.addClassName("cosmos-notification-detail-title-row");
    Div contentMeta = messageMetadata(openedItem, "cosmos-notification-detail-state");
    contentMeta.addClassName("cosmos-notification-detail-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.body(texts, openedItem));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-detail-body");
    Div message = new Div(titleRow, contentMeta, body);
    message.addClassName("cosmos-notification-detail-message");
    if (!openedItem.read()) {
      message.addClassName("cosmos-notification-detail-message--unread");
    }
    Div detailContent = new Div(message);
    detailContent.addClassName("cosmos-notification-detail-content");
    detail.add(detailContent);

    Button detailHistory =
        new Button(
            AppIcon.BELL.create(),
            e -> {
              detail.close();
              closeAction.run();
              UI.getCurrent().navigate(UserNotificationsView.class);
            });
    detailHistory.addThemeVariants(ButtonVariant.LUMO_ICON);
    detailHistory.setAriaLabel(texts.getNotificationsHistory());
    detailHistory.setTooltipText(texts.getNotificationsHistory());
    detailHistory.addClassName("cosmos-notification-detail-history");
    detailHistory.setVisible(compact);
    Button markRead =
        new Button(
            texts.getNotificationsMarkRead(),
            AppIcon.CHECK.create(),
            e -> confirmMarkRead(openedItem));
    markRead.addClassName("cosmos-notification-detail-mark-read");
    markRead.setVisible(!openedItem.read());
    Button detailClose =
        new Button(texts.getNotificationsClose(), AppIcon.CLOSE.create(), e -> detail.close());
    detailClose.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div tools = new Div(detailHistory);
    tools.addClassName("cosmos-notification-detail-tools");
    if (openedItem.type() != UserNotificationType.ADMIN_ANNOUNCEMENT) {
      Button account =
          new Button(
              AppIcon.LOCK.create(),
              e -> {
                detail.close();
                closeAction.run();
                UI.getCurrent()
                    .navigate(AccountView.class, QueryParameters.simple(Map.of("tab", "security")));
              });
      account.addThemeVariants(ButtonVariant.LUMO_ICON);
      account.addClassName("cosmos-notification-detail-account");
      account.setAriaLabel(texts.getNotificationsOpenAccount());
      account.setTooltipText(texts.getNotificationsOpenAccount());
      tools.add(account);
    }
    Button delete = new Button(AppIcon.TRASH.create(), event -> confirmDismiss(openedItem));
    delete.addThemeVariants(ButtonVariant.LUMO_ICON);
    delete.addClassNames("cosmos-notification-detail-delete", "cosmos-delete-button");
    delete.setAriaLabel(texts.getNotificationsDismiss());
    delete.setTooltipText(texts.getNotificationsDismiss());
    tools.add(delete);
    Div primary = new Div(markRead, detailClose);
    primary.addClassName("cosmos-notification-detail-primary");
    Div actions = new Div(tools, primary);
    actions.addClassNames("cosmos-dialog-actions", "cosmos-notification-detail-actions");
    detail.getFooter().add(actions);
  }
}
