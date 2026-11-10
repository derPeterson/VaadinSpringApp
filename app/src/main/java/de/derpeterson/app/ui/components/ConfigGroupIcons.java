package de.derpeterson.app.ui.components;

import de.derpeterson.app.config.AdminConfigGroup;

/** Gemeinsame Icon-Zuordnung für Konfigurations-Cards und deren Navigation. */
public final class ConfigGroupIcons {
  private ConfigGroupIcons() {}

  public static AppIcon forGroup(AdminConfigGroup configGroup) {
    return switch (configGroup) {
      case GENERAL, SMTP -> AppIcon.COG;
      case SECURITY -> AppIcon.LOCK;
      case TWO_FACTOR -> AppIcon.SHIELD_KEYHOLE;
      case EXTERNAL_LOGIN -> AppIcon.CLOUD_LOCK;
      case TOKENS -> AppIcon.KEY;
      case USER_STATUS -> AppIcon.USER;
      case EMAIL_SENDER -> AppIcon.ENVELOPE;
      case NOTIFICATIONS -> AppIcon.PAPERPLANE;
      case QUEUE -> AppIcon.LIST;
    };
  }
}
