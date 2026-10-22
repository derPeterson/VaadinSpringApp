package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;

/** Gemeinsame dekorative CSS-Ebenen für Kontoansichten und Startseite. */
public class CosmosBackgroundComponent extends Div {
  public CosmosBackgroundComponent() {
    addClassName("cosmos-sky");
    getElement().setAttribute("aria-hidden", "true");
    add(
        layer("cosmos-stars", "cosmos-stars--small"),
        layer("cosmos-stars", "cosmos-stars--far"),
        layer("cosmos-galaxy"),
        layer("cosmos-glow", "cosmos-glow--blue"),
        layer("cosmos-glow", "cosmos-glow--violet"),
        layer("cosmos-horizon"),
        layer("cosmos-planet"),
        layer("cosmos-moon"));
  }

  private static Div layer(String... classes) {
    Div layer = new Div();
    layer.addClassNames(classes);
    return layer;
  }
}
