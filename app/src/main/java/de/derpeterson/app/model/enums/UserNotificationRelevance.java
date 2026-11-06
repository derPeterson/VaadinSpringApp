package de.derpeterson.app.model.enums;

/** Fachlicher Hinweiszustand, unabhängig davon, ob der Empfänger ihn gelesen hat. */
public enum UserNotificationRelevance {
  ACTIVE,
  TICKET_ASSIGNED,
  REQUESTER_CLOSED,
  TICKET_CLOSED
}
