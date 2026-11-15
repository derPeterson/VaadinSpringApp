package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;

/** Gemeinsame dekorative CSS-Ebenen für Besucherbereich, Portal und Administration. */
public class CosmosBackgroundComponent extends Div {
  private static final String BASE_VARIANT = "base";
  private static final String ADVANCED_VARIANT = "advanced";
  private static final String STARS_CLASS = "cosmos-stars";
  private static final String SPARK_CLASS = "cosmos-spark";

  // Zum anwendungsweiten Wechsel nur diese Zeile auf BASE_VARIANT oder ADVANCED_VARIANT setzen.
  public static final String ACTIVE_VARIANT = ADVANCED_VARIANT;
  public static final String STYLESHEET =
      "custom-theme/cosmos-background-" + ACTIVE_VARIANT + ".css";

  public CosmosBackgroundComponent() {
    addClassName("cosmos-sky");
    getElement().setAttribute("aria-hidden", "true");

    switch (ACTIVE_VARIANT) {
      case BASE_VARIANT -> addBaseBackgroundLayers();
      case ADVANCED_VARIANT -> addAdvancedBackgroundLayers();
      default -> throw new IllegalStateException("Unknown Cosmos background: " + ACTIVE_VARIANT);
    }
  }

  private void addBaseBackgroundLayers() {
    add(
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars--small"),
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars--far"),
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
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars-a"),
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars-b"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--first"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--second"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--third"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--fourth"),
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
