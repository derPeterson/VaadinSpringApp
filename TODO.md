# Offene Aufgaben

## Aktive Planung: Zwei-Faktor-Authentifizierung

- [ ] Die technisch umgesetzte [2FA-Erweiterung](docs/planung/zwei-faktor-bestaetigung.md)
  nach den automatisierten Prüfungen gemeinsam abnehmen. Dazu gehören die
  Oberflächen in DE/EN, echte Authenticator-Bedienung, Ersatzcode-PDF und
  Superadminrechte für 2FA-Regeln und Anmeldeanbieter. Details: [HUM-09](docs/manual-tests/README.md#hum-09).
  Ergebnisse und Grenzen stehen im [Prüfstand](docs/status.md).

## Separate spätere Umsetzung: Vaadin-Komponenten

- [ ] [Vaadin-Badges und weitere Komponenten](docs/planung/vaadin-komponenten-abgleich.md)
  als eigene Umsetzung starten, Pakete priorisieren und deren Umfang freigeben.
  Die Analyse und Einschätzung sind abgenommen; der Bausteinumbau ist noch nicht beauftragt.
  Er ist vom abgeschlossenen Google-/Microsoft-Login und der 2FA-Umsetzung getrennt.

## Entwicklungsablauf

- [ ] Codex später gemeinsam in IntelliJ über JetBrains Air ausprobieren.
  Den Chat „Allgemeine Entwicklung“ öffnen und an einer kleinen Aufgabe
  testen, wie sich markierter Code als Kontext mitgeben und der Diff direkt
  in der IDE prüfen lässt. Bei einem passenden Laufzeitfehler auch den
  IntelliJ-Debugger ausprobieren.
- [ ] Auf dem Feature-Branch die relevanten Projektdateien mit SonarQube for IDE
  analysieren, berechtigte Findings beheben und die betroffenen Dateien erneut
  prüfen.

## Menschliche Abnahmen

- [ ] Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus und Screenreader.
- [ ] Verständlichkeit der deutschen und englischen Texte durch einen Menschen.

Anleitungen: [Manuelle Sonderprüfungen](docs/manual-tests/README.md).
Offene automatisierte Testaufgaben: [Testbedarf](docs/testbedarf.md).

Erledigte Aufgaben entfernen; aktuelle Funktionen in der Fachdokumentation beschreiben.
