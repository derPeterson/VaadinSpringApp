package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/** Native Zeile für eine Eingabe und ihren getrennt bedienbaren Feldhilfebutton. */
public class FieldHelpRowLayout extends HorizontalLayout {
  public FieldHelpRowLayout(Component inputComponent, ConfigInfoButton fieldHelpButton) {
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    setAlignItems(Alignment.CENTER);
    addClassName("cosmos-config-input-row");

    add(inputComponent, fieldHelpButton);
    setFlexGrow(1, inputComponent);
    setFlexShrink(1, inputComponent);
    setFlexShrink(0, fieldHelpButton);
    inputComponent.getElement().getStyle().set("min-width", "0");
  }
}
