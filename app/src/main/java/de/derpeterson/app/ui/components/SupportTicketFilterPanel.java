package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.service.SupportTicketFilter;
import de.derpeterson.app.service.SupportTicketFilter.Assignment;
import de.derpeterson.app.service.SupportTicketFilter.Reading;
import de.derpeterson.app.service.SupportTicketService.Ticket;

/** Nativer Filterentwurf; erst Anwenden übernimmt ihn in die berechtigte Datenbankabfrage. */
public final class SupportTicketFilterPanel extends AdminSidePanelDialog {
  private final transient MessageProperties messageProperties;
  private final ItemLabelGenerator<Status> statusLabels;
  private final CheckboxGroup<Status> statusFilter = new CheckboxGroup<>();
  private final RadioButtonGroup<Assignment> assignmentFilter = new RadioButtonGroup<>();
  private final CosmosSegmentedTabs<Reading> readingFilterTabs = new CosmosSegmentedTabs<>();
  private final Badge draftFilterCountBadge = new Badge();
  private final TextField ticketNumberField = new TextField();
  private final Span ticketJumpHintSpan = new Span();
  private final Button openTicketButton = new Button(AppIcon.OPEN_SECTION.create());
  private final Button resetFiltersButton = new Button(AppIcon.ROTATE_LEFT.create());
  private final Button cancelFilterChangesButton = new Button(AppIcon.CLOSE.create());
  private final Button applyFiltersButton = new Button(AppIcon.CHECK.create());
  private SupportTicketFilter appliedTicketFilter = SupportTicketFilter.ALL;

  public SupportTicketFilterPanel(
      MessageProperties messageProperties,
      ItemLabelGenerator<Status> statusLabels,
      SerializableRunnable applyTicketFilterAction,
      SerializablePredicate<String> openTicketByNumber) {
    super(messageProperties, AppIcon.FILTER);
    this.messageProperties = messageProperties;
    this.statusLabels = statusLabels;
    setId("support-filter-panel");
    addClassNames("admin-user-filter-drawer", "support-filter-drawer");
    statusFilter.setId("support-filter-status");
    statusFilter.setItems(Status.values());
    statusFilter.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    statusFilter.setWidthFull();
    statusFilter.addClassName("admin-filter-choices");
    assignmentFilter.setId("support-filter-assignment");
    assignmentFilter.setItems(Assignment.values());
    assignmentFilter.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    assignmentFilter.setValue(Assignment.ALL);
    assignmentFilter.setWidthFull();
    readingFilterTabs.setId("support-filter-reading");
    readingFilterTabs.setItems(Reading.values());
    readingFilterTabs.setValue(Reading.ALL);
    readingFilterTabs.addClassNames("admin-filter-enabled", "support-filter-reading");
    readingFilterTabs.setWidthFull();

    VerticalLayout filterFieldsLayout =
        new VerticalLayout(readingFilterTabs, statusFilter, assignmentFilter);
    filterFieldsLayout.setPadding(false);
    filterFieldsLayout.setSpacing(false);
    filterFieldsLayout.setWidthFull();
    filterFieldsLayout.addClassName("admin-user-filters");
    resetFiltersButton.setId("support-filter-reset");
    resetFiltersButton.addClickListener(event -> {
      statusFilter.clear();
      assignmentFilter.setValue(Assignment.ALL);
      readingFilterTabs.setValue(Reading.ALL);
    });
    resetFiltersButton.addClassNames(CosmosCssClasses.DELETE_BUTTON, "admin-user-filter-reset");
    draftFilterCountBadge.addClassName("admin-user-active-filter-count");
    draftFilterCountBadge.setId("support-filter-draft-count");

    HorizontalLayout filterToolsLayout =
        new HorizontalLayout(draftFilterCountBadge, resetFiltersButton);
    filterToolsLayout.setPadding(false);
    filterToolsLayout.setSpacing(false);
    filterToolsLayout.setWidthFull();
    filterToolsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    filterToolsLayout.addClassName("admin-filter-tools");
    statusFilter.addValueChangeListener(event -> refreshDraftFilterCountBadge());
    assignmentFilter.addValueChangeListener(event -> refreshDraftFilterCountBadge());
    readingFilterTabs.addValueChangeListener(event -> refreshDraftFilterCountBadge());
    ticketNumberField.setId("support-jump-number");
    ticketNumberField.setWidthFull();
    ticketNumberField.setAllowedCharPattern("[0-9#]");
    ticketNumberField.setMaxLength(20);
    ticketNumberField.setManualValidation(true);
    openTicketButton.setId("support-jump");
    openTicketButton.addClickListener(event -> {
      if (ticketNumberField.isEmpty()) {
        ticketNumberField.setInvalid(true);
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseFailedTitle,
                messageProperties::getBaseValidationRequiredMessage,
                NotificationHelper.NotificationType.ERROR);
        ticketNumberField.focus();
        return;
      }
      ticketNumberField.setInvalid(false);
      if (openTicketByNumber.test(ticketNumberField.getValue())) {
        close();
      }
    });

    HorizontalLayout ticketJumpRowLayout =
        new HorizontalLayout(ticketNumberField, openTicketButton);
    ticketJumpRowLayout.setPadding(false);
    ticketJumpRowLayout.setSpacing(false);
    ticketJumpRowLayout.setWidthFull();
    ticketJumpRowLayout.setAlignItems(Alignment.END);
    ticketJumpRowLayout.setFlexGrow(1, ticketNumberField);
    ticketJumpRowLayout.addClassName("support-filter-jump");

