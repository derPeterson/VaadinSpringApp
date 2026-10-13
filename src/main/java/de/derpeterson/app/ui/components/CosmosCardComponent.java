package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Gemeinsame Card für alle Kontoansichten; Darstellung ausschließlich im Cosmos-Theme. */
public class CosmosCardComponent extends VerticalLayout {
    public CosmosCardComponent(Component... content) {
        addClassName("cosmos-card");
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        add(content);
    }
}
