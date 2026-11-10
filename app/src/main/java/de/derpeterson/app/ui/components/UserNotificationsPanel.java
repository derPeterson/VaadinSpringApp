package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

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

  // Posteingang und Lesefilter
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

  // Seitenauswahl und Zusammenfassung
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
  private final Span notificationRetentionSpan = new Span();
  private final Span previewHint = new Span();
  private List<Long> unreadIds = List.of();
  private int page;

  // Detailansicht und Bestätigung von Aktionen
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
    Div inboxToolbarDiv = new Div(readFilter, refreshInboxButton);
    inboxToolbarDiv.addClassName("cosmos-notifications-actions");
    configureCommonActions();

    Div inboxPagingDiv =
        new Div(firstPageButton, previousPageButton, pageNumbers, nextPageButton, lastPageButton);
    inboxPagingDiv.addClassName("cosmos-notifications-actions");
    inboxPagingDiv.setVisible(!compact);
    openHistoryButton.setVisible(compact);
    notificationRetentionSpan.addClassName("cosmos-notifications-secondary");

    if (compact) {
      buildPreview(inboxToolbarDiv);
    } else {
      buildInbox(inboxToolbarDiv, inboxPagingDiv);
    }

    add(notificationActionConfirmation);
    refreshTexts();
    addDetachListener(detachEvent -> {
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
          Span filterLabelSpan = new Span(filterLabel(filter));
          Span filterCountSpan = new Span();
          filterCountSpan.addClassName("portal-filter-count");
          filterCountSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
          long total = totalItems;
          long unread = unreadIds.size();
          filterCountSpan.setText(
              Long.toString(
                  switch (filter) {
                    case ALL -> total;
                    case UNREAD -> unread;
                    case READ -> Math.max(0, total - unread);
                  }));

          Div filterOptionDiv = new Div(filterLabelSpan, filterCountSpan);
          filterOptionDiv.addClassName("portal-read-filter-option");
          return filterOptionDiv;
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

  private void buildPreview(Div previewToolbarDiv) {
    SvgIcon previewBellSvgIcon = AppIcon.BELL.create();
    previewBellSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div previewIconDiv = new Div(previewBellSvgIcon);
    previewIconDiv.addClassName(PANEL_ICON);
    inboxDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div previewTitlesDiv = new Div(inboxHeading, inboxDescription);
    previewTitlesDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);
    closeInboxButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeInboxButton.addClassName("cosmos-overlay-close");
    closeInboxButton.addClickListener(e -> closeAction.run());
    unreadCount.addClassName("portal-preview-unread-count");
    Div previewHeaderDiv = new Div(previewIconDiv, previewTitlesDiv, unreadCount, closeInboxButton);
    previewHeaderDiv.addClassName("cosmos-notification-preview-header");
    previewToolbarDiv.addClassName("cosmos-notification-preview-toolbar");
    refreshInboxButton.setIcon(AppIcon.REFRESH.create());
    refreshInboxButton.addClassNames(
        "cosmos-notification-preview-refresh", "cosmos-notification-preview-tool");
    markAllReadButton.addClassNames(
        "cosmos-notification-preview-mark-all", "cosmos-notification-preview-tool");
    Div previewToolsDiv = new Div(refreshInboxButton, markAllReadButton);
    previewToolsDiv.addClassName("cosmos-notification-preview-tools");
    previewToolbarDiv.removeAll();
    previewToolbarDiv.add(readFilter, previewToolsDiv);
    notificationRows.addClassName("cosmos-notification-preview-rows");
    previewHint.addClassName("cosmos-notification-preview-hint");
    previewHint.setVisible(false);
    Div previewContentDiv = new Div(previewToolbarDiv, previewHint, notificationRows);
    previewContentDiv.addClassName("cosmos-notification-preview-content");
    openHistoryButton.setIcon(AppIcon.EYE.create());
    openHistoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div previewHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), notificationRetentionSpan);
    previewHintDiv.addClassName("cosmos-notification-preview-retention");
    Div previewFooterDiv = new Div(openHistoryButton, previewHintDiv);
    previewFooterDiv.addClassName("cosmos-notification-preview-footer");
    add(previewHeaderDiv, previewContentDiv, previewFooterDiv);
  }

  private void buildInbox(Div inboxToolbarDiv, Div inboxPagingDiv) {
    SvgIcon inboxArchiveSvgIcon = AppIcon.BELL.create();
    inboxArchiveSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div inboxIconDiv = new Div(inboxArchiveSvgIcon);
    inboxIconDiv.addClassName(PANEL_ICON);
    inboxDescription.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div titles = new Div(inboxHeading, inboxDescription);
    titles.addClassName(COSMOS_PANEL_HEADING_CLASS);
    Div unreadStatDiv = buildNotificationStatRow(unreadLabel, unreadCount);
    unreadStatDiv.addClassName("cosmos-notification-inbox-stat-unread");
    Div inboxCountsDiv = new Div(buildNotificationStatRow(totalLabel, totalCount), unreadStatDiv);
    inboxCountsDiv.addClassName("cosmos-notification-inbox-counts");
    totalCount.setId("notifications-inbox-total");
    unreadCount.setId("notifications-inbox-unread-total");
    Div inboxHeadingDiv = new Div(inboxIconDiv, titles);
    inboxHeadingDiv.addClassName("cosmos-notification-inbox-heading");

    Div inboxHeaderDiv = new Div(inboxHeadingDiv, inboxCountsDiv);
    inboxHeaderDiv.addClassNames(PANEL_HEADER, "cosmos-notification-inbox-header");
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

    Div inboxToolsDiv = new Div(refreshInboxButton, markAllReadButton, dismissReadButton);
    inboxToolsDiv.addClassName("cosmos-notification-inbox-tools");
    refreshInboxButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    markAllReadButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    dismissReadButton.addClassName(COSMOS_NOTIFICATION_INBOX_TOOL_CLASS);
    inboxToolbarDiv.removeAll();
    Div inboxControlsDiv = new Div(readFilter, inboxToolsDiv);
    inboxControlsDiv.addClassName("cosmos-notification-inbox-controls");
    inboxToolbarDiv.add(inboxControlsDiv, notificationSearchField);
    inboxToolbarDiv.addClassName("cosmos-notification-inbox-toolbar");
    notificationRows.addClassName("cosmos-notification-inbox-rows");
    previousPageButton.setIcon(AppIcon.ANGLE_LEFT.create());
    nextPageButton.setIcon(AppIcon.ANGLE_RIGHT.create());
    pageNumbers.addClassName("cosmos-notification-inbox-page-numbers");
    inboxPagingDiv.addClassName("cosmos-notification-inbox-pagination");
    pageSize.setId("notifications-inbox-page-size");
    pageSize.addClassName("portal-page-size");
    CosmosComboBoxStyling.styleChoice(pageSize, Object::toString, false);
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
    Div inboxSummaryDiv = new Div(pageLabel, pageSize);
    pageTotal.addClassName("cosmos-notification-inbox-range");
    inboxSummaryDiv.addClassName("cosmos-notification-inbox-page-summary");
    Div paginationRowDiv = new Div(inboxSummaryDiv, pageTotal, inboxPagingDiv);
    paginationRowDiv.addClassName("cosmos-notification-inbox-pagination-row");
    Div retentionNoteDiv = new Div(AppIcon.INFO_CIRCLE.create(), notificationRetentionSpan);
    retentionNoteDiv.addClassName("cosmos-notification-inbox-retention");

    Div inboxFooterDiv = new Div(paginationRowDiv, retentionNoteDiv);
    inboxFooterDiv.addClassName("cosmos-notification-inbox-footer");
    add(inboxHeaderDiv, inboxToolbarDiv, notificationRows, inboxFooterDiv);
  }

  private Div buildNotificationStatRow(Span statLabelSpan, Span statValueSpan) {
    statLabelSpan.addClassName("cosmos-notification-inbox-stat-label");
    statValueSpan.addClassName("cosmos-notification-inbox-stat-value");
    Div notificationStatDiv = new Div(statLabelSpan, statValueSpan);
    notificationStatDiv.addClassName("cosmos-notification-inbox-stat");
    return notificationStatDiv;
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
    notificationRetentionSpan.setText(
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

      Page<Item> notificationPage = loadPage();
      if (page > 0 && page >= notificationPage.getTotalPages()) {
        page = Math.max(0, notificationPage.getTotalPages() - 1);
        refreshInbox(notifyResult);
        return;
      }

      unreadIds = userNotificationService.unreadSnapshot(httpSession);
      totalItems = userNotificationService.inboxCount(httpSession);
      totalCount.setText(Long.toString(totalItems));
      unreadCount.setText(Integer.toString(unreadIds.size()));

      refreshReadFilterOptions();

      refreshOpenedDetail(notificationPage);
      markAllReadButton.setEnabled(!unreadIds.isEmpty());
      dismissReadButton.setEnabled(!compact && userNotificationService.readCount(httpSession) > 0);
      renderRows(notificationPage);

      updateInboxPagination(notificationPage);
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

  private void refreshReadFilterOptions() {
    updatingFilterLabels = true;
    try {
      ReadFilter selectedReadFilter = selectedFilter();
      readFilter.getDataProvider().refreshAll();
      readFilter.setValue(selectedReadFilter);
    } finally {
      updatingFilterLabels = false;
    }
  }

  private void updateInboxPagination(Page<Item> notificationPage) {
    totalPages = Math.max(1, notificationPage.getTotalPages());
    firstPageButton.setEnabled(page > 0);
    previousPageButton.setEnabled(page > 0);
    nextPageButton.setEnabled(notificationPage.hasNext());
    lastPageButton.setEnabled(notificationPage.hasNext());
    if (!compact) {
      renderPageNumbers();
    }

    long firstEntryNumber = notificationPage.isEmpty() ? 0 : (long) page * selectedPageSize() + 1;
    long lastEntryNumber =
        notificationPage.isEmpty()
            ? 0
            : (long) page * selectedPageSize() + notificationPage.getNumberOfElements();
    pageTotal.setText(
        messageProperties.getPortalPaginationRange(
            firstEntryNumber, lastEntryNumber, notificationPage.getTotalElements()));
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

  private void renderRows(Page<Item> notificationPage) {
    notificationRows.removeAll();
    if (notificationPage.isEmpty()) {
      notificationRows.add(
          new NotificationEmptyState(
              !compact && !notificationSearchField.isEmpty()
                  ? messageProperties.getNotificationsSearchEmpty()
                  : messageProperties.getNotificationsEmpty()));
    }
    for (Item item : notificationPage) {
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
    TreeSet<Integer> visiblePageIndices = new TreeSet<Integer>();
    visiblePageIndices.add(0);
    visiblePageIndices.add(totalPages - 1);
    int firstVisiblePageIndex = totalPages <= 7 ? 0 : Math.clamp(page - 1L, 1, totalPages - 4);
    int lastVisiblePageIndex =
        totalPages <= 7 ? totalPages - 1 : Math.clamp(page + 1L, 3, totalPages - 2);
    for (int pageIndex = firstVisiblePageIndex; pageIndex <= lastVisiblePageIndex; pageIndex++) {
      visiblePageIndices.add(pageIndex);
    }
    int preceding = -1;
    for (int pageIndex : visiblePageIndices) {
      if (preceding >= 0 && pageIndex - preceding > 1) {
        pageNumbers.add(new Span("…"));
      }
      Button pageNumberButton =
          new Button(
              Integer.toString(pageIndex + 1),
              e -> {
                page = pageIndex;
                refreshInbox();
              });
      pageNumberButton.setAriaLabel(messageProperties.getAdminUsersPaginationPage(pageIndex + 1));
      if (pageIndex == page) {
        pageNumberButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        pageNumberButton.getElement().setAttribute("aria-current", "page");
      }
      pageNumbers.add(pageNumberButton);
      preceding = pageIndex;
    }
  }

  private Div buildNotificationRow(Item item) {
    Button notificationTitleButton =
        new Button(
            UserNotificationTexts.getNotificationTitleText(messageProperties, item),
            e -> runNotificationAction(() -> openNotificationDetails(item)));
    notificationTitleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    notificationTitleButton.addClassName("cosmos-notification-title");

    if (compact) {
      return previewRow(item, notificationTitleButton);
    }

    Div notificationMetadataDiv = messageMetadata(item);
    notificationMetadataDiv.addClassName("cosmos-notification-inbox-meta");
    Button openNotificationButton =
        new Button(
            AppIcon.EYE.create(), e -> runNotificationAction(() -> openNotificationDetails(item)));
    openNotificationButton.addClassNames(
        COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    openNotificationButton.setTooltipText(messageProperties.getNotificationsOpen());
    openNotificationButton.setAriaLabel(
        messageProperties.getNotificationsOpen()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));

    Button markNotificationReadButton =
        new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    markNotificationReadButton.setVisible(item.unread());
    markNotificationReadButton.addClassNames(
        "cosmos-notification-inbox-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    markNotificationReadButton.setTooltipText(messageProperties.getNotificationsMarkRead());
    markNotificationReadButton.setAriaLabel(
        messageProperties.getNotificationsMarkRead()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    Div rowActionsDiv =
        new Div(openNotificationButton, markNotificationReadButton, dismissButton(item));
    rowActionsDiv.addClassName("cosmos-notification-inbox-actions");
    Div rowHeadingDiv = new Div(notificationMetadataDiv, rowActionsDiv);
    rowHeadingDiv.addClassName("cosmos-notification-inbox-row-header");

    Div notificationBodyDiv = new Div();
    notificationBodyDiv.setText(
        UserNotificationTexts.getNotificationBodyText(messageProperties, item));
    notificationBodyDiv.addClassNames(
        COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-inbox-message");
    Div content = new Div(notificationTitleButton, notificationBodyDiv);
    content.addClassName("cosmos-notification-inbox-content");
    Div notificationRowDiv = new Div(rowHeadingDiv, content);
    notificationRowDiv.addClassNames("cosmos-notification-row", "cosmos-notification-inbox-row");
    notificationRowDiv.getElement().setAttribute("data-notification-id", item.id().toString());
    notificationRowDiv.setClassName("cosmos-notification-row--unread", item.unread());
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    notificationRowDiv
        .getElement()
        .addEventListener("click", e -> runNotificationAction(() -> openNotificationDetails(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return notificationRowDiv;
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

  private Div previewRow(Item item, Button previewTitleButton) {
    Div previewMetadataDiv = messageMetadata(item);
    previewMetadataDiv.addClassName("cosmos-notification-preview-meta");
    Div previewBodyDiv = new Div();
    previewBodyDiv.setText(UserNotificationTexts.getNotificationBodyText(messageProperties, item));
    previewBodyDiv.addClassNames(
        COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-preview-message");
    Button open =
        new Button(
            AppIcon.EYE.create(), e -> runNotificationAction(() -> openNotificationDetails(item)));
    open.setAriaLabel(
        messageProperties.getNotificationsOpen()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    open.addClassNames(COSMOS_NOTIFICATION_ROW_ACTION_CLASS, "cosmos-notification-row-open");
    open.setTooltipText(messageProperties.getNotificationsOpen());

    Button markNotificationReadButton =
        new Button(AppIcon.CHECK.create(), e -> confirmMarkRead(item));
    markNotificationReadButton.addClassNames(
        "cosmos-notification-preview-mark-read", COSMOS_NOTIFICATION_ROW_ACTION_CLASS);
    markNotificationReadButton.setTooltipText(messageProperties.getNotificationsMarkRead());
    markNotificationReadButton.setVisible(item.unread());
    markNotificationReadButton.setAriaLabel(
        messageProperties.getNotificationsMarkRead()
            + ": "
            + UserNotificationTexts.getNotificationTitleText(messageProperties, item));
    Div previewActionsDiv = new Div(open, markNotificationReadButton, dismissButton(item));
    previewActionsDiv.addClassName("cosmos-notification-preview-actions");
    Div previewRowDiv =
        new Div(previewTitleButton, previewBodyDiv, previewMetadataDiv, previewActionsDiv);
    previewRowDiv.addClassNames("cosmos-notification-row", "cosmos-notification-preview-row");
    previewRowDiv.getElement().setAttribute("data-notification-id", item.id().toString());
    if (item.unread()) {
      previewRowDiv.addClassName("cosmos-notification-row--unread");
    }
    // Buttons behalten ihre eigene Aktion; die restliche Karte öffnet per einfachem Klick.
    previewRowDiv
        .getElement()
        .addEventListener("click", e -> runNotificationAction(() -> openNotificationDetails(item)))
        .setFilter("!event.composedPath().some(node => node.localName === 'vaadin-button')");
    return previewRowDiv;
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
    Span notificationSourceSpan = new Span(resolveNotificationSourceText(item));
    notificationSourceSpan.addClassNames(
        "cosmos-notification-preview-source",
        administration ? "portal-notification-source-admin" : "portal-notification-source-system");
    Span notificationTimeSpan =
        new Span(
            UserNotificationTexts.formatNotificationTimestamp(
                item.createdAt(), UI.getCurrent().getLocale()));
    notificationTimeSpan.addClassName("cosmos-notification-message-time");
    Span notificationStateSpan =
        new Span(UserNotificationTexts.getNotificationRelevanceText(messageProperties, item));
    notificationStateSpan.getElement().setAttribute(THEME_ATTRIBUTE, BADGE_THEME);
    notificationStateSpan.getElement().setAttribute("data-read", Boolean.toString(item.read()));
    notificationStateSpan.addClassName("cosmos-notification-preview-state");
    notificationStateSpan.addClassNames(stateClasses);
    Component notificationStatusComponent = notificationStateSpan;
    if (item.relevance() != UserNotificationRelevance.ACTIVE) {
      Badge retired =
          new Badge(UserNotificationTexts.getNotificationRelevanceText(messageProperties, item));
      retired.addClassNames("cosmos-notification-preview-state");
      retired.addClassNames(stateClasses);
      retired.getElement().setAttribute("data-relevance", item.relevance().name());
      notificationStatusComponent = retired;
    }

    Div notificationMetadataDiv =
        new Div(notificationSourceSpan, notificationTimeSpan, notificationStatusComponent);
    notificationMetadataDiv.addClassName("cosmos-notification-message-meta");
    return notificationMetadataDiv;
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

  private void buildNotificationDetailHeader(String notificationSubjectText) {
    H2 detailTitleHeading = new H2(messageProperties.getNotificationsDetailTitle());
    detailTitleHeading.addClassName(PANEL_TITLE);

    Span detailDescriptionSpan = new Span(messageProperties.getNotificationsDetailDescription());
    detailDescriptionSpan.addClassName(COSMOS_PANEL_DESCRIPTION_CLASS);
    Div detailHeadingDiv = new Div(detailTitleHeading, detailDescriptionSpan);
    detailHeadingDiv.addClassName(COSMOS_PANEL_HEADING_CLASS);

    SvgIcon detailBellSvgIcon = AppIcon.BELL.create();
    detailBellSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div detailIconDiv = new Div(detailBellSvgIcon);
    detailIconDiv.addClassName(PANEL_ICON);

    Button closeDetailButton =
        new Button(AppIcon.CLOSE_CIRCLE.create(), e -> notificationDetailDialog.close());
    closeDetailButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeDetailButton.addClassName("cosmos-overlay-close");
    closeDetailButton.setAriaLabel(messageProperties.getNotificationsClose());
    closeDetailButton.setTooltipText(messageProperties.getNotificationsClose());

    Div detailHeaderDiv = new Div(detailIconDiv, detailHeadingDiv, closeDetailButton);
    detailHeaderDiv.addClassName("cosmos-notification-detail-header");
    notificationDetailDialog.getHeader().add(detailHeaderDiv);
    notificationDetailDialog.getElement().setProperty("ariaLabel", notificationSubjectText);
  }

  private Div buildNotificationDetailContent(String notificationSubjectText) {
    H3 notificationTitleHeading = new H3(notificationSubjectText);
    notificationTitleHeading.addClassName("cosmos-notification-detail-title");
    Div notificationTitleRowDiv = new Div(notificationTitleHeading);
    notificationTitleRowDiv.addClassName("cosmos-notification-detail-title-row");

    Div notificationMetadataDiv =
        messageMetadata(openedNotification, "cosmos-notification-detail-state");
    notificationMetadataDiv.addClassName("cosmos-notification-detail-meta");

    Div notificationBodyDiv = new Div();
    notificationBodyDiv.setText(
        UserNotificationTexts.getNotificationBodyText(messageProperties, openedNotification));
    notificationBodyDiv.addClassNames(
        COSMOS_NOTIFICATION_BODY_CLASS, "cosmos-notification-detail-body");

    Div notificationMessageDiv =
        new Div(notificationTitleRowDiv, notificationMetadataDiv, notificationBodyDiv);
    notificationMessageDiv.addClassName("cosmos-notification-detail-message");
    if (openedNotification.unread()) {
      notificationMessageDiv.addClassName("cosmos-notification-detail-message--unread");
    }

    Div detailContentDiv = new Div(notificationMessageDiv);
    detailContentDiv.addClassName("cosmos-notification-detail-content");
    notificationDetailDialog.add(detailContentDiv);
    return notificationMessageDiv;
  }

  private void buildNotificationDetailActions(Div notificationMessageDiv) {
    Button openNotificationHistoryButton =
        new Button(
            messageProperties.getNotificationsHistory(),
            AppIcon.BELL.create(),
            e -> {
              detailActionsMenu.close();
              notificationDetailDialog.close();
              closeAction.run();
              UI.getCurrent().navigate(UserNotificationsView.class);
            });
    openNotificationHistoryButton.setAriaLabel(messageProperties.getNotificationsHistory());
    openNotificationHistoryButton.setTooltipText(messageProperties.getNotificationsHistory());
    openNotificationHistoryButton.addClassName("cosmos-notification-detail-history");
    openNotificationHistoryButton.setVisible(compact);

    Button markNotificationReadButton =
        new Button(
            messageProperties.getNotificationsMarkRead(),
            AppIcon.CHECK.create(),
            e -> confirmMarkRead(openedNotification));
    markNotificationReadButton.addClassName("cosmos-notification-detail-mark-read");
    markNotificationReadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    markNotificationReadButton.setVisible(openedNotification.unread());

    HorizontalLayout readActionsLayout = new HorizontalLayout(markNotificationReadButton);
    readActionsLayout.setPadding(false);
    readActionsLayout.setSpacing(false);
    readActionsLayout.setWidthFull();
    readActionsLayout.setJustifyContentMode(JustifyContentMode.END);
    readActionsLayout.addClassName("cosmos-notification-detail-read-actions");
    readActionsLayout.setVisible(openedNotification.unread());
    notificationMessageDiv.add(readActionsLayout);

    Button closeDetailButton =
        new Button(
            messageProperties.getNotificationsClose(),
            AppIcon.CLOSE.create(),
            e -> notificationDetailDialog.close());
    closeDetailButton.addClassName("cosmos-notification-detail-close");

    Button notificationDestinationButton = createNotificationDestinationButton();

    Button dismissNotificationButton =
        new Button(
            messageProperties.getNotificationsDismiss(),
            AppIcon.TRASH.create(),
            event -> {
              detailActionsMenu.close();
              confirmDismiss(openedNotification);
            });
    dismissNotificationButton.addClassNames("cosmos-notification-detail-delete", DELETE_BUTTON);
    dismissNotificationButton.setAriaLabel(messageProperties.getNotificationsDismiss());
    dismissNotificationButton.setTooltipText(messageProperties.getNotificationsDismiss());

    Button moreActionsButton =
        new Button(messageProperties.getNotificationsMoreActions(), AppIcon.ANGLE_DOWN.create());
    moreActionsButton.setIconAfterText(true);
    moreActionsButton.addClassName("cosmos-notification-detail-more");

    detailActionsMenu = new Popover();
    detailActionsMenu.setTarget(moreActionsButton);
    detailActionsMenu.setPosition(PopoverPosition.TOP_START);
    detailActionsMenu.setModal(true);
    detailActionsMenu.setCloseOnEsc(true);
    detailActionsMenu.setCloseOnOutsideClick(true);
    detailActionsMenu.addClassName("cosmos-notification-actions-popover");
    detailActionsMenu.setAriaLabel(messageProperties.getNotificationsMoreActions());

    VerticalLayout detailMenuLayout =
        new VerticalLayout(openNotificationHistoryButton, dismissNotificationButton);
    detailMenuLayout.setPadding(false);
    detailMenuLayout.setSpacing(false);
    detailMenuLayout.addClassNames(
        "cosmos-notification-detail-tools", COSMOS_THEME, REFERENCE_OVERLAY);
    detailActionsMenu.add(detailMenuLayout);
    notificationDetailDialog.add(detailActionsMenu);

    HorizontalLayout primaryActionsLayout = new HorizontalLayout(closeDetailButton);
    primaryActionsLayout.setPadding(false);
    primaryActionsLayout.setSpacing(false);
    primaryActionsLayout.setAlignItems(Alignment.CENTER);
    if (notificationDestinationButton != null) {
      notificationDestinationButton.setAriaLabel(notificationDestinationButton.getText());
      notificationDestinationButton.setTooltipText(notificationDestinationButton.getText());
      notificationDestinationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      notificationDestinationButton.addClassName("cosmos-notification-detail-destination");
      primaryActionsLayout.add(notificationDestinationButton);
    }
    primaryActionsLayout.addClassName("cosmos-notification-detail-primary");

    HorizontalLayout detailActionsLayout =
        new HorizontalLayout(moreActionsButton, primaryActionsLayout);
    detailActionsLayout.setPadding(false);
    detailActionsLayout.setSpacing(false);
    detailActionsLayout.setAlignItems(Alignment.CENTER);
    detailActionsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    detailActionsLayout.setWidthFull();
    detailActionsLayout.addClassNames(
        "cosmos-dialog-actions", "cosmos-notification-detail-actions");
    notificationDetailDialog.getFooter().add(detailActionsLayout);
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
