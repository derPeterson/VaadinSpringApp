package de.derpeterson.app.views.admin;

import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.ui.components.AdminSidePanelDialog;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosSegmentedTabs;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Filterentwurf im Overlay; Anwenden übernimmt, Abbrechen verwirft die Eingaben. */
final class AdminUserFilterBar extends VerticalLayout {
  private final MessageProperties messageProperties;
  private final TextField userSearchField;
  private final FilterControls filterControls;
  private final Button clearFiltersButton = new Button();
  private final Button filterButton = new Button();
  private final Button applyFiltersButton = new Button();
  private final Button cancelFilterChangesButton = new Button();
  private final Badge activeFilterCountBadge = new Badge();
  private final Span filterHintSpan = new Span();
  private final AdminSidePanelDialog filterSidePanel;
  private int activeFilterCount;
  private boolean wereFiltersApplied;
  private SerializableRunnable restoreFilterDraftAction = () -> {};

  record FilterControls(
      CosmosSegmentedTabs<AdminUserFilters.Enabled> enabled,
      CheckboxGroup<UserStatus> status,
      CheckboxGroup<RoleType> role,
      AdminUserActivityFilter activity)
      implements java.io.Serializable {}

  AdminUserFilterBar(
      MessageProperties messageProperties,
      TextField userSearchField,
      Button refreshUsersButton,
      Button addUserButton,
      FilterControls filterControls,
      SerializableRunnable applyFiltersAction) {
    this.messageProperties = messageProperties;
    this.userSearchField = userSearchField;
    this.filterControls = filterControls;

    filterControls.enabled().setItems(AdminUserFilters.Enabled.values());
    filterControls.enabled().setValue(AdminUserFilters.Enabled.ALL);
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("admin-user-filter-bar");

    filterSidePanel = new AdminSidePanelDialog(messageProperties, AppIcon.FILTER);
    filterSidePanel.setId("admin-user-filter-panel");
    filterSidePanel.addClassName("admin-user-filter-drawer");

    filterButton.setPrefixComponent(AppIcon.FILTER.create());
    filterButton.addClassName("admin-user-filter-button");
    filterButton.addClickListener(_ -> openFilterDraft());

    filterSidePanel.addOpenedChangeListener(filterOpenedChangeEvent -> {
      filterButton
          .getElement()
          .setAttribute("aria-expanded", Boolean.toString(filterOpenedChangeEvent.isOpened()));
      if (!filterOpenedChangeEvent.isOpened() && !wereFiltersApplied) {
        restoreFilterDraftAction.run();
      }
    });
    filterButton.getElement().setAttribute("aria-controls", "admin-user-filter-panel");

    activeFilterCountBadge.addClassName("admin-user-active-filter-count");
    clearFiltersButton.setPrefixComponent(AppIcon.ROTATE_LEFT.create());
    clearFiltersButton.addClassNames(DELETE_BUTTON, "admin-user-filter-reset");
    clearFiltersButton.addClickListener(_ -> clearAllFilters());

    Div filterToolsDiv = new Div(activeFilterCountBadge, clearFiltersButton);
    filterToolsDiv.addClassName("admin-filter-tools");
    Div filterFieldsDiv =
        new Div(
            userSearchField,
            filterControls.enabled(),
            filterControls.status(),
            filterControls.role(),
            filterControls.activity());
    filterFieldsDiv.addClassName("admin-user-filters");
    Div filterHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), filterHintSpan);
    filterHintDiv.addClassName("admin-filter-hint");
    filterSidePanel.add(filterToolsDiv, filterFieldsDiv, filterHintDiv);

    cancelFilterChangesButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelFilterChangesButton.addClickListener(_ -> filterSidePanel.close());

    applyFiltersButton.setPrefixComponent(AppIcon.CHECK.create());
    applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyFiltersButton.addClickListener(_ -> {
      if (filterControls.activity().isInvalid()) {
        return;
      }
      applyFiltersAction.run();
      wereFiltersApplied = true;
      filterSidePanel.close();
    });
    Div filterFooterDiv = new Div(cancelFilterChangesButton, applyFiltersButton);
    filterFooterDiv.addClassName("admin-drawer-actions");
    filterSidePanel.getFooter().add(filterFooterDiv);

    Div toolbarActionsDiv = new Div(refreshUsersButton, addUserButton);
    toolbarActionsDiv.addClassName("admin-user-toolbar-actions");
    Div filterToolbarDiv = new Div(filterButton, toolbarActionsDiv);
    filterToolbarDiv.addClassName("admin-user-toolbar");
    add(filterToolbarDiv, filterSidePanel);

    userSearchField.addValueChangeListener(_ -> refreshDraftCount());
    filterControls.enabled().addValueChangeListener(_ -> refreshDraftCount());
    filterControls.status().addValueChangeListener(_ -> refreshDraftCount());
    filterControls.role().addValueChangeListener(_ -> refreshDraftCount());
    filterControls.activity().addValueChangeListener(_ -> refreshDraftCount());
    refreshTexts();
  }

  private void openFilterDraft() {
    String previousSearchText = userSearchField.getValue();
    AdminUserFilters.Enabled previousEnabledFilter = filterControls.enabled().getValue();
    Set<UserStatus> previousUserStatuses = filterControls.status().getValue();
    Set<RoleType> previousRoles = filterControls.role().getValue();
    LocalDateTime previousActivityDateTime = filterControls.activity().getValue();

    restoreFilterDraftAction =
        () -> {
          userSearchField.setValue(previousSearchText);
          filterControls.enabled().setValue(previousEnabledFilter);
          filterControls.status().setValue(previousUserStatuses);
          filterControls.role().setValue(previousRoles);
          filterControls.activity().setValue(previousActivityDateTime);
        };

    wereFiltersApplied = false;
    filterControls.activity().refreshActivityTimePlaceholder();
    filterSidePanel.open();
  }

  void refreshTexts() {
    filterButton.setText(messageProperties.getAdminUsersAddFilter());
    filterSidePanel.setHeading(messageProperties.getAdminUsersFilterPanelTitle());
    filterSidePanel.setDescription(messageProperties.getAdminUsersFilterPanelDescription());
    filterSidePanel.refreshCloseText(messageProperties);
    activeFilterCountBadge.setText(
        messageProperties.getAdminUsersActiveFilterCount(activeFilterCount));
    applyFiltersButton.setText(messageProperties.getAdminUsersApplyFilters());
    cancelFilterChangesButton.setText(messageProperties.getAdminUsersCancel());
    filterHintSpan.setText(messageProperties.getAdminUsersFilterTimeHint());
    clearFiltersButton.setText(messageProperties.getAdminUsersClearFilters());
    userSearchField.setLabel(messageProperties.getAdminUsersSearchLabel());
  }

  void showActiveCount(int activeGroupCount) {
    activeFilterCount = activeGroupCount;
    activeFilterCountBadge.setText(
        messageProperties.getAdminUsersActiveFilterCount(activeGroupCount));
  }

  private void refreshDraftCount() {
    int activeGroupCount =
        AdminUserFilters.countActiveGroups(
            userSearchField.getValue(),
            filterControls.enabled().getValue().value(),
            filterControls.status().getValue(),
            filterControls.role().getValue(),
            !filterControls.activity().isEmpty());
    showActiveCount(activeGroupCount);
  }

  private void clearAllFilters() {
    userSearchField.clear();
    filterControls.enabled().setValue(AdminUserFilters.Enabled.ALL);
    for (HasValue<?, ?> filterInput :
        List.of(filterControls.status(), filterControls.role(), filterControls.activity())) {
      filterInput.clear();
    }
  }
}
