package de.derpeterson.app.model.enums;

public enum UserNotificationType {
  // Vom Superadmin verfasste Mitteilung.
  ADMIN_ANNOUNCEMENT,
  PASSWORD_CHANGED,
  PASSWORD_RESET,
  EMAIL_CHANGED,
  ROLES_CHANGED,
  RECOVERY_REQUEST,
  SUPPORT_REPLY,
  SUPPORT_REQUESTER_CLOSED
}
