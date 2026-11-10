package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

/** Gemeinsame Bereichsüberschrift; die View behält ihre dynamisch übersetzten Texte. */
public class CosmosSectionHeaderComponent extends Div {
  /** Gemeinsamer Kopfbereich mit kleiner Bereichs-/Breadcrumbzeile und Akzentpunkt. */
  public CosmosSectionHeaderComponent(
      Span overviewSpan, Span sectionTitleSpan, Span sectionDescriptionSpan) {
    H1 sectionTitleHeading = new H1();
    Span headingAccentSpan = new Span(".");
    headingAccentSpan.addClassName("cosmos-dashboard-accent");
    headingAccentSpan.getElement().setAttribute("aria-hidden", "true");
    sectionTitleHeading.add(sectionTitleSpan, headingAccentSpan);
    addClassNames("cosmos-section-header", "cosmos-dashboard-introduction");
    sectionTitleHeading.addClassName("cosmos-section-title");
    overviewSpan.addClassName("cosmos-dashboard-kicker");
    sectionDescriptionSpan.addClassName("cosmos-dashboard-description");
    add(overviewSpan, sectionTitleHeading, sectionDescriptionSpan);
  }

  public CosmosSectionHeaderComponent(H1 sectionTitleHeading, Span sectionDescriptionSpan) {
    addClassName("cosmos-section-header");
    sectionTitleHeading.addClassName("cosmos-section-title");
    sectionDescriptionSpan.addClassName("cosmos-dashboard-description");
    add(sectionTitleHeading, sectionDescriptionSpan);
  }
}
