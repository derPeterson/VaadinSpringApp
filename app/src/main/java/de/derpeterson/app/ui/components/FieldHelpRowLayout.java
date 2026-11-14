package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Native Zeile für eine Eingabe und ihren getrennt bedienbaren Feldhilfebutton. */
public class FieldHelpRowLayout extends HorizontalLayout {
  public FieldHelpRowLayout(Component inputComponent, ConfigInfoButton fieldHelpButton) {
    setWidthFull();
    setPadding(false);
    setSpacing(false);
    setAlignItems(Alignment.CENTER);
    addClassName("cosmos-config-input-row");

    // Die Hilfespalte erhält die Eingabebreite auch bei einer größeren Button-Bedienfläche.
    VerticalLayout fieldHelpSlotLayout = new VerticalLayout(fieldHelpButton);
    fieldHelpSlotLayout.setWidth(null);
    fieldHelpSlotLayout.setPadding(false);
    fieldHelpSlotLayout.setSpacing(false);
    fieldHelpSlotLayout.setAlignItems(Alignment.START);
    fieldHelpSlotLayout.addClassName("cosmos-config-help-slot");

    add(inputComponent, fieldHelpSlotLayout);
    setFlexGrow(1, inputComponent);
    setFlexShrink(1, inputComponent);
    setFlexShrink(0, fieldHelpSlotLayout);
    inputComponent.getElement().getStyle().set("min-width", "0");
  }
}
