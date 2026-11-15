package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;

/** Native Inhaltskarte; Rahmen und Anordnung bestimmt weiterhin die jeweilige Fachansicht. */
@StyleSheet("custom-theme/cosmos-content-card.css")
public class CosmosContentCardComponent extends Card {
  public CosmosContentCardComponent(Component... contentComponents) {
    addClassName("cosmos-content-card");
    setAriaRole("group");
    add(contentComponents);
  }
}
