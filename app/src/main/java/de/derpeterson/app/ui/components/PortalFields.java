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
      ComboBox<T> field, SerializableFunction<T, String> label, boolean withDot) {
    field.addClassName("cosmos-reference-choice");
    field.setClassName("cosmos-choice-dot", withDot);
    field.setClassNameGenerator(
        value ->
            withDot
                ? "cosmos-reference-option"
                : "cosmos-reference-option portal-pagination-option");
    field.setRenderer(
        new ComponentRenderer<>(
            value -> {
              Div row = new Div();
              row.addClassName("cosmos-reference-option-row");
              if (withDot) {
                Span dot = new Span();
                dot.addClassName("cosmos-reference-option-dot");
                row.add(dot);
              }
              Span copy = new Span(label.apply(value));
              SvgIcon check = AppIcon.CHECK.create();
              check.addClassName("cosmos-reference-option-check");
              row.add(copy, check);
              return row;
            }));
  }
}
