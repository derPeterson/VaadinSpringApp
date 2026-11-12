package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CosmosSegmentedTabsTest {
  @Test
  void nativeSelectionChangesTheTypedValueExactlyOnce() {
    CosmosSegmentedTabs<String> filterTabs = createFilterTabs();
    List<String> selectedValues = new ArrayList<>();
    filterTabs.addValueChangeListener(event -> selectedValues.add(event.getValue()));

    nativeTabs(filterTabs).setSelectedIndex(1);

    assertEquals("unread", filterTabs.getValue());
    assertEquals(List.of("unread"), selectedValues);
    filterTabs.setValue("all");
    assertEquals(0, nativeTabs(filterTabs).getSelectedIndex());
    assertEquals(List.of("unread", "all"), selectedValues);
  }

  @Test
  void refreshingCountsAndTranslationsKeepsSelectionAndNativeFocusTarget() {
    CosmosSegmentedTabs<String> filterTabs = createFilterTabs();
    filterTabs.setValue("unread");
    Tab selectedTab = nativeTabs(filterTabs).getSelectedTab();
    AtomicInteger filterChangeCount = new AtomicInteger();
    filterTabs.addValueChangeListener(event -> filterChangeCount.incrementAndGet());

    filterTabs.setItemLabelGenerator(value -> "translated " + value);
    filterTabs.setRenderer(new ComponentRenderer<>(value -> new Span(value + " (3)")));
    filterTabs.refreshItems();

    assertEquals("unread", filterTabs.getValue());
    assertSame(selectedTab, nativeTabs(filterTabs).getSelectedTab());
    assertEquals("unread (3)", selectedTab.getElement().getTextRecursively());
    assertEquals("translated unread", selectedTab.getAriaLabel().orElseThrow());
    assertEquals(0, filterChangeCount.get());
  }

  @Test
  void labelsAndDisabledStateReachTheNativeControl() {
    CosmosSegmentedTabs<String> filterTabs = createFilterTabs();
    filterTabs.setLabel("Messages");
    assertEquals("Messages", nativeTabs(filterTabs).getElement().getAttribute("aria-label"));

    filterTabs.setAriaLabel("Message filter");
    assertEquals("Message filter", filterTabs.getAriaLabel().orElseThrow());
    filterTabs.setEnabled(false);
    assertFalse(nativeTabs(filterTabs).isEnabled());

    filterTabs.setEnabled(true);
    assertTrue(nativeTabs(filterTabs).isEnabled());
    assertEquals(List.of("all", "unread"), filterTabs.getItems());
  }

  private CosmosSegmentedTabs<String> createFilterTabs() {
    CosmosSegmentedTabs<String> filterTabs = new CosmosSegmentedTabs<>();
    filterTabs.setItems("all", "unread");
    filterTabs.setValue("all");
    return filterTabs;
  }

  private Tabs nativeTabs(CosmosSegmentedTabs<?> filterTabs) {
    return filterTabs
        .getChildren()
        .filter(Tabs.class::isInstance)
        .map(Tabs.class::cast)
        .findFirst()
        .orElseThrow();
  }
}
