package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.html.Div;

/**
 * Gemeinsamer dekorativer Hintergrund für Besucherbereich, Portal und Administration: Horizont,
 * Sterne, Funkelsterne, Sternschnuppen, Satellit, Wanderstern, Supernova und Mond als native
 * Div-Ebenen. Gestaltung in {@link #STYLESHEET}; die Ebenen sind für Hilfstechnik ausgeblendet.
 */
public class CosmosBackgroundComponent extends Div {
  public static final String STYLESHEET = "custom-theme/cosmos-background.css";

  private static final String STARS_CLASS = "cosmos-stars";
  private static final String SPARK_CLASS = "cosmos-spark";
  private static final String SHOOT_CLASS = "cosmos-shoot";

  public CosmosBackgroundComponent() {
    addClassName("cosmos-sky");
    getElement().setAttribute("aria-hidden", "true");
    add(
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars-a"),
        createBackgroundLayerDiv(STARS_CLASS, "cosmos-stars-b"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--first"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--second"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--third"),
        createBackgroundLayerDiv(SPARK_CLASS, "cosmos-spark--fourth"),
        createBackgroundLayerDiv("cosmos-moon"),
        createBackgroundLayerDiv(SHOOT_CLASS, "cosmos-shoot--first"),
        createBackgroundLayerDiv(SHOOT_CLASS, "cosmos-shoot--second"),
        createBackgroundLayerDiv(SHOOT_CLASS, "cosmos-shoot--third"),
        createBackgroundLayerDiv("cosmos-satellite"),
        createBackgroundLayerDiv("cosmos-drift"),
        createBackgroundLayerDiv("cosmos-nova"),
        createBackgroundLayerDiv("cosmos-horizon"));
    addAttachListener(attachEvent -> connectBackgroundParallax());
  }

  private void connectBackgroundParallax() {
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
