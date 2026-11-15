package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;

/** Gemeinsame dekorative CSS-Ebenen für Besucherbereich, Portal und Administration. */
public class CosmosBackgroundComponent extends Div {
  // Zum anwendungsweiten Wechsel nur diese Zeile auf "base" oder "advanced" setzen.
  public static final String ACTIVE_VARIANT = "advanced";
  public static final String STYLESHEET =
      "custom-theme/cosmos-background-" + ACTIVE_VARIANT + ".css";

  public CosmosBackgroundComponent() {
    addClassName("cosmos-sky");
    getElement().setAttribute("aria-hidden", "true");

    switch (ACTIVE_VARIANT) {
      case "base" -> addBaseBackgroundLayers();
      case "advanced" -> addAdvancedBackgroundLayers();
      default -> throw new IllegalStateException("Unknown Cosmos background: " + ACTIVE_VARIANT);
    }
  }

  private void addBaseBackgroundLayers() {
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

  private void addAdvancedBackgroundLayers() {
    addClassName("cosmos-sky--advanced");
    add(
        createBackgroundLayerDiv("cosmos-stars", "cosmos-stars-a"),
        createBackgroundLayerDiv("cosmos-stars", "cosmos-stars-b"),
        createBackgroundLayerDiv("cosmos-spark", "cosmos-spark--first"),
        createBackgroundLayerDiv("cosmos-spark", "cosmos-spark--second"),
        createBackgroundLayerDiv("cosmos-spark", "cosmos-spark--third"),
        createBackgroundLayerDiv("cosmos-spark", "cosmos-spark--fourth"),
        createBackgroundLayerDiv("cosmos-moon"),
        createBackgroundLayerDiv("cosmos-shoot", "cosmos-shoot--first"),
        createBackgroundLayerDiv("cosmos-shoot", "cosmos-shoot--second"),
        createBackgroundLayerDiv("cosmos-satellite"),
        createBackgroundLayerDiv("cosmos-horizon"));
    addAttachListener(attachEvent -> connectAdvancedBackgroundParallax());
  }

  private void connectAdvancedBackgroundParallax() {
    getElement()
        .executeJs(
            """
            const scene = this;
            // Nach einer Navigation bleibt höchstens ein gemeinsamer Zeiger-Listener aktiv.
            window.__cosmosBackgroundParallaxController?.abort();
            const controller = new AbortController();
            window.__cosmosBackgroundParallaxController = controller;
            const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
            const resetParallax = () => {
              scene.style.removeProperty('--cosmos-background-parallax-x');
              scene.style.removeProperty('--cosmos-background-parallax-y');
            };
            reducedMotion.addEventListener('change', resetParallax, {signal: controller.signal});
            window.addEventListener('pointermove', event => {
              if (!scene.isConnected) {
                controller.abort();
                return;
              }
              if (reducedMotion.matches) {
                return;
              }
              scene.style.setProperty('--cosmos-background-parallax-x',
                (event.clientX / innerWidth * 2 - 1).toFixed(3));
              scene.style.setProperty('--cosmos-background-parallax-y',
                (event.clientY / innerHeight * 2 - 1).toFixed(3));
            }, {passive: true, signal: controller.signal});
            """);
  }

  private static Div createBackgroundLayerDiv(String... layerClassNames) {
    Div backgroundLayerDiv = new Div();
    backgroundLayerDiv.addClassNames(layerClassNames);
    return backgroundLayerDiv;
  }
}
