package de.derpeterson.app.model.enums;

/** Persistenter Zustand eines logischen Bereinigungslaufs; Warten ist kein Fehlschlag. */
public enum EmailQueueCleanupStatus {
  WAITING,
  RUNNING,
  COMPLETED,
  FAILED,
  PARTIAL;

  public boolean isActive() {
    return this == WAITING || this == RUNNING;
  }
}
