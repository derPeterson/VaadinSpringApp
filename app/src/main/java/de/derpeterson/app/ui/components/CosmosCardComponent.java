package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;

/** Gemeinsamer nativer Kartenrahmen für Konto, Administration und Tickets im Cosmos-Theme. */
public class CosmosCardComponent extends Card {
  public CosmosCardComponent(Component... cardContentComponents) {
    addClassNames("cosmos-card", "cosmos-native-card");
    setAriaRole("group");
    setWidthFull();
    add(cardContentComponents);
  }
}
