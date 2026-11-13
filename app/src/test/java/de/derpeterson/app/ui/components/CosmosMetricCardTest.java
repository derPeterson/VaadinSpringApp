package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CosmosMetricCardTest {
  @Test
  void updatingMetricAndTranslationKeepsCallerTypographyAndIdentifier() {
    CosmosMetricCard notificationMetricCard = new CosmosMetricCard();
    notificationMetricCard.setLabel("Ungelesen");
    notificationMetricCard.getLabelSpan().getStyle().set("font-size", "12px");
    notificationMetricCard.getValueSpan().getStyle().set("font-weight", "600");
    notificationMetricCard.getValueSpan().setId("notification-count");

    notificationMetricCard.setValue(12L);
    notificationMetricCard.setLabel("Unread");
    notificationMetricCard.setHighlighted(true);
    notificationMetricCard.setValue("–");

    assertEquals("Unread", notificationMetricCard.getLabel());
    assertEquals("Unread", notificationMetricCard.getAriaLabel().orElseThrow());
    assertEquals("–", notificationMetricCard.getValue());
    assertEquals("12px", notificationMetricCard.getLabelSpan().getStyle().get("font-size"));
    assertEquals("600", notificationMetricCard.getValueSpan().getStyle().get("font-weight"));
    assertEquals("notification-count", notificationMetricCard.getValueSpan().getId().orElseThrow());
  }

  @Test
  void highlightCanBeRemovedWithoutResettingMetric() {
    CosmosMetricCard notificationMetricCard = new CosmosMetricCard();
    notificationMetricCard.setLabel("Gesamt");
    notificationMetricCard.setValue(3L);

    notificationMetricCard.setHighlighted(true);
    assertTrue(notificationMetricCard.hasClassName("cosmos-metric-card--highlighted"));
    notificationMetricCard.setHighlighted(false);

    assertFalse(notificationMetricCard.hasClassName("cosmos-metric-card--highlighted"));
    assertEquals("Gesamt", notificationMetricCard.getLabel());
    assertEquals("3", notificationMetricCard.getValue());
  }
}
