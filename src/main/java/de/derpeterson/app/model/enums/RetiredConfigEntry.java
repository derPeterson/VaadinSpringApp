package de.derpeterson.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Alte Datenbankschlüssel, die beim Start gelöscht und nicht erneut angelegt werden. */
@Getter
@RequiredArgsConstructor
public enum RetiredConfigEntry {
  MAIL_SMTP_SOCKETFACTORY_CLASS("mail.smtp.socketFactory.class");

  private final String key;
}
