# Offener Testbedarf

## Browser-Tests

- **Verpflichtende Ersteinrichtung, Variante A (MFA-17 / MFA-06): offen.**
  Die ergänzten Fälle auf Desktop und Smartphone in DE/EN gezielt ausführen:
  neue Karte und HTML-Referenz, Abmelden ohne Portalzugriff sowie direkter
  QR-Übergang nach frischer Passwortanmeldung ohne zweite Passwortabfrage.
  Die zusätzlich gestartete umfassende 2FA-Runde wurde auf Benutzerwunsch
  gestoppt. Einzelne erfolgreiche Altfälle ersetzen diesen gezielten Nachtest nicht.
  Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md).
- **Verschlüsselte Anbieter-Secrets: offen.** Beim nächsten beauftragten
  Browserlauf das aktuelle Image mit frischem Schema und dem getrennten
  Testschlüssel verwenden. Google-/Microsoft-Anmeldung mit dem eigenen
  OIDC-Testanbieter, Speichern ohne neues Secret und Anwendungsneustart prüfen.
  Erwartung: Die ursprünglichen Zugangsdaten funktionieren weiterhin. Keine
  echten Anbieterzugänge verwenden. Die Verschlüsselungs- und Persistenzgrenzen
  sind bereits durch erfolgreiche JUnit-Fälle abgedeckt.

## JUnit-Tests

Derzeit keine offenen Ergänzungen. Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
