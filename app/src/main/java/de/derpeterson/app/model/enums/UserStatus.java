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
  AVAILABLE("var(--cosmos-success-color)", UserStatus.BACKGROUND_COLOR),
  EMPLOYED("var(--cosmos-error-color)", UserStatus.BACKGROUND_COLOR),
  ABSENT("var(--cosmos-warning-color)", UserStatus.BACKGROUND_COLOR),
  OFFLINE("var(--cosmos-text-secondary-color)", UserStatus.BACKGROUND_COLOR);

  private static final String BACKGROUND_COLOR = "var(--cosmos-base-color)";

  private final String color;
  private final String backgroundColor;

  /** Kleine Lucide-Symbole auf einer dezent gefüllten Statusfläche für Auswahlzeilen. */
  public Component getSelectionComponent() {
    AppIcon statusAppIcon =
        switch (this) {
          case AVAILABLE -> AppIcon.CHECK;
          case EMPLOYED -> AppIcon.MINUS;
          case ABSENT -> AppIcon.CLOCK;
          case OFFLINE -> AppIcon.CLOSE;
        };

    Div statusIconDiv = new Div(statusAppIcon.create());
    statusIconDiv.addClassName("portal-status-symbol");
    statusIconDiv.getStyle().setColor(color);
    return statusIconDiv;
  }

  /** Gemeinsame Kontursymbole für Avatar und Statusauswahl; Farben bleiben semantisch. */
  public Component getOutlineComponent(IconSize iconSize, boolean showAsOverlay) {
    AppIcon statusAppIcon =
        switch (this) {
          case AVAILABLE -> AppIcon.CHECK_CIRCLE;
          case EMPLOYED -> AppIcon.CIRCLE_MINUS;
          case ABSENT -> AppIcon.CLOCK;
          case OFFLINE -> AppIcon.CLOSE_CIRCLE;
        };
    SvgIcon statusSvgIcon = statusAppIcon.create();
    statusSvgIcon.setSize("100%");
    Div statusIconDiv = new Div(statusSvgIcon);
    String iconWidth =
        (showAsOverlay ? Math.round(iconSize.getWidth() * .3) : iconSize.getWidth())
            + iconSize.getUnit().getSymbol();
    String iconHeight =
        (showAsOverlay ? Math.round(iconSize.getHeight() * .3) : iconSize.getHeight())
            + iconSize.getUnit().getSymbol();
    statusIconDiv.setWidth(iconWidth);
    statusIconDiv.setHeight(iconHeight);
    statusIconDiv
        .getStyle()
        .setColor(color)
        .setBackgroundColor(backgroundColor)
        .setBorderRadius("50%");

    if (showAsOverlay) {
      statusIconDiv
          .getStyle()
          .setPosition(Style.Position.ABSOLUTE)
          .setBottom("var(--cosmos-avatar-status-anchor)")
          .setRight("var(--cosmos-avatar-status-anchor)")
          .set("transform", "translate(50%, 50%)");
    }

    return statusIconDiv;
  }

  public static UserStatus getDefaultStatus() {
    return OFFLINE;
  }
}
