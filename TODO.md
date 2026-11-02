# Offene Aufgaben

## Aktive Planung: Zwei-Faktor-Authentifizierung

- [ ] Den abgestimmten [2FA-Plan](docs/planung/zwei-faktor-bestaetigung.md) umsetzen:
  TOTP, Ersatzcodes, abgesicherte Hilfe und eine ausschließlich für Superadmins
  zugängliche Konfiguration nach Rollen, Funktionen und Nachweisfristen.
  Kleine gemeinsame Ticketgrundlage zunächst für Wiederherstellungsanfragen
  berücksichtigen. Admins bearbeiten, Superadmins geben die Wiederherstellung frei.
  Ersatzcodes als druckbare PDF aus HTML-/CSS-Vorlage mit OpenPDF-html, Service-Logo
  und Anleitung anbieten. Vorbereitete Hilfeantwort per Ticket
  und E-Mail vorsehen. Ohne gültigen Nachweis bleibt der Zugang gesperrt, keine Sicherheitsfragen.
  Spätere Feedback-/Problemmeldungen nutzen diese Grundlage als eigene Erweiterung.
  Google-Umgehungswege bei eingerichteter 2FA absichern. Ersteinrichtung ohne Faktor
  mit Google-Bestätigung, E-Mail-Code und erstem TOTP samt akzeptiertem Restrisiko
  umsetzen. Registrierung und Konten ohne 2FA berücksichtigen. Die ausdrücklich
  gewünschte Entwicklungs-Ausnahme nur nach ausdrücklicher Startfreigabe über
  Umgebungsvariable anbieten, einzeln schaltbar halten und warnen.
  Den manuellen Datenbank-Notfallweg für den letzten Superadmin dokumentieren und
  isoliert prüfen, keine zusätzliche Reparaturoberfläche oder verstecktes Notfallkonto.
  Anbieterverwaltung im selben Paket ausschließlich für Superadmins zugänglich machen.
  Der bestehende Fehler bleibt bis zur Behebung und Nachprüfung in [BUGS.md](BUGS.md).

## Separate spätere Umsetzung: Vaadin-Komponenten

- [ ] [Vaadin-Badges und weitere Komponenten](docs/planung/vaadin-komponenten-abgleich.md)
  als eigene Umsetzung starten, Pakete priorisieren und deren Umfang freigeben.
  Die Analyse und Einschätzung sind abgenommen; der Bausteinumbau ist noch nicht beauftragt.
  Er ist vom abgeschlossenen Google-/Microsoft-Login und der 2FA-Umsetzung getrennt.

## Menschliche Abnahmen

- [ ] Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus und Screenreader.
- [ ] Verständlichkeit der deutschen und englischen Texte durch einen Menschen.

Anleitungen: [Manuelle Sonderprüfungen](docs/manual-tests/README.md).
Offene automatisierte Testaufgaben: [Testbedarf](docs/testbedarf.md).

Erledigte Aufgaben entfernen; aktuelle Funktionen in der Fachdokumentation beschreiben.
