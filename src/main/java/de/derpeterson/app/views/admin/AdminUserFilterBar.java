package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Suche und Aktionen bleiben oben; alle Zusatzfilter werden gemeinsam aufgeklappt. */
final class AdminUserFilterBar extends VerticalLayout {
  private static final String ARIA_HIDDEN = "aria-hidden";

  private final MessageProperties texts;
  private final TextField search;
  private final Button clearFilters = new Button();
  private final Button filterButton = new Button();
  private final Button applyFiltersButton = new Button();
  private final Span filterHint = new Span();
  private final Span filterPanelTitle = new Span();
  private final Span activeFilterCountBadge = new Span();
  private int activeFilterCount;
  private final Div filters = new Div();
  private final Span activeFiltersLabel = new Span();
  private final Div activeFilterChips = new Div();
  private final Div activeFiltersRow = new Div(activeFiltersLabel, activeFilterChips);
  private final List<HasValue<?, ?>> controls = new ArrayList<>();

  AdminUserFilterBar(
      MessageProperties texts,
      TextField search,
      Button refresh,
      Button addUser,
      ComboBox<Boolean> enabled,
      ComboBox<UserStatus> status,
      ComboBox<RoleType> role,
      AdminUserActivityFilter activity,
      SerializableRunnable applyFilters) {
    this.texts = texts;
    this.search = search;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("admin-user-filter-bar");

    Icon filterIcon = VaadinIcon.FILTER.create();
    filterIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Icon filterToggle = LumoIcon.ANGLE_DOWN.create();
    filterToggle.addClassName("admin-user-filter-toggle");
    filterToggle.getElement().setAttribute(ARIA_HIDDEN, "true");
    filterButton.addClassName("admin-user-filter-button");
    filterButton.setPrefixComponent(filterIcon);
    filterButton.setSuffixComponent(filterToggle);
    filters.addClassName("admin-user-filters");
    applyFiltersButton.setPrefixComponent(VaadinIcon.CHECK.create());
    applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    applyFiltersButton.addClickListener(event -> applyFilters.run());
    Icon hintIcon = VaadinIcon.INFO_CIRCLE.create();
    hintIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div hint = new Div(hintIcon, filterHint);
    hint.addClassName("cosmos-panel-hint");
    Div filterActions = new Div(applyFiltersButton);
    filterActions.addClassName("cosmos-panel-actions");
    Div filterFooter = new Div(hint, filterActions);
    filterFooter.addClassNames("cosmos-panel-footer", "admin-user-filter-footer");
    Icon panelIcon = VaadinIcon.FILTER.create();
    panelIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    activeFilterCountBadge.getElement().getThemeList().add("badge");
    activeFilterCountBadge.addClassName("admin-user-active-filter-count");
    Div panelHeading = new Div(panelIcon, filterPanelTitle, activeFilterCountBadge);
    panelHeading.addClassName("admin-user-filter-panel-heading");
    Div panelHeader = new Div(panelHeading, clearFilters);
    panelHeader.addClassName("admin-user-filter-panel-header");
    Div filterPanel = new Div(panelHeader, filters, filterFooter);
    filterPanel.setId("admin-user-filter-panel");
    Details filterDetails = new Details("", filterPanel);
    filterDetails.addClassName("admin-user-filter-details");
    filterDetails.setWidthFull();
    filterDetails.setOpened(false);
    filterButton.getElement().setAttribute("aria-controls", "admin-user-filter-panel");
    filterButton.getElement().setAttribute("aria-expanded", "false");
    filterButton.addClickListener(event -> filterDetails.setOpened(!filterDetails.isOpened()));
    filterDetails.addOpenedChangeListener(
        event -> {
          filterButton
              .getElement()
              .setAttribute("aria-expanded", Boolean.toString(event.isOpened()));
          if (event.isOpened()) {
            activity.refreshPlaceholders();
          }
        });

    clearFilters.setPrefixComponent(VaadinIcon.ROTATE_LEFT.create());
    clearFilters.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    clearFilters.addClassName("admin-user-filter-reset");
    clearFilters.addClickListener(
        event -> {
          clearAllFilters();
          applyFilters.run();
        });

    Div actions = new Div(refresh, addUser);
    actions.addClassName("admin-user-toolbar-actions");
    Div toolbar = new Div(search, filterButton, actions);
    toolbar.addClassName("admin-user-toolbar");
    activeFiltersLabel.addClassName("cosmos-panel-meta");
    activeFilterChips.addClassName("admin-user-active-filter-chips");
    activeFiltersRow.addClassName("admin-user-active-filters");
    activeFiltersRow.setVisible(false);
    add(toolbar, activeFiltersRow, filterDetails);

    filters.add(enabled, status, role, activity);
    controls.addAll(List.of(enabled, status, role, activity));
    refreshTexts();
  }

  void refreshTexts() {
    filterButton.setText(texts.getAdminUsersAddFilter());
    filterPanelTitle.setText(texts.getAdminUsersFilterPanelTitle());
    activeFilterCountBadge.setText(texts.getAdminUsersActiveFilterCount(activeFilterCount));
    applyFiltersButton.setText(texts.getAdminUsersApplyFilters());
    filterHint.setText(texts.getAdminUsersFilterTimeHint());
    activeFiltersLabel.setText(texts.getAdminUsersActiveFilters());
    clearFilters.setText(texts.getAdminUsersClearFilters());
  }

  void showActiveFilters(List<ActiveFilter> activeFilters) {
    activeFilterCount = activeFilters.size();
    activeFilterCountBadge.setText(texts.getAdminUsersActiveFilterCount(activeFilterCount));
    activeFilterChips.removeAll();
    for (ActiveFilter filter : activeFilters) {
      Span label = new Span(filter.label() + ": " + filter.value());
      Button remove = new Button(VaadinIcon.CLOSE.create(), event -> filter.remove().run());
      remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      remove.addClassName("admin-user-filter-chip-remove");
      String removeText = texts.getAdminUsersRemoveFilter(filter.label());
      remove.setAriaLabel(removeText);
      remove.setTooltipText(removeText);
      Div chip = new Div(label, remove);
      chip.addClassNames("cosmos-admin-role-chip", "admin-user-active-filter-chip");
      activeFilterChips.add(chip);
    }
    activeFiltersRow.setVisible(!activeFilters.isEmpty());
  }

  record ActiveFilter(String label, String value, SerializableRunnable remove)
      implements Serializable {}

  private void clearAllFilters() {
    search.clear();
    for (HasValue<?, ?> control : controls) {
      control.clear();
    }
  }
}
