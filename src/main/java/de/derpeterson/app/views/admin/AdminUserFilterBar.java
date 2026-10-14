package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Search/actions stay on one baseline; explicitly added filters wrap beneath them. */
final class AdminUserFilterBar extends VerticalLayout {
    private final MessageProperties texts;
    private final TextField search;
    private final Button clearFilters = new Button();
    private final Span addFilterLabel = new Span();
    private final MenuItem addFilterItem;
    private final Div filters = new Div();
    private final List<FilterControl> controls = new ArrayList<>();

    AdminUserFilterBar(MessageProperties texts, TextField search, Button refresh, Button addUser,
                       ComboBox<Boolean> enabled, ComboBox<UserStatus> status,
                       ComboBox<RoleType> role, AdminUserActivityFilter activity) {
        this.texts = texts;
        this.search = search;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("admin-user-filter-bar");

        MenuBar menu = new MenuBar();
        menu.addClassName("admin-user-add-filter");
        menu.getElement().getThemeList().add("cosmos");
        HorizontalLayout label = new HorizontalLayout(VaadinIcon.FILTER.create(), addFilterLabel);
        label.setSpacing(false);
        label.setAlignItems(Alignment.CENTER);
        label.addClassName("admin-user-add-filter-label");
        addFilterItem = menu.addItem(label);

        clearFilters.setPrefixComponent(VaadinIcon.CLOSE_SMALL.create());
        clearFilters.addThemeVariants(ButtonVariant.LUMO_ERROR);
        clearFilters.addClickListener(event -> clearAllFilters());
        clearFilters.setVisible(false);

        Div actions = new Div(refresh, addUser);
        actions.addClassName("admin-user-toolbar-actions");
        Div toolbar = new Div(search, menu, clearFilters, actions);
        toolbar.addClassName("admin-user-toolbar");
        filters.addClassName("admin-user-filters");
        filters.setVisible(false);
        add(toolbar, filters);

        addControl(enabled, enabled, texts::getAdminUsersGridEnabled);
        addControl(status, status, texts::getAdminUsersGridStatus);
        addControl(role, role, texts::getAdminUsersFilterRole);
        addControl(activity, activity, texts::getAdminUsersFilterActiveSince);
        refreshTexts();
    }

    private void addControl(Component input, HasValue<?, ?> value, Supplier<String> label) {
        Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        remove.addClassName("admin-user-filter-remove");
        Div wrapper = new Div(input, remove);
        wrapper.addClassName("admin-user-filter-control");
        wrapper.setVisible(false);
        filters.add(wrapper);

        MenuItem choice = addFilterItem.getSubMenu().addItem(label.get(), event -> {
            wrapper.setVisible(true);
            filters.setVisible(true);
            updateChoices();
        });
        remove.addClickListener(event -> {
            value.clear();
            wrapper.setVisible(false);
            filters.setVisible(controls.stream().anyMatch(control -> control.wrapper().isVisible()));
            updateChoices();
        });
        controls.add(new FilterControl(wrapper, choice, remove, label, value));
    }

    void refreshTexts() {
        addFilterLabel.setText(texts.getAdminUsersAddFilter());
        clearFilters.setText(texts.getAdminUsersClearFilters());
        for (FilterControl control : controls) {
            control.choice().setText(control.label().get());
            String removeText = texts.getAdminUsersRemoveFilter(control.label().get());
            control.remove().setAriaLabel(removeText);
            control.remove().setTooltipText(removeText);
        }
        updateChoices();
    }

    private void clearAllFilters() {
        search.clear();
        for (FilterControl control : controls) {
            control.value().clear();
            control.wrapper().setVisible(false);
        }
        filters.setVisible(false);
        updateChoices();
    }

    private void updateChoices() {
        for (FilterControl control : controls) control.choice().setVisible(!control.wrapper().isVisible());
        addFilterItem.setEnabled(controls.stream().anyMatch(control -> !control.wrapper().isVisible()));
    }

    private record FilterControl(Div wrapper, MenuItem choice, Button remove, Supplier<String> label, HasValue<?, ?> value) {
    }
}
