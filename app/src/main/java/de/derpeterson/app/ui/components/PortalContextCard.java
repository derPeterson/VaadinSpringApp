package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/** Lesbare Objektkarte in Bestätigungen, ohne den eigentlichen Inhalt zu duplizieren. */
public class PortalContextCard extends Div {
  private final Div contextCardCopy = new Div();

  public PortalContextCard(Component symbol, String title, String... details) {
    addClassName("portal-context-card");
    symbol.getElement().setAttribute("aria-hidden", "true");
    Div icon = new Div(symbol);
    icon.addClassName(PANEL_ICON);
    Span heading = new Span(title);
    heading.addClassName("portal-context-title");
    contextCardCopy.add(heading);
    contextCardCopy.addClassName("portal-context-copy");
    for (String value : details) {
      if (value != null && !value.isBlank()) {
        Span detail = new Span(value);
        detail.addClassName("portal-context-detail");
        contextCardCopy.add(detail);
      }
    }
    add(icon, contextCardCopy);
  }

  public void addMetadata(Component metadata) {
    contextCardCopy.add(metadata);
  }
}
