package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
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
  private final transient MessageProperties texts;
  private final ItemLabelGenerator<Status> statusLabels;
  private final CheckboxGroup<Status> statuses = new CheckboxGroup<>();
  private final RadioButtonGroup<Assignment> assignment = new RadioButtonGroup<>();
  private final RadioButtonGroup<Reading> reading = new RadioButtonGroup<>();
  private final Badge draftCount = new Badge();
  private final TextField ticketNumber = new TextField();
  private final Button jump = new Button(AppIcon.OPEN_SECTION.create());
  private final Button reset = new Button(AppIcon.ROTATE_LEFT.create());
  private final Button cancel = new Button(AppIcon.CLOSE.create());
  private final Button apply = new Button(AppIcon.CHECK.create());
  private SupportTicketFilter applied = SupportTicketFilter.ALL;

  public SupportTicketFilterPanel(
      MessageProperties texts,
      ItemLabelGenerator<Status> statusLabels,
      SerializableRunnable onApply,
      SerializablePredicate<String> onJump) {
    super(texts, AppIcon.FILTER);
    this.texts = texts;
    this.statusLabels = statusLabels;
    setId("support-filter-panel");
    addClassNames("admin-user-filter-drawer", "support-filter-drawer");
    statuses.setId("support-filter-status");
    statuses.setItems(Status.values());
    statuses.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    statuses.setWidthFull();
    statuses.addClassName("admin-filter-choices");
    assignment.setId("support-filter-assignment");
    assignment.setItems(Assignment.values());
    assignment.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    assignment.setValue(Assignment.ALL);
    assignment.setWidthFull();
    reading.setId("support-filter-reading");
    reading.setItems(Reading.values());
    reading.setValue(Reading.ALL);
    reading.addClassNames("admin-filter-enabled", "support-filter-reading");
    reading.setWidthFull();
    VerticalLayout fields = new VerticalLayout(statuses, assignment, reading);
    fields.setPadding(false);
    fields.setSpacing(false);
    fields.setWidthFull();
    fields.addClassName("admin-user-filters");
    reset.setId("support-filter-reset");
    reset.addClickListener(
        event -> {
          statuses.clear();
          assignment.setValue(Assignment.ALL);
          reading.setValue(Reading.ALL);
        });
    reset.addClassNames(CosmosClasses.DELETE_BUTTON, "admin-user-filter-reset");
    draftCount.addClassName("admin-user-active-filter-count");
    draftCount.setId("support-filter-draft-count");
    HorizontalLayout tools = new HorizontalLayout(draftCount, reset);
    tools.setPadding(false);
    tools.setSpacing(false);
    tools.setWidthFull();
    tools.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
    tools.addClassName("admin-filter-tools");
    statuses.addValueChangeListener(event -> refreshDraftCount());
    assignment.addValueChangeListener(event -> refreshDraftCount());
    reading.addValueChangeListener(event -> refreshDraftCount());
    ticketNumber.setId("support-jump-number");
    ticketNumber.setWidthFull();
    ticketNumber.setAllowedCharPattern("[0-9]");
    ticketNumber.getElement().setAttribute("inputmode", "numeric");
    ticketNumber.setMaxLength(19);
    ticketNumber.setManualValidation(true);
    jump.setId("support-jump");
    jump.addClickListener(
        event -> {
          if (ticketNumber.isEmpty()) {
            ticketNumber.setInvalid(true);
            NotificationHelper.getInstance()
                .showNotification(
                    texts::getBaseFailedTitle,
                    texts::getBaseValidationRequiredMessage,
                    NotificationHelper.NotificationType.ERROR);
            ticketNumber.focus();
            return;
          }
          ticketNumber.setInvalid(false);
          if (onJump.test(ticketNumber.getValue())) {
            close();
          }
        });
    HorizontalLayout jumpRow = new HorizontalLayout(ticketNumber, jump);
    jumpRow.setPadding(false);
    jumpRow.setSpacing(false);
    jumpRow.setWidthFull();
    jumpRow.setAlignItems(HorizontalLayout.Alignment.END);
    jumpRow.setFlexGrow(1, ticketNumber);
    jumpRow.addClassName("support-filter-jump");
    add(tools, fields, jumpRow);
    cancel.setId("support-filter-cancel");
    cancel.addClickListener(event -> close());
    apply.setId("support-filter-apply");
    apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    apply.addClickListener(
        event -> {
          applied =
              new SupportTicketFilter(
                  statuses.getValue(), assignment.getValue(), reading.getValue(), null);
          onApply.run();
          close();
        });
    HorizontalLayout actions = new HorizontalLayout(cancel, apply);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.setWidthFull();
    actions.addClassName("admin-drawer-actions");
    getFooter().add(actions);
    refreshTexts();
  }

  public void openWithAppliedFilters() {
    statuses.setValue(applied.statuses());
    assignment.setValue(applied.assignment());
    reading.setValue(applied.reading());
    ticketNumber.clear();
    ticketNumber.setInvalid(false);
    open();
  }

  /** Eine ausdrücklich geöffnete Benachrichtigung darf nicht am Listenfilter scheitern. */
  public void resetApplied() {
    applied = SupportTicketFilter.ALL;
  }

  public SupportTicketFilter appliedFilter() {
    return applied;
  }

  public int activeCount() {
    return (applied.statuses().isEmpty() ? 0 : 1)
        + (applied.assignment() == Assignment.ALL ? 0 : 1)
        + (applied.reading() == Reading.ALL ? 0 : 1);
  }

  public boolean matches(Ticket ticket, Long administratorId) {
    if (applied.reading() == Reading.UNREAD && ticket.unreadMessages() == 0) {
      return false;
    }
    return matchesMetadata(ticket, administratorId);
  }

  /** Ein bereits geöffneter Vorgang bleibt nach dem Lesen im Detailbereich erreichbar. */
  public boolean matchesMetadata(Ticket ticket, Long administratorId) {
    return applied.matchesMetadata(ticket, administratorId);
  }

  public void refreshTexts() {
    setHeading(texts.getAdminUsersFilterPanelTitle());
    setDescription(texts.getSupportFilterDescription());
    refreshCloseText(texts);
    statuses.setLabel(texts.getSupportFilterStatus());
    statuses.setItemLabelGenerator(statusLabels);
    assignment.setLabel(texts.getSupportFilterAssignment());
    assignment.setItemLabelGenerator(
        value ->
            switch (value) {
              case ALL -> texts.getAdminUsersFilterAll();
              case MINE -> texts.getSupportFilterMine();
              case UNASSIGNED -> texts.getSupportUnassigned();
            });
    reading.setLabel(texts.getSupportFilterMessages());
    reading.setItemLabelGenerator(
        value ->
            value == Reading.ALL ? texts.getAdminUsersFilterAll() : texts.getNotificationsUnread());
    ticketNumber.setLabel(texts.getSupportJumpTicketNumber());
    jump.setText(texts.getSupportJumpOpen());
    refreshDraftCount();
    reset.setText(texts.getAdminUsersClearFilters());
    cancel.setText(texts.getAdminUsersCancel());
    apply.setText(texts.getAdminUsersApplyFilters());
  }

  private void refreshDraftCount() {
    int count =
        (statuses.isEmpty() ? 0 : 1)
            + (assignment.getValue() == Assignment.ALL ? 0 : 1)
            + (reading.getValue() == Reading.ALL ? 0 : 1);
    draftCount.setText(texts.getAdminUsersActiveFilterCount(count));
  }
}
