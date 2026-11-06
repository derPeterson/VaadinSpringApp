package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.icon.SvgIcon;

/** Lokale Anwendungssymbole aus Lucide, Tabler und Material Symbols. */
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
  BOT("bot"),
  CALENDAR_USER("calendar-days"),
  CHECK("check"),
  CHECK_CHECK("check-check"),
  CHECK_CIRCLE("circle-check"),
  CLIPBOARD_COPY("clipboard-copy"),
  CLIPBOARD_LIST("clipboard-list"),
  CLOSE("x"),
  CLOSE_CIRCLE("circle-x"),
  CLOUD_LOCK(Library.TABLER, "cloud-lock"),
  NUMBER_ONE(Library.TABLER, "square-rounded-number-1"),
  NUMBER_TWO(Library.TABLER, "square-rounded-number-2"),
  NUMBER_THREE(Library.TABLER, "square-rounded-number-3"),
  EXTERNAL_LINK_TABLER(Library.TABLER, "external-link"),
  EXPAND("expand"),
  COG("settings"),
  COGS("settings-2"),
  DASHBOARD("layout-dashboard"),
  DESKTOP("monitor"),
  DOWNLOAD("download"),
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
  LOCK_QUESTION(Library.TABLER, "lock-question"),
  LOCK_RESET(Library.MATERIAL_SYMBOLS, "lock-reset"),
  MENU("menu"),
  MESSAGE("message-square"),
  MESSAGES("messages-square"),
  MINUS("minus"),
  MINIMIZE("minimize"),
  OPEN_SECTION("square-arrow-right-enter"),
  PAPERPLANE("send"),
  PICTURE("image"),
  PLUS("plus"),
  QUESTION_CIRCLE("circle-help"),
  QR_CODE("qr-code"),
  REFRESH("refresh-cw"),
  ROTATE_LEFT("rotate-ccw"),
  SEARCH("search"),
  SHIELD("shield"),
  SHIELD_CHECK("shield-check"),
  SHIELD_COG("shield-cog"),
  SHIELD_KEYHOLE("shield-keyhole"),
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
  private final Library library;

  private enum Library {
    LUCIDE("lucide"),
    TABLER("tabler"),
    MATERIAL_SYMBOLS("material-symbols");
    private final String directory;

    Library(String directory) {
      this.directory = directory;
    }
  }

  AppIcon(String name) {
    this(Library.LUCIDE, name);
  }

  AppIcon(Library library, String name) {
    this.library = library;
    this.name = name;
  }

  public String getSource() {
    return "icons/" + library.directory + "/" + name + ".svg";
  }

  public SvgIcon create() {
    SvgIcon icon = new SvgIcon(getSource());
    icon.addClassName("cosmos-" + library.directory + "-icon");
    icon.getElement().setAttribute("aria-hidden", "true");
    icon.getElement().setAttribute("data-" + library.directory, name);
    return icon;
  }
}
