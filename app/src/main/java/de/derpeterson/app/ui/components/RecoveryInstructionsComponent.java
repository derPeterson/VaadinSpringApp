package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Zeigt die gespeicherte Anleitung als native Schritte, ohne Nachrichtentexte als HTML auszuwerten.
 */
public final class RecoveryInstructionsComponent extends VerticalLayout {
  private final String heading;
  private final String description;

  public RecoveryInstructionsComponent(String instructions) {
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("support-instruction-guide");
    // Doppelte Zeilenumbrüche trennen Kopf, vier Schritte, Warnung und Grenze der festen Vorlage.
    String[] sections = instructions.split("\n\n", -1);
    if (sections.length != 7) {
      throw new IllegalArgumentException("Recovery instructions require seven sections");
    }
    String[] lead = lines(sections[0]);
    heading = lead[0];
    description = lead[1];
    for (int index = 1; index <= 4; index++) {
      String[] step = lines(sections[index]);
      Span title = new Span(step[0]);
      title.addClassName("support-instruction-step-title");
      VerticalLayout copy = new VerticalLayout(title, new Span(step[1]));
      copy.setSpacing(false);
      copy.setPadding(false);
      Badge number = new Badge(Integer.toString(index));
      number.addClassName("support-instruction-number");
      HorizontalLayout row = new HorizontalLayout(number, copy);
      row.setPadding(false);
      row.setSpacing(false);
      row.setAlignItems(Alignment.START);
      row.addClassName("support-instruction-step");
      add(row);
    }
    String[] warning = lines(sections[5]);
    Span warningTitle = new Span(warning[0] + " ");
    warningTitle.addClassName("cosmos-security-warning-label");
    HorizontalLayout notice =
        new HorizontalLayout(AppIcon.ERROR.create(), new Span(warningTitle, new Span(warning[1])));
    notice.setSpacing(false);
    notice.setPadding(false);
    notice.setAlignItems(Alignment.START);
    notice.addClassName("cosmos-security-warning");
    HorizontalLayout limitation =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), new Span(sections[6]));
    limitation.setSpacing(false);
    limitation.setPadding(false);
    limitation.setAlignItems(Alignment.START);
    limitation.addClassName("support-help-notice");
    add(notice, limitation);
  }

  public String getHeading() {
    return heading;
  }

  public String getDescription() {
    return description;
  }

  private static String[] lines(String section) {
    int separator = section.indexOf('\n');
    if (separator <= 0 || separator == section.length() - 1) {
      throw new IllegalArgumentException("Recovery instruction section requires a title and text");
    }
    return new String[] {section.substring(0, separator), section.substring(separator + 1)};
  }
}
