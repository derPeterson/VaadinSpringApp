# Offene Aufgaben

## Laufende Umsetzung

- [ ] [Vaadin-Komponenten und vereinbarte Portalpakete](docs/planung/vaadin-komponenten-abgleich.md)
  weiterführen. Noch offene Komponentenpakete gemeinsam ansehen beziehungsweise
  nach Freigabe umsetzen. Ergebnisse stehen im [Prüfstand](docs/status.md).
  Die ausdrücklich zurückgestellten
  Pakete bleiben bis zu einer späteren Freigabe offen.
  Die nächsten Karten-, Bestätigungsdialog- und Mailqueue-Bereinigungspakete sind in der
  [Nachtarbeitsliste](docs/planung/nachtarbeit.md) gesammelt;
  vollständig zur Umsetzung freigegeben. Danach folgen die lokale
  Zwischenintegration nach `main`, vollständige Sonar-Bereinigung, ein neuer
  Folgebranch und zuletzt die große Testrunde. Die Gesamtumsetzung bleibt offen.

## Zurückgestellte Planungen

- [ ] [Release-Ablauf](docs/planung/releases.md) und [Was ist neu?](docs/planung/neuigkeiten.md)
  später auf eigenen Auftrag umsetzen. Die Planungen sind durchgelesen.
  Am Ende der laufenden Gesamtumsetzung entscheiden, ob ein erster Release
  erstellt werden soll; noch kein Releaseauftrag.
- [ ] [Passkey-Planung](docs/planung/passkeys.md) bei einer späteren Freigabe
  erneut gegen den aktuellen Stand prüfen und umsetzen. Planung durchgelesen,
  Umsetzung zurückgestellt; bestehende 2FA beibehalten.
- [ ] [Öffentlichen Testbetrieb](docs/planung/oeffentlicher-testbetrieb.md) später
  auf eigenen Auftrag einrichten. Planung durchgelesen; Anbieter, Domain, SMTP
  und Datenhaltung vor der Bereitstellung entscheiden.

## Künftige Anwendungsfunktionen

- [ ] Anwendungshintergrund nach mehrtägigem Ausprobieren auswählen:
  **Advanced** („Horizont Plus“) ist zum Ausprobieren ausgewählt, der bisherige
  Hintergrund bleibt als **Base** erhalten. Über `ACTIVE_VARIANT` in
  `CosmosBackgroundComponent` zwischen den eingebundenen Varianten wechseln.
  Nach der Entscheidung ungenutzte Varianten samt Dekorationsebenen, Farb-/
  Verlaufstokens und Umschaltlogik entfernen und die Theme-Dokumentation bereinigen.
  Weitere Vorschläge nur vorübergehend für den Vergleich ergänzen.
  Keine Aufgabe der gesammelten Nachtarbeit.

- [ ] Eine sichtbare Verlaufsliste für Bereinigungsläufe der E-Mail-Queue gemeinsam
  entwerfen. Darstellung, Umfang und Bedienung vor der Umsetzung abstimmen.
  Technische Speicherung und konfigurierbare Aufbewahrung sind bereits vorhanden;
  siehe [Mailqueue-Vertrag](docs/mailqueue.md#ergebnisse-und-wiederholung).
  Die History-Oberfläche bleibt eine eigene spätere Umsetzung.

- [ ] Nutzen und Aufwand der Rolle GUEST bei späteren Funktionserweiterungen
  erneut bewerten. Ursprünglich für „Als Gast anmelden“ gedacht, aktuell ohne
  konkreten Anwendungsfall für einen solchen Zugang. Vorerst erhalten und keinen
  zusätzlichen Gastablauf bauen. Bei der Bewertung berücksichtigen, ob die Rolle
  Berechtigungen, 2FA-Regeln, Oberflächen und Tests unnötig verkompliziert;
  Beibehaltung, konkrete Nutzung oder Entfernung dann bewusst entscheiden.
  [Aktueller Rollenstand](docs/rollen.md#gast--guest). Keine Nachtaufgabe.

- [ ] Allgemeine Supportanfragen für Benutzer ermöglichen, unabhängig von
  Problemen mit der Zwei-Faktor-Authentifizierung. Beispielsweise Hilfe bei
  fehlgeschlagener Kontodeaktivierung oder Kontolöschung anbieten. Den Zugang
  auch für nicht mehr angemeldete Benutzer und nötige Missbrauchsgrenzen bei
  einer späteren Planung klären. Gehört nicht zur laufenden Umsetzung oder
  zu den gesammelten Nachtaufgaben.

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
