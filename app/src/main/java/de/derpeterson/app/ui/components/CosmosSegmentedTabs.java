package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Typisierte Segmentauswahl aus nativen Tabs; Beschriftungsupdates verändern den Wert nicht. */
public class CosmosSegmentedTabs<T> extends CustomField<T> {
  private final Tabs selectionTabs = new Tabs();
  private final Map<T, Tab> tabsByValue = new LinkedHashMap<>();
  private List<T> itemValues = List.of();
  private ItemLabelGenerator<T> itemLabelGenerator = Objects::toString;
  private ComponentRenderer<? extends Component, T> itemRenderer;
  private boolean refreshingItems;

  public CosmosSegmentedTabs() {
    super(null, true);
    addClassName("cosmos-segmented-field");
    setTabIndex(-1);

    selectionTabs.setAutoselect(false);
    selectionTabs.addThemeVariants(
        TabsVariant.LUMO_EQUAL_WIDTH_TABS, TabsVariant.LUMO_HIDE_SCROLL_BUTTONS);
    selectionTabs.addClassName("cosmos-segmented-tabs");
    selectionTabs.setWidthFull();
    selectionTabs.addSelectedChangeListener(event -> {
      if (!refreshingItems) {
        updateValue();
      }
    });
    add(selectionTabs);
  }

  @SafeVarargs
  public final void setItems(T... itemValues) {
    setItems(Arrays.asList(itemValues));
  }

  public void setItems(Collection<T> itemValues) {
    this.itemValues = List.copyOf(itemValues);
    refreshItems();
  }

  public List<T> getItems() {
    return itemValues;
  }

  public void setItemLabelGenerator(ItemLabelGenerator<T> itemLabelGenerator) {
    this.itemLabelGenerator = Objects.requireNonNull(itemLabelGenerator);
    refreshItems();
  }

  public ItemLabelGenerator<T> getItemLabelGenerator() {
    return itemLabelGenerator;
  }

  public void setRenderer(ComponentRenderer<? extends Component, T> itemRenderer) {
    this.itemRenderer = Objects.requireNonNull(itemRenderer);
    refreshItems();
  }

  /**
   * Erneuert Texte, Icons und Badges, ohne Auswahlereignisse oder einen Filterwechsel auszulösen.
   */
  public void refreshItems() {
    refreshingItems = true;
    try {
      boolean itemsChanged = !List.copyOf(tabsByValue.keySet()).equals(itemValues);
      if (itemsChanged) {
        selectionTabs.removeAll();
        tabsByValue.clear();
      }

      for (T itemValue : itemValues) {
        Tab choiceTab = tabsByValue.computeIfAbsent(itemValue, ignoredValue -> new Tab());
        choiceTab.removeAll();
        if (itemRenderer == null) {
          choiceTab.setLabel(itemLabelGenerator.apply(itemValue));
        } else {
          choiceTab.add(itemRenderer.createComponent(itemValue));
        }
        choiceTab.setAriaLabel(itemLabelGenerator.apply(itemValue));
        if (itemsChanged) {
          selectionTabs.add(choiceTab);
        }
      }
      setPresentationValue(getValue());
    } finally {
      refreshingItems = false;
    }
  }

  @Override
  public void setLabel(String label) {
    super.setLabel(label);
    selectionTabs.getElement().setAttribute("aria-label", label);
  }

  public void setAriaLabel(String ariaLabel) {
    selectionTabs.getElement().setAttribute("aria-label", ariaLabel);
  }

  public Optional<String> getAriaLabel() {
    return Optional.ofNullable(selectionTabs.getElement().getAttribute("aria-label"));
  }

  @Override
  protected T generateModelValue() {
    return tabsByValue.entrySet().stream()
        .filter(valueTabEntry -> valueTabEntry.getValue() == selectionTabs.getSelectedTab())
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  @Override
  protected void setPresentationValue(T selectedValue) {
    Tab selectedTab = tabsByValue.get(selectedValue);
    if (selectedTab == null) {
      selectionTabs.setSelectedIndex(-1);
    } else {
      selectionTabs.setSelectedTab(selectedTab);
    }
  }
}
