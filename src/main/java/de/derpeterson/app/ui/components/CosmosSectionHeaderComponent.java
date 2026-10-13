package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

/** Gemeinsame Bereichsüberschrift; die View behält ihre dynamisch übersetzten Texte. */
public class CosmosSectionHeaderComponent extends Div {
    public CosmosSectionHeaderComponent(H1 title, Span description) {
        addClassName("cosmos-section-header");
        title.addClassName("cosmos-section-title");
        description.addClassName("cosmos-dashboard-description");
        add(title, description);
    }
}
