package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

/** Gemeinsame Bereichsüberschrift; die View behält ihre dynamisch übersetzten Texte. */
public class CosmosSectionHeaderComponent extends Div {
  /** Gemeinsamer Kopfbereich mit kleiner Bereichs-/Breadcrumbzeile und Akzentpunkt. */
  public CosmosSectionHeaderComponent(
      Span overviewLabel, Span titleText, Span sectionDescriptionLabel) {
    H1 sectionTitleHeading = new H1();
    Span headingAccentLabel = new Span(".");
    headingAccentLabel.addClassName("cosmos-dashboard-accent");
    headingAccentLabel.getElement().setAttribute("aria-hidden", "true");
    sectionTitleHeading.add(titleText, headingAccentLabel);
    addClassNames("cosmos-section-header", "cosmos-dashboard-introduction");
    sectionTitleHeading.addClassName("cosmos-section-title");
    overviewLabel.addClassName("cosmos-dashboard-kicker");
    sectionDescriptionLabel.addClassName("cosmos-dashboard-description");
    add(overviewLabel, sectionTitleHeading, sectionDescriptionLabel);
  }

  public CosmosSectionHeaderComponent(H1 sectionTitleHeading, Span sectionDescriptionLabel) {
    addClassName("cosmos-section-header");
    sectionTitleHeading.addClassName("cosmos-section-title");
    sectionDescriptionLabel.addClassName("cosmos-dashboard-description");
    add(sectionTitleHeading, sectionDescriptionLabel);
  }
}
