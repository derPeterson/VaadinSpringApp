package de.derpeterson.app.model.enums;

/** Nachweis der aktuellen Adresse; eine Kontofreigabe ist kein E-Mail-Nachweis. */
public enum EmailVerificationStatus {
  VERIFIED,
  PENDING,
  NOT_CHECKED
}
