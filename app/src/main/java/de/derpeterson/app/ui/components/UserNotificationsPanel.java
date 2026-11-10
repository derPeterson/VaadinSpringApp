package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.QueryParameters;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.UserNotificationTexts;
import de.derpeterson.app.model.enums.UserNotificationRelevance;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Item;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.AdminView;
import de.derpeterson.app.views.UserNotificationsView;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

/** Gemeinsame kompakte Vorschau und paginierter eigener Posteingang. */
public class UserNotificationsPanel extends Div {
  private static final String COSMOS_THEME = "cosmos-theme";
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String COSMOS_NOTIFICATION_INBOX_TOOL_CLASS =
      "cosmos-notification-inbox-tool";
  private static final String COSMOS_NOTIFICATION_ROW_ACTION_CLASS =
      "cosmos-notification-row-action";
  private static final String COSMOS_PANEL_DESCRIPTION_CLASS = PANEL_DESCRIPTION;
  private static final String COSMOS_PANEL_HEADING_CLASS = PANEL_HEADING;
  private static final String BADGE_THEME = "badge";
  private static final String THEME_ATTRIBUTE = "theme";
  private static final String COSMOS_NOTIFICATION_BODY_CLASS = "cosmos-notification-body";

  private static final int PREVIEW_LIMIT = 5;
  private final transient MessageProperties messageProperties;
  private final transient UserNotificationService userNotificationService;
  private final transient HttpSession httpSession;
  private final boolean compact;
  private final Div notificationRows = new Div();
  private final H2 inboxHeading = new H2();
  private final Span inboxDescription = new Span();
  private final Button closeInboxButton = new Button(AppIcon.CLOSE_CIRCLE.create());
  private transient Runnable closeAction = () -> {};
  private transient Consumer<Boolean> detailOpenListener = open -> {};
  private final RadioButtonGroup<ReadFilter> readFilter = new RadioButtonGroup<>();
  private boolean updatingFilterLabels;
  private final Button markAllReadButton = new Button();
  private final Button dismissReadButton = new Button(AppIcon.TRASH.create());
  private final Button previousPageButton = new Button();
  private final Button nextPageButton = new Button();
  private final Button refreshInboxButton = new Button();
  private final Button openHistoryButton = new Button();
  private final Span pageLabel = new Span();
  private final ComboBox<Integer> pageSize = new ComboBox<>();
  private final Span pageTotal = new Span();
  private long totalItems;
  private final Button firstPageButton = new Button(AppIcon.ANGLE_DOUBLE_LEFT.create());
  private final Button lastPageButton = new Button(AppIcon.ANGLE_DOUBLE_RIGHT.create());
  private final Div pageNumbers = new Div();
  private final TextField notificationSearchField = new TextField();
  private final Span totalLabel = new Span();
  private final Span totalCount = new Span("0");
  private final Span unreadCount = new Span("0");
  private final Span unreadLabel = new Span();
  private int totalPages = 1;
  private final Span retention = new Span();
  private final Span previewHint = new Span();
  private List<Long> unreadIds = List.of();
  private int page;
  private Dialog notificationDetailDialog;
  private Popover detailActionsMenu;
  private Item openedNotification;
  private final PortalConfirmationDialog notificationActionConfirmation;

  public UserNotificationsPanel(
      MessageProperties messageProperties,
      UserNotificationService userNotificationService,
      HttpSession httpSession,
      boolean compact) {
    this.messageProperties = messageProperties;
    this.userNotificationService = userNotificationService;
    this.httpSession = httpSession;
    this.compact = compact;
    notificationActionConfirmation =
        new PortalConfirmationDialog(
            messageProperties,
            compact ? "notifications-preview-confirmation" : "notifications-inbox-confirmation");
    notificationActionConfirmation.addClassName("portal-notification-confirmation");
    notificationActionConfirmation.addOpenedChangeListener(event -> notifyOverlayState());
    addClassNames(COSMOS_THEME, "cosmos-notifications-panel");
    setId(compact ? "notifications-preview" : "notifications-inbox");
    inboxHeading.addClassName(PANEL_TITLE);
    configureReadFilter();
    Div toolbar = new Div(readFilter, refreshInboxButton);
    toolbar.addClassName("cosmos-notifications-actions");
    configureCommonActions();
    Div paging =
        new Div(firstPageButton, previousPageButton, pageNumbers, nextPageButton, lastPageButton);
    paging.addClassName("cosmos-notifications-actions");
    paging.setVisible(!compact);
    openHistoryButton.setVisible(compact);
    retention.addClassName("cosmos-notifications-secondary");
    if (compact) {
      buildPreview(toolbar);
    } else {
      buildInbox(toolbar, paging);
    }
    add(notificationActionConfirmation);
    refreshTexts();
    addDetachListener(e -> {
      notificationActionConfirmation.close();
      if (notificationDetailDialog != null) {
        notificationDetailDialog.close();
      }
    });
  }

