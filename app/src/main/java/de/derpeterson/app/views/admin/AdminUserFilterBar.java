package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.AppIcon;
import java.util.List;

/** Filterentwurf im Overlay; Anwenden übernimmt, Abbrechen verwirft die Eingaben. */
final class AdminUserFilterBar extends VerticalLayout {
  private final MessageProperties texts;
  private final TextField search;
  private final FilterControls controls;
  private final Button clearFilters = new Button();
  private final Button filterButton = new Button();
  private final Button applyFiltersButton = new Button();
  private final Button cancel = new Button();
  private final Span activeFilterCountBadge = new Span();
  private final Span filterHint = new Span();
  private final AdminSidePanel panel;
  private int activeFilterCount;
  private boolean applied;
  private SerializableRunnable restoreDraft = () -> {};

  record FilterControls(
      RadioButtonGroup<AdminUserFilters.Enabled> enabled,
      CheckboxGroup<UserStatus> status,
      CheckboxGroup<RoleType> role,
      AdminUserActivityFilter activity)
      implements java.io.Serializable {}

  AdminUserFilterBar(
      MessageProperties texts,
      TextField search,
      Button refresh,
      Button addUser,
      FilterControls controls,
      SerializableRunnable applyFilters) {
    this.texts = texts;
    this.search = search;
    this.controls = controls;
    controls.enabled().setItems(AdminUserFilters.Enabled.values());
    controls.enabled().setValue(AdminUserFilters.Enabled.ALL);
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("admin-user-filter-bar");
    panel = new AdminSidePanel(texts, AppIcon.FILTER);
    panel.setId("admin-user-filter-panel");
    panel.addClassName("admin-user-filter-drawer");
    filterButton.setPrefixComponent(AppIcon.FILTER.create());
    filterButton.addClassName("admin-user-filter-button");
    filterButton.addClickListener(
        event -> {
          String query = search.getValue();
          AdminUserFilters.Enabled enabled = controls.enabled().getValue();
          java.util.Set<UserStatus> statuses = controls.status().getValue();
          java.util.Set<RoleType> roles = controls.role().getValue();
          java.time.LocalDateTime since = controls.activity().getValue();
          restoreDraft =
              () -> {
                search.setValue(query);
                controls.enabled().setValue(enabled);
                controls.status().setValue(statuses);
                controls.role().setValue(roles);
                controls.activity().setValue(since);
              };
          applied = false;
          controls.activity().refreshPlaceholders();
          panel.open();
        });
    panel.addOpenedChangeListener(
        event -> {
          filterButton
              .getElement()
              .setAttribute("aria-expanded", Boolean.toString(event.isOpened()));
          if (!event.isOpened() && !applied) {
            restoreDraft.run();
          }
        });
    filterButton.getElement().setAttribute("aria-controls", "admin-user-filter-panel");
    activeFilterCountBadge.addClassName("admin-user-active-filter-count");
    clearFilters.setPrefixComponent(AppIcon.ROTATE_LEFT.create());
    clearFilters.addClassNames("cosmos-delete-button", "admin-user-filter-reset");
    clearFilters.addClickListener(event -> clearAllFilters());
    Div tools = new Div(activeFilterCountBadge, clearFilters);
    tools.addClassName("admin-filter-tools");
    Div fields =
        new Div(
            search, controls.enabled(), controls.status(), controls.role(), controls.activity());
    fields.addClassName("admin-user-filters");
    Div hint = new Div(AppIcon.INFO_CIRCLE.create(), filterHint);
    hint.addClassName("admin-filter-hint");
    panel.add(tools, fields, hint);
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> panel.close());
    applyFiltersButton.setPrefixComponent(AppIcon.CHECK.create());
    applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyFiltersButton.addClickListener(
        event -> {
          if (controls.activity().isInvalid()) {
            return;
          }
          applyFilters.run();
          applied = true;
          panel.close();
        });
    Div footer = new Div(cancel, applyFiltersButton);
    footer.addClassName("admin-drawer-actions");
    panel.getFooter().add(footer);
    Div actions = new Div(refresh, addUser);
    actions.addClassName("admin-user-toolbar-actions");
    Div toolbar = new Div(filterButton, actions);
    toolbar.addClassName("admin-user-toolbar");
    add(toolbar, panel);
    search.addValueChangeListener(event -> refreshDraftCount());
    controls.enabled().addValueChangeListener(event -> refreshDraftCount());
    controls.status().addValueChangeListener(event -> refreshDraftCount());
    controls.role().addValueChangeListener(event -> refreshDraftCount());
    controls.activity().addValueChangeListener(event -> refreshDraftCount());
    refreshTexts();
  }

  void refreshTexts() {
    filterButton.setText(texts.getAdminUsersAddFilter());
    panel.setHeading(texts.getAdminUsersFilterPanelTitle());
    panel.setDescription(texts.getAdminUsersFilterPanelDescription());
    panel.refreshCloseText(texts);
    activeFilterCountBadge.setText(texts.getAdminUsersActiveFilterCount(activeFilterCount));
    applyFiltersButton.setText(texts.getAdminUsersApplyFilters());
    cancel.setText(texts.getAdminUsersCancel());
    filterHint.setText(texts.getAdminUsersFilterTimeHint());
    clearFilters.setText(texts.getAdminUsersClearFilters());
    search.setLabel(texts.getAdminUsersSearchLabel());
  }

  void showActiveCount(int count) {
    activeFilterCount = count;
    activeFilterCountBadge.setText(texts.getAdminUsersActiveFilterCount(count));
  }

  private void refreshDraftCount() {
    int count =
        (search.isEmpty() ? 0 : 1)
            + (controls.enabled().getValue() == AdminUserFilters.Enabled.ALL ? 0 : 1)
            + controls.status().getValue().size()
            + controls.role().getValue().size()
            + (controls.activity().isEmpty() ? 0 : 1);
    showActiveCount(count);
  }

  private void clearAllFilters() {
    search.clear();
    controls.enabled().setValue(AdminUserFilters.Enabled.ALL);
    for (HasValue<?, ?> field : List.of(controls.status(), controls.role(), controls.activity())) {
      field.clear();
    }
  }
}
