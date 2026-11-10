package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.Component;

/** Gleicht ausschließlich sichtbare Textbuttons einer zusammengehörigen Aktionsgruppe an. */
public final class EqualButtonWidths {
  private EqualButtonWidths() {}

  public static void configureEqualButtonWidths(Component buttonGroupComponent) {
    buttonGroupComponent.getElement().setAttribute("data-equal-text-widths", "");

    buttonGroupComponent.addAttachListener(attachEvent -> {
      attachEvent.getUI().getPage().addJavaScript("context://custom-theme/equal-button-widths.js");
      buttonGroupComponent.getElement().executeJs("window.cosmosEqualButtonWidths.observe(this)");
    });
  }
}
