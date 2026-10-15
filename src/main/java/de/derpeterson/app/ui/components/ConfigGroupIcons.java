package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.derpeterson.app.config.AdminConfigGroup;

/** Gemeinsame Icon-Zuordnung für Konfigurations-Cards und deren Navigation. */
public final class ConfigGroupIcons {
  private ConfigGroupIcons() {}

  public static VaadinIcon forGroup(AdminConfigGroup group) {
    return switch (group) {
      case GENERAL, SMTP -> VaadinIcon.COG;
      case SECURITY -> VaadinIcon.LOCK;
      case TOKENS -> VaadinIcon.KEY;
      case USER_STATUS -> VaadinIcon.USER;
      case EMAIL_SENDER -> VaadinIcon.ENVELOPE;
      case QUEUE -> VaadinIcon.LIST;
    };
  }
}
