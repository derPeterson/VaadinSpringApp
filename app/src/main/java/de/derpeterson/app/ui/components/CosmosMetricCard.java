package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Statische Kennzahl mit getrennt gestaltbarer Beschriftung und Wert; keine Aktion. */
@StyleSheet("custom-theme/cosmos-metric-card.css")
public class CosmosMetricCard extends Card {
  private final Span metricLabelSpan = new Span();
  private final Span metricValueSpan = new Span("0");

  public CosmosMetricCard() {
    addClassName("cosmos-metric-card");
    setAriaRole("group");
    metricLabelSpan.addClassName("cosmos-metric-card-label");
    metricValueSpan.addClassName("cosmos-metric-card-value");

    VerticalLayout metricContentLayout = new VerticalLayout(metricLabelSpan, metricValueSpan);
    metricContentLayout.setPadding(false);
    metricContentLayout.setSpacing(false);
    metricContentLayout.setAlignItems(Alignment.CENTER);
    add(metricContentLayout);
  }

  public void setLabel(String metricLabelText) {
    metricLabelSpan.setText(metricLabelText);
    setAriaLabel(metricLabelText);
  }

  public String getLabel() {
    return metricLabelSpan.getText();
  }

  public void setValue(String metricValueText) {
    metricValueSpan.setText(metricValueText);
  }

  public void setValue(long metricValueCount) {
    setValue(Long.toString(metricValueCount));
  }

  public String getValue() {
    return metricValueSpan.getText();
  }

  public void setHighlighted(boolean highlighted) {
    setClassName("cosmos-metric-card--highlighted", highlighted);
  }

  /** Native Textkomponente für aufruferspezifische Schriftgestaltung. */
  public Span getLabelSpan() {
    return metricLabelSpan;
  }

  /** Native Textkomponente für Schriftgestaltung und bestehende Kennzahl-IDs. */
  public Span getValueSpan() {
    return metricValueSpan;
  }
}
