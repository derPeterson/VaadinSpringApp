package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;

/** Gemeinsamer nativer Kartenrahmen für Konto, Administration und Tickets im Cosmos-Theme. */
@StyleSheet("custom-theme/cosmos-native-card.css")
public class CosmosCardComponent extends Card {
  public CosmosCardComponent(Component... cardContentComponents) {
    addClassNames("cosmos-card", "cosmos-native-card");
    setAriaRole("group");
    setWidthFull();
    add(cardContentComponents);
  }
}