    Div ticketJumpInfoDiv = new Div(AppIcon.INFO_CIRCLE.create(), ticketJumpHintSpan);
    ticketJumpInfoDiv.setId("support-jump-info");
    ticketJumpInfoDiv.addClassName("admin-filter-hint");
    add(filterToolsLayout, filterFieldsLayout, ticketJumpRowLayout, ticketJumpInfoDiv);
    cancelFilterChangesButton.setId("support-filter-cancel");
    cancelFilterChangesButton.addClickListener(event -> close());
    applyFiltersButton.setId("support-filter-apply");
    applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyFiltersButton.addClickListener(event -> {
      appliedTicketFilter =
          new SupportTicketFilter(
              statusFilter.getValue(),
              assignmentFilter.getValue(),
              readingFilterTabs.getValue(),
              null);
      applyTicketFilterAction.run();
      close();
    });

    HorizontalLayout filterActionsLayout =
        new HorizontalLayout(cancelFilterChangesButton, applyFiltersButton);
    filterActionsLayout.setPadding(false);
    filterActionsLayout.setSpacing(false);
    filterActionsLayout.setWidthFull();
    filterActionsLayout.addClassName("admin-drawer-actions");
    getFooter().add(filterActionsLayout);
    refreshTexts();
  }

  public void openWithAppliedFilters() {
    statusFilter.setValue(appliedTicketFilter.statuses());
    assignmentFilter.setValue(appliedTicketFilter.assignment());
    readingFilterTabs.setValue(appliedTicketFilter.reading());
    ticketNumberField.clear();
    ticketNumberField.setInvalid(false);
    open();
  }

  /** Eine ausdrücklich geöffnete Benachrichtigung darf nicht am Listenfilter scheitern. */
  public void resetApplied() {
    appliedTicketFilter = SupportTicketFilter.ALL;
  }

  public SupportTicketFilter appliedFilter() {
    return appliedTicketFilter;
  }

  public int activeCount() {
    return (appliedTicketFilter.statuses().isEmpty() ? 0 : 1)
        + (appliedTicketFilter.assignment() == Assignment.ALL ? 0 : 1)
        + (appliedTicketFilter.reading() == Reading.ALL ? 0 : 1);
  }

  public boolean matches(Ticket ticket, Long administratorId) {
    if (appliedTicketFilter.reading() == Reading.UNREAD && ticket.unreadMessages() == 0) {
      return false;
    }

    return matchesMetadata(ticket, administratorId);
  }

  /** Ein bereits geöffneter Vorgang bleibt nach dem Lesen im Detailbereich erreichbar. */
  public boolean matchesMetadata(Ticket ticket, Long administratorId) {
    return appliedTicketFilter.matchesMetadata(ticket, administratorId);
  }

  public void refreshTexts() {
    setHeading(messageProperties.getAdminUsersFilterPanelTitle());
    setDescription(messageProperties.getSupportFilterDescription());
    refreshCloseText(messageProperties);
    statusFilter.setLabel(messageProperties.getSupportFilterStatus());
    statusFilter.setItemLabelGenerator(statusLabels);
    statusFilter.setRenderer(
        new ComponentRenderer<>(status -> {
          Span label = new Span(statusLabels.apply(status));
          label.getElement().setAttribute("data-status", status.name());
          return label;
        }));
    assignmentFilter.setLabel(messageProperties.getSupportFilterAssignment());
    assignmentFilter.setItemLabelGenerator(value ->
        switch (value) {
          case ALL -> messageProperties.getAdminUsersFilterAll();
          case MINE -> messageProperties.getSupportFilterMine();
          case UNASSIGNED -> messageProperties.getSupportUnassigned();
        });
    readingFilterTabs.setLabel(messageProperties.getSupportFilterMessages());
    readingFilterTabs.setItemLabelGenerator(value ->
        value == Reading.ALL
            ? messageProperties.getAdminUsersFilterAll()
            : messageProperties.getNotificationsUnread());
    ticketNumberField.setLabel(messageProperties.getSupportJumpTicketNumber());
    ticketNumberField.setPlaceholder(messageProperties.getSupportJumpTicketNumberPlaceholder());
    ticketJumpHintSpan.setText(messageProperties.getSupportJumpTicketNumberHint());
    openTicketButton.setText(messageProperties.getSupportJumpOpen());
    refreshDraftFilterCountBadge();
    resetFiltersButton.setText(messageProperties.getAdminUsersClearFilters());
    cancelFilterChangesButton.setText(messageProperties.getAdminUsersCancel());
    applyFiltersButton.setText(messageProperties.getAdminUsersApplyFilters());
  }

  private void refreshDraftFilterCountBadge() {
    int activeDraftFilterGroupCount =
        (statusFilter.isEmpty() ? 0 : 1)
            + (assignmentFilter.getValue() == Assignment.ALL ? 0 : 1)
            + (readingFilterTabs.getValue() == Reading.ALL ? 0 : 1);

    draftFilterCountBadge.setText(
        messageProperties.getAdminUsersActiveFilterCount(activeDraftFilterGroupCount));
  }
}
