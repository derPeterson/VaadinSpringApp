# Offene Aufgaben

## Als Nächstes

- [ ] Als nächste separate Umsetzung die [2FA-Bestätigung](docs/planung/zwei-faktor-bestaetigung.md)
  planen und umsetzen. Eingerichtete 2FA muss sensible Kontoaktionen bestätigen und
  darf nicht durch den schwächeren Google-Durchlauf umgangen werden.
  Bekannte Sicherheitsschwäche und Risiko stehen in [BUGS.md](BUGS.md).
- [ ] Nach Design-Merge und Sonar-Bereinigung eine große Testrunde auf `main` durchführen.

## Aktives Feature

- [ ] Google-/Microsoft-Anmeldung und freiwillige Profilangaben nach der noch offenen
  [Stitch-Designentscheidung](docs/design/stitch-kritik-anmeldeanbieter-anmeldung.md)
  abschließen. Die Kontosicherheit und Google-Zwischenlösung sind händisch abgenommen.
  Review, Commit, Integration nach `main`, Sonar und große QA sind beauftragt.
  Der [Umsetzungsplan](docs/planung/google-microsoft-anmeldung.md) bleibt ausdrücklich
  auch nach dem Merge erhalten. Bis zur Entscheidung keine weiteren Änderungen aus
  der Kritik übernehmen. Technischer Gesamtstand: [Prüfstand](docs/status.md).

## Separate spätere Umsetzung: Vaadin-Komponenten

- [ ] [Vaadin-Badges und weitere Komponenten](docs/planung/vaadin-komponenten-abgleich.md)
  als eigene Umsetzung starten, Pakete priorisieren und deren Umfang freigeben.
  Die Analyse und Einschätzung sind abgenommen; der Bausteinumbau ist noch nicht beauftragt.
  Er gehört nicht zur laufenden Google-/Microsoft-Umsetzung und ist keine Voraussetzung
  für deren Abnahme oder Abschluss.

## Menschliche Abnahmen

- [ ] Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus und Screenreader.
- [ ] Verständlichkeit der deutschen und englischen Texte durch einen Menschen.

Anleitungen: [Manuelle Sonderprüfungen](docs/manual-tests/README.md).
Offene automatisierte Testaufgaben: [Testbedarf](docs/testbedarf.md).

Erledigte Aufgaben entfernen; aktuelle Funktionen in der Fachdokumentation beschreiben.
