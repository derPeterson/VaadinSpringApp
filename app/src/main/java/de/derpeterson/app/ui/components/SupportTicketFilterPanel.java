package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.service.SupportTicketService.Ticket;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/** Filtert ausschließlich die bereits für den Adminzugang freigegebene Ticketliste. */
public final class SupportTicketFilterPanel extends AdminSidePanel {
  public enum Assignment {
    ALL,
    MINE,
    UNASSIGNED
  }

  private record Selection(Set<Status> statuses, Assignment assignment) implements Serializable {
    private Selection {
      statuses = Set.copyOf(statuses);
      Objects.requireNonNull(assignment);
    }
  }

  private static final Selection ALL = new Selection(Set.of(), Assignment.ALL);
  private final transient MessageProperties texts;
  private final ItemLabelGenerator<Status> statusLabels;
  private final CheckboxGroup<Status> statuses = new CheckboxGroup<>();
  private final RadioButtonGroup<Assignment> assignment = new RadioButtonGroup<>();
  private final Button reset = new Button(AppIcon.ROTATE_LEFT.create());
  private final Button cancel = new Button(AppIcon.CLOSE.create());
  private final Button apply = new Button(AppIcon.CHECK.create());
  private Selection applied = ALL;

  public SupportTicketFilterPanel(
      MessageProperties texts,
      ItemLabelGenerator<Status> statusLabels,
      SerializableRunnable onApply) {
    super(texts, AppIcon.FILTER);
    this.texts = texts;
    this.statusLabels = statusLabels;
    setId("support-filter-panel");
    statuses.setId("support-filter-status");
    statuses.setItems(Status.values());
    statuses.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
    statuses.setWidthFull();
    assignment.setId("support-filter-assignment");
    assignment.setItems(Assignment.values());
    assignment.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    assignment.setValue(Assignment.ALL);
    assignment.setWidthFull();
    VerticalLayout fields = new VerticalLayout(statuses, assignment);
    fields.setPadding(false);
    fields.setWidthFull();
    add(fields);
    reset.setId("support-filter-reset");
    reset.addClickListener(
        event -> {
          statuses.clear();
          assignment.setValue(Assignment.ALL);
        });
    HorizontalLayout tools = new HorizontalLayout(reset);
    tools.setPadding(false);
    tools.setSpacing(false);
    tools.setWidthFull();
    tools.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
    fields.addComponentAsFirst(tools);
    cancel.setId("support-filter-cancel");
    cancel.addClickListener(event -> close());
    apply.setId("support-filter-apply");
    apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    apply.addClickListener(
        event -> {
          applied = new Selection(statuses.getValue(), assignment.getValue());
          onApply.run();
          close();
        });
    HorizontalLayout actions = new HorizontalLayout(cancel, apply);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.setWidthFull();
    actions.addClassName("cosmos-dialog-actions");
    getFooter().add(actions);
    refreshTexts();
  }

  public void openWithAppliedFilters() {
    statuses.setValue(applied.statuses());
    assignment.setValue(applied.assignment());
    open();
  }

  /** Eine ausdrücklich geöffnete Benachrichtigung darf nicht am Listenfilter scheitern. */
  public void resetApplied() {
    applied = ALL;
  }

  public int activeCount() {
    return (applied.statuses().isEmpty() ? 0 : 1)
        + (applied.assignment() == Assignment.ALL ? 0 : 1);
  }

  public boolean matches(Ticket ticket, Long administratorId) {
    if (!applied.statuses().isEmpty() && !applied.statuses().contains(ticket.status())) {
      return false;
    }
    return switch (applied.assignment()) {
      case ALL -> true;
      case MINE -> administratorId != null && administratorId.equals(ticket.assignee());
      case UNASSIGNED -> ticket.assignee() == null;
    };
  }

  public void refreshTexts() {
    setHeading(texts.getAdminUsersFilterPanelTitle());
    setDescription(texts.getSupportFilterDescription());
    refreshCloseText(texts);
    statuses.setLabel(texts.getSupportFilterStatus());
    statuses.setHelperText(texts.getSupportFilterStatusHint());
    statuses.setItemLabelGenerator(statusLabels);
    assignment.setLabel(texts.getSupportFilterAssignment());
    assignment.setItemLabelGenerator(
        value ->
            switch (value) {
              case ALL -> texts.getAdminUsersFilterAll();
              case MINE -> texts.getSupportFilterMine();
              case UNASSIGNED -> texts.getSupportUnassigned();
            });
    reset.setText(texts.getAdminUsersClearFilters());
    cancel.setText(texts.getAdminUsersCancel());
    apply.setText(texts.getAdminUsersApplyFilters());
  }
}
