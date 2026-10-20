package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Style;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
  AVAILABLE("var(--lumo-success-color)", UserStatus.BACKGROUND_COLOR),
  EMPLOYED("var(--lumo-error-color)", UserStatus.BACKGROUND_COLOR),
  ABSENT("var(--lumo-warning-color)", UserStatus.BACKGROUND_COLOR),
  OFFLINE("var(--lumo-secondary-text-color)", UserStatus.BACKGROUND_COLOR);

  private static final String BACKGROUND_COLOR = "var(--lumo-base-color)";

  private final String color;
  private final String backgroundColor;

  /** Gemeinsame Kontursymbole für Avatar und Statusauswahl; Farben bleiben semantisch. */
  public Component getOutlineComponent(IconSize size, boolean overlay) {
    String mark =
        switch (this) {
          case AVAILABLE -> "<path d=\"m7.5 12 3 3 6-6\"/>";
          case EMPLOYED -> "<path d=\"M7.5 12h9\"/>";
          case ABSENT -> "<path d=\"M12 6.5V12l4 2.5\"/>";
          case OFFLINE -> "<path d=\"m8.5 8.5 7 7m0-7-7 7\"/>";
        };
    Div symbol = new Div();
    symbol
        .getElement()
        .setProperty(
            "innerHTML",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"100%\" height=\"100%\" style=\"display:block\""
                + " fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">"
                + "<circle cx=\"12\" cy=\"12\" r=\"10\"/>"
                + mark
                + "</svg>");
    String width =
        (overlay ? Math.round(size.getWidth() * .3) : size.getWidth()) + size.getUnit().getSymbol();
    String height =
        (overlay ? Math.round(size.getHeight() * .3) : size.getHeight())
            + size.getUnit().getSymbol();
    symbol.setWidth(width);
    symbol.setHeight(height);
    symbol.getStyle().setColor(color).setBackgroundColor(backgroundColor).setBorderRadius("50%");
    if (overlay) {
      symbol
          .getStyle()
          .setPosition(Style.Position.ABSOLUTE)
          .setBottom("var(--cosmos-avatar-status-anchor)")
          .setRight("var(--cosmos-avatar-status-anchor)")
          .set("transform", "translate(50%, 50%)");
    }
    return symbol;
  }

  public static UserStatus getDefaultStatus() {
    return OFFLINE;
  }
}
