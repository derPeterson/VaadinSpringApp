package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

/** Gemeinsame Bereichsüberschrift; die View behält ihre dynamisch übersetzten Texte. */
public class CosmosSectionHeaderComponent extends Div {
  /** Gemeinsamer Kopfbereich mit kleiner Bereichs-/Breadcrumbzeile und Akzentpunkt. */
  public CosmosSectionHeaderComponent(Span overview, Span titleText, Span description) {
    H1 title = new H1();
    Span accent = new Span(".");
    accent.addClassName("cosmos-dashboard-accent");
    accent.getElement().setAttribute("aria-hidden", "true");
    title.add(titleText, accent);
    addClassNames("cosmos-section-header", "cosmos-dashboard-introduction");
    title.addClassName("cosmos-section-title");
    overview.addClassName("cosmos-dashboard-kicker");
    description.addClassName("cosmos-dashboard-description");
    add(overview, title, description);
  }

  public CosmosSectionHeaderComponent(H1 title, Span description) {
    addClassName("cosmos-section-header");
    title.addClassName("cosmos-section-title");
    description.addClassName("cosmos-dashboard-description");
    add(title, description);
  }
}
