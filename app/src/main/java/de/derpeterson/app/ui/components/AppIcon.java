package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.icon.SvgIcon;

/** Gemeinsame Lucide-Symbole; Konturen und Lizenz werden lokal mit der Anwendung ausgeliefert. */
public enum AppIcon {
  ANGLE_DOUBLE_LEFT("chevrons-left"),
  ANGLE_DOUBLE_RIGHT("chevrons-right"),
  ANGLE_DOWN("chevron-down"),
  ANGLE_LEFT("chevron-left"),
  ANGLE_RIGHT("chevron-right"),
  ANGLE_UP("chevron-up"),
  ARCHIVE("archive"),
  ARROW_FORWARD("arrow-right"),
  BELL("bell"),
  CALENDAR_USER("calendar-days"),
  CHECK("check"),
  CHECK_CHECK("check-check"),
  CHECK_CIRCLE("circle-check"),
  CLOSE("x"),
  CLOSE_CIRCLE("circle-x"),
  COG("settings"),
  COGS("settings-2"),
  DASHBOARD("layout-dashboard"),
  DESKTOP("monitor"),
  EDIT("square-pen"),
  ENVELOPE("mail"),
  EYE("eye"),
  FILTER("list-filter"),
  FONT("type"),
  HOME("house"),
  INFO_CIRCLE("info"),
  KEY("key-round"),
  LANGUAGES("languages"),
  LIST("list"),
  LOCK("lock-keyhole"),
  MENU("menu"),
  MESSAGE("message-square"),
  MINUS("minus"),
  PAPERPLANE("send"),
  PICTURE("image"),
  PLUS("plus"),
  QUESTION_CIRCLE("circle-help"),
  REFRESH("refresh-cw"),
  ROTATE_LEFT("rotate-ccw"),
  SEARCH("search"),
  SHIELD("shield"),
  SHIELD_CHECK("shield-check"),
  SHIELD_COG("shield-cog"),
  SIGN_IN("log-in"),
  SIGN_OUT("log-out"),
  TRASH("trash-2"),
  UPLOAD("upload"),
  USER("user-round"),
  USERS("users-round"),
  USER_PLUS("user-round-plus"),
  WARNING("circle-alert"),
  WRENCH("wrench"),
  ERROR("triangle-alert"),
  DEVICES("monitor-smartphone"),
  MOBILE("smartphone"),
  ENVELOPE_OPEN("mail-open"),
  MAIL_CHECK("mail-check"),
  CIRCLE_MINUS("circle-minus"),
  CLOCK("clock");

  private final String name;

  AppIcon(String name) {
    this.name = name;
  }

  public String getSource() {
    return "icons/lucide/" + name + ".svg";
  }

  public SvgIcon create() {
    SvgIcon icon = new SvgIcon(getSource());
    icon.addClassName("cosmos-lucide-icon");
    icon.getElement().setAttribute("aria-hidden", "true");
    icon.getElement().setAttribute("data-lucide", name);
    return icon;
  }
}
