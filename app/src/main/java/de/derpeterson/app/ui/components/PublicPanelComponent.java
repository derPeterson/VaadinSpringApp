package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.events.LanguageChangeEvent;
import java.util.function.Supplier;

/** Dieselben Panelzonen wie im Konto, mit den echten Eingaben und Aktionen der Besucheransicht. */
public class PublicPanelComponent extends CosmosCardComponent {
  public PublicPanelComponent(
      Component icon,
      Component title,
      Supplier<String> subtitle,
      Component[] bodyContent,
      Component... actions) {
    addClassNames(PANEL_CARD, "public-panel");
    Div symbol = new Div(icon);
    symbol.addClassName(PANEL_ICON);
    Span description = new Span(subtitle.get());
    description.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(title, description);
    heading.addClassName(PANEL_HEADING);
    Div header = new Div(symbol, heading);
    header.addClassName(PANEL_HEADER);
    Div body = new Div(bodyContent);
    body.addClassName("public-panel-body");
    Div footer = new Div(actions);
    footer.addClassName("public-panel-footer");
    add(header, body, footer);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> description.setText(subtitle.get()));
  }
}