  private void configureReadFilter() {
    readFilter.setId(compact ? "notifications-preview-filter" : "notifications-inbox-filter");
    readFilter.setItems(List.of(ReadFilter.ALL, ReadFilter.UNREAD, ReadFilter.READ));
    readFilter.setItemLabelGenerator(this::filterLabel);
    readFilter.setRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(filter -> {
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
    readFilter.addValueChangeListener(e -> {
      if (!updatingFilterLabels) {
        page = 0;
        refreshInbox();
      }
    });
  }

  private void configureCommonActions() {
    SvgIcon readAllIcon = AppIcon.CHECK_CHECK.create();
    readAllIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    markAllReadButton.setIcon(readAllIcon);
    dismissReadButton.addClassName(DELETE_BUTTON);
    dismissReadButton.setEnabled(false);
    dismissReadButton.addClickListener(e -> confirmDismissRead());
    refreshInboxButton.addClickListener(e -> refreshInbox(true));
    markAllReadButton.addClickListener(e -> confirmMarkAll());
    previousPageButton.addClickListener(e -> {
      if (page > 0) {
        page--;
      }
      refreshInbox();
    });
    nextPageButton.addClickListener(e -> {
      page++;
      refreshInbox();
    });
    openHistoryButton.addClickListener(e -> {
      closeAction.run();
      UI.getCurrent().navigate(UserNotificationsView.class);
    });
    firstPageButton.addClickListener(e -> {
      page = 0;
      refreshInbox();
    });
    lastPageButton.addClickListener(e -> {
      page = Math.max(0, totalPages - 1);
      refreshInbox();
    });
  }

  private void buildPreview(Div toolbar) {
    SvgIcon icon = AppIcon.BELL.create();
    icon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div badge = new Div(icon);
    badge.addClassName(PANEL_ICON);
    inboxDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(inboxHeading, inboxDescription);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    closeInboxButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeInboxButton.addClassName("cosmos-overlay-close");
    closeInboxButton.addClickListener(e -> closeAction.run());
    unreadCount.addClassName("portal-preview-unread-count");
    Div header = new Div(badge, titles, unreadCount, closeInboxButton);
    header.addClassName("cosmos-notification-preview-header");
    toolbar.addClassName("cosmos-notification-preview-toolbar");
    refreshInboxButton.setIcon(AppIcon.REFRESH.create());
    refreshInboxButton.addClassNames(
        "cosmos-notification-preview-refresh", "cosmos-notification-preview-tool");
    markAllReadButton.addClassNames(
        "cosmos-notification-preview-mark-all", "cosmos-notification-preview-tool");
    Div tools = new Div(refreshInboxButton, markAllReadButton);
    tools.addClassName("cosmos-notification-preview-tools");
    toolbar.removeAll();
    toolbar.add(readFilter, tools);
    notificationRows.addClassName("cosmos-notification-preview-rows");
    previewHint.addClassName("cosmos-notification-preview-hint");
    previewHint.setVisible(false);
    Div content = new Div(toolbar, previewHint, notificationRows);
    content.addClassName("cosmos-notification-preview-content");
    openHistoryButton.setIcon(AppIcon.EYE.create());
    openHistoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div note = new Div(AppIcon.INFO_CIRCLE.create(), retention);
    note.addClassName("cosmos-notification-preview-retention");
    Div footer = new Div(openHistoryButton, note);
    footer.addClassName("cosmos-notification-preview-footer");
    add(header, content, footer);
  }

  private void buildInbox(Div toolbar, Div paging) {
    SvgIcon archive = AppIcon.BELL.create();
    archive.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div icon = new Div(archive);
    icon.addClassName(PANEL_ICON);
    inboxDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(inboxHeading, inboxDescription);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div unreadStat = buildNotificationStatRow(unreadLabel, unreadCount);
    unreadStat.addClassName("cosmos-notification-inbox-stat-unread");
    Div counts = new Div(buildNotificationStatRow(totalLabel, totalCount), unreadStat);
    counts.addClassName("cosmos-notification-inbox-counts");
    totalCount.setId("notifications-inbox-total");
    unreadCount.setId("notifications-inbox-unread-total");
    Div headingGroup = new Div(icon, titles);
    headingGroup.addClassName("cosmos-notification-inbox-heading");
    Div header = new Div(headingGroup, counts);
    header.addClassNames(PANEL_HEADER, "cosmos-notification-inbox-header");
    refreshInboxButton.setIcon(AppIcon.REFRESH.create());
    notificationSearchField.setId("notifications-inbox-search");
    notificationSearchField.setPrefixComponent(AppIcon.SEARCH.create());
    notificationSearchField.setClearButtonVisible(true);
    notificationSearchField.setMaxLength(UserNotificationService.TITLE_LIMIT);
    notificationSearchField.setValueChangeMode(ValueChangeMode.EAGER);
    notificationSearchField.addValueChangeListener(e -> {
      page = 0;
      refreshInbox();
    });
    Div tools = new Div(refreshInboxButton, markAllReadButton, dismissReadButton);
    tools.addClassName("cosmos-notification-inbox-tools");
    refreshInboxButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    markAllReadButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    dismissReadButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    toolbar.removeAll();
    Div controls = new Div(readFilter, tools);
    controls.addClassName("cosmos-notification-inbox-controls");
    toolbar.add(controls, notificationSearchField);
    toolbar.addClassName("cosmos-notification-inbox-toolbar");
    notificationRows.addClassName("cosmos-notification-inbox-rows");
    previousPageButton.setIcon(AppIcon.ANGLE_LEFT.create());
    nextPageButton.setIcon(AppIcon.ANGLE_RIGHT.create());
    pageNumbers.addClassName("cosmos-notification-inbox-page-numbers");
    paging.addClassName("cosmos-notification-inbox-pagination");
    pageSize.setId("notifications-inbox-page-size");
    pageSize.addClassName("portal-page-size");
    PortalFields.styleChoice(pageSize, Object::toString, false);
    pageSize.setItems(10, 20, 50);
    pageSize.setValue(10);
    pageSize.setAllowCustomValue(false);
    pageSize.setWidth("64px");
    pageSize.addValueChangeListener(e -> {
      if (e.getValue() != null) {
        page = 0;
        refreshInbox();
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
    add(header, toolbar, notificationRows, footer);
  }

  private Div buildNotificationStatRow(Span label, Span value) {
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
    notificationActionConfirmation.refreshTexts();
    inboxHeading.setText(messageProperties.getNotificationsTitle());
    inboxDescription.setText(
        compact
            ? messageProperties.getNotificationsDescription()
            : messageProperties.getNotificationsInboxDescription());
    totalLabel.setText(messageProperties.getNotificationsTotal());
    unreadLabel.setText(messageProperties.getNotificationsUnread());
    notificationSearchField.setPlaceholder(messageProperties.getNotificationsSearch());
    notificationSearchField.setAriaLabel(messageProperties.getNotificationsSearch());
    closeInboxButton.setAriaLabel(messageProperties.getNotificationsClose());
    readFilter.setAriaLabel(messageProperties.getNotificationsFilter());
    updatingFilterLabels = true;
    try {
      ReadFilter selected = selectedFilter();
      readFilter.getDataProvider().refreshAll();
      readFilter.setValue(selected);
    } finally {
      updatingFilterLabels = false;
    }
    refreshInboxButton.setAriaLabel(messageProperties.getNotificationsRefresh());
    refreshInboxButton.setTooltipText(messageProperties.getNotificationsRefresh());
    markAllReadButton.setAriaLabel(messageProperties.getNotificationsMarkAll());
    markAllReadButton.setTooltipText(messageProperties.getNotificationsMarkAll());
    dismissReadButton.setTooltipText(messageProperties.getNotificationsDismissRead());
    dismissReadButton.setAriaLabel(messageProperties.getNotificationsDismissRead());
    pageLabel.setText(messageProperties.getAdminUsersPaginationPerPage());
    pageSize.setAriaLabel(messageProperties.getAdminUsersPaginationPerPage());
    previousPageButton.setAriaLabel(messageProperties.getNotificationsPrevious());
    previousPageButton.setTooltipText(messageProperties.getNotificationsPrevious());
    nextPageButton.setAriaLabel(messageProperties.getNotificationsNext());
    nextPageButton.setTooltipText(messageProperties.getNotificationsNext());
    firstPageButton.setAriaLabel(messageProperties.getAdminUsersPaginationFirstPage());
    firstPageButton.setTooltipText(messageProperties.getAdminUsersPaginationFirstPage());
    lastPageButton.setAriaLabel(messageProperties.getAdminUsersPaginationLastPage());
    lastPageButton.setTooltipText(messageProperties.getAdminUsersPaginationLastPage());
    openHistoryButton.setText(messageProperties.getNotificationsHistory());
    refreshRetention();
    refreshPreviewHint();
    if (isAttached()) {
      refreshInbox();
    }
    if (notificationDetailDialog != null
        && notificationDetailDialog.isOpened()
        && openedNotification != null) {
      renderOpenedNotification();
    }
  }

  private void refreshRetention() {
    OptionalInt days = userNotificationService.retentionDays();
    retention.setText(
        days.isPresent()
            ? messageProperties.getNotificationsRetention(days.getAsInt())
            : messageProperties.getNotificationsRetentionUnlimited());
  }

  private ReadFilter selectedFilter() {
    return Objects.requireNonNullElse(readFilter.getValue(), ReadFilter.ALL);
  }

  private String filterLabel(ReadFilter filter) {
    return switch (filter) {
      case ALL -> messageProperties.getNotificationsAll();
      case READ -> messageProperties.getNotificationsRead();
      case UNREAD -> messageProperties.getNotificationsUnread();
    };
  }

  private void refreshPreviewHint() {
    previewHint.setText(
        switch (selectedFilter()) {
          case ALL -> messageProperties.getNotificationsLatest(PREVIEW_LIMIT);
          case READ -> messageProperties.getNotificationsLatestRead(PREVIEW_LIMIT);
          case UNREAD -> messageProperties.getNotificationsLatestUnread(PREVIEW_LIMIT);
        });
    if (compact) {
      inboxDescription.setText(previewHint.getText());
    }
  }

  private Page<Item> loadPage() {
    if (compact) {
      return userNotificationService.inbox(httpSession, selectedFilter(), page, PREVIEW_LIMIT);
    }
    if (notificationSearchField.isEmpty()) {
      return userNotificationService.inbox(httpSession, selectedFilter(), page, selectedPageSize());
    }
    return userNotificationService.searchInbox(
        httpSession,
        selectedFilter(),
        notificationSearchField.getValue(),
        UserNotificationTexts.findMatchingNotificationTypes(
            messageProperties, notificationSearchField.getValue()),
        page,
        selectedPageSize());
  }

  public void refreshInbox() {
    refreshInbox(false);
  }

  private void refreshInbox(boolean notifyResult) {
    try {
      refreshRetention();
      refreshPreviewHint();
      Page<Item> result = loadPage();
      if (page > 0 && page >= result.getTotalPages()) {
        page = Math.max(0, result.getTotalPages() - 1);
        refreshInbox(notifyResult);
        return;
      }
      unreadIds = userNotificationService.unreadSnapshot(httpSession);
      totalItems = userNotificationService.inboxCount(httpSession);
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
      markAllReadButton.setEnabled(!unreadIds.isEmpty());
      dismissReadButton.setEnabled(!compact && userNotificationService.readCount(httpSession) > 0);
      renderRows(result);
      totalPages = Math.max(1, result.getTotalPages());
      firstPageButton.setEnabled(page > 0);
      previousPageButton.setEnabled(page > 0);
      nextPageButton.setEnabled(result.hasNext());
      lastPageButton.setEnabled(result.hasNext());
      if (!compact) {
        renderPageNumbers();
      }
      long firstEntry = result.isEmpty() ? 0 : (long) page * selectedPageSize() + 1;
      long lastEntry =
          result.isEmpty() ? 0 : (long) page * selectedPageSize() + result.getNumberOfElements();
      pageTotal.setText(
          messageProperties.getPortalPaginationRange(
              firstEntry, lastEntry, result.getTotalElements()));
      if (notifyResult) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getNotificationsRefreshed,
                null,
                NotificationHelper.NotificationType.SUCCESS);
      }
    } catch (RuntimeException _) {
      unavailable();
      if (notifyResult) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getNotificationsUnavailable,
                NotificationHelper.NotificationType.ERROR);
      }
    }
  }

  private void refreshOpenedDetail(Page<Item> result) {
    if (notificationDetailDialog != null
        && notificationDetailDialog.isOpened()
        && openedNotification != null) {
      Item current =
          result.stream()
              .filter(item -> item.id().equals(openedNotification.id()))
              .findFirst()
              .orElseGet(() ->
                  userNotificationService
                      .inboxItem(httpSession, openedNotification.id())
                      .orElse(null));
      if (current == null) {
        notificationDetailDialog.close();
      } else if (!current.equals(openedNotification)) {
        openedNotification = current;
        renderOpenedNotification();
      }
    }
  }

  private void renderRows(Page<Item> result) {
    notificationRows.removeAll();
    if (result.isEmpty()) {
      notificationRows.add(
          new NotificationEmptyState(
              !compact && !notificationSearchField.isEmpty()
                  ? messageProperties.getNotificationsSearchEmpty()
                  : messageProperties.getNotificationsEmpty()));
    }
    for (Item item : result) {
      notificationRows.add(buildNotificationRow(item));
    }
  }

  public void unavailable() {
    notificationActionConfirmation.close();
    unreadIds = List.of();
    notificationRows.removeAll();
    notificationRows.add(new Span(messageProperties.getNotificationsUnavailable()));
    markAllReadButton.setEnabled(false);
    dismissReadButton.setEnabled(false);
    previousPageButton.setEnabled(false);
    nextPageButton.setEnabled(false);
    firstPageButton.setEnabled(false);
    lastPageButton.setEnabled(false);
    pageNumbers.removeAll();
    pageTotal.setText("–");
    totalCount.setText("–");
    unreadCount.setText("–");
    if (notificationDetailDialog != null) {
      notificationDetailDialog.close();
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
                refreshInbox();
              });
      button.setAriaLabel(messageProperties.getAdminUsersPaginationPage(index + 1));
      if (index == page) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.getElement().setAttribute("aria-current", "page");
      }
      pageNumbers.add(button);
      preceding = index;
    }
  }

  private Div buildNotificationRow(Item item) {
    Button title =
        new Button(
            UserNotificationTexts.getNotificationTitleText(messageProperties, item),
            e -> runNotificationAction(() -> openNotificationDetails(item)));
    title.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    title.addClassName("cosmos-notification-title");
    if (compact) {
      return previewRow(item, title);
    }
    Div meta = messageMetadata(item);
    meta.addClassName("cosmos-notification-inbox-meta");
    Button open =
        new Button(
            AppIcon.EYE.create(), e -> runNotificationAction(() -> openNotificationDetails(item)));
    open.addClassNames(COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    open.setTooltipText(messageProperties.getNotificationsOpen());
    open.setAriaLabel(
        messageProperties.getNotificationsOpen()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    Button mark = new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    mark.setVisible(item.unread());
    mark.addClassNames("cosmos-notification-inbox-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    mark.setTooltipText(messageProperties.getNotificationsMarkRead());
    mark.setAriaLabel(
        messageProperties.getNotificationsMarkRead()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    Div actions = new Div(open, mark, dismissButton(item));
    actions.addClassName("cosmos-notification-inbox-actions");
    Div top = new Div(meta, actions);
    top.addClassName("cosmos-notification-inbox-row-header");
    Div body = new Div();
    body.setText(UserNotificationTexts.getNotificationBodyText(messageProperties, item));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-inbox-message");
    Div content = new Div(title, body);
    content.addClassName("cosmos-notification-inbox-content");
    Div row = new Div(top, content);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-inbox-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    row.setClassName("cosmos-notification-row--unread", item.unread());
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    row.getElement()
        .addEventListener("click", e -> runNotificationAction(() -> openNotificationDetails(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return row;
  }

  private Button dismissButton(Item item) {
    Button dismiss = new Button(AppIcon.TRASH.create(), e -> confirmDismiss(item));
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    dismiss.addClassNames("cosmos-notification-preview-dismiss", DELETE_BUTTON);
    dismiss.setTooltipText(messageProperties.getNotificationsDismiss());
    dismiss.setAriaLabel(
        messageProperties.getNotificationsDismiss()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    return dismiss;
  }

  private Div previewRow(Item item, Button title) {
    Div meta = messageMetadata(item);
    meta.addClassName("cosmos-notification-preview-meta");
    Div body = new Div();
    body.setText(UserNotificationTexts.getNotificationBodyText(messageProperties, item));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-preview-message");
    Button open =
        new Button(
            AppIcon.EYE.create(), e -> runNotificationAction(() -> openNotificationDetails(item)));
    open.setAriaLabel(
        messageProperties.getNotificationsOpen()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    open.addClassNames(COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    open.setTooltipText(messageProperties.getNotificationsOpen());
    Button markRead = new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    markRead.addClassNames(
        "cosmos-notification-preview-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    markRead.setTooltipText(messageProperties.getNotificationsMarkRead());
    markRead.setVisible(item.unread());
    markRead.setAriaLabel(
        messageProperties.getNotificationsMarkRead()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    Div actions = new Div(open, markRead, dismissButton(item));
    actions.addClassName("cosmos-notification-preview-actions");
    Div row = new Div(title, body, meta, actions);
    row.addClassNames("cosmos-notification-row", "cosmos-notification-preview-row");
    row.getElement().setAttribute("data-notification-id", item.id().toString());
    if (item.unread()) {
      row.addClassName("cosmos-notification-row--unread");
    }
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    row.getElement()
        .addEventListener("click", e -> runNotificationAction(() -> openNotificationDetails(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return row;
  }

  private void runNotificationAction(Runnable action) {
    try {
      action.run();
      refreshInbox();
    } catch (RuntimeException failure) {
      if (failure instanceof org.springframework.security.access.AccessDeniedException) {
        unavailable();
      }
      LoggerFactory.getLogger(getClass()).warn("Notification action failed", safeFailure(failure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getNotificationsUnavailable,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private void openNotificationDetails(Item item) {
    userNotificationService.ownerId(httpSession);
    if (notificationDetailDialog != null) {
      notificationDetailDialog.close();
    }
    openedNotification = item;
    notificationDetailDialog = new Dialog();
    notificationDetailDialog.addOpenedChangeListener(e -> {
      if (!e.isOpened() && detailActionsMenu != null) {
        detailActionsMenu.close();
      }
      notifyOverlayState();
    });
    notificationDetailDialog.addClassNames(
        COSMOS_THEME, REFERENCE_OVERLAY, "cosmos-notification-dialog");
    notificationDetailDialog.setModality(com.vaadin.flow.component.ModalityMode.STRICT);
    notificationDetailDialog.getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    notificationDetailDialog.setCloseOnOutsideClick(true);
    notificationDetailDialog.setCloseOnEsc(true);
    renderOpenedNotification();
    notificationDetailDialog.open();
  }

  private void notifyOverlayState() {
    detailOpenListener.accept(
        notificationActionConfirmation.isOpened()
            || notificationDetailDialog != null && notificationDetailDialog.isOpened());
  }

  private PortalContextCard buildNotificationContextCard(Item item) {
    PortalContextCard card =
        new PortalContextCard(
            AppIcon.BELL.create(),
            UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    card.addMetadata(messageMetadata(item));
    return card;
  }

  private void confirmMarkRead(Item item) {
    notificationActionConfirmation.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getNotificationsMarkRead,
            messageProperties::getPortalNotificationSelectionHint,
            messageProperties::getPortalMarkReadQuestion,
            messageProperties::getPortalMarkReadExplanation,
            () -> buildNotificationContextCard(item),
            messageProperties::getPortalNotificationSyncHint,
            messageProperties::getPortalConfirmAction,
            false),
        () -> runNotificationAction(() -> markRead(item)));
  }

  private void confirmMarkAll() {
    List<Long> selected = List.copyOf(unreadIds);
    if (selected.isEmpty()) {
      return;
    }
    notificationActionConfirmation.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getNotificationsMarkAll,
            messageProperties::getPortalNotificationSelectionHint,
            messageProperties::getPortalMarkAllQuestion,
            messageProperties::getPortalMarkAllExplanation,
            () ->
                new PortalContextCard(
                    AppIcon.BELL.create(),
                    messageProperties.getPortalNotificationAffected(selected.size())),
            messageProperties::getPortalNotificationSyncHint,
            messageProperties::getPortalConfirmAction,
            false,
            AppIcon.CHECK_CHECK::create),
        () ->
            runNotificationAction(() -> {
              boolean includesOpened =
                  openedNotification != null && selected.contains(openedNotification.id());
              userNotificationService.markRead(httpSession, selected);
              if (includesOpened) {
                updateOpenedRead();
              }
            }));
  }

  private void confirmDismiss(Item item) {
    notificationActionConfirmation.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getNotificationsDismiss,
            messageProperties::getPortalNotificationSelectionHint,
            messageProperties::getPortalDeleteNotificationQuestion,
            messageProperties::getPortalDeleteNotificationExplanation,
            () -> buildNotificationContextCard(item),
            messageProperties::getPortalDeleteNotificationHint,
            messageProperties::getPortalDeleteAction,
            true),
        () ->
            runNotificationAction(() -> {
              userNotificationService.dismiss(httpSession, item.id());
              if (notificationDetailDialog != null
                  && openedNotification != null
                  && openedNotification.id().equals(item.id())) {
                notificationDetailDialog.close();
              }
            }));
  }

  private void confirmDismissRead() {
    runNotificationAction(() ->
        notificationActionConfirmation.open(
            new PortalConfirmationDialog.Prompt(
                messageProperties::getNotificationsDismissRead,
                messageProperties::getPortalNotificationSelectionHint,
                messageProperties::getPortalDeleteReadQuestion,
                messageProperties::getPortalDeleteReadExplanation,
                () ->
                    new PortalContextCard(
                        AppIcon.BELL.create(),
                        messageProperties.getPortalNotificationAffected(
                            userNotificationService.readCount(httpSession))),
                messageProperties::getPortalDeleteNotificationHint,
                messageProperties::getPortalDeleteAction,
                true),
            () ->
                runNotificationAction(() -> {
                  userNotificationService.dismissRead(httpSession);
                  if (notificationDetailDialog != null
                      && openedNotification != null
                      && openedNotification.read()) {
                    notificationDetailDialog.close();
                  }
                })));
  }

  private void markRead(Item item) {
    userNotificationService.markRead(httpSession, List.of(item.id()));
    if (openedNotification != null && openedNotification.id().equals(item.id())) {
      updateOpenedRead();
    }
  }

  private void updateOpenedRead() {
    openedNotification =
        new Item(
            openedNotification.id(),
            openedNotification.type(),
            openedNotification.title(),
            openedNotification.body(),
            openedNotification.createdAt(),
            true,
            openedNotification.supportTicketNumber(),
            openedNotification.relevance());
    if (notificationDetailDialog != null && notificationDetailDialog.isOpened()) {
      renderOpenedNotification();
    }
  }

  private Div messageMetadata(Item item, String... stateClasses) {
    boolean administration = item.type() == UserNotificationType.ADMIN_ANNOUNCEMENT;
    Span origin = new Span(resolveNotificationSourceText(item));
    origin.addClassNames(
        "cosmos-notification-preview-source",
        administration ? "portal-notification-source-admin" : "portal-notification-source-system");
    Span time =
        new Span(
            UserNotificationTexts.formatNotificationTimestamp(
                item.createdAt(), UI.getCurrent().getLocale()));
    time.addClassName("cosmos-notification-message-time");
    Span state =
        new Span(UserNotificationTexts.getNotificationRelevanceText(messageProperties, item));
    state.getElement().setAttribute(THEME_ATTRIBUTE, BADGE_THEME);
    state.getElement().setAttribute("data-read", Boolean.toString(item.read()));
    state.addClassName("cosmos-notification-preview-state");
    state.addClassNames(stateClasses);
    Component status = state;
    if (item.relevance() != UserNotificationRelevance.ACTIVE) {
      Badge retired =
          new Badge(UserNotificationTexts.getNotificationRelevanceText(messageProperties, item));
      retired.addClassNames("cosmos-notification-preview-state");
      retired.addClassNames(stateClasses);
      retired.getElement().setAttribute("data-relevance", item.relevance().name());
      status = retired;
    }
    Div metadata = new Div(origin, time, status);
    metadata.addClassName("cosmos-notification-message-meta");
    return metadata;
  }

  private String resolveNotificationSourceText(Item item) {
    return item.type() == UserNotificationType.ADMIN_ANNOUNCEMENT
        ? messageProperties.getNotificationsSourceAdministration()
        : messageProperties.getNotificationsSourceSystem();
  }

  private void renderOpenedNotification() {
    if (detailActionsMenu != null) {
      detailActionsMenu.close();
    }
    notificationDetailDialog.removeAll();
    notificationDetailDialog.getHeader().removeAll();
    notificationDetailDialog.getFooter().removeAll();
    String subject =
        UserNotificationTexts.getNotificationTitleText(messageProperties, openedNotification);
    buildNotificationDetailHeader(subject);
    Div notificationMessage = buildNotificationDetailContent(subject);
    buildNotificationDetailActions(notificationMessage);
  }

  private void buildNotificationDetailHeader(String subject) {
    H2 title = new H2(messageProperties.getNotificationsDetailTitle());
    title.addClassName(PANEL_TITLE);
    Span detailDescription = new Span(messageProperties.getNotificationsDetailDescription());
    detailDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(title, detailDescription);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    SvgIcon icon = AppIcon.BELL.create();
    icon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div badge = new Div(icon);
    badge.addClassName(PANEL_ICON);
    Button closeIcon =
        new Button(AppIcon.CLOSE_CIRCLE.create(), e -> notificationDetailDialog.close());
    closeIcon.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeIcon.addClassName("cosmos-overlay-close");
    closeIcon.setAriaLabel(messageProperties.getNotificationsClose());
    closeIcon.setTooltipText(messageProperties.getNotificationsClose());
    Div header = new Div(badge, titles, closeIcon);
    header.addClassName("cosmos-notification-detail-header");
    notificationDetailDialog.getHeader().add(header);
    notificationDetailDialog.getElement().setProperty("ariaLabel", subject);
  }

  private Div buildNotificationDetailContent(String subject) {
    H3 contentTitle = new H3(subject);
    contentTitle.addClassName("cosmos-notification-detail-title");
    Div titleRow = new Div(contentTitle);
    titleRow.addClassName("cosmos-notification-detail-title-row");
    Div contentMeta = messageMetadata(openedNotification, "cosmos-notification-detail-state");
    contentMeta.addClassName("cosmos-notification-detail-meta");
    Div body = new Div();
    body.setText(
        UserNotificationTexts.getNotificationBodyText(messageProperties, openedNotification));
    body.addClassNames(COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-detail-body");
    Div message = new Div(titleRow, contentMeta, body);
    message.addClassName("cosmos-notification-detail-message");
    if (openedNotification.unread()) {
      message.addClassName("cosmos-notification-detail-message--unread");
    }
    Div detailContent = new Div(message);
    detailContent.addClassName("cosmos-notification-detail-content");
    notificationDetailDialog.add(detailContent);
    return message;
  }

  private void buildNotificationDetailActions(Div notificationMessage) {
    Button detailHistory =
        new Button(
            messageProperties.getNotificationsHistory(),
            AppIcon.BELL.create(),
            e -> {
              detailActionsMenu.close();
              notificationDetailDialog.close();
              closeAction.run();
              UI.getCurrent().navigate(UserNotificationsView.class);
            });
    detailHistory.setAriaLabel(messageProperties.getNotificationsHistory());
    detailHistory.setTooltipText(messageProperties.getNotificationsHistory());
    detailHistory.addClassName("cosmos-notification-detail-history");
    detailHistory.setVisible(compact);
    Button markRead =
        new Button(
            messageProperties.getNotificationsMarkRead(),
            AppIcon.CHECK.create(),
            e -> confirmMarkRead(openedNotification));
    markRead.addClassName("cosmos-notification-detail-mark-read");
    markRead.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    markRead.setVisible(openedNotification.unread());
    HorizontalLayout readActions = new HorizontalLayout(markRead);
    readActions.setPadding(false);
    readActions.setSpacing(false);
    readActions.setWidthFull();
    readActions.setJustifyContentMode(JustifyContentMode.END);
    readActions.addClassName("cosmos-notification-detail-read-actions");
    readActions.setVisible(openedNotification.unread());
    notificationMessage.add(readActions);
    Button detailClose =
        new Button(
            messageProperties.getNotificationsClose(),
            AppIcon.CLOSE.create(),
            e -> notificationDetailDialog.close());
    detailClose.addClassName("cosmos-notification-detail-close");
    Button destination = createNotificationDestinationButton();
    Button delete =
        new Button(
            messageProperties.getNotificationsDismiss(),
            AppIcon.TRASH.create(),
            event -> {
              detailActionsMenu.close();
              confirmDismiss(openedNotification);
            });
    delete.addClassNames("cosmos-notification-detail-delete", DELETE_BUTTON);
    delete.setAriaLabel(messageProperties.getNotificationsDismiss());
    delete.setTooltipText(messageProperties.getNotificationsDismiss());
    Button more =
        new Button(messageProperties.getNotificationsMoreActions(), AppIcon.ANGLE_DOWN.create());
    more.setIconAfterText(true);
    more.addClassName("cosmos-notification-detail-more");
    detailActionsMenu = new Popover();
    detailActionsMenu.setTarget(more);
    detailActionsMenu.setPosition(PopoverPosition.TOP_START);
    detailActionsMenu.setModal(true);
    detailActionsMenu.setCloseOnEsc(true);
    detailActionsMenu.setCloseOnOutsideClick(true);
    detailActionsMenu.addClassName("cosmos-notification-actions-popover");
    detailActionsMenu.setAriaLabel(messageProperties.getNotificationsMoreActions());
    VerticalLayout tools = new VerticalLayout(detailHistory, delete);
    tools.setPadding(false);
    tools.setSpacing(false);
    tools.addClassNames("cosmos-notification-detail-tools", COSMOS_THEME, REFERENCE_OVERLAY);
    detailActionsMenu.add(tools);
    notificationDetailDialog.add(detailActionsMenu);
    HorizontalLayout primary = new HorizontalLayout(detailClose);
    primary.setPadding(false);
    primary.setSpacing(false);
    primary.setAlignItems(Alignment.CENTER);
    if (destination != null) {
      destination.setAriaLabel(destination.getText());
      destination.setTooltipText(destination.getText());
      destination.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      destination.addClassName("cosmos-notification-detail-destination");
      primary.add(destination);
    }
    primary.addClassName("cosmos-notification-detail-primary");
    HorizontalLayout actions = new HorizontalLayout(more, primary);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.setAlignItems(Alignment.CENTER);
    actions.setJustifyContentMode(JustifyContentMode.BETWEEN);
    actions.setWidthFull();
    actions.addClassNames("cosmos-dialog-actions", "cosmos-notification-detail-actions");
    notificationDetailDialog.getFooter().add(actions);
  }

  private Button createNotificationDestinationButton() {
    Button destination = null;
    if (Set.of(
                UserNotificationType.RECOVERY_REQUEST,
                UserNotificationType.SUPPORT_REPLY,
                UserNotificationType.SUPPORT_REQUESTER_CLOSED)
            .contains(openedNotification.type())
        && openedNotification.supportTicketNumber() != null
        && userNotificationService.canOpenTickets(httpSession)) {
      destination =
          new Button(
              messageProperties.getNotificationsOpenTicket(),
              AppIcon.MESSAGE.create(),
              event -> {
                userNotificationService.ownerId(httpSession);
                notificationDetailDialog.close();
                closeAction.run();
                UI.getCurrent()
                    .navigate(
                        AdminView.class,
                        QueryParameters.simple(
                            Map.of(
                                "section",
                                "support",
                                "ticket",
                                openedNotification.supportTicketNumber().substring(1))));
              });
      destination.addClassName("cosmos-notification-detail-ticket");
    }
    if (Set.of(
            UserNotificationType.PASSWORD_CHANGED,
            UserNotificationType.PASSWORD_RESET,
            UserNotificationType.EMAIL_CHANGED,
            UserNotificationType.ROLES_CHANGED)
        .contains(openedNotification.type())) {
      destination =
          new Button(
              messageProperties.getNotificationsOpenAccount(),
              AppIcon.LOCK.create(),
              e -> {
                notificationDetailDialog.close();
                closeAction.run();
                UI.getCurrent()
                    .navigate(AccountView.class, AccountSection.SECURITY.queryParameters());
              });
      destination.addClassName("cosmos-notification-detail-account");
    }
    return destination;
  }
}
