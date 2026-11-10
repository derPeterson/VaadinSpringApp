package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/** Lesbare Objektkarte in Bestätigungen, ohne den eigentlichen Inhalt zu duplizieren. */
public class PortalContextCard extends Div {
  private final Div contextCardContentDiv = new Div();

  public PortalContextCard(
      Component contextIconComponent, String contextTitleText, String... details) {
    addClassName("portal-context-card");
    contextIconComponent.getElement().setAttribute("aria-hidden", "true");
    Div contextIconDiv = new Div(contextIconComponent);
    contextIconDiv.addClassName(PANEL_ICON);

    Span contextHeadingSpan = new Span(contextTitleText);
    contextHeadingSpan.addClassName("portal-context-title");
    contextCardContentDiv.add(contextHeadingSpan);
    contextCardContentDiv.addClassName("portal-context-copy");

    for (String contextDetailText : details) {
      if (contextDetailText != null && !contextDetailText.isBlank()) {
        Span contextDetailSpan = new Span(contextDetailText);
        contextDetailSpan.addClassName("portal-context-detail");
        contextCardContentDiv.add(contextDetailSpan);
      }
    }

    add(contextIconDiv, contextCardContentDiv);
  }

  public void addMetadata(Component metadataComponent) {
    contextCardContentDiv.add(metadataComponent);
  }
}
