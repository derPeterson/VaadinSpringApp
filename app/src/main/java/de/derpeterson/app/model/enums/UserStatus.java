package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.dom.Style;
import de.derpeterson.app.ui.components.AppIcon;
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

  /** Kleine Lucide-Symbole auf einer dezent gefüllten Statusfläche für Auswahlzeilen. */
  public Component getSelectionComponent() {
    AppIcon type =
        switch (this) {
          case AVAILABLE -> AppIcon.CHECK;
          case EMPLOYED -> AppIcon.MINUS;
          case ABSENT -> AppIcon.CLOCK;
          case OFFLINE -> AppIcon.CLOSE;
        };
    Div symbol = new Div(type.create());
    symbol.addClassName("portal-status-symbol");
    String selectionColor =
        switch (this) {
          case AVAILABLE -> "--cosmos-reference-success";
          case EMPLOYED -> "--cosmos-reference-invalid";
          case ABSENT -> "--cosmos-reference-warning";
          case OFFLINE -> "--cosmos-reference-secondary";
        };
    symbol.getStyle().setColor("var(" + selectionColor + ", " + color + ")");
    return symbol;
  }

  /** Gemeinsame Kontursymbole für Avatar und Statusauswahl; Farben bleiben semantisch. */
  public Component getOutlineComponent(IconSize size, boolean overlay) {
    AppIcon type =
        switch (this) {
          case AVAILABLE -> AppIcon.CHECK_CIRCLE;
          case EMPLOYED -> AppIcon.CIRCLE_MINUS;
          case ABSENT -> AppIcon.CLOCK;
          case OFFLINE -> AppIcon.CLOSE_CIRCLE;
        };
    SvgIcon icon = type.create();
    icon.setSize("100%");
    Div symbol = new Div(icon);
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
