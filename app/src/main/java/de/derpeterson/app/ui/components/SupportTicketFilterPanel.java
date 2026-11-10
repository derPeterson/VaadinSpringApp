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
public final class SupportTicketFilterPanel extends AdminSidePanel {
  private final transient MessageProperties messageProperties;
  private final ItemLabelGenerator<Status> statusLabels;
  private final CheckboxGroup<Status> statusFilter = new CheckboxGroup<>();
  private final RadioButtonGroup<Assignment> assignmentFilter = new RadioButtonGroup<>();
  private final RadioButtonGroup<Reading> readingFilter = new RadioButtonGroup<>();
  private final Badge draftFilterCountBadge = new Badge();
  private final TextField ticketNumberField = new TextField();
  private final Span jumpHint = new Span();
  private final Button openTicketButton = new Button(AppIcon.OPEN_SECTION.create());
  private final Button resetFiltersButton = new Button(AppIcon.ROTATE_LEFT.create());
  private final Button cancelFilterChangesButton = new Button(AppIcon.CLOSE.create());
  private final Button applyFiltersButton = new Button(AppIcon.CHECK.create());
  private SupportTicketFilter appliedTicketFilter = SupportTicketFilter.ALL;

  public SupportTicketFilterPanel(
      MessageProperties messageProperties,
      ItemLabelGenerator<Status> statusLabels,
      SerializableRunnable onApply,
      SerializablePredicate<String> onJump) {
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
    readingFilter.setId("support-filter-reading");
    readingFilter.setItems(Reading.values());
    readingFilter.setValue(Reading.ALL);
    readingFilter.addClassNames("admin-filter-enabled", "support-filter-reading");
    readingFilter.setWidthFull();
    VerticalLayout fields = new VerticalLayout(readingFilter, statusFilter, assignmentFilter);
    fields.setPadding(false);
    fields.setSpacing(false);
    fields.setWidthFull();
    fields.addClassName("admin-user-filters");
    resetFiltersButton.setId("support-filter-reset");
    resetFiltersButton.addClickListener(event -> {
      statusFilter.clear();
      assignmentFilter.setValue(Assignment.ALL);
      readingFilter.setValue(Reading.ALL);
    });
    resetFiltersButton.addClassNames(CosmosClasses.DELETE_BUTTON, "admin-user-filter-reset");
    draftFilterCountBadge.addClassName("admin-user-active-filter-count");
    draftFilterCountBadge.setId("support-filter-draft-count");
    HorizontalLayout tools = new HorizontalLayout(draftFilterCountBadge, resetFiltersButton);
    tools.setPadding(false);
    tools.setSpacing(false);
    tools.setWidthFull();
    tools.setJustifyContentMode(JustifyContentMode.BETWEEN);
    tools.addClassName("admin-filter-tools");
    statusFilter.addValueChangeListener(event -> refreshDraftCount());
    assignmentFilter.addValueChangeListener(event -> refreshDraftCount());
    readingFilter.addValueChangeListener(event -> refreshDraftCount());
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
      if (onJump.test(ticketNumberField.getValue())) {
        close();
      }
    });
    HorizontalLayout jumpRow = new HorizontalLayout(ticketNumberField, openTicketButton);
    jumpRow.setPadding(false);
    jumpRow.setSpacing(false);
    jumpRow.setWidthFull();
    jumpRow.setAlignItems(Alignment.END);
    jumpRow.setFlexGrow(1, ticketNumberField);
    jumpRow.addClassName("support-filter-jump");
    Div jumpInfo = new Div(AppIcon.INFO_CIRCLE.create(), jumpHint);
    jumpInfo.setId("support-jump-info");
    jumpInfo.addClassName("admin-filter-hint");
    add(tools, fields, jumpRow, jumpInfo);
    cancelFilterChangesButton.setId("support-filter-cancel");
    cancelFilterChangesButton.addClickListener(event -> close());
    applyFiltersButton.setId("support-filter-apply");
    applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyFiltersButton.addClickListener(event -> {
      appliedTicketFilter =
          new SupportTicketFilter(
              statusFilter.getValue(), assignmentFilter.getValue(), readingFilter.getValue(), null);
      onApply.run();
      close();
    });
    HorizontalLayout actions = new HorizontalLayout(cancelFilterChangesButton, applyFiltersButton);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.setWidthFull();
    actions.addClassName("admin-drawer-actions");
    getFooter().add(actions);
    refreshTexts();
  }

  public void openWithAppliedFilters() {
    statusFilter.setValue(appliedTicketFilter.statuses());
    assignmentFilter.setValue(appliedTicketFilter.assignment());
    readingFilter.setValue(appliedTicketFilter.reading());
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
    readingFilter.setLabel(messageProperties.getSupportFilterMessages());
    readingFilter.setItemLabelGenerator(value ->
        value == Reading.ALL
            ? messageProperties.getAdminUsersFilterAll()
            : messageProperties.getNotificationsUnread());
    ticketNumberField.setLabel(messageProperties.getSupportJumpTicketNumber());
    ticketNumberField.setPlaceholder(messageProperties.getSupportJumpTicketNumberPlaceholder());
    jumpHint.setText(messageProperties.getSupportJumpTicketNumberHint());
    openTicketButton.setText(messageProperties.getSupportJumpOpen());
    refreshDraftCount();
    resetFiltersButton.setText(messageProperties.getAdminUsersClearFilters());
    cancelFilterChangesButton.setText(messageProperties.getAdminUsersCancel());
    applyFiltersButton.setText(messageProperties.getAdminUsersApplyFilters());
  }

  private void refreshDraftCount() {
    int count =
        (statusFilter.isEmpty() ? 0 : 1)
            + (assignmentFilter.getValue() == Assignment.ALL ? 0 : 1)
            + (readingFilter.getValue() == Reading.ALL ? 0 : 1);
    draftFilterCountBadge.setText(messageProperties.getAdminUsersActiveFilterCount(count));
  }
}
