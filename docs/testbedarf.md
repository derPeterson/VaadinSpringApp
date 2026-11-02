# Offener Testbedarf

## Browser-Tests

- **Ersatzcodeausgabe und Sprachwechsel: wartend auf die abschließende 2FA-Prüfung.**
  Der angepasste Lifecycle-Fall prüft dieselben Codes und die bestätigte Sicherung
  nach DE/EN-Wechsel sowie die übersetzte native Downloadaktion. Den vorhandenen
  Lifecycle-Fall mit dem aktuellen Image ausführen; der gezielte PDF-Nachtest
  ersetzt diesen weiteren Ablauf nicht.

- **QR-Einrichtungsscreen mit OTP-Eingabe: wartend auf abschließende UI-Prüfung.**
  Nur diesen Screen in Vaadin und HTML auf Desktop/Smartphone in DE/EN prüfen:
  Standard-Contentfarbe, QR/manueller Schlüssel, App-Links, beide Schrittüberschriften,
  42 px hohe Methodentabs, mittiger QR-Code, schreibgeschütztes Schlüsselfeld und
  Kopieren ohne Gruppierungsleerzeichen einschließlich Erfolgs-/Fehlermeldung,
  native zweizeilige schreibgeschützte TextArea mit mittiger Anordnung,
  normale Feldbeschriftung mit ausschließlich blauem Icon und Suffixbutton sowie dauerhaft leerer
  Flow-Hostwert trotz sichtbarem Schlüssel (auch nach Fokus, Tab- und Sprachwechsel),
  Erhalt des manuellen Tabs bei DE/EN-Wechsel und Pflichtfeldpunkt wie beim
  bestehenden Feldstandard sowie externe App-Links und RFC-6238-Hinweis,
  sechs sichtbare Kästchen ohne Überlauf, Einfügen von `123456` und `123 456`,
  dabei kein zusätzlich sichtbarer Gesamtcode über dem ersten Kästchen,
  Ersetzen, Rücktaste und Pfeiltasten sowie explizite Aktivierung und rote Fehlermarkierung.
  Die bestehenden Einrichtungsfälle an den neuen OTP-Baustein anpassen; längere
  Ersatzcodeeingaben erhalten. Der Benutzer hat diese Prüfung bis zum Ende der
  Oberflächenanpassungen zurückgestellt und keine erneute komplette 2FA-Runde beauftragt.
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

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
