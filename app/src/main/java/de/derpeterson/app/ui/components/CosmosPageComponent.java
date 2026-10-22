package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.time.Clock;
import java.util.function.Supplier;

/** Gemeinsamer Hintergrund, Marke, Card-Bereich und konfigurierbarer Copyright Text. */
public class CosmosPageComponent extends Div {
  private final VerticalLayout cards = new VerticalLayout();

  public CosmosPageComponent(ConfigService config, boolean wide) {
    this(config, wide, Clock.systemDefaultZone());
  }

  public CosmosPageComponent(ConfigService config, boolean wide, Clock clock) {
    this(config, wide, clock, null);
  }

  public CosmosPageComponent(
      ConfigService config, boolean wide, Supplier<String> localizedDefaultTemplate) {
    this(config, wide, Clock.systemDefaultZone(), localizedDefaultTemplate);
  }

  private CosmosPageComponent(
      ConfigService config, boolean wide, Clock clock, Supplier<String> localizedDefaultTemplate) {
    addClassNames("cosmos-page", "cosmos-theme");
    if (wide) {
      addClassName("cosmos-page--wide");
    }
    String serviceName = config.getString(ConfigEntry.SERVICE_NAME);

    Div brand = layer("cosmos-brand");
    Image logo = new Image("custom-theme/service_logo.png", serviceName);
    logo.addClassName("cosmos-logo");
    Span name = new Span(serviceName);
    name.addClassName("cosmos-brand-name");
    brand.add(logo, name);

    cards.addClassName("cosmos-cards");
    cards.setPadding(false);
    cards.setSpacing(false);
    cards.setAlignItems(FlexComponent.Alignment.STRETCH);

    CosmosCopyrightComponent footer =
        new CosmosCopyrightComponent(config, clock, localizedDefaultTemplate);

    VerticalLayout content = new VerticalLayout(brand, cards, footer);
    content.addClassName("cosmos-content");
    content.setPadding(false);
    content.setSpacing(false);
    content.setAlignItems(FlexComponent.Alignment.CENTER);
    content.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    add(new CosmosBackgroundComponent(), content);
  }

  public VerticalLayout getCardContainer() {
    return cards;
  }

  /** Portalisierte Notifications verwenden die Palette der aktuell angezeigten Cosmos-Ansicht. */
  public static boolean isDisplayed(UI ui) {
    return ui != null && containsCosmosPage(ui);
  }

  private static boolean containsCosmosPage(Component component) {
    return component.getElement().getClassList().contains("cosmos-theme")
        || component.getChildren().anyMatch(CosmosPageComponent::containsCosmosPage);
  }

  private static Div layer(String... classes) {
    Div layer = new Div();
    layer.addClassNames(classes);
    return layer;
  }
}
