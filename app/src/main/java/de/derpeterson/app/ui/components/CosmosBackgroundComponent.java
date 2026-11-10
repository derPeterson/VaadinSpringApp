package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;

/** Gemeinsame dekorative CSS-Ebenen für Kontoansichten und Startseite. */
public class CosmosBackgroundComponent extends Div {
  public CosmosBackgroundComponent() {
    addClassName("cosmos-sky");
    getElement().setAttribute("aria-hidden", "true");
    add(
        createBackgroundLayerDiv("cosmos-stars", "cosmos-stars--small"),
        createBackgroundLayerDiv("cosmos-stars", "cosmos-stars--far"),
        createBackgroundLayerDiv("cosmos-galaxy"),
        createBackgroundLayerDiv("cosmos-glow", "cosmos-glow--blue"),
        createBackgroundLayerDiv("cosmos-glow", "cosmos-glow--violet"),
        createBackgroundLayerDiv("cosmos-horizon"),
        createBackgroundLayerDiv("cosmos-planet"),
        createBackgroundLayerDiv("cosmos-moon"));
  }

  private static Div createBackgroundLayerDiv(String... layerClassNames) {
    Div backgroundLayerDiv = new Div();
    backgroundLayerDiv.addClassNames(layerClassNames);
    return backgroundLayerDiv;
  }
}
