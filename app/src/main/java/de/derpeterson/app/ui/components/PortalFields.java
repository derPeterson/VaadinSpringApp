package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;

/** Gemeinsame native Auswahllisten; Werte, Tastatur und Filterung bleiben bei Vaadin. */
public final class PortalFields {
  private PortalFields() {}

  public static <T> void styleChoice(
      ComboBox<T> choiceComboBox,
      SerializableFunction<T, String> itemLabelFunction,
      boolean withDot) {
    choiceComboBox.addClassName("cosmos-reference-choice");
    choiceComboBox.setClassName("cosmos-choice-dot", withDot);
    choiceComboBox.setClassNameGenerator(choiceValue ->
        withDot ? "cosmos-reference-option" : "cosmos-reference-option portal-pagination-option");
    choiceComboBox.setRenderer(
        new ComponentRenderer<>(choiceValue -> {
          Div optionRowLayout = new Div();
          optionRowLayout.addClassName("cosmos-reference-option-row");
          if (withDot) {
            Span statusDotLabel = new Span();
            statusDotLabel.addClassName("cosmos-reference-option-dot");
            optionRowLayout.add(statusDotLabel);
          }
          Span choiceTextLabel = new Span(itemLabelFunction.apply(choiceValue));
          SvgIcon selectedCheckIcon = AppIcon.CHECK.create();
          selectedCheckIcon.addClassName("cosmos-reference-option-check");
          optionRowLayout.add(choiceTextLabel, selectedCheckIcon);
          return optionRowLayout;
        }));
  }
}
