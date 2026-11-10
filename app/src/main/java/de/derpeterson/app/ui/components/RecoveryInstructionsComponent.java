package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Zeigt die gespeicherte Anleitung als native Schritte, ohne Nachrichtentexte als HTML auszuwerten.
 */
public final class RecoveryInstructionsComponent extends VerticalLayout {
  private String instructionHeading;
  private String instructionDescription;

  public RecoveryInstructionsComponent(String instructionText) {
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("support-instruction-guide");
    setInstructions(instructionText);
  }

  /** Aktualisiert die Vorschau beim Sprachwechsel; gespeicherte Nachrichten bleiben unverändert. */
  public void setInstructions(String instructionText) {
    // Doppelte Zeilenumbrüche trennen Kopf, vier Schritte, Warnung und Grenze der festen Vorlage.
    String[] instructionSections = instructionText.split("\n\n", -1);
    if (instructionSections.length != 7) {
      throw new IllegalArgumentException("Recovery instructions require seven sections");
    }
    removeAll();
    String[] introSectionLines = splitInstructionSectionLines(instructionSections[0]);
    instructionHeading = introSectionLines[0];
    instructionDescription = introSectionLines[1];
    for (int index = 1; index <= 4; index++) {
      String[] stepSectionLines = splitInstructionSectionLines(instructionSections[index]);
      Span stepTitleLabel = new Span(stepSectionLines[0]);
      stepTitleLabel.addClassName("support-instruction-step-title");
      VerticalLayout stepCopyLayout =
          new VerticalLayout(stepTitleLabel, new Span(stepSectionLines[1]));
      stepCopyLayout.setSpacing(false);
      stepCopyLayout.setPadding(false);
      Badge stepNumberBadge = new Badge(Integer.toString(index));
      stepNumberBadge.addClassName("support-instruction-number");
      HorizontalLayout stepRowLayout = new HorizontalLayout(stepNumberBadge, stepCopyLayout);
      stepRowLayout.setPadding(false);
      stepRowLayout.setSpacing(false);
      stepRowLayout.setAlignItems(Alignment.START);
      stepRowLayout.addClassName("support-instruction-step");
      add(stepRowLayout);
    }
    String[] warningSectionLines = splitInstructionSectionLines(instructionSections[5]);
    Span warningTitle = new Span(warningSectionLines[0] + " ");
    warningTitle.addClassName("cosmos-security-warning-label");
    HorizontalLayout warningNoticeLayout =
        new HorizontalLayout(
            AppIcon.ERROR.create(), new Span(warningTitle, new Span(warningSectionLines[1])));
    warningNoticeLayout.setSpacing(false);
    warningNoticeLayout.setPadding(false);
    warningNoticeLayout.setAlignItems(Alignment.START);
    warningNoticeLayout.addClassName("cosmos-security-warning");
    HorizontalLayout limitationNoticeLayout =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), new Span(instructionSections[6]));
    limitationNoticeLayout.setSpacing(false);
    limitationNoticeLayout.setPadding(false);
    limitationNoticeLayout.setAlignItems(Alignment.CENTER);
    limitationNoticeLayout.addClassName("support-help-notice");
    add(warningNoticeLayout, limitationNoticeLayout);
  }

  public String getHeading() {
    return instructionHeading;
  }

  public String getDescription() {
    return instructionDescription;
  }

  private static String[] splitInstructionSectionLines(String sectionText) {
    int lineBreakIndex = sectionText.indexOf('\n');
    if (lineBreakIndex <= 0 || lineBreakIndex == sectionText.length() - 1) {
      throw new IllegalArgumentException("Recovery instruction section requires a title and text");
    }
    return new String[] {
      sectionText.substring(0, lineBreakIndex), sectionText.substring(lineBreakIndex + 1)
    };
  }
}
