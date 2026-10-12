package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Responsive registration card; decoration and widths are defined by the theme. */
public class RegistrationCardComponent extends VerticalLayout {
    public RegistrationCardComponent(Component... contentComponents) {
        addClassName("registration-card");
        setWidthFull();
        setSpacing(false);
        setPadding(false);
        add(contentComponents);
    }
}
