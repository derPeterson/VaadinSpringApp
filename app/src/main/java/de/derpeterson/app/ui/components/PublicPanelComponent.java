package de.derpeterson.app.ui.components;

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
    addClassNames("cosmos-panel-card", "public-panel");
    Div symbol = new Div(icon);
    symbol.addClassName("cosmos-panel-icon");
    Span description = new Span(subtitle.get());
    description.addClassName("cosmos-panel-description");
    Div heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    Div header = new Div(symbol, heading);
    header.addClassName("cosmos-panel-header");
    Div body = new Div(bodyContent);
    body.addClassName("public-panel-body");
    Div footer = new Div(actions);
    footer.addClassName("public-panel-footer");
    add(header, body, footer);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> description.setText(subtitle.get()));
  }
}
