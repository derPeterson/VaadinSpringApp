package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;

/** Gleicht ausschließlich sichtbare Textbuttons einer zusammengehörigen Aktionsgruppe an. */
public final class EqualButtonWidths {
  private EqualButtonWidths() {}

  public static void configure(Component group) {
    group.getElement().setAttribute("data-equal-text-widths", "");
    group.addAttachListener(
        event -> {
          event.getUI().getPage().addJavaScript("context://custom-theme/equal-button-widths.js");
          group.getElement().executeJs("window.cosmosEqualButtonWidths.observe(this)");
        });
  }
}
